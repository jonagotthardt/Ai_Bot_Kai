package com.jonasmp.ai.combat;

/**
 * Discrete combat "presets" the adaptive PvP layer chooses between per opponent.
 *
 * <p>Each preset bundles the handful of stellgrößen the {@code BotCombatManager}
 * reads every tick (strafe frequency, preferred range, shield readiness, whether
 * to trade aggressively). The adaptive layer measures reward per preset against a
 * given opponent and keeps picking the one that works — with a small exploration
 * chance so it can re-adapt when the opponent changes their own style.
 *
 * <p>Ranges are expressed for a sword baseline; the combat manager shifts them in
 * a bit for short-reach weapons (axe). Values are deliberately conservative and
 * overlapping so no single preset is strictly dominant — the difference only pays
 * off against opponents it actually counters.
 */
public enum CombatTactic {

   /** Close, relentless pressure. Low blocking, attacks on small openings. Good vs. passive/turtling foes. */
   AGGRESSIVE(0.55, 1.5, 2.6, 0.25, 0.45, false),

   /** All-round default: solid spacing, balanced blocking, clean trades. */
   BALANCED(0.80, 1.5, 3.0, 0.55, 0.60, false),

   /** Kite & punish: max spacing, high blocking, retreats after each trade. Good vs. aggressive rushers/combo players. */
   HIT_AND_RUN(0.90, 2.2, 3.6, 0.65, 0.65, true),

   /**
    * Offensive mirror: turns the opponent's own successful tempo against them. Its
    * fields here are only the fallback defaults used until enough combo data exists;
    * once the combo-learner has a confident profile, {@code BotCombatManager} derives
    * the live range / attack cadence / shield bias from the opponent's measured rhythm
    * and distance. Sits in the bandit like any other arm, so it is reward-gated — Kai
    * only keeps mirroring when it actually out-trades that specific opponent.
    */
   MIRROR(0.75, 1.6, 3.0, 0.40, 0.55, false);

   /** Probability per eligible tick that the bot strafes sideways. */
   public final double strafeChance;
   /** Preferred minimum distance to the target (blocks, sword baseline). */
   public final double rangeMin;
   /** Preferred maximum distance to the target (blocks, sword baseline). */
   public final double rangeMax;
   /** Probability the bot raises its shield when a block trigger fires. */
   public final double shieldChance;
   /** Attack-cooldown charge fraction (0..1) required before swinging at a player. Lower = more aggressive. */
   public final double attackChargeThreshold;
   /** Whether to bias movement toward retreating after a trade (kiting). */
   public final boolean kite;

   CombatTactic(double strafeChance, double rangeMin, double rangeMax,
                double shieldChance, double attackChargeThreshold, boolean kite) {
      this.strafeChance = strafeChance;
      this.rangeMin = rangeMin;
      this.rangeMax = rangeMax;
      this.shieldChance = shieldChance;
      this.attackChargeThreshold = attackChargeThreshold;
      this.kite = kite;
   }
}
