package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

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
		if (gameState.player1 == null || gameState.player2 == null) {
			// Safety guard: if initialise hasn't run yet, do nothing.
			return;
		}
		if (gameState.isPlayer1Turn) {
			// #5: End turn drains remaining mana
			gameState.player1.drainMana(out);

			// switch turn
			gameState.isPlayer1Turn = false;

			// #4: Start of turn grants mana = turnNumber + 1
			gameState.player2.startTurn(out);
		}
		else {
			gameState.player2.drainMana(out);
			gameState.isPlayer1Turn = true;
			gameState.player1.startTurn(out);
		}
		// Forcibly refresh the mana display on both sides to 
		// solve the problem of "delaying one beat" (click the EndTurn 
		// button once to update only the mana value on one side, not the mana value on both sides).
		gameState.player1.showMana(out);
		gameState.player2.showMana(out);
	}

}
