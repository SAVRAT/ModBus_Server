import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

class Test {

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        int startAddress = 0, quantity = 7;
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5000)
                .build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);
        vertx.setPeriodic(50, start -> {
            CompletableFuture<ReadHoldingRegistersResponse> future =
                    master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);

            future.whenCompleteAsync((response, ex) -> {
                if (response != null) {
                    int output = uByteToInt(new short[]{response.getRegisters().getUnsignedByte(8),
                            response.getRegisters().getUnsignedByte(9),
                            response.getRegisters().getUnsignedByte(10),
                            response.getRegisters().getUnsignedByte(11)});
                    System.out.println("Position: " + output);
                    ReferenceCountUtil.release(response);
                } else {
                    System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
                }
//            master.disconnect();
            }, Modbus.sharedExecutor());
        });
    }

    static int uByteToInt(short[] data){
        StringBuilder temp = new StringBuilder();
        for (short datum : data) {
            StringBuilder str = new StringBuilder(Integer.toBinaryString(0xFFFF & datum));
            int length = str.length();
            for (int i = 0; i < (8 - length); i++)
                str.insert(0, "0");
            temp.append(str.toString());
        }

        return Integer.parseInt(temp.toString(), 2);
    }
}