package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.HashSet;
import java.util.Set;

public class TypoCorrector {
   private final WordlistLoader loader;
   private final Set<String> safeWords;

   public TypoCorrector(WordlistLoader loader) {
      this.loader = loader;
      Set<String> sw = new HashSet<>();
      CategoryConfig cfg = loader.getCategory("safewords");
      if (cfg != null && cfg.words != null) {
         for (String w : cfg.words) {
            sw.add(w.toLowerCase());
         }
      }

      this.safeWords = sw;
   }

   public String correct(String text) {
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase();
         String[] words = lower.split("\\s+");
         StringBuilder corrected = new StringBuilder(text);

         for (String word : words) {
            if (word.length() >= 4 && !this.safeWords.contains(word)) {
               String bestMatch = this.findBestMatch(word);
               if (bestMatch != null && !bestMatch.equalsIgnoreCase(word)) {
                  this.replaceIgnoreCase(corrected, word, bestMatch);
               }
            }
         }

         return corrected.toString();
      } else {
         return "";
      }
   }

   private String findBestMatch(String word) {
      int threshold = this.getThreshold(word.length());
      String bestMatch = null;
      int bestDistance = Integer.MAX_VALUE;

      for (String category : this.loader.getCategoryNames()) {
         if (!"safewords".equals(category)) {
            CategoryConfig cfg = this.loader.getCategory(category);
            if (cfg != null && cfg.words != null) {
               for (String listWord : cfg.words) {
                  String lw = listWord.toLowerCase();
                  if (!this.safeWords.contains(lw)) {
                     int dist = damerauLevenshtein(word, lw);
                     if (dist <= threshold && dist < bestDistance) {
                        bestDistance = dist;
                        bestMatch = listWord;
                     }
                  }
               }
            }
         }
      }

      return bestMatch;
   }

   private int getThreshold(int wordLength) {
      if (wordLength <= 4) {
         return 1;
      } else {
         return wordLength <= 6 ? 1 : 2;
      }
   }

   private void replaceIgnoreCase(StringBuilder sb, String target, String replacement) {
      String lowerSb = sb.toString().toLowerCase();
      int idx = lowerSb.indexOf(target);

      while (idx != -1) {
         sb.replace(idx, idx + target.length(), replacement);
         lowerSb = sb.toString().toLowerCase();
         idx = lowerSb.indexOf(target);
      }
   }

   public static int damerauLevenshtein(String s1, String s2) {
      int len1 = s1.length();
      int len2 = s2.length();
      if (len1 == 0) {
         return len2;
      } else if (len2 == 0) {
         return len1;
      } else {
         int[][] dp = new int[len1 + 1][len2 + 1];
         int i = 0;

         while (i <= len1) {
            dp[i][0] = i++;
         }

         i = 0;

         while (i <= len2) {
            dp[0][i] = i++;
         }

         for (int ix = 1; ix <= len1; ix++) {
            for (int k = 1; k <= len2; k++) {
               int cost = s1.charAt(ix - 1) != s2.charAt(k - 1) ? 1 : 0;
               dp[ix][k] = Math.min(Math.min(dp[ix - 1][k] + 1, dp[ix][k - 1] + 1), dp[ix - 1][k - 1] + cost);
               if (ix > 1 && k > 1 && s1.charAt(ix - 1) == s2.charAt(k - 2) && s1.charAt(ix - 2) == s2.charAt(k - 1)) {
                  dp[ix][k] = Math.min(dp[ix][k], dp[ix - 2][k - 2] + cost);
               }
            }
         }

         return dp[len1][len2];
      }
   }
}
