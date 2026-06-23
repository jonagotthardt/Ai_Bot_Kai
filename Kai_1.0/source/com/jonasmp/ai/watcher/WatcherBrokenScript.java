package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherBrokenScript implements Listener {
   private final WatcherCore core;
   private final Random random = ThreadLocalRandom.current();
   private BukkitRunnable mainTask = null;
   private boolean running = false;
   private final Map<String, String> keywordMap = new HashMap<>();
   private final Map<UUID, Long> keywordCooldown = new HashMap<>();
   private final Set<UUID> brokenPlayers = new HashSet<>();

   public WatcherBrokenScript(WatcherCore core) {
      this.core = core;
      this.initKeywords();
   }

   private void initKeywords() {
      this.keywordMap.put("help", "fake_chat");
      this.keywordMap.put("hilfe", "fake_chat");
      this.keywordMap.put("watcher", "sound_glitch");
      this.keywordMap.put("herobrine", "camera_glitch");
      this.keywordMap.put("null", "block_corruption");
      this.keywordMap.put("error", "health_glitch");
      this.keywordMap.put("angst", "inventory_shuffle");
      this.keywordMap.put("fear", "inventory_shuffle");
      this.keywordMap.put("broken", "sky_glitch");
      this.keywordMap.put("script", "door_anomaly");
      this.keywordMap.put("jona", "torch_extinguish");
      this.keywordMap.put("tp", "block_under_foot");
      this.keywordMap.put("kill", "sound_glitch");
      this.keywordMap.put("stop", "chest_anomaly");
      this.keywordMap.put("leave", "item_vanish");
      this.keywordMap.put("exit", "temp_ban");
   }

   public void start() {
      if (!this.running) {
         if (!this.core.getConfig().isBrokenScriptEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/TBS] Broken Script disabled in config.");
         } else if (!this.core.getToggles().isBrokenScriptEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/TBS] Broken Script disabled by toggle.");
         } else {
            this.running = true;
            Bukkit.getPluginManager().registerEvents(this, CoreBootstrap.PLUGIN);
            final int intervalMin = this.core.getConfig().getBrokenScriptEventIntervalMin();
            final int intervalMax = this.core.getConfig().getBrokenScriptEventIntervalMax();
            (this.mainTask = new BukkitRunnable() {
                  long nextEventTick;
                  long tick;

                  {
                     Objects.requireNonNull(WatcherBrokenScript.this);
                     this.nextEventTick = (long)(intervalMin * 20 + WatcherBrokenScript.this.random.nextInt((intervalMax - intervalMin) * 20));
                     this.tick = 0L;
                  }

                  public void run() {
                     if (!WatcherBrokenScript.this.core.isRunning()) {
                        WatcherBrokenScript.this.stop();
                     } else {
                        if (this.tick >= this.nextEventTick) {
                           if (WatcherBrokenScript.this.core.getToggles().isHorrorGlobalEnabled()
                              && WatcherBrokenScript.this.core.getToggles().isBrokenScriptEnabled()) {
                              WatcherBrokenScript.this.tryRandomEvent();
                           }

                           this.nextEventTick = this.tick
                              + (long)(intervalMin * 20)
                              + (long)WatcherBrokenScript.this.random.nextInt((intervalMax - intervalMin) * 20);
                        }

                        if (this.tick % 100L == 0L
                           && WatcherBrokenScript.this.core.getToggles().isHorrorGlobalEnabled()
                           && WatcherBrokenScript.this.core.getToggles().isBrokenScriptEnabled()) {
                           WatcherBrokenScript.this.checkParanoia();
                        }

                        this.tick++;
                     }
                  }
               })
               .runTaskTimer(CoreBootstrap.PLUGIN, 200L, 1L);
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/TBS] Broken Script module active.");
         }
      }
   }

   public void stop() {
      this.running = false;
      HandlerList.unregisterAll(this);
      if (this.mainTask != null) {
         this.mainTask.cancel();
         this.mainTask = null;
      }

      this.keywordCooldown.clear();
      this.brokenPlayers.clear();
   }

   private void tryRandomEvent() {
      Player target = this.getRandomOnlinePlayer();
      if (target != null && target.isOnline()) {
         this.triggerRandomEventOn(target);
      }
   }

   public void triggerRandomEventOn(Player target) {
      if (target.isOnline()) {
         if (this.core.getToggles().isHorrorGlobalEnabled() && this.core.getToggles().isBrokenScriptEnabled()) {
            List<String> enabled = this.core.getConfig().getBrokenScriptEnabledEvents();
            if (!enabled.isEmpty()) {
               String event = enabled.get(this.random.nextInt(enabled.size()));
               this.executeEvent(target, event);
            }
         }
      }
   }

   public void triggerSpecificEvent(Player target, String eventType) {
      if (target.isOnline()) {
         if (this.core.getToggles().isHorrorGlobalEnabled() && this.core.getToggles().isBrokenScriptEnabled()) {
            this.executeEvent(target, eventType);
         }
      }
   }

   private void executeEvent(Player target, String eventType) {
      if (this.core.getToggles().isHorrorGlobalEnabled() && this.core.getToggles().isBrokenScriptEnabled()) {
         if (target == null || !this.core.getAIPlayerBot().isBotPlayer(target)) {
            if (!this.core.getToggles().isEventEnabled(eventType)) {
               CoreBootstrap.PLUGIN.getLogger().fine("[TBS] Event '" + eventType + "' skipped (toggle OFF).");
            } else {
               switch (eventType) {
                  case "block_corruption":
                     BrokenScriptEvents.blockCorruption(target, 3, 8);
                     break;
                  case "item_vanish":
                     BrokenScriptEvents.itemVanish(target, 100 + this.random.nextInt(100));
                     break;
                  case "door_anomaly":
                     BrokenScriptEvents.doorAnomaly(target, 10);
                     break;
                  case "chest_anomaly":
                     BrokenScriptEvents.chestAnomaly(target, 8);
                     break;
                  case "torch_extinguish":
                     BrokenScriptEvents.torchExtinguish(target, 6);
                     break;
                  case "fake_chat":
                     BrokenScriptEvents.fakeChat(target);
                     break;
                  case "health_glitch":
                     BrokenScriptEvents.healthGlitch(target);
                     break;
                  case "camera_glitch":
                     BrokenScriptEvents.cameraGlitch(target);
                     break;
                  case "entity_swap":
                     CoreBootstrap.PLUGIN.getLogger().info("[TBS] entity_swap requested but is disabled (protects player entities)");
                     break;
                  case "sound_glitch":
                     BrokenScriptEvents.soundGlitch(target);
                     break;
                  case "inventory_shuffle":
                     BrokenScriptEvents.inventoryShuffle(target);
                     break;
                  case "sky_glitch":
                     BrokenScriptEvents.skyGlitch(target);
                     break;
                  case "block_under_foot":
                     BrokenScriptEvents.blockUnderFoot(target);
                     break;
                  case "broken_script_end":
                     BrokenScriptEvents.brokenScriptEnd(target);
                     break;
                  case "temp_ban":
                     BrokenScriptEvents.tempBanKick(target, 5 + this.random.nextInt(10));
                     break;
                  default:
                     CoreBootstrap.PLUGIN.getLogger().warning("[TBS] Unknown event: " + eventType);
               }
            }
         }
      }
   }

   private void checkParanoia() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (!player.equals(this.core.getBot().getBotPlayer()) && !this.core.getAIPlayerBot().isBotPlayer(player)) {
            UUID uuid = player.getUniqueId();
            if (!this.brokenPlayers.contains(uuid) && this.random.nextDouble() < 0.01) {
               this.brokenPlayers.add(uuid);
               CoreBootstrap.PLUGIN.getLogger().info("[TBS] " + player.getName() + " is now being watched by the script.");
            }

            if (this.brokenPlayers.contains(uuid) && !(this.random.nextDouble() >= 0.02)) {
               List<String> msgs = this.core.getConfig().getBrokenScriptParanoiaMessages();
               if (!msgs.isEmpty()) {
                  player.sendMessage(msgs.get(this.random.nextInt(msgs.size())));
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      if (this.running) {
         if (this.core.getToggles().isHorrorGlobalEnabled() && this.core.getToggles().isBrokenScriptEnabled()) {
            if (this.core.getConfig().isBrokenScriptKeywordsEnabled()) {
               Player player = event.getPlayer();
               String msg = event.getMessage().toLowerCase();
               long now = System.currentTimeMillis();
               if (this.keywordCooldown.containsKey(player.getUniqueId())) {
                  long last = this.keywordCooldown.get(player.getUniqueId());
                  int cooldownSec = this.core.getConfig().getBrokenScriptKeywordCooldown();
                  if (now - last < (long)cooldownSec * 1000L) {
                     return;
                  }
               }

               for (Entry<String, String> entry : this.keywordMap.entrySet()) {
                  if (msg.contains(entry.getKey())) {
                     String eventType = entry.getValue();
                     if (this.core.getToggles().isEventEnabled(eventType)) {
                        this.keywordCooldown.put(player.getUniqueId(), now);
                        Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
                           if (player.isOnline()) {
                              this.executeEvent(player, eventType);
                           }
                        });
                        CoreBootstrap.PLUGIN.getLogger().info("[TBS] Keyword '" + entry.getKey() + "' triggered by " + player.getName());
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   public void forceBrokenScriptEnd(Player target) {
      if (target.isOnline()) {
         BrokenScriptEvents.brokenScriptEnd(target);
      }
   }

   public void forceTempBan(Player target, int seconds) {
      if (target.isOnline()) {
         BrokenScriptEvents.tempBanKick(target, seconds);
      }
   }

   public boolean isPlayerBroken(Player player) {
      return this.brokenPlayers.contains(player.getUniqueId());
   }

   public Set<UUID> getBrokenPlayers() {
      return new HashSet<>(this.brokenPlayers);
   }

   private Player getRandomOnlinePlayer() {
      List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
      players.removeIf(p -> p.equals(this.core.getBot().getBotPlayer()));
      players.removeIf(p -> this.core.getAIPlayerBot().isBotPlayer(p));
      return players.isEmpty() ? null : players.get(this.random.nextInt(players.size()));
   }
}
