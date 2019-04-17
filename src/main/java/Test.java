import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class Test {
    private static DataBaseConnect dataBaseConnect = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
            "fanDOK");

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        String[] str = {"lu_N_1", "lu_N_2", "lu_N_3", "shlifstanok", "saw_left", "saw_right", "obreznoy", "press1",
                "press2", "press3", "press4", "sushilka"};
//        for (String tableName:str)
        String tableName = "lu_N_1_Data";
            dataBaseConnect.mySQLClient.getConnection(con -> {
                if (con.succeeded()){
                    SQLConnection connection = con.result();
                    int period = 600;
                    String query = "SELECT data, timeStamp FROM lu_N_1_Data WHERE timeStamp > 1555443950;";
                    List<String> batch = new ArrayList<>();
                    batch.add("SELECT data, timeStamp FROM lu_N_1_Data WHERE timeStamp > 1555443950;");
                    batch.add("SELECT startperiod, status FROM lu_N_1 WHERE endperiod > 1555443950;");
                    connection.batch(batch, res -> {
                        if (res.succeeded()){
                            System.out.println(res.result());
                        } else
                            System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + res.cause());
                        connection.close();
                    });
                } else
                    System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
            });
    }
}