package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameSession.
 * Focuses on basic session lifecycle and stability.
 */
public class GameSessionTest {

    private GameSession session;
    private ClientHandler player1;
    private ClientHandler player2;

    /**
     * Minimal ClientHandler implementation for testing.
     */
    private static class TestClientHandler extends ClientHandler {

        private final String username;
        private ClientHandler opponent;
        private int playerId;

        TestClientHandler(String username, GameServer server) throws IOException {
            super(createSocket(), server);
            this.username = username;
        }

        private static Socket createSocket() throws IOException {
            ServerSocket serverSocket = new ServerSocket(0);
            Socket client = new Socket("localhost", serverSocket.getLocalPort());
            Socket serverSide = serverSocket.accept();
            serverSocket.close();
            return serverSide;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean sendPacket(String message) {
            // ignore network output in tests
            return false;
        }

        @Override
        public void setOpponent(ClientHandler opponent) {
            this.opponent = opponent;
        }

        @Override
        public ClientHandler getOpponent() {
            return opponent;
        }

        @Override
        public void setPlayerID(int id) {
            this.playerId = id;
        }

        @Override
        public int getPlayerID() {
            return playerId;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        GameServer server = new GameServer(0);

        player1 = new TestClientHandler("player1", server);
        player2 = new TestClientHandler("player2", server);

        session = new GameSession(player1, player2, 1);
    }

    @Test
    void testStartGameDoesNotThrow() {
        // Starting a session should not cause errors
        assertDoesNotThrow(() -> session.startGame());
    }

    @Test
    void testPlayersAreAssignedOpponentsOnStart() {
        // Players should be linked as opponents after game start
        session.startGame();
        assertEquals(player2, player1.getOpponent());
        assertEquals(player1, player2.getOpponent());
    }

    @Test
    void testHandleMoveAfterGameEndIsIgnored() {
        // Moves after game end should be safely ignored
        session.startGame();
        session.disconnect(player1);

        assertDoesNotThrow(() ->
                                   session.handleMove(player2, 0, -1)
        );
    }

    @Test
    void testIllegalLocationProducesNoCrash() {
        // Illegal board positions should not crash the session
        session.startGame();

        assertDoesNotThrow(() -> session.handleMove(player1, 0, 99)
        );
    }

    @Test
    void testDisconnectEndsGameSafely() {
        // Disconnecting a player should end the session cleanly
        session.startGame();
        assertDoesNotThrow(() -> session.disconnect(player1));
    }

    @Test
    void testStartGameSetsPlayerIds() {
        // startGame should assign player IDs
        session.startGame();
        assertEquals(1, player1.getPlayerID());
        assertEquals(2, player2.getPlayerID());
    }

    @Test
    void testHandleMoveInvalidLocation() {
        // Invalid move location should not crash the session
        session.startGame();
        assertDoesNotThrow(() ->
                                   session.handleMove(player1, 0, 99)
        );
    }
}

