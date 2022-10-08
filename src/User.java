import java.io.Serializable;

/**
 * Encapsulates a users id and their name together
 * Must be serializable so objects of this type can be sent over a socket
 */
public class User implements Serializable {
    private final int uniqueID;
    private final String name;

    /**
     * Constructor for a complex data type to hold user info
     * @param uniqueID  ID of user
     * @param name  name of user
     */
    public User(int uniqueID, String name){
        this.uniqueID = uniqueID;
        this.name = name;
    }

    /**
     * Ensure the toString method only shows the name of the user, this is so the combobox for selecting users doesn't
     * show userID
     * @return username
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns the unique id of the user
     * @return  uniqueID
     */
    public int getUniqueID(){
        return uniqueID;
    }

    /**
     * Returns the username of the user
     * @return  username
     */
    public String getName() {
        return name;
    }
}
