import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private int port = 14002; // Port the server is set-up on
    private ArrayList<Socket> clientSockets = new ArrayList<Socket>(); // ArrayList of every client connected to the server

    /**
     * Repeatedly check for new clients and creates a ClientListener thread to deal with that client
     */
    public void go() {
        System.out.println("Server listening...");

        // Repeatedly look for new clients
        while(true){
            // Open with-resources a new serverSocket on the port
            try(ServerSocket mySocket = new ServerSocket(port)) {
                // Accept a connection from a client, returning the clientSocket
                // This is a blocking call. This will wait here until a client connects
                Socket clientSocket = mySocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Client trying to connect");

                // Create a new serverResponse object and create a thread for it to deal with client
                ServerResponse serverResponse = new ServerResponse(mySocket, clientSocket, this);
                Thread respond = new Thread(serverResponse);

                // Start thread (no need for join as we're in a while(true) loop)
                respond.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Gets the sockets of all the clients we're connected to
     * @return an ArrayList of all clients connected to the server
     */
    public ArrayList<Socket> getClientSockets(){
        return clientSockets;
    }

    public static void main(String[] args) {
        Server myServer = new Server();
        myServer.go();
    }

}
