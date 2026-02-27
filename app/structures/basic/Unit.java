package structures.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;
import akka.actor.ActorRef;

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
	int health;
	int attack;
	String configFile;
	Tile tile;
	boolean canMove = true;
	boolean canAttack = true;
	Player player;
	
	public Unit() {}
	
	public Unit(int id, String configFile, int health, int attack, Player player) {
		this.id = id;
		this.health = health;
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
	
	/**
	 * This command sets the position of the Unit to a specified
	 * tile.
	 * @param tile
	 */
	@JsonIgnore
	public void setPositionByTile(Tile tile) {
		position = new Position(tile.getXpos(),tile.getYpos(),tile.getTilex(),tile.getTiley());
	}
	
	/* Draws a unit on a tile
	and displays initial health and attack stats */
	public void drawUnit(ActorRef out, Tile tile) {	
		Unit sprite = BasicObjectBuilders.loadUnit(configFile, id, Unit.class);
		this.tile = tile;
		tile.setUnit(sprite);
		sprite.setPositionByTile(tile); 
		BasicCommands.drawUnit(out, sprite, tile);
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		
		BasicCommands.setUnitHealth(out, sprite, this.health);
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		
		BasicCommands.setUnitAttack(out, sprite, this.attack);
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
	
}
