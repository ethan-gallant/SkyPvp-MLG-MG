package io.skypvp.mlg;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.skypvp.mlg.commands.MLGCommand;
import io.skypvp.mlg.events.BlockPhysicListener;
import io.skypvp.mlg.events.DamageListener;
import io.skypvp.mlg.events.InteractListener;
import io.skypvp.mlg.events.JoinListener;
import io.skypvp.mlg.events.MLGListener;
import io.skypvp.mlg.events.PlayerMoveListener;
import io.skypvp.mlg.events.QuitListener;
import io.skypvp.mlg.events.WaterFlowListener;
import io.skypvp.mlg.sql.MySQLAdaptor;

public class MLGPlugin extends JavaPlugin {
	private Settings settings = null;
	private MySQLAdaptor adaptor;

	public static MLGPlugin instance;

	public void onEnable() {
		instance = this;
		settings = new Settings(this);
		settings.load();
		this.adaptor = settings.getDatabase().getAdaptor();
	}

	public void onDisable() {
		if (settings != null && settings.getDatabase() != null) {
			for (Player player : getServer().getOnlinePlayers()) {
				MLGPlayer.fromPlayer(player).syncStats();
				MLGPlayer.getPlayermap().remove(player.getUniqueId());
			}
		}

		if (settings != null)
			settings.save();
	}

	public void databaseConnected() {
		loadCommands();
		loadListener();

		for (Player player : getServer().getOnlinePlayers()) {
			MLGPlayer p = new MLGPlayer(player);
			p.reset();
			MLGPlayer.getPlayermap().put(player.getUniqueId(), p);
		}
	}

	private void loadCommands() {
		MLGCommand command = new MLGCommand(this);
		getCommand("mlg").setExecutor(command);
	}

	private void loadListener() {
		MLGListener[] listener = { new BlockPhysicListener(), new DamageListener(), new InteractListener(),
				new JoinListener(), new PlayerMoveListener(), new QuitListener(), new WaterFlowListener() };

		for (MLGListener mlgListener : listener) {
			mlgListener.initEvent();
		}
	}

	public void disable() {
		setEnabled(false);
	}

	public void sendConsoleMessage(String msg) {
		String pluginName = getDescription().getName();
		String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + pluginName + ChatColor.DARK_GRAY + "]";
		getServer().getConsoleSender().sendMessage(prefix + " " + msg);
	}

	public Settings getSettings() {
		return this.settings;
	}

	public MySQLAdaptor getAdaptor() {
		return adaptor;
	}

}
