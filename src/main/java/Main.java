import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception {
        URL fxmlFile = new File("src/main/resources/fxml/sample.fxml").toURI().toURL();
        Parent root = FXMLLoader.load(fxmlFile);
        System.out.println(root);
        primaryStage.setTitle("Laser Scanner");
        primaryStage.setScene(new Scene(root, primaryStage.getWidth(), primaryStage.getHeight()));
        primaryStage.setResizable(false);
//        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
