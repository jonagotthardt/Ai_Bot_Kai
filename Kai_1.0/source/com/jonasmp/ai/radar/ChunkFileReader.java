package com.jonasmp.ai.radar;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;

public class ChunkFileReader {
   public ChunkCache.CachedChunk readChunk(World world, int chunkX, int chunkZ) {
      if (!world.isChunkLoaded(chunkX, chunkZ)) {
         return null;
      } else {
         try {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            int sectionHeight = maxY - minY;
            Material[] blocks = new Material[256 * sectionHeight];

            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  for (int y = minY; y < maxY; y++) {
                     int yIndex = y - minY;
                     int index = (x * 16 + z) * sectionHeight + yIndex;
                     blocks[index] = chunk.getBlock(x, y, z).getType();
                  }
               }
            }

            return new ChunkCache.CachedChunk(blocks, minY, sectionHeight);
         } catch (Exception var14) {
            CoreBootstrap.PLUGIN.getLogger().warning("[ChunkFileReader] Failed to read chunk " + chunkX + "," + chunkZ + ": " + var14.getMessage());
            return null;
         }
      }
   }
}
