public class Buyer extends Client {

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

    private Buyer(String name, String email){

        super(name, email);
    }
}
