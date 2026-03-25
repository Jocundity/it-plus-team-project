package structures.basic;

import java.util.ArrayList;

import commands.BasicCommands;
import structures.GameState;

public class TargetingSystem {
	
	// get all tiles with units that are the enemy of player
	public static ArrayList<Tile> getEnemyUnitTiles(GameState gameState, Player player) {
		ArrayList<Tile> enemyTiles = new ArrayList<Tile>(); 
		
		// Loop through board to find tiles with enemy units
		for (int x = 0; x < 10; x++) {
		    for (int y = 0; y < 6; y++) {
				Tile targetTile = gameState.board.getTile(x, y);
				if (targetTile == null) continue;
				
				Unit unit = targetTile.getUnit();
				if (unit == null) continue;
				if (unit.getPlayer() == player) continue;
				
				enemyTiles.add(targetTile);
			}
		}
		return enemyTiles;    
	}
	
	// get all tiles with units that belong to the player
		public static ArrayList<Tile> getFriendlyUnitTiles(GameState gameState, Player player) {
			ArrayList<Tile> friendlyTiles = new ArrayList<Tile>(); 
			
			// Loop through board to find tiles with enemy units
			for (int x = 0; x < 10; x++) {
			    for (int y = 0; y < 6; y++) {
					Tile targetTile = gameState.board.getTile(x, y);
					if (targetTile == null) continue;
					
					Unit unit = targetTile.getUnit();
					if (unit == null) continue;
					if (unit.getPlayer() != player) continue;
					
					friendlyTiles.add(targetTile);
				}
			}
			return friendlyTiles;    
		}
}
