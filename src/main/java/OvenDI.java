import io.vertx.core.Vertx;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;

class OvenDI {
    private Vertx vertx;
    private DataBaseConnect dataBaseConnect;
    private ModBus_Master modBusMaster;
    private boolean firstTime = true;
    private boolean secondTime = false;
    private boolean third = false;
    private ArrayList<String[]> oldData = new ArrayList<>();
    private ArrayList<String[]> currentData = new ArrayList<>();

    OvenDI(DataBaseConnect dataBaseConnect, Vertx vertx, ModBus_Master modBusMaster){
        this.dataBaseConnect = dataBaseConnect;
        this.vertx = vertx;
        this.modBusMaster = modBusMaster;
    }

    @SuppressWarnings("Duplicates")
    void start(){
        vertx.setPeriodic(5000, event -> {
            System.out.println("Refresh DI...");
            dataBaseConnect.mySQLClient.getConnection(con -> {
                if (con.succeeded()){
                    SQLConnection connection = con.result();
                    connection.query("SELECT (ip, tablename, adress) FROM someTable", res -> {
                        if (res.succeeded()){
                            ResultSet result = res.result();
                            currentData.clear();
                            currentData.addAll(dataBaseConnect.parseData(result));
                            if (firstTime)
                                handle(currentData);
                            if (secondTime)
                                check(currentData);
                        }else System.out.println("ERROR...  " + res.cause());
                        connection.close();
                    });
                }else System.out.println("Connection error: " + con.cause());
            });
        });
    }

    private long timerID;
    @SuppressWarnings("Duplicates")
    private void handle(ArrayList<String[]> data){
        firstTime = false;
        if (!third) {
            oldData = data;
            third = true;
        }
        timerID = vertx.setPeriodic(1000, result -> {
            for (String[] val:oldData){
                System.out.println("        Array: " + Arrays.toString(val));
            }
            secondTime = true;
        });
    }

    @SuppressWarnings("Duplicates")
    private void check(ArrayList<String[]> data){
        boolean qwerty = false;
        if (data.size() == oldData.size())
            for (int i=0; i<data.size(); i++){
                for (int n=0; n<3; n++){
                    if (!data.get(i)[n].equals(oldData.get(i)[n]))
                        qwerty = true;
                }
            }
        else
            qwerty = true;
        if (qwerty){
            vertx.cancelTimer(timerID);
            oldData=data;
            handle(data);
            System.out.println(oldData);
        }
    }
}
