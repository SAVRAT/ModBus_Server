import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.util.ReferenceCountUtil;
import io.vertx.ext.sql.SQLConnection;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("Duplicates")
class Test {
    public static void main(String[] args) {
        DataBaseConnect dataBase = new DataBaseConnect("localhost", "root", "z1x2c3v4",
                "wert");

        dataBase.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                try (SQLConnection connection = con.result()) {
                    for (int i = 0; i < 20; i++) {
                        System.out.println(i);
                        connection.query("insert into s2 values (7)", result -> {
                            System.out.println(result.result().getRows());
                        });
                    }
                }
            }
        });
    }
}