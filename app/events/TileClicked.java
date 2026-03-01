package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.UnitAnimationType;
import structures.basic.Tile;
import structures.basic.Unit;

public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        Tile clickedTile = gameState.board.getTile(tilex, tiley);
        if (clickedTile == null) return;

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

                boolean targetDied = applyDamageAndHandleDeath(out, clickedTile, target, attacker.getAttack());

                // (Story Card 12) Counterattack
                if (!targetDied) {
                    tryCounterAttackOnce(out, attackerTile, attacker, clickedTile, target);
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

                    boolean targetDied = applyDamageAndHandleDeath(out, clickedTile, target, attacker.getAttack());

                    // (Story Card 12) Counterattack after move
                    if (!targetDied) {
                        tryCounterAttackOnce(out, landingTile, attacker, clickedTile, target);
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
                                             Tile targetTile,
                                             Unit target,
                                             int damage) {

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
                                      Tile attackerTile,
                                      Unit attacker,
                                      Tile defenderTile,
                                      Unit defender) {

        if (defender.getHealth() <= 0) return;
        if (!isAdjacent8(attackerTile, defenderTile)) return;

        int ms = BasicCommands.playUnitAnimation(out, defender, UnitAnimationType.attack);
        try { Thread.sleep(ms); } catch (Exception e) {}

        applyDamageAndHandleDeath(out, attackerTile, attacker, defender.getAttack());
    }
}