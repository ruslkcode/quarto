package gameLogic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the class.
 */
public class GameTest {

    /** Game instance used in tests. */
    private Game game;

    /**
     * Creates a fresh game before each test.
     */
    @BeforeEach
    void setUp() {
        game = new Game(1);
    }

    /**
     * Verifies correct initialization of a new game.
     */
    @Test
    void gameInitialStateIsCorrect() {
        assertEquals(1, game.getCurrentPlayer());
        assertEquals(-1, game.getCurrentPieceID());
        assertNotNull(game.getBoard());
        assertEquals(16, game.getAvailablePieces().size());
        assertEquals(16, game.getAllPieces().size());
    }

    /**
     * Tests that all piece IDs are created and available at the start.
     */
    @Test
    void allPiecesAreInitialized() {
        for (int i = 0; i < 16; i++) {
            assertTrue(game.getAllPieces().containsKey(i));
            assertTrue(game.getAvailablePieces().containsKey(i));
        }
    }

    /**
     * Tests validity of the very first move (choosing a piece).
     */
    @Test
    void firstMoveIsValidWhenChoosingPiece() {
        Move firstMove = new Move(3);
        assertTrue(game.isValidMove(firstMove));
    }

    /**
     * Ensures the first move is invalid if a location is provided.
     */
    @Test
    void firstMoveWithLocationIsInvalid() {
        Move invalid = new Move(0, 5);
        assertFalse(game.isValidMove(invalid));
    }

    /**
     * Verifies that executing the first move selects a piece
     * and switches the current player.
     */
    @Test
    void firstMoveUpdatesStateCorrectly() {
        game.doMove(new Move(4));

        assertEquals(2, game.getCurrentPlayer());
        assertEquals(4, game.getCurrentPieceID());
        assertFalse(game.getAvailablePieces().containsKey(4));
    }

    /**
     * Tests a valid regular move after the first move.
     */
    @Test
    void regularMoveIsValid() {
        game.doMove(new Move(2)); // first move
        Move move = new Move(3, 0);

        assertTrue(game.isValidMove(move));
    }

    /**
     * Tests that a regular move updates the board and switches turns.
     */
    @Test
    void regularMoveUpdatesGameState() {
        game.doMove(new Move(1));      // player 1 chooses piece
        game.doMove(new Move(2, 0));   // player 2 places piece

        assertEquals(1, game.getCurrentPlayer());
        assertEquals(2, game.getCurrentPieceID());
        assertNotNull(game.getBoard().getField(0));
    }

    /**
     * Ensures that placing on an occupied field is invalid.
     */
    @Test
    void moveOnOccupiedFieldIsInvalid() {
        game.doMove(new Move(1));
        game.doMove(new Move(2, 0));

        Move invalid = new Move(3, 0);
        assertFalse(game.isValidMove(invalid));
    }

    /**
     * Ensures that using an unavailable piece is invalid.
     */
    @Test
    void moveWithUnavailablePieceIsInvalid() {
        game.doMove(new Move(1));
        game.doMove(new Move(2, 0));

        Move invalid = new Move(1, 1);
        assertFalse(game.isValidMove(invalid));
    }

    /**
     * Tests winner detection when a winning row is formed.
     */
    @Test
    void winnerIsDetectedCorrectly() {
        // Prepare winning row for player 1
        game.doMove(new Move(0));              // P1 → choose piece
        game.doMove(new Move(1, 0));           // P2
        game.doMove(new Move(2, 1));           // P1
        game.doMove(new Move(3, 4));           // P2
        game.doMove(new Move(4, 2));           // P1
        game.doMove(new Move(5, 8));           // P2
        game.doMove(new Move(6, 3));           // P1 completes row

        assertTrue(game.getBoard().hasWinner());
        assertEquals(game.getCurrentPlayer(), game.getWinner());
    }

    /**
     * Tests that the game reports game-over when the board is full.
     */
    @Test
    void gameOverWhenBoardIsFull() {
        Game g = new Game(1);

        // force board full manually
        Board b = g.getBoard();
        Piece p = new Piece(Size.LARGE, Shape.SQUARE, Colour.BLACK, Fill.SOLID);

        for (int i = 0; i < 16; i++) {
            b.setField(i, p);
        }

        assertTrue(g.isGameOver());
    }


    /**
     * Ensures that deepCopy creates an independent game instance.
     */
    @Test
    void deepCopyCreatesIndependentGame() {
        game.doMove(new Move(5));
        Game copy = game.deepCopy();

        assertNotSame(game, copy);
        assertEquals(game.getCurrentPlayer(), copy.getCurrentPlayer());
        assertEquals(game.getCurrentPieceID(), copy.getCurrentPieceID());

        game.doMove(new Move(6, 0));
        assertNull(copy.getBoard().getField(0));
    }
}
