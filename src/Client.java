import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Thread {
    private int port;
    private int id;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private Game game;
    private Grid grid;
    private Object lock;
    volatile boolean isRunning = true;
    private String colorName;
    private Color color;
    private int score = 0;      //keeps track of each players score
    private String tokenMessage = "DRAW";

    public Client(int port, Game game, Grid grid, Object lock) {
        this.port = port;
        this.game = game;
        this.grid = grid;
        this.lock = lock;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port)) {
            System.out.println("Connected to server!");
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            oos = new ObjectOutputStream(os);
            ois = new ObjectInputStream(is);

            // Initial connection handling: Receive client ID from the server
            try {
                Packet packet = (Packet) ois.readObject();
                id = packet.senderId;
                setColor();

                oos.writeObject(new Packet("CONNECT", game, id));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Start the Read/write loop threads to sync game with server
            Thread readThread = new Thread(new ServerListener(id, ois, game, grid, this));
            Thread writeThread = new Thread(new UserInputListener(id, oos, game, lock, this));

            readThread.start();
            writeThread.start();

            try {
                readThread.join();
                writeThread.join();
            } catch (InterruptedException e) {
                ois.close();
                oos.close();
            }

            ois.close();
            oos.close();
        } catch (IOException error) {
            System.out.println("Error connecting to server");
        }
    }

    private void setColor() {
        ColorPair[] colorPairs = new ColorPair[] {
                new ColorPair("BLUE", Color.BLUE),
                new ColorPair("RED", Color.RED),
                new ColorPair("YELLOW", Color.YELLOW),
                new ColorPair("GREEN", Color.GREEN)
        };

        // id - 1 because the server assigns ids starting at 1
        colorName = colorPairs[id - 1].name;
        color = colorPairs[id - 1].color;
    }

    public String getColorName() {
        return colorName;
    }

    public Color getColor() {
        return color;
    }

    public int getScore(){
        return score;
    }
    public void addScore(){
        score += 1;
    }

    public int getClientId() {
        return id;
    }

    public void setTokenMessage(String message) {
        tokenMessage = message;
    }

    public String getTokenMessage() {
        return tokenMessage;
    }

}

class ServerListener implements Runnable {
    private int id;
    private ObjectInputStream ois;
    private Game game;
    private Grid grid;
    private Client client;

    public ServerListener(int id, ObjectInputStream ois, Game game, Grid grid, Client client) {
        this.id = id;
        this.ois = ois;
        this.game = game;
        this.grid = grid;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // READ FROM SERVER AND UPDATE CLIENT GAME STATE
                Packet packetIn = (Packet) ois.readObject();

                switch (packetIn.token) {
                    case "LOCK":
                        int squareIndex = packetIn.index;
                        int senderId = packetIn.senderId;
                        game.getGameSquare(squareIndex).acquireLock(senderId);
                        break;
                    case "DRAW":
                        InputStream in = new ByteArrayInputStream(packetIn.bytes);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        in.close();

                        // Avoid duplicate rendering of own square data
                        if (packetIn.senderId != id) {
                            game.changeSquare(packetIn.index, bufferedImage);
                            grid.updateImage(packetIn.index);
                            grid.repaintSquare(packetIn.index);
                        }
                        break;
                }
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

class UserInputListener implements Runnable {
    private int id;
    private ObjectOutputStream oos;
    private Game game;
    private Object lock;
    private String tokenMessage;
    private Client client;

    public UserInputListener(int id, ObjectOutputStream oos, Game game, Object lock, Client client) {
        this.id = id;
        this.oos = oos;
        this.game = game;
        this.lock = lock;
        this.client = client;
    }

    @Override
    public void run() {
        // WRITE LOOP
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (game.getIsStillDrawing() || true) {
                try {
                    System.out.println(client.getTokenMessage());
                    oos.writeObject(new Packet(client.getTokenMessage(), game, id));
                    game.setStillDrawing(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}