/*  The Grid class simply lays out the UI for the 25 squares of the game.
 *  It contains UI methods for updating the images contained in the squares.
 *  The Square class is where all the action for handling user input happens.
 */
import java.awt.*;
import javax.swing.*;

public class Grid extends JPanel{
    private Square[] squares;
    private Client client;
    private final Game game;
    private final Object lock;

    public Grid(Client client, Game game, Object lock) {
        super(new GridLayout(5,5));
        this.client = client;
        this.game = game;
        this.lock = lock;
        initComponents();
    }

    private void initComponents() {
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setPreferredSize(new Dimension(500, 500));
        squares = new Square[25];
        for(int i = 0; i < 25; i++) {
            squares[i] = new Square(i, client, game, lock);
            add(squares[i]);
        }
    }

    public void setClient(Client client) {
        this.client = client;
        for (Square square : squares) {
            square.setClient(client);
        }
    }

    public synchronized void updateImage(int index) {
        squares[index].setImage(game.getSquareImage(index));
    }

    public synchronized void repaintSquare(int index) {
        squares[index].repaint();
    }
}
