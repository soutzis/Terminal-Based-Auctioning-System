import java.math.BigDecimal;
import java.rmi.RemoteException;

/**
 * This is the main class that will create a seller and take user input.
 * @author Petros Soutzis
 */
public class SellerDriver {

    //main method
    public static void main(String[] args){
        //Default constructor, used to call createClient() method
        Seller temp = new Seller();
        final Seller seller = temp.createClient();
        //initially set to -1 to enter the main program loop
        int choice = -1;

        try
        {
            //MAIN PROGRAM LOOP
            while(choice != Seller.EXIT_PROGRAM && seller.isServerAlive()){
                //will ask the user what they want to do when they are at the initial options-menu
                choice = seller.takeSellerAction();

                //if user chooses to create auction, will ask user for values and send request to server
                if (choice == Seller.CREATE_AUCTION){
                    String description = seller.getItemDescription();
                    BigDecimal reservePrice = seller.getReservePrice();
                    BigDecimal startPrice = seller.getStartingPrice(reservePrice);
                    String auctionID = seller.serverReference
                            .initAuction(startPrice, reservePrice, description, seller);

                    System.out.println("\nAuction has been successfully created. Auction ID is \""+auctionID+"\"");
                }
                //if user chooses to close an auction, this will list all the auctions owned by user and then user
                //can choose one of them.
                else if (choice == Seller.CLOSE_AUCTION){
                    String auctionId = seller.chooseAuctionToClose();

                    //if returned id is null, go to start of loop
                    if(auctionId != null){
                        System.out.println("A request will be sent, to close the auction with id \""+auctionId+"\".");
                        System.out.println(seller.serverReference.closeAuction(auctionId, seller));
                    }
                }
            }
        }
        catch (RemoteException r) {
            System.out.println(Seller.REMOTE_ERROR);
        }

    }
}
