import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Thread {
    private final int port;
    private int id;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private final Game game;
    private final Grid grid;
    private final Object lock;
    private String colorName;
    private Color color;
    private String tokenMessage = "DRAW";
    private int lastChangedSquare;

    public Client(int port, Game game, Grid grid, Object lock) {
        this.port = port;
        this.game = game;
        this.grid = grid;
        this.lock = lock;
    }

    @Override
    public void run() {
        // Opening a socket connection to the server
        // For demo purposes, we have hard coded the IP address of the server to our internal IP address.
        try (Socket socket = new Socket(InetAddress.getByName("192.168.1.70"), port)) {
            // Acquiring input and output streams for the client
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            oos = new ObjectOutputStream(os);
            ois = new ObjectInputStream(is);

            // Initial connection handling: Receive client ID from the server, so that it can be used
            // in future messages sent from this client to identify it with the server.
            try {
                Packet packet = (Packet) ois.readObject();
                id = packet.senderId;

                // Color of the client's pen is set according to its client id on the server
                setColor();

                // Show a hello message on initial connection with server
                if (packet.token.equals("CONNECT")) {
                    JOptionPane.showMessageDialog(grid, "You are player " + id + "!\n Your color is " + colorName + "!");
                }

                // Dialog box for host to start game (Assuming host id is always 1)
                if (this.id == 1) {
                    int numPlayers = 0;
                    int totalPlayers = 4;
                    boolean gameStarted = false;
                    String s = String.format("Number of Players: %d/%d", numPlayers, totalPlayers);
                    Window window = SwingUtilities.windowForComponent(grid);
                    JDialog d = new JDialog(window, "Host");
                    JLabel label = new JLabel((s), JLabel.CENTER);
                    JButton startButton = new JButton("Start Game");
                    JPanel panel = new JPanel();
                    panel.add(label);
                    panel.add(startButton);
                    d.add(panel);
                    d.setSize(300, 150);
                    d.setLocationRelativeTo(window);
                    d.setVisible(true);

                    // Event listener for start button
                    startButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            try {
                                oos.writeObject(new Packet("START", game, id));
                                d.dispose();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });

                    // Listen for new players to join until the start button is pressed
                    while(!gameStarted){
                        packet = (Packet) ois.readObject();

                        //Increase player count when a new player joins
                        if(packet.token.equals("NEW_PLAYER")) {
                            numPlayers++;
                            label.setText(String.format("Number of Players: %d/%d", numPlayers, totalPlayers));
                            System.out.println("Number of Players: " + numPlayers);
                            d.revalidate();
                            d.repaint();
                        }
                        if(packet.token.equals("START")) {
                            gameStarted = true;
                        }
                    }
                }
                else{
                    // Dialog box for waiting for host to start game
                    boolean gameStarted = false;
                    Window window = SwingUtilities.windowForComponent(grid);
                    JDialog d = new JDialog(window, "Host");
                    JLabel label = new JLabel("Waiting for the host to start . . .", JLabel.CENTER);
                    d.add(label);
                    d.setSize(300, 150);
                    d.setLocationRelativeTo(window);
                    while (!gameStarted&&this.id!=1){
                        d.setVisible(true);
                        Packet packetIn = (Packet) ois.readObject();
                        if(packetIn.token.equals("START")) {
                            d.dispose();
                            gameStarted = true;
                        }
                    }
                }                  
            }
            catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
            }   

            // Start the Read/write loop threads to sync client and server game states
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

        // set to id - 1 because the server assigns ids starting from 1
        colorName = colorPairs[id - 1].name;
        color = colorPairs[id - 1].color;
    }

    public Color getColor() {
        return color;
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

    public void setLastChangedSquare(int index) { lastChangedSquare = index; }

    public int getLastChangedSquare() { return lastChangedSquare; }

}

// The first of two sub-threads running on the Client. This thread is responsible for
// listening to the Server for messages and then updating the Client's game state accordingly.
class ServerListener implements Runnable {
    private final int id;
    private final ObjectInputStream ois;
    private final Game game;
    private final Grid grid;
    private final Client client;

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
                // DO FOREVER: Read messages from the server and update client game state
                Packet packetIn = (Packet) ois.readObject();

                // IMPORTANT !
                // Each packet contains a different TOKEN message which identifies which action the client should take
                switch (packetIn.token) {
                    // This message indicates that another player has taken a square and this client cannot
                    // access it
                    case "LOCK" -> {
                        int squareIndex = packetIn.index;
                        int senderId = packetIn.senderId;
                        game.getGameSquare(squareIndex).acquireLock(senderId);
                    }

                    // This message indicates that another player has drawn on a square and the client should
                    // update its board to mirror the server's game state
                    case "DRAW" -> {
                        InputStream in = new ByteArrayInputStream(packetIn.bytes);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        in.close();

                        // Avoid duplicate rendering of own square data (a client already draws its own UI - as
                        // such, there is no need to update it again from the server's broadcast)
                        if (packetIn.senderId != id) {
                            game.changeSquare(packetIn.index, bufferedImage);
                            client.setLastChangedSquare(id);
                            grid.updateImage(packetIn.index);
                            grid.repaintSquare(packetIn.index);
                        }
                    }

                    // This message indicates that another player is no longer using a square, and the lock
                    // should be released
                    case "UNLOCK" -> {
                        InputStream in = new ByteArrayInputStream(packetIn.bytes);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        in.close();
                        // Avoid duplicate rendering of own square data
                        if (packetIn.senderId != id) {
                            game.changeSquare(packetIn.index, bufferedImage);
                            client.setLastChangedSquare(id);
                            grid.updateImage(packetIn.index);
                            grid.repaintSquare(packetIn.index);
                        }
                        int squareIndex = packetIn.index;
                        game.getGameSquare(squareIndex).releaseLock();
                    }

                    // This message indicates that a square has been fully colored in and should be
                    // permanently locked
                    case "FULLY_COLOR" -> {
                        game.setSquareFullyColored(packetIn.index);
                    }

                    // This message indicates that the game is over and the client should show a popup
                    // message
                    case "GAMEOVER" -> {
                        JOptionPane.showMessageDialog(grid,game.winner(game.scores()));
                    }
                }
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

// The second of two sub-threads running on the Client. This thread is responsible for
// listening for any changes to the client's game state and retransmitting them for the server to mirror.
class UserInputListener implements Runnable {
    private final int id;
    private final ObjectOutputStream oos;
    private final Game game;
    private final Object lock;
    private final Client client;

    public UserInputListener(int id, ObjectOutputStream oos, Game game, Object lock, Client client) {
        this.id = id;
        this.oos = oos;
        this.game = game;
        this.lock = lock;
        this.client = client;
    }

    @Override
    public void run() {
        // WRITE LOOP - User mouse actions in the Square class cause different TOKEN messages
        // to be set, and then releases the lock so that the packet is sent to the server with
        // the data needed for the server to update it's game state.
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }

            try {
                // Creating the packet and sending it to server
                oos.writeObject(new Packet(client.getTokenMessage(), game, client.getLastChangedSquare(), id));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}