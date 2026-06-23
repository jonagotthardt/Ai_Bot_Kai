package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.decision.DecisionResult;
import java.util.List;

public class ModerationDecision {
   private double blockThreshold = 0.55;
   private double warnThreshold = 0.2;
   private double banThreshold = 0.9;
   private double kickThreshold = 0.75;
   private double muteThreshold = 0.65;

   public void setThresholds(double block, double warn, double ban, double kick, double mute) {
      this.blockThreshold = block;
      this.warnThreshold = warn;
      this.banThreshold = ban;
      this.kickThreshold = kick;
      this.muteThreshold = mute;
   }

   public ModerationDecision.Decision decide(CategoryScorer.ScoreResult localScore, Double backendToxicity, Double backendScam, double playerThreat) {
      double toxicity = backendToxicity != null ? backendToxicity : localScore.totalRisk;
      double scam = backendScam != null ? backendScam : 0.0;
      double finalRisk = Math.max(localScore.totalRisk, toxicity * 0.7);
      finalRisk = Math.max(finalRisk, scam * 0.5);
      if (playerThreat > 60.0) {
         finalRisk = Math.min(finalRisk + 0.08, 1.0);
      } else if (playerThreat > 30.0) {
         finalRisk = Math.min(finalRisk + 0.04, 1.0);
      }

      String category = localScore.primaryCategory;
      String reason = this.buildReason(localScore, toxicity, scam);
      String rec = localScore.recommendedAction;
      if ("BAN".equalsIgnoreCase(rec)) {
         return new ModerationDecision.Decision(DecisionResult.Action.BAN, finalRisk, reason, category, true);
      } else if ("KICK".equalsIgnoreCase(rec)) {
         return new ModerationDecision.Decision(DecisionResult.Action.KICK, finalRisk, reason, category, false);
      } else if ("BLOCK".equalsIgnoreCase(rec) && finalRisk >= this.blockThreshold) {
         return new ModerationDecision.Decision(DecisionResult.Action.BLOCK, finalRisk, reason, category, false);
      } else if ("severe".equals(category) && localScore.totalRisk >= 0.6) {
         return new ModerationDecision.Decision(DecisionResult.Action.BLOCK, 1.0, reason, category, true);
      } else if (finalRisk >= this.banThreshold) {
         return new ModerationDecision.Decision(DecisionResult.Action.BAN, finalRisk, reason, category, true);
      } else if (finalRisk >= this.kickThreshold) {
         return new ModerationDecision.Decision(DecisionResult.Action.KICK, finalRisk, reason, category, false);
      } else if (finalRisk >= this.blockThreshold) {
         return new ModerationDecision.Decision(DecisionResult.Action.BLOCK, finalRisk, reason, category, false);
      } else if (finalRisk >= this.muteThreshold && playerThreat > 20.0) {
         return new ModerationDecision.Decision(DecisionResult.Action.BLOCK, finalRisk, reason + " [repeat offender]", category, false);
      } else {
         return finalRisk >= this.warnThreshold
            ? new ModerationDecision.Decision(DecisionResult.Action.WARN, finalRisk, reason, category, false)
            : new ModerationDecision.Decision(DecisionResult.Action.ALLOW, finalRisk, "Safe message", category, false);
      }
   }

   private String buildReason(CategoryScorer.ScoreResult score, double toxicity, double scam) {
      StringBuilder sb = new StringBuilder();
      if (!score.matches.isEmpty()) {
         sb.append("[").append(score.primaryCategory).append("] ");
         List<String> words = score.matches.get(score.primaryCategory);
         if (words != null && !words.isEmpty()) {
            int show = Math.min(words.size(), 3);
            sb.append(String.join(", ", words.subList(0, show)));
            if (words.size() > show) {
               sb.append("...");
            }
         }
      }

      if (toxicity > 0.3) {
         sb.append(" Toxicity:").append(String.format("%.2f", toxicity));
      }

      if (scam > 0.3) {
         sb.append(" Scam:").append(String.format("%.2f", scam));
      }

      return sb.toString().trim();
   }

   public static class Decision {
      public final DecisionResult.Action action;
      public final double score;
      public final String reason;
      public final String category;
      public final boolean escalateToBan;

      public Decision(DecisionResult.Action action, double score, String reason, String category, boolean escalateToBan) {
         this.action = action;
         this.score = score;
         this.reason = reason;
         this.category = category;
         this.escalateToBan = escalateToBan;
      }
   }
}
