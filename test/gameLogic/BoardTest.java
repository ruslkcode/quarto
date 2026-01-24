package gameLogic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the class.
 */
public class BoardTest {

    /** Board instance used for testing. */
    private Board board;

    /** Test pieces sharing size and colour attributes. */
    private Piece largeBlackSquareSolid;
    private Piece largeBlackRoundSolid;
    private Piece largeBlackSquareHollow;
    private Piece largeBlackRoundHollow;

    /**
     * Creates a fresh board and test pieces before each test.
     */
    @BeforeEach
    void setUp() {
        board = new Board();

        largeBlackSquareSolid =
                new Piece(Size.LARGE, Shape.SQUARE, Colour.BLACK, Fill.SOLID);
        largeBlackRoundSolid =
                new Piece(Size.LARGE, Shape.ROUND, Colour.BLACK, Fill.SOLID);
        largeBlackSquareHollow =
                new Piece(Size.LARGE, Shape.SQUARE, Colour.BLACK, Fill.HOLLOW);
        largeBlackRoundHollow =
                new Piece(Size.LARGE, Shape.ROUND, Colour.BLACK, Fill.HOLLOW);
    }

    /**
     * Verifies that a newly created board is empty.
     */
    @Test
    void boardStartsEmpty() {
        for (int i = 0; i < 16; i++) {
            assertTrue(board.isEmptyField(i));
            assertNull(board.getField(i));
        }
    }

    /**
     * Tests conversion from row/column coordinates to linear index.
     */
    @Test
    void indexCalculationWorks() {
        assertEquals(0, board.index(0, 0));
        assertEquals(5, board.index(1, 1));
        assertEquals(15, board.index(3, 3));
    }

    /**
     * Tests whether index bounds checking works correctly.
     */
    @Test
    void isFieldChecksBounds() {
        assertTrue(board.isField(0));
        assertTrue(board.isField(15));
        assertFalse(board.isField(-1));
        assertFalse(board.isField(16));

        assertTrue(board.isField(0, 0));
        assertTrue(board.isField(3, 3));
        assertFalse(board.isField(4, 0));
        assertFalse(board.isField(0, 4));
    }

    /**
     * Tests placing a piece on the board and retrieving it.
     */
    @Test
    void setAndGetFieldWorks() {
        board.setField(0, largeBlackSquareSolid);

        assertFalse(board.isEmptyField(0));
        assertEquals(largeBlackSquareSolid, board.getField(0));
    }

    /**
     * Tests that {@link Board#deepCopy()} creates an independent copy.
     */
    @Test
    void deepCopyCreatesIndependentBoard() {
        board.setField(0, largeBlackSquareSolid);

        Board copy = board.deepCopy();

        assertNotSame(board, copy);
        assertEquals(board.getField(0), copy.getField(0));

        board.setField(1, largeBlackRoundSolid);
        assertNull(copy.getField(1));
    }

    /**
     * Ensures the board is not full immediately after creation.
     */
    @Test
    void boardIsNotFullInitially() {
        assertFalse(board.isFull());
    }

    /**
     * Ensures the board reports full when all fields are occupied.
     */
    @Test
    void boardIsFullWhenAllFieldsOccupied() {
        for (int i = 0; i < 16; i++) {
            board.setField(i, largeBlackSquareSolid);
        }
        assertTrue(board.isFull());
    }

    /**
     * Tests detection of a winning row.
     */
    @Test
    void detectsWinningRow() {
        board.setField(board.index(0, 0), largeBlackSquareSolid);
        board.setField(board.index(0, 1), largeBlackRoundSolid);
        board.setField(board.index(0, 2), largeBlackSquareHollow);
        board.setField(board.index(0, 3), largeBlackRoundHollow);

        assertTrue(board.hasRow());
        assertTrue(board.hasWinner());
    }

    /**
     * Tests detection of a winning column.
     */
    @Test
    void detectsWinningColumn() {
        board.setField(board.index(0, 1), largeBlackSquareSolid);
        board.setField(board.index(1, 1), largeBlackRoundSolid);
        board.setField(board.index(2, 1), largeBlackSquareHollow);
        board.setField(board.index(3, 1), largeBlackRoundHollow);

        assertTrue(board.hasColumn());
        assertTrue(board.hasWinner());
    }

    /**
     * Tests detection of a winning diagonal.
     */
    @Test
    void detectsWinningDiagonal() {
        board.setField(board.index(0, 0), largeBlackSquareSolid);
        board.setField(board.index(1, 1), largeBlackRoundSolid);
        board.setField(board.index(2, 2), largeBlackSquareHollow);
        board.setField(board.index(3, 3), largeBlackRoundHollow);

        assertTrue(board.hasDiagonal());
        assertTrue(board.hasWinner());
    }

    /**
     * Verifies that the game ends when a winner exists.
     */
    @Test
    void gameOverWhenWinnerExists() {
        detectsWinningRow();
        assertTrue(board.gameOver());
    }

    /**
     * Verifies that the game ends when the board is completely full.
     */
    @Test
    void gameOverWhenBoardIsFull() {
        for (int i = 0; i < 16; i++) {
            board.setField(i, largeBlackSquareSolid);
        }
        assertTrue(board.gameOver());
    }

    /**
     * Tests detection of a shared attribute among pieces.
     */
    @Test
    void hasCommonAttributeReturnsTrue() {
        Piece[] pieces = {
                largeBlackSquareSolid,
                largeBlackRoundSolid,
                largeBlackSquareHollow,
                largeBlackRoundHollow
        };

        assertTrue(board.hasCommonAttribute(pieces));
    }

    /**
     * Ensures that a null piece prevents attribute matching.
     */
    @Test
    void hasCommonAttributeFailsWithNullPiece() {
        Piece[] pieces = {
                largeBlackSquareSolid,
                null,
                largeBlackSquareHollow,
                largeBlackRoundHollow
        };

        assertFalse(board.hasCommonAttribute(pieces));
    }

    /**
     * Ensures that pieces without any shared attribute are not considered winning.
     */
    @Test
    void hasCommonAttributeFailsWithoutSharedProperty() {
        Piece p1 = new Piece(Size.LARGE, Shape.SQUARE, Colour.BLACK, Fill.SOLID);
        Piece p2 = new Piece(Size.SMALL, Shape.ROUND, Colour.WHITE, Fill.HOLLOW);
        Piece p3 = new Piece(Size.LARGE, Shape.ROUND, Colour.WHITE, Fill.SOLID);
        Piece p4 = new Piece(Size.SMALL, Shape.SQUARE, Colour.BLACK, Fill.HOLLOW);

        assertFalse(board.hasCommonAttribute(new Piece[]{p1, p2, p3, p4}));
    }
}
