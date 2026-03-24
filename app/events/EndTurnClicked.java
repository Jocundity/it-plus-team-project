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

    if (gameState.gameOver) return;

    if (gameState.player1 == null || gameState.player2 == null) {
        // Safety guard: if initialise hasn't run yet, do nothing.
        return;
    }
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
            
         // Reset movement and attack states for Player 2's units
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 6; y++) {
                    try {
                        Tile tile = gameState.board.getTile(x, y);
                        if (tile != null && tile.hasUnit()) {
                            Unit u = tile.getUnit();
                            if (u.getPlayer() == gameState.player2) {
                                // [Bug Fix] If the unit is stunned, it should lose its movement and attack capabilities for this turn, then have the stun status removed at the end of the turn. Otherwise, it should function normally.
                                if (u.getIsStunned()) {
                                    u.setCanMove(false);
                                    u.setCanAttack(false);
                                    u.setIsStunned(false); 
                                } else {
                                    u.setCanMove(true);
                                    u.setCanAttack(true);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Safely ignore out-of-bounds coordinates
                    } 
                }
            }
            
            gameState.player1.showMana(out);
            gameState.player2.showMana(out);

			if (gameState.gameOver) return;
            
            handleAITurn(out, gameState);
            
        } else {
        	// Do nothing if button is clicked during player 2's turn
        	return;
            }
    }
        
private void handleAITurn(final ActorRef out, final GameState gameState) {
    new Thread(() -> {
        try {
            if (gameState.gameOver) return;

            BasicCommands.addPlayer1Notification(out, "Player 2's turn", 2);
            Thread.sleep(2000);

            if (gameState.gameOver) return;

            Card aiCard = gameState.player2.chooseCard(gameState);

            if (gameState.gameOver) return;

            if (aiCard == null) {
                // Execute AI board actions even if no card is played
                gameState.player2.processAIBoardActions(gameState, out);
                Thread.sleep(1500);

                if (gameState.gameOver) return;

                gameState.player2.drainMana(out);

                if (gameState.gameOver) return;

                gameState.isPlayer1Turn = true;
                gameState.player1.startTurn(out);

                // Reset movement and attack states for Player 1's units
                for (int x = 1; x <= 9; x++) {
                    for (int y = 1; y <= 5; y++) {
                        Tile tile = gameState.board.getTile(x, y);
                        if (tile != null && tile.hasUnit()) {
                            Unit u = tile.getUnit();
                            if (u.getPlayer() == gameState.player1) {
                                if (u.getIsStunned()) {
                                    u.setCanMove(false);
                                    u.setCanAttack(false);
                                    u.setIsStunned(false);
                                    BasicCommands.addPlayer1Notification(out, "Stunned units are no longer stunned.", 1);
                                } else {
                                    u.setCanMove(true);
                                    u.setCanAttack(true);
                                }
                            }
                        }
                    }
                }

                if (gameState.gameOver) return;

                gameState.player1.showMana(out);
                gameState.player2.showMana(out);
                BasicCommands.addPlayer1Notification(out, "Player 1's turn", 2);
                return;
            }

            BasicCommands.addPlayer1Notification(out, "Player 2 chose " + aiCard.getCardname(), 2);
            Thread.sleep(2000);

            if (gameState.gameOver) return;

            // Play the selected card
            gameState.player2.playCard(aiCard, gameState, out, gameState.highlightManager);
            Thread.sleep(2000);

            if (gameState.gameOver) return;

            // Execute AI board actions (triggers Rush for specific units)
            gameState.player2.processAIBoardActions(gameState, out);
            Thread.sleep(1500);

            if (gameState.gameOver) return;

            gameState.player2.drainMana(out);
            gameState.player2.drawCard(out);

            if (gameState.gameOver) return;

            // Transition turn back to Player 1
            gameState.isPlayer1Turn = true;
            gameState.player1.startTurn(out);

            // Reset movement and attack states for Player 1's units
            for (int x = 1; x <= 9; x++) {
                for (int y = 1; y <= 5; y++) {
                    Tile tile = gameState.board.getTile(x, y);
                    if (tile != null && tile.hasUnit()) {
                        Unit u = tile.getUnit();
                        if (u.getPlayer() == gameState.player1) {
                            if (u.getIsStunned()) {
                                u.setCanMove(false);
                                u.setCanAttack(false);
                                u.setIsStunned(false);
                                BasicCommands.addPlayer1Notification(out, "Stunned units are no longer stunned.", 1);
                            } else {
                                u.setCanMove(true);
                                u.setCanAttack(true);
                            }
                        }
                    }
                }
            }

            if (gameState.gameOver) return;

            gameState.player1.showMana(out);
            gameState.player2.showMana(out);

            if (!gameState.gameOver) {
                BasicCommands.addPlayer1Notification(out, "Player 1's turn", 2);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}
}
