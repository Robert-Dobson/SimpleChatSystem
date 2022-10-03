import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client{
    // Address and port of server we're connecting to
    private String address;
    private int port;
    Socket serverSocket;
    private ObjectOutputStream outStream;

    // SSH attributes
    private JSch sshManager;
    Session session = null;

    // Client Details
    User clientDetails;
    String username;
    int userID;

    // Other Users
    ArrayList<User> users;

    // Client Listener thread
    Thread listeningThread;

    // UI object
    ChatSessionView UI;

    /**
     * Constructor to create Client Object
     *
     * @param add  the address of the server we're going to connect to
     * @param port the port of the server we're going to connect to
     */
    public Client(String add, int port) {
        this.address = add;
        this.port = port;

        // Port forward to linux bath server
        this.PortForward();

        // Draw UI
        UI = new ChatSessionView(this);
        UI.draw();

        // Connect to Server and start listening for responses
        this.startListening();

        // Start login dialog
        UI.runLoginDialog();
    }

    /**
     * Send a login request to the server
     * @param username name of the user
     */
    public void login(String username) {
        // Update username on Client side
        this.username = username;

        // Send message object containing login request to server
        Message loginMessage = new Message(10, username);
        sendMessage(loginMessage);
    }

    /**
     * Send a login request to the server
     * @param message the message recieved from the server
     */
    public void loginRecieved(String message){
        // If successful, the message will be the client's unique userID, retrieve it
        if (message != null){
            try {
                userID = Integer.parseInt(message);
            }
            catch (NumberFormatException ex){
                ex.printStackTrace();
            }
        }

        // Create user from id and username
        clientDetails = new User(userID, username);
    }

    /**
     * Add message received to corresponding the text file and if currently selected user request GUI to refresh page
     * @param message   message recieved from another user (contains text and user info)
     */
    public void messageReceived(Message message, boolean recieved){
        // Create a new anonymous inner thread to read the message and write to file (to avoid hang ups)
        Thread response = new Thread(){
            public void run(){
                // Get file for user we received the message from (or create one if it doesn't exist)
                User fromUser = message.fromUser;
                File userFile = new File(System.getProperty("user.dir") + "/Data/" + clientDetails.uniqueID + "-" + fromUser.uniqueID + "messages.txt");
                if (!userFile.exists()){
                    try {
                        userFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Add new message to the file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(userFile, true))){
                    // Decide whether the message is from us or the sender
                    if (recieved){
                        bw.write(fromUser.name + ": ");
                    }
                    else{
                        bw.write("You: ");
                    }

                    bw.write(message.message);
                    bw.newLine();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }

                // Ask chat session view to refresh
                UI.refreshText(fromUser.uniqueID);
            }
        };
        response.start();
    }

    /**
     * Update list of other users online both in Client and in the GUI
     * @param newUsers  List of users currently online
     */
    public void updateUsers(ArrayList<User> newUsers){
        this.users = newUsers;
        UI.updateOnlineUsers(newUsers);
    }

    /**
     * Send a message to the server
     * @param message message to send to the server
     */
    public void sendMessage(Message message){
        try{
            // Send message to the server using the output object stream
            outStream.writeObject(message);
            outStream.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Send a message to specified userID containing given text
     * @param text  text to send to the user
     * @param toUser  user to send the message to
     */
    public void sendMessageToUser(String text, User toUser){
        // Create new message to send to recipient via the server
        Message message = new Message(this.clientDetails, toUser, text);
        sendMessage(message);

        // Write message to local file and refresh UI
        // To do this we will manipulate the message slightly to pretend the message is from the receiver to ensure
        // the message is written to the correct file. And set a flag to change the name to "You:" in the text
        Message editedMessage = new Message(toUser, this.clientDetails, text);
        messageReceived(editedMessage, false);

    }

    /**
     * This connects to the server and starts a clientListener thread to receive any messages from the server.
     */
    public void startListening() {
        // Try-with resources: open a socket to connect to server
        try {
            // Create new ClientListener object to receive messages from the server
            this.serverSocket = new Socket(address, port);
            ClientListener listener = new ClientListener(serverSocket, this);
            this.listeningThread = new Thread(listener);
            listeningThread.start();
            this.outStream = new ObjectOutputStream(serverSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This sets up ssh local port forwarding to redirect any traffic sent to localhost port 14002 to
     * linux3.bath.ac.uk port 34752
     */
    public void PortForward() {
        // Use JSch to connect to linux3.bath.ac.uk with ssh
        sshManager = new JSch();
        try {
            sshManager.addIdentity(System.getProperty("user.dir") + "/Data/id_rsa");
            sshManager.setKnownHosts(System.getProperty("user.dir") + "/Data/known_hosts");
            session = sshManager.getSession("rhdd20", "linux3.bath.ac.uk", 22);
            session.connect();

            // Set up port forwarding so any data we send to localhost port is redirected to the linux server
            boolean connected = false;
            while (connected != true) {
                try {
                    session.setPortForwardingL(this.port, this.address, 34752);
                    connected = true;
                } catch (JSchException e) {
                    this.port++;
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
        }

    }

    /**
     * Disconnects from the ssh session, deleting the local port forwarding
     */
    public void Disconnect() {
        // Disconnect port forwarding
        try {
            // If socket isn't already closed
            if (serverSocket.isConnected()){
                // Inform server of disconnect
                Message disconnectRequest = new Message(2, Integer.toString(this.userID));
                sendMessage(disconnectRequest);

                // Disconnect socket
                serverSocket.close();

                // Disconnect session
                session.disconnect();
                session.delPortForwardingL(port);
            }
            System.out.println("Disconnect"); //Remove

        } catch (JSchException | IOException e) {
            // Session already deleted, don't need to do anything
            ;
        }

        // Exit program
        System.exit(0);
    }

    // Create client and sets up connection to server
    public static void main(String[] args) {
        // Create a new client object to connect to localhost:14002
        Client myClient = new Client("localhost", 14002);
    }



}
