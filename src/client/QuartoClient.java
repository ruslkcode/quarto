package client;

import java.io.*;
import java.net.Socket;

/**
 * Represents the network layer of the client application.
 * Handles connection, sending, and receiving messages.
 */
public class QuartoClient {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private MessageListener listener;

    private boolean running = false;
    /*@ private invariant socket != null ==> in != null && out != null; @*/

    /**
     * Interface for handling incoming messages.
     */
    public interface MessageListener {
        /*@
           requires msg != null;
        @*/
        void onMessage(String msg);
    }

    /**
     * Connects to the server with the specified host and port.
     *
     * @param host     the server address
     * @param port     the server port
     * @param listener the callback for received messages
     * @throws IOException if connection fails
     */
    /*@
       requires host != null && !host.isEmpty();
       requires port > 0 && port <= 65535;
       requires listener != null;
       ensures socket != null;
       ensures running == true;
    @*/
    public void connect(String host, int port, MessageListener listener) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.listener = listener;
        this.running = true;

        new Thread(this::listen).start();
    }

    /**
     * Listens for incoming messages from the server.
     * Runs in a separate thread.
     */
    private void listen() {
        try {
            String msg;
            while (running && (msg = in.readLine()) != null) {
                if (listener != null) {
                    listener.onMessage(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost.");
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param msg the message to send
     */
    /*@
       requires msg != null;
       requires out != null;
    @*/
    public void send(String msg) {
        try {
            out.write(msg);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the connection and resources.
     */
    /*@
       ensures running == false;
    @*/
    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }
}