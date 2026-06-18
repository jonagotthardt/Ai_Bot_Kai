package com.jonasmp.ai.decision;

public class DecisionResult {
   private final DecisionResult.Action action;
   private final double score;
   private final String reason;

   public DecisionResult(DecisionResult.Action action, double score, String reason) {
      this.action = action;
      this.score = score;
      this.reason = reason;
   }

   public DecisionResult.Action getAction() {
      return this.action;
   }

   public double getScore() {
      return this.score;
   }

   public String getReason() {
      return this.reason;
   }

   public static enum Action {
      ALLOW,
      WARN,
      BLOCK,
      KICK,
      BAN,
      FLAG;
   }
}
