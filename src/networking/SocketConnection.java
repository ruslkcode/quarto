package networking;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Wrapper for a TCP socket connection.
 * Handles sending and receiving single-line messages over the network.
 * This class is not thread-safe.
 */
public abstract class SocketConnection {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private boolean started = false;

    /*@
      private invariant socket != null;
      private invariant in != null;
      private invariant out != null;
    @*/

    /**
     * Creates a new socket connection using an existing socket.
     *
     * @param socket the socket to wrap
     * @throws IOException if the input or output streams cannot be created
     */
    /*@
      requires socket != null;
      ensures this.socket == socket;
    @*/
    protected SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    /**
     * Creates a new TCP connection to the given host and port.
     * The receiving thread is not started automatically.
     *
     * @param host the server address
     * @param port the server port
     * @throws IOException if the connection cannot be established
     */
    /*@
      requires host != null;
      requires port >= 0 && port <= 65535;
    @*/
    protected SocketConnection(InetAddress host, int port) throws IOException {
        this(new Socket(host, port));
    }

    /**
     * Creates a new TCP connection to the given host and port.
     * The receiving thread is not started automatically.
     *
     * @param host the server hostname
     * @param port the server port
     * @throws IOException if the connection cannot be established
     */
    /*@
      requires host != null;
      requires port >= 0 && port <= 65535;
    @*/
    protected SocketConnection(String host, int port) throws IOException {
        this(new Socket(host, port));
    }

    /**
     * Starts the receiving thread.
     * This method may only be called once.
     *
     * @throws IllegalStateException if called more than once
     */
    /*@
      requires !started;
      ensures started == true;
    @*/
    public void start() {
        if (started) {
            throw new IllegalStateException("Cannot start a SocketConnection twice");
        }
        started = true;
        Thread thread = new Thread(this::receivePackets);
        thread.start();
    }

    /**
     * Continuously receives messages from the socket.
     * For each received line, {@link #handlePackets(String)} is invoked.
     * When the connection closes, {@link #handleDisconnect()} is called.
     */
    /*@
      requires started;
    @*/
    public void receivePackets() {
        handleStart();
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handlePackets(inputLine);
            }
        } catch (IOException e) {
            // connection closed or broken
        } finally {
            close();
            handleDisconnect();
        }
    }

    /**
     * Sends a single-line message to the remote endpoint.
     *
     * @param message the message to send (must not contain newlines)
     * @return true if the message was sent successfully, false otherwise
     */
    /*@
      requires message != null;
      ensures \result == true || \result == false;
    @*/
    public boolean sendPacket(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    /**
     * Closes the socket connection.
     * This will also stop the receiving thread.
     */
    /*@
      ensures true;
    @*/
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Called once when the receiving thread starts.
     * Subclasses may override this method.
     */
    protected void handleStart() {
        // default: do nothing
    }

    /**
     * Handles a single received message.
     * @param message the received message
     */
    /*@
      requires message != null;
    @*/
    protected abstract void handlePackets(String message);

    /**
     * Called when the connection is closed.
     */
    public abstract void handleDisconnect();
}
