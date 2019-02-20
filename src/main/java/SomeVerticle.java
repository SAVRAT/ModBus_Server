import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;

class SomeVerticle extends AbstractVerticle {
    private Controller controller;
    private final String[] host;
    private final int port;
    private int counter = 0;

    SomeVerticle(String[] host, int port, Controller controller) {
        this.host = host;
        this.port = port;
        this.controller = controller;
    }

    private ArrayList<ArrayList<Integer>> tempData;

    @Override
    public void start() {
        vertx.setPeriodic(100, event -> {
//            System.out.println("TICK");
            if (counter == 0) {
                tempData = new ArrayList<>();
                tempData.add(new ArrayList<>());
                tempData.add(new ArrayList<>());
                for (int k = 0; k < host.length; k++) {
                    WebClient client = WebClient.create(vertx);
                    requestAndResponse(client, host[k], tempData.get(k));
                }
            }
        });
    }

    private void requestAndResponse(WebClient client, String address, ArrayList<Integer> outMass) {
        counter++;
        JsonObject object = new JsonObject().put("datatosend",
                new JsonArray().add("iolinkmaster/port[1]/iolinkdevice/pdin")
                .add("iolinkmaster/port[2]/iolinkdevice/pdin")
                .add("iolinkmaster/port[3]/iolinkdevice/pdin")
                .add("iolinkmaster/port[4]/iolinkdevice/pdin")
                .add("iolinkmaster/port[5]/iolinkdevice/pdin")
                .add("iolinkmaster/port[6]/iolinkdevice/pdin")
                .add("iolinkmaster/port[7]/iolinkdevice/pdin")
                .add("iolinkmaster/port[8]/iolinkdevice/pdin"));
        JsonObject json_new = new JsonObject().put("code", 10)
                .put("cid", 4711)
                .put("adr", "/getdatamulti")
                .put("data", object);

        client.post(port, address, "/")
                .putHeader("content-type", "application/json")
                .putHeader("cache-control", "no-cache")
                .sendJsonObject(json_new, ar -> {
                    if (ar.failed()) {
                        System.out.println(ar.cause().getMessage());
                    } else {
                        Integer out;
                        for (int i=1; i<9; i++) {
                            String res = ar.result().bodyAsJsonObject().getJsonObject("data")
                                        .getJsonObject("iolinkmaster/port[" + i + "]/iolinkdevice/pdin").getString("data");
                                if (res != null)
                                    out = Integer.parseInt(res.substring(0, 3), 16);
                                else out = null;
                            outMass.add(out);
                        }
                        counter--;
                        if (counter == 0) handle();
                        client.close();
                    }
                });
    }

    private void handle() {
        ArrayList<Integer> tempAll = new ArrayList<>();
        Collections.reverse(tempData.get(1));
        tempAll.addAll(tempData.get(0));
        tempAll.addAll(tempData.get(1));
        if (tempData.isEmpty())
            System.out.println("No data...");
        controller.outData.add(controller.doSlice(tempAll));
//        System.out.println("Data: " + tempAll);
    }
}