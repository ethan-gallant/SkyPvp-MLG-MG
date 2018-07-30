package io.skypvp.mlg.events;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import io.skypvp.mlg.MLGPlayer;
import io.skypvp.mlg.MLGUtils;

public class PlayerMoveListener extends MLGListener {

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent event) {
		final Player p = event.getPlayer();
		final Block toBlock = event.getTo().getBlock();
		MLGPlayer player = MLGPlayer.fromPlayer(p);
		if (player.isInArena() && toBlock.getLocation().getY() == instance.getSettings().getFirstCorner().getY()) {
			ArrayList<Block> below = MLGUtils.getBlocksBelow(p);
			below.add(toBlock);
			for (Block b : below) {
				if (MLGUtils.WATER_TYPES.contains(b.getType())) {
					player.succeed();
					break;
				}
			}
		}
	}

}
