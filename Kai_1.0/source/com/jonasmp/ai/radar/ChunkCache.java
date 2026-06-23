package com.jonasmp.ai.radar;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

public class ChunkCache {
   private static final long DEFAULT_TTL_MS = 30000L;
   private static final int MAX_CHUNKS = 50;
   private final ConcurrentHashMap<ChunkCache.ChunkKey, ChunkCache.CachedChunk> cache = new ConcurrentHashMap<>();
   private final long ttlMs;

   public ChunkCache() {
      this(30000L);
   }

   public ChunkCache(long ttlMs) {
      this.ttlMs = ttlMs;
   }

   public Material getBlockType(int worldX, int worldY, int worldZ) {
      int chunkX = worldX >> 4;
      int chunkZ = worldZ >> 4;
      ChunkCache.ChunkKey key = new ChunkCache.ChunkKey(chunkX, chunkZ);
      ChunkCache.CachedChunk chunk = this.cache.get(key);
      if (chunk != null && !this.isExpired(chunk)) {
         int localX = worldX & 15;
         int localZ = worldZ & 15;
         return chunk.getBlockType(localX, worldY, localZ);
      } else {
         return null;
      }
   }

   public void put(int chunkX, int chunkZ, ChunkCache.CachedChunk chunk) {
      if (this.cache.size() >= 50) {
         this.evictOldest();
      }

      this.cache.put(new ChunkCache.ChunkKey(chunkX, chunkZ), chunk);
   }

   private void evictOldest() {
      ChunkCache.ChunkKey oldestKey = null;
      long oldestTime = Long.MAX_VALUE;

      for (Entry<ChunkCache.ChunkKey, ChunkCache.CachedChunk> entry : this.cache.entrySet()) {
         if (entry.getValue().loadedAt < oldestTime) {
            oldestTime = entry.getValue().loadedAt;
            oldestKey = entry.getKey();
         }
      }

      if (oldestKey != null) {
         this.cache.remove(oldestKey);
      }
   }

   public boolean isCached(int chunkX, int chunkZ) {
      ChunkCache.CachedChunk chunk = this.cache.get(new ChunkCache.ChunkKey(chunkX, chunkZ));
      return chunk != null && !this.isExpired(chunk);
   }

   public void invalidate(int chunkX, int chunkZ) {
      this.cache.remove(new ChunkCache.ChunkKey(chunkX, chunkZ));
   }

   public int purgeExpired() {
      int count = 0;
      long now = System.currentTimeMillis();

      for (Entry<ChunkCache.ChunkKey, ChunkCache.CachedChunk> entry : this.cache.entrySet()) {
         if (this.isExpired(entry.getValue())) {
            this.cache.remove(entry.getKey());
            count++;
         }
      }

      return count;
   }

   public int size() {
      return this.cache.size();
   }

   public double getEstimatedMemoryMb() {
      long total = 0L;

      for (ChunkCache.CachedChunk chunk : this.cache.values()) {
         if (chunk.blocks != null) {
            total += (long)chunk.blocks.length * 4L;
         }
      }

      return (double)total / 1048576.0;
   }

   private boolean isExpired(ChunkCache.CachedChunk chunk) {
      return System.currentTimeMillis() - chunk.loadedAt > this.ttlMs;
   }

   public static final class CachedChunk {
      public final long loadedAt = System.currentTimeMillis();
      private final Material[] blocks;
      private final int minY;
      private final int sectionHeight;

      public CachedChunk(Material[] blocks, int minY, int sectionHeight) {
         this.blocks = blocks;
         this.minY = minY;
         this.sectionHeight = sectionHeight;
      }

      public Material getBlockType(int localX, int worldY, int localZ) {
         if (localX >= 0 && localX <= 15 && localZ >= 0 && localZ <= 15) {
            int yIndex = worldY - this.minY;
            if (yIndex >= 0 && yIndex < this.sectionHeight) {
               int index = (localX * 16 + localZ) * this.sectionHeight + yIndex;
               if (index >= 0 && index < this.blocks.length) {
                  Material m = this.blocks[index];
                  return m != null ? m : Material.AIR;
               } else {
                  return Material.AIR;
               }
            } else {
               return Material.AIR;
            }
         } else {
            return Material.AIR;
         }
      }
   }

   public static final class ChunkKey {
      public final int x;
      public final int z;

      public ChunkKey(int x, int z) {
         this.x = x;
         this.z = z;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof ChunkCache.ChunkKey other) ? false : this.x == other.x && this.z == other.z;
         }
      }

      @Override
      public int hashCode() {
         return 31 * this.x + this.z;
      }

      @Override
      public String toString() {
         return this.x + "," + this.z;
      }
   }
}
