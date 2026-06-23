package com.jonasmp.ai.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory registry of {@link OpponentProfile}s with lazy load + persistence.
 * Lives on the main thread (the only caller is the combat manager), so the plain
 * {@link HashMap} is safe — no concurrent access.
 *
 * <p>Lookup order mirrors Kai's memory philosophy: RAM map first, disk only on a
 * miss. Profiles are written back on {@link #flush(UUID)} (called at combat end),
 * so there is no per-tick I/O.
 */
public final class OpponentMemory {

   private final OpponentMemoryStorage storage = new OpponentMemoryStorage();
   private final Map<UUID, OpponentProfile> cache = new HashMap<>();

   public OpponentProfile getOrCreate(UUID uuid, String name) {
      OpponentProfile profile = this.cache.get(uuid);
      if (profile == null) {
         profile = this.storage.load(uuid.toString(), name);
         profile.normalise();
         this.cache.put(uuid, profile);
      }
      profile.encounters++;
      profile.lastSeen = System.currentTimeMillis();
      return profile;
   }

   public void flush(UUID uuid) {
      OpponentProfile profile = this.cache.get(uuid);
      if (profile != null) {
         this.storage.save(profile);
      }
   }

   public void flushAll() {
      for (OpponentProfile profile : this.cache.values()) {
         this.storage.save(profile);
      }
   }
}
