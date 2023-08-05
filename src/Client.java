import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;

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
                if(packet.token.equals("CONNECT")) {
                        JOptionPane.showMessageDialog(grid, "You are player " + id + "!\n Your color is " + colorName + "!");
                }
                oos.writeObject(new Packet("CONNECT", game, id));
                //Dialog box for host to start game (Assuming host id is always 1)
                if(this.id==1) {
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
                    //Event listener for start button
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
                    //Listen for new players to join until the start button is pressed
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
                    //Dialog box for waiting for host to start game
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
                    case "LOCK" -> {
                        int squareIndex = packetIn.index;
                        int senderId = packetIn.senderId;
                        game.getGameSquare(squareIndex).acquireLock(senderId);
                    }
                    case "DRAW" -> {
                        InputStream in = new ByteArrayInputStream(packetIn.bytes);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        in.close();
                        // Avoid duplicate rendering of own square data
                        if (packetIn.senderId != id) {
                            game.changeSquare(packetIn.index, bufferedImage);
                            grid.updateImage(packetIn.index);
                            grid.repaintSquare(packetIn.index);
                        }
                    }
                    case "UNLOCK" -> {
                        InputStream in = new ByteArrayInputStream(packetIn.bytes);
                        BufferedImage bufferedImage = ImageIO.read(in);
                        in.close();
                        // Avoid duplicate rendering of own square data
                        if (packetIn.senderId != id) {
                            game.changeSquare(packetIn.index, bufferedImage);
                            grid.updateImage(packetIn.index);
                            grid.repaintSquare(packetIn.index);
                            System.out.println("HERE" + " Sender id: " + packetIn.senderId + ", my id: " + id);
                        }
                        int squareIndex = packetIn.index;
                        game.getGameSquare(squareIndex).releaseLock();
                    }
                }
                if (game.isGameFinished()){
                    JOptionPane.showMessageDialog(grid,game.winner(game.scores()));
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