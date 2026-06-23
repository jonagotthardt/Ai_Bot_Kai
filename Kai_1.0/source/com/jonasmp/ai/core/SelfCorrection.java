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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SelfCorrection implements Listener {
   private static final File MISTAKES_FILE = new File(CoreBootstrap.PLUGIN.getDataFolder(), "ml-data/mistakes.json");
   private static final long STUCK_THRESHOLD_TICKS = 200L;
   private static final double STUCK_DISTANCE = 0.5;
   private final Map<UUID, Location> lastPositions = new ConcurrentHashMap<>();
   private final Map<UUID, Integer> stuckTicks = new ConcurrentHashMap<>();
   private final List<SelfCorrection.Lesson> lessons = new ArrayList<>();

   public SelfCorrection() {
      this.loadLessons();
      this.startStuckChecker();
      CoreBootstrap.PLUGIN.getLogger().info("[SelfCorrection] Loaded " + this.lessons.size() + " lessons");
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player p = event.getEntity();
      if (p.getName().equals("Kai")) {
         String cause = event.getDeathMessage() != null ? event.getDeathMessage() : "unknown";
         Location loc = p.getLocation();
         String context = this.buildContext(p);
         this.recordLesson(
            new SelfCorrection.Lesson(
               "death_" + System.currentTimeMillis(),
               "DEATH",
               context,
               cause,
               this.inferCorrection(context, cause),
               System.currentTimeMillis(),
               this.countRepeats("DEATH", context) + 1
            )
         );
         CoreBootstrap.PLUGIN.getLogger().info("[SelfCorrection] Kai died: " + cause + " | Context: " + context);
      }
   }

   public boolean shouldAvoidGoal(String goalName, String context) {
      for (SelfCorrection.Lesson lesson : this.lessons) {
         if ((lesson.type.equals("DEATH") || lesson.type.equals("STUCK"))
            && (lesson.context.contains(goalName) || lesson.context.contains(context))
            && lesson.repeats >= 2) {
            CoreBootstrap.PLUGIN.getLogger().fine("[SelfCorrection] Avoiding goal '" + goalName + "' due to repeated mistakes (" + lesson.repeats + "x)");
            return true;
         }
      }

      return false;
   }

   public String getCorrectionHint(String goalName) {
      for (SelfCorrection.Lesson lesson : this.lessons) {
         if (lesson.context.contains(goalName)) {
            return lesson.correction;
         }
      }

      return null;
   }

   private void startStuckChecker() {
      (new BukkitRunnable() {
         {
            Objects.requireNonNull(SelfCorrection.this);
         }

         public void run() {
            SelfCorrection.this.checkStuckBots();
         }
      }).runTaskTimer(CoreBootstrap.PLUGIN, 20L, 20L);
   }

   private void checkStuckBots() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (p.getName().equals("Kai")) {
            UUID uuid = p.getUniqueId();
            Location current = p.getLocation();
            Location last = this.lastPositions.get(uuid);
            if (last != null && last.getWorld().equals(current.getWorld())) {
               if (current.distance(last) < 0.5) {
                  int ticks = this.stuckTicks.getOrDefault(uuid, 0) + 1;
                  this.stuckTicks.put(uuid, ticks);
                  if ((long)ticks >= 200L) {
                     String context = this.buildContext(p);
                     this.recordLesson(
                        new SelfCorrection.Lesson(
                           "stuck_" + System.currentTimeMillis(),
                           "STUCK",
                           context,
                           "No movement for " + ticks / 20 + "s at " + current.getBlockX() + "," + current.getBlockY() + "," + current.getBlockZ(),
                           "Try different path or goal",
                           System.currentTimeMillis(),
                           this.countRepeats("STUCK", context) + 1
                        )
                     );
                     CoreBootstrap.PLUGIN
                        .getLogger()
                        .info("[SelfCorrection] Kai stuck at " + current.getBlockX() + "," + current.getBlockY() + "," + current.getBlockZ());
                     this.stuckTicks.put(uuid, 0);
                  }
               } else {
                  this.stuckTicks.put(uuid, 0);
               }
            }

            this.lastPositions.put(uuid, current.clone());
         }
      }
   }

   private String buildContext(Player p) {
      StringBuilder ctx = new StringBuilder();
      ctx.append("world:").append(p.getWorld().getName());
      ctx.append(",time:").append(p.getWorld().getTime());
      if (p.getWorld().getTime() > 13000L && p.getWorld().getTime() < 23000L) {
         ctx.append(",night");
      }

      if (p.getHealth() < 10.0) {
         ctx.append(",low_health");
      }

      if (p.getInventory().getArmorContents()[0] == null) {
         ctx.append(",no_armor");
      }

      if (p.getLocation().getY() < 60.0) {
         ctx.append(",underground");
      }

      return ctx.toString();
   }

   private String inferCorrection(String context, String cause) {
      if (cause.contains("fell") || cause.contains("high place")) {
         return "Check terrain before moving; place blocks to descend";
      } else if (cause.contains("burned") || cause.contains("lava") || cause.contains("fire")) {
         return "Avoid fire/lava; carry water bucket";
      } else if (cause.contains("drowned")) {
         return "Swim up immediately; avoid deep water";
      } else if (cause.contains("starved")) {
         return "Gather food before long tasks";
      } else if (context.contains("night") && cause.contains("slain")) {
         return "Do not fight mobs at night without armor";
      } else {
         return context.contains("STUCK") ? "Teleport to safe location and retry with different path" : "Be more cautious in similar situations";
      }
   }

   private int countRepeats(String type, String context) {
      int count = 0;

      for (SelfCorrection.Lesson lesson : this.lessons) {
         if (lesson.type.equals(type) && lesson.context.equals(context)) {
            count = Math.max(count, lesson.repeats);
         }
      }

      return count;
   }

   private void recordLesson(SelfCorrection.Lesson lesson) {
      for (int i = 0; i < this.lessons.size(); i++) {
         SelfCorrection.Lesson existing = this.lessons.get(i);
         if (existing.type.equals(lesson.type) && existing.context.equals(lesson.context)) {
            this.lessons
               .set(
                  i,
                  new SelfCorrection.Lesson(
                     existing.id, existing.type, existing.context, existing.cause, existing.correction, lesson.timestamp, existing.repeats + 1
                  )
               );
            this.saveLessons();
            return;
         }
      }

      this.lessons.add(lesson);
      this.saveLessons();
   }

   private void saveLessons() {
      try {
         MISTAKES_FILE.getParentFile().mkdirs();

         try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(MISTAKES_FILE), StandardCharsets.UTF_8))) {
            bw.write("[\n");

            for (int i = 0; i < this.lessons.size(); i++) {
               bw.write("  " + this.lessons.get(i).toJson());
               if (i < this.lessons.size() - 1) {
                  bw.write(",");
               }

               bw.write("\n");
            }

            bw.write("]\n");
         }
      } catch (IOException var6) {
         CoreBootstrap.PLUGIN.getLogger().warning("[SelfCorrection] Save failed: " + var6.getMessage());
      }
   }

   private void loadLessons() {
      if (MISTAKES_FILE.exists()) {
         try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(MISTAKES_FILE), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
               sb.append(line);
            }

            String content = sb.toString().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
               int count = 0;

               for (char c : content.toCharArray()) {
                  if (c == '{') {
                     count++;
                  }
               }

               CoreBootstrap.PLUGIN.getLogger().info("[SelfCorrection] Found approximately " + count + " saved lessons");
            }
         } catch (IOException var12) {
            CoreBootstrap.PLUGIN.getLogger().fine("[SelfCorrection] Load failed: " + var12.getMessage());
         }
      }
   }

   public static class Lesson {
      public final String id;
      public final String type;
      public final String context;
      public final String cause;
      public final String correction;
      public final long timestamp;
      public final int repeats;

      public Lesson(String id, String type, String context, String cause, String correction, long timestamp, int repeats) {
         this.id = id;
         this.type = type;
         this.context = context;
         this.cause = cause;
         this.correction = correction;
         this.timestamp = timestamp;
         this.repeats = repeats;
      }

      public String toJson() {
         return "{\"id\":\""
            + escape(this.id)
            + "\",\"type\":\""
            + this.type
            + "\",\"context\":\""
            + escape(this.context)
            + "\",\"cause\":\""
            + escape(this.cause)
            + "\",\"correction\":\""
            + escape(this.correction)
            + "\",\"timestamp\":"
            + this.timestamp
            + ",\"repeats\":"
            + this.repeats
            + "}";
      }

      private static String escape(String s) {
         return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
      }
   }
}
