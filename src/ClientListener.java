import javax.imageio.IIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListener implements Runnable{
    private Socket serverSocket;
    private Client clientObject;

    /**
     * Constructor to create ClientListener object
     * @param serverSocket
     */
    public ClientListener(Socket serverSocket, Client clientObject){
        this.serverSocket = serverSocket;
        this.clientObject = clientObject;
    }

    // Reads any data sent from the server and outputs it
    @Override
    public void run() {
        // Set up the ability to read the data from the server
        try {
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            // Loops, ensuring we can always receive messages from server
            while(true) {
                // Read the message from the server and output it
                try {
                    String serverResponse = serverIn.readLine();
                    if (serverResponse == null){
                        throw new IOException();
                    }
                    System.out.println(serverResponse);
                } catch (IOException e1) {
                    System.out.println("You've now disconnected!");
                    clientObject.Disconnect();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
