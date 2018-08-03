package io.skypvp.mlg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This is the main MLGPlayer class, from this class you can control all the
 * player actions
 * 
 * @author Florian Hergenhahn
 *
 * @since 2.0
 */
public class MLGPlayer {

	/**
	 * List With all the current playing {@link MLGPlayer}'s
	 */
	private static final Map<UUID, MLGPlayer> playerMap = new HashMap<>();

	/**
	 * UUID from {@link MLGPlayer}
	 */
	private final String uuid;

	/**
	 * Username from {@link MLGPlayer}
	 */
	private final String name;

	/**
	 * Points from {@link MLGPlayer}
	 */
	private int points;

	/**
	 * Streak count from {@link MLGPlayer}
	 */
	private int streak;

	/**
	 * This could be <strong>Null</strong> {@link Player} instance from
	 * {@link MLGPlayer}
	 * 
	 */
	private Player player;

	/**
	 * Current height
	 */
	private int inArena;

	/**
	 * {@link MLGArena} instance where the {@link MLGPlayer} currently is
	 */
	private MLGArena arena;

	/**
	 * This gathers all data from MySQL
	 * 
	 * @param player Player instance
	 */
	public MLGPlayer(Player player) {
		this(MLGUtils.getPlayerInfo(player.getUniqueId().toString()), player);
	}

	/**
	 * @param map This {@link Map} contains a complete Sql row (UUID, NAME, POINTS,
	 *            STREAK)
	 */
	public MLGPlayer(Map<String, Object> map) {
		String name = null;
		String uuid = null;
		int points = 0;
		int streak = 0;
		if (map != null && map.size() > 0) {
			name = (String) map.get("NAME");
			uuid = (String) map.get("UUID");
			points = (int) map.get("POINTS");
			streak = (int) map.get("STREAK");
		}

		this.points = points;
		this.streak = streak;
		this.name = name;
		this.uuid = uuid;
	}

	/**
	 * @param map    This {@link Map} contains a complete Sql row (UUID, NAME,
	 *               POINTS, STREAK)
	 * @param player
	 */
	public MLGPlayer(Map<String, Object> map, Player player) {
		String name = player.getName();
		String uuid = player.getUniqueId().toString();
		int points = 0;
		int streak = 0;

		if (map != null && map.size() > 0) {
			points = (int) map.get("POINTS");
			streak = (int) map.get("STREAK");
		} else
			MLGUtils.handlePlayerCreation(player);

		this.points = points;
		this.streak = streak;
		this.name = name;
		this.uuid = uuid;
		this.player = player;
	}

	/**
	 * @param uuid   UUID from Player
	 * @param name   Name from Player
	 * @param points Points from Player
	 * @param streak Streakcount from Player
	 */
	public MLGPlayer(String uuid, String name, int points, int streak) {
		this(uuid, name, points, streak, null);
	}

	/**
	 * @param uuid   UUID from Player
	 * @param name   Name from Player
	 * @param points Points from Player
	 * @param streak Streakcount from Player
	 * @param player {@link Player} instance
	 */
	public MLGPlayer(String uuid, String name, int points, int streak, Player player) {
		this.uuid = uuid;
		this.name = name;
		this.points = points;
		this.player = player;
		this.streak = streak;
	}

