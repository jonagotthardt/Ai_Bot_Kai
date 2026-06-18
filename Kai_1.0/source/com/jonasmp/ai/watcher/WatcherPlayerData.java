package com.jonasmp.ai.watcher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WatcherPlayerData {
   private final UUID playerUuid;
   private final String playerName;
   private int antiCheatStrikes = 0;
   private int suspiciousChatStrikes = 0;
   private int suspiciousCombatStrikes = 0;
   private int suspiciousMovementStrikes = 0;
   private int xrayIndicators = 0;
   private long lastAntiCheatFlag = 0L;
   private long lastSuspiciousChat = 0L;
   private long lastSuspiciousCombat = 0L;
   private long lastSuspiciousMovement = 0L;
   private long lastXrayFlag = 0L;
   private final List<WatcherPlayerData.ObservationRecord> observationHistory = new ArrayList<>();
   private int investigationScore = 0;
   private long investigationStarted = 0L;
   private boolean underInvestigation = false;
   private int freezeCount = 0;
   private long lastFreeze = 0L;
   private int loreAppearances = 0;
   private long lastLoreAppearance = 0L;

   public WatcherPlayerData(UUID playerUuid, String playerName) {
      this.playerUuid = playerUuid;
      this.playerName = playerName;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public int getAntiCheatStrikes() {
      return this.antiCheatStrikes;
   }

   public void incrementAntiCheatStrikes() {
      this.antiCheatStrikes++;
      this.lastAntiCheatFlag = System.currentTimeMillis();
   }

   public int getSuspiciousChatStrikes() {
      return this.suspiciousChatStrikes;
   }

   public void incrementSuspiciousChatStrikes() {
      this.suspiciousChatStrikes++;
      this.lastSuspiciousChat = System.currentTimeMillis();
   }

   public int getSuspiciousCombatStrikes() {
      return this.suspiciousCombatStrikes;
   }

   public void incrementSuspiciousCombatStrikes() {
      this.suspiciousCombatStrikes++;
      this.lastSuspiciousCombat = System.currentTimeMillis();
   }

   public int getSuspiciousMovementStrikes() {
      return this.suspiciousMovementStrikes;
   }

   public void incrementSuspiciousMovementStrikes() {
      this.suspiciousMovementStrikes++;
      this.lastSuspiciousMovement = System.currentTimeMillis();
   }

   public int getXrayIndicators() {
      return this.xrayIndicators;
   }

   public void incrementXrayIndicators() {
      this.xrayIndicators++;
      this.lastXrayFlag = System.currentTimeMillis();
   }

   public int getInvestigationScore() {
      return this.investigationScore;
   }

   public void setInvestigationScore(int score) {
      this.investigationScore = Math.max(0, Math.min(100, score));
   }

   public void addInvestigationScore(int delta) {
      this.setInvestigationScore(this.investigationScore + delta);
   }

   public boolean isUnderInvestigation() {
      return this.underInvestigation;
   }

   public void setUnderInvestigation(boolean under) {
      this.underInvestigation = under;
      if (under && this.investigationStarted == 0L) {
         this.investigationStarted = System.currentTimeMillis();
      }
   }

   public long getInvestigationStarted() {
      return this.investigationStarted;
   }

   public int getFreezeCount() {
      return this.freezeCount;
   }

   public void incrementFreezeCount() {
      this.freezeCount++;
      this.lastFreeze = System.currentTimeMillis();
   }

   public long getLastFreeze() {
      return this.lastFreeze;
   }

   public int getLoreAppearances() {
      return this.loreAppearances;
   }

   public void incrementLoreAppearances() {
      this.loreAppearances++;
      this.lastLoreAppearance = System.currentTimeMillis();
   }

   public long getLastLoreAppearance() {
      return this.lastLoreAppearance;
   }

   public void addObservationRecord(String category, String details, int severity) {
      this.observationHistory.add(new WatcherPlayerData.ObservationRecord(System.currentTimeMillis(), category, details, severity));
      if (this.observationHistory.size() > 50) {
         this.observationHistory.remove(0);
      }
   }

   public List<WatcherPlayerData.ObservationRecord> getObservationHistory() {
      return new ArrayList<>(this.observationHistory);
   }

   public void decayStrikes(int decayMinutes) {
      long now = System.currentTimeMillis();
      long decayMs = (long)decayMinutes * 60000L;
      if (now - this.lastAntiCheatFlag > decayMs) {
         this.antiCheatStrikes = Math.max(0, this.antiCheatStrikes - 1);
      }

      if (now - this.lastSuspiciousChat > decayMs) {
         this.suspiciousChatStrikes = Math.max(0, this.suspiciousChatStrikes - 1);
      }

      if (now - this.lastSuspiciousCombat > decayMs) {
         this.suspiciousCombatStrikes = Math.max(0, this.suspiciousCombatStrikes - 1);
      }

      if (now - this.lastSuspiciousMovement > decayMs) {
         this.suspiciousMovementStrikes = Math.max(0, this.suspiciousMovementStrikes - 1);
      }

      if (now - this.lastXrayFlag > decayMs) {
         this.xrayIndicators = Math.max(0, this.xrayIndicators - 1);
      }
   }

   public int getTotalStrikes() {
      return this.antiCheatStrikes + this.suspiciousChatStrikes + this.suspiciousCombatStrikes + this.suspiciousMovementStrikes + this.xrayIndicators;
   }

   public void reset() {
      this.antiCheatStrikes = 0;
      this.suspiciousChatStrikes = 0;
      this.suspiciousCombatStrikes = 0;
      this.suspiciousMovementStrikes = 0;
      this.xrayIndicators = 0;
      this.investigationScore = 0;
      this.underInvestigation = false;
      this.investigationStarted = 0L;
      this.freezeCount = 0;
      this.observationHistory.clear();
   }

   public static class ObservationRecord {
      public final long timestamp;
      public final String category;
      public final String details;
      public final int severity;

      public ObservationRecord(long timestamp, String category, String details, int severity) {
         this.timestamp = timestamp;
         this.category = category;
         this.details = details;
         this.severity = severity;
      }
   }
}
