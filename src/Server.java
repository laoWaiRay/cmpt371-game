import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    int port;
    private final Game game;    // contains the current game state
    private final Grid grid;    // contains methods for updating the grid UI
    private int nextId = 1;     // id assigned to the next client who connects

    // This is a list of all connected clients
    private final ArrayList<ClientConnection> clientList = new ArrayList<ClientConnection>();

    public Server(int port, Game game, Grid grid) {
        this.port = port;
        this.game = game;
        this.grid = grid;
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

    // Accept client connections on a new thread to not block the stop call
    @Override
    public void run() {
        // Listen for client connections and create new threads
        Thread serverSocketHandler = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connected to new client");
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

                    Thread thread = new Thread(new ClientHandler(clientSocket, ois, oos, game, grid, nextId, this));

                    // Create and add new client connection to the list of connections
                    ClientConnection clientConnection = new ClientConnection(nextId, oos);
                    clientList.add(clientConnection);

                    clientConnection.sendMessage("CONNECT", game, nextId);
                    System.out.println("Sending new player packet to host");

                    //Inform host that a new player has joined
                    clientList.get(0).sendMessage("NEW_PLAYER", game, 0);

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

class ClientHandler implements Runnable {
    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;
    private Game game;
    private Grid grid;
    private int id;
    private Server server;

    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos, Game game,
                         Grid grid, int id, Server server) {
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.game = game;
        this.grid = grid;
        this.id = id;
        this.server = server;
    }

    @Override
    public void run() {
        System.out.println("ClientHandler started for client: " + socket.getInetAddress());
        // Initial connection handling: Assign ID to client that is connecting
        try {
            oos.writeObject(new Packet("CONNECT", game, id));
            Packet packet = (Packet) ois.readObject();
            System.out.println("Received initial packet from sender id: " + packet.senderId);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                // READ
                Packet packetIn = (Packet) ois.readObject();
                int senderId = packetIn.senderId;
                int squareIndex = packetIn.index;

                InputStream is = new ByteArrayInputStream(packetIn.bytes);
                BufferedImage bufferedImage = ImageIO.read(is);
                is.close();

                switch(packetIn.token){
                    //Inform all clients to start
                    case "START" -> {
                        server.messageAllClients("START", game, senderId);
                    }
                    case "DRAW" -> {
                        // Update square with new data from client
                        game.changeSquare(packetIn.index, bufferedImage);
                        grid.updateImage(packetIn.index);
                        grid.repaintSquare(packetIn.index);
                    }
                    case "LOCK" -> {
                        // Attempt to acquire a lock on the square using the client's ID
                        game.getGameSquare(squareIndex).acquireLock(senderId);
                    }
                    case "UNLOCK" -> {
                        // Update square with new data from client
                        game.changeSquare(packetIn.index, bufferedImage);
                        grid.updateImage(packetIn.index);
                        grid.repaintSquare(packetIn.index);

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

class ClientConnection {
    private int id;
    private ObjectOutputStream oos;

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