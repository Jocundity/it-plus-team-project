package events;

import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 */
public class EndTurnClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        if (gameState.player1 == null || gameState.player2 == null) {
            return;
        }

        if (gameState.isPlayer1Turn) {
            System.out.println("[STATE] Human Player ending turn!");
            System.out.println("[ACTION] Drawing card for Human...");
            
            //Story Card #1: Humans draw a card when pressing the end button
            gameState.player1.drawCard(out);
            // Reset mana, switch turn to AI
            gameState.player1.drainMana(out);
            gameState.isPlayer1Turn = false;
            gameState.player2.startTurn(out);
        }
        else {
        	// AI ends turn, switching back to human player
            gameState.player2.drainMana(out);
            gameState.isPlayer1Turn = true;
            gameState.player1.startTurn(out);
        }

        // Refresh the display of both sides' mana values
        gameState.player1.showMana(out);
        gameState.player2.showMana(out);
    }
}