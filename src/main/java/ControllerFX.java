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
import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ControllerFX {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Controller controller = new Controller();
    private Geom geom = new Geom();
    @FXML
    private TextField input;
    @FXML
    private Canvas canvas;
    @FXML
    private ChoiceBox choose;
    private GraphicsContext gc;
    private ObservableList<String> datas = FXCollections
            .observableArrayList("data_1", "data_2", "data_3", "data_4", "ALL");

    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        choose.setItems(datas);
        choose.setValue(datas.get(3));
        ServerMain serverMain = new ServerMain(controller);
    }

    @FXML
    void view_graph(){
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
//        controller.computeRadius(data);
//        for (int h = 0; h < controller.intersect.size(); h += 2) {
//            drawDot(controller.intersect.get(h), controller.intersect.get(h + 1), scale, canvas.getHeight(), gc, Color.BLUE);
//        }
//        double[] tempDot = {controller.centres.get(controller.centres.size()-1)[0][0],
//                controller.centres.get(controller.centres.size()-1)[1][0]};
//        drawDot(tempDot[0], tempDot[1], scale, canvas.getHeight(), gc, Color.CYAN, 5);
//        gc.setStroke(Color.DARKCYAN);
//        gc.strokeOval(scale * (tempDot[0] - controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                canvas.getHeight() - scale * (tempDot[1] + controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1),
//                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1));
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
        if (!check)
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (controller.woodLog) {
            check = true;
//            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            if (controller.outData.size() > 0) {
                graph(controller.outData.get(controller.outData.size() - 1));
                controller.figure.add(controller.outData.get(controller.outData.size()-1));
            }
            Polygon fig = controller.getPolygon(controller.outData.get(controller.outData.size() - 1));
            controller.matrixFigure.add(controller.matrix(0.7, controller.scannerWight, controller.scannerHight, fig));
            System.out.println("Scan...");
        }else {
            if (check){
                controller.matrixFigure.remove(0);
                controller.matrixFigure.remove(controller.matrixFigure.size()-1);
                ArrayList<double[][]> matrix;
                matrix = controller.matrixIntersection(controller.matrixFigure.get(0), controller.matrixFigure.get(1));
                for (ArrayList<double[][]> val : controller.matrixFigure){
                    matrix = controller.matrixIntersection(matrix, val);
                }
                printMatrix(matrix, Color.BROWN, scale);
//                gc.setStroke(Color.RED);
                double[][] test = controller.matrixToSlice(matrix);
                System.out.println(Arrays.deepToString(test));
//                graph(test);
//                gc.setStroke(Color.BLACK);
                controller.matrixFigure.clear();
            }
            check = false;
//            System.out.println("No wood!");
        }
        scheduler.schedule(this::testing, 250, TimeUnit.MILLISECONDS);
    }

    @FXML
    void doStep(){
        double scale = getScale();
//        graph(controller.forMatrix);
        Polygon fig = controller.getPolygon(controller.forMatrix);
        ArrayList<double[][]> matrix = controller.matrix(0.7, controller.scannerWight, controller.scannerHight, fig);
        printMatrix(matrix, Color.LIGHTGREEN, scale);
//        for (double[][] val:matrix) System.out.println(Arrays.deepToString(val));
        double[][] output = controller.matrixToSlice(matrix);
        graph(output);
        controller.computeRadius(output);
        for (int h = 0; h < controller.intersect.size(); h += 2) {
            drawDot(controller.intersect.get(h), controller.intersect.get(h + 1), scale, canvas.getHeight(), gc, Color.BLUE);
        }
        double[] tempDot = {controller.centres.get(controller.centres.size()-1)[0][0],
                controller.centres.get(controller.centres.size()-1)[1][0]};
        drawDot(tempDot[0], tempDot[1], scale, canvas.getHeight(), gc, Color.CYAN, 5);
        gc.setStroke(Color.DARKCYAN);
//        gc.strokeOval(scale * (tempDot[0] - controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                canvas.getHeight() - scale * (tempDot[1] + controller.intersectRad.get(controller.intersectRad.size() - 1)),
//                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1),
//                scale * 2 * controller.intersectRad.get(controller.intersectRad.size() - 1));
    }

    @FXML
    void doMatrix(){
        Point2D dot = new Point(20, 20);
        double scale = getScale();
        Polygon fig_1 = controller.getPolygon(choose());
        double step = 1;
        ArrayList<double[][]> matrix = controller.matrix(step, (int)Math.round(canvas.getWidth()),
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
        figs.add(controller.getPolygon(controller.data));
        ArrayList<double[][]> matrix_1 = controller.matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        figs.add(controller.getPolygon(controller.data1));
        ArrayList<double[][]> matrix_2 = controller.matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        figs.add(controller.getPolygon(controller.data2));
        ArrayList<double[][]> matrix_3 = controller.matrix(0.8, (int)Math.round(canvas.getWidth()),
                (int)Math.round(canvas.getHeight()), figs.get(figs.size()-1));
        printMatrix(matrix_1, Color.LIGHTGREEN, scale);
        printMatrix(matrix_2, Color.CYAN, scale);
        printMatrix(matrix_3, Color.DARKGRAY, scale);
        ArrayList<double[][]> matrix_r = controller.matrixIntersection(matrix_1, matrix_2);
        ArrayList<double[][]> matrix_res = controller.matrixIntersection(matrix_3, matrix_r);
        for (double[][] m : matrix_res) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, Color.PURPLE, 1);
        }
    }

    private double[][] choose(){
        switch (String.valueOf(choose.getValue())){
            case "data_1": return (controller.data);
            case "data_2": return (controller.data1);
            case "data_3": return (controller.data2);
            case "data_4": return (controller.data3);
            default: return controller.data4;
        }
    }

