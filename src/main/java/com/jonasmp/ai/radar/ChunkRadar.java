package com.jonasmp.ai.radar;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.bootstrap.LoadGovernor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ChunkRadar {
   private static final int RADAR_RADIUS_CHUNKS = 1;
   private static final long REFRESH_INTERVAL_TICKS = 100L;
   private final ChunkCache cache;
   private final ChunkFileReader fileReader;
   private final DeltaTracker deltaTracker;
   private final PersistentChunkStore persistentStore;
   private Player trackedBot = null;
   private int refreshTaskId = -1;

   public ChunkRadar() {
      this.cache = new ChunkCache();
      this.fileReader = new ChunkFileReader();
      this.persistentStore = new PersistentChunkStore();
      this.deltaTracker = new DeltaTracker();
      this.deltaTracker.setPersistentStore(this.persistentStore);
   }

   public void startRefreshTask(Player bot) {
      this.trackedBot = bot;
      if (this.refreshTaskId >= 0) {
         Bukkit.getScheduler().cancelTask(this.refreshTaskId);
      }

      this.refreshTaskId = (new BukkitRunnable() {
         {
            Objects.requireNonNull(ChunkRadar.this);
         }

         public void run() {
            if (ChunkRadar.this.trackedBot != null && ChunkRadar.this.trackedBot.isOnline()) {
               ChunkRadar.this.refreshAroundBot();
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(CoreBootstrap.PLUGIN, 20L, 100L).getTaskId();
      CoreBootstrap.PLUGIN.getLogger().info("[ChunkRadar] Main-thread refresh task started for " + bot.getName());
   }

   public void stopRefreshTask() {
      if (this.refreshTaskId >= 0) {
         Bukkit.getScheduler().cancelTask(this.refreshTaskId);
         this.refreshTaskId = -1;
      }

      this.persistentStore.flushAll();
      this.trackedBot = null;
   }

   public PersistentChunkStore getPersistentStore() {
      return this.persistentStore;
   }

   public Material getBlockType(int worldX, int worldY, int worldZ, String worldName) {
      Material delta = this.deltaTracker.getLiveMaterial(worldX, worldY, worldZ, worldName);
      if (delta != null) {
         return delta;
      } else {
         Material cached = this.cache.getBlockType(worldX, worldY, worldZ);
         if (cached != null) {
            return cached;
         }

         Material persisted = this.persistentStore.getMaterial(worldName, worldX, worldY, worldZ);
         if (persisted != null) {
            return persisted;
         }

         World world = Bukkit.getWorld(worldName);
         return world != null ? world.getBlockAt(worldX, worldY, worldZ).getType() : Material.AIR;
      }
   }

   public Material getBlockType(Location loc) {
      return this.getBlockType(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
   }

   public List<Location> scanFor(Player bot, Material target, int radiusBlocks) {
      List<Location> results = new ArrayList<>();
      Location center = bot.getLocation();
      World world = center.getWorld();
      int cx = center.getBlockX();
      int cy = center.getBlockY();
      int cz = center.getBlockZ();
      int radiusChunks = (radiusBlocks >> 4) + 1;
      int botChunkX = cx >> 4;
      int botChunkZ = cz >> 4;

      for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
         for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            int chunkX = botChunkX + dx;
            int chunkZ = botChunkZ + dz;
            if (this.cache.isCached(chunkX, chunkZ)) {
               for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                     int worldX = (chunkX << 4) + x;
                     int worldZ = (chunkZ << 4) + z;
                     if (Math.abs(worldX - cx) <= radiusBlocks && Math.abs(worldZ - cz) <= radiusBlocks) {
                        for (int y = Math.max(world.getMinHeight(), cy - 20); y <= Math.min(world.getMaxHeight(), cy + 20); y++) {
                           if (this.getBlockType(worldX, y, worldZ, world.getName()) == target) {
                              results.add(new Location(world, (double)worldX, (double)y, (double)worldZ));
                           }
                        }
                     }
                  }
               }
            } else {
               for (int[] pos : this.persistentStore.getPositions(world.getName(), chunkX, chunkZ, target)) {
                  if (Math.abs(pos[0] - cx) <= radiusBlocks && Math.abs(pos[2] - cz) <= radiusBlocks
                     && this.deltaTracker.getLiveMaterial(pos[0], pos[1], pos[2], world.getName()) == null) {
                     results.add(new Location(world, (double)pos[0], (double)pos[1], (double)pos[2]));
                  }
               }
            }
         }
      }

      results.sort((a, b) -> {
         double da = a.distanceSquared(center);
         double db = b.distanceSquared(center);
         return Double.compare(da, db);
      });
      return results;
   }

   public Location findNearestTree(Player bot, int radiusBlocks) {
      List<Location> logs = this.scanFor(bot, Material.OAK_LOG, radiusBlocks);
      if (logs.isEmpty()) {
         logs = this.scanFor(bot, Material.BIRCH_LOG, radiusBlocks);
      }

      if (logs.isEmpty()) {
         logs = this.scanFor(bot, Material.SPRUCE_LOG, radiusBlocks);
      }

      if (logs.isEmpty()) {
         logs = this.scanFor(bot, Material.JUNGLE_LOG, radiusBlocks);
      }

      if (logs.isEmpty()) {
         logs = this.scanFor(bot, Material.ACACIA_LOG, radiusBlocks);
      }

      if (logs.isEmpty()) {
         logs = this.scanFor(bot, Material.DARK_OAK_LOG, radiusBlocks);
      }

      return logs.isEmpty() ? null : logs.get(0);
   }

   public Location findNearestOre(Player bot, Material oreType, int radiusBlocks) {
      List<Location> ores = this.scanFor(bot, oreType, radiusBlocks);
      return ores.isEmpty() ? null : ores.get(0);
   }

   public DeltaTracker getDeltaTracker() {
      return this.deltaTracker;
   }

   public int getCacheSize() {
      return this.cache.size();
   }

   public double getCacheMemoryMb() {
      return this.cache.getEstimatedMemoryMb();
   }

   public boolean isChunkCached(int chunkX, int chunkZ) {
      return this.cache.isCached(chunkX, chunkZ);
   }

   private void refreshAroundBot() {
      if (LoadGovernor.isCritical()) {
         return;
      }

      if (this.trackedBot != null && this.trackedBot.isOnline()) {
         Location loc = this.trackedBot.getLocation();
         World world = loc.getWorld();
         int botChunkX = loc.getBlockX() >> 4;
         int botChunkZ = loc.getBlockZ() >> 4;

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               int chunkX = botChunkX + dx;
               int chunkZ = botChunkZ + dz;
               if (!this.cache.isCached(chunkX, chunkZ)) {
                  ChunkCache.CachedChunk chunk = this.fileReader.readChunk(world, chunkX, chunkZ);
                  if (chunk != null) {
                     this.cache.put(chunkX, chunkZ, chunk);
                     this.persistentStore.recordChunk(world, chunkX, chunkZ, chunk);
                  }

                  this.cache.purgeExpired();
                  this.persistentStore.flushBudget(1);
                  return;
               }
            }
         }

         int purged = this.cache.purgeExpired();
         if (purged > 0) {
            CoreBootstrap.PLUGIN.getLogger().fine("[ChunkRadar] Purged " + purged + " expired chunks");
         }
      }
   }
}
