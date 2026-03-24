package structures.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import commands.BasicCommands;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;
import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.WraithlingManager;

/**
 * This is a representation of a Unit on the game board. A unit has a unique id
 * (this is used by the front-end. Each unit has a current UnitAnimationType,
 * e.g. move, or attack. The position is the physical position on the board.
 * UnitAnimationSet contains the underlying information about the animation
 * frames, while ImageCorrection has information for centering the unit on the
 * tile.
 *
 * @author Dr. Richard McCreadie
 *
 */
public class Unit {

    @JsonIgnore
    protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file

    int id;
    UnitAnimationType animation;
    Position position;
    UnitAnimationSet animations;
    ImageCorrection correction;
    int maxHealth;
    int health;
    int attack;
    String configFile;
    Tile tile;
    boolean canMove = true;
    boolean canAttack = true;
    Player player;
    boolean isStunned = false;

    public Unit() {
    }

    public Unit(int id, String configFile, int health, int attack, Player player) {
        this.id = id;
        this.health = health;
        this.maxHealth = health;
        this.attack = attack;
        this.configFile = configFile;
        this.player = player;

    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(0, 0, 0, 0);
        this.correction = correction;
        this.animations = animations;
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
        this.correction = correction;
        this.animations = animations;
    }

    public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
            ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = animation;
        this.position = position;
        this.animations = animations;
        this.correction = correction;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UnitAnimationType getAnimation() {
        return animation;
    }

    public void setAnimation(UnitAnimationType animation) {
        this.animation = animation;
    }

    public ImageCorrection getCorrection() {
        return correction;
    }

