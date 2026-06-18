package com.jonasmp.ai.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Owns the live {@link ComboTracker}s and their persistent {@link ComboProfile}s.
 *
 * <h2>Scope rules (per the design brief)</h2>
 * <ul>
 *   <li><b>Never server-wide:</b> only hits landed <em>on Kai</em> are recorded — the
 *       listener that feeds {@link #recordHit} only fires when Kai is the victim.</li>
 *   <li><b>Combat + 5 min window:</b> a tracker is kept warm until 5 minutes after the
 *       last hit from that attacker, then it is flushed to disk and evicted. A
 *       re-engagement within the window keeps learning seamlessly.</li>
 *   <li><b>Players vs. mobs:</b> players are keyed by UUID (file named after the
 *       player); mobs are aggregated per entity type and persist more slowly.</li>
 * </ul>
 */
public final class ComboLearner {

   /** Keep a tracker alive this long after the last hit, then persist + drop it. */
   private static final long WINDOW_MS = 300_000L;
   /** Throttle player-profile disk writes during a long fight. */
   private static final long PLAYER_FLUSH_INTERVAL_MS = 30_000L;

   private final ComboProfileStorage storage = new ComboProfileStorage();
   private final Map<String, Entry> active = new HashMap<>();

   /** Records one incoming hit against the attacker's tracker. {@code attacker} must be Kai's attacker. */
   public void recordHit(Entity attacker, HitSample sample) {
      if (attacker == null || sample == null) {
         return;
      }
      Entry entry = this.getOrCreate(attacker);
      if (entry == null) {
         return;
      }
      entry.tracker.record(sample);
      entry.lastHitTime = sample.time;
   }

   /** The live tracker for a target if one is warm, else null (no prediction available). */
   public ComboTracker getActiveTracker(Entity target) {
      if (target == null) {
         return null;
      }
      Entry entry = this.active.get(keyFor(target));
      return entry != null ? entry.tracker : null;
   }

   /** Periodic upkeep: evict stale trackers (window elapsed) and flush dirty ones. Call ~1×/s. */
   public void maintenance() {
      long now = System.currentTimeMillis();
      Iterator<Map.Entry<String, Entry>> it = this.active.entrySet().iterator();
      while (it.hasNext()) {
         Entry entry = it.next().getValue();
         if (now - entry.lastHitTime > WINDOW_MS) {
            this.flush(entry);
            it.remove();
         } else if (entry.tracker.isDirty() && now - entry.lastFlushAt > PLAYER_FLUSH_INTERVAL_MS) {
            this.flush(entry);
            entry.lastFlushAt = now;
         }
      }
   }

   /** Flush everything to disk (plugin shutdown). */
   public void flushAll() {
      for (Entry entry : this.active.values()) {
         this.flush(entry);
      }
   }

   // --- internals ---------------------------------------------------------

   private Entry getOrCreate(Entity attacker) {
      String key = keyFor(attacker);
      Entry entry = this.active.get(key);
      if (entry != null) {
         return entry;
      }

      ComboProfile profile;
      if (attacker instanceof Player player) {
         profile = this.storage.loadPlayer(player.getUniqueId().toString(), player.getName());
      } else {
         profile = this.storage.loadMob(attacker.getType().name());
      }
      entry = new Entry(profile, new ComboTracker(profile));
      this.active.put(key, entry);
      return entry;
   }

   private void flush(Entry entry) {
      if (entry == null || !entry.tracker.isDirty()) {
         return;
      }
      entry.tracker.writeInto(entry.profile);
      this.storage.save(entry.profile);
   }

   private static String keyFor(Entity entity) {
      if (entity instanceof Player player) {
         return "p:" + player.getUniqueId();
      }
      return "m:" + entity.getType().name();
   }

   private static final class Entry {
      final ComboProfile profile;
      final ComboTracker tracker;
      long lastHitTime = System.currentTimeMillis();
      long lastFlushAt = System.currentTimeMillis();

      Entry(ComboProfile profile, ComboTracker tracker) {
         this.profile = profile;
         this.tracker = tracker;
      }
   }
}
