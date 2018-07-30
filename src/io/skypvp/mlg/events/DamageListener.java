package io.skypvp.mlg.events;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import io.skypvp.mlg.MLGPlayer;
import io.skypvp.mlg.MLGUtils;

public class DamageListener extends MLGListener  {
	
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent evt) {
        Entity ent = evt.getEntity();

        if(ent instanceof Player) {
            final Player p = (Player) ent;
            MLGPlayer player = MLGPlayer.fromPlayer(p);
            if(evt.getDamage() != 0 && player.isInArena() && evt.getCause() == DamageCause.FALL) {
                new BukkitRunnable() {

                    public void run() {
                        Location pLoc = p.getLocation();
                        if(!MLGUtils.WATER_TYPES.contains(pLoc.getBlock().getType()))
                        	player.fail();
                        else
                        	player.succeed();
                    }

                }.runTaskLater(instance, 2L);
                evt.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent evt) {
        evt.setCancelled(true);
    }
	
}
