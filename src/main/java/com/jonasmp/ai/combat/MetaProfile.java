package com.jonasmp.ai.combat;

/**
 * Global, cross-fight learning record — Kai's *actual* evolving fighting style.
 *
 * <p>Where {@link OpponentProfile} learns one specific opponent and is forgotten/decayed
 * over time, this single persistent profile folds the outcome of <em>every</em> exchange
 * against <em>every</em> opponent into one slow long-term aggregate: how well each
 * {@link CombatTactic} tends to perform overall. Two jobs:
 * <ul>
 *   <li><b>Warm start:</b> a brand-new opponent's bandit is seeded with this prior, so Kai
 *       opens against a stranger with the tactic his accumulated experience says wins most
 *       — instead of cold-cycling through every option from scratch.</li>
 *   <li><b>Continuous improvement:</b> every fight nudges the prior, so Kai's baseline style
 *       keeps shifting toward what actually works for him across many opponents.</li>
 * </ul>
 *
 * <p>Only the tactic bandit generalises across opponents and is aggregated here. Combo
 * rhythm/spacing stay per-opponent (they don't transfer — every opponent's timing differs).
 *
 * <p>Plain fields for Gson, exactly like {@link OpponentProfile}. Updates are O(tactics)
 * and happen at most once per reward window, never per tick.
 */
public final class MetaProfile {

   /** Slow EWMA: the global style should drift, not lurch from a single fight. */
   private static final double META_ALPHA = 0.05;
   /** Need at least this many recorded windows before the prior is trusted enough to seed new opponents. */
   private static final int MIN_UPDATES_TO_SEED = 8;

   public long updates;
   public long lastUpdated = System.currentTimeMillis();

   /** Long-term reward EWMA per tactic, indexed by {@link CombatTactic#ordinal()}. */
   public double[] tacticReward = new double[CombatTactic.values().length];
   /** Total windows attributed to each tactic across all opponents (diagnostics). */
   public long[] tacticUses = new long[CombatTactic.values().length];

   public MetaProfile() {
   }

   /** Repairs arrays after deserialisation if the tactic count changed between versions. */
   public void normalise() {
      int n = CombatTactic.values().length;
      if (this.tacticReward == null || this.tacticReward.length != n) {
         double[] r = new double[n];
         if (this.tacticReward != null) {
            System.arraycopy(this.tacticReward, 0, r, 0, Math.min(this.tacticReward.length, n));
         }
         this.tacticReward = r;
      }
      if (this.tacticUses == null || this.tacticUses.length != n) {
         long[] u = new long[n];
         if (this.tacticUses != null) {
            System.arraycopy(this.tacticUses, 0, u, 0, Math.min(this.tacticUses.length, n));
         }
         this.tacticUses = u;
      }
   }

   /** Folds one window's outcome into the long-term style. */
   public void recordReward(CombatTactic tactic, double reward) {
      this.normalise();
      int i = tactic.ordinal();
      this.tacticUses[i]++;
      this.tacticReward[i] += META_ALPHA * (reward - this.tacticReward[i]);
      this.updates++;
      this.lastUpdated = System.currentTimeMillis();
   }

   /** Whether enough experience has accumulated for the prior to be worth seeding. */
   public boolean hasPrior() {
      return this.updates >= MIN_UPDATES_TO_SEED;
   }

   /**
    * Warm-starts a fresh (untrained) opponent profile with this global prior: copies the
    * learned per-tactic rewards and marks each tactic as virtually tried once, so the bandit
    * exploits the prior immediately while still exploring (epsilon) and quickly overwriting it
    * with opponent-specific data (the per-opponent EWMA adapts far faster than this one).
    */
   public void seedInto(OpponentProfile profile) {
      if (!this.hasPrior() || profile == null) {
         return;
      }
      this.normalise();
      profile.normalise();
      int n = Math.min(profile.tacticReward.length, this.tacticReward.length);
      for (int i = 0; i < n; i++) {
         profile.tacticReward[i] = this.tacticReward[i];
         if (profile.tacticUses[i] == 0) {
            profile.tacticUses[i] = 1;
         }
      }
   }

   /** Human-readable snapshot for debug logging. */
   public String summary() {
      StringBuilder sb = new StringBuilder("meta(").append(this.updates).append(")[");
      CombatTactic[] all = CombatTactic.values();
      for (int i = 0; i < all.length && i < this.tacticReward.length; i++) {
         if (i > 0) {
            sb.append(' ');
         }
         sb.append(all[i].name(), 0, 3).append('=').append(String.format("%.2f", this.tacticReward[i]));
      }
      return sb.append(']').toString();
   }
}
