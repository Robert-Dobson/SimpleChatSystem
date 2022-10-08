import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This class is the entry point of the Client's side of the program.
 * This class handles:
 * - Connecting to the server
 * - Sending messages to the server
 * - Responding to the messages received from the server
 * - Setting up a ClientListener thread to monitor for messages from the server
 * - Setting up the ChatSessionView to manage the GUI
 */
public class Client{
    // Address and port of server we're connecting to
    private final String address;
    private int port;
    private Socket serverSocket;
    private ObjectOutputStream outStream;

    private Session session = null;

    // Client Details and GUI details
    private User clientDetails;
    private String username;
    private int userID;
    private ChatSessionView GUI;

    /**
     * Constructor to create Client Object
     * @param add  the address of the server we're going to connect to
     * @param port the port of the server we're going to connect to
     */
    public Client(String add, int port) {
        this.address = add;
        this.port = port;
    }

    /**
     * Commence connection to the server of the chat system and setup and start the GUI
     */
    public void startClient(){
        // Port forward to linux bath server then connect to server and start listening for responses
        this.portForward();
        this.startListening();

        // Draw GUI and start login dialog
        GUI = new ChatSessionView(this);
        GUI.draw();
        GUI.runLoginDialog();
    }

    /**
     * Send a login request to the server
     * @param username name of the user
     */
    public void login(String username) {
        // Update username on Client side
        this.username = username;

        // Send message object containing login request (with username) to server
        Message loginMessage = new Message(10, username);
        sendMessage(loginMessage);
    }

    /**
     * Respond to log in request return message from the server
     * @param message the message received from the server
     */
    public void loginReceived(String message){
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
     * Add message received to the corresponding text file and if the message is for the user whose messages are
     * currently displayed, request the GUI to refresh the page
     * @param message   message received from another user (contains text and user info)
     */
    public void messageReceived(Message message, boolean received){
        // Create a new anonymous inner thread to read the message and write to file (to avoid hang-ups)
        Thread writeToFile = new Thread(){
            public void run(){
                // Get file for user we received the message from (or create one if it doesn't exist)
                User fromUser = message.getFromUser();
                int fromUserID = fromUser.getUniqueID();
                int clientID = clientDetails.getUniqueID();
                String path = System.getProperty("user.dir") + "/Data/" + clientID + "-" + fromUserID + "messages.txt";
                File userFile = new File(path);

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
                    if (received){
                        bw.write(fromUser.getName() + ": ");
                    }
                    else{
                        bw.write("You: ");
                    }

                    bw.write(message.getMessage());
                    bw.newLine();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }

                // Ask chat session view to refresh
                GUI.refreshText(fromUser.getUniqueID());
            }
        };

        // Start thread to write message received to file
        writeToFile.start();
    }

    /**
     * Update list of other users online in the GUI
     * @param newUsers  List of users currently online
     */
    public void updateUsers(ArrayList<User> newUsers){
        GUI.updateOnlineUsers(newUsers);
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
        // To do this we will manipulate the message slightly to pretend the message is from the user you're sending
        // the message to, to ensure the message is written to the correct file. And set a flag to change the name to
        // "You:" in the text
        Message editedMessage = new Message(toUser, this.clientDetails, text);
        messageReceived(editedMessage, false);
    }

    /**
     * Disconnects from the ssh session, deleting the local port forwarding. Also remove all messages saved on the
     * computer
     */
    public void disconnect() {
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

        } catch (JSchException | IOException e) {
            // Session already deleted, don't need to do anything
        }

        // Remove all message files (to avoid issues rerunning the program
        File messageDirectory = new File(System.getProperty("user.dir") + "/Data/");
        if (messageDirectory.isDirectory()){
            File[] files = messageDirectory.listFiles();
            if (files != null){
                for (File f: files){
                    if (f.getName().endsWith("messages.txt") && f.getName().startsWith(Integer.toString(userID))){
                        f.delete();
                    }
                }
            }
        }

        // Exit program
        System.exit(0);
    }

    /**
     * Returns the clientDetails (containing name and id) of this client
     * @return  Details of client
     */
    public User getClientDetails(){
        return clientDetails;
    }

    /**
     * This sets up ssh local port forwarding to redirect any traffic sent to localhost port 14002 to linux3.bath.ac.uk
     * port 34752
     */
    private void portForward() {
        // Use JSch to connect to linux3.bath.ac.uk via ssh
        JSch sshManager = new JSch();
        try {
            // Uses my login details to connect to my user profile on the SSH server
            // This will have to be changed to whatever server you run the chat system on
            sshManager.addIdentity(System.getProperty("user.dir") + "/Data/id_rsa");
            sshManager.setKnownHosts(System.getProperty("user.dir") + "/Data/known_hosts");
            session = sshManager.getSession("rhdd20", "linux3.bath.ac.uk", 22);
            session.connect();

            // Set up port forwarding so any data we send to localhost port is redirected to the linux server
            boolean connected = false;
            while (!connected) {
                try {
                    session.setPortForwardingL(this.port, this.address, 34752);
                    connected = true;
                } catch (JSchException e) {
                    // If localhost port is in use try next port
                    this.port++;
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
        }

    }

    /**
     * This connects to the server and starts a clientListener thread to receive any messages from the server.
     */
    private void startListening() {
        try {
            // Create new socket to communicate with the server
            this.serverSocket = new Socket(address, port);

            // Create new ClientListener object to receive messages from the server
            ClientListener listener = new ClientListener(serverSocket, this);
            // Client Listener thread and UI object
            Thread listeningThread = new Thread(listener);
            listeningThread.start();

            // Create an output object stream to write messages to the server
            this.outStream = new ObjectOutputStream(serverSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message to the server
     * @param message message to send to the server
     */
    private void sendMessage(Message message){
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
     * Create client and sets up connection to server
     */
    public static void main(String[] args) {
        // Create a new client object to connect to localhost:14002
        Client myClient = new Client("localhost", 14002);
        myClient.startClient();
    }



}
