package structures.basic;

import structures.GameState;
import structures.basic.Card;

import java.util.ArrayList;
import java.util.Collections;

import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

import utils.BasicObjectBuilders;
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
    	else if (cardName.equals("Sundrop Elixir")) {
    		SundropElixir sundropElixir = new SundropElixir();
    		sundropElixir.play(gameState, out, highlightManager, this);
    	}
    	else if (cardName.equals("Wraithling Swarm")) {
            playAIWraithlingSwarm(gameState, out);
        }
    	// Added Unit Summoning logic for AI (Story Card 17)
        else {
            Tile targetTile = null;
            
                // Smart placement logic specifically for Silverguard Squire 
                if (cardName.equals("Silverguard Squire")) {
                    Unit avatar = findAIAvatar(gameState);
                    if (avatar != null) {
                        int ax = avatar.getPosition().getTilex();
                        int ay = avatar.getPosition().getTiley();
                        
                        Tile frontTile = gameState.board.getTile(ax - 1, ay); // Left of Player 2 Avatar
                        Tile backTile = gameState.board.getTile(ax + 1, ay);  // Right of Player 2 Avatar
                        
                        // Priority 1: Place in front
                        if (frontTile != null && !frontTile.hasUnit()) {
                            targetTile = frontTile;
                        } 
                        // Priority 2: Place behind
                        else if (backTile != null && !backTile.hasUnit()) {
                            targetTile = backTile;
                        } 
                        }
                    } 
                
                // Default AI placement logic
                if (targetTile == null) {
                    outerloop:
                    for (int x = 1; x <= 9; x++) {
                        for (int y = 1; y <= 5; y++) {
                            Tile t = gameState.board.getTile(x, y);
                            if (t != null && !t.hasUnit()) {
                                int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
                                for (int[] dir : directions) {
                                    Tile adj = gameState.board.getTile(x + dir[0], y + dir[1]);
                                    if (adj != null && adj.hasUnit() && adj.getUnit().getPlayer() == gameState.player2) {
                                        targetTile = t;
                                        break outerloop;
                                    }
                                }
                            }
                        }
                    }
                }

                // Ultimate fallback logic to prevent skipped summons if no adjacent tiles are available
                if (targetTile == null) {
                    for (int x = 1; x <= 9; x++) {
                        for (int y = 1; y <= 5; y++) {
                            Tile t = gameState.board.getTile(x, y);
                            if (t != null && !t.hasUnit()) {
                                targetTile = t;
                                break;
                            }
                        }
                        if (targetTile != null) break;
                    }
                }

                // Execute summon
                if (targetTile != null) {
                    int unitID = Math.abs(
                            (cardName.hashCode() * 31)
                            + (targetTile.getTilex() * 100)
                            + targetTile.getTiley()
                    );
                    String confFile = utils.StaticConfFiles.getUnitConf(cardName);
                    
                    if (confFile != null) {
                        Unit newUnit = BasicObjectBuilders.loadUnit(confFile, unitID, Unit.class);
                        
                        newUnit.setConfigFile(confFile);
                        newUnit.setPlayer(gameState.player2); 
                        newUnit.setPositionByTile(targetTile);
                        targetTile.setUnit(newUnit);

                        BasicCommands.playEffectAnimation(out, BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon), targetTile);
                        try { Thread.sleep(500); } catch (InterruptedException e) {}
                        
                        BasicCommands.drawUnit(out, newUnit, targetTile);
                        try { Thread.sleep(100); } catch (InterruptedException e) {} 

                        newUnit.setAttack(card.getBigCard().getAttack());
                        newUnit.setHealth(card.getBigCard().getHealth());
                        newUnit.setMaxHealth(card.getBigCard().getHealth());

                        BasicCommands.setUnitAttack(out, newUnit, newUnit.getAttack());
                        try { Thread.sleep(100); } catch (InterruptedException e) {} 

                        BasicCommands.setUnitHealth(out, newUnit, newUnit.getHealth());
                        
                        // Trigger AI Opening Gambit
                        triggerAIOpeningGambit(out, gameState, newUnit, card);
                        
                        newUnit.setCanMove(false);
                        newUnit.setCanAttack(false);
                    }
            }
        }
        
    	
    	// Reduce mana upon successful play
    	this.setMana(getMana() - card.getManacost());
    	this.showMana(out);
    	
    	// Clear highlighted tiles upon successful play
    	highlightManager.clearHighlights(out);
    	
    	
    	
    	// Remove card from hand upon successful play
    	ArrayList<Card> hand = (ArrayList<Card>) getHand();
    	hand.remove(card);
        }
    	 // end of playCard method
    
    private void playAIWraithlingSwarm(GameState gameState, ActorRef out) {
        int summonedCount = 0;

        for (int x = 1; x <= 9 && summonedCount < 3; x++) {
            for (int y = 1; y <= 5 && summonedCount < 3; y++) {
                Tile t = gameState.board.getTile(x, y);
                if (t != null && !t.hasUnit()) {
                    int uniqueId = 7000 + (x * 100) + y + summonedCount;

                    Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, uniqueId, Unit.class);
                    wraithling.setConfigFile(StaticConfFiles.wraithling);
                    wraithling.setPlayer(gameState.player2);

                    wraithling.setAttack(1);
                    wraithling.setHealth(1);
                    wraithling.setMaxHealth(1);

                    wraithling.setPositionByTile(t);
                    t.setUnit(wraithling);

                    BasicCommands.playEffectAnimation(out, BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon), t);
                    try { Thread.sleep(150); } catch (Exception e) {}

                    BasicCommands.drawUnit(out, wraithling, t);
                    try { Thread.sleep(150); } catch (Exception e) {}

                    BasicCommands.setUnitAttack(out, wraithling, 1);
                    try { Thread.sleep(100); } catch (Exception e) {}

                    BasicCommands.setUnitHealth(out, wraithling, 1);
                    try { Thread.sleep(100); } catch (Exception e) {}

                    wraithling.setCanMove(false);
                    wraithling.setCanAttack(false);

                    summonedCount++;
                }
            }
        }
    }
    // Implement the triggerAIOpeningGambit method for AIPlayer (Story 17) 
    private void triggerAIOpeningGambit(ActorRef out, GameState gameState, Unit summonedUnit, Card card) {
        if (card.getCardname().equals("Silverguard Squire")) {
            
            // 1. Locate the AI's Avatar
            Unit avatar = findAIAvatar(gameState);
            if (avatar == null) return;

            int avatarX = avatar.getPosition().getTilex();
            int avatarY = avatar.getPosition().getTiley();

            // 2. Define the guard coordinates: directly left (x-1) and right (x+1) of the Avatar
            int[][] guardPositions = {
                {avatarX - 1, avatarY}, 
                {avatarX + 1, avatarY}
            };

            for (int[] pos : guardPositions) {
                Tile targetTile = gameState.board.getTile(pos[0], pos[1]);

                if (targetTile != null && targetTile.hasUnit()) {
                    Unit targetUnit = targetTile.getUnit();
                    
                    // 3. If there is an AI-owned unit at this position (including the newly summoned Squire itself), apply the buff
                    if (targetUnit.getPlayer() == gameState.player2) {
                        
                        // Play the buff visual effect
                        BasicCommands.playEffectAnimation(out, BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff), targetTile);
                        try { Thread.sleep(200); } catch (InterruptedException e) {}
                        
                        // Apply the permanent buff (+1/+1) to the allied unit
                        targetUnit.applyPermanentBuff(out, 1, 1);
                    }
                }
            }
        }
    }

    // Dedicated helper method for the AI to find its Avatar (ensures full 9x5 board coverage)
    private Unit findAIAvatar(GameState gameState) {
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                Tile tile = gameState.board.getTile(x, y);
                if (tile != null && tile.hasUnit()) {
                    Unit unit = tile.getUnit();
                    // Check if the unit is an Avatar and belongs to this AI player
                    if (unit instanceof Avatar && unit.getPlayer() == gameState.player2) {
                        return unit;
                    }
                }
            }
        }
        return null;
    }
    
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
