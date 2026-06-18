package com.jonasmp.ai.decision;

import com.jonasmp.ai.gateway.AIResponse;

public class DecisionEngine {
   public DecisionResult evaluate(AIResponse ai) {
      double toxicity = ai.getToxicity();
      double scam = ai.getScam();
      if (toxicity >= 0.55) {
         return new DecisionResult(DecisionResult.Action.BLOCK, toxicity, "Toxic language detected");
      } else if (scam >= 0.6) {
         return new DecisionResult(DecisionResult.Action.BLOCK, scam, "Scam/Advertisement detected");
      } else if (toxicity >= 0.2) {
         return new DecisionResult(DecisionResult.Action.WARN, toxicity, "Potentially toxic language");
      } else {
         return scam >= 0.25
            ? new DecisionResult(DecisionResult.Action.WARN, scam, "Suspicious links/advertisement")
            : new DecisionResult(DecisionResult.Action.ALLOW, Math.max(toxicity, scam), "Safe message");
      }
   }
}
