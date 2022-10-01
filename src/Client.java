import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class Client{
    // Address and port of server we're connecting to
    private String address;
    private int port;
    Socket serverSocket;

    // SSH attributes
    private JSch sshManager;
    Session session = null;

    // Client Details
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
            // Open an output object stream to the server, add the message to the stream, then send it
            ObjectOutputStream outStream = new ObjectOutputStream(serverSocket.getOutputStream());
            outStream.writeObject(message);
            outStream.flush();
            System.out.println(message.specialCode);
        }
        catch (IOException e){
            e.printStackTrace();
        }
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
            listeningThread = new Thread(listener);
            listeningThread.start();
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
