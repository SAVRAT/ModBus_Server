import io.vertx.core.Vertx;

import java.util.Arrays;

class ServerMain {
        private Controller controller;
        ServerMain(Controller controller) {
            this.controller = controller;
            DataBaseConnect dataBase = new DataBaseConnect("192.168.6.21", "java", "z1x2c3v4",
                    "fanDOK");
            String[] slaveAddress = {"192.168.0.10", "192.168.0.1"};
            String[] al1302 = {"192.168.0.120", "192.168.0.130"};
            Vertx vertx = Vertx.vertx();
            System.out.println(dataBase.getOven_AI());
            String[] oven_AI_IP = new String[dataBase.oven_AI.size()];
            String[] oven_AI_Tabl = new String[dataBase.oven_AI.size()];
            String[] oven_AI_ID = new String[dataBase.oven_AI.size()];
            for (int i=0; i<dataBase.oven_AI.size(); i++){
                oven_AI_IP[i] = dataBase.oven_AI.get(i)[0];
                oven_AI_Tabl[i] = dataBase.oven_AI.get(i)[1];
                oven_AI_ID[i] = dataBase.oven_AI.get(i)[2];
            }

            ModBus_Master m = new ModBus_Master(0, 10, 1);
//            vertx.deployVerticle(new SomeVerticle(al1302, 80, controller, dataBase));
//            m.start_OBEH(oven_AI, 51); //OBEH DI ModBus
            System.out.println(Arrays.toString(oven_AI_IP));
            m.start_OBEH_AI(oven_AI_IP, 4064, 4071);
//            m.start(slaveAddress); //Siemens S7-1200 ModBus
//            controller.doComp();
        }
}
