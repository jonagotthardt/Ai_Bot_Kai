package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WatcherObserver {
   private final WatcherCore core;
   private long lastRecordTime = 0L;

   public WatcherObserver(WatcherCore core) {
      this.core = core;
   }

   public void record(Player target) {
      if (target != null && target.isOnline()) {
         long now = System.currentTimeMillis();
         if (now - this.lastRecordTime >= 5000L) {
            this.lastRecordTime = now;
            WatcherPlayerData data = this.core.ensurePlayerData(target.getUniqueId(), target.getName());
            Location loc = target.getLocation();
            String locStr = String.format("%.1f,%.1f,%.1f", loc.getX(), loc.getY(), loc.getZ());
            data.addObservationRecord("location", "At " + locStr, 0);
            this.recordMovementSpeed(target, data);
         }
      }
   }

   private void recordMovementSpeed(Player target, WatcherPlayerData data) {
      Location current = target.getLocation();
      Location last = data.getObservationHistory().isEmpty() ? null : null;
   }

   public void onBlockBreak(Player player, String blockType) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.addObservationRecord("block_break", "Broke " + blockType, 1);
   }

   public void onBlockPlace(Player player, String blockType) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.addObservationRecord("block_place", "Placed " + blockType, 1);
   }

   public void onContainerOpen(Player player, String containerType) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.addObservationRecord("container", "Opened " + containerType, 1);
   }

   public void onPlayerCombat(Player attacker, Player victim, double damage) {
      WatcherPlayerData data = this.core.ensurePlayerData(attacker.getUniqueId(), attacker.getName());
      int severity = damage > 5.0 ? 3 : (damage > 2.0 ? 2 : 1);
      data.addObservationRecord("combat", "Hit " + victim.getName() + " for " + String.format("%.1f", damage), severity);
   }

   public void onAntiCheatFlag(Player player, String checkType, double violation) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.incrementAntiCheatStrikes();
      int severity = violation > 10.0 ? 5 : (violation > 5.0 ? 3 : 1);
      data.addObservationRecord("anticheat", "Flagged: " + checkType + " (vl=" + String.format("%.1f", violation), severity);
      CoreBootstrap.PLUGIN.getLogger().info("[Watcher] AntiCheat flag on " + player.getName() + ": " + checkType + "=" + violation);
   }

   public void onSuspiciousChat(Player player, String message, double riskScore) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.incrementSuspiciousChatStrikes();
      int severity = riskScore > 0.8 ? 4 : (riskScore > 0.5 ? 2 : 1);
      data.addObservationRecord("chat", "Risk=" + String.format("%.2f", riskScore) + ": " + message, severity);
   }

   public void onSuspiciousMovement(Player player, String reason) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.incrementSuspiciousMovementStrikes();
      data.addObservationRecord("movement", reason, 3);
   }

   public void onXrayIndicator(Player player, String oreType, int count) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      data.incrementXrayIndicators();
      data.addObservationRecord("xray", "Mined " + count + " " + oreType + " in short time", 4);
   }
}
