package structures.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;
import akka.actor.ActorRef;
import structures.GameState;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile. 
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Unit {

	@JsonIgnore
	protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file
	
	int id;
	UnitAnimationType animation;
	Position position;
	UnitAnimationSet animations;
	ImageCorrection correction;
	int maxHealth;
	int health;
	int attack;
	String configFile;
	Tile tile;
	boolean canMove = true;
	boolean canAttack = true;
	Player player;
	boolean isStunned = false;
	
	public Unit() {}
	
	public Unit(int id, String configFile, int health, int attack, Player player) {
		this.id = id;
		this.health = health;
		this.maxHealth = health;
		this.attack = attack;
		this.configFile = configFile;
		this.player = player;
		
	}
	
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;
		
		position = new Position(0,0,0,0);
		this.correction = correction;
		this.animations = animations;
	}
	
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;
		
		position = new Position(currentTile.getXpos(),currentTile.getYpos(),currentTile.getTilex(),currentTile.getTiley());
		this.correction = correction;
		this.animations = animations;
	}
	
	
	
	public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
			ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = animation;
		this.position = position;
		this.animations = animations;
		this.correction = correction;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public UnitAnimationType getAnimation() {
		return animation;
	}
	public void setAnimation(UnitAnimationType animation) {
		this.animation = animation;
	}

	public ImageCorrection getCorrection() {
		return correction;
	}

	public void setCorrection(ImageCorrection correction) {
		this.correction = correction;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public UnitAnimationSet getAnimations() {
		return animations;
	}

	public void setAnimations(UnitAnimationSet animations) {
		this.animations = animations;
	}
	
	public int getHealth() {
    	return health;
	}

	public void setHealth(int health) {
    	this.health = health;
	}
	
	// Visualise health on screen
	public void showHealth(ActorRef out) {
    	this.health = this.getHealth();
    	BasicCommands.setUnitHealth(out, this, health);
	}
	
	// Method to increase health without going above max health
	public void increaseHealth(ActorRef out, int amount) {
		if (health + amount > maxHealth) {
			this.health = maxHealth;
			
		} else {
			health = health + amount;
		}
		this.showHealth(out);
		
	}
	
	// Add permanent buff method for cards like Silverguard Squire (Story 17)
	public void applyPermanentBuff(ActorRef out, int atkAmount, int hpAmount) {
		// Increase Attack
		this.attack += atkAmount;
		BasicCommands.setUnitAttack(out, this, this.attack);
		
		// Increase Max Health and current Health
		this.maxHealth += hpAmount;
		this.health += hpAmount;
		BasicCommands.setUnitHealth(out, this, this.health);
		
		// If this unit is an Avatar, we must sync the Player object's health as well
		if (this instanceof Avatar) {
			this.player.setHealth(this.health);
			this.player.showLife(out);
		}
	}
	
	// Method to decrease health without going below zero
		public void decreaseHealth(GameState gameState, ActorRef out, int amount) {
			if (health - amount <= 0) {
				this.health = 0;
				this.showHealth(out);
				
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
				
			} else {
				health = health - amount;
				this.showHealth(out);
				
				// Play hit animation followed by idle animation
				BasicCommands.playUnitAnimation(out, this, UnitAnimationType.hit);
				try { Thread.sleep(2000); } catch (Exception e) {}
				BasicCommands.playUnitAnimation(out, this, UnitAnimationType.idle);
			}
			
		}
	
	// Getters and setters for max health
	public int getMaxHealth() {
    	return maxHealth;
	}

	public void setMaxHealth(int maxHealth) { 
    	this.maxHealth = maxHealth;
	}
	
	public int getAttack() {
    	return attack;
	}

	public void setAttack(int attack) {
    	this.attack = attack;
	}
	/**
	 * This command sets the position of the Unit to a specified
	 * tile.
	 * @param tile
	 */
	@JsonIgnore
	public void setPositionByTile(Tile tile) {
		if (tile == null) {
			return;
		}
		this.position = new Position(tile.getXpos(),tile.getYpos(),tile.getTilex(),tile.getTiley());
		this.tile = tile;
	}
	
	/* Draws a unit on a tile
	and displays initial health and attack stats */
	public void drawUnit(ActorRef out, Tile tile) {
		// Get configuration files
		Unit template = BasicObjectBuilders.loadUnit(configFile, id, Unit.class);
		this.animations = template.getAnimations();
		this.correction = template.getCorrection();
		
		// Assign unit to tile
		this.tile = tile;
		tile.setUnit(this);
		this.setPositionByTile(tile);
		
		// Draw unit 
		BasicCommands.drawUnit(out, this, tile);
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		
		BasicCommands.setUnitHealth(out, this, this.health);
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		
		BasicCommands.setUnitAttack(out, this, this.attack);
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
	}
	
	// Getters and setters for player 
	public Player getPlayer() {
		return player;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	// Getters and setters for canMove 
		public boolean getCanMove() {
			return canMove;
		}
		
		public void setCanMove(boolean val) {
			this.canMove = val;
		}
		
		// Getters and setters for canAttack 
				public boolean getCanAttack() {
					return canAttack;
				}
				
				public void setCanAttack(boolean val) {
					this.canAttack = val;
				}
	// Getters and setters for isStunned
	public boolean getIsStunned() {
        return isStunned;
    }
	public void setIsStunned(boolean val) {
        this.isStunned = val;
    }
}
