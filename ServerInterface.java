import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {

    public final int MAX_ITEM_DESCRIPTION_CHARACTERS = 50;
    public final int MIN_ITEM_DESCRIPTION_CHARACTERS = 10;

    /**
     * This method should be called before the client tries to invoke any of the other methods
     * of the server. It registers a client to the server, so that the server knows who its users are,
     * at any point in time.
     * @param client The client that will be registered on the server.
     * @return A message that will indicate if the client was successfully registered.
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur during the execution
     * of a remote method call.
     */
    public String registerClient(Client client) throws RemoteException;

    /**
     *
     * @param startPrice
     * @param minPriceAccepted
     * @param description
     * @param seller
     * @return
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    public String initAuction(BigDecimal startPrice, BigDecimal minPriceAccepted,
                            String description, Seller seller) throws RemoteException;

    /**
     *
     * @param id
     * @param seller
     * @return
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    public String closeAuction(String id, Seller seller) throws RemoteException;

    /**
     *
     * @param id
     * @param amount
     * @param buyer
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    public void bid(String id, BigDecimal amount, Buyer buyer) throws RemoteException;

    /**
     *
     * @return
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    public List<Auction> browseActiveAuctions() throws RemoteException;

    /**
     *
     * @return
     * @throws RemoteException Is thrown for any communication-related exception(s) that may occur
     * during the execution of a remote method call.
     */
    public List browseFinishedAuctions() throws RemoteException;

}
