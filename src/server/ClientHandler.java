package server;

import networking.SocketConnection;
import protocol.Protocol;
import java.io.IOException;
import java.net.Socket;
import org.apache.commons.lang3.StringUtils;

public class ClientHandler extends SocketConnection {

    private String username;
    private GameServer server;
    private ClientHandler opponent;
    private int playerID;
    private long queueJoinTime = 0;


    protected ClientHandler(Socket socket, GameServer server) throws IOException {
        super(socket);
        this.server = server;
    }
    public void setQueueJoinTime(long time) {
        this.queueJoinTime = time;
    }

    public long getQueueJoinTime() {
        return this.queueJoinTime;
    }

    public String getUsername() {
        return username;
    }

    public void setOpponent(ClientHandler opponent) {
        this.opponent = opponent;
    }
    public ClientHandler getOpponent() {
        return opponent;
    }
    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public int getPlayerID(){
        return playerID;
    }

    @Override
    protected void handlePackets(String message) throws NumberFormatException {
        if (StringUtils.isAllBlank(message)){
            return;
        }
        String[] parts = StringUtils.split(message, Protocol.SEPARATOR);
        String command = parts[0];
        try {
            switch (command){
                case Protocol.HELLO:
                    sendPacket(Protocol.HELLO + Protocol.SEPARATOR + "Server is ready");
                    break;

                case Protocol.LOGIN:
                    if (parts.length > 1){
                        String name = StringUtils.stripToNull(parts[1]);
                        if (name != null){
                            if (server.isLoggedIn(name)){
                                sendPacket(Protocol.ERROR + Protocol.SEPARATOR + name + " is " + Protocol.ALREADYLOGGEDIN);
                            }
                            else {
                                this.username = name;
                                sendPacket(Protocol.LOGIN + Protocol.SEPARATOR + "SUCCESS");
                                System.out.println(username + " is logged in");
                            }
                        } else {
                            sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "ERROR: EMPTY USERNAME");
                        }
                    }
                    break;

                case Protocol.QUEUE:
                    if (this.username == null) {
                        sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "YOU HAVE TO LOGIN");
                        return;
                    }
                    server.addToQueue(this);
                    break;

                case Protocol.MOVE:
                    if (parts.length == 2) {
                        int piece = Integer.parseInt(parts[1]);
                        server.handleMove(this, piece, -1);
                    }
                    else if (parts.length == 3) {
                        int location = Integer.parseInt(parts[1]);
                        int piece = Integer.parseInt(parts[2]);

                        server.handleMove(this, piece, location);
                    }
                    else {
                        sendPacket(Protocol.ERROR + Protocol.SEPARATOR + "Invalid Move Format");
                    }
                    break;

                case Protocol.LIST:
                    String users = server.getUserList();
                    sendPacket(Protocol.LIST + Protocol.SEPARATOR + users);
                    break;

                case Protocol.CHAT:
                    if (parts.length > 1) {
                        String text = parts[1];
                        server.broadcast(Protocol.CHAT + Protocol.SEPARATOR + this.username + Protocol.SEPARATOR + text);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GameServer getServer() {
        return this.server;
    }
    @Override
    public void handleDisconnect() {
        System.out.println(Protocol.DISCONNECT + Protocol.SEPARATOR + this.username);
        if (server != null) {
            server.handleDisconnect(this);
        }
    }
}