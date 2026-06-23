package com.jonasmp.ai.moderation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class MuteManager {
   private final Map<UUID, MuteManager.MuteEntry> mutes = new HashMap<>();

   public boolean isMuted(UUID uuid) {
      MuteManager.MuteEntry entry = this.mutes.get(uuid);
      if (entry == null) {
         return false;
      } else if (System.currentTimeMillis() > entry.until) {
         this.mutes.remove(uuid);
         return false;
      } else {
         return true;
      }
   }

   public long getRemainingMs(UUID uuid) {
      MuteManager.MuteEntry entry = this.mutes.get(uuid);
      if (entry == null) {
         return 0L;
      } else {
         long remaining = entry.until - System.currentTimeMillis();
         return Math.max(0L, remaining);
      }
   }

   public void mute(UUID uuid, long durationMs, String reason, String mutedBy) {
      this.mutes.put(uuid, new MuteManager.MuteEntry(System.currentTimeMillis() + durationMs, reason, mutedBy));
   }

   public void unmute(UUID uuid) {
      this.mutes.remove(uuid);
   }

   public MuteManager.MuteEntry getEntry(UUID uuid) {
      return this.mutes.get(uuid);
   }

   public Collection<UUID> getAllMuted() {
      this.mutes.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().until);
      return new HashSet<>(this.mutes.keySet());
   }

   public String getMuteReason(UUID uuid) {
      MuteManager.MuteEntry entry = this.mutes.get(uuid);
      return entry != null ? entry.reason : null;
   }

   public static class MuteEntry {
      public final long until;
      public final String reason;
      public final String mutedBy;

      public MuteEntry(long until, String reason, String mutedBy) {
         this.until = until;
         this.reason = reason;
         this.mutedBy = mutedBy;
      }
   }
}
