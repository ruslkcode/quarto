package gameLogic;

public class Move {
    private int location;
    private static final int NO_LOCATION = -1;
    private int nextPiece;

    /**
     * Constructor for the first move where only the next piece is asked.
     * @param nextPiece the chosen next piece.
     */
    public Move (int nextPiece) {
        this.nextPiece = nextPiece;
        location = NO_LOCATION;
    }

    /**
     * Constructor for the all moves except the first one where the next piece and the location
     * are asked.
     * @param nextPiece the chosen next piece.
     * @param location the chosen location for the previously chosen piece.
     */
    public Move (int nextPiece, int location) {
        this.nextPiece = nextPiece;
        this.location = location;
    }

    /**
     * Getter for the location parameter.
     * @return the location.
     */
    public int getLocation() {return this.location;}

    /**
     * Getter for the location parameter.
     * @return the nextPiece.
     */

    public int getNextPiece() {return this.nextPiece;}

    /**
     * Checks if it is the first move.
     * @return true if yes, no if true.
     */
    public boolean isFirstMove() {
        return location == NO_LOCATION;
    }

}
