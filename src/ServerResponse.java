import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerResponse implements Runnable{
    private ServerSocket mySocket;
    private Socket clientSocket;
    private Server serverObject;
    private String name;
    private String exitString = "!quit";


    /**
     * Constructor to create ServerResponse Object
     * @param mySocket  the ServerSocket of the server
     * @param clientSocket  the socket connected to the client
     * @param serverObject  the server object/instantiation, so we can access Server methods
     */
    public ServerResponse(ServerSocket mySocket, Socket clientSocket, Server serverObject){
        this.mySocket = mySocket; // Get the ServerSocket
        this.clientSocket = clientSocket; // Get the server connected to the client
        this.serverObject = serverObject; // Get the object of server
    }

    /**
     * Sends a message to all connected clients except the passed client
     * @param currentSocket the socket of the client we don't want to send a message to
     * @param message   the message we want to send to the clients
     * @throws IOException
     */
    public void broadcast(Socket currentSocket, String message) throws IOException {
        ArrayList<Socket> sockets = serverObject.getClientSockets();
        for (Socket socket: sockets){
            if (socket != currentSocket){
                // Set up the ability to send the data to each other client
                PrintWriter clientOut = new PrintWriter(socket.getOutputStream(), true);
                clientOut.println(name + ":" + message);
            }
        }
    }

    public void quit(){

    }

    // Reads data from the client we are connected to and broadcasts the message to all other clients connected to the server
    @Override
    public void run() {
        try {
            // Accept a connection from a client
            System.out.println("Server accepted connection from new client");

            // Set up the ability to read the data from the client
            InputStreamReader clientCharStream = new InputStreamReader(clientSocket.getInputStream());
            BufferedReader clientIn = new BufferedReader(clientCharStream);

            // Tell client to enter name
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            clientOut.println("Connected to server!");
            clientOut.println("Please enter your name!");

            // Read name to setup
            try{
                name = clientIn.readLine();
                if (name.equals(exitString)){
                    clientSocket.close();
                    throw new IOException();
                }
            }
            catch (IOException e){
                //Client has disconnected
                System.out.println("New client has disconnected!");
                serverObject.removeClient(clientSocket);
                return;
            }

            System.out.println(name+" has joined!");

            // Read from the client, and broadcast to all other clients
            while(true) {
                String userInput;
                try{
                    userInput = clientIn.readLine();
                    if (userInput.equals(exitString)){
                        clientSocket.close();
                        throw new IOException();
                    }
                }
                catch (IOException e){
                    //Client has disconnected
                    System.out.println(name+" has disconnected!");
                    serverObject.removeClient(clientSocket);
                    return;
                }

                broadcast(clientSocket, userInput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
