package structures.basic;

import java.util.ArrayList;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import utils.StaticConfFiles;

public class Truestrike {
	
	/* This class handles the logic for when the AI character plays
	the Truestrike card. */
	
	public void play(GameState gameState, ActorRef out, HighlightManager highlightManager, Player player) {
		ArrayList<Tile> enemyTiles = TargetingSystem.getEnemyUnitTiles(gameState, player);
		Tile targetTile = null;
		Unit targetUnit = null;
		boolean killable = false;
		
		for (Tile tile : enemyTiles) {
			Unit unit = tile.getUnit();
			
			// Bug fix to prevent targeting of dead units on subsequent turns
			if (unit == null || unit.getHealth() <= 0) {
				continue;
			}
			
			// Kill another unit if possible
			if (unit.getHealth() <= 2) {
				targetTile = tile;
				targetUnit = unit;
				killable = true;
				break;
			}
			
			// If not possible to kill, choose to weaken strong enemies
			else if (unit.getAttack() >= 3 && !killable) {
				targetTile = tile;
				targetUnit = unit;
			}
			
			// If no strong enemies, choose human player's avatar as target
			else if (unit instanceof Avatar) {
				targetTile = tile;
				targetUnit = unit;
			}
		}
		
		if (targetUnit != null && targetTile != null) {
			// Highlight target
			highlightManager.highlightSingleTileRed(targetTile, out);
			try { Thread.sleep(100); } catch (Exception e) {}
			
			// Play animation
			EffectAnimation animation = utils.BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
			BasicCommands.playEffectAnimation(out, animation, targetTile);
			
			// Clear Highlights
			highlightManager.clearHighlights(out);
			try { Thread.sleep(100); } catch (Exception e) {}
			
			// Decrease health
			targetUnit.decreaseHealth(gameState, out, 2);
			
			// Wait to make sure all actions are completed
			// before carrying on with rest of turn
			try { Thread.sleep(3000); } catch (Exception e) {}
		}
		
	}

}
