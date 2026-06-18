package com.jonasmp.ai.memory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

public class PlayerMemory {
   public UUID uuid;
   public int scamFlags = 0;
   public int raidFlags = 0;
   public int aiBlocks = 0;
   public long lastSeen = System.currentTimeMillis();
   public String displayName = "";
   public int totalInteractions = 0;
   public long lastInteraction = 0L;
   public List<String> interests = new ArrayList<>();
   public String humorStyle = "neutral";
   public String formality = "casual";
   public String preferredLength = "medium";
   public long totalPlayTimeMs = 0L;
   public int sessionsPlayed = 0;
   public long currentSessionStart = 0L;
   public long longestSessionMs = 0L;
   public long avgSessionLengthMs = 0L;
   public long firstJoin = System.currentTimeMillis();
   public long lastLogout = 0L;
   public long[] playTimeByHour = new long[24];
   public long[] playTimeByDay = new long[7];
   public int totalDeaths = 0;
   public int totalPlayerKills = 0;
   public int totalMobKills = 0;
   public int pvpWins = 0;
   public int pvpLosses = 0;
   public String lastDeathCause = "";
   public String mostKilledMob = "";
   public int mostKilledMobCount = 0;
   public long totalBlocksPlaced = 0L;
   public long totalBlocksBroken = 0L;
   public Map<String, Long> blocksPlacedByType = new HashMap<>();
   public Map<String, Long> blocksBrokenByType = new HashMap<>();
   public String favoriteBlock = "";
   public String favoriteTool = "";
   public long totalItemsCrafted = 0L;
   public long totalItemsEnchanted = 0L;
   public Map<String, Long> itemsCrafted = new HashMap<>();
   public Map<String, Long> itemsUsed = new HashMap<>();
   public String favoriteItem = "";
   public long distanceTraveled = 0L;
   public long distanceFlown = 0L;
   public String favoriteWorld = "";
   public int netherVisits = 0;
   public int endVisits = 0;
   public int totalChatMessages = 0;
   public int totalAIConversations = 0;
   public int questionCount = 0;
   public long totalCharsTyped = 0L;
   public int avgMessageLength = 0;
   public String emotionalTone = "neutral";
   public String languagePreference = "auto";
   public int emojiUsage = 0;
   public Map<String, Integer> wordFrequency = new HashMap<>();
   public List<String> catchphrases = new ArrayList<>();
   public String playStyle = "unknown";
   public String buildStyle = "unknown";
   public String riskTolerance = "unknown";
   public String socialStyle = "unknown";
   public List<String> topicsAskedAI = new ArrayList<>();
   public List<String> personalFactsShared = new ArrayList<>();
   public List<String> frustrationTriggers = new ArrayList<>();
   public List<String> thingsTheyLike = new ArrayList<>();
   public List<String> thingsTheyDislike = new ArrayList<>();
   public String aiRelationship = "neutral";
   public long lastVoiceAIResponse = 0L;
   public Map<String, Integer> nearbyPlayersTime = new HashMap<>();
   public Map<String, Integer> mentionedPlayers = new HashMap<>();
   public List<String> trustedPlayers = new ArrayList<>();
   public List<String> rivalPlayers = new ArrayList<>();
   public String clanOrGuild = "";
   public String socialRole = "unknown";
   public double peakBalance = 0.0;
   public double totalMoneyEarned = 0.0;
   public double totalMoneySpent = 0.0;
   public int shopVisits = 0;
   public Map<String, Integer> shopItemsBought = new HashMap<>();
   public Map<String, Integer> shopItemsSold = new HashMap<>();
   public String tradingStyle = "unknown";
   public List<String> milestones = new ArrayList<>();
   public int achievementsUnlocked = 0;
   public String currentGoal = "";
   public int maxGearScore = 0;
   public Map<String, Integer> commandUsage = new HashMap<>();
   public String mostUsedCommand = "";
   public int teleportRequestsSent = 0;
   public int teleportRequestsAccepted = 0;
   public long totalAfkTimeMs = 0L;
   public List<PlayerAction> actionLog = new ArrayList<>();
   public List<SuspiciousEvent> suspiciousEvents = new ArrayList<>();
   public int suspicionScore = 0;
   public String adminNotes = "";
   public boolean flaggedAsSuspicious = false;
   public List<String> adminFlags = new ArrayList<>();
   public long lastBlockPlaceTime = 0L;
   public int blocksInLastMinute = 0;
   public Map<String, Integer> suspiciousPatterns = new HashMap<>();
   public int speedFlags = 0;
   public int flyFlags = 0;
   public int xrayFlags = 0;
   public int nukerFlags = 0;
   public int killauraFlags = 0;
   public int autoclickFlags = 0;
   public int containerSpamFlags = 0;
   public int noFallFlags = 0;
   public int reachFlags = 0;
   public List<LocationSnapshot> locationHistory = new ArrayList<>();
   public int chestsOpenedTotal = 0;
   public Map<String, Integer> containerAccesses = new HashMap<>();
   public List<ChatEntry> chatHistory = new ArrayList<>();
   public List<ConnectionEvent> connectionHistory = new ArrayList<>();
   public String realName = "";
   public String age = "";
   public String location = "";
   public String job = "";
   public String education = "";
   public String mbtiType = "";
   public String enneagramType = "";
   public int bigFiveOpenness = 5;
   public int bigFiveConscientiousness = 5;
   public int bigFiveExtraversion = 5;
   public int bigFiveAgreeableness = 5;
   public int bigFiveNeuroticism = 5;
   public List<String> coreValues = new ArrayList<>();
   public List<String> fears = new ArrayList<>();
   public List<String> strengths = new ArrayList<>();
   public List<String> weaknesses = new ArrayList<>();
   public String primaryMotivation = "";
   public String attachmentStyle = "unknown";
   public String selfEsteemIndicator = "unknown";
   public int perfectionismLevel = 5;
   public String communicationStyle = "unknown";
   public String conflictStyle = "unknown";
   public String emotionalExpression = "unknown";
   public String decisionMakingStyle = "unknown";
   public String stressResponse = "unknown";
   public String energySource = "unknown";
   public List<String> technicalInterests = new ArrayList<>();
   public List<String> creativeInterests = new ArrayList<>();
   public List<String> socialInterests = new ArrayList<>();
   public List<String> hobbies = new ArrayList<>();
   public List<String> sharedLifeEvents = new ArrayList<>();
   public List<String> familyMentioned = new ArrayList<>();
   public List<String> dreamsAndGoals = new ArrayList<>();
   public List<String> regrets = new ArrayList<>();
   public int trustLevel = 3;
   public int vulnerabilityLevel = 2;
   public String howTheySeeAI = "";
   public List<String> topicsNeverDiscussed = new ArrayList<>();
   public boolean isPerfectionist = false;
   public boolean isImpulsive = false;
   public boolean isCompetitive = false;
   public boolean isEmpathetic = false;
   public boolean usesHumorAsDefense = false;
   public boolean overthinks = false;
   public boolean seeksValidation = false;
   public boolean avoidsConflict = false;
   public boolean needsControl = false;

   public PlayerMemory(UUID uuid) {
      this.uuid = uuid;
   }

   public int getRiskScore() {
      return this.scamFlags * 3
         + this.raidFlags * 5
         + this.aiBlocks * 2
         + this.speedFlags * 4
         + this.flyFlags * 6
         + this.xrayFlags * 5
         + this.nukerFlags * 7
         + this.killauraFlags * 8
         + this.autoclickFlags * 4
         + this.containerSpamFlags * 3
         + this.noFallFlags * 5
         + this.reachFlags * 6;
   }

