package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.HighlightManager;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * 
 * { 
 *   messageType = “tileClicked”
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        // get the x and y indices of the tile that was clicked
		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

		BasicCommands.addPlayer1Notification(out, "Clicked: " + tilex + ", " + tiley + ",", 2);

		Tile clickedTile = gameState.board.getTile(tilex, tiley);

		if (clickedTile == null) return;

        if (gameState.selectedTile != null && gameState.selectedTile.hasUnit() && !clickedTile.hasUnit()) {
            Tile startTile = gameState.selectedTile;
            Unit unitToMove = startTile.getUnit();

            int dx = Math.abs(clickedTile.getTilex() - startTile.getTilex());
            int dy = Math.abs(clickedTile.getTiley() - startTile.getTiley());

            if (dx <= 2 && dy <= 2) {

                gameState.highlightManager.clearHighlights(clickedTile, out);
                try { Thread.sleep(50); } catch (Exception e) {}

                commands.BasicCommands.moveUnitToTile(out, unitToMove, clickedTile);
                
                clickedTile.setUnit(unitToMove);
                startTile.setUnit(null);
                unitToMove.setPositionByTile(clickedTile);

                gameState.selectedTile = null;
                return;
            }
        }
		
		// (Story card 8) clear highlights
		gameState.highlightManager.clearHighlights(clickedTile, out);

				
		/* (Story card 6) 
		 Highlight tiles if player clicks on unit who 
		 hasn't moved on attacked yet */
	    if (clickedTile.hasUnit()) {
			Unit unitOnTile = clickedTile.getUnit();
			if (unitOnTile.getPlayer() == gameState.player1 && 
                    unitOnTile.getCanMove() && unitOnTile.getCanAttack()) {
                    
                gameState.selectedTile = clickedTile;
                gameState.highlightManager.highlightMovementRange(clickedTile, gameState, out);
            }			
					
		} else {
			gameState.selectedTile = null;
					
	    }
			
			
	    }
    }
