import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Controller {

    ArrayList<Double> rads = new ArrayList<>();

    double[][] data = {{20,65}, {40,85}, {50,110}, {65,125}, {70,130}, {95,125}, {110,120}, {125,115}, {140,95},
            {155,75}, {155,55}, {140,40}, {125,35}, {110,25}, {95,20}, {80,15}, {65,25}, {50,30}, {35,35}, {20,40}};
    double[][] data1 = {{30,70}, {40,90}, {60,95}, {65,125}, {70,140}, {100,130}, {120,120}, {140,105}, {160,100},
            {170,80}, {170,70}, {160,45}, {140,25}, {120,10}, {100,10}, {60,25}, {50,30}, {45,40}, {40,50}, {30,55}};
    double[][] data2 = {{60,20}, {40,40}, {50,60}, {70,80}, {110,80}, {124,60}, {124,40}, {90,20}};
    double[][] data3 = {{25,8.5}, {15,14.2}, {10,20}, {7,25.7}, {11,33.8}, {15,42}, {32,51.7}, {34.6,50}, {46.3,53},
            {51,51.1}, {57,42.7}, {60,34.6}, {63,26}, {55,20.4}, {50,14.7}, {46,9.2}, {25, 8.5}};
    double[][] data4 = {{30,70}, {40,90}, {60,95}, {65,125}, {70,140}, {100,130}, {120,120}, {140,105}, {160,100},
            {170,80}, {170,70}, {160,45}, {140,25}, {120,10}, {100,10}, {60,25}, {50,30}, {45,40}, {40,50}, {30,55}};
    double[][] forMatrix = {{13,35}, {16,45}, {23,47}, {34,45}, {40,37}, {38,30}, {36,20}, {30,19}, {17,19}, {15,25}};


    private final double[] sensorMatrix = {6.5, 12.5, 19, 25.5, 33.5, 44, 50, 48, 62, 50, 45.9, 32.5, 25, 18.5, 12, 6.5};
    final int scannerHight = 82, scannerWight = 116, maxLength = 60, maxHight = 65;
    final double EPS = 0.4;
    boolean woodLog = false;

    ArrayList<ArrayList<double[][]>> matrixFigure = new ArrayList<>();
    ArrayList<double[][]> figure = new ArrayList<>();
    Formul[] formulData;
    ArrayList<double[][]> centres = new ArrayList<>();
    ArrayList<Double> intersectDots = new ArrayList<>();
    ArrayList<Double> intersectRad = new ArrayList<>();
    CopyOnWriteArrayList<double[][]> outData = new CopyOnWriteArrayList<>();



    double[][] doSlice(ArrayList<Integer> data) {
        ArrayList<double[][]> out = new ArrayList<>();
        HashMap<String, double[][]> tempData = new HashMap<>();
        int count = 0;
        //Цикл проверки наличия объекта в сканере
        for (int i = 0; i < data.size(); i++) {
            if (6 < i && i < 9) {
                if (data.get(i) == null || data.get(i) > maxHight)
                    count++;
            }else
                if (data.get(i) == null || data.get(i) > maxLength)
                count++;
        }
        if (count > 11) {
            System.out.println("Count of null: " + count);
            woodLog = false;
        } else {
            woodLog = true;
            // Цикл фильтриции точек
            if (data.size() == 16) {
                // Занесение в HashMap с соответсвующиме стороне индексами
                for (int i = 0; i < 16; i++) {
                    double[][] temp = new double[1][2];
                    if (i < 7) {
                        if (data.get(i) != null && data.get(i) < maxLength) {
                            temp[0][0] = data.get(i);
                            temp[0][1] = sensorMatrix[i];
                            tempData.put("L" + i, temp);
                        }
                    } else if (i < 9) {
                        if (data.get(i) != null && data.get(i) < maxHight) {
                            temp[0][1] = scannerHight - data.get(i);
                            temp[0][0] = sensorMatrix[i];
                            tempData.put("T" + i, temp);
                        }
                    } else {
                        if (data.get(i) != null && data.get(i) < maxLength) {
                            temp[0][0] = scannerWight - data.get(i);
                            temp[0][1] = sensorMatrix[i];
                            tempData.put("R" + i, temp);
                        }
                    }
                }
            }
            // Отсеивание null значений
            for (int i = 0; i < 16; i++) {
                if (i < 7) {
                    if (tempData.get("L" + i) != null)
                        out.add(tempData.get("L" + i));
                } else if (i < 9) {
                    if (tempData.get("T" + i) != null)
                        out.add(tempData.get("T" + i));
                } else {
                    if (tempData.get("R" + i) != null)
                        out.add(tempData.get("R" + i));
                }
            }
//            for (double[][] val:out)
//                System.out.print(Arrays.deepToString(val) + " : ");
            // Запись в выходной массив
            double[][] mass = new double[out.size()][2];
            for (int i = 0; i < out.size(); i++) {
                mass[i] = out.get(i)[0];
            }
//            System.out.println("Mass: " + Arrays.deepToString(mass));
//            System.out.println("Wood Check: " + woodLog);
            return mass;
        }
//        System.out.println("Wood Check: " + woodLog);
        return null;
    }

}