import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class DataBaseConnect {
    SQLClient mySQLClient;

    DataBaseConnect(String host, String username, String password, String dataBase){
        JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("username", username)
                .put("password", password).put("database", dataBase);
        Vertx vertx = Vertx.vertx();
        mySQLClient = MySQLClient.createNonShared(vertx, mySQLClientConfig);
    }
    void databaseWrite(String query, JsonArray jsonArray){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.queryWithParams(query, jsonArray, out -> {
                    if (out.failed())
                        System.out.println("\u001B[33m" + "Write Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else {
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
            }
        });
    }
    void databaseUpdate(String query, JsonArray jsonArray){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.updateWithParams(query, jsonArray, out -> {
                    if (out.failed())
                        System.out.println("\u001B[33m" + "Update Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else {
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
            }
        });
    }

    private void databaseUpdate(String query){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.update(query, out -> {
                    if (out.failed()){
                        System.out.println("\u001B[33m" + "Update Query ERROR" + "\u001B[0m" + " " + res.cause());
                    }
                    connection.close();
                });
            }else {
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
            }
        });
    }

    void databaseReadOEE(String[] device, int newState) {
        mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("SELECT status FROM " + device[4]
                        + " ORDER BY id DESC LIMIT 1;", out -> {
                    if (out.succeeded()) {
                        List<JsonObject> output = out.result().getRows();
                        statusCheck(output, newState, device[4], device[5]);
                    } else {
                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + out.cause());
                    }
                    connection.close();
                });
            } else {
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
            }
        });
    }

    void databaseReadShift(String[] device, float newValue){
        mySQLClient.getConnection(con -> {
            if (con.succeeded()){
                SQLConnection connection = con.result();
                connection.query("SELECT data FROM " + device[4] + "_Data " +
                        "ORDER BY id DESK LIMIT 1;", out -> {
                    if (out.succeeded()){
                        JsonObject output = out.result().toJson();
                        System.out.println("Old: " + output + " New: " + newValue);
                    } else System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + out.cause());
                });
            } else System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }

    void databaseQuery(String query){
        mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query(query, out -> {
                    if (out.failed()) {
                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + out.cause());
                    }
                    connection.close();
                });
            } else {
                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
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

    ArrayList<String[]> parseDataOee(List<JsonObject> resultSet){
        ArrayList<String[]> data = new ArrayList<>();
        for (JsonObject entries : resultSet) {
            String[] row = new String[6];
            if (entries.getString("ip") != null && entries.getString("ip").length() > 0) {
                row[0] = entries.getString("ip");
                row[1] = entries.getString("length");
                row[2] = entries.getString("type");
                row[3] = entries.getString("address");
                row[4] = entries.getString("tablename");
                row[5] = String.valueOf(entries.getInteger("id"));
                data.add(row);
            }
        }
        return data;
    }

    private void statusCheck(List<JsonObject> data, int currentState, String tableName, String parentId){
        int oldStatus = data.get(0).getInteger("status");
        String currentTime = String.valueOf((System.currentTimeMillis())/1000);
        databaseUpdate("UPDATE " + tableName +
                " SET endperiod = " + currentTime + " ORDER BY id DESC LIMIT 1;");
        if (oldStatus != currentState){
            JsonArray toWrite = new JsonArray().add(currentTime).add(currentTime).add(currentState).add(parentId);
            databaseWrite("INSERT INTO " + tableName +
                    " (startperiod, endperiod, status, parentid) VALUE (?, ?, ?, ?);", toWrite);
        }
    }
}
