package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GameServer.
 * Tests core server logic without relying on external network communication.
 */
public class GameServerTest {

    /** Instance of the server used in each test */
    private GameServer server;

    /**
     * Minimal test implementation of ClientHandler.
     * <p>
     * A real local socket is used because ClientHandler initializes
     * input and output streams in its constructor and does not
     * allow null sockets.
     */
    private static class TestClientHandler extends ClientHandler {

        /** Username associated with this test client */
        private final String username;

        /**
         * Creates a test client handler with a real local socket.
         *
         * @param username username of the test client
         * @throws IOException if socket creation fails
         */
        TestClientHandler(String username) throws IOException {
            super(createSocket(), null);
            this.username = username;
        }

        /**
         * Creates a pair of locally connected sockets.
         * <p>
         * A ServerSocket is opened on a random free port, and a client
         * socket connects to it. The accepted server-side socket is
         * returned and used by ClientHandler.
         *
         * @return server-side socket of a local connection
         * @throws IOException if socket creation fails
         */
        private static Socket createSocket() throws IOException {
            ServerSocket serverSocket = new ServerSocket(0); // OS selects free port
            Socket client = new Socket("localhost", serverSocket.getLocalPort());
            Socket serverSide = serverSocket.accept();
            serverSocket.close();

            return serverSide;
        }

        /**
         * Returns the username of this test client.
         *
         * @return username
         */
        @Override
        public String getUsername() {
            return username;
        }

        /**
         * Overrides packet sending to avoid actual network output during tests.
         *
         * @param message message to send (ignored)
         * @return
         */
        @Override
        public boolean sendPacket(String message) {
            // Intentionally left empty
            return false;
        }
    }

    /**
     * Creates a fresh GameServer instance before each test.
     * @throws IOException if server initialization fails
     */
    @BeforeEach
    void setUp() throws IOException {
        server = new GameServer(0); // use random available port
    }

    /**
     * Verifies that a newly created server has no logged-in users.
     */
    @Test
    void testInitialUserListIsEmpty() {
        assertEquals("", server.getUserList());
    }

    /**
     * Verifies that isLoggedIn returns false for an unknown username.
     */
    @Test
    void testIsLoggedInFalse() {
        assertFalse(server.isLoggedIn("unknown"));
    }

    /**
     * Verifies that adding a single player to the queue
     * does not cause any exceptions.
     */
    @Test
    void testAddToQueueDoesNotThrow() throws IOException {
        ClientHandler player = new TestClientHandler("player1");
        assertDoesNotThrow(() -> server.addToQueue(player));
    }

    /**
     * Verifies that when two players join the queue,
     * the server attempts to start a new game session.
     */
    @Test
    void testQueueStartsGameWithTwoPlayers() throws IOException {
        ClientHandler p1 = new TestClientHandler("player1");
        ClientHandler p2 = new TestClientHandler("player2");

        assertDoesNotThrow(() -> {
            server.addToQueue(p1);
            server.addToQueue(p2);
        });
    }

    /**
     * Verifies that disconnecting a player is handled safely
     * and does not cause server errors.
     */
    @Test
    void testHandleDisconnectDoesNotThrow() throws IOException {
        ClientHandler player = new TestClientHandler("player1");
        assertDoesNotThrow(() -> server.handleDisconnect(player));
    }

    /**
     * Verifies that ending a session with two players
     * does not cause any exceptions.
     */
    @Test
    void testEndSessionDoesNotThrow() throws IOException {
        ClientHandler p1 = new TestClientHandler("player1");
        ClientHandler p2 = new TestClientHandler("player2");

        assertDoesNotThrow(() -> server.endSession(p1, p2));
    }
}
