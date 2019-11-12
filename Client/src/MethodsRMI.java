import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MethodsRMI extends Remote { //extends Remote to avoid local application

    public void add(int index, byte[] value, int tag) throws RemoteException;

    public byte[] get(int index, int tag) throws RemoteException;

}