//    private double[][] matrixToSlice(ArrayList<double[][]> matrix){
//        double scale = getScale(), step_y = 1, step_x = 1;;
//        ArrayList<double[][]> line = new ArrayList<>();
//        double[][] out;
//        int last = 0;
//        for (int i=0; i<matrix.size(); i++){
//            boolean check = false;
//            double step = 0;
//            while (!check && step<controller.scannerWight) {
//                step += step_x;
//                for (int x = 0; x < matrix.get(0).length; x++) {
//                    double[][] temp = new double[1][3];
//                    if (Math.abs(step - matrix.get(i)[x][0]) < step_x * 2 && matrix.get(i)[x][0] != 0) {
//                        temp[0][0] = matrix.get(i)[x][0];
//                        temp[0][1] = matrix.get(i)[x][1];
//                        temp[0][2] = x;
//                        drawDot(temp[0][0], temp[0][1], scale, canvas.getHeight(), gc, Color.RED);
//                        line.add(temp);
//                        check = true;
//                    }
//                    if (check) break;
//                }
//            }
//        }
//        last = (int) line.get(line.size()-1)[0][2];
//        for (int i=last+1; i<matrix.get(0).length; i++){
//            boolean check = false;
//            double step = controller.scannerHight;
//            while (!check && step>0) {
//                step -= step_y;
//                for (int y=matrix.size()-1; y>=0; y--){
//                    double[][] temp = new double[1][3];
//                    if (Math.abs(step - matrix.get(y)[i][0]) < step_x * 2 && matrix.get(y)[i][0] != 0) {
//                        temp[0][0] = matrix.get(y)[i][0];
//                        temp[0][1] = matrix.get(y)[i][1];
//                        temp[0][2] = y;
//                        drawDot(temp[0][0], temp[0][1], scale, canvas.getHeight(), gc, Color.BLUE);
//                        line.add(temp);
//                        check = true;
//                    }
//                    if (check) break;
//                }
//            }
//        }
//        last = (int) line.get(line.size()-1)[0][2];
//        for (int i=last-1; i>0; i--) {
//            boolean check = false;
//            double step = controller.scannerWight;
//            while (!check && step>0) {
//                step -= step_x;
//                for (int x = matrix.get(0).length-1; x > 0; x--) {
//                    double[][] temp = new double[1][3];
//                    if (Math.abs(step - matrix.get(i)[x][0]) < step_x * 2 && matrix.get(i)[x][0] != 0) {
//                        temp[0][0] = matrix.get(i)[x][0];
//                        temp[0][1] = matrix.get(i)[x][1];
//                        temp[0][2] = x;
//                        drawDot(temp[0][0], temp[0][1], scale, canvas.getHeight(), gc, Color.GOLD);
//                        line.add(temp);
//                        check = true;
//                    }
//                    if (check) break;
//                }
//            }
//        }
//        last = (int) line.get(line.size()-1)[0][2];
//        for (int i=last-1; i>line.get(0)[0][2]; i--){
//            boolean check = false;
//            double step = controller.scannerHight;
//            while (!check && step>0) {
//                step -= step_y;
//                for (int y=0; y<matrix.size(); y++){
//                    double[][] temp = new double[1][3];
//                    if (Math.abs(step - matrix.get(y)[i][0]) < step_x * 2 && matrix.get(y)[i][0] != 0) {
//                        temp[0][0] = matrix.get(y)[i][0];
//                        temp[0][1] = matrix.get(y)[i][1];
//                        temp[0][2] = y;
//                        drawDot(temp[0][0], temp[0][1], scale, canvas.getHeight(), gc, Color.PURPLE);
//                        line.add(temp);
//                        check = true;
//                    }
//                    if (check) break;
//                }
//            }
//        }
//        out = new double[line.size()][2];
//        for (int i=0; i<out.length; i++){
//            out[i][0] = line.get(i)[0][0];
//            out[i][1] = line.get(i)[0][1];
//        }
//        return out;
//    }
//
//    private ArrayList<double[][]> matrixIntersection(ArrayList<double[][]> first, ArrayList<double[][]> second){
//        ArrayList<double[][]> out = new ArrayList<>();
//        if (first.size() == second.size()) {
//            for (int n = 0; n < first.size(); n++) {
//                double[][] mass_1 = first.get(n);
//                double[][] mass_2 = second.get(n);
//                double[][] outMass = new double[mass_1.length][2];
//                for (int k = 0; k < mass_1.length - 1; k++)
//                    if (mass_1[k][0] != 0 && mass_2[k][0] != 0 &&
//                            mass_1[k][1] != 0 && mass_2[k][1] != 0) {
//                        outMass[k][0] = mass_1[k][0];
//                        outMass[k][1] = mass_1[k][1];
//                    }
//                out.add(outMass);
//            }
//        }else {
//            System.out.println("matrix's have different size.");
//        }
//        return out;
//    }
//
//    private ArrayList<double[][]> matrix(double step, int xSize, int ySize, Polygon fig){
//        ArrayList<double[][]> dotsMatrix = new ArrayList<>();
//        int count = (int) Math.round(xSize/step);
//        for (double y=0; y<ySize; y+=step) {
//            double[][] mass = new double[count][2];
//            for (int x = 0; x < count; x++) {
//                if(fig.contains(x*step, y)) {
//                    mass[x][0] = x * step;
//                    mass[x][1] = y;
//                }else {
//                    mass[x][0] = 0;
//                    mass[x][1] = 0;
//                }
//            }
//            dotsMatrix.add(mass);
//        }
//        return dotsMatrix;
//    }

    private void printMatrix(ArrayList<double[][]> matrix, Color color, double scale){
        for (double[][] m : matrix) {
            for (int k=0; k<m.length-1; k++)
                if (m[k][0]!=0 && m[k][1]!=0)
                    drawDot(m[k][0], m[k][1], scale, canvas.getHeight(), gc, color, 1);
        }
    }

//    private Polygon getPolygon(double[][] fig){
//        int[] x = new int[fig.length];
//        int[] y = new int[fig.length];
//        for (int i=0; i<fig.length; i++){
//            x[i] = (int) Math.round(fig[i][0]);
//            y[i] = (int) Math.round(fig[i][1]);
//        }
//        return new Polygon(x, y, x.length);
//    }
//
//    private double maxDist(Polygon polygon, double x_centre, double y_centre){
//        Geom geometry = new Geom();
//        double max = 0, x_m = 0, y_m = 0, scale = getScale();
//        for (int i=0; i<polygon.npoints; i++){
//            double dist = geometry.distance(x_centre, y_centre, polygon.xpoints[i], polygon.ypoints[i]);
//            drawDot(polygon.xpoints[i], polygon.ypoints[i], scale, canvas.getHeight(), gc, Color.RED, 3);
//            if (dist > max) {
//                max = dist;
//                x_m = polygon.xpoints[i];
//                y_m = polygon.ypoints[i];
//            }
//        }
//        drawDot(x_m, y_m, scale, canvas.getHeight(), gc, Color.BLUE, 3);
//        System.out.println(x_m + " :: " + y_m);
//        return max;
//    }

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
