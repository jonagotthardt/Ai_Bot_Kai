package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

public class WatcherVisuals {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final Random random = ThreadLocalRandom.current();
   private static final String WATCHER_TEAM = "watcher_horror";
   private BukkitRunnable particleTask = null;
   private BukkitRunnable eyeTask = null;

   public WatcherVisuals(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
   }

   public void setupHerobrineEyes() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
         Team team = sb.getTeam("watcher_horror");
         if (team == null) {
            team = sb.registerNewTeam("watcher_horror");
            team.setColor(ChatColor.WHITE);
            team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.NEVER);
            team.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
         }

         team.addEntry(b.getName());
         b.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] Herobrine eyes activated (WHITE GLOW).");
      }
   }

   public void setupRedEyes() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
         Team team = sb.getTeam("watcher_horror");
         if (team != null) {
            team.unregister();
         }

         team = sb.registerNewTeam("watcher_horror");
         team.setColor(ChatColor.DARK_RED);
         team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.NEVER);
         team.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
         team.setCanSeeFriendlyInvisibles(false);
         team.addEntry(b.getName());
         b.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher] DEMON EYES activated (RED GLOW).");
      }
   }

   public void removeEyes() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
         Team team = sb.getTeam("watcher_horror");
         if (team != null) {
            team.removeEntry(b.getName());
            if (team.getEntries().isEmpty()) {
               team.unregister();
            }
         }

         b.removePotionEffect(PotionEffectType.GLOWING);
      }
   }

   public void startParticleAura() {
      if (this.particleTask == null) {
         (this.particleTask = new BukkitRunnable() {
            int tick;
            long nextSoundTick;

            {
               Objects.requireNonNull(WatcherVisuals.this);
               this.tick = 0;
               this.nextSoundTick = (long)(80 + WatcherVisuals.this.random.nextInt(60));
            }

            public void run() {
               if (!WatcherVisuals.this.core.isRunning()) {
                  this.cancel();
                  WatcherVisuals.this.particleTask = null;
               } else {
                  Player b = WatcherVisuals.this.bot.getBotPlayer();
                  if (b != null && b.isOnline()) {
                     Location loc = b.getLocation().add(0.0, 1.5, 0.0);
                     World world = b.getWorld();
                     switch (WatcherVisuals.this.core.getState()) {
                        case IDLE:
                        case OBSERVING:
                           if (this.tick % 8 == 0) {
                              world.spawnParticle(Particle.ASH, loc, 3, 0.3, 0.5, 0.3, 0.01);
                           }

                           if (this.tick % 20 == 0) {
                              world.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.3, 0.2, 0.01);
                           }
                           break;
                        case INVESTIGATING:
                           if (this.tick % 5 == 0) {
                              world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.3, 0.4, 0.3, 0.02);
                           }

                           if (this.tick % 15 == 0) {
                              world.spawnParticle(Particle.SOUL, loc, 1, 0.2, 0.3, 0.2, 0.01);
                           }
                           break;
                        case VISIBLE:
                        case HORROR:
                           world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.4, 0.5, 0.4, 0.03);
                           if (this.tick % 3 == 0) {
                              world.spawnParticle(Particle.END_ROD, loc, 2, 0.2, 0.2, 0.2, 0.05);
                           }

                           if (this.tick % 10 == 0) {
                              world.spawnParticle(Particle.FIREWORK, loc, 5, 0.5, 0.5, 0.5, 0.1);
                           }
                     }

                     if (this.tick % 3 == 0) {
                        WatcherVisuals.this.spawnEyeParticles();
                     }

                     if ((long)this.tick >= this.nextSoundTick) {
                        WatcherVisuals.this.playAmbientSound();
                        this.nextSoundTick = (long)(this.tick + 80 + WatcherVisuals.this.random.nextInt(80));
                     }

                     this.tick++;
                  }
               }
            }
         }).runTaskTimer(CoreBootstrap.PLUGIN, 0L, 5L);
      }
   }

   public void stopParticleAura() {
      if (this.particleTask != null) {
         this.particleTask.cancel();
         this.particleTask = null;
      }
   }

   public void spawnEyeParticles() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Location eyeCenter = b.getLocation().add(0.0, 1.62, 0.0);
         World world = b.getWorld();
         float yaw = b.getLocation().getYaw();
         double yawRad = Math.toRadians((double)yaw);
         double leftX = eyeCenter.getX() + Math.cos(yawRad + (Math.PI / 2)) * 0.15 + Math.sin(-yawRad) * 0.25;
         double leftZ = eyeCenter.getZ() + Math.sin(yawRad + (Math.PI / 2)) * 0.15 + Math.cos(-yawRad) * 0.25;
         Location leftEye = new Location(world, leftX, eyeCenter.getY(), leftZ);
         double rightX = eyeCenter.getX() + Math.cos(yawRad - (Math.PI / 2)) * 0.15 + Math.sin(-yawRad) * 0.25;
         double rightZ = eyeCenter.getZ() + Math.sin(yawRad - (Math.PI / 2)) * 0.15 + Math.cos(-yawRad) * 0.25;
         Location rightEye = new Location(world, rightX, eyeCenter.getY(), rightZ);
         world.spawnParticle(Particle.END_ROD, leftEye, 1, 0.0, 0.0, 0.0, 0.0);
         world.spawnParticle(Particle.END_ROD, rightEye, 1, 0.0, 0.0, 0.0, 0.0);
         world.spawnParticle(Particle.SOUL_FIRE_FLAME, leftEye, 1, 0.02, 0.02, 0.02, 0.001);
         world.spawnParticle(Particle.SOUL_FIRE_FLAME, rightEye, 1, 0.02, 0.02, 0.02, 0.001);
      }
   }

   public void triggerFullJumpScare(Player target) {
      if (target != null && target.isOnline()) {
         Location targetLoc = target.getLocation();
         World world = targetLoc.getWorld();
         this.bot.unvanish();
         Location front = targetLoc.clone().add(targetLoc.getDirection().multiply(2));
         front.setY(targetLoc.getY());
         this.bot.teleport(front);
         this.bot.lookAt(target.getEyeLocation());
         this.setupRedEyes();
         target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
         target.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20, 0, false, false, false));
         world.spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc.add(0.0, 1.5, 0.0), 20, 0.5, 0.5, 0.5, 0.3);
         world.spawnParticle(Particle.END_ROD, targetLoc, 30, 1.0, 1.0, 1.0, 0.5);
         world.spawnParticle(Particle.SOUL_FIRE_FLAME, targetLoc, 20, 0.8, 0.8, 0.8, 0.3);
         target.sendTitle("§c§lHEROBRINE", "§8§oDu solltest nicht hier sein...", 2, 40, 10);
         this.shakeCamera(target, 8, 5);
         this.playJumpScareSound();
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            if (target.isOnline()) {
               this.applyTotalDarkness(target, 160);
               if (this.bot.getBotPlayer() != null) {
                  this.bot.lookAt(target.getEyeLocation());
                  this.spawnEyeParticles();
               }

               Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> this.setupHerobrineEyes(), 100L);
            }
         }, 25L);
      }
   }

   private void shakeCamera(final Player target, final int intensity, final int durationTicks) {
      if (target != null && target.isOnline()) {
         final Location base = target.getLocation();
         BukkitRunnable shakeTask = new BukkitRunnable() {
            int tick;

            {
               Objects.requireNonNull(WatcherVisuals.this);
               this.tick = 0;
            }

            public void run() {
               if (this.tick < durationTicks && target.isOnline()) {
                  double dx = (WatcherVisuals.this.random.nextDouble() - 0.5) * (double)intensity * 0.02;
                  double dz = (WatcherVisuals.this.random.nextDouble() - 0.5) * (double)intensity * 0.02;
                  Location shakeLoc = target.getLocation().add(dx, 0.0, dz);
                  shakeLoc.setYaw(base.getYaw() + (float)((WatcherVisuals.this.random.nextDouble() - 0.5) * (double)intensity * 1.5));
                  shakeLoc.setPitch(base.getPitch() + (float)((WatcherVisuals.this.random.nextDouble() - 0.5) * (double)intensity * 0.8));
                  target.teleport(shakeLoc);
                  this.tick++;
               } else {
                  this.cancel();
               }
            }
         };
         shakeTask.runTaskTimer(CoreBootstrap.PLUGIN, 0L, 1L);
      }
   }

   public void applyTotalDarkness(Player target, int durationTicks) {
      if (target != null && target.isOnline()) {
         target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, false, false));
         target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, durationTicks, 0, false, false, false));
         target.removePotionEffect(PotionEffectType.NIGHT_VISION);
         target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1, false, false, false));
      }
   }

   public void flashEyes(int durationTicks) {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         this.setupRedEyes();
         Location loc = b.getLocation().add(0.0, 1.5, 0.0);
         World world = b.getWorld();
         world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 10, 0.0, 0.0, 0.0, 0.2);
         world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.5, 0.5, 0.1);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> this.setupHerobrineEyes(), (long)durationTicks);
      }
   }

   public void playAmbientSound() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Location loc = b.getLocation();
         World world = b.getWorld();
         switch (this.random.nextInt(5)) {
            case 0:
               this.playCustomOrVanilla(world, loc, "watcher.ambient.cave", SoundCategory.AMBIENT, 0.5F, 0.5F, Sound.AMBIENT_CAVE);
               break;
            case 1:
               this.playCustomOrVanilla(world, loc, "watcher.ambient.whisper", SoundCategory.AMBIENT, 0.4F, 0.7F, Sound.ENTITY_GHAST_AMBIENT);
               break;
            case 2:
               this.playCustomOrVanilla(world, loc, "watcher.heartbeat", SoundCategory.AMBIENT, 0.3F, 0.5F, Sound.BLOCK_SCULK_SENSOR_CLICKING);
               break;
            case 3:
               this.playCustomOrVanilla(world, loc, "watcher.herobrine.stare", SoundCategory.AMBIENT, 0.3F, 0.3F, Sound.ENTITY_ENDERMAN_STARE);
               break;
            case 4:
               this.playCustomOrVanilla(world, loc, "watcher.scream", SoundCategory.HOSTILE, 0.2F, 0.5F, Sound.ENTITY_ENDERMAN_STARE);
         }
      }
   }

   public void playJumpScareSound() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Location loc = b.getLocation();
         World world = b.getWorld();
         this.playCustomOrVanilla(world, loc, "watcher.scream", SoundCategory.HOSTILE, 1.0F, 0.5F, Sound.ENTITY_GHAST_SCREAM);
      }
   }

   public void playHerobrineStareSound() {
      Player b = this.bot.getBotPlayer();
      if (b != null) {
         Location loc = b.getLocation();
         World world = b.getWorld();
         this.playCustomOrVanilla(world, loc, "watcher.herobrine.stare", SoundCategory.AMBIENT, 0.6F, 0.3F, Sound.ENTITY_ENDERMAN_STARE);
      }
   }

   private void playCustomOrVanilla(World world, Location loc, String customSound, SoundCategory category, float volume, float pitch, Sound vanillaFallback) {
      world.playSound(loc, customSound, category, volume, pitch);
      world.playSound(loc, vanillaFallback, category, volume * 0.7F, pitch);
   }

   public void playRandomHerobrineSound(World world, Location loc) {
      switch (this.random.nextInt(5)) {
         case 0:
            this.playCustomOrVanilla(world, loc, "watcher.ambient.cave", SoundCategory.AMBIENT, 1.0F, 0.3F, Sound.AMBIENT_CAVE);
            break;
         case 1:
            this.playCustomOrVanilla(world, loc, "watcher.herobrine.stare", SoundCategory.AMBIENT, 0.6F, 0.2F, Sound.ENTITY_GHAST_AMBIENT);
            break;
         case 2:
            this.playCustomOrVanilla(world, loc, "watcher.heartbeat", SoundCategory.AMBIENT, 0.8F, 0.3F, Sound.BLOCK_SCULK_SENSOR_CLICKING);
            break;
         case 3:
            this.playCustomOrVanilla(world, loc, "watcher.ambient.whisper", SoundCategory.AMBIENT, 0.7F, 0.5F, Sound.ENTITY_ENDERMAN_STARE);
            break;
         case 4:
            this.playCustomOrVanilla(world, loc, "watcher.scream", SoundCategory.HOSTILE, 0.5F, 0.4F, Sound.ENTITY_WARDEN_HEARTBEAT);
      }
   }
}
