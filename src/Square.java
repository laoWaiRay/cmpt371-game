import javax.swing.JComponent;
import javax.swing.*;
import java.awt.*;

public class Square extends JComponent {
    public Square() {
        super();
        setSize(new Dimension(50, 50));
        setBorder(BorderFactory.createLineBorder(Color.black, 1));
    }
}