    public void setCorrection(ImageCorrection correction) {
        this.correction = correction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public UnitAnimationSet getAnimations() {
        return animations;
    }

    public void setAnimations(UnitAnimationSet animations) {
        this.animations = animations;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    // Visualise health on screen
    public void showHealth(ActorRef out) {
        this.health = this.getHealth();
        BasicCommands.setUnitHealth(out, this, health);
    }

    // Method to increase health without going above max health
    public void increaseHealth(ActorRef out, int amount) {
        if (health + amount > maxHealth) {
            this.health = maxHealth;

        } else {
            health = health + amount;
        }
        this.showHealth(out);

    }

    // Add permanent buff method for cards like Silverguard Squire (Story 17)
    public void applyPermanentBuff(ActorRef out, int atkAmount, int hpAmount) {
        // Increase Attack
        this.attack += atkAmount;
        BasicCommands.setUnitAttack(out, this, this.attack);

        // Increase Max Health and current Health
        this.maxHealth += hpAmount;
        this.health += hpAmount;
        BasicCommands.setUnitHealth(out, this, this.health);

        // If this unit is an Avatar, we must sync the Player object's health as well
        if (this instanceof Avatar) {
            this.player.setHealth(this.health);
            this.player.showLife(out);
        }
    }

    // Method to decrease health without going below zero
    public void decreaseHealth(GameState gameState, ActorRef out, int amount) {
        if (health - amount <= 0) {
            this.health = 0;
            this.showHealth(out);

            // Play hit and death animations and delete unit from board
            BasicCommands.playUnitAnimation(out, this, UnitAnimationType.hit);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            BasicCommands.playUnitAnimation(out, this, UnitAnimationType.death);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }

            Tile deathTile = this.tile;
          
            BasicCommands.deleteUnit(out, this);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            
            
            /*
            // Bug fix to make sure dead wraithlings are being removed from board
            if (this.configFile != null && this.configFile.contains("wraithling")) {
            	deathTile.setUnit(null);
                BasicCommands.drawTile(out, deathTile, 0);
                BasicCommands.deleteUnit(out, this);
                
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
            */
            
           

            if (deathTile != null) {
                deathTile.setUnit(null);
                BasicCommands.drawTile(out, deathTile, 0);
                
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
            
            // Trigger death abilities
            triggerUnitDeath(gameState, out);

        } else {
            health = health - amount;
            this.showHealth(out);

            // Play hit animation followed by idle animation
            BasicCommands.playUnitAnimation(out, this, UnitAnimationType.hit);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            BasicCommands.playUnitAnimation(out, this, UnitAnimationType.idle);
        }

    }

    // Getters and setters for max health
    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * This command sets the position of the Unit to a specified tile.
     *
     * @param tile
     */
    @JsonIgnore
    public void setPositionByTile(Tile tile) {
        if (tile == null) {
            return;
        }
        this.position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
        this.tile = tile;
    }

    /* Draws a unit on a tile
	and displays initial health and attack stats */
    public void drawUnit(ActorRef out, Tile tile) {
        // Get configuration files
        Unit template = BasicObjectBuilders.loadUnit(configFile, id, Unit.class);
        this.animations = template.getAnimations();
        this.correction = template.getCorrection();

        // Assign unit to tile
        this.tile = tile;
        tile.setUnit(this);
        this.setPositionByTile(tile);

        // Draw unit 
        BasicCommands.drawUnit(out, this, tile);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BasicCommands.setUnitHealth(out, this, this.health);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BasicCommands.setUnitAttack(out, this, this.attack);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Getters and setters for player 
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
	
	public static void refreshAllKeywordStates(GameState gameState, ActorRef out) {
        Tile[][] tiles = gameState.board.getTiles();

        for (int row = 0; row < tiles.length; row++) {
            for (int col = 0; col < tiles[row].length; col++) {
                Tile t = tiles[row][col];
                if (t != null && t.hasUnit()) {
                    t.getUnit().refreshKeywordStates(gameState, out);
                }
            }
        }
    }
    public void refreshKeywordStates(GameState gameState, ActorRef out) {
        if (this.configFile == null || this.tile == null) {
            return;
        }

        // Silverguard Knight: base 1/5, Zeal = gains +2 attack when adjacent to friendly Avatar
        if (this.configFile.contains("silverguard_knight")) {
            Unit friendlyAvatar = findFriendlyAvatar(gameState);
            if (friendlyAvatar == null || friendlyAvatar.tile == null) {
                return;
            }

            boolean zealActive = isAdjacent8(this.tile, friendlyAvatar.tile);
            int desiredAttack = zealActive ? 3 : 1;

            if (this.attack != desiredAttack) {
                this.attack = desiredAttack;
                BasicCommands.setUnitAttack(out, this, this.attack);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
        }
    }

    private Unit findFriendlyAvatar(GameState gameState) {
        Tile[][] tiles = gameState.board.getTiles();

        for (int row = 0; row < tiles.length; row++) {
            for (int col = 0; col < tiles[row].length; col++) {
                Tile t = tiles[row][col];
                if (t != null && t.hasUnit()) {
                    Unit u = t.getUnit();
                    if (u instanceof Avatar && u.getPlayer() == this.player) {
                        return u;
                    }
                }
            }
        }
        return null;
    }

    private boolean isAdjacent8(Tile a, Tile b) {
        int dx = Math.abs(a.getTilex() - b.getTilex());
        int dy = Math.abs(a.getTiley() - b.getTiley());
        return Math.max(dx, dy) == 1;
    }
    // Getters and setters for canMove 

    public boolean getCanMove() {
        return canMove;
    }

    public void setCanMove(boolean val) {
        this.canMove = val;
    }

    // Getters and setters for canAttack 
    public boolean getCanAttack() {
        return canAttack;
    }

    public void setCanAttack(boolean val) {
        this.canAttack = val;
    }
    // Getters and setters for isStunned

    public boolean getIsStunned() {
        return this.isStunned;
    }

    public void setIsStunned(boolean isStunned) {
        this.isStunned = isStunned;
    }

    // Story Card 18: Unit Death Trigger
    private void triggerUnitDeath(GameState gameState, ActorRef out) {

        Tile[][] tiles = gameState.board.getTiles();

        for (int row = 0; row < tiles.length; row++) {
            for (int col = 0; col < tiles[row].length; col++) {

                Tile t = tiles[row][col];

                if (t != null && t.hasUnit()) {
                    Unit u = t.getUnit();

                    // Shadow Watcher: gain +1/+1 when ANY unit dies
                    if (u.getConfigFile() != null && u.getConfigFile().contains("shadow_watcher")) {
                        u.applyPermanentBuff(out, 1, 1);
                    }

                    // Bloodmoon Priestess: summon Wraithling when ANY unit dies
                    if (u.getConfigFile() != null && u.getConfigFile().contains("bloodmoon_priestess")) {

                        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

                        for (int[] d : dirs) {
                            Tile adj = gameState.board.getTile(
                                    u.getPosition().getTilex() + d[0],
                                    u.getPosition().getTiley() + d[1]
                            );

                            if (adj != null && !adj.hasUnit()) {

                            	WraithlingManager.placeWraithling(gameState, out, adj, u.getPlayer());
                            	
                                break;
                            }
                        } 
                    }
                    // [Story 18 Bugfix] Bad Omen: gain +1 attack when ANY unit dies
                    if (u.getConfigFile() != null && u.getConfigFile().toLowerCase().replace("_", "").contains("badomen")) {
                        // Bad Omen only gains +1 Attack (0 Health)
                        u.applyPermanentBuff(out, 1, 0);
                    }

                    // [Story 18 Bugfix] Shadowdancer: 1 Dmg to Enemy Avatar, 1 Heal to Own Avatar when ANY unit dies
                    if (u.getConfigFile() != null && u.getConfigFile().toLowerCase().replace("_", "").contains("shadowdancer")) {
                        Player owner = u.getPlayer();
                        Player enemy = (owner == gameState.player1) ? gameState.player2 : gameState.player1;

                        Unit owningAvatar = null;
                        Unit enemyAvatar = null;
                        
                        // Scan to find both avatars
                        for (int ix = 0; ix < tiles.length; ix++) {
                            for (int iy = 0; iy < tiles[ix].length; iy++) {
                                Tile tempT = tiles[ix][iy];
                                if (tempT != null && tempT.hasUnit() && tempT.getUnit() instanceof Avatar) {
                                    if (tempT.getUnit().getPlayer() == owner) {
                                        owningAvatar = tempT.getUnit();
                                    } else {
                                        enemyAvatar = tempT.getUnit();
                                    }
                                }
                            }
                        }

                        // Apply 1 damage to enemy avatar
                        if (enemyAvatar != null && enemyAvatar.getHealth() > 0) {
                        	enemyAvatar.decreaseHealth(gameState, out, 1);
                        }
                        
                        // Apply 1 heal to owning avatar (do not exceed max health)
                        if (owningAvatar != null && owningAvatar.getHealth() < owningAvatar.getMaxHealth()) {
                        	owningAvatar.increaseHealth(out, 1);
                        	
                        }
                    }
                }
            }
        }
        
     // Bug fix for units who do not return to idle animation
        // because of Wraithling summoning
        
        for (Tile[] rowTiles : tiles) {
        	for (Tile tile : rowTiles) {
        		if (tile != null && tile.hasUnit()) {
        			Unit unit = tile.getUnit();
            		if (unit.getHealth() != 0) {
            			BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);
            			 try {
                             Thread.sleep(100);
                         } catch (Exception e) {
                         }
            		}
                	  
        		}
        	}
        }
    }

	// Getter and setter for configFile
    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    // (Story Card 24) Check if this unit has Provoke ability
    public boolean hasProvoke() {
        if (configFile == null) return false;

        String config = configFile.toLowerCase();

        return config.contains("swamp_entangler")
            || config.contains("rock_pulveriser")
            || config.contains("silverguard_knight")
            || config.contains("ironcliff_guardian");
    }
}
