import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;


class Test{
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);
        JsonObject object = new JsonObject().put("datatosend",
                new JsonArray()
                        .add("iolinkmaster/port[1]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[2]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[3]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[4]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[5]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[6]/iolinkdevice/pdin")
                        .add("iolinkmaster/port[7]/iolinkdevice/pdin"));
//                        .add("iolinkmaster/port[8]/iolinkdevice/pdin"));
        JsonObject json_new = new JsonObject().put("code", "request")
                .put("cid", 4711)
                .put("adr", "/getdatamulti")
                .put("data", object);
//        System.out.println(json_new);
        String address = "192.168.49.239";
        vertx.setPeriodic(200, run -> {
            client.post(80, address, "/")
                    .putHeader("content-type", "application/json")
                    .putHeader("cache-control", "no-cache")
                    .sendJsonObject(json_new, ar -> {
                        if (ar.succeeded()) {
                            Buffer body = ar.result().body();
                            if (body.getString(2, body.length()-2).equals("400 Bad Request")) {
                                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " 400 Bad Request");
                                System.out.println("---------------");
                            } else {
                                for (int i = 2; i < 8; i++) {
                                    String res = ar.result().bodyAsJsonObject().getJsonObject("data")
                                            .getJsonObject("iolinkmaster/port[" + i + "]/iolinkdevice/pdin")
                                            .getString("data");
                                    if (res != null)
                                        System.out.println(Integer.parseInt(res.substring(0, 3), 16));
                                }
                                System.out.println("---------------");
                            }
                        }
                    });
        });
    }
}