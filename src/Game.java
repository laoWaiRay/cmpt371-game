import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.awt.Graphics;

import javax.swing.JFrame;

public class Game implements Serializable {
    private GameSquare[] squares = new GameSquare[25];
    private boolean isStillDrawing = false;

    public Game() {
        for (int i = 0; i < 25; i++) {
            squares[i] = new GameSquare(i);
        }
    }

    public GameSquare getGameSquare(int id) {
        return squares[id];
    }

    public void changeSquare(int index, BufferedImage newImage) {
        squares[index].setImage(newImage);
    }

    public BufferedImage getSquareImage(int index) {
        return squares[index].getImage();
    }

    public void setStillDrawing(boolean stillDrawing) {
        isStillDrawing = stillDrawing;
    }

    public boolean getIsStillDrawing() {
        return isStillDrawing;
    }

    //checks all squares to see if any square is still blank/white 
    public boolean isGameFinished(){
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

    public synchronized int[] scores(){
        int[] score = new int[4];
        for (int i = 0; i < 4; i++){
            score[i] = 0;
        }
        for (int i = 0; i < 25; i++){
            int rgb = getSquareImage(i).getRGB(50, 50);
            Color colour = new Color(rgb);
            if(colour.equals(Color.BLUE)){
                score[0] += 1;
            }
            else if (colour.equals(Color.RED)){
                score[1] += 1;
            }
            else if (colour.equals(Color.YELLOW)){
                score[2] += 1;
            }
            else if (colour.equals(Color.GREEN)){
                score[3] += 1;
            }
        }
        return score;
    }
    public synchronized String winner(int[] s){
        String winner = "Winner: ";
        int max = s[0];
        for (int i = 1; i < 4; i++){
            if (s[i] > max){
                max = s[i];
            }
        }
        if (s[0] == max){
            winner += "Blue ";
        }
        if (s[1] == max){
            winner += "Red ";
        }
        if (s[2] == max){
            winner += "Yellow ";
        }
        if (s[3] == max){
            winner += "Green ";
        }
        return winner;
    }



}

class GameSquare {
    private BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
    private int lockHolderId = 0;   // Client Ids start from 1
    private boolean fullyColored = false;
    private int squareId;

    public GameSquare(int squareId) {
        this.squareId = squareId;
    }

    // Only allow one client to have access to the square at a time
    public synchronized void acquireLock(int clientId) {
        if (lockHolderId != 0 || fullyColored) return;
        System.out.println("Acquired lock for square: ");
        System.out.println(squareId);
        lockHolderId = clientId;
    }

    public synchronized void releaseLock() {
        System.out.println("Releasing lock for square: " + String.valueOf(squareId));
        lockHolderId = 0;
    }

    public synchronized boolean hasAccess(int clientId) {
        return clientId == lockHolderId;
    }

    public synchronized void setImage(BufferedImage image) {
        this.image = image;
    }

    public synchronized BufferedImage getImage() {
        return image;
    }

    public synchronized void setFullyColored(boolean fullyColored) {
        this.fullyColored = fullyColored;
    }

    public synchronized boolean getFullyColored() {
        return fullyColored;
    }
}
