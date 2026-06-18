package com.jonasmp.ai.threat;

import java.util.UUID;

public class PlayerThreatProfile {
   private final UUID uuid;
   private double threatScore;

   public PlayerThreatProfile(UUID uuid) {
      this.uuid = uuid;
      this.threatScore = 0.0;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public double getThreatScore() {
      return this.threatScore;
   }

   public void addThreat(double value) {
      this.threatScore += value;
      if (this.threatScore > 100.0) {
         this.threatScore = 100.0;
      }
   }

   public void reduceThreat(double value) {
      this.threatScore -= value;
      if (this.threatScore < 0.0) {
         this.threatScore = 0.0;
      }
   }
}
