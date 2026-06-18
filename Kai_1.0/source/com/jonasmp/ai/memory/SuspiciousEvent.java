package com.jonasmp.ai.memory;

public class SuspiciousEvent {
   public long timestamp;
   public String type;
   public String description;
   public int severity;

   public SuspiciousEvent() {
   }

   public SuspiciousEvent(String type, String description, int severity) {
      this.timestamp = System.currentTimeMillis();
      this.type = type;
      this.description = description;
      this.severity = severity;
   }
}
