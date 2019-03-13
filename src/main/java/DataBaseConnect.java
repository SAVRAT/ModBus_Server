import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;

class DataBaseConnect {
    private Vertx vertx = Vertx.vertx();
    SQLClient mySQLClient;
    ArrayList<String[]> oven_AI = new ArrayList<>();

    DataBaseConnect(String host, String username, String password, String dataBase){
        JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("username", username)
                .put("password", password).put("database", dataBase);
        mySQLClient = MySQLClient.createNonShared(vertx, mySQLClientConfig);
    }
    void databaseWrite(String query, JsonArray jsonArray){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.queryWithParams(query, jsonArray, out -> {
                    if (out.succeeded()){
                        System.out.println("Done.");
                    }else {
                        System.out.println("Error... " + res.cause());
                    }
                    connection.close();
                });
            }else {
                System.out.println("Fault to connect!   " + res.cause());
            }
        });
    }

    void dataBaseRead(String query){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()){
                SQLConnection connection = res.result();
                connection.query(query, out -> {
                    if (out.succeeded()){
                        JsonObject output = out.result().toJson();
                    }else {
                        System.out.println("Error... " + res.cause());
                    }
                });
            }else {
                System.out.println("Fault to connect!   " + res.cause());
            }
        });
    }

    ArrayList<String[]> parseData(ResultSet resultSet){
        ArrayList<String[]> data = new ArrayList<>();
        int size = resultSet.getRows().size();
        for (int i=0; i<size; i++){
            String[] row = new String[3];
            if (resultSet.getRows().get(i).getString("ip")!=null){
                row[0] = resultSet.getRows().get(i).getString("ip");
                row[1] = resultSet.getRows().get(i).getString("tablename");
                row[2] = resultSet.getRows().get(i).getString("adress");
                data.add(row);
            }
        }
        return data;
    }
}
