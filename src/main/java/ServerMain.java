import io.vertx.core.Vertx;

class ServerMain {
        private Controller controller;
        ServerMain(Controller controller) {
            this.controller = controller;
            String[] slaveAddress = {"192.168.0.10", "192.168.0.1"};
            String[] oven = {"192.168.0.100"};
            String[] al1302 = {"192.168.0.120", "192.168.0.130"};

            ModBus_Master m = new ModBus_Master(0, 10, 1);
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new SomeVerticle(al1302, 80, controller));
//            VertxWebClient vertxWebClient = new VertxWebClient(al1302, 80, controller);
//            vertxWebClient.start();
//        m.start_OBEH(oven, 51); //OBEH DI ModBus
//        m.start_OBEH_AI(oven, 4064, 4071);
//        m.start(slaveAddress); //Siemens S7-1200 ModBus
//        controller.doComp();
        }
}
