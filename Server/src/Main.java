import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {

        //TODO: Create bulletin board
        //each cell in the array contains a set of value tag pairs => implement with map?

        //TODO: GUI for board for presentation (show that multiple messages can be in same place in array, ...)

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
