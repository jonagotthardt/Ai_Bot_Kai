package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.combat.CombatTactic;
import com.jonasmp.ai.combat.ComboTracker;
import com.jonasmp.ai.combat.OpponentMemory;
import com.jonasmp.ai.combat.OpponentProfile;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.util.Vector;

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
   private int shieldBlockCooldown = 0;
   private int shieldBlockTicks = 0;
   private boolean isShieldBlocking = false;
   private int shieldRaiseDelay = 0;
   private int shieldHoldTarget = 0;
   private static final double FLEE_HEALTH = 6.0;
   private static final double SHIELD_ATTEMPT_CHANCE = 0.55;
   private static final int SHIELD_REACTION_MIN = 2;
   private static final int SHIELD_REACTION_MAX = 4;
   private static final int SHIELD_HOLD_MIN = 6;
   private static final int SHIELD_HOLD_MAX = 12;
   private static final int SHIELD_COOLDOWN = 16;
   private static final double SWORD_OPTIMAL_MIN = 1.5;
   private static final double SWORD_OPTIMAL_MAX = 3.0;
   private static final double AXE_OPTIMAL_MIN = 1.0;
   private static final double AXE_OPTIMAL_MAX = 2.5;
   private static final int REWARD_WINDOW_TICKS = 20;
   /** Vanilla entity-interaction (attack) reach, measured center-to-center. Kai never swings past this. */
   private static final double ATTACK_REACH = 3.0;
   /** Minimum attack charge for a critical hit (vanilla: 84.8%). */
   private static final float CRIT_CHARGE = 0.848F;
   private boolean critPending;
   private static final double PREDICT_MIN_CONFIDENCE = 0.5;
   private static final long PREDICT_LOOKAHEAD_MS = 450L;
   private static final long PREDICT_GRACE_MS = 100L;
   private static final double PREDICT_MAX_BLOCK_RATE = 0.7;
   private static final double PREDICT_BLOCK_SCALE = 0.8;
   private static final int PREDICT_MAX_DELAY_TICKS = 8;
   private static final double MIRROR_MIN_CONFIDENCE = 0.5;
   private static final double MIRROR_FULL_CHARGE_MS = 600.0;
   private boolean comboDefenseEnabled = true;
   private double mirrorStrafe;
   private double mirrorRangeMin;
   private double mirrorRangeMax;
   private double mirrorShield;
   private double mirrorCharge;
   private final OpponentMemory opponentMemory = new OpponentMemory();
   private boolean adaptiveEnabled = true;
   private OpponentProfile activeProfile;
   private CombatTactic activeTactic;
   private UUID adaptiveTargetId;
   private int rewardWindowTicks;

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
      this.attackCooldown = 0;
      this.comboCount = 0;
      this.shieldBlockCooldown = 0;
      this.isShieldBlocking = false;
      this.shieldBlockTicks = 0;
      this.shieldRaiseDelay = 0;
      this.shieldHoldTarget = 0;
      this.lastAttackSlot = -1;
      this.endAdaptive();
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
      this.adaptiveEnabled = cfg.getBoolean("bot.combat.adaptive", true);
      this.comboDefenseEnabled = cfg.getBoolean("bot.combat.combo_defense", true);
   }

   /**
    * Predictive defense: if the opponent's learned combo rhythm says a hit is imminent,
    * returns the shield-raise delay (ticks) so the block lands in the predicted window;
    * -1 if no confident prediction. Stays human: probabilistic, jittered, cooldown-gated.
    */
   private int predictiveShieldDelay(Entity target) {
      if (!this.comboDefenseEnabled || this.aiBot == null || target == null) {
         return -1;
      }
      ComboTracker tracker = this.aiBot.getComboLearner().getActiveTracker(target);
      if (tracker == null) {
         return -1;
      }
      long now = System.currentTimeMillis();
      if (!tracker.isComboActive(now) || tracker.confidence() < PREDICT_MIN_CONFIDENCE) {
         return -1;
      }
      long eta = tracker.nextHitEtaMs(now);
      if (eta == Long.MAX_VALUE || eta > PREDICT_LOOKAHEAD_MS || eta < -PREDICT_GRACE_MS) {
         return -1;
      }
      double pBlock = Math.min(PREDICT_MAX_BLOCK_RATE, tracker.confidence() * PREDICT_BLOCK_SCALE);
      if (Math.random() > pBlock) {
         return -1;
      }
      int delay = (int) Math.round(eta / 50.0) + (int) Math.round((Math.random() - 0.5) * 2.0);
      return Math.max(1, Math.min(delay, PREDICT_MAX_DELAY_TICKS));
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
      this.endAdaptive();
      this.currentTarget = null;
      this.combatTicks = 0;
   }

   private boolean isMirror() {
      return this.activeTactic == CombatTactic.MIRROR;
   }

   private double effStrafeChance() {
      if (this.isMirror()) {
         return this.mirrorStrafe;
      }
      return this.activeTactic != null ? this.activeTactic.strafeChance : this.strafeChance;
   }

   private double effShieldChance() {
      if (this.isMirror()) {
         return this.mirrorShield;
      }
      return this.activeTactic != null ? this.activeTactic.shieldChance : SHIELD_ATTEMPT_CHANCE;
   }

   private double effAttackChargeThreshold() {
      if (this.isMirror()) {
         return this.mirrorCharge;
      }
      return this.activeTactic != null ? this.activeTactic.attackChargeThreshold : 0.5;
   }

   private double optimalMin(boolean hasSword) {
      if (this.isMirror()) {
         return this.mirrorRangeMin - (hasSword ? 0.0 : 0.5);
      }
      if (this.activeTactic != null) {
         return this.activeTactic.rangeMin - (hasSword ? 0.0 : 0.5);
      }
      return hasSword ? SWORD_OPTIMAL_MIN : AXE_OPTIMAL_MIN;
   }

   private double optimalMax(boolean hasSword) {
      if (this.isMirror()) {
         return this.mirrorRangeMax - (hasSword ? 0.0 : 0.5);
      }
      if (this.activeTactic != null) {
         return this.activeTactic.rangeMax - (hasSword ? 0.0 : 0.5);
      }
      return hasSword ? SWORD_OPTIMAL_MAX : AXE_OPTIMAL_MAX;
   }

   private boolean kiteActive() {
      return this.activeTactic != null && this.activeTactic.kite;
   }

   /**
    * Derives the live MIRROR parameters from the current opponent's measured combo:
    * their attack rhythm -> Kai's swing cadence, their typical distance -> Kai's range,
    * with shield bias scaling to match their aggression. Falls back to the MIRROR enum
    * defaults until the combo-learner is confident. Cheap; called per reward window.
    */
   private void refreshMirrorParams() {
      this.mirrorStrafe = CombatTactic.MIRROR.strafeChance;
      this.mirrorRangeMin = CombatTactic.MIRROR.rangeMin;
      this.mirrorRangeMax = CombatTactic.MIRROR.rangeMax;
      this.mirrorShield = CombatTactic.MIRROR.shieldChance;
      this.mirrorCharge = CombatTactic.MIRROR.attackChargeThreshold;
      if (this.aiBot == null || this.currentTarget == null) {
         return;
      }
      ComboTracker tracker = this.aiBot.getComboLearner().getActiveTracker(this.currentTarget);
      if (tracker == null || tracker.confidence() < MIRROR_MIN_CONFIDENCE) {
         return;
      }
      double interval = tracker.avgIntervalMs();
      if (interval > 0.0) {
         this.mirrorCharge = clamp(interval / MIRROR_FULL_CHARGE_MS, 0.4, 0.95);
      }
      double dist = tracker.avgDistance();
      if (dist > 0.0) {
         this.mirrorRangeMin = clamp(dist - 0.4, 1.2, 3.6);
         this.mirrorRangeMax = clamp(dist + 0.6, this.mirrorRangeMin + 0.6, 4.2);
      }
      this.mirrorShield = clamp(this.mirrorCharge * 0.7, 0.2, 0.6);
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   /** Loads/switches the per-opponent profile and picks a tactic when the target changes. */
   private void updateAdaptiveTarget(Entity target) {
      if (this.aiBot == null) {
         return;
      }
      if (!this.adaptiveEnabled || !(target instanceof Player)) {
         if (this.activeProfile != null) {
            this.endAdaptive();
         }
         return;
      }

      UUID id = target.getUniqueId();
      if (this.activeProfile != null && id.equals(this.adaptiveTargetId)) {
         return;
      }

      this.endAdaptive();
      this.adaptiveTargetId = id;
      this.activeProfile = this.opponentMemory.getOrCreate(id, target.getName());
      this.activeTactic = this.activeProfile.selectTactic();
      this.rewardWindowTicks = 0;
      this.aiBot.consumeDamageDealt();
      this.aiBot.consumeDamageTaken();
      if (this.isMirror()) {
         this.refreshMirrorParams();
      }
      this.debugLog("ADAPT_BEGIN " + this.activeProfile.summary() + " tactic=" + this.activeTactic);
   }

   /** Per-tick: closes a reward window, folds the outcome into the profile, re-selects a tactic. */
   private void tickRewardWindow() {
      if (this.activeProfile == null || this.aiBot == null) {
         return;
      }
      this.rewardWindowTicks++;
      if (this.rewardWindowTicks >= REWARD_WINDOW_TICKS) {
         double dealt = this.aiBot.consumeDamageDealt();
         double taken = this.aiBot.consumeDamageTaken();
         double reward = dealt - taken;
         this.activeProfile.recordReward(this.activeTactic, reward);
         this.activeProfile.noteWindowStats(dealt, taken);
         this.rewardWindowTicks = 0;
         CombatTactic next = this.activeProfile.selectTactic();
         this.debugLog("ADAPT_REWARD tactic=" + this.activeTactic + " r=" + String.format("%.1f", reward)
            + " -> " + next + " " + this.activeProfile.summary());
         this.activeTactic = next;
         if (this.isMirror()) {
            this.refreshMirrorParams();
         }
      }
   }

   /** Persists the active profile and clears adaptive state when a fight ends. */
   private void endAdaptive() {
      if (this.activeProfile != null) {
         if (this.aiBot != null && this.rewardWindowTicks > 0) {
            double dealt = this.aiBot.consumeDamageDealt();
            double taken = this.aiBot.consumeDamageTaken();
            double scale = (double) REWARD_WINDOW_TICKS / (double) Math.max(1, this.rewardWindowTicks);
            this.activeProfile.recordReward(this.activeTactic, (dealt - taken) * scale);
         }
         this.opponentMemory.flush(this.adaptiveTargetId);
         this.debugLog("ADAPT_END " + this.activeProfile.summary());
      }
      this.activeProfile = null;
      this.activeTactic = null;
      this.adaptiveTargetId = null;
      this.rewardWindowTicks = 0;
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
               float threshold = (float) this.effAttackChargeThreshold();
               return cooldown >= 0.95F ? true : (isPlayerTarget || health < 8.0 || isSurrounded) && cooldown >= threshold;
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
               this.endAdaptive();
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
               this.updateAdaptiveTarget(target);
               this.combatTicks = 30;
               this.noTargetTicks = 0;
               nmsBot.setRotation(this.computeYaw(bot, target), this.computePitch(bot, target));
               if (justEnteredCombat) {
                  this.debugLog("COMBAT_START target=" + target.getName() + " dist=" + String.format("%.1f", dist));
                  this.selectBestWeapon(bot, nmsBot);

                  this.wasInCombat = true;
                  if (this.aiBot != null) {
                     this.aiBot.onCombatStart(bot);
                  }
               }

               if (this.combatTicks % 2 == 0) {
                  ItemStack hand = bot.getInventory().getItemInMainHand();
                  if (hand == null || hand.getType().isEdible() || this.weaponScore(hand.getType()) < 0) {
                     this.debugLog("WEAPON_SWITCH from=" + (hand != null ? hand.getType().name() : "EMPTY") + " reason=bad_weapon_in_hand");
                     this.lastAttackSlot = -1;
                     this.selectBestWeapon(bot, nmsBot);
                  }
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

               if (this.shieldBlockCooldown > 0) {
                  this.shieldBlockCooldown--;
               }

               this.tickRewardWindow();

               boolean hasSword = this.isHoldingSword(bot);
               double optimalMin = this.optimalMin(hasSword);
               double optimalMax = this.optimalMax(hasSword);
               double health = bot.getHealth();
               int recentHits = this.aiBot != null ? this.aiBot.getRecentHitCount(1500L) : 0;
               boolean underBurst = recentHits >= 2;
               boolean cautious = health < 11.0;
               boolean fleeing = health <= FLEE_HEALTH;
               boolean hasShield = this.hasShieldInOffhand(bot);
               boolean attackReady = this.isAttackReady(bot, target) && dist <= optimalMax + 0.3;
               int predictedShieldDelay = this.predictiveShieldDelay(target);
               if (this.isShieldBlocking) {
                  this.shieldBlockTicks++;
               }

               if (fleeing) {
                  if (this.isShieldBlocking && (attackReady || !hasShield)) {
                     this.releaseShield(nmsBot);
                  } else if (!this.isShieldBlocking && hasShield && this.shieldBlockCooldown <= 0) {
                     this.raiseShield(nmsBot);
                  }

                  double awaySide = (double)this.strafeDir * 0.5;
                  if (this.combatTicks % 6 == 0) {
                     this.strafeDir *= -1;
                  }

                  bot.setSprinting(dist < 6.0 && !this.isShieldBlocking);
                  nmsBot.walkRelative(-0.6, awaySide);
                  if (attackReady && dist <= optimalMin + 0.5) {
                     this.performAttack(bot, nmsBot, target, dist, 0.0);
                  }

                  this.combatTicks++;
                  return true;
               }

               if (this.isShieldBlocking) {
                  boolean stopBlock = attackReady
                     || this.shieldBlockTicks >= this.shieldHoldTarget
                     || dist > optimalMax + 1.5
                     || !underBurst && !cautious;
                  if (stopBlock) {
                     this.releaseShield(nmsBot);
                  } else {
                     nmsBot.walkRelative(dist < optimalMin ? -0.3 : 0.0, (double)this.strafeDir * 0.3);
                     if (this.combatTicks % 5 == 0) {
                        this.strafeDir *= -1;
                     }

                     this.combatTicks++;
                     return true;
                  }
               } else if (this.shieldRaiseDelay > 0) {
                  this.shieldRaiseDelay--;
                  if (this.shieldRaiseDelay <= 0 && hasShield && this.shieldBlockCooldown <= 0 && !attackReady) {
                     this.raiseShield(nmsBot);
                  }
               } else if (hasShield && this.shieldBlockCooldown <= 0 && !attackReady
                     && dist <= optimalMax + 1.2 && predictedShieldDelay > 0) {
                  this.shieldRaiseDelay = predictedShieldDelay;
               } else if (hasShield
                     && this.shieldBlockCooldown <= 0
                     && !attackReady
                     && (underBurst || cautious)
                     && dist <= optimalMax + 1.0
                     && Math.random() < this.effShieldChance()) {
                  this.shieldRaiseDelay = SHIELD_REACTION_MIN + (int)(Math.random() * (double)(SHIELD_REACTION_MAX - SHIELD_REACTION_MIN + 1));
               }

               double sideways = 0.0;
               double forward;
               if (dist > optimalMax) {
                  forward = 0.6;
                  bot.setSprinting(true);
               } else if (dist < optimalMin) {
                  forward = -0.4;
                  bot.setSprinting(false);
               } else {
                  forward = 0.6;
                  if (Math.random() < this.effStrafeChance()) {
                     sideways = (double)this.strafeDir * 0.6;
                     if (this.combatTicks % 3 == 0 && Math.random() < 0.5) {
                        this.strafeDir *= -1;
                     }
                  }

                  bot.setSprinting(true);
               }

               if (underBurst && !attackReady) {
                  forward = Math.min(forward, -0.3);
                  bot.setSprinting(false);
               }

               // Shield-aware offense: a sword swing into a raised shield is fully wasted, so circle to the
               // target's flank (the shield only covers its front semicylinder). An axe instead WANTS the hit
               // — it disables the shield for 5 s — so axe-wielding Kai keeps pressing head-on.
               boolean targetShielding = this.isTargetShielding(target);
               boolean swordVsShield = targetShielding && !this.isHoldingAxe(bot);
               boolean shieldArc = swordVsShield && this.inShieldArc(bot, target);
               if (shieldArc) {
                  sideways = (double) this.strafeDir * 0.8;
                  forward = Math.max(forward, 0.3);
                  bot.setSprinting(true);
               }

               nmsBot.walkRelative(forward, sideways);

               boolean grounded = bot.isOnGround();
               boolean fullCharge = bot.getAttackCooldown() >= 0.95F;
               // Damage discipline: in a clean trade (not eating a burst, not fleeing, not feeding a shield) Kai
               // holds fire until full charge and lands a *descending* jump-crit — ~150% damage instead of the
               // ~40% of a half-charged spam swing. Under burst/flee it drops this and trades fast to interrupt.
               boolean preferCrit = this.jumpCritEnabled && !underBurst && !fleeing && !swordVsShield
                  && dist <= optimalMax;
               boolean wantCrit = preferCrit && fullCharge;
               if (wantCrit && grounded && this.jumpCooldown <= 0) {
                  nmsBot.jump();
                  this.jumpCooldown = 12;
                  this.critPending = true;
               }
               // Abandon a pending crit if we landed without the descending window opening up.
               if (this.critPending && grounded && this.jumpCooldown < 8) {
                  this.critPending = false;
               }

               boolean doAttack;
               if (this.critPending) {
                  doAttack = this.canCritNow(bot);
               } else if (preferCrit) {
                  // Hold low-charge swings, but never waste a fully-charged window when the jump is still on cooldown.
                  doAttack = fullCharge && this.jumpCooldown > 0 && !shieldArc;
               } else {
                  doAttack = attackReady && !shieldArc;
               }
               if (doAttack) {
                  boolean crit = this.canCritNow(bot);
                  if (this.isShieldBlocking) {
                     this.releaseShield(nmsBot);
                  }
                  // Sprinting downgrades a crit into a sprint-knockback attack, so drop sprint on the crit tick.
                  if (crit) {
                     bot.setSprinting(false);
                  }
                  this.performAttack(bot, nmsBot, target, dist, sideways);
                  this.critPending = false;
               }

               if (dist > optimalMax + 1.0) {
                  this.comboCount = 0;
               }

               if (this.wTapEnabled && this.wTapCooldown > 0 && this.wTapCooldown <= 3) {
                  bot.setSprinting(false);
                  double retreat = this.comboCount >= 3 || this.kiteActive() ? -0.3 : -0.15;
                  nmsBot.walkRelative(retreat, sideways * 0.2);
                  if (this.comboCount >= 3) {
                     this.comboCount = 0;
                  }
               }

               this.combatTicks++;
               return true;
            }
         } else {
            if (this.currentTarget != null) {
               this.noTargetTicks++;
               if (this.noTargetTicks >= 40) {
                  this.endAdaptive();
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

   private void raiseShield(NMSBot nmsBot) {
      this.isShieldBlocking = true;
      this.shieldBlockTicks = 0;
      this.shieldRaiseDelay = 0;
      this.shieldHoldTarget = SHIELD_HOLD_MIN + (int)(Math.random() * (double)(SHIELD_HOLD_MAX - SHIELD_HOLD_MIN + 1));
      nmsBot.startBlocking();
      this.debugLog("SHIELD_RAISE hold=" + this.shieldHoldTarget);
   }

   private void releaseShield(NMSBot nmsBot) {
      if (this.isShieldBlocking) {
         this.isShieldBlocking = false;
         this.shieldBlockTicks = 0;
         this.shieldBlockCooldown = SHIELD_COOLDOWN;
         nmsBot.stopBlocking();
         this.debugLog("SHIELD_RELEASE");
      }
   }

   private void performAttack(Player bot, NMSBot nmsBot, Entity target, double dist, double sideways) {
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
      if (!this.canReach(bot, target, dist)) {
         return;
      }
      nmsBot.swingMainHand();
      bot.attack(target);
      this.comboCount++;
      this.attackCooldown = this.getWeaponCooldownTicks(bot);
      this.wTapCooldown = 6;
      this.blockHitCooldown = 3;
      nmsBot.walkRelative(0.4, sideways * 0.3);
   }

   /**
    * Legitimacy gate for melee: only land a hit within vanilla reach (3.0 blocks,
    * center-to-center) and with a clear line of sight, so Kai can never reach past
    * a real player nor strike through walls. Movement still closes the gap; this only
    * suppresses the actual damage swing when it would be illegal.
    */
   private boolean canReach(Player bot, Entity target, double dist) {
      if (dist > ATTACK_REACH) {
         return false;
      }
      return bot.hasLineOfSight(target);
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

   private boolean isHoldingAxe(Player bot) {
      ItemStack hand = bot.getInventory().getItemInMainHand();
      return hand != null && hand.getType().name().endsWith("_AXE");
   }

   private boolean isTargetShielding(Entity target) {
      return target instanceof Player p && p.isBlocking();
   }

   /** True while Kai stands within the target's frontal arc, where a held shield blocks. */
   private boolean inShieldArc(Player bot, Entity target) {
      Vector facing = target.getLocation().getDirection().setY(0.0);
      Vector toBot = bot.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0);
      if (facing.lengthSquared() < 1.0E-4 || toBot.lengthSquared() < 1.0E-4) {
         return true;
      }
      return facing.normalize().dot(toBot.normalize()) > 0.0;
   }

   /** Vanilla crit window: airborne, descending, out of water, and charge >= 84.8%. */
   private boolean canCritNow(Player bot) {
      if (bot.isOnGround() || bot.isInWater()) {
         return false;
      }
      boolean descending = bot.getVelocity().getY() < 0.0 || (this.jumpCooldown > 0 && this.jumpCooldown <= 6);
      if (!descending) {
         return false;
      }
      return bot.getAttackCooldown() >= CRIT_CHARGE;
   }

   private boolean hasShieldInOffhand(Player bot) {
      ItemStack off = bot.getInventory().getItemInOffHand();
      return off != null && off.getType().name().contains("SHIELD");
   }

   private void selectBestWeapon(Player bot, NMSBot nmsBot) {
      int bestSlot = -1;
      double bestDps = 0.0;
      PlayerInventory inv = bot.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null) {
            double dps = this.estimateMeleeDamage(item);
            if (dps > bestDps) {
               bestDps = dps;
               bestSlot = i;
            }
         }
      }

      if (bestSlot >= 0 && bestSlot != this.lastAttackSlot) {
         nmsBot.selectHotbarSlot(bestSlot);
         this.lastAttackSlot = bestSlot;
      }
   }

   /**
    * Enchant-aware melee strength used to pick the genuinely strongest weapon: expected DPS =
    * (base per-hit damage + Sharpness bonus) x attack speed. This lets a Sharpness-V diamond sword
    * out-rank a plain netherite one, and keeps the high-DPS sword over the slower axe by default
    * (the axe is still used opportunistically for shield-breaking in the attack logic).
    * Returns -1 for food / non-weapons.
    */
   private double estimateMeleeDamage(ItemStack item) {
      if (item == null) {
         return -1.0;
      }
      Material t = item.getType();
      if (t.isEdible()) {
         return -1.0;
      }
      double base = this.baseWeaponDamage(t);
      if (base <= 0.0) {
         return -1.0;
      }
      double speed = this.weaponAttackSpeed(t);
      double perHit = base;
      int sharp = item.getEnchantmentLevel(Enchantment.SHARPNESS);
      if (sharp > 0) {
         perHit += 0.5 * sharp + 0.5;
      }
      return perHit * speed;
   }

   /** Vanilla weapon-only base attack damage (displayed value). */
   private double baseWeaponDamage(Material t) {
      String n = t.name();
      if (n.endsWith("_SWORD")) {
         if (t == Material.NETHERITE_SWORD) return 8.0;
         if (t == Material.DIAMOND_SWORD) return 7.0;
         if (t == Material.IRON_SWORD) return 6.0;
         if (t == Material.STONE_SWORD) return 5.0;
         return 4.0;
      }
      if (n.endsWith("_AXE")) {
         if (t == Material.NETHERITE_AXE) return 10.0;
         if (t == Material.DIAMOND_AXE) return 9.0;
         if (t == Material.IRON_AXE) return 9.0;
         if (t == Material.STONE_AXE) return 9.0;
         return 7.0;
      }
      if (t == Material.TRIDENT) return 9.0;
      if (n.equals("MACE")) return 6.0;
      if (n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")) return 2.5;
      return 0.0;
   }

   /** Vanilla attack speed (attacks per second) for the weapon classes Kai uses. */
   private double weaponAttackSpeed(Material t) {
      String n = t.name();
      if (n.endsWith("_SWORD")) return 1.6;
      if (n.endsWith("_AXE")) return 1.0;
      if (t == Material.TRIDENT) return 1.1;
      if (n.equals("MACE")) return 0.6;
      return 1.0;
   }

   private int weaponScore(Material type) {
      if (type != null && type.isEdible()) {
         return -1;
      } else if (type != null && type.name().equals("MACE")) {
         return 22;
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
