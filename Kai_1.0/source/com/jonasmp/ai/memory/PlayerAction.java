package com.jonasmp.ai.memory;

public class PlayerAction {
   public long timestamp;
   public String type;
   public String details;
   public String world;
   public int x;
   public int y;
   public int z;

   public PlayerAction() {
   }

   public PlayerAction(String type, String details, String world, int x, int y, int z) {
      this.timestamp = System.currentTimeMillis();
      this.type = type;
      this.details = details;
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
   }
}
