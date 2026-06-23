package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WatcherFreeze implements Listener {
   private final WatcherCore core;
   private final Map<UUID, WatcherFreeze.FreezeSession> frozenPlayers = new HashMap<>();
   private boolean listenerRegistered = false;

   public WatcherFreeze(WatcherCore core) {
      this.core = core;
   }

   public void freeze(UUID uuid) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
         int duration = this.core.getConfig().getFreezeDurationSeconds();
         long expiresAt = System.currentTimeMillis() + (long)duration * 1000L;
         Location lockLoc = player.getLocation().clone();
         this.frozenPlayers.put(uuid, new WatcherFreeze.FreezeSession(lockLoc, expiresAt));
         String msg = this.core.getConfig().getFreezeMessage();
         player.sendTitle("§c§lFREEZE", "§7" + msg.replace("§c", "").replace("§7", ""), 10, duration * 20, 20);
         player.sendMessage(msg);
         this.ensureListener();
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Froze " + player.getName() + " for " + duration);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> this.unfreeze(uuid), (long)duration * 20L);
      }
   }

   public void unfreeze(UUID uuid) {
      WatcherFreeze.FreezeSession session = this.frozenPlayers.remove(uuid);
      if (session != null) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null && player.isOnline()) {
            player.sendTitle("", "", 0, 0, 0);
            player.sendMessage("§a[Watcher] Freeze lifted. You may move again.");
         }

         if (this.frozenPlayers.isEmpty() && this.listenerRegistered) {
            HandlerList.unregisterAll(this);
            this.listenerRegistered = false;
         }

         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Unfroze " + (player != null ? player.getName() : uuid));
      }
   }

   public void unfreezeAll() {
      for (UUID uuid : new HashMap<>(this.frozenPlayers).keySet()) {
         this.unfreeze(uuid);
      }
   }

   public void tick(UUID uuid) {
      WatcherFreeze.FreezeSession session = this.frozenPlayers.get(uuid);
      if (session != null) {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null && player.isOnline()) {
            Location current = player.getLocation();
            Location lock = session.lockLocation;
            if (current.getX() != lock.getX() || current.getZ() != lock.getZ()) {
               player.teleport(lock);
            }
         }
      }
   }

   public boolean isFrozen(UUID uuid) {
      return this.frozenPlayers.containsKey(uuid);
   }

   private void ensureListener() {
      if (!this.listenerRegistered) {
         Bukkit.getPluginManager().registerEvents(this, CoreBootstrap.PLUGIN);
         this.listenerRegistered = true;
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onMove(PlayerMoveEvent event) {
      UUID uuid = event.getPlayer().getUniqueId();
      if (this.frozenPlayers.containsKey(uuid)) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (to != null) {
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockBreak(BlockBreakEvent event) {
      if (this.frozenPlayers.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      if (this.frozenPlayers.containsKey(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onCombat(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         if (this.frozenPlayers.containsKey(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
         }
      }
   }

   private static class FreezeSession {
      final Location lockLocation;
      final long expiresAt;

      FreezeSession(Location lockLocation, long expiresAt) {
         this.lockLocation = lockLocation;
         this.expiresAt = expiresAt;
      }
   }
}
