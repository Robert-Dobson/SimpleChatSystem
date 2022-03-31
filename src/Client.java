import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    // Address and port of server we're connecting to
    private String address;
    private int port;

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
            ClientListener listener = new ClientListener(serverSocket);

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

    public static void main(String[] args) {
        // Create a new client object to connect to localhost:14002
        Client myClient = new Client("localhost", 14002);
        myClient.go();
    }
}
