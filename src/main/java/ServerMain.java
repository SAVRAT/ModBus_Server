import io.vertx.core.Vertx;

class ServerMain {
    public static void main(String[] args) {
        Controller controller = new Controller();
        Vertx vertx = Vertx.vertx();
        String[] alAddress = {"192.168.49.239", "192.168.49.238"};
        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                    "fanDOK");
        ModBus_Master modBusMaster = new ModBus_Master(dataBase);
        // запускаем вибрацию (автоматически запускает мето start())
        vertx.deployVerticle(new Vibration(dataBase, modBusMaster));
        // запускаем ОЕЕ (автоматически запускает мето start())
        vertx.deployVerticle(new OEE(dataBase, modBusMaster));
        // запускаем сканер (автоматически запускает мето start())
        vertx.deployVerticle(new ScannerVerticle(alAddress, controller, dataBase));

        }
}
