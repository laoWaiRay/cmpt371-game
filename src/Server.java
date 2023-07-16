import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private ServerSocket serverSocket;
    int port;

    public Server(int port) {
        this.port = port;
        startServerSocket();
    }

    private void startServerSocket() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port: " + port);
        } catch (IOException error) {
            System.out.println("Error establishing server socket connection");
        }
    }

    public void stopServerSocket() {
        try {
            System.out.println("HERE@");
            serverSocket.close();
        } catch (IOException error) {
            System.out.println("Could not close server socket");
        }
    }

    // Accept client connections on a new thread to not block the stop call
    @Override
    public void run() {
        // Listen for client connections and create new threads
        Thread serverSocketHandler = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connected to client");
                    InputStream is = clientSocket.getInputStream();
                    OutputStream os = clientSocket.getOutputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    ObjectOutputStream oos = new ObjectOutputStream(os);

                    Thread thread = new Thread(new ClientHandler(clientSocket, ois, oos));
                    thread.start();
                } catch (IOException error) {
                    System.out.println("Error establishing client socket connection");
                    break;
                }
            }
        });

        serverSocketHandler.start();
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;

    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        System.out.println("Connected to server!: ");
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message msg = (Message) ois.readObject();
                if (msg.token.equals("EXIT")) break;
            } catch (IOException error) {
                System.out.println("Error reading from object stream");
                error.printStackTrace();
            } catch (ClassNotFoundException error) {
                System.out.println("Error reading from object stream: Class not found");
            }
        }

        try {
            ois.close();
            oos.close();
            socket.close();
        } catch (IOException error) {
            System.out.println("Error closing socket");
        }
    }
}
