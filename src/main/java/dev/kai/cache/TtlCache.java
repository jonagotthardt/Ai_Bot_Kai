package dev.kai.cache;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A generic time-to-live cache backed by {@link ConcurrentHashMap}.
 *
 * <p>This is the RAM tier used across Kai (radar entries, line-of-sight results, gear choices...).
 * Entries are measured against the server tick counter rather than wall-clock time so cache
 * lifetimes line up exactly with the tick loop and pause when the server lags.
 *
 * <p>Expiry is lazy: a stale entry is dropped on read. {@link #purge(long)} can be called from the
 * load-balanced maintenance pass to reclaim memory for keys that are never read again.
 *
 * <p>Thread-safety: the backing map is concurrent so async callbacks (e.g. async chunk loads) can
 * safely write results, while the main thread reads them.
 */
public final class TtlCache<K, V> {

    private record Entry<V>(V value, long expiresAtTick) {
        boolean isAlive(long now) {
            return now < expiresAtTick;
        }
    }

    private final Map<K, Entry<V>> map = new ConcurrentHashMap<>();
    private final long defaultTtlTicks;

    private long hits;
    private long misses;

    public TtlCache(long defaultTtlTicks) {
        this.defaultTtlTicks = Math.max(1L, defaultTtlTicks);
    }

    public void put(K key, V value, long nowTick) {
        put(key, value, nowTick, defaultTtlTicks);
    }

    public void put(K key, V value, long nowTick, long ttlTicks) {
        map.put(key, new Entry<>(value, nowTick + Math.max(1L, ttlTicks)));
    }

    public @Nullable V get(K key, long nowTick) {
        Entry<V> entry = map.get(key);
        if (entry == null) {
            misses++;
            return null;
        }
        if (!entry.isAlive(nowTick)) {
            map.remove(key, entry);
            misses++;
            return null;
        }
        hits++;
        return entry.value();
    }

    /**
     * Returns the cached value, or computes, stores and returns a fresh one when missing/stale.
     */
    public V getOrCompute(K key, long nowTick, Supplier<V> supplier) {
        V cached = get(key, nowTick);
        if (cached != null) {
            return cached;
        }
        V fresh = supplier.get();
        put(key, fresh, nowTick);
        return fresh;
    }

    public void invalidate(K key) {
        map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    /** Drops every expired entry. Intended to be called from the maintenance pass, not per tick. */
    public int purge(long nowTick) {
        int removed = 0;
        for (Map.Entry<K, Entry<V>> e : map.entrySet()) {
            if (!e.getValue().isAlive(nowTick) && map.remove(e.getKey(), e.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return map.size();
    }

    public long hits() {
        return hits;
    }

    public long misses() {
        return misses;
    }
}
