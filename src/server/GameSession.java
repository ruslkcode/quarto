package server;

import gameLogic.Game;
import gameLogic.Move;
import protocol.Protocol;

public class GameSession {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Game gameLogic;
    private final int gameId;

    public GameSession(ClientHandler player1, ClientHandler player2, int gameId) {
        this.player1 = player1;
        this.player2 = player2;
        this.gameId = gameId;
        this.gameLogic = new Game(0); // Инициализация логики игры
    }

    public void startGame() {
        player1.setPlayerID(0);
        player2.setPlayerID(1);
        player1.setOpponent(player2);
        player2.setOpponent(player1);

        String startPacket = Protocol.NEWGAME + Protocol.SEPARATOR + player1.getUsername() + Protocol.SEPARATOR + player2.getUsername();

        player1.sendPacket(startPacket);
        player2.sendPacket(startPacket);

        System.out.println("Session " + gameId + " started: " + player1.getUsername() + " vs " + player2.getUsername());
    }

    public void handleMove(ClientHandler player, int nextPiece, int location) {
        Move move;
        if (location == -1) {
            move = new Move(nextPiece);
        } else {
            move = new Move(nextPiece, location);
        }
        if (location != -1 && (location < 0 || location > 15)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Illegal location: " + location);
            return;
        }

        boolean isPieceValid;

        if (gameLogic.isValidMove(move)) {
            System.out.println("[SESSION " + gameId + "] Move by " + player.getUsername() + ": Loc=" + location + ", Piece=" + nextPiece);
            gameLogic.doMove(move);

            String msg;
            ClientHandler opponent = player.getOpponent();
            if (location == -1) {
                isPieceValid = (nextPiece >= 0 && nextPiece <= 15);
                msg = Protocol.MOVE + Protocol.SEPARATOR + nextPiece;
            } else {
                isPieceValid = (nextPiece >= 0 && nextPiece <= 17);
                msg = Protocol.MOVE + Protocol.SEPARATOR + location + Protocol.SEPARATOR + nextPiece;
            }
            if (!isPieceValid) {
                player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Illegal piece ID: " + nextPiece);
                return;
            }
            if (opponent != null) {
                opponent.sendPacket(msg);
            }

            player.sendPacket(msg);
        } else {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move");
        }
    }

    public void disconnect(ClientHandler player) {
        ClientHandler opponent;

        if (player == player1) {
            opponent = player2;
        } else {
            opponent = player1;
        }
        if (opponent != null) {
            opponent.sendPacket(Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY);
            opponent.setOpponent(null);
        }
    }
}