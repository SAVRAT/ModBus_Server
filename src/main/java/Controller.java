import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

class Controller {

    private final double[] sensorMatrix = {6.5, 12.5, 19, 25.5, 33.5, 44, 50, 48, 62, 50, 45.9, 32.5, 25, 18.5, 12, 6.5};
    final int scannerHight = 82, scannerWight = 116, maxLength = 60, maxHight = 65;
    private final double EPS = 1;
    boolean woodLog = false;

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
//            System.out.println("Count of null: " + count);
            woodLog = false;
//            System.out.print(".");
        } else {
            woodLog = true;
//            System.out.println("Wood: " + woodLog);
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

    double[] computeRadius(double[][] sliceData) {
//        System.out.println("Start compute Radius, thread name: " + Thread.currentThread().getName());
        Geom geom = new Geom();
        ArrayList<Double> intersectDots = new ArrayList<>();
        ArrayList<Double> intersectRad = new ArrayList<>();
        int counter, testCounter;
        double radius = 5, step_r = 0.4, step_v = 0.3;
        Formul[] formulData = new Formul[sliceData.length];
        geom.lineKoef(formulData, sliceData);
        double[] centreDot = geom.geomCentre(sliceData);
//        double[] centreDot = startDot(sliceData);

        Map<String, Integer> map = new HashMap<>();
        int maxC = 0;
        double maxR = 0;

        boolean finish = false;
        while (!finish){
            intersectDots.clear();
            counter = 0;
            ArrayList<double[]> dotsArray = new ArrayList<>();
//            ArrayList<double[]> debug = new ArrayList<>();
            testCounter = 0;
            while (counter < 1) {
                testCounter++;
                radius += step_r;
//                double[] debugArray = {radius, centreDot[0], centreDot[1]};
//                debug.add(debugArray);
                if (testCounter > 50) {
                    System.out.println("Slice: " + Arrays.deepToString(sliceData));
//                    for (double[] val:debug)
//                        System.out.println(Arrays.toString(val));
                    return new double[3];
                }
                for (int i=0; i<360; i++){
                    double[] tempDot = {
                            radius*Math.sin(Math.toRadians(i))+centreDot[0],
                            radius*Math.cos(Math.toRadians(i))+centreDot[1]
                    };
                    dotsArray.add(tempDot);
                }
                for (int k = 0; k < sliceData.length - 1; k++) {
                    ArrayList<Double> tempDots = new ArrayList<>();
                    double x, y;
                    boolean check = false;
                    Line2D testLine = new Line2D.Double(sliceData[k][0], sliceData[k][1],
                            sliceData[k + 1][0], sliceData[k + 1][1]);

                    for (double[] dot : dotsArray) {
                        if (testLine.intersects(dot[0] - EPS / 2, dot[1] - EPS / 2, EPS, EPS)) {
                            tempDots.add(dot[0]);
                            tempDots.add(dot[1]);
                            check = true;
                            counter++;
                        }
                    }

                    if (check) {
                        int size = tempDots.size();
                        for (int l = 0; l < size; l += 2) {
                            x = tempDots.get(l);
                            y = tempDots.get(l + 1);
                            intersectDots.add(x);
                            intersectDots.add(y);
                            intersectRad.add(radius);
                            String rrr = String.valueOf(radius);
                            int ccc = map.getOrDefault(rrr, 0) + 1;
                            map.put(rrr, ccc);
                            if (ccc > maxC) {
                                maxR = radius;
                                maxC = ccc;
                            }
                        }
                    }
                }
            }
            radius = 5;
            Formul vector;
            double x_v, y_v;
            double[] angles = new double[intersectDots.size() / 2];
            for (int i = 0; i < intersectDots.size(); i += 2) {
                double x = intersectDots.get(i);
                double y = intersectDots.get(i + 1);
                for (int j = 0; j < dotsArray.size(); j++) {
                    if (dotsArray.get(j)[0] == x && dotsArray.get(j)[1] == y) {
                        angles[i / 2] = j; // J * ang, где ang = 1
                    }
                }
            }
            double max = 0;
            int max1 = 0;
            int max2 = 0;
            for (int i = 0; i < angles.length; i++) {
                for (int j = 0; j < angles.length; j++) {
                    if (i != j) {
                        double ai = angles[i];
                        double aj = angles[j];
                        if (ai > 180) ai = ai - 360;
                        if (aj > 180) aj = aj - 360;

                        double diff = Math.abs(ai - aj);
                        if (diff > 180) diff = 360 - diff;
                        if (diff > max) {
                            max = diff;
                            max1 = i;
                            max2 = j;
                        }
                    }
                }
            }
            if (maxC > 40 && maxR == intersectRad.get(intersectRad.size() - 1)) finish = true;

            x_v = (intersectDots.get(max1 * 2) + intersectDots.get(max2 * 2)) / 2;
            y_v = (intersectDots.get(max1 * 2 + 1) + intersectDots.get(max2 * 2 + 1)) / 2;
            vector = geom.lineKoef(x_v, y_v, centreDot[0], centreDot[1]);
            if (!finish) {
                switch (vector.getType()) {
                    case "->": {

                        if (Math.abs(vector.getKoef()) > 10) {
                            centreDot[0] += step_v / 10;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        } else {
                            centreDot[0] += step_v;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }

//                        double tempValue = centreDot[1];
//                        if (Math.abs(tempValue - (vector.getKoef() * (centreDot[0] - x_v) + y_v)) <= step_v){
//                            centreDot[0] += step_v;
//                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
//                        } else {
//                            centreDot[1] += step_v;
//                            centreDot[0] = (centreDot[1] - y_v) / vector.getKoef() + x_v;
//                        }
                    }
                    break;
                    case "<-": {

                        if (Math.abs(vector.getKoef()) > 10) {
                            centreDot[0] -= step_v / 10;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        } else {
                            centreDot[0] -= step_v;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }

//                        double tempValue = centreDot[1];
//                        if (Math.abs(tempValue - (vector.getKoef() * (centreDot[0] - x_v) + y_v)) <= step_v){
//                            centreDot[0] -= step_v;
//                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
//                        } else {
//                            centreDot[1] -= step_v;
//                            centreDot[0] = (centreDot[1] - y_v) / vector.getKoef() + x_v;
//                        }
                    }
                    break;
                    case "vert":
                        centreDot[1] += step_v;
                        break;
                    case "horiz":
                        centreDot[0] += step_v;
                        break;
                }
            }
        }
        // {centreDot[0], centreDot[1]} координаты X и Y центра вписанной окружности

        return new double[]{Collections.max(intersectRad), centreDot[0], centreDot[1]};
    }

    private double[][] matrixToSlice(ArrayList<double[][]> matrix){
        double step_y = 1, step_x = 1;
        ArrayList<double[][]> line = new ArrayList<>();
        double[][] out;
        int last;
        for (double[][] doubles : matrix) {
            boolean check = false;
            double step = 0;
            while (!check && step < scannerWight) {
                step += step_x;
                for (int x = 0; x < matrix.get(0).length; x++) {
                    double[][] temp = new double[1][3];
                    if (Math.abs(step - doubles[x][0]) < step_x * 2 && doubles[x][0] != 0) {
                        temp[0][0] = doubles[x][0];
                        temp[0][1] = doubles[x][1];
                        temp[0][2] = x;
                        line.add(temp);
                        check = true;
                    }
                    if (check) break;
                }
            }
        }
        last = (int) line.get(line.size()-1)[0][2];
        for (int i=last+1; i<matrix.get(0).length; i++){
            boolean check = false;
            double step = scannerHight;
            while (!check && step>0) {
                step -= step_y;
                for (int y=matrix.size()-1; y>=0; y--){
                    double[][] temp = new double[1][3];
                    if (Math.abs(step - matrix.get(y)[i][0]) < step_x * 2 && matrix.get(y)[i][0] != 0) {
                        temp[0][0] = matrix.get(y)[i][0];
                        temp[0][1] = matrix.get(y)[i][1];
                        temp[0][2] = y;
                        line.add(temp);
                        check = true;
                    }
                    if (check) break;
                }
            }
        }
        last = (int) line.get(line.size()-1)[0][2];
        for (int i=last-1; i>0; i--) {
            boolean check = false;
            double step = scannerWight;
            while (!check && step>0) {
                step -= step_x;
                for (int x = matrix.get(0).length-1; x > 0; x--) {
                    double[][] temp = new double[1][3];
                    if (Math.abs(step - matrix.get(i)[x][0]) < step_x * 2 && matrix.get(i)[x][0] != 0) {
                        temp[0][0] = matrix.get(i)[x][0];
                        temp[0][1] = matrix.get(i)[x][1];
                        temp[0][2] = x;
                        line.add(temp);
                        check = true;
                    }
                    if (check) break;
                }
            }
        }
        last = (int) line.get(line.size()-1)[0][2];
        for (int i=last-1; i>line.get(0)[0][2]; i--){
            boolean check = false;
            double step = scannerHight;
            while (!check && step>0) {
                step -= step_y;
                for (int y=0; y<matrix.size(); y++){
                    double[][] temp = new double[1][3];
                    if (Math.abs(step - matrix.get(y)[i][0]) < step_x * 2 && matrix.get(y)[i][0] != 0) {
                        temp[0][0] = matrix.get(y)[i][0];
                        temp[0][1] = matrix.get(y)[i][1];
                        temp[0][2] = y;
                        line.add(temp);
                        check = true;
                    }
                    if (check) break;
                }
            }
        }
        out = new double[line.size()][2];
        for (int i=0; i<out.length; i++){
            out[i][0] = line.get(i)[0][0];
            out[i][1] = line.get(i)[0][1];
        }
        return out;
    }

    private ArrayList<double[][]> matrixIntersection(ArrayList<double[][]> first, ArrayList<double[][]> second){
        ArrayList<double[][]> out = new ArrayList<>();
        if (first.size() == second.size()) {
            for (int n = 0; n < first.size(); n++) {
                double[][] mass_1 = first.get(n);
                double[][] mass_2 = second.get(n);
                double[][] outMass = new double[mass_1.length][2];
                for (int k = 0; k < mass_1.length - 1; k++)
                    if (mass_1[k][0] != 0 && mass_2[k][0] != 0 &&
                            mass_1[k][1] != 0 && mass_2[k][1] != 0) {
                        outMass[k][0] = mass_1[k][0];
                        outMass[k][1] = mass_1[k][1];
                    }
                out.add(outMass);
            }
        }else {
            System.out.println("matrix's have different size.");
        }
        return out;
    }

    private ArrayList<double[][]> matrix(double step,Polygon fig){
        ArrayList<double[][]> dotsMatrix = new ArrayList<>();
        int count = (int) Math.round(scannerWight/step);
        for (double y=0; y<scannerHight; y+=step) {
            double[][] mass = new double[count][2];
            for (int x = 0; x < count; x++) {
                if(fig.contains(x*step, y)) {
                    mass[x][0] = x * step;
                    mass[x][1] = y;
                }else {
                    mass[x][0] = 0;
                    mass[x][1] = 0;
                }
            }
            dotsMatrix.add(mass);
        }
        return dotsMatrix;
    }

    private Polygon getPolygon(double[][] fig){
        int[] x = new int[fig.length];
        int[] y = new int[fig.length];
        for (int i=0; i<fig.length; i++){
            x[i] = (int) Math.round(fig[i][0]);
            y[i] = (int) Math.round(fig[i][1]);
        }

        return new Polygon(x, y, x.length);
    }

    private double polygonArea(double[][] slice){
        double area = 0.0;
        int j = slice.length - 1;
        for (int i = 0; i < slice.length; i++)
        {
            area += (slice[j][0] + slice[i][0]) * (slice[j][1] - slice[i][1]);
            j = i;
        }
        return Math.abs(area / 2.0);
    }

    double figureVolume(double[] rads){
        double volume = 0.0, step = 1680 / 14;

        for (double val:rads){
            volume += 3.14 * Math.pow(val, 2) * step;
        }

        return (double) Math.round(volume);
    }

    double[] usefulVolumeAndCurvature(double[] rads, double[][] centres){

//        System.out.println("Start compute UsefulVolume, thread name: " + Thread.currentThread().getName());

        ArrayList<double[][]> figure = new ArrayList<>();
        for (int n = 0; n < 14; n++) {
            double[][] tempSlice = new double[20][2];
            for (int i = 0; i < 20; i++) {
                tempSlice[i][0] = rads[n] * Math.sin(Math.toRadians(i*18)) + centres[n][0];
                tempSlice[i][1] = rads[n] * Math.cos(Math.toRadians(i*18)) + centres[n][1];
            }
            figure.add(tempSlice);
        }

        ArrayList<double[][]> outMatrix = matrix(0.7, getPolygon(figure.get(0)));
        for (int i = 1; i < figure.size(); i++){
            outMatrix = matrixIntersection(outMatrix,
                    matrix(0.7, getPolygon(figure.get(i))));
        }

        double[][] usefulSlice = matrixToSlice(outMatrix);
        double[] sliceCircle = computeRadius(usefulSlice);
        double maxDist = 0, tempDist;
        for (double[][] val: figure){
            tempDist = maxDist(getPolygon(val), sliceCircle[1], sliceCircle[2]);
            if (maxDist < tempDist)
                maxDist = tempDist;
        }
//        System.out.println("Useful volume done, thread: " + Thread.currentThread().getName()+ " :: " + maxDist);

        return new double[] {(double) 1680*polygonArea(usefulSlice),
                (double) Math.round((maxDist - sliceCircle[0]) / 1.680) / 1000};
    }

    private double maxDist(Polygon polygon, double x_centre, double y_centre){
        Geom geom = new Geom();
        double max = 0;
        for (int i=0; i<polygon.npoints; i++){
            double dist = geom.distance(x_centre, y_centre, polygon.xpoints[i], polygon.ypoints[i]);
            if (dist > max) {
                max = dist;
            }
        }
        return max;
    }

}
