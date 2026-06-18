package dev.kai.radar;

import dev.kai.memory.MemorySystem;
import dev.kai.scheduler.LoadBalancer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-tier situational awareness around Kai.
 *
 * <p>Hierarchy: <b>loaded chunks &rarr; RAM cache</b>. On each refresh the radar walks the chunk ring
 * around Kai; loaded chunks are (re)scanned and become the freshest source, while unloaded chunks
 * keep their last cached summary instead of being force-loaded. This is what lets Kai stay aware of
 * its surroundings without per-tick world scans.
 *
 * <p>Cost is bounded two ways: refreshes only happen on a cadence (not every tick), and each chunk
 * scan is queued onto the {@link LoadBalancer} so the work is spread across ticks under a fixed time
 * budget. Each scan also feeds the {@link MemorySystem}, so perceiving the world updates memory in
 * the same pass rather than triggering a second scan later.
 *
 * <p>MCA-file and SSD-persistence tiers are intentionally out of this phase; the {@link RadarSource}
 * enum and entry model are shaped to accept them without changing callers.
 */
public final class ChunkRadar {

    /** Rough per-entry footprint estimate (object header + counters + map overhead), for stats. */
    private static final int APPROX_BYTES_PER_ENTRY = 96;

    private final MemorySystem memory;
    private final int radiusChunks;
    private final long entryTtlTicks;

    private final Map<Long, ChunkEntry> cache = new ConcurrentHashMap<>();
    private UUID worldId;

    public ChunkRadar(@NotNull MemorySystem memory, int radiusChunks, long entryTtlTicks) {
        this.memory = memory;
        this.radiusChunks = Math.max(1, radiusChunks);
        this.entryTtlTicks = Math.max(1L, entryTtlTicks);
    }

    private static long key(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    /**
     * Queues a refresh of the chunk ring around {@code center}. Loaded chunks are scheduled for a
     * (budgeted) rescan; unloaded chunks are left to their cached entry until it expires.
     */
    public void refresh(@NotNull Location center, @NotNull LoadBalancer balancer, long nowTick) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        if (!world.getUID().equals(worldId)) {
            worldId = world.getUID();
            cache.clear();
        }

        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                int chunkX = cx + dx;
                int chunkZ = cz + dz;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue; // keep cached (RAM tier); never force-load for the radar
                }
                balancer.submit(() -> scanLoadedChunk(world, chunkX, chunkZ, nowTick));
            }
        }
        // Expire entries we no longer maintain so the cache reflects ~the warm ring only.
        cache.values().removeIf(e -> nowTick - e.updatedTick() > entryTtlTicks);
    }

    private void scanLoadedChunk(@NotNull World world, int chunkX, int chunkZ, long nowTick) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return; // unloaded between scheduling and execution
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        int players = 0;
        int hostiles = 0;
        int living = 0;
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
                continue;
            }
            living++;
            if (livingEntity instanceof Player) {
                players++;
            }
            if (livingEntity instanceof Monster) {
                hostiles++;
            }
            memory.remember(livingEntity, nowTick);
        }
        cache.computeIfAbsent(key(chunkX, chunkZ), ChunkEntry::new)
             .set(players, hostiles, living, nowTick, RadarSource.LOADED);
    }

    public int cachedChunks() {
        return cache.size();
    }

    public long approxCacheBytes() {
        return (long) cache.size() * APPROX_BYTES_PER_ENTRY;
    }

    public int knownHostiles() {
        int total = 0;
        for (ChunkEntry e : cache.values()) {
            total += e.hostiles();
        }
        return total;
    }

    public void clear() {
        cache.clear();
        worldId = null;
    }
}
