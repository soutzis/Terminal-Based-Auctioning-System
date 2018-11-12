import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Seller extends Client {

    private ArrayList<String> ownedAuctions;
    public static final int MAX_SELLER_CHOICE = 3; /*maximum number of choices a seller client can take*/
    public static final int MIN_SELLER_CHOICE = 1;/*minimum number of choices a seller client can take*/
    public static final int CREATE_AUCTION = 1;/*if user selects this, they will be prompted to create a new auction*/
    public static final int CLOSE_AUCTION = 2;/*if user selects this, they can choose one of their auctions to close*/
    public static final int EXIT_PROGRAM = 3; /*if the user selects this, the client will terminate*/

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

    private Seller(String name, String email){

        super(name, email);
        ownedAuctions = new ArrayList<>();

    }

    public boolean emailValidator(String email){
        if(email == null)
            return false;

        String validEmailRegEx = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(validEmailRegEx);

        return pattern.matcher(email).matches();
    }

    public void addToOwnedAuctions(String auctionID){

        if (!ownedAuctions.contains(auctionID))
            ownedAuctions.add(auctionID);
    }

    public void removeFromOwnedAuctions(String auctionID){

        ownedAuctions.remove(auctionID);
    }

    public Seller createSellerClient(){
        Scanner scanner = new Scanner(System.in);
        String name, email = null;
        System.out.println("A new seller client will be created.\nPlease enter the required details.");
        System.out.print("Name: ");
        name = scanner.nextLine();
        while(!emailValidator(email)){
            System.out.println("Enter a valid email address. E.g. someone@example.com");
            System.out.print("Email: ");
            email = scanner.nextLine();
        }

        return new Seller(name,email);
    }
    public int takeSellerAction(){
        Scanner scanner = new Scanner(System.in);
        try{
            System.out.print("\nWhat do you want to do? (Enter number of your choice. " +
                    "E.g. To create auction, enter the number 1)"
                    +"\n1. Create Auction"
                    +"\n2. Close an Auction"
                    +"\n3. Exit Program"
                    +"\nChoice = ");

            return scanner.nextInt();
        }
        catch(InputMismatchException ime){
            System.out.println("\nPLEASE ENTER A NUMBER IN THE RANGE OF THE VALUES " +
                    MIN_SELLER_CHOICE+" - "+MAX_SELLER_CHOICE);

            return -1;
        }
    }

    public String getItemDescription(){
        Scanner scanner = new Scanner(System.in);
        String description = null;
        int minChars = serverReference.MIN_ITEM_DESCRIPTION_CHARACTERS;
        int maxChars = serverReference.MAX_ITEM_DESCRIPTION_CHARACTERS;

        while(description==null
                || description.trim().length()<minChars
                || description.trim().length()>maxChars){
            System.out.print("\nPlease enter the description of the item you want to sell. " +
                    "E.g: \"A red pen. A pen that has red ink!\""
                    +"\nYour description must be between "+minChars+" - "+maxChars
                    +" characters long (whitespace does not count)."
                    +"\nDescription: ");
            description = scanner.nextLine();
        }

        return description;
    }

    public BigDecimal getItemStartingPrice(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nPlease enter the price that you want the auction to start at."
                +"\nI.e. An item has a value of £20, a good starting price would be £5, " +
                "which is the 25% of the item's value."
                +"\nStarting Price: ");

        return scanner.nextBigDecimal();
    }

    public BigDecimal getReservePrice(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nPlease enter the MINIMUM price that the item can be sold at."
                +"\nThis price is a secret to bidders. There will be no winner if this price is not reached."
                +"\nReserve Price: ");

        return scanner.nextBigDecimal();
    }

    public String chooseAuctionToClose(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nPlease choose the number of the auction you want to close:");
        for(int i = 0; i<ownedAuctions.size(); i++){
            System.out.println(i+". "+ownedAuctions.get(i));
        }
        int chosenId = scanner.nextInt();

        return ownedAuctions.get(chosenId);
    }


}
