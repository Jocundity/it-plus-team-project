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
    	
    	// Added Unit Summoning logic for AI (Story Card 17)
        else {
            Tile targetTile = null;

            System.out.println("====== DEBUG: AI 正在尝试召唤 " + cardName + " ======");

            try {
                // Smart placement logic specifically for Silverguard Squire 
                if (cardName.equals("Silverguard Squire")) {
                    Unit avatar = findAIAvatar(gameState);
                    if (avatar != null) {
                        int ax = avatar.getPosition().getTilex();
                        int ay = avatar.getPosition().getTiley();
                        System.out.println("DEBUG: AI 找到了主帅，坐标为: (" + ax + ", " + ay + ")");
                        
                        Tile frontTile = gameState.board.getTile(ax - 1, ay); // Left of Player 2 Avatar
                        Tile backTile = gameState.board.getTile(ax + 1, ay);  // Right of Player 2 Avatar
                        
                        // Priority 1: Place in front
                        if (frontTile != null && !frontTile.hasUnit()) {
                            targetTile = frontTile;
                            System.out.println("DEBUG: 决定放在主帅前方 (" + (ax-1) + ", " + ay + ")");
                        } 
                        // Priority 2: Place behind
                        else if (backTile != null && !backTile.hasUnit()) {
                            targetTile = backTile;
                            System.out.println("DEBUG: 决定放在主帅后方 (" + (ax+1) + ", " + ay + ")");
                        } else {
                            System.out.println("DEBUG: 主帅前后都被占满了！转入常规找位逻辑。");
                        }
                    } else {
                        System.out.println("DEBUG: 🚨 警告：AI 找不到自己的主帅！");
                    }
                }
                
                // --- 常规 AI 找位逻辑 (兜底策略) ---
                if (targetTile == null) {
                    System.out.println("DEBUG: 正在执行常规找位逻辑...");
                    outerloop:
                    for (int x = 1; x <= 9; x++) {
                        for (int y = 1; y <= 5; y++) {
                            Tile t = gameState.board.getTile(x, y);
                            if (t != null && !t.hasUnit()) {
                                int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
                                for (int[] dir : directions) {
                                    Tile adj = gameState.board.getTile(x + dir[0], y + dir[1]);
                                    // 💡 替换为 gameState.player2 更安全，避免对象引用地址不一致导致判定失败
                                    if (adj != null && adj.hasUnit() && adj.getUnit().getPlayer() == gameState.player2) {
                                        targetTile = t;
                                        System.out.println("DEBUG: 常规逻辑找到位置: (" + x + ", " + y + ")");
                                        break outerloop;
                                    }
                                }
                            }
                        }
                    }
                }

                // 💡 终极兜底：如果连常规逻辑都找不到位置（比如无路可走），强行找个空位防止吞牌
                if (targetTile == null) {
                    System.out.println("DEBUG: 🚨 严重警告：常规找位也失败！开启全图扫描空位...");
                    for (int x = 1; x <= 9; x++) {
                        for (int y = 1; y <= 5; y++) {
                            Tile t = gameState.board.getTile(x, y);
                            if (t != null && !t.hasUnit()) {
                                targetTile = t;
                                System.out.println("DEBUG: 终极兜底找到空位: (" + x + ", " + y + ")");
                                break;
                            }
                        }
                        if (targetTile != null) break;
                    }
                }

                // 执行召唤过程
                if (targetTile != null) {
                    int unitID = Math.abs(cardName.hashCode() + targetTile.getTilex() + targetTile.getTiley());
                    String confFile = utils.StaticConfFiles.getUnitConf(cardName);
                    
                    if (confFile == null) {
                        System.out.println("DEBUG: 🚨 错误：找不到卡牌的 confFile 配置文件！");
                    } else {
                        Unit newUnit = BasicObjectBuilders.loadUnit(confFile, unitID, Unit.class);
                        
                        newUnit.setPlayer(gameState.player2); // 💡 显式使用 gameState.player2
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

                        System.out.println("DEBUG: 渲染成功！准备触发战吼...");
                        
                        // Trigger AI Opening Gambit
                        triggerAIOpeningGambit(out, gameState, newUnit, card);
                        
                        newUnit.setCanMove(false);
                        newUnit.setCanAttack(false);
                        System.out.println("====== DEBUG: " + cardName + " 完整召唤流程结束 ======");
                    }
                } else {
                    System.out.println("DEBUG: 🚨 灾难性错误：全图都满了，彻底无法放置单位！");
                }
            } catch (Exception e) {
                System.out.println("DEBUG: 🚨 召唤过程中发生代码异常 (Exception)！");
                e.printStackTrace(); // 把具体报错打在控制台上
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
                        System.out.println("DEBUG: 已成功给 (" + pos[0] + ", " + pos[1] + ") 的单位加上 +1/+1 Buff！");
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
