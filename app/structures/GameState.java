package structures;

import structures.basic.AIPlayer;
import structures.basic.Board;
import structures.basic.HighlightManager;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Card;

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
    
    // --- Added for Story 15 (Win/Loss) ---
    public boolean gameOver = false;
    
    // add board
    public Board board;
    
    // add highlight manager for board
    public HighlightManager highlightManager = new HighlightManager();
    
    // record the selected tile for the player
    public Tile selectedTile = null;
	
    // record the selected unit for the player
    public int handPositionClicked = -1;
    
    // record the unit that is being dragged by the player
    public boolean isSpellTargeting = false;
    
    // record whether the player is currently preparing to summon a unit (#32)
    public boolean isUnitSummoning = false;

    // record the currently selected hand cards (#25, #32)
    public Card selectedCard = null;
}
