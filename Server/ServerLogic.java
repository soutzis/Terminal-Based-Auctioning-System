package Server;

import javax.crypto.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.RspList;

/**
 * This class is the Server's implementation (logic). The client need not know about how methods in this class work.
 * @author Petros Soutzis
 */
class ServerLogic extends UnicastRemoteObject implements ServerInterface {
    //server's public key
    private PublicKey publicKey;
    //JGroups
    private static final String CLUSTER_NAME = "ServerCluster";
    private static final int TIMEOUT = 5000;
    private JChannel channel;
    private RpcDispatcher dispatcher;
    private RequestOptions requestOptions;

    private static final Logger LOGGER = Logger.getLogger( ServerLogic.class.getName() );

    /**
     * Package-private constructor of the server implementation.
     * It should be used only by the class representing the host-machine for the server.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur during the execution
     * of a remote method call.
     */
    ServerLogic() throws  RemoteException{
        super();
        final String PUBLIC_KEY_FNAME = "server-public-key.key";
        //read the server's public key, so that clients can access it.
        try {
            this.publicKey = SecurityManager.readPublicKey(PUBLIC_KEY_FNAME);
        }
        catch (InvalidKeySpecException ike) {
            System.out.println("This key is not valid. Stack contents:\n");
            ike.printStackTrace();
        }
        catch (NoSuchAlgorithmException nsae) {
            System.out.println("The algorithm used does not exist. Stack contents:\n");
            nsae.printStackTrace();
        }
        catch (IOException ioe) {
            System.out.println("IO Exception has occurred. Error message:\n"+ioe.getMessage());
        }

        //Setup the cluster that server instances will communicate in.
        setupCluster();
    }

    /**
     * This method initializes the cluster, so that the front end can communicate with ServerReplica() type instances.
     */
    private void setupCluster() {
        //JGroups communication. Building block -> RPC dispatcher to call remote methods.
        //This architecture allows front-end to act as both a sender and receiver.
        try {
            this.channel = new JChannel();
            this.requestOptions = new RequestOptions(ResponseMode.GET_ALL, TIMEOUT);
            this.dispatcher = new RpcDispatcher(this.channel, new ServerReplica());
            this.channel.setDiscardOwnMessages(true);
            this.channel.connect(CLUSTER_NAME);
        }
        catch(Exception e) {
            System.out.println("[SERVER] Failed to connect to cluster");
        }
    }

    /**
     * This method is called in every front-end server method, to ensure that if a replica in the current 'View'
     * has crashed, a new instance is started in its place.
     * The crashed instance address is logged with a SEVERE level.
     * @param suspectedMembers the members that have not sent a heartbeat in logical time-frame
     */
    private synchronized void handleCrash(List suspectedMembers){
        //Get View and check if member is still in view, for each suspected member.
        //If member is still in view, then call method disconnect(), which will gracefully terminate it.
        //If replica has crashed and is already disconnected from view, then start new instance anyway.
        View v = this.channel.getView();
        for (Object o : suspectedMembers){
            Address address = (Address)o;
            if(v.containsMember(address)){
                try {
                    dispatcher.callRemoteMethod(address, "disconnect",
                            new Object[]{}, new Class[]{}, requestOptions);
                }
                catch (Exception e){
                    System.out.println("Error while sending command to terminate replica instance.");
                }
            }
            try{
                //LOG FAILURE AND START NEW INSTANCE
                new ServerReplica().start();
                LOGGER.log(Level.SEVERE, "Server instance "+address
                        +" was suspected and automatically terminated. Terminated instance has been replaced");
            }
            catch (Exception e){
                System.out.println("Could not start new replica instance.");
            }

        }
    }

    /**
     * @return The server's public key
     */
    @Override
    public PublicKey getServerPubKey() {

        return publicKey;
    }

