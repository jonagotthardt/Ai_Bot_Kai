package com.jonasmp.ai.watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class BotMemory {
   public String botName = "";
   public UUID botUuid;
   public long createdAt = System.currentTimeMillis();
   public long lastSaved = 0L;
   public int totalDeaths = 0;
   public int totalMobKills = 0;
   public int totalPlayerKills = 0;
   public int blocksMined = 0;
   public int blocksPlaced = 0;
   public int itemsPickedUp = 0;
   public String lastDeathCause = "";
   public double lastDeathX = 0.0;
   public double lastDeathY = 0.0;
   public double lastDeathZ = 0.0;
   public long lastDeathTime = 0L;
   public List<BotMemory.DeathRecord> deathHistory = new ArrayList<>();
   public Map<String, Integer> killCounts = new HashMap<>();
   public List<BotMemory.ResourceLocation> resourceFinds = new ArrayList<>();
   public List<BotMemory.DangerZone> dangerZones = new ArrayList<>();
   public List<BotMemory.SafeSpot> safeSpots = new ArrayList<>();
   public int[] deathsByHour = new int[24];
   public Map<String, Integer> deathsByBiome = new HashMap<>();
   public Map<String, Integer> weaponKills = new HashMap<>();
   public Map<String, Integer> foodEaten = new HashMap<>();
   public boolean learnedShelterAtNight = false;
   public boolean learnedDontFightLowHealth = false;
   public boolean learnedRunFromCreepers = false;
   public boolean learnedCarryFood = false;
   public boolean learnedDigForSafety = false;
   public int survivalSkill = 0;
   public long longestSurvivalMinutes = 0L;
   public long currentSpawnTime = 0L;
   public List<BotMemory.PlayerInstruction> playerInstructions = new ArrayList<>();
   public List<BotMemory.LearnedLesson> learnedLessons = new ArrayList<>();
   public transient String lastActionDescription = "";
   public transient long lastActionTime = 0L;
   public transient String environmentAhead;

   public BotMemory() {
   }

   public BotMemory(UUID uuid) {
      this.botUuid = uuid;
   }

   public void recordDeath(String cause, Location loc, long time) {
      this.totalDeaths++;
      this.lastDeathCause = cause;
      this.lastDeathX = loc.getX();
      this.lastDeathY = loc.getY();
      this.lastDeathZ = loc.getZ();
      this.lastDeathTime = time;
      BotMemory.DeathRecord dr = new BotMemory.DeathRecord();
      dr.cause = cause;
      dr.x = loc.getX();
      dr.y = loc.getY();
      dr.z = loc.getZ();
      dr.world = loc.getWorld().getName();
      dr.time = time;
      dr.hour = (int)((loc.getWorld().getTime() / 1000L + 6L) % 24L);
      this.deathHistory.add(dr);
      if (this.deathHistory.size() > 50) {
         this.deathHistory.remove(0);
      }

      int[] deathsByHour = this.deathsByHour;
      int hour = dr.hour;
      deathsByHour[hour]++;
      String biome = loc.getBlock().getBiome().toString();
      this.deathsByBiome.merge(biome, 1, Integer::sum);
      if (this.totalDeaths >= 2 && this.deathsByHour[dr.hour] >= 2) {
         this.learnedShelterAtNight = true;
      }

      if (cause.toLowerCase().contains("creeper")) {
         this.learnedRunFromCreepers = true;
      }

      if (cause.toLowerCase().contains("zombie") || cause.toLowerCase().contains("skeleton")) {
         this.learnedDontFightLowHealth = true;
      }

      this.survivalSkill = Math.max(0, this.survivalSkill - 5);
      this.currentSpawnTime = time;
   }

   public void recordKill(String entityType, String weapon) {
      this.totalMobKills++;
      this.killCounts.merge(entityType, 1, Integer::sum);
      if (weapon != null) {
         this.weaponKills.merge(weapon, 1, Integer::sum);
      }

      this.survivalSkill++;
   }

   public void recordResourceFound(String type, Location loc) {
      BotMemory.ResourceLocation rl = new BotMemory.ResourceLocation();
      rl.type = type;
      rl.x = loc.getX();
      rl.y = loc.getY();
      rl.z = loc.getZ();
      rl.world = loc.getWorld().getName();
      rl.time = System.currentTimeMillis();
      this.resourceFinds.add(rl);
      if (this.resourceFinds.size() > 100) {
         this.resourceFinds.remove(0);
      }
   }

   public void recordDanger(Location loc, String reason) {
      BotMemory.DangerZone dz = new BotMemory.DangerZone();
      dz.x = loc.getX();
      dz.y = loc.getY();
      dz.z = loc.getZ();
      dz.world = loc.getWorld().getName();
      dz.reason = reason;
      dz.time = System.currentTimeMillis();
      this.dangerZones.add(dz);
      if (this.dangerZones.size() > 30) {
         this.dangerZones.remove(0);
      }
   }

   public void recordSafeSpot(Location loc) {
      BotMemory.SafeSpot ss = new BotMemory.SafeSpot();
      ss.x = loc.getX();
      ss.y = loc.getY();
      ss.z = loc.getZ();
      ss.world = loc.getWorld().getName();
      ss.time = System.currentTimeMillis();
      this.safeSpots.add(ss);
      if (this.safeSpots.size() > 20) {
         this.safeSpots.remove(0);
      }

      this.learnedDigForSafety = true;
   }

   public void recordBlockMined() {
      this.blocksMined++;
      this.survivalSkill++;
   }

   public void recordBlockPlaced() {
      this.blocksPlaced++;
   }

   public void recordItemPickedUp() {
      this.itemsPickedUp++;
   }

   public void recordFoodEaten(String food) {
      this.foodEaten.merge(food, 1, Integer::sum);
      this.learnedCarryFood = true;
   }

   public void updateSurvivalTime() {
      long mins = (System.currentTimeMillis() - this.currentSpawnTime) / 60000L;
      if (mins > this.longestSurvivalMinutes) {
         this.longestSurvivalMinutes = mins;
      }
   }

   public double getDangerLevel(int hour, String biome) {
      double danger = 0.0;
      if (hour >= 19 || hour <= 5) {
         danger += 0.3;
      }

      if (this.learnedShelterAtNight && (hour >= 19 || hour <= 5)) {
         danger += 0.2;
      }

      danger += Math.min((double)this.deathsByBiome.getOrDefault(biome, 0).intValue() * 0.1, 0.3);
      return Math.min(danger, 1.0);
   }

   public BotMemory.ResourceLocation findNearestResource(String type, Location from, double maxDist) {
      BotMemory.ResourceLocation nearest = null;
      double minDist = Double.MAX_VALUE;

      for (BotMemory.ResourceLocation rl : this.resourceFinds) {
         if (rl.type.equalsIgnoreCase(type) && rl.world.equals(from.getWorld().getName())) {
            double d = from.distance(new Location(from.getWorld(), rl.x, rl.y, rl.z));
            if (!(d >= minDist) && !(d >= maxDist)) {
               minDist = d;
               nearest = rl;
            }
         }
      }

      return nearest;
   }

   public Location findNearestSafeSpot(Location from, double maxDist) {
      BotMemory.SafeSpot nearest = null;
      double minDist = Double.MAX_VALUE;

      for (BotMemory.SafeSpot ss : this.safeSpots) {
         if (ss.world.equals(from.getWorld().getName())) {
            double d = from.distance(new Location(from.getWorld(), ss.x, ss.y, ss.z));
            if (!(d >= minDist) && !(d >= maxDist)) {
               minDist = d;
               nearest = ss;
            }
         }
      }

      return nearest != null ? new Location(from.getWorld(), nearest.x, nearest.y, nearest.z) : null;
   }

   public boolean isNearDanger(Location loc, double radius) {
      for (BotMemory.DangerZone dz : this.dangerZones) {
         if (dz.world.equals(loc.getWorld().getName())) {
            double d = loc.distance(new Location(loc.getWorld(), dz.x, dz.y, dz.z));
            if (d < radius) {
               return true;
            }
         }
      }

      return false;
   }

   public void recordPlayerInstruction(String playerName, String instruction, Location loc) {
      BotMemory.PlayerInstruction pi = new BotMemory.PlayerInstruction();
      pi.playerName = playerName;
      pi.instruction = instruction;
      pi.x = loc != null ? loc.getX() : 0.0;
      pi.y = loc != null ? loc.getY() : 0.0;
      pi.z = loc != null ? loc.getZ() : 0.0;
      pi.world = loc != null ? loc.getWorld().getName() : "";
      pi.time = System.currentTimeMillis();
      this.playerInstructions.add(pi);
      if (this.playerInstructions.size() > 50) {
         this.playerInstructions.remove(0);
      }
   }

   public void recordLesson(String topic, String whatWasWrong, String whatToDoInstead, String taughtBy) {
      BotMemory.LearnedLesson ll = new BotMemory.LearnedLesson();
      ll.topic = topic;
      ll.whatWasWrong = whatWasWrong;
      ll.whatToDoInstead = whatToDoInstead;
      ll.taughtBy = taughtBy;
      ll.time = System.currentTimeMillis();
      ll.timesReinforced = 1;
      this.learnedLessons.add(ll);
      if (this.learnedLessons.size() > 30) {
         this.learnedLessons.remove(0);
      }
   }

   public void reinforceLesson(String topic) {
      for (BotMemory.LearnedLesson ll : this.learnedLessons) {
         if (ll.topic.equalsIgnoreCase(topic)) {
            ll.timesReinforced++;
            ll.time = System.currentTimeMillis();
         }
      }
   }

   public boolean removeLesson(int index) {
      if (index >= 0 && index < this.learnedLessons.size()) {
         this.learnedLessons.remove(index);
         return true;
      } else {
         return false;
      }
   }

   public boolean removeInstruction(int index) {
      if (index >= 0 && index < this.playerInstructions.size()) {
         this.playerInstructions.remove(index);
         return true;
      } else {
         return false;
      }
   }

   public List<BotMemory.LearnedLesson> findLessons(String topic) {
      List<BotMemory.LearnedLesson> result = new ArrayList<>();

      for (BotMemory.LearnedLesson ll : this.learnedLessons) {
         if (ll.topic.toLowerCase().contains(topic.toLowerCase())) {
            result.add(ll);
         }
      }

      return result;
   }

   public static class DangerZone {
      public double x;
      public double y;
      public double z;
      public String world = "";
      public String reason = "";
      public long time = 0L;
   }

   public static class DeathRecord {
      public String cause = "";
      public double x;
      public double y;
      public double z;
      public String world = "";
      public long time = 0L;
      public int hour = 0;
   }

   public static class LearnedLesson {
      public String topic = "";
      public String whatWasWrong = "";
      public String whatToDoInstead = "";
      public String taughtBy = "";
      public long time = 0L;
      public int timesReinforced = 1;
   }

   public static class PlayerInstruction {
      public String playerName = "";
      public String instruction = "";
      public double x;
      public double y;
      public double z;
      public String world = "";
      public long time = 0L;
   }

   public static class ResourceLocation {
      public String type = "";
      public double x;
      public double y;
      public double z;
      public String world = "";
      public long time = 0L;
   }

   public static class SafeSpot {
      public double x;
      public double y;
      public double z;
      public String world = "";
      public long time = 0L;
   }
}
