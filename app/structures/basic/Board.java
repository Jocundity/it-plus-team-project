package structures.basic;

import commands.BasicCommands;
import utils.BasicObjectBuilders;
import akka.actor.ActorRef;

public class Board {
	int rows = 5;
	int cols = 9;
	
	private Tile[][] tiles;

	public Board() {
		tiles = new Tile[rows][cols];
		InitialiseTiles();
		
	}
	
	// Build array of tiles
	private void InitialiseTiles() {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Tile tile = BasicObjectBuilders.loadTile(col + 1, row + 1);
				tile.setXpos(tile.getXpos() - 120);
				tile.setYpos(tile.getYpos() - 120);
				tiles[row][col] = tile;
			}
		}
	}
	
	// Draws a 5x9 board on the screen
	public void drawBoard(ActorRef out) {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Tile tile = tiles[row][col];
				BasicCommands.drawTile(out, tile, 0);
				try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}
	}
	
	// Return array of tiles
	public Tile[][] getTiles() {
		return tiles;
	}
	
	// Get specific tile using 1 as the starting row or column
	public Tile getTile(int x, int y) {
		if ((x >= 1 && x <= cols) && (y >=1 && y <= rows)) {
			return tiles[y - 1][x - 1];
		}
		return null;
	}
	
}
