
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

public class ChatSessionView implements ActionListener {
    // Main GUI Attributes
    private JFrame mainFrame;
    private int height = 500;
    private int width = 900;
    private JTextArea mainText;
    private JTextField userEntry;
    private JButton sendButton;
    private JComboBox<User> selectUser;

    // Login Diag Attributes
    private JDialog diagFrame;
    private JTextField usernameEntry;
    private JButton loginButton;


    // Client Object
    Client currentClient;

    public ChatSessionView(Client clientObject){
        this.currentClient = clientObject;
    }

    /**
     * Draw the main UI for the chat system
     */
    public void draw(){
        // Set look and feel to FlatLaf (modern looking library)
        FlatLightLaf.setup();

        // Create our "container", main frame
        mainFrame = new JFrame();
        mainFrame.setTitle("Simple Chat System");
        mainFrame.setSize(width, height);
        mainFrame.setLayout(new BorderLayout());

        // Setup exit procedure to safely exit the program when closing the window
        mainFrame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent we){
                currentClient.Disconnect();

            }
        });

        // Add top panel
        JPanel topPanel = new JPanel();
        topPanel.setSize(width, 75);
        topPanel.setLayout(new FlowLayout());

        // Add label and combo box for selecting user
        topPanel.add(new JLabel("Select User to Message:"));
        selectUser = new JComboBox<User>();
        selectUser.addActionListener(this);
        topPanel.add(selectUser);

        // Add top panel to main frame
        mainFrame.add(topPanel, BorderLayout.PAGE_START);

        // Add main text, where messages are shown
        mainText = new JTextArea("");
        mainText.setSize(width, height);
        mainText.setEditable(false);
        mainText.setLineWrap(true);
        JScrollPane scrollPanel = new JScrollPane(mainText);
        mainFrame.add(scrollPanel, BorderLayout.CENTER);

        // Add new bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setSize(width, 50);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));

        // Add a user entry box in bottom panel
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

        // Set frame to visible
        mainFrame.setVisible(true);
    }

    /**
     * Open up a new login dialog to let the user login to the chat system
     */
    public void runLoginDialog(){
        // Create our "container", main frame
        diagFrame = new JDialog(mainFrame, "Login", true);
        diagFrame.setSize(300 , 150);
        diagFrame.setLayout(new FlowLayout());

        // Add name entry for users to enter name
        usernameEntry = new JTextField("");
        diagFrame.add(usernameEntry);

        // Add a login button in the bottom panel
        loginButton = new JButton("Login!");
        loginButton.addActionListener(this);
        diagFrame.add(loginButton);

        // Set login as button pressed when pressing enter
        diagFrame.getRootPane().setDefaultButton(loginButton);

        // Set frame to visible
        diagFrame.setVisible(true);
    }

    /**
     * Updates the drop-down menu with all online users
     * @param users list of currently online users
     */
    public void updateOnlineUsers(ArrayList<User> users){
        selectUser.removeAllItems();
        if (users != null){
            for (User user: users){
                selectUser.addItem(user);
            }
        }
    }

    /**
     * Update the main text body with messages held in the given user's file
     * @param userID the userID of the friend's file to load
     */
    public void loadFile(int userID){
        // Load the file in a swing worker thread to avoid GUI hangups
        SwingWorker<String, String> loadFile = new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                // Create file if the file doesn't exist
                File messageFile = new File(System.getProperty("user.dir") + "/Data/" + currentClient.clientDetails.uniqueID + "-" + userID + "messages.txt");
                if (!messageFile.exists()) {
                    try {
                        messageFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Read file contents
                StringBuilder text = new StringBuilder("");
                try (BufferedReader br = new BufferedReader(new FileReader(messageFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line).append("\n");
                    }
                }
                return text.toString();
            }

            @Override
            protected void done() {
                super.done();
                // Clear main text and replace with text we read in
                String text = "";
                try {
                    text = get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                mainText.setText(text);
            }
        };
        loadFile.execute();
    }

    /**
     * Refreshes main text with new message if user that needs updating is selected
     * @param userID
     */
    public void refreshText(int userID){
        // The currently selected user
        User selectedUser = (User) this.selectUser.getSelectedItem();

        // If currently selected user is user to update reload file
        if (selectedUser !=null && userID == selectedUser.uniqueID){
            loadFile(userID);
        }

    }

    /**
     * Responds to any pressed buttons.
     * If send button - send message to requested user
     * If login button - send login request to server
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Called when send button is pressed, Get users input
        if (e.getSource().equals(sendButton)){
            // Get text and user to send to
            String userMessage = userEntry.getText();
            User selectedUser = (User) selectUser.getSelectedItem();
            if (userMessage != null && !userMessage.equals("") && selectedUser != null){
                // Send typed message to selected user
                currentClient.sendMessageToUser(userMessage, selectedUser);
                // Clear user entry
                userEntry.setText("");
            }
        }
        // Called when login button is pressed, log in
        else if (e.getSource().equals(loginButton)){
            String name = usernameEntry.getText();
            currentClient.login(name);
            diagFrame.setVisible(false);
        }
        // Called when users combo box is changed, change messages
        else if (e.getSource().equals(selectUser)){
            User user = (User) selectUser.getSelectedItem();
            if (user != null){
                refreshText(user.uniqueID);
            }
        }
    }
}
