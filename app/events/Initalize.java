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
import structures.basic.Board;
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
public class Initalize implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        gameState.gameInitalised = true;
        gameState.something = true;
        
        /* Create game board */
        gameState.board = new Board();
        gameState.board.drawBoard(out);

        // Allow the front-end ample time to render the 45 tiles to prevent message channel congestion 
        try { Thread.sleep(1000); } catch (Exception e) {}
        
        /* Creates new player objects,
         * sets initial health and mana (Story card 3) */
        gameState.player1 = new Player();
        gameState.player1.showLife(out);
        gameState.player1.startTurn(out);

        gameState.player2 = new AIPlayer();
        gameState.player2.showLife(out);
        gameState.player2.drainMana(out);

        gameState.isPlayer1Turn = true;

        /* Places avatars on the board in starting positions */
        Avatar player1Avatar = new Avatar(gameState.player1, 1);
        Tile tile1 = gameState.board.getTile(2, 3);
        tile1.setUnit(player1Avatar);
        player1Avatar.setPositionByTile(tile1);
        
        // Set the avatar's canMove and canAttack to true at the start of the game for testing purposes
        player1Avatar.setPlayer(gameState.player1);
        player1Avatar.setCanMove(true);
        player1Avatar.setCanAttack(true);
        
        player1Avatar.setAttack(2);
        player1Avatar.setMaxHealth(20);
        player1Avatar.setHealth(20);

        BasicCommands.drawTile(out, tile1, 0);
        try { Thread.sleep(200); } catch (Exception e) {}
        player1Avatar.drawUnit(out, tile1);
        

        Avatar player2Avatar = new Avatar(gameState.player2, 2);
        Tile tile2 = gameState.board.getTile(8, 3);    
        player2Avatar.setPlayer(gameState.player2);
        tile2.setUnit(player2Avatar);
        player2Avatar.setPositionByTile(tile2);
        
        player2Avatar.setAttack(2);
        player2Avatar.setMaxHealth(20);
        player2Avatar.setHealth(20);
        
        BasicCommands.drawTile(out, tile2, 0);
        try { Thread.sleep(200); } catch (Exception e) {}
        player2Avatar.drawUnit(out, tile2);

        try {
            // Initialise both decks
            List<Card> player1Deck = OrderedCardLoader.getPlayer1Cards(1);
            List<Card> player2Deck = OrderedCardLoader.getPlayer2Cards(1);
            
            /*  For testing purposes
            Card darkTerminus = null;
            for (Card c : player1Deck) { if (c.getCardname().equals("Dark Terminus")) darkTerminus = c; }
            if (darkTerminus != null) { player1Deck.remove(darkTerminus); player1Deck.add(0, darkTerminus); }

            Card beamShock = null;
            for (Card c : player2Deck) { if (c.getCardname().equals("Beamshock")) beamShock = c; }
            if (beamShock != null) { player1Deck.add(1, beamShock); }
            */

            gameState.player1.initDeck(player1Deck);
            gameState.player2.initDeck(player2Deck);

            // Each player draws 3 starting cards
            for (int i = 0; i < 3; i++) {
                gameState.player1.drawCard(out);
                gameState.player2.drawCard(out);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        

        //CommandDemo.executeDemo(out);
        //Loaders_2024_Check.test(out);
    }
}