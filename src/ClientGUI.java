import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI implements ActionListener {
    // Address and port of server we're connecting to
    private String address;
    private int port;
    Socket serverSocket;

    // SSH attributes
    private JSch sshManager;
    Session session = null;

    // GUI Attributes
    private JFrame mainFrame;
    private int height = 500;
    private int width = 900;
    private JTextArea mainText;
    private JTextField userEntry;

    /**
     * Constructor to create Client Object
     * @param add   the address of the server we're going to connect to
     * @param port  the port of the server we're going to connect to
     */
    public ClientGUI(String add, int port) {
        this.address = add;
        this.port = port;
    }

    /**
     * Draw UI for simple chat system
     */
    public void draw(){
        // Create our "container", main frame
        mainFrame = new JFrame();
        mainFrame.setTitle("Simple Chat System");
        mainFrame.setSize(width, height);
        mainFrame.setLayout(new BorderLayout());

        // Add maintext, where messages are shown
        mainText = new JTextArea("Test");
        mainText.setSize(width, height);
        mainText.setEditable(false);
        mainText.setLineWrap(true);
        JScrollPane scrollPanel = new JScrollPane(mainText);
        mainFrame.add(scrollPanel, BorderLayout.CENTER);

        // Add new bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setSize(width, 50);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));

        // Add a user entry box in bottom panel
        userEntry = new JTextField();
        userEntry.setEditable(true);
        bottomPanel.add(userEntry);

        // Add a send button in the bottom panel
        JButton sendButton = new JButton("Send!");
        sendButton.addActionListener(this);
        bottomPanel.add(sendButton);

        // Add bottom panel to main frame
        mainFrame.add(bottomPanel, BorderLayout.PAGE_END);

        // Set frame to visible
        mainFrame.setVisible(true);
    }

    /**
     * This starts the client. Creating a ClientSender thread and a ClientListener thread
     * ClientSender Thread lets us send messages to the server
     * ClientListener Thread lets us receive messages from the server
     */
    public void go() {
        // Try-with resources: open a socket to connect to server
        try(Socket serverSocket = new Socket(address, port)) {
            // Create new ClientSender and ClientListener objects to send messages to server and receive messages
            this.serverSocket = serverSocket;
            // ClientSender sender = new ClientSender(serverSocket);
            ClientListenerGUI listener = new ClientListenerGUI(serverSocket, this);
            listener.execute();
            while (true){
                ;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This sets up ssh local port forwarding to redirect any traffic sent to localhost port 14002 to
     * linux3.bath.ac.uk port 34752
     */
    public void PortForward(){
        // Use JSch to connect to linux3.bath.ac.uk with ssh
        sshManager = new JSch();
        try {
            sshManager.addIdentity(System.getProperty("user.dir") + "/Data/id_rsa");
            sshManager.setKnownHosts(System.getProperty("user.dir") + "/Data/known_hosts");
            session = sshManager.getSession("rhdd20", "linux3.bath.ac.uk", 22);
            session.connect();

            // Set up port forwarding so any data we send to localhost port is redirected to the linux server
            boolean connected = false;
            while (connected != true){
                try{
                    session.setPortForwardingL(this.port, this.address, 34752);
                    connected = true;
                }
                catch (JSchException e){
                    this.port++;
                }
            }


        } catch (JSchException e){
            e.printStackTrace();
        }

    }

    /**
     * Disconnects from the ssh session, deleting the local port forwarding
     */
    public void Disconnect(){
        // Disconnect port forwarding
        try{
            session.delPortForwardingL(port);
            session.disconnect();
        }
        catch (JSchException e){
            e.printStackTrace();
        }

        // Exit, commented out to avoid intellij issues
        System.exit(0);
    }

    public static void main(String[] args) {
        // Create a new client object to connect to localhost:14002
        ClientGUI myClient = new ClientGUI("localhost", 14002);

        // Port forward to linux bath server
        myClient.PortForward();

        // Draw UI
        myClient.draw();

        // Start client
        myClient.go();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Called when send button is pressed
        // Get users input
        String userMessage = userEntry.getText();

        // Send the message to the server
        try {
            PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
            serverOut.println(userMessage);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Clear user entry
        userEntry.setText("");
    }

    public JTextArea getMainText(){
        return mainText;
    }
}
