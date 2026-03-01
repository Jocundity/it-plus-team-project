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
 *   messageType = "tileClicked"
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
                
                // hello, this is a change: Lock movement after an attack
                attacker.setCanMove(false);

                // end selection after attack
                gameState.selectedTile = null;
                return;
            }
            // (Story Card 10) Move and Attack combo for non-adjacent enemies
            else if (enemy && !adjacent && attacker.getCanAttack()) {
                
                Tile landingTile = null;
                
                int minDistance = 999;
                
                // 1. Find a valid landing tile: check the 4 adjacent tiles around the target enemy
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] dir : directions) {
                    int checkX = clickedTile.getTilex() + dir[0];
                    int checkY = clickedTile.getTiley() + dir[1];
                    
                    // Get the tile at these coordinates (returns null if out of bounds)
                    Tile adjacentToTarget = gameState.board.getTile(checkX, checkY);
                    
                    // If the tile exists and is empty (no other unit standing there)
                    if (adjacentToTarget != null && !adjacentToTarget.hasUnit()) {
                    	
                    	// Check if this empty tile is within the attacker's movement range
                        int moveDx = Math.abs(adjacentToTarget.getTilex() - attackerTile.getTilex());
                        int moveDy = Math.abs(adjacentToTarget.getTiley() - attackerTile.getTiley());
                        
                        if (moveDx <= 2 && moveDy <= 2) {
                        	//Replaced the basic break with distance calculation
                        	int distFromAttacker = moveDx + moveDy;
                            if (distFromAttacker < minDistance) {
                                minDistance = distFromAttacker;
                                landingTile = adjacentToTarget;
                            }
                        }
                    }
                }
                
                // 2. If a valid landing tile is found, execute the Move + Attack combo
                if (landingTile != null) {
                    
                    // Clear highlights before action
                    gameState.highlightManager.clearHighlights(clickedTile, out);
                    
                    // [Action 1: Move]
                    commands.BasicCommands.moveUnitToTile(out, attacker, landingTile);
                    
                    // Update backend board state
                    landingTile.setUnit(attacker);
                    attackerTile.setUnit(null);
                    attacker.setPositionByTile(landingTile);
                    
                    // VERY IMPORTANT: Wait for the movement animation to finish before attacking
                    try { Thread.sleep(1500); } catch (Exception e) {}
                    
                    // [Action 2: Attack]
                    int ms = BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                    try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
                    
                    // Apply damage
                    int newHealth = target.getHealth() - attacker.getAttack();
                    target.setHealth(newHealth);
                    
                    // Update UI health
                    BasicCommands.setUnitHealth(out, target, Math.max(newHealth, 0));
                    try { Thread.sleep(150); } catch (InterruptedException e) { e.printStackTrace(); }
                    
                 // Check for death
                    if (newHealth <= 0) {
                        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.death);
                        try { Thread.sleep(300); } catch (InterruptedException e) { e.printStackTrace(); }
                        BasicCommands.deleteUnit(out, target);
                        clickedTile.setUnit(null); // Clear the board reference
                    }

                    // [Action 3: End action]
                    attacker.setCanAttack(false);
                    attacker.setCanMove(false);
                    gameState.selectedTile = null;
                    return;
                    
                } else {
                    BasicCommands.addPlayer1Notification(out, "Target is out of reach!", 2);
                }
             }
        }
        
        // Safety: only implement attack/highlight for human turn as per story
        if (gameState.selectedTile != null && gameState.selectedTile.hasUnit() && !clickedTile.hasUnit()) {
            Tile startTile = gameState.selectedTile;
            Unit unitToMove = startTile.getUnit();

            int dx = Math.abs(clickedTile.getTilex() - startTile.getTilex());
            int dy = Math.abs(clickedTile.getTiley() - startTile.getTiley());
            
            // #hello, this is a change: Added condition to check if unit can actually move
            if (dx <= 2 && dy <= 2 && unitToMove.getCanMove()) {

                gameState.highlightManager.clearHighlights(clickedTile, out);
                try { Thread.sleep(50); } catch (Exception e) {}

                commands.BasicCommands.moveUnitToTile(out, unitToMove, clickedTile);
                
                clickedTile.setUnit(unitToMove);
                startTile.setUnit(null);
                unitToMove.setPositionByTile(clickedTile);

                // hello, this is a change: Prevent infinite movement after a valid move
                unitToMove.setCanMove(false);
                
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
                
                // hello, this is a change: Wrap highlights in condition checks
                if (unitOnTile.getCanMove()) {
                	gameState.highlightManager.highlightMovementRange(clickedTile, gameState, out);
                }

                // Story 7: attack highlight (red on adjacent enemy units)
                if (unitOnTile.getCanAttack()) {
                	gameState.highlightManager.highlightAttackTargets(clickedTile, gameState, out);
                }
            }			
					
	        } else {
                 gameState.selectedTile = null;
            }
       } 
} 