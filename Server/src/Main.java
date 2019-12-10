import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DemoGUI.fxml"));
        Parent root = loader.load();
        ControllerDemoGUI controller = loader.getController();
        primaryStage.setTitle("Server Demo GUI");
        primaryStage.setScene(new Scene(root, 1200, 600));
        primaryStage.show();
        primaryStage.setOnHidden(e -> {
            controller.writeBoardToFile();
            Platform.exit();
        });
    }

    public static void main(String[] args) {

        launch(args);

    }
}
