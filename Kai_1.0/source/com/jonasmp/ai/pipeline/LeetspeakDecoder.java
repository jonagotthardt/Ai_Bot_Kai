package com.jonasmp.ai.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LeetspeakDecoder {
   private static final Map<String, String> LEET_MAP;

   public String decode(String input) {
      if (input != null && !input.isEmpty()) {
         StringBuilder sb = new StringBuilder(input);
         List<Entry<String, String>> entries = new ArrayList<>(LEET_MAP.entrySet());
         entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

         for (Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String val = entry.getValue();
            int idx = 0;

            while ((idx = sb.indexOf(key, idx)) != -1) {
               sb.replace(idx, idx + key.length(), val);
               idx += val.length();
            }
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   static {
      (LEET_MAP = new LinkedHashMap<>()).put("vv", "w");
      LEET_MAP.put("|\\|", "n");
      LEET_MAP.put("/\\/\\", "m");
      LEET_MAP.put("()", "o");
      LEET_MAP.put("[]", "o");
      LEET_MAP.put("{}", "o");
      LEET_MAP.put("§", "s");
      LEET_MAP.put("4", "a");
      LEET_MAP.put("@", "a");
      LEET_MAP.put("ä", "a");
      LEET_MAP.put("3", "e");
      LEET_MAP.put("€", "e");
      LEET_MAP.put("1", "i");
      LEET_MAP.put("!", "i");
      LEET_MAP.put("|", "i");
      LEET_MAP.put("0", "o");
      LEET_MAP.put("ö", "o");
      LEET_MAP.put("$", "s");
      LEET_MAP.put("5", "s");
      LEET_MAP.put("7", "t");
      LEET_MAP.put("+", "t");
      LEET_MAP.put("9", "g");
      LEET_MAP.put("6", "g");
      LEET_MAP.put("2", "z");
      LEET_MAP.put("ü", "u");
      LEET_MAP.put("8", "b");
      LEET_MAP.put("(", "c");
      LEET_MAP.put("<", "c");
      LEET_MAP.put("{", "c");
      LEET_MAP.put(")", "d");
      LEET_MAP.put("#", "h");
      LEET_MAP.put("%", "x");
      LEET_MAP.put("?", "q");
      LEET_MAP.put("/\\", "a");
      LEET_MAP.put("/=", "f");
      LEET_MAP.put("|-", "t");
      LEET_MAP.put("_", " ");
   }
}
