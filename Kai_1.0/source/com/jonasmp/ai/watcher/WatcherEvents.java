package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WatcherEvents implements Listener {
   private final WatcherCore core;
   private AntiCheatBridge antiCheatBridge = null;
   private final Map<UUID, WatcherEvents.OreTracker> oreTrackers = new HashMap<>();
   private static final Material[] TRACKED_ORES = new Material[]{
      Material.DIAMOND_ORE,
      Material.DEEPSLATE_DIAMOND_ORE,
      Material.EMERALD_ORE,
      Material.DEEPSLATE_EMERALD_ORE,
      Material.ANCIENT_DEBRIS,
      Material.GOLD_ORE,
      Material.DEEPSLATE_GOLD_ORE,
      Material.IRON_ORE,
      Material.DEEPSLATE_IRON_ORE
   };
   private final Map<UUID, Location> lastLocations = new HashMap<>();
   private final Map<UUID, Long> lastMoveTimes = new HashMap<>();

   public WatcherEvents(WatcherCore core) {
      this.core = core;
   }

   public void register() {
      Bukkit.getPluginManager().registerEvents(this, CoreBootstrap.PLUGIN);
      (this.antiCheatBridge = new AntiCheatBridge(this.core)).register();
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player joined = event.getPlayer();
      if (this.core.getBot() != null && this.core.getBot().isSpawned()) {
         Player botPlayer = this.core.getBot().getBotPlayer();
         if (botPlayer != null && botPlayer.isOnline()) {
            try {
               joined.showPlayer(CoreBootstrap.PLUGIN, botPlayer);
            } catch (Exception var5) {
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      String type = block.getType().name();
      WatcherObserver observer = this.core.getObserver();
      if (observer != null) {
         observer.onBlockBreak(player, type);
      }

      for (Material ore : TRACKED_ORES) {
         if (block.getType() == ore) {
            this.trackOreMine(player, ore.name());
            break;
         }
      }
   }

   private void trackOreMine(Player player, String oreType) {
      WatcherEvents.OreTracker tracker = this.oreTrackers.computeIfAbsent(player.getUniqueId(), k -> new WatcherEvents.OreTracker());
      tracker.addMine(oreType);
      int recentCount = tracker.getRecentCount(oreType, 60000L);
      if (recentCount >= 8) {
         WatcherObserver observer = this.core.getObserver();
         if (observer != null) {
            observer.onXrayIndicator(player, oreType, recentCount);
         }

         tracker.reset(oreType);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      String type = event.getBlock().getType().name();
      WatcherObserver observer = this.core.getObserver();
      if (observer != null) {
         observer.onBlockPlace(player, type);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (event.getPlayer() instanceof Player player) {
         String holderName = event.getInventory().getHolder() != null ? event.getInventory().getHolder().getClass().getSimpleName() : "Unknown";
         if (holderName.contains("Container") || holderName.contains("Chest") || holderName.contains("Shulker")) {
            WatcherObserver observer = this.core.getObserver();
            if (observer != null) {
               observer.onContainerOpen(player, holderName);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onCombat(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player attacker) {
         if (event.getEntity() instanceof Player victim) {
            WatcherObserver observer = this.core.getObserver();
            if (observer != null) {
               observer.onPlayerCombat(attacker, victim, event.getFinalDamage());
            }

            this.core.ensurePlayerData(victim.getUniqueId(), victim.getName()).addObservationRecord("combat_victim", "Hit by " + attacker.getName(), 2);
            AIPlayerBot aiBot = this.core.getAIPlayerBot();
            if (aiBot != null && aiBot.isSpawned()) {
               Player botPlayer = aiBot.getNMSBot().getPlayer();
               if (botPlayer != null && botPlayer.equals(victim)) {
                  aiBot.recordPlayerDamage(attacker);
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (!player.isFlying() && !player.getGameMode().name().contains("SPECTATOR")) {
         UUID uuid = player.getUniqueId();
         Location to = event.getTo();
         Location from = this.lastLocations.get(uuid);
         long now = System.currentTimeMillis();
         long lastTime = this.lastMoveTimes.getOrDefault(uuid, 0L);
         if (from != null && to != null && now - lastTime > 100L) {
            double dist = from.distance(to);
            double timeSec = (double)(now - lastTime) / 1000.0;
            double speed = dist / timeSec;
            if (player.getVehicle() == null && !player.isGliding()) {
               if (speed > 12.0) {
                  WatcherObserver observer = this.core.getObserver();
                  if (observer != null) {
                     observer.onSuspiciousMovement(player, "Extreme speed: " + String.format("%.1f", speed) + " blocks/sec");
                  }
               } else if (speed > 8.0 && from.getY() == to.getY()) {
                  this.core.ensurePlayerData(uuid, player.getName()).addObservationRecord("movement_fast", "Speed=" + String.format("%.1f", speed), 2);
               }
            }
         }

         this.lastLocations.put(uuid, to.clone());
         this.lastMoveTimes.put(uuid, now);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onInteract(PlayerInteractEvent event) {
      if (event.getClickedBlock() != null) {
         Player player = event.getPlayer();
         Material type = event.getClickedBlock().getType();
         if (type == Material.LEVER || type.name().contains("BUTTON") || type.name().contains("PRESSURE_PLATE")) {
            this.core.ensurePlayerData(player.getUniqueId(), player.getName()).addObservationRecord("interaction", "Used " + type.name(), 1);
         }
      }
   }

   private static class OreTracker {
      private final Map<String, List<Long>> mines = new HashMap<>();

      void addMine(String oreType) {
         this.mines.computeIfAbsent(oreType, k -> new ArrayList<>()).add(System.currentTimeMillis());
      }

      int getRecentCount(String oreType, long windowMs) {
         List<Long> times = this.mines.get(oreType);
         if (times == null) {
            return 0;
         } else {
            long cutoff = System.currentTimeMillis() - windowMs;
            times.removeIf(t -> t < cutoff);
            return times.size();
         }
      }

      void reset(String oreType) {
         this.mines.remove(oreType);
      }
   }
}
