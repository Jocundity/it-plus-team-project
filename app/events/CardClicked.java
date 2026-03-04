package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Card;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a card.
 * The event returns the position in the player's hand the card resides within.
 * 
 * { 
 *   messageType = “cardClicked”
 *   position = <hand index position [1-6]>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		
		int handPosition = message.get("position").asInt();
		
		// Get the clicked card from the player's hand using the position
        Card clickedCard = gameState.player1.getHandManager().getHandCards().get(handPosition - 1); 
        if (clickedCard == null) return;

        String cardName = clickedCard.getCardname();

        if (cardName.equals("Dark Terminus") || cardName.equals("Beamshock")) {
            
            // Check if the player has enough mana to play the card
            if (gameState.player1.getMana() < clickedCard.getManacost()) {
                BasicCommands.addPlayer1Notification(out, "Not enough mana!", 2);
                return;
            }

            // Turn on spell targeting mode and record the hand position of the clicked card
            gameState.isSpellTargeting = true;
            gameState.handPositionClicked = handPosition;
            
            // Clear previous standard movement highlights
            if (gameState.selectedTile != null) {
                gameState.highlightManager.clearHighlights(gameState.selectedTile, out);
                gameState.selectedTile = null;
            }

            // Highlight all enemy units on the board for targeting
            Tile[][] allTiles = gameState.board.getTiles();
            int rows = allTiles.length;
            int cols = allTiles[0].length;
            
            for (int x = 1; x <= cols; x++) {
                for (int y = 1; y <= rows; y++) {
                    Tile t = gameState.board.getTile(x, y);
                    // Highlight enemy units with a different highlight type (e.g., 2 for targeting)
                    if (t != null && t.hasUnit() && t.getUnit().getPlayer() != gameState.player1) {
                        BasicCommands.drawTile(out, t, 2); 
                    }
                }
            }
            BasicCommands.addPlayer1Notification(out, "Select enemy target", 2);
            
        } else {
            gameState.isSpellTargeting = false;
            gameState.handPositionClicked = handPosition;
        }
	}

}
