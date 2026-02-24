package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import commands.BasicCommands;
import structures.basic.Card;
import structures.basic.Player;
/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = “endTurnClicked”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        if (gameState.gameInitalised && gameState.player1 != null) {
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

}
