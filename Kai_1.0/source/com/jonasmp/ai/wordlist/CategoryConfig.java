package com.jonasmp.ai.wordlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryConfig {
   public String category;
   public String description;
   public String severity;
   public double score_per_hit;
   public double max_score;
   public String action;
   public List<String> words = new ArrayList<>();
   public List<String> patterns = new ArrayList<>();
   public CategoryConfig.CompoundConfig compounds = new CategoryConfig.CompoundConfig();
   public CategoryConfig.ContextBoost context_boosters = new CategoryConfig.ContextBoost();
   public CategoryConfig.RepetitionRules repetition_rules = new CategoryConfig.RepetitionRules();
   public Map<String, String> leet_mappings = new HashMap<>();

   public static class CompoundConfig {
      public List<String> prefixes = new ArrayList<>();
      public List<String> suffixes = new ArrayList<>();
   }

   public static class ContextBoost {
      public List<String> insult_phrases = new ArrayList<>();
      public double boost = 0.0;
   }

   public static class RepetitionRules {
      public int max_repeated_chars = 4;
      public int max_repeated_words = 3;
      public double max_caps_ratio = 0.7;
      public double max_emoji_ratio = 0.5;
   }
}
