package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherCore {
   private static WatcherCore INSTANCE;
   private final WatcherConfig config;
   private final AIPlayerBot aiPlayerBot;
   private final Map<UUID, WatcherPlayerData> playerData = new ConcurrentHashMap<>();
   private boolean running = false;
   private BukkitRunnable tickTask = null;
   private final AIBridgeWriter bridgeWriter;
   private TeaseBehavior teaseBehavior;

   private WatcherCore() {
      this.config = new WatcherConfig(CoreBootstrap.PLUGIN);
      this.aiPlayerBot = new AIPlayerBot();
      this.bridgeWriter = new AIBridgeWriter(CoreBootstrap.PLUGIN.getDataFolder());
   }

   public static synchronized WatcherCore getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new WatcherCore();
      }

      return INSTANCE;
   }

   public void init() {
      this.config.init();
      if (!this.config.isEnabled()) {
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] System disabled in config.");
      } else {
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Initializing AI Player Bot system...");
         String apiKey = CoreBootstrap.PLUGIN.getConfig().getString("ai.openrouter_api_key", "");
         if (apiKey == null || apiKey.isBlank()) {
            apiKey = this.config.getOpenRouterApiKey();
         }

         if (apiKey != null && !apiKey.isBlank()) {
            this.aiPlayerBot.setApiKey(apiKey);
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher] OpenRouter API key loaded for AI bot.");
         } else {
            CoreBootstrap.PLUGIN
               .getLogger()
               .warning("[Watcher] No OpenRouter API key configured. AI bot will not work. Set 'ai.openrouter_api_key' in plugins/JonaSMP_AI/config.yml");
         }

         Bukkit.getPluginManager().registerEvents(new BotRespawnListener(this.aiPlayerBot), CoreBootstrap.PLUGIN);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] AI bot respawn listener registered.");
         Bukkit.getPluginManager().registerEvents(new BotChatListener(), CoreBootstrap.PLUGIN);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] AI bot chat listener registered. Say 'Kai, ...' to talk to the bot.");
         Bukkit.getScheduler()
            .runTaskLater(
               CoreBootstrap.PLUGIN,
               () -> {
                  if (!this.aiPlayerBot.isSpawned()) {
                     Location spawnLoc = AIPlayerBot.loadLastLocation();
                     if (spawnLoc == null) {
                        spawnLoc = ((World)Bukkit.getWorlds().get(0)).getSpawnLocation();
                        spawnLoc.setY((double)(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1));
                        CoreBootstrap.PLUGIN.getLogger().info("[Watcher] No saved location found, using world spawn.");
                     } else {
                        CoreBootstrap.PLUGIN
                           .getLogger()
                           .info(
                              "[Watcher] Restored bot location: "
                                 + spawnLoc.getWorld().getName()
                                 + " "
                                 + (int)spawnLoc.getX()
                                 + ","
                                 + (int)spawnLoc.getY()
                                 + ","
                                 + (int)spawnLoc.getZ()
                           );
                     }

                     this.aiPlayerBot.spawn(spawnLoc, "Kai");
                     CoreBootstrap.PLUGIN.getLogger().info("[Watcher] AI bot auto-spawned.");
                  }
               },
               40L
            );
         this.teaseBehavior = new TeaseBehavior(this);
         this.teaseBehavior.start();
         this.startTickLoop();
         this.running = true;
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] AI Player Bot active. Tease mode ON for mka197.");
      }
   }

   public void shutdown() {
      this.running = false;
      if (this.teaseBehavior != null) {
         this.teaseBehavior.stop();
         this.teaseBehavior = null;
      }

      if (this.tickTask != null) {
         this.tickTask.cancel();
         this.tickTask = null;
      }

      this.aiPlayerBot.despawn();
      CoreBootstrap.PLUGIN.getLogger().info("[Watcher] System shutdown.");
   }

   public void reload() {
      this.shutdown();
      this.playerData.clear();
      CoreBootstrap.PLUGIN.reloadConfig();
      this.init();
   }

   private void startTickLoop() {
      (this.tickTask = new BukkitRunnable() {
         {
            Objects.requireNonNull(WatcherCore.this);
         }

         public void run() {
            if (WatcherCore.this.running) {
               for (WatcherPlayerData data : WatcherCore.this.playerData.values()) {
                  data.decayStrikes(WatcherCore.this.config.getStrikeDecayMinutes());
               }
            }
         }
      }).runTaskTimer(CoreBootstrap.PLUGIN, 20L, 20L);
   }

   public WatcherPlayerData getPlayerData(UUID uuid) {
      return this.playerData.get(uuid);
   }

   public WatcherPlayerData ensurePlayerData(UUID uuid, String name) {
      return this.playerData.computeIfAbsent(uuid, k -> new WatcherPlayerData(uuid, name));
   }

   public Map<UUID, WatcherPlayerData> getAllPlayerData() {
      return new ConcurrentHashMap<>(this.playerData);
   }

   public WatcherConfig getConfig() {
      return this.config;
   }

   public AIPlayerBot getAIPlayerBot() {
      return this.aiPlayerBot;
   }

   public boolean isRunning() {
      return this.running;
   }

   public WatcherBot getBot() {
      return null;
   }

   public WatcherMovement getMovement() {
      return null;
   }

   public WatcherObserver getObserver() {
      return null;
   }

   public WatcherInvestigation getInvestigation() {
      return null;
   }

   public WatcherFreeze getFreeze() {
      return null;
   }

   public WatcherLore getLore() {
      return null;
   }

   public WatcherHorror getHorror() {
      return null;
   }

   public WatcherVisuals getVisuals() {
      return null;
   }

   public WatcherHerobrineBehavior getHerobrine() {
      return null;
   }

   public WatcherAmbientHorror getAmbientHorror() {
      return null;
   }

   public WatcherStalking getStalking() {
      return null;
   }

   public WatcherCaveDweller getCaveDweller() {
      return null;
   }

   public WatcherBrokenScript getBrokenScript() {
      return null;
   }

   public WatcherToggles getToggles() {
      return null;
   }

   public void beginHorror(Player player) {
   }

   public void endHorror() {
   }

   public void beginUltimateHerobrineMode(Player player) {
   }

   public void endUltimateHerobrineMode() {
   }

   public void triggerLore(Player player) {
   }

   public void observePlayer(UUID uuid) {
   }

   public void beginInvestigation(UUID uuid) {
   }

   public void showVisible(UUID uuid) {
   }

   public void freezePlayer(UUID uuid) {
   }

   public void unfreezePlayer(UUID uuid) {
   }

   public void goIdle() {
   }

   public WatcherState getState() {
      return WatcherState.DISABLED;
   }

   public void setState(WatcherState state) {
   }

   public UUID getAssignedPlayer() {
      return null;
   }

   public AntiCheatBridge getAntiCheatBridge() {
      return null;
   }

   public AIBridgeWriter getBridgeWriter() {
      return this.bridgeWriter;
   }
}
