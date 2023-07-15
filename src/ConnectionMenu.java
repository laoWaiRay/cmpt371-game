import javax.swing.*;
import java.awt.*;

public class ConnectionMenu extends JPanel {
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

        // serverPanel.setPreferredSize(new Dimension(250, 100));
        serverPanel.add(serverLabel);
        serverPanel.add(serverText);
        serverPanel.add(serverButton);
        // clientPanel.setPreferredSize(new Dimension(250, 100));
        clientPanel.add(clientLabel);
        clientPanel.add(clientText);
        clientPanel.add(clientButton);

        add(serverPanel, BorderLayout.WEST);
        add(clientPanel, BorderLayout.EAST);
    }
}
