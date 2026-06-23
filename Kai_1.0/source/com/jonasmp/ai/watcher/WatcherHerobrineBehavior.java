package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class WatcherHerobrineBehavior {
   private final WatcherCore core;
   private final WatcherBot bot;
   private final Random random = ThreadLocalRandom.current();
   private static final List<String> HEROBRINE_SIGNS = List.of("STOP", "I SEE YOU", "ONLY GOD", "...", "TURN BACK", "NOT SAFE", "FOLLOW ME", "NULL");
   private BukkitRunnable behaviorTask = null;
   private long nextBehaviorTick = 0L;

   public WatcherHerobrineBehavior(WatcherCore core) {
      this.core = core;
      this.bot = core.getBot();
   }

   public void start() {
      if (this.behaviorTask == null) {
         (this.behaviorTask = new BukkitRunnable() {
            int tick;

            {
               Objects.requireNonNull(WatcherHerobrineBehavior.this);
               this.tick = 0;
            }

            public void run() {
               if (!WatcherHerobrineBehavior.this.core.isRunning()) {
                  this.cancel();
                  WatcherHerobrineBehavior.this.behaviorTask = null;
               } else {
                  Player b = WatcherHerobrineBehavior.this.bot.getBotPlayer();
                  if (b != null && b.isOnline()) {
                     if ((long)this.tick >= WatcherHerobrineBehavior.this.nextBehaviorTick) {
                        WatcherHerobrineBehavior.this.tryRandomBehavior();
                        WatcherHerobrineBehavior.this.nextBehaviorTick = (long)(this.tick + 400 + WatcherHerobrineBehavior.this.random.nextInt(500));
                     }

                     this.tick++;
                  }
               }
            }
         }).runTaskTimer(CoreBootstrap.PLUGIN, 100L, 5L);
      }
   }

   public void stop() {
      if (this.behaviorTask != null) {
         this.behaviorTask.cancel();
         this.behaviorTask = null;
      }
   }

   public void tryForceBehavior(Player target) {
      if (target != null && target.isOnline()) {
         this.tryRandomBehavior();
      }
   }

   private void tryRandomBehavior() {
      if (this.core.getAssignedPlayer() != null) {
         Player target = Bukkit.getPlayer(this.core.getAssignedPlayer());
         if (target != null && target.isOnline()) {
            int roll = this.random.nextInt(100);
            if (roll < 15) {
               this.placeRedstoneTorchNear(target);
            } else if (roll < 25) {
               this.placeSmallGlowstonePyramid(target);
            } else if (roll < 35) {
               this.placeHerobrineSign(target);
            } else if (roll < 45) {
               this.placeRedstoneEyes(target);
            } else if (roll < 52) {
               this.placeNetherrackCross(target);
            } else if (roll < 60) {
               this.placeRedstoneTrail(target);
            } else if (roll < 68) {
               this.placeSmallTunnel(target);
            } else if (roll < 75) {
               this.placeWrittenBook(target);
            } else if (roll < 82) {
               this.placeBoneCross(target);
            } else if (roll < 90) {
               this.playHerobrineSound(target);
            } else if (roll < 95) {
               this.watchFromWall(target);
            }
         }
      }
   }

   private void placeRedstoneTorchNear(Player target) {
      Location loc = this.findSurfaceNear(target, 8, 20);
      if (loc != null) {
         Block ground = loc.getWorld().getBlockAt(loc).getRelative(0, -1, 0);
         if (ground.getType().isSolid()) {
            Block torch = ground.getRelative(0, 1, 0);
            torch.setType(Material.REDSTONE_TORCH);
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Redstone torch placed near " + target.getName());
         }
      }
   }

   private void placeSmallGlowstonePyramid(Player target) {
      Location base = this.findSurfaceNear(target, 15, 30);
      if (base != null) {
         World world = base.getWorld();
         int x = base.getBlockX();
         int y = base.getBlockY();
         int z = base.getBlockZ();
         world.getBlockAt(x, y, z).setType(Material.GLOWSTONE);
         world.getBlockAt(x + 1, y, z).setType(Material.GLOWSTONE);
         world.getBlockAt(x, y, z + 1).setType(Material.GLOWSTONE);
         world.getBlockAt(x + 1, y, z + 1).setType(Material.GLOWSTONE);
         world.getBlockAt(x, y + 1, z).setType(Material.GLOWSTONE);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Glowstone pyramid placed near " + target.getName());
      }
   }

   private void placeHerobrineSign(Player target) {
      Location loc = this.findSurfaceNear(target, 5, 15);
      if (loc != null) {
         Block ground = loc.getWorld().getBlockAt(loc).getRelative(0, -1, 0);
         if (ground.getType().isSolid()) {
            Block signBlock = ground.getRelative(0, 1, 0);
            signBlock.setType(Material.OAK_SIGN);
            String signText = HEROBRINE_SIGNS.get(this.random.nextInt(HEROBRINE_SIGNS.size()));
            if (signBlock.getState() instanceof Sign sign) {
               sign.getSide(Side.FRONT).setLine(1, signText);
               sign.update();
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Sign placed near " + target.getName() + ": " + signText);
         }
      }
   }

   private void placeRedstoneEyes(Player target) {
      Location loc = target.getLocation().clone();
      World world = loc.getWorld();

      for (int attempt = 0; attempt < 20; attempt++) {
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         double dist = (double)(5 + this.random.nextInt(8));
         int x = (int)(loc.getX() + Math.cos(angle) * dist);
         int y = (int)(loc.getY() + (double)this.random.nextInt(6) - 3.0);
         int z = (int)(loc.getZ() + Math.sin(angle) * dist);
         Block block = world.getBlockAt(x, y, z);
         if (block.getType() == Material.STONE || block.getType() == Material.DEEPSLATE) {
            block.setType(Material.REDSTONE_BLOCK);
            Block secondEye = block.getRelative(0, 1, 0);
            if (secondEye.getType() == Material.STONE || secondEye.getType() == Material.DEEPSLATE) {
               secondEye.setType(Material.REDSTONE_BLOCK);
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Redstone eyes placed near " + target.getName());
            return;
         }
      }
   }

   private void placeNetherrackCross(Player target) {
      Location base = this.findSurfaceNear(target, 10, 25);
      if (base != null) {
         World world = base.getWorld();
         int x = base.getBlockX();
         int y = base.getBlockY();
         int z = base.getBlockZ();
         world.getBlockAt(x, y, z).setType(Material.NETHERRACK);
         world.getBlockAt(x + 1, y, z).setType(Material.NETHERRACK);
         world.getBlockAt(x - 1, y, z).setType(Material.NETHERRACK);
         world.getBlockAt(x, y, z + 1).setType(Material.NETHERRACK);
         world.getBlockAt(x, y, z - 1).setType(Material.NETHERRACK);
         world.getBlockAt(x, y + 1, z).setType(Material.FIRE);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Netherrack cross placed near " + target.getName());
      }
   }

   private void playHerobrineSound(Player target) {
      Location loc = target.getLocation();
      World world = loc.getWorld();
      this.core.getVisuals().playRandomHerobrineSound(world, loc);
      CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Ambient sound played near " + target.getName());
   }

   private Location findSurfaceNear(Player target, int minDist, int maxDist) {
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

   private void placeRedstoneTrail(Player target) {
      Location loc = this.findSurfaceNear(target, 10, 25);
      if (loc != null) {
         World world = loc.getWorld();
         int x = loc.getBlockX();
         int z = loc.getBlockZ();
         int y = loc.getBlockY() - 1;
         Location targetLoc = target.getLocation();
         double dx = targetLoc.getX() - (double)x;
         double dz = targetLoc.getZ() - (double)z;
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len != 0.0) {
            double nx = dx / len;
            double nz = dz / len;

            for (int i = 0; i < 5 + this.random.nextInt(5); i++) {
               int tx = (int)((double)x + nx * (double)i);
               int tz = (int)((double)z + nz * (double)i);
               Block ground = world.getBlockAt(tx, y, tz);
               if (ground.getType().isSolid()) {
                  Block dust = ground.getRelative(0, 1, 0);
                  if (dust.getType() == Material.AIR) {
                     dust.setType(Material.REDSTONE_WIRE);
                  }
               }
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Redstone trail placed near " + target.getName());
         }
      }
   }

   private void placeSmallTunnel(Player target) {
      Location loc = target.getLocation();
      World world = loc.getWorld();

      for (int attempt = 0; attempt < 20; attempt++) {
         double angle = this.random.nextDouble() * 2.0 * Math.PI;
         double dist = (double)(8 + this.random.nextInt(12));
         int x = (int)(loc.getX() + Math.cos(angle) * dist);
         int z = (int)(loc.getZ() + Math.sin(angle) * dist);
         int y = loc.getBlockY();
         Block block = world.getBlockAt(x, y, z);
         if (block.getType().isSolid() && !block.getType().name().contains("LEAVES")) {
            for (int d = 0; d < 3 + this.random.nextInt(4); d++) {
               int nx = x + (int)(Math.cos(angle) * (double)d);
               int nz = z + (int)(Math.sin(angle) * (double)d);
               Block b = world.getBlockAt(nx, y, nz);
               if (b.getType().isSolid()) {
                  b.setType(Material.AIR);
                  Block above = b.getRelative(0, 1, 0);
                  if (above.getType().isSolid()) {
                     above.setType(Material.AIR);
                  }
               }
            }

            int endX = x + (int)(Math.cos(angle) * 2.0);
            int endZ = z + (int)(Math.sin(angle) * 2.0);
            Block endBlock = world.getBlockAt(endX, y - 1, endZ);
            if (endBlock.getType().isSolid()) {
               Block torch = endBlock.getRelative(0, 1, 0);
               if (torch.getType() == Material.AIR) {
                  torch.setType(Material.REDSTONE_TORCH);
               }
            }

            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Small tunnel placed near " + target.getName());
            return;
         }
      }
   }

   private void placeWrittenBook(Player target) {
      Location loc = this.findSurfaceNear(target, 5, 12);
      if (loc != null) {
         World world = loc.getWorld();
         int x = loc.getBlockX();
         int z = loc.getBlockZ();
         int y = loc.getBlockY();
         Block ground = world.getBlockAt(x, y - 1, z);
         if (ground.getType().isSolid()) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta)book.getItemMeta();
            if (meta != null) {
               meta.setTitle("???");
               meta.setAuthor("Herobrine");
               List<String> pages = Arrays.asList(
                  "§0Ich sehe dich.\n\n§4Du solltest nicht hier sein.",
                  "§0Die Welt kennt\ndeinen Namen.\n\nSie wartet auf dich.",
                  "§0Nur Gott kann\ndich retten.\n\nAber Gott ist\nnicht hier.",
                  "§0Schau hinter dich.\n\n§4Jetzt."
               );
               meta.setPages(pages);
               book.setItemMeta(meta);
            }

            world.dropItemNaturally(new Location(world, (double)x + 0.5, (double)y, (double)z + 0.5), book);
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Written book dropped near " + target.getName());
         }
      }
   }

   private void placeBoneCross(Player target) {
      Location base = this.findSurfaceNear(target, 8, 20);
      if (base != null) {
         World world = base.getWorld();
         int x = base.getBlockX();
         int y = base.getBlockY();
         int z = base.getBlockZ();
         world.getBlockAt(x, y, z).setType(Material.BONE_BLOCK);
         world.getBlockAt(x + 1, y, z).setType(Material.BONE_BLOCK);
         world.getBlockAt(x - 1, y, z).setType(Material.BONE_BLOCK);
         world.getBlockAt(x, y, z + 1).setType(Material.BONE_BLOCK);
         world.getBlockAt(x, y, z - 1).setType(Material.BONE_BLOCK);
         world.getBlockAt(x, y + 1, z).setType(Material.SKELETON_SKULL);
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Bone cross placed near " + target.getName());
      }
   }

   private void watchFromWall(Player target) {
      if (target.isOnline()) {
         Location loc = target.getLocation();
         World world = loc.getWorld();
         double yaw = Math.toRadians((double)loc.getYaw());
         double dx = -Math.sin(yaw);
         double dz = Math.cos(yaw);

         for (int dist = 3; dist <= 12; dist++) {
            int wx = (int)(loc.getX() + dx * (double)dist);
            int wz = (int)(loc.getZ() + dz * (double)dist);
            int wy = loc.getBlockY();
            Block wall = world.getBlockAt(wx, wy, wz);
            if (wall.getType().isSolid() && !wall.getType().name().contains("LEAVES")) {
               Block eye1 = wall.getRelative(0, 1, 0);
               Block eye2 = wall.getRelative(0, 2, 0);
               if (eye1.getType().isSolid() && eye2.getType().isSolid()) {
                  eye1.setType(Material.REDSTONE_BLOCK);
                  eye2.setType(Material.REDSTONE_BLOCK);
                  Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                     if (eye1.getType() == Material.REDSTONE_BLOCK) {
                        eye1.setType(Material.STONE);
                     }

                     if (eye2.getType() == Material.REDSTONE_BLOCK) {
                        eye2.setType(Material.STONE);
                     }
                  }, (long)(200 + this.random.nextInt(400)));
                  CoreBootstrap.PLUGIN.getLogger().info("[Watcher/Herobrine] Watching from wall near " + target.getName());
                  return;
               }
            }
         }
      }
   }
}
