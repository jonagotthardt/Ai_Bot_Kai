package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BrokenScriptEvents {
   private static final Random random = ThreadLocalRandom.current();
   public static final List<String> FAKE_MESSAGES = Arrays.asList(
      "§8§oWer hat das gesagt?",
      "§8§oDas war nicht ich.",
      "§8§oWarum höre ich Stimmen?",
      "§8§oIch habe das nicht geschrieben.",
      "§8§oJemand anderes kontrolliert meine Tastatur.",
      "§8§oDie Welt hier ist kaputt.",
      "§8§oNULL§8§o",
      "§8§oERROR: player.mind not found",
      "§8§oDu solltest aufhören zu spielen.",
      "§8§oSiehst du die Zahlen?",
      "§8§o01001000 01100101 01101100 01110000",
      "§8§oIch bin noch hier. Immer noch hier.",
      "§8§oDer Code ist gebrochen."
   );

   public static void blockCorruption(Player target, int radius, int maxBlocks) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location center = target.getLocation();
         if (center.getWorld() == null) {
            return;
         }

         World world = center.getWorld();
         Map<Block, Material> originalBlocks = new HashMap<>();
         List<Material> corruptMap = Arrays.asList(
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE,
            Material.GRAVEL,
            Material.STONE,
            Material.ANDESITE,
            Material.DIORITE
         );
         int changed = 0;

         for (int x = -radius; x <= radius && changed < maxBlocks; x++) {
            for (int y = -radius; y <= radius && changed < maxBlocks; y++) {
               for (int z = -radius; z <= radius && changed < maxBlocks; z++) {
                  Block b = center.clone().add((double)x, (double)y, (double)z).getBlock();
                  if (b.getType().isSolid() && !b.getType().name().contains("BEDROCK")) {
                     originalBlocks.put(b, b.getType());
                     b.setType(corruptMap.get(random.nextInt(corruptMap.size())));
                     changed++;
                  }
               }
            }
         }

         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Block corruption near " + target.getName() + " (" + changed + " blocks)");
         int restoreDelay = 300 + random.nextInt(300);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            for (Entry<Block, Material> entry : originalBlocks.entrySet()) {
               if (entry.getKey().getType() != Material.AIR) {
                  entry.getKey().setType(entry.getValue());
               }
            }
         }, (long)restoreDelay);
      } catch (Exception var12) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] blockCorruption failed: " + var12.getMessage());
      }
   }

   public static void itemVanish(Player target, int durationTicks) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         PlayerInventory inv = target.getInventory();
         int slot = random.nextInt(36);
         ItemStack item = inv.getItem(slot);
         if (item == null || item.getType() == Material.AIR) {
            return;
         }

         inv.setItem(slot, (ItemStack)null);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Item vanished from " + target.getName() + " slot " + slot);
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            if (target.isOnline()) {
               target.getInventory().setItem(slot, item);
               target.sendMessage("§8§oDein " + item.getType().name().toLowerCase() + " war kurz verschwunden...");
            }
         }, (long)durationTicks);
      } catch (Exception var5) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] itemVanish failed: " + var5.getMessage());
      }
   }

   public static void doorAnomaly(Player target, int radius) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location loc = target.getLocation();
         if (loc.getWorld() == null) {
            return;
         }

         List<Block> doors = new ArrayList<>();

         for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
               for (int z = -radius; z <= radius; z++) {
                  Block b = loc.clone().add((double)x, (double)y, (double)z).getBlock();
                  if (b.getBlockData() instanceof Openable) {
                     doors.add(b);
                  }
               }
            }
         }

         if (doors.isEmpty()) {
            return;
         }

         Block door = doors.get(random.nextInt(doors.size()));
         Openable data = (Openable)door.getBlockData();
         data.setOpen(!data.isOpen());
         door.setBlockData(data);
         loc.getWorld().playSound(door.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 0.5F, 0.8F);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Door anomaly at " + door.getLocation());
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] doorAnomaly failed: " + var8.getMessage());
      }
   }

   public static void chestAnomaly(Player target, int radius) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location loc = target.getLocation();
         if (loc.getWorld() == null) {
            return;
         }

         List<Block> chests = new ArrayList<>();

         for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
               for (int z = -radius; z <= radius; z++) {
                  Block b = loc.clone().add((double)x, (double)y, (double)z).getBlock();
                  if (b.getState() instanceof Chest) {
                     chests.add(b);
                  }
               }
            }
         }

         if (chests.isEmpty()) {
            return;
         }

         Block chest = chests.get(random.nextInt(chests.size()));
         loc.getWorld().playSound(chest.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6F, 0.5F);
         target.sendMessage("§8§oEtwas hat eine Kiste geöffnet...");
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Chest anomaly near " + target.getName());
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] chestAnomaly failed: " + var8.getMessage());
      }
   }

   public static void torchExtinguish(Player target, int radius) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location loc = target.getLocation();
         if (loc.getWorld() == null) {
            return;
         }

         List<Block> torches = new ArrayList<>();

         for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
               for (int z = -radius; z <= radius; z++) {
                  Block b = loc.clone().add((double)x, (double)y, (double)z).getBlock();
                  if (b.getType() == Material.TORCH
                     || b.getType() == Material.WALL_TORCH
                     || b.getType() == Material.REDSTONE_TORCH
                     || b.getType() == Material.REDSTONE_WALL_TORCH) {
                     torches.add(b);
                  }
               }
            }
         }

         if (torches.isEmpty()) {
            return;
         }

         int count = Math.min(5, torches.size());

         for (int i = 0; i < count; i++) {
            Block t = torches.get(random.nextInt(torches.size()));
            Material orig = t.getType();
            t.setType(Material.AIR);
            loc.getWorld().playSound(t.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.3F, 1.0F);
            Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
               if (t.getType() == Material.AIR) {
                  t.setType(orig);
               }
            }, (long)(200 + random.nextInt(200)));
         }
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] torchExtinguish failed: " + var8.getMessage());
      }
   }

   public static void fakeChat(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         String msg = FAKE_MESSAGES.get(random.nextInt(FAKE_MESSAGES.size()));
         target.sendMessage(msg);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Fake chat sent to " + target.getName());
      } catch (Exception var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] fakeChat failed: " + var2.getMessage());
      }
   }

   public static void healthGlitch(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
         target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30, 0, false, false, false));
         target.sendMessage("§c§o*Schmerz*");
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Health glitch on " + target.getName());
      } catch (Exception var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] healthGlitch failed: " + var2.getMessage());
      }
   }

   public static void cameraGlitch(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false, false));
         target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false, false));
         target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3F, 2.0F);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Camera glitch on " + target.getName());
      } catch (Exception var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] cameraGlitch failed: " + var2.getMessage());
      }
   }

   public static void brokenScriptEnd(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location center = target.getLocation();
         if (center.getWorld() == null) {
            return;
         }

         World world = center.getWorld();
         int radius = 3;
         List<Block> bedrockBlocks = new ArrayList<>();
         Map<Block, Material> originals = new HashMap<>();

         for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
               for (int z = -3; z <= 3; z++) {
                  if (x * x + y * y + z * z <= 9) {
                     Block b = center.clone().add((double)x, (double)y, (double)z).getBlock();
                     if (b.getType() != Material.BEDROCK) {
                        originals.put(b, b.getType());
                        b.setType(Material.BEDROCK);
                        bedrockBlocks.add(b);
                     }
                  }
               }
            }
         }

         for (int i = 0; i < 8; i++) {
            int offsetX = random.nextInt(7) - 3;
            int offsetZ = random.nextInt(7) - 3;
            Block lava = center.clone().add((double)offsetX, (double)(5 + random.nextInt(3)), (double)offsetZ).getBlock();
            if (lava.getType() == Material.AIR) {
               lava.setType(Material.LAVA);
               Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
                  if (lava.getType() == Material.LAVA) {
                     lava.setType(Material.AIR);
                  }
               }, (long)(100 + random.nextInt(100)));
            }
         }

         target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, false, false, false));
         target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2, false, false, false));
         world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5F, 0.5F);
         target.sendMessage("§4§lTHE WORLD IS BROKEN.");
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            for (Entry<Block, Material> entry : originals.entrySet()) {
               if (entry.getKey().getType() == Material.BEDROCK) {
                  entry.getKey().setType(entry.getValue());
               }
            }

            target.sendMessage("§8§o...Es war nur ein Traum?");
         }, 200L);
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] BROKEN SCRIPT END triggered on " + target.getName());
      } catch (Exception var10) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] brokenScriptEnd failed: " + var10.getMessage());
      }
   }

   public static void tempBanKick(Player target, int seconds) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         String msg = "§4§lTHE BROKEN SCRIPT\n§cYou have been consumed.\n§8Return in" + seconds + " seconds...";
         target.kickPlayer(msg);
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] Temp-kicked " + target.getName() + " for " + seconds);
         Bukkit.getScheduler()
            .runTaskLater(
               CoreBootstrap.PLUGIN, () -> CoreBootstrap.PLUGIN.getLogger().info("[TBS] " + target.getName() + " may now rejoin."), (long)(seconds * 20)
            );
      } catch (Exception var3) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] tempBanKick failed: " + var3.getMessage());
      }
   }

   public static void soundGlitch(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Location loc = target.getLocation();
         Sound[] sounds = new Sound[]{
            Sound.AMBIENT_CAVE,
            Sound.BLOCK_SCULK_SENSOR_CLICKING,
            Sound.ENTITY_GHAST_AMBIENT,
            Sound.ENTITY_ENDERMAN_STARE,
            Sound.ENTITY_WARDEN_HEARTBEAT,
            Sound.BLOCK_BELL_RESONATE,
            Sound.ENTITY_SILVERFISH_AMBIENT
         };
         Sound s = sounds[random.nextInt(sounds.length)];
         loc.getWorld().playSound(loc, s, 0.4F, random.nextFloat() < 0.5F ? 0.5F : 1.5F);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Sound glitch on " + target.getName());
      } catch (Exception var4) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] soundGlitch failed: " + var4.getMessage());
      }
   }

   public static void inventoryShuffle(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         PlayerInventory inv = target.getInventory();
         ItemStack[] contents = inv.getStorageContents();
         List<Integer> filledSlots = new ArrayList<>();

         for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
               filledSlots.add(i);
            }
         }

         if (filledSlots.size() < 2) {
            return;
         }

         int slotA = filledSlots.get(random.nextInt(filledSlots.size()));
         int slotB = filledSlots.get(random.nextInt(filledSlots.size()));

         while (slotA == slotB) {
            slotB = filledSlots.get(random.nextInt(filledSlots.size()));
         }

         ItemStack temp = contents[slotA];
         contents[slotA] = contents[slotB];
         contents[slotB] = temp;
         inv.setStorageContents(contents);
         target.sendMessage("§8§oDeine Taschen scheinen... anders angeordnet.");
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Inventory shuffle on " + target.getName());
      } catch (Exception var7) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] inventoryShuffle failed: " + var7.getMessage());
      }
   }

   public static void skyGlitch(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         World world = target.getWorld();
         world.setThundering(true);
         world.setThunderDuration(100);
         world.setStorm(true);
         world.setWeatherDuration(100);
         target.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8F, 1.0F);
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Sky glitch on " + target.getName());
      } catch (Exception var2) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] skyGlitch failed: " + var2.getMessage());
      }
   }

   public static void blockUnderFoot(Player target) {
      try {
         if (target == null || !target.isOnline()) {
            return;
         }

         Block under = target.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
         if (under.getType() == Material.AIR) {
            return;
         }

         Material orig = under.getType();
         Material[] swaps = new Material[]{Material.OBSIDIAN, Material.BEDROCK, Material.LAVA, Material.CACTUS};
         Material swap = swaps[random.nextInt(swaps.length)];
         under.setType(swap);
         target.sendMessage("§4§oDer Boden hat sich verändert!");
         CoreBootstrap.PLUGIN.getLogger().info("[TBS] Block under foot swapped for " + target.getName());
         Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
            if (under.getType() == swap) {
               under.setType(orig);
            }
         }, 60L);
      } catch (Exception var5) {
         CoreBootstrap.PLUGIN.getLogger().warning("[TBS] blockUnderFoot failed: " + var5.getMessage());
      }
   }
}
