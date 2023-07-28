import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.awt.Graphics;

import javax.swing.JFrame;

public class Game implements Serializable {
    private GameSquare[] squares = new GameSquare[25];
    private int lastChangedSquare = 0;
    private boolean isStillDrawing = false;

    public Game() {
        for (int i = 0; i < 25; i++) {
            squares[i] = new GameSquare();
        }
    }

    public synchronized GameSquare getGameSquare(int id) {
        return squares[id];
    }

    public synchronized void changeSquare(int index, BufferedImage newImage) {
        squares[index].setImage(newImage);
        lastChangedSquare = index;
    }

    public synchronized BufferedImage getSquareImage(int index) {
        return squares[index].getImage();
    }

    public synchronized int getLastChangedSquare() {
        return lastChangedSquare;
    }

    public synchronized void setLastChangedSquare(int squareIndex) {
        lastChangedSquare = squareIndex;
    }

    public synchronized void setStillDrawing(boolean stillDrawing) {
        isStillDrawing = stillDrawing;
    }

    public synchronized boolean getIsStillDrawing() {
        return isStillDrawing;
    }

    //checks all squares to see if any square is still blank/white 
    public synchronized boolean isGameFinished(){
        for (int i = 0; i < 25; i++){
            int rgb = getSquareImage(i).getRGB(50, 50);
            Color colour = new Color(rgb);
            Color def = new Color(0, 0,0);
            if(colour.equals(Color.WHITE) | colour.equals(def)){
                return false;
            }
        }
        return true;
    }
}

class GameSquare {
    private BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    private int lockHolderId = 0;   // Client Ids start from 1
    private boolean fullyColored = false;

    // Only allow one client to have access to the square at a time
    public void acquireLock(int clientId) {
        if (lockHolderId != 0 || fullyColored) return;  // If someone else currently has the lock, give up
        lockHolderId = clientId;
    }

    public void releaseLock() {
        lockHolderId = 0;
    }

    public boolean hasAccess(int clientId) {
        return clientId == lockHolderId;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setFullyColored(boolean fullyColored) {
        this.fullyColored = fullyColored;
    }

    public boolean getFullyColored() {
        return fullyColored;
    }
}
