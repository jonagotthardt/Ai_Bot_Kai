package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BotCombatManager {
   private final AIPlayerBot aiBot;
   private Entity currentTarget;
   private int combatTicks;
   private int attackCooldown;
   private int strafeDir;
   private int jumpCooldown;
   private int wTapCooldown;
   private int blockHitCooldown;
   private boolean enabled;
   private double strafeChance;
   private boolean jumpCritEnabled;
   private boolean wTapEnabled;
   private boolean blockHitEnabled;
   private int lastAttackSlot = -1;
   private int comboCount = 0;
   private boolean wasInCombat = false;
   private int noTargetTicks = 0;
   private boolean isMaceSmashing = false;
   private int windChargeCooldown = 0;
   private int maceSmashTicks = 0;
   private double smashStartY = 0.0;
   private static final int WIND_CHARGE_COOLDOWN = 60;
   private static final double MACE_SMASH_RANGE = 4.0;
   private static final double MACE_SMASH_MIN_FALL = 1.5;
   private int shieldBlockCooldown = 0;
   private int shieldBlockTicks = 0;
   private boolean isShieldBlocking = false;
   private static final int SHIELD_BLOCK_DURATION = 8;
   private static final int SHIELD_BLOCK_COOLDOWN = 20;
   private static final double SWORD_OPTIMAL_MIN = 1.5;
   private static final double SWORD_OPTIMAL_MAX = 3.0;
   private static final double AXE_OPTIMAL_MIN = 1.0;
   private static final double AXE_OPTIMAL_MAX = 2.5;

   private void debugLog(String msg) {
      CoreBootstrap.PLUGIN.getLogger().info("[BotCombatManager/DEBUG] " + msg);
   }

   public BotCombatManager(AIPlayerBot aiBot) {
      this.aiBot = aiBot;
      this.currentTarget = null;
      this.combatTicks = 0;
      this.attackCooldown = 0;
      this.strafeDir = 1;
      this.jumpCooldown = 0;
      this.wTapCooldown = 0;
      this.blockHitCooldown = 0;
      this.reloadConfig();
   }

   public void reset() {
      this.isMaceSmashing = false;
      this.maceSmashTicks = 0;
      this.smashStartY = 0.0;
      this.attackCooldown = 0;
      this.comboCount = 0;
      this.shieldBlockCooldown = 0;
      this.isShieldBlocking = false;
      this.shieldBlockTicks = 0;
      this.windChargeCooldown = 0;
      this.lastAttackSlot = -1;
      this.currentTarget = null;
      this.combatTicks = 0;
      this.noTargetTicks = 0;
      this.debugLog("COMBAT_STATE_RESET");
   }

   public void reloadConfig() {
      FileConfiguration cfg = CoreBootstrap.PLUGIN.getConfig();
      this.enabled = cfg.getBoolean("bot.combat.enabled", true);
      this.strafeChance = cfg.getDouble("bot.combat.strafe_chance", 0.8);
      this.jumpCritEnabled = cfg.getBoolean("bot.combat.jump_crit", true);
      this.wTapEnabled = cfg.getBoolean("bot.combat.w_tap", true);
      this.blockHitEnabled = cfg.getBoolean("bot.combat.block_hit", true);
   }

   public boolean isInCombat() {
      return this.currentTarget != null && this.currentTarget.isValid() && this.combatTicks > 0;
   }

   public int getAttackCooldown() {
      return this.attackCooldown;
   }

   public Entity getCurrentTarget() {
      return this.currentTarget;
   }

   public void clearTarget() {
      this.currentTarget = null;
      this.combatTicks = 0;
   }

   private int getWeaponCooldownTicks(Player bot) {
      ItemStack weapon = bot.getInventory().getItemInMainHand();
      if (weapon != null && weapon.getType() != Material.AIR) {
         Material type = weapon.getType();
         if (type.name().contains("SWORD")) {
            return 12;
         } else if (type.name().contains("AXE")) {
            return 25;
         } else if (type.name().contains("HOE")) {
            return 10;
         } else if (type.name().contains("SHOVEL")) {
            return 8;
         } else {
            return type.name().contains("PICKAXE") ? 10 : 5;
         }
      } else {
         return 5;
      }
   }

   private boolean isAttackReady(Player bot, Entity target) {
      double health = bot.getHealth();
      boolean isPlayerTarget = target instanceof Player;
      boolean isSurrounded = false;

      try {
         isSurrounded = bot.getNearbyEntities(3.0, 2.0, 3.0).size() >= 3;
      } catch (Exception var8) {
      }

      if (health < 4.0) {
         return true;
      } else {
         try {
            float cooldown = bot.getAttackCooldown();
            if (cooldown < 0.01F) {
               return this.attackCooldown <= 0;
            } else {
               return cooldown >= 0.95F ? true : (isPlayerTarget || health < 8.0 || isSurrounded) && cooldown >= 0.5F;
            }
         } catch (Exception var9) {
            return this.attackCooldown <= 0;
         }
      }
   }

   public boolean tick(Player bot, NMSBot nmsBot) {
      if (!this.enabled) {
         return false;
      } else {
         Entity target = this.findPriorityTarget(bot);
         if (target != null && target.isValid() && !target.isDead()) {
            double dist = bot.getLocation().distance(target.getLocation());
            if (dist > 15.0) {
               this.currentTarget = null;
               this.combatTicks = 0;
               return false;
            } else if (dist > 6.0) {
               nmsBot.navigateTo(target.getLocation());
               nmsBot.setRotation(this.computeYaw(bot, target), this.computePitch(bot, target));
               bot.setSprinting(true);
               return true;
            } else {
               boolean justEnteredCombat = this.currentTarget == null;
               this.currentTarget = target;
               this.combatTicks = 30;
               this.noTargetTicks = 0;
               nmsBot.setRotation(this.computeYaw(bot, target), this.computePitch(bot, target));
               if (justEnteredCombat) {
                  this.debugLog("COMBAT_START target=" + target.getName() + " dist=" + String.format("%.1f", dist));
                  int maceSlot = this.findMaceSlot(bot);
                  if (maceSlot >= 0) {
                     nmsBot.selectHotbarSlot(maceSlot);
                     this.lastAttackSlot = maceSlot;
                     this.debugLog("MACE_EQUIPPED slot=" + maceSlot + " on_combat_start");
                  } else {
                     this.selectBestWeapon(bot, nmsBot);
                     this.debugLog("NO_MACE_FOUND best_weapon_selected");
                  }

                  this.wasInCombat = true;
                  if (this.aiBot != null) {
                     this.aiBot.onCombatStart(bot);
                  }
               }

               if (!this.isMaceSmashing && this.combatTicks % 2 == 0) {
                  ItemStack hand = bot.getInventory().getItemInMainHand();
                  if (hand == null || hand.getType().isEdible() || this.weaponScore(hand.getType()) < 0) {
                     this.debugLog("WEAPON_SWITCH from=" + (hand != null ? hand.getType().name() : "EMPTY") + " reason=bad_weapon_in_hand");
                     this.lastAttackSlot = -1;
                     this.selectBestWeapon(bot, nmsBot);
                  }
               } else if (this.isMaceSmashing && this.combatTicks % 2 == 0) {
                  this.debugLog("WEAPON_SWITCH_SKIPPED reason=mace_smash_in_progress");
               }

               if (this.attackCooldown > 0) {
                  this.attackCooldown--;
               }

               if (this.jumpCooldown > 0) {
                  this.jumpCooldown--;
               }

               if (this.wTapCooldown > 0) {
                  this.wTapCooldown--;
               }

               if (this.blockHitCooldown > 0) {
                  this.blockHitCooldown--;
               }

               if (this.windChargeCooldown > 0) {
                  this.windChargeCooldown--;
               }

               if (this.shieldBlockCooldown > 0) {
                  this.shieldBlockCooldown--;
               }

               if (this.isShieldBlocking) {
                  this.shieldBlockTicks++;
                  if (this.shieldBlockTicks >= 8) {
                     this.isShieldBlocking = false;
                     this.shieldBlockTicks = 0;
                     this.shieldBlockCooldown = 20;
                     this.debugLog("SHIELD_BLOCK_END");
                  }
               }

               if (!this.isMaceSmashing && this.windChargeCooldown <= 0) {
                  int windChargeSlot = this.findWindChargeSlot(bot);
                  int maceSlotx = this.findMaceSlot(bot);
                  if (windChargeSlot >= 0 && maceSlotx >= 0 && dist > 3.0 && dist < 12.0) {
                     this.debugLog(
                        "MACE_SMASH_START target="
                           + target.getName()
                           + " windChargeSlot="
                           + windChargeSlot
                           + " maceSlot="
                           + maceSlotx
                           + " dist="
                           + String.format("%.1f", dist)
                     );
                     nmsBot.selectHotbarSlot(windChargeSlot);
                     float currentYaw = bot.getLocation().getYaw();
                     nmsBot.setRotation(currentYaw, 90.0F);
                     nmsBot.useItem();
                     this.debugLog("WIND_CHARGE_THROWN slot=" + windChargeSlot + " yaw=" + String.format("%.1f", currentYaw) + " pitch=90.0");
                     nmsBot.setRotation(this.computeYaw(bot, target), this.computePitch(bot, target));
                     this.windChargeCooldown = 60;
                     this.isMaceSmashing = true;
                     this.maceSmashTicks = 0;
                     this.smashStartY = bot.getLocation().getY();
                     return true;
                  }
               }

               if (this.isMaceSmashing) {
                  this.maceSmashTicks++;
                  int maceSlotx = this.findMaceSlot(bot);
                  if (maceSlotx >= 0) {
                     nmsBot.selectHotbarSlot(maceSlotx);
                  }

                  nmsBot.setRotation(this.computeYaw(bot, target), this.computePitch(bot, target));
                  double currentY = bot.getLocation().getY();
                  double fallen = this.smashStartY - currentY;
                  boolean hasFallenEnough = fallen >= 1.5;
                  boolean notOnGround = !bot.isOnGround();
                  boolean inSmashRange = dist <= 4.0;
                  boolean peakReached = this.maceSmashTicks > 6;
                  this.debugLog(
                     "MACE_SMASH_TRACK ticks="
                        + this.maceSmashTicks
                        + " fallen="
                        + String.format("%.2f", fallen)
                        + " onGround="
                        + bot.isOnGround()
                        + " dist="
                        + String.format("%.1f", dist)
                  );
                  if (peakReached && hasFallenEnough && notOnGround && inSmashRange && this.attackCooldown <= 0) {
                     this.debugLog(
                        "MACE_SMASH_HIT target="
                           + target.getName()
                           + " dist="
                           + String.format("%.1f", dist)
                           + " fallen="
                           + String.format("%.2f", fallen)
                           + " ticks="
                           + this.maceSmashTicks
                     );
                     nmsBot.swingMainHand();
                     bot.attack(target);
                     this.comboCount++;
                     this.attackCooldown = 15;
                     this.isMaceSmashing = false;
                     this.smashStartY = 0.0;
                     return true;
                  } else {
                     if (this.maceSmashTicks > 60) {
                        this.debugLog(
                           "MACE_SMASH_TIMEOUT target=" + target.getName() + " ticks=" + this.maceSmashTicks + " fallen=" + String.format("%.2f", fallen)
                        );
                        this.isMaceSmashing = false;
                        this.smashStartY = 0.0;
                     }

                     return true;
                  }
               } else {
                  boolean hasSword = this.isHoldingSword(bot);
                  double optimalMin = hasSword ? 1.5 : 1.0;
                  double optimalMax = hasSword ? 3.0 : 2.5;
                  double sideways = 0.0;
                  double forward;
                  if (dist > optimalMax) {
                     forward = 0.6;
                     bot.setSprinting(true);
                  } else if (dist < optimalMin) {
                     forward = -0.4;
                     bot.setSprinting(false);
                     if (this.hasShieldInOffhand(bot) && this.shieldBlockCooldown <= 0 && !this.isShieldBlocking) {
                        this.isShieldBlocking = true;
                        this.shieldBlockTicks = 0;
                        nmsBot.useOffhandItem();
                        this.debugLog("SHIELD_BLOCK_START target=" + target.getName() + " dist=" + String.format("%.1f", dist));
                     }
                  } else {
                     forward = 0.6;
                     if (Math.random() < this.strafeChance) {
                        sideways = (double)this.strafeDir * 0.6;
                        if (this.combatTicks % 3 == 0 && Math.random() < 0.5) {
                           this.strafeDir *= -1;
                        }
                     }

                     bot.setSprinting(true);
                  }

                  if (this.isShieldBlocking) {
                     nmsBot.useOffhandItem();
                  }

                  nmsBot.walkRelative(forward, sideways);
                  if (this.jumpCritEnabled && bot.isOnGround() && this.jumpCooldown <= 0 && this.attackCooldown <= 2 && dist <= optimalMax) {
                     nmsBot.jump();
                     this.jumpCooldown = 12;
                  }

                  if (this.isAttackReady(bot, target) && dist <= optimalMax + 0.3) {
                     if (this.isShieldBlocking) {
                        this.isShieldBlocking = false;
                        this.shieldBlockTicks = 0;
                        this.shieldBlockCooldown = 20;
                        this.debugLog("SHIELD_BLOCK_RELEASE_FOR_ATTACK");
                     }

                     ItemStack weapon = bot.getInventory().getItemInMainHand();
                     this.debugLog(
                        "ATTACK target="
                           + target.getName()
                           + " dist="
                           + String.format("%.1f", dist)
                           + " weapon="
                           + (weapon != null ? weapon.getType().name() : "FIST")
                           + " cooldown="
                           + this.attackCooldown
                     );
                     nmsBot.swingMainHand();
                     bot.attack(target);
                     this.comboCount++;
                     this.attackCooldown = this.getWeaponCooldownTicks(bot);
                     this.wTapCooldown = 6;
                     this.blockHitCooldown = 3;
                     nmsBot.walkRelative(0.4, sideways * 0.3);
                  } else if (dist <= optimalMax + 0.3) {
                     this.debugLog(
                        "ATTACK_BLOCKED target="
                           + target.getName()
                           + " dist="
                           + String.format("%.1f", dist)
                           + " cooldown="
                           + this.attackCooldown
                           + " isReady="
                           + this.isAttackReady(bot, target)
                     );
                  }

                  if (dist > optimalMax + 1.0) {
                     this.comboCount = 0;
                  }

                  if (this.wTapEnabled && this.wTapCooldown > 0 && this.wTapCooldown <= 3) {
                     bot.setSprinting(false);
                     double retreat = this.comboCount >= 3 ? -0.3 : -0.15;
                     nmsBot.walkRelative(retreat, sideways * 0.2);
                     if (this.comboCount >= 3) {
                        this.comboCount = 0;
                     }
                  }

                  this.combatTicks++;
                  return true;
               }
            }
         } else {
            if (this.currentTarget != null) {
               this.noTargetTicks++;
               if (this.noTargetTicks >= 40) {
                  this.currentTarget = null;
                  this.combatTicks = 0;
                  this.comboCount = 0;
                  bot.setSprinting(false);
                  bot.setSneaking(false);
               }
            }

            return false;
         }
      }
   }

   private Entity findPriorityTarget(Player bot) {
      Player lastDamaged = this.aiBot != null ? this.aiBot.getLastDamagedByPlayer() : null;
      if (lastDamaged != null && lastDamaged.isOnline() && lastDamaged.getWorld().equals(bot.getWorld())) {
         double dist = bot.getLocation().distance(lastDamaged.getLocation());
         if (dist < 10.0) {
            return lastDamaged;
         }
      }

      return this.aiBot != null
            && this.aiBot.cachedNearestMonster != null
            && this.aiBot.cachedNearestMonster.isValid()
            && !this.aiBot.cachedNearestMonster.isDead()
         ? this.aiBot.cachedNearestMonster
         : this.findNearestMonster(bot, 6.0);
   }

   private Entity findNearestMonster(Player bot, double radius) {
      Entity nearest = null;
      double nearestDist = radius;

      for (Entity e : bot.getNearbyEntities(radius, radius, radius)) {
         if (e instanceof Monster && !e.isDead() && e.isValid()) {
            double d = bot.getLocation().distance(e.getLocation());
            if (d < nearestDist) {
               nearestDist = d;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private float computeYaw(Player bot, Entity target) {
      Location botLoc = bot.getLocation();
      Location targetLoc = target.getLocation().add(0.0, 1.5, 0.0);
      double dx = targetLoc.getX() - botLoc.getX();
      double dz = targetLoc.getZ() - botLoc.getZ();
      return (float)Math.toDegrees(Math.atan2(-dx, dz));
   }

   private float computePitch(Player bot, Entity target) {
      Location botLoc = bot.getLocation();
      Location targetLoc = target.getLocation().add(0.0, 1.5, 0.0);
      double dx = targetLoc.getX() - botLoc.getX();
      double dy = targetLoc.getY() - botLoc.getY();
      double dz = targetLoc.getZ() - botLoc.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      return (float)(-Math.toDegrees(Math.atan2(dy, dist)));
   }

   private boolean isHoldingSword(Player bot) {
      ItemStack hand = bot.getInventory().getItemInMainHand();
      return hand != null && hand.getType().name().contains("SWORD");
   }

   private boolean isHoldingMace(Player bot) {
      ItemStack hand = bot.getInventory().getItemInMainHand();
      return hand != null && hand.getType().name().equals("MACE");
   }

   private int findWindChargeSlot(Player bot) {
      PlayerInventory inv = bot.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null && item.getType().name().equals("WIND_CHARGE")) {
            return i;
         }
      }

      return -1;
   }

   private boolean hasShieldInOffhand(Player bot) {
      ItemStack off = bot.getInventory().getItemInOffHand();
      return off != null && off.getType().name().contains("SHIELD");
   }

   private int findMaceSlot(Player bot) {
      PlayerInventory inv = bot.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null && item.getType().name().equals("MACE")) {
            return i;
         }
      }

      return -1;
   }

   private void selectBestWeapon(Player bot, NMSBot nmsBot) {
      int bestSlot = -1;
      int bestScore = -1;
      PlayerInventory inv = bot.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null) {
            int score = this.weaponScore(item.getType());
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }
      }

      if (bestSlot >= 0 && bestSlot != this.lastAttackSlot) {
         nmsBot.selectHotbarSlot(bestSlot);
         this.lastAttackSlot = bestSlot;
      }
   }

   private int weaponScore(Material type) {
      if (type != null && type.isEdible()) {
         return -1;
      } else if (type != null && type.name().equals("MACE")) {
         return 110;
      } else if (type == Material.NETHERITE_SWORD) {
         return 100;
      } else if (type == Material.DIAMOND_SWORD) {
         return 90;
      } else if (type == Material.IRON_SWORD) {
         return 80;
      } else if (type == Material.STONE_SWORD) {
         return 70;
      } else if (type == Material.WOODEN_SWORD) {
         return 60;
      } else if (type == Material.GOLDEN_SWORD) {
         return 55;
      } else if (type == Material.NETHERITE_AXE) {
         return 50;
      } else if (type == Material.DIAMOND_AXE) {
         return 45;
      } else if (type == Material.IRON_AXE) {
         return 40;
      } else if (type == Material.STONE_AXE) {
         return 35;
      } else if (type == Material.WOODEN_AXE) {
         return 30;
      } else if (type == Material.GOLDEN_AXE) {
         return 28;
      } else if (type == Material.TRIDENT) {
         return 42;
      } else if (type != null && type.name().contains("SWORD")) {
         return 25;
      } else if (type != null && type.name().contains("AXE")) {
         return 20;
      } else {
         return type != null && type.name().contains("MACE") ? 15 : -1;
      }
   }
}
