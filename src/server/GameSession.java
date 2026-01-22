package server;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

public class GameSession {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Game gameLogic;
    private final int gameId;

    private boolean gameEnded = false;

    public GameSession(ClientHandler player1, ClientHandler player2, int gameId) {
        this.player1 = player1;
        this.player2 = player2;
        this.gameId = gameId;
        this.gameLogic = new Game(1);
    }

    public synchronized void startGame() {
        player1.setPlayerID(1);
        player2.setPlayerID(2);

        player1.setOpponent(player2);
        player2.setOpponent(player1);

        String startPacket = Protocol.NEWGAME + Protocol.SEPARATOR + player1.getUsername() + Protocol.SEPARATOR + player2.getUsername();

        player1.sendPacket(startPacket);
        player2.sendPacket(startPacket);

        System.out.println("Session " + gameId + " started: " + player1.getUsername() + " vs " + player2.getUsername());
    }

    public synchronized void handleMove(ClientHandler player, int nextPiece, int location) {
        if (gameEnded) return;

        if (location != -1 && (location < 0 || location > 15)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Illegal location");
            return;
        }

        ClientHandler opponent = player.getOpponent();
        String msg = "";
        if (nextPiece == 16) {
            Move move = new Move(0, location);

            if (gameLogic.getBoard().isEmptyField(location)) {

                gameLogic.doMove(move);

                if (gameLogic.getWinner() != 0) {
                    msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY + Protocol.SEPARATOR + player.getUsername();
                    player.getServer().updateMmr(player.getUsername(), 25);
                    player.getOpponent().getServer().updateMmr(player.getUsername(), -25);
                    player.getServer().endSession(player1, player2);
                } else {
                    msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY + Protocol.SEPARATOR + opponent.getUsername();
                    player.getServer().updateMmr(player.getUsername(), 25);
                    player.getOpponent().getServer().updateMmr(player.getUsername(), -25);

                    player.getServer().endSession(player1, player2);

                }
                gameEnded = true;
            } else {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move (Location occupied)");
                return;
            }
        }
        else if (nextPiece == 17) {
            msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.DRAW;
            player.getServer().endSession(player1, player2);
            gameEnded = true;
        }
        else {
            Move move;
            if (location == -1) move = new Move(nextPiece);
            else move = new Move(nextPiece, location);

            if (gameLogic.isValidMove(move)) {
                gameLogic.doMove(move);

                if (gameLogic.isDraw()) {
                    msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.DRAW;
                    player.getServer().endSession(player1, player2);
                    gameEnded = true;
                } else {
                    if (location == -1) msg = Protocol.MOVE + Protocol.SEPARATOR + nextPiece;
                    else msg = Protocol.MOVE + Protocol.SEPARATOR + location + Protocol.SEPARATOR + nextPiece;
                }
            } else {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move");
                return;
            }
        }
        if (!msg.isEmpty()) {
            player.sendPacket(msg);
            if (opponent != null) opponent.sendPacket(msg);
            if (gameEnded) {
                player.getServer().endSession(player1, player2);
            }
        }
    }
    public void disconnect(ClientHandler player) {
        if (gameEnded) return;

        ClientHandler opponent;
        if (player == player1) {
            opponent = player2;
        } else {
            opponent = player1;
        }

        if (opponent != null) {
            opponent.sendPacket(Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY + Protocol.SEPARATOR + opponent.getUsername());
            opponent.setOpponent(null);
            player.getServer().endSession(player1, player2);

        }
        gameEnded = true;
    }
}