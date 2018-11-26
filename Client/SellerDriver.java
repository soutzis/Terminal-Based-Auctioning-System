package Client;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * This is the main class that will create a seller and take user input.
 * @author Petros Soutzis
 */
public class SellerDriver {


    //main method
    public static void main(String[] args){
        //Default constructor, used to call createClient() method
        Seller seller = new Seller();
        //initially set to -1, until entering the program loops
        int initialChoice = -1;
        int choice = -1;


        try
        {   //////////////AUTHENTICATION OR CREATE NEW USER//////////////
            while(initialChoice != Seller.EXIT_PROGRAM){
                initialChoice = seller.initialMenu();

                if(initialChoice == Seller.LOGIN){
                    //ask seller to provide email that was used for registration
                    String email = seller.emailForAuthentication();
                    //if email is valid, then proceed to authenticate sever and self.
                    if(email != null) {
                        seller = (Seller)seller.challengeResponseAuthentication(email);
                        /*If authentication fails and server returns null,
                        * then set seller to default constructor to provide user with choice again*/
                        if(seller == null){
                            System.out.println("Could not authenticate with server");
                            seller = new Seller();
                        }
                        else {
                            seller.setServerAlive(true);
                            System.out.println("Welcome back "+seller.getName()+"!");
                            initialChoice = Seller.EXIT_PROGRAM;
                        }
                    }
                }
                else if(initialChoice == Seller.NEW_USER){
                    seller = seller.createClient();
                    initialChoice = Seller.EXIT_PROGRAM;
                }
            }

            //////////////MAIN MENU PROGRAM LOOP (after authentication or registration)//////////////
            while(choice != Seller.EXIT_PROGRAM && seller.isServerAlive()){
                //will ask the user what they want to do when they are at the initial options-menu
                choice = seller.takeSellerAction();

                //if user chooses to create auction, will ask user for values and send request to server
                if (choice == Seller.CREATE_AUCTION){
                    String description = seller.getItemDescription();
                    BigDecimal reservePrice = seller.getReservePrice();
                    BigDecimal startPrice = seller.getStartingPrice(reservePrice);
                    String auctionID = seller.getServerReference()
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
                        System.out.println(seller.getServerReference().closeAuction(auctionId, seller));
                    }
                }
            }
        }
        catch (RemoteException r) {
            System.out.println(Seller.REMOTE_ERROR);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

    }
}
