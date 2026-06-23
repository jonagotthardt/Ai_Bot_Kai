package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher {
   private final WordlistLoader loader;

   public RegexMatcher(WordlistLoader loader) {
      this.loader = loader;
   }

   public Map<String, RegexMatcher.RegexResult> matchAll(String text) {
      Map<String, RegexMatcher.RegexResult> results = new HashMap<>();
      if (text != null && !text.isEmpty()) {
         for (String category : this.loader.getCategoryNames()) {
            List<Pattern> patterns = this.loader.getPatterns(category);
            Pattern compound = this.loader.getCompoundPattern(category);
            CategoryConfig config = this.loader.getCategory(category);
            if (config != null) {
               List<String> hits = new ArrayList<>();

               for (Pattern p : patterns) {
                  Matcher m = p.matcher(text);

                  while (m.find()) {
                     hits.add(m.group());
                  }
               }

               if (compound != null) {
                  Matcher i = compound.matcher(text);

                  while (i.find()) {
                     hits.add(i.group());
                  }
               }

               if (!hits.isEmpty()) {
                  double score = Math.min((double)hits.size() * config.score_per_hit, config.max_score);
                  results.put(category, new RegexMatcher.RegexResult(category, hits, score, config.severity));
               }
            }
         }

         return results;
      } else {
         return results;
      }
   }

   public RegexMatcher.RegexResult matchCategory(String text, String category) {
      List<Pattern> patterns = this.loader.getPatterns(category);
      Pattern compound = this.loader.getCompoundPattern(category);
      CategoryConfig config = this.loader.getCategory(category);
      if (config == null) {
         return RegexMatcher.RegexResult.EMPTY;
      } else {
         List<String> hits = new ArrayList<>();

         for (Pattern p : patterns) {
            Matcher m = p.matcher(text);

            while (m.find()) {
               hits.add(m.group());
            }
         }

         if (compound != null) {
            Matcher i = compound.matcher(text);

            while (i.find()) {
               hits.add(i.group());
            }
         }

         if (hits.isEmpty()) {
            return RegexMatcher.RegexResult.EMPTY;
         } else {
            double score = Math.min((double)hits.size() * config.score_per_hit, config.max_score);
            return new RegexMatcher.RegexResult(category, hits, score, config.severity);
         }
      }
   }

   public static class RegexResult {
      public static final RegexMatcher.RegexResult EMPTY = new RegexMatcher.RegexResult("none", Collections.emptyList(), 0.0, "none");
      public final String category;
      public final List<String> matchedPatterns;
      public final double score;
      public final String severity;

      public RegexResult(String category, List<String> matchedPatterns, double score, String severity) {
         this.category = category;
         this.matchedPatterns = matchedPatterns;
         this.score = score;
         this.severity = severity;
      }

      public boolean isEmpty() {
         return this.score <= 0.0;
      }
   }
}
