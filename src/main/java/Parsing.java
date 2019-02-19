import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class Parsing {
    private ArrayList<Integer> global = new ArrayList<>();
    ConcurrentHashMap<String, Integer> data = new ConcurrentHashMap<>();

    ArrayList<Integer> dInt(ByteBuf registers, int length){
        ArrayList<Integer> data = new ArrayList<>();
        for (int i=0; i<length*4; i+=4){
            byte[] arr= {registers.getByte(i),registers.getByte(i+1),
                    registers.getByte(i+2),registers.getByte(i+3)};
            ByteBuffer buff = ByteBuffer.wrap(arr);
            data.add(buff.getInt());
        }
        return data;
    }
    boolean[] byteToBoolArray(ByteBuf buffer){
        boolean[] arr = new boolean[20];
        byte high = buffer.getByte(3);
        byte midl = buffer.getByte(0);
        byte low = buffer.getByte(1);
        for (int i=0; i<arr.length; i++){
            if (i<8) {
                byte a = (byte) Math.pow(2, i);
                arr[i] = ((low & a) != 0);
            }else if (i < 16) {
                byte a = (byte) Math.pow(2, i-8);
                arr[i] = ((midl & a) != 0);
            }else {
                byte a = (byte) Math.pow(2, i-16);
                arr[i] = ((high & a) != 0);
            }
        }
        return arr;
    }
    void toList(int val){
        if (global.size()<8)
        global.add(val);
        System.out.println("Global: " + global);
    }
}