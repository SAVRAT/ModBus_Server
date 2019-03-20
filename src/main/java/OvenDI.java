import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;

class OvenDI {

    private DataBaseConnect dataBaseConnect;
    private Vertx vertx;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();
    private ArrayList<ArrayList<String[]>> forWrite = new ArrayList<>();

    OvenDI(DataBaseConnect dataBaseConnect, Vertx vertx, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.vertx = vertx;
        this.modBusMaster = modBusMaster;
    }

    @SuppressWarnings("Duplicates")
    void start(){
        refreshData();
        vertx.setPeriodic(60000, event -> {
            refreshData();
        });
    }

    private long timerID;
    @SuppressWarnings("Duplicates")
    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
            third = true;
        }
        timerID = vertx.setPeriodic(8000, result -> {

            ArrayList<String> ipAddr = new ArrayList<>();
            for (String[] datum : data) {
                boolean check = false;
                for (String str : ipAddr)
                    if (str.equals(datum[0]))
                        check = true;
                if (!check)
                    ipAddr.add(datum[0]);
            }
//            modBusMaster.buffer = new int[ipAddr.size()];
//            modBusMaster.aiCount = new int[ipAddr.size()];
//            forWrite.clear();
//            for (String ip : ipAddr) {
//                ArrayList<String[]> temp = new ArrayList<>();
//                for (String[] item : data) if (ip.equals(item[0])) temp.add(item);
//                forWrite.add(temp);
//            }

//            for (ArrayList<String[]> val:forWrite) {
//                System.out.println("================");
//                for (String[] row1:val) System.out.println(Arrays.toString(row1));
//            }

            ThreadGroup OEE = new ThreadGroup("OEE READ");
            for (int i=0; i<ipAddr.size(); i++){
                int lamI = i;
                System.out.println(ipAddr.get(i));
//                new Thread(OEE, () -> {
//                    ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(ipAddr.get(lamI)).setPort(502).build();
//                    ModbusTcpMaster master = new ModbusTcpMaster(config);
//                    System.out.println("Thread " + (lamI+1) + " started. IP: " + master.getConfig().getAddress() +
//                            "  id: " + Thread.currentThread().getId());
//                    modBusMaster.buffer[lamI] = 0;
//                    modBusMaster.aiCount[lamI] = forWrite.get(lamI).size();
//                    int count = 0, num = 0;

//                    while (count < forWrite.get(lamI).size()){
//                        System.out.print("");
//                        if (modBusMaster.buffer[lamI] < 1) {
//                            count++;
//                            modBusMaster.buffer[lamI]++;
//                            modBusMaster.sendAndReceive_OBEH_AI(master, forWrite.get(lamI).get(num)[2],
//                                    forWrite.get(lamI).get(num)[1], lamI);
//                            num++;
//                        }
//                    }
//                    System.out.println("Thread " + Thread.currentThread().getId() + " done.");
//                }).start();
            }
            second = true;
        });
    }

    @SuppressWarnings("Duplicates")
    private void check(ArrayList<String[]> data){
        boolean qwerty = false;
        if (data.size() == previous.size())
            for (int i=0; i<data.size(); i++){
                for (int n=0; n<3; n++){
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
                connection.query("SELECT ip, tablename, address, type FROM oborudovanie", res -> {
                    if (res.succeeded()) {
                        ResultSet result = res.result();
                        outData.clear();
                        outData.addAll(dataBaseConnect.parseDataOee(result));
                        if (first)
                            handle(outData);
                        if (second)
                            check(outData);
                    } else System.out.println("error: database read query  " + res.cause());
                    connection.close();
                });
            } else System.out.println("Connection error: " + con.cause());
        });
    }
}
