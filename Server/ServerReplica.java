package Server;

import org.jgroups.*;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Util;

import javax.crypto.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class ServerReplica extends ReceiverAdapter{

    /*RSA Private Key*/
     private PrivateKey privateKey;

    private JChannel mainChannel, synchroChannel;
    private RpcDispatcher dispatcher;

    /*ConcurrentHashMap<K,V> is used, to avoid a ConcurrentModificationException to be thrown
     if one thread tries to modify it while another is iterating over it.
     This is the in-memory database of the server.*/
    private  ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();
    private  ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private  ConcurrentHashMap<String, AuthenticationState> authState = new ConcurrentHashMap<>();

    /*Provides atomic access to operations that check the active status of an auction.*/
    private  Semaphore auctionIsActiveMutex = new Semaphore(1, true);

    private final ServerState state = new ServerState();
    //final ConcurrentHashMap<String, ConcurrentHashMap> state = new ConcurrentHashMap<>();
    //final List<String> state = new LinkedList<>();

    private  final String USER_UNREGISTERED_ERROR = "\nYou have to register with the server first!";
    private  final String AUCTION_NOT_EXISTS_ERROR = "\nThe auction with the id you provided does not exist.";
    private  final String AUCTION_CLOSED_ERROR = "\nUnfortunately, this auction has already been closed by the seller.";

    private  final boolean DEBUG = false;

    /**
     * This is the start method of this server instance. It will place it in 2 Clusters.
     * 2 JChannels for 2 Cluster. 1 Clusters is for executing methods, invoked by the front-end (ServerLogic).
     * Other Cluster is for synchronization between server objects.
     * @throws Exception exception
     */
    void start() throws Exception {

        final String PRIVATE_KEY_FNAME = "server-private-key.key";
        //JGroups
        final String CLUSTER_NAME = "ServerCluster";
        final String CLUSTER_SYNC_NAME = "ServerSync";

        //load private key
        privateKey = SecurityManager.readPrivateKey(PRIVATE_KEY_FNAME);

        this.mainChannel = new JChannel();
        this.mainChannel.connect(CLUSTER_NAME);
        this.dispatcher = new RpcDispatcher(this.mainChannel, this);

        //edit singleton name in udp.xml to allow use of same transport stack
        this.synchroChannel = new JChannel();
        this.synchroChannel.setReceiver(this);
        this.synchroChannel.connect(CLUSTER_SYNC_NAME);
        this.synchroChannel.getState(null,10000);
    }

    /**
     * Method from ReceiverAdapter class. Prints out the newly accepted view of the cluster.
     */
    public void viewAccepted(View new_view){

        System.out.println("CURRENT VIEW: "+new_view);
    }

    /**
     * Method from ReceiverAdapter class that specifies how a received message from another cluster-member is
     * to be handled.
     * @param msg the received message.
     */
    public void receive(Message msg){
        //Unpack the message and set the current state. (Catch up)
        ServerState rcvState = (ServerState)msg.getObject();
        synchronized (state){
            state.setClients(rcvState.getClients());
            state.setAuctions(rcvState.getAuctions());
            state.setAuthState(rcvState.getAuthState());
        }
    }

    /**
     * @param output The object to serialize and pass to other cluster members.
     * @throws Exception exception
     */
    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    /**
     *
     * @param input The shared state's data. It will be converted to 'ServerState' object, so that this instance's
     *              variables can be set to be identical to the shared state.
     * @throws Exception exception
     */
    public void setState(InputStream input) throws Exception{
        ServerState tempState;
        tempState =(ServerState) Util.objectFromStream(new DataInputStream(input));
        synchronized (state){
            state.setClients(tempState.getClients());
            this.clients = tempState.getClients();
            this.auctions = tempState.getAuctions();
            this.authState = tempState.getAuthState();
        }
    }

    /**
     * This method is invoked by the front-end, for a cluster member that is suspected to have left the cluster,
     * but is still in the current view.
     */
    public void disconnect(){

        this.synchroChannel.disconnect();
        this.mainChannel.disconnect();
        System.exit(0);
    }

    /**
     * When this is called, it will send a message to all members of a cluster to notify them that the
     * shared state has changed.
     * @param client_db Variable that stores data about clients.
     * @param auction_db Variable that stores data about auctions.
     * @param authState_db Variable that stores data about authentication state of a client.
     * @param channel The channel to send the state to
     * @throws Exception exception
     */
    private synchronized void updateSharedState(
                                    ConcurrentHashMap<String, Client> client_db,
                                    ConcurrentHashMap<String, Auction> auction_db,
                                    ConcurrentHashMap<String, AuthenticationState> authState_db,
                                    JChannel channel) throws Exception {
        //put HashMaps<>() in state object, so they can be extracted later
        this.state.setClients(client_db);
        this.state.setAuctions(auction_db);
        this.state.setAuthState(authState_db);
        Message msg = new Message(null, this.state);
        channel.send(msg);
    }


    public  ServerRegistrationReply register (Client client){
        ServerRegistrationReply reply = new ServerRegistrationReply();
        //check if a Client exists in the database with the same ID as the provided Client's
        if(clients.containsKey(client.getUid())){
            reply.setSuccess(false);
            String USER_EXISTS_ERROR = "\nA user with this ID is already registered on server!";
            reply.setMsg(USER_EXISTS_ERROR);
        }
        //check if a Client exists in the database with the same email as the Client parsed as parameter
        else if(clients
                .values()
                .stream()
                .anyMatch(regUser -> regUser.getEmail().toUpperCase().equals(client.getEmail().toUpperCase()))){
            reply.setSuccess(false);
            String EMAIL_EXISTS_ERROR = "\nThe email you have entered is already registered with another user.";
            reply.setMsg(EMAIL_EXISTS_ERROR);
        }
        //if all checks are OK, add user to "database" and return
        else{
            /*String constants that are used to indicate if an operation was successful or if an error was encountered.*/
            String USER_REGISTERED_SUCCESS = "\nClient has been successfully registered!";
            reply.setMsg(USER_REGISTERED_SUCCESS);
            clients.put(client.getUid(), client);
            //add change to server 'state' and pass state around to all cluster members
            try {
                updateSharedState(clients,auctions,authState,synchroChannel);
            }
            catch (Exception e) {
                System.out.println("Failed to send state updates to channel");
                e.printStackTrace();
            }
        }
        return reply;

    }

    public  SealedObject authenticationStep1(SealedObject challenge, byte[] signature, int rand){
        SealedObject newChallenge;
        Cipher cipher;
        try{
            cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.PRIVATE_KEY, privateKey);
        }
        catch(NoSuchAlgorithmException nsae){
            return null;
        }
        catch (NoSuchPaddingException nspe) {
            nspe.printStackTrace();
            return null;
        }
        catch (InvalidKeyException ike) {
            ike.printStackTrace();
            return null;
        }

        //client requests that server authenticates itself. Log successful decryption
        AuthenticationRequest request;
        try {
            request = (AuthenticationRequest)challenge.getObject(cipher);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
        catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        }

        if(authState.containsKey(request.getEmail())){
            return null;
        }

        AuthenticationState currentState = new AuthenticationState(true, 1);
        authState.put(request.getEmail(), currentState);
        try {
            updateSharedState(clients,auctions,authState,synchroChannel);
        }
        catch (Exception e) {
            System.out.println("Failed to send state updates to channel");
            e.printStackTrace();
        }

        //IF successfully solved challenge
        AuthenticationReply reply = new AuthenticationReply(request.getNumber(), rand);

        //Find client reference that the user claims to be.
        Optional<Client> optClient= clients
                .values()
                .stream()
                .filter(x -> x.getEmail().toUpperCase().equals(request.getEmail().toUpperCase()))
                .findFirst();
        //If no matching client is found, return null and remove log.
        Client client = optClient.orElse(null);

        if (client == null){
            authState.remove(request.getEmail());
            try {
                updateSharedState(clients,auctions,authState,synchroChannel);
            }
            catch (Exception e) {
                System.out.println("Failed to send state updates to channel");
                e.printStackTrace();
            }
            return null;
        }

        boolean verified = false;

        try{
            verified = SecurityManager.verifySignature(client.getPublicKey(),signature,request);
        }
        catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (SignatureException e) {
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        if(!verified){
            System.out.println("Verification Failed");
            authState.remove(request.getEmail());
            try {
                updateSharedState(clients,auctions,authState,synchroChannel);
            }
            catch (Exception e) {
                System.out.println("Failed to send state updates to channel");
                e.printStackTrace();
            }
            return null;
        }
        //else if client is valid, get their public key and encrypt challenge to send
        try {
            cipher.init(Cipher.PUBLIC_KEY, client.getPublicKey());
            newChallenge = new SealedObject(reply, cipher);
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
        //Now send solved + new challenge back to client and log transaction in auth-state store
        currentState = new AuthenticationState(true, 2, reply.getNewChallengeNumber(), client);
        authState.replace(request.getEmail(), currentState);
        try {
            updateSharedState(clients,auctions,authState,synchroChannel);
        }
        catch (Exception e) {
            System.out.println("Failed to send state updates to channel");
            e.printStackTrace();
        }

        //return solved answer + new challenge
        return newChallenge;
    }

    /**AUTHENTICATION OF CLIENT*/
    public  Client authenticationStep2(SealedObject solvedChallenge){
        AuthenticationState authentication_state;
        AuthenticationRequest solvedByClient = null;
        int sequence;
        int challengeSent;
        //check email and see if state sequence number is 2
        try {
            Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.PRIVATE_KEY, privateKey);
            solvedByClient = (AuthenticationRequest) solvedChallenge.getObject(cipher);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        catch (BadPaddingException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        if(solvedByClient == null)
            return null;

        String email = solvedByClient.getEmail();

        //Check the state of authentication. If all checks pass, then authenticate client
        if(!authState.containsKey(email))
            return null;
        else if(!authState.get(email).getSuccessful())
            return null;
        else if(authState.get(email).getSequenceNumber() != 2)
            return null;
        else {
            authentication_state = authState.get(email);
            sequence = authentication_state.getSequenceNumber();
            challengeSent = authentication_state.getChallengeSent();
            authState.remove(email); //now state does not exist
            try {
                updateSharedState(clients,auctions,authState,synchroChannel);
            }
            catch (Exception e) {
                System.out.println("Failed to send state updates to channel");
                e.printStackTrace();
            }
        }

        if(challengeSent != solvedByClient.getNumber()){
            return null;
        }
        else if(challengeSent == solvedByClient.getNumber() && sequence == 2){
            return authentication_state.getClient();
        }
        else{
            return null;
        }
    }

    /**START AUCTION*/
    public String startAuction(String auctionId, BigDecimal startPrice, BigDecimal minPriceAccepted,
                          String description, Client seller) {

        Auction auction = new Auction(auctionId, seller.getUid(), startPrice, minPriceAccepted, description);
        auctions.put(auction.getAuctionId(), auction);
        try {
            updateSharedState(clients,auctions,authState,synchroChannel);
        }
        catch (Exception e) {
            System.out.println("Failed to send state updates to channel");
            e.printStackTrace();
            /*can set counter and recursively call updateSharedState().
            * If counter reaches a predetermined max, e.g. '5', then return null*/
            return null;
        }

        return auction.getAuctionId();
    }

    public  String stopAuction(String auctionID, Client seller){
        final String sellerID = seller.getUid();
        //check if client exists on server
        String UNAUTHORISED_ACTION_ERROR = "\nYou don't have the authority to complete this action.";
        if(!clients.containsKey(sellerID))
            return USER_UNREGISTERED_ERROR;
            //check if auction id exists on server
        else if(!auctions.containsKey(auctionID))
            return AUCTION_NOT_EXISTS_ERROR;
            //check if seller has the authority to close auction
        else if(!sellerID.equals(auctions.get(auctionID).getSellerId()))
            return UNAUTHORISED_ACTION_ERROR;
            //check if auction was already closed
        else if(!auctions.get(auctionID).isActive())
            return AUCTION_CLOSED_ERROR;

        /*Critical section if closeAuction() + bid() at the same time.
          If closeAuction was first, auction will close and bidder will get an error message,
          otherwise, bid will be placed and then auction will close as soon as it is allowed to enter
          critical section*/
        try{
            auctionIsActiveMutex.acquire();
            auctions.get(auctionID).setActive(false);
            auctionIsActiveMutex.release();
            updateSharedState(clients,auctions,authState,synchroChannel);
        }
        catch (InterruptedException ie){
            if(DEBUG){
                ie.printStackTrace();
            }
        }
        catch (Exception e){
            System.out.println("Failed to send state updates to channel");
            e.printStackTrace();
            return null;
        }

        //Check if reserve was reached
        String RESERVE_NOT_REACHED_MESSAGE = "\nReserve price was not reached!";
        String AUCTION_CLOSED_ACK = "\nAuction has been closed successfully.";
        if(!auctions.get(auctionID).isReserveReached())
            return AUCTION_CLOSED_ACK + RESERVE_NOT_REACHED_MESSAGE;
            //If reserve was reached, read the winner id and find them from the clients map
        else{
            String winnerId = auctions.get(auctionID).getCurrentWinnerId();
            Client winner = clients.get(winnerId);

            return AUCTION_CLOSED_ACK +"\n\nThe winner is the buyer with:" +
                    "\nID = "+winnerId+
                    "\nNAME = "+winner.getName()+
                    "\nEMAIL = "+winner.getEmail()+
                    "\nSOLD FOR £"+auctions.get(auctionID).getCurrentBid()+"!";
        }
    }

    public  String bid(String auctionId, BigDecimal bidAmount, Client buyer) {
        String buyerID = buyer.getUid();

        /*Critical section if closeAuction() + bid() at the same time.
          If closeAuction was first, auction will close and bidder will get an error message,
          otherwise, bid will be placed and then auction will close as soon as it is allowed to enter
          critical section*/
        String BID_REFUSED = "\nYour bid has been refused.";
        try {
            auctionIsActiveMutex.acquire();
            boolean auctionIsActive = auctions.get(auctionId).isActive();
            auctionIsActiveMutex.release();
            if (!auctionIsActive)
                return BID_REFUSED + AUCTION_CLOSED_ERROR;
        }
        catch (InterruptedException ie) {
            if (DEBUG)
                ie.printStackTrace();
        }

        //is user registered on server
        if (!clients.containsKey(buyerID))
            return BID_REFUSED + USER_UNREGISTERED_ERROR;
            //does auction id exist on server
        else if (!auctions.containsKey(auctionId))
            return BID_REFUSED + AUCTION_NOT_EXISTS_ERROR;

        //Get current bid and reserve price of the auction
        BigDecimal cBid = auctions.get(auctionId).getCurrentBid();
        BigDecimal reservePrice = auctions.get(auctionId).getReservePrice();

        //if bidAmount is less than, or equal to currentBid, then refuse bid
        String INSUFFICIENT_BID_ERROR = "\nThe amount specified, is less than, or equal to the current bid.";
        if (bidAmount.compareTo(cBid) < 1)
            return BID_REFUSED + INSUFFICIENT_BID_ERROR;

        else if (bidAmount.compareTo(cBid) > 0) {
            //bid is greater than auction's current bid
            auctions.get(auctionId).setCurrentBid(bidAmount);
            //update shared state
            try {
                updateSharedState(clients,auctions,authState,synchroChannel);
            }
            catch (Exception e) {
                System.out.println("Failed to send state updates to channel");
                return null;
            }
            if (bidAmount.compareTo(reservePrice) >= 0) {
                //if reserve price has been reached, set the winner of this auction
                auctions.get(auctionId).setReserveReached(true);
                auctions.get(auctionId).setCurrentWinner(buyerID);

                //update shared state
                try {
                    updateSharedState(clients,auctions,authState,synchroChannel);
                }
                catch (Exception e) {
                    System.out.println("Failed to send state updates to channel");
                    return null;
                }

                String BID_ACCEPTED = "\nYour bid has been accepted and processed by the server.";
                return BID_ACCEPTED;
            }
        }
        return null;
    }

    public  List<Auction> browseActiveAuctions(){
        return auctions
                .values()
                .stream()
                .filter(Auction::isActive)
                .collect(Collectors.toList());
    }

    public List<Auction> browseAuctionsOfSeller(String sellerId) {
        //if id does not exist on server, return null
        if(!clients.containsKey(sellerId))
            return null;
        else
            return auctions
                    .values()
                    .stream()
                    .filter(Auction::isActive)
                    .filter(auction -> auction.getSellerId().equals(sellerId))
                    .collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception{

        try{
            new ServerReplica().start();
        }
        catch (Exception e){
            System.out.println("Failed to start Server Replica");
        }

    }
}
