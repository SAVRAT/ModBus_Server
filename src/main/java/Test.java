import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.util.ReferenceCountUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

class Test{
    public static void main(String[] args) {
        String address = "192.168.49.241";
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address).setPort(502)
                .setTimeout(Duration.ofSeconds(5)).build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);
        int quantity = 1;
        int startAddress = 0;
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity *2), 1);
        Parsing parse = new Parsing();

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), 1);
                System.out.println("IpAddress:  " + master.getConfig().getAddress() + " :  " + data);
                ReferenceCountUtil.release(response);
            } else {
                System.out.println("Error: " + ex.getMessage() + " :: " + ex);
            }
        }, Modbus.sharedExecutor());
    }
}