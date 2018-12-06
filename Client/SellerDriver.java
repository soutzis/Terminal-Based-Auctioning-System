package Client;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
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

        //////////////AUTHENTICATION OR CREATE NEW USER//////////////
        while(initialChoice != Seller.EXIT_PROGRAM){
            initialChoice = seller.sellerInitialMenu();

            if(initialChoice == Seller.LOGIN){
                //ask seller to provide email that was used for registration
                String email = seller.emailForAuthentication();
                //if email is valid, then proceed to authenticate sever and self.
                if(email != null) {
                    try{
                        seller = (Seller)seller.challengeResponseAuthentication(email);
                    }catch (IOException ioe) {
                        System.out.println("IOException occurred when trying to read your private key from file");
                    } catch (ClassNotFoundException cnfe) {
                        System.out.println("Make sure you have not deleted any class files used by the application");
                    } catch (NoSuchPaddingException nspe) {
                        System.out.println("There was a problem with encrypting your communication");
                    } catch (NoSuchAlgorithmException nsae) {
                        System.out.println("Please check if your key uses 'RSA' for encryption");
                    } catch (IllegalBlockSizeException ibse) {
                        System.out.println("Problem occurred while trying to read key from file.");
                    } catch (InvalidKeyException ike) {
                        System.out.println("The private key provided does not match your public key.");
                    } catch (InvalidKeySpecException ikse) {
                        System.out.println("The algorithm used for the key you provided is invalid.");
                    } catch (SignatureException se) {
                        System.out.println("Your signature file might be corrupted.");
                    }

                    /*If authentication fails and server returns null,
                     * then set seller to default constructor to provide user with choice again*/
                    if(seller == null){
                        System.out.println("Could not authenticate with server");
                        seller = new Seller();
                    }
                    else {
                        seller.setServerAlive(true);
                        System.out.println("\nWelcome back "+seller.getName()+"!");
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
                String auctionID = null;
                try{
                    auctionID = seller.getServerReference()
                            .initAuction(startPrice, reservePrice, description, seller);
                }
                catch (RemoteException r) {
                    System.out.println(Seller.REMOTE_ERROR);
                }
                if(auctionID == null)
                    System.out.println("Server could not create the requested auction");
                else
                    System.out.println("\nAuction has been successfully created. Auction ID is \""+auctionID+"\"");
            }
            //if user chooses to close an auction, this will list all the auctions owned by user and then user
            //can choose one of them.
            else if (choice == Seller.CLOSE_AUCTION){
                String auctionId = seller.chooseAuctionToClose();

                //if returned id is null, go to start of loop
                if(auctionId != null){
                    System.out.println("A request will be sent, to close the auction with id \""+auctionId+"\".");
                    String response = null;
                    try{
                        response = seller.getServerReference().closeAuction(auctionId, seller);
                    }
                    catch (RemoteException r) {
                        System.out.println(Seller.REMOTE_ERROR);
                    }
                    if(response == null)
                        System.out.println("Server could not close auction.");
                    else
                        System.out.println(response);
                }
            }
        }
    }
}
