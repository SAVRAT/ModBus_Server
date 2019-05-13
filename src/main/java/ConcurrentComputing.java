import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;

class ConcurrentComputing {

    private DataBaseConnect dataBaseConnect;            // экземпляр класса работы с БД
    private Controller controller = new Controller();   // экземпляр класса для вычислений

    private ArrayList<double[][]> figure;               // Лист для последующего хранения бревна
    private ArrayList<double[]> res = new ArrayList<>();    // Лист для результатов вычислений

    ConcurrentComputing(ArrayList<double[][]> figure, DataBaseConnect dataBaseConnect) {
        this.figure = new ArrayList<>(figure); // копируем входной лист с координатами бревна
        this.dataBaseConnect = dataBaseConnect; // копируем ссылку на экземпляр БД
        ScannerVerticle.figure.clear(); // очищаем лист в классе ScannerVerticle (класс чтения бревна)
    }
    // метод для вычисления 14 вписанных окружностей для бревна
    // результаты всех вычислений в лист res
    void computeAsync(){
        System.out.println("Figure size: " + figure.size());
        if (figure.size() >= 16) {
            System.out.println("Figure size >= 16");
            for (int i = 1; i < 14; i++) {                          // откидываем 1 срез
                res.add(controller.computeRadius(figure.get(i)));   // если >= 16, считаем первые 13 и последний
            }
            res.add(controller.computeRadius(figure.get(figure.size() - 2)));
        continueCompute();  // запускаем обработку данных
        }else if (figure.size() >= 5){
            System.out.println("Figure size >= 5");
            for (int i = 1; i <= figure.size() - 2; i++) {          // откидываем первый срез
                res.add(controller.computeRadius(figure.get(i))); // если >= 5, считааем со второго и до
            }                                                       // предпоследнего
            int lastIndex = res.size() - 1;
            for (int i = figure.size() - 2; i < 15; i++){           // копируем оставшиеся до 14 из имеющихся
                res.add(res.get(lastIndex));
            }
            continueCompute();  // запускаем обработку данных
        }
    }
    // метод усреднения координат окружностей и радиусов
    // запись данных в базу
    private void continueCompute(){
        double[] rads = new double[14];                 // лист радиусов
        double[][] circleCentres = new double[14][2];   // лист координат центров
        int zeroCount = 0; // для подсчёта количества нулевых значений
        double averageX = 0, averageY = 0, averageR = 0; // для хранения средних величин

        // инициализация листов значений
        for (int i = 0; i < 14; i++) {
            rads[i] = Math.abs(res.get(i)[0]);
            circleCentres[i][0] = Math.abs(res.get(i)[1]);
            circleCentres[i][1] = Math.abs(res.get(i)[2]);
        }
        // цикл: 2 раза усредняем координаты и радиусы окружностей
        for (int n = 0; n < 2; n++) {
            // нахождение сумм, исключая нули
            for (int i = 0; i < 14; i++) {
                if (circleCentres[i][0] != 0)
                    averageX += circleCentres[i][0];
                if (circleCentres[i][1] != 0)
                    averageY += circleCentres[i][1];
                if (rads[i] != 0)
                    averageR += rads[i];
                else
                    zeroCount++; // если был 0 -> счётчик +1
            }
            // подсчёт средних значений
            averageX = averageX / 14 - zeroCount;
            averageY = averageY / 14 - zeroCount;
            averageR = averageR / 14 - zeroCount;
            // обход по листам данных (координаты и радиусы окружностей), усреднение значений
            for (int i = 0; i < 14; i++) {
                // если отклоняется больше, чем на 10% приравниваем к среднему
                if (circleCentres[i][0] / averageX > 1.1 || circleCentres[i][0] / averageX < 0.9 ||
                circleCentres[i][0] == 0) {
                    circleCentres[i][0] = averageX;
                }
                if (circleCentres[i][1] / averageY > 1.1 || circleCentres[i][1] / averageY < 0.9 ||
                circleCentres[i][1] == 0) {
                    circleCentres[i][1] = averageY;
                }
                // если отклоняется больше, чем на 15% приравниваем к среднему
                if (rads[i] / averageR > 1.15 || rads[i] / averageR < 0.85 || rads[i] == 0) {
                    rads[i] = averageR;
                }
            }
            // сбрасываем средние велечины
            averageX = 0;
            averageY = 0;
            averageR = 0;
        }
        // повторно считаем общие суммы, но только для координат
        for (int i = 0; i < 14; i++) {
            averageX += circleCentres[i][0];
            averageY += circleCentres[i][1];
        }
        // считаем среднее
        averageX = averageX / 14;
        averageY = averageY / 14;
        // усредняем
        for (int i = 0; i < 14; i++) {
            if (circleCentres[i][0] / averageX > 1.03 || circleCentres[i][0] / averageX < 0.97) {
                circleCentres[i][0] = averageX;
            }
            if (circleCentres[i][1] / averageY > 1.03 || circleCentres[i][1] / averageY < 0.97) {
                circleCentres[i][1] = averageY;
            }
        }
        System.out.println("==================================================");
        // вывод данных о бревне
        for (int i = 0; i < 14; i++)
            System.out.println(i+1 + " :: " + rads[i] + "  " + circleCentres[i][0] + "  " + circleCentres[i][1]);
        // запись параметров в базу
        dataBaseConnect.mySQLClient.getConnection(con -> {
            if (con.succeeded()) {
                SQLConnection connection = con.result();
                // запрос на очистку таблицы от старых значений
                connection.query("TRUNCATE woodData_3;", result -> {
                    connection.close();
                    if (result.succeeded())
                        // пишем новые, предварительно округляя
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
        // входной/выходной диаметры, объёмы, кривизна
        double[] usefulVolumeAndCurvature = controller.usefulVolumeAndCurvature(rads, circleCentres);
        double inputRad = rads[0] * 2.3, outputRad = rads[13] * 2.3,
            volume = (double) Math.round(controller.figureVolume(rads) * 0.38 / 1000) / 1000,
            usefulVolume = (double) Math.round(usefulVolumeAndCurvature[0] * 0.48 / 1000) / 1000,
            curvature = usefulVolumeAndCurvature[1];
        // небольшая проверка на значение полезного объёма
        if (usefulVolume > volume)
            usefulVolume = volume * 0.95;
        System.out.println("Input Diameter: " + inputRad);
        System.out.println("Output Diameter: " + outputRad);
        System.out.println("Figure Volume: " + volume);
        System.out.println("Useful Volume: " + usefulVolume);
        System.out.println("Curvature: " + curvature);
        // запись параметров в базу
        woodParamsToDatabase(inputRad, outputRad, volume, usefulVolume, curvature);
    }
    // метод для записи параметров бревна (входной/выходной диаметры, объёмы, кривизна) в базу
    private void woodParamsToDatabase(double inputRad, double outputRad,
                                      double volume, double usefulVolume, double curvature){
        // вычисление среднего радиуса
        double avgRad = (inputRad + outputRad) / 2;
        JsonArray dataArray = new JsonArray().add(curvature).add(inputRad).add(outputRad)
                .add(avgRad).add(volume).add(usefulVolume);
        // делаем запрос в базу
        dataBaseConnect.databaseWrite("INSERT INTO woodParams (curvature, inputRad," +
                " outputRad, avrRad, volume, usefulVolume, timeStamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP());", dataArray);
    }
}
