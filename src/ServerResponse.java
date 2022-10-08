import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Deals with any messages sent from the client and provides the relevant response. This is run as a thread for each
 * client to allow multiple client to interact with the server at once.
 */
public class ServerResponse implements Runnable{
    // Socket attributes
    private final Socket clientSocket;
    private final Server serverObject;
    private Boolean socketActive = true;

    // Object streams to and from client
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    private int userID;

    /**
     * Constructor to create ServerResponse Object
     * @param clientSocket  the socket connected to the client
     * @param serverObject  the server object/instantiation, so we can access Server methods
     */
    public ServerResponse(Socket clientSocket, Server serverObject){
        this.clientSocket = clientSocket; // Get the server connected to the client
        this.serverObject = serverObject; // Get the object of server
    }

    /**
     * Reads any messages received from the client and executes the appropriate response method
     */
    @Override
    public void run() {
        // Accept a connection from a client
        System.out.println("Server accepted connection from new client");
        // Set up the ability to receive and send messages to the client
        try {
            clientIn = new ObjectInputStream(clientSocket.getInputStream());
            clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
           System.out.println("ERROR: Failed to create input and output streams to new client");
           this.socketActive = false;
        }

        // While socket is active, wait for messages from client, respond then repeat
        while (socketActive) {
            try {
                // Receive message from client, this is a blocking call
                Message message = (Message) clientIn.readObject();

                if (message != null) {
                    // Decide how to respond to message based off the special code
                    int specialCode = message.getSpecialCode();
                    switch (specialCode) {
                        // Normal message received
                        case 0:
                            // Pass message onto correct user
                            directMessageResponse(message);
                            break;

                        // Disconnect Request Received
                        case 2:
                            // Remove client from server list
                            this.socketActive = false;
                            serverObject.removeUser(this.userID, this.clientSocket);

                            // Update Users to all other clients
                            sendUsers();
                            break;

                        // Login Request Received
                        case 10:
                            loginResponse(message);
                            break;
                    }
                }
            } catch (IOException e){
                // The socket is closed, remove user
                this.socketActive = false;
                serverObject.removeUser(this.userID, this.clientSocket);
                // Update online users
                sendUsers();
            } catch (ClassNotFoundException e1) {
                System.out.println("ERROR: Message class is missing");
            }

        }
        // Close input and output stream
        try {
            clientIn.close();
            clientOut.close();
        } catch (IOException e) {
            System.out.println("ERROR: Failed to close input and output streams of client");
        }
    }

    /**
     * Send a message to the requested client.
     * @param requestReply  message to send to client
     * @param sendSocket socket in which to send the message to
     */
    private void sendMessage(Message requestReply, SocketInfo sendSocket){
        try{
            // Get output stream of client we're sending to from socket info
            ObjectOutputStream sendOut = sendSocket.getOutputStream();

            // Send message to the client using the output stream
            sendOut.writeObject(requestReply);
            sendOut.flush();
        } catch (IOException e) {
           // Socket Closed
            System.out.println("ERROR: Failed to send message to user " + sendSocket.getClientID());
        }
    }

    /**
     * Process request for a direct message and forward the message onto the relevant user
     * @param directMessage message to pass onto user
     */
    private void directMessageResponse(Message directMessage){
        // Get list of online client's sockets and the userID to send the message to
        ArrayList<SocketInfo> users = this.serverObject.getClientSockets();
        User sendToUser = directMessage.getToUser();
        SocketInfo sendToSocket = null;

        // Find socket of the user we want to send the message to
        for (SocketInfo user: users){
            if (sendToUser.getUniqueID() == user.getClientID()){
                sendToSocket = user;
                break;
            }
        }

        // Send message to the socket if user has been found
        if (sendToSocket != null){
            sendMessage(directMessage, sendToSocket);
        }
        else{
            // Couldn't find user
            System.out.println("ERROR: Couldn't find user " + sendToUser.getUniqueID() + " so failed to send message");
        }
    }

    /**
     * Registers the login attempt and sends back to the client a unique user id
     * @param message   message from client containing login attempt info
     */
    private void loginResponse(Message message){
        // Create user object to hold the clients details
        String name = message.getMessage();
        this.userID = serverObject.getUserID();
        User userDetails = new User(userID, name);

        // Create socket info object
        SocketInfo socketInfo = new SocketInfo(userDetails.getUniqueID(), this.clientSocket, this.clientOut);

        // Add current user to users
        // Lock on server object to ensure that the list isn't sent by other threads before the user is added
        synchronized (serverObject){
            serverObject.addUser(userDetails, socketInfo);
            serverObject.notify();
        }

        // Create and send response message to user
        String messageContents = Integer.toString(userID);
        Message requestReply = new Message(11, messageContents);
        sendMessage(requestReply, socketInfo);

        // Add to log
        System.out.println("New User (ID: "+ this.userID  + ", Name:" + name + ")");

        // Send list of connected users to all current clients (to keep their list up to date with the new addition)
        sendUsers();
    }

    /**
     * Send list of currently online users to all clients
     */
    private void sendUsers(){
        // Get array list of users and send to every user
        // Lock on server object to ensure that the list isn't sent by other threads before the user is added
        ArrayList<User> users;
        ArrayList<SocketInfo> sockets;
        synchronized (serverObject){
            users = serverObject.getUsers();
            sockets = serverObject.getClientSockets();
            serverObject.notify();
        }

        // Create shallow copy of users to fix bug where clients will keep their old version of this array rather than
        // update with the new version
        ArrayList<User> newUsers = new ArrayList<User>(users);

        // Send list of users to each online user
        Message requestReply = new Message(newUsers);
        for (SocketInfo socket: sockets){
            if (socket != null){
                sendMessage(requestReply, socket);
            }
        }
    }
}
