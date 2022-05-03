import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.jcraft.jsch.*;

public class Client {
    // Address and port of server we're connecting to
    private String address;
    private int port;

    private JSch sshManager;
    Session session = null;


    /**
     * Constructor to create Client Object
     * @param add   the address of the server we're going to connect to
     * @param port  the port of the server we're going to connect to
     */
    public Client(String add, int port) {
        this.address = add;
        this.port = port;
    }

    /**
     * This starts the server. Creating a ClientSender thread and a ClientListener thread
     * ClientSender Thread lets us send messages to the server
     * ClientListener Thread lets us receive messages from the server
     */
    public void go() {
        // Try-with resources: open a socket to connect to server
        try(Socket serverSocket = new Socket(address, port)) {
            // Create new ClientSender and ClientListener objects to send messages to server and receive messages
            ClientSender sender = new ClientSender(serverSocket);
            ClientListener listener = new ClientListener(serverSocket, this);

            // Create new thread objects passing ClientSender and ClientListener so they can be run in new threads
            // We're using multithreading, so we can send and receive messages at the same time
            Thread senderThread = new Thread(sender);
            Thread listenerThread = new Thread(listener);

            // Start threads
            senderThread.start();
            listenerThread.start();

            // Block this main thread until both threads have finished
            senderThread.join();
            listenerThread.join();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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
        Client myClient = new Client("localhost", 14002);

        // Port forward to linux bath server
        myClient.PortForward();

        // Start client
        myClient.go();
    }
}