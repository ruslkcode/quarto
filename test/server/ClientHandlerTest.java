package server;

import networking.SocketConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ClientHandler} class.
 *
 * These tests verify that incoming protocol commands are correctly
 * interpreted and forwarded to the {@link GameServer}.
 *
 * Real sockets are not used. Instead, piped streams simulate
 * client–server communication.
 */
class ClientHandlerTest {

    private FakeGameServer server;
    private ClientHandler handler;

    private BufferedWriter clientOut;
    private BufferedReader clientIn;

    /**
     * Sets up a ClientHandler instance with a fake socket and
     * a fake game server before each test.
     */
    @BeforeEach
    void setUp() throws Exception {
        server = new FakeGameServer();

        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);

        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);

        Socket fakeSocket = new FakeSocket(serverInput, serverOutput);

        handler = new ClientHandler(fakeSocket, server);
        handler.start();

        clientOut = new BufferedWriter(new OutputStreamWriter(clientOutput));
        clientIn = new BufferedReader(new InputStreamReader(clientInput));
    }

    /**
     * Closes the handler after each test to release resources.
     */
    @AfterEach
    void tearDown() {
        handler.close();
    }

    /**
     * Tests that a valid LOGIN command results in a successful login response.
     */
    @Test
    void loginSucceedsWithValidUsername() throws Exception {
        clientOut.write(Protocol.LOGIN + "~Alice\n");
        clientOut.flush();

        String response = clientIn.readLine();
        assertEquals(Protocol.LOGIN + "~SUCCESS", response);
    }

    /**
     * Tests that a QUEUE command causes the client to be added to the queue
     * on the game server.
     */
    @Test
    void queueCommandAddsPlayerToQueue() throws Exception {
        clientOut.write(Protocol.LOGIN + "~Bob\n");
        clientOut.flush();
        clientIn.readLine(); // consume login response

        clientOut.write(Protocol.QUEUE + "\n");
        clientOut.flush();

        waitUntil(() -> server.queueCalled);

        assertTrue(server.queueCalled);
    }

    /**
     * Tests that a MOVE command is correctly forwarded to the game server
     * with the expected location and piece identifiers.
     */
    @Test
    void moveCommandIsForwardedToServer() throws Exception {
        clientOut.write(Protocol.LOGIN + "~Carl\n");
        clientOut.flush();
        clientIn.readLine();

        clientOut.write(Protocol.MOVE + "~4~9\n");
        clientOut.flush();

        waitUntil(() -> server.moveCalled);

        assertTrue(server.moveCalled);
        assertEquals(4, server.lastLocation);
        assertEquals(9, server.lastPiece);
    }

    /**
     * Utility method that blocks until a given condition becomes true
     * or a timeout is reached.
     *
     * @param condition the condition to wait for
     */
    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long timeout = System.currentTimeMillis() + 500;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < timeout) {
            Thread.sleep(5);
        }
    }

    /**
     * Minimal fake implementation of {@link GameServer} used for testing.
     * It records whether certain methods are invoked.
     */
    private static class FakeGameServer extends GameServer {

        boolean queueCalled = false;
        boolean moveCalled = false;
        boolean disconnectCalled = false;

        int lastPiece = -1;
        int lastLocation = -1;

        FakeGameServer() throws IOException {
            super(0);
        }

        @Override
        public synchronized void addToQueue(ClientHandler player) {
            queueCalled = true;
        }

        @Override
        public synchronized void handleMove(ClientHandler player, int piece, int location) {
            moveCalled = true;
            lastPiece = piece;
            lastLocation = location;
        }

        @Override
        public synchronized void handleDisconnect(ClientHandler player) {
            disconnectCalled = true;
        }
    }

    /**
     * Fake socket implementation that provides predefined input
     * and output streams for testing purposes.
     */
    private static class FakeSocket extends Socket {

        private final InputStream in;
        private final OutputStream out;

        FakeSocket(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }
    }
}
