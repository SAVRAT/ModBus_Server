import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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
    private final int nRequests = 1;
    private int counter = 0;
    private Parsing parse = new Parsing();
    private DataBaseConnect dataBaseConnect;
    private Vertx vertx;
    int[] buffer;
    int[] aiCount;

    ModBus_Master(DataBaseConnect dataBaseConnect, Vertx vertx) {
        this.dataBaseConnect = dataBaseConnect;
        this.vertx = vertx;
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

        int quantity = 10;
        int startAddress = 0;
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity *2), 0);
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

//    void start_OBEH(String[] slaveAddress, int regAddr) {
//        started = true;
//        for (String address : slaveAddress) {
//            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address)
//                    .setPort(502)
//                    .build();
//            ModbusTcpMaster master = new ModbusTcpMaster(config);
//            new Thread(() -> {
//                for (int j = 0; j < nRequests; j++) {
//                    System.out.println(master.getConfig().getAddress());
//                    sendAndReceive_OBEH(master, regAddr);
//                }
//                System.out.println("===================");
//            }).start();
//        }
//    }

    void sendAndReceive_OBEH_DI(String address, String tableName, String ID){
        ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(address).setPort(502).build();
        ModbusTcpMaster master = new ModbusTcpMaster(config);

        Parsing parse = new Parsing();
        final int regAddr = Integer.valueOf(ID);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(regAddr, 2), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null){
                ByteBuf out = response.getRegisters().readSlice(4);
                System.out.println(Arrays.toString(parse.byteToBoolArray(out)));
                String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
                JsonArray jsonArray = new JsonArray();
                jsonArray.add("data");
                jsonArray.add(String.valueOf(((double) System.currentTimeMillis())/1000));
                dataBaseConnect.databaseWrite(query, jsonArray);
                ReferenceCountUtil.release(response);
            }else {
                System.out.println("ERROR");
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
        }, Modbus.sharedExecutor());
    }

//    void start_OBEH_AI() {
//        if (counter==0)
//        for (int i=0; i<oven_AI_IP.length; i++) {
//            String table = oven_AI_Tabl[i], analogID = oven_AI_ID[i];
//            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(oven_AI_IP[i])
//                    .setPort(502)
//                    .build();
//            ModbusTcpMaster master = new ModbusTcpMaster(config);
//            new Thread(() -> sendAndReceive_OBEH_AI(master, table, analogID)).start();
//        }
//    }

    void sendAndReceive_OBEH_AI(ModbusTcpMaster master, String ID, String tableName, int bufId){

        final int addr = 4063 + Integer.valueOf(ID);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(addr, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                byte[] b = {response.getRegisters().getByte(1),
                        response.getRegisters().getByte(0)};
                int res = (int) ByteBuffer.wrap(b).getShort();
                parse.data.put(String.valueOf(addr), res);
                String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
                int time = (int) (((double) System.currentTimeMillis())/1000);
                JsonArray jsonArray = new JsonArray().add(res).add(time);
                //System.out.println("DataBase writeData: " + jsonArray);
                dataBaseConnect.databaseWrite(query, jsonArray);
                ReferenceCountUtil.safeRelease(response);
            } else {
                System.out.println("ERROR response from ip: " + master.getConfig().getAddress());
                parse.data.put(String.valueOf(addr), 0);
                logger.error("Completed exceptionally, message={}", ex.getMessage(), ex);
            }
            decBuffer(bufId);
            if (aiCount[bufId] == 0){
                master.disconnect();
            }
        }, Modbus.sharedExecutor());

    }


    private void decBuffer(int bufId){
        buffer[bufId]--;
        aiCount[bufId]--;
    }

    private void handle(){
        counter = 0;
        System.out.println(parse.data);
        System.out.println("---------------------------------------------");
    }

    void stop() {
        started = false;
        masters.forEach(ModbusTcpMaster::disconnect);
        masters.clear();
    }
}