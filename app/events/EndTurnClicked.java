package events;

import com.fasterxml.jackson.databind.JsonNode;
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 *
 * @author Dr. Richard McCreadie
 */
public class EndTurnClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        if (gameState.player1 == null || gameState.player2 == null) {
            return;
        }

        if (gameState.isPlayer1Turn) {
            gameState.player1.drainMana(out);
            gameState.isPlayer1Turn = false;
            gameState.player2.startTurn(out);
        }
        else {
            gameState.player2.drainMana(out);
            gameState.isPlayer1Turn = true;
            gameState.player1.startTurn(out);
        }

        gameState.player1.showMana(out);
        gameState.player2.showMana(out);

     // hello this is a change
        /* Story Card #2 (Over Draw):
         * Refactored drawing logic to use the encapsulated drawCard method inside Player.
         * This ensures all Over Draw rules and UI notifications are handled in one place.
         */
        Player player = gameState.player1;
        player.drawCard(out);
    }
}