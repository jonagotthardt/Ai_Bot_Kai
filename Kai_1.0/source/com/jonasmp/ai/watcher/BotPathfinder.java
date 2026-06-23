package com.jonasmp.ai.watcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BotPathfinder {
   private static final double COST_STRAIGHT = 1.0;
   private static final double COST_DIAGONAL = 1.414;
   private static final double COST_JUMP_UP = 2.5;
   private static final double COST_FALL = 0.5;
   private static final double COST_WATER = 8.0;
   private static final double COST_LAVA = 999.0;
   private static final int MAX_PATH_LENGTH = 100;
   private static final int MAX_FALL_DISTANCE = 3;
   private static final int MAX_SEARCH_NODES = 1000;
   private static final int[][] DIRECTIONS = new int[][]{
      {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}, {0, 1, 0}, {0, -1, 0}
   };

   public static List<Location> findPath(Location start, Location goal) {
      return findPath(start, goal, 200);
   }

   public static List<Location> findPath(Location start, Location goal, int maxLength) {
      World world = start.getWorld();
      if (!world.equals(goal.getWorld())) {
         return Collections.emptyList();
      } else {
         BotPathfinder.Node startNode = new BotPathfinder.Node(start.getBlockX(), start.getBlockY(), start.getBlockZ());
         BotPathfinder.Node goalNode = new BotPathfinder.Node(goal.getBlockX(), goal.getBlockY(), goal.getBlockZ());
         if (start.distance(goal) < 2.0) {
            return Collections.singletonList(goal);
         } else {
            PriorityQueue<BotPathfinder.Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
            Set<BotPathfinder.Node> closedSet = new HashSet<>();
            Map<BotPathfinder.Node, BotPathfinder.Node> cameFrom = new HashMap<>();
            Map<BotPathfinder.Node, Double> gScore = new HashMap<>();
            gScore.put(startNode, 0.0);
            startNode.fScore = heuristic(startNode, goalNode);
            openSet.add(startNode);
            int nodesChecked = 0;

            while (!openSet.isEmpty() && nodesChecked < 5000) {
               BotPathfinder.Node current = openSet.poll();
               nodesChecked++;
               if (current.equals(goalNode)) {
                  return reconstructPath(cameFrom, current, world);
               }

               closedSet.add(current);

               for (int[] dir : DIRECTIONS) {
                  BotPathfinder.Node neighbor = new BotPathfinder.Node(current.x + dir[0], current.y + dir[1], current.z + dir[2]);
                  if (!closedSet.contains(neighbor)) {
                     double moveCost = getMoveCost(world, current, neighbor);
                     if (moveCost < 999.0) {
                        double tentativeG = gScore.getOrDefault(current, Double.MAX_VALUE) + moveCost;
                        if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                           cameFrom.put(neighbor, current);
                           gScore.put(neighbor, tentativeG);
                           neighbor.fScore = tentativeG + heuristic(neighbor, goalNode);
                           if (!openSet.contains(neighbor)) {
                              openSet.add(neighbor);
                           }
                        }
                     }
                  }
               }
            }

            return Collections.emptyList();
         }
      }
   }

   private static double heuristic(BotPathfinder.Node a, BotPathfinder.Node b) {
      double dx = (double)(a.x - b.x);
      double dy = (double)(a.y - b.y);
      double dz = (double)(a.z - b.z);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double getMoveCost(World world, BotPathfinder.Node current, BotPathfinder.Node neighbor) {
      boolean isDiagonal = current.x != neighbor.x && current.z != neighbor.z;
      double baseCost = isDiagonal ? 1.414 : 1.0;
      Block destBlock = world.getBlockAt(neighbor.x, neighbor.y, neighbor.z);
      Material destType = destBlock.getType();
      if (destType.name().contains("LAVA")) {
         return 999.0;
      } else if (destType.name().contains("WATER")) {
         return baseCost + 8.0;
      } else if (current.y != neighbor.y) {
         if (neighbor.y > current.y) {
            if (neighbor.y - current.y > 1) {
               return 999.0;
            } else if (!isPassable(destType)) {
               return 999.0;
            } else {
               Block above = world.getBlockAt(neighbor.x, neighbor.y + 1, neighbor.z);
               if (!isPassable(above.getType())) {
                  return 999.0;
               } else {
                  Block ground = world.getBlockAt(neighbor.x, neighbor.y - 1, neighbor.z);
                  if (!ground.getType().isSolid()) {
                     return 999.0;
                  } else {
                     Block currentHead = world.getBlockAt(current.x, current.y + 2, current.z);
                     return !isPassable(currentHead.getType()) ? 999.0 : baseCost + 2.5;
                  }
               }
            }
         } else if (neighbor.y >= current.y) {
            return baseCost;
         } else {
            int drop = current.y - neighbor.y;
            if (drop > 3) {
               return 999.0;
            } else if (!isPassable(destType)) {
               return 999.0;
            } else {
               Block feet = world.getBlockAt(neighbor.x, neighbor.y + 1, neighbor.z);
               if (!isPassable(feet.getType())) {
                  return 999.0;
               } else {
                  Block ground2 = world.getBlockAt(neighbor.x, neighbor.y - 1, neighbor.z);
                  return !ground2.getType().isSolid() && !ground2.getType().name().contains("WATER") ? 999.0 : baseCost + 0.5 * (double)drop;
               }
            }
         }
      } else if (!isPassable(destType)) {
         return 999.0;
      } else {
         Block feetBlock = world.getBlockAt(neighbor.x, neighbor.y + 1, neighbor.z);
         if (!isPassable(feetBlock.getType())) {
            return 999.0;
         } else {
            Block groundBlock = world.getBlockAt(neighbor.x, neighbor.y - 1, neighbor.z);
            if (!groundBlock.getType().isSolid() && !isPassable(groundBlock.getType())) {
               int fallDist = 0;

               for (int dy = 1; dy <= 3; dy++) {
                  Block below = world.getBlockAt(neighbor.x, neighbor.y - 1 - dy, neighbor.z);
                  if (below.getType().isSolid() || below.getType().name().contains("WATER")) {
                     fallDist = dy;
                     break;
                  }
               }

               return fallDist == 0 ? 999.0 : baseCost + 0.5 * (double)fallDist;
            } else {
               return baseCost;
            }
         }
      }
   }

   private static boolean isPassable(Material type) {
      if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
         String name = type.name();
         return name.contains("SAPLING")
            || name.contains("GRASS") && !name.contains("BLOCK")
            || name.contains("FLOWER")
            || name.contains("TORCH")
            || name.contains("SIGN")
            || name.contains("BUTTON")
            || name.contains("LEVER")
            || name.contains("RAIL")
            || name.contains("TRAPDOOR")
            || name.contains("DOOR") && !name.contains("BLOCK")
            || name.contains("CARPET")
            || name.contains("SNOW") && !name.contains("BLOCK")
            || name.contains("VINE")
            || name.contains("LADDER")
            || name.contains("WIRE")
            || name.contains("TRIPWIRE")
            || name.contains("BANNER")
            || name.contains("HEAD")
            || name.contains("SKULL")
            || name.contains("PLATE")
            || name.contains("MUSHROOM") && !name.contains("BLOCK");
      } else {
         return true;
      }
   }

   private static List<Location> reconstructPath(Map<BotPathfinder.Node, BotPathfinder.Node> cameFrom, BotPathfinder.Node current, World world) {
      List<Location> path = new ArrayList<>();
      BotPathfinder.Node node = current;

      while (node != null) {
         path.add(new Location(world, (double)node.x + 0.5, (double)node.y, (double)node.z + 0.5));
         node = cameFrom.get(node);
      }

      Collections.reverse(path);
      return path;
   }

   private static class Node {
      final int x;
      final int y;
      final int z;
      double fScore = Double.MAX_VALUE;

      Node(int x, int y, int z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof BotPathfinder.Node n) ? false : this.x == n.x && this.y == n.y && this.z == n.z;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.x, this.y, this.z);
      }

      @Override
      public String toString() {
         return "(" + this.x + "," + this.y + "," + this.z;
      }
   }
}
