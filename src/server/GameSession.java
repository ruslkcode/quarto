package server;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

/**
 * GameSession represents a single active game between two players.
 */
public class GameSession {

    /** First player in the session */
    private final ClientHandler player1;

    /** Second player in the session */
    private final ClientHandler player2;

    /** Game logic instance handling rules and board state */
    private final Game gameLogic;

    /** Unique identifier for this game session */
    private final int gameId;

    /** Indicates whether the game has already ended */
    private boolean gameEnded = false;

    /*@
      @ private invariant player1 != null;
      @ private invariant player2 != null;
      @ private invariant gameLogic != null;
      @ private invariant gameId > 0;
      @*/

    /**
     * Creates a new game session for two players.
     *
     * @param player1 first player
     * @param player2 second player
     * @param gameId unique game identifier
     */
    public GameSession(ClientHandler player1, ClientHandler player2, int gameId) {
        this.player1 = player1;
        this.player2 = player2;
        this.gameId = gameId;
        this.gameLogic = new Game(1);
    }

    /**
     * Starts the game session.
     * <p>
     * Assigns player IDs, sets opponents, and sends a NEWGAME
     * protocol message to both players.
     */
    /*@
      @ assignable player1.*, player2.*;
      @*/
    public synchronized void startGame() {

        // Assign internal player IDs
        player1.setPlayerID(1);
        player2.setPlayerID(2);

        // Set opponents for both players
        player1.setOpponent(player2);
        player2.setOpponent(player1);

        // Prepare NEWGAME protocol message
        String startPacket = Protocol.NEWGAME + Protocol.SEPARATOR
                + player1.getUsername() + Protocol.SEPARATOR
                + player2.getUsername();

        // Notify both players that the game has started
        player1.sendPacket(startPacket);
        player2.sendPacket(startPacket);

        System.out.println("Session " + gameId + " started: "
                                   + player1.getUsername() + " vs " + player2.getUsername());
    }

    /**
     * Handles a move sent by a player.
     * <p>
     * This method validates the move, updates the game state,
     * checks for win or draw conditions, and sends appropriate
     * protocol messages to both players.
     *
     * @param player the player making the move
     * @param nextPiece the next piece or special command (16 = win, 17 = draw)
     * @param location board location, or -1 if not applicable
     */
    /*@
      @ requires player != null;
      @*/
    public synchronized void handleMove(ClientHandler player, int nextPiece, int location) {

        // Ignore moves if the game has already ended
        if (gameEnded) return;

        // Validate board location if provided
        if (location != -1 && (location < 0 || location > 15)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Illegal location");
            return;
        }

        ClientHandler opponent = player.getOpponent();
        String msg = "";

        // === WIN CLAIM ===
        if (nextPiece == 16) {

            // Winning move must include a placement
            if (location == -1) {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Winning move requires a location");
                return;
            }

            // Cannot claim victory if no piece was previously selected
            if (gameLogic.getCurrentPieceID() == -1) {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Cannot claim victory without a placed piece");
                return;
            }

            // Perform the final move
            Move move = new Move(gameLogic.getCurrentPieceID(), location);
            gameLogic.doMove(move);

            // Check if the win claim is valid
            if (gameLogic.getWinner() == player.getPlayerID()) {

                // Correct win claim
                msg = Protocol.GAMEOVER + Protocol.SEPARATOR
                        + Protocol.VICTORY + Protocol.SEPARATOR
                        + player.getUsername();

                player.getServer().updateMmr(player.getUsername(), 25);
                player.getServer().updateMmr(opponent.getUsername(), -25);
            } else {

                // False win claim -> opponent wins
                msg = Protocol.GAMEOVER + Protocol.SEPARATOR
                        + Protocol.VICTORY + Protocol.SEPARATOR
                        + opponent.getUsername();

                player.getServer().updateMmr(player.getUsername(), -25);
                player.getServer().updateMmr(opponent.getUsername(), 25);
            }

            gameEnded = true;
        }

        // === DRAW CLAIM ===
        else if (nextPiece == 17) {

            if (gameLogic.isDraw()) {
                msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.DRAW;
            } else {

                // False draw claim -> opponent wins
                msg = Protocol.GAMEOVER + Protocol.SEPARATOR
                        + Protocol.VICTORY + Protocol.SEPARATOR
                        + opponent.getUsername();

                player.getServer().updateMmr(player.getUsername(), -25);
                player.getServer().updateMmr(opponent.getUsername(), 25);
            }

            gameEnded = true;
        }

        // === NORMAL MOVE ===
        else {

            Move move = (location == -1)
                    ? new Move(nextPiece)
                    : new Move(nextPiece, location);

            // Validate move according to game rules
            if (!gameLogic.isValidMove(move)) {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid move");
                return;
            }

            gameLogic.doMove(move);

            // Automatic draw detection
            if (gameLogic.isDraw()) {
                msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.DRAW;
                gameEnded = true;
            } else {

                // Send MOVE message depending on whether a placement occurred
                if (location == -1) {
                    msg = Protocol.MOVE + Protocol.SEPARATOR + nextPiece;
                } else {
                    msg = Protocol.MOVE + Protocol.SEPARATOR
                            + location + Protocol.SEPARATOR + nextPiece;
                }
            }
        }

        // Send resulting message to both players
        if (!msg.isEmpty()) {
            player.sendPacket(msg);
            if (opponent != null) opponent.sendPacket(msg);

            // Notify server that the session has ended
            if (gameEnded) {
                player.getServer().endSession(player1, player2);
            }
        }
    }

    /**
     * Handles a player disconnection during an active game.
     * The remaining player automatically wins the game.
     * @param player the disconnected player
     */
    /*@
      @ requires player != null;
      @*/
    public void disconnect(ClientHandler player) {

        // Ignore disconnects if the game has already ended
        if (gameEnded) return;

        ClientHandler opponent;

        // Determine the remaining player
        if (player == player1) {
            opponent = player2;
        } else {
            opponent = player1;
        }

        // Notify the opponent of victory
        if (opponent != null) {
            opponent.sendPacket(
                    Protocol.GAMEOVER + Protocol.SEPARATOR
                            + Protocol.VICTORY + Protocol.SEPARATOR
                            + opponent.getUsername()
            );

            opponent.setOpponent(null);
            player.getServer().endSession(player1, player2);
        }

        gameEnded = true;
    }
}
