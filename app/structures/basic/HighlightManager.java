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
	
	// Core movement validation rule
	public boolean isValidMove(Tile start, Tile target, GameState gs) {
		int dx = Math.abs(start.getTilex() - target.getTilex());
		int dy = Math.abs(start.getTiley() - target.getTiley());

		// The target tile must not have a unit, and cannot be the starting tile
		if (target.hasUnit() || (dx == 0 && dy == 0)) return false;

		// 1. Allow 1 tile adjacent movement (up, down, left, right)
		if (dx + dy == 1) return true;
		// 2. Allow 1 tile diagonal movement
		if (dx == 1 && dy == 1) return true;

		// 3. Allow 2 tiles straight movement, provided the middle tile is empty (no moving through units)
		if (dx == 2 && dy == 0) {
			int midX = (start.getTilex() + target.getTilex()) / 2;
			Tile midTile = gs.board.getTile(midX, start.getTiley());
			return midTile != null && !midTile.hasUnit();
		}
		if (dx == 0 && dy == 2) {
			int midY = (start.getTiley() + target.getTiley()) / 2;
			Tile midTile = gs.board.getTile(start.getTilex(), midY);
			return midTile != null && !midTile.hasUnit();
		}

		// All other cases (e.g., L-shapes, far diagonals) are invalid
		return false;
	}
	
	// (Story card 6) Highlight valid movement range
		public void highlightMovementRange(Tile tile, GameState gameState, ActorRef out) {
			for (int x = 0; x <= 9; x++) {
				for (int y = 0; y <= 5; y++) {
					Tile targetTile = gameState.board.getTile(x, y);
					if (targetTile != null && isValidMove(tile, targetTile, gameState)) {
						BasicCommands.drawTile(out, targetTile, 1); // 1 = white highlight
						try { Thread.sleep(80); } catch (Exception e) {} // Slightly reduce delay for smoother rendering
						targetTiles.add(targetTile);
					}
				}
			}
		}
		
		// (Story card 7) Accurately identify attackable enemies
		public void highlightAttackTargets(Tile unitTile, GameState gameState, ActorRef out) {
			if (unitTile == null || !unitTile.hasUnit()) return;
			Unit attacker = unitTile.getUnit();
			if (attacker.getPlayer() != gameState.player1 || !attacker.getCanAttack()) return;

			int ax = unitTile.getTilex();
			int ay = unitTile.getTiley();

			for (int x = 0; x <= 9; x++) {
				for (int y = 0; y <= 5; y++) {
					Tile targetTile = gameState.board.getTile(x, y);
					if (targetTile == null || !targetTile.hasUnit() || targetTile.getUnit().getPlayer() == gameState.player1) continue;

					int ex = targetTile.getTilex();
					int ey = targetTile.getTiley();

					// 1. If the enemy is adjacent (8 directions), highlight in red directly
					if (Math.max(Math.abs(ex - ax), Math.abs(ey - ay)) <= 1) {
						BasicCommands.drawTile(out, targetTile, 2);
						try { Thread.sleep(80); } catch (Exception e) {}
						targetTiles.add(targetTile);
						continue;
					}

					// 2. Predict move+attack combo: scan the 8 tiles around the enemy
					if (attacker.getCanMove()) {
						boolean canReach = false;
						int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
						for (int[] dir : directions) {
							Tile landingTile = gameState.board.getTile(ex + dir[0], ey + dir[1]);
							// Directly call our standard movement validation method!
							if (landingTile != null && isValidMove(unitTile, landingTile, gameState)) {
								canReach = true;
								break;
							}
						}
						if (canReach) {
							BasicCommands.drawTile(out, targetTile, 2);
							try { Thread.sleep(80); } catch (Exception e) {}
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
					try { Thread.sleep(30); } catch (Exception e) {}
				}
			}
			targetTiles.clear();
		}
	        // (Story card 32) highlight the summonable legal tiles
	        public void highlightSummonableTiles(GameState gameState, ActorRef out) {
	            // Clear all existing tile highlights on the game board
	            clearHighlights(null, out);
	    
	            // Scan entire board to find Player 1's units
	            for (int x = 0; x <= 9; x++) {
	                for (int y = 0; y <= 5; y++) {
	                    Tile t = gameState.board.getTile(x, y);
	            
	                    // Check if current tile contains a friendly (Player 1) unit
	                    if (t != null && t.hasUnit() && t.getUnit().getPlayer() == gameState.player1) {
	                
	                        // Check all 8 adjacent directions around the friendly unit
	                        int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
	                        for (int[] dir : directions) {
	                            int checkX = x + dir[0];
	                            int checkY = y + dir[1];
	                            Tile adjTile = gameState.board.getTile(checkX, checkY);
	                    
	                            // Highlight valid empty adjacent tiles (within board bounds, unoccupied, not already highlighted)
	                            if (adjTile != null && !adjTile.hasUnit() && !targetTiles.contains(adjTile)) {
	                                BasicCommands.drawTile(out, adjTile, 1); // 1 = white highlight
	                                targetTiles.add(adjTile);
	                                try { Thread.sleep(20); } catch (Exception e) {} // Short delay for smooth highlight animation
	                            }
	                        }
	                    }
	                }
	            }
        	}
	
	
	
	
		
		
		
		
					

		
		
		
		
	


}
