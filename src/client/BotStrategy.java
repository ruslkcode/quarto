package client;

import gameLogic.Game;
import gameLogic.Move;
import java.util.List;

/**
 * An interface for different implementations of strategies.
 */
public interface BotStrategy {
    /**
     * Returns the name of the strategy.
     */

    /*@
      ensures \result != null;
    @*/
    String getName();

    /**
     * Returns a next legal move.
     * @param game the current game.
     * @return the next legal move, given the current state of the game.
     */

    /*@
      requires game != null;
      ensures \result != null;
    @*/
    Move determineMove(Game game);
}
