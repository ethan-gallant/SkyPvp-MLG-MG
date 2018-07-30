package io.skypvp.mlg.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.skypvp.mlg.MLGPlugin;
import io.skypvp.mlg.MLGArena;
import io.skypvp.mlg.MLGPlayer;
import io.skypvp.mlg.MLGUtils;
import io.skypvp.mlg.Settings;

public class MLGCommand implements CommandExecutor {

	final MLGPlugin instance;
	final Settings settings;

	public MLGCommand(MLGPlugin main) {
		this.instance = main;
		this.settings = main.getSettings();
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String cmdLbl, String[] args) {

		if (!(sender instanceof Player))
			return true;

		Player player = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("mlg")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
				if (sender.hasPermission("mlg.leaderboard") || sender.isOp()) {
					new BukkitRunnable() {
						public void run() {
							List<MLGPlayer> leaderBoad = MLGPlayer.LeaderBoard.getLeaderBoard();

							if (leaderBoad.size() <= 0) {
								sender.sendMessage(MLGUtils.NO_STATS);
								return;
							}

							sender.sendMessage("                     ");
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lLeaderboard"));
							sender.sendMessage(
									ChatColor.translateAlternateColorCodes('&', "&2&l------------------------"));

							int position = 0;
							for (MLGPlayer players : leaderBoad) {
								position++;
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
										String.format("&c %d. &6 %s - &e %d Points", position, players.getName(),
												players.getPoints())));
							}

						}
					}.runTask(instance);

				} else {
					sender.sendMessage(MLGUtils.NO_PERMISSION);
					return true;
				}
			} else if (args.length == 2 && args[0].equalsIgnoreCase("setcorner")) {
				if (player != null && player.hasPermission("mlg.admin") || player.isOp()) {
					if (args[1].equalsIgnoreCase("1"))
						setCorner(player, "first");
					else if (args[1].equalsIgnoreCase("2"))
						setCorner(player, "second");

					return true;
				} else if (player != null && !player.hasPermission("mlg.admin")) {
					player.sendMessage(MLGUtils.NO_PERMISSION);
					return true;
				} else {
					sender.sendMessage(MLGUtils.MUST_BE_PLAYER);
					return true;
				}
			} else {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cDid you mean to do &3/mlg top&c?"));
				return true;
			}
		}

		return false;
	}

	public void setCorner(Player p, String corner) {
		Location loc = p.getLocation();
		if (corner.equalsIgnoreCase("first")) {
			instance.getSettings().setFirstCorner(loc);
		} else if (corner.equalsIgnoreCase("second")) {
			instance.getSettings().setSecondCorner(loc);
		}

		if (instance.getSettings().getFirstCorner() != null && instance.getSettings().getSecondCorner() != null
				&& MLGArena.fromName("default") == null)
			MLGArena.getArenas().add(
					new MLGArena(instance.getSettings().getFirstCorner(), instance.getSettings().getSecondCorner()));
		instance.getSettings().setWorld(loc.getWorld());
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', String
				.format("&aSet the %s corner to x: %s, y: %s, z: %s.", corner, loc.getX(), loc.getY(), loc.getZ())));
	}

}
