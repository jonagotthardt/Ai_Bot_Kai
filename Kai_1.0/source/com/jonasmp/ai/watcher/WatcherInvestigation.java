package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.decision.DecisionResult;
import com.jonasmp.ai.pipeline.ChatModerationPipeline;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WatcherInvestigation {
   private final WatcherCore core;
   private long lastEscalationCheck = 0L;

   public WatcherInvestigation(WatcherCore core) {
      this.core = core;
   }

   public void tick(Player target) {
      if (target != null && target.isOnline()) {
         long now = System.currentTimeMillis();
         if (now - this.lastEscalationCheck >= 3000L) {
            this.lastEscalationCheck = now;
            WatcherPlayerData data = this.core.getPlayerData(target.getUniqueId());
            if (data != null) {
               this.updateInvestigationScore(data);
               this.checkEscalation(target, data);
            }
         }
      }
   }

   public void onPipelineResult(Player player, ChatModerationPipeline.PipelineResult result) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      if (result.action == DecisionResult.Action.BLOCK) {
         data.addInvestigationScore(15);
         data.incrementSuspiciousChatStrikes();
      } else if (result.action == DecisionResult.Action.WARN) {
         data.addInvestigationScore(8);
      }

      if (data.getInvestigationScore() >= 30 && !data.isUnderInvestigation()) {
         this.core.beginInvestigation(player.getUniqueId());
      }
   }

   private void updateInvestigationScore(WatcherPlayerData data) {
      int base = data.getAntiCheatStrikes() * 10
         + data.getSuspiciousChatStrikes() * 8
         + data.getSuspiciousCombatStrikes() * 6
         + data.getSuspiciousMovementStrikes() * 7
         + data.getXrayIndicators() * 12;
      if (data.getTotalStrikes() >= 3) {
         base += 10;
      }

      if (data.getTotalStrikes() >= 5) {
         base += 20;
      }

      data.setInvestigationScore(base);
   }

   private void checkEscalation(Player target, WatcherPlayerData data) {
      int score = data.getInvestigationScore();
      if (score >= 50 && this.core.getState() != WatcherState.VISIBLE && this.core.getState() != WatcherState.FREEZING) {
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Escalating " + target.getName() + " to VISIBLE (score=" + score);
         this.core.showVisible(target.getUniqueId());
      } else {
         if (score >= 80 && this.core.getState() != WatcherState.FREEZING && this.core.getConfig().isFreezeEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[Watcher] Escalating " + target.getName() + " to FREEZE (score=" + score);
            this.core.freezePlayer(target.getUniqueId());
            this.notifyAdmins(target.getName() + " auto-frozen by Watcher (investigation score=" + score);
         }
      }
   }

   public void clearInvestigation(UUID uuid) {
      WatcherPlayerData data = this.core.getPlayerData(uuid);
      if (data != null) {
         data.setUnderInvestigation(false);
         data.setInvestigationScore(0);
      }
   }

   private void notifyAdmins(String message) {
      String formatted = "§8[§4Watcher§8] §c" + message;
      Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("jonasmpai.watcher.notify")).forEach(p -> p.sendMessage(formatted));
   }
}
