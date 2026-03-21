package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Card;

/**
 * Indicates that the user has clicked an object on the game canvas, in this
 * case a card. The event returns the position in the player's hand the card
 * resides within.
 *
 * {
 * messageType = 鈥渃ardClicked鈥� position = <hand index position [1-6]> }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
    	
    	gameState.highlightManager.clearHighlights(out);

        // If it is not currently Player 1's turn, directly ignore all click operations on the hand cards.
        if (!gameState.isPlayer1Turn) {
            return;
        }

        int handPosition = message.get("position").asInt();

        // Check if the clicked position exceeds the length of the current hand card list
        int cardIndex = handPosition - 1;
        if (cardIndex < 0 || cardIndex >= gameState.player1.getHandManager().getHandCards().size()) {
            return; // If it goes out of bounds (clicking on an empty position), the click is directly ignored to prevent crashes
        }

        // Safely get cards
        Card clickedCard = gameState.player1.getHandManager().getHandCards().get(cardIndex);
        if (clickedCard == null) {
            return;
        }

        // (Story 32) Mana pre-check: Prevent casting/spawning if insufficient
        if (gameState.player1.getMana() < clickedCard.getManacost()) {
            BasicCommands.addPlayer1Notification(out, "Not enough mana!", 2);
            return;
        }

        // Clear highlight from previously selected board tile
        if (gameState.selectedTile != null) {
            gameState.highlightManager.clearHighlights(out);
            gameState.selectedTile = null;
        }

        // (Story 32) UI feedback: Unhighlight all other hand cards, only highlight the currently clicked card
        for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
            Card c = gameState.player1.getHandManager().getHandCards().get(i);
            BasicCommands.drawCard(out, c, i + 1, 0); // 0 = normal state
        }
        BasicCommands.drawCard(out, clickedCard, handPosition, 1); // 1 = highlighted state

        // Track selected card globally for use in TileClicked handler
        gameState.selectedCard = clickedCard;
        gameState.handPositionClicked = handPosition;

        String cardName = clickedCard.getCardname();

            // List the cards that are NOT units based on your JSON files
            if (cardName.equals("Wraithling Swarm")
                    || cardName.equals("Dark Terminus")
                    || cardName.equals("Beamshock")
                    || cardName.equals("Sundrop Elixir")
                    || cardName.equals("Horn of the Forsaken")) {

            // This is a spell/artifact, NOT a unit
            gameState.isUnitSummoning = false;
            gameState.isSpellTargeting = true; // Use this flag for spells instead

            // Clear old highlights and don't show summonable tiles
            gameState.highlightManager.clearHighlights(out);
            BasicCommands.addPlayer1Notification(out, "Spell Selected: " + cardName, 2);

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
                    gameState.highlightManager.clearHighlights(out);
                    gameState.selectedTile = null;
                }

                // (Story 31) Highlight valid spell targets through HighlightManager
                gameState.highlightManager.highlightSpellTargets(gameState, out);
                BasicCommands.addPlayer1Notification(out, "Select enemy target", 2);
            } // =====================================================================
            // Spell Targeting Logic: Wraithling Swarm
            // =====================================================================
            else if (cardName.equals("Wraithling Swarm")) {

                // 1. Validate mana availability before proceeding with spell targeting
                if (gameState.player1.getMana() < clickedCard.getManacost()) {
                    BasicCommands.addPlayer1Notification(out, "Not enough mana!", 2);
                    return;
                }

                // 2. Enable spell targeting mode and store the selected card index
                gameState.isSpellTargeting = true;
                gameState.handPositionClicked = handPosition;

                // 3. Clear any existing board highlights to prevent visual overlap
                if (gameState.selectedTile != null) {
                    gameState.highlightManager.clearHighlights(out);
                    gameState.selectedTile = null;
                }

                // 4. Use HighlightManager to properly track and highlight empty tiles
                gameState.highlightManager.highlightEmptyTiles(gameState, out);

                // 5. Provide UI feedback to the user instructing them on the next action
                BasicCommands.addPlayer1Notification(out, "Select empty tile to swarm!", 2);
            }
            
            else if (cardName.equals("Horn of the Forsaken")) {
                gameState.handPositionClicked = handPosition;
                gameState.highlightManager.highlightFriendlyAvatar(gameState, out); 
                BasicCommands.addPlayer1Notification(out, "Select your Avatar to equip", 2);
            }

        } else {
            // Only trigger summoning mode if it's NOT a spell
            gameState.isSpellTargeting = false;
            gameState.isUnitSummoning = true;
            gameState.highlightManager.highlightSummonableTiles(gameState, out);
        }
    }
}
