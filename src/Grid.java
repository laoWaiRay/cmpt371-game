import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class Grid extends JPanel{
    // Keep array of squares to track how much each one is colored in???
    private Square[] squares;
    private Client client;
    private Game game;

    public Grid(Client client, Game game) {
        super(new GridLayout(5,5));
        this.client = client;
        System.out.println(client);
        this.game = game;
        initComponents();
    }

    private void initComponents() {
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setPreferredSize(new Dimension(500, 500));
        squares = new Square[25];
        for(int i = 0; i < 25; i++) {
            squares[i] = new Square(i, client, game);
            add(squares[i]);
        }
    }

    public void setClient(Client client) {
        this.client = client;
        for (Square square : squares) {
            square.setClient(client);
        }
    }

    public void updateImage(int index) {
        squares[index].setImage(game.getSquare(index));
    }

    public void repaintSquare(int index) {
        squares[index].repaint();
    }
}
