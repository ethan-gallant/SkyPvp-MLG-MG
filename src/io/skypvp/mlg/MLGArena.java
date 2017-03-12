package io.skypvp.mlg;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MLGArena extends JavaPlugin {

    private final HashMap<UUID, Integer> points = new HashMap<UUID, Integer>();
    private final HashMap<UUID, Integer> mlgStreaks = new HashMap<UUID, Integer>();
    private final HashMap<UUID, Integer> inArena = new HashMap<UUID, Integer>();
    private Settings settings = null;

    public void onEnable() {
        settings = new Settings(this);
        settings.load();
    }

    public void onDisable() {
        if(settings != null && settings.getDatabase() != null) {
            for(Player p : getServer().getOnlinePlayers()) {
                settings.getDatabase().handlePlayerExit(p.getUniqueId());
            }
        }

        if(settings != null) {
            settings.save();
        }
    }

    public void showStats(final Player p) {
        p.sendMessage("                              ");
        int points = getPoints(p.getUniqueId());
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&2You currently have &e%s &2%s&r&2.", points, (points != 1) ? "points" : "point")));

        int streak = getStreak(p.getUniqueId());
        if(streak != 0) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&aYou are on a &6M&dL&9G &eStreak &aof &c&l%d&r&a!", streak)));
        }
    }

    public void resetPlayer(final Player p) {
        inArena.remove(p.getUniqueId());
        p.teleport(settings.getWorld().getSpawnLocation());
        MLGUtils.giveSpecialBuckets(p);
    }

    public void databaseConnected() {
        Events events = new Events(this);
        getServer().getPluginManager().registerEvents(events, this);

        Commands commands = new Commands(this);
        getCommand("mlg").setExecutor(commands);

        for(Player p : getServer().getOnlinePlayers()) {
            MLGUtils.handlePlayerEntrance(p, this);
            resetPlayer(p);
        }
    }

    public void setInArena(UUID uuid, int dropHeight) {
        inArena.put(uuid, dropHeight);
    }

    public int getDropHeight(UUID uuid) {
        if(!isInArena(uuid)) return -1;
        return inArena.get(uuid);
    }

    public boolean isInArena(UUID uuid) {
        return inArena.get(uuid) != null;
    }

    public void setPoints(UUID uuid, int p) {
        points.put(uuid, p);
    }

    public int getPoints(UUID uuid) {
        if(points.get(uuid) != null) {
            return points.get(uuid);
        }

        return 0;
    }

    public void setStreak(UUID uuid, int streak) {
        mlgStreaks.put(uuid, streak);
    }

    public int getStreak(UUID uuid) {
        if(mlgStreaks.get(uuid) != null) {
            return mlgStreaks.get(uuid);
        }

        return 0;
    }

    public HashMap<UUID, Integer> getPoints() {
        return this.points;
    }

    public HashMap<UUID, Integer> getStreaks() {
        return this.mlgStreaks;
    }

    public HashMap<UUID, Integer> getInArena() {
        return this.inArena;
    }

    public void disable() {
        setEnabled(false);
    }

    public void sendConsoleMessage(String msg) {
        String pluginName = getDescription().getName();
        String prefix = ChatColor.DARK_GRAY +  "[" + ChatColor.AQUA + pluginName + ChatColor.DARK_GRAY + "]";
        getServer().getConsoleSender().sendMessage(prefix + " " + msg);
    }

    public Settings getSettings() {
        return this.settings;
    }

}
