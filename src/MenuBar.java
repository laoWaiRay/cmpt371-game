import javax.swing.*;

public class MenuBar extends JMenuBar{
    JMenu menu;
    JMenuItem closeServer;
    JMenuItem closeGame;

    public MenuBar() {
        super();
        initComponents();
    }

    private void initComponents() {
        JMenu menu = new JMenu("Menu");
        JMenuItem closeServer = new JMenuItem("Close Server");
        JMenuItem closeGame = new JMenuItem("Quit Game");

        menu.add(closeServer);
        menu.add(closeGame);
        this.add(menu);
    }
}
