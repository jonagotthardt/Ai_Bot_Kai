package com.jonasmp.ai.gateway;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.config.DebugConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class OpenRouterGateway {
   private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
   private static final Gson GSON = new Gson();
   private static final List<String> FREE_TIER_MODELS = Arrays.asList(
      "google/gemma-4-26b-a4b-it", "meta-llama/llama-4-scout", "mistralai/mistral-small-3.1", "deepseek/deepseek-chat-v3"
   );
   private final String apiKey;
   private final String preferredModel;
   private final int timeoutMs;
   private String activeModel;
   private boolean modelChecked = false;
   private static final String SYSTEM_PROMPT = "You are a chat moderation AI for a German/English Minecraft server.\nYour job is to classify player chat messages as ALLOW, WARN, BLOCK, or BAN.\n\nRules:\n- ALLOW: harmless chat, greetings, jokes, normal gaming talk, compliments\n- WARN: mild rudeness, slightly toxic but not severe, caps spam\n- BLOCK: clear insults, swearing directed at someone, spam, scam attempts\n- BAN: hate speech, severe toxicity, threats, doxxing, explicit sexual content, real-life threats\n\nContext matters! \"Du bist ein Arschloch\" = BLOCK. \"Das Wetter ist scheiße\" = ALLOW.\n\"fick dich\" = BLOCK. \"lol fick dich\" in friendly context between friends = WARN.\n\"Hallo Freund, danke für die Hilfe\" = ALLOW always.\n\nReturn ONLY a JSON object, no markdown, no explanation:\n{\"action\": \"ALLOW|WARN|BLOCK|BAN\", \"risk\": 0.0-1.0, \"reason\": \"short reason in German\"}";
   private static final String HEALTH_TEST_MESSAGE = "Test healthcheck";

   public OpenRouterGateway(String apiKey, String preferredModel, int timeoutMs) {
      this.apiKey = apiKey;
      this.preferredModel = preferredModel;
      this.timeoutMs = timeoutMs;
      this.activeModel = preferredModel;
   }

   public boolean isConfigured() {
      return this.apiKey != null && this.apiKey.length() > 10;
   }

   public String getActiveModel() {
      return this.activeModel;
   }

   public void runHealthCheck() {
      if (!this.isConfigured()) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter healthcheck skipped — no API key");
      } else {
         CoreBootstrap.PLUGIN.getLogger().info("[AI] Running OpenRouter healthcheck...");
         if (this.testModel(this.preferredModel)) {
            this.activeModel = this.preferredModel;
            CoreBootstrap.PLUGIN.getLogger().info("[AI] OpenRouter OK — using " + this.activeModel);
            this.modelChecked = true;
         } else {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter primary model '" + this.preferredModel + "' FAILED. Trying free-tier fallbacks...");

            for (String fallback : FREE_TIER_MODELS) {
               if (this.testModel(fallback)) {
                  this.activeModel = fallback;
                  CoreBootstrap.PLUGIN.getLogger().info("[AI] OpenRouter switched to FREE-TIER model: " + this.activeModel);
                  this.modelChecked = true;
                  return;
               }
            }

            CoreBootstrap.PLUGIN.getLogger().severe("[AI] OpenRouter ALL models FAILED. Direct fallback is DISABLED.");
            this.activeModel = null;
            this.modelChecked = true;
         }
      }
   }

   private boolean testModel(String testModel) {
      try {
         HttpURLConnection conn = this.createConnection();
         conn.setReadTimeout(8000);
         JsonObject systemMsg = new JsonObject();
         systemMsg.addProperty("role", "system");
         systemMsg.addProperty(
            "content",
            "You are a chat moderation AI for a German/English Minecraft server.\nYour job is to classify player chat messages as ALLOW, WARN, BLOCK, or BAN.\n\nRules:\n- ALLOW: harmless chat, greetings, jokes, normal gaming talk, compliments\n- WARN: mild rudeness, slightly toxic but not severe, caps spam\n- BLOCK: clear insults, swearing directed at someone, spam, scam attempts\n- BAN: hate speech, severe toxicity, threats, doxxing, explicit sexual content, real-life threats\n\nContext matters! \"Du bist ein Arschloch\" = BLOCK. \"Das Wetter ist scheiße\" = ALLOW.\n\"fick dich\" = BLOCK. \"lol fick dich\" in friendly context between friends = WARN.\n\"Hallo Freund, danke für die Hilfe\" = ALLOW always.\n\nReturn ONLY a JSON object, no markdown, no explanation:\n{\"action\": \"ALLOW|WARN|BLOCK|BAN\", \"risk\": 0.0-1.0, \"reason\": \"short reason in German\"}"
         );
         JsonObject userMsg = new JsonObject();
         userMsg.addProperty("role", "user");
         userMsg.addProperty("content", "Player 'test' says:\n\"Test healthcheck\"\n\nClassify ONLY as JSON.");
         JsonObject payload = new JsonObject();
         payload.addProperty("model", testModel);
         payload.add("messages", GSON.toJsonTree(new JsonObject[]{systemMsg, userMsg}));
         payload.addProperty("temperature", 0.05);
         payload.addProperty("max_tokens", 80);
         JsonObject responseFormat = new JsonObject();
         responseFormat.addProperty("type", "json_object");
         payload.add("response_format", responseFormat);

         try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
         }

         int code = conn.getResponseCode();
         if (code == 200) {
            boolean var12;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
               StringBuilder response = new StringBuilder();

               String line;
               while ((line = br.readLine()) != null) {
                  response.append(line.trim());
               }

               JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
               result.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
               var12 = true;
            }

            return var12;
         }

         if (code == 429) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter model '" + testModel + "' rate limited (429)");
         } else if (code == 402) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter model '" + testModel + "' out of credits (402)");
         }
      } catch (Exception var17) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter test '" + testModel + "' failed: " + var17.getMessage());
         }
      }

      return false;
   }

   private HttpURLConnection createConnection() throws IOException {
      URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("HTTP-Referer", "https://jonasmp.de");
      conn.setRequestProperty("X-Title", "JonaSMP AI Moderation");
      conn.setDoOutput(true);
      conn.setConnectTimeout(Math.min(this.timeoutMs, 3000));
      conn.setReadTimeout(this.timeoutMs);
      return conn;
   }

   public AIResponse analyze(String message, String playerId) {
      if (!this.isConfigured()) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter not configured (no API key)");
         }

         return null;
      } else {
         if (!this.modelChecked) {
            this.runHealthCheck();
         }

         if (this.activeModel == null) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter has no working model. Skipping direct call.");
            return null;
         } else {
            try {
               HttpURLConnection conn = this.createConnection();
               JsonObject systemMsg = new JsonObject();
               systemMsg.addProperty("role", "system");
               systemMsg.addProperty(
                  "content",
                  "You are a chat moderation AI for a German/English Minecraft server.\nYour job is to classify player chat messages as ALLOW, WARN, BLOCK, or BAN.\n\nRules:\n- ALLOW: harmless chat, greetings, jokes, normal gaming talk, compliments\n- WARN: mild rudeness, slightly toxic but not severe, caps spam\n- BLOCK: clear insults, swearing directed at someone, spam, scam attempts\n- BAN: hate speech, severe toxicity, threats, doxxing, explicit sexual content, real-life threats\n\nContext matters! \"Du bist ein Arschloch\" = BLOCK. \"Das Wetter ist scheiße\" = ALLOW.\n\"fick dich\" = BLOCK. \"lol fick dich\" in friendly context between friends = WARN.\n\"Hallo Freund, danke für die Hilfe\" = ALLOW always.\n\nReturn ONLY a JSON object, no markdown, no explanation:\n{\"action\": \"ALLOW|WARN|BLOCK|BAN\", \"risk\": 0.0-1.0, \"reason\": \"short reason in German\"}"
               );
               JsonObject userMsg = new JsonObject();
               userMsg.addProperty("role", "user");
               userMsg.addProperty("content", "Player '" + (playerId != null ? playerId : "unknown") + "' says:\n\"" + message + "\"\n\nClassify ONLY as JSON.");
               JsonObject payload = new JsonObject();
               payload.addProperty("model", this.activeModel);
               payload.add("messages", GSON.toJsonTree(new JsonObject[]{systemMsg, userMsg}));
               payload.addProperty("temperature", 0.05);
               payload.addProperty("max_tokens", 80);
               JsonObject responseFormat = new JsonObject();
               responseFormat.addProperty("type", "json_object");
               payload.add("response_format", responseFormat);

               try (OutputStream os = conn.getOutputStream()) {
                  byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                  os.write(input, 0, input.length);
               }

               int code = conn.getResponseCode();
               if (code != 200) {
                  if (code == 429 || code == 402) {
                     CoreBootstrap.PLUGIN
                        .getLogger()
                        .warning("[AI] OpenRouter model '" + this.activeModel + "' returned " + code + ". Retrying with free tier...");
                     String oldModel = this.activeModel;
                     this.runHealthCheck();
                     if (this.activeModel != null && !this.activeModel.equals(oldModel)) {
                        return this.analyze(message, playerId);
                     }
                  }

                  if (DebugConfig.isDebugEnabled()) {
                     CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter HTTP " + code);
                  }

                  return null;
               } else {
                  StringBuilder response = new StringBuilder();
                  BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                  String line;
                  try {
                     while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                     }
                  } catch (Throwable var20) {
                     try {
                        br.close();
                     } catch (Throwable var17) {
                        var20.addSuppressed(var17);
                     }

                     throw var20;
                  }

                  br.close();
                  JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
                  line = result.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                  JsonObject parsed = JsonParser.parseString(line).getAsJsonObject();
                  String action = parsed.has("action") ? parsed.get("action").getAsString() : "ALLOW";
                  double risk = parsed.has("risk") ? parsed.get("risk").getAsDouble() : 0.0;
                  String reason = parsed.has("reason") ? parsed.get("reason").getAsString() : "no reason";
                  action = action.toUpperCase();
                  if (!action.matches("ALLOW|WARN|BLOCK|BAN|KICK|MUTE")) {
                     action = "ALLOW";
                  }

                  risk = Math.max(0.0, Math.min(1.0, risk));
                  if (DebugConfig.isDebugEnabled()) {
                     CoreBootstrap.PLUGIN
                        .getLogger()
                        .info("[AI-DEBUG] OpenRouter[" + this.activeModel + "]=" + action + " | Risk=" + String.format("%.2f", risk) + " | Reason=" + reason);
                  }

                  return new AIResponse(risk, 0.0, 0.0, risk, action, 0.55, 0.2, "unknown", message, false);
               }
            } catch (Exception var21) {
               if (DebugConfig.isDebugEnabled()) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter direct call failed: " + var21.getMessage());
               }

               return null;
            }
         }
      }
   }
}
