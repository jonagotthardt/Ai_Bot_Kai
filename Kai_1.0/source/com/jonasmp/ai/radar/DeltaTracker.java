package com.jonasmp.ai.radar;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class DeltaTracker implements Listener {
   private final Map<String, DeltaTracker.DeltaEntry> deltas = new ConcurrentHashMap<>();
   private static final int MAX_ENTRIES = 10000;

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Block block = event.getBlock();
      String key = this.makeKey(block);
      this.deltas.put(key, new DeltaTracker.DeltaEntry(Material.AIR, System.currentTimeMillis()));
      this.evictIfNeeded();
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Block block = event.getBlock();
      String key = this.makeKey(block);
      this.deltas.put(key, new DeltaTracker.DeltaEntry(block.getType(), System.currentTimeMillis()));
      this.evictIfNeeded();
   }

   public Material getLiveMaterial(int worldX, int worldY, int worldZ, String worldName) {
      String key = worldName + ":" + worldX + ":" + worldY + ":" + worldZ;
      DeltaTracker.DeltaEntry entry = this.deltas.get(key);
      return entry != null ? entry.material : null;
   }

   public void invalidateChunk(String worldName, int chunkX, int chunkZ) {
      String prefix = worldName + ":" + chunkX + ":" + chunkZ + ":";
      this.deltas.keySet().removeIf(k -> k.startsWith(prefix));
   }

   private String makeKey(Block block) {
      return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
   }

   private void evictIfNeeded() {
      if (this.deltas.size() > 10000) {
         long cutoff = System.currentTimeMillis() - 300000L;
         this.deltas.values().removeIf(e -> e.timestamp < cutoff);
      }
   }

   private static final class DeltaEntry {
      final Material material;
      final long timestamp;

      DeltaEntry(Material material, long timestamp) {
         this.material = material;
         this.timestamp = timestamp;
      }
   }
}
