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

    @SuppressWarnings("Duplicates")
    void start(){
        vertx.setPeriodic(5000, event -> {
            System.out.println("Refresh AI...");
            dataBaseConnect.mySQLClient.getConnection(con -> {
                if (con.succeeded()) {
                    SQLConnection connection = con.result();
                    connection.query("SELECT (ip, tablename, adress) FROM point_control", res -> {
                        if (res.succeeded()) {
                            ResultSet result = res.result();
                            outData.clear();
                            outData.addAll(dataBaseConnect.parseData(result));
                            if (first)
                                handle(outData);
                            if (second)
                                check(outData);
                        } else System.out.println("ERROR...  " + res.cause());
                        connection.close();
                    });
                } else System.out.println("Connection error: " + con.cause());
            });
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
        timerID = vertx.setPeriodic(1000, result -> {
            for (String[] val:previous){
                System.out.println("        Array: " + Arrays.toString(val));
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
}
