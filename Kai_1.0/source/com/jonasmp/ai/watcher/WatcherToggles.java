package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public class WatcherToggles {
   private static final List<String> ALL_EVENTS = Arrays.asList(
      "block_corruption",
      "item_vanish",
      "door_anomaly",
      "chest_anomaly",
      "torch_extinguish",
      "fake_chat",
      "health_glitch",
      "camera_glitch",
      "entity_swap",
      "sound_glitch",
      "inventory_shuffle",
      "sky_glitch",
      "block_under_foot",
      "broken_script_end",
      "temp_ban"
   );
   private final File file;
   private YamlConfiguration config;

   public WatcherToggles() {
      File dir = new File(CoreBootstrap.PLUGIN.getDataFolder(), "watcher");
      dir.mkdirs();
      this.file = new File(dir, "toggles.yml");
      this.load();
   }

   private void load() {
      if (!this.file.exists()) {
         this.config = new YamlConfiguration();
         this.setDefaults();
         this.save();
      } else {
         this.config = YamlConfiguration.loadConfiguration(this.file);
         this.ensureDefaults();
      }
   }

   private void setDefaults() {
      this.config.set("bot_enabled", true);
      this.config.set("horror_global_enabled", true);
      this.config.set("broken_script_enabled", true);

      for (String event : ALL_EVENTS) {
         this.config.set("events." + event, true);
      }
   }

   private void ensureDefaults() {
      if (!this.config.isSet("bot_enabled")) {
         this.config.set("bot_enabled", true);
      }

      if (!this.config.isSet("horror_global_enabled")) {
         this.config.set("horror_global_enabled", true);
      }

      if (!this.config.isSet("broken_script_enabled")) {
         this.config.set("broken_script_enabled", true);
      }

      for (String event : ALL_EVENTS) {
         if (!this.config.isSet("events." + event)) {
            this.config.set("events." + event, true);
         }
      }

      this.save();
   }

   public void save() {
      try {
         this.config.save(this.file);
      } catch (IOException var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[WatcherToggles] Failed to save toggles: " + var2.getMessage());
      }
   }

   public boolean isBotEnabled() {
      return this.config.getBoolean("bot_enabled", true);
   }

   public boolean isHorrorGlobalEnabled() {
      return this.config.getBoolean("horror_global_enabled", true);
   }

   public boolean isBrokenScriptEnabled() {
      return this.config.getBoolean("broken_script_enabled", true);
   }

   public boolean isEventEnabled(String eventType) {
      return this.config.getBoolean("events." + eventType, true);
   }

   public boolean toggleBot() {
      boolean next = !this.isBotEnabled();
      this.config.set("bot_enabled", next);
      this.save();
      return next;
   }

   public boolean toggleHorrorGlobal() {
      boolean next = !this.isHorrorGlobalEnabled();
      this.config.set("horror_global_enabled", next);
      this.save();
      return next;
   }

   public boolean toggleBrokenScript() {
      boolean next = !this.isBrokenScriptEnabled();
      this.config.set("broken_script_enabled", next);
      this.save();
      return next;
   }

   public boolean toggleEvent(String eventType) {
      if (!ALL_EVENTS.contains(eventType)) {
         return false;
      } else {
         boolean next = !this.isEventEnabled(eventType);
         this.config.set("events." + eventType, next);
         this.save();
         return next;
      }
   }

   public List<String> getAllEventTypes() {
      return ALL_EVENTS;
   }

   public String getToggleStatus() {
      StringBuilder sb = new StringBuilder();
      sb.append("§6[TOGGLE STATUS]\n");
      sb.append("§eBot: ").append(this.isBotEnabled() ? "§aON" : "§cOFF").append("\n");
      sb.append("§eHorror Global: ").append(this.isHorrorGlobalEnabled() ? "§aON" : "§cOFF").append("\n");
      sb.append("§eBroken Script: ").append(this.isBrokenScriptEnabled() ? "§aON" : "§cOFF").append("\n");
      sb.append("§eEvents:\n");

      for (String event : ALL_EVENTS) {
         sb.append("  §7").append(event).append(": ").append(this.isEventEnabled(event) ? "§aON" : "§cOFF").append("\n");
      }

      return sb.toString();
   }
}
