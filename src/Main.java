import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JFrame {
    private MenuBar menuBar = new MenuBar();
    private ConnectionMenu connectionMenu;
    private Grid grid;
    private Game game = new Game();
    private Client client;
    private Server server;
    private final Object lock = new Object();

    public Main() {
        super("371 Game");
        grid = new Grid(client, game, lock);
        connectionMenu = new ConnectionMenu(game, client, this, grid, lock);

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

    public void setClient(Client client) {
        this.client = client;
        grid.setClient(client);
        System.out.println("Setting client in main to new client: " + this.client);
    }

    public static void main(String[] args) {
        new Main().setVisible(true);
    }
}
