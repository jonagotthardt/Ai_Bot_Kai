package com.jonasmp.ai.filter;

import java.util.regex.Pattern;

public class SplitWordDetector {
   private static final Pattern SPLIT_BY_WHITESPACE = Pattern.compile("(?i)(?:\\p{L}\\s){3,}\\p{L}");
   private static final Pattern SPLIT_BY_PUNCT = Pattern.compile("(?i)(?:\\p{L}\\p{P}){3,}\\p{L}");
   private static final Pattern CAMEL_CASE = Pattern.compile("(?i)[a-z]+[A-Z][a-z]+");

   public boolean detect(String rawText, String cleanedText) {
      if (rawText != null && !rawText.isEmpty()) {
         if (SPLIT_BY_WHITESPACE.matcher(rawText).find()) {
            String compact = rawText.replaceAll("\\s+", "");
            if (this.looksLikeBadWord(compact)) {
               return true;
            }
         }

         if (SPLIT_BY_PUNCT.matcher(cleanedText).find()) {
            String compact = cleanedText.replaceAll("[^\\p{L}]", "");
            if (this.looksLikeBadWord(compact)) {
               return true;
            }
         }

         if (CAMEL_CASE.matcher(rawText).find()) {
            String spaced = rawText.replaceAll("(?i)([a-z])([A-Z])", "$1 $2").toLowerCase();
            if (this.looksLikeBadWord(spaced.replaceAll("\\s+", ""))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean looksLikeBadWord(String compact) {
      if (compact.length() < 3) {
         return false;
      } else {
         String lower = compact.toLowerCase();
         return lower.matches(".*f[cku].*")
            || lower.matches(".*s[hct].*")
            || lower.matches(".*a[s$].*")
            || lower.matches(".*b[tch].*")
            || lower.matches(".*d[ick].*")
            || lower.matches(".*n[gger].*")
            || lower.matches(".*h[ure].*")
            || lower.matches(".*w[ich].*")
            || lower.matches(".*sc[h]+.*");
      }
   }
}
