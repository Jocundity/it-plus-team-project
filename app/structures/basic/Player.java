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

	int health;
	int mana;
	String avatarConfigFile;
    private Deck deck;
    private HandManager handManager;
	
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
        this.deck = new Deck();
        this.handManager = new HandManager();
	}
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
        this.deck = new Deck();
        this.handManager = new HandManager();
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
	
	// Gets the configuration file for the avatar placed on board
	public String getAvatarConfigFile() {
		return StaticConfFiles.humanAvatar;
	}
	
	// Show Life total (health) on screen 
	public void showLife(ActorRef out) {
		BasicCommands.setPlayer1Health(out, this);
		try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
	}
	public Deck getDeck() {
		return deck;
	}
    public HandManager getHandManager() {
        return handManager;
    }
	
	
}
