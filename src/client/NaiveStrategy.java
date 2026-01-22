package client;

import gameLogic.Game;
import gameLogic.Move;
import gameLogic.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple bot strategy that picks a random valid move.
 * Useful for testing or easy difficulty.
 */

public class NaiveStrategy implements BotStrategy {

    @Override
    public String getName() {
        return "Naive (Random)";
    }

    /**
     * Determines the next move randomly.
     * @param game the current game state
     * @return a random valid move, or null if no moves are possible
     */
    /*@
       requires game != null;
       ensures \result != null;
    @*/
    @Override
    public Move determineMove(Game game) {
        List<Move> moves = getValidMoves(game);

        if (moves.isEmpty()) {
            return null; // Should ideally not happen if logic is correct
        }

        // Pick a random index
        int randomIndex = (int) (Math.random() * moves.size());
        return moves.get(randomIndex);
    }

    /**
     * Generates all valid moves for the current game state.
     * Handles the special case of the last move (when no pieces are left to give).
     *
     * @param game the current game state.
     * @return the list of valid moves.
     */
    /*@
       requires game != null;
       ensures \result != null;
       ensures \result.size() > 0;
    @*/
    List<Move> getValidMoves(Game game) {
        List<Move> result = new ArrayList<>();
        Map<Integer, Piece> pieces = game.getAvailablePieces();

        // 1. First move: Board is empty, we only pick a piece to give.
        if (game.getCurrentPieceID() == -1) {
            for (int key : pieces.keySet()) {
                result.add(new Move(key));
            }
            return result;
        }

        // 2. Normal & Last move
        for (int i = 0; i < 16; i++) {
            if (game.getBoard().isEmptyField(i)) {

                // CRITICAL FIX: If there are no pieces left to give (Turn 16),
                // we still need to place our current piece!
                if (pieces.isEmpty()) {
                    // Pass 0 (or -1) as nextPiece, it doesn't matter as game ends
                    result.add(new Move(0, i));
                } else {
                    // Normal case: Place piece at 'i', give piece 'key'
                    for (int key : pieces.keySet()) {
                        result.add(new Move(key, i));
                    }
                }
            }
        }
        return result;
    }
}