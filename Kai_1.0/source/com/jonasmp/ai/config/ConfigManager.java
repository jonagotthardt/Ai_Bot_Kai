package com.jonasmp.ai.config;

import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
   private final JavaPlugin plugin;

   public ConfigManager(JavaPlugin plugin) {
      this.plugin = plugin;
   }

   public void init() {
      this.plugin.saveDefaultConfig();
      this.plugin.reloadConfig();
      this.plugin.getConfig().options().copyDefaults(true);
      this.plugin.saveConfig();
      this.plugin.getLogger().info("[AI] Config loaded (missing defaults auto-filled)");
   }

   public boolean isAIEnabled() {
      return this.plugin.getConfig().getBoolean("ai.enabled");
   }

   public int getAITimeout() {
      return this.plugin.getConfig().getInt("ai.timeout_ms");
   }

   public double getBlockThreshold() {
      return this.plugin.getConfig().getDouble("moderation.block_threshold");
   }

   public double getWarnThreshold() {
      return this.plugin.getConfig().getDouble("moderation.warn_threshold");
   }

   public String getBackendUrl() {
      return this.plugin.getConfig().getString("ai.backend_url", "http://127.0.0.1:8000");
   }

   public String getHuggingFaceApiKey() {
      return this.plugin.getConfig().getString("ai.huggingface_api_key", "");
   }

   public String getGroqApiKey() {
      return this.plugin.getConfig().getString("ai.groq_api_key", "");
   }

   public String getAIProvider() {
      return this.plugin.getConfig().getString("ai.provider", "huggingface");
   }

   public String getAIModel() {
      return this.plugin.getConfig().getString("ai.model", "unitary/toxic-bert");
   }

   public boolean isWebSearchEnabled() {
      return this.plugin.getConfig().getBoolean("ai.web_search.enabled", false);
   }

   public boolean isDirectOpenRouterEnabled() {
      return this.plugin.getConfig().getBoolean("ai.direct_openrouter_enabled", true);
   }

   public String getOpenRouterApiKey() {
      return this.plugin.getConfig().getString("ai.openrouter_api_key", "");
   }

   public String getOpenRouterModel() {
      return this.plugin.getConfig().getString("ai.openrouter_model", "meta-llama/llama-3.3-70b-instruct");
   }

   public int getOpenRouterTimeout() {
      return this.plugin.getConfig().getInt("ai.openrouter_timeout_ms", 3000);
   }

   public boolean isCacheEnabled() {
      return this.plugin.getConfig().getBoolean("performance.enable_cache");
   }

   public boolean isPhysicalMemoryWarningEnabled() {
      return this.plugin.getConfig().getBoolean("performance.physical_memory_warning", true);
   }

   public int getCacheTTL() {
      return this.plugin.getConfig().getInt("performance.cache_ttl_seconds");
   }

   public double getPipelineBlockThreshold() {
      return this.plugin.getConfig().getDouble("pipeline.block_threshold", 0.55);
   }

   public double getPipelineWarnThreshold() {
      return this.plugin.getConfig().getDouble("pipeline.warn_threshold", 0.2);
   }

   public double getPipelineBanThreshold() {
      return this.plugin.getConfig().getDouble("pipeline.ban_threshold", 0.9);
   }

   public double getPipelineKickThreshold() {
      return this.plugin.getConfig().getDouble("pipeline.kick_threshold", 0.75);
   }

   public double getPipelineMuteThreshold() {
      return this.plugin.getConfig().getDouble("pipeline.mute_threshold", 0.65);
   }

   public boolean isLocalPipelineEnabled() {
      return this.plugin.getConfig().getBoolean("pipeline.local_enabled", true);
   }

   public boolean isBackendFallbackEnabled() {
      return this.plugin.getConfig().getBoolean("pipeline.backend_fallback", true);
   }

   public boolean isTeacherModeEnabled() {
      return this.plugin.getConfig().getBoolean("teacher_mode.enabled", true);
   }

   public boolean isPublicShamingEnabled() {
      return this.plugin.getConfig().getBoolean("teacher_mode.public_shaming", false);
   }

   public int getVoiceKickStrikes() {
      return this.plugin.getConfig().getInt("voice_moderation.kick_after_strikes", 5);
   }

   public int getVoiceStrikeDecayMinutes() {
      return this.plugin.getConfig().getInt("voice_moderation.strike_decay_minutes", 30);
   }
}
