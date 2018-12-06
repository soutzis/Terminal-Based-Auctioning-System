package Client;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * This is the main class that will create a buyer and take user input.
 * @author Petros Soutzis
 */
public class BuyerDriver {

    public static void main(String[] args){
        //Default constructor, used to call createClient() method
        Buyer buyer = new Buyer();
        
        //initially set to -1, until entering the program loops
        int initialChoice = -1;
        int choice = -1;


        //////////////AUTHENTICATION OR CREATE NEW USER//////////////
        while(initialChoice != Buyer.EXIT_INITIAL){
            initialChoice = buyer.buyerInitialMenu();

            if(initialChoice == Buyer.LOGIN){
                //ask buyer to provide email that was used for registration
                String email = buyer.emailForAuthentication();
                //if email is valid, then proceed to authenticate sever and self.
                if(email != null) {
                    try {
                        buyer = (Buyer) buyer.challengeResponseAuthentication(email);
                    } catch (IOException ioe) {
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
                     * then set buyer to default constructor to provide user with choice again*/
                    if(buyer == null){
                        System.out.println("Could not authenticate with server");
                        buyer = new Buyer();
                    }
                    else {
                        buyer.setServerAlive(true);
                        System.out.println("\nWelcome back "+buyer.getName()+"!");
                        initialChoice = Buyer.EXIT_INITIAL;
                    }
                }
            }
            else if(initialChoice == Buyer.NEW_USER){
                buyer = buyer.createClient();
                initialChoice = Buyer.EXIT_INITIAL;
            }
        }

        //////////////MAIN MENU PROGRAM LOOP (after authentication or registration)//////////////
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
                    else
                        System.out.println("An error has occurred while placing bid. Please try again.");
                }
            }

        }
    }
}
