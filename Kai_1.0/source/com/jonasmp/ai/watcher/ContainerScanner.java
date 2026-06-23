package com.jonasmp.ai.watcher;

import com.jonasmp.ai.JonaSMP_AI;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ContainerScanner {
   public static ContainerScanner.ContainerScanResult scanBlock(Block block) {
      if (block == null) {
         return null;
      } else {
         Material type = block.getType();
         BlockState state = block.getState();
         Inventory inv = null;
         String containerType = type.name();
         if (state instanceof Chest) {
            inv = ((Chest)state).getInventory();
         } else if (state instanceof Furnace) {
            inv = ((Furnace)state).getInventory();
         } else if (state instanceof Hopper) {
            inv = ((Hopper)state).getInventory();
         } else if (state instanceof BrewingStand) {
            inv = ((BrewingStand)state).getInventory();
         }

         if (inv == null) {
            return null;
         } else {
            Map<Integer, String> slots = new HashMap<>();
            int totalItems = 0;

            for (int i = 0; i < inv.getSize(); i++) {
               ItemStack item = inv.getItem(i);
               if (item != null && item.getType() != Material.AIR) {
                  slots.put(i, item.getType().name() + "x" + item.getAmount());
                  totalItems += item.getAmount();
               }
            }

            CoreBootstrap.PLUGIN
               .getLogger()
               .fine(
                  "[ContainerScanner] Scanned "
                     + containerType
                     + " at "
                     + block.getX()
                     + ","
                     + block.getY()
                     + ","
                     + block.getZ()
                     + " — "
                     + slots.size()
                     + " slots occupied."
               );
            return new ContainerScanner.ContainerScanResult(containerType, slots, totalItems);
         }
      }
   }

   public static Block findNearestContainer(Player bot, Material[] types, double maxDistance) {
      try {
         if (JonaSMP_AI.getInstance() != null && JonaSMP_AI.getInstance().getChunkRadar() != null) {
            int radius = (int)maxDistance;

            for (Material m : types) {
               List<Location> found = JonaSMP_AI.getInstance().getChunkRadar().scanFor(bot, m, radius);
               if (!found.isEmpty()) {
                  return found.get(0).getBlock();
               }
            }
         }
      } catch (Exception var22) {
      }

      Block nearest = null;
      double minDistSq = Double.MAX_VALUE;
      Location loc = bot.getLocation();
      int r = (int)Math.ceil(maxDistance);
      int cx = loc.getBlockX();
      int cy = loc.getBlockY();
      int cz = loc.getBlockZ();

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               Block b = loc.getWorld().getBlockAt(cx + dx, cy + dy, cz + dz);

               for (Material mx : types) {
                  if (b.getType() == mx) {
                     double distSq = (double)(dx * dx + dy * dy + dz * dz);
                     if (distSq < minDistSq) {
                        minDistSq = distSq;
                        nearest = b;
                     }
                  }
               }
            }
         }
      }

      return nearest;
   }

   public static class ContainerScanResult {
      public final String type;
      public final Map<Integer, String> slots;
      public final int totalItems;
      public final boolean isEmpty;

      public ContainerScanResult(String type, Map<Integer, String> slots, int totalItems) {
         this.type = type;
         this.slots = slots;
         this.totalItems = totalItems;
         this.isEmpty = totalItems == 0;
      }
   }
}
