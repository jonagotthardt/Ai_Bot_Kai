package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;

public class AIAnalyzer {
   private final AIPlayerBot bot;
   private final File analysisDir;
   private final File suggestionsDir;
   private final String apiKey;
   private final String model;

   public AIAnalyzer(AIPlayerBot bot, String apiKey, String model) {
      this.bot = bot;
      this.apiKey = apiKey;
      this.model = model;
      this.analysisDir = new File(CoreBootstrap.PLUGIN.getDataFolder(), "analysis");
      this.suggestionsDir = new File(CoreBootstrap.PLUGIN.getDataFolder(), "suggestions");
      if (!this.analysisDir.exists()) {
         this.analysisDir.mkdirs();
      }

      if (!this.suggestionsDir.exists()) {
         this.suggestionsDir.mkdirs();
      }
   }

   public void analyzeFile(String sourcePath, String context) {
      File srcFile = new File(sourcePath);
      if (!srcFile.exists()) {
         this.saveReport("error_" + System.currentTimeMillis() + ".txt", "File not found: " + sourcePath);
      } else {
         CompletableFuture.runAsync(
            () -> {
               try {
                  String code = new String(Files.readAllBytes(srcFile.toPath()), StandardCharsets.UTF_8);
                  String truncated = code.length() > 4000 ? code.substring(0, 4000) + "\n... [truncated, file is " + code.length() + " chars]" : code;
                  String prompt = this.buildAnalysisPrompt(sourcePath, truncated, context);
                  String response = this.bot.callOpenRouter(prompt, this.model);
                  if (response != null) {
                     String reportName = "analysis_" + srcFile.getName().replace(".java", "") + "_" + System.currentTimeMillis() + ".md";
                     this.saveReport(reportName, response);
                     Bukkit.getScheduler()
                        .runTask(CoreBootstrap.PLUGIN, () -> CoreBootstrap.PLUGIN.getLogger().info("[AIAnalyzer] Analysis saved: " + reportName));
                  }
               } catch (Exception var9) {
                  Bukkit.getScheduler()
                     .runTask(CoreBootstrap.PLUGIN, () -> CoreBootstrap.PLUGIN.getLogger().warning("[AIAnalyzer] Failed: " + var9.getMessage()));
               }
            }
         );
      }
   }

   public void selfDiagnose() {
      String[] array = new String[]{
         "F:\\JonaSMP_AI\\src\\main\\java\\com\\jonasmp\\ai\\watcher\\NMSBot.java",
         "F:\\JonaSMP_AI\\src\\main\\java\\com\\jonasmp\\ai\\watcher\\AIPlayerBot.java",
         "F:\\JonaSMP_AI\\src\\main\\java\\com\\jonasmp\\ai\\watcher\\BotGoalPlanner.java"
      };

      for (String file : array) {
         this.analyzeFile(file, "Self-diagnosis: check for common Minecraft bot bugs (floating, stuck, recursion, infinite loops, NMS compatibility).");
      }
   }

   public void suggestFix(String problemDescription, String affectedFile) {
      File srcFile = new File(affectedFile);
      if (srcFile.exists()) {
         CompletableFuture.runAsync(
            () -> {
               try {
                  String code = new String(Files.readAllBytes(srcFile.toPath()), StandardCharsets.UTF_8);
                  String truncated = code.length() > 3000 ? code.substring(0, 3000) + "\n..." : code;
                  String prompt = this.buildFixPrompt(problemDescription, affectedFile, truncated);
                  String response = this.bot.callOpenRouter(prompt, this.model);
                  if (response != null) {
                     String name = "suggestion_" + srcFile.getName().replace(".java", "") + "_" + System.currentTimeMillis() + ".md";
                     this.saveSuggestion(name, response);
                     Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> CoreBootstrap.PLUGIN.getLogger().info("[AIAnalyzer] Suggestion saved: " + name));
                  }
               } catch (Exception var9) {
                  Bukkit.getScheduler()
                     .runTask(CoreBootstrap.PLUGIN, () -> CoreBootstrap.PLUGIN.getLogger().warning("[AIAnalyzer] Suggest fix failed: " + var9.getMessage()));
               }
            }
         );
      }
   }

   public List<String> listReports() {
      List<String> result = new ArrayList<>();
      File[] reports = this.analysisDir.listFiles((d, n) -> n.endsWith(".md") || n.endsWith(".txt"));
      if (reports != null) {
         for (File f : reports) {
            result.add("[ANALYSIS] " + f.getName());
         }
      }

      File[] suggs = this.suggestionsDir.listFiles((d, n) -> n.endsWith(".md") || n.endsWith(".txt"));
      if (suggs != null) {
         for (File f2 : suggs) {
            result.add("[SUGGESTION] " + f2.getName());
         }
      }

      return result;
   }

   private String buildAnalysisPrompt(String filePath, String code, String context) {
      return "You are an expert Minecraft plugin developer reviewing Java code for a Paper/NMS fake-player bot.\n\nFILE:"
         + filePath
         + "\nCONTEXT: "
         + context
         + "\n\nCODE:\n```java\n"
         + code
         + "\n```\n\nAnalyze this code for:\n1. Bugs (NullPointerException, infinite loops, StackOverflowError)\n2. NMS compatibility issues (wrong method names, missing reflection)\n3. Performance problems (inefficient loops, redundant calls)\n4. Logic errors (wrong conditions, missing edge cases)\n\nFor each issue, provide:\n- Line number or method name\n- What the bug is\n- Suggested fix (as a code snippet)\n\nFormat as markdown. Be specific and actionable.";
   }

   private String buildFixPrompt(String problem, String filePath, String code) {
      return "You are an expert Minecraft plugin developer. Fix this bug:\n\nPROBLEM:"
         + problem
         + "\nFILE: "
         + filePath
         + "\n\nCODE:\n```java\n"
         + code
         + "\n```\n\nProvide the FIXED version of the affected code section. Only output the corrected code, no explanations.";
   }

   private void saveReport(String name, String content) {
      try {
         File f = new File(this.analysisDir, name);
         Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
      } catch (Exception var4) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIAnalyzer] Save failed: " + var4.getMessage());
      }
   }

   private void saveSuggestion(String name, String content) {
      try {
         File f = new File(this.suggestionsDir, name);
         Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
      } catch (Exception var4) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIAnalyzer] Save suggestion failed: " + var4.getMessage());
      }
   }
}
