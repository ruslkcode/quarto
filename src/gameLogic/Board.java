package gameLogic;

/**
 * Represents a 4x4 Quarto board.
 * The board stores pieces placed on the fields and provides
 * methods to query and update the board state.
 * A field is empty if it contains {@code null}.
 */
public class Board {

    public static final int DIM = 4;

    private final Piece[] fields;

    /*@
      private invariant fields != null;
      private invariant fields.length == DIM * DIM;
    @*/

    /**
     * Constructs an empty 4x4 board.
     * All fields are initially empty.
     */
    /*@
      ensures (\forall int i; 0 <= i && i < DIM*DIM; fields[i] == null);
    @*/
    public Board() {
        this.fields = new Piece[DIM * DIM];
    }

    /**
     * Creates a copy of this board.
     * The pieces themselves are not copied, as they are immutable.
     *
     * @return a copy of this board
     */
    /*@
      ensures \result != this;
      ensures (\forall int i; 0 <= i && i < DIM*DIM;
               \result.fields[i] == this.fields[i]);
    @*/
    public Board deepCopy() {
        Board result = new Board();
        for (int i = 0; i < fields.length; i++) {
            result.fields[i] = this.fields[i];
        }
        return result;
    }

    /**
     * Converts a (row, col) pair to a linear index.
     *
     * @param row the row index
     * @param col the column index
     * @return the corresponding linear index
     */
    /*@
      requires 0 <= row && row < DIM;
      requires 0 <= col && col < DIM;
      ensures \result == row * DIM + col;
    @*/
    public int index(int row, int col) {
        return row * DIM + col;
    }

    /**
     * Checks whether an index refers to a valid board field.
     *
     * @param index the index to check
     * @return true if the index is valid
     */
    /*@
      ensures \result == (0 <= index && index < DIM*DIM);
    @*/
    public boolean isField(int index) {
        return index >= 0 && index < fields.length;
    }

    /**
     * Checks whether a (row, col) pair refers to a valid board field.
     *
     * @param row the row index
     * @param col the column index
     * @return true if the pair is valid
     */
    /*@
      ensures \result == (0 <= row && row < DIM && 0 <= col && col < DIM);
    @*/
    public boolean isField(int row, int col) {
        return row >= 0 && row < DIM && col >= 0 && col < DIM;
    }

    /**
     * Returns the piece stored at the given index.
     *
     * @param i the field index
     * @return the piece on the field, or null if the field is empty
     */
    /*@
      requires isField(i);
    @*/
    public Piece getField(int i) {
        return fields[i];
    }

    /**
     * Returns the piece stored at the given (row, col) position.
     *
     * @param row the row index
     * @param col the column index
     * @return the piece on the field, or null if the field is empty
     */
    /*@
      requires isField(row, col);
    @*/
    public Piece getField(int row, int col) {
        return fields[index(row, col)];
    }

    /**
     * Checks whether a field is empty.
     *
     * @param i the field index
     * @return true if the field is empty
     */
    /*@
      requires isField(i);
      ensures \result == (fields[i] == null);
    @*/
    public boolean isEmptyField(int i) {
        return fields[i] == null;
    }

    /**
     * Places a piece on the given field.
     *
     * @param i the field index
     * @param p the piece to place
     */
    /*@
      requires isField(i);
      requires fields[i] == null;
      ensures fields[i] == p;
    @*/
    public void setField(int i, Piece p) {
        fields[i] = p;
    }

    /**
     * Checks whether the board is completely filled.
     *
     * @return true if all fields are occupied
     */
    /*@
      ensures \result == (\forall int i; 0 <= i && i < DIM*DIM; fields[i] != null);
    @*/
    public boolean isFull() {
        for (int i = 0; i < DIM * DIM; i++) {
            if (fields[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether a row exists that is completely filled and whose pieces
     * share at least one common attribute.
     *
     * @return true if such a row exists
     */
    public boolean hasRow() {
        for (int row = 0; row < DIM; row++) {
            Piece[] pieces = new Piece[DIM];
            for (int col = 0; col < DIM; col++) {
                pieces[col] = getField(row, col);
            }
            if (hasCommonAttribute(pieces)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a column exists that is completely filled and whose pieces
     * share at least one common attribute.
     *
     * @return true if such a column exists
     */
    public boolean hasColumn() {
        for (int col = 0; col < DIM; col++) {
            Piece[] pieces = new Piece[DIM];
            for (int row = 0; row < DIM; row++) {
                pieces[row] = getField(row, col);
            }
            if (hasCommonAttribute(pieces)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether one of the two diagonals is completely filled and whose
     * pieces share at least one common attribute.
     *
     * @return true if such a diagonal exists
     */
    public boolean hasDiagonal() {
        Piece[] pieces = new Piece[DIM];

        for (int i = 0; i < DIM; i++) {
            pieces[i] = getField(i, i);
        }
        if (hasCommonAttribute(pieces)) return true;

        for (int i = 0; i < DIM; i++) {
            pieces[i] = getField(i, DIM - 1 - i);
        }
        return hasCommonAttribute(pieces);
    }

    /**
     * Checks whether the board contains a winning configuration.
     * A winning configuration exists if at least one row, column,
     * or diagonal shares a common attribute.
     *
     * @return true if the board has a winner
     */
    public boolean hasWinner() {
        return hasRow() || hasColumn() || hasDiagonal();
    }

    /**
     * Checks whether the game is over.
     * The game is over if the board has a winner or is completely full.
     *
     * @return true if the game is over
     */
    /*@
      ensures \result == (hasWinner() || isFull());
    @*/
    public boolean gameOver() {
        return hasWinner() || isFull();
    }

    /**
     * Checks whether the given pieces share at least one common attribute.
     *
     * @param pieces an array of pieces
     * @return true if all pieces are non-null and share a common attribute
     */
    private boolean hasCommonAttribute(Piece[] pieces) {
        for (Piece p : pieces) {
            if (p == null) return false;
        }

        Colour c = pieces[0].getColour();
        boolean sameColour = true;
        for (Piece p : pieces) {
            if (p.getColour() != c) {
                sameColour = false;
                break;
            }
        }
        if (sameColour) return true;

        Shape s = pieces[0].getShape();
        boolean sameShape = true;
        for (Piece p : pieces) {
            if (p.getShape() != s) {
                sameShape = false;
                break;
            }
        }
        if (sameShape) return true;

        Size sz = pieces[0].getSize();
        boolean sameSize = true;
        for (Piece p : pieces) {
            if (p.getSize() != sz) {
                sameSize = false;
                break;
            }
        }
        if (sameSize) return true;

        Fill f = pieces[0].getFill();
        boolean sameFill = true;
        for (Piece p : pieces) {
            if (p.getFill() != f) {
                sameFill = false;
                break;
            }
        }
        return sameFill;
    }
}
