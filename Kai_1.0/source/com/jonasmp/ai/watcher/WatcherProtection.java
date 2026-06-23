package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;

public class WatcherProtection implements Listener {
   private final WatcherCore core;

   public WatcherProtection(WatcherCore core) {
      this.core = core;
   }

   public void register() {
      Bukkit.getPluginManager().registerEvents(this, CoreBootstrap.PLUGIN);
   }

   private boolean isWatcher(Entity entity) {
      if (entity instanceof Player player) {
         String watcherName = this.core.getConfig().getWatcherName();
         return player.getName().equals(watcherName);
      } else {
         return false;
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onAnyDamage(EntityDamageEvent event) {
      if (this.isWatcher(event.getEntity())) {
         event.setCancelled(true);
         event.setDamage(0.0);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (this.isWatcher(event.getEntity())) {
         event.setCancelled(true);
         event.setDamage(0.0);
         Entity damager = event.getDamager();
         Player attacker = null;
         if (damager instanceof Player player2) {
            attacker = player2;
         } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player player) {
            attacker = player;
         }

         if (attacker != null && attacker.isOnline()) {
            attacker.sendMessage("§8[§4Watcher§8] §cYou cannot attack the Watcher.");
         }
      } else {
         if (this.isWatcher(event.getDamager())) {
            event.setCancelled(true);
            event.setDamage(0.0);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onEntityTarget(EntityTargetEvent event) {
      if (this.isWatcher(event.getTarget())) {
         event.setCancelled(true);
         event.setTarget((Entity)null);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onPotionSplash(PotionSplashEvent event) {
      for (Entity entity : event.getAffectedEntities()) {
         if (this.isWatcher(entity)) {
            event.setIntensity((LivingEntity)entity, 0.0);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInteractEntity(PlayerInteractEntityEvent event) {
      if (this.isWatcher(event.getRightClicked())) {
         event.setCancelled(true);
         Player player = event.getPlayer();
         if (player.isOnline()) {
            player.sendMessage("§8[§4Watcher§8] §cYou cannot interact with the Watcher.");
         }
      }
   }

   public void zeroVelocityIfWatcher(Player player) {
      if (this.isWatcher(player)) {
         player.setVelocity(Vector.getRandom().multiply(0));
      }
   }
}
