import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

class SomeVerticle extends AbstractVerticle {
    private Controller controller;
    private final String[] host;
    private final int port;
    private ArrayList<double[][]> localList = new ArrayList<>();
    private int counter = 0;

    SomeVerticle(String[] host, int port, Controller controller) {
        this.host = host;
        this.port = port;
        this.controller = controller;
    }

    private ArrayList<ArrayList<Integer>> tempData;

    @Override
    public void start() {

        vertx.setPeriodic(1000, event -> {
//            WebClient client = WebClient.create(vertx);
            System.out.println("TICK");
            if (counter == 0) {
                tempData = new ArrayList<>();
                tempData.add(new ArrayList<>());
                tempData.add(new ArrayList<>());
                for (int k = 0; k < host.length; k++) {
//            System.out.println("HOST: " + h);
                    for (int i = 1; i < 9; i++) {
                        WebClient client = WebClient.create(vertx);
                        requestAndResponse(client, i, host[k], tempData.get(k));
                    }
//                    for (int i = 5; i < 9; i++) {
//                        requestAndResponse(client_2, i, host[k], tempData.get(k));
//                    }
                }
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                client_1.close();
            }
//            client_1.close();
//            client_2.close();
        });
//        vertx.setPeriodic(700, event -> {
//
//        });
    }

    private void requestAndResponse(WebClient client, int dataID, String address, ArrayList<Integer> outMass) {
        counter++;
        System.out.println("IncCounter: " + counter);
        String adr = "/iolinkmaster/port[" + dataID + "]/iolinkdevice/pdin/getdata";
        JsonObject json = new JsonObject().put("code", 10)
                .put("cid", 4711)
                .put("adr", adr);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.post(port, address, "/")
                .putHeader("content-type", "application/json")
                .putHeader("cache-control", "no-cache")
                .sendJsonObject(json, ar -> {
                    if (ar.failed()) {
                        System.out.println(ar.cause().getMessage());
                    } else {
                        try {
                            String res = ar.result().body().toJsonObject().getJsonObject("data").getString("value");
                            int output = Integer.parseInt(res.substring(0, 3), 16);
//                            System.out.println("out: " + output);
                            outMass.add(output);
                            counter--;
                            System.out.println("DecCount: " + counter);
                        } catch (NullPointerException e) {
                            System.out.println("Null ********");
                            outMass.add(null);
                        }
                        if (counter == 4) handle(client);
                        client.close();
                    }
                });
    }

    private void handle(WebClient client) {
        ArrayList<Integer> tempAll = new ArrayList<>();
        Collections.reverse(tempData.get(1));
        tempAll.addAll(tempData.get(0));
        tempAll.addAll(tempData.get(1));
        controller.outData.add(controller.doSlice(tempAll));
//        for (double[][] val : controller.outData) {
//            System.out.println("Out: " + Arrays.deepToString(val));
//        }
        for (Integer val : tempAll) {
            System.out.println("Temp: " + val);
        }
        for (double[][] val : localList) {
            System.out.println("Local: " + Arrays.deepToString(val));
        }
        System.out.println("===");
//        client.close();
    }
}