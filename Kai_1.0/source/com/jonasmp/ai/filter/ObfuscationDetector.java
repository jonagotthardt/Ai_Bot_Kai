package com.jonasmp.ai.filter;

import java.util.regex.Pattern;

public class ObfuscationDetector {
   private static final Pattern MIXED_SCRIPT = Pattern.compile("(?i)(?=.*\\p{InBasic_Latin})(?=.*[\\p{InCyrillic}\\p{InGreek}])");
   private static final Pattern LETTER_SPLIT = Pattern.compile("(?i)(?:\\p{L}\\p{P}?){3,}\\p{L}");
   private static final Pattern VERTICAL_TEXT = Pattern.compile("(?m)^\\p{L}$");
   private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF\\u2060\\u180E]");
   private static final Pattern EXCESSIVE_SPACING = Pattern.compile("\\p{L}\\s{2,}\\p{L}");

   public boolean detect(String rawText, String cleanedText, String aggressiveClean) {
      if (rawText == null || rawText.isEmpty()) {
         return false;
      } else if (MIXED_SCRIPT.matcher(rawText).find()) {
         return true;
      } else if (INVISIBLE_CHARS.matcher(rawText).find()) {
         return true;
      } else if (EXCESSIVE_SPACING.matcher(rawText).find()) {
         return true;
      } else {
         if (LETTER_SPLIT.matcher(cleanedText).find()) {
            int letters = 0;
            int punctuated = 0;

            for (int i = 0; i < cleanedText.length(); i++) {
               char c = cleanedText.charAt(i);
               if (Character.isLetter(c)) {
                  letters++;
                  if (i > 0 && i < cleanedText.length() - 1) {
                     char prev = cleanedText.charAt(i - 1);
                     char next = cleanedText.charAt(i + 1);
                     if (Character.isLetter(prev) && !Character.isLetterOrDigit(next)) {
                        punctuated++;
                     }
                  }
               }
            }

            if (letters > 0 && (double)punctuated / (double)letters > 0.3) {
               return true;
            }
         }

         if (aggressiveClean.length() >= 4 && aggressiveClean.length() <= 20) {
            String reversed = new StringBuilder(aggressiveClean).reverse().toString();
            if (reversed.matches("(?i)(fuck|kcuf|tihs|kcus|rettub|elohssa|tnuc)")) {
               return true;
            }
         }

         return false;
      }
   }
}
