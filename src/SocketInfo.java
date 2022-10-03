import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketInfo {
    int clientID;
    Socket clientSocket;
    ObjectOutputStream outputStream;

    /**
     * Constructor for data type to hold a clients id and its associated socket
     * @param clientID
     * @param clientSocket
     */
    public SocketInfo(int clientID, Socket clientSocket, ObjectOutputStream outputStream){
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.outputStream = outputStream;
    }

}
