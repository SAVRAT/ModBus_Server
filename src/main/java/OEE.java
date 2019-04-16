import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class OEE extends AbstractVerticle {

    private DataBaseConnect dataBaseConnect;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();
    private CopyOnWriteArrayList<String> oeeTables = new CopyOnWriteArrayList<>();

    OEE(DataBaseConnect dataBaseConnect, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.modBusMaster = modBusMaster;
    }

    public void start(){
        refreshData();
        vertx.setPeriodic(60000, event -> refreshData());
        vertx.setPeriodic(10000, event -> {
            oeeCompute();
        });
    }

    private long timerID;

    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
            previous.forEach(val -> oeeTables.add(val[4]));
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
            previous = data;
            oeeTables.clear();
            previous.forEach(val -> oeeTables.add(val[4]));
            handle(data);
            System.out.println(previous);
        }
    }

    private void refreshData(){
        System.out.println("Refresh OEE...");
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("SELECT tablename, address, ip, length, type, id, plain FROM oborudovanie;", res -> {
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
            } else
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }

    private void oeeCompute(){
        for (String tableName:oeeTables)
            dataBaseConnect.mySQLClient.getConnection(con -> {
                if (con.succeeded()){
                    SQLConnection connection = con.result();
                    int period = 600;
                    String query = "SELECT  UNIX_TIMESTAMP()-" + period + " AS startPeriod, UNIX_TIMESTAMP() AS endPeriod," +
                            " (SUM(endperiod)-SUM(startperiod))/" + period + "*100 AS workTime " +
                            "FROM " + tableName + " WHERE status = 1 AND endperiod > UNIX_TIMESTAMP()-" + period + ";";
                    connection.query(query, res -> {
                        if (res.succeeded()){
                            JsonArray oeeRow = res.result().getResults().get(0);
                            if (oeeRow.getString(2) == null) {
                                oeeRow.remove(2);
                                oeeRow.add("0.00");
                            }
                            if (Float.valueOf(oeeRow.getString(2)) > 100){
                                oeeRow.remove(2);
                                oeeRow.add("100.00");
                            }
                            System.out.println(tableName + ": " + oeeRow);
                            dataBaseConnect.databaseWrite("INSERT INTO " + tableName +
                                    "_oee (startperiod, endperiod, A) VALUES (?, ?, ?);", oeeRow);
                        } else
                            System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                        connection.close();
                    });
                } else
                    System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
            });
    }
}
