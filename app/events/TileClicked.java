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

public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
   
    	// If it is not currently Player 1's turn, all clicks on the board will be directly ignored.
    	if (!gameState.isPlayer1Turn) {
            return; 
        }
    	
        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();
        if (gameState.gameOver) return;

        Tile clickedTile = gameState.board.getTile(tilex, tiley);
        if (clickedTile == null) return;

        // (Story Card 25) Unit Summoning Execution Phase
        if (gameState.isUnitSummoning && gameState.selectedCard != null && gameState.handPositionClicked != -1) {
            
            // Validate summon tile: must be empty and adjacent (8-direction) to a friendly unit
            boolean isValidSummonTile = false;
            if (!clickedTile.hasUnit()) {
                int[][] directions = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}};
                for (int[] dir : directions) {
                    Tile adjTile = gameState.board.getTile(clickedTile.getTilex() + dir[0], clickedTile.getTiley() + dir[1]);
                    if (adjTile != null && adjTile.hasUnit() && adjTile.getUnit().getPlayer() == gameState.player1) {
                        isValidSummonTile = true;
                        break;
                    }
                }
            }

            if (isValidSummonTile) {
                Card cardToSummon = gameState.selectedCard;
                
                // Deduct mana cost from player and refresh mana UI display
                gameState.player1.setMana(gameState.player1.getMana() - cardToSummon.getManacost());
                gameState.player1.showMana(out);
                
                // Play the summoning circle effect
                BasicCommands.playEffectAnimation(out, utils.BasicObjectBuilders.loadEffect(utils.StaticConfFiles.f1_summon), clickedTile);
                try { Thread.sleep(500); } catch (Exception e) {}
                
                // Resolve unit config file by card name
                String confFile = getUnitConfigFile(cardToSummon.getCardname());
                
                // Create a monster unit and set it to belong to Player 1
                int unitID = Math.abs(cardToSummon.getCardname().hashCode() + clickedTile.getTilex() + clickedTile.getTiley());
                Unit newUnit = utils.BasicObjectBuilders.loadUnit(confFile, unitID, Unit.class);
                newUnit.setPlayer(gameState.player1);
                
                // Place unit on target tile and render to game view
                newUnit.setPositionByTile(clickedTile);
                clickedTile.setUnit(newUnit);
                BasicCommands.drawUnit(out, newUnit, clickedTile);
                try { Thread.sleep(100); } catch (Exception e) {}
                
                // Sync unit stats (ATK/HP) with card values and update UI
                newUnit.setAttack(cardToSummon.getBigCard().getAttack());
                newUnit.setHealth(cardToSummon.getBigCard().getHealth());
                BasicCommands.setUnitAttack(out, newUnit, newUnit.getAttack());
                BasicCommands.setUnitHealth(out, newUnit, newUnit.getHealth());
                
                // Newly summoned units are "exhausted" (can't move/attack) for the current turn
                newUnit.setCanMove(false);
                newUnit.setCanAttack(false);
                
                // Remove consumed card from player's hand
                gameState.player1.getHandManager().removeCard(gameState.handPositionClicked - 1);
                
                // Safely obtain cards
                gameState.highlightManager.clearHighlights(out);
                
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
            
            // 
            Tile[][] allTiles = gameState.board.getTiles();
            int rows = allTiles.length;
            int cols = allTiles[0].length;

            // Only allow targeting enemy units
            if (clickedTile.hasUnit() && clickedTile.getUnit().getPlayer() != gameState.player1) {
                
                Card spellCard = gameState.player1.getHandManager().getHandCards().get(gameState.handPositionClicked - 1);
                structures.basic.Unit targetUnit = clickedTile.getUnit();

                // Execute [28] Dark Terminus
                if (spellCard.getCardname().equals("Dark Terminus")) {
                    targetUnit.setHealth(0);
                    BasicCommands.playUnitAnimation(out, targetUnit, structures.basic.UnitAnimationType.death);
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    
                    clickedTile.setUnit(null); 
                    BasicCommands.deleteUnit(out, targetUnit); 
                    BasicCommands.addPlayer1Notification(out, "Unit Destroyed!", 2);

                    // Add a new Wraithling unit to the board at the clicked tile
                    // 999 is just a temporary ID
                    structures.basic.Unit wraithling = utils.BasicObjectBuilders.loadUnit(utils.StaticConfFiles.wraithling, 999, structures.basic.Unit.class); 
                    wraithling.setPlayer(gameState.player1);
                    wraithling.setPositionByTile(clickedTile);
                    clickedTile.setUnit(wraithling); 
                    
                    // Draw the Wraithling on the board and then immediately set its health and attack to 1/1 to reflect the card's effect
                    BasicCommands.drawUnit(out, wraithling, clickedTile);
                    try { Thread.sleep(100); } catch (Exception e) {}
                    
                    BasicCommands.setUnitAttack(out, wraithling, 1);
                    BasicCommands.setUnitHealth(out, wraithling, 1);
                }

                // Execute [29] Beamshock
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
        if (gameState.selectedTile != null &&
            gameState.selectedTile.hasUnit() &&
            clickedTile.hasUnit()) {

            Tile attackerTile = gameState.selectedTile;
            Unit attacker = attackerTile.getUnit();
            Unit target = clickedTile.getUnit();

            boolean enemy = (target.getPlayer() != attacker.getPlayer());
            boolean adjacent = isAdjacent8(attackerTile, clickedTile);

            if (enemy && adjacent && attacker.getCanAttack()) {

                gameState.highlightManager.clearHighlights(out);

                // Attack enemy and play attack animation
                BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                try { Thread.sleep(1000); } catch (Exception e) {}
                target.decreaseHealth(gameState, out, attacker.getAttack());

                // (Story Card 12) Counterattack
                if (target.getHealth() > 0) {
                	BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    attacker.decreaseHealth(gameState, out, target.getAttack());
    				BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);
                }

                attacker.setCanAttack(false);
                attacker.setCanMove(false);
                gameState.selectedTile = null;
                return;
            }

            // =========================
            // (Story Card 10) Move + Attack
            // =========================
            else if (enemy && attacker.getCanAttack() && attacker.getCanMove()) {

                Tile landingTile = null;
                

                // Scan 8 directions around the enemy for a valid landing spot
                int[][] directions = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
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
                    try { Thread.sleep(1500); } catch (Exception e) {}

                    // Attack
                    BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    target.decreaseHealth(gameState, out, attacker.getAttack());
                    
                    // (Story Card 12) Counterattack after move
                    if (target.getHealth() > 0 && isAdjacent8(landingTile, clickedTile)) {
                    	BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
                        try { Thread.sleep(1000); } catch (Exception e) {}
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
        if (gameState.selectedTile != null &&
                gameState.selectedTile.hasUnit() &&
                !clickedTile.hasUnit()) {

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
    
    // Map the card names to the monster model configuration files
    private String getUnitConfigFile(String cardName) {
        switch (cardName) {
            // Abyssian (Player 1)'s monster
            case "Bad Omen": return "conf/gameconfs/units/bad_omen.json";
            case "Gloom Chaser": return "conf/gameconfs/units/gloom_chaser.json";
            case "Shadow Watcher": return "conf/gameconfs/units/shadow_watcher.json";
            case "Nightsorrow Assassin": return "conf/gameconfs/units/nightsorrow_assassin.json";
            case "Rock Pulveriser": return "conf/gameconfs/units/rock_pulveriser.json";
            case "Bloodmoon Priestess": return "conf/gameconfs/units/bloodmoon_priestess.json";
            case "Shadowdancer": return "conf/gameconfs/units/shadowdancer.json";
        
            // Lyonar (Player 2)'s monster
            case "Skyrock Golem": return "conf/gameconfs/units/skyrock_golem.json";
            case "Swamp Entangler": return "conf/gameconfs/units/swamp_entangler.json";
            case "Silverguard Knight": return "conf/gameconfs/units/silverguard_knight.json";
            case "Saberspine Tiger": return "conf/gameconfs/units/saberspine_tiger.json";
            case "Young Flamewing": return "conf/gameconfs/units/young_flamewing.json";
            case "Silverguard Squire": return "conf/gameconfs/units/silverguard_squire.json";
            case "Ironcliff Guardian": return "conf/gameconfs/units/ironcliff_guardian.json";
            default: return utils.StaticConfFiles.wraithling; 
        }
    }
    
}