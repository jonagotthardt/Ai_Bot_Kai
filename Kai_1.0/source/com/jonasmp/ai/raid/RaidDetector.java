package com.jonasmp.ai.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RaidDetector {
   private final Map<UUID, List<Long>> messageLog;
   private final long TIME_WINDOW = 10000L;
   private final int LIMIT = 12;

   public RaidDetector() {
      this.messageLog = new ConcurrentHashMap<>();
   }

   public boolean isRaid(UUID playerId) {
      long now = System.currentTimeMillis();
      List<Long> timestamps = this.messageLog.computeIfAbsent(playerId, k -> new ArrayList<>());
      timestamps.add(now);
      timestamps.removeIf(t -> now - t > 10000L);
      return timestamps.size() >= 12;
   }
}
