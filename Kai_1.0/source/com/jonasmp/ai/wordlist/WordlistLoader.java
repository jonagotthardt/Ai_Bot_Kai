package com.jonasmp.ai.wordlist;

import com.google.gson.Gson;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.util.Trie;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WordlistLoader {
   private static final Gson GSON = new Gson();
   private static final String WORDLIST_DIR = "wordlists";
   private final Map<String, CategoryConfig> categories = new HashMap<>();
   private final Map<String, Trie> tries = new HashMap<>();
   private final Map<String, List<Pattern>> regexPatterns = new HashMap<>();
   private final Map<String, Pattern> compoundPatterns = new HashMap<>();

   public void load() {
      File dataFolder = CoreBootstrap.PLUGIN.getDataFolder();
      File wordlistFolder = new File(dataFolder, "wordlists");
      if (!wordlistFolder.exists()) {
         wordlistFolder.mkdirs();
         this.copyDefaultsFromJar(wordlistFolder);
      }

      Map<String, List<CategoryConfig>> configGroups = new HashMap<>();
      String name = null;
      File[] files = wordlistFolder.listFiles((dir, fname) -> fname.endsWith(".json"));
      if (files != null) {
         for (File file : files) {
            try {
               FileReader reader = new FileReader(file);

               try {
                  CategoryConfig config = (CategoryConfig)GSON.fromJson(reader, CategoryConfig.class);
                  if (config != null && config.category != null) {
                     name = config.category.toLowerCase();
                     configGroups.computeIfAbsent(name, k -> new ArrayList<>()).add(config);
                     reader.close();
                  } else {
                     reader.close();
                  }
               } catch (Throwable var20) {
                  try {
                     reader.close();
                  } catch (Throwable var19) {
                     var20.addSuppressed(var19);
                  }

                  throw var20;
               }
            } catch (IOException var21) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI] Failed to load wordlist: " + file.getName());
            }
         }

         for (Entry<String, List<CategoryConfig>> entry : configGroups.entrySet()) {
            String name2 = entry.getKey();
            List<CategoryConfig> group = entry.getValue();
            CategoryConfig master = this.mergeConfigs(group);
            this.categories.put(name2, master);
            Trie trie = new Trie();

            for (String word : master.words) {
               trie.insert(word.toLowerCase());
            }

            trie.build();
            this.tries.put(name2, trie);
            List<Pattern> patterns = new ArrayList<>();

            for (String regex : master.patterns) {
               try {
                  patterns.add(Pattern.compile(regex, 2));
               } catch (PatternSyntaxException var18) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AI] Invalid regex in " + name2 + ": " + regex);
               }
            }

            this.regexPatterns.put(name2, patterns);
            if (master.compounds != null && !master.compounds.prefixes.isEmpty() && !master.compounds.suffixes.isEmpty()) {
               StringBuilder sb = new StringBuilder();
               sb.append("(?i)\\b(");

               for (int i = 0; i < master.compounds.prefixes.size(); i++) {
                  if (i > 0) {
                     sb.append("|");
                  }

                  sb.append(Pattern.quote(master.compounds.prefixes.get(i)));
               }

               sb.append(")(");

               for (int i = 0; i < master.compounds.suffixes.size(); i++) {
                  if (i > 0) {
                     sb.append("|");
                  }

                  sb.append(Pattern.quote(master.compounds.suffixes.get(i)));
               }

               sb.append(")\\b");

               try {
                  this.compoundPatterns.put(name2, Pattern.compile(sb.toString()));
               } catch (PatternSyntaxException var17) {
               }
            }

            CoreBootstrap.PLUGIN
               .getLogger()
               .info("[AI] Loaded wordlist: " + name2 + " (" + master.words.size() + " words, " + patterns.size() + " patterns, " + group.size() + " file(s))");
         }
      }
   }

   private CategoryConfig mergeConfigs(List<CategoryConfig> group) {
      if (group.isEmpty()) {
         return new CategoryConfig();
      } else if (group.size() == 1) {
         return group.get(0);
      } else {
         CategoryConfig master = new CategoryConfig();
         master.category = group.get(0).category;
         master.description = group.get(0).description;
         master.severity = group.get(0).severity;
         master.score_per_hit = group.get(0).score_per_hit;
         master.max_score = group.get(0).max_score;
         master.action = group.get(0).action;
         master.words = new ArrayList<>();
         master.patterns = new ArrayList<>();
         master.context_boosters = new CategoryConfig.ContextBoost();
         master.context_boosters.insult_phrases = new ArrayList<>();
         master.context_boosters.boost = group.get(0).context_boosters != null ? group.get(0).context_boosters.boost : 0.15;
         master.compounds = new CategoryConfig.CompoundConfig();
         master.compounds.prefixes = new ArrayList<>();
         master.compounds.suffixes = new ArrayList<>();
         master.leet_mappings = new HashMap<>();
         Set<String> seenWords = new HashSet<>();
         Set<String> seenPatterns = new HashSet<>();
         Set<String> seenPrefixes = new HashSet<>();
         Set<String> seenSuffixes = new HashSet<>();
         Set<String> seenPhrases = new HashSet<>();

         for (CategoryConfig cfg : group) {
            if (cfg.words != null) {
               for (String w : cfg.words) {
                  if (seenWords.add(w.toLowerCase())) {
                     master.words.add(w);
                  }
               }
            }

            if (cfg.patterns != null) {
               for (String p : cfg.patterns) {
                  if (seenPatterns.add(p)) {
                     master.patterns.add(p);
                  }
               }
            }

            if (cfg.compounds != null) {
               if (cfg.compounds.prefixes != null) {
                  for (String px : cfg.compounds.prefixes) {
                     if (seenPrefixes.add(px.toLowerCase())) {
                        master.compounds.prefixes.add(px);
                     }
                  }
               }

               if (cfg.compounds.suffixes != null) {
                  for (String s : cfg.compounds.suffixes) {
                     if (seenSuffixes.add(s.toLowerCase())) {
                        master.compounds.suffixes.add(s);
                     }
                  }
               }
            }

            if (cfg.context_boosters != null && cfg.context_boosters.insult_phrases != null) {
               for (String phrase : cfg.context_boosters.insult_phrases) {
                  if (seenPhrases.add(phrase.toLowerCase())) {
                     master.context_boosters.insult_phrases.add(phrase);
                  }
               }
            }

            if (cfg.leet_mappings != null) {
               master.leet_mappings.putAll(cfg.leet_mappings);
            }
         }

         return master;
      }
   }

   private void copyDefaultsFromJar(File targetFolder) {
      for (String name : new String[]{
         "safewords.json",
         "severe.json",
         "profanity.json",
         "profanity_extended.json",
         "profanity_german.json",
         "profanity_batch_1.json",
         "profanity_batch_2.json",
         "profanity_batch_3.json",
         "hate.json",
         "scam.json",
         "spam.json"
      }) {
         try {
            InputStream in = CoreBootstrap.PLUGIN.getResource("wordlists/" + name);

            try {
               if (in == null) {
                  if (in != null) {
                     in.close();
                  }
               } else {
                  File out = new File(targetFolder, name);
                  FileOutputStream fos = new FileOutputStream(out);

                  try {
                     in.transferTo(fos);
                  } catch (Throwable var14) {
                     try {
                        fos.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }

                     throw var14;
                  }

                  fos.close();
                  if (in != null) {
                     in.close();
                  }
               }
            } catch (Throwable var15) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }
         } catch (IOException var16) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Failed to copy default wordlist: " + name);
         }
      }
   }

   public CategoryConfig getCategory(String name) {
      return this.categories.get(name.toLowerCase());
   }

   public Trie getTrie(String name) {
      return this.tries.get(name.toLowerCase());
   }

   public List<Pattern> getPatterns(String name) {
      return this.regexPatterns.getOrDefault(name.toLowerCase(), Collections.emptyList());
   }

   public Pattern getCompoundPattern(String name) {
      return this.compoundPatterns.get(name.toLowerCase());
   }

   public Set<String> getCategoryNames() {
      return new HashSet<>(this.categories.keySet());
   }

   public boolean hasCategory(String name) {
      return this.categories.containsKey(name.toLowerCase());
   }
}
