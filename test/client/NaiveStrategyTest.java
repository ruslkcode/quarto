package client;

import gameLogic.Game;
import gameLogic.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NaiveStrategy}.
 *
 * Tests focus on correctness and safety of generated moves,
 * not on randomness or strategy quality.
 */
public class NaiveStrategyTest {

    private NaiveStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new NaiveStrategy();
    }

    /**
     * Strategy must always have a name.
     */
    @Test
    void nameIsCorrect() {
        assertEquals("Naive", strategy.getName());
    }

    /**
     * On the first move, the strategy must only choose a piece.
     */
    @Test
    void firstMoveReturnsPieceOnly() {
        Game game = new Game(1);

        Move move = strategy.determineMove(game);

        assertNotNull(move);
        assertTrue(move.isFirstMove());
        assertTrue(game.getAvailablePieces().containsKey(move.getNextPiece()));
    }

    /**
     * After the first move, the strategy must return a placement move.
     */
    @Test
    void normalMoveReturnsLocationAndPiece() {
        Game game = new Game(1);
        game.doMove(new Move(3)); // first move done

        Move move = strategy.determineMove(game);

        assertNotNull(move);
        assertFalse(move.isFirstMove());
        assertTrue(move.getLocation() >= 0 && move.getLocation() < 16);
        assertTrue(game.getBoard().isEmptyField(move.getLocation()));
    }

    /**
     * The strategy must never return an invalid move.
     */
    @Test
    void returnedMoveIsAlwaysValid() {
        Game game = new Game(1);

        for (int i = 0; i < 10; i++) {
            Move move = strategy.determineMove(game);
            assertTrue(game.isValidMove(move));
            game.doMove(move);
        }
    }
}
