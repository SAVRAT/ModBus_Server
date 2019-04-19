import io.vertx.core.Vertx;

@SuppressWarnings("Duplicates")
class Test {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Controller controller = new Controller();
        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                "fanDOK");
        String[] alAddress = {"192.168.49.239", "192.168.49.238"};

        vertx.deployVerticle(new ScannerVerticle(alAddress, controller, dataBase));
    }

}