package com.jonasmp.ai.gateway;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.config.DebugConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchTool {
   private static final int TIMEOUT_MS = 6000;
   private static final int MAX_RESULTS = 4;
   private static final int MAX_SNIPPET_LEN = 300;
   private static final Pattern DDG_RESULT = Pattern.compile(
      "<div[^>]*class=\"[^\"]*result[^\"]*\">.*?<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*>(.*?)</a>.*?<a[^>]*class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</a>.*?</div>",
      34
   );
   private static volatile String lastQuery = null;
   private static volatile String lastHtmlPreview = null;
   private static volatile List<WebSearchTool.SearchResult> lastResults = null;
   private static volatile String lastContext = null;

   public static String getLastQuery() {
      return lastQuery;
   }

   public static String getLastHtmlPreview() {
      return lastHtmlPreview;
   }

   public static List<WebSearchTool.SearchResult> getLastResults() {
      return lastResults;
   }

   public static String getLastContext() {
      return lastContext;
   }

   public String searchForContext(String query) {
      List<WebSearchTool.SearchResult> results = lastResults = this.search(query);
      if (results.isEmpty()) {
         lastContext = null;
         return null;
      } else {
         StringBuilder ctx = new StringBuilder("Aktuelle Web-Suchergebnisse zum Thema:\n");

         for (int i = 0; i < results.size(); i++) {
            WebSearchTool.SearchResult r = results.get(i);
            ctx.append("[")
               .append(i + 1)
               .append("] ")
               .append(r.title.replaceAll("<[^>]+>", "").trim())
               .append(": ")
               .append(r.snippet.replaceAll("<[^>]+>", "").trim())
               .append("\n");
         }

         ctx.append("Nutze diese Informationen, um aktuell und korrekt zu antworten.");
         return lastContext = ctx.toString();
      }
   }

   public List<WebSearchTool.SearchResult> search(String query) {
      if (query != null && !query.isBlank()) {
         String q = lastQuery = this.sanitizeQuery(query);
         new ArrayList();
         List<WebSearchTool.SearchResult> results = this.searchDdgInstantAnswer(q);
         if (!results.isEmpty()) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] WebSearch: DDG Instant Answer returned " + results.size() + " results");
            }

            return results;
         } else {
            results = this.searchDdgHtml(q);
            if (!results.isEmpty()) {
               if (DebugConfig.isDebugEnabled()) {
                  CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] WebSearch: DDG HTML returned " + results.size() + " results");
               }

               return results;
            } else {
               results = this.searchBing(q);
               if (DebugConfig.isDebugEnabled()) {
                  CoreBootstrap.PLUGIN.getLogger().info("[AI-DEBUG] WebSearch: Bing returned " + results.size() + " results");
               }

               return results;
            }
         }
      } else {
         return List.of();
      }
   }

   private List<WebSearchTool.SearchResult> searchDdgInstantAnswer(String query) {
      List<WebSearchTool.SearchResult> results = new ArrayList<>();

      try {
         String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
         URL url = new URL("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1");
         HttpURLConnection conn = (HttpURLConnection)url.openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
         conn.setConnectTimeout(6000);
         conn.setReadTimeout(6000);
         int code = conn.getResponseCode();
         if (code < 200 || code >= 300) {
            return results;
         }

         StringBuilder json = new StringBuilder();
         BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

         String line;
         try {
            while ((line = br.readLine()) != null) {
               json.append(line);
            }
         } catch (Throwable var16) {
            try {
               br.close();
            } catch (Throwable var15) {
               var16.addSuppressed(var15);
            }

            throw var16;
         }

         br.close();
         JsonObject obj = JsonParser.parseString(json.toString()).getAsJsonObject();
         line = obj.has("AbstractText") ? obj.get("AbstractText").getAsString() : "";
         String abstractSource = obj.has("AbstractSource") ? obj.get("AbstractSource").getAsString() : "DuckDuckGo";
         if (!line.isEmpty()) {
            results.add(new WebSearchTool.SearchResult(abstractSource + " (Instant Answer)", line, ""));
         }

         if (obj.has("RelatedTopics")) {
            JsonArray topics = obj.getAsJsonArray("RelatedTopics");

            for (int i = 0; i < topics.size() && results.size() < 4; i++) {
               JsonObject topic = topics.get(i).getAsJsonObject();
               if (topic.has("Text")) {
                  String text = topic.get("Text").getAsString();
                  if (!text.isEmpty()) {
                     results.add(new WebSearchTool.SearchResult("Related", text, ""));
                  }
               }
            }
         }
      } catch (Exception var17) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] DDG Instant Answer failed: " + var17.getMessage());
         }
      }

      return results;
   }

   private List<WebSearchTool.SearchResult> searchDdgHtml(String query) {
      List<WebSearchTool.SearchResult> results = new ArrayList<>();

      for (String base : new String[]{"https://html.duckduckgo.com/html/?q=", "https://lite.duckduckgo.com/lite/?q="}) {
         try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URL url = new URL(base + encoded);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty(
               "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
            );
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,de;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Referer", "https://duckduckgo.com/");
            conn.setRequestProperty("DNT", "1");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
               StringBuilder html = new StringBuilder();
               BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

               String line;
               try {
                  while ((line = br.readLine()) != null) {
                     html.append(line).append("\n");
                  }
               } catch (Throwable var21) {
                  try {
                     br.close();
                  } catch (Throwable var20) {
                     var21.addSuppressed(var20);
                  }

                  throw var21;
               }

               br.close();
               String htmlStr = html.toString();
               lastHtmlPreview = htmlStr.length() > 2000 ? htmlStr.substring(0, 2000) + "..." : htmlStr;
               if (base.contains("/lite/")) {
                  Pattern liteLink = Pattern.compile("<a[^>]+class=\"result-link\"[^>]*>(.*?)</a>", 34);
                  Pattern liteSnippet = Pattern.compile("<td[^>]+class=\"result-snippet\"[^>]*>(.*?)</td>", 34);
                  Matcher linkM = liteLink.matcher(htmlStr);
                  Matcher snipM = liteSnippet.matcher(htmlStr);

                  while (linkM.find() && snipM.find() && results.size() < 4) {
                     String title = this.stripHtml(linkM.group(1)).trim();
                     String snippet = this.stripHtml(snipM.group(1)).trim();
                     if (!snippet.isEmpty()) {
                        results.add(new WebSearchTool.SearchResult(this.decodeHtmlEntities(title), this.decodeHtmlEntities(snippet), ""));
                     }
                  }
               } else {
                  Matcher m = DDG_RESULT.matcher(htmlStr);

                  while (m.find() && results.size() < 4) {
                     String title2 = this.stripHtml(m.group(1).trim());
                     String snippet2 = this.stripHtml(m.group(2).trim());
                     if (!snippet2.isEmpty()) {
                        results.add(new WebSearchTool.SearchResult(this.decodeHtmlEntities(title2), this.decodeHtmlEntities(snippet2), ""));
                     }
                  }
               }

               if (!results.isEmpty()) {
                  break;
               }
            }
         } catch (Exception var22) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] DDG HTML endpoint failed: " + var22.getMessage());
            }
         }
      }

      return results;
   }

   private List<WebSearchTool.SearchResult> searchBing(String query) {
      List<WebSearchTool.SearchResult> results = new ArrayList<>();

      try {
         String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
         URL url = new URL("https://www.bing.com/search?q=" + encoded + "&count=4");
         HttpURLConnection conn = (HttpURLConnection)url.openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
         );
         conn.setRequestProperty("Accept", "text/html,*/*");
         conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
         conn.setRequestProperty("Accept-Encoding", "identity");
         conn.setRequestProperty("Referer", "https://www.bing.com/");
         conn.setInstanceFollowRedirects(true);
         conn.setConnectTimeout(6000);
         conn.setReadTimeout(6000);
         int code = conn.getResponseCode();
         if (code < 200 || code >= 300) {
            if (DebugConfig.isDebugEnabled()) {
               CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Bing HTTP " + code);
            }

            return results;
         }

         StringBuilder html = new StringBuilder();

         String line;
         try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
               html.append(line).append("\n");
            }
         }

         String var16 = html.toString();
         Pattern bingResult = Pattern.compile(
            "<li[^>]*class=\"b_algo\"[^>]*>.*?<h2>.*?<a[^>]*>(.*?)</a>.*?</h2>.*?<div[^>]*class=\"b_caption\"[^>]*>.*?<p>(.*?)</p>.*?</div>.*?</li>", 34
         );
         Matcher m = bingResult.matcher(var16);

         while (m.find() && results.size() < 4) {
            String title = this.stripHtml(m.group(1)).trim();
            String snippet = this.stripHtml(m.group(2)).trim();
            if (!snippet.isEmpty()) {
               results.add(new WebSearchTool.SearchResult(this.decodeHtmlEntities(title), this.decodeHtmlEntities(snippet), ""));
            }
         }
      } catch (Exception var15) {
         if (DebugConfig.isDebugEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI-DEBUG] Bing search failed: " + var15.getMessage());
         }
      }

      return results;
   }

   private String stripHtml(String html) {
      return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
   }

   private String sanitizeQuery(String query) {
      String q = query.replaceAll("^/(ai|frage|ask|search)\\s+", "");
      q = q.replaceAll("(?i)^(suche (im internet|im web|auf google|auf duckduckgo|danach|nach))\\s*", "");
      q = q.replaceAll("(?i)^(google|duckduckgo|suche|finde|schau)\\s+(nach|im internet|im web|\\b)\\s*", "");
      q = q.replaceAll("(?i)(\\b(suche|google|finde)\\s+(mir|bitte|mal)?\\s+(nach|danach))\\s*", "");
      q = q.replaceAll("@[A-Za-z0-9_]+", "");
      q = q.replaceAll("§[0-9a-fk-or]", "");
      return q.trim();
   }

   private String decodeHtmlEntities(String text) {
      return text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
   }

   public static class SearchResult {
      public final String title;
      public final String snippet;
      public final String url;

      public SearchResult(String title, String snippet, String url) {
         this.title = title;
         this.snippet = snippet;
         this.url = url;
      }
   }
}
