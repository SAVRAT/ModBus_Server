import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

class Test {

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        Parsing parse = new Parsing();
        int startAddress = 0, quantity = 2;
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder("192.168.49.234").setPort(5000)
                .build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);
//        System.out.println("Request done.");

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), quantity);
                System.out.println("IpAddress:  " + master.getConfig().getAddress() + " :  " + data);

                ReferenceCountUtil.release(response);
            } else {
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
            }
            master.disconnect();
        }, Modbus.sharedExecutor());
    }
}