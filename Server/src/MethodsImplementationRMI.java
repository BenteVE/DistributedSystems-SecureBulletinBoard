import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MethodsImplementationRMI extends UnicastRemoteObject implements MethodsRMI {

    BulletinBoard bulletinBoard;

    public MethodsImplementationRMI(BulletinBoard bulletinBoard) throws RemoteException {
        this.bulletinBoard = bulletinBoard;
    }

    @Override
    public void add(int index, byte[] tag, byte[] value) throws RemoteException{
        //place the value on a specific index in the bulletin board, associated with a specific tag
        bulletinBoard.addToBoard(index, tag, value);
        System.out.println("Added to index " + index + " and tag " + tag);
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
        byte[] value = bulletinBoard.getFromBoard(index, hashedTag);
        System.out.println("Returned " + value);
        return value;

    }

}