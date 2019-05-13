import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class OEE extends AbstractVerticle implements SystemLog{

    private DataBaseConnect dataBaseConnect;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();

    OEE(DataBaseConnect dataBaseConnect, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.modBusMaster = modBusMaster;
    }

    public void start(){
        refreshData();
        // раз в минуту обновляем данные из таблицы "оборудование"
        vertx.setPeriodic(60000, event -> refreshData());
    }

    private long timerID;
    // запись ОЕЕ в базу
    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
            third = true;
        }
        // запись ОЕЕ раз в 2 секунды
        timerID = vertx.setPeriodic(2000, result -> {
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
    // проверка отличия данных от тех, что в базе
    // если отличается, то останавливаем таймер, обновляем данные и запускаем handle()
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
            handle(data);
            System.out.println(previous);
        }
    }
    // обновление данных из базы
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
                            handle(outData);    // при первом вызове
                        if (second)
                            check(outData);     // при втором и последющих
                    } else
                        writeLogString("Query ERROR while refresh OEE" + res.cause());
//                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            } else
                writeLogString("DataBase ERROR while refresh OEE" + con.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }
}
