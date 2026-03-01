package events;

import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
//hello, this is a change: import these classes
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 */
public class EndTurnClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (gameState.player1 == null || gameState.player2 == null) {
            // Safety guard: if initialise hasn't run yet, do nothing.
            return;
        }

        if (gameState.isPlayer1Turn) {

            // Human ends turn
            System.out.println("[STATE] Human Player ending turn!");
            System.out.println("[ACTION] Drawing card for Human...");

            // Story Card #1: Humans draw a card when pressing the end button
            gameState.player1.drawCard(out);

            // Reset mana, switch turn to AI
            gameState.player1.drainMana(out);

            // Switch to Player 2
            gameState.isPlayer1Turn = false;
            gameState.player2.startTurn(out);

        } else {

            // AI ends turn, switching back to human player
            gameState.player2.drainMana(out);

            // Switch to Player 1
            gameState.isPlayer1Turn = true;
            gameState.player1.startTurn(out);

            // Player 1 draws 1 card (rendered)
            //gameState.player1.drawCard(out); 
            //This line of code has been commented out because it would cause the human player to draw a card at the end of the AI's turn.
            
            // hello, this is a change: 
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 5; y++) {
                    Tile tile = gameState.board.getTile(x, y);
                    if (tile != null && tile.hasUnit()) {
                        Unit u = tile.getUnit();
                        // As long as it is a unit of the human player, all of them will recover their stamina
                        if (u.getPlayer() == gameState.player1) {
                            u.setCanMove(true);
                            u.setCanAttack(true);
                        }
                    }
                }
            }
            // change end
        }

        // Refresh mana display on both sides
        gameState.player1.showMana(out);
        gameState.player2.showMana(out);
    }
}