    /**
     * This method will add a client to the server's "in-memory database", so the client can invoke server's methods.
     * @param client The client object that wants to register with the server
     * @return A message indicating if the client was successfully registered.
     */
    @Override
    public synchronized ServerRegistrationReply registerClient(Client client){
        try{
            RspList responses = this.dispatcher.callRemoteMethods(null,
                    "register",
                    new Object[]{client},
                    new Class[]{Client.class},
                    this.requestOptions);
            //If non-null replies are more than responses, handle crashed instances.
            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0){
                ServerRegistrationReply reply = new ServerRegistrationReply();
                reply.setMsg("Something went wrong with the server. Please try again");
                reply.setSuccess(false);
            }
            return (ServerRegistrationReply)responses.getResults().get(0);

        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    @Override
    public synchronized SealedObject authenticateServer(SealedObject challenge, byte[] signature) {
        int rand = new Random().nextInt();
        try{
            RspList responses = this.dispatcher.callRemoteMethods(null,
                    "authenticationStep1",
                    new Object[]{challenge, signature, rand},
                    new Class[]{SealedObject.class, byte[].class, int.class},
                    this.requestOptions);

            //If non-null replies are more than responses, handle crashed instances.
            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            return (SealedObject)responses.getResults().get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    @Override
    public synchronized Client authenticateClient(SealedObject solvedChallenge) {
        try{
            RspList responses = this.dispatcher.callRemoteMethods(null,
                    "authenticationStep2",
                    new Object[]{solvedChallenge},
                    new Class[]{SealedObject.class},
                    this.requestOptions);

            //If non-null replies are more than responses, handle crashed instances.
            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            return (Client)responses.getResults().get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }


    /**
     * This will create an auction and store it in the HashMap containing al the server's auctions.
     * @param startPrice The price that an auction will start at, for an item.
     * @param minPriceAccepted The minimum price that an item can be sold at.
     * @param description The description of the item for sale.
     * @param seller The seller-object that initiated the auction. Its ID is used to indicate who owns this auction.
     * @return The ID of the newly initialised Auction.
     */
    @Override
    public synchronized String initAuction(BigDecimal startPrice, BigDecimal minPriceAccepted,
                              String description, Client seller) {
        String auctionId = UUID.randomUUID().toString();
        try{
            RspList responses = this.dispatcher.callRemoteMethods(null,
                    "startAuction",
                    new Object[]{auctionId, startPrice,minPriceAccepted,description,seller},
                    new Class[]{String.class, BigDecimal.class, BigDecimal.class, String.class, Client.class},
                    this.requestOptions);

            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            return (String)responses.getResults().get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    /**
     * Check, if the Client that attempts to close the auction with the specified ID, is its owner.
     * It will also check if closing the auction is a valid operation. If yes, it will return a message that either
     * is error-related, or it indicates that the reserve price was not reached, or who the winner is and their details.
     * Method is thread safe. It uses the synchronized keyword to ensure that
     * @param auctionID The ID of the auction to close.
     * @param seller The seller that wants to close the auction.
     * @return Message indicating success of closing the auction with details of the winner or with indication that the
     * reserve price was not reached. If operation was not successful, it will return a message explaining the reason.
     */
    @Override
    public synchronized String closeAuction(String auctionID, Client seller){
        try{
            RspList responses = this.dispatcher.callRemoteMethods(null,
                    "stopAuction",
                    new Object[]{auctionID,seller},
                    new Class[]{String.class, Client.class},
                    this.requestOptions);

            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            return (String)responses.getResults().get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    /**
     * Allows a user to place a bid.
     * @param auctionId The id of the auction to bid at.
     * @param bidAmount The amount of money that the buyer wants to place as a bid.
     * @param buyer The Client object that called this method, to bid at an auction.
     * @return A message that indicates if the bid was either successful or not (including reason for rejected bid).
     */
    @Override
    public synchronized String bid(String auctionId, BigDecimal bidAmount, Client buyer){
        try{
            RspList responses = this.dispatcher.callRemoteMethods(
                    null,
                    "bid",
                    new Object[]{auctionId,bidAmount,buyer},
                    new Class[]{String.class, BigDecimal.class, Client.class},
                    this.requestOptions);

            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            return (String)responses.getResults().get(0);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    /**
     * @return a List of all active auctions on server.
     */
    @Override
    public List<Auction> browseActiveAuctions() {
        try{
            RspList responses = this.dispatcher.callRemoteMethods(
                    null,
                    "browseActiveAuctions",
                    new Object[]{},
                    new Class[]{},
                    this.requestOptions);

            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            @SuppressWarnings("unchecked")
            List<Auction> reply = (List<Auction>) responses.getFirst();

            return reply;
        }

        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

    /**
     * @param sellerId The id of the seller to view auctions of.
     * @return a List of all the active auctions on server, owned by the Client with id equal to the one specified.
     */
    @Override
    public List<Auction> browseAuctionsOfSeller(String sellerId) {
        try{
            RspList responses = this.dispatcher.callRemoteMethods(
                    null,
                    "browseAuctionsOfSeller",
                    new Object[]{sellerId},
                    new Class[]{String.class},
                    this.requestOptions);

            if(responses.getResults().size() < responses.size())
                handleCrash(responses.getSuspectedMembers());

            if(responses.getResults().size() == 0)
                return null;

            @SuppressWarnings("unchecked")
            List<Auction> reply = (List<Auction>) responses.getFirst();

            return reply;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to get response");
            return null;
        }
    }

}
