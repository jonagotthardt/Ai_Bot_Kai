package com.jonasmp.ai.watcher;

import com.jonasmp.ai.combat.HitSample;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

/**
 * Feeds incoming-damage events to the bot so it can retaliate against the player
 * who hit it, and captures rich per-hit telemetry for the combo-learner.
 *
 * <p>Event-driven (no polling) and does only O(1) work per hit, so it adds no
 * per-tick cost. It reacts to both melee hits and projectiles fired by a player
 * (arrows, tridents, etc.) — the projectile's shooter is resolved to the player.
 */
public final class BotCombatListener implements Listener {

   private final AIPlayerBot aiBot;

   public BotCombatListener(AIPlayerBot aiBot) {
      this.aiBot = aiBot;
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onCombat(EntityDamageByEntityEvent event) {
      if (this.aiBot == null || !this.aiBot.isSpawned()) {
         return;
      }

      Player bot = this.aiBot.getNMSBot().getPlayer();
      if (bot == null) {
         return;
      }

      if (event.getEntity().equals(bot)) {
         double damage = event.getFinalDamage();
         this.aiBot.noteIncomingHit();
         this.aiBot.noteDamageTaken(damage);

         LivingEntity attacker = resolveAttacker(event);
         if (attacker != null && !attacker.equals(bot)) {
            if (attacker instanceof Player playerAttacker) {
               this.aiBot.recordPlayerDamage(playerAttacker);
            }
            this.aiBot.getComboLearner().recordHit(attacker, buildSample(bot, attacker, damage));
         }
         return;
      }

      if (bot.equals(event.getDamager()) && !event.getEntity().equals(bot)) {
         this.aiBot.noteDamageDealt(event.getFinalDamage());
      }
   }

   /** Captures the server-observable details of one incoming hit. O(1), no world scan. */
   private static HitSample buildSample(Player bot, LivingEntity attacker, double damage) {
      Location botLoc = bot.getLocation();
      Location atkLoc = attacker.getLocation();
      double distance = botLoc.distance(atkLoc);

      Vector forward = botLoc.getDirection().setY(0.0);
      Vector toAttacker = atkLoc.toVector().subtract(botLoc.toVector()).setY(0.0);
      HitSample.Side side = resolveSide(forward, toAttacker);

      double approachDot = 0.0;
      Vector vel = attacker.getVelocity().setY(0.0);
      if (vel.lengthSquared() > 1.0E-4 && toAttacker.lengthSquared() > 1.0E-4) {
         Vector toBot = toAttacker.clone().multiply(-1.0).normalize();
         approachDot = vel.normalize().dot(toBot);
      }

      boolean sprint = attacker instanceof Player p && p.isSprinting();
      boolean crit = attacker instanceof Player pc
         && !pc.isOnGround()
         && pc.getFallDistance() > 0.0F
         && !pc.isSprinting();
      String weapon = resolveWeapon(attacker);

      return new HitSample(System.currentTimeMillis(), distance, atkLoc.getYaw(), atkLoc.getPitch(),
         approachDot, side, sprint, crit, weapon, damage);
   }

   /** FRONT/BACK by alignment, LEFT/RIGHT by horizontal cross product. Convention is consistent, not absolute. */
   private static HitSample.Side resolveSide(Vector forward, Vector toAttacker) {
      if (forward.lengthSquared() < 1.0E-4 || toAttacker.lengthSquared() < 1.0E-4) {
         return HitSample.Side.FRONT;
      }
      Vector f = forward.clone().normalize();
      Vector t = toAttacker.clone().normalize();
      double dot = f.dot(t);
      if (dot > 0.5) {
         return HitSample.Side.FRONT;
      }
      if (dot < -0.5) {
         return HitSample.Side.BACK;
      }
      double crossY = f.getZ() * t.getX() - f.getX() * t.getZ();
      return crossY > 0.0 ? HitSample.Side.RIGHT : HitSample.Side.LEFT;
   }

   private static String resolveWeapon(LivingEntity attacker) {
      if (attacker.getEquipment() != null) {
         ItemStack hand = attacker.getEquipment().getItemInMainHand();
         if (hand != null && !hand.getType().isAir()) {
            return hand.getType().name();
         }
      }
      return attacker instanceof Player ? "FIST" : attacker.getType().name();
   }

   private static LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof LivingEntity direct) {
         return direct;
      }

      if (event.getDamager() instanceof Projectile projectile) {
         ProjectileSource source = projectile.getShooter();
         if (source instanceof LivingEntity shooter) {
            return shooter;
         }
      }

      return null;
   }
}
