package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherStalking {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final WatcherVisuals visuals;
   private final Random random = ThreadLocalRandom.current();
   private BukkitRunnable stalkTask = null;
   private UUID currentStalkTarget = null;
   private long stalkStartTime = 0L;
   private long nextStalkTick = 0L;
   private boolean isStalking = false;
   private final List<String> STALK_MESSAGES = Arrays.asList(
      "§8§oEtwas bewegt sich im Nebel...",
      "§8§oDu spürst, dass jemand dich beobachtet.",
      "§8§oDie Silhouette am Horizont... war die deine?",
      "§8§oEin Schatten bewegt sich zwischen den Bäumen.",
      "§8§oWenn du dich umdrehst... ist er schon weg.",
      "§8§oDer Nebel birgt mehr als nur Feuchtigkeit.",
      "§8§oSchritte. Nicht deine.",
      "§8§oWeiße Augen im Dunkeln. Du hattest sie gesehen."
   );

   public WatcherStalking(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
      this.visuals = core.getVisuals();
   }

   public void start() {
      CoreBootstrap.PLUGIN.getLogger().info("[WatcherStalking] Disabled — not starting.");
   }

   public void stop() {
      if (this.stalkTask != null) {
         this.stalkTask.cancel();
         this.stalkTask = null;
      }

      this.endStalk();
   }

   private void tryBeginStalk() {
      Player target = this.pickStalkTarget();
      if (target != null) {
         World world = target.getWorld();
         boolean isDark = world.getTime() > 13000L || world.getTime() < 1000L;
         boolean isRaining = world.hasStorm();
         boolean isInDark = target.getLocation().getBlock().getLightLevel() < 7;
         boolean requireDark = this.core.getConfig().isStalkingRequireDark();
         boolean requireRain = this.core.getConfig().isStalkingRequireRain();
         if (!requireDark || isDark || isInDark) {
            if (!requireRain || isRaining) {
               int chance = this.core.getConfig().getStalkingChance();
               if (this.random.nextInt(100) >= chance) {
                  this.nextStalkTick = (long)(600 + this.random.nextInt(600));
               } else {
                  this.beginStalk(target);
               }
            }
         }
      }
   }

   public void beginStalk(Player target) {
      if (target.isOnline()) {
         this.currentStalkTarget = target.getUniqueId();
         this.stalkStartTime = System.currentTimeMillis();
         this.isStalking = true;
         Location stalkPos = this.calculateStalkPosition(target);
         if (stalkPos == null) {
            this.endStalk();
         } else {
            this.bot.teleport(stalkPos);
            this.bot.lookAt(target.getEyeLocation());
            this.bot.unvanish();
            this.visuals.setupHerobrineEyes();
            int msgChance = this.core.getConfig().getStalkingConfig().getInt("message_chance", 40);
            if (this.random.nextInt(100) < msgChance) {
               List<String> msgs = this.core.getConfig().getStalkingMessages();
               if (msgs.isEmpty()) {
                  msgs = this.STALK_MESSAGES;
               }

               target.sendMessage(msgs.get(this.random.nextInt(msgs.size())));
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Stalking] Now stalking " + target.getName());
         }
      }
   }

   private void tickStalk() {
      Player target = Bukkit.getPlayer(this.currentStalkTarget);
      if (target != null && target.isOnline()) {
         Location botLoc = this.bot.getLocation();
         Location targetLoc = target.getLocation();
         if (botLoc == null) {
            this.endStalk();
         } else {
            double distance = botLoc.distance(targetLoc);
            long elapsed = System.currentTimeMillis() - this.stalkStartTime;
            if (this.isPlayerLookingAt(target, botLoc)) {
               this.endStalk();
            } else {
               int durMin = this.core.getConfig().getStalkingDurationMin() * 1000;
               int durMax = this.core.getConfig().getStalkingDurationMax() * 1000;
               if (elapsed > (long)(durMin + this.random.nextInt(durMax - durMin))) {
                  if (this.random.nextDouble() < 0.3) {
                     this.creepCloser(target);
                  } else {
                     this.endStalk();
                  }
               } else {
                  if (this.random.nextDouble() < 0.15) {
                     this.bot.lookAt(target.getEyeLocation());
                  }

                  if (distance > 50.0) {
                     Location newPos = this.calculateStalkPosition(target);
                     if (newPos != null) {
                        this.bot.teleport(newPos);
                     }
                  }
               }
            }
         }
      } else {
         this.endStalk();
      }
   }

   private void creepCloser(Player target) {
      Location botLoc = this.bot.getLocation();
      Location targetLoc = target.getLocation();
      if (botLoc != null && targetLoc != null) {
         double dx = targetLoc.getX() - botLoc.getX();
         double dz = targetLoc.getZ() - botLoc.getZ();
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len != 0.0) {
            double moveDist = Math.min(3.0, len - 8.0);
            Location newLoc = botLoc.clone().add(dx / len * moveDist, 0.0, dz / len * moveDist);
            newLoc.setY(targetLoc.getY());
            this.bot.teleport(newLoc);
            this.bot.lookAt(target.getEyeLocation());
            if (moveDist < 15.0) {
               this.visuals.setupRedEyes();
            }
         }
      }
   }

   private void endStalk() {
      if (this.isStalking) {
         this.isStalking = false;
         this.currentStalkTarget = null;
         this.bot.vanish();
         this.visuals.setupHerobrineEyes();
         this.nextStalkTick = (long)(800 + this.random.nextInt(1200));
      }
   }

   private Location calculateStalkPosition(Player target) {
      Location targetLoc = target.getLocation();
      World world = targetLoc.getWorld();
      int minDist = this.core.getConfig().getStalkingDistanceMin();
      int maxDist = this.core.getConfig().getStalkingDistanceMax();

      for (int attempt = 0; attempt < 20; attempt++) {
         double angle = (double)(targetLoc.getYaw() + 180.0F) + (this.random.nextDouble() - 0.5) * 60.0;
         double rad = Math.toRadians(angle);
         double dist = (double)(minDist + this.random.nextInt(maxDist - minDist));
         double x = targetLoc.getX() + Math.cos(rad) * dist;
         double z = targetLoc.getZ() + Math.sin(rad) * dist;
         int bx = (int)x;
         int bz = (int)z;
         int by = world.getHighestBlockYAt(bx, bz);
         Location result = new Location(world, x, (double)(by + 1), z);
         if (result.getY() > targetLoc.getY() - 5.0 && result.getY() < targetLoc.getY() + 10.0) {
            return result;
         }
      }

      return null;
   }

   private boolean isPlayerLookingAt(Player player, Location targetLoc) {
      Location eyeLoc = player.getEyeLocation();
      float yaw = eyeLoc.getYaw();
      float pitch = eyeLoc.getPitch();
      double yawRad = Math.toRadians((double)yaw);
      double pitchRad = Math.toRadians((double)pitch);
      double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
      double dirY = -Math.sin(pitchRad);
      double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
      double toX = targetLoc.getX() - eyeLoc.getX();
      double toY = targetLoc.getY() - eyeLoc.getY();
      double toZ = targetLoc.getZ() - eyeLoc.getZ();
      double toLen = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
      if (toLen == 0.0) {
         return false;
      } else {
         toX /= toLen;
         toY /= toLen;
         toZ /= toLen;
         double dot = dirX * toX + dirY * toY + dirZ * toZ;
         return dot > 0.9 && toLen < 40.0;
      }
   }

   private Player pickStalkTarget() {
      List<Player> candidates = new ArrayList<>();
      Player botPlayer = this.bot.getBotPlayer();

      for (Player p : Bukkit.getOnlinePlayers()) {
         if (!p.equals(botPlayer) && p.getLocation().getBlock().getLightLevel() < 8) {
            candidates.add(p);
         }
      }

      if (candidates.isEmpty()) {
         for (Player px : Bukkit.getOnlinePlayers()) {
            if (!px.equals(botPlayer)) {
               candidates.add(px);
            }
         }
      }

      return candidates.isEmpty() ? null : candidates.get(this.random.nextInt(candidates.size()));
   }

   public boolean isStalking() {
      return this.isStalking;
   }

   public UUID getCurrentStalkTarget() {
      return this.currentStalkTarget;
   }
}
