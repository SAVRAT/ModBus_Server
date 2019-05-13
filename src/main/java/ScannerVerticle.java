import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.WriteMultipleRegistersResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ScannerVerticle extends AbstractVerticle implements SystemLog{
    private Controller controller;
    private DataBaseConnect dataBaseConnect;
    private Parsing parse = new Parsing();
    private final String[] host;
    private int counter = 0;
    private int startAddress = 0;
    private int quantity = 7;
    private int oldPosition = 0;
    private int[] partCounter = new int[2];
    private boolean check = false, processWood = false;
    static ArrayList<double[][]> figure = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> tempData;
    private ModbusTcpMasterConfig config_1 = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5000)
            .build();
    private ModbusTcpMasterConfig config_2 = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5001)
            .build();
    private ModbusTcpMaster master_1 = new ModbusTcpMaster(config_1);
    private ModbusTcpMaster master_2 = new ModbusTcpMaster(config_2);
    private CompletableFuture<ReadHoldingRegistersResponse> future_1;
    private CompletableFuture<WriteMultipleRegistersResponse> future_2;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    ScannerVerticle(String[] host, Controller controller, DataBaseConnect dataBaseConnect) {
        this.host = host;
        this.controller = controller;
        this.dataBaseConnect = dataBaseConnect;
    }

    @Override
    public void start() {
        // раз в 10 секунд запись виброиндикации (лампочки на щите в цеху раскрижёвки)
        vertx.setPeriodic(10000, event -> vibroIndication());
        // каждые 180 милисекунд чтение данных со сканера
        vertx.setPeriodic(180, event -> {
//            System.out.println("TICK");
            if (counter == 0) {
                tempData = new ArrayList<>();
                tempData.add(new ArrayList<>());
                tempData.add(new ArrayList<>());
                for (int k = 0; k < host.length; k++) {
                    WebClient client = WebClient.create(vertx);
                    Integer[] temp = new Integer[8];
                    requestAndResponse(client, host[k], tempData.get(k), temp, k);
                }
                future_1 = master_1.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);
            }
        });
    }

    // чтение данных со сканера (по два запроса на каждую AL1302)
    private void requestAndResponse(WebClient client, String address, ArrayList<Integer> outMass,
                                    Integer[] temp, int num) {
        counter++;
        counter++;
        JsonObject[] jsonObject = {new JsonObject().put("datatosend",
                new JsonArray()
                        .add("iolinkmaster/port[1]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[2]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[3]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[4]/iolinkdevice/pdin")),
                new JsonObject().put("datatosend", new JsonArray()
                        .add("iolinkmaster/port[5]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[6]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[7]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[8]/iolinkdevice/pdin"))};

                for (int i = 0; i < 2; i++) {
                    partCounter[num]++;
                    JsonObject json = new JsonObject().put("code", "request")
                            .put("cid", 4711)
                            .put("adr", "/getdatamulti")
                            .put("data", jsonObject[i]);
                    final int part = i + 1;
                    // делаем запрос
                    client.post(80, address, "/")
                            .putHeader("content-type", "application/json")
                            .putHeader("cache-control", "no-cache")
                            .sendJsonObject(json, ar -> {
                                // пришёл ответ
                                if (ar.succeeded()) {
                                    Buffer body = ar.result().body();
                                    // проверка на статус 400
                                    if (body.getString(2, body.length()-2).equals("400 Bad Request")) {
                                        writeLogString("ERROR, 400 Bad Request");
//                                        System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " 400 Bad Request");
                                    } else {
                                        // если всё Ок
                                        int k = 1;
                                        if (part == 2)
                                            k = 5;
                                        for (int n = k; n <= k+3; n++) {
                                            String res = ar.result().bodyAsJsonObject().getJsonObject("data")
                                                    .getJsonObject("iolinkmaster/port[" + n + "]/iolinkdevice/pdin")
                                                    .getString("data");
                                            if (res != null) {
                                                temp[n-1] = Integer.parseInt(res.substring(0, 3), 16);
                                            } else {
                                                temp[n-1] = null;
                                            }
                                        }
                                    }
                                } else
                                    writeLogString("AL ERROR " + ar.cause() + " :: " + ar.cause().getMessage());
//                                    System.out.println("\u001B[33m" + "AL ERROR" + "\u001B[0m" + " " + ar.cause().getMessage());
                                counter--;
                                partCounter[num]--;
                                if (partCounter[num] == 0) {
                                    outMass.addAll(Arrays.asList(temp));
                                    client.close();
                                }
                                if (counter == 0)
                                   try { handle();} catch (Throwable t){
                                       t.printStackTrace();
                                   }
                            });
                }
//        System.out.println("Increment: " + counter);
//        JsonObject object = new JsonObject().put("datatosend",
//                new JsonArray().add("iolinkmaster/port[1]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[2]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[3]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[4]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[5]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[6]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[7]/iolinkdevice/pdin")
//                .add("iolinkmaster/port[8]/iolinkdevice/pdin"));
//        JsonObject json_new = new JsonObject().put("code", 10)
//                .put("cid", 4711)
//                .put("adr", "/getdatamulti")
//                .put("data", object);
//
//        client.post(port, address, "/")
//                .putHeader("content-type", "application/json")
//                .putHeader("cache-control", "no-cache")
//                .sendJsonObject(json_new, ar -> {
//                    if (ar.failed()) {
//                        System.out.println("\u001B[33m" + "AL ERROR" + "\u001B[0m" + " " + ar.cause().getMessage());
//                    } else {
//                        Integer out;
//                        for (int i=1; i<9; i++) {
//                            String res = ar.result().bodyAsJsonObject().getJsonObject("data")
//                                        .getJsonObject("iolinkmaster/port[" + i + "]/iolinkdevice/pdin")
//                                    .getString("data");
//                                if (res != null)
//                                    out = Integer.parseInt(res.substring(0, 3), 16);
//                                else out = null;
//                            outMass.add(out);
//                        }
//                        counter--;
//                        System.out.println("Decrement: " + counter);
//                        if (counter == 0)
//                            handle();
//                        client.close();
//                    }
//                });
    }
    // метод запускается, когда считаны все данные со сканера
    private void handle() {

        ArrayList<Integer> tempAll = new ArrayList<>();
        // формирование значений с датчиков
        Collections.reverse(tempData.get(1));
        tempAll.addAll(tempData.get(0));
        tempAll.addAll(tempData.get(1));
        double[][] tempVal = controller.doSlice(tempAll);
        // получение ответа от контроллера конвейера
        future_1.whenComplete((res, ex) -> {
            if (res!=null){
                int output = parse.uByteToInt(new short[]{res.getRegisters().getUnsignedByte(8),
                        res.getRegisters().getUnsignedByte(9),
                        res.getRegisters().getUnsignedByte(10),
                        res.getRegisters().getUnsignedByte(11)});
                // проверяем движется ли он
                conveyorCheck(output);
            } else
                writeLogString("ERROR conveyor controller connect " + ex.getMessage());
//                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
        });
        // если конвейер едёт и есть бревно в сканере - считываем
        if (controller.woodLog && processWood) {
            System.out.println("Read wood...");
            figure.add(tempVal);
            check = true;
        } else {
            // если был заход в предыдущий цикл (check = true)
            if (check && processWood) {
                // запускаем в отдельном пуле потоков считаться параметры бревна
                executorService.execute(() -> {
                    ConcurrentComputing concurrentComputing =
                            new ConcurrentComputing(figure, dataBaseConnect);
                    concurrentComputing.computeAsync();
                });
                check = false;
            }
        }
    }

    // проверка движения конвейера
    private void conveyorCheck(int currentPosition){
        if (currentPosition > oldPosition){
            oldPosition = currentPosition;
            processWood = true;
//            System.out.println("Conveyor run");
        } else {
            processWood = false;
        }
    }

    // чтение с базы состояние лампочек
    private void vibroIndication(){
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()){
                SQLConnection connection = con.result();
                connection.query("SELECT * FROM vibroIndication;", res -> {
                    if (res.succeeded()) {
                        JsonArray result = res.result().getResults().get(0);
                        byte[] byteArray = new byte[4];
                        for (int i = 0; i < 27; i++) {
                            if (result.getInteger(i).equals(1)) {
                                if (i < 8)
                                    byteArray[0] += Math.pow(2, i);
                                else if (i < 16)
                                    byteArray[1] += Math.pow(2, i - 8);
                                else if (i < 24)
                                    byteArray[2] += Math.pow(2, i - 16);
                                else
                                    byteArray[3] += Math.pow(2, i - 24);
                            }
                        }
                        // отправляем запрос на запись регистров в ПЛК
                        future_2 = master_2.sendRequest(
                                new WriteMultipleRegistersRequest(2, 2, byteArray), 2);
                        future_2.whenComplete((res_1, ex_1) -> {
                            if (res_1 == null)
                                writeLogString("ERROR while write indication panel " +
                                        ex_1.getMessage());
//                                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex_1.getMessage());
                        });
                    } else
                        writeLogString("ERROR select from vibroindication " + res.cause());
//                        System.out.println("\u001B[33m" + "Write Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else
                writeLogString("DataBase ERROR " + con.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }

}
