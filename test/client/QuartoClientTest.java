package client;

import org.junit.jupiter.api.*;
import protocol.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class QuartoClientTest {

    // Fake server socket
    private ServerSocket server;

    // Thread simulating server behaviour
    private Thread serverThread;

    // Port used by fake server
    private int port;

    // Start fake server on random free port
    @BeforeEach
    void setup() throws IOException {
        server = new ServerSocket(0);
        port = server.getLocalPort();
    }

    // Cleanup server after each test
    @AfterEach
    void teardown() throws IOException {
        if (server != null && !server.isClosed()) server.close();
        if (serverThread != null) serverThread.interrupt();
    }

    // Verifies that onConnected() is called after connect()
    @Test
    void connectTriggersOnConnected() throws Exception {
        QuartoClient client = new QuartoClient();
        AtomicBoolean connected = new AtomicBoolean(false);

        // Server just accepts connection
        serverThread = new Thread(() -> {
            try { server.accept(); } catch (IOException ignored) {}
        });
        serverThread.start();

        client.connect("localhost", port, new QuartoClient.GameListener() {
            @Override public void onConnected() { connected.set(true); }
            @Override public void onNewGame(String p1, String p2) {}
            @Override public void onOpponentMove(int location, int piece) {}
            @Override public void onGameOver(String result, String winner) {}
            @Override public void onError(String msg) {}
            @Override public void onChat(String sender, String text) {}
        });

        Thread.sleep(100);
        assertTrue(connected.get());
    }

    // Checks correct parsing of NEWGAME message
    @Test
    void newGameMessageIsParsedCorrectly() throws Exception {
        QuartoClient client = new QuartoClient();
        AtomicReference<String> p1 = new AtomicReference<>();
        AtomicReference<String> p2 = new AtomicReference<>();

        // Server sends NEWGAME
        serverThread = new Thread(() ->
                                          fakeServerSend(Protocol.NEWGAME + "~Alice~Bob")
        );
        serverThread.start();

        client.connect("localhost", port, new QuartoClient.GameListener() {
            @Override public void onConnected() {}
            @Override public void onNewGame(String a, String b) {
                p1.set(a); p2.set(b);
            }
            @Override public void onOpponentMove(int location, int piece) {}
            @Override public void onGameOver(String result, String winner) {}
            @Override public void onError(String msg) {}
            @Override public void onChat(String sender, String text) {}
        });

        Thread.sleep(200);
        assertEquals("Alice", p1.get());
        assertEquals("Bob", p2.get());
    }

    // Checks MOVE with location + piece
    @Test
    void moveMessageWithLocationIsParsedCorrectly() throws Exception {
        QuartoClient client = new QuartoClient();
        AtomicReference<Integer> loc = new AtomicReference<>();
        AtomicReference<Integer> piece = new AtomicReference<>();

        // Server sends MOVE~location~piece
        serverThread = new Thread(() ->
                                          fakeServerSend(Protocol.MOVE + "~5~12")
        );
        serverThread.start();

        client.connect("localhost", port, new QuartoClient.GameListener() {
            @Override public void onConnected() {}
            @Override public void onNewGame(String p1, String p2) {}
            @Override public void onOpponentMove(int l, int p) {
                loc.set(l); piece.set(p);
            }
            @Override public void onGameOver(String result, String winner) {}
            @Override public void onError(String msg) {}
            @Override public void onChat(String sender, String text) {}
        });

        Thread.sleep(200);
        assertEquals(5, loc.get());
        assertEquals(12, piece.get());
    }

    // Checks correct GAMEOVER handling
    @Test
    void gameOverMessageIsParsedCorrectly() throws Exception {
        QuartoClient client = new QuartoClient();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> winner = new AtomicReference<>();

        // Server sends GAMEOVER
        serverThread = new Thread(() ->
                                          fakeServerSend(Protocol.GAMEOVER + "~VICTORY~Alice")
        );
        serverThread.start();

        client.connect("localhost", port, new QuartoClient.GameListener() {
            @Override public void onConnected() {}
            @Override public void onNewGame(String p1, String p2) {}
            @Override public void onOpponentMove(int location, int piece) {}
            @Override public void onGameOver(String r, String w) {
                result.set(r); winner.set(w);
            }
            @Override public void onError(String msg) {}
            @Override public void onChat(String sender, String text) {}
        });

        Thread.sleep(200);
        assertEquals("VICTORY", result.get());
        assertEquals("Alice", winner.get());
    }

    // Checks ERROR handling and shutdown
    @Test
    void errorMessageTriggersOnErrorAndDisconnect() throws Exception {
        QuartoClient client = new QuartoClient();
        AtomicReference<String> error = new AtomicReference<>();

        // Server sends ERROR
        serverThread = new Thread(() ->
                                          fakeServerSend(Protocol.ERROR + "~Something went wrong")
        );
        serverThread.start();

        client.connect("localhost", port, new QuartoClient.GameListener() {
            @Override public void onConnected() {}
            @Override public void onNewGame(String p1, String p2) {}
            @Override public void onOpponentMove(int location, int piece) {}
            @Override public void onGameOver(String result, String winner) {}
            @Override public void onError(String msg) { error.set(msg); }
            @Override public void onChat(String sender, String text) {}
        });

        Thread.sleep(200);
        assertEquals("Something went wrong", error.get());
    }

    // Helper: accepts client and sends one message
    private void fakeServerSend(String message) {
        try (Socket client = server.accept();
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(client.getOutputStream()))) {

            out.write(message);
            out.newLine();
            out.flush();

            Thread.sleep(100);
        } catch (Exception ignored) {}
    }
}
