import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Controller {

    private Geometry geom = new Geometry();
    ArrayList<Double> rads = new ArrayList<>();

    double[][] data1 = {{30,70}, {40,90}, {60,95}, {65,125}, {70,140}, {100,130}, {120,120}, {140,105}, {160,100},
            {170,80}, {170,70}, {160,45}, {140,25}, {120,10}, {100,10}, {60,25}, {50,30}, {45,40}, {40,50}, {30,55}};
    double[][] data = {{20,65}, {40,85}, {50,110}, {65,125}, {70,130}, {95,125}, {110,120}, {125,115}, {140,95},
            {155,75}, {155,55}, {140,40}, {125,35}, {110,25}, {95,20}, {80,15}, {65,25}, {50,30}, {35,35}, {20,40}};
    double[][] data2 = {{60,20}, {40,40}, {50,60}, {70,80}, {110,80}, {124,60}, {124,40}, {90,20}};


    double[][] data3 = {{25,8.5}, {15,14.2}, {10,20}, {7,25.7}, {11,33.8}, {15,42}, {32,51.7}, {34.6,50}, {46.3,53},
            {51,51.1}, {57,42.7}, {60,34.6}, {63,26}, {55,20.4}, {50,14.7}, {46,9.2}};

    private final double[] sensorMatrix = {8.5, 14.2, 20, 25.7, 33.8, 42, 51.7, 34.6, 46.3, 51.1, 42.7, 34.6, 26, 20.4, 14.7, 9.2};
    private final int scannerHight = 89, scannerWight = 80, maxLength = 55, maxHight = 65;
    final double EPS = 0.6, convStep = 10;

    Formul[] formulData = new Formul[20];
    ArrayList<double[][]> figure = new ArrayList<>();
    ArrayList<double[][]> centres = new ArrayList<>();
    ArrayList<Double> intersectDots = new ArrayList<>();
    ArrayList<Double> intersectRad = new ArrayList<>();
    CopyOnWriteArrayList<double[][]> outData = new CopyOnWriteArrayList<>();

//    double[][] doSlice(ArrayList<Integer> data){
//        HashMap<String, double[][]> out = new HashMap<>();
//        HashMap<String, Integer> tempData = new HashMap<>();
//        double[][] def = new double[16][2];
//        int count = 0;
//        for (Integer dat : data) {
//            if (dat == null)
//                count++;
//        }
//        if (count>6){
//            System.out.println("STOP.");
//            return def;
//        }else
//        if (data.size()==16) {
//            for (int i = 0; i < data.size(); i++) {
//                if (i < 7)
//                    tempData.put("L_" + (i + 1), data.get(i));
//                else if (i < 9)
//                    tempData.put("T_" + (i + 1), data.get(i));
//                else
//                    tempData.put("R_" + (i + 1), data.get(i));
//            }
//            System.out.println("1: " + tempData);
//            for (int i = 6; i >= 0; i--) {
//                if (data.get(i) == null || data.get(i) > maxLength)
//                    tempData.replace("L_" + (i + 1), 999);
//                else break;
//            }
//            for (int i = 9; i < 16; i++) {
//                if (data.get(i) == null || data.get(i) > maxLength)
//                    tempData.replace("R_" + (i + 1), 999);
//                else break;
//            }
//            System.out.println("2: " + tempData);
//            for (int i = 0; i < 16; i++) {
//                double[][] dot = new double[1][2];
//                if (i == 0) {
//                    if (tempData.get("L_" + (i + 1)) == null)
//                        tempData.replace("L_" + (i + 1), tempData.get("L_" + (i + 2)));
//                    dot[0][0] = tempData.get("L_" + (i + 1));
//                    dot[0][1] = sensorMatrix[i];
//                    out.put("L_" + (i + 1), dot);
//                } else if (i < 7) {
//                    if (tempData.get("L_" + (i + 1)) == null) {
//                        int avg = (tempData.get("L_" + (i)) + tempData.get("L_" + (i + 2))) / 2;
//                        tempData.replace("L_" + (i + 1), avg);
//                    }
//                    dot[0][0] = tempData.get("L_" + (i + 1));
//                    dot[0][1] = sensorMatrix[i];
//                    out.put("L_" + (i + 1), dot);
//                } else if (i < 9) {
//                    if ((tempData.get("T_" + (8)) == null || tempData.get("T_" + (8))>maxLength) &&
//                            (tempData.get("T_" + (9)) == null || tempData.get("T_" + (9))>maxLength)) {
//                        return def;
//                    }else {
//                        if (tempData.get("T_" + (8)) == null || tempData.get("T_" + (8))>maxLength)
//                            tempData.replace("T_" + (8), tempData.get("T_" + (9)));
//                        if (tempData.get("T_" + (9)) == null || tempData.get("T_" + (9))>maxLength)
//                            tempData.replace("T_" + (9), tempData.get("T_" + (8)));
//                    }
//                    dot[0][0] = sensorMatrix[i];
//                    dot[0][1] = scannerHight - tempData.get("T_" + (i + 1));
//                    out.put("T_" + (i + 1), dot);
//                } else {
//                    if (i == 15) {
//                        if (tempData.get("R_" + (i + 1)) == null)
//                            tempData.replace("R_" + (i + 1), tempData.get("R_" + (i)));
//                    } else if (tempData.get("R_" + (i + 1)) == null) {
//                        int avg = (tempData.get("R_" + (i)) + tempData.get("R_" + (i + 2))) / 2;
//                        tempData.replace("R_" + (i + 1), avg);
//                    }
//                    dot[0][0] = scannerWight - tempData.get("R_" + (i + 1));
//                    dot[0][1] = sensorMatrix[i];
//                    out.put("R_" + (i + 1), dot);
//                }
////                System.out.println("Dot: " + Arrays.deepToString(dot));
//            }
//        }
//        System.out.println("Out: " + out);
//        ArrayList<double[][]> res = new ArrayList<>();
//        for (int i=0; i<out.size(); i++){
//            double[][] temp = new double[1][2];
//            if (i<7){
//                if (out.get("L_" + (i + 1))[0][0] < 800 && out.get("L_" + (i + 1))[0][0] > 0) {
//                    temp[0][0] = out.get("L_" + (i + 1))[0][0];
//                    temp[0][1] = out.get("L_" + (i + 1))[0][1];
//                    res.add(temp);
//                }
//            }else if (i<9){
//                temp[0][0] = out.get("T_" + (i + 1))[0][0];
//                temp[0][1] = out.get("T_" + (i + 1))[0][1];
//                res.add(temp);
//            }else if (out.get("R_" + (i + 1))[0][0] < 800 && out.get("R_" + (i + 1))[0][0] > 0){
//                temp[0][0] = out.get("R_" + (i + 1))[0][0];
//                temp[0][1] = out.get("R_" + (i + 1))[0][1];
//                res.add(temp);
//            }
//        }
//        double[][] ret = new double[res.size()][2];
//        for (int i=0; i<res.size(); i++){
//            ret[i][0] = res.get(i)[0][0];
//            ret[i][1] = res.get(i)[0][1];
//        }
//        System.out.println("Ret: " + Arrays.deepToString(ret));
//        return ret;
//    }

    double[][] doSlice(ArrayList<Integer> data){
        ArrayList<double[][]> out = new ArrayList<>();
        HashMap<String, double[][]> tempData = new HashMap<>();
        if (data.size()==16) {
            for (int i = 0; i < 16; i++) {
                double[][] temp = new double[1][2];
                if (i < 7) {
                    if (data.get(i)!=null && data.get(i)<maxLength) {
                        temp[0][0] = data.get(i);
                        temp[0][1] = sensorMatrix[i];
                        tempData.put("L" + i, temp);
                    }
                } else if (i < 9) {
                    if (data.get(i)!=null && data.get(i)<maxHight) {
                        temp[0][1] = scannerHight-data.get(i);
                        temp[0][0] = sensorMatrix[i];
                        tempData.put("P" + i, temp);
                    }
                } else {
                    if (data.get(i)!=null && data.get(i)<maxLength) {
                        temp[0][0] = scannerWight-data.get(i);
                        temp[0][1] = sensorMatrix[i];
                        tempData.put("R" + i, temp);
                    }
                }
            }
        }

        for (int i=0; i<16; i++){
            if (i < 7) {
                if (tempData.get("L" + i) != null)
                    out.add(tempData.get("L" + i));
            } else if (i < 9) {
                if (tempData.get("T"+i)!=null)
                    out.add(tempData.get("T" + i));
        } else {
                if (tempData.get("R"+i)!=null)
                    out.add(tempData.get("R" + i));
            }
        }
        double[][] mass = new double[out.size()][2];
        for (int i=0; i<out.size(); i++){
            mass[i] = out.get(i)[0];
        }
        System.out.println("Mass: " + Arrays.deepToString(mass));
        return mass;
    }
}