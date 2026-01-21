package client;

import gameLogic.Game;
import gameLogic.Move;


public class AIClient extends AbstractClient {

    /**
     * The strategy used by this AI client to determine moves.
     */
    private final BotStrategy strategy;

    /**
     * The thinking time in milliseconds before making a move.
     */
    private final long thinkingTime;

    /**
     * Constructs an AI client without any thinking time.
     *
     * @param name the name of the AI client
     * @param strategy the strategy used to determine moves
     */
    /*@
      requires name != null;
      requires strategy != null;
      ensures getName() == name;
    @*/
    public AIClient(String name, BotStrategy strategy) {
        super(name);
        this.strategy = strategy;
        this.thinkingTime = 0;
    }

    /**
     * Constructs an AI client with a specified thinking time.
     * @param name the name of the AI client
     * @param strategy the strategy used to determine moves
     * @param thinkingTime the thinking time in milliseconds (non-negative)
     */

    /*@
      requires name != null;
      requires strategy != null;
      requires thinkingTime >= 0;
      ensures getName() == name;
    @*/
    public AIClient(String name, BotStrategy strategy, long thinkingTime) {
        super(name);
        this.strategy = strategy;
        this.thinkingTime = thinkingTime;
    }

    /**
     * Determines the next move for the AI client.
     * If a thinking time is configured, the client will wait for the specified
     * duration before delegating the move decision to its strategy.
     * @param game the current game state
     * @return a move chosen by the configured strategy
     */
    /*@
      requires game != null;
      ensures \result != null;
    @*/
    @Override
    public Move determineMove(Game game) {
        if (thinkingTime > 0) {
            try {
                Thread.sleep(thinkingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return strategy.determineMove(game);
    }
}
