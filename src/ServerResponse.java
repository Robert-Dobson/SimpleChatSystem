import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerResponse implements Runnable{
    // Socket attributes
    private ServerSocket mySocket;
    private Socket clientSocket;
    private Server serverObject;
    private Boolean socketActive = true;

    // Streams to and from client
    ObjectInputStream clientIn;
    ObjectOutputStream clientOut;

    // Client Details
    private String name;
    private int userID;


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
        while (socketActive){
            try {
                // Set up the ability to receive and send messages to the client
                clientIn = new ObjectInputStream(clientSocket.getInputStream());
                clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
                try{
                    // Recieve message from client, this is a blocking call
                    Message message = (Message) clientIn.readObject();

                    if (message != null){
                        // Decide how to respond to message based off the special code
                        int specialCode = message.specialCode;
                        switch(specialCode){
                            // Disconnect Request Received
                            case 2:
                                // Send request recieved message to client
                                this.socketActive = false;
                                serverObject.removeUser(this.userID, this.clientSocket);
                                break;

                            // Login Request Received
                            case 10:
                                LoginResponse(message);
                                break;
                        }
                    }
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                // The socket is closed, remove user
                this.socketActive = false;
                serverObject.removeUser(this.userID, this.clientSocket);
            }
        }
    }

    /**
     * Send a message back to the client
     * @param requestReply  message to send to client
     */
    public void sendMessage(Message requestReply){
        try {
            // Send message to the client using the output stream
            clientOut.writeObject(requestReply);
            clientOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the login attempt and sends back a unique user id
     * @param message   message from client containing login attempt info
     */
    public void LoginResponse(Message message){
        // Add username
        this.name = message.message;

        // Get unique user id
        userID = serverObject.getUserID();
        String messageContents = Integer.toString(userID);

        // Create and send response message to user
        Message requestReply = new Message(11, messageContents);
        sendMessage(requestReply);

        // Add current user to users
        serverObject.addUser(userID, name);

        // Send list of connected users to Client
        sendUsers();

        // Add to log
        System.out.println("New User (ID: "+ this.userID  + ", Name:" + this.name + ")");
    }

    /**
     * Send list of currently online users to the client
     */
    public void sendUsers(){
        // Get array list of users and send
        ArrayList<User> users = serverObject.getUsers();
        Message requestReply = new Message(users);
        sendMessage(requestReply);
    }
}
