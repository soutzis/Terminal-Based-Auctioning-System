import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.UUID;

public abstract class Client implements Serializable {

    protected String uid;
    protected String name;
    protected String email;
    protected ServerInterface serverReference;

    /**
     * Default Client constructor
     */
    public Client(){}

    public Client(String name, String email){
        this.uid = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        try {
            serverReference = (ServerInterface)Naming.lookup("rmi://localhost:1099/AuctionSystem");
            System.out.println(serverReference.registerClient(this));
        }
        catch (NotBoundException e) {
            e.printStackTrace();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getUid() {

        return uid;
    }

    public String getName() {

        return name;
    }

    public String getEmail() {

        return email;
    }
}
