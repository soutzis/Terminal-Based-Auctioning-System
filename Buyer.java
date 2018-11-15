import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * This class represents a Buyer and can be used to place bids on auctioned items.
 * @author Petros Soutzis
 */
public class Buyer extends Client {

    public static final int MAX_BUYER_CHOICE = 2; /*maximum number of choices a seller client can take*/
    public static final int MIN_BUYER_CHOICE = 1;/*minimum number of choices a seller client can take*/
    public static final int BROWSE_AUCTIONS = 1;/*if user selects this, they can choose one of their auctions to close*/
    public static final int EXIT_PROGRAM = 2; /*if the user selects this, the client will terminate*/

    /*These String constants are used for guiding the user through various steps that require their input*/
    private final String INITIAL_USER_INPUT = "\nWhat do you want to do? (Enter the number of your choice. " +
            "E.g. To browse active auctions, enter the number 1)"
            +"\n1. Browse list of active auctions"
            +"\n2. Exit program"
            +"\nChoice = ";
    private final String CHOOSE_AUCTION_MSG = "\nTo place a bid, simply enter the number of the auction you " +
            "would like to bid at and you will be prompted to enter the details.\nOtherwise, press \"0\" " +
            "to quit and return at the initial menu.";
    private final String CHOOSE_AUCTION_INPUT = "\nEnter the number(#) of your choice: ";
    private final String PLACE_BID_MSG = "(Example: To bid £25, enter \"25\")\nEnter your bid and press \"Enter\": ";

    /*Warnings that are shown to the user, when they provide an invalid input*/
    private final String NO_ACTIVE_AUCTIONS_ERROR = "\nUnfortunately, there are no auctions available!";
    private final String BID_INVALID_ERROR = "\nYour bid was rejected, because it was invalid. Enter a valid bid.";
    private final String INDEX_OUT_OF_BOUNDS_ERROR = "\nYOUR INPUT IS OUT OF THE ACCEPTABLE RANGE";
    private final String INITIAL_MENU_ERROR = "\nYOUR INPUT WAS REJECTED!" +
            "\nPLEASE ENTER A *NUMERIC VALUE* IN THE RANGE OF THE VALUES "+MIN_BUYER_CHOICE+" - "+MAX_BUYER_CHOICE;


    /**
     * This is the default constructor for a Buyer Client-application.
     * It is used to create an initial instance of a Buyer. That initial instance
     * can then be used to create a "real" instance, by asking the user to provide
     * 'name' & 'email' fields, at run-time. A Universally Unique Identified (UUID)
     *  will also be automatically generated when the private-access constructor is called.
     */
    public Buyer(){
        //empty body
    }

    /**
     * This will create a Buyer object that can be used to bid on auctions on the server.
     * @param name The name of the user. Can be anything, even a number.
     * @param email The email of the user. This has to be a syntactically valid email
     */
    private Buyer(String name, String email){

        super(name, email);
    }

    /**
     * This method is called to create a Seller-type object, which has Client() as a base-class
     * @return a new Buyer() with a randomly generated Universally Unique ID.
     */
    @Override
    public Buyer createClient() {
        Scanner scanner = new Scanner(System.in);
        String name, email = null;
        System.out.println("A new buyer client will be created.\nPlease enter the required details.");
        System.out.print("Name: ");
        name = scanner.nextLine();
        //if provided email address does not conform to an email's syntax, then spin
        while(!emailValidator(email)){
            System.out.println("Enter a valid email address. E.g. someone@example.com");
            System.out.print("Email: ");
            email = scanner.nextLine();
        }
        //Will return this if registration was successful. If not, recursive call of method.
        Buyer buyer = new Buyer(name, email);

        if(buyer.getUid()==null && buyer.isServerAlive()){
            System.out.println("Failed to register user. Client program will prompt for registration again...");
            return createClient();
        }
        else
            return buyer;
    }

    /**
     * This method is called, to ask the user which action they want to take.
     * It will be called recursively, until the user enters a valid input.
     * @return An integer constant that matches the user's choice.
     */
    public int takeBuyerAction(){
        Scanner scanner = new Scanner(System.in);
        try{
            System.out.print(INITIAL_USER_INPUT);

            int choice = scanner.nextInt();
            if(choice < Buyer.MIN_BUYER_CHOICE || choice > Buyer.MAX_BUYER_CHOICE){
                System.out.println(INITIAL_MENU_ERROR);

                return takeBuyerAction();
            }
            else
                return choice;
        }
        catch(InputMismatchException ime){
            System.out.println(INITIAL_MENU_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());

            return takeBuyerAction();
        }
    }

    /**
     * This is used by buyers, to bid on items.
     * A formatted table will be shown to the user, containing all currently active auctions.
     * The user can then choose one by selecting the number displayed for a given auction, or quit by pressing '0'.
     * @return The id of the auction that the user chose to bid at.
     */
    public String browseAuctionToBid(){
        Scanner scanner = new Scanner(System.in);
        int choice;

        try{
            ArrayList<Auction> auctions = (ArrayList<Auction>)serverReference.browseActiveAuctions();
            //If list is empty, show error to user.
            if(auctions.isEmpty()){
                System.out.println(NO_ACTIVE_AUCTIONS_ERROR);
                return null;
            }

            //display auctions in a nice, formatted way
            System.out.println(CHOOSE_AUCTION_MSG);
            System.out.println(serverReference.AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.format(serverReference.AUCTIONS_TABLE_ATTRIBUTES_FORMAT,
                    "#","Auction ID", "Item Description", "Current Bid");
            System.out.println(serverReference.AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.println("0. Quit Browsing");
            for(int i=1; i<=auctions.size(); i++){
                Auction a = auctions.get(i-1);
                System.out.format("%-3s%36s%50s%16s%n",(i+". "),
                        a.getAuctionId(),a.getDescription(),"£"+a.getCurrentBid());
            }
            System.out.println(serverReference.AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.print(CHOOSE_AUCTION_INPUT);

            choice = scanner.nextInt();

            if (choice == 0)
                return null;
            else
                return auctions.get(choice-1).getAuctionId();
        }
        catch (RemoteException re){
            System.out.println(REMOTE_ERROR);
            if(DEBUG)
                System.out.println(re.getMessage());

            return null;
        }
        catch(InputMismatchException ime){
            System.out.println(INDEX_OUT_OF_BOUNDS_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());
            return browseAuctionToBid();
        }
        catch(IndexOutOfBoundsException ioobe){
            System.out.println(INDEX_OUT_OF_BOUNDS_ERROR);
            if(DEBUG)
                ioobe.printStackTrace();
            return browseAuctionToBid();
        }
    }

    /**
     * This method is used to place a bid on a specific auction, using the auctions ID.
     * After placing a bid, the server will reply with a message, indicating either success or error+explanation.
     * If an invalid input is entered (e.g. text in place of numeric), the method will return null.
     * @param auctionId is the id of an auction.
     * @return the reply of the server, about the bid placed.
     */
    public String placeBid(String auctionId){
        try{
            Scanner scanner = new Scanner(System.in);
            BigDecimal bidAmount;
            System.out.print(PLACE_BID_MSG);
            bidAmount = scanner.nextBigDecimal();

            return serverReference.bid(auctionId, bidAmount, this);
        }
        catch(RemoteException re){
            System.out.println(REMOTE_ERROR);
            if(DEBUG)
                System.out.println(re.getMessage());

            return null;
        }
        catch(InputMismatchException ime){
            System.out.println(BID_INVALID_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());
            return null;
        }

    }
}
