import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This class is the entry point of the Server's side of the program.
 * This class handles:
 *  - Starting the server and listening for new clients
 *  - Starting a ServerResponse thread for each new client to respond to any messages received from that client
 *  - Maintain a list of currently open sockets
 *  - Maintain a list of currently online users
 */
public class Server {
    // Server details and list of client sockets
    private final int port = 34752;
    private ArrayList<SocketInfo> clientSockets = new ArrayList<>();

    // List of users and their ids and a tracker for id so no user is given the same id
    private ArrayList<User> onlineUsers = new ArrayList<>();
    private int currentUserID = -1;

    /**
     * Repeatedly checks for new clients and creates a ClientListener thread to deal with that client
     */
    public void go() {
        // Throughout we keep a log in the standard input (i.e. terminal)
        System.out.println("Server is listening...");
        boolean firstMessage = true;

        // Repeatedly looks for new clients
        while(true){
            // Open with-resources a new serverSocket on the port, this starts the server
            try(ServerSocket mySocket = new ServerSocket(port)) {
                if (firstMessage){
                    System.out.println("Server is running on port, "+ mySocket.getLocalPort());
                    firstMessage = false;
                }

                // Accept a connection from a client, returning the clientSocket
                // This is a blocking call. This will wait here until a client connects
                Socket clientSocket = mySocket.accept();
                System.out.println("Receiving new connection request");

                // Create a new serverResponse object and create a thread for it to deal with client
                ServerResponse serverResponse = new ServerResponse(clientSocket, this);
                Thread respond = new Thread(serverResponse);

                // Start thread (no need for join as we're in a while(true) loop)
                respond.start();
            } catch (IOException e) {
                // Failed to open socket
                System.out.println("ERROR: Failed to start server, failure opening new server socket");
            }
        }

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
     * Add a new user to online users and store socket info
     * @param user  user details
     * @param clientSocketInfo  socket information for client
     */
    public void addUser(User user, SocketInfo clientSocketInfo){
        onlineUsers.add(user);
        clientSockets.add(clientSocketInfo);
    }

    /**
     * Returns list of currently online users (along with their details)
     * @return arrayList of online users
     */
    public ArrayList<User> getUsers(){
        return onlineUsers;
    }

    /**
     * Returns list of each client socket (and the userids of the clients)
     * @return arrayList of each user's id and socket
     */
    public ArrayList<SocketInfo> getClientSockets() {
        return clientSockets;
    }

    /**
     * Remove user from online users and their socket, the user has disconnected
     * @param uniqueID userID of the user to remove
     * @param clientSocket socket of the user we're removing
     */
    public void removeUser(int uniqueID, Socket clientSocket){
        // Remove user (based off unique ID) from online users list
        String name = "Unknown User";
        for (User user: this.onlineUsers){
            if (user.getUniqueID() == uniqueID){
                name = user.getName();
                this.onlineUsers.remove(user);
                break;
            }
        }

        // Remove client socket from list
        for (SocketInfo socket: this.clientSockets){
            if (socket.getClientSocket() == clientSocket){
                this.clientSockets.remove(socket);
                break;
            }
        }

        System.out.println("User ID "+ uniqueID + " (" + name + ") has disconnected");
    }

    public static void main(String[] args) {
        Server myServer = new Server();
        myServer.go();
    }

}
