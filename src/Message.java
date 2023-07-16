import java.awt.image.BufferedImage;

public class Message {
    String token = null;
    BufferedImage image = null;

    public Message(String token) {
        this.token = token;
    }

    public Message(BufferedImage image) {
        this.image = image;
    }
}
