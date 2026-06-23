package com.jonasmp.ai.memory;

import com.google.gson.Gson;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

public class MemoryStorage {
   private final Gson gson = new Gson();
   private final File folder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "memory");

   public MemoryStorage() {
      if (!this.folder.exists()) {
         this.folder.mkdirs();
      }
   }

   public void save(PlayerMemory memory) {
      try {
         File file = new File(this.folder, memory.uuid.toString() + ".json");
         FileWriter writer = new FileWriter(file);
         this.gson.toJson(memory, writer);
         writer.close();
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public PlayerMemory load(UUID uuid) {
      try {
         File file = new File(this.folder, uuid.toString() + ".json");
         if (!file.exists()) {
            return new PlayerMemory(uuid);
         } else {
            FileReader reader = new FileReader(file);
            PlayerMemory memory = (PlayerMemory)this.gson.fromJson(reader, PlayerMemory.class);
            reader.close();
            return memory;
         }
      } catch (Exception var5) {
         var5.printStackTrace();
         return new PlayerMemory(uuid);
      }
   }
}
