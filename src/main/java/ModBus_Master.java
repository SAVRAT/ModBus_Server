import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;

class ModBus_Master {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Parsing parse = new Parsing();
    private DataBaseConnect dataBaseConnect;
    int[] buffer;
    int[] aiCount;

    ModBus_Master(DataBaseConnect dataBaseConnect) {
        this.dataBaseConnect = dataBaseConnect;
    }

    void sendAndReceive_PLC(ModbusTcpMaster master, int quantity, ArrayList<String[]> writeData) {
        Parsing parse = new Parsing();
        int startAddress = 0;

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);
//        System.out.println("Request done.");

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), quantity);
//                System.out.println("IpAddress:  " + master.getConfig().getAddress() + " :  " + data);
                for (String[] device:writeData){
                    if (device[0].equals(master.getConfig().getAddress())){
                        int startId = Integer.valueOf(device[3])-1;
                        int currentState = deviceState(data.get(startId));
//                        System.out.println(device[0] + " :: " + currentState);
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                ReferenceCountUtil.release(response);
                moduleOk(master);
            } else {
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
                moduleError(master);
            }
            master.disconnect();
        }, Modbus.sharedExecutor());
    }

    void sendAndReceive_OBEH_DI(ModbusTcpMaster master, int quantity, ArrayList<String[]> writeData){

        Parsing parse = new Parsing();
        final int startAddress = 51;

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(startAddress, quantity), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null){
                ByteBuf out = response.getRegisters().readSlice(4);
                boolean[] data = parse.byteToBoolArray(out);

                for (String[] device:writeData){
                    if (device[0].equals(master.getConfig().getAddress())){
                        int startId = Integer.valueOf(device[3])-1;
                        int currentState = deviceStateOven(data, startId);
//                        System.out.println(device[0] + " :: " + currentState);
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                ReferenceCountUtil.release(response);
                moduleOk(master);
            }else {
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
                moduleError(master);
            }
            master.disconnect();
        }, Modbus.sharedExecutor());
    }

    void sendAndReceive_OBEH_AI(ModbusTcpMaster master, String ID, String tableName, int bufId){
        final int addr = 4063 + Integer.valueOf(ID);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(addr, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                short[] tempValue = {response.getRegisters().getUnsignedByte(1),
                        response.getRegisters().getUnsignedByte(0)};
                int res = byteToInt(tempValue);
                String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
                int time = (int) (((double) System.currentTimeMillis())/1000);
                JsonArray jsonArray = new JsonArray().add(res).add(time);
                dataBaseConnect.databaseWrite(query, jsonArray);
                ReferenceCountUtil.safeRelease(response);
                moduleOk(master);
            } else {
                moduleError(master);
                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
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

    private int deviceState(int status){
        int stat = 0;
        if (status == 1280 || status == 1792) stat = 2;
        else if (status == 768) stat = 1;
        else if (status == 256) stat = 3;
        return stat;
    }

    private int deviceStateOven(boolean[] statusWord, int startId){
        int outStat = 0;
        if (statusWord[startId]) {
            if (statusWord[startId + 2]) outStat = 2;
            else if (statusWord[startId + 1]) outStat = 1;
            else outStat = 3;
        }
        return outStat;
    }

    private int byteToInt(short[] data){
        StringBuilder temp = new StringBuilder();
        for (int n=0; n<2; n++) {
            StringBuilder str = new StringBuilder(Integer.toBinaryString(0xFFFF & data[n]));
            int length = str.length();
            for (int i = 0; i < (8 - length); i++)
                str.insert(0, "0");
            temp.append(str.toString());
        }
        int result = Integer.parseInt(temp.toString(), 2);

        return (int) Math.round(((double)result)/65535*50);
    }
}