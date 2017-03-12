package io.skypvp.mlg;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Commands implements CommandExecutor {

    final MLGArena instance;
    final Settings settings;

    public Commands(MLGArena main) {
        this.instance = main;
        this.settings = main.getSettings();
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String cmdLbl, String[] args) {
        Player player = null;
        if(cmd.getName().equalsIgnoreCase("mlg")) {
            if(sender instanceof Player) player = (Player) sender;

            if(args.length == 1 && args[0].equalsIgnoreCase("top")) {
                if(sender.hasPermission("mlg.leaderboard") || sender.isOp()) {
                    new BukkitRunnable() {

                        public void run() {
                            Connection conn = null;

                            try {
                                conn = settings.getDatabase().openConnection(false);
                                for(Player p : instance.getServer().getOnlinePlayers()) {
                                    settings.getDatabase().updatePlayerStats(p.getUniqueId(), conn);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }


                            final MySQLQuery rs = settings.getDatabase().query(String.format("SELECT * FROM %s ORDER BY POINTS DESC LIMIT 10", MLGUtils.TABLE_NAME));
                            final ResultSet results = rs.getResults();

                            new BukkitRunnable() {

                                public void run() {
                                    try {
                                        int leaders = 0;
                                        sender.sendMessage("                     ");
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lLeaderboard"));
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2&l------------------------"));
                                        do {
                                            leaders += 1;
                                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                                    String.format("&c%d. &6%s - &e%d Points", leaders, 
                                                            results.getString("NAME"), results.getInt("POINTS"))));
                                        } while(results.next());

                                        if(leaders == 0) {
                                            sender.sendMessage(MLGUtils.NO_STATS);
                                        }

                                        new BukkitRunnable() {

                                            public void run() {
                                                rs.close();
                                            }

                                        }.runTaskAsynchronously(instance);

                                    } catch (SQLException e) {
                                        sender.sendMessage(MLGUtils.NO_STATS);
                                        instance.sendConsoleMessage(ChatColor.DARK_RED + String.format("ERROR: Failed to iterate through MySQL leaderboard. Error: %s.", e.getMessage()));
                                    }
                                }

                            }.runTaskLater(instance, 1L);
                        }

                    }.runTaskAsynchronously(instance);
                    return true;
                }else {
                    sender.sendMessage(MLGUtils.NO_PERMISSION);
                    return true;
                }
            }else if(args.length == 2 && args[0].equalsIgnoreCase("setcorner")) {
                if(player != null && player.hasPermission("mlg.admin") || player.isOp()) {
                    if(args[1].equalsIgnoreCase("1")) {
                        setCorner(player, "first");
                    }else if(args[1].equalsIgnoreCase("2")) {
                        setCorner(player, "second");
                    }

                    return true;
                }else if(player != null && !player.hasPermission("mlg.admin")) {
                    player.sendMessage(MLGUtils.NO_PERMISSION);
                    return true;
                }else {
                    sender.sendMessage(MLGUtils.MUST_BE_PLAYER);
                    return true;
                }
            }else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cDid you mean to do &3/mlg top&c?"));
                return true;
            }
        }

        return false;
    }

    public void setCorner(Player p, String corner) {
        Location loc = p.getLocation();
        if(corner.equalsIgnoreCase("first")) {
            instance.getSettings().setFirstCorner(loc);
        }else if(corner.equalsIgnoreCase("second")) {
            instance.getSettings().setSecondCorner(loc);
        }

        instance.getSettings().setWorld(loc.getWorld());
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&aSet the %s corner to x: %s, y: %s, z: %s.", corner, loc.getX(), loc.getY(), loc.getZ())));
    }

}
