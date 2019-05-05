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
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

class ScannerVerticle extends AbstractVerticle {
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
    private ArrayList<ArrayList<Integer>> tempData;
    private ModbusTcpMasterConfig config_1 = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5000)
            .build();
    private ModbusTcpMasterConfig config_2 = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5001)
            .build();
    private ModbusTcpMaster master_1 = new ModbusTcpMaster(config_1);
    private ModbusTcpMaster master_2 = new ModbusTcpMaster(config_2);
    private CompletableFuture<ReadHoldingRegistersResponse> future_1;
    private CompletableFuture<WriteMultipleRegistersResponse> future_2;
    private ExecutorService executor = Executors.newFixedThreadPool(16);

    ScannerVerticle(String[] host, Controller controller, DataBaseConnect dataBaseConnect) {
        this.host = host;
        this.controller = controller;
        this.dataBaseConnect = dataBaseConnect;
    }

    @Override
    public void start() {
        vertx.setPeriodic(10000, event -> vibroIndication());
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
                    client.post(80, address, "/")
                            .putHeader("content-type", "application/json")
                            .putHeader("cache-control", "no-cache")
                            .sendJsonObject(json, ar -> {
                                if (ar.succeeded()) {
                                    Buffer body = ar.result().body();
                                    if (body.getString(2, body.length()-2).equals("400 Bad Request")) {
                                        System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " 400 Bad Request");
                                    } else {
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
                                    System.out.println("\u001B[33m" + "AL ERROR" + "\u001B[0m" + " " + ar.cause().getMessage());
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

    private void handle() {

        ArrayList<Integer> tempAll = new ArrayList<>();
        Collections.reverse(tempData.get(1));
        tempAll.addAll(tempData.get(0));
        tempAll.addAll(tempData.get(1));
        double[][] tempVal = controller.doSlice(tempAll);
        future_1.whenComplete((res, ex) -> {
            if (res!=null){
                int output = parse.uByteToInt(new short[]{res.getRegisters().getUnsignedByte(8),
                        res.getRegisters().getUnsignedByte(9),
                        res.getRegisters().getUnsignedByte(10),
                        res.getRegisters().getUnsignedByte(11)});
                conveyorCheck(output);
            } else
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
        });
        if (controller.woodLog && processWood) {
            controller.figure.add(tempVal);
            check = true;
        } else {
            if (check && processWood) {
                System.out.println("+++++++Compute wood!+++++++");
                compute(controller.figure);
                controller.figure.clear();
                check = false;
            }
        }
    }

    private void conveyorCheck(int currentPosition){
        if (currentPosition > oldPosition){
            oldPosition = currentPosition;
            processWood = true;
            System.out.println("Conveyor run");
        } else {
            processWood = false;
//            System.out.println("Conveyor stop");
        }
    }

    private void woodParamsToDatabase(double inputRad, double outputRad,
                                      double volume, double usefullVolume, double curvature){
        double avgRad = (inputRad + outputRad) / 2;
        JsonArray dataArray = new JsonArray().add(curvature).add(inputRad).add(outputRad)
                .add(avgRad).add(volume).add(usefullVolume);
        dataBaseConnect.databaseWrite("INSERT INTO woodParams (curvature, inputRad," +
                " outputRad, avrRad, volume, usefulVolume, timeStamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP());", dataArray);
    }

    private synchronized void compute (ArrayList<double[][]> figure) {
        final ArrayList<double[][]> tempFigure = new ArrayList<>(figure);
        ArrayList<CompletableFuture<double[]>> futureResultList = new ArrayList<>();
        double[] rads = new double[14];
        double[][] circleCentres = new double[14][2];
        if (tempFigure.size() >= 16){
            System.out.println("Figure Size >= 16");
//            futureResultList.add(CompletableFuture.supplyAsync(() ->
//                    controller.computeRadius(tempFigure.get(1)), executor)
//                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS).exceptionally(ex -> {
//                        ex.getCause().printStackTrace();
//                        System.out.println(ex.getMessage());
//                        return null;
//                    }));
            for (int i = 1; i < 14; i++){
                int index = i;
                futureResultList.add(CompletableFuture.supplyAsync(() ->
                        controller.computeRadius(tempFigure.get(index)), executor)
                        .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
            }
            futureResultList.add(CompletableFuture.supplyAsync(() ->
                    controller.computeRadius(tempFigure.get(tempFigure.size()-2)), executor)
                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
        }else if (tempFigure.size() >= 5){
            System.out.println("Figure Size >= 5");
//            for (int i = 1; i < tempFigure.size()-2; i++){
//                int index = i;
//                futureResultList.add(CompletableFuture.supplyAsync(() ->
//                        controller.computeRadius(tempFigure.get(index)))
//                        .completeOnTimeout(new double[3],1, TimeUnit.SECONDS).exceptionally(ex -> {
//                            ex.getCause().printStackTrace();
//                            System.out.println(ex.getMessage());
//                            return null;
//                        }));
//            }

            futureResultList.add(CompletableFuture.supplyAsync(() ->
                    controller.computeRadius(tempFigure.get(1)), executor)
                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
//            for (int i = 2; i < tempFigure.size()-2; i++){
//                int index = i;
//                futureResultList.add(CompletableFuture.supplyAsync(() ->
//                        controller.computeRadius(tempFigure.get(index)))
//                        .completeOnTimeout(new double[3],1, TimeUnit.SECONDS).exceptionally(ex -> {
//                            ex.getCause().printStackTrace();
//                            System.out.println(ex.getMessage());
//                            return null;
//                        }));
//            }
//            for (int i = 0; i < 16 - tempFigure.size(); i++){
//                int index  = i+tempFigure.size()-2;
//                futureResultList.add(CompletableFuture.supplyAsync(() -> {
//                    System.out.println("START: "+index+" --------"+(tempFigure.size()-3));
//                    return controller.computeRadius(tempFigure.get(tempFigure.size()-3));
//                }).completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
//            }
            futureResultList.add(CompletableFuture.supplyAsync(() ->
                    controller.computeRadius(tempFigure.get(tempFigure.size()-2)), executor)
                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
        }
        if (tempFigure.size() >= 5) {
            System.out.println("Figure Size >= 5, enter to doAll future");
            futureResultList.add(CompletableFuture.supplyAsync(() ->
                    controller.figureVolume(tempFigure, (double) 1680 / tempFigure.size()), executor)
                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
            futureResultList.add(CompletableFuture.supplyAsync(() ->
                    controller.usefulVolume(tempFigure), executor)
                    .completeOnTimeout(new double[3],1, TimeUnit.SECONDS));
            CompletableFuture[] futureResultArray = futureResultList.toArray(new CompletableFuture[0]);

            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureResultArray);

            CompletableFuture<List<double[]>> finalResults = combinedFuture
                    .thenApply(val ->
                            futureResultList.stream().map(CompletableFuture::join).collect(Collectors.toList()));

            finalResults.whenComplete((res, throwable) -> {
                if(throwable != null) throwable.printStackTrace();
                System.out.println("Futures done!");
                if (res.size() < 16)
                    System.out.println("Futures count: " + res.size());
                double averageX = 0, averageY = 0, averageR = 0;
//                for (int i = 0; i < 14; i++) {
//                    rads[i] = res.get(i)[0];
//                    circleCentres[i][0] = res.get(i)[1];
//                    circleCentres[i][1] = res.get(i)[2];
//                }
//                for (int n = 0; n < 2; n++) {
//                    for (int i = 0; i < 14; i++) {
//                        averageX += circleCentres[i][0];
//                        averageY += circleCentres[i][1];
//                        averageR += rads[i];
//                    }
//                    averageX = averageX / 14;
//                    averageY = averageY / 14;
//                    averageR = averageR / 14;
//                    for (int i = 0; i < 14; i++) {
//                        if (circleCentres[i][0] / averageX > 1.1 || circleCentres[i][0] / averageX < 0.9) {
//                            circleCentres[i][0] = averageX;
//                        }
//                        if (circleCentres[i][1] / averageY > 1.1 || circleCentres[i][1] / averageY < 0.9) {
//                            circleCentres[i][1] = averageY;
//                        }
//                        if (rads[i] / averageR > 1.15 || rads[i] / averageR < 0.85) {
//                            rads[i] = averageR;
//                        }
//                    }
//                    averageX = 0;
//                    averageY = 0;
//                }
//                for (int i = 0; i < 14; i++) {
//                    averageX += circleCentres[i][0];
//                    averageY += circleCentres[i][1];
//                }
//                averageX = averageX / 14;
//                averageY = averageY / 14;
//                for (int i = 0; i < 14; i++) {
//                    if (circleCentres[i][0] / averageX > 1.03 || circleCentres[i][0] / averageX < 0.97) {
//                        circleCentres[i][0] = averageX;
//                    }
//                    if (circleCentres[i][1] / averageY > 1.03 || circleCentres[i][1] / averageY < 0.97) {
//                        circleCentres[i][1] = averageY;
//                    }
//                }
                System.out.println("==================================================");
                for (int i = 0; i < 2; i++)
                    System.out.println(rads[i] + "  " + circleCentres[i][0] + "  " + circleCentres[i][1]);
                dataBaseConnect.mySQLClient.getConnection(con -> {
                    if (con.succeeded()) {
                        SQLConnection connection = con.result();
                        connection.query("TRUNCATE woodData_3;", result -> {
                            connection.close();
                            if (result.succeeded())
                                for (int i = 0; i < 2; i++) {
                                    JsonArray toDatabase = new JsonArray().add(i + 1)
                                            .add((double) Math.round(circleCentres[i][0] * 10) / 10)
                                            .add((double) Math.round(circleCentres[i][1] * 10) / 10)
                                            .add((double) Math.round(rads[i] * 1.2 * 10) / 10);
                                    dataBaseConnect.databaseWrite("INSERT INTO woodData_3 VALUES (?, ?, ?, ?)",
                                            toDatabase);
                                }
                        });
                    }
                });

                double inputRad = (double) Math.round(res.get(0)[0] * 2.2 * 10) / 10,
                        outputRad = (double) Math.round(res.get(res.size() - 3)[0] * 2.2 * 10) / 10,
//                double inputRad = rads[0] * 2.3, outputRad = rads[13] * 2.3;
                        volume = (double) Math.round(res.get(res.size() - 2)[0] * 0.38 / 1000) / 1000,
                        usefulVolume = (double) Math.round(res.get(res.size() - 1)[0] * 0.48 / 1000) / 1000,
                        curvature = res.get(res.size()-1)[1];
                System.out.println("Input Diameter: " + inputRad);
                System.out.println("Output Diameter: " + outputRad);
                System.out.println("Figure Volume: " + volume);
                System.out.println("Usefull Volume: " + usefulVolume);
                System.out.println("Curvature: " + curvature);
//                woodParamsToDatabase(inputRad, outputRad, volume, usefulVolume, curvature);
            });
        }
    }

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
                        future_2 = master_2.sendRequest(
                                new WriteMultipleRegistersRequest(2, 2, byteArray), 2);
                        future_2.whenComplete((res_1, ex_1) -> {
                            if (res_1 == null)
                                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex_1.getMessage());
                        });
                    } else
                        System.out.println("\u001B[33m" + "Write Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }

}
