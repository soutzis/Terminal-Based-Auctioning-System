import java.math.BigDecimal;

public class Auction {
    private String auctionId, sellerId, currentWinnerId, lastBidderId, description;
    private BigDecimal startPrice, minPriceAccepted, currentBid;
    private boolean isActive, reserveReached;

    Auction(String sellerId, String auctionId, BigDecimal startPrice,
            BigDecimal minPriceAccepted, String description){

        this.currentWinnerId = null;
        this.sellerId = sellerId;
        this.auctionId = auctionId;
        this.startPrice = startPrice;
        this.minPriceAccepted = minPriceAccepted;
        this.description = description;
        this.isActive = true;
        this.reserveReached = false;
        this.currentBid = startPrice;
    }

    public void setActive(boolean active) {

        isActive = active;
    }

    public void setReserveReached(boolean reserveReached) {

        this.reserveReached = reserveReached;
    }

    public void setCurrentWinner(String buyerId) {

        this.currentWinnerId = buyerId;
    }

    public void setCurrentBid(BigDecimal currentBid) {

        this.currentBid = currentBid;
    }

    public String getAuctionId() {

        return auctionId;
    }

    public String getSellerId() {

        return sellerId;
    }

    public String getCurrentWinnerId() {

        return currentWinnerId;
    }

    public String getLastBidderId(){

        return lastBidderId;
    }

    public BigDecimal getStartPrice() {

        return startPrice;
    }

    public BigDecimal getMinPriceAccepted() {

        return minPriceAccepted;
    }

    public BigDecimal getCurrentBid() {

        return currentBid;
    }

    public String getDescription() {

        return description;
    }

    public boolean isActive() {

        return isActive;
    }

    public boolean isReserveReached() {

        return reserveReached;
    }
}
