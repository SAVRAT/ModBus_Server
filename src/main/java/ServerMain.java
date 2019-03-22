import io.vertx.core.Vertx;

class ServerMain {
    ServerMain(Controller controller) {
            DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                    "fanDOK");
//        DataBaseConnect dataBase = new DataBaseConnect("localhost", "server", "z1x2c3v4",
//                "wert");
        Vertx vertx = Vertx.vertx();
        ModBus_Master modBusMaster = new ModBus_Master(dataBase, vertx);
        OvenAI ovenAI = new OvenAI(dataBase, vertx, modBusMaster);
        PLC plc = new PLC(dataBase, vertx, modBusMaster);

        String[] al1302 = {"192.168.0.120", "192.168.0.130"};
//        Test test = new Test();
//        test.testing();
//        ovenAI.start();
        plc.start();

//            vertx.deployVerticle(new SomeVerticle(al1302, 80, controller, dataBase));
        }
}
