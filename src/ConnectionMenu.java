import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConnectionMenu extends JPanel {
    private Server server;
    private Client client;

    JPanel serverPanel = new JPanel(new FlowLayout());
    JPanel clientPanel = new JPanel(new FlowLayout());

    public ConnectionMenu() {
        super(new FlowLayout());
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

        // serverPanel.setPreferredSize(new Dimension(250, 100));
        serverPanel.add(serverLabel);
        serverPanel.add(serverText);
        serverPanel.add(serverButton);
        JButton serverStopButton = new JButton("Stop");
        serverStopButton.addActionListener(serverStopHandler);
        serverPanel.add(serverStopButton);
        // clientPanel.setPreferredSize(new Dimension(250, 100));
        clientPanel.add(clientLabel);
        clientPanel.add(clientText);
        clientPanel.add(clientButton);

        add(serverPanel, BorderLayout.WEST);
        add(clientPanel, BorderLayout.EAST);
    }

    private final ActionListener serverStartHandler = e -> {
        server = new Server(8080);
        server.start();
    };

    private final ActionListener serverStopHandler = e -> {
        server.stopServerSocket();
    };
}
