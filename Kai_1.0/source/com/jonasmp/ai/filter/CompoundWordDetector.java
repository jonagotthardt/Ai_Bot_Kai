package com.jonasmp.ai.filter;

import com.jonasmp.ai.wordlist.CategoryConfig;
import com.jonasmp.ai.wordlist.WordlistLoader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompoundWordDetector {
   public boolean detect(String cleanedText, WordlistLoader loader) {
      if (cleanedText != null && !cleanedText.isEmpty()) {
         for (String category : loader.getCategoryNames()) {
            CategoryConfig config = loader.getCategory(category);
            if (config != null && config.compounds != null) {
               List<String> prefixes = config.compounds.prefixes;
               List<String> suffixes = config.compounds.suffixes;
               if (!prefixes.isEmpty() && !suffixes.isEmpty()) {
                  StringBuilder sb = new StringBuilder();
                  sb.append("(?i)\\b(?:");

                  for (int i = 0; i < prefixes.size(); i++) {
                     if (i > 0) {
                        sb.append("|");
                     }

                     sb.append(Pattern.quote(prefixes.get(i)));
                  }

                  sb.append(")(?:");

                  for (int i = 0; i < suffixes.size(); i++) {
                     if (i > 0) {
                        sb.append("|");
                     }

                     sb.append(Pattern.quote(suffixes.get(i)));
                  }

                  sb.append(")\\b");

                  try {
                     Pattern p = Pattern.compile(sb.toString());
                     Matcher m = p.matcher(cleanedText);
                     if (m.find()) {
                        return true;
                     }
                  } catch (Exception var11) {
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
