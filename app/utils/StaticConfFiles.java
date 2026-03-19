package utils;

/**
 * This is a utility class that just has short-cuts to the location of various
 * config files. 
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class StaticConfFiles {

	// Board Pieces
	public final static String tileConf = "conf/gameconfs/tile.json";
	public final static String gridConf = "conf/gameconfs/grid.json";
	
	// Avatars
	public final static String humanAvatar = "conf/gameconfs/avatars/avatar1.json";
	public final static String aiAvatar = "conf/gameconfs/avatars/avatar2.json";
	
	// Tokens
	public final static String wraithling = "conf/gameconfs/units/wraithling.json";
	
	// Effects
	public final static String f1_inmolation = "conf/gameconfs/effects/f1_inmolation.json";
	public final static String f1_buff = "conf/gameconfs/effects/f1_buff.json";
	public final static String f1_martyrdom = "conf/gameconfs/effects/f1_martyrdom.json";
	public final static String f1_projectiles = "conf/gameconfs/effects/f1_projectiles.json";
	public final static String f1_summon = "conf/gameconfs/effects/f1_summon.json";
	
	// Added central mapping logic for Story Card 17
		public static String getUnitConf(String cardName) {
			switch (cardName) {
				// Abyssian (Player 1) units
				case "Bad Omen": return "conf/gameconfs/units/bad_omen.json";
				case "Gloom Chaser": return "conf/gameconfs/units/gloom_chaser.json";
				case "Shadow Watcher": return "conf/gameconfs/units/shadow_watcher.json";
				case "Nightsorrow Assassin": return "conf/gameconfs/units/nightsorrow_assassin.json";
				case "Rock Pulveriser": return "conf/gameconfs/units/rock_pulveriser.json";
				case "Bloodmoon Priestess": return "conf/gameconfs/units/bloodmoon_priestess.json";
				case "Shadowdancer": return "conf/gameconfs/units/shadowdancer.json";

				// Lyonar (Player 2) units
				case "Skyrock Golem": return "conf/gameconfs/units/skyrock_golem.json";
				case "Swamp Entangler": return "conf/gameconfs/units/swamp_entangler.json";
				case "Silverguard Knight": return "conf/gameconfs/units/silverguard_knight.json";
				case "Saberspine Tiger": return "conf/gameconfs/units/saberspine_tiger.json";
				case "Young Flamewing": return "conf/gameconfs/units/young_flamewing.json";
				case "Silverguard Squire": return "conf/gameconfs/units/silverguard_squire.json";
				case "Ironcliff Guardian": return "conf/gameconfs/units/ironcliff_guardian.json";

				default: return null;
			}
		}
	
}
