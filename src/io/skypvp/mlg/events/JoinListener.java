package io.skypvp.mlg.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.skypvp.mlg.MLGPlayer;

public class JoinListener extends MLGListener  {
	
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        final Player player = evt.getPlayer();
        
        MLGPlayer p = new MLGPlayer(player);
        MLGPlayer.getPlayermap().put(player.getUniqueId(), p);
        System.out.println("HELLO");
        new BukkitRunnable() {

            public void run() {
                p.giveSpecialBuckets();
                System.out.println("COOOOL");
            }
        }.runTaskLater(instance, 10L);
    }
}
