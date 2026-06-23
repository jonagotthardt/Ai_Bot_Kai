package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class LanguagePrefixManager {
   private static final String TEAM_PREFIX = "lpm_";
   private static JavaPlugin plugin;
   private static String BOT_NAME = null;
   private static final Map<String, String> SUFFIX_MAP = new HashMap<>();
   private static final Map<UUID, String> CURRENT_LP_SUFFIX = new HashMap<>();

   public static void init(JavaPlugin pl) {
      plugin = pl;
      Bukkit.getScheduler().runTaskLater(pl, () -> {
         syncAllOnline();
         startGuardTask();
      }, 5L);
   }

   public static void setBotName(String name) {
      BOT_NAME = name;
      CoreBootstrap.PLUGIN.getLogger().info("[LanguagePrefixManager] Bot name registered: " + name);
   }

   public static void syncAllOnline() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         updatePlayerPrefix(p);
      }
   }

   public static void updatePlayerPrefix(Player player) {
      if (player != null && player.isOnline()) {
         String suffix;
         if (BOT_NAME != null && BOT_NAME.equalsIgnoreCase(player.getName())) {
            suffix = " §7[§5BOT§7]§r";
         } else {
            String lang = "de";
            if (CoreBootstrap.PLAYER_LANGUAGE_STORE != null) {
               lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId());
            }

            suffix = SUFFIX_MAP.getOrDefault(lang, SUFFIX_MAP.get("de"));
         }

         Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
         String teamName = "lpm_" + player.getUniqueId().toString().replace("-", "").substring(0, 12);
         Team team = board.getTeam(teamName);
         if (team == null) {
            team = board.registerNewTeam(teamName);
         }

         if (!suffix.equals(team.getSuffix())) {
            team.setSuffix(suffix);
         }

         updateLuckPermsSuffix(player, suffix);
         if (!team.hasEntry(player.getName())) {
            for (Team t : board.getTeams()) {
               if (t.hasEntry(player.getName()) && !t.getName().equals(teamName)) {
                  t.removeEntry(player.getName());
               }
            }

            team.addEntry(player.getName());
         }
      }
   }

   private static void updateLuckPermsSuffix(Player player, String suffix) {
      UUID uuid = player.getUniqueId();
      String current = CURRENT_LP_SUFFIX.get(uuid);
      if (!suffix.equals(current)) {
         CURRENT_LP_SUFFIX.put(uuid, suffix);
         if (plugin != null) {
            String cmd = "lp user " + player.getName() + " meta setsuffix \"" + suffix;
            Bukkit.getScheduler().runTask(plugin, () -> {
               try {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
               } catch (Exception var2x) {
               }
            });
         }
      }
   }

   private static void startGuardTask() {
      (new BukkitRunnable() {
         public void run() {
            LanguagePrefixManager.syncAllOnline();
         }
      }).runTaskTimer(plugin, 60L, 60L);
   }

   static {
      SUFFIX_MAP.put("de", " §7[§cDE§7]§r");
      SUFFIX_MAP.put("en", " §7[§9EN§7]§r");
   }
}
