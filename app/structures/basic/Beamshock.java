package structures.basic;

import java.util.ArrayList;
import structures.basic.Avatar;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.HighlightManager;
import commands.BasicCommands;
import structures.GameState;
import akka.actor.ActorRef;
import utils.StaticConfFiles;

public class Beamshock {
	
	/* This class handles the logic for when the AI character plays
	 * the Beamshock card.
	 */
	
	public void play(GameState gameState, ActorRef out, HighlightManager highlightManager, Player player) {
		// Get enemy units
		ArrayList<Tile> enemyTiles = TargetingSystem.getEnemyUnitTiles(gameState, gameState.player1);
		Tile targetTile = null;
		int maxAttack = -1;
		
		// Stun the unit with the highest attack
		for (Tile tile: enemyTiles) {
			Unit targetUnit = tile.getUnit();
			if (targetUnit instanceof Avatar) {
				continue; // Do not target enemy avatar
			}
				
				if (targetUnit.getAttack() > maxAttack) {
					maxAttack = targetUnit.getAttack();
					targetTile = tile;
				}
				
		}
		
		if (targetTile != null) {
			Unit targetUnit = targetTile.getUnit();
			
			// Highlight target
			highlightManager.highlightSingleTileRed(targetTile, out);
			try { Thread.sleep(100); } catch (Exception e) {}
						
			// Play animation
			EffectAnimation animation = utils.BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
			BasicCommands.playEffectAnimation(out, animation, targetTile);
						
			// Clear Highlights
			highlightManager.clearHighlights(out);
			try { Thread.sleep(100); } catch (Exception e) {}
			
			targetUnit.setIsStunned(true);
			// Notify the player that the unit has been stunned
	        BasicCommands.addPlayer1Notification(out, "Unit Stunned!", 2);
	        
	     // Wait to make sure all actions are completed
	     // before carrying on with rest of turn
	     try { Thread.sleep(3000); } catch (Exception e) {}
		}
    	
	}        
}