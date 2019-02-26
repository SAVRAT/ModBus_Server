import io.vertx.core.Vertx;

import java.util.ArrayList;

public class test {
    private DataBaseConnect dataBase;
    private Vertx vertx;
    private ArrayList<String[]> oven_AI;

    test(DataBaseConnect dataBase, Vertx vertx){
        this.dataBase = dataBase;
        this.vertx = vertx;
        oven_AI =  dataBase.getOven_AI();
    }
    ModBus_Master modBus_master = new ModBus_Master(0, 10, 1);
    
    public static void main(String[] args) {
        doQuery("table");
    }

    void start() {
        vertx.setPeriodic(3000, res -> {
            dataBase.getOven_AI();
            modBus_master.oven_AI_IP = new String[dataBase.oven_AI.size()];
            modBus_master.oven_AI_Tabl = new String[dataBase.oven_AI.size()];
            modBus_master.oven_AI_ID = new String[dataBase.oven_AI.size()];
            for (int i = 0; i < dataBase.oven_AI.size(); i++) {
                modBus_master.oven_AI_IP[i] = dataBase.oven_AI.get(i)[0];
                modBus_master.oven_AI_Tabl[i] = dataBase.oven_AI.get(i)[1];
                modBus_master.oven_AI_ID[i] = dataBase.oven_AI.get(i)[2];
            }
        });
        modBus_master.start_OBEH_AI();
    }

    static void doQuery(String tableName){
        double time = ((double) System.currentTimeMillis())/1000;
        System.out.println((int) time);
        String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
    }
}
