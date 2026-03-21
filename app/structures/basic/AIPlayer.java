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

	// AI-exclusive global unit ID generator, with the starting value set to 5000 to avoid conflicts with human players
    private static int aiUnitIdCounter = 5000;
    
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

                if (targetTile != null) {
                	// Use absolutely unique counter ID for unit
                    int unitID = aiUnitIdCounter++; 
                    String confFile = utils.StaticConfFiles.getUnitConf(cardName);
                    
                    if (confFile != null) {
                        Unit newUnit = BasicObjectBuilders.loadUnit(confFile, unitID, Unit.class);
                        newUnit.setConfigFile(confFile);
                        newUnit.setPlayer(gameState.player2); 
                        newUnit.setPositionByTile(targetTile);
                        
                        // Set correct stats to backend object before tile assignment/rendering
                        newUnit.setAttack(card.getBigCard().getAttack());
                        newUnit.setHealth(card.getBigCard().getHealth());
                        newUnit.setMaxHealth(card.getBigCard().getHealth());
                        
                        targetTile.setUnit(newUnit);

                        BasicCommands.playEffectAnimation(out, BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon), targetTile);
                        try { Thread.sleep(500); } catch (Exception e) {}
                        
                        // Ensure correct stats are passed even if frontend throttles
                        BasicCommands.drawUnit(out, newUnit, targetTile);
                        try { Thread.sleep(100); } catch (Exception e) {} 

                        BasicCommands.setUnitAttack(out, newUnit, newUnit.getAttack());
                        try { Thread.sleep(100); } catch (Exception e) {} 

                        BasicCommands.setUnitHealth(out, newUnit, newUnit.getHealth());
                        
                        triggerAIOpeningGambit(out, gameState, newUnit, card);
                        
                     // Story Card #23: Rush
                     // Saberspine Tiger can move and attack immediately
                     if (cardName.equals("Saberspine Tiger")) {
                         newUnit.setCanMove(true);
                         newUnit.setCanAttack(true);
                     } else {
                         // Default: Summoning Sickness
                         newUnit.setCanMove(false);
                         newUnit.setCanAttack(false);
                     };
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
                	//Use an absolutely unique counter ID
                	int uniqueId = aiUnitIdCounter++;

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
    
    // Story Card: AI Board Actions (Movement & Attacking)
    public void processAIBoardActions(GameState gameState, ActorRef out) {
        Unit humanAvatar = findHumanAvatar(gameState);
        if (humanAvatar == null) return;
        Tile targetTile = getTileByUnit(gameState, humanAvatar);

        // Scan the board safely from index 0
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 6; y++) {
                try {
                    Tile tile = gameState.board.getTile(x, y);
                    if (tile != null && tile.hasUnit() && tile.getUnit().getPlayer() == gameState.player2) {
                        Unit aiUnit = tile.getUnit();
                        
                        // 1. Try attacking an adjacent enemy first
                        if (aiUnit.getCanAttack()) {
                            boolean attacked = tryAttackAdjacentEnemy(out, gameState, tile, aiUnit);
                            if (attacked) continue;
                        }

                        // 2. If no adjacent enemy, move towards the target and then attack
                        if (aiUnit.getCanMove()) {
                            Tile bestMoveTile = findBestMoveTowardsTarget(gameState, tile, targetTile);
                            
                            if (bestMoveTile != null && bestMoveTile != tile) {
                                BasicCommands.moveUnitToTile(out, aiUnit, bestMoveTile);
                                bestMoveTile.setUnit(aiUnit);
                                tile.setUnit(null);
                                aiUnit.setPositionByTile(bestMoveTile);
                                aiUnit.setCanMove(false); 
                                
                                try { Thread.sleep(2500); } catch (Exception e) {} 
                                
                                if (aiUnit.getCanAttack()) {
                                    tryAttackAdjacentEnemy(out, gameState, bestMoveTile, aiUnit);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Safely ignore out-of-bounds tiles
                } 
            }
        }
    }

    private boolean tryAttackAdjacentEnemy(ActorRef out, GameState gameState, Tile attackerTile, Unit attacker) {
        int ax = attackerTile.getTilex();
        int ay = attackerTile.getTiley();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        
        for (int[] dir : directions) {
            Tile adjTile = gameState.board.getTile(ax + dir[0], ay + dir[1]);
            if (adjTile != null && adjTile.hasUnit()) {
                Unit target = adjTile.getUnit();
                
                if (target.getPlayer() != attacker.getPlayer()) {
                    BasicCommands.playUnitAnimation(out, attacker, structures.basic.UnitAnimationType.attack);
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    
                    target.decreaseHealth(gameState, out, attacker.getAttack());
                    attacker.setCanAttack(false);
                    attacker.setCanMove(false); 
                    
                    // Story Card #12: Counterattack
                    if (target.getHealth() > 0) {
                        BasicCommands.playUnitAnimation(out, target, structures.basic.UnitAnimationType.attack);
                        try { Thread.sleep(1000); } catch (Exception e) {}
                        attacker.decreaseHealth(gameState, out, target.getAttack());
                        BasicCommands.playUnitAnimation(out, target, structures.basic.UnitAnimationType.idle);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private Tile findBestMoveTowardsTarget(GameState gameState, Tile startTile, Tile targetTile) {
        Tile bestTile = startTile;
        double minDistance = calculateDistance(startTile, targetTile);

        // Standard movement range of 2 tiles
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int cx = startTile.getTilex() + dx;
                int cy = startTile.getTiley() + dy;
                
                try {
                    Tile candidate = gameState.board.getTile(cx, cy);
                    
                if (candidate != null && !candidate.hasUnit()) {
                    // Validate path with official movement rules
                	// Prevent front-end animation freeze on blocked paths
                    if (gameState.highlightManager.isValidMove(startTile.getTilex(), startTile.getTiley(), cx, cy, candidate, gameState)) {
                        double dist = calculateDistance(candidate, targetTile);
                        if (dist < minDistance) {
                            minDistance = dist;
                            bestTile = candidate;
                        }
                    }
                }
            } catch (Exception e) {
                // Safely ignore out-of-bounds tiles
            }
        }
    }
    return bestTile;
}

    private double calculateDistance(Tile t1, Tile t2) {
        return Math.sqrt(Math.pow(t1.getTilex() - t2.getTilex(), 2) + Math.pow(t1.getTiley() - t2.getTiley(), 2));
    }

    private Unit findHumanAvatar(GameState gameState) {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 6; y++) {
                try {
                    Tile tile = gameState.board.getTile(x, y);
                    if (tile != null && tile.hasUnit()) {
                        Unit unit = tile.getUnit();
                        if (unit instanceof Avatar && unit.getPlayer() == gameState.player1) {
                            return unit;
                        }
                    }
                } catch (Exception e) {
                    // Safely ignore out-of-bounds tiles
                } 
            }
        }
        return null;
    }
    
    private Tile getTileByUnit(GameState gameState, Unit unit) {
        return gameState.board.getTile(unit.getPosition().getTilex(), unit.getPosition().getTiley());
    }
    
}
