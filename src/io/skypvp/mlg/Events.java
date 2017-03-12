package io.skypvp.mlg;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import net.minecraft.server.v1_7_R4.EntityPlayer;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Events implements Listener {
	
	final MLGArena instance;
	final Settings settings;
	
	public Events(MLGArena main) {
		this.instance = main;
		this.settings = instance.getSettings();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt) {
		final Player p = evt.getPlayer();
		
		// Let's setup the MySQL stats
		MLGUtils.handlePlayerEntrance(p, instance);
		
		new BukkitRunnable() {
			
			public void run() {
				MLGUtils.giveSpecialBuckets(p);
			}
			
		}.runTaskLater(instance, 10L);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt) {
		final Player p = evt.getPlayer();
		
		new BukkitRunnable() {
			
			public void run() {
				settings.getDatabase().handlePlayerExit(p.getUniqueId());
			}
			
		}.runTaskAsynchronously(instance);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent evt) {
		Player p = evt.getPlayer();
		ItemStack item = p.getItemInHand();
		
		if(MLGUtils.isSpecialBucket(item) && Arrays.asList(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK).contains(evt.getAction())) {
			int height = MLGUtils.getSpecialBucketHeight(item);
			
			if(MLGUtils.areCornersSetup(instance)) {
				MLGUtils.enterArena(instance, p, height);
				p.sendMessage("                     ");
				p.sendMessage(ChatColor.GOLD + "Good luck!");
				p.sendMessage("                     ");
				p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1F, 1F);
			}else {
				instance.sendConsoleMessage(ChatColor.DARK_RED + "Player attempted to play MLG while both corners of drop region have not been set!");
				p.sendMessage(ChatColor.RED + "An error occurred trying to execute that action. Contact admins.");
			}
			
			evt.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerClearBucket(final PlayerBucketEmptyEvent evt) {
		final Player p = evt.getPlayer();
		if(instance.isInArena(p.getUniqueId())) {
			final Block clickedBlock = evt.getBlockClicked().getLocation().add(0, 1, 0).getBlock();
			Location loc = clickedBlock.getLocation();
			if(!MLGUtils.isArenaFloor(instance, loc)) {
				loc.getWorld().playEffect(loc, Effect.SMOKE, 20);
				loc.getWorld().playSound(loc, Sound.GHAST_FIREBALL, 1F, 1F);
				evt.setCancelled(true);
			}
			
			if(loc.getY() > settings.getFirstCorner().getY()) {
				p.sendMessage(ChatColor.RED + "You can't place blocks on the wall!");
				p.playSound(p.getLocation(), Sound.IRONGOLEM_HIT, 1F, 1F);
				instance.resetPlayer(p);
				return;
			}
			
			new BukkitRunnable() {
				
				public void run() {
					if(MLGUtils.WATER_TYPES.contains(clickedBlock.getType())) {
						clickedBlock.setType(Material.AIR);
						ArrayList<Block> below = getBlocksBelow(p);
						if(p.getLocation().getBlock() == clickedBlock || below.contains(clickedBlock)) {
							MLGUtils.success(instance, p);
						}
					}
				}
				
			}.runTaskLater(instance, 5L);
		}
	}
	
	@EventHandler
	public void onWaterFlow(BlockFromToEvent evt) {
		Block block = evt.getToBlock();
		if(block.getType() == Material.WATER) {
			evt.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent evt) {
		Block b = evt.getBlock();
		if(b.getType() == Material.STATIONARY_WATER) {
			evt.setCancelled(true);
		}
	}
	
	public ArrayList<Block> getBlocksBelow(Player player) {
	    ArrayList<Block> blocksBelow = new ArrayList<Block>();
	    EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
	    AxisAlignedBB boundingBox = entityPlayer.boundingBox;
	    World world = player.getWorld();
	    double yBelow = player.getLocation().getY();
	    Block northEast = new Location(world, boundingBox.d, yBelow, boundingBox.c).getBlock();
	    Block northWest = new Location(world, boundingBox.a, yBelow, boundingBox.c).getBlock();
	    Block southEast = new Location(world, boundingBox.d, yBelow, boundingBox.f).getBlock();
	    Block southWest = new Location(world, boundingBox.a, yBelow, boundingBox.f).getBlock();
	    Block[] blocks = {northEast, northWest, southEast, southWest};
	    for (Block block : blocks) {
	        if (!blocksBelow.isEmpty()) {
	            boolean duplicateExists = false;
	            for (int i = 0; i < blocksBelow.size(); i++) {
	                if (blocksBelow.get(i).equals(block)) {
	                    duplicateExists = true;
	                }
	            }
	            if (!duplicateExists) {
	                blocksBelow.add(block);
	            }
	        } else {
	            blocksBelow.add(block);
	        }
	    }
	    return blocksBelow;
	}
	
	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent evt) {
		final Player p = evt.getPlayer();
		final Block toBlock = evt.getTo().getBlock();
		if(instance.isInArena(p.getUniqueId()) && toBlock.getLocation().getY() == instance.getSettings().getFirstCorner().getY()) {
			ArrayList<Block> below = getBlocksBelow(p);
			below.add(toBlock);
			for(Block b : below) {
				if(MLGUtils.WATER_TYPES.contains(b)) {
					MLGUtils.success(instance, p);
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent evt) {
		Entity ent = evt.getEntity();
		
		if(ent instanceof Player) {
			final Player p = (Player) ent;
			if(evt.getDamage() != 0 && instance.isInArena(p.getUniqueId()) && evt.getCause() == DamageCause.FALL) {
				new BukkitRunnable() {
					
					public void run() {
						Location pLoc = p.getLocation();
						if(!MLGUtils.WATER_TYPES.contains(pLoc.getBlock().getType())) {
							MLGUtils.failure(instance, p);
						}else {
							MLGUtils.success(instance, p);
						}
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
