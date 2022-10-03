import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerResponse implements Runnable{
    // Socket attributes
    private ServerSocket mySocket;
    private Socket clientSocket;
    private final Server serverObject;
    private Boolean socketActive = true;

    // Streams to and from client
    ObjectInputStream clientIn;
    ObjectOutputStream clientOut;

    // Client Details
    private String name;
    private int userID;
    private User userDetails;


    /**
     * Constructor to create ServerResponse Object
     * @param mySocket  the ServerSocket of the server
     * @param clientSocket  the socket connected to the client
     * @param serverObject  the server object/instantiation, so we can access Server methods
     */
    public ServerResponse(ServerSocket mySocket, Socket clientSocket, Server serverObject){
        this.mySocket = mySocket; // Get the ServerSocket
        this.clientSocket = clientSocket; // Get the server connected to the client
        this.serverObject = serverObject; // Get the object of server
    }

    /**
     * Reads any messages from the client and executes the appropriate response method
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
            e.printStackTrace();
        }
        while (socketActive) {
            try {
                // Recieve message from client, this is a blocking call
                Message message = (Message) clientIn.readObject();

                if (message != null) {
                    // Decide how to respond to message based off the special code
                    int specialCode = message.specialCode;
                    switch (specialCode) {
                        // Normal message received
                        case 0:
                            // Pass message onto correct user
                            directMessageResponse(message);
                            break;

                        // Disconnect Request Received
                        case 2:
                            // Send request recieved message to client
                            this.socketActive = false;
                            serverObject.removeUser(this.userID, this.clientSocket);
                            // Update Users
                            sendUsers();
                            break;

                        // Login Request Received
                        case 10:
                            LoginResponse(message);
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
                e1.printStackTrace();
            }

        }
        // Close input and output stream
        try {
            clientIn.close();
            clientOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message to the requested client
     * @param requestReply  message to send to client
     * @param sendSocket socket in which to send the message to
     */
    public void sendMessage(Message requestReply, SocketInfo sendSocket){
        try{
            // Get output stream from socket Info
            ObjectOutputStream sendOut = sendSocket.outputStream;
            // Send message to the client using the output stream
            sendOut.writeObject(requestReply);
            sendOut.flush();
        } catch (IOException e) {
           // Socket Closed
            System.out.println("Failed to send");
        }
    }

    public void directMessageResponse(Message directMessage){
        // Get list of online client's sockets and userID to send the message to
        ArrayList<SocketInfo> users = this.serverObject.getClientSockets();
        User sendToUser = directMessage.toUser;
        SocketInfo sendToSocket = null;

        // Find socket to send the message to
        for (SocketInfo user: users){
            if (sendToUser.uniqueID == user.clientID){
                sendToSocket = user;
                break;
            }
        }

        // Send message to socket if user has been found
        if (sendToSocket != null){
            sendMessage(directMessage, sendToSocket);
        }
    }

    /**
     * Registers the login attempt and sends back a unique user id
     * @param message   message from client containing login attempt info
     */
    public void LoginResponse(Message message){
        // Add username and user ID
        this.name = message.message;
        this.userID = serverObject.getUserID();
        this.userDetails = new User(userID, name);

        String messageContents = Integer.toString(userID);

        // Create socket info object
        SocketInfo socketInfo = new SocketInfo(userDetails.uniqueID, this.clientSocket, this.clientOut);

        // Add current user to users
        // Lock on server object to ensure that the list isn't sent by other threads before the user is added
        synchronized (serverObject){
            serverObject.addUser(this.userDetails, socketInfo);
            serverObject.notify();
        }

        // Create and send response message to user
        Message requestReply = new Message(11, messageContents);
        sendMessage(requestReply, socketInfo);

        // Add to log
        System.out.println("New User (ID: "+ this.userID  + ", Name:" + this.name + ")");

        // Send list of connected users to Client
        sendUsers();
    }

    /**
     * Send list of currently online users to all clients
     */
    public void sendUsers(){
        // Get array list of users and send to every user
        // Lock on server object to ensure that the list isn't sent by other threads before the user is added
        ArrayList<User> users;
        ArrayList<SocketInfo> sockets;
        synchronized (serverObject){
            users = serverObject.getUsers();
            sockets = serverObject.getClientSockets();
            serverObject.notify();
        }

        // Create shallow copy of users to fix bug where clients keep their old version of this array
        ArrayList<User> newUsers = new ArrayList<User>();
        newUsers.addAll(users);

        Message requestReply = new Message(newUsers);
        for (SocketInfo socket: sockets){
            if (socket != null){
                sendMessage(requestReply, socket);
            }
        }
    }
}
