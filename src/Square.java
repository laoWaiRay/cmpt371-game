 /*   General game flow for the player

  *   1) On Click, the client sends a token to the server indicating it wants to acquire
  *      a lock for the square
  *
  *   2) Lock is only given if not currently acquired by another player, AND
  *      the square has not been fully colored
  *
  *   3) Once acquired, dragging the mouse allows the client to color on the square
  *
  *   4) On mouse release, a release token is sent to the server indicating the square
  *      should be unlocked. Additionally, every time the mouse is released, the server
  *      is sent a final snapshot of the square, which it then uses to calculate whether
  *      the square should be fully colored in or reset back to white. If all squares are
  *      colored in, the server broadcasts a GAME OVER message to all players
  * */
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Square extends JPanel implements MouseMotionListener, MouseListener {
    private BufferedImage img;
    private Color brush_color = null;
    public int id;
    final static int width = 100;
    final static int height = 100;
    final static int brush_size = 10;
    private Client client;
    private final Game game;
    private final Object lock;

    public Square(int id, Client client, Game game, Object lock) {
        super();
        this.id = id;
        setPreferredSize(new Dimension(width, height));
        setBorder(BorderFactory.createLineBorder(Color.black, 1));
        setBackground(Color.WHITE);

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.addMouseMotionListener(this);
        this.addMouseListener(this);

        this.client = client;
        this.game = game;
        this.lock = lock;
    }

    public void setImage(BufferedImage image) {
        img = image;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setColors() {
        brush_color = client.getColor();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (client == null) return;
        if (brush_color == null)
            setColors();

        // Only update the game state if this client has acquired the lock
        if (game.getGameSquare(id).hasAccess(client.getClientId())) {
            client.setTokenMessage("DRAW");
            Graphics g = img.getGraphics();

            // Coloring the square
            g.setColor(brush_color);
            Point p = e.getPoint();
            g.fillOval(p.x - brush_size, p.y - brush_size, brush_size, brush_size);

            // NOTE: Clients always update their own UI when coloring a square, for more responsiveness
            // on the client side. It also sends a message to the server with this information, which
            // the server broadcasts to everyone. The client simply ignores the DRAW message if the
            // sender ID matches itself.
            synchronized (lock) {
                g.dispose();
                game.changeSquare(id, img);
                client.setLastChangedSquare(id);
                lock.notifyAll();
                repaint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!game.getGameSquare(id).hasAccess(client.getClientId())) return;

        double percentColored = calculateColoredPercentage();
        BufferedImage updatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = updatedImage.getGraphics();

        if (percentColored > 0.5) {
            g.setColor(brush_color);
        } else {
            g.setColor(Color.WHITE);
        }

        g.fillRect(0, 0, width, height);
        img = updatedImage;
        g.dispose();
        repaint();

        // On mouse release, send a message to the server to unlock the square
        synchronized (lock) {
            game.changeSquare(id, img);
            client.setTokenMessage("UNLOCK");
            client.setLastChangedSquare(id);
            lock.notifyAll();
        }
    }

    @Override
    public void mousePressed (MouseEvent e) {
        // Whenever the mouse is pressed, attempt to acquire the lock for this square
        synchronized (lock) {
            client.setLastChangedSquare(id);
            client.setTokenMessage("LOCK");
            lock.notifyAll();
        }
    }

    @Override
    public void mouseClicked (MouseEvent e) {
    }
    @Override
    public void mouseEntered (MouseEvent e) {
    }
    @Override
    public void mouseExited (MouseEvent e) {
    }
    @Override
    public void mouseMoved(MouseEvent e) {
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(img, 0, 0, null);
    }

    // HELPER METHOD: returns percentage of square that is colored in
    private double calculateColoredPercentage() {
        int coloredPixels = 0;
        int totalPixels = width * height;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y);
                if(brush_color != null) {
                    if (pixel == brush_color.getRGB()) {
                        coloredPixels++;
                    }

                }
            }
        }

        return (double) coloredPixels / totalPixels;
    }
}
