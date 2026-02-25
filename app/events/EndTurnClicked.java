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

        Player player = gameState.player1;
        Card drawnCard = player.getDeck().drawTopCard();

        if (drawnCard != null) {
            boolean added = player.getHandManager().addCardToHand(drawnCard);

            if (added) {
                int handPosition = player.getHandManager().getHandCards().size();
                BasicCommands.drawCard(out, drawnCard, handPosition, 0);
            } else {
                BasicCommands.addPlayer1Notification(out, "Hand is full! Card discarded.", 2);
            }
        } else {
            BasicCommands.addPlayer1Notification(out, "Deck is empty!", 2);
        }
    }
}