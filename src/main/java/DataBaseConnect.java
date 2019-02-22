import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class DataBaseConnect {
    private String host;
    private int port;
    private String username;
    private String password;
    private String dataBase;
    private Vertx vertx;
    DataBaseConnect(String host, int port, String username, String password,
                    String dataBase, Vertx vertx){
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dataBase = dataBase;
        this.vertx = vertx;
    }

    private JsonObject mySQLClientConfig = new JsonObject().put("host", host).put("port", port).put("username", username)
            .put("password", password).put("database", dataBase);
    private SQLClient mySQLClient = MySQLClient.createNonShared(vertx, mySQLClientConfig);

    void databaseWrite(String query){
        mySQLClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                connection.query(query, out -> {
                    if (out.succeeded()){
                        System.out.println(out.result());
                    }else {
                        System.out.println("Error in query!");
                    }
                });
            }else {
                System.out.println("Fault to connect!");
            }
        });
    }

}
