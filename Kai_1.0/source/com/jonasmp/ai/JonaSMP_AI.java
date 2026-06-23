package com.jonasmp.ai;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.commands.AICommands;
import com.jonasmp.ai.core.AFKWakeUpListener;
import com.jonasmp.ai.core.ActivityTracker;
import com.jonasmp.ai.core.EventRouter;
import com.jonasmp.ai.core.GreetingEngine;
import com.jonasmp.ai.core.GriefingDetector;
import com.jonasmp.ai.core.LanguageChatListener;
import com.jonasmp.ai.core.LanguageInventoryListener;
import com.jonasmp.ai.core.LanguagePrefixManager;
import com.jonasmp.ai.core.PatternExtractor;
import com.jonasmp.ai.core.PlayerWatcher;
import com.jonasmp.ai.core.SelfCorrection;
import com.jonasmp.ai.core.ServerMonitor;
import com.jonasmp.ai.memory.PlayerActivityListener;
import com.jonasmp.ai.radar.ChunkRadar;
import com.jonasmp.ai.rules.RuleInventoryListener;
import com.jonasmp.ai.shop.ShopCommandExecutor;
import com.jonasmp.ai.watcher.WatcherCommands;
import com.jonasmp.ai.watcher.WatcherCore;
import com.jonasmp.ai.watcher.WatcherEvents;
import com.jonasmp.ai.watcher.WatcherProtection;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class JonaSMP_AI extends JavaPlugin {
   private static JonaSMP_AI instance;
   private ChunkRadar chunkRadar;

   public void onLoad() {
      instance = this;
      this.getLogger().info("[AI] Loading core system...");
   }

   public void onEnable() {
      this.saveDefaultConfig();
      CoreBootstrap.init(this);
      this.getServer().getPluginManager().registerEvents(new EventRouter(CoreBootstrap.MUTE_MANAGER), this);
      this.getServer().getPluginManager().registerEvents(new PlayerActivityListener(), this);
      this.getServer().getPluginManager().registerEvents(new PlayerWatcher(), this);
      new PatternExtractor().start();
      this.getServer().getPluginManager().registerEvents(new SelfCorrection(), this);
      this.chunkRadar = new ChunkRadar();
      this.getServer().getPluginManager().registerEvents(this.chunkRadar.getDeltaTracker(), this);
      Bukkit.getScheduler().runTaskLater(this, () -> {
         try {
            WatcherCore core = WatcherCore.getInstance();
            if (!core.isRunning()) {
               core.init();
               this.getLogger().info("[AI] WatcherCore auto-initialized. Kai should be spawning...");
            }
         } catch (Exception var2x) {
            this.getLogger().warning("[AI] WatcherCore auto-init failed: " + var2x.getMessage());
         }
      }, 60L);
      AICommands commands = new AICommands(CoreBootstrap.MUTE_MANAGER);
      PluginCommand aiCmd = this.getCommand("ai");
      if (aiCmd != null) {
         aiCmd.setExecutor(commands);
         aiCmd.setTabCompleter(commands);
      } else {
         this.getLogger().severe("[AI] Command /ai NOT registered in plugin.yml!");
      }

      PluginCommand fCmd = this.getCommand("f");
      if (fCmd != null) {
         fCmd.setExecutor(commands);
         fCmd.setTabCompleter(commands);
      } else {
         this.getLogger().severe("[AI] Command /f NOT registered in plugin.yml!");
      }

      WatcherCommands watcherCommands = new WatcherCommands();
      PluginCommand watcherCmd = this.getCommand("watcher");
      if (watcherCmd != null) {
         watcherCmd.setExecutor(watcherCommands);
         watcherCmd.setTabCompleter(watcherCommands);
      } else {
         this.getLogger().severe("[AI] Command /watcher NOT registered in plugin.yml!");
      }

      ShopCommandExecutor shopCommands = new ShopCommandExecutor(CoreBootstrap.SHOP_BRIDGE);

      for (String shopCmd : new String[]{"shopbuy", "shopsell", "shopprice", "shoplist", "shopsearch"}) {
         PluginCommand cmd = this.getCommand(shopCmd);
         if (cmd != null) {
            cmd.setExecutor(shopCommands);
            cmd.setTabCompleter(shopCommands);
         } else {
            this.getLogger().severe("[AI] Command /" + shopCmd + " NOT registered in plugin.yml!");
         }
      }

      new ServerMonitor().start();
      this.getServer().getPluginManager().registerEvents(new GreetingEngine(), this);
      ActivityTracker activityTracker = new ActivityTracker();
      this.getServer().getPluginManager().registerEvents(activityTracker, this);
      activityTracker.start();
      this.getServer().getPluginManager().registerEvents(new GriefingDetector(), this);
      this.getServer().getPluginManager().registerEvents(new AFKWakeUpListener(this), this);
      Bukkit.getScheduler().runTaskLater(this, () -> LanguagePrefixManager.init(this), 1L);
      this.getServer().getPluginManager().registerEvents(new LanguageInventoryListener(), this);
      this.getServer().getPluginManager().registerEvents(new LanguageChatListener(), this);
      this.getServer().getPluginManager().registerEvents(new RuleInventoryListener(), this);
      new WatcherEvents(CoreBootstrap.WATCHER).register();
      new WatcherProtection(CoreBootstrap.WATCHER).register();
      this.getLogger().info("[AI] Plugin fully active");
      this.getLogger().info("[AI] AI Provider: HuggingFace (model=" + CoreBootstrap.CONFIG.getAIModel());
      this.getLogger().info("[AI] Commands: /ai mute, /ai unmute, /ai status, /ai reload");
   }

   public void onDisable() {
      if (CoreBootstrap.MEMORY_ENGINE != null) {
         CoreBootstrap.MEMORY_ENGINE.saveAll();
         CoreBootstrap.MEMORY_ENGINE.exportAllProfiles();
      }

      if (CoreBootstrap.THREAT_MANAGER != null) {
         CoreBootstrap.THREAT_MANAGER.save();
      }

      if (CoreBootstrap.MESSAGE_CACHE != null) {
         CoreBootstrap.MESSAGE_CACHE.shutdown();
      }

      if (CoreBootstrap.WATCHER != null) {
         CoreBootstrap.WATCHER.shutdown();
      }

      this.getLogger().info("[AI] Shutting down system...");
   }

   public static JonaSMP_AI getInstance() {
      return instance;
   }

   public ChunkRadar getChunkRadar() {
      return this.chunkRadar;
   }
}
