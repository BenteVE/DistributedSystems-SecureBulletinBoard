import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MethodsImplementationRMI extends UnicastRemoteObject implements MethodsRMI {

    //Constructor
    public MethodsImplementationRMI () throws RemoteException{}

    @Override
    public void add(int index, byte[] value, int tag) throws RemoteException{
        //TODO
        //place the value on a specific index in the bulletin board, associated with a specific tag
    }

    @Override
    public byte[] get(int index, int tag) throws RemoteException{
        //TODO
        //use hash on tag to get the hashed tag
        //IF there is a message on that index of the Bulletin board with that hashed tag
        //AND the message can be decrypted by Bob
        //THEN return the message
        //ELSE return NULL
    }

}