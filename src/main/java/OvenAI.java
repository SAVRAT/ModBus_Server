import io.vertx.core.Vertx;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;

class OvenAI {

    private DataBaseConnect dataBaseConnect;
    private Vertx vertx;
    private ModBus_Master modBusMaster;
    private boolean first = true;
    private boolean second = false;
    private boolean third = false;
    private ArrayList<String[]> previous = new ArrayList<>();
    private ArrayList<String[]> outData = new ArrayList<>();

    OvenAI(DataBaseConnect dataBaseConnect, Vertx vertx, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.vertx = vertx;
        this.modBusMaster = modBusMaster;
    }

    void start(){
        vertx.setPeriodic(5000, qqq -> {
            System.out.println("Refresh...");
            dataBaseConnect.mySQLClient.getConnection(res -> {
                if (res.succeeded()) {
                    SQLConnection connection = res.result();
                    connection.query("SELECT * FROM point_control", con -> {
                        if (con.succeeded()) {
                            ResultSet result = con.result();
                            int size = result.getRows().size();
                            outData.clear();
                            for (int i = 0; i < size; i++) {
                                String[] module = new String[3];
                                if (result.getRows().get(i).getString("ip") != null) {
                                    module[0] = result.getRows().get(i).getString("ip");
                                    module[1] = result.getRows().get(i).getString("tablename");
                                    module[2] = result.getRows().get(i).getString("adress");
                                    outData.add(module);
                                }
                            }
                            if (first)
                                handle(outData);
                            if (second)
                                check(outData);
                        } else {
                            System.out.println("ERROR...  " + con.cause());
                        }
                        connection.close();
                    });
                } else {
                    System.out.println("Fault to connect!   " + res.cause());
                }
            });
        });
    }
    private int count = 0;
    private long timerID;

    private void handle(ArrayList<String[]> data){
        first = false;
        if (!third) {
            previous = data;
            third = true;
        }
        timerID = vertx.setPeriodic(1000, result -> {
            count++;
            System.out.println("        Count: " + count);
            for (String[] val:previous){
                System.out.println("        Array: " + Arrays.toString(val));
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
}
