package com.jonasmp.ai.punishment;

public class PunishmentResult {
   private final PunishmentType type;
   private final String reason;
   private final long duration;

   public PunishmentResult(PunishmentType type, String reason, long duration) {
      this.type = type;
      this.reason = reason;
      this.duration = duration;
   }

   public PunishmentType getType() {
      return this.type;
   }

   public String getReason() {
      return this.reason;
   }

   public long getDuration() {
      return this.duration;
   }
}
