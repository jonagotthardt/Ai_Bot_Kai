package com.jonasmp.ai.rules;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.core.LanguageSelectionGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RuleInventoryListener implements Listener {
   private static final String GUI_TITLE = "§c§lServer Regeln / Server Rules";

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         if (event.getClickedInventory() != null) {
            String title = event.getView().getTitle();
            if (title.equals("§c§lServer Regeln / Server Rules")) {
               event.setCancelled(true);
               if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                  String name = event.getCurrentItem().getItemMeta().getDisplayName();
                  if (!name.contains("✔") && !name.contains("Accept")) {
                     if (name.contains("✘") || name.contains("Decline")) {
                        player.closeInventory();
                        player.kick(Component.text("§cDu musst die Server-Regeln akzeptieren, um beizutreten.\n\n§7You must accept the server rules to join."));
                     }
                  } else {
                     CoreBootstrap.FIRST_JOIN_TRACKER.setAccepted(player.getUniqueId());
                     player.closeInventory();
                     player.sendMessage("§a§lRegeln akzeptiert! Willkommen auf Yona SMP!");
                     player.sendMessage("§7§lRules accepted! Welcome to Yona SMP!");
                     Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                        if (player.isOnline()) {
                           player.sendMessage("§e§lWillkommen! / Welcome!");
                           player.sendMessage("§7Bitte wähle deine Sprache / Please select your language:");
                           LanguageSelectionGUI.open(player);
                        }
                     }, 10L);
                  }
               }
            }
         }
      }
   }
}
