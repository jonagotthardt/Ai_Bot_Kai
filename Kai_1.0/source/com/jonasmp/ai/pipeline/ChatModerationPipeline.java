package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.cache.MessageCache;
import com.jonasmp.ai.config.DebugConfig;
import com.jonasmp.ai.decision.DecisionResult;
import com.jonasmp.ai.filter.CompoundWordDetector;
import com.jonasmp.ai.filter.EmojiInjectorDetector;
import com.jonasmp.ai.filter.ObfuscationDetector;
import com.jonasmp.ai.filter.SplitWordDetector;
import com.jonasmp.ai.filter.UnicodeAttackDetector;
import com.jonasmp.ai.gateway.AIResponse;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ChatModerationPipeline {
   private final TextNormalizer normalizer = new TextNormalizer();
   private final LeetspeakDecoder leetspeakDecoder = new LeetspeakDecoder();
   private final SafeWordFilter safeWordFilter;
   private final RegexMatcher regexMatcher;
   private final MessageCache cache;
   private final ObfuscationDetector obfuscationDetector = new ObfuscationDetector();
   private final SplitWordDetector splitWordDetector = new SplitWordDetector();
   private final UnicodeAttackDetector unicodeAttackDetector = new UnicodeAttackDetector();
   private final EmojiInjectorDetector emojiInjectorDetector = new EmojiInjectorDetector();
   private final CompoundWordDetector compoundWordDetector = new CompoundWordDetector();
   private static volatile long lastChatTimestamp = System.currentTimeMillis();
   private static volatile int messagesLastMinute = 0;
   private static volatile long lastMinuteWindow = System.currentTimeMillis();
   private static final double MIN_CHAT_CHANCE = 0.25;
   private static final double MAX_CHAT_CHANCE = 0.8;

   public ChatModerationPipeline(WordlistLoader wordlistLoader, MessageCache cache) {
      this.cache = cache;
      this.safeWordFilter = new SafeWordFilter(wordlistLoader);
      this.regexMatcher = new RegexMatcher(wordlistLoader);
   }

   public ChatModerationPipeline.PipelineResult process(String rawMessage, String playerId) {
      long start = System.nanoTime();

      try {
         ChatModerationPipeline.PipelineResult cached = this.cache.get(rawMessage);
         if (cached != null) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] CACHE HIT for message hash");
            }

            return cached;
         } else {
            String text = this.normalizer.normalize(rawMessage);
            if (text.isEmpty()) {
               return new ChatModerationPipeline.PipelineResult(DecisionResult.Action.ALLOW, 0.0, "Empty message", "none", false, 0L, null);
            } else {
               String leetDecoded = this.leetspeakDecoder.decode(text);
               boolean obf = this.obfuscationDetector.detect(rawMessage, text, leetDecoded);
               boolean split = this.splitWordDetector.detect(rawMessage, text);
               boolean unicode = this.unicodeAttackDetector.detect(rawMessage, 0.0);
               boolean emoji = this.emojiInjectorDetector.detect(rawMessage);
               boolean compound = this.compoundWordDetector.detect(leetDecoded, CoreBootstrap.WORDLIST_LOADER);
               ChatModerationPipeline.DetectionFlags flags = new ChatModerationPipeline.DetectionFlags(obf, unicode, emoji, split, compound);
               Map<String, RegexMatcher.RegexResult> regexResults = this.regexMatcher.matchAll(leetDecoded);
               double localRisk = 0.0;
               String topCategory = "none";
               String localReason = "";

               for (Entry<String, RegexMatcher.RegexResult> entry : regexResults.entrySet()) {
                  RegexMatcher.RegexResult res = entry.getValue();
                  if (res.score > localRisk) {
                     localRisk = res.score;
                     topCategory = res.category;
                  }
               }

               if (!regexResults.isEmpty()) {
                  localReason = "Local: " + topCategory + " (" + String.format("%.2f", localRisk);
               }

               if (obf || unicode || split || emoji) {
                  localRisk = Math.min(localRisk + 0.15, 1.0);
               }

               if (compound) {
                  localRisk = Math.min(localRisk + 0.2, 1.0);
               }

               boolean anyLocalHit = obf || split || unicode || emoji || compound || !regexResults.isEmpty();
               if (!anyLocalHit && this.safeWordFilter.isSafe(text)) {
                  return new ChatModerationPipeline.PipelineResult(DecisionResult.Action.ALLOW, 0.0, "Safe word whitelist", "none", false, 0L, flags);
               } else if (CoreBootstrap.FALSE_POSITIVE_STORE != null && CoreBootstrap.FALSE_POSITIVE_STORE.isKnownFalsePositive(rawMessage)) {
                  return new ChatModerationPipeline.PipelineResult(
                     DecisionResult.Action.ALLOW, 0.0, "Known false positive (reported by player)", "none", false, 0L, flags
                  );
               } else {
                  double blockThreshold = CoreBootstrap.CONFIG.getBlockThreshold();
                  double warnThreshold = CoreBootstrap.CONFIG.getWarnThreshold();
                  if (localRisk >= blockThreshold) {
                     long durationMs = (System.nanoTime() - start) / 1000000L;
                     ChatModerationPipeline.PipelineResult localResult = new ChatModerationPipeline.PipelineResult(
                        DecisionResult.Action.BLOCK, localRisk, localReason.isEmpty() ? "Local filter block" : localReason, "local", false, durationMs, flags
                     );
                     this.cache.put(rawMessage, localResult, 300000L);
                     return localResult;
                  } else if (localRisk >= warnThreshold) {
                     long durationMs = (System.nanoTime() - start) / 1000000L;
                     ChatModerationPipeline.PipelineResult localResult = new ChatModerationPipeline.PipelineResult(
                        DecisionResult.Action.WARN, localRisk, localReason.isEmpty() ? "Local filter warn" : localReason, "local", false, durationMs, flags
                     );
                     this.cache.put(rawMessage, localResult, 300000L);
                     return localResult;
                  } else {
                     if (CoreBootstrap.CONFIG.isAIEnabled() && CoreBootstrap.AI_GATEWAY != null) {
                        try {
                           int aiTimeout = CoreBootstrap.CONFIG.getAITimeout();
                           if (aiTimeout <= 0) {
                              aiTimeout = 5000;
                           }

                           AIResponse aiResponse = CoreBootstrap.AI_GATEWAY.analyze(rawMessage, playerId, "chat", aiTimeout);
                           if (aiResponse != null && !"FALLBACK".equals(aiResponse.getAiAction()) && !"UNKNOWN".equals(aiResponse.getAiAction())) {
                              DecisionResult.Action mlAction = parseBackendAction(aiResponse.getAiAction());
                              double risk = aiResponse.getRisk();
                              if (risk < warnThreshold) {
                                 mlAction = DecisionResult.Action.ALLOW;
                              } else if (risk < blockThreshold) {
                                 mlAction = DecisionResult.Action.WARN;
                              }

                              long durationMs2 = (System.nanoTime() - start) / 1000000L;
                              if (DebugConfig.isDebugEnabled()) {
                                 CoreBootstrap.PLUGIN
                                    .getLogger()
                                    .info(
                                       "[AI-DEBUG] AI="
                                          + mlAction
                                          + " | Risk="
                                          + String.format("%.2f", risk)
                                          + " | Thresholds="
                                          + warnThreshold
                                          + "/"
                                          + blockThreshold
                                          + " | Ms="
                                          + durationMs2
                                    );
                              }

                              double chatChance = this.calculateChatChance();
                              if (mlAction == DecisionResult.Action.ALLOW && aiResponse.isChatEligible() && Math.random() < chatChance) {
                                 Player senderPlayer = Bukkit.getPlayer(playerId);
                                 if (senderPlayer != null && senderPlayer.hasPermission("jonasmpai.chat.spontaneous")) {
                                    String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(senderPlayer.getUniqueId());
                                    String context = this.buildPlayerContext(senderPlayer);
                                    Bukkit.getScheduler().runTaskAsynchronously(CoreBootstrap.PLUGIN, () -> {
                                       String reply = CoreBootstrap.AI_GATEWAY.chat(playerId, rawMessage, lang, context);
                                       if (reply != null && !reply.isEmpty()) {
                                          Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
                                             String prefix = "en".equals(lang) ? "§b[AI] §f" : "§b[KI] §f";
                                             senderPlayer.sendMessage(prefix + reply);
                                          });
                                       }
                                    });
                                 }
                              }

                              ChatModerationPipeline.PipelineResult mlResult = new ChatModerationPipeline.PipelineResult(
                                 mlAction,
                                 aiResponse.getRisk(),
                                 "AI: " + aiResponse.getAiAction(),
                                 "ai",
                                 mlAction == DecisionResult.Action.BAN,
                                 durationMs2,
                                 flags
                              );
                              this.cache.put(rawMessage, mlResult, mlAction == DecisionResult.Action.ALLOW ? 30000L : 300000L);
                              return mlResult;
                           }
                        } catch (Exception var38) {
                           if (DebugConfig.isDebugEnabled()) {
                              CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] AI backend failed: " + var38.getMessage());
                           }
                        }
                     }

                     long durationMs = (System.nanoTime() - start) / 1000000L;
                     if (localRisk >= blockThreshold) {
                        ChatModerationPipeline.PipelineResult fallbackResult = new ChatModerationPipeline.PipelineResult(
                           DecisionResult.Action.BLOCK,
                           localRisk,
                           localReason.isEmpty() ? "Local fallback block (AI offline)" : localReason + " (AI offline)",
                           "local-fallback",
                           false,
                           durationMs,
                           flags
                        );
                        this.cache.put(rawMessage, fallbackResult, 300000L);
                        return fallbackResult;
                     } else if (localRisk >= warnThreshold) {
                        ChatModerationPipeline.PipelineResult fallbackResult = new ChatModerationPipeline.PipelineResult(
                           DecisionResult.Action.WARN,
                           localRisk,
                           localReason.isEmpty() ? "Local fallback warn (AI offline)" : localReason + " (AI offline)",
                           "local-fallback",
                           false,
                           durationMs,
                           flags
                        );
                        this.cache.put(rawMessage, fallbackResult, 300000L);
                        return fallbackResult;
                     } else {
                        ChatModerationPipeline.PipelineResult safeResult = new ChatModerationPipeline.PipelineResult(
                           DecisionResult.Action.ALLOW, localRisk, "Safe (AI offline, no local hits)", "none", false, durationMs, flags
                        );
                        this.cache.put(rawMessage, safeResult, 30000L);
                        return safeResult;
                     }
                  }
               }
            }
         }
      } catch (Throwable var39) {
         long durationMs3 = (System.nanoTime() - start) / 1000000L;
         CoreBootstrap.PLUGIN.getLogger().severe("[AI] Pipeline CRASHED for message: \"" + rawMessage + "\" | Error: " + var39.getMessage());
         var39.printStackTrace();
         return new ChatModerationPipeline.PipelineResult(
            DecisionResult.Action.ALLOW, 0.0, "Pipeline error – allowed as failsafe", "none", false, durationMs3, null
         );
      }
   }

   private static int actionPriority(String x) {
      String upperCase = x.toUpperCase();

      return switch (upperCase) {
         case "BAN" -> 5;
         case "KICK" -> 4;
         case "BLOCK" -> 3;
         case "MUTE" -> 2;
         case "WARN" -> 1;
         default -> 0;
      };
   }

   private static String pickStricterAction(String a, String b) {
      return actionPriority(a) >= actionPriority(b) ? a : b;
   }

   private static DecisionResult.Action parseBackendAction(String action) {
      if (action == null) {
         return DecisionResult.Action.ALLOW;
      } else {
         String upperCase = action.toUpperCase();

         return switch (upperCase) {
            case "BAN" -> DecisionResult.Action.BAN;
            case "KICK" -> DecisionResult.Action.KICK;
            case "BLOCK" -> DecisionResult.Action.BLOCK;
            case "WARN" -> DecisionResult.Action.WARN;
            case "MUTE" -> DecisionResult.Action.BLOCK;
            default -> DecisionResult.Action.ALLOW;
         };
      }
   }

   private String buildPlayerContext(Player player) {
      StringBuilder ctx = new StringBuilder();
      ctx.append("Player is in world '").append(player.getWorld().getName()).append("'");
      if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
         ctx.append(" holding a ").append(player.getInventory().getItemInMainHand().getType().name().toLowerCase().replace("_", " "));
      }

      if (player.isSprinting()) {
         ctx.append(" and sprinting");
      }

      if (player.isSneaking()) {
         ctx.append(" and sneaking");
      }

      if (player.isFlying()) {
         ctx.append(" and flying");
      }

      if (player.getLocation().getY() < 20.0) {
         ctx.append(" deep underground");
      } else if (player.getLocation().getY() > 100.0) {
         ctx.append(" high up");
      }

      ctx.append(".");
      return ctx.toString();
   }

   private double calculateChatChance() {
      long now = System.currentTimeMillis();
      if (now - lastMinuteWindow > 60000L) {
         messagesLastMinute = 0;
         lastMinuteWindow = now;
      }

      messagesLastMinute++;
      int online = Bukkit.getOnlinePlayers().size();
      if (online <= 0) {
         online = 1;
      }

      double playerBonus = 0.35 / Math.sqrt((double)online);
      double secondsSinceLastChat = (double)(now - lastChatTimestamp) / 1000.0;
      double inactivityBonus = Math.min(secondsSinceLastChat / 300.0, 0.3);
      double activityPenalty = 0.0;
      if (messagesLastMinute > 20) {
         activityPenalty = 0.1;
      } else if (messagesLastMinute > 10) {
         activityPenalty = 0.05;
      }

      double totalChance = 0.25 + playerBonus + inactivityBonus - activityPenalty;
      totalChance = Math.min(totalChance, 0.8);
      totalChance = Math.max(totalChance, 0.25);
      lastChatTimestamp = now;
      if (DebugConfig.isDebugEnabled()) {
         CoreBootstrap.PLUGIN
            .getLogger()
            .info(
               String.format(
                  "[AI-DEBUG] ChatChance=%.1f%% | Players=%d | Inactive=%.0fs | Msg/Min=%d",
                  totalChance * 100.0,
                  online,
                  secondsSinceLastChat,
                  messagesLastMinute
               )
            );
      }

      return totalChance;
   }

   public static class DetectionFlags {
      public final boolean obfuscation;
      public final boolean unicodeAttack;
      public final boolean emojiInjection;
      public final boolean splitWords;
      public final boolean compoundWords;

      public DetectionFlags(boolean obfuscation, boolean unicodeAttack, boolean emojiInjection, boolean splitWords, boolean compoundWords) {
         this.obfuscation = obfuscation;
         this.unicodeAttack = unicodeAttack;
         this.emojiInjection = emojiInjection;
         this.splitWords = splitWords;
         this.compoundWords = compoundWords;
      }
   }

   public static class PipelineResult {
      public final DecisionResult.Action action;
      public final double score;
      public final String reason;
      public final String category;
      public final boolean escalateToBan;
      public final long processingTimeMs;
      public final ChatModerationPipeline.DetectionFlags flags;

      public PipelineResult(
         DecisionResult.Action action,
         double score,
         String reason,
         String category,
         boolean escalateToBan,
         long processingTimeMs,
         ChatModerationPipeline.DetectionFlags flags
      ) {
         this.action = action;
         this.score = score;
         this.reason = reason;
         this.category = category;
         this.escalateToBan = escalateToBan;
         this.processingTimeMs = processingTimeMs;
         this.flags = flags;
      }
   }
}
