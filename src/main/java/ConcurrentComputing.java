import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

class ConcurrentComputing {

    private DataBaseConnect dataBaseConnect;
    private Controller controller = new Controller();

    private ArrayList<double[][]> figure;
    private ArrayList<CompletableFuture<double[]>> futureResultList = new ArrayList<>();
    private CompletableFuture<ArrayList<double[]>> mainFuture;
    private ExecutorService executor;// = Executors.newFixedThreadPool(1);
    private Thread futureThread;

    ConcurrentComputing(ArrayList<double[][]> figure, DataBaseConnect dataBaseConnect, ExecutorService outExecutor) {
        this.figure = new ArrayList<>(figure);
        this.dataBaseConnect = dataBaseConnect;
        this.executor = outExecutor;
        ScannerVerticle.figure.clear();
    }

    void computeAsync(){
        System.out.println("Figure size: " + figure.size());
//        if (figure.size() >= 16) {
//            System.out.println("Figure Size >= 16");
//            for (int i = 1; i < 14; i++) {
//                int index = i;
//                futureResultList.add(CompletableFuture.supplyAsync(() -> {
//                    System.out.println("Start compute " + index);
//                    return controller.computeRadius(figure.get(index));
//                }).completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
//            }
//            futureResultList.add(CompletableFuture.supplyAsync(() -> {
//                System.out.println("Start compute 14");
//                return controller.computeRadius(figure.get(figure.size() - 2));
//            }).completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
//            continueCompute();
//        }else if (figure.size() >= 5){
//            System.out.println("Figure Size >= 5");
//            for (int i = 1; i < figure.size()-2; i++){
//                int index = i;
//                futureResultList.add(CompletableFuture.supplyAsync(() -> {
//                    System.out.println("Start compute " + index);
//                    return controller.computeRadius(figure.get(index));
//                }).completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
//            }
//            for (int i = figure.size()-2; i < 15; i++){
//                futureResultList.add(CompletableFuture.supplyAsync(() -> {
//                    System.out.println("Start compute to fill all");
//                    return controller.computeRadius(figure.get(figure.size()-2));
//                }).completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
//            }
//
//            continueCompute();
//        }
        if (figure.size() >= 16) {
            System.out.println("Figure size >= 16");
            mainFuture = CompletableFuture.supplyAsync(() -> {
                futureThread = Thread.currentThread();
                ArrayList<double[]> outData = new ArrayList<>();
                for (int i = 1; i < 14; i++) {
                    outData.add(controller.computeRadius(figure.get(i)));
                }
                outData.add(controller.computeRadius(figure.get(figure.size() - 2)));
                outData.add(controller.figureVolume(figure, (double) 1680 / figure.size()));
                return outData;
            }, executor).completeOnTimeout(new ArrayList<>(), 1, TimeUnit.SECONDS);
            continueCompute();
        }else if (figure.size() >= 5){
            System.out.println("Figure size >= 5");
            mainFuture = CompletableFuture.supplyAsync(() -> {
                futureThread = Thread.currentThread();
                ArrayList<double[]> outData = new ArrayList<>();
                for (int i = 1; i < figure.size() - 2; i++) {
                    outData.add(controller.computeRadius(figure.get(i)));
                }
                int lastIndex = outData.size() - 1;
                for (int i = figure.size() - 2; i < 15; i++){
                    outData.add(outData.get(lastIndex));
                }
                outData.add(controller.figureVolume(figure, (double) 1680 / figure.size()));
                return outData;
            }, executor).completeOnTimeout(new ArrayList<>(), 1, TimeUnit.SECONDS);
            continueCompute();
        }
    }

    private void continueCompute(){
        System.out.println("Enter to continue compute");
        double[] rads = new double[14];
        double[][] circleCentres = new double[14][2];

        mainFuture.whenComplete((res, ex) -> {
            System.out.println("Future complite!");
//            for (int i = 0; i < res.size(); i++) {
//                System.out.println(i + " :: " + Arrays.toString(res.get(i)));
//            }

            double averageX = 0, averageY = 0, averageR = 0;
                for (int i = 0; i < 14; i++) {
                    rads[i] = res.get(i)[0];
                    circleCentres[i][0] = res.get(i)[1];
                    circleCentres[i][1] = res.get(i)[2];
                }
                for (int n = 0; n < 2; n++) {
                    for (int i = 0; i < 14; i++) {
                        averageX += circleCentres[i][0];
                        averageY += circleCentres[i][1];
                        averageR += rads[i];
                    }
                    averageX = averageX / 14;
                    averageY = averageY / 14;
                    averageR = averageR / 14;
                    for (int i = 0; i < 14; i++) {
                        if (circleCentres[i][0] / averageX > 1.1 || circleCentres[i][0] / averageX < 0.9) {
                            circleCentres[i][0] = averageX;
                        }
                        if (circleCentres[i][1] / averageY > 1.1 || circleCentres[i][1] / averageY < 0.9) {
                            circleCentres[i][1] = averageY;
                        }
                        if (rads[i] / averageR > 1.15 || rads[i] / averageR < 0.85) {
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

//        futureResultList.add(CompletableFuture.supplyAsync(() ->
//                controller.figureVolume(figure, (double) 1680 / figure.size()))
//                .completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
////        futureResultList.add(CompletableFuture.supplyAsync(() ->
////                controller.usefulVolume(figure))
////                .completeOnTimeout(new double[3], 1, TimeUnit.SECONDS));
//
//        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureResultList.toArray(new CompletableFuture[0]));
//
//        CompletableFuture<List<double[]>> finalResults = combinedFuture
//                .thenApply(val ->
//                        futureResultList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
//
//        finalResults.thenAcceptAsync(res -> {
//            System.out.println("Futures done!");
//            System.out.println("Futures count: " + res.size());
//
//            double inputRad = (double) Math.round(res.get(0)[0] * 2.2 * 10) / 10,
//                    outputRad = (double) Math.round(res.get(res.size() - 3)[0] * 2.2 * 10) / 10,
                double inputRad = rads[0] * 2.3, outputRad = rads[13] * 2.3,
                    volume = (double) Math.round(res.get(res.size() - 1)[0] * 0.38 / 1000) / 1000;
////                    usefulVolume = (double) Math.round(res.get(res.size() - 1)[0] * 0.48 / 1000) / 1000,
////                    curvature = res.get(res.size() - 1)[1];
            System.out.println("Input Diameter: " + inputRad);
            System.out.println("Output Diameter: " + outputRad);
            System.out.println("Figure Volume: " + volume);
////            System.out.println("Usefull Volume: " + usefulVolume);
////            System.out.println("Curvature: " + curvature);

            futureThread.interrupt();
        });
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
