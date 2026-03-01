package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import java.util.ArrayList;
import java.util.Collections;
import structures.basic.Unit;

public class HighlightManager {
	
	ArrayList<Tile> targetTiles = new ArrayList<Tile> ();
	

	public HighlightManager() {
		// TODO Auto-generated constructor stub
	}
	
	// (Story card 6)
	public void highlightMovementRange(Tile tile, GameState gameState, ActorRef out) {
		// get position of unit tile
		int unitx = tile.getTilex();
		int unity = tile.getTiley();
		
		ArrayList<Tile> diagonalTiles = new ArrayList<Tile>();		
		for (int x = 1; x <= 9; x++) {
			for (int y = 1; y <= 5; y++) {
				// get distance between unit tile and other board tiles
				int distx = Math.abs(unitx - x);
				int disty = Math.abs(unity - y);
				
				// get diagonal tiles
				if (distx == 1 && disty == 1) {
					Tile diag = gameState.board.getTile(x, y); 
					if (diag != null)
					diagonalTiles.add(diag);
				}
			}
				
		}
			
		// Handle diagonal tiles
		for (Tile t: diagonalTiles) {
		// Only highlight tile if there is no unit on it
			if (!t.hasUnit()) {
				BasicCommands.drawTile(out, t, 1);
				try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
				targetTiles.add(t);
			}
		}
		
		
		// Handle horizontal tiles
		Tile leftClose = gameState.board.getTile(unitx - 1, unity);
		Tile leftFar = gameState.board.getTile(unitx - 2, unity);
		Tile rightClose = gameState.board.getTile(unitx + 1, unity);
		Tile rightFar = gameState.board.getTile(unitx + 2, unity);
		
		if (leftClose != null && !leftClose.hasUnit()) {
			BasicCommands.drawTile(out, leftClose, 1);
			targetTiles.add(leftClose);
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			
			// Only highlight far tile if there is no unit on close tile
			if (leftFar!= null && !leftFar.hasUnit()) {
				BasicCommands.drawTile(out, leftFar, 1);
				targetTiles.add(leftFar);
				try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
		
		if (rightClose != null && !rightClose.hasUnit()) {
			BasicCommands.drawTile(out, rightClose, 1);
			targetTiles.add(rightClose);
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			
			if (rightFar!= null && !rightFar.hasUnit()) {
				BasicCommands.drawTile(out, rightFar, 1);
				targetTiles.add(rightFar);
				try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
		
		// Handle vertical tiles
				Tile topClose = gameState.board.getTile(unitx, unity - 1);
				Tile topFar = gameState.board.getTile(unitx, unity - 2);
				Tile bottomClose = gameState.board.getTile(unitx, unity + 1);
				Tile bottomFar = gameState.board.getTile(unitx, unity + 2);
				
				if (topClose != null && !topClose.hasUnit()) {
					BasicCommands.drawTile(out, topClose, 1);
					targetTiles.add(topClose);
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					
					// Only highlight far tile if there is no unit on close tile
					if (topFar!= null && !topFar.hasUnit()) {
						BasicCommands.drawTile(out, topFar, 1);
						targetTiles.add(topFar);
						try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					}
				}
				
				if (bottomClose != null && !bottomClose.hasUnit()) {
					BasicCommands.drawTile(out, bottomClose, 1);
					targetTiles.add(bottomClose);
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					
					if (bottomFar!= null && !bottomFar.hasUnit()) {
						BasicCommands.drawTile(out, bottomFar, 1);
						targetTiles.add(bottomFar);
						try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					}
				}
				
				
			
	}
	// (Story card 7) highlight adjacent enemy units in RED
public void highlightAttackTargets(Tile unitTile, GameState gameState, ActorRef out) {

    if (unitTile == null || !unitTile.hasUnit()) return;

    Unit attacker = unitTile.getUnit();

    // only highlight for human player's units, and only if canAttack
    if (attacker.getPlayer() != gameState.player1) return;
    if (!attacker.getCanAttack()) return;
    // hello, this is a change: Remove the hard-coded 4 adjacent grids and instead scan all grids for enemies.
    int attackerX = unitTile.getTilex();
    int attackerY = unitTile.getTiley();

    // Scan the entire board for potential enemies
    for (int x = 0; x <= 9; x++) {
        for (int y = 0; y <= 5; y++) {
            Tile targetTile = gameState.board.getTile(x, y);
            
            // Skip if tile is empty or has our own unit
            if (targetTile == null || !targetTile.hasUnit()) continue;
            if (targetTile.getUnit().getPlayer() == gameState.player1) continue;

            // Calculate distance to the enemy
            int dx = Math.abs(targetTile.getTilex() - attackerX);
            int dy = Math.abs(targetTile.getTiley() - attackerY);
            boolean isAdjacent = (dx + dy == 1);

            // Scenario 1: Enemy is directly adjacent (Story Card 7 part 1)
            if (isAdjacent) {
                BasicCommands.drawTile(out, targetTile, 2); // 2 represents red highlight
                try { Thread.sleep(50); } catch (Exception e) {}
                targetTiles.add(targetTile);
            } 
            // Scenario 2: Enemy is far, but our unit hasn't moved yet (Story Card 7 part 2)
            else if (attacker.getCanMove()) {
                
                // Check the 4 tiles around the enemy to find a valid landing spot
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                boolean canReach = false;
                
                for (int[] dir : directions) {
                    int checkX = targetTile.getTilex() + dir[0];
                    int checkY = targetTile.getTiley() + dir[1];
                    Tile adjacentToTarget = gameState.board.getTile(checkX, checkY);

                    // If there is an empty tile next to the enemy within our movement range
                    if (adjacentToTarget != null && !adjacentToTarget.hasUnit()) {
                        int moveDx = Math.abs(adjacentToTarget.getTilex() - attackerX);
                        int moveDy = Math.abs(adjacentToTarget.getTiley() - attackerY);

                        if (moveDx <= 2 && moveDy <= 2) {
                            canReach = true; // Found a valid landing spot!
                            break;
                        }
                    }
                }

                // Highlight the far enemy in red if we can reach them
                if (canReach) {
                    BasicCommands.drawTile(out, targetTile, 2);
                    try { Thread.sleep(50); } catch (Exception e) {}
                    targetTiles.add(targetTile);
                }
            }
        }
    }
}
		// Reverts Board back to default state (Story card 8) [FIXED]
	public void clearHighlights(Tile tile, ActorRef out) {
		// Always clear whatever was highlighted before
			for (Tile t : targetTiles) {
				if (t != null) {
					BasicCommands.drawTile(out, t, 0);
				}
			}
			targetTiles.clear();
		}
			
	
	
	
	
		
		
		
		
					

		
		
		
		
	


}
