package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.core.LanguagePrefixManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WatcherBot {
   private final WatcherCore core;
   private final NMSBot nmsBot;
   private String botName = null;
   private boolean spawned = false;

   public WatcherBot(WatcherCore core) {
      this.core = core;
      this.nmsBot = new NMSBot();
   }

   public void spawn() {
      if (!this.spawned) {
         String desiredName = this.core.getConfig().getWatcherName();
         Location spawnLoc = ((World)Bukkit.getWorlds().get(0)).getSpawnLocation();
         this.botName = desiredName;
         this.spawned = true;
         LanguagePrefixManager.setBotName(desiredName);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Spawning NMS bot '" + desiredName + "' at " + spawnLoc);
         this.nmsBot.spawn(spawnLoc, desiredName);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, this::configureBot, 30L);
      }
   }

   public void despawn() {
      if (this.spawned) {
         this.nmsBot.despawn();
         this.spawned = false;
         this.botName = null;
      }
   }

   private void configureBot() {
      if (!this.nmsBot.isSpawned()) {
         CoreBootstrap.PLUGIN.getLogger().warning("[Watcher] Bot did not spawn successfully.");
         this.spawned = false;
      } else {
         Player bot = this.nmsBot.getPlayer();
         if (bot == null) {
            CoreBootstrap.PLUGIN.getLogger().warning("[Watcher] Bot player is null after spawn.");
         } else {
            try {
               bot.setCollidable(false);
            } catch (Exception var9) {
            }

            try {
               bot.setInvulnerable(true);
            } catch (Exception var8) {
            }

            try {
               bot.setSilent(true);
            } catch (Exception var7) {
            }

            try {
               bot.setSleepingIgnored(true);
            } catch (Exception var6) {
            }

            try {
               bot.setCanPickupItems(false);
            } catch (Exception var5) {
            }

            try {
               bot.removePotionEffect(PotionEffectType.INVISIBILITY);
            } catch (Exception var4) {
            }

            try {
               bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
            } catch (Exception var3) {
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Bot '" + this.botName + "' configured. UUID=" + bot.getUniqueId() + " | VISIBLE MODE | NMS");
         }
      }
   }

   public void vanish() {
      this.nmsBot.vanish();
      CoreBootstrap.PLUGIN.getLogger().fine("[Watcher] Bot vanished.");
   }

   public void unvanish() {
      this.nmsBot.unvanish();
      CoreBootstrap.PLUGIN.getLogger().fine("[Watcher] Bot unvanished (VISIBLE).");
   }

   public void teleport(Location loc) {
      this.nmsBot.teleport(loc);
   }

   public void lookAt(Location target) {
      this.nmsBot.lookAt(target);
   }

   public void smoothLookAt(Location target, float speed) {
      Player bot = this.getBotPlayer();
      if (bot != null && target != null) {
         Location botLoc = bot.getLocation();
         double dx = target.getX() - botLoc.getX();
         double dy = target.getY() - botLoc.getY();
         double dz = target.getZ() - botLoc.getZ();
         double distance = Math.sqrt(dx * dx + dz * dz);
         float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
         float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, distance)));
         float currentYaw = botLoc.getYaw();
         float currentPitch = botLoc.getPitch();
         float yawDiff = targetYaw - currentYaw;

         while (yawDiff > 180.0F) {
            yawDiff -= 360.0F;
         }

         while (yawDiff < -180.0F) {
            yawDiff += 360.0F;
         }

         float newYaw = currentYaw + yawDiff * speed;
         float newPitch = currentPitch + (targetPitch - currentPitch) * speed;
         bot.teleport(new Location(botLoc.getWorld(), botLoc.getX(), botLoc.getY(), botLoc.getZ(), newYaw, newPitch));
      }
   }

   public Location getLocation() {
      Player bot = this.getBotPlayer();
      return bot != null ? bot.getLocation() : null;
   }

   public Player getBotPlayer() {
      return this.nmsBot.getPlayer();
   }

   public boolean isSpawned() {
      return this.spawned && this.nmsBot.isSpawned();
   }

   public String getBotName() {
      return this.botName;
   }
}
