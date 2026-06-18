package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class BotChatListener implements Listener {
   private static final String[] TRIGGERS = new String[]{"kai", "hey kai", "bot", "ai", "hey bot", "hey ai"};

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onChat(AsyncPlayerChatEvent event) {
      String msg = event.getMessage().trim();
      String lower = msg.toLowerCase();
      boolean addressed = false;
      String commandPart = lower;

      for (String trigger : TRIGGERS) {
         if (lower.startsWith(trigger)) {
            addressed = true;
            commandPart = lower.substring(trigger.length()).trim();
            break;
         }
      }

      if (addressed) {
         Player player = event.getPlayer();
         WatcherCore core = WatcherCore.getInstance();
         if (core != null) {
            AIPlayerBot aiBot = core.getAIPlayerBot();
            if (aiBot != null && aiBot.isSpawned()) {
               String response = aiBot.handlePlayerChat(player, commandPart, msg);
               if (response != null && !response.isEmpty()) {
                  Bukkit.getScheduler().runTask(CoreBootstrap.PLUGIN, () -> {
                     Player botPlayer = aiBot.getNMSBot().getPlayer();
                     if (botPlayer != null) {
                        botPlayer.chat(response);
                     } else {
                        Bukkit.broadcastMessage(ChatColor.GRAY + "[Kai] " + ChatColor.WHITE + response);
                     }
                  });
               }
            } else {
               player.sendMessage(ChatColor.GRAY + "[Kai] " + ChatColor.RED + "Ich bin gerade nicht da...");
            }
         }
      }
   }
}
