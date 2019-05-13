import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;

class Vibration extends AbstractVerticle implements SystemLog{

    private DataBaseConnect dataBaseConnect;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();
    private ArrayList<ArrayList<String[]>> forWrite = new ArrayList<>();

    Vibration(DataBaseConnect dataBaseConnect, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.modBusMaster = modBusMaster;
    }

    public void start(){
        refreshData();
        vertx.setPeriodic(60000, event -> refreshData());
    }

    private long timerID;
    private void handle(ArrayList<String[]> data) throws IndexOutOfBoundsException{
        first = false;
        if (!third) {
            previous = data;
            third = true;
        }
        timerID = vertx.setPeriodic(5000, result -> {

//            System.out.println("Vibro write...");
            ArrayList<String> ipAddr = new ArrayList<>();
            for (String[] datum : data) {
                boolean check = false;
                for (String str : ipAddr)
                    if (str.equals(datum[0]))
                        check = true;
                if (!check)
                    ipAddr.add(datum[0]);
            }
            modBusMaster.buffer = new int[ipAddr.size()];
            modBusMaster.aiCount = new int[ipAddr.size()];
            forWrite.clear();
            for (String ip : ipAddr) {
                ArrayList<String[]> temp = new ArrayList<>();
                for (String[] item : data) if (ip.equals(item[0])) temp.add(item);
                forWrite.add(temp);
            }

//            for (ArrayList<String[]> val:forWrite) {
//                System.out.println("================");
//                for (String[] row1:val) System.out.println(Arrays.toString(row1));
//            }
            ThreadGroup ovenAI = new ThreadGroup("OVEN AI READ");
            for (int i=0; i<ipAddr.size(); i++){
                int lamI = i;
                new Thread(ovenAI, () -> {
                    ArrayList<String[]> tempForWrite = forWrite.get(lamI);
                    ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(ipAddr.get(lamI)).setPort(502).build();
                    ModbusTcpMaster master = new ModbusTcpMaster(config);
                    modBusMaster.buffer[lamI] = 0;
                    modBusMaster.aiCount[lamI] = tempForWrite.size();
                    int count = 0, num = 0;

                    while (count < tempForWrite.size()){
                        System.out.print("");
                        if (modBusMaster.buffer[lamI] < 1) {
                            count++;
                            modBusMaster.buffer[lamI]++;
                            modBusMaster.sendAndReceive_OBEH_AI(master, tempForWrite.get(num)[2],
                                    tempForWrite.get(num)[1], lamI);
                            num++;
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

    private void refreshData(){
        System.out.println("Refresh AI...");
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("SELECT ip, tablename, adress FROM point_control", res -> {
                    if (res.succeeded()) {
                        ResultSet result = res.result();
                        outData.clear();
                        outData.addAll(dataBaseConnect.parseData(result));
                        if (first)
                            handle(outData);
                        if (second)
                            check(outData);
                    } else
                        writeLogString("Query ERROR while refresh AI " + res.cause());
//                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            } else
                writeLogString("DataBase ERROR while refresh AI " + con.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }
}
