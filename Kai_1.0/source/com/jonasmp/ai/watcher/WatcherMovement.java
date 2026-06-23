package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class WatcherMovement {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final Random random = ThreadLocalRandom.current();
   private final List<Location> patrolPoints = new ArrayList<>();
   private int currentPatrolIndex = 0;
   private long nextPatrolMove = 0L;
   private long nextLookChange = 0L;
   private long lastFollowUpdate = 0L;
   private Location lastTargetLoc = null;
   private double offsetX = 0.0;
   private double offsetZ = 0.0;
   private long offsetResetTime = 0L;

   public WatcherMovement(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
      this.loadPatrolPoints();
   }

   public void observe(Player target) {
      if (target != null && target.isOnline()) {
         long now = System.currentTimeMillis();
         if (now >= this.lastFollowUpdate + 250L) {
            this.lastFollowUpdate = now;
            Location targetLoc = target.getLocation();
            Location botLoc = this.bot.getLocation();
            if (botLoc != null) {
               double distance = botLoc.distance(targetLoc);
               int followDist = this.core.getConfig().getFollowDistance();
               if (distance > (double)(followDist + 10)) {
                  this.teleportNear(target, followDist);
               } else if (distance < 4.0) {
                  this.moveAway(target, 4.0);
               } else {
                  this.maintainDistance(target, followDist);
                  this.naturalLookAt(target);
               }
            }
         }
      }
   }

   public void investigate(Player target) {
      if (target != null && target.isOnline()) {
         long now = System.currentTimeMillis();
         if (now >= this.lastFollowUpdate + 200L) {
            this.lastFollowUpdate = now;
            Location targetLoc = target.getLocation();
            Location botLoc = this.bot.getLocation();
            if (botLoc != null) {
               double distance = botLoc.distance(targetLoc);
               int invDist = this.core.getConfig().getInvestigationDistance();
               if (distance > (double)(invDist + 8)) {
                  this.teleportNear(target, invDist);
               } else if (distance < 3.0) {
                  this.moveAway(target, 3.0);
               } else {
                  this.maintainDistance(target, invDist);
               }

               this.bot.smoothLookAt(target.getEyeLocation(), 0.4F);
            }
         }
      }
   }

   public void approach(Player target, double minDistance) {
      if (target != null && target.isOnline()) {
         long now = System.currentTimeMillis();
         if (now >= this.lastFollowUpdate + 800L) {
            this.lastFollowUpdate = now;
            Location targetLoc = target.getLocation();
            Location botLoc = this.bot.getLocation();
            if (botLoc != null) {
               double distance = botLoc.distance(targetLoc);
               if (distance <= minDistance + 1.0) {
                  this.bot.smoothLookAt(target.getEyeLocation(), 0.3F);
               } else {
                  double speed = 1.2;
                  double dx = targetLoc.getX() - botLoc.getX();
                  double dz = targetLoc.getZ() - botLoc.getZ();
                  double len = Math.sqrt(dx * dx + dz * dz);
                  if (len != 0.0) {
                     double nx = dx / len;
                     double nz = dz / len;
                     Location newLoc = botLoc.clone().add(nx * 1.2, 0.0, nz * 1.2);
                     newLoc.setY(botLoc.getY());
                     this.bot.teleport(newLoc);
                     this.bot.smoothLookAt(target.getEyeLocation(), 0.2F);
                  }
               }
            }
         }
      }
   }

   public void teleportNear(Player target, int distance) {
      if (target != null && target.isOnline()) {
         Location targetLoc = target.getLocation();
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         double dx = Math.cos(angle) * (double)distance;
         double dz = Math.sin(angle) * (double)distance;
         Location dest = targetLoc.clone().add(dx, 0.0, dz);
         dest.setY((double)(dest.getWorld().getHighestBlockYAt(dest) + 1));
         this.bot.teleport(dest);
         this.bot.lookAt(target.getEyeLocation());
      }
   }

   public void teleportTo(Location loc) {
      if (loc != null) {
         this.bot.teleport(loc);
      }
   }

   public void patrol() {
      BotGoalPlanner planner = this.core.getAIPlayerBot().getGoalPlanner();
      if (planner == null || planner.getCurrentGoal() == BotGoalPlanner.GoalType.IDLE) {
         if (this.patrolPoints.isEmpty()) {
            this.randomWander();
         } else {
            long now = System.currentTimeMillis();
            if (now >= this.nextPatrolMove) {
               Location target = this.patrolPoints.get(this.currentPatrolIndex);
               Location botLoc = this.bot.getLocation();
               if (botLoc != null) {
                  double distance = botLoc.distance(target);
                  if (distance < 3.0) {
                     this.currentPatrolIndex = (this.currentPatrolIndex + 1) % this.patrolPoints.size();
                     this.nextPatrolMove = now + (long)(1500 + this.random.nextInt(2500));
                  } else {
                     if (distance > 30.0) {
                        this.bot.teleport(target);
                     } else {
                        double dx = target.getX() - botLoc.getX();
                        double dz = target.getZ() - botLoc.getZ();
                        double len = Math.sqrt(dx * dx + dz * dz);
                        if (len > 0.0) {
                           double speed = Math.max(1.5, this.core.getConfig().getPatrolSpeed());
                           Location step = botLoc.clone().add(dx / len * speed, 0.0, dz / len * speed);
                           step.setY(botLoc.getY());
                           this.bot.teleport(step);
                        }
                     }

                     if (now > this.nextLookChange) {
                        this.randomHeadMovement();
                        this.nextLookChange = now + (long)(500 + this.random.nextInt(1500));
                     }
                  }
               }
            }
         }
      }
   }

   private void randomWander() {
      long now = System.currentTimeMillis();
      if (now >= this.nextPatrolMove) {
         this.nextPatrolMove = now + 3000L;
         Location botLoc = this.bot.getLocation();
         if (botLoc != null) {
            World world = botLoc.getWorld();
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            double dist = (double)(5 + this.random.nextInt(15));
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            Location dest = botLoc.clone().add(dx, 0.0, dz);
            dest.setY((double)(world.getHighestBlockYAt(dest) + 1));
            this.bot.teleport(dest);
            this.randomHeadMovement();
         }
      }
   }

   private void maintainDistance(Player target, int desiredDistance) {
      Location targetLoc = target.getLocation();
      Location botLoc = this.bot.getLocation();
      if (botLoc != null) {
         long now = System.currentTimeMillis();
         if (now > this.offsetResetTime) {
            this.offsetX = (this.random.nextDouble() - 0.5) * 4.0;
            this.offsetZ = (this.random.nextDouble() - 0.5) * 4.0;
            this.offsetResetTime = now + 3000L + (long)this.random.nextInt(3000);
         }

         double angle = Math.atan2(targetLoc.getZ() - botLoc.getZ(), targetLoc.getX() - botLoc.getX());
         double destX = targetLoc.getX() - Math.cos(angle) * (double)desiredDistance + this.offsetX;
         double destZ = targetLoc.getZ() - Math.sin(angle) * (double)desiredDistance + this.offsetZ;
         Location dest = new Location(botLoc.getWorld(), destX, botLoc.getY(), destZ);
         dest.setY((double)(dest.getWorld().getHighestBlockYAt(dest) + 1));
         if (botLoc.distance(dest) > 1.5) {
            double dx = dest.getX() - botLoc.getX();
            double dz = dest.getZ() - botLoc.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.0) {
               double speed = 0.8;
               Location step = botLoc.clone().add(dx / len * 0.8, 0.0, dz / len * 0.8);
               step.setY(botLoc.getY());
               this.bot.teleport(step);
            }
         }
      }
   }

   private void moveAway(Player target, double distance) {
      Location targetLoc = target.getLocation();
      Location botLoc = this.bot.getLocation();
      if (botLoc != null) {
         double dx = botLoc.getX() - targetLoc.getX();
         double dz = botLoc.getZ() - targetLoc.getZ();
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len != 0.0) {
            double speed = Math.max(distance, 2.0);
            Location dest = botLoc.clone().add(dx / len * speed, 0.0, dz / len * speed);
            dest.setY((double)(dest.getWorld().getHighestBlockYAt(dest) + 1));
            this.bot.teleport(dest);
         }
      }
   }

   private void naturalLookAt(Player target) {
      long now = System.currentTimeMillis();
      if (now >= this.nextLookChange) {
         this.nextLookChange = now + 500L + (long)this.random.nextInt(1500);
         if (this.random.nextDouble() < 0.7) {
            this.bot.smoothLookAt(target.getEyeLocation(), 0.15F);
         } else {
            this.randomHeadMovement();
         }
      }
   }

   private void randomHeadMovement() {
      Location botLoc = this.bot.getLocation();
      if (botLoc != null) {
         float yaw = this.random.nextFloat() * 360.0F - 180.0F;
         float pitch = this.random.nextFloat() * 60.0F - 30.0F;
         this.bot.teleport(new Location(botLoc.getWorld(), botLoc.getX(), botLoc.getY(), botLoc.getZ(), yaw, pitch));
      }
   }

   private void loadPatrolPoints() {
      for (YamlConfiguration cfg : this.core.getConfig().loadPatrolConfigs()) {
         if (cfg.contains("points")) {
            for (String key : cfg.getConfigurationSection("points").getKeys(false)) {
               String path = "points." + key;
               World world = Bukkit.getWorld(cfg.getString(path + ".world", "world"));
               if (world != null) {
                  double x = cfg.getDouble(path + ".x");
                  double y = cfg.getDouble(path + ".y");
                  double z = cfg.getDouble(path + ".z");
                  this.patrolPoints.add(new Location(world, x, y, z));
               }
            }
         }
      }

      if (!this.patrolPoints.isEmpty()) {
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Loaded " + this.patrolPoints.size() + " patrol points.");
      }
   }
}
