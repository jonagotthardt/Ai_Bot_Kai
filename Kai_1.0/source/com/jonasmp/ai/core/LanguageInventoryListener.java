package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class LanguageInventoryListener implements Listener {
   private static final String GUI_TITLE = "§8§lWähle deine Sprache / Select your language";

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         if (event.getClickedInventory() != null) {
            String title = event.getView().getTitle();
            if (title.equals("§8§lWähle deine Sprache / Select your language")) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                  String name = event.getCurrentItem().getItemMeta().getDisplayName();
                  if (name.contains("Deutsch")) {
                     CoreBootstrap.PLAYER_LANGUAGE_STORE.setLanguage(player.getUniqueId(), "de");
                     player.sendMessage("§a[AI] Sprache auf §c§lDEUTSCH§a gesetzt.");
                     LanguagePrefixManager.updatePlayerPrefix(player);
                     player.closeInventory();
                  } else if (name.contains("English")) {
                     CoreBootstrap.PLAYER_LANGUAGE_STORE.setLanguage(player.getUniqueId(), "en");
                     player.sendMessage("§a[AI] Language set to §9§lENGLISH§a.");
                     LanguagePrefixManager.updatePlayerPrefix(player);
                     player.closeInventory();
                  }
               }
            }
         }
      }
   }
}
