package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.HashSet;
import java.util.Set;

public class SafeWordFilter {
   private final Set<String> safeWords = new HashSet<>();

   public SafeWordFilter(WordlistLoader loader) {
      CategoryConfig cfg = loader.getCategory("safewords");
      if (cfg != null && cfg.words != null) {
         for (String w : cfg.words) {
            this.safeWords.add(w.toLowerCase());
         }
      }
   }

   public boolean isSafe(String text) {
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase();
         String[] tokens = lower.split("\\s+");
         int safeCount = 0;
         int totalMeaningful = 0;

         for (String token : tokens) {
            String clean = token.replaceAll("[^a-zäöüßáéíóúàèìòùâêîôûãõñç]", "");
            if (clean.length() >= 2) {
               totalMeaningful++;
               if (this.safeWords.contains(clean)) {
                  safeCount++;
               }
            }
         }

         return totalMeaningful == 0 || (double)safeCount / (double)totalMeaningful >= 0.8;
      } else {
         return true;
      }
   }

   public boolean isSafeWord(String word) {
      return this.safeWords.contains(word.toLowerCase());
   }
}
