package structures.basic;

import java.util.ArrayList;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import utils.StaticConfFiles;

public class SundropElixir {
	/* This class handles the logic for when the AI character plays
	the Sundrop Elixir card. */

	public void play(GameState gameState, ActorRef out, HighlightManager highlightManager, Player player) {
		ArrayList<Tile> friendlyTiles = TargetingSystem.getFriendlyUnitTiles(gameState, player);
		Tile targetTile = null;
		Unit targetUnit = null;
		
		// Play card on avatar if AI player has lost 4 or more health
		if (player.getHealth() <= player.getMaxHealth() - 4) {
			for (Tile tile : friendlyTiles) {
				if (tile.getUnit() instanceof Avatar) {
					targetTile = tile;
					targetUnit = tile.getUnit();	
				}
			}
			
		// If AI player's health has lost less than 4 health, 
		//	choose most damaged unit
		} else {
			int highestDamage = 0;
			for (Tile tile: friendlyTiles) {
				Unit unit = tile.getUnit();
				int unitDamage = unit.getMaxHealth() - unit.getHealth();
				if (unitDamage > highestDamage) {
					targetTile = tile;
					targetUnit = unit;
				}
			}
		}
		
		// Play animation and increase health
		if (targetUnit != null) {
			// Highlight target
			highlightManager.highlightSingleTileRed(targetTile, out);
			try { Thread.sleep(100); } catch (Exception e) {}
			
			// Play animation
			EffectAnimation animation = utils.BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
			BasicCommands.playEffectAnimation(out, animation, targetTile);
			
			// Clear Highlights
			highlightManager.clearHighlights(out);
			try { Thread.sleep(100); } catch (Exception e) {}
			
			// Increase health
			targetUnit.increaseHealth(out, 4);
			
			// Wait to make sure all actions are completed
			// before carrying on with rest of turn
			try { Thread.sleep(3000); } catch (Exception e) {}
		}
		
	}
}
