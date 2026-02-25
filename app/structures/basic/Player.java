package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

// hello this is a change
import java.util.ArrayList;
import java.util.List;

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
	
	// hello this is a change
    public List<Card> hand;
	public List<Card> deck;
		
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
		this.turnNumber = 1;
		
		// hello this is a change
				this.hand = new ArrayList<>();
				this.deck = new ArrayList<>();
	}
	
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
		
		// hello this is a change
		this.hand = new ArrayList<>();
		this.deck = new ArrayList<>();
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
	
	// hello this is a change
		/**
		 * Draws a card from the top of the deck. 
		 * If the hand already has 6 cards, the drawn card is deleted (lost).
		 */
	    public void drawCard() {
	    	// Ensure the deck is not empty before drawing
	    	if (deck != null && !deck.isEmpty()) {
	    		// Remove the first card from the deck (simulating drawing the top card)
			    Card drawnCard = deck.remove(0); 
			
			    // Check if hand is full
			    if (hand.size() < 6) {
			    	hand.add(drawnCard); // Normal draw: add to hand
			    	} 
			        // If hand.size() >= 6, we do NOT add the card to the hand.
			        // Since we already removed it from the deck, the card is now lost/deleted.
		}
	}
}
