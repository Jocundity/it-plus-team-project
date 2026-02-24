package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

/**
 * A basic representation of of the Player. A player
 * has health and mana.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Player {

	private int health;
	private int mana;
	private int turnNumber;
	private String avatarConfigFile;
	
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
		this.turnNumber = 1;
	}
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
	}
	public int getHealth() {
		return health;
	}
	public void setHealth(int health) {
		this.health = health;
	}
	public int getMana() {
		return mana;
	}
	public void setMana(int mana) {
		this.mana = mana;
	}
	public int getTurnNumber() {
		return turnNumber;
	}
	
	// Gets the configuration file for the avatar placed on board
	public String getAvatarConfigFile() {
		return StaticConfFiles.humanAvatar;
	}
	
	// Show Life total (health) on screen 
	public void showLife(ActorRef out) {
		BasicCommands.setPlayer1Health(out, this);
		try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	}
	// Show Mana on screen (left side)
	public void showMana(ActorRef out) {
		BasicCommands.setPlayer1Mana(out, this);
		try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	}
	public void startTurn(ActorRef out) {

		mana = turnNumber + 1; // turn1 -> 2, turn2 -> 3...
		turnNumber++;
		showMana(out);
	}
	public void drainMana(ActorRef out) {
		mana = 0;
		showMana(out);
	}
}
