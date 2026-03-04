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

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();
        if (gameState.gameOver) return;

        Tile clickedTile = gameState.board.getTile(tilex, tiley);
        if (clickedTile == null) return;

        //
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

            } else {
                BasicCommands.addPlayer1Notification(out, "Invalid Target!", 2);
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

                gameState.highlightManager.clearHighlights(clickedTile, out);

                int ms = BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                try { Thread.sleep(ms); } catch (Exception e) {}

                boolean targetDied = applyDamageAndHandleDeath(out, gameState, clickedTile, target, attacker.getAttack());

                // (Story Card 12) Counterattack
                if (!targetDied) {
                    tryCounterAttackOnce(out, gameState, attackerTile, attacker, clickedTile, target);
                }

                attacker.setCanAttack(false);
                attacker.setCanMove(false);
                gameState.selectedTile = null;
                return;
            }

            // =========================
            // (Story Card 10) Move + Attack
            // =========================
            else if (enemy && attacker.getCanAttack()) {

                Tile landingTile = null;
                int minDistance = 999;

                int[][] directions = {{0,1},{0,-1},{1,0},{-1,0}};
                for (int[] dir : directions) {
                    Tile candidate = gameState.board.getTile(
                        clickedTile.getTilex() + dir[0],
                        clickedTile.getTiley() + dir[1]);

                    if (candidate != null && !candidate.hasUnit()) {

                        int dx = Math.abs(candidate.getTilex() - attackerTile.getTilex());
                        int dy = Math.abs(candidate.getTiley() - attackerTile.getTiley());

                        if (dx <= 2 && dy <= 2) {
                            int dist = dx + dy;
                            if (dist < minDistance) {
                                minDistance = dist;
                                landingTile = candidate;
                            }
                        }
                    }
                }

                if (landingTile != null) {

                    gameState.highlightManager.clearHighlights(clickedTile, out);

                    BasicCommands.moveUnitToTile(out, attacker, landingTile);

                    landingTile.setUnit(attacker);
                    attackerTile.setUnit(null);
                    attacker.setPositionByTile(landingTile);

                    try { Thread.sleep(1500); } catch (Exception e) {}

                    int ms = BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
                    try { Thread.sleep(ms); } catch (Exception e) {}

                    boolean targetDied = applyDamageAndHandleDeath(out, gameState, clickedTile, target, attacker.getAttack());

                    // (Story Card 12) Counterattack after move
                    if (!targetDied) {
                        tryCounterAttackOnce(out, gameState, landingTile, attacker, clickedTile, target);
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

            int dx = Math.abs(clickedTile.getTilex() - startTile.getTilex());
            int dy = Math.abs(clickedTile.getTiley() - startTile.getTiley());

            if (dx <= 2 && dy <= 2 && unit.getCanMove()) {

                gameState.highlightManager.clearHighlights(clickedTile, out);

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
        gameState.highlightManager.clearHighlights(clickedTile, out);

        if (clickedTile.hasUnit()) {
            Unit unit = clickedTile.getUnit();

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
}