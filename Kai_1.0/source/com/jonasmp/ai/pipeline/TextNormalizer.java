package com.jonasmp.ai.pipeline;

import java.text.Normalizer;
import java.text.Normalizer.Form;

public class TextNormalizer {
   public String normalize(String input) {
      if (input != null && !input.isEmpty()) {
         String normalized = Normalizer.normalize(input, Form.NFKC);
         normalized = normalized.replaceAll("[\\p{Mn}\\p{Me}\\p{Cf}]", "");
         normalized = normalized.replaceAll("[\\p{Cc}&&[^\\t\\n\\r]]", " ");
         normalized = normalized.replaceAll("\\s+", " ");
         return normalized.toLowerCase().trim();
      } else {
         return "";
      }
   }
}
