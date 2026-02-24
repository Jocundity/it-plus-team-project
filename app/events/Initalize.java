package events;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import structures.basic.Card;
import utils.OrderedCardLoader;
import akka.actor.ActorRef;
import commands.BasicCommands;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.AIPlayer;
import structures.basic.Avatar;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// hello this is a change
		
		gameState.gameInitalised = true;
		
		gameState.something = true;
		
		/* Creates new player objects,
		 * sets initial health */
		Player player1 = new Player();
        gameState.player1 = player1;
		player1.showLife(out);
		AIPlayer player2 = new AIPlayer();
		player2.showLife(out);
		
		/* Places avatars on the board in starting positions 
		 * and sets initial health and attack*/
		Avatar player1Avatar = new Avatar(player1, 1);
		Tile tile1 = BasicObjectBuilders.loadTile(2, 3);
		BasicCommands.drawTile(out, tile1, 0);
		player1Avatar.drawUnit(out, tile1);
		
		Avatar player2Avatar = new Avatar(player2, 2);
		Tile tile2 = BasicObjectBuilders.loadTile(8, 3);
		BasicCommands.drawTile(out, tile2, 0);
		player2Avatar.drawUnit(out, tile2);

        try {
            List<Card> player1Deck = OrderedCardLoader.getPlayer1Cards(1);
            player1.getDeck().setCards(player1Deck);

            for (int i = 1; i <= 3; i++) {
                Card drawnCard = player1.getDeck().drawTopCard();
                if (drawnCard != null) {
                    boolean added = player1.getHandManager().addCardToHand(drawnCard);
                    if (added) {
                        BasicCommands.drawCard(out, drawnCard, i, 0);
                        Thread.sleep(200);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		

		
		
		
		// User 1 makes a change
		//CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		//Loaders_2024_Check.test(out);
	}

}


