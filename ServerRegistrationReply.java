import java.io.Serializable;
import java.util.UUID;

/**
 * This class represents a reply from the server, when a client initially connects and tries to register.
 * @author Petros Soutzis
 */
public class ServerRegistrationReply implements Serializable {

    private boolean success;/*indicates success of client registration*/
    private String msg;/*message from the server*/
    private String uid;/*the id given to the client*/

    /**
     * Constructor will set success to true initially and assign a random UUID.
     */
    public ServerRegistrationReply()
    {
        this.success = true;
        this.uid = UUID.randomUUID().toString();
    }

    /**
     * @return if registration is successful
     */
    public boolean isSuccessful() {

        return success;
    }

    /**
     * @return the server's text reply
     */
    public String getMsg() {

        return msg;
    }

    /**
     * @return the id assigned to user
     */
    public String getUid() {

        return uid;
    }

    /**
     * @param success is what the isSuccess variable will be changed to. If it is FALSE, it will set uid to null.
     */
    public void setSuccess(boolean success) {

        this.success = success;
        if(!success)
            this.uid = null;
    }

    /**
     * @param msg Is what the server's text reply will be
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }
}
