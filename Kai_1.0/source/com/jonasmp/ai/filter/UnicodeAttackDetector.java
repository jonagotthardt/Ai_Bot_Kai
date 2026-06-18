package com.jonasmp.ai.filter;

public class UnicodeAttackDetector {
   public boolean detect(String text, double unicodeRiskScore) {
      if (text != null && !text.isEmpty()) {
         if (unicodeRiskScore >= 0.5) {
            return true;
         } else {
            for (int i = 0; i < text.length(); i++) {
               int cp = text.codePointAt(i);
               if (this.isBidirectionalOverride(cp)) {
                  return true;
               }

               if (this.isCombiningMark(cp)) {
                  int count = 0;

                  for (int j = i; j < Math.min(i + 6, text.length()) && this.isCombiningMark(text.codePointAt(j)); j++) {
                     count++;
                  }

                  if (count >= 4) {
                     return true;
                  }
               }
            }

            int ascii = 0;
            int nonAscii = 0;

            for (int k = 0; k < text.length(); k++) {
               char c = text.charAt(k);
               if (c <= 127 && c >= ' ') {
                  ascii++;
               } else if (c > 127) {
                  nonAscii++;
               }
            }

            int total = ascii + nonAscii;
            return total > 5 && (double)nonAscii / (double)total > 0.4;
         }
      } else {
         return false;
      }
   }

   private boolean isBidirectionalOverride(int cp) {
      return cp == 8238 || cp == 8237 || cp == 8236 || cp == 8206 || cp == 8207 || cp == 8294 || cp == 8295 || cp == 8296 || cp == 8297;
   }

   private boolean isCombiningMark(int cp) {
      int type = Character.getType(cp);
      return type == 6 || type == 8 || type == 7;
   }
}
