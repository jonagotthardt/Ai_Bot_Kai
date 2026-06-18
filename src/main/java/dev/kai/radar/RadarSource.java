package dev.kai.radar;

/**
 * Where a {@link ChunkEntry}'s data came from, in descending order of freshness.
 *
 * <p>The radar hierarchy is <b>loaded chunks &rarr; RAM cache</b> (with MCA/SSD tiers planned as a
 * later phase). Loaded chunks are authoritative; cached entries are reused so Kai never force-loads
 * or re-scans terrain just to stay aware of it.
 */
public enum RadarSource {
    LOADED,
    RAM_CACHE
}
