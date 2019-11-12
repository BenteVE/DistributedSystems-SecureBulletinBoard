import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }


    public static void main(String[] args) {

        launch(args);

        try{
            // fire to localhost port 1099
            Registry myRegistry= LocateRegistry.getRegistry("localhost", 1099);
            //TODO read in from file for more flexibility

            // search for SecureBulletinBoard
            MethodsRMI implementation = (MethodsRMI) myRegistry.lookup("SecureBulletinBoard");

            //call methods implemented by server with: impl.method();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
