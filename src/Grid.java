import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Grid extends JPanel{
    // Keep array of squares to track how much each one is colored in???
    private Square[] squares;

    public Grid() {
        super(new GridLayout(5,5));
        initComponents();
    }

    private void initComponents() {
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        setPreferredSize(new Dimension(500, 500));
        squares = new Square[25];
        for(int i = 0; i < 25; i++) {
            squares[i] = new Square(i);
            add(squares[i]);
        }
    }
}
