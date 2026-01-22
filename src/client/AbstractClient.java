package client;

import gameLogic.Game;
import gameLogic.Move;

public abstract class AbstractClient {
    private final String name;

    /**
     * Creates a new Player object.
     */
    /*@ requires name != null;
        ensures getName() == name;
    @*/
    public AbstractClient(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the player.
     * @return the name of the player
     */
    public String getName() {
        return name;
    }

    /**
     * Determines the next move, if the game still has available moves.
     * @param game the current game
     * @return the player's choice
     */
    /*@ requires !game.isGameOver();
        ensures game.isValidMove(\result);
     @*/
    public abstract Move determineMove(Game game);
}
