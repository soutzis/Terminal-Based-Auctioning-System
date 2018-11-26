package Client;

/**
 * This is the main class that will create a seller and take user input.
 * @author Petros Soutzis
 */
public class BuyerDriver {

    public static void main(String[] args){
        //Default constructor, used to call createClient() method
        Buyer temp = new Buyer();
        final Buyer buyer = temp.createClient();
        //initially set to -1 to enter the main program loop
        int choice = -1;

        //MAIN PROGRAM LOOP
        while(choice != Buyer.EXIT_PROGRAM && buyer.isServerAlive()){
            //spin if not a valid action
            choice = buyer.takeBuyerAction();

            /*if user selects to browse auctions, all active auctions will be displayed for them
             and then they will be prompted to choose one of the auctions to bid at, or simply quit by pressing '0'*/
            if (choice == Buyer.BROWSE_AUCTIONS){
                String selectedAuctionID = buyer.browseAuctionToBid();
                if(selectedAuctionID!=null){
                    String serverReply = buyer.placeBid(selectedAuctionID);
                    if(serverReply != null)
                        System.out.println(serverReply);
                }
            }

        }
    }
}
