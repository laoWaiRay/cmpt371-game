import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Square extends JPanel implements MouseMotionListener, MouseListener {
    private BufferedImage img;
    private Color brush_color = Color.BLUE;
    public int id;
    final static int width = 100;
    final static int height = 100;
    final static int brush_size = 10;
    private Client client;
    private Game game;
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

    public void setBrushColor(Color color) {
        brush_color = color;
    }

    public void setImage(BufferedImage image) {
        img = image;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Graphics g = img.getGraphics();

        // COLOR THE SQUARE
        g.setColor(brush_color);
        Point p = e.getPoint();
        g.fillOval(p.x - brush_size, p.y - brush_size, brush_size, brush_size);

        // UPDATE GAME STATE WITH NEW BUFFERED IMAGE
        synchronized (lock) {
            g.dispose();
            game.changeSquare(id, img);
            game.setStillDrawing(true);
            lock.notifyAll();
            repaint();
        }
    }

    // RETURNS Percentage of square that is colored in
    private double calculateColoredPercentage() {
        int coloredPixels = 0;
        int totalPixels = width * height;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y);
                if (pixel == brush_color.getRGB()) {
                    coloredPixels++;
                }
            }
        }

        return (double) coloredPixels / totalPixels;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        double percentColored = calculateColoredPercentage();
        if (percentColored > 0.5) {
            setBackground(brush_color);
        } else {
            setBackground(Color.WHITE);
        }

        game.changeSquare(id, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    @Override
    public void mousePressed (MouseEvent e) {
    /* TODO - 2023/7/15 | 17:41 | raymondly
    *   LOCK THE SQUARE WHILE A USER IS DRAGGING MOUSE INSIDE OF IT
    * */
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
}
