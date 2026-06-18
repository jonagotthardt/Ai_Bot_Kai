package com.jonasmp.ai.memory;

public class ChatEntry {
   public long timestamp;
   public String message;
   public String channel;

   public ChatEntry() {
   }

   public ChatEntry(String message, String channel) {
      this.timestamp = System.currentTimeMillis();
      this.message = message;
      this.channel = channel;
   }
}
