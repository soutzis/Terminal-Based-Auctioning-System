import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Abstract class that represents Client. Any seller or buyer will inherit from this class.
 * @author Petros Soutzis
 */
public abstract class Client implements Serializable {

    public static final String REMOTE_ERROR = "\nSomething went wrong when contacting the server. Please try again!";

    private String uid;
    private String name;
    private String email;
    private boolean serverAlive = false;

    protected ServerInterface serverReference;
    protected final boolean DEBUG = false;  /*For debugging purposes of classes that inherit from Client*/

    private final String MALFORMEDURL_ERROR = "Please check if the URL of the server is valid and try again.";
    private final String NOT_BOUND_ERROR = "It appears that the server name you provided can't be found." +
            "\nVerify that the server name is correct and try again.";
    private final String REMOTE_ERROR_TROUBLESHOOT = "Please make sure that your Client program's source code " +
            "was not altered since the server was booted-up.\nRestart the server and try again.";

    /**
     * This is the default constructor for a Client-application. It needs to be called first,
     * before to initialize either a buyer or seller type client.
     * It is used to create an initial instance of a Client. That initial instance
     * can then be used to create a "real" instance of a Seller/Buyer, by asking the user to provide
     * 'name' & 'email' fields, at run-time. A Universally Unique Identified (UUID)
     *  will also be automatically generated when the private-access constructor is called.
     */
    protected Client(){}

    /**
     * Constructor of Client that will create a reference to the remote server and
     * also assign a random unique ID to itself.
     * @param name The name that the Client will have
     * @param email The email of the Client.
     */
    protected Client(String name, String email){
        this.uid = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        try {
            serverReference = (ServerInterface)Naming.lookup("rmi://localhost/AuctionSystem");
            serverAlive = true;
            System.out.println("\n--->"+serverReference.registerClient(this)+"<---");
        }
        catch (NotBoundException nbe) {
            System.out.println(NOT_BOUND_ERROR);
            if(DEBUG){
                nbe.printStackTrace();
            }
        }
        catch (MalformedURLException murle) {
            System.out.println(MALFORMEDURL_ERROR);
            if(DEBUG)
                murle.printStackTrace();
        }
        catch (RemoteException re) {
            System.out.println(REMOTE_ERROR);
            System.out.println(REMOTE_ERROR_TROUBLESHOOT);
            serverAlive = false;
            if(DEBUG)
                re.printStackTrace();
        }
    }

    /**
     * Abstract method that will be used to ask for user input at runtime
     * @return a new instantiated Client
     */
    public abstract Client createClient();

    /**
     * @return the client's uid
     */
    public String getUid() {

        return uid;
    }

    /**
     * @return the client's name
     */
    public String getName() {

        return name;
    }

    /**
     * @return the clients email
     */
    public String getEmail() {

        return email;
    }

    /**
     * @return true if client has connected to the server
     */
    public boolean isServerAlive() {

        return serverAlive;
    }

    /**
     * This method is used to confirm that an email used for a client instance is valid.
     * @param email The email that will have its syntax checked
     * @return True if the email is valid, False otherwise.
     */
    protected boolean emailValidator(String email){
        if(email == null)
            return false;

        String validEmailRegEx = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(validEmailRegEx);

        return pattern.matcher(email).matches();
    }
}
