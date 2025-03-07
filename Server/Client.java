package Server;

import javax.crypto.*;
import java.io.*;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
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
    protected final String NAME_REGEX = "^[a-zA-Z0-9_]{5,30}$";/*name validation*/

    protected final boolean DEBUG = true;  /*For debugging purposes of classes that inherit from Client*/

    private PublicKey publicKey;

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
     * Create a client for the first time
     * Constructor of Client that will create a reference to the remote server and
     * also assign a random unique ID to itself. This Constructor is used to Register a new client with the server
     * @param name The name that the Client will have
     * @param email The email of the Client.
     */
    protected Client(String name, String email){
        this.uid = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;


        try {
            KeyPair pair = SecurityManager.generateNewKeyPair();
            publicKey = pair.getPublic();
            SecurityManager.writeKeyToDisk(pair.getPrivate(), email.toLowerCase()+"_privateKey.key");
            SecurityManager.writeKeyToDisk(publicKey, email.toLowerCase()+"_publicKey.pub");

            serverReference = (ServerInterface)Naming.lookup("rmi://localhost/AuctionSystem");
            ServerRegistrationReply reply = serverReference.registerClient(this);

            if(reply == null){
                System.out.println("\n>>Server appears to be down. Could not get a reply.");
                this.serverAlive = false;
            }

            else if(reply.isSuccessful()){
                this.serverAlive = true;

                System.out.println("\nServer >>> "+reply.getMsg()+"\n>>> Server");
                System.out.println("*Your unique ID is: "+this.uid+"*");
            }
            else if(!reply.isSuccessful()){
                System.out.println(reply.getMsg()+"\n");
                this.serverAlive = true;
                this.uid = null;
            }
        }
        catch (NotBoundException nbe) {
            System.out.println(NOT_BOUND_ERROR);
            if(DEBUG){
                nbe.printStackTrace();
            }
        }
        catch (MalformedURLException mue) {
            System.out.println(MALFORMED_URL_ERROR);
            if(DEBUG)
                mue.printStackTrace();
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
        catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("Could not write key to disk");
        }
    }

    public PublicKey getPublicKey() {

        return publicKey;
    }

    protected Client authenticateClient(String email, String password){

        return null;
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
     * @return the client's email
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

    public String emailForAuthentication(){
        Scanner scanner = new Scanner(System.in);

        System.out.println("The private key generated when you first registered will be used.");
        System.out.println("Please enter the email address you provided when you registered.");
        System.out.print("Email: ");
        String email = scanner.nextLine();
        try{
            //if provided email address does not conform to an email's syntax, then spin
            if(!detailsValidator(email, EMAIL_REGEX)){
                System.out.println("Email provided is not a valid email address");
                return null;
            }

        }
        //Tell the user that they did wrong.
        catch(InputMismatchException ime){
            System.out.println("That is not a valid input");
            if(DEBUG)
                System.out.println(ime.getMessage());

            return emailForAuthentication();
        }

        return email;
    }

    public Client challengeResponseAuthentication(String email)
            throws IOException, ClassNotFoundException, NoSuchPaddingException,
            NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException,
            InvalidKeySpecException, SignatureException {

        /*Create a reference to the server stub*/
        ServerInterface localServerReference = null;
        try{
            localServerReference = (ServerInterface)Naming.lookup("rmi://localhost/AuctionSystem");
        }
        catch (NotBoundException e) {
            e.printStackTrace();
        }

        //Load private key from disk. We assume that client stores key to disk prior to logging on server
        PrivateKey privateKey = SecurityManager.readPrivateKey(email.toLowerCase()+"_privateKey.key");

        //Prepare authentication request to send to server. This will verify server identity.
        AuthenticationRequest request = new AuthenticationRequest(email, new Random().nextInt());
        byte[] signature = SecurityManager.generateDigitalSignature(privateKey,request);
        PublicKey serverKey = localServerReference.getServerPubKey();
        Cipher encryptRequest = Cipher.getInstance(serverKey.getAlgorithm());
        encryptRequest.init(Cipher.PUBLIC_KEY, serverKey);
        //Seal AuthenticationRequest with server's public key
        SealedObject challengeReq = new SealedObject( request,encryptRequest );

        //Prepare decryption cipher using own private key
        Cipher decryptReply = Cipher.getInstance(privateKey.getAlgorithm());
        decryptReply.init(Cipher.PRIVATE_KEY, privateKey);
        AuthenticationReply reply;
        try{
            //First send request, then decrypt server's reply. If this fails, return null and display a message
           reply =(AuthenticationReply)localServerReference
                   .authenticateServer(challengeReq, signature).getObject(decryptReply);
        }
        catch(NullPointerException npe){
            return null;
        }
        catch (BadPaddingException bpe) {
            System.out.println(bpe.getMessage());
            return null;
        }

        /*If the number from decrypted reply is the same as the one sent initially to server,
            then proceed with self authentication, by sending yet another encrypted !request! with solved answer*/
        if(request.getNumber() == reply.getSolvedChallengeNumber()){
            AuthenticationRequest request2 = new AuthenticationRequest(email, reply.getNewChallengeNumber());
            SealedObject solvedChallenge = new SealedObject(request2, encryptRequest);
            try{
                return localServerReference.authenticateClient(solvedChallenge);
            }
            catch (RemoteException re){
                System.out.println("Could not complete authentication");
                return null;
            }
        }
        //If response received does not match challenge sent, then return null
        else
            return null;
    }
}
