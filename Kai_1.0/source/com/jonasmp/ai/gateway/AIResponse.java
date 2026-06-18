package com.jonasmp.ai.gateway;

public class AIResponse {
   private final double toxicity;
   private final double scam;
   private final double memoryRisk;
   private final double risk;
   private final String aiAction;
   private final double adaptiveBlockThreshold;
   private final double adaptiveWarnThreshold;
   private final String language;
   private final String originalMessage;
   private final boolean chatEligible;

   public AIResponse(
      double toxicity,
      double scam,
      double memoryRisk,
      double risk,
      String aiAction,
      double adaptiveBlockThreshold,
      double adaptiveWarnThreshold,
      String language,
      String originalMessage
   ) {
      this(toxicity, scam, memoryRisk, risk, aiAction, adaptiveBlockThreshold, adaptiveWarnThreshold, language, originalMessage, false);
   }

   public AIResponse(
      double toxicity,
      double scam,
      double memoryRisk,
      double risk,
      String aiAction,
      double adaptiveBlockThreshold,
      double adaptiveWarnThreshold,
      String language,
      String originalMessage,
      boolean chatEligible
   ) {
      this.toxicity = toxicity;
      this.scam = scam;
      this.memoryRisk = memoryRisk;
      this.risk = risk;
      this.aiAction = aiAction;
      this.adaptiveBlockThreshold = adaptiveBlockThreshold;
      this.adaptiveWarnThreshold = adaptiveWarnThreshold;
      this.language = language;
      this.originalMessage = originalMessage;
      this.chatEligible = chatEligible;
   }

   public double getToxicity() {
      return this.toxicity;
   }

   public double getScam() {
      return this.scam;
   }

   public double getMemoryRisk() {
      return this.memoryRisk;
   }

   public double getRisk() {
      return this.risk;
   }

   public String getAiAction() {
      return this.aiAction;
   }

   public double getAdaptiveBlockThreshold() {
      return this.adaptiveBlockThreshold;
   }

   public double getAdaptiveWarnThreshold() {
      return this.adaptiveWarnThreshold;
   }

   public String getLanguage() {
      return this.language;
   }

   public String getOriginalMessage() {
      return this.originalMessage;
   }

   public boolean isChatEligible() {
      return this.chatEligible;
   }
}
