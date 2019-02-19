import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

class VertxWebClient {

    private final String[] host;
    private final int port;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    private ArrayList<double[][]> localList = new ArrayList<>();

    private Controller controller;

    VertxWebClient(String[] host, int port, Controller controller){
        this.host = host;
        this.port = port;
        this.controller = controller;
    }

    void start() {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        ArrayList<ArrayList<Integer>> tempData = new ArrayList<>();
        ArrayList<Integer> tempAll = new ArrayList<>();
        tempData.add(new ArrayList<>());
        tempData.add(new ArrayList<>());
//        System.out.println("TICK");
        for (int k=0; k<host.length; k++) {
            for (int i = 1; i < 9; i++) {
                requestAndResponse(client, i, host[k], tempData.get(k));
            }
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.close();
        Collections.reverse(tempData.get(1));
        tempAll.addAll(tempData.get(0));
        tempAll.addAll(tempData.get(1));
//        System.out.println("first: " + tempAll);
        controller.outData.add(controller.doSlice(tempAll));
        if (tempAll.isEmpty()){
            System.out.println("No data.");
        }
        scheduler.schedule(this::start, 80, TimeUnit.MILLISECONDS);
    }

    private void requestAndResponse(io.vertx.ext.web.client.WebClient client, int dataID, String address,
                                    ArrayList<Integer> outMass){
        String adr = "/iolinkmaster/port["+ dataID + "]/iolinkdevice/pdin/getdata";
        JsonObject json = new JsonObject().put("code", 10)
                .put("cid", 4711)
                .put("adr", adr);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.post(port, address, "/")
                .putHeader("content-type", "application/json")
                .putHeader("cache-control", "no-cache")
                .sendJsonObject(json, ar -> {
                    if (ar.failed()){
                        System.out.println(ar.cause().getMessage());
                    }else {
                        try {
                            String res = ar.result().body().toJsonObject()
                                    .getJsonObject("data").getString("value");
                            int output = Integer.parseInt(res.substring(0, 3), 16);
//                            System.out.println("out: " + output);
                            outMass.add(output);
                        }catch (NullPointerException e){
//                            System.out.println("Null *******");
                            outMass.add(null);
                        }

                    }
                });
    }
}