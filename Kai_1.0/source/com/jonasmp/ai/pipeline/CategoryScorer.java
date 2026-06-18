package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CategoryScorer {
   private final WordlistLoader loader;

   public CategoryScorer(WordlistLoader loader) {
      this.loader = loader;
   }

   public CategoryScorer.ScoreResult score(
      String originalText, String processedText, Map<String, WordlistMatcher.MatchResult> wordlistHits, Map<String, RegexMatcher.RegexResult> regexHits
   ) {
      Map<String, Double> categoryScores = new HashMap<>();
      Map<String, List<String>> allMatches = new HashMap<>();
      Set<String> allCategories = new HashSet<>();
      allCategories.addAll(wordlistHits.keySet());
      allCategories.addAll(regexHits.keySet());

      for (String cat : allCategories) {
         double score = 0.0;
         List<String> matches = new ArrayList<>();
         WordlistMatcher.MatchResult wr = wordlistHits.get(cat);
         if (wr != null && !wr.isEmpty()) {
            score = Math.max(score, wr.score);
            matches.addAll(wr.matchedWords);
         }

         RegexMatcher.RegexResult rr = regexHits.get(cat);
         if (rr != null && !rr.isEmpty()) {
            score = Math.max(score, rr.score);
            matches.addAll(rr.matchedPatterns);
         }

         CategoryConfig config = this.loader.getCategory(cat);
         if (config != null && config.context_boosters != null) {
            String lower = originalText.toLowerCase();

            for (String phrase : config.context_boosters.insult_phrases) {
               if (lower.contains(phrase.toLowerCase())) {
                  score = Math.min(score + config.context_boosters.boost, config.max_score);
                  matches.add("[context:" + phrase);
                  break;
               }
            }
         }

         if (score > 0.0) {
            categoryScores.put(cat, score);
            allMatches.put(cat, matches);
         }
      }

      double totalRisk = 0.0;

      for (double score2 : categoryScores.values()) {
         totalRisk = Math.max(totalRisk, score2);
      }

      if (categoryScores.size() >= 2) {
         totalRisk = Math.min(totalRisk * 1.15, 1.0);
      }

      String primaryCategory = "none";
      double maxScore = -1.0;

      for (Entry<String, Double> e : categoryScores.entrySet()) {
         if (e.getValue() > maxScore) {
            maxScore = e.getValue();
            primaryCategory = e.getKey();
         }
      }

      String action = "ALLOW";
      CategoryConfig primaryConfig = this.loader.getCategory(primaryCategory);
      if (primaryConfig != null && primaryConfig.action != null) {
         action = primaryConfig.action;
      } else if (totalRisk >= 0.85) {
         action = "BLOCK";
      } else if (totalRisk >= 0.5) {
         action = "WARN";
      }

      return new CategoryScorer.ScoreResult(categoryScores, allMatches, totalRisk, primaryCategory, action);
   }

   public static class ScoreResult {
      public final Map<String, Double> categoryScores;
      public final Map<String, List<String>> matches;
      public final double totalRisk;
      public final String primaryCategory;
      public final String recommendedAction;

      public ScoreResult(
         Map<String, Double> categoryScores, Map<String, List<String>> matches, double totalRisk, String primaryCategory, String recommendedAction
      ) {
         this.categoryScores = Collections.unmodifiableMap(categoryScores);
         this.matches = Collections.unmodifiableMap(matches);
         this.totalRisk = totalRisk;
         this.primaryCategory = primaryCategory;
         this.recommendedAction = recommendedAction;
      }
   }
}
