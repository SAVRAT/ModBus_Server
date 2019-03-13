import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class Test {
    Parsing parse = new Parsing();

    void testing(String address){
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address).setPort(502).build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);

        for (int i=1; i<4; i++){
            //try {
            //    Thread.sleep(100);
            //} catch (InterruptedException e) {
            //    e.printStackTrace();
            //}
            int ID = i;
            new Thread(() -> sendAndReceive_OBEH_AI(address, String.valueOf(ID), master)).start();
            //sendAndReceive_OBEH_AI(address, String.valueOf(i), master);
        }
    }

    void sendAndReceive_OBEH_AI(String address, String ID, ModbusTcpMaster master){

        final int addr = 4063 + Integer.valueOf(ID);

//        try {
//            Thread.sleep(25);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(addr, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
//            counter++;
//            System.out.println("Increment: " + counter);
            if (response != null) {
                byte[] b = {response.getRegisters().getByte(1),
                        response.getRegisters().getByte(0)};
                int res = (int) ByteBuffer.wrap(b).getShort();
                parse.data.put(String.valueOf(addr), res);
                System.out.println("Result: " + res);
            } else {
                System.out.println("ERROR response");
                parse.data.put(String.valueOf(addr), 0);
            }
//                    if (counter==1)
//                        handle();
        }, Modbus.sharedExecutor());
//        scheduler.schedule(this::start_OBEH_AI, 1000, TimeUnit.MILLISECONDS);
    }

}
