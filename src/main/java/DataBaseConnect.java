import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;

class DataBaseConnect implements SystemLog{
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
                        writeLogString("Write Query ERROR (" + query + ") " + res.cause());
//                        System.out.println("\u001B[33m" + "Write Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else {
                writeLogString("DataBase ERROR (" + query + ") " + res.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
            }
        });
    }

    void databaseUpdate(String query, JsonArray jsonArray){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.updateWithParams(query, jsonArray, out -> {
                    if (out.failed())
                        writeLogString("Update Query ERROR (" + query + ") " + res.cause());
//                        System.out.println("\u001B[33m" + "Update Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else {
                writeLogString("DataBase ERROR (" + query + ") " + res.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
            }
        });
    }
    // чтение текущего статуса оборудования из БД
    void databaseReadOEE(String[] device, int newState) {
        mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("SELECT status FROM " + device[4]
                        + " ORDER BY id DESC LIMIT 1;", out -> {
                    if (out.succeeded()) {
                        List<JsonObject> output = out.result().getRows();
                        // проверка статуса (юолы ли изменение)
                        statusCheck(output, newState, device);
                    } else
                        writeLogString("Query ERROR (OEE Read) " + out.cause());
//                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + out.cause());
                    connection.close();
                });
            } else
                writeLogString("DataBase ERROR (OEE Read) " + con.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }
    // чтение текущей смены из БД
    void databaseReadShift(String[] device, float newValue, int shift){
        mySQLClient.getConnection(con -> {
            if (con.succeeded()){
                SQLConnection connection = con.result();
                String tableName = device[4] + "_Data";
                connection.query("SELECT data FROM " + tableName
                        + " ORDER BY id DESC LIMIT 1;", out -> {
                    if (out.succeeded()){
                        JsonObject output = out.result().toJson();
                        // проверка изменения смены
                        shiftStatusCheck(output, newValue, tableName, shift);
//                        System.out.println("Old: " + output + " New: " + newValue);
                    } else
                        writeLogString("Query ERROR (READ shift) " + out.cause());
//                        System.out.println("\u001B[33m" + "Query ERROR" + "\u001B[0m" + " " + out.cause());
                    connection.close();
                });
            } else
                writeLogString("DataBase ERROR (READ shift) " + con.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + con.cause());
        });
    }
    // парсинг данных для ОВЕН AI
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
    // парсинг данных для ОЕЕ (ПЛК и ОВЕН DI)
    ArrayList<String[]> parseDataOee(List<JsonObject> resultSet){
        ArrayList<String[]> data = new ArrayList<>();
        for (JsonObject entries : resultSet) {
            String[] row = new String[7];
            if (entries.getString("ip") != null && entries.getString("ip").length() > 0) {
                row[0] = entries.getString("ip");
                row[1] = entries.getString("length");
                row[2] = entries.getString("type");
                row[3] = entries.getString("address");
                row[4] = entries.getString("tablename");
                row[5] = String.valueOf(entries.getInteger("id"));
                row[6] = String.valueOf(entries.getInteger("plain"));
                data.add(row);
            }
        }
        return data;
    }

    private void databaseUpdate(String query){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.update(query, out -> {
                    if (out.failed())
                        writeLogString("Update Query ERROR (" + query + ") " + out.cause());
//                        System.out.println("\u001B[33m" + "Update Query ERROR" + "\u001B[0m" + " " + res.cause());
                    connection.close();
                });
            }else
                writeLogString("DataBase ERROR (" + query + ") " + res.cause());
//                System.out.println("\u001B[33m" + "DataBase ERROR" + "\u001B[0m" + " " + res.cause());
        });
    }

    // метод проверки по изменению смены
    private void shiftStatusCheck(JsonObject data, float newValue, String tableName, int shift){
        float oldValue = Float.parseFloat(data.getJsonArray("rows").getJsonObject(0).getString("data"));
        String currentTime = String.valueOf((System.currentTimeMillis())/1000);
        // если новая смена == старой, то просто обновляем метку времени
        // если смена изменилась вставляем новую строку
        if (oldValue != newValue){
            JsonArray toWrite = new JsonArray().add(String.valueOf(newValue)).add(shift).add(currentTime);
            databaseWrite("INSERT INTO " + tableName + " (data, shift, timeStamp) VALUE (?, ?, ?)", toWrite);
        } else {
            databaseUpdate("UPDATE " + tableName +
                    " SET timeStamp = " + currentTime + " ORDER BY id DESC LIMIT 1;");
        }
    }

    // метод для обновления таблици ОЕЕ
    private void statusCheck(List<JsonObject> data, int currentState, String[] device){
        int oldState = data.get(0).getInteger("status");
        String currentTime = String.valueOf((System.currentTimeMillis())/1000);
        // в любом случае обновляем метку времени
        databaseUpdate("UPDATE " + device[4] +
                " SET endperiod = " + currentTime + " ORDER BY id DESC LIMIT 1;");
        // если новый статус отличается от старого, то вставляем новую строку
        if (oldState != currentState){
            JsonArray toWrite = new JsonArray().add(currentTime).add(currentTime).add(currentState).add(device[5]);
            databaseWrite("INSERT INTO " + device[4] +
                    " (startperiod, endperiod, status, parentid) VALUE (?, ?, ?, ?);", toWrite);
        }
        // если статус меняется с 3 на 1, то проверяем время со статусом 3, если меньше, чем разрешённое время простоя,
        // то меняем с 3 на 1
        if (oldState == 3 && currentState == 1){
           databaseUpdate("UPDATE " + device[4] + " SET status = 1 WHERE status = 3" +
                   " AND startperiod > " + (Integer.valueOf(currentTime) - Integer.valueOf(device[6]) * 60) + ";");
        }
    }
}
