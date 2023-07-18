import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private ServerSocket serverSocket;
    int port;
    private Game game;
    private Grid grid;
    private int nextId = 1;

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

    // Accept client connections on a new thread to not block the stop call
    @Override
    public void run() {
        // Listen for client connections and create new threads
        Thread serverSocketHandler = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Connected to new client");
                        ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

                        Thread thread = new Thread(new ClientHandler(clientSocket, ois, oos, game, grid, nextId));
                        nextId++;
                        thread.start();
                    } catch (IOException error) {
                        System.out.println("Could not read data from client");
                        break;
                    }
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

    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos, Game game, Grid grid, int id) {
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.game = game;
        this.grid = grid;
        this.id = id;
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
                System.out.println("READING ON SERVER");
                Packet packetIn = (Packet) ois.readObject();
                int senderId = packetIn.senderId;
                InputStream is = new ByteArrayInputStream(packetIn.bytes);
                BufferedImage bufferedImage = ImageIO.read(is);
                is.close();

                game.changeSquare(packetIn.index, bufferedImage);
                grid.updateImage(packetIn.index);
                grid.repaintSquare(packetIn.index);

                // WRITE
                System.out.println("WRITING ON SERVER");
                oos.writeObject(new Packet("DRAW", game, senderId));
            } catch (IOException error) {
                System.out.println("Error reading from object stream");
                error.printStackTrace();
            } catch (ClassNotFoundException error) {
                System.out.println("Error reading from object stream: Class not found");
            }
        }
    }
}

/* TODO - 2023/7/18 | 01:16 | raymondly
*   Make it so that server maintains a pool of all of it's client threads,
*   and then broadcasts received packets to ALL clients.
*   How to handle concurrency?
*   Clients can read packets from server until they read their own sender id packet,
*   which lets them know they can send again?
* */
