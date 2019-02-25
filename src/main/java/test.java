import java.util.ArrayList;

public class test {
    private DataBaseConnect dataBase;
    test(DataBaseConnect dataBase){
        this.dataBase = dataBase;
        ArrayList<String[]> oven_AI =  dataBase.getOven_AI();
    }



}
