package client;

import gameLogic.Game;
import gameLogic.Move;
import gameLogic.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple bot strategy that selects a random valid move.
 * Fully protocol-safe: supports last move, win (16), and draw (17).
 */
public class NaiveStrategy implements BotStrategy {

    @Override
    public String getName() {
        return "Naive";
    }

    /**
     * Determines the next move by randomly selecting
     * one of the valid moves.
     *
     * @param game current game state
     * @return a valid move encoded according to protocol
     */
    /*@
      requires game != null;
      requires !game.isGameOver();
      ensures \result != null;
    @*/
    @Override
    public Move determineMove(Game game) {

        List<Move> moves = getValidMoves(game);

        // No legal moves → game must be finished
        if (moves.size() == 1) {
            return game.getWinner() != 0
                    ? new Move(16, moves.get(0).getLocation())
                    : new Move(17, moves.get(0).getLocation());
        }

        // Check for immediate win or draw
        for (Move move : moves) {
            Game copy = game.deepCopy();
            copy.doMove(move);

            if (copy.getWinner() == game.getCurrentPlayer()) {
                return move.isFirstMove()
                        ? new Move(16)
                        : new Move(16, move.getLocation());
            }

            if (copy.isDraw()) {
                return move.isFirstMove()
                        ? new Move(17)
                        : new Move(17, move.getLocation());
            }
        }

        // Otherwise: random move
        int idx = (int) (Math.random() * moves.size());
        return moves.get(idx);
    }

    /**
     * Generates all valid moves for the current game state.
     * Correctly handles:
     * - first move
     * - normal turns
     * - last move (no pieces left)
     *
     * @param game current game state
     * @return list of valid moves (possibly empty)
     */
    /*@
      requires game != null;
      ensures \result != null;
    @*/
    private List<Move> getValidMoves(Game game) {

        List<Move> result = new ArrayList<>();
        Map<Integer, Piece> pieces = game.getAvailablePieces();

        // First move: only choose a piece
        if (game.getCurrentPieceID() == -1) {
            for (int pieceId : pieces.keySet()) {
                result.add(new Move(pieceId));
            }
            return result;
        }

        // Normal / last move
        for (int field = 0; field < 16; field++) {
            if (!game.getBoard().isEmptyField(field)) {
                continue;
            }

            // Last move: no piece to give
            if (pieces.isEmpty()) {
                result.add(new Move(0, field));
            } else {
                for (int pieceId : pieces.keySet()) {
                    result.add(new Move(pieceId, field));
                }
            }
        }

        return result;
    }
}
