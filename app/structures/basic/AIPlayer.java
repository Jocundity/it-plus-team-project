package structures.basic;

import structures.GameState;
import structures.basic.Card;

import java.util.ArrayList;
import java.util.Collections;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

public class AIPlayer extends Player {
	String avatarConfigFile;

	public AIPlayer() {
		super();// TODO Auto-generated constructor stub
	}

	public AIPlayer(int health, int mana) {
		super(health, mana);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getAvatarConfigFile() {
		return StaticConfFiles.aiAvatar;
	}

	// Show Life total (health) on screen 
	@Override
	public void showLife(ActorRef out) {
		BasicCommands.setPlayer2Health(out, this);
		try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	}
	@Override
	public void showMana(ActorRef out) {
		BasicCommands.setPlayer2Mana(out, this);
		try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	}

	    // AI draw logic (no UI rendering)
    @Override
    public void drawCard(ActorRef out) {
        Card drawn = getDeck().drawTopCard();
        if (drawn == null) return;

        // Add to AI hand only (do not render to avoid drawing in player1 slots)
        getHandManager().addCardToHand(drawn);
    }
    
    // Choose random card from hand
    public Card chooseCard(GameState gameState) {
    	ArrayList<Card> hand = (ArrayList<Card>) getHand();
    	
    	if (hand.isEmpty()) {
    		return null;
    	}
    	
    	// Filter cards by mana cost to find playable cards
    	ArrayList<Card> playable = new ArrayList<Card>();
    	for (Card card : hand) {
    		if (getMana() >= card.manacost) {
    			
    			// Do not add Sundrop Elixir if all units at max health
    			if (card.getCardname().equals("Sundrop Elixir")) {
    				if (anyDamaged(gameState)) {
    					playable.add(card);
    				}
    			} else {
    				playable.add(card);
    			}
    			
    		}
    	}
    	
    	if (playable.isEmpty()) {
    		return null;
    	}
    	
    	int choice = (int) (Math.random() * playable.size());
    	return playable.get(choice);
    }
    
    // Play card
    public void playCard(Card card, GameState gameState, ActorRef out, HighlightManager highlightManager) {
    	String cardName = card.getCardname();
    	
    	// Story Card #26: Spell Ability (Direct Damage)
    	// Play True Strike (Deal 2 damage to an enemy unit)
    	if (cardName.equals("Truestrike")) {
    		Truestrike truestrike = new Truestrike();
    		truestrike.play(gameState, out, highlightManager, this);
    	}
    	
    	// Story Card #27: Spell Ability (Heal)
    	// Play Sundrop Elixer (increase allied unit health by 4)
    	if (cardName.equals("Sundrop Elixir")) {
    		SundropElixir sundropElixir = new SundropElixir();
    		sundropElixir.play(gameState, out, highlightManager, this);
    	}
    	
    	// Reduce mana upon successful play
    	this.setMana(getMana() - card.getManacost());
    	this.showMana(out);
    	
    	// Clear highlighted tiles upon successful play
    	highlightManager.clearHighlights(out);
    	
    	
    	
    	// Remove card from hand upon successful play
    	ArrayList<Card> hand = (ArrayList<Card>) getHand();
    	hand.remove(card);
    
    	
    	
    } // end of playCard method
    
    // helper method to check for damaged units before choosing Sundroop Elixir
    private boolean anyDamaged(GameState gameState) {
    	ArrayList<Tile> friendlyTiles = TargetingSystem.getFriendlyUnitTiles(gameState, this);
    	
    	// Loop through tiles with units belonging to player
    	// and check if health is less than max health
    	for (Tile tile : friendlyTiles) {
    		Unit unit = tile.getUnit();
    		if (unit.getHealth() < unit.getMaxHealth()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    
    
}
