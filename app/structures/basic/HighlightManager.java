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

    int x = unitTile.getTilex();
    int y = unitTile.getTiley();

    // 4-adjacent tiles
    Tile[] adj = new Tile[] {
        gameState.board.getTile(x - 1, y),
        gameState.board.getTile(x + 1, y),
        gameState.board.getTile(x, y - 1),
        gameState.board.getTile(x, y + 1)
    };

    for (Tile t : adj) {
        if (t == null) continue;

        // only highlight if there is an enemy unit on that tile
        if (t.hasUnit() && t.getUnit().getPlayer() != gameState.player1) {
            BasicCommands.drawTile(out, t, 2); // try 2 as "red"
            try { Thread.sleep(50); } catch (Exception e) {}
            targetTiles.add(t);
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
