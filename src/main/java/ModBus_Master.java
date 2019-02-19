import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

class ModBus_Master {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<ModbusTcpMaster> masters = new CopyOnWriteArrayList<>();
    private volatile boolean started = false;
    private final int nRequests;
    private final int quantity;
    private final int startAddress;

    ModBus_Master(int startAddress, int quantity, int nRequests) {
        this.quantity = quantity;
        this.nRequests = nRequests;
        this.startAddress = startAddress;
    }

    void start(String[] slaveAddress) {
        started = true;
        for (String address : slaveAddress) {
            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address)
                    .setPort(502)
                    .build();
            ModbusTcpMaster master = new ModbusTcpMaster(config);
            new Thread(() -> {
                for (int j = 0; j < nRequests; j++) {
                    System.out.println(master.getConfig().getAddress());
                    sendAndReceive(master);
                }
                System.out.println("===================");
            }).start();
        }
    }

    private void sendAndReceive(ModbusTcpMaster master) {
        if (!started) return;

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity*2), 0);
        Parsing parse = new Parsing();

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), 10);
                System.out.println("IpAddress:  " + master.getConfig().getAddress() + " :  " + data);
                ReferenceCountUtil.release(response);
            } else {
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
            scheduler.schedule(() -> sendAndReceive(master), 10, TimeUnit.MILLISECONDS);
        }, Modbus.sharedExecutor());
    }

    void start_OBEH(String[] slaveAddress, int regAddr) {
        started = true;
        for (String address : slaveAddress) {
            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address)
                    .setPort(502)
                    .build();
            ModbusTcpMaster master = new ModbusTcpMaster(config);
            new Thread(() -> {
                for (int j = 0; j < nRequests; j++) {
                    System.out.println(master.getConfig().getAddress());
                    sendAndReceive_OBEH(master, regAddr);
                }
                System.out.println("===================");
            }).start();
        }
    }

    void start_OBEH_AI(String[] slaveAddress, int startAddr, int endAddr) {
        started = true;
        for (String address : slaveAddress) {
            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address)
                    .setPort(502)
                    .build();
            ModbusTcpMaster master = new ModbusTcpMaster(config);
            new Thread(() -> {
                                for (int j = 0; j < nRequests; j++) {
                    System.out.println(master.getConfig().getAddress());
                    sendAndReceive_OBEH_AI(master, startAddr, endAddr);
                }
                System.out.println("===================");
            }).start();
        }
    }

    private void sendAndReceive_OBEH(ModbusTcpMaster master, int regAddr){
        Parsing parse = new Parsing();
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(regAddr, 2), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null){
                ByteBuf out = response.getRegisters().readSlice(4);
                System.out.println(Arrays.toString(parse.byteToBoolArray(out)));;
                ReferenceCountUtil.release(response);
            }else {
                System.out.println("ERROR");
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
            scheduler.schedule(() -> sendAndReceive_OBEH(master, regAddr), 1000, TimeUnit.MILLISECONDS);
        }, Modbus.sharedExecutor());
    }

    private void sendAndReceive_OBEH_AI(ModbusTcpMaster master, int startAddr, int endAddr){
            ArrayList<Integer> data = new ArrayList<>();
            Parsing parse = new Parsing();
            for (int regAddr=startAddr; regAddr<=endAddr; regAddr++) {
                final int addr = regAddr;
//                System.out.println(regAddr);
                try {
                    Thread.sleep(18);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CompletableFuture<ReadHoldingRegistersResponse> future =
                        master.sendRequest(new ReadHoldingRegistersRequest(regAddr, 1), 1);
                future.whenCompleteAsync((response, ex) -> {
                    if (response != null) {
                        byte[] b = {response.getRegisters().getByte(1),
                                response.getRegisters().getByte(0)};
//                        System.out.println(response.getRegisters().getByte(1) +
//                                " :: " + response.getRegisters().getByte(0));
//                        parse.toList((int) ByteBuffer.wrap(b).getShort());
                        int res = (int) ByteBuffer.wrap(b).getShort();
                        System.out.println("addr: " + addr);
                        System.out.println(res);
                        parse.data.put(String.valueOf(addr), res);
//                        data.add(res);
//                        future.join(new ReadHoldingRegistersRequest);
                        ReferenceCountUtil.safeRelease(response);
//                        ReferenceCountUtil.release(response);
                    } else {
                        System.out.println("ERROR");
                        logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
                    }
                }, Modbus.sharedExecutor());
            }
        System.out.println("---------------------------------------------");
        System.out.println(data);
        parse.data.put("9999", 9999);
        System.out.println(parse.data);
//        scheduler.schedule(() -> sendAndReceive_OBEH_AI(master, startAddr, endAddr), 800, TimeUnit.MILLISECONDS);
            }

    void stop() {
        started = false;
        masters.forEach(ModbusTcpMaster::disconnect);
        masters.clear();
    }
}