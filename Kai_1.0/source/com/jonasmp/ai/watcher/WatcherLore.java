package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WatcherLore {
   private final WatcherCore core;
   private final Random random = ThreadLocalRandom.current();
   private long lastAppearance = 0L;
   private long nextAppearanceRoll = 0L;

   public WatcherLore(WatcherCore core) {
      this.core = core;
   }

   public void tick() {
      WatcherConfig cfg = this.core.getConfig();
      if (cfg.isLoreModeEnabled()) {
         long now = System.currentTimeMillis();
         if (now >= this.nextAppearanceRoll) {
            this.nextAppearanceRoll = now + 30000L + (long)this.random.nextInt(60000);
            int chance = cfg.getLoreChancePercent();
            if (this.random.nextInt(100) < chance) {
               Collection<? extends Player> playerColl = Bukkit.getOnlinePlayers();
               if (!playerColl.isEmpty()) {
                  Player[] players = playerColl.toArray(new Player[0]);
                  Player target = players[this.random.nextInt(players.length)];
                  if (this.core.getState() != WatcherState.INVESTIGATING
                     && this.core.getState() != WatcherState.FREEZING
                     && this.core.getState() != WatcherState.VISIBLE) {
                     this.triggerAppearance(target);
                  }
               }
            }
         }
      }
   }

   public void triggerAppearance(Player target) {
      if (target != null && target.isOnline()) {
         WatcherConfig cfg = this.core.getConfig();
         WatcherPlayerData data = this.core.ensurePlayerData(target.getUniqueId(), target.getName());
         long now = System.currentTimeMillis();
         int cooldownMinutes = cfg.getLoreEffectCooldownMinutes();
         if (now - data.getLastLoreAppearance() >= (long)cooldownMinutes * 60000L) {
            data.incrementLoreAppearances();
            this.lastAppearance = now;
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Lore] Appearing to " + target.getName());
            int minDist = cfg.getLoreMinDistance();
            int maxDist = cfg.getLoreMaxDistance();
            int distance = minDist + this.random.nextInt(maxDist - minDist);
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            Location targetLoc = target.getLocation();
            World world = targetLoc.getWorld();
            double x = targetLoc.getX() + Math.cos(angle) * (double)distance;
            double z = targetLoc.getZ() + Math.sin(angle) * (double)distance;
            double y = (double)(world.getHighestBlockYAt((int)x, (int)z) + 1);
            Location appearLoc = new Location(world, x, y, z);
            this.core.getBot().unvanish();
            this.core.getBot().teleport(appearLoc);
            this.core.getBot().lookAt(target.getEyeLocation());
            this.playLoreSound(target);
            this.sendLoreTitle(target);
            int vanishDelay = 3 + this.random.nextInt(5);
            Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
               this.core.getBot().vanish();
               if (this.core.getState() == WatcherState.LORE) {
                  this.core.goIdle();
               }
            }, (long)vanishDelay * 20L);
         }
      }
   }

   private void playLoreSound(Player target) {
      WatcherConfig cfg = this.core.getConfig();
      String[] soundKeys = new String[]{"lore_appear", "lore_stare", "lore_vanish"};
      String key = soundKeys[this.random.nextInt(soundKeys.length)];
      if (cfg.isSoundEnabled(key)) {
         try {
            Sound sound = null;
            NamespacedKey ns = NamespacedKey.fromString(cfg.getSoundName(key).toLowerCase());
            if (ns != null) {
               sound = (Sound)Registry.SOUNDS.get(ns);
            }

            if (sound == null) {
               sound = Sound.valueOf(cfg.getSoundName(key));
            }

            target.playSound(target.getLocation(), sound, cfg.getSoundVolume(key), cfg.getSoundPitch(key));
         } catch (IllegalArgumentException var7) {
         }
      }
   }

   private void sendLoreTitle(Player target) {
      WatcherConfig cfg = this.core.getConfig();
      List<String> titles = cfg.getTitlePool("lore");
      List<String> subtitles = cfg.getSubtitlePool("lore");
      if (!titles.isEmpty() || !subtitles.isEmpty()) {
         String title = titles.isEmpty() ? "" : titles.get(this.random.nextInt(titles.size()));
         String subtitle = subtitles.isEmpty() ? "" : subtitles.get(this.random.nextInt(subtitles.size()));
         target.sendTitle(title, subtitle, 10, 40, 20);
      }
   }
}
