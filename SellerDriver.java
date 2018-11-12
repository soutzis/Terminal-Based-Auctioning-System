import java.math.BigDecimal;
import java.rmi.RemoteException;

public class SellerDriver {

    public static void main(String[] args){
        Seller temp = new Seller();
        final Seller seller = temp.createSellerClient();
        int choice = -1;

        try
        {
            //MAIN PROGRAM LOOP
            while(choice != Seller.EXIT_PROGRAM){
                //spin if not a valid action
                if (choice < Seller.MIN_SELLER_CHOICE || choice > Seller.MAX_SELLER_CHOICE){
                    choice = seller.takeSellerAction();
                }
                else if (choice == Seller.CREATE_AUCTION){
                    BigDecimal startPrice = seller.getItemStartingPrice();
                    BigDecimal reservePrice = seller.getReservePrice();
                    String description = seller.getItemDescription();
                    String auctionID = seller.serverReference.initAuction(startPrice, reservePrice, description, seller);
                    seller.addToOwnedAuctions(auctionID);
                    System.out.println("\nAuction has been successfully created. Auction ID is \""+auctionID+"\"");
                    choice = -1;
                }
                else if (choice == Seller.CLOSE_AUCTION){
                    String auctionId = seller.chooseAuctionToClose();
                    System.out.println("Auction with the id \""+auctionId+"\" will be closed.");
                    System.out.println(seller.serverReference.closeAuction(auctionId, seller));
                    seller.removeFromOwnedAuctions(auctionId);
                    choice = -1;
                }
            }
        }
        catch (RemoteException r) {
            r.printStackTrace();
        }

    }
}
