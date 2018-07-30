package io.skypvp.mlg.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;

public class WaterFlowListener extends MLGListener  {
	
    @EventHandler
    public void onWaterFlow(BlockFromToEvent evt) {
        Block block = evt.getToBlock();
        if(block.getType() == Material.WATER) {
            evt.setCancelled(true);
        }
    }
	
}
