import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class OEE extends AbstractVerticle {

    private DataBaseConnect dataBaseConnect;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();
//    private CopyOnWriteArrayList<String> oeeTables = new CopyOnWriteArrayList<>();

    OEE(DataBaseConnect dataBaseConnect, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.modBusMaster = modBusMaster;
    }

    public void start(){
        refreshData();
        vertx.setPeriodic(60000, event -> refreshData());
//        vertx.setPeriodic(60000, event -> oeeCompute());
    }

    private long timerID;

    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
//            previous.forEach(val -> oeeTables.add(val[4]));
            third = true;
        }
        timerID = vertx.setPeriodic(2000, result -> {
//            System.out.println("OEE write...");
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
//            oeeTables.clear();
//            previous.forEach(val -> oeeTables.add(val[4]));
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

//    private void oeeCompute(){
////        System.out.println(oeeTables);
//        for (String tableName:oeeTables)
//            dataBaseConnect.mySQLClient.getConnection(con -> {
//                if (con.succeeded()){
//                    SQLConnection connection = con.result();
//                    int period = 600;
//                    String query = "SELECT  UNIX_TIMESTAMP()-" + period + ", UNIX_TIMESTAMP()," +
//                            " CAST((SUM(endperiod)-SUM(startperiod))/" + period + " AS DEC(3)) " +
//                            "FROM " + tableName + " WHERE status = 1 AND endperiod > UNIX_TIMESTAMP()-" +
//                            period + ";";
//                    connection.query(query, res -> {
//                            if (res.succeeded()) {
//                                JsonArray oeeRow = res.result().getResults().get(0);
//                                if (oeeRow.getValue(2) == null) {
//                                    oeeRow.remove(2);
//                                    oeeRow.add(0);
//                                }
//                                if (Integer.valueOf(oeeRow.getString(2)) > 100) {
//                                    oeeRow.remove(2);
//                                    oeeRow.add(100);
//                                } else {
//                                    int value = Integer.valueOf(oeeRow.getString(2));
//                                    oeeRow.remove(2);
//                                    oeeRow.add(value);
//                                }
////                                System.out.println("tableName: " + oeeRow);
//                            } else
//                                System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
//                            connection.close();
//                        });
//                        if (tableName.contains("lu_N")){
////                            System.out.println("Table: " + tableName);
//                            dataBaseConnect.mySQLClient.getConnection(con_1 -> {
//                                if (con_1.succeeded()) {
//                                    SQLConnection connection1 = con_1.result();
//                                    connection1.query("SELECT data, timeStamp" +
//                                            " FROM " + tableName + "_Data WHERE timeStamp > UNIX_TIMESTAMP()-" +
//                                            period + ";", res -> {
//                                        if (res.succeeded()){
//                                            if (res.result().getResults().size() > 1) {
//                                                int P = oee_P(res.result().getResults(), 0.002041f);
//                                                System.out.println(tableName + " OOE P: " + P);
//                                            } else {
//                                                int P = 0;
//                                                System.out.println(tableName + " OEE P: " + P);
//                                            }
//                                        } else
//                                            System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
//                                        connection1.close();
//                                    });
//                                }
//
//                            });
//                        } else {
//                            int P = 70;
////                                        System.out.println(tableName + ": " + oeeRow.getValue(2) + " :: " + P);
//                        }
////                            dataBaseConnect.databaseWrite("INSERT INTO " + tableName +
////                                    "_oee (startperiod, endperiod, A) VALUES (?, ?, ?);", oeeRow);
//                } else
//                    System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
//            });
//    }
//
//    private int oee_P(List<JsonArray> dataBaseOut, float prodRate){
//        ArrayList<Float> data = new ArrayList<>();
//        ArrayList<Integer> timeStamp = new ArrayList<>();
//        float dataSum;
//        float timeSum;
//        boolean isZero = false;
//        int zeroPos = 0;
//        for (JsonArray val:dataBaseOut) {
//            data.add(Float.valueOf(val.getString(0)));
//            timeStamp.add(Integer.valueOf(val.getString(1)));
////            System.out.println(val);
//        }
//        for (int i = 0; i < data.size(); i++){
//            if (data.get(i) == 0) {
//                isZero = true;
//                zeroPos = i;
//            }
//        }
//        if (isZero){
//            if (zeroPos == 0){
//                dataSum = data.get(data.size()-1) - data.get(1);
//                timeSum = timeStamp.get(data.size()-1) - timeStamp.get(1);
//            } else if (zeroPos == 1){
//                dataSum = data.get(data.size()-1) - data.get(2) + data.get(0);
//                timeSum = timeStamp.get(data.size()-2) - timeStamp.get(1);
//            } else if (zeroPos == (data.size()-1)){
//                dataSum = data.get(data.size()-2) - data.get(0);
//                timeSum = timeStamp.get(data.size()-2) - timeStamp.get(0);
//            } else if (zeroPos == (data.size()-2)){
//                dataSum = data.get(data.size()-3) - data.get(0) + data.get(data.size()-1);
//                timeSum = timeStamp.get(data.size()-2) - timeStamp.get(0);
//            } else {
//                dataSum = data.get(zeroPos - 1) - data.get(0) +
//                        data.get(data.size()-1) - data.get(zeroPos+1);
//                timeSum = timeStamp.get(zeroPos - 1) - timeStamp.get(0) +
//                        timeStamp.get(timeStamp.size()-1) - timeStamp.get(zeroPos + 1);
//            }
//        }else {
//            dataSum = data.get(data.size() - 1) - data.get(0);
//            timeSum = timeStamp.get(timeStamp.size() - 1) - timeStamp.get(0);
//        }
////        System.out.println("ALL Data: " + dataSum);
////        System.out.println("ALL Time: " + timeSum);
////        System.out.println("Result: " + (dataSum/timeSum)/prodRate);
//
//        return Math.round((dataSum/timeSum)/prodRate*100);
//    }
}
