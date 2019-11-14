import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MethodsImplementationRMI extends UnicastRemoteObject implements MethodsRMI {

    //Constructor
    public MethodsImplementationRMI () throws RemoteException{}

    @Override
    public void add(int index, byte[] tag, byte[] value) throws RemoteException{
        //place the value on a specific index in the bulletin board, associated with a specific tag
        BulletinBoard.addToBoard(index, tag, value);
    }

    @Override
    public byte[] get(int index, byte[] tag) throws RemoteException, NoSuchAlgorithmException {
        //use hash on tag to get the hashed tag
        //Digest Message
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] SHAbyteArray = md.digest(tag);

        //IF there is a message on that index of the Bulletin board with that hashed tag
        //AND the message can be decrypted by Bob
        //THEN return the message
        //ELSE return NULL
        if (BulletinBoard.getFromBoard(index, tag) != null)
            return get(index, tag);
        else
            return null;

    }

}