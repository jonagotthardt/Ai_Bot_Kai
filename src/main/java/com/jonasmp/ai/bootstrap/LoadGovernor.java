package com.jonasmp.ai.bootstrap;

import org.bukkit.Bukkit;

/**
 * Lightweight server-load governor.
 *
 * <p>Kai's control loop is already tick-bound — it runs once per server tick via
 * {@code runTaskTimer(..., 1L)}, so it automatically slows down together with the
 * server when TPS drops (it can never tick faster than the server). This governor
 * does NOT change that scheduling; it only decides how much <em>optional</em>
 * per-tick work (perception block-scans, radar chunk reads, gear/enchant scans)
 * is affordable right now, so Kai never amplifies a lag spiral while staying
 * responsive in combat.
 *
 * <p>It reads Paper's rolling TPS ({@link org.bukkit.Server#getTPS()}) and the
 * mean tick time ({@link org.bukkit.Server#getAverageTickTime()}) and returns the
 * more severe of the two verdicts. The mean-tick-time signal reacts faster to
 * sudden spikes than the 1-minute TPS average. Reading both is a couple of array
 * accesses — safe and negligible on the main thread.
 */
public final class LoadGovernor {

   public enum Load {
      /** Server healthy: full behaviour. */
      NORMAL,
      /** Mild lag: stretch expensive cadences, skip non-essential scans. */
      STRESSED,
      /** Heavy lag: combat + movement only, all optional work paused. */
      CRITICAL
   }

   private static final double STRESSED_TPS = 18.0;
   private static final double CRITICAL_TPS = 14.0;
   // Fallback / fast signal: mean tick time in ms (50 ms == 20 TPS).
   private static final double STRESSED_MSPT = 47.0;
   private static final double CRITICAL_MSPT = 55.0;

   private LoadGovernor() {
   }

   public static Load current() {
      Load byTps = Load.NORMAL;
      double tps = readTps();
      if (tps > 0.0) {
         byTps = tps < CRITICAL_TPS ? Load.CRITICAL : (tps < STRESSED_TPS ? Load.STRESSED : Load.NORMAL);
      }

      Load byMspt = Load.NORMAL;
      double mspt = readMspt();
      if (mspt > 0.0) {
         byMspt = mspt >= CRITICAL_MSPT ? Load.CRITICAL : (mspt >= STRESSED_MSPT ? Load.STRESSED : Load.NORMAL);
      }

      return byTps.ordinal() >= byMspt.ordinal() ? byTps : byMspt;
   }

   public static boolean isCritical() {
      return current() == Load.CRITICAL;
   }

   private static double readTps() {
      try {
         double[] tps = Bukkit.getServer().getTPS();
         if (tps != null && tps.length > 0) {
            return tps[0];
         }
      } catch (Throwable var1) {
      }

      return -1.0;
   }

   private static double readMspt() {
      try {
         return Bukkit.getServer().getAverageTickTime();
      } catch (Throwable var1) {
         return -1.0;
      }
   }
}
