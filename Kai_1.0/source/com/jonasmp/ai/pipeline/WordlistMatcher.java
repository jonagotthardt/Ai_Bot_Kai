package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.util.Trie;
import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordlistMatcher {
   private final WordlistLoader loader;

   public WordlistMatcher(WordlistLoader loader) {
      this.loader = loader;
   }

   public Map<String, WordlistMatcher.MatchResult> matchAll(String text) {
      Map<String, WordlistMatcher.MatchResult> results = new HashMap<>();
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase();

         for (String category : this.loader.getCategoryNames()) {
            if (!"safewords".equals(category)) {
               Trie trie = this.loader.getTrie(category);
               CategoryConfig config = this.loader.getCategory(category);
               if (trie != null && config != null) {
                  List<String> matches = trie.search(lower);
                  int wholeWordCount = 0;
                  List<String> validMatches = new ArrayList<>();

                  for (String match : matches) {
                     int count = this.countWholeWordMatches(lower, match);
                     if (count > 0) {
                        wholeWordCount += count;
                        validMatches.add(match);
                     }
                  }

                  if (wholeWordCount > 0) {
                     double score = Math.min((double)wholeWordCount * config.score_per_hit, config.max_score);
                     results.put(category, new WordlistMatcher.MatchResult(category, validMatches, score, config.severity));
                  }
               }
            }
         }

         return results;
      } else {
         return results;
      }
   }

   private int countWholeWordMatches(String text, String word) {
      String w = word.toLowerCase();
      int count = 0;

      for (int idx = 0; (idx = text.indexOf(w, idx)) != -1; idx++) {
         boolean leftBoundary = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1));
         boolean rightBoundary = idx + w.length() == text.length() || !Character.isLetterOrDigit(text.charAt(idx + w.length()));
         if (leftBoundary && rightBoundary) {
            count++;
         }
      }

      return count;
   }

   public WordlistMatcher.MatchResult matchCategory(String text, String category) {
      Trie trie = this.loader.getTrie(category);
      CategoryConfig config = this.loader.getCategory(category);
      if (trie != null && config != null) {
         String lower = text.toLowerCase();
         List<String> matches = trie.search(lower);
         int wholeWordCount = 0;
         List<String> validMatches = new ArrayList<>();

         for (String match : matches) {
            int count = this.countWholeWordMatches(lower, match);
            if (count > 0) {
               wholeWordCount += count;
               validMatches.add(match);
            }
         }

         if (wholeWordCount == 0) {
            return WordlistMatcher.MatchResult.EMPTY;
         } else {
            double score = Math.min((double)wholeWordCount * config.score_per_hit, config.max_score);
            return new WordlistMatcher.MatchResult(category, validMatches, score, config.severity);
         }
      } else {
         return WordlistMatcher.MatchResult.EMPTY;
      }
   }

   public boolean hasAnyMatch(String text) {
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase();

         for (String category : this.loader.getCategoryNames()) {
            if (!"safewords".equals(category)) {
               Trie trie = this.loader.getTrie(category);
               if (trie != null) {
                  for (String match : trie.search(lower)) {
                     if (this.countWholeWordMatches(lower, match) > 0) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static class MatchResult {
      public static final WordlistMatcher.MatchResult EMPTY = new WordlistMatcher.MatchResult("none", Collections.emptyList(), 0.0, "none");
      public final String category;
      public final List<String> matchedWords;
      public final double score;
      public final String severity;

      public MatchResult(String category, List<String> matchedWords, double score, String severity) {
         this.category = category;
         this.matchedWords = matchedWords;
         this.score = score;
         this.severity = severity;
      }

      public boolean isEmpty() {
         return this.score <= 0.0;
      }
   }
}
