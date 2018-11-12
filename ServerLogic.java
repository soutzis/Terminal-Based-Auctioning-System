import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ServerLogic extends UnicastRemoteObject implements ServerInterface{

    private HashMap<String, Auction> auctions = new HashMap<>();
    private HashMap<String, Client> clients = new HashMap<>();

    private static final String USER_REGISTERED_SUCCESS = "User has been successfully registered!";
    private static final String USER_EXISTS_ERROR = "A user with this ID is already registered on server!";
    private static final String USER_UNREGISTERED_ERROR = "You have to register with the server first!";
    private static final String AUCTION_NOT_EXISTS_ERROR = "The auction with the id you provided does not exist.";
    private static final String UNAUTHORISED_ACTION_ERROR = "You don't have the authority to complete this action.";
    private static final String RESERVE_NOT_REACHED_ERROR = "Reserve price was not reached!";
    private static final String AUCTION_CLOSED = "Auction has been closed successfully.";

    ServerLogic() throws  RemoteException{
        super();
    }

    /**
     * This method will add a client to the server's "in-memory database", so the client can invoke server's methods.
     * @param client The client object that wants to register with the server
     * @return A message indicating if the client was successfully registered.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur during
     * the execution of a remote method call.
     */
    @Override
    public String registerClient(Client client) throws RemoteException {
        if(clients.containsKey(client.uid))
            return USER_EXISTS_ERROR;
        else{
            clients.put(client.uid, client);
            return USER_REGISTERED_SUCCESS;
        }
    }

    @Override
    public synchronized String initAuction(BigDecimal startPrice, BigDecimal minPriceAccepted,
                            String description, Seller seller) throws RemoteException {

        String auctionID = UUID.randomUUID().toString();
        Auction auction = new Auction(seller.getUid(), auctionID, startPrice, minPriceAccepted, description);
        this.auctions.put(auctionID, auction);

        return auctionID;
    }

    @Override
    public String closeAuction(String id, Seller seller) throws RemoteException {

        if(!clients.containsKey(seller.uid))
            return USER_UNREGISTERED_ERROR;

        if(!auctions.containsKey(id))
            return AUCTION_NOT_EXISTS_ERROR;

        Auction auction = auctions.get(id);

        if(!seller.getUid().equals(auction.getSellerId()))
            return UNAUTHORISED_ACTION_ERROR;

        auction.setActive(false);

        if(auction.getCurrentWinnerId() == null)
            return "\n"+AUCTION_CLOSED+"\n"+RESERVE_NOT_REACHED_ERROR;
        else{
            Buyer winner = (Buyer)clients.get(auction.getCurrentWinnerId());

            return "The winner is the buyer with:\nID = "+winner.getUid()+
                    "\nNAME = "+winner.getName()+
                    "\nEMAIL = "+winner.getEmail();
        }
    }

    @Override
    public synchronized void bid(String id, BigDecimal amount, Buyer buyer) throws RemoteException {

    }

    @Override
    public List<Auction> browseActiveAuctions() {
        List<Auction> activeAuctions = new ArrayList<>();
        auctions.values().forEach(item -> {
            if (item.isActive()) {
                activeAuctions.add(item);
            }});

        return activeAuctions;
    }

    @Override
    public List<Auction> browseFinishedAuctions() {
        List<Auction> activeAuctions = new ArrayList<>();
        auctions.values().forEach(item -> {
            if (!item.isActive()) {
                activeAuctions.add(item);
            }});

        return activeAuctions;
    }
}
