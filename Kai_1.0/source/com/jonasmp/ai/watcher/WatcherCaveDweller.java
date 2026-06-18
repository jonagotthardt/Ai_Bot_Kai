package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherCaveDweller {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final WatcherVisuals visuals;
   private final Random random = ThreadLocalRandom.current();
   private BukkitRunnable caveTask = null;
   private final Map<UUID, Integer> darknessScore = new HashMap<>();
   private final Set<UUID> warnedPlayers = new HashSet<>();
   private long lastGlobalAppear = 0L;
   private long lastGlobalHorror = 0L;
   private static final long APPEAR_COOLDOWN_MS = 8000L;
   private static final long HORROR_COOLDOWN_MS = 15000L;
   private static final long FLICKER_INTERVAL_TICKS = 40L;
   private final List<String> CAVE_MESSAGES = Arrays.asList(
      "§8§oDie Dunkelheit hier ist... lebendig.",
      "§8§oHörst du das Atmen? Es ist nicht deins.",
      "§8§oIn der Tiefe wartet etwas.",
      "§8§oDas Licht ist keine Sicherheit mehr.",
      "§8§oDie Schatten bewegen sich. Wirklich.",
      "§8§oZu tief gegraben. Zu weit gegangen.",
      "§8§oDie Höhle kennt deinen Namen.",
      "§8§oNicht alle, die in die Tiefe gehen, kommen zurück."
   );

   public WatcherCaveDweller(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
      this.visuals = core.getVisuals();
   }

   public void start() {
      if (this.caveTask == null) {
         if (!this.core.getConfig().isCaveDwellerEnabled()) {
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Cave] Cave dweller disabled in config.");
         } else {
            final int checkInterval = this.core.getConfig().getCaveCheckInterval();
            (this.caveTask = new BukkitRunnable() {
               int tick;

               {
                  Objects.requireNonNull(WatcherCaveDweller.this);
                  this.tick = 0;
               }

               public void run() {
                  if (!WatcherCaveDweller.this.core.isRunning()) {
                     this.cancel();
                     WatcherCaveDweller.this.caveTask = null;
                  } else {
                     if (this.tick % checkInterval == 0) {
                        WatcherCaveDweller.this.checkPlayersInDarkness();
                     }

                     if ((long)this.tick % 40L == 0L) {
                        WatcherCaveDweller.this.flickerForDarkPlayers();
                     }

                     this.tick++;
                  }
               }
            }).runTaskTimer(CoreBootstrap.PLUGIN, 100L, 20L);
         }
      }
   }

   public void stop() {
      if (this.caveTask != null) {
         this.caveTask.cancel();
         this.caveTask = null;
      }

      this.darknessScore.clear();
      this.warnedPlayers.clear();
   }

   private void checkPlayersInDarkness() {
      int darkLight = this.core.getConfig().getCaveDarkLightLevel();
      int warningAt = this.core.getConfig().getCaveDarknessWarningAt();
      int appearAt = this.core.getConfig().getCaveDarknessAppearAt();
      int horrorAt = this.core.getConfig().getCaveDarknessHorrorAt();

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (!player.equals(this.bot.getBotPlayer())) {
            Location loc = player.getLocation();
            int light = loc.getBlock().getLightLevel();
            boolean isInCave = this.isUnderground(loc);
            UUID uuid = player.getUniqueId();
            int score = this.darknessScore.getOrDefault(uuid, 0);
            if (light >= darkLight && !isInCave) {
               if (score > 0) {
                  this.darknessScore.put(uuid, Math.max(0, score - 2));
               }

               this.warnedPlayers.remove(uuid);
            } else {
               this.darknessScore.put(uuid, ++score);
               if (score == warningAt && !this.warnedPlayers.contains(uuid)) {
                  this.warnedPlayers.add(uuid);
                  List<String> msgs = this.core.getConfig().getCaveWarningMessages();
                  if (msgs.isEmpty()) {
                     msgs = this.CAVE_MESSAGES;
                  }

                  player.sendMessage(msgs.get(this.random.nextInt(msgs.size())));
                  this.playCaveSound(player);
               }

               if (score >= appearAt && this.random.nextDouble() < 0.05) {
                  long now = System.currentTimeMillis();
                  if (now - this.lastGlobalAppear >= 8000L) {
                     this.lastGlobalAppear = now;
                     this.appearInDarkness(player);
                  }

                  this.darknessScore.put(uuid, 0);
               }

               if (score >= horrorAt) {
                  long now = System.currentTimeMillis();
                  if (now - this.lastGlobalHorror >= 15000L) {
                     this.lastGlobalHorror = now;
                     this.triggerCaveHorror(player);
                  }

                  this.darknessScore.put(uuid, 0);
               }
            }
         }
      }
   }

   private void flickerForDarkPlayers() {
      if (this.core.getConfig().isCaveLightFlickerEnabled()) {
         int chance = this.core.getConfig().getCavesConfig().getInt("light_flicker.chance", 10);

         for (Entry<UUID, Integer> entry : this.darknessScore.entrySet()) {
            if (entry.getValue() >= 3) {
               Player player = Bukkit.getPlayer(entry.getKey());
               if (player != null && player.isOnline() && this.random.nextInt(100) < chance) {
                  this.flickerNearbyLight(player);
               }
            }
         }
      }
   }

   private void flickerNearbyLight(Player player) {
      Location loc = player.getLocation();
      World world = loc.getWorld();
      int cx = loc.getBlockX();
      int cy = loc.getBlockY();
      int cz = loc.getBlockZ();
      List<Block> lights = new ArrayList<>();

      for (int dx = -3; dx <= 3; dx++) {
         for (int dy = -2; dy <= 2; dy++) {
            for (int dz = -3; dz <= 3; dz++) {
               Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
               if (this.isLightBlock(b.getType())) {
                  lights.add(b);
               }
            }
         }
      }

      if (!lights.isEmpty()) {
         Block light = lights.get(this.random.nextInt(lights.size()));
         Material original = light.getType();
         light.setType(Material.AIR);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            if (light.getType() == Material.AIR) {
               light.setType(original);
            }
         }, (long)(10 + this.random.nextInt(20)));
      }
   }

   private boolean isLightBlock(Material mat) {
      return mat == Material.TORCH
         || mat == Material.WALL_TORCH
         || mat == Material.LANTERN
         || mat == Material.SOUL_LANTERN
         || mat == Material.SOUL_TORCH
         || mat == Material.GLOWSTONE
         || mat == Material.SHROOMLIGHT
         || mat == Material.SEA_LANTERN
         || mat == Material.REDSTONE_LAMP
         || mat == Material.REDSTONE_TORCH;
   }

   private void appearInDarkness(Player player) {
      if (player.isOnline()) {
         Location playerLoc = player.getLocation();
         World world = playerLoc.getWorld();
         int minDist = this.core.getConfig().getCaveAppearanceMinDistance();
         int maxDist = this.core.getConfig().getCaveAppearanceMaxDistance();
         int darkLevel = this.core.getConfig().getCavesConfig().getInt("appearance.dark_spot_light_level", 3);
         int durMin = this.core.getConfig().getCavesConfig().getInt("appearance.duration_min_ticks", 40);
         int durMax = this.core.getConfig().getCavesConfig().getInt("appearance.duration_max_ticks", 80);

         for (int attempt = 0; attempt < 20; attempt++) {
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            double dist = (double)(minDist + this.random.nextInt(maxDist - minDist));
            int x = (int)(playerLoc.getX() + Math.cos(angle) * dist);
            int z = (int)(playerLoc.getZ() + Math.sin(angle) * dist);
            int y = (int)playerLoc.getY();
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.AIR || !block.getType().isSolid()) {
               Block ground = block.getRelative(0, -1, 0);
               if (ground.getType().isSolid()) {
                  Location appearLoc = new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5);
                  if (appearLoc.getBlock().getLightLevel() < darkLevel) {
                     this.bot.teleport(appearLoc);
                     this.bot.unvanish();
                     this.bot.lookAt(player.getEyeLocation());
                     this.visuals.setupRedEyes();
                     Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                        this.bot.vanish();
                        this.visuals.setupHerobrineEyes();
                     }, (long)(durMin + this.random.nextInt(durMax - durMin)));
                     world.playSound(appearLoc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT, 0.6F, 0.3F);
                     CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Cave] Appeared in darkness near " + player.getName());
                     return;
                  }
               }
            }
         }
      }
   }

   public void triggerCaveHorror(Player player) {
      if (player.isOnline()) {
         int darknessDur = this.core.getConfig().getCavesConfig().getInt("horror.darkness_duration", 100);
         int slownessDur = this.core.getConfig().getCavesConfig().getInt("horror.slowness_duration", 60);
         int botDist = this.core.getConfig().getCavesConfig().getInt("horror.bot_distance", 3);
         int botDur = this.core.getConfig().getCavesConfig().getInt("horror.bot_duration", 30);
         player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, darknessDur, 0, false, false, false));
         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDur, 0, false, false, false));
         Location playerLoc = player.getLocation();
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         Location closeLoc = playerLoc.clone().add(Math.cos(angle) * (double)botDist, 0.0, Math.sin(angle) * (double)botDist);
         closeLoc.setY(playerLoc.getY());
         this.bot.teleport(closeLoc);
         this.bot.unvanish();
         this.visuals.setupRedEyes();
         this.bot.lookAt(player.getEyeLocation());
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            this.bot.vanish();
            this.visuals.setupHerobrineEyes();
         }, (long)botDur);
         List<String> msgs = this.core.getConfig().getCavesConfig().getStringList("messages.horror");
         String msg = msgs.isEmpty() ? "§4§lDu spürst die Kälte der Dunkelheit..." : msgs.get(0);
         player.sendMessage(msg);
         player.getWorld().playSound(playerLoc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT, 1.0F, 0.3F);
         CoreBootstrap.PLUGIN.getLogger().warning("[Watcher/Cave] Cave horror triggered on " + player.getName());
      }
   }

   private void playCaveSound(Player player) {
      Location loc = player.getLocation();
      Sound[] sounds = new Sound[]{Sound.AMBIENT_CAVE, Sound.ENTITY_WARDEN_HEARTBEAT, Sound.BLOCK_SCULK_SENSOR_CLICKING, Sound.ENTITY_ENDERMAN_STARE};
      Sound sound = sounds[this.random.nextInt(sounds.length)];
      loc.getWorld().playSound(loc, sound, SoundCategory.AMBIENT, 0.4F, 0.4F);
   }

   private boolean isUnderground(Location loc) {
      World world = loc.getWorld();
      if (world == null) {
         return false;
      } else {
         String env = world.getEnvironment().name();
         return env.equals("NORMAL") ? loc.getY() < 55.0 : env.equals("NETHER") && loc.getY() < 100.0;
      }
   }
}
