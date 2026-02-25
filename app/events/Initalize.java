package events;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
import structures.basic.AIPlayer;
import structures.basic.Avatar;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 *
 * @author Dr. Richard McCreadie
 */
public class Initalize implements EventProcessor{

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        // hello this is a change

        gameState.gameInitalised = true;
        gameState.something = true;

        /* Creates new player objects,
         * sets initial health and mana (Teammate's logic) */
        gameState.player1 = new Player();
        gameState.player1.showLife(out);
        gameState.player1.startTurn(out);

        gameState.player2 = new AIPlayer();
        gameState.player2.showLife(out);
        gameState.player2.drainMana(out);

        gameState.isPlayer1Turn = true;

        /* Places avatars on the board in starting positions
         * and sets initial health and attack*/
        Avatar player1Avatar = new Avatar(gameState.player1, 1);
        Tile tile1 = BasicObjectBuilders.loadTile(2, 3);
        BasicCommands.drawTile(out, tile1, 0);
        player1Avatar.drawUnit(out, tile1);

        Avatar player2Avatar = new Avatar(gameState.player2, 2);
        Tile tile2 = BasicObjectBuilders.loadTile(8, 3);
        BasicCommands.drawTile(out, tile2, 0);
        player2Avatar.drawUnit(out, tile2);

        try {
<<<<<<< HEAD
            // Initialise both decks
            gameState.player1.initDeck(OrderedCardLoader.getPlayer1Cards(1));
            gameState.player2.initDeck(OrderedCardLoader.getPlayer2Cards(1));

            // Each player draws 3 starting cards
            for (int i = 0; i < 3; i++) {
                gameState.player1.drawCard(out);  // renders UI
                gameState.player2.drawCard(out);  // logic only (no UI)
=======
            List<Card> player1Deck = OrderedCardLoader.getPlayer1Cards(1);
            gameState.player1.getDeck().setCards(player1Deck);
        
            // hello this is a change
            /* Story Card #2 (Over Draw):
             * Refactored initialization drawing logic to use the unified drawCard method.
             */
            for (int i = 0; i < 3; i++) {
                gameState.player1.drawCard(out);
>>>>>>> origin/wangminxuan
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // User 1 makes a change
        //CommandDemo.executeDemo(out);
        //Loaders_2024_Check.test(out);
    }

}
