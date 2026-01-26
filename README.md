# Project 99 – Quarto

This project is a client–server implementation of the board game Quarto.
It was developed as part of the Software Systems course.

The application uses a text-based user interface (TUI) and supports:
Human vs Human games and Human vs AI games using different AI strategies.
The system is implemented using standard Java and TCP socket communication.

---

## Requirements

- Java JDK 25 (or compatible version 23 or higher)
- IntelliJ IDEA or standard Java command-line tools
- No external libraries are required

---

## External Dependencies

This project does not use any external dependencies or libraries.
Only standard Java libraries are used.

---

## Entry Points

The following classes contain a `public static void main(String[] args)` method.

Server entry point:
server.GameServer

Human client entry point (text-based interface):
client.QuartoTUI

AI client entry point:
client.QuartoTUI  
(The AI mode is selected interactively at startup.)

---

## How to Build the Project

Using IntelliJ IDEA:
1. Open the project in IntelliJ IDEA.
2. Make sure the correct JDK is selected:
   File → Project Structure → Project SDK.
3. Build the project using:
   Build → Build Project.

Using the command line (from the project root):

---

## How to Run the Program

### Start the Server

From the project root directory:

The server must be started before any clients connect.

---

### Start the Client (Human or AI)

From the project root directory:

After starting the client, the user is prompted to:
- Enter a username
- Choose between Human mode or AI mode
- Connect to the server and queue for a game

---

## How to Run the Tests

Using IntelliJ IDEA:
1. Right-click the `test` directory.
2. Select “Run All Tests”.

All tests are implemented using JUnit and cover the core game logic,
client behavior, and server functionality.

---

## Usage Instructions

Available client commands:
- `queue` – join the matchmaking queue
- `rank` – show the ranking list
- `quit` – exit the client

During a game, the client provides on-screen instructions for making moves.

---

## Authors

Artem Novokhatnii  
Ruslan Morozov

---

## Project Status

The project is complete and fulfills the requirements of the Software Systems course.
