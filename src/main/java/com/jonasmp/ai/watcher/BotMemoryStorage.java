package com.jonasmp.ai.watcher;

import com.google.gson.Gson;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

public class BotMemoryStorage {
   private final Gson gson = new Gson();
   private final File folder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_memory");

   public BotMemoryStorage() {
      if (!this.folder.exists()) {
         this.folder.mkdirs();
      }
   }

   public void save(BotMemory memory) {
      if (memory != null && memory.botUuid != null) {
         try {
            File file = new File(this.folder, memory.botUuid.toString() + ".json");
            FileWriter writer = new FileWriter(file);
            this.gson.toJson(memory, writer);
            writer.close();
            memory.lastSaved = System.currentTimeMillis();
         } catch (Exception var4) {
            CoreBootstrap.PLUGIN.getLogger().warning("[BotMemory] Save failed: " + var4.getMessage());
         }
      }
   }

   public BotMemory load(UUID uuid) {
      try {
         File file = new File(this.folder, uuid.toString() + ".json");
         if (!file.exists()) {
            return new BotMemory(uuid);
         } else {
            FileReader reader = new FileReader(file);
            BotMemory memory = (BotMemory)this.gson.fromJson(reader, BotMemory.class);
            reader.close();
            return memory != null ? memory : new BotMemory(uuid);
         }
      } catch (Exception var5) {
         return new BotMemory(uuid);
      }
   }
}
