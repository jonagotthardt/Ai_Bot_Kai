package com.jonasmp.ai.commands;

import com.jonasmp.ai.JonaSMP_AI;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.config.DebugConfig;
import com.jonasmp.ai.core.LanguagePrefixManager;
import com.jonasmp.ai.core.LanguageSelectionGUI;
import com.jonasmp.ai.gateway.ChatSession;
import com.jonasmp.ai.gateway.WebSearchTool;
import com.jonasmp.ai.memory.PlayerMemory;
import com.jonasmp.ai.moderation.MuteManager;
import com.jonasmp.ai.radar.ChunkRadar;
import com.jonasmp.ai.watcher.AIPlayerBot;
import com.jonasmp.ai.watcher.WatcherCore;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AICommands implements CommandExecutor, TabCompleter {
   private final MuteManager muteManager;

   public AICommands(MuteManager muteManager) {
      this.muteManager = muteManager;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if ("f".equalsIgnoreCase(command.getName())) {
         if (args.length > 0 && args[0].equalsIgnoreCase("radar")) {
            this.handleRadar(sender);
            return true;
         } else if (!sender.hasPermission("jonasmpai.chat.query")) {
            sender.sendMessage("§cYou don't have permission to use AI chat!");
            return true;
         } else if (args.length == 0) {
            sender.sendMessage("§cUsage: /f <deine Frage>");
            sender.sendMessage("§7/f web <deine Frage> §f- Mit aktiver Internet-Suche");
            sender.sendMessage("§7/f radar §f- ChunkRadar Status");
            return true;
         } else {
            boolean webMode = args[0].equalsIgnoreCase("web");
            String question = webMode ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : String.join(" ", args);
            this.handleDirectChat(sender, question, webMode);
            return true;
         }
      } else if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("lang") || sub.equals("language") || sub.equals("sprache")) {
            this.handleLanguage(sender, args);
            return true;
         } else if (!sub.equals("falsepositive") && !sub.equals("fp")) {
            if (!sub.equals("frage") && !sub.equals("ask") && !sub.equals("chat")) {
               if (sub.equals("status")) {
                  if (!sender.hasPermission("jonasmpai.admin.status")) {
                     sender.sendMessage("§cYou don't have permission!");
                     return true;
                  } else {
                     this.handleStatus(sender, args);
                     return true;
                  }
               } else if (!sub.equals("mute") && !sub.equals("unmute") && !sub.equals("unban")) {
                  if (sub.equals("stats")) {
                     if (!sender.hasPermission("jonasmpai.admin.stats")) {
                        sender.sendMessage("§cYou don't have permission to view AI stats!");
                        return true;
                     } else {
                        this.handleStats(sender);
                        return true;
                     }
                  } else if (sub.equals("train")) {
                     if (!sender.hasPermission("jonasmpai.admin.train")) {
                        sender.sendMessage("§cYou don't have permission to train the AI!");
                        return true;
                     } else {
                        this.handleTrain(sender);
                        return true;
                     }
                  } else if (sub.equals("reload")) {
                     if (!sender.hasPermission("jonasmpai.admin.reload")) {
                        sender.sendMessage("§cYou don't have permission to reload!");
                        return true;
                     } else {
                        this.handleReload(sender);
                        return true;
                     }
                  } else if (sub.equals("rules")) {
                     if (!sender.hasPermission("jonasmpai.rules.reset")) {
                        sender.sendMessage("§cYou don't have permission to manage rules!");
                        return true;
                     } else {
                        this.handleRules(sender, args);
                        return true;
                     }
                  } else if (sub.equals("voice")) {
                     if (!sender.hasPermission("jonasmpai.admin.voice")) {
                        sender.sendMessage("§cYou don't have permission to manage voice chat moderation!");
                        return true;
                     } else {
                        this.handleVoice(sender, args);
                        return true;
                     }
                  } else if (!sub.equals("debug")) {
                     this.sendHelp(sender);
                     return true;
                  } else if (!sender.hasPermission("jonasmpai.admin.debug")) {
                     sender.sendMessage("§cYou don't have permission to use debug!");
                     return true;
                  } else {
                     this.handleDebug(sender, args);
                     return true;
                  }
               } else if (!sender.hasPermission("jonasmpai.admin.moderate")) {
                  sender.sendMessage("§cYou don't have permission to moderate!");
                  return true;
               } else {
                  switch (sub) {
                     case "mute":
                        this.handleMute(sender, args);
                        break;
                     case "unmute":
                        this.handleUnmute(sender, args);
                        break;
                     case "unban":
                        this.handleUnban(sender, args);
                  }

                  return true;
               }
            } else if (!sender.hasPermission("jonasmpai.chat.query")) {
               sender.sendMessage("§cYou don't have permission to use AI chat!");
               return true;
            } else {
               this.handleChat(sender, args);
               return true;
            }
         } else if (!sender.hasPermission("jonasmpai.falsepositive")) {
            sender.sendMessage("§cYou don't have permission to report false positives!");
            return true;
         } else {
            this.handleFalsePositive(sender, args);
            return true;
         }
      }
   }

   private void handleMute(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§cUsage: /ai mute <player> [duration] [reason]");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
         } else {
            long durationMs = TimeUnit.MINUTES.toMillis(10L);
            if (args.length >= 3) {
               try {
                  durationMs = TimeUnit.MINUTES.toMillis(Long.parseLong(args[2]));
               } catch (NumberFormatException var10) {
                  sender.sendMessage("§cInvalid duration. Use minutes (e.g. 10)");
                  return;
               }
            }

            String reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Muted by admin";
            String mutedBy = sender instanceof Player ? ((Player)sender).getName() : "Console";
            long minutes = durationMs / 60000L;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempmute " + target.getName() + " " + minutes + "m " + reason);
            target.sendMessage("§c§l[AI] You have been muted!");
            target.sendMessage("§cReason: " + reason);
            target.sendMessage("§cDuration: " + this.formatDuration(durationMs));
            sender.sendMessage("§aMuted " + target.getName() + " for " + this.formatDuration(durationMs));
            Bukkit.broadcast("§6[AI] " + target.getName() + " was muted by " + mutedBy + " for " + reason, "jonasmpai.admin");
         }
      }
   }

   private void handleUnmute(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§cUsage: /ai unmute <player>");
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
         } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unmute " + target.getName());
            CoreBootstrap.AI_GATEWAY.sendFeedback(target.getUniqueId().toString(), "", "false_positive");
            target.sendMessage("§a[AI] You have been unmuted!");
            sender.sendMessage("§aUnmuted " + target.getName());
            sender.sendMessage("§7Feedback sent to AI backend (false positive recorded).");
         }
      }
   }

   private void handleUnban(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§cUsage: /ai unban <player>");
      } else {
         String targetName = args[1];
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unban " + targetName);
         CoreBootstrap.AI_GATEWAY.sendFeedback(targetName, "", "false_positive");
         sender.sendMessage("§aUnbanned " + targetName);
         sender.sendMessage("§7Feedback sent to AI backend (false positive recorded).");
      }
   }

   private void handleTrain(CommandSender sender) {
      sender.sendMessage("§eTriggering AI training...");
      Bukkit.getScheduler().runTaskAsynchronously(CoreBootstrap.PLUGIN, () -> {
         boolean success = CoreBootstrap.AI_GATEWAY.requestTraining();
         Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
            if (success) {
               sender.sendMessage("§a§lAI Training Complete!");
               sender.sendMessage("§7The ML model has been retrained on admin feedback.");
               sender.sendMessage("§7Future decisions will use the improved model.");
            } else {
               sender.sendMessage("§cTraining failed or not enough data.");
               sender.sendMessage("§7Need at least 5 admin feedbacks (false_positive/confirmed).");
               sender.sendMessage("§7Use /ai unmute or /ai unban when the AI is wrong.");
            }
         });
      });
   }

   private void handleStats(CommandSender sender) {
      sender.sendMessage("§e§l=== AI System Stats ===");
      Bukkit.getScheduler().runTaskAsynchronously(CoreBootstrap.PLUGIN, () -> {
         Map<String, Map<String, Object>> stats = CoreBootstrap.AI_GATEWAY.fetchStats();
         Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
            if (stats == null) {
               sender.sendMessage("§cFailed to fetch stats from backend.");
            } else {
               sender.sendMessage("§7Backend Status: §aONLINE");
               Map<String, Object> system = stats.get("system");
               if (system != null) {
                  sender.sendMessage("§e--- System ---");
                  sender.sendMessage("§7Messages analyzed: §f" + system.get("total_messages"));
                  sender.sendMessage("§7Total blocks: §c" + system.get("total_blocks"));
                  sender.sendMessage("§7Total warns: §e" + system.get("total_warns"));
                  sender.sendMessage("§7False positives: §a" + system.get("false_positives"));
                  sender.sendMessage("§7True positives: §a" + system.get("true_positives"));
                  sender.sendMessage("§7ML retrain count: §b" + system.get("ml_retrain_count"));
               }

               Map<String, Object> players = stats.get("players");
               if (players != null) {
                  sender.sendMessage("§e--- Players ---");
                  sender.sendMessage("§7Total tracked: §f" + players.get("total_players"));
                  sender.sendMessage("§7Trusted players: §a" + players.get("trusted_players"));
                  sender.sendMessage("§7Repeat offenders: §c" + players.get("repeat_offenders"));
               }

               sender.sendMessage("§7§oUse /ai train to retrain the ML model.");
            }
         });
      });
   }

   private void handleStatus(CommandSender sender, String[] args) {
      if (args.length < 2) {
         Collection<UUID> muted = this.muteManager.getAllMuted();
         if (muted.isEmpty()) {
            sender.sendMessage("§aNo players currently muted.");
            return;
         }

         sender.sendMessage("§e§l=== AI Mute Status ===");

         for (UUID uuid : muted) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString().substring(0, 8);
            long remaining = this.muteManager.getRemainingMs(uuid);
            String reason = this.muteManager.getMuteReason(uuid);
            sender.sendMessage("§c" + name + " §7- " + this.formatDuration(remaining) + " left (" + reason);
         }
      } else {
         Player target = Bukkit.getPlayer(args[1]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
         }

         boolean muted2 = this.muteManager.isMuted(target.getUniqueId());
         sender.sendMessage("§eStatus for " + target.getName());
         sender.sendMessage(muted2 ? "§c§lMUTED" : "§aNot muted");
         if (muted2) {
            long remaining2 = this.muteManager.getRemainingMs(target.getUniqueId());
            String reason2 = this.muteManager.getMuteReason(target.getUniqueId());
            sender.sendMessage("§cRemaining: " + this.formatDuration(remaining2));
            sender.sendMessage("§cReason: " + reason2);
         }
      }
   }

   private void handleRules(CommandSender sender, String[] args) {
      if (args.length >= 3 && args[1].equalsIgnoreCase("reset")) {
         Player target = Bukkit.getPlayer(args[2]);
         if (target == null) {
            sender.sendMessage("§cPlayer not found or not online: " + args[2]);
         } else {
            CoreBootstrap.FIRST_JOIN_TRACKER.resetPlayer(target.getUniqueId());
            sender.sendMessage("§aReset rule acceptance for " + target.getName());
            sender.sendMessage("§7They will see the rules GUI on next join.");
         }
      } else {
         sender.sendMessage("§cUsage: /ai rules reset <player>");
         sender.sendMessage("§7Resets a player's rule acceptance so they must accept again.");
      }
   }

   private void handleReload(CommandSender sender) {
      CoreBootstrap.PLUGIN.reloadConfig();
      CoreBootstrap.CONFIG.init();
      sender.sendMessage("§aJonaSMP_AI config reloaded!");
      sender.sendMessage("§7Backend: " + CoreBootstrap.CONFIG.getBackendUrl());
      sender.sendMessage("§7AI Enabled: " + CoreBootstrap.CONFIG.isAIEnabled());
   }

   private void handleDebug(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§e§l=== AI Debug ===");
         sender.sendMessage("§7/ai debug test    §f- Test backend connection");
         sender.sendMessage("§7/ai debug on      §f- Enable debug logging");
         sender.sendMessage("§7/ai debug off     §f- Disable debug logging");
         sender.sendMessage("§7/ai debug status  §f- Show debug status");
         sender.sendMessage("§7/ai debug search  §f- Show last web-search diagnostics");
      } else {
         String lowerCase = args[1].toLowerCase();
         switch (lowerCase) {
            case "test":
               sender.sendMessage("§eTesting backend connection...");
               boolean ok = CoreBootstrap.AI_GATEWAY.testConnection();
               if (ok) {
                  sender.sendMessage("§a§lBackend OK!§r§a Connected to " + CoreBootstrap.CONFIG.getBackendUrl());
               } else {
                  sender.sendMessage("§c§lBackend UNREACHABLE!§r§c Check if start_backend.bat is running.");
                  sender.sendMessage("§cURL: " + CoreBootstrap.CONFIG.getBackendUrl());
               }
               break;
            case "on":
               DebugConfig.setDebugEnabled(true);
               sender.sendMessage("§aDebug logging enabled. Check console for [AI-DEBUG] messages.");
               break;
            case "off":
               DebugConfig.setDebugEnabled(false);
               sender.sendMessage("§7Debug logging disabled.");
               break;
            case "status":
               sender.sendMessage("§e§l=== AI Debug Status ===");
               sender.sendMessage("§7Backend URL: §f" + CoreBootstrap.CONFIG.getBackendUrl());
               sender.sendMessage("§7Debug logging: " + (DebugConfig.isDebugEnabled() ? "§aON" : "§cOFF"));
               sender.sendMessage("§7Voice debug logging: " + (DebugConfig.isVoiceDebugEnabled() ? "§aON" : "§cOFF"));
               sender.sendMessage("§7AI enabled: " + (CoreBootstrap.CONFIG.isAIEnabled() ? "§aYES" : "§cNO"));
               sender.sendMessage("§7Block threshold: §f" + CoreBootstrap.CONFIG.getBlockThreshold());
               sender.sendMessage("§7Warn threshold: §f" + CoreBootstrap.CONFIG.getWarnThreshold());
               break;
            case "search":
               sender.sendMessage("§e§l=== Last Web Search Debug ===");
               String q = WebSearchTool.getLastQuery();
               if (q == null) {
                  sender.sendMessage("§cNo search has been performed yet.");
                  return;
               }

               sender.sendMessage("§7Query: §f" + q);
               List<WebSearchTool.SearchResult> results = WebSearchTool.getLastResults();
               if (results != null && !results.isEmpty()) {
                  sender.sendMessage("§aResults: " + results.size());

                  for (int i = 0; i < results.size(); i++) {
                     WebSearchTool.SearchResult r = results.get(i);
                     sender.sendMessage(
                        "§7["
                           + (i + 1)
                           + "] §f"
                           + r.title.replaceAll("<[^>]+>", "")
                           + "§7 | §f"
                           + r.snippet.replaceAll("<[^>]+>", "").substring(0, Math.min(100, r.snippet.length()))
                           + "..."
                     );
                  }
               } else {
                  sender.sendMessage("§cResults: NONE (0 found)");
               }

               String ctx = WebSearchTool.getLastContext();
               if (ctx != null) {
                  sender.sendMessage("§7Context length: §f" + ctx.length() + " chars");
               } else {
                  sender.sendMessage("§cContext: NULL (not built — results were empty)");
               }

               String html = WebSearchTool.getLastHtmlPreview();
               if (html != null && html.startsWith("HTTP")) {
                  sender.sendMessage("§cHTML: " + html);
               } else if (html != null) {
                  sender.sendMessage("§7HTML preview (first 200 chars): §f" + html.substring(0, Math.min(200, html.length())));
               }
               break;
            default:
               sender.sendMessage("§cUnknown debug subcommand. Use /ai debug test|on|off|status|search");
         }
      }
   }

   private void handleVoice(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§e§l=== Voice Chat Moderation ===");
         sender.sendMessage("§7/ai voice status §f- Show voice moderation status");
         sender.sendMessage("§7/ai voice toggle §f- Enable/disable voice moderation");
         sender.sendMessage("§7/ai voice mute <player> §f- Exclude player from voice moderation");
         sender.sendMessage("§7/ai voice unmute <player> §f- Re-include player in voice moderation");
         sender.sendMessage("§7/ai voice logs <player> §f- Show last voice transcripts");
         sender.sendMessage("§7/ai voice logs <player> export §f- Export transcripts to file");
         sender.sendMessage("§7/ai voice debug §f- Toggle console debug spam for voice chat");
      } else {
         String lowerCase = args[1].toLowerCase();
         switch (lowerCase) {
            case "status":
               this.handleVoiceStatus(sender);
               break;
            case "toggle":
               this.handleVoiceToggle(sender);
               break;
            case "mute":
               this.handleVoiceMute(sender, args);
               break;
            case "unmute":
               this.handleVoiceUnmute(sender, args);
               break;
            case "logs":
               this.handleVoiceLogs(sender, args);
               break;
            case "debug":
               this.handleVoiceDebug(sender);
               break;
            default:
               sender.sendMessage("§cUnknown voice subcommand. Use /ai voice status|toggle|mute|unmute|logs|debug");
         }
      }
   }

   private void handleVoiceStatus(CommandSender sender) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleVoiceDebug(CommandSender sender) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleVoiceToggle(CommandSender sender) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleVoiceMute(CommandSender sender, String[] args) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleVoiceUnmute(CommandSender sender, String[] args) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleVoiceLogs(CommandSender sender, String[] args) {
      sender.sendMessage("§c[Voice] Voice chat moderation is not available in this build.");
   }

   private void handleFalsePositive(CommandSender sender, String[] args) {
      String message = null;
      if (args.length < 2) {
         if (!(sender instanceof Player player)) {
            sender.sendMessage("§cUsage: /ai falsepositive <message that was wrongly blocked>");
            sender.sendMessage("§7Console must provide the message directly.");
            return;
         }

         if (CoreBootstrap.LAST_BLOCKED_STORE != null) {
            message = CoreBootstrap.LAST_BLOCKED_STORE.getLastBlocked(player.getUniqueId());
         }

         if (message == null) {
            sender.sendMessage("§cNo recently blocked message found for you.");
            sender.sendMessage("§7Type the blocked message after the command: /ai falsepositive <message>");
            return;
         }

         sender.sendMessage("§7Using your last blocked message: §f\"" + message);
      } else {
         message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
      }

      String playerId = sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "console";
      CoreBootstrap.FALSE_POSITIVE_STORE.report(message, playerId, "player_report");
      sender.sendMessage("§aThank you! Your report has been saved.");
      sender.sendMessage("§7The AI will learn from this and similar messages will be allowed in the future.");
      sender.sendMessage("§7Reports so far: §f" + CoreBootstrap.FALSE_POSITIVE_STORE.getCount());
   }

   private void handleLanguage(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can set language.");
      } else if (args.length < 2) {
         LanguageSelectionGUI.open(player);
      } else {
         String lang = args[1].toLowerCase();
         if (!lang.equals("de") && !lang.equals("en")) {
            sender.sendMessage("§cUngültige Sprache. Nutze: /ai lang de  oder  /ai lang en");
         } else {
            CoreBootstrap.PLAYER_LANGUAGE_STORE.setLanguage(player.getUniqueId(), lang);
            LanguagePrefixManager.updatePlayerPrefix(player);
         }
      }
   }

   private void handleChat(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§cUsage: /ai frage <deine Frage>");
         sender.sendMessage("§7Beispiel: /ai frage Was ist der beste Schwert-Enchant?");
      } else if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can use this command.");
      } else {
         String question = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
         String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId());
         String thinking = "en".equals(lang) ? "§7[AI] Thinking..." : "§7[KI] Denke nach...";
         sender.sendMessage(thinking);
         Bukkit.getScheduler().runTaskAsynchronously(CoreBootstrap.PLUGIN, () -> {
            String reply = CoreBootstrap.AI_GATEWAY.chat(player.getUniqueId().toString(), question, lang);
            Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
               if (reply != null) {
                  String prefix = "en".equals(lang) ? "§b[AI] §f" : "§b[KI] §f";
                  sender.sendMessage(prefix + reply);
               } else if ("en".equals(lang)) {
                  sender.sendMessage("§c[AI] Sorry, I couldn't answer your question.");
                  sender.sendMessage("§7[AI] Make sure the AI backend is running (http://localhost:8000).");
               } else {
                  sender.sendMessage("§c[KI] Tut mir leid, ich konnte deine Frage nicht beantworten.");
                  sender.sendMessage("§7[KI] Stelle sicher, dass der AI-Backend läuft (http://localhost:8000).");
               }
            });
         });
      }
   }

   private void handleDirectChat(CommandSender sender, String question, boolean webMode) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("§cOnly players can use this command.");
      } else {
         String playerId = player.getUniqueId().toString();
         String lower = question.toLowerCase().trim();
         if (!lower.equals("tps")
            && !lower.equals("lag")
            && !lower.equals("serverlag")
            && !lower.equals("wie laggy")
            && !lower.equals("wie viel tps")
            && !lower.startsWith("wie viel tps")
            && !lower.startsWith("tps ")) {
            if (!lower.equals("ram") && !lower.equals("speicher") && !lower.equals("memory") && !lower.equals("wie viel ram") && !lower.startsWith("ram ")) {
               if (!lower.equals("online")
                  && !lower.equals("spieler")
                  && !lower.equals("wer ist online")
                  && !lower.equals("who is online")
                  && !lower.equals("spieler online")
                  && !lower.equals("wie viele spieler")) {
                  if (!lower.equals("entities") && !lower.equals("entity count") && !lower.equals("wie viele entities")) {
                     if (!lower.equals("chunks") && !lower.equals("chunk count") && !lower.equals("wie viele chunks")) {
                        if (!lower.equals("uptime") && !lower.equals("onlinezeit") && !lower.equals("wie lange läuft")) {
                           if (!lower.equals("version") && !lower.equals("server version") && !lower.equals("mc version") && !lower.equals("minecraft version")
                              )
                            {
                              if (lower.equals("worlds") || lower.equals("welten") || lower.equals("welten liste")) {
                                 if (!sender.hasPermission("jonasmpai.stats.advanced")) {
                                    sender.sendMessage("§cYou don't have permission to view worlds!");
                                    return;
                                 }

                                 sender.sendMessage("§b[Server] Welten:");

                                 for (World w2 : Bukkit.getWorlds()) {
                                    sender.sendMessage("§7  " + w2.getName() + " (" + w2.getEnvironment());
                                 }
                              } else {
                                 if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
                                    if (lower.startsWith("was hat ") || lower.startsWith("inv ") || lower.startsWith("inventar ")) {
                                       String targetName = this.extractPlayerName(question);
                                       if (targetName != null) {
                                          Player target = Bukkit.getPlayer(targetName);
                                          if (target != null) {
                                             sender.sendMessage("§e[Admin] Inventar von " + target.getName());
                                             ItemStack[] inv = target.getInventory().getContents();

                                             for (int i = 0; i < inv.length; i++) {
                                                if (inv[i] != null && inv[i].getType() != Material.AIR) {
                                                   sender.sendMessage("§7  Slot " + i + ": " + inv[i].getType() + " x" + inv[i].getAmount());
                                                }
                                             }
                                          } else {
                                             sender.sendMessage("§cSpieler nicht online: " + targetName);
                                          }

                                          return;
                                       }
                                    }

                                    if (lower.startsWith("wo ist ") || lower.startsWith("loc ") || lower.startsWith("location ") || lower.startsWith("pos ")) {
                                       String targetName = this.extractPlayerName(question);
                                       if (targetName != null) {
                                          Player target = Bukkit.getPlayer(targetName);
                                          if (target != null) {
                                             Location loc = target.getLocation();
                                             sender.sendMessage(
                                                String.format(
                                                   "§e[Admin] %s ist bei X=%.1f Y=%.1f Z=%.1f in %s",
                                                   target.getName(),
                                                   loc.getX(),
                                                   loc.getY(),
                                                   loc.getZ(),
                                                   loc.getWorld().getName()
                                                )
                                             );
                                          } else {
                                             sender.sendMessage("§cSpieler nicht online: " + targetName);
                                          }

                                          return;
                                       }
                                    }

                                    if (lower.startsWith("info ") || lower.startsWith("profil ") || lower.startsWith("report ") || lower.startsWith("check ")) {
                                       String targetName = this.extractPlayerName(question);
                                       if (targetName != null) {
                                          Player target = Bukkit.getPlayer(targetName);
                                          UUID targetUuid = null;
                                          if (target != null) {
                                             targetUuid = target.getUniqueId();
                                          } else {
                                             OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                                             if (offline.hasPlayedBefore()) {
                                                targetUuid = offline.getUniqueId();
                                             }
                                          }

                                          if (targetUuid != null) {
                                             String report = CoreBootstrap.MEMORY_ENGINE.getAdminReport(targetUuid);

                                             for (String line : report.split("\\n")) {
                                                sender.sendMessage(line);
                                             }

                                             CoreBootstrap.MEMORY_ENGINE.exportProfile(targetUuid);
                                             PlayerMemory mem = CoreBootstrap.MEMORY_ENGINE.get(targetUuid);
                                             String fileName = mem.displayName.isEmpty()
                                                ? "unknown"
                                                : mem.displayName.replaceAll("[\\\\/:*?\"<>|]", "_") + "_profile.txt";
                                             sender.sendMessage("§a[Profile] Report exported to: plugins/JonaSMP_AI/player_profiles/" + fileName);
                                          } else {
                                             sender.sendMessage("§cSpieler nicht bekannt: " + targetName);
                                          }

                                          return;
                                       }
                                    }
                                 }

                                 String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId());
                                 ChatSession chatSession = CoreBootstrap.AI_GATEWAY.getChatSession();
                                 if (webMode) {
                                    chatSession.setWebSearchEnabled(playerId, true);
                                    this.doChatWithOptionalWebSearch(sender, playerId, question, lang, true);
                                    return;
                                 }

                                 if (chatSession.hasPendingWebQuery(playerId)) {
                                    String pending = chatSession.getPendingWebQuery(playerId);
                                    String answer = lower.trim();
                                    boolean isYes = answer.matches("^(ja|yes|j|y|ok|sure|bitte|gerne|go|mach|suche|search|find|finde).*");
                                    boolean isNo = answer.matches("^(nein|no|n|nope|stop|nicht|nichts|ab|cancel|abbrechen).*");
                                    if (isYes) {
                                       chatSession.clearPendingWebQuery(playerId);
                                       chatSession.setWebSearchEnabled(playerId, true);
                                       String searchMsg = "en".equals(lang) ? "§7[AI] Searching the web..." : "§7[KI] Suche im Internet...";
                                       sender.sendMessage(searchMsg);
                                       this.doChatWithOptionalWebSearch(sender, playerId, pending, lang, true);
                                    } else if (isNo) {
                                       chatSession.clearPendingWebQuery(playerId);
                                       String cancelled = "en".equals(lang)
                                          ? "§7[AI] Web search cancelled. Asking without web data..."
                                          : "§7[KI] Internet-Suche abgebrochen. Frage ohne Web-Daten...";
                                       sender.sendMessage(cancelled);
                                       this.doChatWithOptionalWebSearch(sender, playerId, pending, lang, false);
                                    } else {
                                       String again = "en".equals(lang)
                                          ? "§7[AI] Please answer with yes/no (ja/nein). Should I search the web for: \"" + pending + "\"?"
                                          : "§7[KI] Bitte antworte mit ja oder nein. Soll ich im Internet suchen nach: \"" + pending + "\"?";
                                       sender.sendMessage(again);
                                    }

                                    return;
                                 }

                                 if (!chatSession.isWebSearchEnabled(playerId) && this.mightNeedWebSearch(lower)) {
                                    chatSession.setPendingWebQuery(playerId, question);
                                    String ask = "en".equals(lang)
                                       ? "§e[AI] This question might need current web info.\n§7Should I search the internet for: \""
                                          + question
                                          + "\"? §aReply yes/ja or no/nein"
                                       : "§e[KI] Diese Frage braucht vielleicht aktuelle Web-Infos.\n§7Soll ich im Internet suchen nach: \""
                                          + question
                                          + "\"? §aAntworte ja oder nein";
                                    sender.sendMessage(ask);
                                    return;
                                 }

                                 this.doChatWithOptionalWebSearch(sender, playerId, question, lang, chatSession.isWebSearchEnabled(playerId));
                              }
                           } else {
                              if (!sender.hasPermission("jonasmpai.stats.basic")) {
                                 sender.sendMessage("§cYou don't have permission to view version!");
                                 return;
                              }

                              sender.sendMessage("§b[Server] " + Bukkit.getName() + " " + Bukkit.getVersion());
                           }
                        } else {
                           if (!sender.hasPermission("jonasmpai.stats.basic")) {
                              sender.sendMessage("§cYou don't have permission to view uptime!");
                              return;
                           }

                           long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
                           long seconds = uptimeMs / 1000L;
                           long minutes = seconds / 60L;
                           long hours = minutes / 60L;
                           long days = hours / 24L;
                           String uptimeStr;
                           if (days > 0L) {
                              uptimeStr = days + "d " + hours % 24L + "h " + minutes % 60L;
                           } else if (hours > 0L) {
                              uptimeStr = hours + "h " + minutes % 60L;
                           } else {
                              uptimeStr = minutes + "m " + seconds % 60L;
                           }

                           sender.sendMessage("§b[Server] Uptime: " + uptimeStr);
                        }
                     } else {
                        if (!sender.hasPermission("jonasmpai.stats.advanced")) {
                           sender.sendMessage("§cYou don't have permission to view chunk stats!");
                           return;
                        }

                        int totalChunks = 0;

                        for (World w : Bukkit.getWorlds()) {
                           totalChunks += w.getLoadedChunks().length;
                        }

                        sender.sendMessage("§b[Server] " + totalChunks + " Chunks geladen");
                     }
                  } else {
                     if (!sender.hasPermission("jonasmpai.stats.advanced")) {
                        sender.sendMessage("§cYou don't have permission to view entity stats!");
                        return;
                     }

                     int totalEntities = 0;

                     for (World w : Bukkit.getWorlds()) {
                        totalEntities += w.getEntities().size();
                     }

                     sender.sendMessage("§b[Server] " + totalEntities + " Entities geladen");
                  }
               } else {
                  if (!sender.hasPermission("jonasmpai.stats.basic")) {
                     sender.sendMessage("§cYou don't have permission to view online players!");
                     return;
                  }

                  int online = Bukkit.getOnlinePlayers().size();
                  int max2 = Bukkit.getMaxPlayers();
                  sender.sendMessage("§b[Server] " + online + "/" + max2 + " Spieler online");
                  if (online > 0) {
                     StringBuilder names = new StringBuilder("§7  ");

                     for (Player p : Bukkit.getOnlinePlayers()) {
                        names.append(p.getName()).append(", ");
                     }

                     sender.sendMessage(names.substring(0, Math.max(0, names.length() - 2)));
                  }
               }
            } else {
               if (!sender.hasPermission("jonasmpai.stats.advanced")) {
                  sender.sendMessage("§cYou don't have permission to view RAM stats!");
                  return;
               }

               Runtime rt = Runtime.getRuntime();
               long total = rt.totalMemory() / 1024L / 1024L;
               long free = rt.freeMemory() / 1024L / 1024L;
               long used = total - free;
               long max = rt.maxMemory() / 1024L / 1024L;
               sender.sendMessage("§b[Server] JVM Heap: " + used + "MB / " + max + "MB used (" + free + "MB free)");
               if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
                  try {
                     if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean sunBean) {
                        long physTotal = sunBean.getTotalPhysicalMemorySize() / 1024L / 1024L;
                        long physFree = sunBean.getFreePhysicalMemorySize() / 1024L / 1024L;
                        long virtCommitted = sunBean.getCommittedVirtualMemorySize() / 1024L / 1024L;
                        long processPhysUsed = physTotal - physFree;
                        long swapUsed = Math.max(0L, virtCommitted - processPhysUsed);
                        double swapRatio = virtCommitted > 0L ? (double)swapUsed / (double)virtCommitted * 100.0 : 0.0;
                        sender.sendMessage("§7[Server] Physischer RAM: " + processPhysUsed + "MB / " + physTotal + "MB frei (" + physFree + "MB frei)");
                        if (swapUsed > 0L) {
                           sender.sendMessage("§c[Server] SWAP/Pagefile: " + swapUsed + "MB (" + String.format("%.1f", swapRatio) + "% des Prozesses)");
                        } else {
                           sender.sendMessage("§a[Server] Kein SWAP aktiv — läuft komplett über Hardware-RAM");
                        }

                        sender.sendMessage("§7[Server] Virtueller Speicher (Gesamt): " + virtCommitted + "MB");
                     }
                  } catch (Exception var30) {
                  }
               }
            }
         } else {
            if (!sender.hasPermission("jonasmpai.stats.basic")) {
               sender.sendMessage("§cYou don't have permission to view TPS!");
               return;
            }

            double[] tps = Bukkit.getTPS();
            double currentTps = tps.length > 0 ? Math.min(tps[0], 20.0) : 20.0;
            String color = currentTps >= 18.0 ? "§a" : (currentTps >= 15.0 ? "§e" : "§c");
            sender.sendMessage(color + "[Server] TPS: " + String.format("%.2f", currentTps) + "/20.0");
         }
      }
   }

   private boolean mightNeedWebSearch(String lower) {
      String[] array = new String[]{
         "aktuell",
         "neu",
         "news",
         "wetter",
         "weather",
         "preis",
         "price",
         "kosten",
         "kurs",
         " heute ",
         " heute",
         "jetzt",
         "momentan",
         "grade",
         "gerade",
         "live",
         "realtime",
         "2024",
         "2025",
         "2026",
         "bundeskanzler",
         "präsident",
         "wahl",
         "ergebnis",
         "ergebnisse",
         "tabelle",
         "standings",
         "platzierung",
         "score",
         " champions ",
         "euro",
         "dollar",
         "btc",
         "bitcoin",
         "aktie",
         "stock",
         "börse",
         "exchange rate",
         "wer ist ",
         "who is ",
         "was ist ",
         "what is ",
         "wie viel ",
         "how much ",
         "wann ",
         "when ",
         "wo ",
         "where ",
         "warum ",
         "why ",
         "suche ",
         "search ",
         "google ",
         "finde ",
         "find ",
         "minecraft update",
         "mc version",
         "snapshot",
         "paper mc",
         "spigot",
         "patch notes",
         "changelog",
         "update",
         "neuigkeit",
         "nachrichten"
      };

      for (String kw : array) {
         if (lower.contains(kw.toLowerCase())) {
            return true;
         }
      }

      return false;
   }

   private void doChatWithOptionalWebSearch(CommandSender sender, String playerId, String question, String lang, boolean useWebSearch) {
      StringBuilder questionHolder = new StringBuilder(question);
      if (useWebSearch) {
         String thinking = "en".equals(lang) ? "§7[AI] Thinking + searching web..." : "§7[KI] Denke + suche im Web...";
         sender.sendMessage(thinking);
      } else {
         String thinking = "en".equals(lang) ? "§7[AI] Thinking..." : "§7[KI] Denke nach...";
         sender.sendMessage(thinking);
      }

      Bukkit.getScheduler()
         .runTaskAsynchronously(
            CoreBootstrap.PLUGIN,
            () -> {
               if (useWebSearch) {
                  try {
                     WebSearchTool searchTool = new WebSearchTool();
                     String webContext = searchTool.searchForContext(question);
                     if (webContext != null) {
                        questionHolder.setLength(0);
                        questionHolder.append("[INTERNET-RECHERCHE]\n")
                           .append(webContext)
                           .append("\n[/INTERNET-RECHERCHE]\n\nNutze die obigen Web-Ergebnisse, um aktuell und korrekt zu antworten.\nFrage des Spielers: ")
                           .append(question);
                     }
                  } catch (Exception var8x) {
                     if (DebugConfig.isDebugEnabled()) {
                        CoreBootstrap.PLUGIN.getLogger().warning("[AI] Web search failed: " + var8x.getMessage());
                     }
                  }
               }

               String reply = CoreBootstrap.AI_GATEWAY.chat(playerId, questionHolder.toString(), lang);
               Bukkit.getScheduler()
                  .runTask(
                     CoreBootstrap.PLUGIN,
                     () -> {
                        if (reply != null) {
                           String prefix = useWebSearch
                              ? ("en".equals(lang) ? "§b[AI§b§o+Web§b] §f" : "§b[KI§b§o+Web§b] §f")
                              : ("en".equals(lang) ? "§b[AI] §f" : "§b[KI] §f");
                           sender.sendMessage(prefix + reply);
                        } else if ("en".equals(lang)) {
                           sender.sendMessage("§c[AI] Sorry, I couldn't answer your question.");
                        } else {
                           sender.sendMessage("§c[KI] Tut mir leid, ich konnte deine Frage nicht beantworten.");
                        }
                     }
                  );
            }
         );
   }

   private String extractPlayerName(String question) {
      String[] parts = question.split(" ");
      if (parts.length >= 3) {
         return parts[2];
      } else {
         return parts.length >= 2 ? parts[1] : null;
      }
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage("§e§l=== JonaSMP AI Commands ===");
      if (sender.hasPermission("jonasmpai.chat.query")) {
         sender.sendMessage("§7/f <Frage> §f- Schnelle KI-Frage");
         sender.sendMessage("§7/f web <Frage> §f- KI-Frage mit aktiver Internet-Suche");
         sender.sendMessage("§7/ai frage <Text> §f- Ask the AI a question");
      }

      if (sender.hasPermission("jonasmpai.stats.basic")) {
         sender.sendMessage("§7/f tps §f- Server TPS / Lag-Check");
         sender.sendMessage("§7/f online §f- Spieler online");
         sender.sendMessage("§7/f uptime §f- Server-Laufzeit");
         sender.sendMessage("§7/f version §f- Server-Version");
      }

      if (sender.hasPermission("jonasmpai.stats.advanced")) {
         sender.sendMessage("§7/f ram §f- Server RAM-Nutzung");
         sender.sendMessage("§7/f entities §f- Entity-Anzahl");
         sender.sendMessage("§7/f chunks §f- Geladene Chunks");
         sender.sendMessage("§7/f worlds §f- Liste aller Welten");
      }

      sender.sendMessage("§7/ai lang <de|en> §f- Sprache wechseln (zeigt [DE]/[EN] Prefix)");
      if (sender.hasPermission("jonasmpai.falsepositive")) {
         sender.sendMessage("§7/ai falsepositive <message> §f- Report wrongly blocked message");
      }

      if (sender.hasPermission("jonasmpai.admin.moderate")) {
         sender.sendMessage("§7/ai mute <player> [minutes] [reason]");
         sender.sendMessage("§7/ai unmute <player> §f- Unmute + tell AI it was wrong");
         sender.sendMessage("§7/ai unban <player> §f- Unban + tell AI it was wrong");
      }

      if (sender.hasPermission("jonasmpai.admin.train")) {
         sender.sendMessage("§7/ai train §f- Retrain ML model on admin feedback");
      }

      if (sender.hasPermission("jonasmpai.admin.stats")) {
         sender.sendMessage("§7/ai stats §f- Show AI system statistics");
      }

      if (sender.hasPermission("jonasmpai.admin.status")) {
         sender.sendMessage("§7/ai status [player]");
      }

      if (sender.hasPermission("jonasmpai.admin.reload")) {
         sender.sendMessage("§7/ai reload");
      }

      if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
         sender.sendMessage("§7/f info <spieler> §f- Vollst. Profil + Verdachtsreport");
         sender.sendMessage("§7/f profil <spieler> §f- Alias für info");
         sender.sendMessage("§7/f check <spieler> §f- Schnellcheck");
         sender.sendMessage("§7/f report <spieler> §f- Verdachtsreport");
      }

      if (sender.hasPermission("jonasmpai.admin.voice")) {
         sender.sendMessage("§7/ai voice status §f- Voice moderation status");
         sender.sendMessage("§7/ai voice toggle §f- Enable/disable voice moderation");
         sender.sendMessage("§7/ai voice mute <player> §f- Exclude from voice moderation");
         sender.sendMessage("§7/ai voice unmute <player> §f- Re-include in voice moderation");
         sender.sendMessage("§7/ai voice logs <player> §f- Show voice transcripts");
         sender.sendMessage("§7/ai voice logs <player> export §f- Export voice transcripts to file");
         sender.sendMessage("§7/ai voice debug §f- Toggle console voice-chat debug spam");
      }

      if (sender.hasPermission("jonasmpai.admin.debug")) {
         sender.sendMessage("§7/ai debug <test|on|off|status|search>");
      }

      if (sender.hasPermission("jonasmpai.rules.reset")) {
         sender.sendMessage("§7/ai rules reset <player> §f- Reset rule acceptance");
      }

      if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
         sender.sendMessage("§7/f was hat <spieler> §f- Admin: Inventar anzeigen");
         sender.sendMessage("§7/f wo ist <spieler> §f- Admin: Position anzeigen");
      }
   }

   private String formatDuration(long ms) {
      long seconds = ms / 1000L;
      long minutes = seconds / 60L;
      long hours = minutes / 60L;
      long days = hours / 24L;
      if (days > 0L) {
         return days + "d " + hours % 24L;
      } else if (hours > 0L) {
         return hours + "h " + minutes % 60L;
      } else {
         return minutes > 0L ? minutes + "m " + seconds % 60L : seconds + "";
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if ("f".equalsIgnoreCase(command.getName())) {
         if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("web");
            suggestions.add("radar");
            if (sender.hasPermission("jonasmpai.stats.basic")) {
               suggestions.addAll(Arrays.asList("tps", "lag", "online", "uptime", "version"));
            }

            if (sender.hasPermission("jonasmpai.stats.advanced")) {
               suggestions.addAll(Arrays.asList("ram", "entities", "chunks", "worlds"));
            }

            if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
               suggestions.addAll(Arrays.asList("was", "wo", "info", "profil", "report", "check"));
            }

            return this.filterSuggestions(suggestions, args[0]);
         } else {
            if (sender.hasPermission("jonasmpai.admin.playerinfo")) {
               String first = args[0].toLowerCase();
               if ("was".equals(first)) {
                  if (args.length == 2) {
                     return this.filterSuggestions(Collections.singletonList("hat"), args[1]);
                  }

                  if (args.length == 3 && "hat".equalsIgnoreCase(args[1])) {
                     return this.playerNames(args[2]);
                  }
               }

               if ("wo".equals(first)) {
                  if (args.length == 2) {
                     return this.filterSuggestions(Collections.singletonList("ist"), args[1]);
                  }

                  if (args.length == 3 && "ist".equalsIgnoreCase(args[1])) {
                     return this.playerNames(args[2]);
                  }
               }

               if (("info".equals(first) || "profil".equals(first) || "report".equals(first) || "check".equals(first)) && args.length == 2) {
                  return this.playerNames(args[1]);
               }
            }

            return Collections.emptyList();
         }
      } else if (args.length == 1) {
         List<String> base = new ArrayList<>();
         if (sender.hasPermission("jonasmpai.chat.query")) {
            base.addAll(Arrays.asList("frage", "ask", "chat"));
         }

         if (sender.hasPermission("jonasmpai.falsepositive")) {
            base.addAll(Arrays.asList("falsepositive", "fp"));
         }

         base.addAll(Arrays.asList("lang", "language", "sprache"));
         if (sender.hasPermission("jonasmpai.admin.status")) {
            base.add("status");
         }

         if (sender.hasPermission("jonasmpai.admin.moderate")) {
            base.addAll(Arrays.asList("mute", "unmute", "unban"));
         }

         if (sender.hasPermission("jonasmpai.admin.train")) {
            base.add("train");
         }

         if (sender.hasPermission("jonasmpai.admin.stats")) {
            base.add("stats");
         }

         if (sender.hasPermission("jonasmpai.admin.reload")) {
            base.add("reload");
         }

         if (sender.hasPermission("jonasmpai.admin.debug")) {
            base.add("debug");
         }

         if (sender.hasPermission("jonasmpai.admin.voice")) {
            base.add("voice");
         }

         if (sender.hasPermission("jonasmpai.rules.reset")) {
            base.add("rules");
         }

         return this.filterSuggestions(base, args[0]);
      } else {
         if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("mute") || sub.equals("unmute") || sub.equals("unban")) && sender.hasPermission("jonasmpai.admin.moderate")) {
               List<String> names = new ArrayList<>();

               for (Player p : Bukkit.getOnlinePlayers()) {
                  names.add(p.getName());
               }

               return this.filterSuggestions(names, args[1]);
            }

            if (sub.equals("status") && sender.hasPermission("jonasmpai.admin.status")) {
               List<String> names = new ArrayList<>();

               for (Player p : Bukkit.getOnlinePlayers()) {
                  names.add(p.getName());
               }

               return this.filterSuggestions(names, args[1]);
            }

            if (sub.equals("debug") && sender.hasPermission("jonasmpai.admin.debug")) {
               return this.filterSuggestions(Arrays.asList("on", "off", "status", "test", "search"), args[1]);
            }

            if (sub.equals("lang") || sub.equals("language") || sub.equals("sprache")) {
               return this.filterSuggestions(Arrays.asList("de", "en"), args[1]);
            }

            if (sub.equals("voice") && sender.hasPermission("jonasmpai.admin.voice")) {
               return this.filterSuggestions(Arrays.asList("status", "toggle", "mute", "unmute", "logs", "debug"), args[1]);
            }

            if (sub.equals("rules") && sender.hasPermission("jonasmpai.rules.reset")) {
               if (args.length == 2) {
                  return this.filterSuggestions(Collections.singletonList("reset"), args[1]);
               }

               if (args.length == 3 && "reset".equalsIgnoreCase(args[1])) {
                  List<String> names = new ArrayList<>();

                  for (Player p : Bukkit.getOnlinePlayers()) {
                     names.add(p.getName());
                  }

                  return this.filterSuggestions(names, args[2]);
               }
            }
         }

         if (args.length == 3) {
            String subx = args[0].toLowerCase();
            if (subx.equals("voice") && sender.hasPermission("jonasmpai.admin.voice")) {
               String voiceSub = args[1].toLowerCase();
               if (voiceSub.equals("mute") || voiceSub.equals("unmute")) {
                  return this.playerNames(args[2]);
               }

               if (voiceSub.equals("logs")) {
                  return this.playerNames(args[2]);
               }
            }
         }

         if (args.length == 4) {
            String subx = args[0].toLowerCase();
            if (subx.equals("voice") && sender.hasPermission("jonasmpai.admin.voice")) {
               String voiceSubx = args[1].toLowerCase();
               if (voiceSubx.equals("logs")) {
                  return this.filterSuggestions(Collections.singletonList("export"), args[3]);
               }
            }
         }

         return Collections.emptyList();
      }
   }

   private void handleRadar(CommandSender sender) {
      JonaSMP_AI plugin = (JonaSMP_AI)CoreBootstrap.PLUGIN;
      ChunkRadar radar = plugin.getChunkRadar();
      if (radar == null) {
         sender.sendMessage("§c[Radar] ChunkRadar nicht initialisiert.");
      } else {
         sender.sendMessage("§e§l=== ChunkRadar Status ===");
         sender.sendMessage("§7Gecachte Chunks: §f" + radar.getCacheSize());
         sender.sendMessage("§7Cache-Speicher: §f" + String.format("%.2f", radar.getCacheMemoryMb()) + " MB");
         sender.sendMessage("§7Modus: §fBukkit-API (geladene Chunks)");

         try {
            WatcherCore core = WatcherCore.getInstance();
            AIPlayerBot bot = core.getAIPlayerBot();
            if (bot != null && bot.isSpawned()) {
               Player p = bot.getNMSBot().getPlayer();
               if (p != null) {
                  int cx = p.getLocation().getBlockX() >> 4;
                  int cz = p.getLocation().getBlockZ() >> 4;
                  boolean cached = radar.isChunkCached(cx, cz);
                  sender.sendMessage("§7Bot-Chunk: §f" + cx + "," + cz + " §7(" + (cached ? "§agecacht" : "§cnicht gecacht") + "§7)");
               }
            }
         } catch (Exception var10) {
         }

         sender.sendMessage("§7/f radar §f- Zeigt diesen Status");
      }
   }

   private List<String> filterSuggestions(List<String> options, String typed) {
      if (typed != null && !typed.isEmpty()) {
         String lower = typed.toLowerCase();
         List<String> filtered = new ArrayList<>();

         for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) {
               filtered.add(opt);
            }
         }

         return filtered;
      } else {
         return options;
      }
   }

   private List<String> playerNames(String typed) {
      List<String> names = new ArrayList<>();

      for (Player p : Bukkit.getOnlinePlayers()) {
         names.add(p.getName());
      }

      return this.filterSuggestions(names, typed);
   }
}
