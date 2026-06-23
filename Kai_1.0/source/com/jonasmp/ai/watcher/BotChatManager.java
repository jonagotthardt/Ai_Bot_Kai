package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class BotChatManager {
   private final AIPlayerBot aiBot;
   private final Queue<BotChatManager.ChatEvent> eventQueue;
   private long lastSelfChatTime;
   private boolean enabled;
   private int minIntervalMs;
   private int maxMessageLength;
   private boolean useAiGeneration;
   private final Random random;
   private static final int DEFAULT_INTERVAL_MS = 30000;
   private static final int DEFAULT_MAX_LENGTH = 60;
   private static final int CRITICAL_INTERVAL_MS = 5000;
   private static final int MAX_QUEUE_SIZE = 3;

   public BotChatManager(AIPlayerBot aiBot) {
      this.aiBot = aiBot;
      this.eventQueue = new LinkedList<>();
      this.lastSelfChatTime = 0L;
      this.random = new Random();
      this.reloadConfig();
   }

   public void reloadConfig() {
      FileConfiguration cfg = CoreBootstrap.PLUGIN.getConfig();
      this.enabled = cfg.getBoolean("bot.chat.enabled", true);
      this.minIntervalMs = cfg.getInt("bot.chat.self_chat_interval_seconds", 30) * 1000;
      this.maxMessageLength = cfg.getInt("bot.chat.max_message_length", 60);
      this.useAiGeneration = cfg.getBoolean("bot.chat.use_ai_generation", true);
   }

   public void tick() {
      if (this.enabled && !this.eventQueue.isEmpty()) {
         BotChatManager.ChatEvent event = this.eventQueue.peek();
         long now = System.currentTimeMillis();
         int interval = event.critical ? 5000 : this.minIntervalMs;
         if (now - this.lastSelfChatTime >= (long)interval) {
            this.eventQueue.poll();
            this.processEvent(event);
         }
      }
   }

   public void triggerEvent(String eventType, String context) {
      this.triggerEvent(eventType, context, false);
   }

   public void triggerEvent(String eventType, String context, boolean critical) {
      if (this.enabled) {
         if (this.eventQueue.size() < 3) {
            this.eventQueue.offer(new BotChatManager.ChatEvent(eventType, context, critical));
         }
      }
   }

   private void processEvent(BotChatManager.ChatEvent event) {
      if (this.useAiGeneration && this.aiBot != null) {
         this.generateAndSendAiMessage(event);
      } else {
         String msg = this.pickFallbackTemplate(event.type, event.context);
         if (msg != null) {
            this.sendAsKai(msg);
         }
      }
   }

   private void generateAndSendAiMessage(BotChatManager.ChatEvent event) {
      Player botPlayer = this.aiBot.getNMSBot().getPlayer();
      if (botPlayer != null) {
         String prompt = this.buildPrompt(event, botPlayer);
         String model = this.aiBot.currentModel != null ? this.aiBot.currentModel : "meta-llama/llama-4-scout";
         CompletableFuture.runAsync(() -> {
            try {
               String response = this.aiBot.callOpenRouter(prompt, model, 60);
               if (response != null && !response.isBlank()) {
                  String clean = response.trim().split("\n")[0].trim();
                  clean = clean.replace("\"", "").replace("'", "");
                  if (clean.length() > this.maxMessageLength) {
                     clean = clean.substring(0, this.maxMessageLength);
                  }

                  String finalMsg = clean;
                  Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> this.sendAsKai(finalMsg));
               } else {
                  this.useFallback(event);
               }
            } catch (Exception var7) {
               CoreBootstrap.PLUGIN.getLogger().fine("[BotChatManager] AI generation failed: " + var7.getMessage());
               Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> this.useFallback(event));
            }
         });
      }
   }

   private void useFallback(BotChatManager.ChatEvent event) {
      String msg = this.pickFallbackTemplate(event.type, event.context);
      if (msg != null) {
         this.sendAsKai(msg);
      }
   }

   private String buildPrompt(BotChatManager.ChatEvent event, Player bot) {
      StringBuilder sb = new StringBuilder();
      sb.append("Du bist Kai, ein Minecraft-Bot mit Persönlichkeit.");
      sb.append(" Schreibe EINEN kurzen Satz (max ").append(this.maxMessageLength).append(" Zeichen).");
      sb.append(" Sei freundlich, etwas naiv, enthusiastisch. Sprache: Deutsch.");
      sb.append(" Antwort nur den Satz, keine Erklärungen, keine Anführungszeichen.\n\n");
      sb.append("Event: ").append(event.type);
      if (event.context != null) {
         sb.append(" | Kontext: ").append(event.context);
      }

      sb.append(" | HP=").append((int)bot.getHealth()).append("/20");
      sb.append(" | Hunger=").append(bot.getFoodLevel()).append("/20");
      return sb.toString();
   }

   private String pickFallbackTemplate(String eventType, String context) {
      List<String> templates = this.getTemplates(eventType, context);
      return templates != null && !templates.isEmpty() ? templates.get(this.random.nextInt(templates.size())) : null;
   }

   private List<String> getTemplates(String eventType, String context) {
      return switch (eventType) {
         case "death" -> Arrays.asList(
         "Autsch... das hat weh getan!",
         "Nicht schon wieder...",
         "Ich brauche dringend bessere Rüstung!",
         "Das war knapp... oder auch nicht.",
         "RIP Kai... aber ich bin wieder da!"
      );
         case "burrow" -> Arrays.asList(
         "Hilfe! Monster! Ich verstecke mich!", "Schnell, unter die Erde!", "Das wird ein langer Tag...", "Bitte nicht hier rein..."
      );
         case "healed" -> Arrays.asList(
         "Puh, geschafft! Ich lebe noch!", "Wieder fit! Auf geht's!", "Das war knapp, aber ich bin bereit!", "Danke, Erde, für den Schutz!"
      );
         case "ore_found" -> context != null && context.contains("DIAMOND")
         ? Arrays.asList(
            "DIAMANTEN! Ich habe DIAMANTEN gefunden!",
            "Oh mein Gott, ist das ein Diamant?!",
            "Juhu! Endlich Diamanten!",
            "Mein erster Diamant... ich könnte weinen!"
         )
         : (
            context != null && context.contains("IRON")
               ? Arrays.asList("Eisen! Das wird nützlich!", "Endlich Eisen für bessere Werkzeuge!", "Eisenerz! Mein Glückstag!")
               : (
                  context != null && context.contains("GOLD")
                     ? Arrays.asList("Gold! Ich bin reich!", "Bling bling! Gold gefunden!")
                     : Arrays.asList("Schönes Erz gefunden!", "Das wird nützlich!", "Mein Inventar freut sich!")
               )
         );
         case "night" -> Arrays.asList("Die Sonne geht unter... brrr.", "Wird dunkel. Zeit für Schutz!", "Ich hasse die Nacht...");
         case "hunger" -> Arrays.asList("Ich habe so einen Hunger...", "Mein Magen knurrt lauter als ein Creeper!", "Essen... ich brauche Essen...");
         case "player_nearby" -> Arrays.asList("Hallo! Wer bist du?", "Hey! Hast du vielleicht Essen?", "Schön, jemanden zu sehen!");
         default -> null;
      };
   }

   private void sendAsKai(String message) {
      if (message != null && !message.isBlank()) {
         Player botPlayer = this.aiBot.getNMSBot().getPlayer();
         if (botPlayer != null && botPlayer.isOnline()) {
            botPlayer.chat(message);
            this.lastSelfChatTime = System.currentTimeMillis();
            CoreBootstrap.PLUGIN.getLogger().fine("[BotChatManager] Kai: " + message);
         }
      }
   }

   private static record ChatEvent(String type, String context, boolean critical) {
   }
}
