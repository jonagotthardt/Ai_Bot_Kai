package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WatcherHorror {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final Random random = ThreadLocalRandom.current();
   private UUID horrorTarget = null;
   private int horrorPhase = 0;
   private long nextHorrorTick = 0L;
   private long nextWhisper = 0L;
   private int whisperIndex = 0;
   private long lastTeleportTime = 0L;
   private long lastPotionTime = 0L;
   private static final long TELEPORT_COOLDOWN_MS = 2500L;
   private static final long POTION_COOLDOWN_MS = 3000L;
   private final List<String> PHASE1_WHISPERS = Arrays.asList(
      "§8§oDu hörst mich nicht... aber ich höre dich.",
      "§8§oDie Dunkelheit hier ist nicht normal.",
      "§8§oDein Herz schlägt schneller. Ich merke es.",
      "§8§oJemand schaut dich an. Schau nicht hin.",
      "§8§oDie Welt hier merkt sich jeden Schritt.",
      "§8§oDu bist nicht allein. Du warst es nie.",
      "§8§oDie Blöcke hinter dir... sie haben sich verändert.",
      "§8§oKennst du das Gefühl, beobachtet zu werden?"
   );
   private final List<String> PHASE2_WHISPERS = Arrays.asList(
      "§4§lIch bin hinter dir.",
      "§4§lDreh dich nicht um.",
      "§4§oDein eigener Schatten... ist nicht deiner.",
      "§4§oIch kopiere jeden deiner Schritte.",
      "§4§lSchau in die Dunkelheit. Ich schaue zurück.",
      "§4§oWer hat das Schild platziert? Ich weiß es.",
      "§4§lDu hörst Schritte. §oAber du bist allein.",
      "§4§oDie Kisten waren gestern noch voll. Wo ist alles hin?"
   );
   private final List<String> PHASE3_WHISPERS = Arrays.asList(
      "§0§kAAA§4§l DU BIST NICHT ALLEIN IN DIESEM RAUM §0§kAAA",
      "§4§lDas Bild friert ein. Der Sound bleibt.",
      "§4§lDein Client zuckt. Das ist kein Bug.",
      "§4§lDein Name steht in den Logs. Für immer.",
      "§0§kXXX§c§l ICH SEHE DICH §0§kXXX",
      "§4§lDie Frequenz in deinen Ohren... ist berechnet.",
      "§4§lDu kannst nicht ausloggen. Nicht wirklich.",
      "§4§lWenn du denkst, es ist vorbei... schau hinter dich."
   );

   public WatcherHorror(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
   }

   public void beginHorror(Player target) {
      if (target != null && target.isOnline()) {
         this.horrorTarget = target.getUniqueId();
         this.horrorPhase = 1;
         this.whisperIndex = 0;
         this.nextHorrorTick = System.currentTimeMillis();
         this.nextWhisper = System.currentTimeMillis() + 3000L;
         this.core.setState(WatcherState.HORROR);
         this.bot.unvanish();
         this.teleportBehind(target, 6.0);
         this.bot.lookAt(target.getEyeLocation());
         target.sendMessage("§0§k...§8§o Etwas hat dich bemerkt. §0§k...");
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/HORROR] Horror mode began on " + target.getName());
      }
   }

   public void endHorror() {
      if (this.horrorTarget != null) {
         Player target = Bukkit.getPlayer(this.horrorTarget);
         if (target != null && target.isOnline()) {
            target.sendMessage("§aDie Präsenz verschwindet... vorläufig.");
            target.removePotionEffect(PotionEffectType.BLINDNESS);
            target.removePotionEffect(PotionEffectType.DARKNESS);
            target.removePotionEffect(PotionEffectType.SLOWNESS);
         }

         this.horrorTarget = null;
         this.horrorPhase = 0;
         this.core.setState(WatcherState.IDLE);
         this.core.getVisuals().setupHerobrineEyes();
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/HORROR] Horror mode ended.");
      }
   }

   public void tick() {
      if (this.horrorTarget != null && this.horrorPhase != 0) {
         long now = System.currentTimeMillis();
         Player target = Bukkit.getPlayer(this.horrorTarget);
         if (target != null && target.isOnline()) {
            long elapsed = now - this.nextHorrorTick;
            if (elapsed > 15000L && this.horrorPhase == 1) {
               this.horrorPhase = 2;
               target.sendMessage("§4§lDie Stille wird dichter...");
            } else if (elapsed > 35000L && this.horrorPhase == 2) {
               this.horrorPhase = 3;
               this.triggerPhase3(target);
            }

            if (now > this.nextWhisper) {
               this.sendWhisper(target);
               this.nextWhisper = now + (long)(8000 + this.random.nextInt(12000));
            }

            switch (this.horrorPhase) {
               case 1:
                  this.tickPhase1(target);
                  break;
               case 2:
                  this.tickPhase2(target);
                  break;
               case 3:
                  this.tickPhase3Active(target);
            }
         } else {
            this.endHorror();
         }
      }
   }

   private void tickPhase1(Player target) {
      Location botLoc = this.bot.getLocation();
      Location targetLoc = target.getLocation();
      if (botLoc != null && targetLoc != null) {
         double distance = botLoc.distance(targetLoc);
         long now = System.currentTimeMillis();
         if (now - this.lastTeleportTime >= 2500L) {
            if (distance > 20.0) {
               this.teleportBehind(target, 15.0);
               this.lastTeleportTime = now;
            } else if (distance < 5.0) {
               this.moveAway(target, 10.0);
               this.lastTeleportTime = now;
            }
         }

         this.bot.lookAt(target.getEyeLocation());
         if (now - this.lastPotionTime >= 3000L && this.random.nextDouble() < 0.05 && !target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false, false));
            this.lastPotionTime = now;
         }
      }
   }

   private void tickPhase2(Player target) {
      Location botLoc = this.bot.getLocation();
      Location targetLoc = target.getLocation();
      if (botLoc != null && targetLoc != null) {
         double distance = botLoc.distance(targetLoc);
         long now = System.currentTimeMillis();
         if (now - this.lastTeleportTime >= 2500L) {
            if (distance > 12.0) {
               this.teleportBehind(target, 6.0);
               this.lastTeleportTime = now;
            } else if (distance > 4.0) {
               double dx = targetLoc.getX() - botLoc.getX();
               double dz = targetLoc.getZ() - botLoc.getZ();
               double len = Math.sqrt(dx * dx + dz * dz);
               if (len > 0.0) {
                  Location step = botLoc.clone().add(dx / len * 0.5, 0.0, dz / len * 0.5);
                  step.setY(targetLoc.getY());
                  this.bot.teleport(step);
               }
            }
         }

         this.bot.lookAt(target.getEyeLocation());
         if (now - this.lastPotionTime >= 3000L) {
            if (this.random.nextDouble() < 0.08 && !target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
               target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, false, false));
               this.lastPotionTime = now;
            }

            if (this.random.nextDouble() < 0.05) {
               this.core.getVisuals().applyTotalDarkness(target, 60);
            }
         }
      }
   }

   private void triggerPhase3(Player target) {
      this.core.getVisuals().triggerFullJumpScare(target);
      this.core.getVisuals().applyTotalDarkness(target, 200);
      Location front = target.getLocation().clone().add(target.getLocation().getDirection().multiply(2));
      front.setY((double)(front.getWorld().getHighestBlockYAt(front) + 1));
      this.bot.teleport(front);
      this.bot.lookAt(target.getEyeLocation());
      CoreBootstrap.PLUGIN.getLogger().warning("[Watcher/HORROR] PHASE 3 FULL JUMPSCARE on " + target.getName());
   }

   private void tickPhase3Active(Player target) {
      long now = System.currentTimeMillis();
      this.bot.lookAt(target.getEyeLocation());
      if (this.random.nextDouble() < 0.15) {
         this.core.getVisuals().spawnEyeParticles();
      }

      if (now - this.lastTeleportTime >= 2500L && this.random.nextDouble() < 0.05) {
         Location front = target.getLocation().clone().add(target.getLocation().getDirection().multiply(1.5));
         front.setY(target.getY());
         this.bot.teleport(front);
         this.lastTeleportTime = now;
      }

      if (this.random.nextDouble() < 0.12) {
         this.core.getVisuals().applyTotalDarkness(target, 80);
      }

      if (this.random.nextDouble() < 0.02) {
         this.core.getVisuals().playJumpScareSound();
         if (!target.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false, false));
         }
      }
   }

   private void sendWhisper(Player target) {
      List<String> whispers = switch (this.horrorPhase) {
         case 1 -> this.PHASE1_WHISPERS;
         case 2 -> this.PHASE2_WHISPERS;
         case 3 -> this.PHASE3_WHISPERS;
         default -> this.PHASE1_WHISPERS;
      };
      if (!whispers.isEmpty()) {
         String msg = whispers.get(this.random.nextInt(whispers.size()));
         target.sendMessage(msg);
      }
   }

   private void teleportBehind(Player target, double distance) {
      Location targetLoc = target.getLocation();
      double yaw = Math.toRadians((double)(targetLoc.getYaw() + 180.0F));
      double dx = -Math.sin(yaw) * distance;
      double dz = Math.cos(yaw) * distance;
      Location behind = targetLoc.clone().add(dx, 0.0, dz);
      behind.setY(targetLoc.getY());
      this.bot.teleport(behind);
   }

   private void moveAway(Player target, double distance) {
      Location targetLoc = target.getLocation();
      Location botLoc = this.bot.getLocation();
      if (botLoc != null) {
         double dx = botLoc.getX() - targetLoc.getX();
         double dz = botLoc.getZ() - targetLoc.getZ();
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len != 0.0) {
            Location dest = botLoc.clone().add(dx / len * distance, 0.0, dz / len * distance);
            dest.setY(botLoc.getY());
            this.bot.teleport(dest);
         }
      }
   }

   public boolean isActive() {
      return this.horrorTarget != null;
   }

   public UUID getHorrorTarget() {
      return this.horrorTarget;
   }

   public int getHorrorPhase() {
      return this.horrorPhase;
   }
}
