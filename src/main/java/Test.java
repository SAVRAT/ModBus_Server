import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.CompletableFuture;

class Test{
    public static void main(String[] args) {
        String address = "192.168.49.237";
        Parsing parse = new Parsing();
        final int regAddr = 0, quantity = 2;

        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address).setPort(5000)
                .build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(regAddr, quantity), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null){
                ByteBuf out = response.getRegisters().readSlice(4);
                System.out.println(parse.dInt(out, 2));
                ReferenceCountUtil.release(response);
            }else {
                System.out.println("ERROR: " + ex.getMessage() + " :: " + ex);
            }
        }, Modbus.sharedExecutor());
    }
}