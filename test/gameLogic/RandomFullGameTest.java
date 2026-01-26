package gameLogic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that simulates a complete Quarto game
 * using random but legal moves.
 */
public class RandomFullGameTest {

    /**
     * Simulates a full game from start to finish using random valid moves.
     * Verifies that the game state remains valid after each move and that
     * the game eventually reaches a terminal state.
     */
    @Test
    void randomFullGamePlaysToCompletion() {
        Game game = new Game(1);
        Random random = new Random();

        int movesPlayed = 0;

        while (!game.isGameOver()) {

            assertFalse(game.isGameOver(), "Game ended unexpectedly");

            int currentPiece = game.getCurrentPieceID();

            // First move: only choose a piece
            if (currentPiece == -1) {
                Map<Integer, Piece> available = game.getAvailablePieces();
                assertFalse(available.isEmpty());

                int nextPiece = pickRandomKey(available, random);
                Move move = new Move(nextPiece);

                assertTrue(game.isValidMove(move));
                game.doMove(move);
            }
            // Regular move: place piece and give next piece
            else {
                Board board = game.getBoard();
                List<Integer> freeFields = getFreeFields(board);

                assertFalse(freeFields.isEmpty());

                int location = freeFields.get(random.nextInt(freeFields.size()));
                int nextPiece = pickRandomKey(game.getAvailablePieces(), random);

                Move move = new Move(nextPiece, location);

                assertTrue(game.isValidMove(move));
                game.doMove(move);
            }

            movesPlayed++;

            assertTrue(movesPlayed <= 17);
            assertGameStateValid(game);
        }

        assertTrue(game.isGameOver());

        // After game over, no move should be valid
        Move illegalMove = new Move(0);
        assertFalse(game.isValidMove(illegalMove));
    }

    /**
     * Selects a random key from a map of available pieces.
     *
     * @param map    map containing available pieces
     * @param random random number generator
     * @return randomly selected piece ID
     */
    private int pickRandomKey(Map<Integer, Piece> map, Random random) {
        List<Integer> keys = new ArrayList<>(map.keySet());
        return keys.get(random.nextInt(keys.size()));
    }

    /**
     * Returns a list of all empty board positions.
     *
     * @param board the current game board
     * @return list of free field indices
     */
    private List<Integer> getFreeFields(Board board) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            if (board.isEmptyField(i)) {
                free.add(i);
            }
        }
        return free;
    }

    /**
     * Verifies basic invariants of the game state after a move.
     *
     * @param game the current game instance
     */
    private void assertGameStateValid(Game game) {
        Board board = game.getBoard();

        int occupied = 0;
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            if (!board.isEmptyField(i)) {
                occupied++;
            }
        }

        assertTrue(occupied <= 16, "Board contains too many pieces");

        int currentPiece = game.getCurrentPieceID();
        if (currentPiece != -1) {
            assertTrue(game.getAllPieces().containsKey(currentPiece),
                       "Current piece must exist");
            assertFalse(game.getAvailablePieces().containsKey(currentPiece),
                        "Current piece must not be available");
        }
    }
}

