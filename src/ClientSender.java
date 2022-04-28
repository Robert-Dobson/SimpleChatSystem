import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Using Runnable interface to implement multithreading
public class ClientSender implements Runnable{
    // Socket connecting to server
    private Socket serverSocket;

    /**
     * Constructor creates a ClientSender Object
     * @param serverSocket  the socket we use to communicate with server
     */
    public ClientSender(Socket serverSocket){
        // Receive socket for the server when instantiated
        this.serverSocket = serverSocket;
    }

    // This is run when we start the thread
    @Override
    public void run() {
        // Set up the ability to read user input from keyboard
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // Set up the ability to send the data to the server
        try {
            PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);

            // Always allow user input
            while(true) {
                // Get user input and send to server
                String userInputString = userInput.readLine();
                serverOut.println(userInputString);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
