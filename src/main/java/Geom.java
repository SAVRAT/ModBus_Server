import java.util.Arrays;

class Geom {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    double[] geomCentre(double[][] dots){
        double[] mass = new double[2];
        //noinspection OptionalGetWithoutIsPresent
        mass[0] = (Arrays.stream(compute(dots, 0)).min().getAsDouble() +
                Arrays.stream(compute(dots, 0)).max().getAsDouble())/2;
        mass[1] = (Arrays.stream(compute(dots, 1)).min().getAsDouble() +
                Arrays.stream(compute(dots, 1)).max().getAsDouble())/2;
        return mass;
    }

    private double[] compute(double[][] input, int k){
        double[] mass = new double[input.length];
        for (int i=0; i<input.length; i++){
            mass[i] = input[i][k];
        }
        return mass;
    }

    void lineKoef(Formul[] mass, double[][] dots){
        for (int i=0; i<dots.length-1; i++){
            double k = 0;
            String type = "";
            double x2 = dots[i + 1][0];
            double y2 = dots[i + 1][1];

            if (dots[i][0] == x2){
                k = x2;
                type = "vert";
            }else if (dots[i][1] == y2){
                k = y2;
                type = "horiz";
            }else if (dots[i][0] > x2){
                k = (y2 - dots[i][1])/(x2 - dots[i][0]);
                type = "<-";
            }else if (dots[i][0] < x2){
                k = (y2 - dots[i][1])/(x2 - dots[i][0]);
                type = "->";
            }
            mass[i] = new Formul(k, type);
            //формула Y = k * (X - dots[i][0]) + dots[i][1]
        }
    }

    Formul lineKoef(double x1, double y1, double x2, double y2){
        double k = 0;
        String type = "";
        if (x1 == x2){
            k = x2;
            type = "vert";
        }else if (y1 == y2){
            k = y2;
            type = "horiz";
        }else if (x1 > x2){
            k = (y2 - y1)/(x2 - x1);
            type = "<-";
        }else if (x1 < x2){
            k = (y2 - y1)/(x2 - x1);
            type = "->";
        }
        //формула Y = m.formulData[i][0] * (X - m.data[i][0]) + m.data[i][1]
        return new Formul(k, type);
    }

    double distance(double x1, double y1, double x2, double y2){
        return Math.abs(Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));
    }
}
