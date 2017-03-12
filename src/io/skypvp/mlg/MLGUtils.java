package io.skypvp.mlg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class MLGUtils {

    /**
     * The different y heights that players will be teleported to.
     */

    public static final HashMap<Integer, Integer> Y_LEVELS = new HashMap<Integer, Integer>();
    public static final List<Material> WATER_TYPES = Arrays.asList(Material.WATER, Material.STATIONARY_WATER);

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
    public static final String NO_PERMISSION = ChatColor.DARK_RED + "You don't have permission to execute that command.";
    public static final String NO_STATS = ChatColor.RED + "There are no stats :(";
    public static final String MUST_BE_PLAYER = ChatColor.RED + "You must be a player to execute this command.";

    public static void success(final MLGArena instance, final Player p) {
        final int pointsEarned = Y_LEVELS.get(instance.getDropHeight(p.getUniqueId()));
        final String pointStr = (pointsEarned == 1) ? "point" : "points";
        instance.resetPlayer(p);
        instance.setPoints(p.getUniqueId(), instance.getPoints(p.getUniqueId()) + pointsEarned);
        instance.getInArena().remove(p.getUniqueId());

        new BukkitRunnable() {

            public void run() {
                if(pointsEarned == MLGUtils.Y_LEVELS.get(255)) {
                    int streak = instance.getStreak(p.getUniqueId());
                    instance.setStreak(p.getUniqueId(), streak + 1);

                    if(streak != 0) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&aWow! You're on a &6M&dL&bG &eStreak &aof &c&l%d&r&a. &2&lKeep it up!", streak)));
                    }

                    Sound[] notes = {Sound.NOTE_BASS, 
                            Sound.NOTE_BASS_DRUM, 
                            Sound.NOTE_BASS_GUITAR, 
                            Sound.NOTE_PIANO, 
                            Sound.NOTE_PLING, 
                            Sound.NOTE_SNARE_DRUM, 
                            Sound.NOTE_STICKS};

                    Sound sound = notes[ThreadLocalRandom.current().nextInt(0, notes.length)];
                    p.playSound(p.getLocation(), sound, 1F, 4F);
                }

                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&lGreat Job! &r&aYou've done it!"));
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&eYou've earned &6&l%d &r&e%s!", pointsEarned, pointStr)));
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);

                instance.showStats(p);
            }

        }.runTaskLater(instance, 3L);
    }

    public static void failure(final MLGArena instance, final Player p) {
        final int points = Y_LEVELS.get(instance.getDropHeight(p.getUniqueId()));
        instance.getInArena().remove(p.getUniqueId());
        instance.resetPlayer(p);
        new BukkitRunnable() {

            public void run() {
                if(points == MLGUtils.Y_LEVELS.get(255)) {
                    int streak = instance.getStreak(p.getUniqueId());
                    instance.setStreak(p.getUniqueId(), 0);

                    if(streak != 0) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&cYou've lost your &6M&dL&bG &eStreak&c&r&c :(", streak)));
                        p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
                    }
                }


                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4&lZoinks! &r&cBetter luck next time!"));
                p.playSound(p.getLocation(), Sound.IRONGOLEM_HIT, 1F, 1F);

                instance.showStats(p);
            }

        }.runTaskLater(instance, 3L);
    }

    public static boolean areCornersSetup(MLGArena instance) {
        Location firstCorner = instance.getSettings().getFirstCorner();
        Location secondCorner = instance.getSettings().getSecondCorner();

        for(double d : new double[] {firstCorner.getX(), firstCorner.getZ(), secondCorner.getX(), secondCorner.getZ()}) {
            if(d == 0.0) return false;
        }

        return true;
    }

    public static boolean isArenaFloor(MLGArena instance, Location loc) {
        Location firstCorner = instance.getSettings().getFirstCorner();
        Location secondCorner = instance.getSettings().getSecondCorner();

        double minX = Math.min(firstCorner.getX(), secondCorner.getX());
        double maxX = Math.max(firstCorner.getX(), secondCorner.getX());
        double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
        double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

        return (minX <= loc.getX() && loc.getX() <= maxX && minZ <= loc.getZ() && loc.getZ() <= maxZ && loc.getY() <= firstCorner.getY() + 1);
    }

    @SuppressWarnings("deprecation")
    public static void enterArena(MLGArena instance, Player p, int height) {
        if(MLGUtils.areCornersSetup(instance)) {
            Location firstCorner = instance.getSettings().getFirstCorner();
            Location secondCorner = instance.getSettings().getSecondCorner();

            double minX = Math.min(firstCorner.getX(), secondCorner.getX());
            double maxX = Math.max(firstCorner.getX(), secondCorner.getX());
            double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
            double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

            double x = ThreadLocalRandom.current().nextInt((int) minX + 1, (int) maxX - 1);
            double z = ThreadLocalRandom.current().nextInt((int) minZ + 1, (int) maxZ - 1);

            for(int i = 0; i < 9; i++) {
                p.getInventory().setItem(i, new ItemStack(Material.WATER_BUCKET, 1));
            }

            p.updateInventory();

            instance.setInArena(p.getUniqueId(), height);
            p.teleport(new Location(instance.getSettings().getWorld(), x, height, z));
        }
    }

    public static void handlePlayerEntrance(final Player p, final MLGArena instance) {
        final Database database = instance.getSettings().getDatabase();

        new BukkitRunnable() {

            public void run() {
                MySQLQuery query = database.query(String.format("SELECT * FROM %s WHERE UUID='%s';", MLGUtils.TABLE_NAME, p.getUniqueId()));
                ResultSet rs = query.getResults();

                // We need to handle creating new profiles and obtaining them.
                if(rs == null) {
                    try {
                        String update = String.format("INSERT INTO %s (UUID, NAME, POINTS, STREAK) VALUES (?, ?, 0, 0)",
                                MLGUtils.TABLE_NAME);
                        PreparedStatement statement = database.openConnection(false).prepareStatement(update);
                        statement.setString(1, p.getUniqueId().toString());
                        statement.setString(2, p.getName());
                        statement.execute();

                        instance.getPoints().put(p.getUniqueId(), 0);
                        instance.getStreaks().put(p.getUniqueId(), 0);
                    } catch (SQLException e) {
                        instance.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while creating row for new player.");
                        e.printStackTrace();
                    }
                }else {
                    try {
                        instance.getPoints().put(p.getUniqueId(), rs.getInt("POINTS"));
                    } catch (SQLException e) {
                        instance.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while obtaining row for player.");
                        e.printStackTrace();
                    }
                }
            }

        }.runTaskAsynchronously(instance);
    }

    public static void giveSpecialBuckets(Player p) {
        p.getInventory().setContents(new ItemStack[p.getInventory().getContents().length]);
        Integer[] levels = Y_LEVELS.keySet().toArray(new Integer[Y_LEVELS.keySet().size()]);
        Arrays.sort(levels);

        int i = 0;
        for(int height : levels) {
            p.getInventory().setItem(i, generateBucketFor(height));
            i++;
        }
    }

    public static ItemStack generateBucketFor(int yHeight) {
        ItemStack bucket = new ItemStack(Material.WATER_BUCKET, 1);
        ItemMeta meta = bucket.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', String.format("&b&l%s &8> &e%d Points", String.valueOf(yHeight), Y_LEVELS.get(yHeight))));
        bucket.setItemMeta(meta);
        return bucket;
    }

    public static int getSpecialBucketHeight(ItemStack item) {
        /**
         * This code can only pass if this item is a special bucket.
         */

        if(isSpecialBucket(item)) {
            String displayName = ChatColor.stripColor(ChatColor.stripColor(item.getItemMeta().getDisplayName().substring(0, item.getItemMeta().getDisplayName().indexOf(">") - 3)));
            return Integer.valueOf(displayName);
        }

        return -1;
    }

    /**
     * Checks if an ItemStack is one of our special buckets.
     * @param ItemStack item
     * @return if its one of our special buckets or not.
     */

    public static boolean isSpecialBucket(ItemStack item) {
        if(item == null || item != null && item.getType() == Material.AIR) return false;

        /**
         * Let's check if this item 
         */

        if(item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName().substring(0, item.getItemMeta().getDisplayName().indexOf(">") - 3));
            try {
                return item.getType() == Material.WATER_BUCKET && Y_LEVELS.keySet().contains(Integer.parseInt(displayName));
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }
}
