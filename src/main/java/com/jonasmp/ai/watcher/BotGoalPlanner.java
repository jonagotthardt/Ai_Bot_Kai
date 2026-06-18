package com.jonasmp.ai.watcher;

import com.jonasmp.ai.JonaSMP_AI;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BotGoalPlanner {
   private BotGoalPlanner.GoalType currentGoal = BotGoalPlanner.GoalType.IDLE;
   private final List<BotGoalPlanner.Step> goalSteps = new ArrayList<>();
   private int currentStepIndex = 0;
   private int stepStuckCounter = 0;
   private long goalStartTime = 0L;
   public Location targetLocation = null;
   public Entity nearestThreat = null;
   public boolean isNight = false;
   public double healthPercent = 1.0;
   public int foodLevel = 20;
   public int woodCount = 0;
   public int plankCount = 0;
   public int stickCount = 0;
   public int stoneCount = 0;
   public int coalCount = 0;
   public int ironCount = 0;
   public boolean hasWoodenPickaxe = false;
   public boolean hasStonePickaxe = false;
   public boolean hasIronPickaxe = false;
   public double surfaceY = 0.0;
   public long stepStartTick = 0L;
   private long playerOverrideUntil = 0L;
   Player followTargetPlayer = null;
   private Block currentMiningBlock = null;
   private static final Map<UUID, Integer> stuckTicks = new HashMap<>();
   private static final Map<UUID, Location> lastPositions = new HashMap<>();
   private static final Map<UUID, List<Location>> botPaths = new HashMap<>();
   private static final Map<UUID, Location> botPathTargets = new HashMap<>();
   private static final Map<UUID, Long> botPathComputeTime = new HashMap<>();
   private static final long PATH_RECALC_INTERVAL_MS = 5000L;
   private static final double PATH_DEVIATION_THRESHOLD = 3.0;

   public void setPlayerOverride(long until) {
      this.playerOverrideUntil = until;
   }

   public void setGoal(BotGoalPlanner.GoalType goal) {
      this.currentGoal = goal;
      this.goalSteps.clear();
      this.currentStepIndex = 0;
      this.stepStuckCounter = 0;
      this.stepStartTick = 0L;
      this.surfaceY = 0.0;
      this.currentMiningBlock = null;
      this.goalStartTime = System.currentTimeMillis();
      switch (goal) {
         case GATHER_WOOD:
            this.buildGatherWood();
            break;
         case GATHER_STONE:
            this.buildGatherStone();
            break;
         case CRAFT_BASIC_TOOLS:
            this.buildCraftTools();
            break;
         case MINE_SURFACE_ORES:
            this.buildMineOres();
            break;
         case DIG_FOR_DIAMONDS:
            this.buildDigDiamonds();
            break;
         case BUILD_SHELTER:
            this.buildShelter();
            break;
         case FIND_FOOD:
            this.buildFindFood();
            break;
         case EXPLORE:
            this.buildExplore();
            break;
         case SURVIVE_NIGHT:
            this.buildSurviveNight();
            break;
         case FLEE_DANGER:
            this.buildFlee();
            break;
         case SMELT_ORES:
            this.buildSmeltOres();
            break;
         case DEEP_MINE:
            this.buildDeepMine();
            break;
         case COOK_FOOD:
            this.buildCookFood();
            break;
         case BUILD_BASIC_HOUSE:
            this.buildBasicHouse();
            break;
         case BRIDGE_GAP:
            this.buildBridgeGap();
            break;
         case FILL_HOLE:
            this.buildFillHole();
            break;
         case PLACE_TORCHES:
            this.buildPlaceTorches();
            break;
         case FARM_CROPS:
            this.buildFarmCrops();
            break;
         case BREED_ANIMALS:
            this.buildBreedAnimals();
            break;
         case SHEAR_SHEEP:
            this.buildShearSheep();
            break;
         case TRADE_VILLAGER:
            this.buildTradeVillager();
            break;
         case ENCHANT_GEAR:
            this.buildEnchantGear();
            break;
         case BREW_POTIONS:
            this.buildBrewPotions();
            break;
         case BUILD_NETHER_PORTAL:
            this.buildNetherPortal();
            break;
         case EXPLORE_END:
            this.buildExploreEnd();
      }

      CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Goal: " + goal + " (" + this.goalSteps.size() + " steps)");
   }

   public boolean tick(Player bot, NMSBot nmsBot) {
      this.updateContext(bot);
      if (this.followTargetPlayer != null && this.followTargetPlayer.isOnline()) {
         this.targetLocation = this.followTargetPlayer.getLocation().clone();
      }

      boolean playerOverride = System.currentTimeMillis() < this.playerOverrideUntil;
      if (!playerOverride && this.currentGoal == BotGoalPlanner.GoalType.IDLE) {
         if (this.nearestThreat != null) {
            this.setGoal(BotGoalPlanner.GoalType.FLEE_DANGER);
         } else if (this.isNight) {
            this.setGoal(BotGoalPlanner.GoalType.SURVIVE_NIGHT);
         }
      }

      if (this.currentGoal == BotGoalPlanner.GoalType.IDLE) {
         return false;
      } else if (this.goalStartTime > 0L && System.currentTimeMillis() - this.goalStartTime > 300000L) {
         CoreBootstrap.PLUGIN.getLogger().warning("[GoalPlanner] " + this.currentGoal + " ABORTED after 5min timeout.");
         this.setGoal(BotGoalPlanner.GoalType.IDLE);
         return false;
      } else if (this.currentStepIndex >= this.goalSteps.size()) {
         CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] " + this.currentGoal + " done.");
         this.setGoal(BotGoalPlanner.GoalType.IDLE);
         return false;
      } else {
         BotGoalPlanner.Step step = this.goalSteps.get(this.currentStepIndex);
         if (step.condition.isMet(bot, this)) {
            CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Step done: " + step.id);
            this.currentStepIndex++;
            this.stepStuckCounter = 0;
            this.surfaceY = bot.getLocation().getY();
            this.stepStartTick = System.currentTimeMillis();
            return true;
         } else {
            if (this.stepStartTick == 0L) {
               this.surfaceY = bot.getLocation().getY();
               this.stepStartTick = System.currentTimeMillis();
            }

            boolean progress = step.action.execute(bot, nmsBot, this);
            if (progress) {
               this.stepStuckCounter = 0;
            } else {
               this.stepStuckCounter++;
               if (this.stepStuckCounter > 600) {
                  BotGoalPlanner.Step fb = step.fallback.onBlocked(bot, this, "stuck");
                  if (fb != null) {
                     this.goalSteps.set(this.currentStepIndex, fb);
                     this.stepStuckCounter = 0;
                  }
               }
            }

            return true;
         }
      }
   }

   public BotGoalPlanner.GoalType getCurrentGoal() {
      return this.currentGoal;
   }

   public String getCurrentStepDesc() {
      return this.currentStepIndex >= this.goalSteps.size() ? "Done" : this.goalSteps.get(this.currentStepIndex).description;
   }

   private void updateContext(Player bot) {
      PlayerInventory inv = bot.getInventory();
      this.woodCount = countAny(
         inv,
         Material.OAK_LOG,
         Material.BIRCH_LOG,
         Material.SPRUCE_LOG,
         Material.JUNGLE_LOG,
         Material.ACACIA_LOG,
         Material.DARK_OAK_LOG,
         Material.MANGROVE_LOG,
         Material.CHERRY_LOG
      );
      this.plankCount = countAny(
         inv,
         Material.OAK_PLANKS,
         Material.BIRCH_PLANKS,
         Material.SPRUCE_PLANKS,
         Material.JUNGLE_PLANKS,
         Material.ACACIA_PLANKS,
         Material.DARK_OAK_PLANKS,
         Material.MANGROVE_PLANKS,
         Material.CHERRY_PLANKS,
         Material.BAMBOO_PLANKS
      );
      this.stickCount = countItem(inv, Material.STICK);
      this.stoneCount = countItem(inv, Material.COBBLESTONE);
      this.coalCount = countItem(inv, Material.COAL, Material.CHARCOAL);
      this.ironCount = countItem(inv, Material.RAW_IRON, Material.IRON_INGOT);
      this.hasWoodenPickaxe = hasItem(inv, Material.WOODEN_PICKAXE);
      this.hasStonePickaxe = hasItem(inv, Material.STONE_PICKAXE);
      this.hasIronPickaxe = hasItem(inv, Material.IRON_PICKAXE);
      long time = bot.getWorld().getTime();
      this.isNight = time >= 13000L && time <= 23000L;
      this.healthPercent = bot.getHealth() / bot.getMaxHealth();
      this.foodLevel = bot.getFoodLevel();
      this.nearestThreat = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : bot.getNearbyEntities(10.0, 4.0, 10.0)) {
         String t = e.getType().name();
         if (t.contains("ZOMBIE")
            || t.contains("SKELETON")
            || t.contains("CREEPER")
            || t.contains("SPIDER")
            || t.contains("WITCH")
            || t.contains("PHANTOM")
            || t.contains("ENDERMAN")) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               this.nearestThreat = e;
            }
         }
      }
   }

   private static int countItem(PlayerInventory inv, Material... types) {
      int c = 0;

      for (ItemStack item : inv.getContents()) {
         if (item != null) {
            for (Material m : types) {
               if (item.getType() == m) {
                  c += item.getAmount();
               }
            }
         }
      }

      return c;
   }

   private static int countAny(PlayerInventory inv, Material... types) {
      return countItem(inv, types);
   }

   private static boolean hasItem(PlayerInventory inv, Material type) {
      return countItem(inv, type) > 0;
   }

   private static boolean hasAny(PlayerInventory inv, Material... types) {
      return countItem(inv, types) > 0;
   }

   private static Block findNearestBlock(Player bot, Material[] types, int range) {
      Location loc = bot.getLocation();
      World world = loc.getWorld();
      int cx = loc.getBlockX();
      int cy = loc.getBlockY();
      int cz = loc.getBlockZ();
      Set<Material> targetSet = new HashSet<>();

      for (Material m : types) {
         targetSet.add(m);
      }

      Block best = null;
      double bestDist = Double.MAX_VALUE;

      for (int y = Math.max(cy - 5, world.getMinHeight()); y <= Math.min(cy + 5, world.getMaxHeight()); y++) {
         for (int layer = 0; layer <= range; layer++) {
            for (int x = -layer; x <= layer; x++) {
               for (int z = -layer; z <= layer; z++) {
                  if (Math.abs(x) == layer || Math.abs(z) == layer) {
                     Block b = world.getBlockAt(cx + x, y, cz + z);
                     if (targetSet.contains(b.getType())) {
                        double d = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc);
                        if (d < bestDist) {
                           bestDist = d;
                           best = b;
                        }
                     }
                  }
               }
            }

            if (best != null && bestDist <= (double)(layer * layer + 25)) {
               return best;
            }
         }
      }

      if (best != null) {
         return best;
      } else {
         int yMin = Math.max(cy - range, world.getMinHeight());
         int yMax = Math.min(cy + range, world.getMaxHeight());

         for (int layer = 0; layer <= range; layer++) {
            for (int x = -layer; x <= layer; x++) {
               for (int zx = -layer; zx <= layer; zx++) {
                  if (Math.abs(x) == layer || Math.abs(zx) == layer) {
                     for (int y = yMin; y <= yMax; y++) {
                        Block b = world.getBlockAt(cx + x, y, cz + zx);
                        if (targetSet.contains(b.getType())) {
                           double d = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc);
                           if (d < bestDist) {
                              bestDist = d;
                              best = b;
                           }
                        }
                     }
                  }
               }
            }

            if (best != null) {
               return best;
            }
         }

         return best;
      }
   }

   private static Block findNearestTree(Player bot, int range) {
      try {
         if (JonaSMP_AI.getInstance() != null && JonaSMP_AI.getInstance().getChunkRadar() != null) {
            Location found = JonaSMP_AI.getInstance().getChunkRadar().findNearestTree(bot, range);
            if (found != null) {
               Block b = found.getBlock();
               if (hasLeavesNearby(b, 5)) {
                  return b;
               }
            }
         }
      } catch (Exception var19) {
      }

      Location loc = bot.getLocation();
      World world = loc.getWorld();
      int cx = loc.getBlockX();
      int cy = loc.getBlockY();
      int cz = loc.getBlockZ();
      Block best = null;
      double bestDist = Double.MAX_VALUE;
      int yMin = Math.max(cy - 10, world.getMinHeight());
      int yMax = Math.min(cy + 20, world.getMaxHeight());

      for (int layer = 0; layer <= range; layer++) {
         for (int x = -layer; x <= layer; x++) {
            for (int z = -layer; z <= layer; z++) {
               if (Math.abs(x) == layer || Math.abs(z) == layer) {
                  for (int y = yMin; y <= yMax; y++) {
                     Block b = world.getBlockAt(cx + x, y, cz + z);
                     if (isLog(b.getType())) {
                        double d = b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc);
                        if (d < bestDist) {
                           bestDist = d;
                           best = b;
                        }
                     }
                  }
               }
            }
         }

         if (best != null && bestDist <= (double)(layer * layer + 100) && hasLeavesNearby(best, 5)) {
            return best;
         }
      }

      return best != null && hasLeavesNearby(best, 5) ? best : null;
   }

   private static boolean isLog(Material type) {
      return type == Material.OAK_LOG
         || type == Material.BIRCH_LOG
         || type == Material.SPRUCE_LOG
         || type == Material.JUNGLE_LOG
         || type == Material.ACACIA_LOG
         || type == Material.DARK_OAK_LOG
         || type == Material.MANGROVE_LOG
         || type == Material.CHERRY_LOG;
   }

   private static boolean hasLeavesNearby(Block log, int radius) {
      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               Block b = log.getWorld().getBlockAt(log.getX() + x, log.getY() + y, log.getZ() + z);
               if (b.getType().name().contains("LEAVES")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static Entity findNearestAnimal(Player bot, double range) {
      Entity best = null;
      double bestDist = Double.MAX_VALUE;

      for (Entity e : bot.getNearbyEntities(range, range / 2.0, range)) {
         String t = e.getType().name();
         if (t.contains("COW") || t.contains("PIG") || t.contains("SHEEP") || t.contains("CHICKEN") || t.contains("RABBIT")) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= bestDist)) {
               bestDist = d;
               best = e;
            }
         }
      }

      return best;
   }

   private static boolean isCliffAhead(Player bot, double distance) {
      Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(distance));
      Block ground = ahead.clone().subtract(0.0, 1.0, 0.0).getBlock();
      Block ground2 = ahead.clone().subtract(0.0, 2.0, 0.0).getBlock();
      Block air = ahead.getBlock();
      if (air.getType() == Material.AIR && ground.getType() == Material.AIR) {
         Block ground3 = ahead.clone().subtract(0.0, 3.0, 0.0).getBlock();
         if (ground2.getType() == Material.AIR && ground3.getType() == Material.AIR) {
            return true;
         }
      }

      return false;
   }

   private static boolean isLavaAhead(Player bot, double distance) {
      Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(distance));
      Block ground = ahead.clone().subtract(0.0, 1.0, 0.0).getBlock();
      Block air = ahead.getBlock();
      return ground.getType() == Material.LAVA || air.getType() == Material.LAVA;
   }

   private static void walkTo(Player bot, NMSBot nmsBot, Location target, double speed) {
      Location botLoc = bot.getLocation();
      double dx = target.getX() - botLoc.getX();
      double dz = target.getZ() - botLoc.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 1.5) {
         nmsBot.cancelNavigation();
         botPaths.remove(bot.getUniqueId());
      } else if (isLavaAhead(bot, 1.5)) {
         botPaths.remove(bot.getUniqueId());
         float yaw = botLoc.getYaw();
         double rad = Math.toRadians((double)yaw + 90.0);
         double sideX = -Math.sin(rad) * 2.0;
         double sideZ = Math.cos(rad) * 2.0;
         Location detour = botLoc.clone().add(sideX, 0.0, sideZ);
         List<Location> detourPath = new ArrayList<>();
         detourPath.add(detour);
         botPaths.put(bot.getUniqueId(), detourPath);
         botPathTargets.put(bot.getUniqueId(), detour.clone());
         botPathComputeTime.put(bot.getUniqueId(), System.currentTimeMillis());
         nmsBot.setRotation((float)Math.toDegrees(Math.atan2(-sideX, sideZ)), botLoc.getPitch());
         nmsBot.walkRelative(speed, 0.0);
         CoreBootstrap.PLUGIN.getLogger().warning("[GoalPlanner] Lava ahead! Detouring.");
      } else if (isCliffAhead(bot, 1.5)) {
         botPaths.remove(bot.getUniqueId());
         float yaw = botLoc.getYaw();
         double rad = Math.toRadians((double)yaw + 90.0);
         double sideX = -Math.sin(rad) * 3.0;
         double sideZ = Math.cos(rad) * 3.0;
         Location detour = botLoc.clone().add(sideX, 0.0, sideZ);
         detour.setY((double)(botLoc.getWorld().getHighestBlockYAt(detour) + 1));
         List<Location> detourPath = new ArrayList<>();
         detourPath.add(detour);
         botPaths.put(bot.getUniqueId(), detourPath);
         botPathTargets.put(bot.getUniqueId(), detour.clone());
         botPathComputeTime.put(bot.getUniqueId(), System.currentTimeMillis());
         nmsBot.setRotation((float)Math.toDegrees(Math.atan2(-sideX, sideZ)), botLoc.getPitch());
         nmsBot.walkRelative(speed, 0.0);
         CoreBootstrap.PLUGIN
            .getLogger()
            .info("[GoalPlanner] Cliff ahead — detouring to " + detour.getBlockX() + "," + detour.getBlockY() + "," + detour.getBlockZ());
      } else {
         UUID botId = bot.getUniqueId();
         List<Location> path = botPaths.get(botId);
         Location cachedTarget = botPathTargets.get(botId);
         long lastCompute = botPathComputeTime.getOrDefault(botId, 0L);
         long now = System.currentTimeMillis();
         boolean needRecompute = path == null || path.isEmpty() || cachedTarget == null || cachedTarget.distance(target) > 2.0 || now - lastCompute > 2000L;
         if (!needRecompute && path != null && !path.isEmpty()) {
            double minDistToPath = Double.MAX_VALUE;

            for (Location waypoint : path) {
               double d = botLoc.distance(waypoint);
               if (d < minDistToPath) {
                  minDistToPath = d;
               }
            }

            if (minDistToPath > 3.0) {
               needRecompute = true;
            }
         }

         if (needRecompute) {
            path = BotPathfinder.findPath(botLoc, target);
            botPaths.put(botId, path);
            botPathTargets.put(botId, target.clone());
            botPathComputeTime.put(botId, now);
            if (!path.isEmpty()) {
               CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] A* path computed: " + path.size() + " waypoints to target.");
            } else {
               CoreBootstrap.PLUGIN.getLogger().fine("[GoalPlanner] A* path failed, falling back to direct.");
            }
         }

         if (path != null && !path.isEmpty()) {
            while (!path.isEmpty() && botLoc.distance(path.get(0)) < 1.2) {
               path.remove(0);
            }

            if (!path.isEmpty()) {
               Location nextWaypoint = path.get(0);
               double wdx = nextWaypoint.getX() - botLoc.getX();
               double wdz = nextWaypoint.getZ() - botLoc.getZ();
               float yaw = (float)Math.toDegrees(Math.atan2(-wdx, wdz));
               nmsBot.setRotation(yaw, 5.0F);
               double wdist = Math.sqrt(wdx * wdx + wdz * wdz);
               if (wdist > 0.01) {
                  nmsBot.walkRelative(wdx / wdist * speed * 2.0, wdz / wdist * speed * 2.0);
               }

               if (nextWaypoint.getY() > botLoc.getY() && bot.isOnGround()) {
                  nmsBot.jump();
               }

               return;
            }
         }

         float yaw2 = (float)Math.toDegrees(Math.atan2(-dx, dz));
         nmsBot.setRotation(yaw2, 5.0F);
         if (!nmsBot.isNavigating()) {
            nmsBot.navigateTo(target);
         }
      }
   }

   private static void pickupNearbyItems(Player bot) {
      for (Entity e : bot.getNearbyEntities(4.0, 3.0, 4.0)) {
         if (e instanceof Item) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= 2.5)) {
               HashMap<Integer, ItemStack> leftover = bot.getInventory().addItem(new ItemStack[]{((Item)e).getItemStack()});
               if (leftover.isEmpty()) {
                  e.remove();
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Picked up " + ((Item)e).getItemStack().getType());
               }
            }
         }
      }
   }

   private boolean mineBlockTick(NMSBot nmsBot, Block block, Player bot) {
      if (this.currentMiningBlock == null || !this.currentMiningBlock.equals(block)) {
         this.currentMiningBlock = block;
         int ticksNeeded = nmsBot.startMining(block);
         if (ticksNeeded == Integer.MAX_VALUE) {
            this.currentMiningBlock = null;
            nmsBot.cancelMining();
            return false;
         }
      }

      nmsBot.lookAt(block.getLocation().add(0.5, 0.5, 0.5));
      nmsBot.tickMining();
      if (nmsBot.isMining() && nmsBot.getMiningProgress() >= nmsBot.getMiningRequired()) {
         nmsBot.breakBlock(block);
         this.currentMiningBlock = null;
         pickupNearbyItems(bot);
         return false;
      } else {
         return true;
      }
   }

   private static double normalizeAngle(double angle) {
      while (angle > 180.0) {
         angle -= 360.0;
      }

      while (angle < -180.0) {
         angle += 360.0;
      }

      return angle;
   }

   private static void wander(Player bot, NMSBot nmsBot) {
      Biome biome = bot.getLocation().getBlock().getBiome();
      String biomeName = biome.toString().toUpperCase();
      double angle;
      if (!biomeName.contains("DESERT") && !biomeName.contains("OCEAN") && !biomeName.contains("BEACH")) {
         angle = (double)((int)(Math.random() * 4.0)) * 1.5708 + (Math.random() - 0.5) * 0.5;
      } else {
         angle = Math.random() > 0.5 ? 0.785 : 2.356;
      }

      double speed = 0.18 + Math.random() * 0.12;
      nmsBot.walkRelative(Math.cos(angle) * speed * 2.0, Math.sin(angle) * speed * 2.0);
      if (bot.isOnGround() && Math.random() < 0.15) {
         nmsBot.jump();
      }
   }

   private void buildGatherWood() {
      this.goalSteps.add(new BotGoalPlanner.Step("GW1", "Find and chop nearest tree", (bot, nmsBot, p) -> {
         Block tree = findNearestTree(bot, 16);
         if (tree != null) {
            double dist = bot.getLocation().distance(tree.getLocation().add(0.5, 0.5, 0.5));
            if (dist > 3.5) {
               walkTo(bot, nmsBot, tree.getLocation().add(0.5, 0.0, 0.5), 0.28);
               return true;
            } else {
               nmsBot.lookAt(tree.getLocation().add(0.5, 0.5, 0.5));
               return this.mineBlockTick(nmsBot, tree, bot);
            }
         } else {
            Biome biome = bot.getLocation().getBlock().getBiome();
            String biomeName = biome.toString().toUpperCase();
            if (!biomeName.contains("DESERT") && !biomeName.contains("OCEAN") && !biomeName.contains("BEACH")) {
               wander(bot, nmsBot);
            } else {
               (p.targetLocation = bot.getLocation().add(20.0, 0.0, 20.0)).setY(bot.getLocation().getY());
               walkTo(bot, nmsBot, p.targetLocation, 0.25);
            }

            return true;
         }
      }, (bot, p) -> p.woodCount + p.plankCount / 4 + p.stickCount / 8 >= 4, (bot, p, r) -> null));
   }

   private void buildGatherStone() {
      if (!this.hasWoodenPickaxe) {
         this.goalSteps.add(new BotGoalPlanner.Step("GS0", "Get pickaxe first", (bot, nmsBot, p) -> {
            p.setGoal(BotGoalPlanner.GoalType.CRAFT_BASIC_TOOLS);
            return false;
         }, (bot, p) -> p.hasWoodenPickaxe, (bot, p, r) -> null));
      }

      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "GS1",
               "Find stone",
               (bot, nmsBot, p) -> {
                  Block stone = findNearestBlock(bot, new Material[]{Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE}, 16);
                  if (stone == null) {
                     wander(bot, nmsBot);
                     return true;
                  } else {
                     walkTo(bot, nmsBot, p.targetLocation = stone.getLocation().add(0.5, 0.0, 0.5), 0.28);
                     return true;
                  }
               },
               (bot, p) -> findNearestBlock(bot, new Material[]{Material.STONE}, 4) != null,
               (bot, p, r) -> new BotGoalPlanner.Step(
                     "GS1-F",
                     "Dig down",
                     (b, nms, pl) -> {
                        Block below = b.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                        return below.getType() != Material.DIRT && below.getType() != Material.GRASS_BLOCK && below.getType() != Material.SAND
                           || this.mineBlockTick(nms, below, b);
                     },
                     (b, pl) -> b.getLocation().subtract(0.0, 1.0, 0.0).getBlock().getType() == Material.STONE,
                     (b, pl, rr) -> null
                  )
            )
         );
      this.goalSteps.add(new BotGoalPlanner.Step("GS2", "Mine 20 stone", (bot, nmsBot, p) -> {
         Block stone2 = findNearestBlock(bot, new Material[]{Material.STONE}, 4);
         if (stone2 == null) {
            return false;
         } else {
            nmsBot.lookAt(stone2.getLocation().add(0.5, 0.5, 0.5));
            return this.mineBlockTick(nmsBot, stone2, bot);
         }
      }, (bot, p) -> p.stoneCount >= 20, (bot, p, r) -> null));
   }

   private void buildCraftTools() {
      this.goalSteps.add(new BotGoalPlanner.Step("CT0", "Get 4 logs if needed", (bot, nmsBot, p) -> {
         if (p.woodCount + p.plankCount / 4 >= 4) {
            return true;
         } else {
            p.setGoal(BotGoalPlanner.GoalType.GATHER_WOOD);
            return false;
         }
      }, (bot, p) -> p.woodCount + p.plankCount / 4 >= 4, (bot, p, r) -> null));
      this.goalSteps
         .add(new BotGoalPlanner.Step("CT1", "Wait for planks to be crafted", (bot, nmsBot, p) -> true, (bot, p) -> p.plankCount >= 4, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("CT2", "Wait for sticks", (bot, nmsBot, p) -> true, (bot, p) -> p.stickCount >= 2, (bot, p, r) -> null));
      this.goalSteps
         .add(new BotGoalPlanner.Step("CT3", "Wait for wooden pickaxe", (bot, nmsBot, p) -> true, (bot, p) -> p.hasWoodenPickaxe, (bot, p, r) -> null));
   }

   private void buildMineOres() {
      if (!this.hasWoodenPickaxe && !this.hasStonePickaxe) {
         this.goalSteps.add(new BotGoalPlanner.Step("MO0", "Get pickaxe", (bot, nmsBot, p) -> {
            p.setGoal(BotGoalPlanner.GoalType.CRAFT_BASIC_TOOLS);
            return false;
         }, (bot, p) -> p.hasWoodenPickaxe, (bot, p, r) -> null));
      }

      this.goalSteps.add(new BotGoalPlanner.Step("MO1", "Find coal/iron ore", (bot, nmsBot, p) -> {
         Block ore = findNearestBlock(bot, new Material[]{Material.COAL_ORE, Material.IRON_ORE}, 16);
         if (ore == null) {
            wander(bot, nmsBot);
            return true;
         } else {
            walkTo(bot, nmsBot, p.targetLocation = ore.getLocation().add(0.5, 0.0, 0.5), 0.28);
            return true;
         }
      }, (bot, p) -> findNearestBlock(bot, new Material[]{Material.COAL_ORE, Material.IRON_ORE}, 4) != null, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("MO2", "Mine ore", (bot, nmsBot, p) -> {
         Block ore2 = findNearestBlock(bot, new Material[]{Material.COAL_ORE, Material.IRON_ORE}, 4);
         if (ore2 == null) {
            return false;
         } else {
            nmsBot.lookAt(ore2.getLocation().add(0.5, 0.5, 0.5));
            return this.mineBlockTick(nmsBot, ore2, bot);
         }
      }, (bot, p) -> p.coalCount >= 3 || p.ironCount >= 2, (bot, p, r) -> null));
   }

   private void buildDigDiamonds() {
      if (!this.hasStonePickaxe) {
         this.goalSteps.add(new BotGoalPlanner.Step("DD0", "Get stone pickaxe", (bot, nmsBot, p) -> {
            if (!p.hasWoodenPickaxe) {
               p.setGoal(BotGoalPlanner.GoalType.CRAFT_BASIC_TOOLS);
            } else {
               p.setGoal(BotGoalPlanner.GoalType.GATHER_STONE);
            }

            return false;
         }, (bot, p) -> p.hasStonePickaxe, (bot, p, r) -> null));
      }

      this.goalSteps.add(new BotGoalPlanner.Step("DD1", "Dig stair down to Y=12", (bot, nmsBot, p) -> {
         if (bot.getLocation().getY() <= 14.0) {
            return true;
         } else {
            Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
            if (below.getType().isSolid() && below.getType() != Material.BEDROCK && !this.mineBlockTick(nmsBot, below, bot)) {
            }

            nmsBot.walkRelative(0.3, 0.0);
            return true;
         }
      }, (bot, p) -> bot.getLocation().getY() <= 14.0, (bot, p, r) -> new BotGoalPlanner.Step("DD1-F", "Place torch", (b, nms, pl) -> {
            if (b.getLocation().getBlock().getLightLevel() < 7) {
               for (int i = 0; i < 9; i++) {
                  ItemStack item = b.getInventory().getItem(i);
                  if (item != null && item.getType() == Material.TORCH) {
                     nms.selectHotbarSlot(i);
                     nms.placeBlock(b.getLocation().subtract(0.0, 1.0, 0.0).getBlock());
                     break;
                  }
               }
            }

            return true;
         }, (b, pl) -> b.getLocation().getBlock().getLightLevel() >= 7, (b, pl, rr) -> null)));
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "DD2",
               "Strip mine",
               (bot, nmsBot, p) -> {
                  Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(1));
                  Block front = ahead.getBlock();
                  if (front.getType().isSolid() && front.getType() != Material.BEDROCK && !this.mineBlockTick(nmsBot, front, bot)) {
                  }

                  nmsBot.walkRelative(0.25, 0.0);
                  Block diamond = findNearestBlock(bot, new Material[]{Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE}, 5);
                  if (diamond != null) {
                     p.targetLocation = diamond.getLocation();
                  }

                  return true;
               },
               (bot, p) -> countItem(bot.getInventory(), Material.DIAMOND) > 0 ? false : findNearestBlock(bot, new Material[]{Material.DIAMOND_ORE}, 3) == null,
               (bot, p, r) -> null
            )
         );
      this.goalSteps.add(new BotGoalPlanner.Step("DD3", "Mine diamonds", (bot, nmsBot, p) -> {
         Block d = findNearestBlock(bot, new Material[]{Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE}, 5);
         if (d == null) {
            return false;
         } else {
            nmsBot.lookAt(d.getLocation().add(0.5, 0.5, 0.5));
            return this.mineBlockTick(nmsBot, d, bot);
         }
      }, (bot, p) -> countItem(bot.getInventory(), Material.DIAMOND) >= 3, (bot, p, r) -> null));
   }

   private void buildShelter() {
      this.goalSteps.add(new BotGoalPlanner.Step("BS0", "Gather wood+stone", (bot, nmsBot, p) -> {
         if (p.woodCount < 8) {
            p.setGoal(BotGoalPlanner.GoalType.GATHER_WOOD);
            return false;
         } else if (p.stoneCount < 16) {
            p.setGoal(BotGoalPlanner.GoalType.GATHER_STONE);
            return false;
         } else {
            return true;
         }
      }, (bot, p) -> p.woodCount >= 8 && p.stoneCount >= 16, (bot, p, r) -> null));
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "BS1",
               "Build 5x5 floor",
               (bot, nmsBot, p) -> {
                  Location base = p.targetLocation != null ? p.targetLocation : bot.getLocation();

                  for (int dx = -2; dx <= 2; dx++) {
                     for (int dz = -2; dz <= 2; dz++) {
                        Block b = base.clone().add((double)dx, -1.0, (double)dz).getBlock();
                        if (b.getType() == Material.AIR || b.getType() == Material.SHORT_GRASS || b.getType() == Material.TALL_GRASS) {
                           for (int i = 0; i < 9; i++) {
                              ItemStack item = bot.getInventory().getItem(i);
                              if (item != null
                                 && (item.getType() == Material.COBBLESTONE || item.getType() == Material.STONE || item.getType() == Material.OAK_PLANKS)) {
                                 nmsBot.selectHotbarSlot(i);
                                 nmsBot.placeBlock(b);
                                 return true;
                              }
                           }
                        }
                     }
                  }

                  return true;
               },
               (bot, p) -> {
                  Location base2 = p.targetLocation != null ? p.targetLocation : bot.getLocation();
                  int missing = 0;

                  for (int dx2 = -2; dx2 <= 2; dx2++) {
                     for (int dz2 = -2; dz2 <= 2; dz2++) {
                        Block b2 = base2.clone().add((double)dx2, -1.0, (double)dz2).getBlock();
                        if (b2.getType() == Material.AIR || b2.getType() == Material.SHORT_GRASS || b2.getType() == Material.GRASS_BLOCK) {
                           missing++;
                        }
                     }
                  }

                  return missing <= 2;
               },
               (bot, p, r) -> null
            )
         );
      this.goalSteps.add(new BotGoalPlanner.Step("BS2", "Build walls", (bot, nmsBot, p) -> {
         Location base3 = p.targetLocation != null ? p.targetLocation : bot.getLocation();

         for (int dy = 0; dy < 3; dy++) {
            for (int dx3 = -2; dx3 <= 2; dx3++) {
               for (int dz3 = -2; dz3 <= 2; dz3++) {
                  if ((Math.abs(dx3) >= 2 || Math.abs(dz3) >= 2) && (Math.abs(dx3) != 2 || Math.abs(dz3) != 2) && (dx3 != 0 || dz3 != 2 || dy != 0)) {
                     Block b3 = base3.clone().add((double)dx3, (double)dy, (double)dz3).getBlock();
                     if (b3.getType() == Material.AIR) {
                        for (int j = 0; j < 9; j++) {
                           ItemStack item2 = bot.getInventory().getItem(j);
                           if (item2 != null && item2.getType().isBlock()) {
                              nmsBot.selectHotbarSlot(j);
                              nmsBot.placeBlock(b3);
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }

         return true;
      }, (bot, p) -> p.stoneCount < 4, (bot, p, r) -> null));
   }

   private void buildFindFood() {
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "FF1",
               "Find food source",
               (bot, nmsBot, p) -> {
                  Entity animal = findNearestAnimal(bot, 16.0);
                  if (animal != null) {
                     p.targetLocation = animal.getLocation();
                     walkTo(bot, nmsBot, animal.getLocation(), 0.3);
                     return true;
                  } else {
                     Block berries = findNearestBlock(bot, new Material[]{Material.SWEET_BERRY_BUSH}, 16);
                     if (berries != null) {
                        walkTo(bot, nmsBot, berries.getLocation(), 0.25);
                        return true;
                     } else {
                        Block crops = findNearestBlock(bot, new Material[]{Material.WHEAT, Material.POTATOES, Material.CARROTS, Material.BEETROOTS}, 16);
                        if (crops != null) {
                           walkTo(bot, nmsBot, crops.getLocation(), 0.25);
                           return true;
                        } else {
                           for (int dx = -5; dx <= 5; dx++) {
                              for (int dz = -5; dz <= 5; dz++) {
                                 Block water = bot.getLocation().add((double)dx, -1.0, (double)dz).getBlock();
                                 if (water.getType() == Material.WATER) {
                                    walkTo(bot, nmsBot, water.getLocation(), 0.2);
                                    return true;
                                 }
                              }
                           }

                           wander(bot, nmsBot);
                           return true;
                        }
                     }
                  }
               },
               (bot, p) -> {
                  Entity a = findNearestAnimal(bot, 3.0);
                  return a != null && bot.getLocation().distance(a.getLocation()) < 2.5
                     ? false
                     : findNearestBlock(bot, new Material[]{Material.SWEET_BERRY_BUSH, Material.WHEAT, Material.POTATOES, Material.CARROTS}, 2) == null;
               },
               (bot, p, r) -> new BotGoalPlanner.Step("FF1-F", "Search wider for food", (b, nms, pl) -> {
                     Biome biome = b.getLocation().getBlock().getBiome();
                     if (biome.toString().toUpperCase().contains("DESERT")) {
                        Entity rabbit = findNearestAnimal(b, 16.0);
                        if (rabbit != null) {
                           walkTo(b, nms, rabbit.getLocation(), 0.3);
                        }
                     } else {
                        wander(b, nms);
                     }

                     return true;
                  }, (b, pl) -> b.getFoodLevel() >= 15, (b, pl, rr) -> null)
            )
         );
      this.goalSteps.add(new BotGoalPlanner.Step("FF2", "Harvest food", (bot, nmsBot, p) -> {
         Entity a2 = findNearestAnimal(bot, 3.0);
         if (a2 != null) {
            nmsBot.selectHotbarSlot(0);
            nmsBot.swingMainHand();
            bot.attack(a2);
            return true;
         } else {
            Block berry = findNearestBlock(bot, new Material[]{Material.SWEET_BERRY_BUSH}, 2);
            if (berry != null) {
               return this.mineBlockTick(nmsBot, berry, bot);
            } else {
               Block crop = findNearestBlock(bot, new Material[]{Material.WHEAT, Material.POTATOES, Material.CARROTS, Material.BEETROOTS}, 2);
               return crop != null ? this.mineBlockTick(nmsBot, crop, bot) : false;
            }
         }
      }, (bot, p) -> bot.getFoodLevel() >= 18, (bot, p, r) -> null));
   }

   private void buildExplore() {
      this.goalSteps.add(new BotGoalPlanner.Step("EX1", "Walk random direction", (bot, nmsBot, p) -> {
         if (p.targetLocation == null || bot.getLocation().distance(p.targetLocation) < 2.0) {
            double angle = Math.random() * 2.0 * Math.PI;
            (p.targetLocation = bot.getLocation().add(Math.cos(angle) * 40.0, 0.0, Math.sin(angle) * 40.0)).setY(bot.getLocation().getY());
         }

         walkTo(bot, nmsBot, p.targetLocation, 0.25);
         return true;
      }, (bot, p) -> false, (bot, p, r) -> null));
   }

   private void buildFlee() {
      this.goalSteps.add(new BotGoalPlanner.Step("FL1", "Run from threat", (bot, nmsBot, p) -> {
         if (p.nearestThreat == null) {
            return true;
         } else {
            double adx = bot.getLocation().getX() - p.nearestThreat.getLocation().getX();
            double adz = bot.getLocation().getZ() - p.nearestThreat.getLocation().getZ();
            double dist = Math.sqrt(adx * adx + adz * adz);
            double adx2;
            double adz2;
            if (dist < 0.01) {
               adx2 = 1.0;
               adz2 = 0.0;
            } else {
               adx2 = adx / dist;
               adz2 = adz / dist;
            }

            Location target = bot.getLocation().add(adx2 * 10.0, 0.0, adz2 * 10.0);
            target.setY(bot.getLocation().getY());
            walkTo(bot, nmsBot, target, 0.5);
            bot.setSprinting(true);
            return true;
         }
      }, (bot, p) -> p.nearestThreat == null || bot.getLocation().distance(p.nearestThreat.getLocation()) > 15.0, (bot, p, r) -> null));
   }

   private void buildSmeltOres() {
      this.goalSteps.add(new BotGoalPlanner.Step("SO0", "Find or place furnace", (bot, nmsBot, p) -> {
         Block furnace = findNearestBlock(bot, new Material[]{Material.FURNACE, Material.BLAST_FURNACE}, 32);
         if (furnace != null) {
            p.targetLocation = furnace.getLocation();
            if (bot.getLocation().distance(furnace.getLocation()) > 2.5) {
               walkTo(bot, nmsBot, furnace.getLocation().add(0.5, 0.0, 0.5), 0.3);
               return true;
            } else {
               return true;
            }
         } else {
            for (int i = 0; i < 9; i++) {
               ItemStack item = bot.getInventory().getItem(i);
               if (item != null && item.getType() == Material.FURNACE) {
                  nmsBot.selectHotbarSlot(i);
                  Block placeAt = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                  if (placeAt.getType().isSolid()) {
                     nmsBot.placeBlock(bot.getLocation().getBlock());
                     return true;
                  }
               }
            }

            return false;
         }
      }, (bot, p) -> {
         Block furnace = findNearestBlock(bot, new Material[]{Material.FURNACE, Material.BLAST_FURNACE}, 3);
         return furnace != null;
      }, (bot, p, r) -> null));
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "SO1",
               "Smelt ores",
               (bot, nmsBot, p) -> {
                  Block furnace = findNearestBlock(bot, new Material[]{Material.FURNACE, Material.BLAST_FURNACE}, 3);
                  return furnace != null;
               },
               (bot, p) -> countItem(bot.getInventory(), Material.IRON_INGOT) > 0
                     || countItem(bot.getInventory(), Material.GOLD_INGOT) > 0
                     || countItem(bot.getInventory(), Material.RAW_IRON) == 0,
               (bot, p, r) -> null
            )
         );
   }

   private void buildDeepMine() {
      if (!this.hasIronPickaxe && !this.hasStonePickaxe) {
         this.goalSteps.add(new BotGoalPlanner.Step("DM0", "Get pickaxe first", (bot, nmsBot, p) -> {
            p.setGoal(BotGoalPlanner.GoalType.CRAFT_BASIC_TOOLS);
            return false;
         }, (bot, p) -> p.hasStonePickaxe || p.hasIronPickaxe, (bot, p, r) -> null));
      }

      this.goalSteps.add(new BotGoalPlanner.Step("DM1", "Dig stair down to diamond depth", (bot, nmsBot, p) -> {
         if (bot.getLocation().getY() <= -54.0) {
            return true;
         } else {
            Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
            if (below.getType() != Material.LAVA && below.getType() != Material.CAULDRON) {
               if (below.getType().isSolid() && below.getType() != Material.BEDROCK && !this.mineBlockTick(nmsBot, below, bot)) {
               }

               nmsBot.walkRelative(0.3, 0.0);
               return true;
            } else {
               nmsBot.walkRelative(0.0, 0.35);
               CoreBootstrap.PLUGIN.getLogger().warning("[GoalPlanner] Lava below! Sidestepping.");
               return true;
            }
         }
      }, (bot, p) -> bot.getLocation().getY() <= -54.0, (bot, p, r) -> null));
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "DM2",
               "Strip mine at Y=-54",
               (bot, nmsBot, p) -> {
                  Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(1));
                  Block front = ahead.getBlock();
                  if (front.getType().isSolid() && front.getType() != Material.BEDROCK && !this.mineBlockTick(nmsBot, front, bot)) {
                  }

                  nmsBot.walkRelative(0.25, 0.0);
                  Block ore = findNearestBlock(
                     bot,
                     new Material[]{
                        Material.DIAMOND_ORE,
                        Material.DEEPSLATE_DIAMOND_ORE,
                        Material.REDSTONE_ORE,
                        Material.DEEPSLATE_REDSTONE_ORE,
                        Material.LAPIS_ORE,
                        Material.DEEPSLATE_LAPIS_ORE,
                        Material.GOLD_ORE,
                        Material.DEEPSLATE_GOLD_ORE
                     },
                     5
                  );
                  if (ore != null) {
                     p.targetLocation = ore.getLocation();
                  }

                  return true;
               },
               (bot, p) -> countItem(bot.getInventory(), Material.DIAMOND) >= 5 || countItem(bot.getInventory(), Material.REDSTONE) >= 16,
               (bot, p, r) -> null
            )
         );
   }

   private void buildCookFood() {
      this.goalSteps.add(new BotGoalPlanner.Step("CF0", "Find cooking station", (bot, nmsBot, p) -> {
         Block furnace = findNearestBlock(bot, new Material[]{Material.FURNACE, Material.BLAST_FURNACE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE}, 24);
         if (furnace != null) {
            if (bot.getLocation().distance(furnace.getLocation()) > 2.5) {
               walkTo(bot, nmsBot, furnace.getLocation().add(0.5, 0.0, 0.5), 0.3);
               return true;
            } else {
               return true;
            }
         } else {
            return false;
         }
      }, (bot, p) -> {
         Block furnace = findNearestBlock(bot, new Material[]{Material.FURNACE, Material.BLAST_FURNACE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE}, 3);
         return furnace != null;
      }, (bot, p, r) -> null));
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "CF1",
               "Cook raw food",
               (bot, nmsBot, p) -> true,
               (bot, p) -> {
                  boolean hasCooked = countItem(bot.getInventory(), Material.COOKED_BEEF) > 0
                     || countItem(bot.getInventory(), Material.COOKED_PORKCHOP) > 0
                     || countItem(bot.getInventory(), Material.COOKED_CHICKEN) > 0
                     || countItem(bot.getInventory(), Material.COOKED_COD) > 0
                     || countItem(bot.getInventory(), Material.COOKED_SALMON) > 0;
                  boolean hasRaw = countItem(bot.getInventory(), Material.BEEF) > 0
                     || countItem(bot.getInventory(), Material.PORKCHOP) > 0
                     || countItem(bot.getInventory(), Material.CHICKEN) > 0
                     || countItem(bot.getInventory(), Material.COD) > 0
                     || countItem(bot.getInventory(), Material.SALMON) > 0;
                  return hasCooked || !hasRaw;
               },
               (bot, p, r) -> null
            )
         );
   }

   private void buildBasicHouse() {
      this.goalSteps.add(new BotGoalPlanner.Step("BH0", "Gather building blocks", (bot, nmsBot, p) -> {
         int blockCount = 0;

         for (int i = 0; i < 9; i++) {
            ItemStack item = bot.getInventory().getItem(i);
            if (item != null && item.getType().isBlock()) {
               blockCount += item.getAmount();
            }
         }

         if (blockCount < 24) {
            p.setGoal(BotGoalPlanner.GoalType.GATHER_WOOD);
            return false;
         } else {
            return true;
         }
      }, (bot, p) -> true, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("BH1", "Build house walls", (bot, nmsBot, p) -> {
         Location base = bot.getLocation().clone();
         int placed = 0;

         for (int h = 0; h < 3; h++) {
            for (int x = 0; x < 4; x++) {
               for (int z = 0; z < 4; z++) {
                  if (x <= 0 || x >= 3 || z <= 0 || z >= 3) {
                     Block b = base.clone().add((double)x, (double)h, (double)z).getBlock();
                     if (b.getType() == Material.AIR) {
                        for (int i = 0; i < 9; i++) {
                           ItemStack item = bot.getInventory().getItem(i);
                           if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                              nmsBot.selectHotbarSlot(i);
                              nmsBot.placeBlock(b);
                              if (++placed >= 8) {
                                 return true;
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }

         CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] House walls placed: " + placed);
         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("BH2", "Build roof", (bot, nmsBot, p) -> {
         Location base = bot.getLocation().clone().add(0.0, 3.0, 0.0);
         int placed = 0;

         for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
               Block b = base.clone().add((double)x, 0.0, (double)z).getBlock();
               if (b.getType() == Material.AIR) {
                  for (int i = 0; i < 9; i++) {
                     ItemStack item = bot.getInventory().getItem(i);
                     if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                        nmsBot.selectHotbarSlot(i);
                        nmsBot.placeBlock(b);
                        if (++placed >= 8) {
                           return true;
                        }
                        break;
                     }
                  }
               }
            }
         }

         CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Roof placed: " + placed);
         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("BH3", "Place torch inside", (bot, nmsBot, p) -> {
         for (int i = 0; i < 9; i++) {
            ItemStack item = bot.getInventory().getItem(i);
            if (item != null && item.getType() == Material.TORCH) {
               nmsBot.selectHotbarSlot(i);
               nmsBot.placeBlock(bot.getLocation().add(1.0, 0.0, 1.0).getBlock());
               break;
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildBridgeGap() {
      this.goalSteps.add(new BotGoalPlanner.Step("BG0", "Place block ahead to bridge gap", (bot, nmsBot, p) -> {
         Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(1.2));
         Block ground = ahead.clone().subtract(0.0, 1.0, 0.0).getBlock();
         if (ground.getType() == Material.AIR) {
            for (int i = 0; i < 9; i++) {
               ItemStack item = bot.getInventory().getItem(i);
               if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                  nmsBot.selectHotbarSlot(i);
                  nmsBot.placeBlock(ground);
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Bridged gap ahead.");
                  break;
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildFillHole() {
      this.goalSteps.add(new BotGoalPlanner.Step("FH0", "Fill hole below with block", (bot, nmsBot, p) -> {
         Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
         if (below.getType() == Material.AIR || below.getType() == Material.CAVE_AIR || below.getType() == Material.VOID_AIR) {
            for (int i = 0; i < 9; i++) {
               ItemStack item = bot.getInventory().getItem(i);
               if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                  nmsBot.selectHotbarSlot(i);
                  nmsBot.placeBlock(below);
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Filled hole below.");
                  break;
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildPlaceTorches() {
      this.goalSteps.add(new BotGoalPlanner.Step("PT0", "Place torch if dark", (bot, nmsBot, p) -> {
         if (bot.getLocation().getBlock().getLightLevel() < 7) {
            for (int i = 0; i < 9; i++) {
               ItemStack item = bot.getInventory().getItem(i);
               if (item != null && item.getType() == Material.TORCH) {
                  nmsBot.selectHotbarSlot(i);
                  Block placeAt = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                  if (placeAt.getType().isSolid()) {
                     nmsBot.placeBlock(bot.getLocation().getBlock());
                     CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Placed torch (light was " + bot.getLocation().getBlock().getLightLevel() + ").");
                  }
                  break;
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildFarmCrops() {
      this.goalSteps.add(new BotGoalPlanner.Step("FC0", "Find farmland with crops", (bot, nmsBot, p) -> {
         Block crop = findNearestBlock(bot, new Material[]{Material.WHEAT, Material.POTATOES, Material.CARROTS, Material.BEETROOTS}, 16);
         if (crop != null) {
            p.targetLocation = crop.getLocation();
            walkTo(bot, nmsBot, crop.getLocation().add(0.5, 0.0, 0.5), 0.3);
            if (bot.getLocation().distance(crop.getLocation()) < 2.0) {
               Ageable ageable = (Ageable)crop.getBlockData();
               if (ageable.getAge() == ageable.getMaximumAge() && !this.mineBlockTick(nmsBot, crop, bot)) {
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Harvested crop at " + crop.getX() + "," + crop.getY() + "," + crop.getZ());
               }
            }

            return true;
         } else {
            return false;
         }
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildBreedAnimals() {
      this.goalSteps.add(new BotGoalPlanner.Step("BA0", "Find breedable animals", (bot, nmsBot, p) -> {
         for (Entity e : bot.getNearbyEntities(8.0, 4.0, 8.0)) {
            if (e instanceof Animals) {
               Animals animal = (Animals)e;
               if (animal.isAdult() && animal.getLoveModeTicks() <= 0) {
                  walkTo(bot, nmsBot, animal.getLocation(), 0.3);
                  if (bot.getLocation().distance(animal.getLocation()) < 2.5) {
                     Material[] foods = new Material[]{Material.WHEAT, Material.CARROT, Material.WHEAT_SEEDS};

                     for (int i = 0; i < 9; i++) {
                        ItemStack item = bot.getInventory().getItem(i);
                        if (item != null) {
                           for (Material food : foods) {
                              if (item.getType() == food) {
                                 nmsBot.selectHotbarSlot(i);
                                 nmsBot.swingMainHand();
                                 CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Fed animal: " + animal.getType().name());
                                 return true;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildShearSheep() {
      this.goalSteps.add(new BotGoalPlanner.Step("SS0", "Find sheep and shear", (bot, nmsBot, p) -> {
         for (Entity e : bot.getNearbyEntities(8.0, 4.0, 8.0)) {
            if (e instanceof Sheep) {
               Sheep sheep = (Sheep)e;
               if (!sheep.isSheared()) {
                  walkTo(bot, nmsBot, sheep.getLocation(), 0.3);
                  if (bot.getLocation().distance(sheep.getLocation()) < 2.0) {
                     for (int i = 0; i < 9; i++) {
                        ItemStack item = bot.getInventory().getItem(i);
                        if (item != null && item.getType() == Material.SHEARS) {
                           nmsBot.selectHotbarSlot(i);
                           nmsBot.swingMainHand();
                           CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Sheared sheep.");
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildTradeVillager() {
      this.goalSteps.add(new BotGoalPlanner.Step("TV0", "Find villager and trade", (bot, nmsBot, p) -> {
         for (Entity e : bot.getNearbyEntities(8.0, 4.0, 8.0)) {
            if (e instanceof Villager) {
               walkTo(bot, nmsBot, e.getLocation(), 0.3);
               if (bot.getLocation().distance(e.getLocation()) < 2.5) {
                  nmsBot.swingMainHand();
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Interacted with villager for trade.");
                  return true;
               }
            }
         }

         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildEnchantGear() {
      this.goalSteps.add(new BotGoalPlanner.Step("EG0", "Find enchanting table", (bot, nmsBot, p) -> {
         Block table = findNearestBlock(bot, new Material[]{Material.ENCHANTING_TABLE}, 16);
         if (table != null) {
            walkTo(bot, nmsBot, table.getLocation().add(0.5, 0.0, 0.5), 0.3);
            if (bot.getLocation().distance(table.getLocation()) < 2.0) {
               CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Standing at enchanting table.");
            }

            return true;
         } else {
            return false;
         }
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildBrewPotions() {
      this.goalSteps.add(new BotGoalPlanner.Step("BP0", "Find brewing stand", (bot, nmsBot, p) -> {
         Block stand = findNearestBlock(bot, new Material[]{Material.BREWING_STAND}, 16);
         if (stand != null) {
            walkTo(bot, nmsBot, stand.getLocation().add(0.5, 0.0, 0.5), 0.3);
            if (bot.getLocation().distance(stand.getLocation()) < 2.0) {
               CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Standing at brewing stand.");
            }

            return true;
         } else {
            return false;
         }
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildNetherPortal() {
      this.goalSteps.add(new BotGoalPlanner.Step("NP0", "Build nether portal", (bot, nmsBot, p) -> {
         boolean hasObsidian = false;
         boolean hasFlint = false;

         for (int i = 0; i < 9; i++) {
            ItemStack item = bot.getInventory().getItem(i);
            if (item != null) {
               if (item.getType() == Material.OBSIDIAN) {
                  hasObsidian = true;
               }

               if (item.getType() == Material.FLINT_AND_STEEL) {
                  hasFlint = true;
               }
            }
         }

         if (hasObsidian && hasFlint) {
            CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Ready to build portal (materials available).");
            return true;
         } else {
            CoreBootstrap.PLUGIN.getLogger().warning("[GoalPlanner] Missing obsidian or flint & steel for portal.");
            return false;
         }
      }, (bot, p) -> true, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("NP1", "Light the portal", (bot, nmsBot, p) -> {
         CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Would light portal here.");
         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildExploreEnd() {
      this.goalSteps.add(new BotGoalPlanner.Step("EE0", "Find stronghold", (bot, nmsBot, p) -> {
         CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Would throw eye of ender to find stronghold.");
         return true;
      }, (bot, p) -> true, (bot, p, r) -> null));
   }

   private void buildSurviveNight() {
      this.goalSteps
         .add(
            new BotGoalPlanner.Step(
               "SN0",
               "Find and use a bed",
               (bot, nmsBot, p) -> {
                  Block bed = findNearestBlock(
                     bot,
                     new Material[]{
                        Material.WHITE_BED,
                        Material.ORANGE_BED,
                        Material.MAGENTA_BED,
                        Material.LIGHT_BLUE_BED,
                        Material.YELLOW_BED,
                        Material.LIME_BED,
                        Material.PINK_BED,
                        Material.GRAY_BED,
                        Material.LIGHT_GRAY_BED,
                        Material.CYAN_BED,
                        Material.PURPLE_BED,
                        Material.BLUE_BED,
                        Material.BROWN_BED,
                        Material.GREEN_BED,
                        Material.RED_BED,
                        Material.BLACK_BED
                     },
                     16
                  );
                  if (bed == null) {
                     return false;
                  } else {
                     walkTo(bot, nmsBot, bed.getLocation().add(0.5, 0.0, 0.5), 0.3);
                     return true;
                  }
               },
               (bot, p) -> {
                  long time = bot.getWorld().getTime();
                  return time < 13000L || time > 23000L;
               },
               (bot, p, r) -> null
            )
         );
      this.goalSteps.add(new BotGoalPlanner.Step("SN1", "Dig straight down for hidey hole", (bot, nmsBot, p) -> {
         Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
         if (below.getType() != Material.BEDROCK && below.getType() != Material.AIR) {
            nmsBot.lookAt(below.getLocation().add(0.5, 0.5, 0.5));
            if (!this.mineBlockTick(nmsBot, below, bot)) {
               if (bot.isOnGround()) {
                  Location down = bot.getLocation().subtract(0.0, 1.0, 0.0);
                  nmsBot.teleport(down);
               }

               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      }, (bot, p) -> {
         long stepTicks = p.stepStartTick;
         int depth = (int)(p.surfaceY - bot.getLocation().getY());
         return depth >= 2 || System.currentTimeMillis() - stepTicks > 8000L;
      }, (bot, p, r) -> new BotGoalPlanner.Step("SN1-F", "Wait in open (no tools to dig)", (b, nms, pl) -> {
            b.setSneaking(true);
            return true;
         }, (b, pl) -> {
            long time2 = b.getWorld().getTime();
            return time2 < 13000L || time2 > 23000L;
         }, (b, pl, rr) -> null)));
      this.goalSteps.add(new BotGoalPlanner.Step("SN2", "Seal top with dirt block", (bot, nmsBot, p) -> {
         Block above = bot.getLocation().add(0.0, 2.0, 0.0).getBlock();
         if (above.getType() != Material.AIR) {
            return true;
         } else {
            for (int i = 0; i < 9; i++) {
               ItemStack item = bot.getInventory().getItem(i);
               if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                  nmsBot.selectHotbarSlot(i);
                  nmsBot.placeBlock(above);
                  CoreBootstrap.PLUGIN.getLogger().info("[GoalPlanner] Sealed hidey hole.");
                  return true;
               }
            }

            bot.setSneaking(true);
            return true;
         }
      }, (bot, p) -> {
         long time3 = bot.getWorld().getTime();
         return time3 < 13000L || time3 > 23000L;
      }, (bot, p, r) -> null));
      this.goalSteps.add(new BotGoalPlanner.Step("SN3", "Break out when morning", (bot, nmsBot, p) -> {
         bot.setSneaking(false);

         for (int dy = 1; dy <= 3; dy++) {
            Block above2 = bot.getLocation().add(0.0, (double)dy, 0.0).getBlock();
            if (above2.getType().isSolid() && above2.getType() != Material.BEDROCK) {
               if (!this.mineBlockTick(nmsBot, above2, bot)) {
                  return false;
               }

               return true;
            }
         }

         if (bot.isOnGround()) {
            nmsBot.jump();
         }

         return true;
      }, (bot, p) -> {
         Block head = bot.getLocation().add(0.0, 2.0, 0.0).getBlock();
         Block above3 = bot.getLocation().add(0.0, 1.0, 0.0).getBlock();
         return head.getType() == Material.AIR && above3.getType() == Material.AIR;
      }, (bot, p, r) -> null));
   }

   public static enum GoalType {
      IDLE,
      GATHER_WOOD,
      GATHER_STONE,
      CRAFT_BASIC_TOOLS,
      MINE_SURFACE_ORES,
      DIG_FOR_DIAMONDS,
      BUILD_SHELTER,
      FIND_FOOD,
      EXPLORE,
      SURVIVE_NIGHT,
      FLEE_DANGER,
      SMELT_ORES,
      DEEP_MINE,
      COOK_FOOD,
      BUILD_BASIC_HOUSE,
      BRIDGE_GAP,
      FILL_HOLE,
      PLACE_TORCHES,
      FARM_CROPS,
      BREED_ANIMALS,
      SHEAR_SHEEP,
      TRADE_VILLAGER,
      ENCHANT_GEAR,
      BREW_POTIONS,
      BUILD_NETHER_PORTAL,
      EXPLORE_END;
   }

   public static class Step {
      public final String id;
      public final String description;
      public final BotGoalPlanner.StepAction action;
      public final BotGoalPlanner.StepCondition condition;
      public final BotGoalPlanner.StepFallback fallback;

      public Step(String id, String description, BotGoalPlanner.StepAction action, BotGoalPlanner.StepCondition condition, BotGoalPlanner.StepFallback fallback) {
         this.id = id;
         this.description = description;
         this.action = action;
         this.condition = condition;
         this.fallback = fallback;
      }
   }

   public interface StepAction {
      boolean execute(Player var1, NMSBot var2, BotGoalPlanner var3);
   }

   public interface StepCondition {
      boolean isMet(Player var1, BotGoalPlanner var2);
   }

   public interface StepFallback {
      BotGoalPlanner.Step onBlocked(Player var1, BotGoalPlanner var2, String var3);
   }
}
