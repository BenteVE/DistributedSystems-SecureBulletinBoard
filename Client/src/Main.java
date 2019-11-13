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

    public  static void bump(){
        // TODO: generate initial key, index and tag for communication (once for A to B and once for B to A)
        // => write to file for later use?
    }

    public static void send(String message){
        //TODO: implement function to send messages to bulletin board
        //generate new index for next message in bulletin board (depends on size of array bulletin board)
        //generate new tag to protect next message from malicious deleting
        //encrypt message, new index and new tag with the symmetric key
        //use function add (implemented by server) to place encrypted package on specific index, associated with hashed tag (!NOT the new tag)
        //replace the stored old index and tag with the new index and tag
        //use a key deriviation function to generate a new symmetric key from the old key
    }

    public static String receive(){
        //TODO: implement function to receive messages for bulletin board
        //use function get (implemented by server) to check a specific index and tag in the bulletin board
        //(use get function with regular tag, server hashes tag)
        //IF there is a message on that index of the Bulletin board with that specific tag (message will be returned by get function)
        //AND the message can be decrypted by Bob
        //THEN:
        // - replace the current index and tag by the new index and tag that were piggybacked on the message
        // - use a key deriviation function to generate a new symmetric key from the old key
        // - return the message
        //ELSE: return NULL
    }
}
