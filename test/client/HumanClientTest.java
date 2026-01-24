package client;

import gameLogic.Game;
import gameLogic.Move;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HumanClient}.
 *
 * These tests simulate user input via a Scanner backed
 * by a ByteArrayInputStream.
 */
public class HumanClientTest {

    /**
     * Creates a HumanClient with predefined console input.
     */
    private HumanClient createClientWithInput(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(in);
        return new HumanClient("TestPlayer", scanner);
    }

    /**
     * Tests the first move: choosing only a piece.
     */
    @Test
    void firstMoveReturnsPieceOnly() {
        Game game = new Game(1);
        HumanClient client = createClientWithInput("5\n");

        Move move = client.determineMove(game);

        assertTrue(move.isFirstMove());
        assertEquals(5, move.getNextPiece());
    }

    /**
     * Tests that invalid input on first move is rejected
     * until a valid piece index is provided.
     */
    @Test
    void firstMoveRejectsInvalidInput() {
        Game game = new Game(1);

        // invalid text → out of range → valid
        HumanClient client = createClientWithInput(
                "abc\n" +
                        "20\n" +
                        "3\n"
        );

        Move move = client.determineMove(game);

        assertTrue(move.isFirstMove());
        assertEquals(3, move.getNextPiece());
    }

    /**
     * Tests a normal move: place a piece and give the next one.
     */
    @Test
    void normalMoveReturnsLocationAndPiece() {
        Game game = new Game(1);
        game.doMove(new Move(4)); // first move already done

        HumanClient client = createClientWithInput(
                "6\n" +   // location
                        "9\n"     // next piece
        );

        Move move = client.determineMove(game);

        assertFalse(move.isFirstMove());
        assertEquals(6, move.getLocation());
        assertEquals(9, move.getNextPiece());
    }

    /**
     * Tests claiming victory with "win" command.
     */
    @Test
    void winCommandCreatesWinningMove() {
        Game game = new Game(1);
        game.doMove(new Move(2)); // first move

        HumanClient client = createClientWithInput(
                "win\n" +
                        "7\n"
        );

        Move move = client.determineMove(game);

        assertEquals(16, move.getNextPiece());
        assertEquals(7, move.getLocation());
    }

    /**
     * Tests claiming a draw with "draw" command.
     */
    @Test
    void drawCommandCreatesDrawMove() {
        Game game = new Game(1);
        game.doMove(new Move(1)); // first move

        HumanClient client = createClientWithInput("draw\n");

        Move move = client.determineMove(game);

        assertEquals(17, move.getNextPiece());
        assertTrue(move.isFirstMove() || move.getLocation() == -1);
    }

    /**
     * Tests rejection of invalid location before accepting a valid one.
     */
    @Test
    void invalidLocationIsRejected() {
        Game game = new Game(1);
        game.doMove(new Move(3));

        HumanClient client = createClientWithInput(
                "99\n" +   // invalid location
                        "-1\n" +   // invalid location
                        "4\n" +    // valid location
                        "6\n"      // next piece
        );

        Move move = client.determineMove(game);

        assertEquals(4, move.getLocation());
        assertEquals(6, move.getNextPiece());
    }
}
