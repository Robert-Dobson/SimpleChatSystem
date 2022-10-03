import java.io.Serializable;
import java.util.ArrayList;

// Must be serializable so objects of this type can be sent over a socket
public class Message implements Serializable{
    User fromUser;
    User toUser;
    /*
        There are several special codes which indicates to client and/or server what the purpose of the message is
        See below a list of current codes:
        - 0: Normal message
        - 1: Connection Request
        - 2: Disconnection Request
        - 10: Login request
        - 11: Login accepted/rejected
        - 20: Refresh Users
     */
    int specialCode;
    String message;
    ArrayList<User> users;

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
     * Constructor for special request messages
     * @param specialCode  specialCode that indicates purpose of message
     * @param message message to send
     */
    public Message(int specialCode, String message){
        this.specialCode = specialCode;
        this.message = message;
    }

    /**
     * Constructor for user refresh messages
     * @param users
     */
    public Message(ArrayList<User> users){
        this.specialCode = 20;
        this.users = users;
    }
}
