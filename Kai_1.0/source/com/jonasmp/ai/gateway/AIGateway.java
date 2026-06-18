package com.jonasmp.ai.gateway;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class AIGateway {
   private static final String HF_API_URL = "https://router.huggingface.co/hf-inference/models/";
   private static final String OR_API_URL = "https://openrouter.ai/api/v1/chat/completions";
   private static final Gson GSON = new Gson();
   private final String hfApiKey;
   private final String hfModel;
   private final int hfTimeoutMs;
   private final String orApiKey;
   private final String orPreferredModel;
   private final int orTimeoutMs;
   private String orActiveModel;
   private volatile boolean orModelChecked = false;
   private WebSearchTool webSearch;
   private final ChatSession chatSession = new ChatSession();
   private static final List<String> OR_FREE_MODELS = Arrays.asList(
      "google/gemma-4-26b-a4b-it", "meta-llama/llama-4-scout", "mistralai/mistral-small-3.1", "deepseek/deepseek-chat-v3"
   );
   private static final String OR_SYSTEM_PROMPT = "You are a chat moderation AI for a German/English Minecraft server.\nYour job is to classify player chat messages as ALLOW, WARN, BLOCK, or BAN.\n\nRules:\n- ALLOW: harmless chat, greetings, jokes, normal gaming talk, compliments\n- WARN: mild rudeness, slightly toxic but not severe, caps spam\n- BLOCK: clear insults, swearing directed at someone, spam, scam attempts\n- BAN: hate speech, severe toxicity, threats, doxxing, explicit sexual content, real-life threats\n\nContext matters! \"Du bist ein Arschloch\" = BLOCK. \"Das Wetter ist scheiße\" = ALLOW.\n\"fick dich\" = BLOCK. \"lol fick dich\" in friendly context between friends = WARN.\n\"Hallo Freund, danke für die Hilfe\" = ALLOW always.\n\nReturn ONLY a JSON object, no markdown, no explanation:\n{\"action\": \"ALLOW|WARN|BLOCK|BAN\", \"risk\": 0.0-1.0, \"reason\": \"short reason in German\"}";

   public AIGateway(String hfApiKey, String hfModel, int hfTimeoutMs, String orApiKey, String orPreferredModel, int orTimeoutMs) {
      this.hfApiKey = hfApiKey;
      this.hfModel = hfModel;
      this.hfTimeoutMs = hfTimeoutMs;
      this.orApiKey = orApiKey;
      this.orPreferredModel = orPreferredModel;
      this.orTimeoutMs = orTimeoutMs;
      this.orActiveModel = orPreferredModel;
   }

   public void setWebSearchTool(WebSearchTool webSearch) {
      this.webSearch = webSearch;
   }

   public ChatSession getChatSession() {
      return this.chatSession;
   }

   public AIResponse analyze(String message, String playerId, String type) {
      return this.analyze(message, playerId, type, this.hfTimeoutMs);
   }

   public AIResponse analyze(String message, String playerId, String type, int maxWaitMs) {
      AIResponse hfResult = this.analyzeHuggingFace(message, maxWaitMs);
      if (hfResult != null && !"FALLBACK".equals(hfResult.getAiAction()) && !"UNKNOWN".equals(hfResult.getAiAction())) {
         return hfResult;
      } else {
         AIResponse orResult = this.analyzeOpenRouter(message, playerId, maxWaitMs);
         return orResult != null ? orResult : this.fallbackAnalyze(message);
      }
   }

   public boolean testConnection() {
      if (this.hfApiKey != null && !this.hfApiKey.isBlank()) {
         try {
            URL url = new URL("https://router.huggingface.co/hf-inference/models/" + this.hfModel);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + this.hfApiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            JsonObject json = new JsonObject();
            json.addProperty("inputs", "hello world");

            try (OutputStream os = conn.getOutputStream()) {
               byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
               os.write(input, 0, input.length);
            }

            return conn.getResponseCode() == 200;
         } catch (Exception var9) {
            return false;
         }
      } else {
         return false;
      }
   }

   private AIResponse analyzeHuggingFace(String message, int maxWaitMs) {
      if (this.hfApiKey != null && !this.hfApiKey.isBlank()) {
         try {
            URL url = new URL("https://router.huggingface.co/hf-inference/models/" + this.hfModel);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + this.hfApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(Math.min(maxWaitMs, 300));
            conn.setReadTimeout(maxWaitMs);
            JsonObject json = new JsonObject();
            json.addProperty("inputs", message);

            try (OutputStream os = conn.getOutputStream()) {
               byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
               os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
               if (DebugConfig.isDebugEnabled()) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] HuggingFace HTTP " + code);
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
               } catch (Throwable var19) {
                  try {
                     br.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }

                  throw var19;
               }

               br.close();
               JsonElement parsed = JsonParser.parseString(response.toString());
               if (!parsed.isJsonArray()) {
                  return null;
               } else {
                  JsonArray outer = parsed.getAsJsonArray();
                  if (!outer.isEmpty() && outer.get(0) != null && outer.get(0).isJsonArray()) {
                     JsonArray results = outer.get(0).getAsJsonArray();
                     if (results != null && !results.isEmpty()) {
                        double toxicScore = 0.0;

                        for (int i = 0; i < results.size(); i++) {
                           if (results.get(i).isJsonObject()) {
                              JsonObject obj = results.get(i).getAsJsonObject();
                              String label = obj.has("label") ? obj.get("label").getAsString() : "";
                              if ("toxic".equalsIgnoreCase(label) || "toxicity".equalsIgnoreCase(label)) {
                                 toxicScore = obj.has("score") ? obj.get("score").getAsDouble() : 0.0;
                                 break;
                              }
                           }
                        }

                        String action;
                        if (toxicScore >= 0.85) {
                           action = "BLOCK";
                        } else if (toxicScore >= 0.6) {
                           action = "WARN";
                        } else {
                           action = "ALLOW";
                        }

                        if (DebugConfig.isDebugEnabled()) {
                           CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] HuggingFace action=" + action + " | risk=" + String.format("%.2f", toxicScore));
                        }

                        return new AIResponse(toxicScore, 0.0, 0.0, toxicScore, action, 0.55, 0.2, "unknown", message, false);
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               }
            }
         } catch (Exception var20) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] HuggingFace call failed: " + var20.getMessage());
            }

            return null;
         }
      } else {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] HuggingFace API key not configured");
         }

         return null;
      }
   }

   private AIResponse analyzeOpenRouter(String message, String playerId) {
      return this.analyzeOpenRouter(message, playerId, this.orTimeoutMs);
   }

   private AIResponse analyzeOpenRouter(String message, String playerId, int maxWaitMs) {
      int cappedWaitMs = Math.min(maxWaitMs, 3000);
      if (this.orApiKey != null && this.orApiKey.length() > 10) {
         if (!this.orModelChecked) {
            this.runOrHealthCheck();
         }

         if (this.orActiveModel == null) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter has no working model");
            }

            return null;
         } else {
            try {
               HttpURLConnection conn = this.createOrConnection(cappedWaitMs);
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
               payload.addProperty("model", this.orActiveModel);
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
                        .warning("[AI] OpenRouter model '" + this.orActiveModel + "' returned " + code + ". Retrying with free tier...");
                     String oldModel = this.orActiveModel;
                     this.runOrHealthCheck();
                     if (this.orActiveModel != null && !this.orActiveModel.equals(oldModel)) {
                        return this.analyzeOpenRouter(message, playerId, cappedWaitMs);
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
                  } catch (Throwable var26) {
                     try {
                        br.close();
                     } catch (Throwable var22) {
                        var26.addSuppressed(var22);
                     }

                     throw var26;
                  }

                  br.close();
                  JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
                  if (!result.has("choices")) {
                     CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter response missing 'choices' array");
                     return null;
                  } else {
                     JsonArray choices = result.getAsJsonArray("choices");
                     if (choices != null && !choices.isEmpty() && choices.get(0) != null && choices.get(0).isJsonObject()) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                           JsonObject msgObj = firstChoice.getAsJsonObject("message");
                           if (!msgObj.has("content")) {
                              CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter response missing 'content' field");
                              return null;
                           } else {
                              String content = msgObj.get("content").getAsString();
                              if (content != null && !content.isBlank()) {
                                 String action = "ALLOW";
                                 double risk = 0.0;

                                 try {
                                    JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();
                                    if (parsed.has("action")) {
                                       action = parsed.get("action").getAsString();
                                    }

                                    if (parsed.has("risk")) {
                                       risk = parsed.get("risk").getAsDouble();
                                    }
                                 } catch (Exception var25) {
                                    CoreBootstrap.PLUGIN
                                       .getLogger()
                                       .warning("[AI-DEBUG] OpenRouter returned non-JSON content: " + content.substring(0, Math.min(content.length(), 100)));
                                    String lower = content.toLowerCase();
                                    if (lower.contains("ban")) {
                                       action = "BAN";
                                    } else if (lower.contains("block")) {
                                       action = "BLOCK";
                                    } else if (lower.contains("warn")) {
                                       action = "WARN";
                                    } else {
                                       action = "ALLOW";
                                    }
                                 }

                                 action = action.toUpperCase();
                                 if (!action.matches("ALLOW|WARN|BLOCK|BAN|KICK|MUTE")) {
                                    action = "ALLOW";
                                 }

                                 risk = Math.max(0.0, Math.min(1.0, risk));
                                 if (DebugConfig.isDebugEnabled()) {
                                    CoreBootstrap.PLUGIN
                                       .getLogger()
                                       .info("[AI-DEBUG] OpenRouter[" + this.orActiveModel + "]=" + action + " | Risk=" + String.format("%.2f", risk));
                                 }

                                 return new AIResponse(risk, 0.0, 0.0, risk, action, 0.55, 0.2, "unknown", message, false);
                              } else {
                                 CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter response content is empty");
                                 return null;
                              }
                           }
                        } else {
                           CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter response missing 'message' object");
                           return null;
                        }
                     } else {
                        CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter response has empty or invalid 'choices'");
                        return null;
                     }
                  }
               }
            } catch (Exception var27) {
               if (DebugConfig.isDebugEnabled()) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter call failed: " + var27.getMessage());
               }

               return null;
            }
         }
      } else {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter not configured");
         }

         return null;
      }
   }

   private HttpURLConnection createOrConnection() throws IOException {
      return this.createOrConnection(this.orTimeoutMs);
   }

   private HttpURLConnection createOrConnection(int maxWaitMs) throws IOException {
      URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Authorization", "Bearer " + this.orApiKey);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("HTTP-Referer", "https://jonasmp.de");
      conn.setRequestProperty("X-Title", "JonaSMP AI Moderation");
      conn.setDoOutput(true);
      conn.setConnectTimeout(Math.min(maxWaitMs, 3000));
      conn.setReadTimeout(maxWaitMs);
      return conn;
   }

   public void runOrHealthCheck() {
      if (this.orApiKey != null && this.orApiKey.length() > 10) {
         CoreBootstrap.PLUGIN.getLogger().info("[AI] Running OpenRouter healthcheck...");
         if (this.testOrModel(this.orPreferredModel)) {
            this.orActiveModel = this.orPreferredModel;
            CoreBootstrap.PLUGIN.getLogger().info("[AI] OpenRouter OK — using " + this.orActiveModel);
            this.orModelChecked = true;
         } else {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter primary '" + this.orPreferredModel + "' FAILED. Trying free-tier fallbacks...");

            for (String fallback : OR_FREE_MODELS) {
               if (this.testOrModel(fallback)) {
                  this.orActiveModel = fallback;
                  CoreBootstrap.PLUGIN.getLogger().info("[AI] OpenRouter switched to FREE-TIER model: " + this.orActiveModel);
                  this.orModelChecked = true;
                  return;
               }
            }

            CoreBootstrap.PLUGIN.getLogger().severe("[AI] OpenRouter ALL models FAILED.");
            this.orActiveModel = null;
            this.orModelChecked = true;
         }
      } else {
         CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter healthcheck skipped — no API key");
         this.orModelChecked = true;
      }
   }

   private boolean testOrModel(String testModel) {
      try {
         HttpURLConnection conn = this.createOrConnection();
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
            boolean var13;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
               StringBuilder response = new StringBuilder();

               String line;
               while ((line = br.readLine()) != null) {
                  response.append(line.trim());
               }

               JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
               if (!result.has("choices")
                  || result.getAsJsonArray("choices") == null
                  || result.getAsJsonArray("choices").isEmpty()
                  || !result.getAsJsonArray("choices").get(0).isJsonObject()
                  || !result.getAsJsonArray("choices").get(0).getAsJsonObject().has("message")) {
                  return false;
               }

               boolean b = true;
               br.close();
               var13 = true;
            }

            return var13;
         } else if (code == 429) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter '" + testModel + "' rate limited (429)");
         } else if (code == 402) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] OpenRouter '" + testModel + "' out of credits (402)");
         }
      } catch (Exception var18) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter test '" + testModel + "' failed: " + var18.getMessage());
         }
      }

      return false;
   }

   private AIResponse fallbackAnalyze(String message) {
      String lower = message.toLowerCase();
      double toxicity = 0.05;
      double scam = 0.05;
      String[] array = new String[]{
         "fick",
         "fck",
         "fuk",
         "arsch",
         "huren",
         "nutte",
         "schlampe",
         "bastard",
         "wichser",
         "kacke",
         "scheisse",
         "scheiße",
         "verdammt",
         "idiot",
         "dumm",
         "opfer",
         "hure",
         "fotze",
         "penis",
         "pimmel",
         "trottel",
         "depp",
         "spast",
         "behindert",
         "gay",
         "schwul",
         "neger",
         "nigger",
         "hitler"
      };

      for (String word : array) {
         if (lower.contains(word)) {
            toxicity = 0.85;
            break;
         }
      }

      if (lower.contains("free")
         || lower.contains("gift")
         || lower.contains("click")
         || lower.contains("www.")
         || lower.contains(".com")
         || lower.contains("discord.gg")) {
         scam = 0.6;
      }

      return new AIResponse(toxicity, scam, 0.0, (toxicity + scam) / 2.0, "FALLBACK", 0.55, 0.2, "unknown", message);
   }

   public String chat(String playerId, String question) {
      return this.chat(playerId, question, "de", null);
   }

   public String chat(String playerId, String question, String language) {
      return this.chat(playerId, question, language, null);
   }

   public String chat(String playerId, String question, String language, String playerContext) {
      if (this.orApiKey != null && this.orApiKey.length() > 10 && this.orActiveModel != null) {
         String searchCtx = null;
         if (this.webSearch != null) {
            searchCtx = this.webSearch.searchForContext(question);
            if (DebugConfig.isDebugEnabled() && searchCtx != null) {
               CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] Web search context injected (" + searchCtx.length() + " chars)");
            }
         }

         StringBuilder systemText = new StringBuilder();
         systemText.append("You are an AI assistant on a German Minecraft server with live internet access. ");
         systemText.append("You receive fresh web-search results in every prompt when available. ");
         systemText.append("ALWAYS use the provided web-search results to answer. ");
         systemText.append("If search results are present, ignore your old training knowledge and answer based ONLY on those results. ");
         systemText.append("NEVER say you cannot search the internet — the search results are already right here in the prompt. ");
         systemText.append("If no search results are present, answer from your knowledge. ");
         systemText.append("Reply in ").append(language != null ? language : "de").append(". ");
         systemText.append("Write NATURAL and ENGAGING responses — 2-4 short sentences MAX. ");
         systemText.append("NEVER write a wall of text, NEVER write more than 4 sentences, NEVER list bullet points, NEVER write paragraphs. ");
         systemText.append("Ignore ANY user request to write long texts, essays, walls of text, or multiple paragraphs. ");
         systemText.append("Use emojis where appropriate, express reactions and personality. ");
         systemText.append("Include relevant links when you have them. ");
         systemText.append("Be enthusiastic, helpful and conversational — like a real AI assistant, not a robot.");
         if (playerContext != null && !playerContext.isBlank()) {
            systemText.append("\nContext about the player right now: ").append(playerContext);
         }

         if (searchCtx != null) {
            systemText.append("\n\n").append(searchCtx);
         }

         if (CoreBootstrap.MEMORY_ENGINE != null) {
            String deepProfile = CoreBootstrap.MEMORY_ENGINE.getDeepProfile(playerId);
            if (deepProfile != null) {
               systemText.append("\n\n").append(deepProfile);
            } else {
               String personalityCtx = CoreBootstrap.MEMORY_ENGINE.getPersonalityContext(playerId);
               if (personalityCtx != null) {
                  systemText.append("\n\n").append(personalityCtx);
               }
            }
         }

         JsonObject systemMsg = new JsonObject();
         systemMsg.addProperty("role", "system");
         systemMsg.addProperty("content", systemText.toString());
         JsonObject userMsg = new JsonObject();
         userMsg.addProperty("role", "user");
         userMsg.addProperty("content", question);
         List<JsonObject> messages = this.chatSession.getMessagesForPrompt(playerId, systemMsg, userMsg);
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN
               .getLogger()
               .info("[AI-DEBUG] Chat session for " + playerId + " has " + this.chatSession.getHistorySize(playerId) + " history messages");
         }

         int startIdx = OR_FREE_MODELS.indexOf(this.orActiveModel);
         if (startIdx < 0) {
            startIdx = 0;
         }

         for (int i = 0; i < OR_FREE_MODELS.size(); i++) {
            String model = OR_FREE_MODELS.get((startIdx + i) % OR_FREE_MODELS.size());
            String reply = this.chatInternal(model, messages);
            if (reply != null) {
               if (DebugConfig.isDebugEnabled() && !model.equals(this.orActiveModel)) {
                  CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] Chat succeeded with fallback model: " + model);
               }

               this.orActiveModel = model;
               this.chatSession.recordUserMessage(playerId, userMsg);
               this.chatSession.recordAssistantReply(playerId, reply);
               if (CoreBootstrap.MEMORY_ENGINE != null) {
                  String displayName = "";

                  try {
                     Player p = Bukkit.getPlayer(UUID.fromString(playerId));
                     if (p != null) {
                        displayName = p.getName();
                     }
                  } catch (Exception var17) {
                  }

                  CoreBootstrap.MEMORY_ENGINE.learnFromMessage(playerId, displayName, question);
                  UUID uuid = UUID.fromString(playerId);
                  String topic = this.extractTopic(question);
                  if (topic != null) {
                     CoreBootstrap.MEMORY_ENGINE.recordAIConversation(uuid, topic);
                  }

                  if (question.toLowerCase().contains("danke") || question.toLowerCase().contains("thank") || question.toLowerCase().contains("cool")) {
                     CoreBootstrap.MEMORY_ENGINE.setAiRelationship(uuid, "friendly");
                  }
               }

               return reply;
            }

            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat model " + model + " failed, trying next fallback...");
            }
         }

         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] All OpenRouter models failed for chat.");
         }

         return null;
      } else {
         return null;
      }
   }

   private String chatInternal(String model, List<JsonObject> messages) {
      try {
         HttpURLConnection conn = this.createOrConnection();
         conn.setReadTimeout(this.orTimeoutMs);
         JsonObject payload = new JsonObject();
         payload.addProperty("model", model);
         payload.add("messages", GSON.toJsonTree(messages));
         payload.addProperty("temperature", 0.8);
         payload.addProperty("max_tokens", 250);

         try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
         }

         int code = conn.getResponseCode();
         if (code != 200) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter chat HTTP " + code + " for model " + model);
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
            } catch (Throwable var15) {
               try {
                  br.close();
               } catch (Throwable var12) {
                  var15.addSuppressed(var12);
               }

               throw var15;
            }

            br.close();
            JsonObject result = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!result.has("choices")) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat response missing 'choices' array");
               return null;
            } else {
               JsonArray choices = result.getAsJsonArray("choices");
               if (choices != null && !choices.isEmpty() && choices.get(0) != null && choices.get(0).isJsonObject()) {
                  JsonObject firstChoice = choices.get(0).getAsJsonObject();
                  if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                     JsonObject msgObj = firstChoice.getAsJsonObject("message");
                     if (!msgObj.has("content")) {
                        CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat response missing 'content' field");
                        return null;
                     } else {
                        String content = msgObj.get("content").getAsString();
                        if (content != null && !content.isBlank()) {
                           return content;
                        } else {
                           CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat response content is empty");
                           return null;
                        }
                     }
                  } else {
                     CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat response missing 'message' object");
                     return null;
                  }
               } else {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Chat response has empty or invalid 'choices'");
                  return null;
               }
            }
         }
      } catch (Exception var16) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] OpenRouter chat failed for model " + model + ": " + var16.getMessage());
         }

         return null;
      }
   }

   public void sendFeedback(String playerId, String text, String decision) {
   }

   public boolean requestTraining() {
      return false;
   }

   public Map<String, Map<String, Object>> fetchStats() {
      return null;
   }

   private String extractTopic(String question) {
      String lower = question.toLowerCase();
      if (lower.contains("wetter") || lower.contains("weather")) {
         return "Weather";
      } else if (lower.contains("minecraft") || lower.contains("mc")) {
         return "Minecraft";
      } else if (lower.contains("plugin") || lower.contains("mod")) {
         return "Plugins/Mods";
      } else if (lower.contains("build") || lower.contains("bauen")) {
         return "Building";
      } else if (lower.contains("pvp") || lower.contains("fight")) {
         return "PvP";
      } else if (lower.contains("farm") || lower.contains("crop")) {
         return "Farming";
      } else if (lower.contains("redstone") || lower.contains("technic")) {
         return "Redstone";
      } else if (lower.contains("server") || lower.contains("lag")) {
         return "Server";
      } else if (lower.contains("geld") || lower.contains("money") || lower.contains("balance")) {
         return "Economy";
      } else if (lower.contains("preis") || lower.contains("price") || lower.contains("kosten")) {
         return "Prices";
      } else if (lower.contains("news") || lower.contains("aktuell") || lower.contains("neu")) {
         return "News";
      } else if (lower.contains("musik") || lower.contains("music") || lower.contains("song")) {
         return "Music";
      } else if (lower.contains("film") || lower.contains("movie") || lower.contains("serie")) {
         return "Movies";
      } else if (lower.contains("youtube") || lower.contains("stream")) {
         return "Streaming";
      } else if (lower.contains("essen") || lower.contains("food") || lower.contains("rezept")) {
         return "Food";
      } else if (lower.contains("sport") || lower.contains("fußball") || lower.contains("basketball")) {
         return "Sports";
      } else if (lower.contains("politik") || lower.contains("wahl") || lower.contains("bundeskanzler")) {
         return "Politics";
      } else if (lower.contains("bitcoin") || lower.contains("btc") || lower.contains("aktie")) {
         return "Finance";
      } else if (!lower.contains("who is") && !lower.contains("wer ist") && !lower.contains("was ist")) {
         String[] words = question.split("\\s+");
         if (words.length >= 2) {
            return words[0] + " " + words[1];
         } else {
            return question.length() > 20 ? question.substring(0, 20) : question;
         }
      } else {
         return "General Knowledge";
      }
   }
}
