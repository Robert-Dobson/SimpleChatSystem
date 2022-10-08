import java.io.Serializable;
import java.util.ArrayList;

/**
 * Objects of this class are used to send messages between clients and the server. We use this class to encapsulate
 * all the relevant metadata needed when sending messages.
 * Metadata includes:
 * - User that sent the message
 * - User that the message is intended for (optional)
 * - Special code that indicates the purpose of the message
 * - The Message itself
 * - A list of the currently online users (optional)
 *
 * There are several special codes which indicates to client and/or server what the purpose of the message is
 * See below a list of current codes:
 * - 0: Normal message
 * - 1: Connection Request
 * - 2: Disconnection Request
 * - 10: Login request
 * - 11: Login accepted/rejected
 * - 20: Refresh Users
 *
 * This class must be serializable so objects of this type can be sent over a socket
 */
public class Message implements Serializable{
    private final int specialCode;
    private String message;
    private ArrayList<User> users;
    private User fromUser;
    private User toUser;

    /**
     * Constructor for standard messages
     * @param toUser  user the message is intended for
     * @param fromUser user the message is sent from
     * @param message message to send
     */
    public Message(User fromUser, User toUser, String message){
        this.specialCode = 0;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
    }

    /**
     * Overloaded constructor for special request messages (i.e. login requests)
     * @param specialCode  specialCode that indicates purpose of message
     * @param message message to send
     */
    public Message(int specialCode, String message){
        this.specialCode = specialCode;
        this.message = message;
    }

    /**
     * Overloaded Constructor for user refresh messages
     * @param users list of users that are currently online
     */
    public Message(ArrayList<User> users){
        this.specialCode = 20;
        this.users = users;
    }

    /**
     * Returns user details of the user that sent the message
     * @return fromUser
     */
    public User getFromUser() {
        return fromUser;
    }

    /**
     * Returns special code of the message indicating its purpose
     * @return  special code
     */
    public int getSpecialCode() {
        return specialCode;
    }

    /**
     * Returns text message contained in this message
     * @return  message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns user details of the user that the message is for
     * @return  toUser
     */
    public User getToUser() {
        return toUser;
    }

    /**
     * Returns arraylist of currently online users
     * @return  arraylist of users
     */
    public ArrayList<User> getUsers() {
        return users;
    }
}
