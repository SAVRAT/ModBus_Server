// класс дял хранения коэффициента и наклона прямых
class Formul {
    private double koef;
    private String type;

    Formul(double koef, String type){
        this.koef = koef;
        this.type = type;
    }
    double getKoef(){
        return koef;
    }
    String getType(){
        return type;
    }
}
