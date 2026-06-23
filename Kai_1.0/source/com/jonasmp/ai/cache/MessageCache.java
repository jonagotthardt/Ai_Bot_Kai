package com.jonasmp.ai.cache;

import com.jonasmp.ai.pipeline.ChatModerationPipeline;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageCache {
   private final ConcurrentHashMap<String, MessageCache.CacheEntry> cache = new ConcurrentHashMap<>();
   private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "JonaSMP-CacheCleaner");
      t.setDaemon(true);
      return t;
   });
   private long defaultTtlMs = 120000L;

   public MessageCache() {
      this.cleaner.scheduleAtFixedRate(this::cleanExpired, 30L, 30L, TimeUnit.SECONDS);
   }

   public void setDefaultTtl(long ttlMs) {
      this.defaultTtlMs = ttlMs;
   }

   public ChatModerationPipeline.PipelineResult get(String message) {
      MessageCache.CacheEntry entry = this.cache.get(this.hash(message));
      if (entry == null) {
         return null;
      } else if (System.currentTimeMillis() > entry.expiresAt) {
         this.cache.remove(this.hash(message));
         return null;
      } else {
         return entry.result;
      }
   }

   public void put(String message, ChatModerationPipeline.PipelineResult result) {
      this.put(message, result, this.defaultTtlMs);
   }

   public void put(String message, ChatModerationPipeline.PipelineResult result, long ttlMs) {
      this.cache.put(this.hash(message), new MessageCache.CacheEntry(result, System.currentTimeMillis() + ttlMs));
   }

   public void invalidate(String message) {
      this.cache.remove(this.hash(message));
   }

   public void clear() {
      this.cache.clear();
   }

   public int size() {
      return this.cache.size();
   }

   public void shutdown() {
      this.cleaner.shutdown();
   }

   private void cleanExpired() {
      long now = System.currentTimeMillis();
      this.cache.entrySet().removeIf(e -> now > e.getValue().expiresAt);
   }

   private String hash(String message) {
      return Integer.toHexString(message.hashCode());
   }

   private static class CacheEntry {
      final ChatModerationPipeline.PipelineResult result;
      final long expiresAt;

      CacheEntry(ChatModerationPipeline.PipelineResult result, long expiresAt) {
         this.result = result;
         this.expiresAt = expiresAt;
      }
   }
}
