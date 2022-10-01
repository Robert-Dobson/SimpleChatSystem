import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private int port = 34752; // Port the server is set-up on
    private ArrayList<Socket> clientSockets = new ArrayList<Socket>(); // ArrayList of every client connected to the server

    // List of users and their ids and a tracker for id
    private ArrayList<User> onlineUsers = new ArrayList<>();
    private int currentUserID = -1;

    /**
     * Repeatedly check for new clients and creates a ClientListener thread to deal with that client
     */
    public void go() {
        System.out.println("Server is listening...");
        boolean firstMessage = true;

        // Repeatedly look for new clients
        while(true){
            // Open with-resources a new serverSocket on the port
            try(ServerSocket mySocket = new ServerSocket(port)) {
                if (firstMessage){
                    System.out.println("Server is running on port, "+ mySocket.getLocalPort());
                    firstMessage = false;
                }

                // Accept a connection from a client, returning the clientSocket
                // This is a blocking call. This will wait here until a client connects
                Socket clientSocket = mySocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Receiving new connection request");

                // Create a new serverResponse object and create a thread for it to deal with client
                ServerResponse serverResponse = new ServerResponse(mySocket, clientSocket, this);
                Thread respond = new Thread(serverResponse);

                // Start thread (no need for join as we're in a while(true) loop)
                respond.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Gets the sockets of all the clients we're connected to
     * @return an ArrayList of all clients connected to the server
     */
    public ArrayList<Socket> getClientSockets(){
        return clientSockets;
    }

    /**
     * Get a new unique user id for the new user (for now just increments a counter)
     * @return  an unique user ID
     */
    public int getUserID(){
        currentUserID++;
        return currentUserID;
    }

    /**
     * Add a new user to online users
     * @param uniqueID  userID of the user
     * @param name  name of the user
     */
    public void addUser(int uniqueID, String name){
        onlineUsers.add(new User(uniqueID, name));
    }

    /**
     * @return arrayList of online users
     */
    public ArrayList<User> getUsers(){
        return onlineUsers;
    }

    /**
     * Remove user from online users and their socket
     * @param uniqueID userID of the user
     * @param clientSocket socket of the user we're removing
     */
    public void removeUser(int uniqueID, Socket clientSocket){
        // Remove user (based off unique ID) from online users list
        String name = "Unknown User";
        for (User user: this.onlineUsers){
            if (user.uniqueID == uniqueID){
                name = user.name;
                this.onlineUsers.remove(user);
                break;
            }
        }

        // Remove client socket from list
        this.clientSockets.remove(clientSocket);
        
        System.out.println("User ID "+ Integer.toString(uniqueID) + " (" + name +") has disconnected");
    }

    public static void main(String[] args) {
        Server myServer = new Server();
        myServer.go();
    }

}
