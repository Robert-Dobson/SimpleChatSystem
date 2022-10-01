import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

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
            while(true) {
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
                            // Login Request Accepted/Rejected
                            case 11:
                                clientObject.loginRecieved(serverMessage.message);
                            // Refresh Users
                            case 20:
                                clientObject.updateUsers(serverMessage.users);
                        }
                    }
                } catch (IOException e1) {
                    System.out.println("Disconnected!");
                    clientObject.Disconnect();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
