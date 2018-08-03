package io.skypvp.mlg.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.skypvp.mlg.MLGPlayer;

public class QuitListener extends MLGListener  {
	
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        final Player player = evt.getPlayer();

        MLGPlayer mp = MLGPlayer.fromPlayer(player);
        
        
        new BukkitRunnable() {

            public void run() {
                mp.syncStats();
                MLGPlayer.getPlayermap().remove(player.getUniqueId());
            }

        }.runTaskAsynchronously(instance);
    }
}
