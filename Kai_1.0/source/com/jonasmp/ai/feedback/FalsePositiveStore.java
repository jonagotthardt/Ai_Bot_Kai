package com.jonasmp.ai.feedback;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FalsePositiveStore {
   private final File storeFile;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final Set<String> knownFalsePositives = new HashSet<>();
   private final List<FalsePositiveStore.FalsePositiveEntry> entries = new ArrayList<>();

   public FalsePositiveStore(File dataFolder) {
      this.storeFile = new File(dataFolder, "false_positives.json");
      this.load();
   }

   public synchronized void report(String message, String playerId, String reason) {
      String normalized = this.normalize(message);
      if (!this.knownFalsePositives.contains(normalized)) {
         this.knownFalsePositives.add(normalized);
         this.entries.add(new FalsePositiveStore.FalsePositiveEntry(normalized, playerId, System.currentTimeMillis(), reason));
         this.save();
      }
   }

   public boolean isKnownFalsePositive(String message) {
      return this.knownFalsePositives.contains(this.normalize(message));
   }

   private String normalize(String message) {
      return message == null ? "" : message.toLowerCase().trim().replaceAll("\\s+", " ");
   }

   private void load() {
      if (this.storeFile.exists()) {
         try (Reader reader = Files.newBufferedReader(this.storeFile.toPath())) {
            List<FalsePositiveStore.FalsePositiveEntry> loaded = (List<FalsePositiveStore.FalsePositiveEntry>)this.gson
               .fromJson(reader, (new TypeToken<List<FalsePositiveStore.FalsePositiveEntry>>() {
                  {
                     Objects.requireNonNull(FalsePositiveStore.this);
                  }
               }).getType());
            if (loaded != null) {
               this.entries.addAll(loaded);

               for (FalsePositiveStore.FalsePositiveEntry e : loaded) {
                  this.knownFalsePositives.add(e.normalizedMessage);
               }
            }
         } catch (IOException var7) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Failed to load false positives: " + var7.getMessage());
         }
      }
   }

   private void save() {
      try (Writer writer = Files.newBufferedWriter(this.storeFile.toPath())) {
         this.gson.toJson(this.entries, writer);
      } catch (IOException var6) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AI] Failed to save false positives: " + var6.getMessage());
      }
   }

   public int getCount() {
      return this.knownFalsePositives.size();
   }

   private static class FalsePositiveEntry {
      String normalizedMessage;
      String playerId;
      long timestamp;
      String reason;

      FalsePositiveEntry(String normalizedMessage, String playerId, long timestamp, String reason) {
         this.normalizedMessage = normalizedMessage;
         this.playerId = playerId;
         this.timestamp = timestamp;
         this.reason = reason;
      }
   }
}
