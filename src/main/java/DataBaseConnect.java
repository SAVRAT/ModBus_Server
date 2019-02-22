import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

class DataBaseConnect extends AbstractVerticle {
    private SQLClient mySQLClient;

    DataBaseConnect(String host, String username, String password,
                    String dataBase){
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
                        System.out.println(out.result());
                    }else {
                        System.out.println("Error in query!");
                    }
                });
            }else {

                System.out.println("Fault to connect!   " + res.cause());
            }
        });
    }

}
