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
        try {
            // Player 1 starts
            player1.setPlayerID(1);
            player2.setPlayerID(2);

            player1.setOpponent(player2);
            player2.setOpponent(player1);

            String msg = Protocol.NEWGAME + Protocol.SEPARATOR + player1.getUsername() + Protocol.SEPARATOR + player2.getUsername();
            player1.sendPacket(msg);
            player2.sendPacket(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Обрабатывает ход. ТЕПЕРЬ САМ ПРОВЕРЯЕТ ПОБЕДУ.
     * Больше не ждем код 16.
     */
    public synchronized void handleMove(ClientHandler player, int nextPiece, int location) {
        if (gameEnded) return;

        if (location != -1 && (location < 0 || location > 15)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Illegal location");
            return;
        }

        ClientHandler opponent = player.getOpponent();
        String msg = "";

        // Обрабатываем ход (обычный, победный 16 или ничья 17 теперь не важны, логика едина)
        Move move = (location == -1) ? new Move(nextPiece) : new Move(nextPiece, location);

        if (!gameLogic.isValidMove(move)) {
            player.sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid move");
            return;
        }

        gameLogic.doMove(move);

        // --- АВТОМАТИЧЕСКАЯ ПРОВЕРКА ПОСЛЕ КАЖДОГО ХОДА ---
        int winnerId = gameLogic.getWinner();
        if (winnerId != 0) {
            // Если есть победитель, определяем его имя
            String winnerName = (winnerId == 1) ? player1.getUsername() : player2.getUsername();
            msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY + Protocol.SEPARATOR + winnerName;
            gameEnded = true;
        } else if (gameLogic.isDraw()) {
            msg = Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.DRAW;
            gameEnded = true;
        } else {
            // Игра продолжается, пересылаем ход
            if (location == -1) {
                msg = Protocol.MOVE + Protocol.SEPARATOR + nextPiece;
            } else {
                msg = Protocol.MOVE + Protocol.SEPARATOR + location + Protocol.SEPARATOR + nextPiece;
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
        ClientHandler opponent = player.getOpponent();

        if (opponent != null) {
            opponent.sendPacket(Protocol.GAMEOVER + Protocol.SEPARATOR + Protocol.VICTORY + Protocol.SEPARATOR + opponent.getUsername());
            player.getServer().endSession(player1, player2);
        }
        gameEnded = true;
    }
}