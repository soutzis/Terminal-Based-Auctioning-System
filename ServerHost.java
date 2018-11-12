import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerHost {

    /*Constants that the server will use, to fire-up the rmi registry service.
    This includes the host ip address/hostname, server name and port number.
    Port number used is the default for rmiregistry service, which is port '1099'*/
    private final String RMI_HOST_ADDRESS = "rmi://127.0.0.1/";
    private final String SERVER_NAME = "AuctionSystem";
    private final int PORT_NUMBER = Registry.REGISTRY_PORT;

    public ServerHost(){

    try {
        ServerInterface auctionSystem = new ServerLogic();
        LocateRegistry.createRegistry(PORT_NUMBER);
        Naming.rebind(RMI_HOST_ADDRESS + SERVER_NAME, auctionSystem);
    }
    catch (RemoteException e) {
        e.printStackTrace();
    }
    catch (MalformedURLException e) {
        e.printStackTrace();
    }

    }

    public static void main(String args[]) {
        //Run the server
        new ServerHost();
    }
}
