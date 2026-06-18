package com.jonasmp.ai.punishment;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.decision.DecisionResult;
import com.jonasmp.ai.gateway.AIResponse;
import java.util.UUID;

public class AdaptivePunishmentEngine {
   public PunishmentResult evaluate(UUID uuid, DecisionResult.Action action, AIResponse ai, String message) {
      double threat = CoreBootstrap.THREAT_MANAGER.getThreat(uuid);
      String safeMsg = this.escapeForDisplay(message);
      StringBuilder reason = new StringBuilder();
      if (ai != null) {
         if (ai.getToxicity() >= 0.55) {
            reason.append("Toxic language detected. ");
         } else if (ai.getToxicity() >= 0.2) {
            reason.append("Potentially toxic language. ");
         }

         if (ai.getScam() >= 0.6) {
            reason.append("Scam/Advertisement detected. ");
         } else if (ai.getScam() >= 0.25) {
            reason.append("Suspicious links/advertisement. ");
         }
      }

      if (action == DecisionResult.Action.BLOCK) {
         reason.append("Message blocked by AI. ");
      }

      if (reason.length() == 0) {
         reason.append("Violated server rules. ");
      }

      reason.append("Msg: ").append(safeMsg).append(". ");
      String finalReason = reason.toString();
      if (action == DecisionResult.Action.WARN) {
         return new PunishmentResult(PunishmentType.WARN, finalReason, (long)threat);
      } else {
         double score = 0.0;
         if (ai != null) {
            score += ai.getToxicity() * 55.0;
            score += ai.getScam() * 35.0;
         }

         if (threat >= 40.0) {
            score += 10.0;
         } else if (threat >= 20.0) {
            score += 5.0;
         }

         reason.append("Score: ").append(Math.round(score)).append("/100.");
         finalReason = reason.toString();
         if (score >= 45.0) {
            return new PunishmentResult(PunishmentType.BAN, finalReason, (long)score);
         } else if (score >= 30.0) {
            return new PunishmentResult(PunishmentType.KICK, finalReason, (long)score);
         } else {
            return score >= 15.0
               ? new PunishmentResult(PunishmentType.MUTE, finalReason, (long)score)
               : new PunishmentResult(PunishmentType.WARN, finalReason, (long)score);
         }
      }
   }

   private String escapeForDisplay(String msg) {
      if (msg != null && !msg.isEmpty()) {
         String cleaned = msg.replace("\"", "'").replace("\n", " ");
         if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 47) + "...";
         }

         return cleaned;
      } else {
         return "(empty)";
      }
   }
}
