import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application{

    private BulletinBoard controller;
    private boolean runUpdater = true;


    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DemoGUI.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        primaryStage.setTitle("Server Demo GUI");
        primaryStage.setScene(new Scene(root, 500, 700));
        primaryStage.show();
    }

    @Override
    public void stop(){
        controller.stopServer();
        controller.writeBoardToFile();
        System.exit(0);
    }

    public static void main(String[] args) {

        launch(args);

    }
}
