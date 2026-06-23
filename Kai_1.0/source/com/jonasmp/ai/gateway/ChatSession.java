package com.jonasmp.ai.gateway;

import com.google.gson.JsonObject;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.config.DebugConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSession {
   private static final long SESSION_TTL_MS = 300000L;
   private final Map<String, ChatSession.SessionData> sessions = new ConcurrentHashMap<>();
   private final Map<String, Boolean> webSearchEnabled = new ConcurrentHashMap<>();
   private final Map<String, String> pendingWebQuery = new ConcurrentHashMap<>();

   public List<JsonObject> getMessagesForPrompt(String playerId, JsonObject systemMsg, JsonObject currentUserMsg) {
      ChatSession.SessionData session = this.sessions.computeIfAbsent(playerId, k -> new ChatSession.SessionData());
      long now = System.currentTimeMillis();
      if (now - session.lastActivity > 300000L) {
         session.history.clear();
         this.webSearchEnabled.remove(playerId);
         this.pendingWebQuery.remove(playerId);
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] Chat session expired for " + playerId + ", memory wiped.");
         }
      }

      session.lastActivity = now;
      List<JsonObject> messages = new ArrayList<>();
      messages.add(systemMsg);
      messages.addAll(session.history);
      messages.add(currentUserMsg);
      return messages;
   }

   public void recordAssistantReply(String playerId, String content) {
      ChatSession.SessionData session = this.sessions.get(playerId);
      if (session != null) {
         JsonObject assistantMsg = new JsonObject();
         assistantMsg.addProperty("role", "assistant");
         assistantMsg.addProperty("content", content);
         session.history.add(assistantMsg);
      }
   }

   public void recordUserMessage(String playerId, JsonObject userMsg) {
      ChatSession.SessionData session = this.sessions.computeIfAbsent(playerId, k -> new ChatSession.SessionData());
      session.history.add(userMsg);
      session.lastActivity = System.currentTimeMillis();
      int maxHistory = 20;

      while (session.history.size() > 20) {
         session.history.remove(0);
      }
   }

   public void clearSession(String playerId) {
      this.sessions.remove(playerId);
   }

   public int getHistorySize(String playerId) {
      ChatSession.SessionData session = this.sessions.get(playerId);
      return session == null ? 0 : session.history.size();
   }

   public boolean isWebSearchEnabled(String playerId) {
      return this.webSearchEnabled.getOrDefault(playerId, false);
   }

   public void setWebSearchEnabled(String playerId, boolean enabled) {
      this.webSearchEnabled.put(playerId, enabled);
   }

   public boolean hasPendingWebQuery(String playerId) {
      return this.pendingWebQuery.containsKey(playerId);
   }

   public String getPendingWebQuery(String playerId) {
      return this.pendingWebQuery.get(playerId);
   }

   public void setPendingWebQuery(String playerId, String query) {
      this.pendingWebQuery.put(playerId, query);
   }

   public void clearPendingWebQuery(String playerId) {
      this.pendingWebQuery.remove(playerId);
   }

   private static class SessionData {
      final List<JsonObject> history = new ArrayList<>();
      long lastActivity = System.currentTimeMillis();
   }
}
