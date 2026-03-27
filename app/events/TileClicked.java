package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.UnitAnimationType;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.Avatar;
import structures.basic.Player;
import structures.basic.Card;
import structures.basic.WraithlingManager;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class TileClicked implements EventProcessor {

	// Global unit ID generator exclusively for human players, starting value set to 2000
    private static int playerUnitIdCounter = 2000;
    
    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        // If it is not currently Player 1's turn, all clicks on the board will be directly ignored.
        if (!gameState.isPlayer1Turn) {
            return;
        }

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();
        if (gameState.gameOver) {
            return;
        }

        Tile clickedTile = gameState.board.getTile(tilex, tiley);
        if (clickedTile == null) {
            return;
        }

        // (Story Card 25) Unit Summoning Execution Phase
        if (gameState.isUnitSummoning && gameState.selectedCard != null && gameState.handPositionClicked != -1) {

            // Validate summon tile: must be empty and adjacent (8-direction) to a friendly unit
            boolean isValidSummonTile = false;
            if (!clickedTile.hasUnit()) {
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                for (int[] dir : directions) {
                    Tile adjTile = gameState.board.getTile(clickedTile.getTilex() + dir[0], clickedTile.getTiley() + dir[1]);
                    if (adjTile != null && adjTile.hasUnit() && adjTile.getUnit().getPlayer() == gameState.player1) {
                        isValidSummonTile = true;
                        break;
                    }
                }
            }

            if (isValidSummonTile) {

                gameState.highlightManager.clearHighlights(out);

                Card cardToSummon = gameState.selectedCard;

                // Deduct mana cost from player and refresh mana UI display
                gameState.player1.setMana(gameState.player1.getMana() - cardToSummon.getManacost());
                gameState.player1.showMana(out);

                // Play the summoning circle effect
                BasicCommands.playEffectAnimation(out, utils.BasicObjectBuilders.loadEffect(utils.StaticConfFiles.f1_summon), clickedTile);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }

                // Resolve unit config file by card name
                String confFile = utils.StaticConfFiles.getUnitConf(cardToSummon.getCardname());

                // Determine who the new unit belongs to based on whose turn it is currently.
                Player currentPlayer = gameState.isPlayer1Turn ? gameState.player1 : gameState.player2;

                // Use absolutely unique counter ID for unit
                int unitID = playerUnitIdCounter++; 
                Unit newUnit = utils.BasicObjectBuilders.loadUnit(confFile, unitID, Unit.class);
                newUnit.setConfigFile(confFile);
                newUnit.setPlayer(currentPlayer);
                newUnit.setPositionByTile(clickedTile);

                // Set actual stats before rendering unit model
                newUnit.setAttack(cardToSummon.getBigCard().getAttack());
                newUnit.setHealth(cardToSummon.getBigCard().getHealth());
                newUnit.setMaxHealth(cardToSummon.getBigCard().getHealth());
                
                clickedTile.setUnit(newUnit);
                
                BasicCommands.drawUnit(out, newUnit, clickedTile);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }

                // Sync unit stats (ATK/HP) with card values and update UI
                newUnit.setAttack(cardToSummon.getBigCard().getAttack());
                newUnit.setHealth(cardToSummon.getBigCard().getHealth());
                newUnit.setMaxHealth(cardToSummon.getBigCard().getHealth());
                BasicCommands.setUnitAttack(out, newUnit, newUnit.getAttack());
                BasicCommands.setUnitHealth(out, newUnit, newUnit.getHealth());

                // Trigger Opening Gambit for Story Card 17
                triggerOpeningGambit(out, gameState, newUnit, cardToSummon);

                // Newly summoned units are "exhausted" (can't move/attack) for the current turn
                // Story Card #23: Rush
                // Saberspine Tiger can move and attack immediately
                if (cardToSummon.getCardname().equals("Saberspine Tiger")) {
                    newUnit.setCanMove(true);
                    newUnit.setCanAttack(true);
                } else {
                    // Default: Summoning Sickness
                    newUnit.setCanMove(false);
                    newUnit.setCanAttack(false);
                }
                // Remove consumed card from player's hand
                gameState.player1.getHandManager().removeCard(gameState.handPositionClicked - 1);

                // Refresh hand UI: Clear all hand slots on the screen
                for (int i = 1; i <= 6; i++) {
                    BasicCommands.deleteCard(out, i);
                }
                // Redraw the remaining cards according to the real data
                for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
                    Card c = gameState.player1.getHandManager().getHandCards().get(i);
                    BasicCommands.drawCard(out, c, i + 1, 0); // 0 = Normal without highlighting
                }

                gameState.isUnitSummoning = false;
                gameState.selectedCard = null;
                gameState.handPositionClicked = -1;

                return; // Exit early - summon completed successfully

            } else {
                // Notify player of invalid summon location
                BasicCommands.addPlayer1Notification(out, "Invalid Summon Location!", 2);
                return;
            }
        }

        // Spell Targeting Logic
        if (gameState.isSpellTargeting && gameState.handPositionClicked != -1) {

            Tile[][] allTiles = gameState.board.getTiles();
            int rows = allTiles.length;
            int cols = allTiles[0].length;

            Card spellCard = gameState.player1.getHandManager().getHandCards().get(gameState.handPositionClicked - 1);

            if (spellCard.getCardname().equals("Wraithling Swarm")) {
                if (clickedTile.hasUnit()) {
                    BasicCommands.addPlayer1Notification(out, "Invalid Target! Must be empty.", 2);
                    return;
                }

                // 1. Summon the first Wraithling at the clicked tile
                WraithlingManager.placeWraithling(gameState, out, clickedTile, gameState.player1);
                int summonedCount = 1;

                // 2. Prioritize clustering the remaining Wraithlings around the clicked tile
                java.util.List<Tile> adjacentTiles = new java.util.ArrayList<>();
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                
                for (int[] dir : directions) {
                    try {
                        Tile adj = gameState.board.getTile(clickedTile.getTilex() + dir[0], clickedTile.getTiley() + dir[1]);
                        if (adj != null && !adj.hasUnit()) {
                            adjacentTiles.add(adj);
                        }
                    } catch (Exception e) {} // Ignore out of bounds
                }
                
                // Randomize the adjacent positions
                java.util.Collections.shuffle(adjacentTiles); 

                for (Tile t : adjacentTiles) {
                    if (summonedCount >= 3) break;
                    WraithlingManager.placeWraithling(gameState, out, t, gameState.player1);
                    summonedCount++;
                }

                // 3. Fallback: If adjacent tiles are full, place remaining on random empty tiles across the board
                if (summonedCount < 3) {
                    java.util.List<Tile> allEmptyTiles = new java.util.ArrayList<>();
                    for (int x = 0; x < 9; x++) {
                        for (int y = 0; y < 5; y++) {
                            try {
                                Tile t = gameState.board.getTile(x, y);
                                if (t != null && !t.hasUnit()) {
                                    allEmptyTiles.add(t);
                                }
                            } catch (Exception e) {}
                        }
                    }
                    java.util.Collections.shuffle(allEmptyTiles);
                    
                    for (Tile t : allEmptyTiles) {
                        if (summonedCount >= 3) break;
                        WraithlingManager.placeWraithling(gameState, out, t, gameState.player1);
                        summonedCount++;
                    }
                }

                BasicCommands.addPlayer1Notification(out, "Swarm Unleashed!", 2);

                // Deduct mana, update UI, and remove the card from hand
                gameState.player1.setMana(gameState.player1.getMana() - spellCard.getManacost());
                gameState.player1.showMana(out);
                BasicCommands.deleteCard(out, gameState.handPositionClicked);
                gameState.player1.getHandManager().removeCard(gameState.handPositionClicked - 1);

                for (int i = 1; i <= 6; i++) {
                    BasicCommands.deleteCard(out, i);
                }
                for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
                    Card c = gameState.player1.getHandManager().getHandCards().get(i);
                    BasicCommands.drawCard(out, c, i + 1, 0);
                }
                
                gameState.isSpellTargeting = false;
                gameState.highlightManager.clearHighlights(out);
                return;
            } // For targeted damage/stun spells, validate that the clicked tile has an enemy unit and then apply the effect
            
            else if (spellCard.getCardname().equals("Horn of the Forsaken")) {
                // Validate target: must be Player 1's Avatar
                if (!clickedTile.hasUnit() || !(clickedTile.getUnit() instanceof Avatar) || clickedTile.getUnit().getPlayer() != gameState.player1) {
                    BasicCommands.addPlayer1Notification(out, "Invalid Target! Select your Avatar.", 2);
                    return; // keep targeting mode active
                }

                // Equip Horn
                gameState.player1.setHornEquipped(true);
                gameState.player1.setHornDurability(3);
                BasicCommands.addPlayer1Notification(out, "Horn equipped (Durability: 3)", 2);

                // Deduct mana, update UI, remove card
                gameState.player1.setMana(gameState.player1.getMana() - spellCard.getManacost());
                gameState.player1.showMana(out);
                BasicCommands.deleteCard(out, gameState.handPositionClicked);
                gameState.player1.getHandManager().removeCard(gameState.handPositionClicked - 1);

                for (int i = 1; i <= 6; i++) {
                    BasicCommands.deleteCard(out, i);
                }
                for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
                    Card c = gameState.player1.getHandManager().getHandCards().get(i);
                    BasicCommands.drawCard(out, c, i + 1, 0);
                }
                
                gameState.isSpellTargeting = false;
                gameState.highlightManager.clearHighlights(out);
                return;
            }
            
            else if (clickedTile.hasUnit() && clickedTile.getUnit().getPlayer() != gameState.player1) {

                structures.basic.Unit targetUnit = clickedTile.getUnit();

                // Execute [28] Dark Terminus
                if (spellCard.getCardname().equals("Dark Terminus")) {
                    // Prevent targeting Avatars
                    if (targetUnit instanceof Avatar) {
                    	
                        BasicCommands.addPlayer1Notification(out, "Cannot target Avatar!", 2);
                        return;
                    }

                    targetUnit.setHealth(0);
                    BasicCommands.playUnitAnimation(out, targetUnit, structures.basic.UnitAnimationType.death);
                    

                    clickedTile.setUnit(null);
                    BasicCommands.deleteUnit(out, targetUnit);
                    BasicCommands.addPlayer1Notification(out, "Unit Destroyed!", 2);

                    WraithlingManager.placeWraithling(gameState, out, clickedTile, gameState.player1);
                   

                    
                } 

                // Deduct mana, update UI, and remove the card from hand
                gameState.player1.setMana(gameState.player1.getMana() - spellCard.getManacost());
                gameState.player1.showMana(out);

                // Remove the card from the player's hand and update the UI
                BasicCommands.deleteCard(out, gameState.handPositionClicked);
                gameState.player1.getHandManager().removeCard(gameState.handPositionClicked - 1);

                // Refresh hand UI after spell card is used
                for (int i = 1; i <= 6; i++) {
                    BasicCommands.deleteCard(out, i);
                }
                for (int i = 0; i < gameState.player1.getHandManager().getHandCards().size(); i++) {
                    Card c = gameState.player1.getHandManager().getHandCards().get(i);
                    BasicCommands.drawCard(out, c, i + 1, 0);
                }

            } else {
                BasicCommands.addPlayer1Notification(out, "Invalid Target!", 2);
                return; // keep spell targeting mode active
            }

            // Whether the spell was successfully cast or not, we exit spell targeting mode and reset the clicked card position
            gameState.isSpellTargeting = false;
            gameState.handPositionClicked = -1;
            
            // Clear targeting highlights
            gameState.highlightManager.clearHighlights(out);

            // Reset selection states
            gameState.isSpellTargeting = false;
            gameState.selectedCard = null;
            gameState.handPositionClicked = -1;
            
            // Ensure all unit animations return to idle
            Tile[][] tiles = gameState.board.getTiles();
            for (Tile[] row : tiles) {
                for (Tile t : row) {
                    if (t != null && t.hasUnit() && t.getUnit().getHealth() > 0) {
                        BasicCommands.playUnitAnimation(out, t.getUnit(), UnitAnimationType.idle);
                    }
                }
            }

            return;
        }


        // =========================
        // (Story Card 9) Adjacent Attack
        // =========================
        if (gameState.selectedTile != null
                && gameState.selectedTile.hasUnit()
                && clickedTile.hasUnit()) {

            Tile attackerTile = gameState.selectedTile;
            Unit attacker = attackerTile.getUnit();
            Unit target = clickedTile.getUnit();
            
            if (attacker.getPlayer() != gameState.player1) {
                gameState.selectedTile = null;
                return;
            }

            boolean enemy = (target.getPlayer() != attacker.getPlayer());
            boolean adjacent = isAdjacent8(attackerTile, clickedTile);

            if (enemy && adjacent && attacker.getCanAttack()) {

                // (Story Card 24) If adjacent Provoke exists, only Provoke can be attacked
                if (isBlockedByProvoke(attackerTile, clickedTile, gameState)) {
                    BasicCommands.addPlayer1Notification(out, "This unit must attack a Provoke enemy first!", 2);
                    return;
                }

                gameState.highlightManager.clearHighlights(out);

                // Attack enemy and play attack animation
                BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                try { Thread.sleep(1000); } catch (Exception e) {}

                // Story Card 20: record target health before damage
                int oldTargetHealth = target.getHealth();

                int damage = attacker.getAttack();

                if (damage > 0) {
                    target.decreaseHealth(gameState, out, damage);
                }
                
                // Trigger On Hit summon if attacker dealt damage to target
                if (target.getHealth() < oldTargetHealth) {
                    triggerUnitDealsDamage(attacker, target, gameState, out);
                }
                
                // (Story Card 12) Counterattack
                if (target.getHealth() > 0) {
                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
                    try { Thread.sleep(1000); } catch (Exception e) {}

                    // Story Card 20: record attacker health before counterattack damage
                    int oldAttackerHealth = attacker.getHealth();
                    int counterDamage = target.getAttack();

                    if (counterDamage > 0) {
                        attacker.decreaseHealth(gameState, out, counterDamage);
                    }

                    // Story Card 20: counterattack also counts as unit dealing damage
                    if (attacker.getHealth() < oldAttackerHealth) {
                        triggerUnitDealsDamage(target, attacker, gameState, out);
                    }

                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);
                }
                
                BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);

                attacker.setCanAttack(false);
                attacker.setCanMove(false);
                gameState.selectedTile = null;
                return;
            }
            // (Story Card 10) Move + Attack
            // =========================
            else if (enemy && attacker.getCanAttack() && attacker.getCanMove()) {

                Tile landingTile = null;

                // Scan 8 directions around the enemy for a valid landing spot
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                for (int[] dir : directions) {
                    int candidateX = clickedTile.getTilex() + dir[0];
                    int candidateY = clickedTile.getTiley() + dir[1];

                    Tile candidate = gameState.board.getTile(candidateX, candidateY);

                    // Call the standard rule to check if we can legally move there
                    if (candidate != null && gameState.highlightManager.isValidMove(attackerTile.getTilex(), attackerTile.getTiley(), candidateX, candidateY, candidate, gameState)) {
                        landingTile = candidate;
                        break;
                    }
                }

                if (landingTile != null) {

                    gameState.highlightManager.clearHighlights(out);

                    // Move
                    BasicCommands.moveUnitToTile(out, attacker, landingTile);
                    landingTile.setUnit(attacker);
                    attackerTile.setUnit(null);
                    attacker.setPositionByTile(landingTile);

                    try {
                        Thread.sleep(2500);
                    } catch (Exception e) {
                    }
                    // Attack
                    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                    try { Thread.sleep(1000); } catch (Exception e) {}

                    // Story Card 20: record target health before damage
                    int oldTargetHealth = target.getHealth();

                    int damage = attacker.getAttack();

                    if (damage > 0) {
                        target.decreaseHealth(gameState, out, damage);
                    }
                    
                    // Trigger On Hit immediately when attack deals damage (attacker = attacker, target = victim)
                    if (target.getHealth() < oldTargetHealth) {
                        triggerUnitDealsDamage(attacker, target, gameState, out);
                    }
                    
                    // (Story Card 12) Counterattack after move
                    if (target.getHealth() > 0 && isAdjacent8(landingTile, clickedTile)) {
                        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);

                    try { Thread.sleep(1000); } catch (Exception e) {}

                    // Story Card 20: record attacker health before counterattack damage
                    int oldAttackerHealth = attacker.getHealth();

                    int counterDamage = target.getAttack();

                    if (counterDamage > 0) {
                        attacker.decreaseHealth(gameState, out, counterDamage);
                    }

                    
                    // Story Card 20: counterattack also counts as unit dealing damage
                    if (attacker.getHealth() < oldAttackerHealth) {
                        triggerUnitDealsDamage(target, attacker, gameState, out);
                    }

                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);
                    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
                
                    }

                    attacker.setCanAttack(false);
                    attacker.setCanMove(false);
                    gameState.selectedTile = null;
                    return;
                }
            }
        }

        // =========================
        // Movement only
        // =========================
        if (gameState.selectedTile != null
                && gameState.selectedTile.hasUnit()
                && !clickedTile.hasUnit()) {

            Tile startTile = gameState.selectedTile;
            Unit unit = startTile.getUnit();
            
            if (unit.getPlayer() != gameState.player1) {
                gameState.selectedTile = null; 
                BasicCommands.addPlayer1Notification(out, "You cannot move an enemy unit!", 2);
                return;
            }

            // Directly call the standard rule in HighlightManager to validate the move
            if (unit.getCanMove() && gameState.highlightManager.isValidMove(startTile.getTilex(), startTile.getTiley(), clickedTile.getTilex(), clickedTile.getTiley(), clickedTile, gameState)) {
                gameState.highlightManager.clearHighlights(out);
                BasicCommands.moveUnitToTile(out, unit, clickedTile);
                clickedTile.setUnit(unit);
                startTile.setUnit(null);
                unit.setPositionByTile(clickedTile);
                unit.setCanMove(false);
                gameState.selectedTile = null;
                return;
            } else {
            	gameState.selectedTile = null;
                gameState.highlightManager.clearHighlights(out);
                BasicCommands.addPlayer1Notification(out, "Invalid Move!", 2);
            }
            return;
        }

        // =========================
        // Highlight selection
        // =========================
        gameState.highlightManager.clearHighlights(out);

        if (clickedTile.hasUnit()) {
            Unit unit = clickedTile.getUnit();

            if (unit.getPlayer() == gameState.player1) {

                gameState.selectedTile = clickedTile;

                // Only highlight if the unit is not stunned
                if (!unit.getIsStunned()) {
                	if (unit.getCanMove()) {
                        gameState.highlightManager.highlightMovementRange(clickedTile, gameState, out);
                    }

                    if (unit.getCanAttack()) {
                        gameState.highlightManager.highlightAttackTargets(clickedTile, gameState, out);
                    }
                } else {
                	BasicCommands.addPlayer1Notification(out, "This unit is stunned!", 2);
                }
            } else {
                gameState.selectedTile = null;
            }
        } else {
            gameState.selectedTile = null;
        }
    }

    // ======================================================
    // (Story Card 12 & 13 Helper Functions)
    // ======================================================
    private boolean isAdjacent8(Tile a, Tile b) {
        int dx = Math.abs(a.getTilex() - b.getTilex());
        int dy = Math.abs(a.getTiley() - b.getTiley());
        return Math.max(dx, dy) == 1; // include diagonal
    }
    
    private boolean isBlockedByProvoke(Tile attackerTile, Tile targetTile, GameState gameState) {
    if (attackerTile == null || targetTile == null) return false;
    if (!attackerTile.hasUnit() || !targetTile.hasUnit()) return false;

    Unit attacker = attackerTile.getUnit();
    Unit target = targetTile.getUnit();

    int[][] directions = {
        {0, 1}, {0, -1}, {1, 0}, {-1, 0},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    boolean adjacentEnemyProvokeExists = false;

    for (int[] dir : directions) {
        Tile adjTile = gameState.board.getTile(
            attackerTile.getTilex() + dir[0],
            attackerTile.getTiley() + dir[1]
        );

        if (adjTile != null && adjTile.hasUnit()) {
            Unit adjUnit = adjTile.getUnit();

            boolean isEnemy = adjUnit.getPlayer() != attacker.getPlayer();
            boolean hasProvoke = adjUnit.hasProvoke();

            if (isEnemy && hasProvoke) {
                adjacentEnemyProvokeExists = true;

                // If the current attack target is itself an adjacent Provoke unit, the attack is permitted.
                if (adjUnit == target) {
                    return false;
                }
            }
        }
    }

    // If there are enemy Provoke units nearby but the current target is not them, block this attack
    return adjacentEnemyProvokeExists;
}

 
    // Find the avatar unit belonging to the specified player
    private Unit findAvatar(GameState gameState, Player player) {

        for (int x = 0; x <= 9; x++) {
            for (int y = 0; y <= 5; y++) {

                Tile tile = gameState.board.getTile(x, y);

                if (tile != null && tile.hasUnit()) {

                    Unit unit = tile.getUnit();

                    if (unit instanceof Avatar && unit.getPlayer() == player) {
                        return unit;
                    }
                }
            }
        }

        return null;
    }

    // Story Card 30 Wraithling Swarm Summoning Logic
    private void summonWraithlingAt(ActorRef out, GameState gameState, Tile tile) {
    	// Use absolutely unique counter ID (replace coordinate hash algorithm) to completely eliminate unit collision
        int uniqueId = playerUnitIdCounter++;

        Unit wraithling = utils.BasicObjectBuilders.loadUnit(utils.StaticConfFiles.wraithling, uniqueId, Unit.class);
        wraithling.setConfigFile(utils.StaticConfFiles.wraithling);
        wraithling.setPlayer(gameState.player1);

        // Initialize base stats for Wraithling (1/1) BEFORE drawing
        wraithling.setAttack(1);
        wraithling.setHealth(1);
        wraithling.setMaxHealth(1);

        wraithling.setPositionByTile(tile);
        tile.setUnit(wraithling);

        // Render summoning effect animation
        BasicCommands.playEffectAnimation(out, utils.BasicObjectBuilders.loadEffect(utils.StaticConfFiles.f1_summon), tile);
        try {
            Thread.sleep(150);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Render unit model on the board
        BasicCommands.drawUnit(out, wraithling, tile);
        try {
            Thread.sleep(150);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Force stat UI refresh
        BasicCommands.setUnitAttack(out, wraithling, 1);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }

        BasicCommands.setUnitHealth(out, wraithling, 1);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }

        // Newly summoned units suffer from summoning sickness
        wraithling.setCanMove(false);
        wraithling.setCanAttack(false);
    }
    // (Story Card 20) Trigger when a unit successfully deals damage to an enemy unit
    private void triggerUnitDealsDamage(Unit dealer, Unit target, GameState gameState, ActorRef out) {
        if (dealer == null || target == null) return;
        if (dealer.getPlayer() == target.getPlayer()) return;

        // Check: If the damage dealer is Player 1's Avatar equipped with the Horn
        if (dealer instanceof Avatar && dealer.getPlayer() == gameState.player1) {
            
            // Verify if the Horn artifact is currently equipped and has durability remaining
            if (gameState.player1.isHornEquipped() && gameState.player1.getHornDurability() > 0) {
                
                // Find all unoccupied adjacent tiles to summon a Wraithling
                java.util.List<Tile> emptyAdjTiles = new java.util.ArrayList<>();
                int ax = dealer.getPosition().getTilex();
                int ay = dealer.getPosition().getTiley();
                int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                
                for (int[] dir : directions) {
                    try {
                        Tile adjTile = gameState.board.getTile(ax + dir[0], ay + dir[1]);
                        // Ensure the tile is empty before adding it to the valid list
                        if (adjTile != null && !adjTile.hasUnit()) {
                            emptyAdjTiles.add(adjTile);
                        }
                    } catch (Exception e) {} // Safely ignore out-of-bounds array exceptions
                }

                // If valid empty tiles exist, randomly select one for the summon
                if (!emptyAdjTiles.isEmpty()) {
                    java.util.Collections.shuffle(emptyAdjTiles);
                    Tile summonTile = emptyAdjTiles.get(0);
                    
                    // Call the manager to place the Wraithling on the board
                    WraithlingManager.placeWraithling(gameState, out, summonTile, gameState.player1);
                    BasicCommands.addPlayer1Notification(out, "Horn of the Forsaken triggers!", 2);
                }
            }
        }
    }

    // Story Card 17 Opening Gambit Logic
    private void triggerOpeningGambit(ActorRef out, GameState gameState, Unit summonedUnit, Card card) {
        String cardName = card.getCardname();
        int x = summonedUnit.getPosition().getTilex();
        int y = summonedUnit.getPosition().getTiley();

        // 1. Gloom Chaser
        if (cardName.equals("Gloom Chaser")) {
            int targetX = x - 1;
            Tile behindTile = gameState.board.getTile(targetX, y);
            if (behindTile != null && !behindTile.hasUnit()) {
            	WraithlingManager.placeWraithling(gameState, out, behindTile, gameState.player1);
                
            }
        } // 2. Nightsorrow Assassin
        else if (cardName.equals("Nightsorrow Assassin")) {
            int[][] neighbors = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] dir : neighbors) {
                Tile adjTile = gameState.board.getTile(x + dir[0], y + dir[1]);
                if (adjTile != null && adjTile.hasUnit()) {
                    Unit target = adjTile.getUnit();

                    boolean isEnemy = target.getPlayer() != summonedUnit.getPlayer();
                    boolean isDamaged = target.getHealth() < target.getMaxHealth();
                    boolean isAvatarByType = target instanceof Avatar;
                    boolean isAvatarByConfig = target.getConfigFile() != null && target.getConfigFile().contains("avatars/");

                    // Only destroy damaged enemy NON-avatar units
                    if (isEnemy && isDamaged && !isAvatarByType && !isAvatarByConfig) {
                        target.decreaseHealth(gameState, out, target.getHealth());
                        break;
                    }
                }
            }
        }
    }
}
