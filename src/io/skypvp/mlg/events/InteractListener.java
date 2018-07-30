package io.skypvp.mlg.events;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.skypvp.mlg.MLGArena;
import io.skypvp.mlg.MLGPlayer;
import io.skypvp.mlg.MLGUtils;

public class InteractListener extends MLGListener  {

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent evt) {
		Player p = evt.getPlayer();
		ItemStack item = p.getItemInHand();
		MLGPlayer player = MLGPlayer.fromPlayer(p);
		if (MLGUtils.isSpecialBucket(item)
				&& Arrays.asList(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK).contains(evt.getAction())) {
			int height = MLGUtils.getSpecialBucketHeight(item);
			
			MLGArena arena = MLGArena.fromName("default");
			if (arena.areCornersSetup()) {
				player.enterArena(arena,height);
				p.sendMessage("                     ");
				p.sendMessage(ChatColor.GOLD + "Good luck!");
				p.sendMessage("                     ");
				p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1F, 1F);
			} else {
				instance.sendConsoleMessage(ChatColor.DARK_RED
						+ "Player attempted to play MLG while both corners of drop region have not been set!");
				p.sendMessage(ChatColor.RED + "An error occurred trying to execute that action. Contact admins.");
			}

			evt.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerClearBucket(final PlayerBucketEmptyEvent evt) {
		final Player p = evt.getPlayer();
		MLGPlayer player = MLGPlayer.fromPlayer(p);
		if (player.isInArena()) {
			final Block clickedBlock = evt.getBlockClicked().getLocation().add(0, 1, 0).getBlock();
			Location loc = clickedBlock.getLocation();
			if (!player.getArena().isArenaFloor(loc)) {
				loc.getWorld().playEffect(loc, Effect.SMOKE, 20);
				loc.getWorld().playSound(loc, Sound.GHAST_FIREBALL, 1F, 1F);
				evt.setCancelled(true);
			}

			if (loc.getY() > settings.getFirstCorner().getY()) {
				p.sendMessage(ChatColor.RED + "You can't place blocks on the wall!");
				p.playSound(p.getLocation(), Sound.IRONGOLEM_HIT, 1F, 1F);
				player.reset();
				return;
			}

			new BukkitRunnable() {

				public void run() {
					if (MLGUtils.WATER_TYPES.contains(clickedBlock.getType())) {
						clickedBlock.setType(Material.AIR);
						ArrayList<Block> below = MLGUtils.getBlocksBelow(p);
						if (p.getLocation().getBlock() == clickedBlock || below.contains(clickedBlock)) {
							MLGPlayer.fromPlayer(p).succeed();
						}
					}
				}

			}.runTaskLater(instance, 5L);
		}
	}


}
