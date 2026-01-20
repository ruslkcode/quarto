package gameLogic;

import java.util.Objects;

public class Piece {
    private final Size size;
    private final Shape shape;
    private final Colour colour;
    private final Fill fill;

    /*@
      private invariant size != null;
      private invariant shape != null;
      private invariant colour != null;
      private invariant fill != null;
    @*/

    /**
     * Constructs the object Piece.
     * @param size the size of the piece.
     * @param shape the shape of the piece.
     * @param colour the colour of the piece.
     * @param fill the fill of the piece.
     */
    public Piece(Size size, Shape shape, Colour colour, Fill fill) {
        this.size = size;
        this.shape = shape;
        this.colour = colour;
        this.fill = fill;
    }

    /**
     * @return the shape of the current piece.
     */
    /*@ ensures \result == size; @*/
    public Size getSize() {
        return size;
    }

    /**
     * @return the shape of the current piece.
     */
    /*@ ensures \result == shape; @*/
    public Shape getShape() {
        return shape;
    }

    /**
     * @return the colour of the current piece.
     */
    /*@ ensures \result == colour; @*/
    public Colour getColour() {
        return colour;
    }

    /**
     * @return the fill of the current piece.
     */
    /*@ ensures \result == fill; @*/
    public Fill getFill() {
        return fill;
    }

    /**
     * Transfers to string the Piece object.
     */

    public String toString() {
        return colour + " " + shape + " " + size + " " + fill;
    }


    /**
     * Compare two different Piece objects.
     * @param o the reference object with which to compare.
     * @return true if objects are similar, false if not.
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece)) return false;
        Piece piece = (Piece) o;
        return size == piece.size &&
                shape == piece.shape &&
                colour == piece.colour &&
                fill == piece.fill;
    }

    /**
     * Transfers the objects in hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(size, shape, colour, fill);
    }
}
