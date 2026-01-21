package client;

import client.BotStrategy;
import gameLogic.Game;
import gameLogic.Move;
import gameLogic.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NaiveStrategy implements BotStrategy{
    private ArrayList<Move> moves;

    @Override
    public String getName() {
        return "Naive";
    }


    @Override
    public Move determineMove(Game game) {
        ArrayList<Move> moves = new ArrayList<>(getValidMoves(game));
        if (moves.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * moves.size());
        return moves.get(randomIndex);
    }

    /**
     * Getter for all the valid moves in the game.
     * @param game the current game.
     * @return the list of valid moves.
     */

    List<Move> getValidMoves(Game game) {
        List<Move> result = new ArrayList<>();
        Map<Integer, Piece> pieces = game.getAvailablePieces();
        if (game.getCurrentPieceID() == -1) { //Filling the result with only pieces if it is the first move.
            for (int key : pieces.keySet()) {
                result.add(new Move(key));
            }
            return result;
        }

        for (int i = 0; i < 16; i++) { // For all other moves puts a combination of field id and piece id.
            if (game.getBoard().isEmptyField(i)) {
                for (int key : pieces.keySet()) {
                    Move move = new Move(key, i);
                    result.add(move);
                }
            }
        }
        return result;
    }

}
