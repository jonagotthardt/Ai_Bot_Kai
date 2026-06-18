package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class LanguageChatListener implements Listener {
   private static final String SUFFIX_DE = " §7[§cDE§7]§r";
   private static final String SUFFIX_EN = " §7[§9EN§7]§r";

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onChat(AsyncPlayerChatEvent event) {
      if (CoreBootstrap.PLAYER_LANGUAGE_STORE != null) {
         Player player = event.getPlayer();
         String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId());
         String suffix = "en".equals(lang) ? " §7[§9EN§7]§r" : " §7[§cDE§7]§r";
         String format = event.getFormat();
         if (format.contains("%1$s") && format.contains("%2$s")) {
            format = format.replace("%1$s", "%1$s" + suffix);
         } else if (format.contains("%s")) {
            format = format.replace("%s", "%s" + suffix);
         }

         event.setFormat(format);
      }
   }
}
