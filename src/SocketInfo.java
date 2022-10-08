import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Encapsulates details of each client's socket, their ID and the relevant output stream to send messages on
 */
public class SocketInfo {
    private final int clientID;
    private final Socket clientSocket;
    private final ObjectOutputStream outputStream;

    /**
     * Constructor for SocketInfo objects to hold details of each client's socket
     * @param clientID  id of client associated with the socket
     * @param clientSocket  the socket itself
     * @param outputStream  an open object output stream used to send messages to the client
     */
    public SocketInfo(int clientID, Socket clientSocket, ObjectOutputStream outputStream){
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.outputStream = outputStream;
    }

    /**
     * Returns clientID associated with socket
     * @return  clientID
     */
    public int getClientID() {
        return clientID;
    }

    /**
     * Returns active client socket
     * @return  clientSocket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Returns object output stream used to send messages to client
     * @return  outputStream
     */
    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }
}
