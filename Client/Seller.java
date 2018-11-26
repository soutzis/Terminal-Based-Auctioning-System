package Client;

import Server.Auction;
import Server.Client;
import Server.ServerInterface;

import java.io.Console;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Seller extends Client {

    public static final int MAX_SELLER_CHOICE = 3; /*maximum number of choices a seller client can take at start menu*/
    public static final int MIN_SELLER_CHOICE = 1;/*minimum number of choices a seller client can take at start menu*/
    public static final int CREATE_AUCTION = 1;/*if user selects this, they will be prompted to create a new auction*/
    public static final int CLOSE_AUCTION = 2;/*if user selects this, they can choose one of their auctions to close*/
    public static final int EXIT_PROGRAM = 3; /*if the user selects this, the client will terminate*/
    public static final int LOGIN = 1; /*if the user selects this, the client will authenticate with server*/
    public static final int NEW_USER = 2; /*if the user selects this, new client will be created*/

    /*These String constants are used for guiding the user through various steps that require their input*/
    private final String ACTION_USER_INPUT = "\nWhat do you want to do? (Enter the number of your choice. " +
            "E.g. To create auction, enter the number \"1\")"
            +"\n1. Create Auction"
            +"\n2. Close an Auction"
            +"\n3. Exit program"
            +"\nChoice: ";
    private final String RESERVE_PRICE_MSG = "\nPlease enter the MINIMUM price that the item can be sold at."
            +"\nThis price is a secret to bidders. There will be no winner if this price is not reached.";
    private final String RESERVE_PRICE_INPUT = "\nReserve Price (£): ";
    private final String START_PRICE_MSG ="\nPlease enter the price that you want the auction to start at."
            +"\nI.e. An item has a value of £20, a good starting price would be £5, " +
            "which is the 25% of the item's value."
            +"\nThis price MUST be less than, or equal to the \"Reserve Price\"";
    private final String START_PRICE_INPUT = "\nStarting Price (£): ";
    private final String PRICE_ROUNDING_WARNING = "\nNOTE: The decimal points of the price you enter, will be " +
            "rounded to the closest second decimal point of a number.\nSee examples below."
            +"\n[ £0.014536 -> £0.01 ], [ £0.0156 -> £0.02 ], [ £1.456768 -> £1.46 ]";
    private final String DESCRIPTION_MSG = "\nPlease enter the description of the item you want to sell. " +
            "E.g: \"A red pen. A pen that has red ink!\""
            +"\nMake sure you enter something concise and meaningful!!"
            +"\nYour description must be between "+ ServerInterface.MIN_ITEM_DESCRIPTION_CHARS +
            " - "+ ServerInterface.MAX_ITEM_DESCRIPTION_CHARS +
            " characters long (whitespace does not count)."
            +"\nDescription: ";

    /*Warnings that are shown to the user, when they provide an invalid input*/
    private final String INPUT_NOT_PRICE_ERROR = "\nPLEASE ENTER A *NUMERIC VALUE* FOR THE PRICE.";
    private final String INPUT_NOT_NUMERICAL_ERROR = "\nPLEASE ENTER A *NUMERIC VALUE* AS INPUT.";
    private final String INDEX_OUT_OF_BOUNDS_ERROR = "\nYOUR INPUT IS OUT OF THE ACCEPTABLE RANGE";
    private final String MIN_PRICE_ACCEPTED_ERROR = "\nThe price must be £0.01 (one cent) or more.";
    private final String START_PRICE_ERROR = "\nSTARTING PRICE CAN NOT BE GREATER THAN THE RESERVE PRICE!";
    private final String NO_ACTIVE_AUCTIONS_ERROR = "\nUnfortunately you own no active auctions at the moment.";
    private final String MAIN_MENU_ERROR = "\nYOUR INPUT WAS REJECTED!" +
            "\nPLEASE ENTER A *NUMERIC VALUE* IN THE RANGE OF THE VALUES "+MIN_SELLER_CHOICE+" - "+MAX_SELLER_CHOICE;

    /**
     * This is the default constructor for a Seller Client-application.
     * It is used to create an initial instance of a Seller. That initial instance
     * can then be used to create a "real" instance, by asking the user to provide
     * 'name' & 'email' fields, at run-time. A Universally Unique Identified (UUID)
     *  will also be automatically generated when the private-access constructor is called.
     */
    public Seller(){
        //empty body
    }

    /**
     * This will create a Seller object that can be used to create or close auctions on the server.
     * @param name The name of the user. Can be anything, even a number.
     * @param email The email of the user. This has to be a syntactically valid email
     * @param password Te password of the user.
     */
    private Seller(String name, String email, String password){

        super(name, email, password);
    }

    /**
     * This method is called to create a Seller-type object, which has Client() as a base-class
     * @return a new Seller() with a randomly generated Universally Unique ID.
     */
    @Override
    public Seller createClient(){
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();
        String name = null, email = null;
        char[] password, verification;
        System.out.println("A new client will be registered with the server.\nPlease enter the required details.");

        while(!detailsValidator(name, NAME_REGEX)){
            System.out.println("Enter a name, that consists only of AlphaNumeric characters and is 5-30 characters long!");
            System.out.print("Name: ");
            name = scanner.nextLine();
        }
        //if provided email address does not conform to an email's syntax, then spin
        while(!detailsValidator(email, EMAIL_REGEX)){
            System.out.println("Enter a valid email address. E.g. someone@example.com");
            System.out.print("Email: ");
            email = scanner.nextLine();
        }

        do{
            //password = console.readPassword("Enter your password: ");
            //verification = console.readPassword("Re-enter your password: ");
            password = "test".toCharArray();
            verification = "test".toCharArray();
        }while(!Arrays.equals(password,verification));

        //Will return this if registration was successful. If not, recursive call of method.
        Seller seller = new Seller(name, email, new String(password));

        //If seller id is null and server is responsive, prompt to enter details again and retry to register
        if(seller.getUid()==null && seller.isServerAlive()){
            System.out.println("Failed to register user. Client program will prompt for registration again...");
            return createClient();
        }
        else
            return seller;
    }

    //todo check errors
    public int initialMenu(){
        Scanner scanner = new Scanner(System.in);
        try{
            System.out.println("Please choose one of the following:");
            System.out.println("1. Authenticate with server");
            System.out.println("2. Create new user account");
            System.out.println("3. Exit Program");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            if (choice < MIN_SELLER_CHOICE || choice > MAX_SELLER_CHOICE){
                System.out.println(MAIN_MENU_ERROR);
                return initialMenu();
            }
            else
                return choice;
        }
        //Tell the user that they did wrong.
        catch(InputMismatchException ime){
            System.out.println(MAIN_MENU_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());

            return initialMenu();
        }
    }

    public String emailForAuthentication(){
        Scanner scanner = new Scanner(System.in);

        System.out.println("The private key generated when you first registered will be used.");
        System.out.println("Please enter the email address you provided when you registered.");
        System.out.print("Email: ");
        String email = scanner.nextLine();
        try{
            //if provided email address does not conform to an email's syntax, then spin
            if(!detailsValidator(email, EMAIL_REGEX)){
                System.out.println("Email provided is not a valid email address");
                return null;
            }

        }
        //Tell the user that they did wrong.
        catch(InputMismatchException ime){
            System.out.println("That is not a valid input");
            if(DEBUG)
                System.out.println(ime.getMessage());

            return emailForAuthentication();
        }

        return email;
    }

    /**
     * This method is called, to ask the user which action they want to take.
     * It will be called recursively, until the user enters a valid input.
     * @return An integer constant that matches the user's choice.
     */
    public int takeSellerAction(){
        Scanner scanner = new Scanner(System.in);
        try{
            System.out.print(ACTION_USER_INPUT);

            int choice = scanner.nextInt();

            //If choice is out of the range of possible choices
            if (choice < MIN_SELLER_CHOICE || choice > MAX_SELLER_CHOICE){
                System.out.println(MAIN_MENU_ERROR);
                return takeSellerAction();
            }
            else
                return choice;
        }
        //Tell the user that they did wrong.
        catch(InputMismatchException ime){
            System.out.println(MAIN_MENU_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());

            return takeSellerAction();
        }
    }

    /**
     * Will be used to ask user to enter the item's description at runtime.
     * @return A String that will be used as an auction's description
     */
    public String getItemDescription(){
        Scanner scanner = new Scanner(System.in);
        String description = null;

        //If description entered exceeds the character limit, or if it is below the minimum characters required, spin
        while(description==null
                || description.trim().length()< ServerInterface.MIN_ITEM_DESCRIPTION_CHARS
                || description.trim().length()> ServerInterface.MAX_ITEM_DESCRIPTION_CHARS){
            System.out.print(DESCRIPTION_MSG);
            description = scanner.nextLine();
        }

        return description;
    }

    /**
     * Will be used to ask the user to enter the reserve price for an item at runtime.
     * @return The reserve price that the user has provided.
     */
    public BigDecimal getReservePrice(){

        return getPriceInput(INPUT_NOT_PRICE_ERROR, RESERVE_PRICE_INPUT, RESERVE_PRICE_MSG, PRICE_ROUNDING_WARNING);
    }

    /**
     * Asks for user input, to determine what the starting price for an item is going to be.
     * It compares the minimum acceptable price (reserve price) with the starting price and
     * returns a custom error to the user, IF the starting price is greater. Starting price
     * can be <u>AT MOST</u>, equal to the reserve price, so that the seller can get their minimum amount
     * of profit with the very first bid placed by a Buyer.
     * @param reservePrice The minimum acceptable price for an item to be sold at.
     * @return The starting price of an item.
     */
    public BigDecimal getStartingPrice(BigDecimal reservePrice){
        BigDecimal input =
                getPriceInput(INPUT_NOT_PRICE_ERROR, START_PRICE_INPUT, START_PRICE_MSG, PRICE_ROUNDING_WARNING);

        if(input.compareTo(reservePrice) > 0){
            System.out.println(START_PRICE_ERROR);
            return getStartingPrice(reservePrice);
        }

        return input;
    }

    /**
     * Will be used to close an auction. Method will return all the auctions owned by the client calling this method.
     * @return The id of the auction that will be closed.
     */
    public String chooseAuctionToClose(){
        Scanner scanner = new Scanner(System.in);
        try {
            //Get **this** seller's auctions.
            final ArrayList<Auction> myAuctions =
                    (ArrayList<Auction>) getServerReference().browseAuctionsOfSeller(this.getUid());

            //If the list provided by the server is empty, then inform user and return.
            if(myAuctions.isEmpty()){
                System.out.println(NO_ACTIVE_AUCTIONS_ERROR);
                return null;
            }
            System.out.println("\nPlease choose the number of the auction you want to close or press \"0\" to quit.");
            System.out.println(getServerReference().AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.format(getServerReference().AUCTIONS_TABLE_ATTRIBUTES_FORMAT,
                    "#","Auction ID", "Item Description", "Current Bid");
            System.out.println(getServerReference().AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.println("0. Go to previous menu");
            for (int i = 1; i <= myAuctions.size(); i++) {
                Auction a = myAuctions.get(i-1);
                System.out.format("%-3s%36s%50s%16s%n",(i+". "),
                        a.getAuctionId(),a.getDescription(),"£"+a.getCurrentBid());
            }
            System.out.println(getServerReference().AUCTIONS_TABLE_LINE_SEPARATOR);
            System.out.print("\nEnter the number(#) of your choice: ");
            //Get index of auction in List
            int choice = scanner.nextInt();

            if (choice == 0)
                return null;

            else
                return myAuctions.get(choice-1).getAuctionId();
        }
        catch (RemoteException re) {
            System.out.println(REMOTE_ERROR);
            if(DEBUG)
                System.out.println(re.getMessage());
            return null;
        }
        catch(IndexOutOfBoundsException ioobe){
            System.out.println(INDEX_OUT_OF_BOUNDS_ERROR);
            if(DEBUG)
                ioobe.printStackTrace();
            return chooseAuctionToClose();
        }
        catch (InputMismatchException ime){
            System.out.println(INPUT_NOT_NUMERICAL_ERROR);
            if(DEBUG)
                System.out.println(ime.getMessage());
            return chooseAuctionToClose();
        }
    }

    /**
     *
     * @param errorMsg This is the message to display when an exception is thrown, caused by the user's input.
     * @param inputIndication This is just an indicating message for the user to enter their input.
     * @param instructions vararg for entering numerous (more than 1) instruction, before asking for user input.
     * @return The (valid) price that the user has entered for an item. (Either staring or reserve price).
     */
    private BigDecimal getPriceInput(String errorMsg,String inputIndication, String... instructions){
        Scanner scanner = new Scanner(System.in);
        try{
            for(String s : instructions)
                System.out.print(s);
            System.out.print(inputIndication);
            BigDecimal priceInput = scanner.nextBigDecimal().setScale(2, RoundingMode.HALF_DOWN);

            if(priceInput.compareTo(new BigDecimal(0)) <= 0){
                System.out.println(MIN_PRICE_ACCEPTED_ERROR);
                return getPriceInput(errorMsg,inputIndication,instructions);
            }
            return priceInput;
        }
        catch (InputMismatchException ime){
            System.out.println(errorMsg);
            if(DEBUG)
                System.out.println(ime.getMessage());
            return getPriceInput(errorMsg,inputIndication,instructions);
        }
    }


}
