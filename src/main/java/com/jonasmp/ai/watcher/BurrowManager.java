package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BurrowManager {
   private BurrowManager.BurrowState state = BurrowManager.BurrowState.NOT_BURROWED;
   private int burrowTicks = 0;
   private int escapeTicks = 0;
   private long lastCheckTime = 0L;
   private static final long CHECK_INTERVAL_MS = 1000L;
   private static final int MAX_BURROW_TICKS = 200;
   private static final int MIN_ESCAPE_TICKS = 60;
   private static final double BURROW_HP_THRESHOLD = 6.0;
   private static final double MIN_ESCAPE_HEALTH = 14.0;
   private static final double HEALING_HP_THRESHOLD = 10.0;
   private static final int HEALING_PHASE_TICKS = 200;
   private static final double MONSTER_CHECK_RANGE = 8.0;
   private BiConsumer<String, String> chatCallback;

   public boolean isBurrowed() {
      return this.state == BurrowManager.BurrowState.BURROWED;
   }

   public boolean isHealingPhase() {
      return this.state == BurrowManager.BurrowState.ESCAPING && this.escapeTicks < 200;
   }

   public void setChatCallback(BiConsumer<String, String> callback) {
      this.chatCallback = callback;
   }

   public boolean needsHealing(Player bot) {
      return bot.getHealth() < 10.0;
   }

   public void tick(Player bot, NMSBot nmsBot) {
      long now = System.currentTimeMillis();
      if (now - this.lastCheckTime >= 1000L) {
         this.lastCheckTime = now;
         boolean monstersNearby = this.hasMonstersNearby(bot, 8.0);
         double health = bot.getHealth();
         boolean lowHealth = health <= 6.0;
         boolean shouldBurrow = lowHealth && monstersNearby;
         switch (this.state) {
            case NOT_BURROWED:
               if (shouldBurrow) {
                  this.tryBurrow(bot, nmsBot);
               }
               break;
            case BURROWED:
               this.burrowTicks++;
               this.handleBurrowedTick(bot, nmsBot, monstersNearby, health);
               break;
            case ESCAPING:
               this.escapeTicks++;
               if (this.escapeTicks >= 200) {
                  this.state = BurrowManager.BurrowState.NOT_BURROWED;
                  this.escapeTicks = 0;
                  CoreBootstrap.PLUGIN.getLogger().fine("[BurrowManager] Healing phase ended, resuming normal activity.");
               }
         }
      }
   }

   private boolean hasMonstersNearby(Player bot, double range) {
      for (Entity e : bot.getNearbyEntities(range, range / 2.0, range)) {
         if (e instanceof Monster) {
            return true;
         }
      }

      return false;
   }

   private void tryBurrow(Player bot, NMSBot nmsBot) {
      Location loc = bot.getLocation();
      if (WorldProtection.isProtected(loc)) {
         CoreBootstrap.PLUGIN.getLogger().warning("[BurrowManager] Burrow BLOCKED -- inside spawn protection");
      } else if (loc.getY() < -50.0) {
         CoreBootstrap.PLUGIN.getLogger().warning("[BurrowManager] Burrow BLOCKED -- too close to void");
      } else {
         PlayerInventory inv = bot.getInventory();

         for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType().name().endsWith("_BED")) {
               Block ground = loc.subtract(0.0, 1.0, 0.0).getBlock();
               if (ground.getType().isSolid()) {
                  Block bedSpot = loc.add(1.0, 0.0, 0.0).getBlock();
                  if (bedSpot.getType() == Material.AIR && !WorldProtection.isProtected(bedSpot.getLocation())) {
                     nmsBot.selectHotbarSlot(i);
                     nmsBot.placeBlock(bedSpot);
                     bot.teleport(bedSpot.getLocation().add(0.5, 0.0, 0.5));
                     CoreBootstrap.PLUGIN.getLogger().fine("[BurrowManager] Placed bed");
                     return;
                  }
               }
            }
         }

         Block under = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
         if (WorldProtection.isAllowedToBreak(under)) {
            nmsBot.swingMainHand();
            under.breakNaturally();
            bot.teleport(bot.getLocation().subtract(0.0, 1.0, 0.0));
            this.state = BurrowManager.BurrowState.BURROWED;
            this.burrowTicks = 0;
            CoreBootstrap.PLUGIN.getLogger().fine("[BurrowManager] Emergency burrow");
            if (this.chatCallback != null) {
               this.chatCallback.accept("burrow", null);
            }
         }
      }
   }

   private void handleBurrowedTick(Player bot, NMSBot nmsBot, boolean monstersNearby, double health) {
      Block above = bot.getLocation().add(0.0, 1.0, 0.0).getBlock();
      if (above.getType() == Material.AIR) {
         PlayerInventory inv = bot.getInventory();

         for (int j = 0; j < 9; j++) {
            ItemStack item = inv.getItem(j);
            if (item != null && item.getType().isBlock() && item.getType().isSolid() && WorldProtection.isAllowedToPlace(item.getType())) {
               inv.setHeldItemSlot(j);
               above.setType(item.getType());
               item.setAmount(item.getAmount() - 1);
               break;
            }
         }
      }

      boolean safe = !monstersNearby && health >= 14.0;
      boolean forceEscape = this.burrowTicks > 200;
      if (safe && this.burrowTicks > 60 || forceEscape) {
         Block aboveHead = bot.getLocation().add(0.0, 1.0, 0.0).getBlock();
         if (aboveHead.getType() != Material.AIR && WorldProtection.isAllowedToBreak(aboveHead)) {
            nmsBot.swingMainHand();
            aboveHead.breakNaturally();
         }

         bot.teleport(bot.getLocation().add(0.0, 1.0, 0.0));
         this.state = BurrowManager.BurrowState.ESCAPING;
         this.escapeTicks = 0;
         this.burrowTicks = 0;
         CoreBootstrap.PLUGIN.getLogger().fine("[BurrowManager] Unburrowed" + (forceEscape ? " (forced escape)" : "") + ", entering healing phase.");
         if (this.chatCallback != null) {
            this.chatCallback.accept("healed", null);
         }
      }
   }

   private static enum BurrowState {
      NOT_BURROWED,
      BURROWED,
      ESCAPING;
   }
}
