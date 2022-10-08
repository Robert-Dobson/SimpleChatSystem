import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Repeatedly checks for messages from the server and executes the relevant Client object method. This is run as a
 * thread so the blocking calls when waiting for a message doesn't freeze the program.
 */
public class ClientListener implements Runnable{
    private final Socket serverSocket;
    private final Client clientObject;

    /**
     * Constructor to create ClientListener object
     * @param serverSocket  the socket that's connected to the server
     * @param clientObject  instance of client that started this thread
     */
    public ClientListener(Socket serverSocket, Client clientObject){
        this.serverSocket = serverSocket;
        this.clientObject = clientObject;
    }

    /**
     * Continuously checks for messages from the server and decides how best to respond
     */
    @Override
    public void run() {
        // Set up the ability to read the data from the server
        try {
            ObjectInputStream in = new ObjectInputStream(serverSocket.getInputStream());

            // Loops, ensuring we can always receive messages from server
            while(!serverSocket.isClosed()) {
                // Read the message from the server and processes it
                try {
                    Message serverMessage = (Message)in.readObject();
                    if (serverMessage == null){
                        throw new IOException();
                    }
                    else {
                        // Decides how to respond to message based off special code
                        int specialCode = serverMessage.getSpecialCode();
                        switch(specialCode){
                            // Direct message received
                            case 0:
                                clientObject.messageReceived(serverMessage, true);
                                break;

                            // Login Request Accepted/Rejected
                            case 11:
                                clientObject.loginReceived(serverMessage.getMessage());
                                break;
                            // Refresh Users
                            case 20:
                                clientObject.updateUsers(serverMessage.getUsers());
                                break;
                        }
                    }
                } catch (IOException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
