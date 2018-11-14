import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class is the Server's implementation (logic). The client need not know about how methods in this class work.
 * @author Petros Soutzis
 */
public class ServerLogic extends UnicastRemoteObject implements ServerInterface{

    /*ConcurrentHashMap<K,V> is used, to avoid a ConcurrentModificationException to be thrown
     if one thread tries to modify it while another is iterating over it.
     This is the in-memory database of the server.*/
    private ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();

    /*Provides atomic access to operations that check the active status of an auction.*/
    private Semaphore isActiveMutex = new Semaphore(1, true);

    /*String constants that are used to indicate if an operation was successful or if an error was encountered.*/
    private final String USER_REGISTERED_SUCCESS = "Client has been successfully registered!";
    private final String USER_EXISTS_ERROR = "\nA user with this ID is already registered on server!";
    private final String USER_UNREGISTERED_ERROR = "\nYou have to register with the server first!";
    private final String AUCTION_NOT_EXISTS_ERROR = "\nThe auction with the id you provided does not exist.";
    private final String UNAUTHORISED_ACTION_ERROR = "\nYou don't have the authority to complete this action.";
    private final String RESERVE_NOT_REACHED_MESSAGE = "\nReserve price was not reached!";
    private final String AUCTION_CLOSED_ACK = "\nAuction has been closed successfully.";
    private final String AUCTION_CLOSED_ERROR = "\nUnfortunately, this auction has already been closed by the seller.";
    private final String BID_REFUSED = "\nYour bid has been refused.";
    private final String BID_ACCEPTED = "\nYour bid has been accepted and processed by the server.";
    private final String INSUFFICIENT_BID_ERROR = "\nThe amount specified, is less than, or equal to the current bid.";

    /*When 'DEBUG' is set to true, developer can view error-related messages when an exception is thrown.*/
    private final boolean DEBUG = false;

    /**
     * Package-private constructor of the server implementation.
     * It should be used only by the class representing the host-machine for the server.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur during the execution
     * of a remote method call.
     */
    ServerLogic() throws  RemoteException{
        super();
    }

    /**
     * This method will add a client to the server's "in-memory database", so the client can invoke server's methods.
     * @param client The client object that wants to register with the server
     * @return A message indicating if the client was successfully registered.
     */
    @Override
    public String registerClient(Client client){
        //check if a Client exists in the database with the same ID as the provided Client's
        if(clients.containsKey(client.getUid()))
            return USER_EXISTS_ERROR;
        else{
            clients.put(client.getUid(), client);
            return USER_REGISTERED_SUCCESS + " Client ID = "+client.getUid();
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

        String auctionID = UUID.randomUUID().toString();
        Auction auction = new Auction(seller.getUid(), auctionID, startPrice, minPriceAccepted, description);
        auctions.put(auctionID, auction);

        return auctionID;
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
        final String sellerID = seller.getUid();
        //check if client exists on server
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
            isActiveMutex.acquire();
            auctions.get(auctionID).setActive(false);
            isActiveMutex.release();
        }
        catch (InterruptedException ie){
            if(DEBUG){
                ie.printStackTrace();
            }
        }

        //Check if reserve was reached
        if(!auctions.get(auctionID).isReserveReached())
            return AUCTION_CLOSED_ACK+RESERVE_NOT_REACHED_MESSAGE;
        //If reserve was reached, read the winner id and find them from the clients map
        else{
            Buyer winner = (Buyer)clients.get(auctions.get(auctionID).getCurrentWinnerId());

            return AUCTION_CLOSED_ACK+"\n\nThe winner is the buyer with:" +
                    "\nID = "+winner.getUid()+
                    "\nNAME = "+winner.getName()+
                    "\nEMAIL = "+winner.getEmail();
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
        String buyerID = buyer.getUid();
        String serverReply = null;

        /*Critical section if closeAuction() + bid() at the same time.
          If closeAuction was first, auction will close and bidder will get an error message,
          otherwise, bid will be placed and then auction will close as soon as it is allowed to enter
          critical section*/
        try{
            isActiveMutex.acquire();
            if(!auctions.get(auctionId).isActive())
                serverReply = BID_REFUSED+AUCTION_CLOSED_ERROR;
            isActiveMutex.release();
        }
        catch (InterruptedException ie){
            if(DEBUG)
                ie.printStackTrace();
        }

        //is user registered on server
        if(!clients.containsKey(buyerID))
            return BID_REFUSED+USER_UNREGISTERED_ERROR;
        //does auction id exist on server
        else if(!auctions.containsKey(auctionId))
            return BID_REFUSED+AUCTION_NOT_EXISTS_ERROR;

        //Get current bid and reserve price of the auction
        BigDecimal cBid = auctions.get(auctionId).getCurrentBid();
        BigDecimal reservePrice = auctions.get(auctionId).getReservePrice();

        //if bidAmount is less than, or equal to currentBid, then refuse bid
        if(bidAmount.compareTo(cBid) < 1)
            return BID_REFUSED+INSUFFICIENT_BID_ERROR;

        else if(bidAmount.compareTo(cBid) > 0){
            //bid is greater than auction's current bid
            auctions.get(auctionId).setCurrentBid(bidAmount);
            if(bidAmount.compareTo(reservePrice) >= 0){
                //if reserve price has been reached, set the winner of this auction
                auctions.get(auctionId).setReserveReached(true);
                auctions.get(auctionId).setCurrentWinner(buyerID);
            }
            return BID_ACCEPTED;
        }
        return serverReply;
    }

    /**
     * @return a List of all active auctions on server.
     */
    @Override
    public List<Auction> browseActiveAuctions() {
        return auctions
                .values()
                .stream()
                .filter(Auction::isActive)
                .collect(Collectors.toList());
    }

    /**
     * @param sellerId The id of the seller to view auctions of.
     * @return a List of all the active auctions on server, owned by the Client with id equal to the one specified.
     */
    @Override
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

    /*
    //This method is for scalability of program.
    //Might be useful to be able to see closed auctions for both buyers and sellers
    public List<Auction> browseFinishedAuctions() {
        List<Auction> activeAuctions = new ArrayList<>();
        auctions.values().forEach(item -> {
            if (!item.isActive()) {
                activeAuctions.add(item);
            }});

        return activeAuctions;
    }*/

}
