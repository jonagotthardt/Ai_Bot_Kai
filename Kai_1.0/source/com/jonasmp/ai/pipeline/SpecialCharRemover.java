package com.jonasmp.ai.pipeline;

import java.util.regex.Pattern;

public class SpecialCharRemover {
   private static final Pattern PUNCTUATION_SPACER = Pattern.compile("(?<=\\p{L})[\\p{P}\\p{S}|._/\\-]+(?=\\p{L})");
   private static final Pattern REPEATED_SPECIAL = Pattern.compile("([\\p{P}\\p{S}])\\1{2,}");
   private static final Pattern WHITESPACE_NORMALIZER = Pattern.compile("\\s+");

   public String clean(String text) {
      if (text != null && !text.isEmpty()) {
         String cleaned = PUNCTUATION_SPACER.matcher(text).replaceAll("");
         cleaned = REPEATED_SPECIAL.matcher(cleaned).replaceAll("$1");
         cleaned = WHITESPACE_NORMALIZER.matcher(cleaned).replaceAll(" ");
         return cleaned.trim();
      } else {
         return "";
      }
   }

   public String aggressiveClean(String text) {
      return text == null ? "" : text.replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim();
   }
}
