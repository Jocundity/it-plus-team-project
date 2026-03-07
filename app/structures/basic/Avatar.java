package structures.basic;

import commands.BasicCommands;
import akka.actor.ActorRef;
import structures.GameState;

public class Avatar extends Unit {
	Player player;

	public Avatar(Player player, int id) {
		super(id, player.getAvatarConfigFile(), player.getHealth(), 2, player);
		this.player = player;
		
	}

	public Avatar(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super(id, animations, correction);
		// TODO Auto-generated constructor stub
	}

	public Avatar(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super(id, animations, correction, currentTile);
		// TODO Auto-generated constructor stub
	}

	public Avatar(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
			ImageCorrection correction) {
		super(id, animation, position, animations, correction);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	// Method to decrease health without going below zero
	// Make sure life total is updated
			public void decreaseHealth(GameState gameState, ActorRef out, int amount) {
				if (health - amount <= 0) {
					this.health = 0;
					this.showHealth(out);
					this.player.setHealth(health);
					
					// Play hit and death animations and delete unit from board
					BasicCommands.playUnitAnimation(out, this, UnitAnimationType.hit);
					try { Thread.sleep(2000); } catch (Exception e) {}
					BasicCommands.playUnitAnimation(out, this, UnitAnimationType.death);
					try { Thread.sleep(2000); } catch (Exception e) {}
					
					Tile deathTile = this.tile;
					BasicCommands.deleteUnit(out, this);
					try { Thread.sleep(1000); } catch (Exception e) {}
					
					if (deathTile != null) {
						deathTile.setUnit(null);
						BasicCommands.drawTile(out, deathTile, 0);
						
					}
					
					// Display Game Over Message
					if (!(this.player instanceof AIPlayer)) {
	                    BasicCommands.addPlayer1Notification(out, "Game Over - You Lose!", 5);
	                } else {
	                    BasicCommands.addPlayer1Notification(out, "Victory - You Win!", 5);
	                }

	                gameState.gameOver = true;
					
				} else {
					health = health - amount;
					this.showHealth(out);
					this.player.setHealth(health);
					
					// Play hit animation followed by idle animation
					BasicCommands.playUnitAnimation(out, this, UnitAnimationType.hit);
					try { Thread.sleep(2000); } catch (Exception e) {}
					BasicCommands.playUnitAnimation(out, this, UnitAnimationType.idle);
				}
				
				// Update life totals
				if (!(this.player instanceof AIPlayer)) {
					BasicCommands.setPlayer1Health(out, this.player);
					try { Thread.sleep(2000); } catch (Exception e) {}
				} else {
					BasicCommands.setPlayer2Health(out, this.player);
					try { Thread.sleep(2000); } catch (Exception e) {}
				}
			}
	
	@Override
	// Method to increase health without going above max health
	// Make sure life tptal is updated
	public void increaseHealth(ActorRef out, int amount) {
		if (health + amount > maxHealth) {
			this.health = maxHealth;
			
		} else {
			health = health + amount;
		}
		this.showHealth(out);
		this.player.setHealth(health);
		
		// Update life totals
		if (!(this.player instanceof AIPlayer)) {
			BasicCommands.setPlayer1Health(out, this.player);
			try { Thread.sleep(2000); } catch (Exception e) {}
		} else {
			BasicCommands.setPlayer2Health(out, this.player);
			try { Thread.sleep(2000); } catch (Exception e) {}
		}
		
	}

}
