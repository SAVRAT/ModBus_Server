import io.vertx.core.Vertx;

class ServerMain {
    ServerMain(Controller controller) {
        Vertx vertx = Vertx.vertx();
        String[] alAddress = {"192.168.49.239", "192.168.49.238"};
        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                    "fanDOK");
        ModBus_Master modBusMaster = new ModBus_Master(dataBase);
        vertx.deployVerticle(new Vibration(dataBase, modBusMaster));
        vertx.deployVerticle(new OEE(dataBase, modBusMaster));
        vertx.deployVerticle(new ScannerVerticle(alAddress, controller, dataBase));

//        Test test = new Test();
//        test.testing();

        }
}
