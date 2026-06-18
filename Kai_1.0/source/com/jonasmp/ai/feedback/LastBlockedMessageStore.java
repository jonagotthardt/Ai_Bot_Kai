package com.jonasmp.ai.feedback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastBlockedMessageStore {
   private final Map<UUID, String> lastBlocked = new ConcurrentHashMap<>();

   public void recordBlocked(UUID playerId, String rawMessage) {
      if (playerId != null && rawMessage != null) {
         this.lastBlocked.put(playerId, rawMessage);
      }
   }

   public String getLastBlocked(UUID playerId) {
      return this.lastBlocked.get(playerId);
   }

   public void clear(UUID playerId) {
      this.lastBlocked.remove(playerId);
   }
}
