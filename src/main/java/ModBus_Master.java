import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadDiscreteInputsResponse;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.sun.org.apache.xpath.internal.operations.Mod;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
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

    private void sendAndReceive_PLC(ModbusTcpMaster master, int quantity, int address) {
        int startAddress = 0;
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);
        System.out.println("Request done.");
        Parsing parse = new Parsing();

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), quantity);
                System.out.println("IpAddress:  " + master.getConfig().getAddress() + " :  " + data);
                ReferenceCountUtil.release(response);
            } else {
                System.out.println("Error: " + ex.getMessage() + " :: " + ex);
            }
        }, Modbus.sharedExecutor());
    }

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

    void sendAndReceive_OBEH_AI(ModbusTcpMaster master, String ID, String tableName, int bufId){

        final int addr = 4063 + Integer.valueOf(ID);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(addr, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                byte[] b = {response.getRegisters().getByte(1),
                        response.getRegisters().getByte(0)};
                int res = Math.abs((int) ByteBuffer.wrap(b).getShort());
                parse.data.put(String.valueOf(addr), res);
                String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
                int time = (int) (((double) System.currentTimeMillis())/1000);
                JsonArray jsonArray = new JsonArray().add(res).add(time);
                //System.out.println("DataBase writeData: " + jsonArray);
                dataBaseConnect.databaseWrite(query, jsonArray);
                ReferenceCountUtil.safeRelease(response);
                moduleOk(master);
            } else {
                moduleError(master);
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

    private void moduleError(ModbusTcpMaster master){
        String checkQuery = "UPDATE status_connection SET status = 0 WHERE ip = ?;";
        JsonArray jsonArray = new JsonArray().add(master.getConfig().getAddress());
        dataBaseConnect.databaseWrite(checkQuery, jsonArray);
    }

    private void moduleOk(ModbusTcpMaster master){
        String checkQuery = "UPDATE status_connection SET status = 1 WHERE ip = ?;";
        JsonArray jsonArray = new JsonArray().add(master.getConfig().getAddress());
        dataBaseConnect.databaseUpdate(checkQuery, jsonArray);
    }

    private void decBuffer(int bufId){
        buffer[bufId]--;
        aiCount[bufId]--;
    }
}