package com.jonasmp.ai.personality;

import java.util.UUID;

public class PersonalityProfile {
   private final UUID uuid;
   private PersonalityType type = PersonalityType.SAFE;
   private int riskScore;

   public PersonalityProfile(UUID uuid) {
      this.uuid = uuid;
   }

   public void updateRisk(int score) {
      this.riskScore = score;
      this.recalc();
   }

   private void recalc() {
      if (this.riskScore >= 60) {
         this.type = PersonalityType.HIGH_RISK;
      } else if (this.riskScore >= 45) {
         this.type = PersonalityType.SCAMMER;
      } else if (this.riskScore >= 35) {
         this.type = PersonalityType.SPAMMER;
      } else if (this.riskScore >= 25) {
         this.type = PersonalityType.TOXIC;
      } else if (this.riskScore >= 15) {
         this.type = PersonalityType.SUSPICIOUS;
      } else if (this.riskScore >= 5) {
         this.type = PersonalityType.LOW_RISK;
      } else {
         this.type = PersonalityType.SAFE;
      }
   }

   public PersonalityType getType() {
      return this.type;
   }

   public int getRiskScore() {
      return this.riskScore;
   }

   public UUID getUuid() {
      return this.uuid;
   }
}