   private void ensureInit() {
      if (this.interests == null) {
         this.interests = new ArrayList<>();
      }

      if (this.humorStyle == null) {
         this.humorStyle = "neutral";
      }

      if (this.formality == null) {
         this.formality = "casual";
      }

      if (this.preferredLength == null) {
         this.preferredLength = "medium";
      }

      if (this.playStyle == null) {
         this.playStyle = "unknown";
      }

      if (this.buildStyle == null) {
         this.buildStyle = "unknown";
      }

      if (this.riskTolerance == null) {
         this.riskTolerance = "unknown";
      }

      if (this.socialStyle == null) {
         this.socialStyle = "unknown";
      }

      if (this.emotionalTone == null) {
         this.emotionalTone = "neutral";
      }

      if (this.languagePreference == null) {
         this.languagePreference = "auto";
      }

      if (this.aiRelationship == null) {
         this.aiRelationship = "neutral";
      }

      if (this.socialRole == null) {
         this.socialRole = "unknown";
      }

      if (this.tradingStyle == null) {
         this.tradingStyle = "unknown";
      }

      if (this.lastDeathCause == null) {
         this.lastDeathCause = "";
      }

      if (this.favoriteBlock == null) {
         this.favoriteBlock = "";
      }

      if (this.favoriteTool == null) {
         this.favoriteTool = "";
      }

      if (this.favoriteItem == null) {
         this.favoriteItem = "";
      }

      if (this.favoriteWorld == null) {
         this.favoriteWorld = "";
      }

      if (this.mostKilledMob == null) {
         this.mostKilledMob = "";
      }

      if (this.displayName == null) {
         this.displayName = "";
      }

      if (this.clanOrGuild == null) {
         this.clanOrGuild = "";
      }

      if (this.currentGoal == null) {
         this.currentGoal = "";
      }

      if (this.mostUsedCommand == null) {
         this.mostUsedCommand = "";
      }

      if (this.catchphrases == null) {
         this.catchphrases = new ArrayList<>();
      }

      if (this.topicsAskedAI == null) {
         this.topicsAskedAI = new ArrayList<>();
      }

      if (this.personalFactsShared == null) {
         this.personalFactsShared = new ArrayList<>();
      }

      if (this.frustrationTriggers == null) {
         this.frustrationTriggers = new ArrayList<>();
      }

      if (this.thingsTheyLike == null) {
         this.thingsTheyLike = new ArrayList<>();
      }

      if (this.thingsTheyDislike == null) {
         this.thingsTheyDislike = new ArrayList<>();
      }

      if (this.milestones == null) {
         this.milestones = new ArrayList<>();
      }

      if (this.trustedPlayers == null) {
         this.trustedPlayers = new ArrayList<>();
      }

      if (this.rivalPlayers == null) {
         this.rivalPlayers = new ArrayList<>();
      }

      if (this.wordFrequency == null) {
         this.wordFrequency = new HashMap<>();
      }

      if (this.blocksPlacedByType == null) {
         this.blocksPlacedByType = new HashMap<>();
      }

      if (this.blocksBrokenByType == null) {
         this.blocksBrokenByType = new HashMap<>();
      }

      if (this.itemsCrafted == null) {
         this.itemsCrafted = new HashMap<>();
      }

      if (this.itemsUsed == null) {
         this.itemsUsed = new HashMap<>();
      }

      if (this.nearbyPlayersTime == null) {
         this.nearbyPlayersTime = new HashMap<>();
      }

      if (this.mentionedPlayers == null) {
         this.mentionedPlayers = new HashMap<>();
      }

      if (this.commandUsage == null) {
         this.commandUsage = new HashMap<>();
      }

      if (this.shopItemsBought == null) {
         this.shopItemsBought = new HashMap<>();
      }

      if (this.shopItemsSold == null) {
         this.shopItemsSold = new HashMap<>();
      }

      if (this.playTimeByHour == null) {
         this.playTimeByHour = new long[24];
      }

      if (this.playTimeByDay == null) {
         this.playTimeByDay = new long[7];
      }

      if (this.actionLog == null) {
         this.actionLog = new ArrayList<>();
      }

      if (this.suspiciousEvents == null) {
         this.suspiciousEvents = new ArrayList<>();
      }

      if (this.adminFlags == null) {
         this.adminFlags = new ArrayList<>();
      }

      if (this.suspiciousPatterns == null) {
         this.suspiciousPatterns = new HashMap<>();
      }

      if (this.locationHistory == null) {
         this.locationHistory = new ArrayList<>();
      }

      if (this.containerAccesses == null) {
         this.containerAccesses = new HashMap<>();
      }

      if (this.chatHistory == null) {
         this.chatHistory = new ArrayList<>();
      }

      if (this.connectionHistory == null) {
         this.connectionHistory = new ArrayList<>();
      }

      if (this.adminNotes == null) {
         this.adminNotes = "";
      }

      if (this.realName == null) {
         this.realName = "";
      }

      if (this.age == null) {
         this.age = "";
      }

      if (this.location == null) {
         this.location = "";
      }

      if (this.job == null) {
         this.job = "";
      }

      if (this.education == null) {
         this.education = "";
      }

      if (this.adminFlags == null) {
         this.adminFlags = new ArrayList<>();
      }

      if (this.suspiciousPatterns == null) {
         this.suspiciousPatterns = new HashMap<>();
      }

      if (this.mbtiType == null) {
         this.mbtiType = "";
      }

      if (this.enneagramType == null) {
         this.enneagramType = "";
      }

      if (this.primaryMotivation == null) {
         this.primaryMotivation = "";
      }

      if (this.attachmentStyle == null) {
         this.attachmentStyle = "unknown";
      }

      if (this.selfEsteemIndicator == null) {
         this.selfEsteemIndicator = "unknown";
      }

      if (this.communicationStyle == null) {
         this.communicationStyle = "unknown";
      }

      if (this.conflictStyle == null) {
         this.conflictStyle = "unknown";
      }

      if (this.emotionalExpression == null) {
         this.emotionalExpression = "unknown";
      }

      if (this.decisionMakingStyle == null) {
         this.decisionMakingStyle = "unknown";
      }

      if (this.stressResponse == null) {
         this.stressResponse = "unknown";
      }

      if (this.energySource == null) {
         this.energySource = "unknown";
      }

      if (this.howTheySeeAI == null) {
         this.howTheySeeAI = "";
      }

      if (this.coreValues == null) {
         this.coreValues = new ArrayList<>();
      }

      if (this.fears == null) {
         this.fears = new ArrayList<>();
      }

      if (this.strengths == null) {
         this.strengths = new ArrayList<>();
      }

      if (this.weaknesses == null) {
         this.weaknesses = new ArrayList<>();
      }

      if (this.technicalInterests == null) {
         this.technicalInterests = new ArrayList<>();
      }

      if (this.creativeInterests == null) {
         this.creativeInterests = new ArrayList<>();
      }

      if (this.socialInterests == null) {
         this.socialInterests = new ArrayList<>();
      }

      if (this.hobbies == null) {
         this.hobbies = new ArrayList<>();
      }

      if (this.sharedLifeEvents == null) {
         this.sharedLifeEvents = new ArrayList<>();
      }

      if (this.familyMentioned == null) {
         this.familyMentioned = new ArrayList<>();
      }

      if (this.dreamsAndGoals == null) {
         this.dreamsAndGoals = new ArrayList<>();
      }

      if (this.regrets == null) {
         this.regrets = new ArrayList<>();
      }

      if (this.topicsNeverDiscussed == null) {
         this.topicsNeverDiscussed = new ArrayList<>();
      }
   }

