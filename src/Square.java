import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Square extends JPanel implements MouseMotionListener {
    BufferedImage img;
    final static int width = 500;
    final static int height = 500;

    public Square() {
        super();
        setPreferredSize(new Dimension(width, height));
        setBorder(BorderFactory.createLineBorder(Color.black, 1));

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        this.addMouseMotionListener(this);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Graphics g = img.getGraphics();
        g.setColor(Color.BLUE);  // SET A BORDER COLOR
        g.drawRect(1, 1, width - 2, height - 2);

        // COLOR THE SQUARE
        g.setColor(Color.blue);
        Point p = e.getPoint();
        g.fillOval(p.x,p.y,5,5);

        g.dispose();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }
}
