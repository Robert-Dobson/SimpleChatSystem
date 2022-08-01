import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;

public class ClientListenerGUI extends SwingWorker {
    private Socket serverSocket;
    private ClientGUI clientObject;

    /**
     * Constructor to create ClientListener object
     * @param serverSocket
     * @param clientObject
     */
    public ClientListenerGUI(Socket serverSocket, ClientGUI clientObject){
        this.serverSocket = serverSocket;
        this.clientObject = clientObject;
    }

    // Reads any data sent from the server and outputs it
    @Override
    protected String doInBackground(){
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
                    publish(serverResponse);
                } catch (IOException e1) {
                    System.out.println("Disconnected!");
                    clientObject.Disconnect();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Appends any received messages to the text area on the GUI
    @Override
    protected void process(List text) {
        JTextArea mainText = clientObject.getMainText();
        StringBuilder currentText = new StringBuilder(mainText.getText());
        for (Object line:text){
            String lineText = line.toString() + System.lineSeparator();
            currentText.append(lineText);
        }
        mainText.setText(currentText.toString());
    }
}
