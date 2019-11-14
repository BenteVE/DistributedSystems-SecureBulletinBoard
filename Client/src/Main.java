import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.crypto.*;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

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

    public static void send(MethodsRMI impl, String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, NotBoundException {
        //generate new index for next message in bulletin board (depends on size of array bulletin board)
        SecureRandom random = new SecureRandom();
        int nextIndex = random.nextInt(128);

        //generate new tag to protect next message from malicious deleting
        byte[] nextTag = new byte[8];
        random.nextBytes(nextTag);

        //create Message Object with index, tag and message string
        Message message = new Message(nextIndex, nextTag, value);

        //Get symmetric key for communication partner
        //TODO: get current symmetric key

        //Create and initialise cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, SymmetricKey);

        //Encrypt object with cipher
        byte[] cipherMessage = cipher.doFinal(message.getBytes());

        //TODO: read index and tag from file for certain communication partner
        int index = ;
        int tag = ;

        //hash the tag to Bulletin Board
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedTag = md.digest(tag);

        //use function add (implemented by server) to place encrypted package on specific index, associated with hashed tag
        impl.add(index, hashedTag, cipherMessage);

        //replace the stored old index and tag with the new index and tag
        //TODO: write nextIndex and nextTag to file for certain communication partner

        //use a key deriviation function to generate a new symmetric key from the old key
        //TODO: implement key derivation function

        //TODO: write derived key to file for communication partnet

    }

    public static String receive(MethodsRMI impl) throws RemoteException, NoSuchPaddingException, NoSuchAlgorithmException {
        //TODO: implement function to receive messages for bulletin board
        //get current index and value from file
        int index = ;
        byte[] tag = ;

        //use function get (implemented by server) to check a specific index and tag in the bulletin board
        //(use get function with regular tag, server hashes tag to match hashed tag associated with value in Bulletin Board)
        byte[] encryptedByteArray = impl.get(index, tag);

        //IF there is a message on that index of the Bulletin board with that specific tag (message will be returned by get function)
        if (encryptedByteArray != null){

            //Get symmetric key for communication partner
            //TODO: get current symmetric key

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, SymmetricKey);

            //Decrypt the bytearray
            byte[] decryptedByteArray = cipher.doFinal(encryptedByteArray);

            //Convert decrypted bytearray into Message object
            Message message = Message.getInstance(decryptedByteArray);

            //IF the message is successfully decrypted by Bob
            //TODO: replace the current index and tag by the new index and tag that were piggybacked on the message

            //TODO: use a key deriviation function to generate a new symmetric key from the old key

            //return the message
            return ;
        }
        //Else return null => currently no message in board
        else
            return null;

    }
}
