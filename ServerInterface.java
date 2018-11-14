import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * This is the Interface for the server implementation. It explains how methods implemented on the server work.
 * @author Petros Soutzis
 */
public interface ServerInterface extends Remote {

    public static final int MAX_ITEM_DESCRIPTION_CHARACTERS = 50;/*Maximum characters for auction description*/
    public static final int MIN_ITEM_DESCRIPTION_CHARACTERS = 10;/*Minimum characters for auction description*/
    public static final String AUCTIONS_TABLE_LINE_SEPARATOR = "--------------------------------------------" +
            "-----------------------------------------------------------------";/*For auctions' output-formatting*/
    public static final String AUCTIONS_TABLE_ATTRIBUTES_FORMAT = "%-3s%-36s%50s%16s%n";/*Format of auction attributes*/


    /**
     * This method should be called before the client tries to invoke any of the other methods
     * of the server. It registers a client to the server, so that the server knows who its users are,
     * at any point in time.
     * @param client The Client() object that will be registered on the server.
     * @return A message that will indicate if the client was successfully registered.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur during the execution
     * of a remote method call.
     */
    String registerClient(Client client) throws RemoteException;

    /**
     * This will create a new Auction-type object and set the Client-type object provided as its owner.
     * @param startPrice The price that an auction will start at, for an item.
     * @param minPriceAccepted The minimum price that an item can be sold at.
     * @param description The description of the item for sale.
     * @param seller The seller-object that initiated the auction. Its ID is used to indicate who owns this auction.
     * @return The ID of the newly initialised Auction.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    String initAuction(BigDecimal startPrice, BigDecimal minPriceAccepted,
                       String description, Client seller) throws RemoteException;

    /**
     * This method will be used to verify that closing the auction with the provided auction ID is an
     * allowed operation for the Client that invoked this method and will return a message accordingly.
     * @param auctionID The ID of the auction to close.
     * @param seller The seller that wants to close the auction.
     * @return A message, indicating the winner of the auction, or that the minimum acceptable price has not
     * been reached, or the reason why closing this auction failed.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    String closeAuction(String auctionID, Client seller) throws RemoteException;

    /**
     * This method allows a Client to place a bid for an auction.
     * @param auctionId The id of the auction to bid at.
     * @param bidAmount The amount of money that the buyer wants to place as a bid.
     * @param buyer The Client object that called this method, to bid at an auction.
     * @return A message that indicates if the bid was either successful or not (including reason for rejected bid).
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    String bid(String auctionId, BigDecimal bidAmount, Client buyer) throws RemoteException;

    /**
     * @return A List-type object, containing all the  <b>active</b> auctions that were initiated on the server.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    List<Auction> browseActiveAuctions() throws RemoteException;

    /**
     * @param sellerId The id of the seller to view auctions of.
     * @return A List-type object, containing all the  <b>active</b> auctions that were initiated on the server by
     * the seller with the ID passed as a parameter.
     * If the seller-ID is equal to null or doesn't exist on the server, the method will return null.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    List<Auction> browseAuctionsOfSeller(String sellerId) throws RemoteException;

}
