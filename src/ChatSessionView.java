import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * This class handles creating the GUI, updating the GUI and dealing with inputs from the GUI.
 * The class will deal with the user interface needs of the Client object
 */
public class ChatSessionView implements ActionListener {
    // Main GUI Attributes
    private JFrame mainFrame;
    private JTextArea mainText;
    private JTextField userEntry;
    private JButton sendButton;
    private JComboBox<User> selectUser;

    // Login Diag Attributes
    private JDialog diagFrame;
    private JTextField usernameEntry;
    private JButton loginButton;

    // Client Object
    private final Client currentClient;

    /**
     * Constructs a ChatSessionView object used to manage the GUI
     * @param clientObject  object of client the GUI will represent
     */
    public ChatSessionView(Client clientObject){
        this.currentClient = clientObject;
    }

    /**
     * Draw the main GUI for the chat system
     */
    public void draw(){
        // Set look and feel to FlatLaf (modern looking GUI library)
        FlatLightLaf.setup();

        // Create our "container", main frame
        mainFrame = new JFrame();
        mainFrame.setTitle("Simple Chat System");
        int height = 500;
        int width = 900;
        mainFrame.setSize(width, height);
        mainFrame.setLayout(new BorderLayout());

        // Setup exit procedure to safely exit the program when closing the window
        // This will ensure we disconnect from the server and that cached messages are deleted
        mainFrame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent we){
                currentClient.disconnect();
            }
        });

        // Add top panel, this will contain user selection
        JPanel topPanel = new JPanel();
        topPanel.setSize(width, 75);
        topPanel.setLayout(new FlowLayout());

        // Add label and combo box for selecting user
        topPanel.add(new JLabel("Select User to Message:"));
        selectUser = new JComboBox<>();
        selectUser.addActionListener(this);
        topPanel.add(selectUser);

        // Add top panel to main frame
        mainFrame.add(topPanel, BorderLayout.PAGE_START);

        // Add main text area, this is where messages are shown
        // Main text area will be scrollable (using scrollPanel) and will have line-wrap
        mainText = new JTextArea("");
        mainText.setSize(width, height);
        mainText.setEditable(false);
        mainText.setLineWrap(true);
        JScrollPane scrollPanel = new JScrollPane(mainText);
        mainFrame.add(scrollPanel, BorderLayout.CENTER);

        // Add new bottom panel, this is where you can type and send messages
        JPanel bottomPanel = new JPanel();
        bottomPanel.setSize(width, 50);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));

        // Add a user entry box for messages in bottom panel
        userEntry = new JTextField();
        userEntry.setEditable(true);
        bottomPanel.add(userEntry);

        // Add a send button in the bottom panel
        sendButton = new JButton("Send!");
        sendButton.addActionListener(this);
        bottomPanel.add(sendButton);

        // Add bottom panel to main frame
        mainFrame.add(bottomPanel, BorderLayout.PAGE_END);

        // When you press enter, the send button will be pressed
        mainFrame.getRootPane().setDefaultButton(sendButton);

        // Make main GUI visible
        mainFrame.setVisible(true);
    }

    /**
     * Open up a new login dialog to let the user login to the chat system
     * Currently, login process only involves entering a name
     */
    public void runLoginDialog(){
        // Create our "container", main frame
        diagFrame = new JDialog(mainFrame, "Login", true);
        int width = 300;
        int height = 150;
        diagFrame.setSize(width , height);
        diagFrame.setLayout(new FlowLayout());

        // Add name entry for users to enter name
        usernameEntry = new JTextField("");
        diagFrame.add(usernameEntry);

        // Add a login button in the bottom panel
        loginButton = new JButton("Login!");
        loginButton.addActionListener(this);
        diagFrame.add(loginButton);

        // When you press enter, the login button will be pressed
        diagFrame.getRootPane().setDefaultButton(loginButton);

        // Make Login Dialog visible
        diagFrame.setVisible(true);
    }

    /**
     * Updates the drop-down menu with all online users
     * @param users list of currently online users
     */
    public void updateOnlineUsers(ArrayList<User> users){
        // Removes current list and adds each user in the new list one by one
        selectUser.removeAllItems();
        if (users != null){
            for (User user: users){
                selectUser.addItem(user);
            }
        }
    }

    /**
     * Refreshes main text with new message if user that needs updating is selected
     * @param userID    The ID of the user that needs updating (used to check if the user is currently displayed)
     */
    public void refreshText(int userID){
        // The currently selected user
        User selectedUser = (User) this.selectUser.getSelectedItem();

        // If we've updated the current user's file reload the text on screen by reading the file again
        if (selectedUser !=null && userID == selectedUser.getUniqueID()){
            loadFile(userID);
        }

    }

    /**
     * Responds to any GUI events (pressed button, change combo-box menu, etc.)
     * If send button - send message to requested user
     * If login button - send login request to server
     * If combo-box menu - load the messages for the new user
     * @param e Details of the event that triggered this method
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Called when send button is pressed, Get users input
        if (e.getSource().equals(sendButton)){
            // Get text of the message and the user to send the message to
            String userMessage = userEntry.getText();
            User selectedUser = (User) selectUser.getSelectedItem();
            if (userMessage != null && !userMessage.equals("") && selectedUser != null){
                // Send typed message to selected user
                currentClient.sendMessageToUser(userMessage, selectedUser);
                // Clear user entry, so new message can be typed
                userEntry.setText("");
            }
        }
        // Called when login button is pressed, log in
        else if (e.getSource().equals(loginButton)){
            // Get username and send to log in method. Then close login dialog
            String name = usernameEntry.getText();
            currentClient.login(name);
            diagFrame.setVisible(false);
        }
        // Called when users combo box is changed, change messages
        else if (e.getSource().equals(selectUser)){
            // Get the newly selected user and then load their messages to the GUI
            User user = (User) selectUser.getSelectedItem();
            if (user != null){
                refreshText(user.getUniqueID());
            }
        }
    }

    /**
     * Update the main text body with messages held in the given user's file
     * @param userID the userID of the friend's file to load
     */
    private void loadFile(int userID){
        // Load the file in a swing worker thread to avoid GUI hangups
        // First define the swing worker in an anonymous inner class
        SwingWorker<String, String> loadFile = new SwingWorker<>() {
            /**
             * Read in the contents of the message cache of the selected user.
             * If the file doesn't exist then a new file is created with no contents
             *
             * @return The file contents as a string
             */
            @Override
            protected String doInBackground() {
                // Create file if the file doesn't exist
                User clientDetails = currentClient.getClientDetails();
                int clientID = clientDetails.getUniqueID();
                File messageFile = new File(System.getProperty("user.dir") + "/Data/" + clientID + "-" + userID + "messages.txt");
                if (!messageFile.exists()) {
                    try {
                        messageFile.createNewFile();
                    } catch (IOException e) {
                        // Error creating file
                        return "Error creating message file, please try again";
                    }
                }

                // Read file contents line by line
                StringBuilder text = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(messageFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Error reading the file
                    return "Error reading message file, please try again";
                }
                return text.toString();
            }

            /**
             * Clears the main body of text on the UI and replaces its content with the text we read in the
             * doInBackground() method
             */
            @Override
            protected void done() {
                super.done();
                // Clear main text and replace with text we've read in
                String text = "";
                try {
                    text = get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                mainText.setText(text);
            }
        };

        // Execute swing worker to read in file
        loadFile.execute();
    }
}
