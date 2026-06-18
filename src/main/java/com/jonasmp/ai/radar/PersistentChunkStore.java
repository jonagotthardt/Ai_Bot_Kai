package com.jonasmp.ai.radar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Tier 4 of the ChunkRadar hierarchy: a persistent, on-disk memory of the
 * <em>notable</em> blocks Kai has already seen (ores, logs, lava/water, obsidian,
 * ancient debris, spawners). It lets {@code scanFor}/{@code findNearestOre/Tree}
 * answer for areas Kai explored earlier — even after a server restart and even
 * when those chunks are no longer in the RAM cache — <strong>without ever
 * force-loading a chunk</strong>.
 *
 * <h2>Why this design is cheap</h2>
 * <ul>
 *   <li>Only "notable" blocks are stored, not full chunks → a chunk record is a
 *       few hundred ints, not ~98k materials.</li>
 *   <li>Records are grouped into <b>region files</b> (32×32 chunks per file) so we
 *       have few files and few open/close cycles.</li>
 *   <li>Regions are loaded lazily, kept in a small LRU map, and written back in a
 *       <b>budgeted</b> way (the radar flushes at most one dirty region per refresh
 *       cycle), so there is never a per-tick I/O spike.</li>
 *   <li>All work is plain data + file I/O (no Bukkit API), so it is safe to call
 *       from the radar's main-thread refresh without touching the world.</li>
 * </ul>
 *
 * <h2>Staleness</h2>
 * Persisted hints can go stale. {@link #invalidateBlock} (fed by the DeltaTracker's
 * break/place events) keeps loaded regions in sync; and whenever Kai actually
 * revisits an area the chunk is re-read live and the record is overwritten.
 */
public final class PersistentChunkStore {

   /** 1 region = 2^5 = 32 chunks on each axis. */
   private static final int REGION_SHIFT = 5;
   /** Max regions kept in RAM before the least-recently-used one is flushed + evicted. */
   private static final int MAX_LOADED_REGIONS = 16;

