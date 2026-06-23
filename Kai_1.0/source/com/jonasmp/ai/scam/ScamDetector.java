package com.jonasmp.ai.scam;

import java.util.List;
import java.util.regex.Pattern;

public class ScamDetector {
   private final List<Pattern> patterns = List.of(
      Pattern.compile("(?i)https?://"),
      Pattern.compile("(?i)discord\\.gg/"),
      Pattern.compile("(?i)free\\s+rank"),
      Pattern.compile("(?i)giveaway"),
      Pattern.compile("(?i)join\\s+my\\s+server"),
      Pattern.compile("(?i)ip:\\s*\\d+\\.\\d+\\.\\d+\\.\\d+"),
      Pattern.compile("(?i)free\\s+money"),
      Pattern.compile("(?i)nitro")
   );

   public boolean isScam(String message) {
      if (message == null) {
         return false;
      } else {
         for (Pattern p : this.patterns) {
            if (p.matcher(message).find()) {
               return true;
            }
         }

         return false;
      }
   }

   public double scamScore(String message) {
      if (message == null) {
         return 0.0;
      } else {
         double score = 0.0;

         for (Pattern p : this.patterns) {
            if (p.matcher(message).find()) {
               score += 20.0;
            }
         }

         return Math.min(score, 100.0);
      }
   }
}
