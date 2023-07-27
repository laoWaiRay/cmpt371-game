import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.awt.Graphics;

import javax.swing.JFrame;

public class Game implements Serializable {
    private BufferedImage[] squares = new BufferedImage[25];
    private int lastChangedSquare = 0;
    private boolean isStillDrawing = false;

    public Game() {
        for (int i = 0; i < 25; i++) {
            squares[i] = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public synchronized void changeSquare(int index, BufferedImage newImage) {
        squares[index] = newImage;
        lastChangedSquare = index;
    }

    public synchronized BufferedImage getSquare(int index) {
        return squares[index];
    }

    public synchronized int getLastChangedSquare() {
        return lastChangedSquare;
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
            int rgb = getSquare(i).getRGB(50, 50);
            Color colour = new Color(rgb);
            Color def = new Color(0, 0,0);
            if(colour.equals(Color.WHITE) | colour.equals(def)){
                return false;
            }
        }
        return true;
    }
    
}
