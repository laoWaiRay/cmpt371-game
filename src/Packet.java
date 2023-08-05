import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Packet implements Serializable {
    String token;
    byte[] bytes;
    int index;
    int senderId;

    public Packet(String token, Game game, int senderId) {
        this.token = token;
        this.senderId = senderId;
        bytes = bufferedImageToByteArray(game.getSquareImage(this.index));
        // System.out.println("PACKET MADE WITH SQUARE INDEX");
        // System.out.println(this.index);
        // System.out.println("AND TOKEN");
        // System.out.println(this.token);
    }

    public Packet(String token, Game game, int squareIndex, int senderId) {
        this.token = token;
        this.index = squareIndex;
        this.senderId = senderId;
        bytes = bufferedImageToByteArray(game.getSquareImage(this.index));
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


