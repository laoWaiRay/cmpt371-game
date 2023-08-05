/*  Server is a networking class responsible for establishing socket connections with each client.
 *  For each connected client, a new thread is started which listens for messages from
 *  that client, updates a shared game state, and then broadcasts the updated game state
 *  to all players.
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    int port;   // default is 8080
    private final Game game;    // contains the current game state
    private int nextId = 1;     // id assigned to the next client who connects

    // This is a list of all connected clients
    private final ArrayList<ClientConnection> clientList = new ArrayList<ClientConnection>();

    public Server(int port, Game game) {
        this.port = port;
        this.game = game;
        startServerSocket();
    }

    // Opens the server socket on the host's IP address and default port 8080
    private void startServerSocket() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port: " + port);
        } catch (IOException error) {
            System.out.println("Error establishing server socket connection");
        }
    }

    // Closes the server socket
    public void stopServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException error) {
            System.out.println("Could not close server socket");
        }
    }

    // Constructs and broadcasts the same packet to all clients
    public synchronized void messageAllClients(String token, Game game, int senderId) {
        for (ClientConnection client : clientList) {
            client.sendMessage(token, game, senderId);
        }
    }

    // Constructs and broadcasts the same packet to all clients, including the changed square ID
    // which the client can use to update their UI
    public synchronized void messageAllClients(String token, Game game, int squareIndex, int senderId) {
        for (ClientConnection client : clientList) {
            client.sendMessage(token, game, squareIndex, senderId);
        }
    }

    // Accept client connections on a new thread to not block the stop server call
    @Override
    public void run() {
        // DO FOREVER LOOP: Listen for client connections and create new threads to handle each client
        Thread serverSocketHandler = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // We are using object streams for the data we are sending - see Packet class for more details
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

                    // Creating the client handler thread for the newly connected client
                    Thread thread = new Thread(new ClientHandler(ois, game, this));

                    // Create and add new client (with a unique id) to the list of connected clients
                    ClientConnection clientConnection = new ClientConnection(nextId, oos);
                    clientList.add(clientConnection);

                    // On initial connection to a new client, we just send them a packet with a greeting message
                    clientConnection.sendMessage("CONNECT", game, nextId);

                    // The first client to connect should also be the host. Here we are informing them
                    // whenever a new player has joined the game
                    clientList.get(0).sendMessage("NEW_PLAYER", game, 0);

                    // Increment the id for the next client and start the client handler thread for this client
                    nextId++;
                    thread.start();
                } catch (IOException error) {
                    System.out.println("Could not read data from client");
                    break;
                }
            }
        });

        serverSocketHandler.start();
    }
}

// This is the logic for handling the clients in each of their respective threads
class ClientHandler implements Runnable {
    private final ObjectInputStream ois;
    private final Game game;
    private final Server server;

    public ClientHandler(ObjectInputStream ois, Game game, Server server) {
        this.ois = ois;
        this.game = game;
        this.server = server;
    }

    @Override
    public void run() {
        // The job of the client handler is to listen to individual clients for any messages,
        // update the server's game state accordingly, and then broadcast the server's version
        // of the game state to all clients
        while (true) {
            try {
                // READ - packets from clients contain the ID of the sender, a token message, the index of the square
                // that they are updating, and the image of the square according to the client
                Packet packetIn = (Packet) ois.readObject();
                int senderId = packetIn.senderId;
                int squareIndex = packetIn.index;

                InputStream is = new ByteArrayInputStream(packetIn.bytes);
                BufferedImage bufferedImage = ImageIO.read(is);
                is.close();

                // IMPORTANT !
                // Each packet contains a different TOKEN message which identifies which action the server should take
                switch(packetIn.token){
                    // Inform all clients to start the game
                    case "START" -> {
                        server.messageAllClients("START", game, senderId);
                    }
                    // This is sent to update a specific square in the server's game state with a new image
                    case "DRAW" -> {
                        // Update square with new data from client
                        game.changeSquare(packetIn.index, bufferedImage);
                    }
                    // This token is sent to indicate that a client is trying to acquire the mutex for
                    // a given square. The lock is only acquired if no other client is using it AND the square
                    // has not been fully colored.
                    case "LOCK" -> {
                        // Attempt to acquire a lock on the square using the client's ID
                        game.getGameSquare(squareIndex).acquireLock(senderId);
                    }
                    // This token informs the server that a client has finished and is no longer using a square.
                    case "UNLOCK" -> {
                        // Update server game state with new data from client
                        game.changeSquare(packetIn.index, bufferedImage);

                        // Check if square is fully colored - if yes, lock square permanently
                        int rgb = bufferedImage.getRGB(50, 50);
                        Color colour = new Color(rgb);
                        Color def = new Color(0, 0,0);
                        if (!(colour.equals(Color.WHITE) | colour.equals(def))) {
                            // Setting the square to fully colored locks it forever
                            game.setSquareFullyColored(packetIn.index);
                            server.messageAllClients("FULLY_COLOR", game, packetIn.index, 0);
                        }

                        // Unlock the square (if the square is fully colored, it does not unlock)
                        game.getGameSquare(squareIndex).releaseLock();

                        // Check if game is over and inform all clients
                        if (game.isGameFinished()) {
                            server.messageAllClients("GAMEOVER", game, 0);
                            return;
                        }
                    }
                }

                // WRITE - broadcast new game state to all clients
                switch (packetIn.token) {
                    case "DRAW" -> server.messageAllClients("DRAW", game, squareIndex, senderId);
                    case "LOCK" -> server.messageAllClients("LOCK", game, squareIndex, senderId);
                    case "UNLOCK" -> server.messageAllClients("UNLOCK", game, squareIndex, senderId);
                }
            } catch (IOException error) {
                System.out.println("Error reading from object stream");
                error.printStackTrace();
            } catch (ClassNotFoundException error) {
                System.out.println("Error reading from object stream: Class not found");
            }                

        }
    }
}

// This is a simple class to represent clients, with a helper method which is used in the
// server's broadcast method
class ClientConnection {
    private final int id;
    private final ObjectOutputStream oos;

    public ClientConnection(int id, ObjectOutputStream oos) {
        this.id = id;
        this.oos = oos;
    }

    // Constructs a packet and sends it from the server to this client
    public void sendMessage(String token, Game game, int senderId) {
        try {
            oos.writeObject(new Packet(token, game, senderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sends a message from the server to the client with a specific square id for updating
    public void sendMessage(String token, Game game, int squareIndex, int senderId) {
        try {
            oos.writeObject(new Packet(token, game, squareIndex, senderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns the id of the connected client
    public int getId() {
        return id;
    }
}