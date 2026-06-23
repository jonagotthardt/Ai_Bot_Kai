package com.jonasmp.ai.personality;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.memory.PlayerMemory;
import java.util.HashMap;
import java.util.UUID;

public class PersonalityEngine {
   private final HashMap<UUID, PersonalityProfile> cache = new HashMap<>();

   public PersonalityProfile get(UUID uuid) {
      return this.cache.computeIfAbsent(uuid, PersonalityProfile::new);
   }

   public void update(UUID uuid) {
      PlayerMemory memory = CoreBootstrap.MEMORY_ENGINE.get(uuid);
      int threat = (int)Math.round(CoreBootstrap.THREAT_MANAGER.getThreat(uuid));
      int risk = memory.getRiskScore() + threat;
      PersonalityProfile profile = this.get(uuid);
      profile.updateRisk(risk);
   }

   public PersonalityType getType(UUID uuid) {
      return this.get(uuid).getType();
   }
}
