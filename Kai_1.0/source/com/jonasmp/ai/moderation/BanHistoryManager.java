package com.jonasmp.ai.moderation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BanHistoryManager {
   private final File file;
   private final YamlConfiguration config;
   private final Map<UUID, Integer> banCounts = new HashMap<>();

   public BanHistoryManager(JavaPlugin plugin) {
      this.file = new File(plugin.getDataFolder(), "ban_history.yml");
      if (!this.file.exists()) {
         try {
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      this.load();
   }

   private void load() {
      for (String key : this.config.getKeys(false)) {
         try {
            UUID uuid = UUID.fromString(key);
            int count = this.config.getInt(key, 0);
            this.banCounts.put(uuid, count);
         } catch (Exception var5) {
         }
      }
   }

   public void save() {
      for (Entry<UUID, Integer> entry : this.banCounts.entrySet()) {
         this.config.set(entry.getKey().toString(), entry.getValue());
      }

      try {
         this.config.save(this.file);
      } catch (IOException var3) {
         var3.printStackTrace();
      }
   }

   public int getBanCount(UUID uuid) {
      return this.banCounts.getOrDefault(uuid, 0);
   }

   public int incrementBanCount(UUID uuid) {
      int count = this.getBanCount(uuid) + 1;
      this.banCounts.put(uuid, count);
      this.save();
      return count;
   }

   public String getBanDuration(UUID uuid) {
      int count = this.getBanCount(uuid);

      return switch (count) {
         case 1 -> "1h";
         case 2 -> "6h";
         case 3 -> "24h";
         case 4 -> "72h";
         default -> "permanent";
      };
   }

   public String getFormattedDuration(UUID uuid) {
      int count = this.getBanCount(uuid);

      return switch (count) {
         case 1 -> "1 hour";
         case 2 -> "6 hours";
         case 3 -> "1 day";
         case 4 -> "3 days";
         default -> "permanent";
      };
   }
}
