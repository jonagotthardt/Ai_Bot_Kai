package com.jonasmp.ai.filter;

public class EmojiInjectorDetector {
   public boolean detect(String text) {
      if (text != null && !text.isEmpty()) {
         int emojiCount = 0;
         int letterCount = 0;
         boolean emojiBetweenLetters = false;
         char prev = 0;
         int i = 0;

         while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (this.isEmoji(cp)) {
               emojiCount++;
               if (prev != 0 && i + Character.charCount(cp) < text.length()) {
                  int nextCp = text.codePointAt(i + Character.charCount(cp));
                  if (Character.isLetter(prev) && Character.isLetter(nextCp)) {
                     emojiBetweenLetters = true;
                  }
               }
            } else if (Character.isLetter(cp)) {
               letterCount++;
            }

            prev = (char)cp;
            i += Character.charCount(cp);
         }

         if (emojiBetweenLetters) {
            return true;
         } else {
            i = emojiCount + letterCount;
            return i > 0 && (double)emojiCount / (double)i > 0.5 || emojiCount >= 5 && text.matches(".*(.)\\1{4,}.*");
         }
      } else {
         return false;
      }
   }

   private boolean isEmoji(int cp) {
      return cp >= 128512 && cp <= 128591
         || cp >= 127744 && cp <= 128511
         || cp >= 128640 && cp <= 128767
         || cp >= 127456 && cp <= 127487
         || cp >= 9728 && cp <= 9983
         || cp >= 9984 && cp <= 10175
         || cp >= 65024 && cp <= 65039
         || cp >= 129280 && cp <= 129535
         || cp >= 129536 && cp <= 129647
         || cp == 8205
         || cp >= 128992 && cp <= 129023
         || cp >= 8960 && cp <= 9215;
   }
}
