import io.vertx.core.Vertx;

class ServerMain {
    ServerMain(Controller controller) {
        String[] alAddress = {"192.168.49.239", "192.168.49.238"};
        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                    "fanDOK");
//        DataBaseConnect dataBase = new DataBaseConnect("localhost", "server", "z1x2c3v4",
//                "wert");
        Vertx vertx = Vertx.vertx();
        ModBus_Master modBusMaster = new ModBus_Master(dataBase);
        Vibration vibration = new Vibration(dataBase, vertx, modBusMaster);
        OEE OEE = new OEE(dataBase, vertx, modBusMaster);

//        Test test = new Test();
//        test.testing();

//        vibration.start();
//        OEE.start();
        vertx.deployVerticle(new ScannerVerticle(alAddress, controller, dataBase));
        }
}
