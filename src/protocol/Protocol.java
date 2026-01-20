package protocol;

/**
 * Protocol class with constants for creating protocol messages
 */
public final class Protocol {

    public static final String SEPARATOR = "~";


    public static final String HELLO = "HELLO";
    public static final String LOGIN = "LOGIN";
    public static final String ALREADYLOGGEDIN = "ALREADYLOGGEDIN";
    public static final String LIST = "LIST";
    public static final String NEWGAME = "NEWGAME";
    public static final String MOVE = "MOVE";
    public static final String GAMEOVER = "GAMEOVER";

    public static final String ERROR = "ERROR";

    public static final String QUEUE = "QUEUE";

    //game over
    public static final String VICTORY = "VICTORY";
    public static final String DRAW = "DRAW";
    public static final String DISCONNECT = "DISCONNECT";

    // extentions
    public static final String RANK = "RANK";
    public static final String CHAT = "CHAT";
    public static final String WHISPER = "WHISPER"; // [cite: 348]
    public static final String NOISE = "NOISE";
    public static final String NAMEDQUEUES = "NAMEDQUEUES";


    private Protocol() {
        // Private constructor to prevent instantiation
    }
}
