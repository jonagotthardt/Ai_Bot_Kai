package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.config.DebugConfig;
import com.jonasmp.ai.decision.DecisionResult;
import com.jonasmp.ai.moderation.MuteManager;
import com.jonasmp.ai.pipeline.ChatModerationPipeline;
import com.jonasmp.ai.punishment.AdaptivePunishmentEngine;
import com.jonasmp.ai.punishment.PunishmentResult;
import com.jonasmp.ai.raid.RaidDetector;
import com.jonasmp.ai.threat.ThreatScoreManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventRouter implements Listener {
   private final ThreatScoreManager threatManager = CoreBootstrap.THREAT_MANAGER;
   private final RaidDetector raidDetector = new RaidDetector();
   private final AdaptivePunishmentEngine adaptivePunishmentEngine = new AdaptivePunishmentEngine();
   private final MuteManager muteManager;
   private final TeacherModeResponder teacherResponder;

   public EventRouter(MuteManager muteManager) {
      this.muteManager = muteManager;
      this.teacherResponder = new TeacherModeResponder();
   }

   private void sync(Runnable run) {
      Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, run);
   }

   @EventHandler
   public void onChat(AsyncChatEvent event) {
      try {
         this.handleChatInternal(event);
      } catch (Throwable var3) {
         CoreBootstrap.PLUGIN
            .getLogger()
            .severe("[AI] CRITICAL: Chat handler crashed for " + event.getPlayer().getName() + ". Allowing message through. Error: " + var3.getMessage());
         var3.printStackTrace();
         event.getPlayer().sendMessage("§c[AI] Internal error – your message was allowed.");
      }
   }

   private void handleChatInternal(AsyncChatEvent event) {
      if (!event.isCancelled()) {
         Player player = event.getPlayer();
         UUID uuid = player.getUniqueId();
         String playerId = uuid.toString();
         if (this.muteManager.isMuted(uuid)) {
            long remaining = this.muteManager.getRemainingMs(uuid);
            String reason = this.muteManager.getMuteReason(uuid);
            event.setCancelled(true);
            player.sendMessage("§c§l[AI] You are muted!");
            player.sendMessage("§cRemaining: " + this.formatTime(remaining));
            if (reason != null) {
               player.sendMessage("§cReason: " + reason);
            }
         } else {
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            if (!player.hasPermission("jonasmpai.moderation.bypass")) {
               if (this.raidDetector.isRaid(uuid)) {
                  CoreBootstrap.MEMORY_ENGINE.update(playerId, "RAID");
                  this.threatManager.addThreat(uuid, 30.0);
                  event.setCancelled(true);
                  this.sync(() -> {
                     player.sendMessage("§cChat Raid detected!");
                     player.kick(Component.text("Chat Raid detected!"));
                  });
               } else {
                  ChatModerationPipeline pipeline = CoreBootstrap.MODERATION_PIPELINE;
                  if (pipeline != null) {
                     ChatModerationPipeline.PipelineResult result = pipeline.process(message, playerId);
                     if (result == null) {
                        CoreBootstrap.PLUGIN.getLogger().warning("[AI] Pipeline returned null for player " + player.getName());
                     } else {
                        if (DebugConfig.isDebugEnabled()) {
                           String flagsStr = "";
                           if (result.flags != null) {
                              flagsStr = " | Obf="
                                 + result.flags.obfuscation
                                 + " | Uni="
                                 + result.flags.unicodeAttack
                                 + " | Emoji="
                                 + result.flags.emojiInjection;
                           }

                           CoreBootstrap.PLUGIN
                              .getLogger()
                              .info(
                                 "[AI-DEBUG] Player="
                                    + player.getName()
                                    + " | Msg=\""
                                    + message
                                    + "\" | Action="
                                    + result.action
                                    + " | Score="
                                    + String.format("%.2f", result.score)
                                    + " | Cat="
                                    + result.category
                                    + " | Ms="
                                    + result.processingTimeMs
                                    + flagsStr
                              );
                        }

                        if (result.action == DecisionResult.Action.BAN) {
                           event.setCancelled(true);
                           if (CoreBootstrap.LAST_BLOCKED_STORE != null) {
                              CoreBootstrap.LAST_BLOCKED_STORE.recordBlocked(uuid, message);
                           }

                           this.threatManager.addThreat(uuid, 50.0);
                           CoreBootstrap.MEMORY_ENGINE.update(playerId, "AI_BAN");
                           this.sync(() -> {
                              this.teacherResponder.respond(player, result.action, message, result.score);
                              int banCount = CoreBootstrap.BAN_HISTORY_MANAGER.incrementBanCount(uuid);
                              String duration = CoreBootstrap.BAN_HISTORY_MANAGER.getBanDuration(uuid);
                              if ("permanent".equals(duration)) {
                                 Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + player.getName() + " " + result.reason);
                              } else {
                                 Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + player.getName() + " " + duration + " " + result.reason);
                              }

                              CoreBootstrap.PLUGIN.getLogger().warning("[AI] Progressive ban #" + banCount + " for " + player.getName());
                           });
                        } else if (result.action == DecisionResult.Action.KICK) {
                           event.setCancelled(true);
                           if (CoreBootstrap.LAST_BLOCKED_STORE != null) {
                              CoreBootstrap.LAST_BLOCKED_STORE.recordBlocked(uuid, message);
                           }

                           this.threatManager.addThreat(uuid, 30.0);
                           CoreBootstrap.MEMORY_ENGINE.update(playerId, "AI_KICK");
                           this.sync(() -> {
                              this.teacherResponder.respond(player, result.action, message, result.score);
                              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " AI Kick: " + result.reason);
                           });
                        } else if (result.action == DecisionResult.Action.BLOCK) {
                           event.setCancelled(true);
                           if (CoreBootstrap.LAST_BLOCKED_STORE != null) {
                              CoreBootstrap.LAST_BLOCKED_STORE.recordBlocked(uuid, message);
                           }

                           this.threatManager.addThreat(uuid, 20.0);
                           CoreBootstrap.MEMORY_ENGINE.update(playerId, "AI_BLOCK");
                           PunishmentResult punishment = this.adaptivePunishmentEngine.evaluate(uuid, result.action, null, message);
                           if (DebugConfig.isDebugEnabled()) {
                              CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] BLOCK -> " + punishment.getType() + " | Reason=\"" + punishment.getReason());
                           }

                           this.teacherResponder.respond(player, result.action, message, result.score);
                           this.applyPunishment(player, punishment, event);
                        } else if (result.action == DecisionResult.Action.WARN) {
                           this.threatManager.addThreat(uuid, 5.0);
                           this.teacherResponder.respond(player, result.action, message, result.score);
                           player.sendMessage("§e[AI] Warning: " + result.reason);
                           CoreBootstrap.MEMORY_ENGINE.recordPunishment(playerId, "WARN");
                        } else {
                           this.threatManager.reduceThreat(uuid, 15.0);
                           CoreBootstrap.MEMORY_ENGINE.logChat(uuid, message, "global");
                           CoreBootstrap.MEMORY_ENGINE.learnFromMessage(playerId, player.getName(), message);
                           CoreBootstrap.PERSONALITY_ENGINE.update(uuid);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void applyPunishment(Player player, PunishmentResult punishment, AsyncChatEvent event) {
      this.sync(() -> {
         switch (punishment.getType()) {
            case WARN:
               player.sendMessage("§e[AI] Warning: " + punishment.getReason());
               break;
            case MUTE:
               event.setCancelled(true);
               long minutes = punishment.getDuration() / 60000L;
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempmute " + player.getName() + " " + minutes + "m " + punishment.getReason());
               player.sendMessage("§6[AI] Muted: " + punishment.getReason());
               break;
            case KICK:
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " AI Kick: " + punishment.getReason());
               break;
            case BAN:
               event.setCancelled(true);
               int banCount = CoreBootstrap.BAN_HISTORY_MANAGER.incrementBanCount(player.getUniqueId());
               String duration = CoreBootstrap.BAN_HISTORY_MANAGER.getBanDuration(player.getUniqueId());
               String formatted = CoreBootstrap.BAN_HISTORY_MANAGER.getFormattedDuration(player.getUniqueId());
               if ("permanent".equals(duration)) {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + player.getName() + " " + punishment.getReason());
               } else {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + player.getName() + " " + duration + " " + punishment.getReason());
               }

               CoreBootstrap.PLUGIN.getLogger().warning("[AI] Progressive ban #" + banCount + " for " + player.getName() + " (" + formatted);
         }
      });
   }

   @EventHandler
   public void onCommand(PlayerCommandPreprocessEvent event) {
      Player player = event.getPlayer();
      if (this.muteManager.isMuted(player.getUniqueId())) {
         String cmd = event.getMessage().toLowerCase();
         if (cmd.startsWith("/ai ")) {
            return;
         }

         event.setCancelled(true);
         player.sendMessage("§c§l[AI] You are muted and cannot use commands!");
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      UUID uuid = event.getPlayer().getUniqueId();
      CoreBootstrap.MEMORY_ENGINE.onJoin(uuid);
   }

   private String formatTime(long ms) {
      long seconds = ms / 1000L;
      long minutes = seconds / 60L;
      long hours = minutes / 60L;
      if (hours > 0L) {
         return hours + "h " + minutes % 60L;
      } else {
         return minutes > 0L ? minutes + "m " + seconds % 60L : seconds + "";
      }
   }
}
