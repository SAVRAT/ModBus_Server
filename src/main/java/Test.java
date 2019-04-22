import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.WriteMultipleRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("Duplicates")
class Test {
//    public static void main(String[] args) {
//        Vertx vertx = Vertx.vertx();
//        Controller controller = new Controller();
//        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
//                "fanDOK");
//        String[] alAddress = {"192.168.49.239", "192.168.49.238"};
//
//        vertx.deployVerticle(new ScannerVerticle(alAddress, controller, dataBase));
//    }

    public static void main(String[] args) {
        DataBaseConnect dataBase = new DataBaseConnect("192.168.49.53", "java", "z1x2c3v4",
                "fanDOK");
        int address = 2, quantity = 2;
//        int[] word = {1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] byteArray= {36, 73, -110, 36};


        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5001)
                .build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new WriteMultipleRegistersRequest(address, quantity, byteArray), 2);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ReferenceCountUtil.release(response);
            } else {
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
            }
            master.disconnect();
        }, Modbus.sharedExecutor());
    }


}