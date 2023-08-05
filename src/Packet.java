import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

// Packets employ token-based messaging. In addition to a token, they also send
// a byte array containing the image for a square, the sender ID, and the index of
// the square that is being modified (if applicable)
//
// Tokens used include: CONNECT, START, NEW_PLAYER, DRAW, LOCK, UNLOCK, and GAMEOVER
public class Packet implements Serializable {
    String token;
    byte[] bytes;
    int index;
    int senderId;

    public Packet(String token, Game game, int senderId) {
        this.token = token;
        this.senderId = senderId;
        bytes = bufferedImageToByteArray(game.getSquareImage(this.index));
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


