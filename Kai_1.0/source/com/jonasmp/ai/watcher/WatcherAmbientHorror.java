package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherAmbientHorror {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final Random random = ThreadLocalRandom.current();
   private BukkitRunnable ambientTask = null;
   private final List<String> AMBIENT_MESSAGES = Arrays.asList(
      "§8§oDu hörst etwas im Dunkeln...",
      "§8§oEin kalter Luftzug streicht über deinen Nacken.",
      "§8§oWar das ein Schritt hinter dir?",
      "§8§oDie Stille hier ist... falsch.",
      "§8§oDu spürst jemandes Blick auf deinem Rücken.",
      "§8§oDie Blöcke flüstern. Oder war das nur der Wind?",
      "§8§oDein Herz schlägt schneller. Ich merke es.",
      "§8§oJemand hat deinen Namen geflüstert.",
      "§8§oDie Dunkelheit hier ist nicht normal.",
      "§8§oDu solltest nicht allein sein..."
   );
   private final List<String> SIGN_TEXTS = Arrays.asList(
      "STOP", "TURN BACK", "NOT SAFE", "I SEE YOU", "FOLLOW ME", "ONLY GOD", "NULL", "...", "DONT LOOK", "BEHIND YOU"
   );

   public WatcherAmbientHorror(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
   }

   public void start() {
      if (this.ambientTask == null) {
         if (!this.core.getConfig().isAmbientEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Ambient] Ambient horror disabled in config.");
         } else {
            final int intervalMin = this.core.getConfig().getAmbientEventIntervalMin();
            final int intervalMax = this.core.getConfig().getAmbientEventIntervalMax();
            (this.ambientTask = new BukkitRunnable() {
               int tick;
               long nextEventTick;

               {
                  Objects.requireNonNull(WatcherAmbientHorror.this);
                  this.tick = 0;
                  this.nextEventTick = (long)(intervalMin * 20 + WatcherAmbientHorror.this.random.nextInt((intervalMax - intervalMin) * 20));
               }

               public void run() {
                  if (!WatcherAmbientHorror.this.core.isRunning()) {
                     this.cancel();
                     WatcherAmbientHorror.this.ambientTask = null;
                  } else {
                     if ((long)this.tick >= this.nextEventTick) {
                        WatcherAmbientHorror.this.tryRandomAmbientEvent();
                        this.nextEventTick = (long)(this.tick + intervalMin * 20 + WatcherAmbientHorror.this.random.nextInt((intervalMax - intervalMin) * 20));
                     }

                     this.tick++;
                  }
               }
            }).runTaskTimer(CoreBootstrap.PLUGIN, 100L, 2L);
         }
      }
   }

   public void stop() {
      if (this.ambientTask != null) {
         this.ambientTask.cancel();
         this.ambientTask = null;
      }
   }

   public void triggerEventOnPlayer(Player target) {
      if (target != null && target.isOnline()) {
         this.tryRandomAmbientEvent(target);
      }
   }

   private void tryRandomAmbientEvent() {
      Player target = this.getRandomOnlinePlayer();
      if (target != null && target.isOnline()) {
         this.tryRandomAmbientEvent(target);
      }
   }

   private void tryRandomAmbientEvent(Player target) {
      int roll = this.random.nextInt(100);
      int cumulative = 0;
      if (this.core.getConfig().isAmbientEventEnabled("ambient_message")) {
         cumulative += this.core.getConfig().getAmbientEventChance("ambient_message");
         if (roll < cumulative) {
            this.sendAmbientMessage(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("light_flicker")) {
         cumulative += this.core.getConfig().getAmbientEventChance("light_flicker");
         if (roll < cumulative) {
            this.flickerNearbyLights(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("distant_sound")) {
         cumulative += this.core.getConfig().getAmbientEventChance("distant_sound");
         if (roll < cumulative) {
            this.playDistantSound(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("ambient_sign")) {
         cumulative += this.core.getConfig().getAmbientEventChance("ambient_sign");
         if (roll < cumulative) {
            this.placeAmbientSign(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("brief_nausea")) {
         cumulative += this.core.getConfig().getAmbientEventChance("brief_nausea");
         if (roll < cumulative) {
            this.briefNausea(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("fake_footsteps")) {
         cumulative += this.core.getConfig().getAmbientEventChance("fake_footsteps");
         if (roll < cumulative) {
            this.fakeFootsteps(target);
            return;
         }
      }

      if (this.core.getConfig().isAmbientEventEnabled("torch_extinguish")) {
         cumulative += this.core.getConfig().getAmbientEventChance("torch_extinguish");
         if (roll < cumulative) {
            this.briefTorchExtinguish(target);
         }
      }
   }

   private void sendAmbientMessage(Player target) {
      if (target.isOnline()) {
         List<String> messages = this.core.getConfig().getAmbientMessages();
         if (messages.isEmpty()) {
            messages = this.AMBIENT_MESSAGES;
         }

         String msg = messages.get(this.random.nextInt(messages.size()));
         target.sendMessage(msg);
      }
   }

   private void flickerNearbyLights(Player target) {
      if (target.isOnline()) {
         Location loc = target.getLocation();
         World world = loc.getWorld();
         List<Block> lights = new ArrayList<>();
         int cx = loc.getBlockX();
         int cy = loc.getBlockY();
         int cz = loc.getBlockZ();

         for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
               for (int dz = -8; dz <= 8; dz++) {
                  Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                  if (this.isLightSource(b.getType())) {
                     lights.add(b);
                  }
               }
            }
         }

         if (!lights.isEmpty()) {
            int count = Math.min(1 + this.random.nextInt(3), lights.size());
            Collections.shuffle(lights);

            for (int i = 0; i < count; i++) {
               Block light = lights.get(i);
               Material original = light.getType();
               light.setType(Material.AIR);
               int delay = 20 + this.random.nextInt(40);
               Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                  if (light.getType() == Material.AIR) {
                     light.setType(original);
                  }
               }, (long)delay);
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Ambient] Lights flickered near " + target.getName());
         }
      }
   }

   private boolean isLightSource(Material mat) {
      return mat == Material.TORCH
         || mat == Material.WALL_TORCH
         || mat == Material.LANTERN
         || mat == Material.SOUL_LANTERN
         || mat == Material.SOUL_TORCH
         || mat == Material.REDSTONE_TORCH
         || mat == Material.REDSTONE_WALL_TORCH
         || mat == Material.GLOWSTONE
         || mat == Material.SHROOMLIGHT
         || mat == Material.JACK_O_LANTERN
         || mat == Material.SEA_LANTERN;
   }

   private void playDistantSound(Player target) {
      if (target.isOnline()) {
         Location loc = target.getLocation();
         World world = loc.getWorld();
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         double dist = (double)(15 + this.random.nextInt(20));
         Location soundLoc = loc.clone().add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
         Sound[] sounds = new Sound[]{
            Sound.AMBIENT_CAVE,
            Sound.BLOCK_SCULK_SENSOR_CLICKING,
            Sound.ENTITY_GHAST_AMBIENT,
            Sound.ENTITY_ENDERMAN_STARE,
            Sound.BLOCK_WOOD_BREAK,
            Sound.ENTITY_WARDEN_HEARTBEAT,
            Sound.AMBIENT_BASALT_DELTAS_LOOP,
            Sound.BLOCK_STONE_BREAK
         };
         Sound sound = sounds[this.random.nextInt(sounds.length)];
         world.playSound(soundLoc, sound, SoundCategory.AMBIENT, 0.3F, 0.5F + this.random.nextFloat() * 0.5F);
      }
   }

   private void placeAmbientSign(Player target) {
      if (target.isOnline()) {
         Location loc = this.findValidSurface(target, 8, 25);
         if (loc != null) {
            Block ground = loc.getWorld().getBlockAt(loc).getRelative(0, -1, 0);
            if (ground.getType().isSolid()) {
               Block signBlock = ground.getRelative(0, 1, 0);
               if (signBlock.getType() == Material.AIR) {
                  signBlock.setType(Material.OAK_SIGN);
                  if (signBlock.getState() instanceof Sign sign) {
                     List<String> texts = this.core.getConfig().getAmbientSignTexts();
                     if (texts.isEmpty()) {
                        texts = this.SIGN_TEXTS;
                     }

                     sign.getSide(Side.FRONT).setLine(1, texts.get(this.random.nextInt(texts.size())));
                     sign.update();
                  }

                  int minTicks = this.core.getConfig().getAmbientConfig().getInt("sign.remove_after_min_minutes", 5) * 1200;
                  int maxTicks = this.core.getConfig().getAmbientConfig().getInt("sign.remove_after_max_minutes", 15) * 1200;
                  Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                     if (signBlock.getState() instanceof Sign) {
                        signBlock.setType(Material.AIR);
                     }
                  }, (long)(minTicks + this.random.nextInt(maxTicks - minTicks)));
                  CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Ambient] Sign placed near " + target.getName());
               }
            }
         }
      }
   }

   private void briefNausea(Player target) {
      if (target.isOnline()) {
         target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false, false));
      }
   }

   private void fakeFootsteps(Player target) {
      if (target.isOnline()) {
         Location loc = target.getLocation();
         World world = loc.getWorld();
         double yaw = Math.toRadians((double)(loc.getYaw() + 180.0F));

         for (int i = 0; i < 2 + this.random.nextInt(3); i++) {
            int delay = i * 15;
            int step = i;
            Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
               if (target.isOnline()) {
                  double dist = (double)(4 + step * 2);
                  Location stepLoc = loc.clone().add(-Math.sin(yaw) * dist, 0.0, Math.cos(yaw) * dist);
                  stepLoc.setY(loc.getY());
                  world.playSound(stepLoc, Sound.BLOCK_STONE_STEP, SoundCategory.AMBIENT, 0.4F, 0.8F);
               }
            }, (long)delay);
         }
      }
   }

   private void briefTorchExtinguish(Player target) {
      if (target.isOnline()) {
         Location loc = target.getLocation();
         World world = loc.getWorld();
         int cx = loc.getBlockX();
         int cy = loc.getBlockY();
         int cz = loc.getBlockZ();

         for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
               for (int dz = -3; dz <= 3; dz++) {
                  Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                  if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                     Material original = b.getType();
                     b.setType(Material.AIR);
                     Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                        if (b.getType() == Material.AIR) {
                           b.setType(original);
                        }
                     }, (long)(60 + this.random.nextInt(40)));
                     return;
                  }
               }
            }
         }
      }
   }

   private Player getRandomOnlinePlayer() {
      List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
      if (players.isEmpty()) {
         return null;
      } else {
         Player botPlayer = this.bot.getBotPlayer();
         players.remove(botPlayer);
         return players.isEmpty() ? null : players.get(this.random.nextInt(players.size()));
      }
   }

   private Location findValidSurface(Player target, int minDist, int maxDist) {
      World world = target.getWorld();
      Location targetLoc = target.getLocation();

      for (int attempt = 0; attempt < 15; attempt++) {
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         double dist = (double)(minDist + this.random.nextInt(maxDist - minDist));
         double x = targetLoc.getX() + Math.cos(angle) * dist;
         double z = targetLoc.getZ() + Math.sin(angle) * dist;
         int bx = (int)x;
         int bz = (int)z;
         int by = (int)targetLoc.getY();
         Location result = new Location(world, (double)bx + 0.5, (double)(by + 1), (double)bz + 0.5);
         if (result.distance(targetLoc) > 5.0) {
            return result;
         }
      }

      return null;
   }
}
