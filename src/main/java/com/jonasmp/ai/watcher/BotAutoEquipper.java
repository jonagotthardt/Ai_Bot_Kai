package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
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
      {"GOLDEN_APPLE", "16"},
      {"SHIELD", "1"}
   };
   private static final String[][] STARTER_KIT_DIAMOND = new String[][]{
      {"DIAMOND_SWORD", "1"},
      {"DIAMOND_HELMET", "1"},
      {"DIAMOND_CHESTPLATE", "1"},
      {"DIAMOND_LEGGINGS", "1"},
      {"DIAMOND_BOOTS", "1"},
      {"GOLDEN_APPLE", "16"},
      {"SHIELD", "1"}
   };
   private static final String[][] STARTER_KIT_IRON = new String[][]{
      {"IRON_SWORD", "1"},
      {"IRON_HELMET", "1"},
      {"IRON_CHESTPLATE", "1"},
      {"IRON_LEGGINGS", "1"},
      {"IRON_BOOTS", "1"},
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

   /**
    * One acquisition per cycle (cooldown-gated), highest priority first: gear that keeps Kai
    * alive and dangerous, then the consumables the operator wants stocked. Returns after the
    * first buy it issues so a single command goes out per check.
    */
   private void checkAndBuyMissing(Player bot) {
      // 1) Survival gear: a chestplate, a sword, a shield.
      if (!this.hasArmorEquipped() && !this.hasAnyArmorPiece()) {
         if (!this.sendShopBuy("NETHERITE_CHESTPLATE", "1") && !this.sendShopBuy("DIAMOND_CHESTPLATE", "1")) {
            this.sendShopBuy("IRON_CHESTPLATE", "1");
         }
         return;
      }
      if (!this.hasWeapon()
         && !this.hasInInventory("NETHERITE_SWORD", 1)
         && !this.hasInInventory("DIAMOND_SWORD", 1)
         && !this.hasInInventory("IRON_SWORD", 1)) {
         if (!this.sendShopBuy("NETHERITE_SWORD", "1") && !this.sendShopBuy("DIAMOND_SWORD", "1")) {
            this.sendShopBuy("IRON_SWORD", "1");
         }
         return;
      }
      if (!this.hasInInventory("SHIELD", 1)) {
         if (this.sendShopBuy("SHIELD", "1")) {
            return;
         }
      }

      // 2) Consumables, in priority order. restock() returns true once it has issued a buy.
      FileConfiguration cfg = cfg();
      if (this.restock("TOTEM_OF_UNDYING", cfg.getInt("bot.shop.keep_totems", 2))) {
         return;
      }
      if (this.restock("GOLDEN_APPLE", cfg.getInt("bot.shop.keep_golden_apples", 16))) {
         return;
      }
      if (this.restockFood(cfg.getInt("bot.shop.keep_food", 32))) {
         return;
      }
      if (this.restock("EXPERIENCE_BOTTLE", cfg.getInt("bot.shop.keep_xp_bottles", 64))) {
         return;
      }
      if (this.restock(cfg.getString("bot.shop.building_block_item", "COBBLESTONE"),
            cfg.getInt("bot.shop.keep_building_blocks", 64))) {
         return;
      }
      if (cfg.getBoolean("bot.shop.buy_ender_pearls", false)) {
         this.restock("ENDER_PEARL", cfg.getInt("bot.shop.keep_ender_pearls", 16));
      }
   }

   /** Tops a single stackable item up to {@code target}; issues at most one buy. */
   private boolean restock(String item, int target) {
      if (target <= 0) {
         return false;
      }
      Material mat = Material.getMaterial(item);
      if (mat == null) {
         return false;
      }
      int have = this.countInInventory(mat);
      if (have >= target) {
         return false;
      }
      return this.sendShopBuy(item, String.valueOf(Math.min(64, target - have)));
   }

   private boolean restockFood(int target) {
      if (target <= 0) {
         return false;
      }
      int food = 0;
      for (Material m : new Material[]{Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.GOLDEN_CARROT}) {
         food += this.countInInventory(m);
      }
      if (food >= target) {
         return false;
      }
      return this.sendShopBuy("COOKED_BEEF", String.valueOf(Math.min(64, target - food)));
   }

   public void onCombatStart(Player bot) {
      this.aiBot.autoEquipBestGear(bot);
      this.aiBot.selectBestWeapon(bot);
      // Quick pre-fight top-ups of the two life-savers.
      if (this.commandCooldown <= 0 && this.countInInventory(Material.GOLDEN_APPLE) < 2) {
         this.sendShopBuy("GOLDEN_APPLE", "8");
      } else if (this.commandCooldown <= 0
         && cfg().getInt("bot.shop.keep_totems", 2) > 0
         && this.countInInventory(Material.TOTEM_OF_UNDYING) < 1) {
         this.sendShopBuy("TOTEM_OF_UNDYING", "1");
      }
   }

   /**
    * Issues a buy by running the configured shop command (default {@code /shopbuy {item} {amount}})
    * as the bot itself, so it goes through the server's real economy exactly like a human player.
    * Returns true when a command was dispatched (used to throttle to one buy per cycle); the next
    * inventory check decides whether it actually succeeded.
    */
   private boolean sendShopBuy(String item, String amount) {
      if (this.commandCooldown > 0) {
         return false;
      }
      FileConfiguration cfg = cfg();
      if (!cfg.getBoolean("bot.shop.enabled", true)) {
         return false;
      }
      Player bot = this.nmsBot.getPlayer();
      if (bot == null) {
         return false;
      }
      String cmd = cfg.getString("bot.shop.buy_command", "shopbuy {item} {amount}")
         .replace("{item}", item)
         .replace("{amount}", amount);
      try {
         bot.performCommand(cmd);
         this.commandCooldown = COMMAND_COOLDOWN_TICKS;
         this.buyCooldown = 40;
         CoreBootstrap.PLUGIN.getLogger().info("[BotAutoEquipper] Shop command: /" + cmd);
         return true;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[BotAutoEquipper] Buy command failed: " + ex.getMessage());
         this.commandCooldown = 16;
         return false;
      }
   }

   private static FileConfiguration cfg() {
      return CoreBootstrap.PLUGIN.getConfig();
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
