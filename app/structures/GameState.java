package structures;

import structures.basic.Player;
import structures.basic.AIPlayer;
import structures.basic.Tile;
import structures.basic.Board;
import structures.basic.HighlightManager;

import java.util.List;

import commands.BasicCommands;

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
    
    // add board
    public Board board;
    
    // add highlight manager for board
    public HighlightManager highlightManager = new HighlightManager();
    
    // record the selected tile for the player
    public Tile selectedTile = null;
		
    
}
