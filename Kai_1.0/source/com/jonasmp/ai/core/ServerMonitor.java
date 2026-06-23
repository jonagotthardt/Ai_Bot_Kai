package com.jonasmp.ai.core;

import com.jonasmp.ai.JonaSMP_AI;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.radar.ChunkRadar;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ServerMonitor implements Runnable {
   private static final double TPS_WARN_THRESHOLD = 15.0;
   private static final double TPS_CRITICAL = 10.0;
   private static final double RAM_WARN_PERCENT = 0.85;
   private static final double RAM_CRITICAL_PERCENT = 0.95;
   private static final double SWAP_WARN_PERCENT = 0.5;
   private static final long WARN_COOLDOWN_MS = 180000L;
   private static final long CRITICAL_COOLDOWN_MS = 60000L;
   private long lastTpsWarn = 0L;
   private long lastTpsCritical = 0L;
   private long lastRamWarn = 0L;
   private long lastRamCritical = 0L;
   private long lastSwapWarn = 0L;
   private boolean tpsCriticalActive = false;
   private boolean ramCriticalActive = false;

   public void start() {
      Bukkit.getScheduler().runTaskTimerAsynchronously(CoreBootstrap.PLUGIN, this, 600L, 600L);
      CoreBootstrap.PLUGIN.getLogger().info("[AI] ServerMonitor started (30s interval)");
   }

   @Override
   public void run() {
      long now = System.currentTimeMillis();
      double[] tpsArray = Bukkit.getTPS();
      double tps = tpsArray.length > 0 ? Math.min(tpsArray[0], 20.0) : 20.0;
      if (tps < 10.0) {
         if (now - this.lastTpsCritical > 60000L) {
            this.lastTpsCritical = now;
            this.tpsCriticalActive = true;
            this.broadcastToAdmins("§c§l[AI-WARNUNG] §cKRITISCHER LAG! TPS ist auf " + String.format("%.1f", tps) + "/20.0 gefallen!");
         }
      } else if (tps < 15.0) {
         if (now - this.lastTpsWarn > 180000L) {
            this.lastTpsWarn = now;
            this.tpsCriticalActive = false;
            this.broadcastToAdmins("§e[AI-WARNUNG] §eServer laggt! TPS: " + String.format("%.1f", tps) + "/20.0");
         }
      } else if (this.tpsCriticalActive) {
         this.tpsCriticalActive = false;
         this.broadcastToAdmins("§a[AI-INFO] §aTPS hat sich erholt: " + String.format("%.1f", tps) + "/20.0");
      }

      Runtime rt = Runtime.getRuntime();
      long total = rt.totalMemory();
      long free = rt.freeMemory();
      long max = rt.maxMemory();
      double usedPercent = (double)(total - free) / (double)max;

      try {
         ChunkRadar radar = JonaSMP_AI.getInstance().getChunkRadar();
         if (radar != null) {
            int cacheSize = radar.getCacheSize();
            double cacheMem = radar.getCacheMemoryMb();
            if (cacheSize > 0) {
               CoreBootstrap.PLUGIN.getLogger().info("[AI-Monitor] ChunkRadar cache: " + cacheSize + " chunks (~" + String.format("%.1f", cacheMem) + " MB)");
            }
         }
      } catch (Exception var35) {
      }

      if (usedPercent >= 0.95) {
         if (now - this.lastRamCritical > 60000L) {
            this.lastRamCritical = now;
            this.ramCriticalActive = true;
            long usedMb = (total - free) / 1024L / 1024L;
            long maxMb = max / 1024L / 1024L;
            this.broadcastToAdmins(
               "§c§l[AI-WARNUNG] §cKRITISCHER RAM! " + usedMb + "MB / " + maxMb + "MB (" + String.format("%.0f", usedPercent * 100.0) + "%)"
            );
         }
      } else if (usedPercent >= 0.85) {
         if (now - this.lastRamWarn > 180000L) {
            this.lastRamWarn = now;
            this.ramCriticalActive = false;
            long usedMb = (total - free) / 1024L / 1024L;
            long maxMb = max / 1024L / 1024L;
            this.broadcastToAdmins("§e[AI-WARNUNG] §eRAM wird knapp: " + usedMb + "MB / " + maxMb + "MB (" + String.format("%.0f", usedPercent * 100.0) + "%)");
         }
      } else if (this.ramCriticalActive) {
         this.ramCriticalActive = false;
         this.broadcastToAdmins("§a[AI-INFO] §aRAM-Nutzung ist wieder im grünen Bereich.");
      }

      if (CoreBootstrap.CONFIG == null || CoreBootstrap.CONFIG.isPhysicalMemoryWarningEnabled()) {
         try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
            long physTotal = osBean.getTotalPhysicalMemorySize();
            long physFree = osBean.getFreePhysicalMemorySize();
            long virtCommitted = osBean.getCommittedVirtualMemorySize();
            long processPhysUsed = physTotal - physFree;
            long swapUsed = Math.max(0L, virtCommitted - processPhysUsed);
            double swapRatio = physTotal > 0L ? (double)swapUsed / (double)virtCommitted : 0.0;
            if (swapUsed > 0L && swapRatio >= 0.5 && now - this.lastSwapWarn > 180000L) {
               this.lastSwapWarn = now;
               long swapMb = swapUsed / 1024L / 1024L;
               long physMb = processPhysUsed / 1024L / 1024L;
               long virtMb = virtCommitted / 1024L / 1024L;
               this.broadcastToAdmins(
                  "§c[AI-WARNUNG] §cServer läuft zu "
                     + String.format("%.0f", swapRatio * 100.0)
                     + "% über SWAP/Pagefile! Phys: "
                     + physMb
                     + "MB | Swap: "
                     + swapMb
                     + "MB | Gesamt: "
                     + virtMb
                     + "MB"
               );
            }
         } catch (Exception var34) {
         }
      }
   }

   private void broadcastToAdmins(String message) {
      Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("jonasmpai.admin.monitor")) {
               p.sendMessage(message);
            }
         }

         CoreBootstrap.PLUGIN.getLogger().warning(message.replaceAll("§[0-9a-fk-or]", ""));
      });
   }
}
