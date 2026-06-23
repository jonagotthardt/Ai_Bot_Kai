package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AFKWakeUpListener implements Listener {
   private final JavaPlugin plugin;
   private final Random random = new Random();
   private static final int AFK_THRESHOLD_SECONDS = 300;
   private static final int TICKLE_INTERVAL_SECONDS = 20;
   private static final double MIN_MOVE_DISTANCE_SQ = 0.25;
   private final Map<UUID, Long> lastActivity = new HashMap<>();
   private final Map<UUID, Integer> tickleCount = new HashMap<>();
   private final Set<UUID> currentlyAFK = new HashSet<>();
   private final String[] WHISPERS_DE = new String[]{
      "§7§oPsst... noch da?",
      "§7§oHallo? Erde an Spieler?",
      "§7§oBeweg dich mal ein bisschen...",
      "§7§oIch glaube, du bist eingeschlafen...",
      "§7§o*leises Räuspern*",
      "§7§oHey, alles okay bei dir?",
      "§7§oDie Pixel werden kalt wenn du so stillstehst...",
      "§7§oWach auf, Abenteuer wartet!",
      "§7§o*zärtliches Nudge*",
      "§7§oDu lebst noch?"
   };
   private final String[] WHISPERS_EN = new String[]{
      "§7§oPsst... still there?",
      "§7§oHello? Earth to player?",
      "§7§oMove a little bit...",
      "§7§oI think you fell asleep...",
      "§7§o*soft clearing of throat*",
      "§7§oHey, everything okay?",
      "§7§oThe pixels are getting cold if you stand so still...",
      "§7§oWake up, adventure awaits!",
      "§7§o*tender nudge*",
      "§7§oYou still alive?"
   };

   public AFKWakeUpListener(JavaPlugin plugin) {
      this.plugin = plugin;
      this.startTickeTask();
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      if (event.getTo() != null) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if (from.toVector().distanceSquared(to.toVector()) > 0.25) {
            this.resetActivity(event.getPlayer().getUniqueId());
         }
      }
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      this.resetActivity(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID uuid = event.getPlayer().getUniqueId();
      this.lastActivity.remove(uuid);
      this.tickleCount.remove(uuid);
      this.currentlyAFK.remove(uuid);
   }

   private void resetActivity(UUID uuid) {
      this.lastActivity.put(uuid, System.currentTimeMillis());
      if (this.currentlyAFK.remove(uuid)) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && p.isOnline()) {
            String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(uuid) : "de";
            String back = "en".equals(lang) ? "§a§o*The AI nods approvingly*" : "§a§o*Die KI nickt zufrieden*";
            p.sendMessage(back);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3F, 1.2F);
         }
      }

      this.tickleCount.remove(uuid);
   }

   private void startTickeTask() {
      (new BukkitRunnable() {
         {
            Objects.requireNonNull(AFKWakeUpListener.this);
         }

         public void run() {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
               UUID uuid = player.getUniqueId();
               long last = AFKWakeUpListener.this.lastActivity.getOrDefault(uuid, now);
               long idleSeconds = (now - last) / 1000L;
               if (idleSeconds >= 300L) {
                  if (!AFKWakeUpListener.this.currentlyAFK.contains(uuid)) {
                     AFKWakeUpListener.this.currentlyAFK.add(uuid);
                     String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(uuid) : "de";
                     String msg = "en".equals(lang) ? "§7§o*The AI watches you curiously...*" : "§7§o*Die KI beobachtet dich neugierig...*";
                     player.sendMessage(msg);
                  }

                  int count = AFKWakeUpListener.this.tickleCount.getOrDefault(uuid, 0) + 1;
                  AFKWakeUpListener.this.tickleCount.put(uuid, count);
                  if (count % 20 == 0) {
                     String lang2 = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(uuid) : "de";
                     AFKWakeUpListener.this.ticklePlayer(player, count, lang2);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 20L, 20L);
   }

   private void ticklePlayer(Player player, int count, String lang) {
      int phase = count / 20;
      double strength = Math.min(0.03 + (double)phase * 0.01, 0.08);
      Vector direction = new Vector((this.random.nextDouble() - 0.5) * 2.0, 0.05 + this.random.nextDouble() * 0.05, (this.random.nextDouble() - 0.5) * 2.0)
         .normalize()
         .multiply(strength);
      player.setVelocity(direction);

      Particle particle = switch (phase % 4) {
         case 0 -> Particle.NOTE;
         case 1 -> Particle.HAPPY_VILLAGER;
         case 2 -> Particle.WITCH;
         default -> Particle.ENCHANT;
      };
      player.getWorld().spawnParticle(particle, player.getLocation().add(0.0, 1.5, 0.0), 3, 0.2, 0.2, 0.2, 0.01);

      Sound sound = switch (phase % 5) {
         case 0 -> Sound.BLOCK_AMETHYST_BLOCK_CHIME;
         case 1 -> Sound.BLOCK_NOTE_BLOCK_CHIME;
         case 2 -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
         case 3 -> Sound.BLOCK_NOTE_BLOCK_FLUTE;
         default -> Sound.ENTITY_BAT_AMBIENT;
      };
      float pitch = 0.8F + this.random.nextFloat() * 0.4F;
      player.playSound(player.getLocation(), sound, 0.15F, pitch);
      if (phase % 3 == 0) {
         String[] pool = "en".equals(lang) ? this.WHISPERS_EN : this.WHISPERS_DE;
         String whisper = pool[this.random.nextInt(pool.length)];
         player.sendMessage(whisper);
      }

      if (phase % 7 == 0 && phase > 0) {
         Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
         player.getWorld().spawnParticle(Particle.END_ROD, eye, 2, 0.05, 0.05, 0.05, 0.01);
         player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.1F, 1.5F);
      }
   }
}
