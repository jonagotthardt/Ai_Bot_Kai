package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class AIBridgeWriter {
   private static final long WRITE_INTERVAL_TICKS = 100L;
   private static final int MAX_BLOCK_BREAKS = 20;
   private final File bridgeFile;
   private boolean enabled;
   private BukkitRunnable task;
   private final List<String> recentBlockBreaks;

   public AIBridgeWriter(File dataFolder) {
      this.bridgeFile = new File(dataFolder, "ai-bridge.json");
      this.enabled = false;
      this.recentBlockBreaks = new ArrayList<>();
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            this.startTask();
         } else {
            this.stopTask();
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void recordBlockBreak(String info) {
      this.recentBlockBreaks.add(info);

      while (this.recentBlockBreaks.size() > 20) {
         this.recentBlockBreaks.remove(0);
      }
   }

   private void startTask() {
      if (this.task == null) {
         this.task = new BukkitRunnable() {
            {
               Objects.requireNonNull(AIBridgeWriter.this);
            }

            public void run() {
               AIBridgeWriter.this.writeSnapshot();
            }
         };
         this.task.runTaskTimerAsynchronously(CoreBootstrap.PLUGIN, 100L, 100L);
      }
   }

   private void stopTask() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }

   public void writeSnapshot() {
      WatcherCore core = WatcherCore.getInstance();
      AIPlayerBot aiBot = core.getAIPlayerBot();
      if (aiBot != null && aiBot.isSpawned()) {
         Player bot = aiBot.getNMSBot().getPlayer();
         if (bot != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
            sb.append("  \"botName\": \"").append(escapeJson(bot.getName())).append("\",\n");
            Location loc = bot.getLocation();
            sb.append("  \"position\": {\n");
            sb.append("    \"x\": ").append((double)Math.round(loc.getX() * 100.0) / 100.0).append(",\n");
            sb.append("    \"y\": ").append((double)Math.round(loc.getY() * 100.0) / 100.0).append(",\n");
            sb.append("    \"z\": ").append((double)Math.round(loc.getZ() * 100.0) / 100.0).append(",\n");
            sb.append("    \"world\": \"").append(escapeJson(loc.getWorld().getName())).append("\"\n");
            sb.append("  },\n");
            BotGoalPlanner gp = aiBot.getGoalPlanner();
            sb.append("  \"goal\": \"").append(escapeJson(String.valueOf(gp.getCurrentGoal()))).append("\",\n");
            sb.append("  \"step\": \"").append(escapeJson(gp.getCurrentStepDesc())).append("\",\n");
            sb.append("  \"status\": {\n");
            sb.append("    \"health\": ").append((double)Math.round(bot.getHealth() * 10.0) / 10.0).append(",\n");
            sb.append("    \"maxHealth\": ").append((double)Math.round(bot.getMaxHealth() * 10.0) / 10.0).append(",\n");
            sb.append("    \"food\": ").append(bot.getFoodLevel()).append("\n");
            sb.append("  },\n");
            PlayerInventory inv = bot.getInventory();
            Map<String, Integer> totals = new HashMap<>();

            for (int i = 0; i < inv.getSize(); i++) {
               ItemStack item = inv.getItem(i);
               if (item != null && item.getType() != Material.AIR) {
                  totals.merge(String.valueOf(item.getType()), item.getAmount(), Integer::sum);
               }
            }

            sb.append("  \"inventory\": {\n");
            boolean first = true;

            for (Entry<String, Integer> entry : totals.entrySet()) {
               if (!first) {
                  sb.append(",\n");
               }

               sb.append("    \"").append(escapeJson(entry.getKey())).append("\": ").append(entry.getValue());
               first = false;
            }

            sb.append("\n  },\n");
            sb.append("  \"recentBreaks\": [\n");
            first = true;

            for (String b : this.recentBlockBreaks) {
               if (!first) {
                  sb.append(",\n");
               }

               sb.append("    \"").append(escapeJson(b)).append("\"");
               first = false;
            }

            sb.append("\n  ]\n");
            sb.append("}\n");

            try (FileWriter writer = new FileWriter(this.bridgeFile)) {
               writer.write(sb.toString());
            } catch (IOException var15) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AIBridge] Failed to write snapshot: " + var15.getMessage());
            }
         }
      }
   }

   private static String escapeJson(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }
}
