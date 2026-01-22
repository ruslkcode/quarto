package client;

import gameLogic.Game;
import gameLogic.Move;

import java.util.Scanner;

public class HumanClient extends AbstractClient {

    private final Scanner scanner;

    /**
     * Creates a new human client.
     *
     * @param name the name of the player
     */
    public HumanClient(String name, Scanner scanner) {
        super(name);
        this.scanner = scanner;
    }

    /**
     * Determines the move for a human player.
     *
     * Supported commands during a normal turn:
     * - win  : claim victory (nextPiece = 16)
     * - draw : claim draw (nextPiece = 17)
     *
     * @param game the current game state
     * @return the chosen move
     */
    /*@
      requires game != null;
      ensures \result != null;
    @*/
    @Override
    public Move determineMove(Game game) {

        while (true) {

            // FIRST MOVE: only choose a piece
            if (game.getCurrentPieceID() == -1) {
                System.out.print("Enter piece index to give (0-15): ");
                String input = scanner.nextLine().trim();

                int pieceId;
                try {
                    pieceId = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Enter a number.");
                    continue;
                }

                if (pieceId < 0 || pieceId > 15) {
                    System.out.println("Piece index must be between 0 and 15.");
                    continue;
                }

                return new Move(pieceId);
            }

            // NORMAL TURN
            System.out.print("Enter field index (0-15), or 'win', or 'draw': ");
            String input = scanner.nextLine().trim().toLowerCase();

            // Claim victory
            if (input.equals("win")) {
                System.out.print("Enter field index for winning move (0-15): ");
                String locInput = scanner.nextLine().trim();

                int location;
                try {
                    location = Integer.parseInt(locInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Enter a number.");
                    continue;
                }

                if (location < 0 || location > 15) {
                    System.out.println("Field index must be between 0 and 15.");
                    continue;
                }

                return new Move(16, location);
            }

            // Claim draw
            if (input.equals("draw")) {
                return new Move(17);
            }

            // Normal move: place + give piece
            int location;
            try {
                location = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number, 'win', or 'draw'.");
                continue;
            }

            if (location < 0 || location > 15) {
                System.out.println("Field index must be between 0 and 15.");
                continue;
            }

            System.out.print("Enter piece index to give (0-15): ");
            String pieceInput = scanner.nextLine().trim();

            int pieceId;
            try {
                pieceId = Integer.parseInt(pieceInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number.");
                continue;
            }

            if (pieceId < 0 || pieceId > 15) {
                System.out.println("Piece index must be between 0 and 15.");
                continue;
            }

            return new Move(pieceId, location);
        }
    }
}
