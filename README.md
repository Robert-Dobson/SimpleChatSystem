# Simple Chat System
This is a direct messaging service built in Java using networking, multithreading and Swing. There are two sides of the project: Client and Server. Clients connect to the remote server and when a client wants to send a message to another user they send the message to the server which then forwards it to the intended client.

Example of Clients talking to each other:
![image](https://user-images.githubusercontent.com/43008203/194764443-a60fe0ef-b804-4bdd-b88a-c28c76e1bc3d.png)

Example of the Server running: 

![Screenshot 2022-10-09 161123](https://user-images.githubusercontent.com/43008203/194764568-acde80ee-f308-4bb0-a82f-d1bb2a3da0fa.png)


## How to run
Currently this project is designed to run on the University of Bath's Linux server (specifically linux3.bath.ac.uk) so changes would have to be made to run on your own server. The application currently works by connecting via SSH to your user profile on the University's SSH server so to do this you need to first add your login details to the program.

If you're a University of Bath student to direct clients to your account complete the following:
1. Add a folder called "Data" to the project directory 
2. Add your SSH private key to this folder (call the file `id_rsa`). Follow this guide to generate a private and public key https://www.ssh.com/academy/ssh/keygen. 
3. Add a `known_hosts` file containing a linux3.bath.ac.uk entry. The easiest way to do that is to connect to linux3.bath.ac.uk yourself then copy your `known_hosts` file (found at `~/.ssh/known_hosts`) into the data folder. 
5. Change all instances of rhdd20 to your bath username in `Client.java` (you can do a simple find and replace)

If you want to set the program up on your own SSH server, you will need to change the `portForward()` method in `Client.java` to port forward to your linux server, replacing instances of linux3.bath.ac.uk to your server address. Then follow the steps for University of Bath students replacing `rhdd20` with your username and `linux3.bath.ac.uk` with your server address

Once you've made these changes to run the Client please run the `Client.java` main method and to run the Server copy across all .java files (except `Client.java`, `ClientListener.java` and `ChatSessionView.java`) and please run the `Server.java` main method on the server. It is important that the server is running first to avoid errors when connecting.

Note due to changes in University of Bath's security, Clients must now be connected to eduroam on campus or to the University's VPN for the program to work (if using linux3.bath.ac.uk as your server). This is to allow clients to connect to the linux server.

## Development
This project intially started as a terminal based chatroom program where the server and clients were running on the same computer and all messages were broadcast to all clients online. Later on, I decided to expand the project by first enabling the server to run on the University of Bath's Linux server to enable communication between different machines. Then, I developed a GUI using Java Swing. Most recently, I revamped the project to turn it from a chatroom program to a direct messaing program, allowing messages to be directed specifically to a client rather than to all online users.  
