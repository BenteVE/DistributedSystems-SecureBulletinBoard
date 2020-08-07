import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MethodsRMI extends Remote { //extends Remote to avoid local application

    public void add(int index, byte[] tag, byte[] value) throws RemoteException;

    public byte[] get(int index, byte[] tag) throws RemoteException;

    public boolean changeServer() throws RemoteException;


    }