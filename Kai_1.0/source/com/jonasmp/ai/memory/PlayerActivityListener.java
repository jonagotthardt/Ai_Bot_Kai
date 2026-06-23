package com.jonasmp.ai.memory;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerActivityListener implements Listener {
   private final Map<UUID, Location> lastPositions = new HashMap<>();
   private final Map<UUID, Long> lastMoveCheck = new HashMap<>();
   private static final long MOVE_CHECK_INTERVAL_MS = 5000L;
   private final Map<UUID, Integer> recentOreBreaks = new HashMap<>();
   private final Map<UUID, Long> oreCheckWindow = new HashMap<>();
   private final Map<UUID, Deque<Long>> containerOpenTimes = new HashMap<>();
   private final Map<UUID, Deque<Long>> flyMoveTimes = new HashMap<>();
   private static final int CONTAINER_SPAM_THRESHOLD = 12;
   private static final long CONTAINER_SPAM_WINDOW_MS = 10000L;
   private static final int FLY_SUSPICION_TICKS = 15;
   private final Map<UUID, Long> lastLocationSnapshot = new HashMap<>();

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      CoreBootstrap.MEMORY_ENGINE.onJoin(p.getUniqueId());
      CoreBootstrap.MEMORY_ENGINE.logConnection(p.getUniqueId(), "JOIN", 0L);
      Location loc = p.getLocation();
      CoreBootstrap.MEMORY_ENGINE.snapshotLocation(p.getUniqueId(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      PlayerMemory mem = CoreBootstrap.MEMORY_ENGINE.get(p.getUniqueId());
      if (mem.sessionsPlayed == 1) {
         mem.recordMilestone("First join to the server");
      }

      CoreBootstrap.MEMORY_ENGINE.save(p.getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      CoreBootstrap.MEMORY_ENGINE.onQuit(p.getUniqueId());
      CoreBootstrap.MEMORY_ENGINE.logConnection(p.getUniqueId(), "QUIT", 0L);
      Location loc = p.getLocation();
      CoreBootstrap.MEMORY_ENGINE.snapshotLocation(p.getUniqueId(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onDeath(PlayerDeathEvent event) {
      Player p = event.getEntity();
      String cause = event.getDeathMessage() != null ? event.getDeathMessage() : "unknown";
      CoreBootstrap.MEMORY_ENGINE.onDeath(p.getUniqueId(), cause);
      Location loc = p.getLocation();
      CoreBootstrap.MEMORY_ENGINE.logAction(p.getUniqueId(), "DEATH", cause, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player p = event.getPlayer();
      String type = event.getBlock().getType().name();
      Location loc = event.getBlock().getLocation();
      CoreBootstrap.MEMORY_ENGINE.onBlockBroken(p.getUniqueId(), type);
      CoreBootstrap.MEMORY_ENGINE.logAction(p.getUniqueId(), "BLOCK_BREAK", type, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      String tool = p.getInventory().getItemInMainHand().getType().name();
      if (!tool.equals("AIR")) {
         CoreBootstrap.MEMORY_ENGINE.onItemUsed(p.getUniqueId(), tool);
      }

      if (type.contains("ORE") || type.contains("DIAMOND") || type.contains("EMERALD") || type.contains("ANCIENT")) {
         this.checkXraySuspicion(p.getUniqueId(), type, loc);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player p = event.getPlayer();
      String type = event.getBlock().getType().name();
      Location loc = event.getBlock().getLocation();
      CoreBootstrap.MEMORY_ENGINE.onBlockPlaced(p.getUniqueId(), type);
      CoreBootstrap.MEMORY_ENGINE.logAction(p.getUniqueId(), "BLOCK_PLACE", type, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      this.checkBurstPlace(p.getUniqueId());
   }

   private void checkXraySuspicion(UUID uuid, String type, Location loc) {
      long now = System.currentTimeMillis();
      Long windowStart = this.oreCheckWindow.get(uuid);
      if (windowStart != null && now - windowStart <= 60000L) {
         int count = this.recentOreBreaks.getOrDefault(uuid, 0) + 1;
         this.recentOreBreaks.put(uuid, count);
         if (count >= 15) {
            CoreBootstrap.MEMORY_ENGINE.addXrayFlag(uuid);
            CoreBootstrap.MEMORY_ENGINE
               .logSuspicious(uuid, "XRAY_PATTERN", "Broke " + count + " ores in 60s at " + loc.getWorld().getName() + " (possible X-Ray)", 6);
         }
      } else {
         this.oreCheckWindow.put(uuid, now);
         this.recentOreBreaks.put(uuid, 1);
      }
   }

   private void checkBurstPlace(UUID uuid) {
      PlayerMemory mem = CoreBootstrap.MEMORY_ENGINE.get(uuid);
      long now = System.currentTimeMillis();
      if (now - mem.lastBlockPlaceTime > 60000L) {
         mem.blocksInLastMinute = 1;
         mem.lastBlockPlaceTime = now;
      } else {
         mem.blocksInLastMinute++;
         if (mem.blocksInLastMinute >= 40) {
            CoreBootstrap.MEMORY_ENGINE.addNukerFlag(uuid);
            CoreBootstrap.MEMORY_ENGINE.logSuspicious(uuid, "BURST_PLACE", "Placed " + mem.blocksInLastMinute + " blocks in 60s (possible bot/nuker)", 5);
            mem.blocksInLastMinute = 0;
         }
      }
   }

   private void checkSpeedAndFly(Player p, UUID uuid, Location from, Location to) {
      if (from.getWorld().equals(to.getWorld())) {
         if (!p.isFlying() || !p.getAllowFlight()) {
            if (!p.isGliding()) {
               if (!p.isInsideVehicle()) {
                  double horizontalDist = Math.sqrt(Math.pow(to.getX() - from.getX(), 2.0) + Math.pow(to.getZ() - from.getZ(), 2.0));
                  double verticalDist = to.getY() - from.getY();
                  boolean onGround = p.isOnGround();
                  double speed = horizontalDist * 20.0;
                  if (speed > 12.0 && onGround) {
                     CoreBootstrap.MEMORY_ENGINE.addSpeedFlag(uuid);
                     CoreBootstrap.MEMORY_ENGINE.logSuspicious(uuid, "SPEED_HACK", "Speed " + String.format("%.1f", speed) + " b/s on ground", 7);
                  }

                  if (!onGround && horizontalDist > 0.2 && verticalDist > -0.5 && verticalDist < 0.5) {
                     Deque<Long> flyDeque = this.flyMoveTimes.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                     flyDeque.addLast(System.currentTimeMillis());

                     while (!flyDeque.isEmpty() && System.currentTimeMillis() - flyDeque.peekFirst() > 2000L) {
                        flyDeque.pollFirst();
                     }

                     if (flyDeque.size() >= 15) {
                        CoreBootstrap.MEMORY_ENGINE.addFlyFlag(uuid);
                        CoreBootstrap.MEMORY_ENGINE.logSuspicious(uuid, "FLY_HACK", "Sustained horizontal air movement without falling", 8);
                        flyDeque.clear();
                     }
                  }
               }
            }
         }
      }
   }

   private void checkContainerSpam(UUID uuid) {
      Deque<Long> deque = this.containerOpenTimes.computeIfAbsent(uuid, k -> new ArrayDeque<>());
      long now = System.currentTimeMillis();
      deque.addLast(now);

      while (!deque.isEmpty() && now - deque.peekFirst() > 10000L) {
         deque.pollFirst();
      }

      if (deque.size() >= 12) {
         CoreBootstrap.MEMORY_ENGINE.addContainerSpamFlag(uuid);
         CoreBootstrap.MEMORY_ENGINE.logSuspicious(uuid, "CONTAINER_SPAM", "Opened " + deque.size() + " containers in 10s", 5);
         deque.clear();
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityDeath(EntityDeathEvent event) {
      Entity killer = event.getEntity().getKiller();
      if (killer instanceof Player p) {
         String mobType = event.getEntityType().name();
         Location loc = event.getEntity().getLocation();
         CoreBootstrap.MEMORY_ENGINE.onMobKill(p.getUniqueId(), mobType);
         CoreBootstrap.MEMORY_ENGINE
            .logAction(p.getUniqueId(), "MOB_KILL", mobType, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onWorldChange(PlayerChangedWorldEvent event) {
      Player p = event.getPlayer();
      CoreBootstrap.MEMORY_ENGINE.onWorldChange(p.getUniqueId(), p.getWorld().getName());
      Location loc = p.getLocation();
      CoreBootstrap.MEMORY_ENGINE.snapshotLocation(p.getUniqueId(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onCraft(CraftItemEvent event) {
      if (event.getWhoClicked() instanceof Player p) {
         if (event.getRecipe() != null && event.getRecipe().getResult() != null) {
            String type = event.getRecipe().getResult().getType().name();
            CoreBootstrap.MEMORY_ENGINE.onItemCrafted(p.getUniqueId(), type);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onMove(PlayerMoveEvent event) {
      if (event.getFrom().getBlockX() != event.getTo().getBlockX()
         || event.getFrom().getBlockY() != event.getTo().getBlockY()
         || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
         Player p = event.getPlayer();
         UUID uuid = p.getUniqueId();
         long now = System.currentTimeMillis();
         Long lastCheck = this.lastMoveCheck.get(uuid);
         if (lastCheck == null || now - lastCheck >= 5000L) {
            Location last = this.lastPositions.get(uuid);
            if (last != null && last.getWorld().equals(event.getTo().getWorld())) {
               double dist = last.distance(event.getTo());
               boolean flying = p.isFlying();
               if (dist > 1.0) {
                  CoreBootstrap.MEMORY_ENGINE.onDistanceTraveled(uuid, (long)dist, flying);
               }
            }

            this.lastPositions.put(uuid, event.getTo().clone());
            this.lastMoveCheck.put(uuid, now);
         }

         Long lastSnap = this.lastLocationSnapshot.get(uuid);
         if (lastSnap == null || now - lastSnap >= 30000L) {
            Location loc = event.getTo();
            CoreBootstrap.MEMORY_ENGINE.snapshotLocation(uuid, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            this.lastLocationSnapshot.put(uuid, now);
         }

         this.checkSpeedAndFly(p, uuid, event.getFrom(), event.getTo());
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (event.getPlayer() instanceof Player p) {
         String type = event.getInventory().getType().name();
         Location loc = null;
         if (event.getInventory().getLocation() != null) {
            loc = event.getInventory().getLocation();
         }

         CoreBootstrap.MEMORY_ENGINE.logContainerAccess(p.getUniqueId(), type);
         if (loc != null) {
            CoreBootstrap.MEMORY_ENGINE
               .logAction(p.getUniqueId(), "CONTAINER_OPEN", type, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
         }

         this.checkContainerSpam(p.getUniqueId());
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInteract(PlayerInteractEvent event) {
      Player p = event.getPlayer();
      String handItem = p.getInventory().getItemInMainHand().getType().name();
      if (!handItem.equals("AIR") && !handItem.contains("BLOCK")) {
         CoreBootstrap.MEMORY_ENGINE.onItemUsed(p.getUniqueId(), handItem);
      }

      if (event.getClickedBlock() != null) {
         String type = event.getClickedBlock().getType().name();
         Location loc = event.getClickedBlock().getLocation();
         if (type.contains("CHEST") || type.contains("BARREL") || type.contains("SHULKER") || type.contains("HOPPER")) {
            CoreBootstrap.MEMORY_ENGINE.logContainerAccess(p.getUniqueId(), type);
            CoreBootstrap.MEMORY_ENGINE
               .logAction(p.getUniqueId(), "CONTAINER_INTERACT", type, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
         }
      }
   }
}
