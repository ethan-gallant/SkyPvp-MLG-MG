package io.skypvp.mlg.events;

import org.bukkit.event.Listener;

import io.skypvp.mlg.MLGPlugin;
import io.skypvp.mlg.Settings;

public abstract class MLGListener implements Listener {
	
	protected MLGPlugin instance = MLGPlugin.instance;
	protected Settings settings = instance.getSettings();
	
	public void initEvent() {
		MLGPlugin.instance.getServer().getPluginManager().registerEvents(this, MLGPlugin.instance);
	}
	
}
