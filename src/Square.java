import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Square extends JPanel implements MouseMotionListener, MouseListener {
    private BufferedImage img;
    private String color_name;
    private Color brush_color = null;
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

    public void setColors() {
        brush_color = client.getColor();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        /* TODO - 2023/7/26 | 01:29 | raymondly
        *   INITIALLY, ALL SQUARES SHOULD BE LOCKED ON THE SERVER.
        *   CLIENT SENDS A TOKEN REQUEST TO SERVER FOR THE LOCK ON THE SQUARE,
        *   BEFORE ALLOWING THE USER TO DRAW.
        *   LOCK SHOULD ONLY BE GIVEN IF THE SQUARE IS NOT CURRENTLY ACQUIRED
        *   BY ANOTHER PLAYER, AND THE SQUARE IS NOT FULLY COLORED.
        *   ONCE ACQUIRED, THE USER CAN UPDATE THE SQUARE
        *   ON MOUSE RELEASE, SEND A RELEASE TOKEN TO INDICATE
        *   TO SERVER TO RELEASE THE LOCK
        * */
        if (client == null) return;
        if (brush_color == null)
            setColors();

        // Only update the game state if this client has acquired access to the square from server
        if (game.getGameSquare(id).hasAccess(client.getClientId())) {
            client.setTokenMessage("DRAW");
            Graphics g = img.getGraphics();

            // COLOR THE SQUARE
            g.setColor(brush_color);
            Point p = e.getPoint();
            g.fillOval(p.x - brush_size, p.y - brush_size, brush_size, brush_size);

            // UPDATE GAME STATE WITH NEW BUFFERED IMAGE
            synchronized (lock) {
                g.dispose();
                game.changeSquare(id, img);
                client.setLastChangedSquare(id);
                game.setStillDrawing(true);
                lock.notifyAll();
                repaint();
            }
        }
    }

    // RETURNS Percentage of square that is colored in
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

    @Override
    public void mouseReleased(MouseEvent e) {
        System.out.println(game.getGameSquare(id).hasAccess(client.getClientId()));
        System.out.println(game.getGameSquare(id));
        System.out.println(client.getClientId());
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

        // UPDATE GAME STATE WITH NEW BUFFERED IMAGE
        synchronized (lock) {
            game.changeSquare(id, img);
            game.setStillDrawing(true);
            client.setTokenMessage("UNLOCK");
            client.setLastChangedSquare(id);
            System.out.println("DEBUGGING last square id " + String.valueOf(id));
            lock.notifyAll();
            // repaint();
            if(game.isGameFinished()){
                //System.out.println("Game Over");
                int [] scores = game.scores();
                for(int i =0; i<4;i++){
                    System.out.println(scores[i]);
                }
                String s = game.winner(scores);
                System.out.println("Winner: " + s);
                
                //change screen to game over screen with winner shown
            }
        }
    }

    @Override
    public void mousePressed (MouseEvent e) {
    /* TODO - 2023/7/15 | 17:41 | raymondly
    *   LOCK THE SQUARE WHILE A USER IS DRAGGING MOUSE INSIDE OF IT
    * */
        synchronized (lock) {
            System.out.println("DEBUG2 sq id" + String.valueOf(id));
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
}
