package com.jonasmp.ai.combat;

/**
 * Persistent, long-term summary of how one opponent attacks Kai. Plain fields for
 * Gson (de)serialisation. Players get their own file (keyed by UUID, named after the
 * player); mobs are aggregated per entity type (one file per mob type) and learn
 * more slowly because their attack pattern barely changes.
 *
 * <p>This is the on-disk mirror of a live {@link ComboTracker}: the tracker seeds
 * itself from a profile on combat start and writes its aggregates back on flush.
 */
public final class ComboProfile {

   public String id = "";
   public String name = "";
   public boolean mob;

   public long firstSeen = System.currentTimeMillis();
   public long lastSeen = System.currentTimeMillis();
   public long lastSavedAt;

   public int totalHits;

   /** Rhythm: mean and rough spread of the gap between consecutive combo hits (ms). */
   public double avgIntervalMs;
   public double intervalSpreadMs;

   public double avgDistance;
   public double avgComboLength;
   public double sprintRate;
   public double critRate;

   /** Cumulative (decayed) counts per {@link HitSample.Side}: FRONT, LEFT, RIGHT, BACK. */
   public int[] sideCounts = new int[4];

   public ComboProfile() {
   }

   public ComboProfile(String id, String name, boolean mob) {
      this.id = id;
      this.name = name;
      this.mob = mob;
   }

   public void normalise() {
      if (this.sideCounts == null || this.sideCounts.length != 4) {
         int[] s = new int[4];
         if (this.sideCounts != null) {
            System.arraycopy(this.sideCounts, 0, s, 0, Math.min(this.sideCounts.length, 4));
         }
         this.sideCounts = s;
      }
   }

   /** Index of the side the opponent hits from most often, or -1 if unknown. */
   public int dominantSide() {
      int best = -1;
      int bestCount = 0;
      for (int i = 0; i < this.sideCounts.length; i++) {
         if (this.sideCounts[i] > bestCount) {
            bestCount = this.sideCounts[i];
            best = i;
         }
      }
      return best;
   }
}
