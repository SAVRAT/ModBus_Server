import io.vertx.core.Vertx;

class ServerMain {
    ServerMain(Controller controller) {
//            DataBaseConnect dataBase = new DataBaseConnect("192.168.6.21", "java", "z1x2c3v4",
//                    "fanDOK");
        DataBaseConnect dataBase = new DataBaseConnect("localhost", "server", "z1x2c3v4",
                "wert");
        Vertx vertx = Vertx.vertx();
        ModBus_Master modBusMaster = new ModBus_Master(dataBase, vertx);
        OvenAI ovenAI = new OvenAI(dataBase, vertx, modBusMaster);
        OvenDI ovenDI = new OvenDI(dataBase, vertx, modBusMaster);

        String[] slaveAddress = {"192.168.0.10", "192.168.0.1"};
        String[] al1302 = {"192.168.0.120", "192.168.0.130"};

        ovenAI.start();
//        ovenDI.start();

//            vertx.deployVerticle(new SomeVerticle(al1302, 80, controller, dataBase));
//            modBusMaster.start_OBEH(oven_AI, 51); //OBEH DI ModBus
//            modBusMaster.start(slaveAddress); //Siemens S7-1200 ModBus
        }
}