   private final Gson gson = new GsonBuilder().create();
   private final File baseFolder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "chunk_store");

   /** LRU map of region key -> data. Access-ordered so the eldest entry is the LRU victim. */
   private final LinkedHashMap<String, RegionData> loaded =
      new LinkedHashMap<>(16, 0.75f, true) {
         @Override
         protected boolean removeEldestEntry(Map.Entry<String, RegionData> eldest) {
            if (this.size() > MAX_LOADED_REGIONS) {
               PersistentChunkStore.this.flushRegion(eldest.getKey(), eldest.getValue());
               return true;
            }
            return false;
         }
      };

   private final Set<String> dirty = new LinkedHashSet<>();

   public PersistentChunkStore() {
      if (!this.baseFolder.exists()) {
         this.baseFolder.mkdirs();
      }
   }

   /** Whether a material is worth remembering long-term (resources + hazards Kai cares about). */
   public static boolean isNotable(Material m) {
      if (m == null) {
         return false;
      }
      String n = m.name();
      return n.endsWith("_ORE")
         || n.endsWith("_LOG")
         || m == Material.ANCIENT_DEBRIS
         || m == Material.LAVA
         || m == Material.WATER
         || m == Material.OBSIDIAN
         || m == Material.CRYING_OBSIDIAN
         || m == Material.SPAWNER;
   }

   /**
    * Extracts the notable blocks from a freshly read chunk and stores them, replacing
    * any previous record for that chunk. Called when the radar pulls a chunk into the
    * RAM cache (at most one new chunk per refresh cycle).
    */
   public void recordChunk(World world, int chunkX, int chunkZ, ChunkCache.CachedChunk chunk) {
      if (world == null || chunk == null) {
         return;
      }
      Material[] blocks = chunk.rawBlocks();
      if (blocks == null) {
         return;
      }
      int sectionHeight = chunk.sectionHeight();
      if (sectionHeight <= 0) {
         return;
      }

      ChunkFeatures features = new ChunkFeatures();
      features.minY = chunk.minY();
      features.savedAt = System.currentTimeMillis();

      for (int index = 0; index < blocks.length; index++) {
         Material m = blocks[index];
         if (!isNotable(m)) {
            continue;
         }
         int yIndex = index % sectionHeight;
         int xz = index / sectionHeight;
         int localX = xz / 16;
         int localZ = xz % 16;
         int packed = (yIndex << 8) | (localZ << 4) | localX;
         features.features.computeIfAbsent(m.name(), k -> new ArrayList<>()).add(packed);
      }

      String regionKey = regionKey(world.getName(), chunkX, chunkZ);
      RegionData region = this.getRegion(world.getName(), chunkX, chunkZ, true);
      if (region == null) {
         return;
      }
      region.chunks.put(chunkKey(chunkX, chunkZ), features);
      this.dirty.add(regionKey);
   }

   public boolean hasChunk(String worldName, int chunkX, int chunkZ) {
      RegionData region = this.getRegion(worldName, chunkX, chunkZ, true);
      return region != null && region.chunks.containsKey(chunkKey(chunkX, chunkZ));
   }

   /**
    * Returns world-space positions of {@code target} that Kai previously recorded in
    * the given chunk, or an empty list if the chunk was never explored / has none.
    * Never loads a chunk.
    */
   public List<int[]> getPositions(String worldName, int chunkX, int chunkZ, Material target) {
      List<int[]> out = new ArrayList<>();
      RegionData region = this.getRegion(worldName, chunkX, chunkZ, true);
      if (region == null) {
         return out;
      }
      ChunkFeatures features = region.chunks.get(chunkKey(chunkX, chunkZ));
      if (features == null) {
         return out;
      }
      List<Integer> packedList = features.features.get(target.name());
      if (packedList == null) {
         return out;
      }
      int baseX = chunkX << 4;
      int baseZ = chunkZ << 4;
      for (int packed : packedList) {
         int localX = packed & 15;
         int localZ = (packed >> 4) & 15;
         int yIndex = packed >> 8;
         out.add(new int[]{baseX + localX, features.minY + yIndex, baseZ + localZ});
      }
      return out;
   }

   /** Exact-position lookup of a remembered notable block, or null if not recorded. */
   public Material getMaterial(String worldName, int worldX, int worldY, int worldZ) {
      int chunkX = worldX >> 4;
      int chunkZ = worldZ >> 4;
      RegionData region = this.getRegion(worldName, chunkX, chunkZ, true);
      if (region == null) {
         return null;
      }
      ChunkFeatures features = region.chunks.get(chunkKey(chunkX, chunkZ));
      if (features == null) {
         return null;
      }
      int localX = worldX & 15;
      int localZ = worldZ & 15;
      int yIndex = worldY - features.minY;
      if (yIndex < 0) {
         return null;
      }
      int packed = (yIndex << 8) | (localZ << 4) | localX;
      for (Map.Entry<String, List<Integer>> entry : features.features.entrySet()) {
         if (entry.getValue().contains(packed)) {
            try {
               return Material.valueOf(entry.getKey());
            } catch (IllegalArgumentException ex) {
               return null;
            }
         }
      }
      return null;
   }

   /**
    * Keeps a remembered chunk in sync with an observed block change. Only touches
    * regions already in RAM (cheap); unloaded regions are corrected the next time
    * Kai revisits and the chunk is re-read live.
    */
   public void invalidateBlock(String worldName, int worldX, int worldY, int worldZ, Material newMaterial) {
      int chunkX = worldX >> 4;
      int chunkZ = worldZ >> 4;
      String regionKey = regionKey(worldName, chunkX, chunkZ);
      RegionData region = this.loaded.get(regionKey);
      if (region == null) {
         return;
      }
      ChunkFeatures features = region.chunks.get(chunkKey(chunkX, chunkZ));
      if (features == null) {
         return;
      }
      int localX = worldX & 15;
      int localZ = worldZ & 15;
      int yIndex = worldY - features.minY;
      if (yIndex < 0) {
         return;
      }
      Integer packed = (yIndex << 8) | (localZ << 4) | localX;

      boolean changed = false;
      for (List<Integer> list : features.features.values()) {
         changed |= list.remove(packed);
      }
      features.features.values().removeIf(List::isEmpty);

      if (isNotable(newMaterial)) {
         features.features.computeIfAbsent(newMaterial.name(), k -> new ArrayList<>()).add(packed);
         changed = true;
      }

      if (changed) {
         this.dirty.add(regionKey);
      }
   }

   /** Writes up to {@code max} dirty regions to disk. Called budgeted from the refresh cycle. */
   public int flushBudget(int max) {
      int written = 0;
      while (written < max && !this.dirty.isEmpty()) {
         String key = this.dirty.iterator().next();
         this.dirty.remove(key);
         RegionData region = this.loaded.get(key);
         if (region != null) {
            this.flushRegion(key, region);
            written++;
         }
      }
      return written;
   }

   /** Writes every dirty region. Call on plugin shutdown. */
   public void flushAll() {
      for (String key : new ArrayList<>(this.dirty)) {
         RegionData region = this.loaded.get(key);
         if (region != null) {
            this.flushRegion(key, region);
         }
      }
      this.dirty.clear();
   }

   // --- internals ---------------------------------------------------------

   private RegionData getRegion(String worldName, int chunkX, int chunkZ, boolean loadFromDisk) {
      String key = regionKey(worldName, chunkX, chunkZ);
      RegionData region = this.loaded.get(key);
      if (region != null) {
         return region;
      }
      if (!loadFromDisk) {
         return null;
      }
      region = this.readRegion(worldName, chunkX >> REGION_SHIFT, chunkZ >> REGION_SHIFT);
      this.loaded.put(key, region);
      return region;
   }

   private RegionData readRegion(String worldName, int regionX, int regionZ) {
      File file = this.regionFile(worldName, regionX, regionZ);
      if (!file.exists()) {
         return new RegionData();
      }
      try (FileReader reader = new FileReader(file)) {
         RegionData data = this.gson.fromJson(reader, RegionData.class);
         if (data == null) {
            return new RegionData();
         }
         if (data.chunks == null) {
            data.chunks = new HashMap<>();
         }
         return data;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[ChunkStore] Read failed for " + file.getName() + ": " + ex.getMessage());
         return new RegionData();
      }
   }

   private void flushRegion(String regionKey, RegionData region) {
      String[] parts = regionKey.split("\\|");
      if (parts.length != 3) {
         return;
      }
      String worldName = parts[0];
      int regionX = Integer.parseInt(parts[1]) >> REGION_SHIFT;
      int regionZ = Integer.parseInt(parts[2]) >> REGION_SHIFT;
      File file = this.regionFile(worldName, regionX, regionZ);
      File parent = file.getParentFile();
      if (parent != null && !parent.exists()) {
         parent.mkdirs();
      }
      try (FileWriter writer = new FileWriter(file)) {
         this.gson.toJson(region, writer);
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[ChunkStore] Write failed for " + file.getName() + ": " + ex.getMessage());
      }
   }

   private File regionFile(String worldName, int regionX, int regionZ) {
      File worldDir = new File(this.baseFolder, sanitize(worldName));
      return new File(worldDir, "r." + regionX + "." + regionZ + ".json");
   }

   /** Region key embeds the chunk coords (not region coords) for O(1) lookup; region is derived on flush. */
   private static String regionKey(String worldName, int chunkX, int chunkZ) {
      int regionX = chunkX >> REGION_SHIFT;
      int regionZ = chunkZ >> REGION_SHIFT;
      return worldName + "|" + (regionX << REGION_SHIFT) + "|" + (regionZ << REGION_SHIFT);
   }

   private static String chunkKey(int chunkX, int chunkZ) {
      return chunkX + "," + chunkZ;
   }

   private static String sanitize(String worldName) {
      return worldName.replaceAll("[^a-zA-Z0-9_.-]", "_");
   }

   // --- serialised data ---------------------------------------------------

   static final class RegionData {
      Map<String, ChunkFeatures> chunks = new HashMap<>();
   }

   static final class ChunkFeatures {
      long savedAt;
      int minY;
      Map<String, List<Integer>> features = new HashMap<>();
   }
}
