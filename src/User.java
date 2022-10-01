import java.io.Serializable;

// Must be serializable so objects of this type can be sent over a socket
public class User implements Serializable {
    int uniqueID;
    String name;

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
     * The toString of a user just shows its name.
     * @return
     */
    @Override
    public String toString() {
        return name;
    }
}
