package com.jonasmp.ai.watcher;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Feeds incoming-damage events to the bot so it can retaliate against the player
 * who hit it. This is the trigger that makes {@link BotCombatManager#findPriorityTarget}
 * prioritise an actual attacker over the nearest monster.
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
         this.aiBot.noteIncomingHit();
         this.aiBot.noteDamageTaken(event.getFinalDamage());

         Player attacker = resolveAttacker(event);
         if (attacker != null && !attacker.equals(bot)) {
            this.aiBot.recordPlayerDamage(attacker);
         }
         return;
      }

      if (bot.equals(event.getDamager()) && !event.getEntity().equals(bot)) {
         this.aiBot.noteDamageDealt(event.getFinalDamage());
      }
   }

   private static Player resolveAttacker(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player direct) {
         return direct;
      }

      if (event.getDamager() instanceof Projectile projectile) {
         ProjectileSource source = projectile.getShooter();
         if (source instanceof Player shooter) {
            return shooter;
         }
      }

      return null;
   }
}
