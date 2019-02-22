import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControllerFX {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Controller controller = new Controller();
    private Geometry geom = new Geometry();
    @FXML
    private TextField input;
    @FXML
    private Canvas canvas;
    @FXML
    private ChoiceBox choose;
    private GraphicsContext gc;
    private ObservableList<String> datas = FXCollections.observableArrayList("data_1", "data_2", "data_3", "data_4", "ALL");

    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        choose.setItems(datas);
        choose.setValue(datas.get(3));
        ServerMain serverMain = new ServerMain(controller);
    }

    @FXML
    void view_graph(){
//        input.setText("10");
        graph(choose());
    }

    @FXML
    void clear(){
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    @FXML
    void view_centre(){
    }

    @FXML
    void doAll(){
        double[][] data = choose();
//        input.setText("4.5");
        double scale = getScale();
//        double[] centreDot = geom.geomCentre(data);
        graph(data);
        centre(data);
        compute(data);
        for (int h = 0; h < controller.intersectDots.size(); h += 2) {
            drawDot(controller.intersectDots.get(h), controller.intersectDots.get(h + 1), scale, canvas.getHeight(), gc, Color.BLUE);
        }
        double[] tempDot = {controller.centres.get(controller.centres.size()-1)[0][0],
                controller.centres.get(controller.centres.size()-1)[1][0]};
        drawDot(tempDot[0], tempDot[1], scale, canvas.getHeight(), gc, Color.CYAN, 5);
        gc.setStroke(Color.DARKCYAN);
        gc.strokeOval(scale * (tempDot[0] - controller.intersectRad.get(controller.intersectRad.size() - 1)),
                canvas.getHeight() - scale * (tempDot[1] + controller.intersectRad.get(controller.intersectRad.size() - 1)),
                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1),
                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1));
    }

    @FXML
    void intersection(){
        if (controller.woodLog) {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            if (controller.outData.size() > 0) {
                graph(controller.outData.get(controller.outData.size() - 1));
            }
            System.out.println("Scanning...");
        }else {
            System.out.println("No wood!");
        }
        scheduler.schedule(this::intersection, 120, TimeUnit.MILLISECONDS);
    }

    private boolean check = false;
    @FXML
    void testing(){
        double scale = getScale();
        if (controller.woodLog) {
            check = true;
//            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            if (controller.outData.size() > 0) {
                graph(controller.outData.get(controller.outData.size() - 1));
            }
            Polygon fig = getPolygon(controller.outData.get(controller.outData.size() - 1));
            controller.figure.add(matrix(0.7, controller.scannerWight, controller.scannerHight, fig));
            System.out.println("Scanning...");
        }else {
            if (check){
                controller.figure.remove(0);
                controller.figure.remove(controller.figure.size()-1);
                ArrayList<double[][]> matrix;
                matrix = matrixIntersection(controller.figure.get(0), controller.figure.get(1));
                for (ArrayList<double[][]> val : controller.figure){
                    matrix = matrixIntersection(matrix, val);
                }
                for (double[][] m : matrix) {
                    for (int k=0; k<m.length-1; k++)
                        if (m[k][0]!=0 && m[k][1]!=0)
                            drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.BROWN, 1);
                }


//                gc.setStroke(Color.RED);
                double[][] test = matrixToSlice(matrix);
                System.out.println(Arrays.deepToString(test));
//                graph(test);
//                gc.setStroke(Color.BLACK);
                controller.figure.clear();
            }
            check = false;
            System.out.println("No wood!");
        }
        scheduler.schedule(this::testing, 500, TimeUnit.MILLISECONDS);
    }

    @FXML
    void doStep(){
    }

    @FXML
    void doMatrix(){
        Point2D dot = new Point(20, 20);
        double scale = getScale();
        Polygon fig_1 = getPolygon(choose());
        double step = 1;
        ArrayList<double[][]> matrix = matrix(step, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), fig_1);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (double[][] m : matrix) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.LIGHTBLUE, 1);
        }
        drawDot(dot.getX(), dot.getY(), scale, canvas.getHeight(), gc, Color.RED, 5);
        System.out.println(fig_1.contains(dot));
    }

    @FXML
    void getResult(){
        double scale = getScale();
        ArrayList<Polygon> figs = new ArrayList<>();
        figs.add(getPolygon(controller.data));
        ArrayList<double[][]> matrix_1 = matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        figs.add(getPolygon(controller.data1));
        ArrayList<double[][]> matrix_2 = matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        figs.add(getPolygon(controller.data2));
        ArrayList<double[][]> matrix_3 = matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        for (double[][] m : matrix_1) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.BROWN, 1);
        }
        for (double[][] m : matrix_2) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.CYAN, 1);
        }
        for (double[][] m : matrix_3) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.DARKGRAY, 1);
        }
        ArrayList<double[][]> matrix_r = matrixIntersection(matrix_1, matrix_2);
        ArrayList<double[][]> matrix_res = matrixIntersection(matrix_3, matrix_r);
        for (double[][] m : matrix_res) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.LIGHTGREEN, 1);
        }
    }

    private double[][] choose(){
        switch (String.valueOf(choose.getValue())){
            case "data_1": return (controller.data);
            case "data_2": return (controller.data1);
            case "data_3": return (controller.data2);
            case "data_4": return (controller.data3);
            default: return controller.data;
        }
    }

    private double[][] matrixToSlice(ArrayList<double[][]> matrix){
        ArrayList<double[]> temp = new ArrayList<>();
        double[][] slice;
        for (double[][] val : matrix){
            for (int i=0; i<val.length; i++){
                if (val[i][0]!=0 && val[i-1][0]==0)
                    temp.add(val[i]);
                if (val[i][0]!=0 && val[i+1][0]==0)
                    temp.add(val[i]);
            }
        }
        slice = new double[temp.size()][2];
        for (int i=0; i<temp.size(); i++){
            slice[i][0] = temp.get(i)[0];
            slice[i][1] = temp.get(i)[1];
        }
        return slice;
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

    private ArrayList<double[][]> matrix(double step, int xSize, int ySize, Polygon fig){
        ArrayList<double[][]> dotsMatrix = new ArrayList<>();
        int count = (int) Math.round(xSize/step);
        for (double y=0; y<ySize; y+=step) {
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

    private void compute(double[][] sliceData) {

        long start = System.currentTimeMillis();
        double radius = 5, step_r = 0.8, step_v = 0.6, count;
        geom.lineKoef(controller.formulData, sliceData);
        double[] centreDot = geom.geomCentre(sliceData);

        Map<String, Integer> map = new HashMap<>();
        int maxC = 0;
        double maxR = 0;

        boolean finish = false;
        while (!finish) {
            controller.intersectDots.clear();
            count = 0;
            double ang = 1;
            double[][] dots = geom.dots(centreDot[0], centreDot[1], radius, ang);
            while (count < 1) {
                radius += step_r;
                dots = geom.dots(centreDot[0], centreDot[1], radius, ang);
                for (int k = 0; k < sliceData.length-1; k++) {
                    ArrayList<Double> tempDots = new ArrayList<>();
                    double x, y;
                    boolean check = false;
                    Line2D testLine = new Line2D.Double(sliceData[k][0], sliceData[k][1],
                            sliceData[k + 1][0], sliceData[k + 1][1]);

                    for (double[] dot : dots) {
                        if (testLine.intersects(dot[0] - controller.EPS / 2,
                                dot[1] - controller.EPS / 2, controller.EPS, controller.EPS)) {
                            tempDots.add(dot[0]);
                            tempDots.add(dot[1]);
                            check = true;
                            count++;
                        }
                    }
                    if (check) {
                        int size = tempDots.size();
                        for (int l = 0; l < size; l += 2) {
                            x = tempDots.get(l);
                            y = tempDots.get(l + 1);
                            controller.intersectDots.add(x);
                            controller.intersectDots.add(y);
                            controller.intersectRad.add(radius);
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
            double[] angles = new double[controller.intersectDots.size() / 2];
            for (int i = 0; i < controller.intersectDots.size(); i += 2) {
                double x = controller.intersectDots.get(i);
                double y = controller.intersectDots.get(i + 1);
                for (int j = 0; j < dots.length; j++) {
                    if (dots[j][0] == x && dots[j][1] == y) {
                        angles[i/2] = j * ang;
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
            if (maxC > 40 && maxR == controller.intersectRad.get(controller.intersectRad.size() - 1)) finish = true;

            x_v = (controller.intersectDots.get(max1 * 2) + controller.intersectDots.get(max2 * 2)) / 2;
            y_v = (controller.intersectDots.get(max1 * 2 + 1) + controller.intersectDots.get(max2 * 2 + 1)) / 2;
            vector = geom.lineKoef(x_v, y_v, centreDot[0], centreDot[1]);
            if (!finish) {
                switch (vector.getType()) {
                    case "->": {
                        if (Math.abs(vector.getKoef())>10) {
                            centreDot[0] += step_v/10;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }else {
                            centreDot[0] += step_v;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }
                    }
                    break;
                    case "<-": {
                        if (Math.abs(vector.getKoef())>10) {
                            centreDot[0] -= step_v/10;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }else {
                            centreDot[0] -= step_v;
                            centreDot[1] = vector.getKoef() * (centreDot[0] - x_v) + y_v;
                        }
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
//            drawDot(centreDot[0], centreDot[1], scale, canvas.getHeight(), gc, Color.YELLOWGREEN);
//            gc.strokeOval(scale * (centreDot[0] - controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                    canvas.getHeight() - scale * (centreDot[1] + controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                    scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1),
//                    scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1));
        double[][] mass = {{centreDot[0]}, {centreDot[1]}};
        controller.centres.add(mass);
        controller.rads.add(Collections.max(controller.intersectRad));

        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");

//        System.out.println("Intersect Dots: " + controller.intersectDots);
//        System.out.println("Centre:" + Arrays.deepToString(controller.centres.get(controller.centres.size() - 1)));
//        System.out.println("Radius: " + controller.rads.get(controller.rads.size()-1));
    }

    private double maxDist(Polygon polygon, double x_centre, double y_centre){
        Geometry geometry = new Geometry();
        double max = 0, x_m = 0, y_m = 0, scale = getScale();
        for (int i=0; i<polygon.npoints; i++){
            double dist = geometry.distance(x_centre, y_centre, polygon.xpoints[i], polygon.ypoints[i]);
            drawDot(polygon.xpoints[i], polygon.ypoints[i], scale, canvas.getHeight(), gc, Color.RED, 3);
            if (dist > max) {
                max = dist;
                x_m = polygon.xpoints[i];
                y_m = polygon.ypoints[i];
            }
        }
        drawDot(x_m, y_m, scale, canvas.getHeight(), gc, Color.BLUE, 3);
        System.out.println(x_m + " :: " + y_m);
        return max;
    }

    private double getScale() {
        double scale;
        try {
            scale = Double.valueOf(input.getText());
        } catch (Exception e) {
            scale = 1;
        }
        return scale;
    }

    private void graph(double[][] sliceData) {
//        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
//        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double scale = getScale();
        if (scale < 2) scale = 1;
        for (int i = 0; i < sliceData.length; i++) {
            if (i == sliceData.length - 1) {
                gc.strokeLine(scale * sliceData[i][0], canvas.getHeight() - scale * sliceData[i][1],
                        scale * sliceData[0][0], canvas.getHeight() - scale * sliceData[0][1]);
            } else
                gc.strokeLine(scale * sliceData[i][0], canvas.getHeight() - scale * sliceData[i][1],
                        scale * sliceData[i + 1][0], canvas.getHeight() - scale * sliceData[i + 1][1]);
        }
    }

    private void centre(double[][] sliceData) {
        double scale = getScale(), height = canvas.getHeight();
        double[] centreDot = geom.geomCentre(sliceData);
        drawDot(centreDot[0], centreDot[1], scale, height, gc, Color.RED);
    }

    private void drawDot(double x_c, double y_c, double scale, double height, GraphicsContext gc, Paint color){
        double r = 2;
        gc.setFill(color);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.fillOval(scale*x_c - r/2, (height- scale*y_c - r/2), r,r);
        gc.strokeOval(scale*x_c - r/2, (height - scale*y_c - r/2), r,r);
    }
    private void drawDot(double x_c, double y_c, double scale, double height, GraphicsContext gc, Paint color, double r){
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(2);
        gc.strokeOval(scale*x_c - r/2, (height - scale*y_c - r/2), r,r);
        gc.fillOval(scale*x_c - r/2, (height- scale*y_c - r/2), r,r);
    }
}
