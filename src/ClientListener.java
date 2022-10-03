import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientListener implements Runnable{
    private Socket serverSocket;
    private Client clientObject;

    /**
     * Constructor to create ClientListener object
     * @param serverSocket
     * @param clientObject
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
                        int specialCode = serverMessage.specialCode;
                        switch(specialCode){
                            // Direct message received
                            case 0:
                                clientObject.messageReceived(serverMessage, true);
                                break;

                            // Login Request Accepted/Rejected
                            case 11:
                                clientObject.loginRecieved(serverMessage.message);
                                break;
                            // Refresh Users
                            case 20:
                                clientObject.updateUsers(serverMessage.users);
                                break;
                        }
                    }
                } catch (IOException | ClassNotFoundException e1) {
                    ;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
