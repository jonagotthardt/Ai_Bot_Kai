package com.jonasmp.ai.combat;

/**
 * Per-opponent learning record. Tracks how well each {@link CombatTactic} has
 * performed against one specific opponent and picks the next tactic with an
 * epsilon-greedy bandit.
 *
 * <p>Plain fields (no getters/setters) because instances are (de)serialised with
 * Gson exactly like {@code BotMemory}. All learning is cheap statistics — a reward
 * EWMA per tactic — so an update is O(number of tactics) and runs at most once per
 * reward window (~1s), never per tick.
 *
 * <p>Reward is "net combat value" over a window: damage Kai dealt minus damage it
 * took. Positive means the current tactic is winning the exchange.
 */
public final class OpponentProfile {

   /** EWMA smoothing factor for reward updates (higher = adapts faster, noisier). */
   private static final double REWARD_ALPHA = 0.30;
   /** Exploration probability: chance to try a non-best tactic. Kept > 0 so Kai never freezes into one readable pattern. */
   private static final double EPSILON = 0.18;
   /** Per-day decay applied to learned rewards on load so stale knowledge fades. */
   private static final double DAILY_DECAY = 0.85;
   private static final long DAY_MS = 86_400_000L;

   public String uuid = "";
   public String name = "";
   public long firstSeen = System.currentTimeMillis();
   public long lastSeen = System.currentTimeMillis();
   public int encounters;

   /** Reward EWMA per tactic, indexed by {@link CombatTactic#ordinal()}. */
   public double[] tacticReward = new double[CombatTactic.values().length];
   /** How often each tactic was selected (for exploration of untried tactics + diagnostics). */
   public int[] tacticUses = new int[CombatTactic.values().length];

   /** Aggregate diagnostics (not used for control yet, useful for tuning / debug). */
   public double avgDamageTakenPerWindow;
   public double avgDamageDealtPerWindow;

   public OpponentProfile() {
   }

   public OpponentProfile(String uuid, String name) {
      this.uuid = uuid;
      this.name = name;
   }

   /** Repairs arrays after deserialisation (e.g. if the tactic count changed between versions). */
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
         int[] u = new int[n];
         if (this.tacticUses != null) {
            System.arraycopy(this.tacticUses, 0, u, 0, Math.min(this.tacticUses.length, n));
         }
         this.tacticUses = u;
      }
   }

   /** Fades learned rewards toward 0 based on how long ago this opponent was last seen. */
   public void applyTimeDecay() {
      long elapsed = System.currentTimeMillis() - this.lastSeen;
      if (elapsed <= 0L) {
         return;
      }
      double days = (double) elapsed / (double) DAY_MS;
      double factor = Math.pow(DAILY_DECAY, days);
      for (int i = 0; i < this.tacticReward.length; i++) {
         this.tacticReward[i] *= factor;
      }
   }

   /**
    * Chooses the next tactic. Any never-tried tactic is picked first (cheap initial
    * exploration); otherwise epsilon-greedy: usually the best-reward tactic, sometimes
    * a random one so the bot keeps probing and can re-adapt to an opponent's changes.
    */
   public CombatTactic selectTactic() {
      CombatTactic[] all = CombatTactic.values();

      for (CombatTactic t : all) {
         if (this.tacticUses[t.ordinal()] == 0) {
            return t;
         }
      }

      if (Math.random() < EPSILON) {
         return all[(int) (Math.random() * all.length)];
      }

      CombatTactic best = all[0];
      double bestReward = this.tacticReward[0];
      for (int i = 1; i < all.length; i++) {
         if (this.tacticReward[i] > bestReward) {
            bestReward = this.tacticReward[i];
            best = all[i];
         }
      }
      return best;
   }

   /** Folds one window's outcome into the EWMA for the tactic that was active. */
   public void recordReward(CombatTactic tactic, double reward) {
      int i = tactic.ordinal();
      this.tacticUses[i]++;
      this.tacticReward[i] += REWARD_ALPHA * (reward - this.tacticReward[i]);
      this.lastSeen = System.currentTimeMillis();
   }

   public void noteWindowStats(double damageDealt, double damageTaken) {
      this.avgDamageDealtPerWindow += 0.2 * (damageDealt - this.avgDamageDealtPerWindow);
      this.avgDamageTakenPerWindow += 0.2 * (damageTaken - this.avgDamageTakenPerWindow);
   }

   /** Human-readable snapshot for debug logging. */
   public String summary() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.name).append('[');
      CombatTactic[] all = CombatTactic.values();
      for (int i = 0; i < all.length; i++) {
         if (i > 0) {
            sb.append(' ');
         }
         sb.append(all[i].name(), 0, 3).append('=')
            .append(String.format("%.2f", this.tacticReward[i]))
            .append('/').append(this.tacticUses[i]);
      }
      return sb.append(']').toString();
   }
}
