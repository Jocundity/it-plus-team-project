package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import java.util.ArrayList;
import java.util.Collections;


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
			for (int y = 1; y <= 9; y++) {
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
	
	
	// Reverts Board back to default state (Story card 8)
	public void clearHighlights(Tile tile, ActorRef out) {
		if (targetTiles.contains(tile)) {
			for (Tile t : targetTiles) {
				if (t != null) {
					BasicCommands.drawTile(out, t, 0);
				}
			}
			targetTiles.clear();
		}
			
	}
	
	
	
		
		
		
		
					

		
		
		
		
	


}
