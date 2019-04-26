import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;

class Parsing {

    ArrayList<Integer> dInt(ByteBuf registers, int length){
        ArrayList<Integer> output = new ArrayList<>();
        for (int i=0; i<length*2; i+=2){
            byte[] arr= {registers.getByte(i),registers.getByte(i+1)};
            ByteBuffer buff = ByteBuffer.wrap(arr);
            int res = buff.getShort();
//            System.out.println((i/2 + 1) + ": " + Arrays.toString(arr) + " :: " + res);
            output.add(res);
        }
        return output;
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

    int uByteToInt(short[] data){
        StringBuilder temp = new StringBuilder();
        for (short datum : data) {
            StringBuilder str = new StringBuilder(Integer.toBinaryString(0xFFFF & datum));
            int length = str.length();
            for (int i = 0; i < (8 - length); i++)
                str.insert(0, "0");
            temp.append(str.toString());
        }

        return Integer.parseInt(temp.toString(), 2);
    }

    float byteToFloat(byte[] data){

        return ByteBuffer.wrap(data).getFloat();
    }

    int deviceStateOven(boolean[] statusWord, int startId){
        int outStat = 0;
        if (statusWord[startId]) {
            if (statusWord[startId + 2]) outStat = 2;
            else if (statusWord[startId + 1]) outStat = 1;
            else if (statusWord[startId]) outStat = 3;
        }
        return outStat;
    }

    int deviceState(int status){
        int stat = 0;
        if (status == 1280 || status == 1792) stat = 2;
        else if (status == 768) stat = 1;
        else if (status == 256) stat = 3;
        return stat;
    }
}