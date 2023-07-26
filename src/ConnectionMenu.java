import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConnectionMenu extends JPanel {
    private Server server;
    private Client client;
    private Game game;
    private Main parent;
    private Grid grid;
    private Object lock;

    JPanel serverPanel = new JPanel(new FlowLayout());
    JPanel clientPanel = new JPanel(new FlowLayout());

    public ConnectionMenu(Game game, Client client, Main parent, Grid grid, Object lock) {
        super(new FlowLayout());
        this.parent = parent;
        this.game = game;
        this.client = client;
        this.grid = grid;
        this.lock = lock;

        initComponents();
    }

    private void initComponents() {
        setPreferredSize(new Dimension(500, 100));

        JLabel serverLabel = new JLabel("Start a server:");
        JLabel clientLabel = new JLabel("Join a server:");
        JTextField serverText = new JTextField(16);
        JTextField clientText =new JTextField(16);
        JButton serverButton = new JButton("Create");
        JButton clientButton = new JButton("Join");
        serverButton.addActionListener(serverStartHandler);
        clientButton.addActionListener(clientStartHandler);

        serverPanel.add(serverLabel);
        serverPanel.add(serverText);
        serverPanel.add(serverButton);
        JButton serverStopButton = new JButton("Stop");
        serverStopButton.addActionListener(serverStopHandler);
        serverPanel.add(serverStopButton);

        clientPanel.add(clientLabel);
        clientPanel.add(clientText);
        clientPanel.add(clientButton);

        add(serverPanel, BorderLayout.WEST);
        add(clientPanel, BorderLayout.EAST);
    }

    private final ActionListener serverStartHandler = e -> {
        server = new Server(8080, game, grid);
        server.start();
    };

    private final ActionListener serverStopHandler = e -> {
        server.stopServerSocket();
    };

    private final ActionListener clientStartHandler = e -> {
        client = new Client(8080, game, grid, lock);
        parent.setClient(client);
        client.start();
    };
}
