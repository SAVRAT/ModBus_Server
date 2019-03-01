import io.vertx.core.Vertx;

class ServerMain {
        private Controller controller;
        ServerMain(Controller controller) {
            this.controller = controller;
//            DataBaseConnect dataBase = new DataBaseConnect("192.168.6.21", "java", "z1x2c3v4",
//                    "fanDOK");
            DataBaseConnect dataBase = new DataBaseConnect("localhost", "server", "z1x2c3v4",
                    "wert");
            Vertx vertx = Vertx.vertx();
            OvenAI OvenAI = new OvenAI(dataBase, vertx);
            ModBus_Master modBusMaster = new ModBus_Master(0, 10, 1, dataBase, vertx);

            String[] slaveAddress = {"192.168.0.10", "192.168.0.1"};
            String[] al1302 = {"192.168.0.120", "192.168.0.130"};

            OvenAI.start();

//            vertx.deployVerticle(new SomeVerticle(al1302, 80, controller, dataBase));
//            m.start_OBEH(oven_AI, 51); //OBEH DI ModBus
//            m.start_OBEH_AI(oven_AI_IP, 4064, 4071);
//            m.start(slaveAddress); //Siemens S7-1200 ModBus
        }
}
