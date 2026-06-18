package com.jonasmp.ai.core;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LanguageSelectionGUI {
   private static final String TITLE = "§8§lWähle deine Sprache / Select your language";

   public static void open(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, "§8§lWähle deine Sprache / Select your language");
      ItemStack de = new ItemStack(Material.RED_BANNER);
      ItemMeta deMeta = de.getItemMeta();
      if (deMeta != null) {
         deMeta.setDisplayName("§c§lDeutsch");
         deMeta.setLore(Arrays.asList("§7Klicke hier für Deutsch", "§7", "§7Dein Prefix wird: §7[§cDE§7]"));
         de.setItemMeta(deMeta);
      }

      inv.setItem(11, de);
      ItemStack en = new ItemStack(Material.BLUE_BANNER);
      ItemMeta enMeta = en.getItemMeta();
      if (enMeta != null) {
         enMeta.setDisplayName("§9§lEnglish");
         enMeta.setLore(Arrays.asList("§7Click here for English", "§7", "§7Your prefix will be: §7[§9EN§7]"));
         en.setItemMeta(enMeta);
      }

      inv.setItem(15, en);
      ItemStack info = new ItemStack(Material.BOOK);
      ItemMeta infoMeta = info.getItemMeta();
      if (infoMeta != null) {
         infoMeta.setDisplayName("§e§lWähle deine Sprache");
         infoMeta.setLore(Arrays.asList("§7Wähle die Sprache, in der du", "§7mit dem Server kommunizierst.", "§7", "§7[DE] = Deutsch", "§7[EN] = English"));
         info.setItemMeta(infoMeta);
      }

      inv.setItem(13, info);
      ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta fm = filler.getItemMeta();
      if (fm != null) {
         fm.setDisplayName(" ");
         filler.setItemMeta(fm);
      }

      for (int i = 0; i < 27; i++) {
         if (inv.getItem(i) == null) {
            inv.setItem(i, filler);
         }
      }

      player.openInventory(inv);
   }
}
