package io.skypvp.mlg;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MLGArena {

	private static final List<MLGArena> arenas = new ArrayList<>();

	private final String name;
	private final List<Player> players = new ArrayList<>();
	private final Location pos1;
	private final Location pos2;

	public MLGArena(Location pos1, Location pos2) {
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.name = "default";
	}

	public MLGArena(String name, Location pos1, Location pos2) {
		this.name = name;
		this.pos1 = pos1;
		this.pos2 = pos2;
	}

	public boolean isArenaFloor(Location currentLocation) {
		Location firstCorner = this.pos1;
		Location secondCorner = this.pos2;

		double minX = Math.min(firstCorner.getX(), secondCorner.getX());
		double maxX = Math.max(firstCorner.getX(), secondCorner.getX());
		double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
		double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

		return (minX <= currentLocation.getX() && currentLocation.getX() <= maxX && minZ <= currentLocation.getZ()
				&& currentLocation.getZ() <= maxZ && currentLocation.getY() <= firstCorner.getY() + 1);
	}

	public boolean areCornersSetup() {
		Location firstCorner = this.pos1;
		Location secondCorner = this.pos2;

		for (double d : new double[] { firstCorner.getX(), firstCorner.getZ(), secondCorner.getX(),
				secondCorner.getZ() }) {
			if (d == 0.0)
				return false;
		}

		return true;
	}

	public String getName() {
		return name;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public Location getPos1() {
		return pos1;
	}

	public Location getPos2() {
		return pos2;
	}

	public static List<MLGArena> getArenas() {
		return arenas;
	}

	public static MLGArena fromName(String name) {
		for (MLGArena mlgArena : arenas) {
			if(mlgArena.getName().equals(name))
				return mlgArena;
		}
		return null;
	}
}
