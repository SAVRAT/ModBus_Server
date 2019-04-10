import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ScannerVerticle extends AbstractVerticle {
    private Controller controller;
    private DataBaseConnect dataBaseConnect;
    private ExecutorService service = Executors.newFixedThreadPool(4);
    private List<Runnable> tasks = new ArrayList<>();
    private final String[] host;
    private int counter = 0, writeCounter = 0;
    private int[] partCounter = new int[2];
    private boolean check = false;

    ScannerVerticle(String[] host, Controller controller, DataBaseConnect dataBaseConnect) {
        this.host = host;
        this.controller = controller;
        this.dataBaseConnect = dataBaseConnect;
    }

    private ArrayList<ArrayList<Integer>> tempData;

    @Override
    public void start() {
        vertx.setPeriodic(180, event -> {
            System.out.println("TICK");
            if (counter == 0) {
                tempData = new ArrayList<>();
                tempData.add(new ArrayList<>());
                tempData.add(new ArrayList<>());
                for (int k = 0; k < host.length; k++) {
                    WebClient client = WebClient.create(vertx);
                    Integer[] temp = new Integer[8];
                    requestAndResponse(client, host[k], tempData.get(k), temp, k);
                }
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
                                    handle();
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
        if (tempData.isEmpty())
            System.out.println("No data...");
        System.out.println("Handle...");
        double[][] tempVal = controller.doSlice(tempAll);
        controller.outData.add(tempVal);
        if (controller.woodLog) {
            writeCounter++;
            controller.figure.add(tempVal);
            check = true;
            if (writeCounter > 1)
                toDatabase(tempVal);
        } else {
            if (check) {
                writeCounter = 0;
                compute(controller.figure);
                controller.figure.clear();
                dataBaseConnect.mySQLClient.getConnection(con -> {
                    if (con.succeeded()){
                        SQLConnection connection = con.result();
                        connection.query("truncate table woodData_2;", res -> {
                            if (res.succeeded()){
                                connection.query("INSERT INTO woodData_2 SELECT * FROM woodData;", res2 -> {
                                    if (res2.succeeded()){
                                        dataBaseConnect.databaseQuery("truncate table woodData;");
                                    } else System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                                    connection.close();
                                });
                            } else System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                        });
                    } else System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
                });
                check = false;
            }
        }

//        System.out.println(jsonArray);
//        System.out.println("Data: " + tempAll);
    }

    private void toDatabase(double[][] data){
        StringBuilder x_str = new StringBuilder();
        StringBuilder y_str = new StringBuilder();
        for (double[] datum : data) {
            x_str.append(datum[0]).append(",");
            y_str.append(datum[1]).append(",");
        }
        JsonArray dataArray = new JsonArray().add(x_str).add(y_str);
        dataBaseConnect.databaseWrite("INSERT INTO woodData (xData, yData) VALUE (?, ?);", dataArray);
    }

    private void compute (ArrayList<double[][]> figure){
        double[] rads = new double[2];
        tasks.add(() -> rads[0] = controller.computeRadius(figure.get(1)));
        tasks.add(() -> rads[1] = controller.computeRadius(figure.get(figure.size()-2)));
        CompletableFuture<?>[] futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, service))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
        service.shutdown();
        if (service.isShutdown()){
            System.out.println("IN/OUT Rads: " + Arrays.toString(rads));
        }
    }
}