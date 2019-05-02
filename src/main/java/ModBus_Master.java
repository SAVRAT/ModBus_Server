import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.json.JsonArray;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

class ModBus_Master {
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

        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                ArrayList<Integer> data = parse.dInt(response.getRegisters(), quantity);
                for (String[] device:writeData){
                    if (device[0].equals(master.getConfig().getAddress())){
                        int startId = Integer.valueOf(device[3])-1;
                        int currentState = parse.deviceState(data.get(startId));
                        if (device[4].contains("lu_N")){
                            float f = (float) Math.round(parse.byteToFloat(new byte[]{
                                    response.getRegisters().getByte(startId*2 + 2),
                                    response.getRegisters().getByte(startId*2 + 3),
                                    response.getRegisters().getByte(startId*2 + 4),
                                    response.getRegisters().getByte(startId*2 + 5)})*1000)/1000;
                            int shift = parse.uByteToInt(new short[]{response.getRegisters()
                                    .getUnsignedByte(startId*2 + 6), response.getRegisters()
                                    .getUnsignedByte(startId*2 + 7)});
                            dataBaseConnect.databaseReadShift(device, f, shift);
                        }
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                ReferenceCountUtil.release(response);
                moduleOk(master);
            } else {
                for (String[] device:writeData){
                    if (device[0].equals(master.getConfig().getAddress())){
                        int currentState = 0;
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                System.out.println("\u001B[41m" + "PLC ERROR" + "\u001B[0m" + " " + ex.getMessage());
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
                        int currentState = parse.deviceStateOven(data, startId);
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                ReferenceCountUtil.release(response);
                moduleOk(master);
            }else {
                boolean[] data = new boolean[20];
                for (String[] device:writeData){
                    if (device[0].equals(master.getConfig().getAddress())){
                        int startId = Integer.valueOf(device[3])-1;
                        int currentState = parse.deviceStateOven(data, startId);
                        dataBaseConnect.databaseReadOEE(device, currentState);
                    }
                }
                System.out.println("\u001B[41m" + "DI ERROR" + "\u001B[0m" + " " + ex.getMessage());
                moduleError(master);
            }
            master.disconnect();
        }, Modbus.sharedExecutor());
    }

    void sendAndReceive_OBEH_AI(ModbusTcpMaster master, String ID, String tableName, int bufId){
        Parsing parse = new Parsing();
        final int addr = 4063 + Integer.valueOf(ID);

        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(addr, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                short[] tempValue = {response.getRegisters().getUnsignedByte(1),
                        response.getRegisters().getUnsignedByte(0)};
                int res = (int) Math.round(((double) parse.uByteToInt(tempValue))/65535*50);
                String query = "INSERT INTO " + tableName + " (value, time) VALUES (?, ?)";
                int time = (int) (((double) System.currentTimeMillis())/1000);
                JsonArray jsonArray = new JsonArray().add(res).add(time);
                dataBaseConnect.databaseWrite(query, jsonArray);
                ReferenceCountUtil.safeRelease(response);
                moduleOk(master);
            } else {
                moduleError(master);
//                System.out.println("\u001B[41m" + "ERROR" + "\u001B[0m" + " " + ex.getMessage());
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