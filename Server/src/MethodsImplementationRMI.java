import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MethodsImplementationRMI extends UnicastRemoteObject implements MethodsRMI {

    //Constructor
    public MethodsImplementationRMI () throws RemoteException{}

    @Override
    public void add(int index, byte[] value, int tag) throws RemoteException{
        //TODO
    }

    @Override
    public byte[] get(int index, int tag) throws RemoteException{
        //TODO
    }

}