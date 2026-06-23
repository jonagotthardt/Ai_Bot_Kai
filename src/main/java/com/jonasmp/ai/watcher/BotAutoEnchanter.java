package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class BotAutoEnchanter {
   private final NMSBot nmsBot;
   private int enchantCooldown = 0;
   private int scanCooldown = 0;
   private static final int ENCHANT_COOLDOWN_TICKS = 5;
   private static final int SCAN_INTERVAL_TICKS = 200;
   private static final Map<Enchantment, Integer> BASE_ARMOR_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> HELMET_EXTRA_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> BOOTS_EXTRA_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> SWORD_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> MACE_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> PICKAXE_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> AXE_ENCHANTS = new HashMap<>();
   private static final Map<Enchantment, Integer> SHIELD_ENCHANTS = new HashMap<>();

   public BotAutoEnchanter(NMSBot nmsBot) {
      this.nmsBot = nmsBot;
   }

   public void tick(Player bot) {
      if (this.enchantCooldown > 0) {
         this.enchantCooldown--;
      } else {
         this.scanCooldown--;
         if (this.scanCooldown <= 0) {
            this.scanCooldown = 200;
            this.scanAndEnchantAll(bot);
         }
      }
   }

   public void scanAndEnchantAll(Player bot) {
      PlayerInventory inv = bot.getInventory();
      int enchantedCount = 0;
      ItemStack[] armor = new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};
      String[] armorNames = new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

      for (int i = 0; i < armor.length; i++) {
         if (this.enchantItem(armor[i], armorNames[i])) {
            enchantedCount++;
         }
      }

      for (ItemStack item : inv.getContents()) {
         if (item != null && item.getType() != Material.AIR) {
            String type = item.getType().name();
            if (type.contains("SWORD")) {
               if (this.enchantItem(item, "SWORD")) {
                  enchantedCount++;
               }
            } else if (type.equals("MACE")) {
               if (this.enchantItem(item, "MACE")) {
                  enchantedCount++;
               }
            } else if (type.contains("PICKAXE")) {
               if (this.enchantItem(item, "PICKAXE")) {
                  enchantedCount++;
               }
            } else if (type.contains("AXE") && !type.contains("PICKAXE")) {
               if (this.enchantItem(item, "AXE")) {
                  enchantedCount++;
               }
            } else if (type.contains("SHIELD") && this.enchantItem(item, "SHIELD")) {
               enchantedCount++;
            }
         }
      }

      if (enchantedCount > 0) {
         CoreBootstrap.PLUGIN.getLogger().info("[BotAutoEnchanter] Scanned and enchanted " + enchantedCount + " items");
      }
   }

   public boolean enchantItem(ItemStack item, String itemTypeHint) {
      if (item == null || item.getType() == Material.AIR) {
         return false;
      } else if (this.isAlreadyEnchanted(item)) {
         return false;
      } else if (this.dispatchEnchantCommand(item)) {
         // Routed through the server's /enchant command (legit, costs the bot resources).
         return true;
      } else {
         String type = item.getType().name();
         Map<Enchantment, Integer> toApply = new HashMap<>();
         if (type.contains("HELMET") || type.contains("CHESTPLATE") || type.contains("LEGGINGS") || type.contains("BOOTS")) {
            toApply.putAll(BASE_ARMOR_ENCHANTS);
            if (type.contains("HELMET")) {
               toApply.putAll(HELMET_EXTRA_ENCHANTS);
            }

            if (type.contains("BOOTS")) {
               toApply.putAll(BOOTS_EXTRA_ENCHANTS);
            }
         } else if (type.contains("SWORD")) {
            toApply.putAll(SWORD_ENCHANTS);
         } else if (type.equals("MACE")) {
            toApply.putAll(MACE_ENCHANTS);
         } else if (type.contains("PICKAXE")) {
            toApply.putAll(PICKAXE_ENCHANTS);
         } else if (type.contains("AXE") && !type.contains("PICKAXE")) {
            toApply.putAll(AXE_ENCHANTS);
         } else if (type.contains("SHIELD")) {
            toApply.putAll(SHIELD_ENCHANTS);
         }

         if (toApply.isEmpty()) {
            return false;
         } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
               return false;
            } else {
               int applied = 0;
               int failed = 0;

               for (Entry<Enchantment, Integer> entry : toApply.entrySet()) {
                  try {
                     Enchantment enchant = entry.getKey();
                     int level = entry.getValue();
                     if (enchant != null && !meta.hasEnchant(enchant)) {
                        meta.addEnchant(enchant, level, true);
                        applied++;
                     }
                  } catch (Exception var12) {
                     failed++;
                  }
               }

               if (applied > 0) {
                  item.setItemMeta(meta);
                  this.enchantCooldown = 5;
                  CoreBootstrap.PLUGIN.getLogger().info("[BotAutoEnchanter] ENCHANT: item=" + type + ", enchants=" + applied + "/" + (applied + failed));
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   public void enchantCurrentItem(Player bot) {
      ItemStack hand = bot.getInventory().getItemInMainHand();
      if (hand != null && hand.getType() != Material.AIR) {
         String type = hand.getType().name();
         if (type.contains("SWORD")) {
            this.enchantItem(hand, "SWORD");
         } else if (type.contains("HELMET")) {
            this.enchantItem(hand, "HELMET");
         } else if (type.contains("CHESTPLATE")) {
            this.enchantItem(hand, "CHESTPLATE");
         } else if (type.contains("LEGGINGS")) {
            this.enchantItem(hand, "LEGGINGS");
         } else if (type.contains("BOOTS")) {
            this.enchantItem(hand, "BOOTS");
         } else if (type.equals("MACE")) {
            this.enchantItem(hand, "MACE");
         } else if (type.contains("PICKAXE")) {
            this.enchantItem(hand, "PICKAXE");
         } else if (type.contains("AXE") && !type.contains("PICKAXE")) {
            this.enchantItem(hand, "AXE");
         } else if (type.contains("SHIELD")) {
            this.enchantItem(hand, "SHIELD");
         }
      }
   }

   /**
    * When {@code bot.shop.use_enchant_command} is set, enchants the item by running the configured
    * server command (default {@code /enchant {item}}) as the bot, instead of applying enchants
    * directly. One command per cooldown window so a full-gear scan does not spam the server.
    * Returns true when a command was dispatched (caller then stops the direct-enchant fallback).
    */
   private boolean dispatchEnchantCommand(ItemStack item) {
      FileConfiguration cfg = CoreBootstrap.PLUGIN.getConfig();
      if (!cfg.getBoolean("bot.shop.use_enchant_command", true)) {
         return false;
      }
      if (this.enchantCooldown > 0) {
         // Skip this item this pass; do NOT fall through to direct enchanting.
         return true;
      }
      Player bot = this.nmsBot.getPlayer();
      if (bot == null) {
         return false;
      }
      String cmd = cfg.getString("bot.shop.enchant_command", "enchant {item}")
         .replace("{item}", item.getType().name());
      try {
         bot.performCommand(cmd);
         this.enchantCooldown = 10;
         CoreBootstrap.PLUGIN.getLogger().info("[BotAutoEnchanter] Enchant command: /" + cmd);
         return true;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[BotAutoEnchanter] Enchant command failed: " + ex.getMessage());
         this.enchantCooldown = 16;
         return true;
      }
   }

   public boolean isAlreadyEnchanted(ItemStack item) {
      if (item == null) {
         return false;
      } else {
         ItemMeta meta = item.getItemMeta();
         return meta == null ? false : !meta.getEnchants().isEmpty();
      }
   }

   public int getEnchantCount(ItemStack item) {
      if (item == null) {
         return 0;
      } else {
         ItemMeta meta = item.getItemMeta();
         return meta == null ? 0 : meta.getEnchants().size();
      }
   }

   static {
      try {
         BASE_ARMOR_ENCHANTS.put(Enchantment.PROTECTION, 4);
         BASE_ARMOR_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         BASE_ARMOR_ENCHANTS.put(Enchantment.MENDING, 1);
      } catch (Exception var8) {
      }

      try {
         HELMET_EXTRA_ENCHANTS.put(Enchantment.RESPIRATION, 3);
         HELMET_EXTRA_ENCHANTS.put(Enchantment.AQUA_AFFINITY, 1);
      } catch (Exception var7) {
      }

      try {
         BOOTS_EXTRA_ENCHANTS.put(Enchantment.DEPTH_STRIDER, 3);
         BOOTS_EXTRA_ENCHANTS.put(Enchantment.FEATHER_FALLING, 4);
      } catch (Exception var6) {
      }

      try {
         SWORD_ENCHANTS.put(Enchantment.SHARPNESS, 5);
         SWORD_ENCHANTS.put(Enchantment.FIRE_ASPECT, 2);
         SWORD_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         SWORD_ENCHANTS.put(Enchantment.MENDING, 1);
      } catch (Exception var5) {
      }

      try {
         MACE_ENCHANTS.put(Enchantment.SHARPNESS, 5);
         MACE_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         MACE_ENCHANTS.put(Enchantment.MENDING, 1);
      } catch (Exception var4) {
      }

      try {
         PICKAXE_ENCHANTS.put(Enchantment.EFFICIENCY, 5);
         PICKAXE_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         PICKAXE_ENCHANTS.put(Enchantment.MENDING, 1);
         PICKAXE_ENCHANTS.put(Enchantment.FORTUNE, 3);
      } catch (Exception var3) {
      }

      try {
         AXE_ENCHANTS.put(Enchantment.SHARPNESS, 5);
         AXE_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         AXE_ENCHANTS.put(Enchantment.MENDING, 1);
      } catch (Exception var2) {
      }

      try {
         SHIELD_ENCHANTS.put(Enchantment.UNBREAKING, 3);
         SHIELD_ENCHANTS.put(Enchantment.MENDING, 1);
      } catch (Exception var1) {
      }
   }
}
