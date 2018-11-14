import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This class is a POJO that represents an Auction.
 * @author Petros Soutzis
 */
public class Auction implements Serializable {
    private String auctionId, sellerId, currentWinnerId, description;
    private BigDecimal currentBid;
    private final BigDecimal reservePrice;
    private boolean isActive, reserveReached;

    /**
     * This is the Auction constructor. It will initialize an auction with the current winner set to null,
     * isActive set to true and current bid is set to be equal to the provided starting price.
     * @param sellerId The ID of the seller who initiated this auction
     * @param auctionId The ID of the auction itself
     * @param startPrice The start price of the auction
     * @param minPriceAccepted The reserve price of the auction
     * @param description The description of the item for sale
     */
    public Auction(String sellerId, String auctionId, BigDecimal startPrice,
            BigDecimal minPriceAccepted, String description){

        this.currentWinnerId = null;
        this.sellerId = sellerId;
        this.auctionId = auctionId;
        this.reservePrice = minPriceAccepted;
        this.description = description;
        this.isActive = true;
        this.reserveReached = false;
        this.currentBid = startPrice;
    }

    /**
     * Mutator of variable indicating if auction is active.
     * @param active The boolean to be applied to isActive
     */
    public void setActive(boolean active) {

        isActive = active;
    }

    /**
     * Mutator of variable indicating if reserve price is reached
     * @param reserveReached The boolean to set reserveReached at.
     */
    public void setReserveReached(boolean reserveReached) {

        this.reserveReached = reserveReached;
    }

    /**
     * Mutator of variable holding current winner id
     * @param buyerId the id of the buyer to set as winner
     */
    public void setCurrentWinner(String buyerId) {

        this.currentWinnerId = buyerId;
    }

    /**
     * Mutator of variable holding current bid
     * @param currentBid the new current bid
     */
    public void setCurrentBid(BigDecimal currentBid) {

        this.currentBid = currentBid;
    }

    /**
     * Accessor for auction id
     * @return This auction's id
     */
    public String getAuctionId() {

        return auctionId;
    }

    /**
     * @return The ID of the seller who owns this auction.
     */
    public String getSellerId() {

        return sellerId;
    }

    /**
     * @return The id of the winner of this auction
     */
    public String getCurrentWinnerId() {

        return currentWinnerId;
    }

    /**
     * @return The reserve price for this auction
     */
    public BigDecimal getReservePrice() {

        return reservePrice;
    }

    /**
     * @return The current bid of this auction.
     */
    public BigDecimal getCurrentBid() {

        return currentBid;
    }

    /**
     * @return The description of this auction
     */
    public String getDescription() {

        return description;
    }

    /**
     * @return If this auction is active or not
     */
    public boolean isActive() {

        return isActive;
    }

    /**
     * @return If reserve price has been reached or not
     */
    public boolean isReserveReached() {

        return reserveReached;
    }
}
