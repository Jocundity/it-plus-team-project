package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.BigCard;
import structures.basic.Card;
import structures.basic.MiniCard;
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
            gameState.highlightManager.clearHighlights(out);
            
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
        	
            // Story Card #1: Humans draw a card when pressing the end button
            gameState.player1.drawCard(out);

            // Reset mana, switch turn to AI
            gameState.player1.drainMana(out);

            // Switch to Player 2
            gameState.isPlayer1Turn = false;
            gameState.player2.startTurn(out);
            
            gameState.player1.showMana(out);
            gameState.player2.showMana(out);
            
            handleAITurn(out, gameState);
            
        } else {
        	// Do nothing if button is clicked during player 2's turn
        	return;
            }
    }
        
        private void handleAITurn(final ActorRef out, final GameState gameState) {
        	new Thread(() -> {
        		try {
        			BasicCommands.addPlayer1Notification(out, "Player 2's turn", 2);
        			Thread.sleep(2000);
        			
        			// Let AI choose and play card 
        			// ** only 1 card per turn for now **
        			Card aiCard = gameState.player2.chooseCard(gameState);
        			BasicCommands.addPlayer1Notification(out, "Player 2 chose " + aiCard.getCardname(), 2);
        			Thread.sleep(2000);
        			gameState.player2.playCard(aiCard, gameState, out, gameState.highlightManager);
        			Thread.sleep(2000);
        			
        			
            		
            		// End turn actions
        			Thread.sleep(2000);
            		gameState.player2.drainMana(out);
            		gameState.player2.drawCard(out);
            		
            		// Switch back to Player 1
            		Thread.sleep(2000);
            		gameState.isPlayer1Turn = true;
            		gameState.player1.startTurn(out);
            		
            		// Ensure that Player 1's units can move and attack
            		for (int x = 0; x <= 9; x++) {
            			for (int y = 0; y <= 5; y++) {
            				Tile tile = gameState.board.getTile(x, y);
            				if (tile != null && tile.hasUnit()) {
            					Unit u = tile.getUnit();
            					if (u.getPlayer() == gameState.player1) {
            						u.setCanMove(true);
            						u.setCanAttack(true);
            					}
            				}
            			}
            		}
            	
            	// Show mana amounts on screen
            	gameState.player1.showMana(out);
            	gameState.player2.showMana(out);
            	
            	// Notify Player 1 that it's their turn
            	BasicCommands.addPlayer1Notification(out, "Player 1's turn", 2);
            	
        		} catch (Exception e) {
                    e.printStackTrace();
        		}
        		
        	}).start();
        }
}
