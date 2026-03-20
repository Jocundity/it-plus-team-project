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

                summonWraithlingAt(out, gameState, clickedTile);

                // Scan the board to summon up to 2 additional Wraithlings on available empty tiles
                int summonedCount = 1;
                for (int x = 0; x < 15 && summonedCount < 3; x++) {
                    for (int y = 0; y < 15 && summonedCount < 3; y++) {
                        try {
                            Tile t = gameState.board.getTile(x, y);
                            if (t != null && !t.hasUnit() && t != clickedTile) {
                                summonWraithlingAt(out, gameState, t);
                                summonedCount++;
                            }
                        } catch (Exception e) {
                            // Silently ignore array out-of-bounds
                        }
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
            } // For targeted damage/stun spells, validate that the clicked tile has an enemy unit and then apply the effect
            else if (clickedTile.hasUnit() && clickedTile.getUnit().getPlayer() != gameState.player1) {

                structures.basic.Unit targetUnit = clickedTile.getUnit();

                // Execute [28] Dark Terminus
                if (spellCard.getCardname().equals("Dark Terminus")) {
                    targetUnit.setHealth(0);
                    BasicCommands.playUnitAnimation(out, targetUnit, structures.basic.UnitAnimationType.death);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }

                    clickedTile.setUnit(null);
                    BasicCommands.deleteUnit(out, targetUnit);
                    BasicCommands.addPlayer1Notification(out, "Unit Destroyed!", 2);

                    // Use absolutely unique counter ID
                    structures.basic.Unit wraithling = utils.BasicObjectBuilders.loadUnit(utils.StaticConfFiles.wraithling, playerUnitIdCounter++, structures.basic.Unit.class);
                    
                    // Critical fix: Assign stats in advance
                    wraithling.setAttack(1);
                    wraithling.setHealth(1);
                    wraithling.setMaxHealth(1);
                    
                    wraithling.setConfigFile(utils.StaticConfFiles.wraithling);
                    wraithling.setPlayer(gameState.player1);
                    wraithling.setPositionByTile(clickedTile);
                    clickedTile.setUnit(wraithling);

                    // Draw the Wraithling on the board and then immediately set its health and attack to 1/1 to reflect the card's effect
                    BasicCommands.drawUnit(out, wraithling, clickedTile);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }

                    BasicCommands.setUnitAttack(out, wraithling, 1);
                    BasicCommands.setUnitHealth(out, wraithling, 1);
                } // Execute [29] Beamshock
                else if (spellCard.getCardname().equals("Beamshock")) {
                    targetUnit.setIsStunned(true);
                    BasicCommands.addPlayer1Notification(out, "Unit Stunned!", 2);
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

            // Clear all highlights (including the red ones for spell targeting)
            for (int x = 1; x <= cols; x++) {
                for (int y = 1; y <= rows; y++) {
                    Tile t = gameState.board.getTile(x, y);
                    if (t != null) {
                        BasicCommands.drawTile(out, t, 0);
                    }
                }
            }

            // Clear targeting highlights
            gameState.highlightManager.clearHighlights(out);

            // Reset selection states
            gameState.isSpellTargeting = false;
            gameState.selectedCard = null;
            gameState.handPositionClicked = -1;

            return;
        }

        BasicCommands.addPlayer1Notification(out, "Clicked: " + tilex + ", " + tiley, 2);

        // =========================
        // (Story Card 9) Adjacent Attack
        // =========================
        if (gameState.selectedTile != null
                && gameState.selectedTile.hasUnit()
                && clickedTile.hasUnit()) {

            Tile attackerTile = gameState.selectedTile;
            Unit attacker = attackerTile.getUnit();
            Unit target = clickedTile.getUnit();

            boolean enemy = (target.getPlayer() != attacker.getPlayer());
            boolean adjacent = isAdjacent8(attackerTile, clickedTile);

            if (enemy && adjacent && attacker.getCanAttack()) {

                gameState.highlightManager.clearHighlights(out);

                // Attack enemy and play attack animation
                BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                target.decreaseHealth(gameState, out, attacker.getAttack());
                triggerHornOnHitIfNeeded(out, gameState, attacker, target);
                // (Story Card 12) Counterattack
                if (target.getHealth() > 0) {
                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    attacker.decreaseHealth(gameState, out, target.getAttack());
                    BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);
                }

                attacker.setCanAttack(false);
                attacker.setCanMove(false);
                gameState.selectedTile = null;
                return;
            } // =========================
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
                        Thread.sleep(1500);
                    } catch (Exception e) {
                    }
                    // Attack
                    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    target.decreaseHealth(gameState, out, attacker.getAttack());
                    triggerHornOnHitIfNeeded(out, gameState, attacker, target);
                    
                    // (Story Card 12) Counterattack after move
                    if (target.getHealth() > 0 && isAdjacent8(landingTile, clickedTile)) {
                        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                        attacker.decreaseHealth(gameState, out, target.getAttack());
                        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);
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
            }
        }

        // =========================
        // Highlight selection
        // =========================
        gameState.highlightManager.clearHighlights(out);

        if (clickedTile.hasUnit()) {
            Unit unit = clickedTile.getUnit();

            // --- DEBUG LINES ---
            String debugMsg = String.format("Tile: %d,%d | Unit: %d,%d",
                    clickedTile.getTilex(), clickedTile.getTiley(),
                    unit.getPosition().getTilex(), unit.getPosition().getTiley());
            BasicCommands.addPlayer1Notification(out, debugMsg, 2);
            // -------------------

            if (unit.getPlayer() == gameState.player1) {

                gameState.selectedTile = clickedTile;

                if (unit.getCanMove()) {
                    gameState.highlightManager.highlightMovementRange(clickedTile, gameState, out);
                }

                if (unit.getCanAttack()) {
                    gameState.highlightManager.highlightAttackTargets(clickedTile, gameState, out);
                }
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

    /* Simplified and moved logic to (decreaseHealth)
   *  inside of Unit class for better reusability  

    
    // (Story Card 13) Unified Damage + Death Handling
    private boolean applyDamageAndHandleDeath(ActorRef out,
                                             GameState gameState,
                                             Tile targetTile,
                                             Unit target,
                                             int damage) {
        // Story 14: Avatar Damage
        if (target instanceof Avatar) {

            Player owner = target.getPlayer();

            int newPlayerHealth = owner.getHealth() - damage;
            owner.setHealth(newPlayerHealth);
        
            //Synchronize Avatar's own blood volume (for UI bubble)
            target.setHealth(newPlayerHealth);
            BasicCommands.setUnitHealth(out, target, Math.max(newPlayerHealth, 0));
            owner.showLife(out);

        // Story 15: Win / Loss
            if (newPlayerHealth <= 0) {

                if (owner == gameState.player1) {
                    BasicCommands.addPlayer1Notification(out, "Game Over - You Lose!", 5);
                } else {
                    BasicCommands.addPlayer1Notification(out, "Victory - You Win!", 5);
                }

                gameState.gameOver = true;
            }

            return false; // Avatar will not be deleted.
        }        

        int newHealth = target.getHealth() - damage;
        target.setHealth(newHealth);

        BasicCommands.setUnitHealth(out, target, Math.max(newHealth, 0));
        try { Thread.sleep(150); } catch (Exception e) {}

        if (newHealth <= 0) {
            BasicCommands.playUnitAnimation(out, target, UnitAnimationType.death);
            try { Thread.sleep(300); } catch (Exception e) {}

            BasicCommands.deleteUnit(out, target);

            if (targetTile != null) {
                targetTile.setUnit(null);
            }

            return true;
        }

        return false;
    }

    // (Story Card 12) Counterattack
    private void tryCounterAttackOnce(ActorRef out,
                                      GameState gameState,
                                      Tile attackerTile,
                                      Unit attacker,
                                      Tile defenderTile,
                                      Unit defender) {

        if (defender.getHealth() <= 0) return;
        if (!isAdjacent8(attackerTile, defenderTile)) return;

        int ms = BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.attack);
        try { Thread.sleep(ms); } catch (Exception e) {}

        applyDamageAndHandleDeath(out, gameState, attackerTile, attacker, defender.getAttack());
    }
     */
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

    private void triggerHornOnHitIfNeeded(ActorRef out, GameState gameState, Unit attacker, Unit target) {
        // Horn only applies to player 1 avatar
        if (!(attacker instanceof Avatar)) {
            return;
        }
        if (attacker.getPlayer() != gameState.player1) {
            return;
        }
        if (!gameState.player1.isHornEquipped()) {
            return;
        }

        // Horn only triggers when damaging an enemy NON-avatar unit
        if (target == null) {
            return;
        }
        if (target.getPlayer() == attacker.getPlayer()) {
            return;
        }
        if (target instanceof Avatar) {
            return;
        }

        // Find an empty adjacent tile around player 1 avatar
        Unit avatar = findAvatar(gameState, gameState.player1);
        if (avatar == null || avatar.getPosition() == null) {
            return;
        }

        int ax = avatar.getPosition().getTilex();
        int ay = avatar.getPosition().getTiley();

        int[][] directions = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] dir : directions) {
            Tile adj = gameState.board.getTile(ax + dir[0], ay + dir[1]);
            if (adj != null && !adj.hasUnit()) {
                summonWraithlingAt(out, gameState, adj);
                break;
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
            	// Use absolutely unique counter ID
                Unit wraithling = utils.BasicObjectBuilders.loadUnit(utils.StaticConfFiles.wraithling, playerUnitIdCounter++, Unit.class);
                wraithling.setConfigFile(utils.StaticConfFiles.wraithling);
                wraithling.setPlayer(gameState.player1);

                // Set the unit's attributes in the backend data first
                wraithling.setAttack(1);
                wraithling.setHealth(1);
                wraithling.setMaxHealth(1);

                wraithling.setPositionByTile(behindTile);
                behindTile.setUnit(wraithling);

                BasicCommands.playEffectAnimation(out, utils.BasicObjectBuilders.loadEffect(utils.StaticConfFiles.f1_summon), behindTile);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                } // Wait for the summoning effect animation

                // Instruct the front-end to draw the unit model
                BasicCommands.drawUnit(out, wraithling, behindTile);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                } // Wait 100ms to allow the front-end to finish rendering the model

                // Update Attack UI
                BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttack());
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                } // Separate the UI update commands to prevent asynchronous overlapping

                // Update Health UI
                BasicCommands.setUnitHealth(out, wraithling, wraithling.getHealth());
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
