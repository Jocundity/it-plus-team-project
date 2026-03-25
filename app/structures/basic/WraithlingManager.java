package structures.basic;

import structures.basic.Unit;
import structures.basic.Player;
import structures.basic.Tile;
import structures.GameState;
import akka.actor.ActorRef;
import commands.BasicCommands;

public class WraithlingManager {
	static int uniqueId = 8000;
	
	public WraithlingManager() {
	}
	
	// Create a wraithling unit
	public static Unit createWraithling(Player player) {
		Unit wraithling = utils.BasicObjectBuilders.loadUnit(
                utils.StaticConfFiles.wraithling,
                uniqueId,
                Unit.class
        );

        wraithling.setConfigFile(utils.StaticConfFiles.wraithling);
        wraithling.setPlayer(player);

        wraithling.setAttack(1);
        wraithling.setHealth(1);
        wraithling.setMaxHealth(1);
        wraithling.setCanMove(false);
        wraithling.setCanAttack(false);
        
        uniqueId++;
        return wraithling;
	}
	
	// Create wraithling and place on board
	public static void placeWraithling(GameState gameState, ActorRef out, Tile tile, Player player) {
		Unit wraithling = createWraithling(player);
		wraithling.setPositionByTile(tile);
		tile.setUnit(wraithling);
		
		// Display visuals
		BasicCommands.playEffectAnimation(
	            out,
	            utils.BasicObjectBuilders.loadEffect(utils.StaticConfFiles.f1_summon),
	            tile
	    );
		
		try { Thread.sleep(150); } catch (Exception e) {}

	    BasicCommands.drawUnit(out, wraithling, tile);
	    try { Thread.sleep(150); } catch (Exception e) {}

	    BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttack());
	    try { Thread.sleep(150); } catch (Exception e) {}

	    BasicCommands.setUnitHealth(out, wraithling, wraithling.getHealth());
	    try { Thread.sleep(150); } catch (Exception e) {}
	    
	    BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);
	    try { Thread.sleep(150); } catch (Exception e) {}
		
		
	}
	
}