import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Thread {
    int port;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private Game game;
    private Grid grid;
    volatile boolean isRunning = true;

    public Client(int port, Game game, Grid grid) {
        this.port = port;
        this.game = game;
        this.grid = grid;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port)) {
            System.out.println("Connected to server!");
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            oos = new ObjectOutputStream(os);
            ois = new ObjectInputStream(is);

            // Read/write loop to sync game with server
            while(isRunning) {
                try {
                    // WRITE
                    oos.writeObject(new Packet("DRAW", game));

                    // READ
                    Packet packetIn = (Packet) ois.readObject();

                    InputStream in = new ByteArrayInputStream(packetIn.bytes);
                    BufferedImage bufferedImage = ImageIO.read(in);
                    in.close();

                    game.changeSquare(packetIn.index, bufferedImage);
                    grid.updateImage(packetIn.index);
                    grid.repaintSquare(packetIn.index);
                } catch (IOException error) {
                    System.out.println("Error syncing game with server");
                } catch (ClassNotFoundException error) {
                    System.out.println("Class not found");
                }
            }

            ois.close();
            oos.close();
        } catch (IOException error) {
            System.out.println("Error connecting to server");
        }
    }
}