	/**
	 * If {@link MLGPlayer#player} is not null than the player gets a message with
	 * the current stats
	 */
	public void showStats() {
		if (this.player == null)
			return;

		this.player.sendMessage("                              ");
		this.player.sendMessage(ChatColor.translateAlternateColorCodes('&',
				String.format("&2You currently have &e%s &2%s&r&2.", points, (points != 1) ? "points" : "point")));
		if (streak != 0) {
			this.player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					String.format("&aYou are on a &6M&dL&9G &eStreak &aof &c&l%d&r&a!", streak)));
		}
	}

	/**
	 * This synchronises the current 'local' stats with sql
	 */
	public void syncStats() {
		try {
			Connection conn = MLGPlugin.instance.getAdaptor().getConnection();
			PreparedStatement statement = conn
					.prepareStatement("UPDATE " + MLGUtils.TABLE_NAME + " SET POINTS=?,STREAK=? WHERE UUID=?");
			statement.setInt(1, points);
			statement.setInt(2, streak);
			statement.setString(3, this.uuid);
			statement.execute();

			statement.close();
			conn.close();
		} catch (SQLException e) {
			MLGPlugin.instance.sendConsoleMessage(
					ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
			e.printStackTrace();
		}
	}

	/**
	 * This gets executes if the player fails
	 */
	public void fail() {

		final int points = MLGUtils.Y_LEVELS.get(getDropHeight());
		this.reset();
		Integer streak = new Integer(this.streak);
		this.streak = 0;

		if (this.player == null)
			return;

		new BukkitRunnable() {

			public void run() {
				if (points == MLGUtils.Y_LEVELS.get(255)) {
					if (streak != 0) {
						player.sendMessage(ChatColor.translateAlternateColorCodes('&',
								String.format("&cYou've lost your &6M&dL&bG &eStreak&c&r&c :(", streak)));
						player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
					}
				}

				player.sendMessage(
						ChatColor.translateAlternateColorCodes('&', "&4&lZoinks! &r&cBetter luck next time!"));
				player.playSound(player.getLocation(), Sound.IRONGOLEM_HIT, 1F, 1F);

				showStats();
			}

		}.runTaskLater(MLGPlugin.instance, 3L);
	}

	/**
	 * This gets executed if the Player succeeds
	 */
	public void succeed() {
		final int pointsEarned = MLGUtils.Y_LEVELS.get(getDropHeight());
		final String pointStr = (pointsEarned == 1) ? "point" : "points";
		this.reset();
		setPoints(getPoints() + pointsEarned);
		streak++;
		new BukkitRunnable() {

			public void run() {
				if (pointsEarned == MLGUtils.Y_LEVELS.get(255)) {
					if (streak != 0) {
						player.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format(
								"&aWow! You're on a &6M&dL&bG &eStreak &aof &c&l%d&r&a. &2&lKeep it up!", streak)));
					}

					Sound[] notes = { Sound.NOTE_BASS, Sound.NOTE_BASS_DRUM, Sound.NOTE_BASS_GUITAR, Sound.NOTE_PIANO,
							Sound.NOTE_PLING, Sound.NOTE_SNARE_DRUM, Sound.NOTE_STICKS };

					Sound sound = notes[ThreadLocalRandom.current().nextInt(0, notes.length)];
					player.playSound(player.getLocation(), sound, 1F, 4F);
				}

				player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lGreat Job! &r&aYou've done it!"));
				player.sendMessage(ChatColor.translateAlternateColorCodes('&',
						String.format("&eYou've earned &6&l%d &r&e%s!", pointsEarned, pointStr)));
				player.playSound(player.getLocation(), Sound.LEVEL_UP, 1F, 1F);

				showStats();
			}

		}.runTaskLater(MLGPlugin.instance, 3L);
	}

	/**
	 * Player eneters the given arena
	 * @param arena {@link MLGArena} instance
	 * @param height The current height from Player
	 */
	public void enterArena(MLGArena arena, int height) {
		if (this.player == null)
			return;

		if (arena.areCornersSetup()) {
			Location firstCorner = arena.getPos1();
			Location secondCorner = arena.getPos2();

			double minX = Math.min(firstCorner.getX(), secondCorner.getX());
			double maxX = Math.max(firstCorner.getX(), secondCorner.getX());
			double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
			double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

			double x = ThreadLocalRandom.current().nextInt((int) minX + 1, (int) maxX - 1);
			double z = ThreadLocalRandom.current().nextInt((int) minZ + 1, (int) maxZ - 1);

			for (int i = 0; i < 9; i++) {
				player.getInventory().setItem(i, new ItemStack(Material.WATER_BUCKET, 1));
			}

			this.arena = arena;
			this.arena.getPlayers().add(player);
			player.updateInventory();
			this.setInArena(height);
			player.teleport(new Location(arena.getPos1().getWorld(), x, height, z));
		}
	}

	/**
	 * Gives the player special buckets
	 */
	public void giveSpecialBuckets() {
		if (this.player == null)
			return;

		player.getInventory().setContents(new ItemStack[player.getInventory().getContents().length]);
		Integer[] levels = MLGUtils.Y_LEVELS.keySet().toArray(new Integer[MLGUtils.Y_LEVELS.keySet().size()]);
		Arrays.sort(levels);

		int i = 0;
		for (int height : levels) {
			player.getInventory().setItem(i, MLGUtils.generateBucketFor(height));
			i++;
		}
	}

	/**
	 * @return The dropheight for the arena
	 */
	public int getDropHeight() {
		if (!isInArena())
			return -1;
		return inArena;
	}

	/**
	 * Resets the Player to the world spawn
	 */
	public void reset() {

		if (this.player == null)
			return;

		inArena = 0;
		this.player.teleport(MLGPlugin.instance.getSettings().getWorld().getSpawnLocation());
		this.giveSpecialBuckets();
	}

	public int getStreak() {
		return streak;
	}

	public void setStreak(int streak) {
		this.streak = streak;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public String getUuid() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public boolean isInArena() {
		return inArena >= 0;
	}

	public void setInArena(int inArena) {
		this.inArena = inArena;
	}

	public MLGArena getArena() {
		return arena;
	}

	public void setArena(MLGArena arena) {
		this.arena = arena;
	}

	public static Map<UUID, MLGPlayer> getPlayermap() {
		return playerMap;
	}

	public static MLGPlayer fromPlayer(Player player) {
		return fromPlayer(player.getUniqueId());

	}

	public static MLGPlayer fromPlayer(String uuid) {
		return fromPlayer(UUID.fromString(uuid));
	}

	public static MLGPlayer fromPlayer(UUID uuid) {

		if (playerMap.containsKey(uuid))
			return playerMap.get(uuid);

		return null;
	}

	public static List<MLGPlayer> getOnlinePlayers() {
		List<MLGPlayer> players = new ArrayList<>();
		players.addAll(playerMap.values());
		return players;
	}

	public static class LeaderBoard {

		private static long cacheTime = 0;
		private static List<MLGPlayer> cachedList = null;

		/**
		 * @return Sorted {@link List} with {@link MLGPlayer}'s
		 */
		public static List<MLGPlayer> getLeaderBoard() {

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, 5);
			if (cacheTime >= cal.getTimeInMillis() && cachedList != null)
				return cachedList;

			List<Map<String, Object>> set = MLGPlugin.instance.getSettings().getDatabase().getAllPlayers();
			LinkedList<MLGPlayer> players = new LinkedList<>();
			players.addAll(MLGPlayer.getOnlinePlayers());

			for (Map<String, Object> map : set) {
				if (!players.contains(MLGPlayer.fromPlayer((String) map.get("UUID"))))
					players.add(new MLGPlayer(map));
			}

			players.sort(new Comparator<MLGPlayer>() {

				@Override
				public int compare(MLGPlayer o1, MLGPlayer o2) {
					return o1.getPoints() > o2.getPoints() ? o1.points : o2.points;
				}
			});

			cachedList = players;
			cacheTime = cal.getTimeInMillis();

			return players;
		}

	}
}
