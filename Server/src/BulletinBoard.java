import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class BulletinBoard implements Serializable{

    private int boardsize = 25;
    private String filename = "serverBulletinBoard";
    private BulletinBoardCell[] board;
    private Registry registry;

    @FXML private TilePane tilePane;

    public void initialize(){
        File file = new File(filename);
        if(file.exists()) loadBoardFromFile();
        else createNewBoard();

        updateBoard();

        startServer();

        System.out.println("system is ready");
    }

    private void startServer(){
        try{
            // create on port 1099
            registry = LocateRegistry.createRegistry(1099);

            MethodsImplementationRMI methodsImplementationRMI = new MethodsImplementationRMI(board);

            // create a new service named SecureBulletinBoard
            registry.rebind("SecureBulletinBoard", methodsImplementationRMI);

        } catch(Exception e) { e.printStackTrace(); }
    }

    protected void stopServer() {
        try {
            registry.unbind("SecureBulletinBoard");
            UnicastRemoteObject.unexportObject(registry,true);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private void createNewBoard(){
        board = new BulletinBoardCell[boardsize];
        for(int i = 0; i < boardsize; i++){
            board[i] = new BulletinBoardCell();
        }
    }

    public void updateBoard(){
        tilePane.getChildren().clear();
        for (int i = 0; i < boardsize; i++){
            Label label = new Label(String.valueOf(board[i].getMessageAmountCell()));
            label.setPrefSize(100, 100);
            label.setFont(new Font("Arial", 30));
            label.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(5))));
            tilePane.getChildren().add(label);
        }
    }

    protected void writeBoardToFile(){
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename))) {
            objectOutputStream.writeObject(board);
            System.out.println("Board saved to file");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadBoardFromFile(){
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename))){
            board = (BulletinBoardCell[]) objectInputStream.readObject();
            System.out.println("Board loaded from file");
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected int getSize(){
        return boardsize;
    }

}


