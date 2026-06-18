package com.jonasmp.ai.watcher;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class CraftingPlanner {
   private static final int MAX_STICKS = 8;
   private static final int MAX_PLANKS = 32;
   private static final int MIN_LOGS = 4;
   private static final int MIN_PLANKS = 8;
   private static final double MIN_HP_ARMOR = 10.0;
   private static final List<Material> LOGS = Arrays.asList(
      Material.OAK_LOG,
      Material.BIRCH_LOG,
      Material.SPRUCE_LOG,
      Material.JUNGLE_LOG,
      Material.ACACIA_LOG,
      Material.DARK_OAK_LOG,
      Material.MANGROVE_LOG,
      Material.CHERRY_LOG
   );
   private static final List<Material> PLANKS = Arrays.asList(
      Material.OAK_PLANKS,
      Material.SPRUCE_PLANKS,
      Material.BIRCH_PLANKS,
      Material.JUNGLE_PLANKS,
      Material.ACACIA_PLANKS,
      Material.DARK_OAK_PLANKS,
      Material.MANGROVE_PLANKS,
      Material.CHERRY_PLANKS,
      Material.BAMBOO_PLANKS
   );
   private static final List<Material> WOOLS = Arrays.asList(
      Material.WHITE_WOOL,
      Material.ORANGE_WOOL,
      Material.MAGENTA_WOOL,
      Material.LIGHT_BLUE_WOOL,
      Material.YELLOW_WOOL,
      Material.LIME_WOOL,
      Material.PINK_WOOL,
      Material.GRAY_WOOL,
      Material.LIGHT_GRAY_WOOL,
      Material.CYAN_WOOL,
      Material.PURPLE_WOOL,
      Material.BLUE_WOOL,
      Material.BROWN_WOOL,
      Material.GREEN_WOOL,
      Material.RED_WOOL,
      Material.BLACK_WOOL
   );

   private CraftingPlanner() {
   }

   public static void craftIfNeeded(Player bot) {
      PlayerInventory inv = bot.getInventory();
      int totalLogs = countAny(inv, LOGS);
      int totalPlanks = countAny(inv, PLANKS);
      if (totalLogs > 4 && totalPlanks < 32) {
         for (Material log : LOGS) {
            int logCount = countItem(inv, log);
            if (logCount >= 1) {
               if (totalLogs - 1 >= 4) {
                  Material plank = Material.valueOf(log.name().replace("_LOG", "_PLANKS"));
                  removeItems(inv, log, 1);
                  inv.addItem(new ItemStack[]{new ItemStack(plank, 4)});
                  return;
               }
               break;
            }
         }
      }

      int sticks = countItem(inv, Material.STICK);
      if (sticks < 8 && totalPlanks >= 10) {
         removeAnyPlanks(inv, 2);
         inv.addItem(new ItemStack[]{new ItemStack(Material.STICK, 4)});
      } else if (totalPlanks >= 2 && sticks >= 1 && !hasItem(bot, Material.WOODEN_SWORD) && reserveOK(totalLogs, totalPlanks, 2, 0)) {
         removeAnyPlanks(inv, 2);
         removeItems(inv, Material.STICK, 1);
         inv.addItem(new ItemStack[]{new ItemStack(Material.WOODEN_SWORD)});
      } else if (totalPlanks >= 3 && sticks >= 2 && !hasItem(bot, Material.WOODEN_AXE) && reserveOK(totalLogs, totalPlanks, 3, 2)) {
         removeAnyPlanks(inv, 3);
         removeItems(inv, Material.STICK, 2);
         inv.addItem(new ItemStack[]{new ItemStack(Material.WOODEN_AXE)});
      } else if (totalPlanks >= 3 && sticks >= 2 && !hasItem(bot, Material.WOODEN_PICKAXE) && reserveOK(totalLogs, totalPlanks, 3, 2)) {
         removeAnyPlanks(inv, 3);
         removeItems(inv, Material.STICK, 2);
         inv.addItem(new ItemStack[]{new ItemStack(Material.WOODEN_PICKAXE)});
      } else {
         int cobble = countItem(inv, Material.COBBLESTONE);
         if (cobble >= 2 && sticks >= 1 && !hasItem(bot, Material.STONE_SWORD)) {
            removeItems(inv, Material.COBBLESTONE, 2);
            removeItems(inv, Material.STICK, 1);
            inv.addItem(new ItemStack[]{new ItemStack(Material.STONE_SWORD)});
         } else if (cobble >= 3 && sticks >= 2 && !hasItem(bot, Material.STONE_AXE)) {
            removeItems(inv, Material.COBBLESTONE, 3);
            removeItems(inv, Material.STICK, 2);
            inv.addItem(new ItemStack[]{new ItemStack(Material.STONE_AXE)});
         } else if (cobble >= 3 && sticks >= 2 && !hasItem(bot, Material.STONE_PICKAXE)) {
            removeItems(inv, Material.COBBLESTONE, 3);
            removeItems(inv, Material.STICK, 2);
            inv.addItem(new ItemStack[]{new ItemStack(Material.STONE_PICKAXE)});
         } else {
            int woolCount = countAny(inv, WOOLS);
            if (woolCount >= 3 && totalPlanks >= 3 && !hasItem(bot, Material.WHITE_BED) && reserveOK(totalLogs, totalPlanks, 3, 0)) {
               removeAnyWool(inv, 3);
               removeAnyPlanks(inv, 3);
               inv.addItem(new ItemStack[]{new ItemStack(Material.WHITE_BED)});
            } else {
               if (bot.getHealth() >= 10.0) {
                  int leather = countItem(inv, Material.LEATHER);
                  if (leather >= 5 && !hasItem(bot, Material.LEATHER_HELMET)) {
                     removeItems(inv, Material.LEATHER, 5);
                     inv.addItem(new ItemStack[]{new ItemStack(Material.LEATHER_HELMET)});
                     return;
                  }

                  if (leather >= 8 && !hasItem(bot, Material.LEATHER_CHESTPLATE)) {
                     removeItems(inv, Material.LEATHER, 8);
                     inv.addItem(new ItemStack[]{new ItemStack(Material.LEATHER_CHESTPLATE)});
                     return;
                  }

                  if (leather >= 7 && !hasItem(bot, Material.LEATHER_LEGGINGS)) {
                     removeItems(inv, Material.LEATHER, 7);
                     inv.addItem(new ItemStack[]{new ItemStack(Material.LEATHER_LEGGINGS)});
                     return;
                  }

                  if (leather >= 4 && !hasItem(bot, Material.LEATHER_BOOTS)) {
                     removeItems(inv, Material.LEATHER, 4);
                     inv.addItem(new ItemStack[]{new ItemStack(Material.LEATHER_BOOTS)});
                     return;
                  }
               }
            }
         }
      }
   }

   private static boolean reserveOK(int logs, int planks, int planksNeeded, int sticksNeeded) {
      return planks - planksNeeded - sticksNeeded * 2 >= 8 && logs >= 4;
   }

   private static int countItem(PlayerInventory inv, Material type) {
      int count = 0;

      for (ItemStack item : inv.getContents()) {
         if (item != null && item.getType() == type) {
            count += item.getAmount();
         }
      }

      return count;
   }

   private static int countAny(PlayerInventory inv, List<Material> types) {
      int count = 0;

      for (Material t : types) {
         count += countItem(inv, t);
      }

      return count;
   }

   private static boolean hasItem(Player bot, Material material) {
      for (int i = 0; i < bot.getInventory().getSize(); i++) {
         ItemStack item = bot.getInventory().getItem(i);
         if (item != null && item.getType() == material) {
            return true;
         }
      }

      return false;
   }

   private static void removeItems(PlayerInventory inv, Material type, int amount) {
      for (int i = 0; i < inv.getSize() && amount > 0; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null && item.getType() == type) {
            int remove = Math.min(amount, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            if (item.getAmount() <= 0) {
               inv.setItem(i, (ItemStack)null);
            }

            amount -= remove;
         }
      }
   }

   private static void removeAnyPlanks(PlayerInventory inv, int amount) {
      for (Material plank : PLANKS) {
         while (amount > 0 && countItem(inv, plank) > 0) {
            removeItems(inv, plank, 1);
            amount--;
         }
      }
   }

   private static void removeAnyWool(PlayerInventory inv, int amount) {
      for (Material wool : WOOLS) {
         while (amount > 0 && countItem(inv, wool) > 0) {
            removeItems(inv, wool, 1);
            amount--;
         }
      }
   }
}
