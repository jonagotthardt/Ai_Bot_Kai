package com.jonasmp.ai.memory;

public class ConnectionEvent {
   public long timestamp;
   public String type;
   public long durationMs;

   public ConnectionEvent() {
   }

   public ConnectionEvent(String type, long durationMs) {
      this.timestamp = System.currentTimeMillis();
      this.type = type;
      this.durationMs = durationMs;
   }
}
