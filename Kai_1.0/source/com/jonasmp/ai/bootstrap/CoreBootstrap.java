package com.jonasmp.ai.bootstrap;

import com.jonasmp.ai.cache.MessageCache;
import com.jonasmp.ai.config.ConfigManager;
import com.jonasmp.ai.death.DeathMessageSystem;
import com.jonasmp.ai.feedback.FalsePositiveStore;
import com.jonasmp.ai.feedback.LastBlockedMessageStore;
import com.jonasmp.ai.gateway.AIGateway;
import com.jonasmp.ai.gateway.WebSearchTool;
import com.jonasmp.ai.language.PlayerLanguageStore;
import com.jonasmp.ai.memory.MemoryEngine;
import com.jonasmp.ai.moderation.BanHistoryManager;
import com.jonasmp.ai.moderation.MuteManager;
import com.jonasmp.ai.personality.PersonalityEngine;
import com.jonasmp.ai.pipeline.ChatModerationPipeline;
import com.jonasmp.ai.rules.FirstJoinTracker;
import com.jonasmp.ai.shop.DynamicShopBridge;
import com.jonasmp.ai.threat.ThreatDecayTask;
import com.jonasmp.ai.threat.ThreatScoreManager;
import com.jonasmp.ai.watcher.WatcherCore;
import com.jonasmp.ai.wordlist.WordlistLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CoreBootstrap {
   public static ConfigManager CONFIG;
   public static AIGateway AI_GATEWAY;
   public static WebSearchTool WEB_SEARCH;
   public static ThreatScoreManager THREAT_MANAGER;
   public static ThreatDecayTask THREAT_DECAY_TASK;
   public static MemoryEngine MEMORY_ENGINE;
   public static PersonalityEngine PERSONALITY_ENGINE;
   public static MuteManager MUTE_MANAGER;
   public static BanHistoryManager BAN_HISTORY_MANAGER;
   public static FalsePositiveStore FALSE_POSITIVE_STORE;
   public static LastBlockedMessageStore LAST_BLOCKED_STORE;
   public static PlayerLanguageStore PLAYER_LANGUAGE_STORE;
   public static FirstJoinTracker FIRST_JOIN_TRACKER;
   public static WordlistLoader WORDLIST_LOADER;
   public static MessageCache MESSAGE_CACHE;
   public static ChatModerationPipeline MODERATION_PIPELINE;
   public static WatcherCore WATCHER;
   public static DeathMessageSystem DEATH_MESSAGES;
   public static DynamicShopBridge SHOP_BRIDGE;
   public static JavaPlugin PLUGIN;

   public static void init(JavaPlugin plugin) {
      PLUGIN = plugin;
      (CONFIG = new ConfigManager(plugin)).init();
      MEMORY_ENGINE = new MemoryEngine();
      THREAT_MANAGER = new ThreatScoreManager(plugin);
      String hfKey = CONFIG.getHuggingFaceApiKey();
      String hfModel = CONFIG.getAIModel();
      int hfTimeout = CONFIG.getAITimeout();
      String orKey = CONFIG.getOpenRouterApiKey();
      String orModel = CONFIG.getOpenRouterModel();
      int orTimeout = CONFIG.getOpenRouterTimeout();
      AI_GATEWAY = new AIGateway(hfKey, hfModel, hfTimeout, orKey, orModel, orTimeout);
      plugin.getLogger().info("[AI] HuggingFace + OpenRouter gateway initialized (HF=" + hfModel + ", OR=" + orModel);
      if (CONFIG.isWebSearchEnabled()) {
         WEB_SEARCH = new WebSearchTool();
         AI_GATEWAY.setWebSearchTool(WEB_SEARCH);
         plugin.getLogger().info("[AI] Web search enabled (DuckDuckGo)");
      }

      if (orKey != null && orKey.length() > 10) {
         Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> AI_GATEWAY.runOrHealthCheck());
      }

      MUTE_MANAGER = new MuteManager();
      BAN_HISTORY_MANAGER = new BanHistoryManager(plugin);
      FALSE_POSITIVE_STORE = new FalsePositiveStore(plugin.getDataFolder());
      LAST_BLOCKED_STORE = new LastBlockedMessageStore();
      PERSONALITY_ENGINE = new PersonalityEngine();
      (WORDLIST_LOADER = new WordlistLoader()).load();
      (MESSAGE_CACHE = new MessageCache()).setDefaultTtl((long)CONFIG.getCacheTTL() * 1000L);
      PLAYER_LANGUAGE_STORE = new PlayerLanguageStore();
      FIRST_JOIN_TRACKER = new FirstJoinTracker();
      MODERATION_PIPELINE = new ChatModerationPipeline(WORDLIST_LOADER, MESSAGE_CACHE);
      (THREAT_DECAY_TASK = new ThreatDecayTask()).start();
      (WATCHER = WatcherCore.getInstance()).init();
      (DEATH_MESSAGES = new DeathMessageSystem()).register();
      SHOP_BRIDGE = new DynamicShopBridge(plugin.getLogger());
      plugin.getLogger().info("[AI] Core systems initialized");
      plugin.getLogger().info("[AI] Memory + Personality active");
      plugin.getLogger().info("[AI] Threat + AI Gateway online");
      plugin.getLogger().info("[AI] Pipeline loaded with " + WORDLIST_LOADER.getCategoryNames().size() + " categories");
   }
}
