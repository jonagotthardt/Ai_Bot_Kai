package com.jonasmp.ai.rules;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RuleAcceptanceGUI {
   private static final String TITLE = "§c§lServer Regeln / Server Rules";

   public static void open(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 36, "§c§lServer Regeln / Server Rules");
      inv.setItem(
         10,
         createRuleItem(
            Material.BOOK,
            "§e§l1. Respektiere alle Spieler",
            Arrays.asList(
               "§7Beleidigungen, Hassrede und",
               "§7Mobbing werden nicht toleriert.",
               "§7Verwarnung bis BAN.",
               "§f",
               "§7Respect all players. No insults,",
               "§7hate speech or harassment."
            )
         )
      );
      inv.setItem(
         12,
         createRuleItem(
            Material.BOOK,
            "§e§l2. Kein Griefing / Hacking",
            Arrays.asList(
               "§7Keine unerlaubten Modifikationen",
               "§7und kein Zerstören fremder Bauten.",
               "§7Permanenter BAN.",
               "§f",
               "§7No cheating or griefing others' builds.",
               "§7Permanent BAN."
            )
         )
      );
      inv.setItem(
         14,
         createRuleItem(
            Material.BOOK,
            "§e§l3. Kein Spam / Werbung",
            Arrays.asList(
               "§7Wiederholtes Spamming und",
               "§7Werbung für andere Server ist",
               "§7verboten. Kick bis Mute.",
               "§f",
               "§7No spamming or advertising.",
               "§7Kick to Mute."
            )
         )
      );
      inv.setItem(
         16,
         createRuleItem(
            Material.BOOK,
            "§e§l4. AFK-Regeln beachten",
            Arrays.asList(
               "§7AFK-Farmen sind erlaubt, aber",
               "§7bitte lagfrei halten.",
               "§7Auto-Clicker sind verboten.",
               "§f",
               "§7AFK farms allowed, keep it lag-free.",
               "§7Auto-clickers forbidden."
            )
         )
      );
      inv.setItem(
         20,
         createRuleItem(
            Material.BOOK,
            "§e§l5. Chat-Moderation aktiv",
            Arrays.asList(
               "§7KI-gestützte Chat-Moderation",
               "§7ist aktiv. Beleidigungen werden",
               "§7automatisch erkannt.",
               "§f",
               "§7AI chat moderation is active.",
               "§7Insults are auto-detected."
            )
         )
      );
      inv.setItem(
         22,
         createRuleItem(
            Material.BOOK,
            "§e§l6. Admin hat letztes Wort",
            Arrays.asList(
               "§7Admins entscheiden in Streitfällen.",
               "§7Diskussionen per Discord.",
               "§7Entscheidungen sind endgültig.",
               "§f",
               "§7Admin decisions are final.",
               "§7Discuss via Discord."
            )
         )
      );
      ItemStack accept = new ItemStack(Material.LIME_WOOL);
      ItemMeta acceptMeta = accept.getItemMeta();
      if (acceptMeta != null) {
         acceptMeta.setDisplayName("§a§l✔ Regeln akzeptieren / Accept Rules");
         acceptMeta.setLore(
            Arrays.asList("§7Klicke hier, um die Regeln zu akzeptieren", "§7und auf den Server zu kommen.", "§f", "§7Click to accept and join the server.")
         );
         accept.setItemMeta(acceptMeta);
      }

      inv.setItem(32, accept);
      ItemStack decline = new ItemStack(Material.RED_WOOL);
      ItemMeta declineMeta = decline.getItemMeta();
      if (declineMeta != null) {
         declineMeta.setDisplayName("§c§l✘ Regeln ablehnen / Decline Rules");
         declineMeta.setLore(Arrays.asList("§7Du wirst vom Server gekickt.", "§f", "§7You will be kicked from the server."));
         decline.setItemMeta(declineMeta);
      }

      inv.setItem(30, decline);
      ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta fm = filler.getItemMeta();
      if (fm != null) {
         fm.setDisplayName(" ");
         filler.setItemMeta(fm);
      }

      for (int i = 0; i < 36; i++) {
         if (inv.getItem(i) == null) {
            inv.setItem(i, filler);
         }
      }

      player.openInventory(inv);
   }

   private static ItemStack createRuleItem(Material mat, String name, List<String> lore) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }
}
