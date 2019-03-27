import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class PLC {

    private DataBaseConnect dataBaseConnect;
    private Vertx vertx;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();

    PLC(DataBaseConnect dataBaseConnect, Vertx vertx, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.vertx = vertx;
        this.modBusMaster = modBusMaster;
    }

    @SuppressWarnings("Duplicates")
    void start(){
        refreshData();
        vertx.setPeriodic(60000, event -> refreshData());
    }

    private long timerID;
    @SuppressWarnings("Duplicates")
    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
            third = true;
        }
        timerID = vertx.setPeriodic(2000, result -> {
            System.out.println("OEE write...");
            ArrayList<String[]> modBusDevice = new ArrayList<>();
            for (String[] datum : data) {
                boolean check = false;
                for (String[] str : modBusDevice)
                    if (str[0].equals(datum[0])) {
                        check = true;
                    }
                if (!check) {
                    String[] arr = {datum[0], datum[1], datum[2]};
                    modBusDevice.add(arr);
                }
            }

//            for (String[] row:data) System.out.println("data: " + Arrays.toString(row));
//            for (String[] row:modBusDevice)
//                System.out.println("Row: " + Arrays.toString(row));


            ThreadGroup OEE = new ThreadGroup("OEE READ");
            for (String[] device:modBusDevice){
                new Thread(OEE, () -> {

//                    System.out.println("OEE Thread started. IP: " + device[0] +
//                            "  id: " + Thread.currentThread().getId());
                    switch (device[2]) {
                        case "plc": {
                            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(device[0]).setPort(502)
                                    .build();
                            ModbusTcpMaster master = new ModbusTcpMaster(config);
                            modBusMaster.sendAndReceive_PLC(master, Integer.valueOf(device[1]), data);
                            break;
                        }
                        case "owen": {
                            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(device[0]).setPort(502)
                                    .build();
                            ModbusTcpMaster master = new ModbusTcpMaster(config);
                            modBusMaster.sendAndReceive_OBEH_DI(master, Integer.valueOf(device[1]), data);
                            break;
                        }
                        case "plc_5000": {
                            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(device[0]).setPort(5000)
                                    .build();
                            ModbusTcpMaster master = new ModbusTcpMaster(config);
                            modBusMaster.sendAndReceive_PLC(master, Integer.valueOf(device[1]), data);
                            break;
                        }
                    }

                }).start();
            }
            second = true;
        });
    }

    @SuppressWarnings("Duplicates")
    private void check(ArrayList<String[]> data){
        boolean qwerty = false;
        if (data.size() == previous.size())
            for (int i=0; i<data.size(); i++){
                for (int n=0; n<6; n++){
                    if (!data.get(i)[n].equals(previous.get(i)[n]))
                        qwerty = true;
                }
            }
        else
            qwerty = true;
        if (qwerty){
            vertx.cancelTimer(timerID);
            previous=data;
            handle(data);
            System.out.println(previous);
        }
    }

    @SuppressWarnings("Duplicates")
    private void refreshData(){
        System.out.println("Refresh OEE...");
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("SELECT tablename, address, ip, length, type, id FROM oborudovanie;", res -> {
                    if (res.succeeded()) {
                        List<JsonObject> result = res.result().getRows();
                        outData.clear();
                        outData.addAll(dataBaseConnect.parseDataOee(result));
                        if (first)
                            handle(outData);
                        if (second)
                            check(outData);
                    } else System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            } else System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }
}
