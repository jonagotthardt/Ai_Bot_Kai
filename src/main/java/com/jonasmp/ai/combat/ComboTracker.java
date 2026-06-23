package com.jonasmp.ai.combat;

/**
 * Live, in-RAM analysis of one opponent's incoming combo while a fight is ongoing.
 * Seeded from a persisted {@link ComboProfile} so prior knowledge applies from the
 * first tick, it folds each {@link HitSample} into cheap EWMA aggregates (no scans,
 * no allocation per hit) and predicts when the next hit of an ongoing combo is due.
 *
 * <p>"Combo" = a run of hits each within {@link #COMBO_GAP_MS} of the previous one.
 * A larger gap starts a new combo. The mean inter-hit interval is the rhythm Kai
 * uses to time its defensive reaction.
 */
public final class ComboTracker {

   /** Hits closer together than this (ms) belong to the same combo. */
   private static final long COMBO_GAP_MS = 900L;
   /** Learning rate for players (reactive) vs. mobs (slow — they don't change tactics). */
   private static final double ALPHA_PLAYER = 0.30;
   private static final double ALPHA_MOB = 0.08;
   /** Hits needed before predictions are considered trustworthy. */
   private static final int CONFIDENCE_HITS = 4;

   private final boolean mob;
   private final double alpha;

   private long lastHitTime;
   private int currentComboLen;
   private boolean dirty;

   private double avgIntervalMs;
   private double intervalSpreadMs;
   private double avgDistance;
   private double avgComboLength;
   private double sprintRate;
   private double critRate;
   private final int[] sideCounts = new int[4];
   private int totalHits;

   public ComboTracker(ComboProfile seed) {
      this.mob = seed != null && seed.mob;
      this.alpha = this.mob ? ALPHA_MOB : ALPHA_PLAYER;
      if (seed != null) {
         seed.normalise();
         this.avgIntervalMs = seed.avgIntervalMs;
         this.intervalSpreadMs = seed.intervalSpreadMs;
         this.avgDistance = seed.avgDistance;
         this.avgComboLength = seed.avgComboLength;
         this.sprintRate = seed.sprintRate;
         this.critRate = seed.critRate;
         this.totalHits = seed.totalHits;
         System.arraycopy(seed.sideCounts, 0, this.sideCounts, 0, 4);
      }
   }

   public void record(HitSample s) {
      if (this.lastHitTime > 0L && s.time - this.lastHitTime <= COMBO_GAP_MS) {
         double interval = s.time - this.lastHitTime;
         double delta = interval - this.avgIntervalMs;
         if (this.avgIntervalMs <= 0.0) {
            this.avgIntervalMs = interval;
         } else {
            this.avgIntervalMs += this.alpha * delta;
            this.intervalSpreadMs += this.alpha * (Math.abs(delta) - this.intervalSpreadMs);
         }
         this.currentComboLen++;
      } else {
         if (this.currentComboLen > 0) {
            this.foldComboLength(this.currentComboLen);
         }
         this.currentComboLen = 1;
      }

      this.avgDistance = ewma(this.avgDistance, s.distance);
      this.sprintRate = ewma(this.sprintRate, s.sprint ? 1.0 : 0.0);
      this.critRate = ewma(this.critRate, s.crit ? 1.0 : 0.0);
      if (s.side != null) {
         this.sideCounts[s.side.ordinal()]++;
      }
      this.totalHits++;
      this.lastHitTime = s.time;
      this.dirty = true;
   }

   private void foldComboLength(int len) {
      this.avgComboLength = this.avgComboLength <= 0.0 ? len : this.avgComboLength + this.alpha * (len - this.avgComboLength);
   }

   private double ewma(double current, double sample) {
      return current <= 0.0 ? sample : current + this.alpha * (sample - current);
   }

   public boolean isComboActive(long now) {
      return this.totalHits >= 2 && this.avgIntervalMs > 0.0 && now - this.lastHitTime <= COMBO_GAP_MS;
   }

   /**
    * Milliseconds until the next hit of the ongoing combo is expected, or
    * {@link Long#MAX_VALUE} if no combo is active / no rhythm learned yet.
    * May be negative if the predicted moment has just passed (still in the window).
    */
   public long nextHitEtaMs(long now) {
      if (!this.isComboActive(now)) {
         return Long.MAX_VALUE;
      }
      return (long) (this.lastHitTime + this.avgIntervalMs) - now;
   }

   /** 0..1 trust in this tracker's predictions, scaling with how much it has observed. */
   public double confidence() {
      return Math.min(1.0, (double) this.totalHits / (double) CONFIDENCE_HITS);
   }

   public double avgIntervalMs() {
      return this.avgIntervalMs;
   }

   public double avgDistance() {
      return this.avgDistance;
   }

   public long lastHitTime() {
      return this.lastHitTime;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public boolean isMob() {
      return this.mob;
   }

   /** Copies the live aggregates back into a profile for persistence. */
   public void writeInto(ComboProfile profile) {
      profile.normalise();
      profile.totalHits = this.totalHits;
      profile.avgIntervalMs = this.avgIntervalMs;
      profile.intervalSpreadMs = this.intervalSpreadMs;
      profile.avgDistance = this.avgDistance;
      profile.avgComboLength = this.avgComboLength;
      profile.sprintRate = this.sprintRate;
      profile.critRate = this.critRate;
      System.arraycopy(this.sideCounts, 0, profile.sideCounts, 0, 4);
      profile.lastSeen = System.currentTimeMillis();
      this.dirty = false;
   }
}
