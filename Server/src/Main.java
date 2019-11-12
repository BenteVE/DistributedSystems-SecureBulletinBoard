import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {

        try{
            // create on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            //TODO read in from file for more flexibility

            // create a new service named SecureBulletinBoard
            registry.rebind("SecureBulletinBoard", new MethodsImplementationRMI());

        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("system is ready");
    }
}
