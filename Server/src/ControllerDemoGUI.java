import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.xml.soap.Text;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class ControllerDemoGUI {

    private int BOARDSIZE = 100;
    private String filename = "serverBulletinBoard";
    protected BulletinBoard bulletinBoard;

    @FXML TilePane tilePane;

    public void initialize(){

        String[] strings = new String[25];
        for (int i = 0; i<25; i++)
            strings[i]=String.valueOf(i);

        for (String s : strings){
            Label label = new Label(s, new Rectangle(10, 100));
            label.setPrefSize(100, 100);
            Font font = new Font("Arial", 30);
            label.setFont(font);
            label.setTextAlignment(TextAlignment.JUSTIFY);
            label.setWrapText(true);
            tilePane.getChildren().add(label);
        }

        File file = new File(filename);
        if(file.exists()) loadBoardFromFile();
        else createNewBoard();

        startServer();

        System.out.println("system is ready");
    }

    private void startServer(){
        try{
            // create on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            //TODO read in from file for more flexibility

            MethodsImplementationRMI methodsImplementationRMI = new MethodsImplementationRMI(bulletinBoard);

            // create a new service named SecureBulletinBoard
            registry.rebind("SecureBulletinBoard", methodsImplementationRMI);

        } catch(Exception e) { e.printStackTrace(); }
    }

    private void createNewBoard(){
        bulletinBoard = new BulletinBoard(BOARDSIZE);
    }

    protected void writeBoardToFile(){
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename))) {
            objectOutputStream.writeObject(bulletinBoard);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadBoardFromFile(){
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename))){
            bulletinBoard = (BulletinBoard) objectInputStream.readObject();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
