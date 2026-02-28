package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.UnitAnimationType;
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

		Tile clickedTile = gameState.board.getTile(tilex, tiley);
		if (clickedTile == null) return;

        // Optional debug
        BasicCommands.addPlayer1Notification(out, "Clicked: " + tilex + ", " + tiley + ",", 2);
        
        // (Story card 9) If selected unit can attack, and click adjacent enemy -> attack
        if (gameState.selectedTile != null && gameState.selectedTile.hasUnit() && clickedTile.hasUnit()) {

            Tile attackerTile = gameState.selectedTile;
            Unit attacker = attackerTile.getUnit();
            Unit target = clickedTile.getUnit();

            boolean enemy = (target.getPlayer() != attacker.getPlayer());
            int dx = Math.abs(clickedTile.getTilex() - attackerTile.getTilex());
            int dy = Math.abs(clickedTile.getTiley() - attackerTile.getTiley());
            boolean adjacent = (dx + dy == 1);

            if (enemy && adjacent && attacker.getCanAttack()) {

                // clear old highlights
                gameState.highlightManager.clearHighlights(clickedTile, out);

                // play attack animation
                int ms = BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                try {Thread.sleep(ms);} catch (InterruptedException e) {e.printStackTrace();}

                // damage
                int newHealth = target.getHealth() - attacker.getAttack();
                target.setHealth(newHealth);

                // update UI
                BasicCommands.setUnitHealth(out, target, Math.max(newHealth, 0));
                try {Thread.sleep(150);} catch (InterruptedException e) {e.printStackTrace();}

                // if dead -> delete
                if (newHealth <= 0) {
                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.death);
                    try {Thread.sleep(300);} catch (InterruptedException e) {e.printStackTrace();}
                    BasicCommands.deleteUnit(out, target);
                    clickedTile.setUnit(null);
                }

                // mark attacker has attacked
                attacker.setCanAttack(false);

                // end selection after attack
                gameState.selectedTile = null;
                return;
            }
        }
        // Safety: only implement attack/highlight for human turn as per story
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
			if (unitOnTile.getPlayer() == gameState.player1){
                    
                gameState.selectedTile = clickedTile;
                gameState.highlightManager.highlightMovementRange(clickedTile, gameState, out);

                // Story 7: attack highlight (red on adjacent enemy units)
                gameState.highlightManager.highlightAttackTargets(clickedTile, gameState, out);
            }			
					
		} else {
			gameState.selectedTile = null;
					
	    }
			
			
	    }
    }
