package events;

import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
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

        	// Mandatory UI and State Cleanup before turn ends
        	
        	// 1. Clear all board highlights (movement/attack ranges)
            gameState.highlightManager.clearHighlights(null, out);
            
            // 2. Deselect the currently selected tile/unit
            gameState.selectedTile = null;
            
            // 3. Forcefully disable summoning and spell targeting modes
            gameState.isUnitSummoning = false;
            gameState.isSpellTargeting = false;
            
            // 4. Reset hand selection and restore card UI to default state (0 = normal)
            gameState.handPositionClicked = -1;
            gameState.selectedCard = null;
            for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
                structures.basic.Card c = gameState.player1.getHandManager().getHandCards().get(i);
                BasicCommands.drawCard(out, c, i + 1, 0); 
            }
        	
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
            for (int x = 0; x <= 9; x++) {
                for (int y = 0; y <= 5; y++) {
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
