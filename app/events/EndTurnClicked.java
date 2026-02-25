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
            // Safety guard: if initialise hasn't run yet, do nothing.
            return;
        }

        if (gameState.isPlayer1Turn) {
            // Player 1 ends turn
            gameState.player1.drainMana(out);

            // Switch to Player 2
            gameState.isPlayer1Turn = false;
            gameState.player2.startTurn(out);

            // Player 2 draws 1 card (logic only)
            gameState.player2.drawCard(out);
        } else {
            // Player 2 ends turn
            gameState.player2.drainMana(out);

            // Switch to Player 1
            gameState.isPlayer1Turn = true;
            gameState.player1.startTurn(out);

            // Player 1 draws 1 card (rendered)
            gameState.player1.drawCard(out);
        }

        // Refresh mana display on both sides
        gameState.player1.showMana(out);
        gameState.player2.showMana(out);
    }
}
