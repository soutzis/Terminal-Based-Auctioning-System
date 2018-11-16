import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.regex.Pattern;

/**
 * Abstract class that represents Client. Any seller or buyer will inherit from this class.
 * @author Petros Soutzis
 */
public abstract class Client implements Serializable {

    private String uid, name, email;
    private boolean serverAlive = false;
    private ServerInterface serverReference;

    private final String MALFORMED_URL_ERROR = "Please check if the URL of the server is valid and try again.";
    private final String NOT_BOUND_ERROR = "It appears that the server name you provided can't be found." +
            "\nVerify that the server name is correct and try again.";
    private final String REMOTE_ERROR_TROUBLESHOOT = "Please make sure that your Client program's source code " +
            "was not altered since the server was booted-up.\nRestart the server and try again.";
    public static final String REMOTE_ERROR = "\nSomething went wrong when contacting the server!";
    protected final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";/*email validation*/
    protected final String NAME_REGEX = "^[a-zA-Z0-9]{5,30}$";/*name validation*/

    protected final boolean DEBUG = false;  /*For debugging purposes of classes that inherit from Client*/

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
        this.uid = null;
        this.name = name;
        this.email = email;
        this.serverAlive = true;
        try {
            serverReference = (ServerInterface)Naming.lookup("rmi://localhost/AuctionSystem");
            ServerRegistrationReply reply = serverReference.registerClient(this);
            System.out.println("Server >> "+reply.getMsg()+"\n<< Server");
            if(reply.isSuccessful()){
                this.uid = reply.getUid();
                System.out.println("*Your unique ID is: "+this.uid+"*");
            }
        }
        catch (NotBoundException nbe) {
            System.out.println(NOT_BOUND_ERROR);
            if(DEBUG){
                nbe.printStackTrace();
            }
        }
        catch (MalformedURLException murle) {
            System.out.println(MALFORMED_URL_ERROR);
            if(DEBUG)
                murle.printStackTrace();
        }
        /*If remote exception is thrown, set the serverAlive boolean to false,
        so that the client program wont enter the main loop, since it will be futile.*/
        catch (RemoteException re) {
            System.out.println(REMOTE_ERROR);
            System.out.println(REMOTE_ERROR_TROUBLESHOOT);
            this.serverAlive = false;
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
     * @return the serverInterface instance. Can't be tampered with from subclasses or classes that reference a Client()
     */
    public ServerInterface getServerReference() {

        return serverReference;
    }

    /**
     * Mutator method for serverAlive boolean. It can be used to terminate clients automatically.
     * @param serverAlive is what will change serverAlive to
     */
    public void setServerAlive(boolean serverAlive) {

        this.serverAlive = serverAlive;
    }

    /**
     * This method is used to confirm that the details that the user has entered are valid.
     * It allows 5 - 30 characters for a user name and only syntactically correct email addresses.
     * @param str The input that will have its syntax checked.
     * @param regex The regular expression for the input that is allowed.
     * @return True if user input is valid, False otherwise.
     */
    protected boolean detailsValidator(String str, String regex){
        if(str == null)
            return false;

        Pattern pattern = Pattern.compile(regex);

        return pattern.matcher(str).matches();
    }
}
