import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    int port;
    private Game game;
    private Grid grid;
    private int nextId = 1;
    private ArrayList<ClientConnection> clientList = new ArrayList<ClientConnection>();

    public Server(int port, Game game, Grid grid) {
        this.port = port;
        this.game = game;
        this.grid = grid;
        startServerSocket();
    }

    private void startServerSocket() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port: " + port);
        } catch (IOException error) {
            System.out.println("Error establishing server socket connection");
        }
    }

    public void stopServerSocket() {
        try {
            System.out.println("Closing server socket...");
            serverSocket.close();
        } catch (IOException error) {
            System.out.println("Could not close server socket");
        }
    }

    public void messageAllClients(String token, Game game, int senderId) {
        for (ClientConnection client : clientList) {
            System.out.println("Messaging client id: " + client.getId());
            client.sendMessage(token, game, senderId);
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
                    ClientConnection clientConnection = new ClientConnection(nextId, oos, ois);
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
                InputStream is = new ByteArrayInputStream(packetIn.bytes);
                BufferedImage bufferedImage = ImageIO.read(is);
                is.close();
                switch(packetIn.token){
                    //Inform all clients to start
                    case "START" -> {
                        server.messageAllClients("START", game, senderId);
                    }
                    case "DRAW" -> {
                        game.changeSquare(packetIn.index, bufferedImage);
                        grid.updateImage(packetIn.index);
                        grid.repaintSquare(packetIn.index);
                    }
                }
                // WRITE
                server.messageAllClients("DRAW", game, senderId);
                // oos.writeObject(new Packet("DRAW", game, senderId));
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
    private ObjectInputStream ois;
    private String color;

    public ClientConnection(int id, ObjectOutputStream oos, ObjectInputStream ois) {
        this.id = id;
        this.oos = oos;
        this.ois = ois;
    }

    public void sendMessage(String token, Game game, int senderId) {
        try {
            oos.writeObject(new Packet(token, game, senderId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }
}