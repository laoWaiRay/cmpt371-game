import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Packet implements Serializable {
    String token = null;
    byte[] bytes;
    int index;
    int senderId;

    public Packet(String token, Game game, int senderId) {
        this.token = token;
        this.index = game.getLastChangedSquare();
        this.senderId = senderId;
        bytes = bufferedImageToByteArray(game.getSquare(this.index));
    }

    private byte[] bufferedImageToByteArray(BufferedImage img) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (IOException error) {
            System.out.println("Error converting image to bytes");
        }

        return baos.toByteArray();
    }
}

