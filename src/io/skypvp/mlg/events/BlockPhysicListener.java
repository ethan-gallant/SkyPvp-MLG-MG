package io.skypvp.mlg.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPhysicsEvent;

public class BlockPhysicListener extends MLGListener {	
    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent evt) {
        Block b = evt.getBlock();
        if(b.getType() == Material.STATIONARY_WATER) {
            evt.setCancelled(true);
        }
    }
	
}