   public String getPersonalityContext() {
      this.ensureInit();
      if (this.totalInteractions < 1 && this.sessionsPlayed < 2) {
         return null;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("=== PLAYER PROFILE: ").append(this.displayName.isEmpty() ? "Unknown" : this.displayName).append(" ===\n");
         long daysSinceFirstJoin = (System.currentTimeMillis() - this.firstJoin) / 86400000L;
         sb.append("You have known this player for ").append(daysSinceFirstJoin).append(" days. ");
         sb.append("They have played ").append(this.sessionsPlayed).append(" sessions (total: ").append(this.fmtTime(this.totalPlayTimeMs)).append("). ");
         if (this.avgSessionLengthMs > 0L) {
            sb.append("Avg session: ").append(this.fmtTime(this.avgSessionLengthMs)).append(". ");
         }

         sb.append("\n");
         if (!"unknown".equals(this.playStyle)) {
            sb.append("Play style: ").append(this.playStyle).append(". ");
         }

         if (!"unknown".equals(this.buildStyle)) {
            sb.append("Build style: ").append(this.buildStyle).append(". ");
         }

         if (!"unknown".equals(this.riskTolerance)) {
            sb.append("Risk tolerance: ").append(this.riskTolerance).append(". ");
         }

         if (!"unknown".equals(this.socialStyle)) {
            sb.append("Social: ").append(this.socialStyle).append(". ");
         }

         sb.append("\n");
         sb.append("Stats: ")
            .append(this.totalDeaths)
            .append(" deaths, ")
            .append(this.totalPlayerKills)
            .append(" PvP kills, ")
            .append(this.totalMobKills)
            .append(" mob kills. ");
         if (this.totalBlocksPlaced > 0L || this.totalBlocksBroken > 0L) {
            sb.append("Blocks placed: ").append(this.totalBlocksPlaced).append(", broken: ").append(this.totalBlocksBroken).append(". ");
         }

         if (!this.favoriteBlock.isEmpty()) {
            sb.append("Fav block: ").append(this.favoriteBlock).append(". ");
         }

         if (!this.favoriteItem.isEmpty()) {
            sb.append("Fav item: ").append(this.favoriteItem).append(". ");
         }

         if (!this.favoriteWorld.isEmpty()) {
            sb.append("Fav world: ").append(this.favoriteWorld).append(". ");
         }

         sb.append("\n");
         sb.append("Chat: ").append(this.humorStyle).append(", ").append(this.formality).append(", tone: ").append(this.emotionalTone).append(". ");
         if (this.avgMessageLength > 0) {
            sb.append("Avg msg: ").append(this.avgMessageLength).append(" chars. ");
         }

         if (this.emojiUsage > 0) {
            sb.append("Uses emojis. ");
         }

         sb.append("\n");
         if (!this.interests.isEmpty()) {
            sb.append("Interests: ").append(String.join(", ", this.interests)).append(". ");
         }

         if (!this.thingsTheyLike.isEmpty()) {
            sb.append("Likes: ").append(String.join(", ", this.thingsTheyLike)).append(". ");
         }

         if (!this.thingsTheyDislike.isEmpty()) {
            sb.append("Dislikes: ").append(String.join(", ", this.thingsTheyDislike)).append(". ");
         }

         sb.append("\n");
         if (!this.trustedPlayers.isEmpty()) {
            sb.append("Friends: ").append(String.join(", ", this.trustedPlayers)).append(". ");
         }

         if (!this.rivalPlayers.isEmpty()) {
            sb.append("Rivals: ").append(String.join(", ", this.rivalPlayers)).append(". ");
         }

         if (!this.clanOrGuild.isEmpty()) {
            sb.append("Clan: ").append(this.clanOrGuild).append(". ");
         }

         if (!"unknown".equals(this.socialRole)) {
            sb.append("Role: ").append(this.socialRole).append(". ");
         }

         sb.append("\n");
         if (this.totalMoneyEarned > 0.0 || this.totalMoneySpent > 0.0) {
            sb.append("Money: earned ")
               .append(String.format("%.0f", this.totalMoneyEarned))
               .append(", spent ")
               .append(String.format("%.0f", this.totalMoneySpent))
               .append(". ");
            if (!"unknown".equals(this.tradingStyle)) {
               sb.append("Trader: ").append(this.tradingStyle).append(". ");
            }

            sb.append("\n");
         }

         if (!this.aiRelationship.equals("neutral")) {
            sb.append("Relationship: ").append(this.aiRelationship).append(". ");
         }

         if (!this.personalFactsShared.isEmpty()) {
            int n = Math.min(2, this.personalFactsShared.size());
            sb.append("Facts: ");

            for (int i = 0; i < n; i++) {
               sb.append("\"").append(this.personalFactsShared.get(i)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.topicsAskedAI.isEmpty()) {
            int n = Math.min(3, this.topicsAskedAI.size());
            sb.append("Topics: ").append(String.join(", ", this.topicsAskedAI.subList(0, n))).append(". ");
            sb.append("\n");
         }

         if ("short".equals(this.preferredLength)) {
            sb.append("Prefers SHORT answers.\n");
         } else if ("long".equals(this.preferredLength)) {
            sb.append("Prefers DETAILED answers.\n");
         } else {
            sb.append("Prefers MEDIUM answers.\n");
         }

         if (!this.currentGoal.isEmpty()) {
            sb.append("Goal: ").append(this.currentGoal).append(".\n");
         }

         if (!this.lastDeathCause.isEmpty()) {
            sb.append("Last death: ").append(this.lastDeathCause).append(".\n");
         }

         if (!this.milestones.isEmpty()) {
            sb.append("Milestones: ").append(String.join(", ", this.milestones.subList(0, Math.min(3, this.milestones.size())))).append(".\n");
         }

         sb.append("=== END PROFILE ===");
         return sb.toString();
      }
   }

   private String fmtTime(long ms) {
      long h = ms / 3600000L;
      long m = ms / 60000L % 60L;
      if (h > 24L) {
         long d = h / 24L;
         h %= 24L;
         return d + "d " + h;
      } else {
         return h + "h " + m;
      }
   }

   public void learnFromMessage(String message) {
      this.ensureInit();
      this.totalInteractions++;
      this.lastInteraction = System.currentTimeMillis();
      String lower = message.toLowerCase();
      this.extractTopics(lower);
      this.detectStyle(lower);
      this.updateWordFrequency(message);
      this.detectEmotionalTone(lower);
      this.detectPersonalFacts(message);
      this.detectDeepPersonalFacts(message);
      this.detectPreferences(message);
      this.detectPsychologicalPatterns(lower, message);
      this.detectCommunicationStyle(lower);
      this.detectStressResponse(lower);
      this.detectEnergySource(lower);
      this.detectCompetitiveness(lower);
      this.detectPerfectionism(lower);
      this.detectEmpathy(lower);
      this.detectDreamsAndGoals(message);
      this.detectRegrets(message);
      this.detectFamilyMentions(message);
      this.detectLifeEvents(message);
      this.updateTrustAndVulnerability(message);
      this.totalChatMessages++;
      this.totalCharsTyped = this.totalCharsTyped + (long)message.length();
      this.avgMessageLength = (int)(this.totalCharsTyped / (long)Math.max(1, this.totalChatMessages));
      if (message.contains("?")
         || message.contains("wie")
         || message.contains("was")
         || message.contains("wer")
         || message.contains("wo")
         || message.contains("warum")) {
         this.questionCount++;
      }

      int emojis = this.countEmojis(message);
      this.emojiUsage += emojis;
   }

   public void recordAIConversation(String topic) {
      this.ensureInit();
      this.totalAIConversations++;
      if (topic != null && !topic.isBlank() && !this.topicsAskedAI.contains(topic)) {
         this.topicsAskedAI.add(topic);
         if (this.topicsAskedAI.size() > 1000) {
            this.topicsAskedAI.remove(0);
         }
      }
   }

   public void recordPersonalFact(String fact) {
      this.ensureInit();
      if (fact != null && !fact.isBlank()) {
         if (!this.personalFactsShared.contains(fact)) {
            this.personalFactsShared.add(fact);
            if (this.personalFactsShared.size() > 1000) {
               this.personalFactsShared.remove(0);
            }
         }
      }
   }

   public void recordFrustration(String trigger) {
      this.ensureInit();
      if (trigger != null && !trigger.isBlank()) {
         if (!this.frustrationTriggers.contains(trigger)) {
            this.frustrationTriggers.add(trigger);
            if (this.frustrationTriggers.size() > 1000) {
               this.frustrationTriggers.remove(0);
            }
         }
      }
   }

   public void recordLike(String thing) {
      this.ensureInit();
      if (thing != null && !thing.isBlank()) {
         if (!this.thingsTheyLike.contains(thing)) {
            this.thingsTheyLike.add(thing);
            if (this.thingsTheyLike.size() > 1000) {
               this.thingsTheyLike.remove(0);
            }
         }
      }
   }

   public void recordDislike(String thing) {
      this.ensureInit();
      if (thing != null && !thing.isBlank()) {
         if (!this.thingsTheyDislike.contains(thing)) {
            this.thingsTheyDislike.add(thing);
            if (this.thingsTheyDislike.size() > 1000) {
               this.thingsTheyDislike.remove(0);
            }
         }
      }
   }

   public void recordMilestone(String milestone) {
      this.ensureInit();
      if (milestone != null && !milestone.isBlank()) {
         if (!this.milestones.contains(milestone)) {
            this.milestones.add(milestone);
         }
      }
   }

   public void onJoin() {
      this.ensureInit();
      this.sessionsPlayed++;
      this.currentSessionStart = System.currentTimeMillis();
      this.lastSeen = this.currentSessionStart;
   }

   public void onQuit() {
      this.ensureInit();
      if (this.currentSessionStart > 0L) {
         long sessionMs = System.currentTimeMillis() - this.currentSessionStart;
         this.totalPlayTimeMs += sessionMs;
         if (sessionMs > this.longestSessionMs) {
            this.longestSessionMs = sessionMs;
         }

         this.avgSessionLengthMs = this.totalPlayTimeMs / (long)Math.max(1, this.sessionsPlayed);
         this.currentSessionStart = 0L;
      }

      this.lastLogout = System.currentTimeMillis();
   }

   public void onDeath(String cause) {
      this.ensureInit();
      this.totalDeaths++;
      if (cause != null) {
         this.lastDeathCause = cause;
      }
   }

   public void onPlayerKill(String victimName) {
      this.ensureInit();
      this.totalPlayerKills++;
      this.pvpWins++;
   }

   public void onMobKill(String mobType) {
      this.ensureInit();
      this.totalMobKills++;
      if (mobType != null) {
         if (mobType.equals(this.mostKilledMob)) {
            this.mostKilledMobCount++;
         } else if (this.mostKilledMobCount == 0) {
            this.mostKilledMob = mobType;
            this.mostKilledMobCount = 1;
         }
      }
   }

   public void onBlockPlaced(String blockType) {
      this.ensureInit();
      this.totalBlocksPlaced++;
      this.blocksPlacedByType.merge(blockType, 1L, Long::sum);
      this.updateFavoriteBlock();
      this.detectPlayStyle();
   }

   public void onBlockBroken(String blockType) {
      this.ensureInit();
      this.totalBlocksBroken++;
      this.blocksBrokenByType.merge(blockType, 1L, Long::sum);
      this.detectPlayStyle();
   }

   public void onItemCrafted(String itemType) {
      this.ensureInit();
      this.totalItemsCrafted++;
      this.itemsCrafted.merge(itemType, 1L, Long::sum);
   }

   public void onItemUsed(String itemType) {
      this.ensureInit();
      this.itemsUsed.merge(itemType, 1L, Long::sum);
      this.updateFavoriteItem();
   }

   public void onWorldChange(String worldName) {
      this.ensureInit();
      if (worldName != null) {
         String w = worldName.toLowerCase();
         if (w.contains("nether")) {
            this.netherVisits++;
         }

         if (w.contains("end") || w.contains("the_end")) {
            this.endVisits++;
         }

         if (this.favoriteWorld.isEmpty()) {
            this.favoriteWorld = worldName;
         }

         this.detectRiskTolerance();
      }
   }

   public void onDistanceTraveled(long blocks, boolean flying) {
      this.ensureInit();
      if (flying) {
         this.distanceFlown += blocks;
      } else {
         this.distanceTraveled += blocks;
      }
   }

   public void onCommand(String cmd) {
      this.ensureInit();
      this.commandUsage.merge(cmd, 1, Integer::sum);
      this.updateMostUsedCommand();
   }

   public void onTeleportRequest() {
      this.ensureInit();
      this.teleportRequestsSent++;
   }

   public void onTeleportAccept() {
      this.ensureInit();
      this.teleportRequestsAccepted++;
   }

   public void onMoneyEarned(double amount) {
      this.ensureInit();
      this.totalMoneyEarned += amount;
      this.detectTradingStyle();
   }

   public void onMoneySpent(double amount) {
      this.ensureInit();
      this.totalMoneySpent += amount;
      this.detectTradingStyle();
   }

   public void onShopBuy(String item) {
      this.ensureInit();
      this.shopVisits++;
      this.shopItemsBought.merge(item, 1, Integer::sum);
   }

   public void onShopSell(String item) {
      this.ensureInit();
      this.shopVisits++;
      this.shopItemsSold.merge(item, 1, Integer::sum);
   }

   public void onNearbyPlayer(String playerName, int seconds) {
      this.ensureInit();
      this.nearbyPlayersTime.merge(playerName, seconds, Integer::sum);
      this.updateSocialStyle();
   }

   public void onPlayerMention(String playerName) {
      this.ensureInit();
      this.mentionedPlayers.merge(playerName, 1, Integer::sum);
   }

   public void setPeakBalance(double balance) {
      this.ensureInit();
      if (balance > this.peakBalance) {
         this.peakBalance = balance;
      }
   }

   public void setCurrentGoal(String goal) {
      this.ensureInit();
      if (goal != null && !goal.isBlank()) {
         this.currentGoal = goal;
      }
   }

   public void setBuildStyle(String style) {
      this.ensureInit();
      if (style != null && !style.isBlank()) {
         this.buildStyle = style;
      }
   }

   public void setClanOrGuild(String name) {
      this.ensureInit();
      if (name != null && !name.isBlank()) {
         this.clanOrGuild = name;
      }
   }

   public void setSocialRole(String role) {
      this.ensureInit();
      if (role != null && !role.isBlank()) {
         this.socialRole = role;
      }
   }

   public void setAiRelationship(String rel) {
      this.ensureInit();
      if (rel != null && !rel.isBlank()) {
         this.aiRelationship = rel;
      }
   }

   private void updateFavoriteBlock() {
      long max = 0L;

      for (Entry<String, Long> e : this.blocksPlacedByType.entrySet()) {
         if (e.getValue() > max) {
            max = e.getValue();
            this.favoriteBlock = e.getKey();
         }
      }
   }

   private void updateFavoriteItem() {
      long max = 0L;

      for (Entry<String, Long> e : this.itemsUsed.entrySet()) {
         if (e.getValue() > max) {
            max = e.getValue();
            this.favoriteItem = e.getKey();
         }
      }
   }

   private void updateMostUsedCommand() {
      int max = 0;

      for (Entry<String, Integer> e : this.commandUsage.entrySet()) {
         if (e.getValue() > max) {
            max = e.getValue();
            this.mostUsedCommand = e.getKey();
         }
      }
   }

   private void updateSocialStyle() {
      if (this.nearbyPlayersTime.isEmpty()) {
         this.socialStyle = "solo";
      } else if (this.nearbyPlayersTime.size() <= 3) {
         this.socialStyle = "small_group";
      } else {
         this.socialStyle = "large_group";
      }
   }

   private void detectPlayStyle() {
      if (this.totalBlocksPlaced > this.totalBlocksBroken * 2L && this.totalBlocksPlaced > 100L) {
         this.playStyle = "builder";
      } else if (this.totalPlayerKills > 5 && this.totalBlocksPlaced < this.totalBlocksBroken) {
         this.playStyle = "pvper";
      } else if (this.netherVisits > 2 || this.endVisits > 0) {
         this.playStyle = "explorer";
      } else if (this.totalItemsCrafted > 50L) {
         this.playStyle = "farmer";
      } else if (this.totalBlocksPlaced <= 50L || !this.blocksPlacedByType.containsKey("REDSTONE") && !this.blocksPlacedByType.containsKey("REDSTONE_TORCH")) {
         if (!this.nearbyPlayersTime.isEmpty() && this.nearbyPlayersTime.size() > 2) {
            this.playStyle = "socializer";
         }
      } else {
         this.playStyle = "redstoner";
      }
   }

   private void detectRiskTolerance() {
      if (this.endVisits > 0 && this.netherVisits > 5) {
         this.riskTolerance = "reckless";
      } else if (this.netherVisits > 2) {
         this.riskTolerance = "moderate";
      } else if (this.sessionsPlayed > 2) {
         this.riskTolerance = "cautious";
      }
   }

   private void detectTradingStyle() {
      if (this.totalMoneyEarned > this.totalMoneySpent * 3.0) {
         this.tradingStyle = "hoarder";
      } else if (this.totalMoneySpent > this.totalMoneyEarned * 2.0) {
         this.tradingStyle = "spender";
      } else if (this.shopVisits > 10) {
         this.tradingStyle = "trader";
      } else if (this.totalMoneyEarned > 1000.0 || this.totalMoneySpent > 1000.0) {
         this.tradingStyle = "investor";
      } else if (this.totalMoneyEarned > 0.0 || this.totalMoneySpent > 0.0) {
         this.tradingStyle = "casual";
      }
   }

   private void detectEmotionalTone(String lower) {
      if (lower.contains("lol") || lower.contains("haha") || lower.contains("xd") || lower.contains(":)")) {
         this.emotionalTone = "happy";
      } else if (lower.contains("fuck") || lower.contains("scheiße") || lower.contains("hass") || lower.contains("verdammt")) {
         this.emotionalTone = "angry";
      } else if (lower.contains("?!") || lower.contains("omg") || lower.contains("wow") || lower.contains("krass")) {
         this.emotionalTone = "excited";
      } else if (lower.contains("traurig") || lower.contains("sad") || lower.contains(":(")) {
         this.emotionalTone = "sad";
      }
   }

   private void extractTopics(String lower) {
      if (lower.contains("minecraft") || lower.contains("mc") || lower.contains("server")) {
         this.addInterest("Minecraft");
      }

      if (lower.contains("redstone") || lower.contains("technic")) {
         this.addInterest("Redstone");
      }

      if (lower.contains("build") || lower.contains("bauen") || lower.contains("haus")) {
         this.addInterest("Building");
      }

      if (lower.contains("pvp") || lower.contains("fight") || lower.contains("duell")) {
         this.addInterest("PvP");
      }

      if (lower.contains("farm") || lower.contains("crop") || lower.contains("anbau")) {
         this.addInterest("Farming");
      }

      if (lower.contains("yt") || lower.contains("youtube") || lower.contains("stream")) {
         this.addInterest("YouTube/Streaming");
      }

      if (lower.contains("plugin") || lower.contains("mod")) {
         this.addInterest("Plugins/Mods");
      }

      if (lower.contains("hack") || lower.contains("cheat") || lower.contains("x-ray")) {
         this.addInterest("Game mechanics");
      }

      if (lower.contains("music") || lower.contains("song") || lower.contains("musik")) {
         this.addInterest("Music");
      }

      if (lower.contains("movie") || lower.contains("film") || lower.contains("serie")) {
         this.addInterest("Movies/Series");
      }
   }

   private void addInterest(String topic) {
      if (!this.interests.contains(topic)) {
         this.interests.add(topic);
      }

      if (this.interests.size() > 1000) {
         this.interests.remove(0);
      }
   }

   private void detectStyle(String lower) {
      if (lower.contains("lol") || lower.contains("lmao") || lower.contains("xd") || lower.contains("haha")) {
         this.humorStyle = "friendly/playful";
      } else if (lower.contains("digga") || lower.contains("bruder") || lower.contains("alter") || lower.contains("bro") || lower.contains("man")) {
         this.humorStyle = "casual/street";
         this.formality = "slang";
      } else if (lower.contains("wtf") || lower.contains("fuck") || lower.contains("damn")) {
         this.humorStyle = "edgy";
      }

      if (lower.contains("kurz") || lower.contains("short") || lower.contains("knackig")) {
         this.preferredLength = "short";
      } else if (lower.contains("lang") || lower.contains("detailed") || lower.contains("ausführlich")) {
         this.preferredLength = "long";
      }
   }

   private void updateWordFrequency(String message) {
      String[] split = message.toLowerCase().split("\\s+");

      for (String w : split) {
         w = w.replaceAll("[^a-zäöüß0-9]", "");
         if (w.length() >= 3 && w.length() <= 20) {
            this.wordFrequency.merge(w, 1, Integer::sum);
            if (this.wordFrequency.get(w) >= 3 && !this.catchphrases.contains(w)) {
               this.catchphrases.add(w);
               if (this.catchphrases.size() > 1000) {
                  this.catchphrases.remove(0);
               }
            }
         }
      }
   }

   private void detectPersonalFacts(String message) {
      String lower = message.toLowerCase();
      String[] array = new String[]{
         "ich bin ",
         "i am ",
         "mein name ist ",
         "my name is ",
         "ich wohne ",
         "i live ",
         "ich habe ",
         "i have ",
         "ich mag ",
         "i like ",
         "ich hasse ",
         "i hate ",
         "ich spiele ",
         "i play "
      };

      for (String p : array) {
         if (lower.contains(p)) {
            int idx = lower.indexOf(p) + p.length();
            int end = message.indexOf(46, idx);
            if (end < 0) {
               end = message.indexOf(33, idx);
            }

            if (end < 0) {
               end = message.indexOf(63, idx);
            }

            if (end < 0) {
               end = Math.min(idx + 40, message.length());
            }

            String fact = message.substring(idx, end).trim();
            if (fact.length() > 3 && fact.length() < 50) {
               this.recordPersonalFact(fact);
            }
         }
      }
   }

   private void detectPreferences(String message) {
      String lower = message.toLowerCase();
      if (lower.contains("mag") || lower.contains("like") || lower.contains("liebe") || lower.contains("love")) {
         this.extractPreference(message, true);
      }

      if (lower.contains("hasse") || lower.contains("hate") || lower.contains("doof") || lower.contains("suck") || lower.contains("bad")) {
         this.extractPreference(message, false);
      }
   }

   private void extractPreference(String message, boolean like) {
      String[] split = message.split("[ ,.!?]");

      for (int i = 0; i < split.length - 1; i++) {
         String w = split[i].toLowerCase();
         if (w.contains("mag") || w.contains("like") || w.contains("liebe") || w.contains("love") || w.contains("hasse") || w.contains("hate")) {
            String obj = split[i + 1].trim();
            if (obj.length() > 2 && obj.length() < 20) {
               if (like) {
                  this.recordLike(obj);
               } else {
                  this.recordDislike(obj);
               }
            }
         }
      }
   }

   private int countEmojis(String message) {
      int count = 0;

      for (int cp : message.codePoints().toArray()) {
         if (cp >= 128512 && cp <= 128591
            || cp >= 127744 && cp <= 128511
            || cp >= 128640 && cp <= 128767
            || cp >= 9728 && cp <= 9983
            || cp >= 9984 && cp <= 10175
            || cp >= 129280 && cp <= 129535
            || cp >= 127462 && cp <= 127487) {
            count++;
         }
      }

      return count;
   }

   public void logAction(String type, String details, String world, int x, int y, int z) {
      this.ensureInit();
      this.actionLog.add(new PlayerAction(type, details, world, x, y, z));
      if (this.actionLog.size() > 10000) {
         this.actionLog.remove(0);
      }
   }

   public void logSuspicious(String type, String description, int severity) {
      this.ensureInit();
      this.suspiciousEvents.add(new SuspiciousEvent(type, description, severity));
      if (this.suspiciousEvents.size() > 10000) {
         this.suspiciousEvents.remove(0);
      }

      this.suspicionScore += severity;
      if (this.suspicionScore >= 30) {
         this.flaggedAsSuspicious = true;
      }

      this.suspiciousPatterns.merge(type, 1, Integer::sum);
   }

   public void addAdminNote(String note) {
      this.ensureInit();
      if (note != null && !note.isBlank()) {
         if (!this.adminNotes.isEmpty()) {
            this.adminNotes = this.adminNotes + " | ";
         }

         this.adminNotes = this.adminNotes + "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + "] " + note;
      }
   }

   public void addAdminFlag(String flag) {
      this.ensureInit();
      if (flag != null && !flag.isBlank()) {
         if (!this.adminFlags.contains(flag)) {
            this.adminFlags.add(flag);
         }

         if ("hacker".equalsIgnoreCase(flag) || "cheater".equalsIgnoreCase(flag) || "xray".equalsIgnoreCase(flag)) {
            this.flaggedAsSuspicious = true;
         }
      }
   }

   public void removeAdminFlag(String flag) {
      this.ensureInit();
      this.adminFlags.remove(flag);
      if (this.adminFlags.isEmpty()) {
         this.flaggedAsSuspicious = false;
      }
   }

   public void snapshotLocation(String world, int x, int y, int z) {
      this.ensureInit();
      this.locationHistory.add(new LocationSnapshot(world, x, y, z));
      if (this.locationHistory.size() > 10000) {
         this.locationHistory.remove(0);
      }
   }

   public void logChat(String message, String channel) {
      this.ensureInit();
      this.chatHistory.add(new ChatEntry(message, channel));
      if (this.chatHistory.size() > 10000) {
         this.chatHistory.remove(0);
      }
   }

   public void logConnection(String type, long durationMs) {
      this.ensureInit();
      this.connectionHistory.add(new ConnectionEvent(type, durationMs));
      if (this.connectionHistory.size() > 10000) {
         this.connectionHistory.remove(0);
      }
   }

   public void logContainerAccess(String containerType) {
      this.ensureInit();
      this.chestsOpenedTotal++;
      this.containerAccesses.merge(containerType, 1, Integer::sum);
   }

   public String getAdminReport() {
      return this.buildAdminReport();
   }

   private String buildAdminReport() {
      this.ensureInit();
      StringBuilder sb = new StringBuilder();
      sb.append("§e§l========================================\n");
      sb.append("§e§l   ADMIN REPORT: ").append(this.displayName.isEmpty() ? "Unknown" : this.displayName).append("\n");
      sb.append("§e§l========================================\n\n");
      sb.append("§c§l[MODERATION & SUSPICION]\n");
      sb.append("§cSuspicion Score: §f").append(this.suspicionScore).append(" ");
      sb.append(this.flaggedAsSuspicious ? "§c[FLAGGED]" : "§a[CLEAN]").append("\n");
      sb.append("§cScam Flags: §f")
         .append(this.scamFlags)
         .append(" | Raid Flags: ")
         .append(this.raidFlags)
         .append(" | AI Blocks: ")
         .append(this.aiBlocks)
         .append("\n");
      sb.append("§cRisk Score: §f").append(this.getRiskScore()).append("\n");
      if (!this.adminFlags.isEmpty()) {
         sb.append("§cAdmin Flags: §f").append(String.join(", ", this.adminFlags)).append("\n");
      }

      int totalCheats = this.speedFlags
         + this.flyFlags
         + this.xrayFlags
         + this.nukerFlags
         + this.killauraFlags
         + this.autoclickFlags
         + this.containerSpamFlags
         + this.noFallFlags
         + this.reachFlags;
      if (totalCheats > 0) {
         sb.append("§4§l[CHEATING FLAGS]\n");
         sb.append("  §cSpeed: §f").append(this.speedFlags).append(" | ");
         sb.append("§cFly: §f").append(this.flyFlags).append(" | ");
         sb.append("§cXRay: §f").append(this.xrayFlags).append(" | ");
         sb.append("§cNuker: §f").append(this.nukerFlags).append("\n");
         sb.append("  §cKillAura: §f").append(this.killauraFlags).append(" | ");
         sb.append("§cAutoClick: §f").append(this.autoclickFlags).append(" | ");
         sb.append("§cContainerSpam: §f").append(this.containerSpamFlags).append(" | ");
         sb.append("§cNoFall: §f").append(this.noFallFlags).append(" | ");
         sb.append("§cReach: §f").append(this.reachFlags).append("\n");
      }

      SuspiciousEvent e = null;
      if (!this.suspiciousEvents.isEmpty()) {
         sb.append("§cSuspicious Events (last 100):\n");

         for (int i = Math.max(0, this.suspiciousEvents.size() - 100); i < this.suspiciousEvents.size(); i++) {
            e = this.suspiciousEvents.get(i);
            sb.append("  §c[")
               .append(e.severity)
               .append("] §7")
               .append(this.fmtDate(e.timestamp))
               .append(" §f")
               .append(e.type)
               .append(": ")
               .append(e.description)
               .append("\n");
         }
      }

      if (!this.suspiciousPatterns.isEmpty()) {
         sb.append("§cPattern counts: §f");
         this.suspiciousPatterns.forEach((kk, v) -> sb.append(kk).append("=").append(v).append(" "));
         sb.append("\n");
      }

      if (!this.adminNotes.isEmpty()) {
         sb.append("§cAdmin Notes:\n");

         for (String note : this.adminNotes.split("\\|")) {
            sb.append("  §7- ").append(note.trim()).append("\n");
         }
      }

      sb.append("\n");
      if (!this.realName.isEmpty() || !this.age.isEmpty() || !this.location.isEmpty()) {
         sb.append("§b§l[IDENTITY FROM CHAT]\n");
         if (!this.realName.isEmpty()) {
            sb.append("§bReal Name: §f").append(this.realName).append("\n");
         }

         if (!this.age.isEmpty()) {
            sb.append("§bAge: §f").append(this.age).append("\n");
         }

         if (!this.location.isEmpty()) {
            sb.append("§bLocation: §f").append(this.location).append("\n");
         }

         if (!this.job.isEmpty()) {
            sb.append("§bJob: §f").append(this.job).append("\n");
         }

         if (!this.education.isEmpty()) {
            sb.append("§bEducation: §f").append(this.education).append("\n");
         }

         sb.append("\n");
      }

      sb.append("§a§l[PLAYTIME & SESSIONS]\n");
      sb.append("§aSessions: §f").append(this.sessionsPlayed).append("\n");
      sb.append("§aTotal Playtime: §f").append(this.fmtTime(this.totalPlayTimeMs)).append("\n");
      sb.append("§aAvg Session: §f").append(this.fmtTime(this.avgSessionLengthMs)).append("\n");
      sb.append("§aLongest Session: §f").append(this.fmtTime(this.longestSessionMs)).append("\n");
      sb.append("§aFirst Join: §f").append(this.fmtDate(this.firstJoin)).append("\n");
      sb.append("§aLast Seen: §f").append(this.fmtDate(this.lastSeen)).append("\n");
      sb.append("§aAFK Time: §f").append(this.fmtTime(this.totalAfkTimeMs)).append("\n");
      sb.append("\n");
      sb.append("§4§l[COMBAT]\n");
      sb.append("§4Total Deaths: §f").append(this.totalDeaths).append("\n");
      sb.append("§4Player Kills: §f")
         .append(this.totalPlayerKills)
         .append(" | PvP Wins: ")
         .append(this.pvpWins)
         .append(" | Losses: ")
         .append(this.pvpLosses)
         .append("\n");
      sb.append("§4Mob Kills: §f").append(this.totalMobKills).append("\n");
      sb.append("§4Most Killed Mob: §f").append(this.mostKilledMob.isEmpty() ? "none" : this.mostKilledMob + " (x" + this.mostKilledMobCount).append("\n");
      sb.append("§4Last Death: §f").append(this.lastDeathCause.isEmpty() ? "none" : this.lastDeathCause).append("\n");
      sb.append("\n");
      sb.append("§2§l[BUILDING & MINING]\n");
      sb.append("§2Blocks Placed: §f").append(this.totalBlocksPlaced).append(" | Broken: ").append(this.totalBlocksBroken).append("\n");
      sb.append("§2Favorite Block: §f").append(this.favoriteBlock.isEmpty() ? "unknown" : this.favoriteBlock).append("\n");
      if (!this.blocksPlacedByType.isEmpty()) {
         sb.append("§2Placed by Type (top 20):\n");
         this.blocksPlacedByType
            .entrySet()
            .stream()
            .sorted((aa, bb) -> Long.compare(bb.getValue(), aa.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("\n"));
      }

      if (!this.blocksBrokenByType.isEmpty()) {
         sb.append("§2Broken by Type (top 20):\n");
         this.blocksBrokenByType
            .entrySet()
            .stream()
            .sorted((aa, bb) -> Long.compare(bb.getValue(), aa.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("\n"));
      }

      sb.append("\n");
      sb.append("§d§l[ITEMS & CRAFTING]\n");
      sb.append("§dItems Crafted: §f").append(this.totalItemsCrafted).append("\n");
      sb.append("§dFavorite Item: §f").append(this.favoriteItem.isEmpty() ? "unknown" : this.favoriteItem).append("\n");
      sb.append("§dFavorite Tool: §f").append(this.favoriteTool.isEmpty() ? "unknown" : this.favoriteTool).append("\n");
      if (!this.itemsCrafted.isEmpty()) {
         sb.append("§dCrafted (top 20):\n");
         this.itemsCrafted
            .entrySet()
            .stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("\n"));
      }

      sb.append("\n");
      sb.append("§3§l[MOVEMENT & EXPLORATION]\n");
      sb.append("§3Distance: §f").append(this.distanceTraveled).append(" blocks | Flown: ").append(this.distanceFlown).append(" blocks\n");
      sb.append("§3Nether Visits: §f").append(this.netherVisits).append(" | End: ").append(this.endVisits).append("\n");
      sb.append("§3Favorite World: §f").append(this.favoriteWorld.isEmpty() ? "unknown" : this.favoriteWorld).append("\n");
      sb.append("\n");
      sb.append("§e§l[CHAT BEHAVIOR]\n");
      sb.append("§eMessages: §f")
         .append(this.totalChatMessages)
         .append(" | AI Interactions: ")
         .append(this.totalInteractions)
         .append(" | AI Convos: ")
         .append(this.totalAIConversations)
         .append("\n");
      sb.append("§eQuestions: §f")
         .append(this.questionCount)
         .append(" | Avg Length: ")
         .append(this.avgMessageLength)
         .append(" chars | Emojis: ")
         .append(this.emojiUsage)
         .append("\n");
      sb.append("§eHumor: §f")
         .append(this.humorStyle)
         .append(" | Formality: ")
         .append(this.formality)
         .append(" | Tone: ")
         .append(this.emotionalTone)
         .append("\n");
      sb.append("§ePreferred Length: §f").append(this.preferredLength).append(" | Language: ").append(this.languagePreference).append("\n");
      if (!this.interests.isEmpty()) {
         sb.append("§eInterests: §f").append(String.join(", ", this.interests)).append("\n");
      }

      if (!this.catchphrases.isEmpty()) {
         sb.append("§eCatchphrases: §f").append(String.join(", ", this.catchphrases)).append("\n");
      }

      if (!this.wordFrequency.isEmpty()) {
         sb.append("§eTop Words (top 50):\n");
         this.wordFrequency
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(50L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append("=").append(ee.getValue()).append("\n"));
      }

      sb.append("\n");
      sb.append("§b§l[SOCIAL]\n");
      sb.append("§bStyle: §f").append(this.socialStyle).append(" | Role: ").append(this.socialRole).append("\n");
      sb.append("§bClan: §f").append(this.clanOrGuild.isEmpty() ? "none" : this.clanOrGuild).append("\n");
      if (!this.trustedPlayers.isEmpty()) {
         sb.append("§bTrusted: §f").append(String.join(", ", this.trustedPlayers)).append("\n");
      }

      if (!this.rivalPlayers.isEmpty()) {
         sb.append("§bRivals: §f").append(String.join(", ", this.rivalPlayers)).append("\n");
      }

      if (!this.nearbyPlayersTime.isEmpty()) {
         sb.append("§bTime Near Players (top 20):\n");
         this.nearbyPlayersTime
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("s\n"));
      }

      sb.append("§bTP Requests: Sent=").append(this.teleportRequestsSent).append(" Accepted=").append(this.teleportRequestsAccepted).append("\n");
      sb.append("\n");
      sb.append("§6§l[ECONOMY]\n");
      sb.append("§6Money Earned: §f")
         .append(String.format("%.2f", this.totalMoneyEarned))
         .append(" | Spent: ")
         .append(String.format("%.2f", this.totalMoneySpent))
         .append("\n");
      sb.append("§6Peak Balance: §f").append(String.format("%.2f", this.peakBalance)).append(" | Trading: ").append(this.tradingStyle).append("\n");
      sb.append("§6Shop Visits: §f").append(this.shopVisits).append("\n");
      if (!this.shopItemsBought.isEmpty()) {
         sb.append("§6Bought (top 20):\n");
         this.shopItemsBought
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("\n"));
      }

      if (!this.shopItemsSold.isEmpty()) {
         sb.append("§6Sold (top 20):\n");
         this.shopItemsSold
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(20L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("\n"));
      }

      sb.append("\n");
      sb.append("§8§l[COMMANDS]\n");
      sb.append("§8Most Used: §f").append(this.mostUsedCommand.isEmpty() ? "none" : this.mostUsedCommand).append("\n");
      if (!this.commandUsage.isEmpty()) {
         sb.append("§8Usage (top 50):\n");
         this.commandUsage
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(50L)
            .forEach(ee -> sb.append("  §7").append(ee.getKey()).append(" = ").append(ee.getValue()).append("x\n"));
      }

      sb.append("\n");
      sb.append("§a§l[PLAYSTYLE]\n");
      sb.append("§aPlay Style: §f")
         .append(this.playStyle)
         .append(" | Build: ")
         .append(this.buildStyle)
         .append(" | Risk: ")
         .append(this.riskTolerance)
         .append("\n");
      sb.append("§aGoal: §f").append(this.currentGoal.isEmpty() ? "none" : this.currentGoal).append("\n");
      sb.append("§aAchievements: §f").append(this.achievementsUnlocked).append(" | Gear Score: ").append(this.maxGearScore).append("\n");
      if (!this.milestones.isEmpty()) {
         sb.append("§aMilestones: §f").append(String.join(", ", this.milestones)).append("\n");
      }

      sb.append("\n");
      sb.append("§2§l[CONTAINERS]\n");
      sb.append("§2Chests Opened: §f").append(this.chestsOpenedTotal).append("\n");
      if (!this.containerAccesses.isEmpty()) {
         sb.append("§2Types:\n");
         this.containerAccesses.forEach((k, v) -> sb.append("  §7").append(k).append(" = ").append(v).append("\n"));
      }

      sb.append("\n");
      if (!this.locationHistory.isEmpty()) {
         sb.append("§3§l[LOCATION HISTORY (last 100)]\n");

         for (int i = Math.max(0, this.locationHistory.size() - 100); i < this.locationHistory.size(); i++) {
            LocationSnapshot l = this.locationHistory.get(i);
            sb.append("  §7")
               .append(this.fmtDate(l.timestamp))
               .append(" ")
               .append(l.world)
               .append(" (")
               .append(l.x)
               .append(",")
               .append(l.y)
               .append(",")
               .append(l.z)
               .append(")\n");
         }

         if (this.locationHistory.size() > 100) {
            sb.append("  §7... and ").append(this.locationHistory.size() - 100).append(" more entries\n");
         }

         sb.append("\n");
      }

      if (!this.actionLog.isEmpty()) {
         sb.append("§8§l[ACTION LOG (last 200)]\n");

         for (int i = Math.max(0, this.actionLog.size() - 200); i < this.actionLog.size(); i++) {
            PlayerAction pa = this.actionLog.get(i);
            sb.append("  §7")
               .append(this.fmtDate(pa.timestamp))
               .append(" [")
               .append(pa.type)
               .append("] ")
               .append(pa.details)
               .append(" | ")
               .append(pa.world)
               .append("(")
               .append(pa.x)
               .append(",")
               .append(pa.y)
               .append(",")
               .append(pa.z)
               .append(")\n");
         }

         if (this.actionLog.size() > 200) {
            sb.append("  §7... and ").append(this.actionLog.size() - 200).append(" more entries\n");
         }

         sb.append("\n");
      }

      if (!this.chatHistory.isEmpty()) {
         sb.append("§e§l[CHAT HISTORY (last 200)]\n");

         for (int i = Math.max(0, this.chatHistory.size() - 200); i < this.chatHistory.size(); i++) {
            ChatEntry c = this.chatHistory.get(i);
            sb.append("  §7").append(this.fmtDate(c.timestamp)).append(" [").append(c.channel).append("] ").append(c.message).append("\n");
         }

         if (this.chatHistory.size() > 200) {
            sb.append("  §7... and ").append(this.chatHistory.size() - 200).append(" more entries\n");
         }

         sb.append("\n");
      }

      if (!this.connectionHistory.isEmpty()) {
         sb.append("§a§l[CONNECTION HISTORY (last 100)]\n");

         for (int i = Math.max(0, this.connectionHistory.size() - 100); i < this.connectionHistory.size(); i++) {
            ConnectionEvent c2 = this.connectionHistory.get(i);
            sb.append("  §7").append(this.fmtDate(c2.timestamp)).append(" ").append(c2.type);
            if (c2.durationMs > 0L) {
               sb.append(" (").append(this.fmtTime(c2.durationMs)).append(")");
            }

            sb.append("\n");
         }

         if (this.connectionHistory.size() > 100) {
            sb.append("  §7... and ").append(this.connectionHistory.size() - 100).append(" more entries\n");
         }

         sb.append("\n");
      }

      sb.append("§5§l========================================\n");
      sb.append("§5§l   DEEP PSYCHOLOGICAL PROFILE\n");
      sb.append("§5§l========================================\n\n");
      if (!this.realName.isEmpty() || !this.age.isEmpty() || !this.location.isEmpty()) {
         sb.append("§5§l[IDENTITY]\n");
         if (!this.realName.isEmpty()) {
            sb.append("§5Name: §f").append(this.realName).append("\n");
         }

         if (!this.age.isEmpty()) {
            sb.append("§5Age: §f").append(this.age).append("\n");
         }

         if (!this.location.isEmpty()) {
            sb.append("§5Location: §f").append(this.location).append("\n");
         }

         if (!this.job.isEmpty()) {
            sb.append("§5Job: §f").append(this.job).append("\n");
         }

         if (!this.education.isEmpty()) {
            sb.append("§5Education: §f").append(this.education).append("\n");
         }

         sb.append("\n");
      }

      sb.append("§5§l[PERSONALITY FRAMEWORKS]\n");
      if (!this.mbtiType.isEmpty()) {
         sb.append("§5MBTI (guess): §f").append(this.mbtiType).append("\n");
      }

      if (!this.enneagramType.isEmpty()) {
         sb.append("§5Enneagram: §f").append(this.enneagramType).append("\n");
      }

      sb.append("§5Big Five: §fOpenness=")
         .append(this.bigFiveOpenness)
         .append(" Conscientiousness=")
         .append(this.bigFiveConscientiousness)
         .append(" Extraversion=")
         .append(this.bigFiveExtraversion)
         .append(" Agreeableness=")
         .append(this.bigFiveAgreeableness)
         .append(" Neuroticism=")
         .append(this.bigFiveNeuroticism)
         .append("\n\n");
      sb.append("§5§l[CORE PSYCHOLOGY]\n");
      if (!this.coreValues.isEmpty()) {
         sb.append("§5Core Values: §f").append(String.join(", ", this.coreValues)).append("\n");
      }

      if (!this.fears.isEmpty()) {
         sb.append("§5Fears: §f").append(String.join(", ", this.fears)).append("\n");
      }

      if (!this.strengths.isEmpty()) {
         sb.append("§5Strengths: §f").append(String.join(", ", this.strengths)).append("\n");
      }

      if (!this.weaknesses.isEmpty()) {
         sb.append("§5Weaknesses: §f").append(String.join(", ", this.weaknesses)).append("\n");
      }

      if (!this.primaryMotivation.isEmpty()) {
         sb.append("§5Motivation: §f").append(this.primaryMotivation).append("\n");
      }

      sb.append("§5Attachment: §f")
         .append(this.attachmentStyle)
         .append(" | Self-Esteem: ")
         .append(this.selfEsteemIndicator)
         .append(" | Perfectionism: ")
         .append(this.perfectionismLevel)
         .append("/10\n\n");
      sb.append("§5§l[COMMUNICATION & SOCIAL]\n");
      sb.append("§5Style: §f")
         .append(this.communicationStyle)
         .append(" | Conflict: ")
         .append(this.conflictStyle)
         .append(" | Stress: ")
         .append(this.stressResponse)
         .append(" | Energy: ")
         .append(this.energySource)
         .append("\n\n");
      sb.append("§5§l[BEHAVIORAL PATTERNS]\n");
      sb.append("§5Flags: §f");
      if (this.isPerfectionist) {
         sb.append("[Perfectionist] ");
      }

      if (this.isImpulsive) {
         sb.append("[Impulsive] ");
      }

      if (this.isCompetitive) {
         sb.append("[Competitive] ");
      }

      if (this.isEmpathetic) {
         sb.append("[Empathetic] ");
      }

      if (this.usesHumorAsDefense) {
         sb.append("[Humor-as-defense] ");
      }

      if (this.overthinks) {
         sb.append("[Overthinker] ");
      }

      if (this.seeksValidation) {
         sb.append("[Validation-seeking] ");
      }

      if (this.avoidsConflict) {
         sb.append("[Conflict-avoidant] ");
      }

      if (this.needsControl) {
         sb.append("[Needs-control] ");
      }

      sb.append("\n\n");
      if (!this.technicalInterests.isEmpty() || !this.creativeInterests.isEmpty() || !this.hobbies.isEmpty()) {
         sb.append("§5§l[INTERESTS]\n");
         if (!this.technicalInterests.isEmpty()) {
            sb.append("§5Technical: §f").append(String.join(", ", this.technicalInterests)).append("\n");
         }

         if (!this.creativeInterests.isEmpty()) {
            sb.append("§5Creative: §f").append(String.join(", ", this.creativeInterests)).append("\n");
         }

         if (!this.socialInterests.isEmpty()) {
            sb.append("§5Social: §f").append(String.join(", ", this.socialInterests)).append("\n");
         }

         if (!this.hobbies.isEmpty()) {
            sb.append("§5Hobbies: §f").append(String.join(", ", this.hobbies)).append("\n");
         }

         sb.append("\n");
      }

      if (!this.sharedLifeEvents.isEmpty() || !this.familyMentioned.isEmpty() || !this.dreamsAndGoals.isEmpty() || !this.regrets.isEmpty()) {
         sb.append("§5§l[LIFE CONTEXT]\n");
         if (!this.sharedLifeEvents.isEmpty()) {
            sb.append("§5Events: §f");

            for (int j = 0; j < Math.min(3, this.sharedLifeEvents.size()); j++) {
               sb.append("\"").append(this.sharedLifeEvents.get(j)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.familyMentioned.isEmpty()) {
            sb.append("§5Family: §f");

            for (int j = 0; j < Math.min(3, this.familyMentioned.size()); j++) {
               sb.append("\"").append(this.familyMentioned.get(j)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.dreamsAndGoals.isEmpty()) {
            sb.append("§5Goals: §f");

            for (int j = 0; j < Math.min(3, this.dreamsAndGoals.size()); j++) {
               sb.append("\"").append(this.dreamsAndGoals.get(j)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.regrets.isEmpty()) {
            sb.append("§5Regrets: §f");

            for (int j = 0; j < Math.min(2, this.regrets.size()); j++) {
               sb.append("\"").append(this.regrets.get(j)).append("\" ");
            }

            sb.append("\n");
         }

         sb.append("\n");
      }

      sb.append("§5§l[AI RELATIONSHIP]\n");
      sb.append("§5Trust: §f")
         .append(this.trustLevel)
         .append("/10 | Vulnerability: ")
         .append(this.vulnerabilityLevel)
         .append("/10 | Sees AI: ")
         .append(this.howTheySeeAI.isEmpty() ? "unknown" : this.howTheySeeAI)
         .append("\n");
      sb.append("§5AI Relationship Label: §f").append(this.aiRelationship).append("\n");
      if (!this.topicsAskedAI.isEmpty()) {
         sb.append("§5Topics Asked AI: §f").append(String.join(", ", this.topicsAskedAI)).append("\n");
      }

      if (!this.personalFactsShared.isEmpty()) {
         sb.append("§5Personal Facts (last 50):\n");

         for (int i = Math.max(0, this.personalFactsShared.size() - 50); i < this.personalFactsShared.size(); i++) {
            sb.append("  §7\"").append(this.personalFactsShared.get(i)).append("\"\n");
         }

         if (this.personalFactsShared.size() > 50) {
            sb.append("  §7... and ").append(this.personalFactsShared.size() - 50).append(" more\n");
         }
      }

      if (!this.frustrationTriggers.isEmpty()) {
         sb.append("§5Frustration Triggers: §f").append(String.join(", ", this.frustrationTriggers)).append("\n");
      }

      if (!this.thingsTheyLike.isEmpty()) {
         sb.append("§5Likes: §f").append(String.join(", ", this.thingsTheyLike)).append("\n");
      }

      if (!this.thingsTheyDislike.isEmpty()) {
         sb.append("§5Dislikes: §f").append(String.join(", ", this.thingsTheyDislike)).append("\n");
      }

      sb.append("\n");
      sb.append("§e§l========================================\n");
      sb.append("§e§l   END OF REPORT\n");
      sb.append("§e§l========================================");
      return sb.toString();
   }

   private String fmtDate(long ts) {
      return ts <= 0L ? "N/A" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ts));
   }

   public boolean wasAt(String world, int x, int y, int z, long withinMs, int radius) {
      this.ensureInit();
      long now = System.currentTimeMillis();

      for (LocationSnapshot snap : this.locationHistory) {
         if (now - snap.timestamp <= withinMs && snap.world.equalsIgnoreCase(world)) {
            int dx = Math.abs(snap.x - x);
            int dy = Math.abs(snap.y - y);
            int dz = Math.abs(snap.z - z);
            if (dx <= radius && dy <= radius && dz <= radius) {
               return true;
            }
         }
      }

      return false;
   }

   private void detectDeepPersonalFacts(String message) {
      String lower = message.toLowerCase();
      String[][] array = new String[][]{
         {"ich bin ", "realNameOrAge"},
         {"i am ", "realNameOrAge"},
         {"mein name ist ", "realName"},
         {"my name is ", "realName"},
         {"ich wohne ", "location"},
         {"i live in ", "location"},
         {"ich komme aus ", "location"},
         {"ich bin ", "age"},
         {"i'm ", "age"},
         {"ich arbeite als ", "job"},
         {"i work as ", "job"},
         {"ich bin ", "job"},
         {"ich habe eine ausbildung", "education"},
         {"i studied ", "education"},
         {"mein bruder ", "family"},
         {"meine schwester ", "family"},
         {"meine mutter ", "family"},
         {"mein vater ", "family"},
         {"my brother ", "family"},
         {"my sister ", "family"},
         {"my mom ", "family"},
         {"my dad ", "family"}
      };

      for (String[] pat : array) {
         if (lower.contains(pat[0])) {
            int idx = lower.indexOf(pat[0]) + pat[0].length();
            int end = this.findSentenceEnd(message, idx);
            String fact = message.substring(idx, Math.min(end, message.length())).trim();
            if (fact.length() > 2 && fact.length() < 60) {
               String s = pat[1];
               switch (s) {
                  case "realName":
                     if (this.realName.isEmpty()) {
                        this.realName = fact;
                     }
                     break;
                  case "location":
                     if (this.location.isEmpty()) {
                        this.location = fact;
                     }
                     break;
                  case "job":
                     if (this.job.isEmpty()) {
                        this.job = fact;
                     }
                     break;
                  case "education":
                     if (this.education.isEmpty()) {
                        this.education = fact;
                     }
                     break;
                  case "family":
                     if (!this.familyMentioned.contains(fact)) {
                        this.familyMentioned.add(fact);
                     }
                     break;
                  case "realNameOrAge":
                     if (fact.matches("\\d+")) {
                        this.age = fact;
                     } else if (this.realName.isEmpty() && !fact.equals("müde") && !fact.equals("hungrig") && !fact.equals("glücklich")) {
                        this.realName = fact;
                     }
               }
            }
         }
      }
   }

   private int findSentenceEnd(String text, int start) {
      for (int i = start; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '.' || c == '!' || c == '?' || c == ',' || c == ';') {
            return i;
         }
      }

      return Math.min(start + 40, text.length());
   }

   private void detectPsychologicalPatterns(String lower, String message) {
      if (lower.contains("muss perfekt sein")
         || lower.contains("alles kontrollieren")
         || lower.contains("genau so")
         || lower.contains("nicht gut genug")
         || lower.contains("muss richtig sein")) {
         this.isPerfectionist = true;
         this.perfectionismLevel = Math.min(10, this.perfectionismLevel + 1);
         if (!this.coreValues.contains("Kontrolle")) {
            this.coreValues.add("Kontrolle");
         }

         if (!this.coreValues.contains("Perfektion")) {
            this.coreValues.add("Perfektion");
         }
      }

      if (lower.contains("einfach mal") || lower.contains("spontan") || lower.contains("was solls") || lower.contains("yolo") || lower.contains("egal")) {
         this.isImpulsive = true;
      }

      if (lower.contains("was haltet ihr")
         || lower.contains("findet ihr")
         || lower.contains("bin ich gut")
         || lower.contains("was meint ihr")
         || lower.contains("bewertet")) {
         this.seeksValidation = true;
         if (!this.coreValues.contains("Anerkennung")) {
            this.coreValues.add("Anerkennung");
         }
      }

      if (lower.contains("überlege")
         || lower.contains("denke zu viel")
         || lower.contains("kann nicht aufhören")
         || lower.contains("grübel")
         || lower.contains("analysiere")) {
         this.overthinks = true;
      }

      if (lower.contains("vermeide") || lower.contains("will keinen streit") || lower.contains("lieber frieden") || lower.contains("passt schon")) {
         this.avoidsConflict = true;
         this.conflictStyle = "vermeidend";
      }

      if (lower.contains("ich will entscheiden") || lower.contains("mein server") || lower.contains("ich bestimme") || lower.contains("unter meiner kontrolle")
         )
       {
         this.needsControl = true;
         if (!this.coreValues.contains("Kontrolle")) {
            this.coreValues.add("Kontrolle");
         }
      }

      if ((lower.contains("lol") || lower.contains("haha") || lower.contains("xd"))
         && (lower.contains("traurig") || lower.contains("deprimiert") || lower.contains("schlecht") || lower.contains("mist"))) {
         this.usesHumorAsDefense = true;
      }

      if (lower.contains("ich bin nutzlos") || lower.contains("ich kann nichts") || lower.contains("bin ich gut genug") || lower.contains("niemand mag mich")) {
         this.selfEsteemIndicator = "low";
      } else if (lower.contains("ich bin stolz") || lower.contains("ich habe es geschafft") || lower.contains("ich bin zufrieden")) {
         if ("low".equals(this.selfEsteemIndicator)) {
            this.selfEsteemIndicator = "moderate";
         } else {
            this.selfEsteemIndicator = "high";
         }
      }

      if (lower.contains("vertraue niemandem") || lower.contains("allein besser") || lower.contains("brauche niemanden")) {
         this.attachmentStyle = "avoidant";
      } else if (lower.contains("brauche jemanden") || lower.contains("will nicht allein") || lower.contains("verlasse mich nicht")) {
         this.attachmentStyle = "anxious";
      } else if (lower.contains("vertraue meinen freunden") || lower.contains("gemeinsam stark")) {
         this.attachmentStyle = "secure";
      }
   }

   private void detectCommunicationStyle(String lower) {
      if (lower.contains("ehrlich gesagt") || lower.contains("ich sag dir direkt") || lower.contains("um ehrlich zu sein") || lower.contains("ehrlich")) {
         this.communicationStyle = "direkt";
      }

      if ((lower.contains("vielleicht") || lower.contains("könnte man") || lower.contains("eventuell") || lower.contains("was meinst du"))
         && !"direkt".equals(this.communicationStyle)) {
         this.communicationStyle = "diplomatisch";
      }

      if (lower.contains("sarkastisch") || lower.contains("ironisch") || lower.contains("natürlich...") || lower.contains("klar doch")) {
         this.communicationStyle = "sarkastisch";
      }
   }

   private void detectStressResponse(String lower) {
      if (lower.contains("ich muss weg") || lower.contains("ich ignoriere") || lower.contains("konfrontiere nicht")) {
         this.stressResponse = "vermeidend";
      } else if (lower.contains("ich regle das") || lower.contains("ich fixe") || lower.contains("sofort erledigen")) {
         this.stressResponse = "proaktiv";
      } else if (lower.contains("ich werde wütend") || lower.contains("ich flippe aus") || lower.contains("ich kann nicht")) {
         this.stressResponse = "emotional";
      }
   }

   private void detectEnergySource(String lower) {
      if (lower.contains("allein") || lower.contains("ruhe") || lower.contains("stille") || lower.contains("menschen ermüden")) {
         this.energySource = "introvert";
      } else if (lower.contains("mit leuten") || lower.contains("party") || lower.contains("freunde treffen") || lower.contains("draußen")) {
         this.energySource = "extrovert";
      } else if (lower.contains("manchmal allein") || lower.contains("manchmal mit leuten")) {
         this.energySource = "ambivert";
      }
   }

   private void detectCompetitiveness(String lower) {
      if (lower.contains("besser als")
         || lower.contains("ich will gewinnen")
         || lower.contains("nummer 1")
         || lower.contains("der beste")
         || lower.contains("konkurrenz")) {
         this.isCompetitive = true;
         if (!this.coreValues.contains("Wettbewerb")) {
            this.coreValues.add("Wettbewerb");
         }
      }
   }

   private void detectPerfectionism(String lower) {
      if (lower.contains("muss perfekt")
         || lower.contains("kann nicht so lassen")
         || lower.contains("nochmal von vorne")
         || lower.contains("pixel perfect")
         || lower.contains("clean")) {
         this.isPerfectionist = true;
         this.perfectionismLevel = Math.min(10, this.perfectionismLevel + 1);
      }
   }

   private void detectEmpathy(String lower) {
      if (lower.contains("verstehe dich")
         || lower.contains("tut mir leid für")
         || lower.contains("das ist hart für")
         || lower.contains("ich fühle mit")
         || lower.contains("kann ich helfen")) {
         this.isEmpathetic = true;
      }
   }

   private void detectDreamsAndGoals(String message) {
      String lower = message.toLowerCase();
      String[] array = new String[]{
         "ich will ",
         "i want to ",
         "ich möchte ",
         "i would like to ",
         "mein traum ist ",
         "my dream is ",
         "mein ziel ist ",
         "my goal is ",
         "ich plane ",
         "i plan to "
      };

      for (String p : array) {
         if (lower.contains(p)) {
            int idx = lower.indexOf(p) + p.length();
            int end = this.findSentenceEnd(message, idx);
            String goal = message.substring(idx, Math.min(end, message.length())).trim();
            if (goal.length() > 5 && goal.length() < 80 && !this.dreamsAndGoals.contains(goal)) {
               this.dreamsAndGoals.add(goal);
               if (this.dreamsAndGoals.size() > 1000) {
                  this.dreamsAndGoals.remove(0);
               }
            }
         }
      }
   }

   private void detectRegrets(String message) {
      String lower = message.toLowerCase();
      String[] array = new String[]{"hätte ich ", "wenn ich damals ", "ich bereue ", "schade dass ", "hätte besser ", "wäre ich nur "};

      for (String p : array) {
         if (lower.contains(p)) {
            int idx = lower.indexOf(p);
            int end = this.findSentenceEnd(message, idx);
            String regret = message.substring(idx, Math.min(end, message.length())).trim();
            if (regret.length() > 5 && regret.length() < 80 && !this.regrets.contains(regret)) {
               this.regrets.add(regret);
               if (this.regrets.size() > 1000) {
                  this.regrets.remove(0);
               }
            }
         }
      }
   }

   private void detectFamilyMentions(String message) {
      String lower = message.toLowerCase();
      String[] array = new String[]{
         "mein bruder ", "meine schwester ", "meine mutter ", "mein vater ", "meine eltern ", "meine oma ", "mein opa ", "meine tante ", "mein onkel "
      };

      for (String f : array) {
         if (lower.contains(f)) {
            int idx = lower.indexOf(f);
            int end = this.findSentenceEnd(message, idx);
            String mention = message.substring(idx, Math.min(end, message.length())).trim();
            if (mention.length() > 5 && mention.length() < 60 && !this.familyMentioned.contains(mention)) {
               this.familyMentioned.add(mention);
            }
         }
      }
   }

   private void detectLifeEvents(String message) {
      String lower = message.toLowerCase();
      String[] array = new String[]{
         "ich habe geheiratet",
         "ich bin umgezogen",
         "ich habe meinen job gekündigt",
         "ich habe die schule beendet",
         "ich bin verlobt",
         "ich habe ein kind",
         "ich habe meine ausbildung",
         "ich habe geprüfung",
         "ich bin krank",
         "i got married",
         "i moved",
         "i quit my job",
         "i graduated",
         "i got engaged",
         "i have a child"
      };

      for (String e : array) {
         if (lower.contains(e)) {
            int idx = lower.indexOf(e);
            int end = this.findSentenceEnd(message, idx);
            String evt = message.substring(idx, Math.min(end, message.length())).trim();
            if (evt.length() > 5 && !this.sharedLifeEvents.contains(evt)) {
               this.sharedLifeEvents.add(evt);
               if (this.sharedLifeEvents.size() > 1000) {
                  this.sharedLifeEvents.remove(0);
               }
            }
         }
      }
   }

   private void updateTrustAndVulnerability(String message) {
      String lower = message.toLowerCase();
      if (lower.contains("danke") || lower.contains("thank") || lower.contains("du hilfst mir") || lower.contains("guter rat")) {
         this.trustLevel = Math.min(10, this.trustLevel + 1);
      }

      if (lower.contains("ich fühle")
         || lower.contains("i feel")
         || lower.contains("ich bin traurig")
         || lower.contains("ich habe angst")
         || lower.contains("niemand versteht")
         || lower.contains("ich bin allein")
         || lower.contains("meine vergangenheit")
         || lower.contains("ich habe probleme")
         || lower.contains("ich bin deprimiert")
         || lower.contains("ich bin gestresst")) {
         this.vulnerabilityLevel = Math.min(10, this.vulnerabilityLevel + 1);
      }

      if (lower.contains("du bist nur ein bot") || lower.contains("du bist nur code") || lower.contains("ai halt")) {
         this.howTheySeeAI = "tool";
      } else if (lower.contains("du bist ein guter freund") || lower.contains("du verstehst mich") || lower.contains("mit dir reden")) {
         this.howTheySeeAI = "companion";
      } else if (lower.contains("bist du real") || lower.contains("hast du gefühle")) {
         this.howTheySeeAI = "curious";
      }
   }

   public String getDeepProfile() {
      this.ensureInit();
      if (this.totalInteractions < 3) {
         return null;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("=== DEEP PLAYER PROFILE ===\n");
         if (!this.realName.isEmpty() || !this.age.isEmpty() || !this.location.isEmpty()) {
            sb.append("Known identity: ");
            if (!this.realName.isEmpty()) {
               sb.append("Name=").append(this.realName).append(" ");
            }

            if (!this.age.isEmpty()) {
               sb.append("Age=").append(this.age).append(" ");
            }

            if (!this.location.isEmpty()) {
               sb.append("Location=").append(this.location).append(" ");
            }

            if (!this.job.isEmpty()) {
               sb.append("Job=").append(this.job).append(" ");
            }

            if (!this.education.isEmpty()) {
               sb.append("Education=").append(this.education).append(" ");
            }

            sb.append("\n");
         }

         if (!this.mbtiType.isEmpty()) {
            sb.append("MBTI guess: ").append(this.mbtiType).append(". ");
         }

         if (!this.enneagramType.isEmpty()) {
            sb.append("Enneagram guess: ").append(this.enneagramType).append(". ");
         }

         sb.append("Big Five (guess): Openness=")
            .append(this.bigFiveOpenness)
            .append(" Conscientiousness=")
            .append(this.bigFiveConscientiousness)
            .append(" Extraversion=")
            .append(this.bigFiveExtraversion)
            .append(" Agreeableness=")
            .append(this.bigFiveAgreeableness)
            .append(" Neuroticism=")
            .append(this.bigFiveNeuroticism)
            .append(".\n");
         if (!this.coreValues.isEmpty()) {
            sb.append("Core values: ").append(String.join(", ", this.coreValues)).append(".\n");
         }

         if (!this.fears.isEmpty()) {
            sb.append("Expressed fears: ").append(String.join(", ", this.fears)).append(".\n");
         }

         if (!this.strengths.isEmpty()) {
            sb.append("Observed strengths: ").append(String.join(", ", this.strengths)).append(".\n");
         }

         if (!this.weaknesses.isEmpty()) {
            sb.append("Observed weaknesses: ").append(String.join(", ", this.weaknesses)).append(".\n");
         }

         if (!this.primaryMotivation.isEmpty()) {
            sb.append("Primary motivation: ").append(this.primaryMotivation).append(".\n");
         }

         if (!this.attachmentStyle.equals("unknown")) {
            sb.append("Attachment style guess: ").append(this.attachmentStyle).append(".\n");
         }

         if (!this.selfEsteemIndicator.equals("unknown")) {
            sb.append("Self-esteem indicator: ").append(this.selfEsteemIndicator).append(".\n");
         }

         if (this.perfectionismLevel > 5) {
            sb.append("Perfectionism level: high (").append(this.perfectionismLevel).append("/10).\n");
         }

         if (!this.communicationStyle.equals("unknown")) {
            sb.append("Communication style: ").append(this.communicationStyle).append(". ");
         }

         if (!this.conflictStyle.equals("unknown")) {
            sb.append("Conflict style: ").append(this.conflictStyle).append(". ");
         }

         if (!this.stressResponse.equals("unknown")) {
            sb.append("Stress response: ").append(this.stressResponse).append(". ");
         }

         if (!this.energySource.equals("unknown")) {
            sb.append("Energy source: ").append(this.energySource).append(". ");
         }

         sb.append("\n");
         sb.append("Behavioral patterns: ");
         if (this.isPerfectionist) {
            sb.append("perfectionist ");
         }

         if (this.isImpulsive) {
            sb.append("impulsive ");
         }

         if (this.isCompetitive) {
            sb.append("competitive ");
         }

         if (this.isEmpathetic) {
            sb.append("empathetic ");
         }

         if (this.usesHumorAsDefense) {
            sb.append("uses-humor-as-defense ");
         }

         if (this.overthinks) {
            sb.append("overthinks ");
         }

         if (this.seeksValidation) {
            sb.append("seeks-validation ");
         }

         if (this.avoidsConflict) {
            sb.append("conflict-avoidant ");
         }

         if (this.needsControl) {
            sb.append("needs-control ");
         }

         sb.append("\n");
         if (!this.technicalInterests.isEmpty()) {
            sb.append("Technical interests: ").append(String.join(", ", this.technicalInterests)).append(".\n");
         }

         if (!this.creativeInterests.isEmpty()) {
            sb.append("Creative interests: ").append(String.join(", ", this.creativeInterests)).append(".\n");
         }

         if (!this.socialInterests.isEmpty()) {
            sb.append("Social interests: ").append(String.join(", ", this.socialInterests)).append(".\n");
         }

         if (!this.hobbies.isEmpty()) {
            sb.append("Hobbies: ").append(String.join(", ", this.hobbies)).append(".\n");
         }

         if (!this.sharedLifeEvents.isEmpty()) {
            sb.append("Shared life events: ");

            for (int i = 0; i < Math.min(3, this.sharedLifeEvents.size()); i++) {
               sb.append("\"").append(this.sharedLifeEvents.get(i)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.familyMentioned.isEmpty()) {
            sb.append("Family mentioned: ");

            for (int i = 0; i < Math.min(3, this.familyMentioned.size()); i++) {
               sb.append("\"").append(this.familyMentioned.get(i)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.dreamsAndGoals.isEmpty()) {
            sb.append("Dreams/goals: ");

            for (int i = 0; i < Math.min(3, this.dreamsAndGoals.size()); i++) {
               sb.append("\"").append(this.dreamsAndGoals.get(i)).append("\" ");
            }

            sb.append("\n");
         }

         if (!this.regrets.isEmpty()) {
            sb.append("Expressed regrets: ");

            for (int i = 0; i < Math.min(2, this.regrets.size()); i++) {
               sb.append("\"").append(this.regrets.get(i)).append("\" ");
            }

            sb.append("\n");
         }

         sb.append("AI relationship: trust=").append(this.trustLevel).append("/10 vulnerability=").append(this.vulnerabilityLevel).append("/10");
         if (!this.howTheySeeAI.isEmpty()) {
            sb.append(" seesAIas=").append(this.howTheySeeAI);
         }

         sb.append(".\n");
         if (!this.playStyle.equals("unknown")) {
            sb.append("Play style: ").append(this.playStyle).append(". ");
         }

         if (!this.buildStyle.equals("unknown")) {
            sb.append("Build style: ").append(this.buildStyle).append(". ");
         }

         sb.append("\n");
         if (!this.interests.isEmpty()) {
            sb.append("Interests: ").append(String.join(", ", this.interests)).append(".\n");
         }

         if (!this.thingsTheyLike.isEmpty()) {
            sb.append("Likes: ").append(String.join(", ", this.thingsTheyLike)).append(".\n");
         }

         if (!this.thingsTheyDislike.isEmpty()) {
            sb.append("Dislikes: ").append(String.join(", ", this.thingsTheyDislike)).append(".\n");
         }

         if (!this.personalFactsShared.isEmpty()) {
            sb.append("Personal facts shared: ");
            int n = Math.min(5, this.personalFactsShared.size());

            for (int j = 0; j < n; j++) {
               sb.append("\"").append(this.personalFactsShared.get(j)).append("\" ");
            }

            sb.append("\n");
         }

         sb.append("=== END DEEP PROFILE ===\n");
         return sb.toString();
      }
   }
}
