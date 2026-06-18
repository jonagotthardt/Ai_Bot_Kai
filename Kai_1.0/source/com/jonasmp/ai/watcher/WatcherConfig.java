package com.jonasmp.ai.watcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WatcherConfig {
   private final JavaPlugin plugin;
   private final File watcherDir;
   private YamlConfiguration mainConfig;
   private YamlConfiguration messagesConfig;
   private YamlConfiguration namesConfig;
   private YamlConfiguration skinsConfig;
   private YamlConfiguration soundsConfig;
   private YamlConfiguration titlesConfig;
   private YamlConfiguration triggersConfig;
   private YamlConfiguration loreConfig;
   private YamlConfiguration ambientConfig;
   private YamlConfiguration stalkingConfig;
   private YamlConfiguration cavesConfig;
   private YamlConfiguration brokenScriptConfig;
   private final File patrolsDir;
   private final File investigationDir;
   private final File loreDir;
   private final File logsDir;
   private final File ambientDir;
   private final File stalkingDir;
   private final File cavesDir;
   private final File brokenScriptDir;

   public WatcherConfig(JavaPlugin plugin) {
      this.plugin = plugin;
      this.watcherDir = new File(plugin.getDataFolder(), "watcher");
      this.patrolsDir = new File(this.watcherDir, "patrols");
      this.investigationDir = new File(this.watcherDir, "investigation");
      this.loreDir = new File(this.watcherDir, "lore");
      this.logsDir = new File(this.watcherDir, "logs");
      this.ambientDir = new File(this.watcherDir, "ambient");
      this.stalkingDir = new File(this.watcherDir, "stalking");
      this.cavesDir = new File(this.watcherDir, "caves");
      this.brokenScriptDir = new File(this.watcherDir, "broken_script");
   }

   public void init() {
      this.ensureDirs();
      this.mainConfig = this.loadConfig("config.yml");
      this.messagesConfig = this.loadConfig("messages.yml");
      this.namesConfig = this.loadConfig("names.yml");
      this.skinsConfig = this.loadConfig("skins.yml");
      this.soundsConfig = this.loadConfig("sounds.yml");
      this.titlesConfig = this.loadConfig("titles.yml");
      this.triggersConfig = this.loadConfig("investigation/triggers.yml");
      this.loreConfig = this.loadConfig("lore/effects.yml");
      this.ambientConfig = this.loadConfig("ambient/settings.yml");
      this.stalkingConfig = this.loadConfig("stalking/settings.yml");
      this.cavesConfig = this.loadConfig("caves/settings.yml");
      this.brokenScriptConfig = this.loadConfig("broken_script/settings.yml");
      this.plugin.getLogger().info("[Watcher] Loaded " + this.countConfigs() + " config files from " + this.watcherDir.getPath());
   }

   private void ensureDirs() {
      this.watcherDir.mkdirs();
      this.patrolsDir.mkdirs();
      this.investigationDir.mkdirs();
      this.loreDir.mkdirs();
      this.logsDir.mkdirs();
      this.ambientDir.mkdirs();
      this.stalkingDir.mkdirs();
      this.cavesDir.mkdirs();
      this.brokenScriptDir.mkdirs();
   }

   private YamlConfiguration loadConfig(String relativePath) {
      File file = new File(this.watcherDir, relativePath);
      if (!file.exists()) {
         String resourcePath = "watcher/" + relativePath;
         if (this.plugin.getResource(resourcePath) != null) {
            this.plugin.saveResource(resourcePath, false);
         } else {
            try {
               file.getParentFile().mkdirs();
               file.createNewFile();
            } catch (IOException var5) {
               this.plugin.getLogger().warning("[Watcher] Could not create config file: " + file.getPath());
            }
         }
      }

      return YamlConfiguration.loadConfiguration(file);
   }

   private int countConfigs() {
      int count = 0;
      if (this.mainConfig != null) {
         count++;
      }

      if (this.messagesConfig != null) {
         count++;
      }

      if (this.namesConfig != null) {
         count++;
      }

      if (this.skinsConfig != null) {
         count++;
      }

      if (this.soundsConfig != null) {
         count++;
      }

      if (this.titlesConfig != null) {
         count++;
      }

      if (this.triggersConfig != null) {
         count++;
      }

      if (this.loreConfig != null) {
         count++;
      }

      if (this.ambientConfig != null) {
         count++;
      }

      if (this.stalkingConfig != null) {
         count++;
      }

      if (this.cavesConfig != null) {
         count++;
      }

      if (this.brokenScriptConfig != null) {
         count++;
      }

      return count;
   }

   public boolean isEnabled() {
      return this.mainConfig.getBoolean("enabled", true);
   }

   public boolean isAiPlayerMode() {
      return this.mainConfig.getBoolean("ai_player_mode", true);
   }

   public String getWatcherName() {
      return this.mainConfig.getString("bot.name", "Herobrine");
   }

   public boolean isVanishOnJoin() {
      return this.mainConfig.getBoolean("bot.vanish_on_join", true);
   }

   public boolean isCollisionEnabled() {
      return this.mainConfig.getBoolean("bot.collision", false);
   }

   public boolean isInvulnerable() {
      return this.mainConfig.getBoolean("bot.invulnerable", true);
   }

   public boolean showInTabList() {
      return this.mainConfig.getBoolean("bot.show_in_tab", false);
   }

   public boolean sendJoinLeaveMessages() {
      return this.mainConfig.getBoolean("bot.join_leave_messages", false);
   }

   public int getFollowDistance() {
      return this.mainConfig.getInt("movement.follow_distance", 12);
   }

   public int getInvestigationDistance() {
      return this.mainConfig.getInt("movement.investigation_distance", 6);
   }

   public double getPatrolSpeed() {
      return this.mainConfig.getDouble("movement.patrol_speed", 0.4);
   }

   public int getObservationIntervalTicks() {
      return this.mainConfig.getInt("observation.interval_ticks", 100);
   }

   public int getStrikeDecayMinutes() {
      return this.mainConfig.getInt("investigation.strike_decay_minutes", 15);
   }

   public int getInvestigationThreshold() {
      return this.mainConfig.getInt("investigation.auto_investigate_threshold", 3);
   }

   public boolean isFreezeEnabled() {
      return this.mainConfig.getBoolean("freeze.enabled", true);
   }

   public int getFreezeDurationSeconds() {
      return this.mainConfig.getInt("freeze.duration_seconds", 30);
   }

   public String getFreezeMessage() {
      return this.mainConfig.getString("freeze.message", "§cSuspicious activity detected. Please wait for staff review.");
   }

   public String getResourcePackUrl() {
      return this.mainConfig.getString("resource_pack.url", "");
   }

   public String getResourcePackHash() {
      return this.mainConfig.getString("resource_pack.hash", "");
   }

   public boolean isLoreModeEnabled() {
      return this.mainConfig.getBoolean("lore.enabled", false);
   }

   public int getLoreChancePercent() {
      return this.mainConfig.getInt("lore.appearance_chance_percent", 5);
   }

   public int getLoreMinDistance() {
      return this.mainConfig.getInt("lore.min_distance", 20);
   }

   public int getLoreMaxDistance() {
      return this.mainConfig.getInt("lore.max_distance", 50);
   }

   public String getOpenRouterApiKey() {
      return this.mainConfig.getString("ai.openrouter_api_key", "");
   }

   public String getMessage(String key, String fallback) {
      return this.messagesConfig.getString(key, fallback);
   }

   public List<String> getMessageList(String key) {
      return this.messagesConfig.getStringList(key);
   }

   public List<String> getRandomNames() {
      return this.namesConfig.getStringList("names");
   }

   public String pickRandomName() {
      List<String> names = this.getRandomNames();
      return names.isEmpty() ? this.getWatcherName() : names.get((int)(Math.random() * (double)names.size()));
   }

   public String getSkinValue() {
      return this.skinsConfig.getString("skin.value", "");
   }

   public String getSkinSignature() {
      return this.skinsConfig.getString("skin.signature", "");
   }

   public boolean useRandomSkin() {
      return this.skinsConfig.getBoolean("random_skins.enabled", false);
   }

   public List<String> getRandomSkinNames() {
      return this.skinsConfig.getStringList("random_skins.names");
   }

   public boolean isSoundEnabled(String soundKey) {
      return this.soundsConfig.getBoolean(soundKey + ".enabled", false);
   }

   public String getSoundName(String soundKey) {
      return this.soundsConfig.getString(soundKey + ".sound", "AMBIENT_CAVE");
   }

   public float getSoundVolume(String soundKey) {
      return (float)this.soundsConfig.getDouble(soundKey + ".volume", 1.0);
   }

   public float getSoundPitch(String soundKey) {
      return (float)this.soundsConfig.getDouble(soundKey + ".pitch", 1.0);
   }

   public List<String> getTitlePool(String category) {
      return this.titlesConfig.getStringList(category + ".titles");
   }

   public List<String> getSubtitlePool(String category) {
      return this.titlesConfig.getStringList(category + ".subtitles");
   }

   public boolean isTriggerEnabled(String triggerType) {
      return this.triggersConfig.getBoolean(triggerType + ".enabled", true);
   }

   public int getTriggerThreshold(String triggerType) {
      return this.triggersConfig.getInt(triggerType + ".threshold", 3);
   }

   public boolean isLoreEffectEnabled(String effect) {
      return this.loreConfig.getBoolean("effects." + effect + ".enabled", false);
   }

   public int getLoreEffectCooldownMinutes() {
      return this.loreConfig.getInt("cooldown_minutes", 10);
   }

   public boolean isAmbientEnabled() {
      return this.ambientConfig.getBoolean("enabled", true);
   }

   public int getAmbientEventIntervalMin() {
      return this.ambientConfig.getInt("event_interval_min", 15);
   }

   public int getAmbientEventIntervalMax() {
      return this.ambientConfig.getInt("event_interval_max", 45);
   }

   public boolean isAmbientEventEnabled(String event) {
      return this.ambientConfig.getBoolean("events." + event + ".enabled", true);
   }

   public int getAmbientEventChance(String event) {
      return this.ambientConfig.getInt("events." + event + ".chance", 10);
   }

   public List<String> getAmbientMessages() {
      return this.ambientConfig.getStringList("messages");
   }

   public List<String> getAmbientSignTexts() {
      return this.ambientConfig.getStringList("sign.texts");
   }

   public boolean isStalkingEnabled() {
      return this.stalkingConfig.getBoolean("enabled", true);
   }

   public int getStalkingCheckInterval() {
      return this.stalkingConfig.getInt("check_interval", 40);
   }

   public boolean isStalkingRequireDark() {
      return this.stalkingConfig.getBoolean("conditions.require_dark", true);
   }

   public boolean isStalkingRequireRain() {
      return this.stalkingConfig.getBoolean("conditions.require_rain", false);
   }

   public int getStalkingChance() {
      return this.stalkingConfig.getInt("stalk_chance", 15);
   }

   public int getStalkingDistanceMin() {
      return this.stalkingConfig.getInt("distance.min", 25);
   }

   public int getStalkingDistanceMax() {
      return this.stalkingConfig.getInt("distance.max", 40);
   }

   public int getStalkingDurationMin() {
      return this.stalkingConfig.getInt("duration.min", 8);
   }

   public int getStalkingDurationMax() {
      return this.stalkingConfig.getInt("duration.max", 15);
   }

   public List<String> getStalkingMessages() {
      return this.stalkingConfig.getStringList("messages");
   }

   public boolean isCaveDwellerEnabled() {
      return this.cavesConfig.getBoolean("enabled", true);
   }

   public int getCaveCheckInterval() {
      return this.cavesConfig.getInt("check_interval", 2);
   }

   public int getCaveDarknessWarningAt() {
      return this.cavesConfig.getInt("darkness_score.warning_at", 5);
   }

   public int getCaveDarknessAppearAt() {
      return this.cavesConfig.getInt("darkness_score.appear_at", 10);
   }

   public int getCaveDarknessHorrorAt() {
      return this.cavesConfig.getInt("darkness_score.horror_at", 15);
   }

   public int getCaveDarkLightLevel() {
      return this.cavesConfig.getInt("dark_light_level", 4);
   }

   public boolean isCaveLightFlickerEnabled() {
      return this.cavesConfig.getBoolean("light_flicker.enabled", true);
   }

   public int getCaveAppearanceMinDistance() {
      return this.cavesConfig.getInt("appearance.min_distance", 5);
   }

   public int getCaveAppearanceMaxDistance() {
      return this.cavesConfig.getInt("appearance.max_distance", 12);
   }

   public List<String> getCaveWarningMessages() {
      return this.cavesConfig.getStringList("messages.warning");
   }

   public File getWatcherDir() {
      return this.watcherDir;
   }

   public File getPatrolsDir() {
      return this.patrolsDir;
   }

   public File getInvestigationDir() {
      return this.investigationDir;
   }

   public File getLoreDir() {
      return this.loreDir;
   }

   public File getLogsDir() {
      return this.logsDir;
   }

   public File getAmbientDir() {
      return this.ambientDir;
   }

   public File getStalkingDir() {
      return this.stalkingDir;
   }

   public File getCavesDir() {
      return this.cavesDir;
   }

   public File getBrokenScriptDir() {
      return this.brokenScriptDir;
   }

   public YamlConfiguration getAmbientConfig() {
      return this.ambientConfig;
   }

   public YamlConfiguration getStalkingConfig() {
      return this.stalkingConfig;
   }

   public YamlConfiguration getCavesConfig() {
      return this.cavesConfig;
   }

   public YamlConfiguration getBrokenScriptConfig() {
      return this.brokenScriptConfig;
   }

   public boolean isBrokenScriptEnabled() {
      return this.brokenScriptConfig.getBoolean("enabled", true);
   }

   public int getBrokenScriptEventIntervalMin() {
      return this.brokenScriptConfig.getInt("event_interval_min", 20);
   }

   public int getBrokenScriptEventIntervalMax() {
      return this.brokenScriptConfig.getInt("event_interval_max", 50);
   }

   public List<String> getBrokenScriptEnabledEvents() {
      return this.brokenScriptConfig.getStringList("random_events");
   }

   public boolean isBrokenScriptKeywordsEnabled() {
      return this.brokenScriptConfig.getBoolean("keyword_events.enabled", true);
   }

   public int getBrokenScriptKeywordCooldown() {
      return this.brokenScriptConfig.getInt("keyword_events.cooldown_seconds", 30);
   }

   public List<String> getBrokenScriptParanoiaMessages() {
      return this.brokenScriptConfig.getStringList("paranoia_messages");
   }

   public List<YamlConfiguration> loadPatrolConfigs() {
      List<YamlConfiguration> configs = new ArrayList<>();
      File[] files = this.patrolsDir.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files == null) {
         return configs;
      } else {
         for (File file : files) {
            configs.add(YamlConfiguration.loadConfiguration(file));
         }

         return configs;
      }
   }

   public void reload() {
      this.init();
   }
}
