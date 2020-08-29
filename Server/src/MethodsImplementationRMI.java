//import javafx.fxml.FXML;
//import javafx.scene.control.TextArea;

import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MethodsImplementationRMI extends UnicastRemoteObject implements MethodsRMI {
    BulletinBoardCell[] bulletinBoard;
    int boardSize;
    BulletinBoard board;

    //@FXML
    //private TextArea statusServer;

    public MethodsImplementationRMI(BulletinBoardCell[] bulletinBoard, int boardSize, BulletinBoard board) throws RemoteException {
        this.bulletinBoard = bulletinBoard;
        this.boardSize = boardSize;
        this.board = board;
    }

    @Override
    public void add(int index, byte[] tag, byte[] value) throws RemoteException{
        //place the value on a specific index in the bulletin board, associated with a specific tag
        bulletinBoard[index].addToCell(tag, value);
        System.out.println("Added to index " + index + " and tag " + tag);
        Platform.runLater(
                () -> {
                    board.updateBoard();
                    board.getMessagecount().setText("Messages in board: " + String.valueOf(board.getCountMessagesBoard()));
                }
        );
        board.getStatusServer().appendText("Added to index " + index + "\n");
    }

    @Override
    public byte[] get(int index, byte[] tag) throws RemoteException, NoSuchAlgorithmException {
        System.out.println("Called get for index " + index + " and tag " + tag);

        //use hash on tag to get the hashed tag
        //Digest Message
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedTag = md.digest(tag);


        //IF there is a message on that index of the Bulletin board with that hashed tag
        //AND the message can be decrypted by Bob
        //THEN return the message
        //ELSE return NULL

        //Get value from board (NULL if board is empty)
        byte[] value = bulletinBoard[index].getFromCell(hashedTag);
        System.out.println("Returned " + value);
        if(value!= null){
            Platform.runLater(
                    () -> {
                        board.updateBoard();
                        board.getMessagecount().setText("Messages in board: " + String.valueOf(board.getCountMessagesBoard()));
                    }
            );
            board.getStatusServer().appendText("Called get for index " + index + "\n");
            board.getStatusServer().appendText("Returned from index " + index + "\n");
        }
        return value;

    }

    @Override
    public boolean changeServer() throws RemoteException{
        int teller = board.getCountMessagesBoard();
        if(teller > 2){
            System.out.println("true");
            board.getStatusServer().appendText("Server overloaded, some clients have to change server");
            return true;
        }else{
            System.out.println("false");
            return false;
        }
    }

}