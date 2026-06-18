package com.jonasmp.ai.rules;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class FirstJoinTracker {
   private final File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "player_rules_accepted.yml");
   private FileConfiguration config;

   public FirstJoinTracker() {
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var2) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not create player_rules_accepted.yml");
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public boolean hasAccepted(UUID playerId) {
      return this.config.getBoolean(playerId.toString(), false);
   }

   public void setAccepted(UUID playerId) {
      this.config.set(playerId.toString(), true);
      this.save();
   }

   public void resetPlayer(UUID playerId) {
      this.config.set(playerId.toString(), null);
      this.save();
   }

   private void save() {
      try {
         this.config.save(this.file);
      } catch (IOException var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not save player_rules_accepted.yml");
      }
   }
}
