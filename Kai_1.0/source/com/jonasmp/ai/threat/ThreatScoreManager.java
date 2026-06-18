package com.jonasmp.ai.threat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ThreatScoreManager {
   private final HashMap<UUID, PlayerThreatProfile> profiles = new HashMap<>();
   private final File file;
   private final YamlConfiguration config;
   private final JavaPlugin plugin;

   public ThreatScoreManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "threats.yml");
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
            double score = this.config.getDouble(key);
            PlayerThreatProfile profile = new PlayerThreatProfile(uuid);
            profile.addThreat(score);
            this.profiles.put(uuid, profile);
         } catch (Exception var7) {
         }
      }
   }

   public void save() {
      for (UUID uuid : this.profiles.keySet()) {
         this.config.set(uuid.toString(), this.getScore(uuid));
      }

      try {
         this.config.save(this.file);
      } catch (IOException var3) {
         var3.printStackTrace();
      }
   }

   public PlayerThreatProfile getProfile(UUID uuid) {
      return this.profiles.computeIfAbsent(uuid, PlayerThreatProfile::new);
   }

   public void addThreat(UUID uuid, double value) {
      this.getProfile(uuid).addThreat(value);
   }

   public void reduceThreat(UUID uuid, double value) {
      this.getProfile(uuid).reduceThreat(value);
   }

   public double getScore(UUID uuid) {
      return this.getProfile(uuid).getThreatScore();
   }

   public boolean isDangerous(UUID uuid) {
      return this.getScore(uuid) >= 80.0;
   }

   public double getThreat(UUID uuid) {
      return this.getScore(uuid);
   }
}
