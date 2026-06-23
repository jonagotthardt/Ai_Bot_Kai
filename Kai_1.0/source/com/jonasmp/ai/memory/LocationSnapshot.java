package com.jonasmp.ai.memory;

public class LocationSnapshot {
   public long timestamp;
   public String world;
   public int x;
   public int y;
   public int z;

   public LocationSnapshot() {
   }

   public LocationSnapshot(String world, int x, int y, int z) {
      this.timestamp = System.currentTimeMillis();
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
   }
}
