// *  Main driver file for initializing the multiplayer game, "Deny and Conquer"
import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    private final MenuBar menuBar = new MenuBar();
    private final ConnectionMenu connectionMenu;
    private final Grid grid;
    private Client client;

    public Main() {
        super("CMPT 371 Deny and Conquer");
        Object lock = new Object();
        Game game = new Game();
        grid = new Grid(client, game, lock);
        connectionMenu = new ConnectionMenu(game, client, this, grid, lock);

        initComponents();

        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new FlowLayout());
        setJMenuBar(menuBar);
        add(connectionMenu);
        add(grid);
    }

    public void setClient(Client client) {
        this.client = client;
        grid.setClient(client);
    }

    public static void main(String[] args) {
        new Main().setVisible(true);
    }
}
