package com.jonasmp.ai.watcher;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BotAutoEquipper {
   private final AIPlayerBot aiBot;
   private final NMSBot nmsBot;
   private Runnable onBuyCallback = null;
   private int buyCooldown = 0;
   private int commandCooldown = 0;
   private int equipCheckCooldown = 0;
   private boolean starterKitDone = false;
   private int lastDeathCount = 0;
   private static final int COMMAND_COOLDOWN_TICKS = 8;
   private static final int EQUIP_CHECK_INTERVAL = 60;
   private static final String[][] STARTER_KIT = new String[][]{
      {"NETHERITE_SWORD", "1"},
      {"NETHERITE_HELMET", "1"},
      {"NETHERITE_CHESTPLATE", "1"},
      {"NETHERITE_LEGGINGS", "1"},
      {"NETHERITE_BOOTS", "1"},
      {"MACE", "1"},
      {"WIND_CHARGE", "64"},
      {"GOLDEN_APPLE", "16"},
      {"SHIELD", "1"}
   };
   private static final String[][] STARTER_KIT_DIAMOND = new String[][]{
      {"DIAMOND_SWORD", "1"},
      {"DIAMOND_HELMET", "1"},
      {"DIAMOND_CHESTPLATE", "1"},
      {"DIAMOND_LEGGINGS", "1"},
      {"DIAMOND_BOOTS", "1"},
      {"MACE", "1"},
      {"WIND_CHARGE", "64"},
      {"GOLDEN_APPLE", "16"},
      {"SHIELD", "1"}
   };
   private static final String[][] STARTER_KIT_IRON = new String[][]{
      {"IRON_SWORD", "1"},
      {"IRON_HELMET", "1"},
      {"IRON_CHESTPLATE", "1"},
      {"IRON_LEGGINGS", "1"},
      {"IRON_BOOTS", "1"},
      {"MACE", "1"},
      {"WIND_CHARGE", "64"},
      {"GOLDEN_APPLE", "8"},
      {"SHIELD", "1"}
   };

   public BotAutoEquipper(AIPlayerBot aiBot, NMSBot nmsBot) {
      this.aiBot = aiBot;
      this.nmsBot = nmsBot;
   }

   public void setOnBuyCallback(Runnable callback) {
      this.onBuyCallback = callback;
   }

   public void tick(Player bot) {
      if (this.commandCooldown > 0) {
         this.commandCooldown--;
      }

      if (this.equipCheckCooldown > 0) {
         this.equipCheckCooldown--;
      }

      if (this.buyCooldown > 0) {
         this.buyCooldown--;
         if (this.buyCooldown <= 0 && this.onBuyCallback != null) {
            this.onBuyCallback.run();
         }
      }

      int currentDeaths = bot.getStatistic(Statistic.DEATHS);
      if (currentDeaths > this.lastDeathCount) {
         this.starterKitDone = false;
         this.lastDeathCount = currentDeaths;
      }

      if (!this.starterKitDone && bot.getHealth() > 0.0 && !bot.isDead()) {
         if (this.buyStarterKit()) {
            this.starterKitDone = true;
            this.aiBot.autoEquipBestGear(bot);
         }
      } else {
         if (this.equipCheckCooldown <= 0) {
            this.equipCheckCooldown = 60;
            this.checkAndBuyMissing(bot);
         }
      }
   }

   private boolean buyStarterKit() {
      String[][][] kits = new String[][][]{STARTER_KIT, STARTER_KIT_DIAMOND, STARTER_KIT_IRON};

      for (String[][] kit : kits) {
         boolean allAcquired = true;

         for (String[] item : kit) {
            if (!this.hasInInventory(item[0], Integer.parseInt(item[1]))) {
               if (this.sendShopBuy(item[0], item[1])) {
                  return false;
               }

               allAcquired = false;
            }
         }

         if (allAcquired) {
            return true;
         }
      }

      return true;
   }

   private void checkAndBuyMissing(Player bot) {
      PlayerInventory inv = bot.getInventory();
      if (!this.hasArmorEquipped() && !this.hasAnyArmorPiece()) {
         if (!this.sendShopBuy("NETHERITE_CHESTPLATE", "1")) {
            if (!this.sendShopBuy("DIAMOND_CHESTPLATE", "1")) {
               this.sendShopBuy("IRON_CHESTPLATE", "1");
            }
         }
      } else if (this.hasWeapon()
         || this.hasInInventory("NETHERITE_SWORD", 1)
         || this.hasInInventory("DIAMOND_SWORD", 1)
         || this.hasInInventory("IRON_SWORD", 1)) {
         int gapples = this.countInInventory(Material.GOLDEN_APPLE);
         if (gapples < 4) {
            int toBuy = Math.min(16, 4 - gapples + 8);
            this.sendShopBuy("GOLDEN_APPLE", String.valueOf(toBuy));
         } else {
            int foodItems = 0;

            for (Material food : new Material[]{Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.GOLDEN_CARROT}) {
               foodItems += this.countInInventory(food);
            }

            if (foodItems < 32) {
               this.sendShopBuy("COOKED_BEEF", "16");
            } else if (!this.hasInInventory("MACE", 1)) {
               this.sendShopBuy("MACE", "1");
            } else {
               int windCharges = this.countInInventory(Material.WIND_CHARGE);
               if (windCharges < 16) {
                  this.sendShopBuy("WIND_CHARGE", String.valueOf(Math.min(64, 64 - windCharges)));
               } else {
                  if (!this.hasInInventory("SHIELD", 1)) {
                     this.sendShopBuy("SHIELD", "1");
                  }
               }
            }
         }
      } else if (!this.sendShopBuy("NETHERITE_SWORD", "1")) {
         if (!this.sendShopBuy("DIAMOND_SWORD", "1")) {
            this.sendShopBuy("IRON_SWORD", "1");
         }
      }
   }

   public void onCombatStart(Player bot) {
      this.aiBot.autoEquipBestGear(bot);
      this.aiBot.selectBestWeapon(bot);
      if (this.countInInventory(Material.GOLDEN_APPLE) < 2 && this.commandCooldown <= 0) {
         this.sendShopBuy("GOLDEN_APPLE", "8");
      }

      if (!this.hasInInventory("MACE", 1) && this.commandCooldown <= 0) {
         this.sendShopBuy("MACE", "1");
      }

      int windCharges = this.countInInventory(Material.WIND_CHARGE);
      if (windCharges < 8 && this.commandCooldown <= 0) {
         this.sendShopBuy("WIND_CHARGE", "32");
      }
   }

   /**
    * Kai 2.0 has no economy/shop integration, so acquisition via a shop is a
    * no-op. The bot equips from whatever is already in its inventory (managed by
    * {@link AIPlayerBot#autoEquipBestGear} / {@link AIPlayerBot#selectBestWeapon}).
    */
   private boolean sendShopBuy(String item, String amount) {
      return false;
   }

   private boolean hasInInventory(String materialName, int amount) {
      Material mat = Material.getMaterial(materialName);
      return mat == null ? false : this.countInInventory(mat) >= amount;
   }

   private int countInInventory(Material mat) {
      Player bot = this.nmsBot.getPlayer();
      if (bot == null) {
         return 0;
      } else {
         int count = 0;

         for (ItemStack stack : bot.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) {
               count += stack.getAmount();
            }
         }

         return count;
      }
   }

   private boolean hasArmorEquipped() {
      Player bot = this.nmsBot.getPlayer();
      if (bot == null) {
         return false;
      } else {
         PlayerInventory inv = bot.getInventory();
         return inv.getHelmet() != null && inv.getChestplate() != null && inv.getLeggings() != null && inv.getBoots() != null;
      }
   }

   private boolean hasWeapon() {
      Player bot = this.nmsBot.getPlayer();
      if (bot == null) {
         return false;
      } else {
         ItemStack hand = bot.getInventory().getItemInMainHand();
         if (hand == null) {
            return false;
         } else {
            String name = hand.getType().name();
            return name.contains("SWORD") || name.contains("AXE") || name.contains("MACE");
         }
      }
   }

   private boolean hasAnyArmorPiece() {
      return this.hasInInventory("NETHERITE_CHESTPLATE", 1)
         || this.hasInInventory("DIAMOND_CHESTPLATE", 1)
         || this.hasInInventory("IRON_CHESTPLATE", 1)
         || this.hasInInventory("GOLDEN_CHESTPLATE", 1)
         || this.hasInInventory("CHAINMAIL_CHESTPLATE", 1)
         || this.hasInInventory("LEATHER_CHESTPLATE", 1);
   }
}
