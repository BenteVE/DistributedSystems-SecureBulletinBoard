import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {

        //TODO: GUI for board for presentation (show that multiple messages can be in same place in array, ...)

        //TODO: read and write board from file

        try{
            // create on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            //TODO read in from file for more flexibility

            MethodsImplementationRMI methodsImplementationRMI = new MethodsImplementationRMI();

            // create a new service named SecureBulletinBoard
            registry.rebind("SecureBulletinBoard", methodsImplementationRMI);

        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("system is ready");
    }
}
