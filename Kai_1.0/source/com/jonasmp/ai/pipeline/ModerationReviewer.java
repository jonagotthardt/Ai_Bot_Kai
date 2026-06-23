package com.jonasmp.ai.pipeline;

import com.jonasmp.ai.decision.DecisionResult;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModerationReviewer {
   private final SafeWordFilter safeWordFilter;
   private static final Set<String> POSITIVE_CONTEXT;
   private static final int AGGRESSIVE_CAPS_RATIO = 70;

   public ModerationReviewer(WordlistLoader loader) {
      this.safeWordFilter = new SafeWordFilter(loader);
   }

   public ModerationReviewer.ReviewResult review(
      String rawMessage, DecisionResult.Action originalAction, double originalScore, String originalCategory, Map<String, List<String>> matchedWords
   ) {
      if (originalAction == DecisionResult.Action.ALLOW) {
         return ModerationReviewer.ReviewResult.NO_CHANGE;
      } else {
         String lower = rawMessage.toLowerCase();
         String[] tokens = lower.split("\\s+");
         int totalTokens = 0;
         int positiveTokens = 0;
         int safeTokens = 0;

         for (String token : tokens) {
            String clean = token.replaceAll("[^a-zäöüßáéíóúàèìòùâêîôûãõñç]", "");
            if (clean.length() >= 2) {
               totalTokens++;
               if (POSITIVE_CONTEXT.contains(clean)) {
                  positiveTokens++;
               }

               if (this.safeWordFilter.isSafeWord(clean)) {
                  safeTokens++;
               }
            }
         }

         if (totalTokens > 0) {
            double positiveRatio = (double)positiveTokens / (double)totalTokens;
            double safeRatio = (double)safeTokens / (double)totalTokens;
            if (positiveRatio >= 0.5) {
               return ModerationReviewer.ReviewResult.OVERRIDE_ALLOW;
            }

            if (safeRatio >= 0.7 && originalAction != DecisionResult.Action.BAN && originalAction != DecisionResult.Action.KICK) {
               return ModerationReviewer.ReviewResult.OVERRIDE_ALLOW;
            }
         }

         int matchCount = matchedWords.values().stream().mapToInt(List::size).sum();
         boolean isSevereOrHate = "severe".equals(originalCategory) || "hate".equals(originalCategory);
         if (!isSevereOrHate && totalTokens >= 2 && totalTokens <= 6 && matchCount <= 1 && originalScore < 0.8) {
            return ModerationReviewer.ReviewResult.OVERRIDE_ALLOW;
         } else {
            boolean hasFriendlyEmoji = rawMessage.contains(":)")
               || rawMessage.contains(":D")
               || rawMessage.contains(":d")
               || rawMessage.contains("xD")
               || rawMessage.contains("<3");
            boolean hasAggressiveCaps = this.isAggressiveCaps(rawMessage);
            boolean hasAggressivePunct = rawMessage.contains("!!!") || rawMessage.contains("???") || rawMessage.contains("?!");
            if (!isSevereOrHate && hasFriendlyEmoji && !hasAggressiveCaps && !hasAggressivePunct && matchCount <= 2) {
               return ModerationReviewer.ReviewResult.OVERRIDE_ALLOW;
            } else if ((originalAction == DecisionResult.Action.BAN || originalAction == DecisionResult.Action.KICK) && originalScore < 0.92) {
               return ModerationReviewer.ReviewResult.OVERRIDE_BLOCK;
            } else {
               return !isSevereOrHate && originalAction == DecisionResult.Action.BLOCK && originalScore < 0.65 && totalTokens <= 5
                  ? ModerationReviewer.ReviewResult.OVERRIDE_WARN
                  : ModerationReviewer.ReviewResult.NO_CHANGE;
            }
         }
      }
   }

   private boolean isAggressiveCaps(String text) {
      int upper = 0;
      int letters = 0;

      for (char c : text.toCharArray()) {
         if (Character.isLetter(c)) {
            letters++;
            if (Character.isUpperCase(c)) {
               upper++;
            }
         }
      }

      return letters > 3 && upper * 100 / letters > 70;
   }

   static {
      String[] words = new String[]{
         "danke",
         "bitte",
         "hallo",
         "hi",
         "hey",
         "guten",
         "morgen",
         "tag",
         "abend",
         "freund",
         "freunde",
         "freundlich",
         "freundschaft",
         "lieb",
         "liebe",
         "liebling",
         "schön",
         "super",
         "toll",
         "cool",
         "nice",
         "gut",
         "besser",
         "beste",
         "hilfe",
         "helfen",
         "hilfst",
         "dank",
         "dankeschön",
         "vielen",
         "herzlichen",
         "willkommen",
         "welcome",
         "grüße",
         "grüß",
         "servus",
         "ciao",
         "tschüss",
         "bye",
         "glückwunsch",
         "congrats",
         "alles",
         "gute",
         "geburtstag",
         "spass",
         "spaß",
         "lustig",
         "witzig",
         "haha",
         "hehe",
         "lol",
         "xd",
         "net",
         "nett",
         "nettig",
         "süß",
         "süss",
         "süßer",
         "süsser",
         "schatz",
         "schatzi",
         "honig",
         "engel",
         "engelchen",
         "bruder",
         "schwester",
         "brudi",
         "brudii",
         "fam",
         "homie",
         "kumpel",
         "kollege",
         "team",
         "clan",
         "guild",
         "gilde",
         "party",
         "event",
         "gemeinsam",
         "zusammen",
         "cooles",
         "coole",
         "geil",
         "geiles",
         "krass",
         "stark",
         "respekt",
         "gg",
         "wp",
         "ggs",
         "gespielt",
         "try",
         "nt",
         "n1",
         "für",
         "deine",
         "dein",
         "unterstützung",
         "support",
         "verzeihung",
         "entschuldigung",
         "sorry",
         "sry",
         "mybad",
         "passiert",
         "kein",
         "problem",
         "np",
         "no",
         "prob",
         "klar",
         "kannst",
         "mir",
         "kann",
         "ich",
         "dir",
         "helfen",
         "wie",
         "gehts",
         "geht's"
      };
      POSITIVE_CONTEXT = new HashSet<>(Arrays.asList(words));
   }

   public static enum ReviewResult {
      OVERRIDE_ALLOW,
      OVERRIDE_WARN,
      OVERRIDE_BLOCK,
      NO_CHANGE;
   }
}
