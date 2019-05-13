import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;

class ConcurrentComputing {

    private DataBaseConnect dataBaseConnect;
    private Controller controller = new Controller();

    private ArrayList<double[][]> figure;
    private ArrayList<double[]> res = new ArrayList<>();

    ConcurrentComputing(ArrayList<double[][]> figure, DataBaseConnect dataBaseConnect) {
        this.figure = new ArrayList<>(figure);
        this.dataBaseConnect = dataBaseConnect;
        ScannerVerticle.figure.clear();
    }

    void computeAsync(){
        System.out.println("Figure size: " + figure.size());
        if (figure.size() >= 16) {
            System.out.println("Figure size >= 16");
            for (int i = 1; i < 14; i++) {
                res.add(controller.computeRadius(figure.get(i)));
            }
            res.add(controller.computeRadius(figure.get(figure.size() - 2)));
        continueCompute();
        }else if (figure.size() >= 5){
            System.out.println("Figure size >= 5");
            for (int i = 1; i <= figure.size() - 2; i++) {
                res.add(controller.computeRadius(figure.get(i)));
            }
            int lastIndex = res.size() - 1;
            for (int i = figure.size() - 2; i < 15; i++){
                res.add(res.get(lastIndex));
            }
            continueCompute();
        }
    }

    private void continueCompute(){
        double[] rads = new double[14];
        double[][] circleCentres = new double[14][2];
        int zeroCount = 0;
        double averageX = 0, averageY = 0, averageR = 0;

        for (int i = 0; i < 14; i++) {
            rads[i] = Math.abs(res.get(i)[0]);
            circleCentres[i][0] = Math.abs(res.get(i)[1]);
            circleCentres[i][1] = Math.abs(res.get(i)[2]);
        }
        for (int n = 0; n < 2; n++) {
            for (int i = 0; i < 14; i++) {
                if (circleCentres[i][0] != 0)
                    averageX += circleCentres[i][0];
                if (circleCentres[i][1] != 0)
                    averageY += circleCentres[i][1];
                if (rads[i] != 0)
                    averageR += rads[i];
                else
                    zeroCount++;
            }
            averageX = averageX / 14 - zeroCount;
            averageY = averageY / 14 - zeroCount;
            averageR = averageR / 14 - zeroCount;
            for (int i = 0; i < 14; i++) {
                if (circleCentres[i][0] / averageX > 1.1 || circleCentres[i][0] / averageX < 0.9 ||
                circleCentres[i][0] == 0) {
                    circleCentres[i][0] = averageX;
                }
                if (circleCentres[i][1] / averageY > 1.1 || circleCentres[i][1] / averageY < 0.9 ||
                circleCentres[i][1] == 0) {
                    circleCentres[i][1] = averageY;
                }
                if (rads[i] / averageR > 1.15 || rads[i] / averageR < 0.85 || rads[i] == 0) {
                    rads[i] = averageR;
                }
            }
            averageX = 0;
            averageY = 0;
        }
        for (int i = 0; i < 14; i++) {
            averageX += circleCentres[i][0];
            averageY += circleCentres[i][1];
        }
        averageX = averageX / 14;
        averageY = averageY / 14;
        for (int i = 0; i < 14; i++) {
            if (circleCentres[i][0] / averageX > 1.03 || circleCentres[i][0] / averageX < 0.97) {
                circleCentres[i][0] = averageX;
            }
            if (circleCentres[i][1] / averageY > 1.03 || circleCentres[i][1] / averageY < 0.97) {
                circleCentres[i][1] = averageY;
            }
        }
        System.out.println("==================================================");
        for (int i = 0; i < 14; i++)
            System.out.println(i+1 + " :: " + rads[i] + "  " + circleCentres[i][0] + "  " + circleCentres[i][1]);
        for (int i = 14; i < res.size(); i++) {
            System.out.println(i + 1 + " :: " + Arrays.toString(res.get(i)));
        }
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                connection.query("TRUNCATE woodData_3;", result -> {
                    connection.close();
                    if (result.succeeded())
                        for (int i = 0; i < 14; i++) {
                            JsonArray toDatabase = new JsonArray().add(i + 1)
                                    .add((double) Math.round(circleCentres[i][0] * 10) / 10)
                                    .add((double) Math.round(circleCentres[i][1] * 10) / 10)
                                    .add((double) Math.round(rads[i] * 1.2 * 10) / 10);
                            dataBaseConnect.databaseWrite("INSERT INTO woodData_3 VALUES (?, ?, ?, ?)",
                                    toDatabase);
                        }
                });
            }
        });

        double[] usefulVolumeAndCurvature = controller.usefulVolumeAndCurvature(rads, circleCentres);
        double inputRad = rads[0] * 2.3, outputRad = rads[13] * 2.3,
            volume = (double) Math.round(controller.figureVolume(rads) * 0.38 / 1000) / 1000,
            usefulVolume = (double) Math.round(usefulVolumeAndCurvature[0] * 0.48 / 1000) / 1000,
            curvature = usefulVolumeAndCurvature[1];
        if (usefulVolume > volume)
            usefulVolume = volume * 0.95;
        System.out.println("Input Diameter: " + inputRad);
        System.out.println("Output Diameter: " + outputRad);
        System.out.println("Figure Volume: " + volume);
        System.out.println("Useful Volume: " + usefulVolume);
        System.out.println("Curvature: " + curvature);

        woodParamsToDatabase(inputRad, outputRad, volume, usefulVolume, curvature);
    }

    private void woodParamsToDatabase(double inputRad, double outputRad,
                                      double volume, double usefulVolume, double curvature){
        double avgRad = (inputRad + outputRad) / 2;
        JsonArray dataArray = new JsonArray().add(curvature).add(inputRad).add(outputRad)
                .add(avgRad).add(volume).add(usefulVolume);
        dataBaseConnect.databaseWrite("INSERT INTO woodParams (curvature, inputRad," +
                " outputRad, avrRad, volume, usefulVolume, timeStamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP());", dataArray);
    }
}
