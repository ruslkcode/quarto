package client;

import gameLogic.Game;
import gameLogic.Move;
import java.util.Scanner;

public class HumanClient extends AbstractClient {

    private final Scanner scanner;

    /**
     * Creates a new Player object.
     *
     * @param name the name of the player.
     */
    public HumanClient(String name) {
        super(name);
        this.scanner = new Scanner(System.in);
    }

    /**
     * Determines the move if it is the HumanClient. Asks only for the piece id, if it is the first move.
     * Asks for both piece id and the index of the field, if it is not the first move.
     * @param game the current game.
     * @return the correctly formed Move parameter.
     */
    @Override
    public Move determineMove(Game game) {
        while (true) {

            // FIRST MOVE: only choose a piece
            if (game.getCurrentPieceID() == -1) {
                System.out.print("Enter piece index to give (0-15): ");
                String pieceInput = scanner.nextLine().trim();

                int pieceId;
                try {
                    pieceId = Integer.parseInt(pieceInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input! Please enter a number.");
                    continue;
                }

                if (pieceId < 0 || pieceId >= 16) {
                    System.out.println("Index out of range! Try again.");
                    continue;
                }

                return new Move(pieceId);
            }

            // NORMAL MOVE: place + choose next piece
            System.out.print("Enter field index (0-15): ");
            String indexInput = scanner.nextLine().trim();

            int index;
            try {
                index = Integer.parseInt(indexInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            if (index < 0 || index >= 16) {
                System.out.println("Index out of range! Try again.");
                continue;
            }

            System.out.print("Enter piece index to give (0-15): ");
            String pieceInput = scanner.nextLine().trim();

            int pieceId;
            try {
                pieceId = Integer.parseInt(pieceInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            if (pieceId < 0 || pieceId >= 16) {
                System.out.println("Index out of range! Try again.");
                continue;
            }

            return new Move(index, pieceId);
        }
    }
}
