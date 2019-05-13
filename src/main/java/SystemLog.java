import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;
// интерфейс с дефолтной реализацией метода для ведения лога
public interface SystemLog {
    DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
            "fanDOK");

    default void writeLogString (String message){
        System.out.println("\u001B[33m Write SystemLog \u001B[0m");
        dataBase.mySQLClient.getConnection(con -> {
            if (con.succeeded()){
                SQLConnection connection = con.result();
                connection.queryWithParams("INSERT INTO SystemLog (logMessage, timeStamp) VALUES (?, UNIX_TIMESTAMP());",
                        new JsonArray().add(message), res -> {});
            }
        });
    }
}
