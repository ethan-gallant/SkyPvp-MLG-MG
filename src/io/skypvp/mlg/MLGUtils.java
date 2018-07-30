package io.skypvp.mlg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.skypvp.mlg.sql.Database;
import net.minecraft.server.v1_8_R1.AxisAlignedBB;
import net.minecraft.server.v1_8_R1.EntityPlayer;

public class MLGUtils {

	/**
	 * The different y heights that players will be teleported to.
	 */

	public static final HashMap<Integer, Integer> Y_LEVELS = new HashMap<Integer, Integer>();
	public static final List<Material> WATER_TYPES = Arrays.asList(Material.WATER, Material.STATIONARY_WATER);
	static protected MLGPlugin instance = MLGPlugin.instance;

	static {
		Y_LEVELS.put(10, 1);
		Y_LEVELS.put(30, 5);
		Y_LEVELS.put(50, 10);
		Y_LEVELS.put(100, 50);
		Y_LEVELS.put(150, 100);
		Y_LEVELS.put(170, 120);
		Y_LEVELS.put(200, 150);
		Y_LEVELS.put(250, 170);
		Y_LEVELS.put(255, 200);
	}

	public static final String TABLE_NAME = "mlg";
	public static final String NO_PERMISSION = ChatColor.DARK_RED
			+ "You don't have permission to execute that command.";
	public static final String NO_STATS = ChatColor.RED + "There are no stats :(";
	public static final String MUST_BE_PLAYER = ChatColor.RED + "You must be a player to execute this command.";

	public static Map<String, Object> getPlayerInfo(String uuid) {
		Database database = instance.getSettings().getDatabase();
		List<Map<String, Object>> list = database
				.query(String.format("SELECT * FROM " + TABLE_NAME + " WHERE UUID='" + uuid + "';"));
		return list.size() > 0 ? list.get(0) : null;
	}

	public static void handlePlayerCreation(Player player) {
		Database database = instance.getSettings().getDatabase();

		try {
			Connection con = database.getAdaptor().getConnection();
			PreparedStatement statement = con.prepareStatement(
					"INSERT INTO " + MLGUtils.TABLE_NAME + " (UUID, NAME, POINTS; STREAK) VALUES (?, ?, 0, 0)");
			statement.setString(1, player.getUniqueId().toString());
			statement.setString(2, player.getName());
			statement.execute();
		} catch (SQLException e) {
			instance.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while creating row for new player.");
			e.printStackTrace();
		}

	}

	public static ItemStack generateBucketFor(int yHeight) {
		ItemStack bucket = new ItemStack(Material.WATER_BUCKET, 1);
		ItemMeta meta = bucket.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
				String.format("&b&l%s &8> &e%d Points", String.valueOf(yHeight), Y_LEVELS.get(yHeight))));
		bucket.setItemMeta(meta);
		return bucket;
	}

	public static int getSpecialBucketHeight(ItemStack item) {
		/**
		 * This code can only pass if this item is a special bucket.
		 */

		if (isSpecialBucket(item)) {
			String displayName = ChatColor.stripColor(ChatColor.stripColor(item.getItemMeta().getDisplayName()
					.substring(0, item.getItemMeta().getDisplayName().indexOf(">") - 3)));
			return Integer.valueOf(displayName);
		}

		return -1;
	}

	/**
	 * Checks if an ItemStack is one of our special buckets.
	 * 
	 * @param ItemStack item
	 * @return if its one of our special buckets or not.
	 */

	public static boolean isSpecialBucket(ItemStack item) {
		if (item == null || item != null && item.getType() == Material.AIR)
			return false;

		/**
		 * Let's check if this item
		 */

		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
			String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName().substring(0,
					item.getItemMeta().getDisplayName().indexOf(">") - 3));
			try {
				return item.getType() == Material.WATER_BUCKET
						&& Y_LEVELS.keySet().contains(Integer.parseInt(displayName));
			} catch (NumberFormatException e) {
				return false;
			}
		}

		return false;
	}

	public static ArrayList<Block> getBlocksBelow(Player player) {
		ArrayList<Block> blocksBelow = new ArrayList<Block>();
		EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
		AxisAlignedBB boundingBox = entityPlayer.getBoundingBox();
		World world = player.getWorld();
		double yBelow = player.getLocation().getY();
		Block northEast = new Location(world, boundingBox.d, yBelow, boundingBox.c).getBlock();
		Block northWest = new Location(world, boundingBox.a, yBelow, boundingBox.c).getBlock();
		Block southEast = new Location(world, boundingBox.d, yBelow, boundingBox.f).getBlock();
		Block southWest = new Location(world, boundingBox.a, yBelow, boundingBox.f).getBlock();
		Block[] blocks = { northEast, northWest, southEast, southWest };
		for (Block block : blocks) {
			if (!blocksBelow.isEmpty()) {
				boolean duplicateExists = false;
				for (int i = 0; i < blocksBelow.size(); i++) {
					if (blocksBelow.get(i).equals(block)) {
						duplicateExists = true;
					}
				}
				if (!duplicateExists) {
					blocksBelow.add(block);
				}
			} else {
				blocksBelow.add(block);
			}
		}
		return blocksBelow;
	}
}
