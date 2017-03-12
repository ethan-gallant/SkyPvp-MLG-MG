package io.skypvp.mlg;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class Settings {

    final MLGArena main;
    private Database database;
    private final File configFile;
    private final YamlConfiguration config;

    private World world;
    private Location firstCorner;
    private Location secondCorner;

    public Settings(MLGArena instance) {
        this.main = instance;
        this.configFile = new File(main.getDataFolder() + "/config.yml");
        this.database = null;
        this.world = null;
        this.firstCorner = null;
        this.secondCorner = null;

        // Let's make sure everything is ready.
        boolean configAvailable = configFile.exists();
        if(!configAvailable) {
            main.saveDefaultConfig();
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public double[] getCornerData(String sectionName) {
        ConfigurationSection corner = config.getConfigurationSection(sectionName);
        double x = corner.getDouble("x");
        double y = corner.getDouble("y");
        double z = corner.getDouble("z");
        return new double[] {x, y, z};
    }

    public void save() {
        if(world != null) {
            config.set("world", world.getName());

            if(firstCorner != null) {
                ConfigurationSection fCorner = config.getConfigurationSection("firstCorner");
                fCorner.set("x", firstCorner.getX());
                fCorner.set("y", firstCorner.getY());
                fCorner.set("z", firstCorner.getZ());
            }

            if(secondCorner != null) {
                ConfigurationSection sCorner = config.getConfigurationSection("secondCorner");
                sCorner.set("x", secondCorner.getX());
                sCorner.set("y", secondCorner.getY());
                sCorner.set("z", secondCorner.getZ());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            main.sendConsoleMessage(ChatColor.DARK_RED + String.format("ERROR: Failed to save config file. Error: %s.", e.getMessage()));
        }
    }

    public void load() {
        // Let's load the world.
        String worldName = config.getString("world");
        world = Bukkit.getWorld(worldName);

        if(world == null) {
            main.sendConsoleMessage(ChatColor.DARK_RED + String.format("World %s does not exist! Update your config!", world));
            main.disable();
            return;
        }

        // Let's load the corner data.
        double[] firstCornerData = getCornerData("firstCorner");
        double[] secondCornerData = getCornerData("secondCorner");
        firstCorner = new Location(world, firstCornerData[0], firstCornerData[1], firstCornerData[2]);
        secondCorner = new Location(world, secondCornerData[0], secondCornerData[1], secondCornerData[2]);

        // Let's load the database settings.
        ConfigurationSection db = config.getConfigurationSection("database");
        String dbHost = db.getString("host");
        String dbPort = db.getString("port");
        String dbUsername = db.getString("username");
        String dbPassword = db.getString("password");
        String dbDatabase = db.getString("database");
        database = new Database(main, dbHost, dbPort, dbUsername, dbPassword, dbDatabase);

        new BukkitRunnable() {

            Connection conn = null;

            public void run() {
                try {
                    conn = database.openConnection(true);
                    if(!database.tableExists(conn)) database.createTable(conn);
                } catch (SQLException e) {
                    main.sendConsoleMessage(ChatColor.DARK_RED + "Failed to connect to MySQL database. Are your settings correct in the config?");
                    e.printStackTrace();
                    main.disable();
                } finally {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

        }.runTaskAsynchronously(main);
    }

    public Database getDatabase() {
        return this.database;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public World getWorld() {
        return this.world;
    }

    public void setFirstCorner(Location loc) {
        this.firstCorner = loc;
    }

    public Location getFirstCorner() {
        return this.firstCorner;
    }

    public void setSecondCorner(Location loc) {
        this.secondCorner = loc;
    }

    public Location getSecondCorner() {
        return this.secondCorner;
    }
}
