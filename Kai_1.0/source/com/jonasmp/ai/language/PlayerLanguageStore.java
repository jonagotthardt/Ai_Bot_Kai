package com.jonasmp.ai.language;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerLanguageStore {
   private final File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "player_languages.yml");
   private FileConfiguration config;

   public PlayerLanguageStore() {
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var2) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not create player_languages.yml");
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public String getLanguage(UUID playerId) {
      String lang = this.config.getString(playerId.toString());
      if (lang != null) {
         return lang;
      } else {
         Player p = Bukkit.getPlayer(playerId);
         if (p != null) {
            String locale = p.getLocale().toLowerCase();
            if (locale.startsWith("en")) {
               this.setLanguage(playerId, "en");
               CoreBootstrap.PLUGIN.getLogger().info("[AI-Lang] Auto-marked " + p.getName() + " as ENGLISH (locale: " + locale);
               return "en";
            }
         }

         return "de";
      }
   }

   public void setLanguage(UUID playerId, String language) {
      if (!language.equals("de") && !language.equals("en")) {
         language = "de";
      }

      String previous = this.config.getString(playerId.toString());
      this.config.set(playerId.toString(), language);
      this.save();
      Player p = Bukkit.getPlayer(playerId);
      if (p != null) {
         String action = previous == null ? "SET" : "CHANGED";
         CoreBootstrap.PLUGIN.getLogger().info("[AI-Lang] " + action + " " + p.getName() + " -> " + language.toUpperCase());
         if ("en".equals(language)) {
            p.sendMessage("§a[AI] Language set to §b§lENGLISH§a.");
         } else {
            p.sendMessage("§a[AI] Sprache auf §b§lDEUTSCH§a gesetzt.");
         }
      }
   }

   public boolean hasLanguage(UUID playerId) {
      return this.config.contains(playerId.toString());
   }

   private void save() {
      try {
         this.config.save(this.file);
      } catch (IOException var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not save player_languages.yml");
      }
   }
}
