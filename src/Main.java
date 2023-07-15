import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JFrame {
    private MenuBar menuBar = new MenuBar();
    private ConnectionMenu connectionMenu = new ConnectionMenu();
    private Grid grid = new Grid();

    public Main() {
        super("371 Game");
        initComponents();

        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new FlowLayout());
        setJMenuBar(menuBar);
        add(connectionMenu);
        add(grid);
    }

    public static void main(String[] args) {
        new Main().setVisible(true);
    }
}
