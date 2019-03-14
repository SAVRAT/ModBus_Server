import com.digitalpetri.modbus.codec.Modbus;
import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

class Test {
    Parsing parse = new Parsing();
    int[] buffer;

    void testing(){
        ArrayList<String[]> data = new ArrayList<>();
        ArrayList<ArrayList<String[]>> forWrite = new ArrayList<>();
        String[] row = new String[3];
        row[0] = "192.168.49.245";
        row[1] = "table_1";
        row[2] = "1";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.245";
        row[1] = "table_1";
        row[2] = "2";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.245";
        row[1] = "table_1";
        row[2] = "3";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.246";
        row[1] = "table_1";
        row[2] = "1";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.246";
        row[1] = "table_1";
        row[2] = "2";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.246";
        row[1] = "table_1";
        row[2] = "3";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.246";
        row[1] = "table_2";
        row[2] = "6";
        data.add(row);
        row = new String[3];
        row[0] = "192.168.49.246";
        row[1] = "table_2";
        row[2] = "4";
        data.add(row);

        for (String[] val:data)
            System.out.println(Arrays.toString(val));

        ArrayList<String> ipAddr = new ArrayList<>();
        for (String[] datum : data) {
            boolean check = false;
            for (String str : ipAddr)
                if (str.equals(datum[0]))
                    check = true;
            if (!check)
                ipAddr.add(datum[0]);
        }
        buffer = new int[ipAddr.size()];
        for (String ip : ipAddr) {
            ArrayList<String[]> temp = new ArrayList<>();
            for (String[] item : data) if (ip.equals(item[0])) temp.add(item);
            forWrite.add(temp);
        }

        System.out.println("IP Addresses: " + ipAddr);
        for (int i = 0; i < forWrite.size(); i++) {
            System.out.println("Number: " + (i+1));
            for (String[] stroke:forWrite.get(i))
                System.out.print(Arrays.toString(stroke) + " :: ");
            System.out.println("");
        }

        for (int i=0; i<ipAddr.size(); i++){
            int lamI = i;
            new Thread(() -> {
                ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(ipAddr.get(lamI)).setPort(502).build();
                ModbusTcpMaster master = new ModbusTcpMaster(config);
                System.out.println("Thread " + (lamI+1) + " started. IP: " + master.getConfig().getAddress());
                buffer[lamI] = 0;
                int count = 0, num = 0;

                while (count < forWrite.get(lamI).size()){
                    System.out.print("");
                    if (buffer[lamI] < 1) {
                        count++;
                        buffer[lamI]++;
                        sendAndReceive_OBEH_AI(master, forWrite.get(lamI).get(num)[2], lamI);
                        num++;
                    }
                }
            }).start();
        }
    }

    private void sendAndReceive_OBEH_AI(ModbusTcpMaster master, String id, int bufId){
        final int port = 4063 + Integer.valueOf(id);
        CompletableFuture<ReadHoldingRegistersResponse> future =
                master.sendRequest(new ReadHoldingRegistersRequest(port, 1), 1);
        future.whenCompleteAsync((response, ex) -> {
            if (response != null) {
                byte[] b = {response.getRegisters().getByte(1),
                        response.getRegisters().getByte(0)};
                int res = (int) ByteBuffer.wrap(b).getShort();
                parse.data.put(String.valueOf(port), res);
                System.out.println("Result " + master.getConfig().getAddress() + " :: " + id + ": " + res);
            } else {
                System.out.println("ERROR response");
                parse.data.put(String.valueOf(port), 0);
            }
            decBuffer(bufId);
        }, Modbus.sharedExecutor());
    }

    void decBuffer(int id){
        buffer[id]--;
    }
}
