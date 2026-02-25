package structures;

import structures.basic.AIPlayer;
import structures.basic.Player;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * * @author Dr. Richard McCreadie
 *
 */
public class GameState {

    public boolean gameInitalised = false;

    public boolean something = false;

    // --- Added for turn/mana management (Story #4/#5) ---
    public Player player1;
    public AIPlayer player2;

    // true = Player 1's turn, false = Player 2's turn
    public boolean isPlayer1Turn = true;
}
