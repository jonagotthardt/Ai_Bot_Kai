package com.jonasmp.ai.watcher;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class BlockPlacementRules {
   private static final int MAX_BUILD_HEIGHT_ABOVE = 5;
   private static final int MAX_BUILD_DISTANCE = 10;
   private static final int MAX_BRIDGE_GAP = 2;

   private BlockPlacementRules() {
   }

   public static boolean canPlaceHere(Location botLoc, Location placeLoc, Material type) {
      if (botLoc != null && placeLoc != null && type != null) {
         if (botLoc.distance(placeLoc) > 10.0) {
            return false;
         } else if (placeLoc.getY() > botLoc.getY() + 5.0) {
            return false;
         } else {
            Block below = placeLoc.getBlock().getRelative(BlockFace.DOWN);
            return below.getType().isSolid() || isValidSupport(below.getType());
         }
      } else {
         return false;
      }
   }

   private static boolean isValidSupport(Material type) {
      return type.isSolid()
         || type == Material.DIRT
         || type == Material.GRASS_BLOCK
         || type == Material.STONE
         || type == Material.COBBLESTONE
         || type == Material.SAND
         || type == Material.GRAVEL
         || type == Material.NETHERRACK
         || type == Material.END_STONE
         || type.name().contains("PLANKS")
         || type.name().contains("LOG")
         || type.name().contains("CONCRETE");
   }

   public static boolean isBridgeValid(Location from, Location to) {
      if (from != null && to != null) {
         double dist = from.distance(to);
         if (dist > 10.0) {
            return false;
         } else {
            double dx = Math.abs(from.getX() - to.getX());
            double dz = Math.abs(from.getZ() - to.getZ());
            return !(dx > 2.0) && !(dz > 2.0);
         }
      } else {
         return false;
      }
   }

   public static double maxBuildHeight(Location botLoc) {
      return botLoc.getY() + 5.0;
   }

   public static double maxBuildDistance() {
      return 10.0;
   }
}
