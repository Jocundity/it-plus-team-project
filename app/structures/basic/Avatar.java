package structures.basic;

public class Avatar extends Unit {
	Player player;

	public Avatar(Player player, int id) {
		super(id, player.getAvatarConfigFile(), player.getHealth(), 2, player);
		this.player = player;
		
	}

	public Avatar(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super(id, animations, correction);
		// TODO Auto-generated constructor stub
	}

	public Avatar(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super(id, animations, correction, currentTile);
		// TODO Auto-generated constructor stub
	}

	public Avatar(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
			ImageCorrection correction) {
		super(id, animation, position, animations, correction);
		// TODO Auto-generated constructor stub
	}

}
