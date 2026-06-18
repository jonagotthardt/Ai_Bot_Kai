package dev.kai.radar;

import org.jetbrains.annotations.NotNull;

/**
 * Cached summary of a single chunk as last perceived by the {@link ChunkRadar}.
 *
 * <p>Deliberately tiny (a handful of counters, no block/entity references) so thousands of entries
 * stay cheap in RAM and never pin chunks or entities in memory.
 */
public final class ChunkEntry {

    private final long key;
    private int players;
    private int hostiles;
    private int living;
    private long updatedTick;
    private RadarSource source;

    ChunkEntry(long key) {
        this.key = key;
    }

    void set(int players, int hostiles, int living, long updatedTick, @NotNull RadarSource source) {
        this.players = players;
        this.hostiles = hostiles;
        this.living = living;
        this.updatedTick = updatedTick;
        this.source = source;
    }

    public long key() {
        return key;
    }

    public int players() {
        return players;
    }

    public int hostiles() {
        return hostiles;
    }

    public int living() {
        return living;
    }

    public long updatedTick() {
        return updatedTick;
    }

    public @NotNull RadarSource source() {
        return source;
    }
}
