package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.bukkit.scheduler.BukkitRunnable;

public class PatternExtractor {
   private static final File DATA_DIR = new File(CoreBootstrap.PLUGIN.getDataFolder(), "ml-data");
   private static final File PATTERNS_FILE = new File(CoreBootstrap.PLUGIN.getDataFolder(), "ml-data/patterns.json");

   public void start() {
      (new BukkitRunnable() {
         {
            Objects.requireNonNull(PatternExtractor.this);
         }

         public void run() {
            PatternExtractor.this.extractPatterns();
         }
      }).runTaskTimerAsynchronously(CoreBootstrap.PLUGIN, 20L, 6000L);
      CoreBootstrap.PLUGIN.getLogger().info("[PatternExtractor] Started (interval: 5min)");
   }

   public void extractPatterns() {
      try {
         if (!DATA_DIR.exists() || !DATA_DIR.isDirectory()) {
            return;
         }

         File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith("_actions.jsonl"));
         if (files == null || files.length == 0) {
            return;
         }

         List<PatternExtractor.Pattern> allPatterns = new ArrayList<>();

         for (File file : files) {
            String playerName = file.getName().replace("_actions.jsonl", "");
            List<PlayerWatcher.PlayerAction> actions = this.readActions(file);
            if (!actions.isEmpty()) {
               allPatterns.addAll(this.extractSequences(playerName, actions));
               allPatterns.addAll(this.extractSpatialPatterns(playerName, actions));
            }
         }

         allPatterns = this.mergeAndSort(allPatterns);
         this.writePatterns(allPatterns);
         CoreBootstrap.PLUGIN.getLogger().info("[PatternExtractor] Extracted " + allPatterns.size() + " patterns");
      } catch (Exception var9) {
         CoreBootstrap.PLUGIN.getLogger().fine("[PatternExtractor] Error: " + var9.getMessage());
      }
   }

   private List<PlayerWatcher.PlayerAction> readActions(File file) {
      List<PlayerWatcher.PlayerAction> result = new ArrayList<>();

      String line;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
         while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
               String actionType = this.extractJsonField(line, "actionType");
               String blockType = this.extractJsonField(line, "blockType");
               String toolUsed = this.extractJsonField(line, "toolUsed");
               String outcome = this.extractJsonField(line, "outcome");
               long timestamp = Long.parseLong(this.extractJsonField(line, "timestamp"));
               String world = this.extractJsonField(line, "world");
               int blockX = Integer.parseInt(this.extractJsonField(line, "blockX"));
               int blockY = Integer.parseInt(this.extractJsonField(line, "blockY"));
               int blockZ = Integer.parseInt(this.extractJsonField(line, "blockZ"));
               String desc = actionType;
               if (blockType != null && !blockType.isEmpty()) {
                  desc = actionType + ":" + blockType;
               }

               if (toolUsed != null && !toolUsed.equals("AIR")) {
                  desc = desc + " (" + toolUsed + ")";
               }

               PlayerWatcher.PlayerAction action = new PlayerWatcher.PlayerAction(
                  timestamp, null, null, actionType, blockType, world, 0, 0, blockX, blockY, blockZ, toolUsed, true, outcome
               );
               result.add(action);
            }
         }
      } catch (IOException var19) {
         CoreBootstrap.PLUGIN.getLogger().fine("[PatternExtractor] Read error: " + var19.getMessage());
      }

      return result;
   }

   private String extractJsonField(String json, String field) {
      String search = "\"" + field + "\":";
      int idx = json.indexOf(search);
      if (idx < 0) {
         return "";
      } else {
         int start = idx + search.length();

         while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
         }

         if (start >= json.length()) {
            return "";
         } else {
            char c = json.charAt(start);
            if (c == '"') {
               int end = json.indexOf(34, start + 1);

               while (end > 0 && json.charAt(end - 1) == '\\') {
                  end = json.indexOf(34, end + 1);
               }

               return end > start ? json.substring(start + 1, end) : "";
            } else {
               int end = start;

               while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) {
                  end++;
               }

               return json.substring(start, end).trim();
            }
         }
      }
   }

   private List<PatternExtractor.Pattern> extractSequences(String playerName, List<PlayerWatcher.PlayerAction> actions) {
      List<PatternExtractor.Pattern> patterns = new ArrayList<>();
      Map<String, Integer> pairCounts = new HashMap<>();
      Map<String, Long> pairLastSeen = new HashMap<>();

      for (int i = 1; i < actions.size(); i++) {
         String a1 = actions.get(i - 1).toString();
         String a2 = actions.get(i).toString();
         String pair = a1 + " -> " + a2;
         pairCounts.merge(pair, 1, Integer::sum);
         pairLastSeen.put(pair, actions.get(i).timestamp);
      }

      for (Entry<String, Integer> entry : pairCounts.entrySet()) {
         if (entry.getValue() >= 3) {
            String[] parts = entry.getKey().split(" -> ");
            if (parts.length == 2) {
               patterns.add(
                  new PatternExtractor.Pattern(
                     "seq_" + System.currentTimeMillis() + "_" + patterns.size(),
                     playerName,
                     "SEQUENCE",
                     "Frequent action pair (" + entry.getValue() + "x)",
                     Arrays.asList(parts[0], parts[1]),
                     entry.getValue(),
                     pairLastSeen.getOrDefault(entry.getKey(), System.currentTimeMillis())
                  )
               );
            }
         }
      }

      return patterns;
   }

   private List<PatternExtractor.Pattern> extractSpatialPatterns(String playerName, List<PlayerWatcher.PlayerAction> actions) {
      List<PatternExtractor.Pattern> patterns = new ArrayList<>();
      Map<String, List<PlayerWatcher.PlayerAction>> placesByType = new HashMap<>();

      for (PlayerWatcher.PlayerAction a : actions) {
         if ("BLOCK_PLACE".equals(a.actionType) && a.blockType != null) {
            placesByType.computeIfAbsent(a.blockType, k -> new ArrayList<>()).add(a);
         }
      }

      for (Entry<String, List<PlayerWatcher.PlayerAction>> entry : placesByType.entrySet()) {
         List<PlayerWatcher.PlayerAction> list = entry.getValue();
         if (list.size() >= 3) {
            int sameYCount = 0;
            int prevY = Integer.MIN_VALUE;

            for (PlayerWatcher.PlayerAction ax : list) {
               if (ax.blockY == prevY) {
                  sameYCount++;
               }

               prevY = ax.blockY;
            }

            if (sameYCount >= 2) {
               patterns.add(
                  new PatternExtractor.Pattern(
                     "spatial_" + System.currentTimeMillis() + "_" + patterns.size(),
                     playerName,
                     "SPATIAL",
                     "Placed " + entry.getKey() + " on same Y level (" + list.size() + " times)",
                     Collections.singletonList("BLOCK_PLACE:" + entry.getKey()),
                     list.size(),
                     list.get(list.size() - 1).timestamp
                  )
               );
            }
         }
      }

      return patterns;
   }

   private List<PatternExtractor.Pattern> mergeAndSort(List<PatternExtractor.Pattern> patterns) {
      Map<String, PatternExtractor.Pattern> merged = new HashMap<>();

      for (PatternExtractor.Pattern p : patterns) {
         String key = p.playerName + "|" + p.description;
         PatternExtractor.Pattern existing = merged.get(key);
         if (existing == null || p.frequency > existing.frequency) {
            merged.put(key, p);
         }
      }

      List<PatternExtractor.Pattern> result = new ArrayList<>(merged.values());
      result.sort((a, b) -> Integer.compare(b.frequency, a.frequency));
      return result;
   }

   private void writePatterns(List<PatternExtractor.Pattern> patterns) {
      try {
         PATTERNS_FILE.getParentFile().mkdirs();

         try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PATTERNS_FILE), StandardCharsets.UTF_8))) {
            bw.write("[\n");

            for (int i = 0; i < patterns.size(); i++) {
               bw.write("  " + patterns.get(i).toJson());
               if (i < patterns.size() - 1) {
                  bw.write(",");
               }

               bw.write("\n");
            }

            bw.write("]\n");
         }
      } catch (IOException var7) {
         CoreBootstrap.PLUGIN.getLogger().warning("[PatternExtractor] Failed to write patterns: " + var7.getMessage());
      }
   }

   public List<PatternExtractor.Pattern> loadPatterns() {
      List<PatternExtractor.Pattern> result = new ArrayList<>();
      return !PATTERNS_FILE.exists() ? result : result;
   }

   public static class Pattern {
      public final String id;
      public final String playerName;
      public final String type;
      public final String description;
      public final List<String> actions;
      public final int frequency;
      public final long lastSeen;

      public Pattern(String id, String playerName, String type, String description, List<String> actions, int frequency, long lastSeen) {
         this.id = id;
         this.playerName = playerName;
         this.type = type;
         this.description = description;
         this.actions = actions;
         this.frequency = frequency;
         this.lastSeen = lastSeen;
      }

      public String toJson() {
         StringBuilder sb = new StringBuilder();
         sb.append("{");
         sb.append("\"id\":\"").append(escape(this.id)).append("\"");
         sb.append(",\"playerName\":\"").append(escape(this.playerName)).append("\"");
         sb.append(",\"type\":\"").append(this.type).append("\"");
         sb.append(",\"description\":\"").append(escape(this.description)).append("\"");
         sb.append(",\"actions\":[");

         for (int i = 0; i < this.actions.size(); i++) {
            if (i > 0) {
               sb.append(",");
            }

            sb.append("\"").append(escape(this.actions.get(i))).append("\"");
         }

         sb.append("]");
         sb.append(",\"frequency\":").append(this.frequency);
         sb.append(",\"lastSeen\":").append(this.lastSeen);
         sb.append("}");
         return sb.toString();
      }

      private static String escape(String s) {
         return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
      }
   }
}
