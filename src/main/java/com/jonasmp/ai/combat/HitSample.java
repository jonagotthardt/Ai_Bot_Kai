package com.jonasmp.ai.combat;

/**
 * One observed incoming hit on Kai, captured server-side at the moment of damage.
 * Pure value object — building it is O(1) and touches no world data beyond reading
 * the attacker's already-loaded state.
 */
public final class HitSample {

   /** Which side of Kai the hit came from, relative to where Kai was facing. */
   public enum Side {
      FRONT, LEFT, RIGHT, BACK
   }

   public final long time;
   public final double distance;
   public final float attackerYaw;
   public final float attackerPitch;
   /** How directly the attacker was moving toward Kai: 1 = straight in, 0 = sideways, <0 = retreating. */
   public final double approachDot;
   public final Side side;
   public final boolean sprint;
   public final boolean crit;
   public final String weapon;
   public final double damage;

   public HitSample(long time, double distance, float attackerYaw, float attackerPitch,
                    double approachDot, Side side, boolean sprint, boolean crit,
                    String weapon, double damage) {
      this.time = time;
      this.distance = distance;
      this.attackerYaw = attackerYaw;
      this.attackerPitch = attackerPitch;
      this.approachDot = approachDot;
      this.side = side;
      this.sprint = sprint;
      this.crit = crit;
      this.weapon = weapon;
      this.damage = damage;
   }
}
