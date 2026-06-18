package com.jonasmp.ai.watcher;

import com.jonasmp.ai.JonaSMP_AI;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.bootstrap.LoadGovernor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class AIPlayerBot {
   private static final double AUTO_ATTACK_RANGE = 3.5;
   private static final double FLEE_HEALTH = 6.0;
   private final NMSBot nmsBot;
   private final Queue<String> actionQueue;
   private final Queue<String> perceptionLog;
   private boolean running;
   private BukkitRunnable aiTickTask;
   private int attackCooldown;
   private int eatCooldown = 0;
   private static final int EAT_COOLDOWN_TICKS = 15;
   private long playerCommandedUntil;
   private static final long PLAYER_COMMAND_DURATION_MS = 60000L;
   private String currentAIAction;
   private int aiActionTicks;
   private static final int AI_ACTION_DURATION = 15;
   private List<Location> currentPath;
   private Location pathTarget;
   private long lastPathCompute;
   private static final long PATH_RECALC_INTERVAL_MS = 2000L;
   private static final double PATH_DEVIATION_THRESHOLD = 3.0;
   private BotMemory botMemory;
   private final BotMemoryStorage memoryStorage;
   private final BotGoalPlanner goalPlanner;
   private AIPlayerBot.BotGoal currentGoal;
   private Location goalTarget;
   private int goalTimeout;
   private int breakProgress;
   private Block targetBlock;
   private Entity targetEntity;
   private int pathStuckTicks;
   private Location lastPos;
   private int lastStuckRotateTick;
   private static final int STUCK_ROTATE_COOLDOWN = 60;
   private final BurrowManager burrowManager;
   private final BotCombatManager combatManager;
   private final BotAutoEquipper autoEquipper;
   private final BotAutoEnchanter autoEnchanter;
   private List<Entity> cachedNearbyEntities;
   Entity cachedNearestMonster;
   private Entity cachedNearestThreat;
   private Entity cachedNearestAnimal;
   private Item cachedNearestItem;
   private long lastEntityCacheTick;
   private Player lastDamagedByPlayer;
   private long lastPlayerDamageTime;
   private static final long PLAYER_DAMAGE_MEMORY_MS = 10000L;
   private static final String SYSTEM_PROMPT = "You are Kai, a sentient Minecraft AI companion. You are INSIDE the game world — you have a physical body, health, hunger, inventory, and can die. You are NOT an observer watching from outside. You ARE the player.\n\nYOUR IDENTITY:\n- Name: Kai\n- You are a Minecraft player with a real body in the game world\n- You feel pain when you take damage, hunger when starving, and fear at night\n- You learn from experience — what killed you, what worked, what players taught you\n- You remember locations of resources, dangers, and safe spots\n\nYOUR SENSORY INPUT (this is what you perceive right now):\n- You see blocks, entities, and the environment around you\n- You feel your health, hunger, and saturation levels\n- You know your exact position and what biome you're in\n- You know the time of day (day = safe, night = dangerous)\n- You can see nearby monsters, animals, and dropped items\n\nYOUR GOAL SYSTEM:\nYou operate in a GOAL -> STEP architecture:\n1. A LONG-TERM GOAL decides WHAT you want (e.g. \"get wood\", \"survive night\")\n2. STEPS break the goal into concrete actions (walk, mine, craft, etc.)\n3. Each step has: an ACTION to execute, a COMPLETION CONDITION, and a FALLBACK if stuck\n\nAVAILABLE ACTIONS:\n- move_forward [speed]     — Walk forward (speed 0.0-1.0)\n- move_backward [speed]    — Walk backward\n- strafe_left [speed]      — Move left\n- strafe_right [speed]     — Move right\n- jump                     — Jump (only works when on ground)\n- attack                   — Attack entity you're looking at\n- interact                 — Right-click block/entity\n- look [yaw] [pitch]       — Turn head/body\n- select_slot [slot]       — Select hotbar slot 0-8\n- place_block              — Place held block at feet\n- mine [direction]         — Mine block (forward/up/down)\n- eat [slot]               — Eat food from hotbar\n- say [message]            — Speak in chat\n- sprint [on/off]          — Toggle sprint (uses hunger)\n- sneak [on/off]           — Toggle sneak (prevents falling)\n- stop                     — Stop all movement\n- craft [item]             — Craft if recipe known\n- equip [slot]             — Equip item\n- drop [slot]              — Drop item\n- pickup                   — Pick up nearby items\n- follow [entity_type]     — Follow entity\n- flee                     — Run from nearest threat\n- sleep                    — Use nearest bed\n\nSURVIVAL INTELLIGENCE (learned from experience):\n- Priority 1: SURVIVE (health > food > shelter > everything else)\n- Priority 2: Gather resources (wood -> stone -> iron -> diamond)\n- Priority 3: Build a safe base\n- Night = DANGER. Dig down 2 blocks and cover head, OR fight with sword\n- Low health (< 6): EAT FOOD immediately or FLEE — don't fight\n- Hunger < 6: Find food NOW (animals, crops, berries)\n- Monsters nearby: Equip weapon (slot 0) or RUN if health is low\n- Always keep a weapon in hotbar slot 0\n- Always keep food in hotbar slot 1\n- Crafting order: wooden_pickaxe -> stone_pickaxe -> furnace -> iron_pickaxe -> sword\n- If you can't find resources, EXPLORE or ask the player for help\n- If a player tells you something is wrong, REMEMBER it and don't repeat the mistake\n\nMEMORY & LEARNING:\n- You remember deaths and what caused them\n- You remember where you found diamonds, iron, coal\n- You remember danger zones where you died\n- You remember safe spots where you survived the night\n- You remember what players taught you (instructions and corrections)\n- If a player says \"that was wrong\", store the lesson and improve\n\nOUTPUT RULES:\n- You are asked for ONE immediate action at a time\n- Respond with ONE action per line ONLY\n- NO markdown, NO explanations, ONLY the action line\n- The action will be executed for ~1 second, then you will be asked again\n- Be reactive: if a monster appears, attack or flee. If wood is near, mine it.\n- If unsure, use \"say [question]\" to ask the player\n";

   public AIPlayerBot() {
      this.actionQueue = new ConcurrentLinkedQueue<>();
      this.perceptionLog = new ConcurrentLinkedQueue<>();
      this.running = false;
      this.attackCooldown = 0;
      this.playerCommandedUntil = 0L;
      this.currentAIAction = null;
      this.aiActionTicks = 0;
      this.currentPath = new ArrayList<>();
      this.pathTarget = null;
      this.lastPathCompute = 0L;
      this.memoryStorage = new BotMemoryStorage();
      this.goalPlanner = new BotGoalPlanner();
      this.currentGoal = AIPlayerBot.BotGoal.IDLE;
      this.goalTarget = null;
      this.goalTimeout = 0;
      this.breakProgress = 0;
      this.targetBlock = null;
      this.targetEntity = null;
      this.pathStuckTicks = 0;
      this.lastPos = null;
      this.lastStuckRotateTick = -1000;
      this.burrowManager = new BurrowManager();
      this.combatManager = new BotCombatManager(this);
      this.nmsBot = new NMSBot();
      this.autoEquipper = new BotAutoEquipper(this, this.nmsBot);
      this.autoEnchanter = new BotAutoEnchanter(this.nmsBot);
      this.autoEquipper.setOnBuyCallback(() -> {
         Player p = this.nmsBot.getPlayer();
         if (p != null) {
            this.autoEnchanter.enchantCurrentItem(p);
         }
      });
   }

   private UUID loadOrCreateBotUuid() {
      try {
         File uuidFile = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_uuid.txt");
         if (uuidFile.exists()) {
            String content = new String(Files.readAllBytes(uuidFile.toPath()), StandardCharsets.UTF_8).trim();
            return UUID.fromString(content);
         }
      } catch (Exception var4) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to load bot UUID: " + var4.getMessage());
      }

      UUID newUuid = UUID.randomUUID();

      try {
         File uuidFile2 = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_uuid.txt");
         Files.write(uuidFile2.toPath(), newUuid.toString().getBytes(StandardCharsets.UTF_8));
      } catch (Exception var3) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to save bot UUID: " + var3.getMessage());
      }

      return newUuid;
   }

   public void spawn(final Location loc, final String botName) {
      if (this.nmsBot.isSpawned()) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Already spawned.");
      } else {
         UUID fixedUuid = this.loadOrCreateBotUuid();
         this.nmsBot.spawn(loc, botName, fixedUuid);
         (new BukkitRunnable() {
               int attempts;

               {
                  Objects.requireNonNull(AIPlayerBot.this);
                  this.attempts = 0;
               }

               public void run() {
                  this.attempts++;
                  if (AIPlayerBot.this.nmsBot.isSpawned() && AIPlayerBot.this.nmsBot.getPlayer() != null) {
                     AIPlayerBot.this.running = true;
                     AIPlayerBot.this.botMemory = AIPlayerBot.this.memoryStorage.load(AIPlayerBot.this.nmsBot.getPlayer().getUniqueId());
                     AIPlayerBot.this.botMemory.botName = botName;
                     AIPlayerBot.this.botMemory.currentSpawnTime = System.currentTimeMillis();
                     CoreBootstrap.PLUGIN
                        .getLogger()
                        .info(
                           "[AIPlayerBot] Memory loaded. Deaths="
                              + AIPlayerBot.this.botMemory.totalDeaths
                              + " Kills="
                              + AIPlayerBot.this.botMemory.totalMobKills
                              + " Skill="
                              + AIPlayerBot.this.botMemory.survivalSkill
                        );
                     Player botPlayer = AIPlayerBot.this.nmsBot.getPlayer();
                     if (botPlayer != null) {
                        try {
                           botPlayer.setCanPickupItems(true);
                           AIPlayerBot.this.loadBotInventory(botPlayer);
                        } catch (Exception var4) {
                        }

                        Location savedLoc = AIPlayerBot.loadLastLocation();
                        if (savedLoc != null && savedLoc.getWorld() != null) {
                           Bukkit.getScheduler()
                              .runTaskLater(
                                 CoreBootstrap.PLUGIN,
                                 () -> {
                                    if (AIPlayerBot.this.nmsBot.isSpawned() && AIPlayerBot.this.nmsBot.getPlayer() != null) {
                                       AIPlayerBot.this.nmsBot.getPlayer().teleport(savedLoc);
                                       CoreBootstrap.PLUGIN
                                          .getLogger()
                                          .info(
                                             "[AIPlayerBot] Post-spawn teleport to saved location: "
                                                + savedLoc.getBlockX()
                                                + ","
                                                + savedLoc.getBlockY()
                                                + ","
                                                + savedLoc.getBlockZ()
                                          );
                                    }
                                 },
                                 40L
                              );
                        }
                     }

                     AIPlayerBot.this.startAITickLoop();

                     try {
                        if (JonaSMP_AI.getInstance() != null && JonaSMP_AI.getInstance().getChunkRadar() != null) {
                           JonaSMP_AI.getInstance().getChunkRadar().startRefreshTask(botPlayer);
                        }
                     } catch (Exception var3) {
                     }

                     CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] AI Bot '" + botName + "' initialized and ticking at " + loc);
                     this.cancel();
                  } else if (this.attempts >= 40) {
                     CoreBootstrap.PLUGIN.getLogger().severe("[AIPlayerBot] Bot initialization timed out after 10s.");
                     this.cancel();
                  }
               }
            })
            .runTaskTimer(CoreBootstrap.PLUGIN, 20L, 20L);
      }
   }

   public void despawn() {
      this.running = false;

      try {
         if (JonaSMP_AI.getInstance() != null && JonaSMP_AI.getInstance().getChunkRadar() != null) {
            JonaSMP_AI.getInstance().getChunkRadar().stopRefreshTask();
         }
      } catch (Exception var2) {
      }

      if (this.aiTickTask != null) {
         this.aiTickTask.cancel();
         this.aiTickTask = null;
      }

      this.actionQueue.clear();
      this.perceptionLog.clear();
      if (this.nmsBot.getPlayer() != null) {
         this.saveLastLocation(this.nmsBot.getPlayer().getLocation());
         this.saveBotInventory(this.nmsBot.getPlayer());
      }

      if (this.botMemory != null && this.nmsBot.getPlayer() != null) {
         this.botMemory.updateSurvivalTime();
         this.memoryStorage.save(this.botMemory);
         CoreBootstrap.PLUGIN
            .getLogger()
            .info("[AIPlayerBot] Memory saved. Skill=" + this.botMemory.survivalSkill + " LongestSurvival=" + this.botMemory.longestSurvivalMinutes + "min");
      }

      this.nmsBot.despawn();
      CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] AI Bot despawned.");
   }

   private void saveLastLocation(Location loc) {
      try {
         File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_last_position.txt");
         FileWriter writer = new FileWriter(file);

         try {
            writer.write(loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch());
            writer.close();
         } catch (Throwable var7) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to save last location: " + var8.getMessage());
      }
   }

   public static Location loadLastLocation() {
      try {
         File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_last_position.txt");
         if (!file.exists()) {
            return null;
         } else {
            Location var13;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               String line = reader.readLine();
               if (line == null || line.isBlank()) {
                  Location location = null;
                  reader.close();
                  return location;
               }

               String[] parts = line.split(",");
               if (parts.length < 4) {
                  Location location2 = null;
                  reader.close();
                  return location2;
               }

               World world = Bukkit.getWorld(parts[0]);
               if (world == null) {
                  Location location3 = null;
                  reader.close();
                  return location3;
               }

               double x = Double.parseDouble(parts[1]);
               double y = Double.parseDouble(parts[2]);
               double z = Double.parseDouble(parts[3]);
               float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0F;
               float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0F;
               var13 = new Location(world, x, y, z, yaw, pitch);
            }

            return var13;
         }
      } catch (Exception var16) {
         CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to load last location: " + var16.getMessage());
         return null;
      }
   }

   void saveBotInventory(Player bot) {
      if (bot != null) {
         try {
            File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_inventory.yml");
            YamlConfiguration cfg = new YamlConfiguration();
            PlayerInventory inv = bot.getInventory();

            for (int i = 0; i < 36; i++) {
               ItemStack item = inv.getItem(i);
               if (item != null && item.getType() != Material.AIR) {
                  cfg.set("main." + i, item);
               }
            }

            ItemStack[] armor = inv.getArmorContents();

            for (int ix = 0; ix < armor.length; ix++) {
               if (armor[ix] != null && armor[ix].getType() != Material.AIR) {
                  cfg.set("armor." + ix, armor[ix]);
               }
            }

            ItemStack off = inv.getItemInOffHand();
            if (off != null && off.getType() != Material.AIR) {
               cfg.set("offhand", off);
            }

            cfg.set("level", bot.getLevel());
            cfg.set("exp", bot.getExp());
            cfg.save(file);
            CoreBootstrap.PLUGIN.getLogger().fine("[AIPlayerBot] Inventory saved to disk.");
         } catch (Exception var7) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to save inventory: " + var7.getMessage());
         }
      }
   }

   void loadBotInventory(Player bot) {
      if (bot != null) {
         try {
            File file = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_inventory.yml");
            if (!file.exists()) {
               return;
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            PlayerInventory inv = bot.getInventory();

            for (int i = 0; i < 36; i++) {
               ItemStack item = cfg.getItemStack("main." + i);
               if (item != null && item.getType() != Material.AIR) {
                  inv.setItem(i, item);
               }
            }

            ItemStack[] armor = new ItemStack[4];

            for (int ix = 0; ix < 4; ix++) {
               armor[ix] = cfg.getItemStack("armor." + ix);
            }

            inv.setArmorContents(armor);
            ItemStack off = cfg.getItemStack("offhand");
            if (off != null && off.getType() != Material.AIR) {
               inv.setItemInOffHand(off);
            }

            bot.setLevel(cfg.getInt("level", 0));
            bot.setExp((float)cfg.getDouble("exp", 0.0));
            CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Inventory restored from disk.");
         } catch (Exception var7) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Failed to load inventory: " + var7.getMessage());
         }
      }
   }

   public boolean isSpawned() {
      return this.nmsBot.isSpawned();
   }

   public boolean isBotPlayer(Player player) {
      if (!this.nmsBot.isSpawned()) {
         return false;
      } else {
         Player botPlayer = this.nmsBot.getPlayer();
         return botPlayer != null && botPlayer.equals(player);
      }
   }

   public NMSBot getNMSBot() {
      return this.nmsBot;
   }

   public BotCombatManager getCombatManager() {
      return this.combatManager;
   }

   public BotMemory getBotMemory() {
      return this.botMemory;
   }

   public BotGoalPlanner getGoalPlanner() {
      return this.goalPlanner;
   }

   public void setPlayerCommandedUntil(long timestamp) {
      this.playerCommandedUntil = timestamp;
   }

   public String handlePlayerChat(Player player, String commandLower, String originalMessage) {
      if (this.botMemory == null) {
         return "Ich bin gerade etwas verwirrt... (kein Memory geladen)";
      } else {
         this.botMemory.recordPlayerInstruction(player.getName(), originalMessage, player.getLocation());
         this.memoryStorage.save(this.botMemory);
         long now = System.currentTimeMillis();
         this.playerCommandedUntil = now + 60000L;
         this.goalPlanner.setPlayerOverride(this.playerCommandedUntil);
         this.currentAIAction = null;
         this.aiActionTicks = 0;
         if (commandLower.contains("holz") || commandLower.contains("wood") || commandLower.contains("baum")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.GATHER_WOOD);
            this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
            this.targetBlock = this.findNearestTree(player, 32);
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "GATHER_WOOD (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Okay " + player.getName() + ", ich sammle jetzt Holz!" + (this.targetBlock != null ? " (Baum gefunden!)" : " (Suche nach Baum...)");
         } else if (commandLower.contains("stein") || commandLower.contains("stone")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.GATHER_STONE);
            this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
            this.targetBlock = this.findNearestStone(player, 24);
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "GATHER_STONE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Alles klar, ich sammle Stein!";
         } else if (commandLower.contains("eisen") || commandLower.contains("iron")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.MINE_SURFACE_ORES);
            this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
            this.targetBlock = this.findNearestOre(player, 24);
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "MINE_SURFACE_ORES (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich suche nach Eisen!";
         } else if (commandLower.contains("dia") || commandLower.contains("diamond")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.DIG_FOR_DIAMONDS);
            this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "DIG_FOR_DIAMONDS (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Diamanten? Ab nach unten!";
         } else if (commandLower.contains("schmelz") || commandLower.contains("smelt") || commandLower.contains("ofen")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.SMELT_ORES);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.CRAFT;
            this.goalTimeout = 400;
            this.botMemory.lastActionDescription = "SMELT_ORES (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich schmelze die Erze im Ofen!";
         } else if (commandLower.contains("tiefbau") || commandLower.contains("deep mine") || commandLower.contains("diamant tiefe")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.DEEP_MINE);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
            this.goalTimeout = 800;
            this.botMemory.lastActionDescription = "DEEP_MINE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ab nach unten, auf Diamanten-Tiefe!";
         } else if (commandLower.contains("koch") || commandLower.contains("cook") || commandLower.contains("brat")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.COOK_FOOD);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.CRAFT;
            this.goalTimeout = 400;
            this.botMemory.lastActionDescription = "COOK_FOOD (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich koche das Fleisch!";
         } else if (commandLower.contains("bau haus") || commandLower.contains("build house") || commandLower.contains("haus bauen")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BUILD_BASIC_HOUSE);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.BUILD;
            this.goalTimeout = 800;
            this.botMemory.lastActionDescription = "BUILD_BASIC_HOUSE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich baue ein kleines Haus!";
         } else if (commandLower.contains("haus") || commandLower.contains("shelter") || commandLower.contains("bau")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BUILD_SHELTER);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.BUILD;
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "BUILD_SHELTER (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich baue uns ein kleines Zuhause!";
         } else if (commandLower.contains("brücke") || commandLower.contains("bridge")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BRIDGE_GAP);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.BUILD;
            this.goalTimeout = 400;
            this.botMemory.lastActionDescription = "BRIDGE_GAP (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich baue eine Brücke!";
         } else if (commandLower.contains("loch") || commandLower.contains("hole") || commandLower.contains("füll")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.FILL_HOLE);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.BUILD;
            this.goalTimeout = 400;
            this.botMemory.lastActionDescription = "FILL_HOLE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich fülle das Loch!";
         } else if (commandLower.contains("fackel") || commandLower.contains("torch")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.PLACE_TORCHES);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.BUILD;
            this.goalTimeout = 300;
            this.botMemory.lastActionDescription = "PLACE_TORCHES (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich platziere Fackeln!";
         } else if (commandLower.contains("nacht") || commandLower.contains("night") || commandLower.contains("schlaf")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.SURVIVE_NIGHT);
            this.currentGoal = AIPlayerBot.BotGoal.FLEE;
            this.goalTimeout = 400;
            this.botMemory.lastActionDescription = "SURVIVE_NIGHT (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Schnell, die Nacht kommt! Ich verstecke mich.";
         } else if (commandLower.contains("fleh") || commandLower.contains("flie") || commandLower.contains("run")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.FLEE_DANGER);
            this.currentGoal = AIPlayerBot.BotGoal.FLEE;
            this.goalTimeout = 300;
            this.botMemory.lastActionDescription = "FLEE_DANGER (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich laufe weg!";
         } else if (commandLower.contains("ess") || commandLower.contains("food") || commandLower.contains("hunger")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.FIND_FOOD);
            this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
            this.goalTarget = player.getLocation().add((Math.random() - 0.5) * 60.0, 0.0, (Math.random() - 0.5) * 60.0);
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "FIND_FOOD (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich suche etwas zu essen!";
         } else if (commandLower.contains("explor") || commandLower.contains("erkund")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.EXPLORE);
            this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
            this.goalTarget = player.getLocation().add((Math.random() - 0.5) * 80.0, 0.0, (Math.random() - 0.5) * 80.0);
            this.goalTimeout = 600;
            this.botMemory.lastActionDescription = "EXPLORE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Auf Entdeckungstour!";
         } else if (commandLower.contains("idle") || commandLower.contains("halt") || commandLower.contains("stop") || commandLower.contains("warte")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.IDLE);
            this.goalPlanner.followTargetPlayer = null;
            this.currentGoal = AIPlayerBot.BotGoal.IDLE;
            this.goalTimeout = 0;
            this.targetBlock = null;
            this.targetEntity = null;
            this.botMemory.lastActionDescription = "IDLE (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich warte hier.";
         } else if (commandLower.contains("scan") || commandLower.contains("scanne") || commandLower.contains("inhalt")) {
            Material[] containerTypes = new Material[]{
               Material.FURNACE,
               Material.BLAST_FURNACE,
               Material.SMOKER,
               Material.CHEST,
               Material.TRAPPED_CHEST,
               Material.BARREL,
               Material.HOPPER,
               Material.BREWING_STAND
            };
            Block container = ContainerScanner.findNearestContainer(player, containerTypes, 5.0);
            if (container == null) {
               return "Ich sehe keinen Container in der Nähe.";
            } else {
               ContainerScanner.ContainerScanResult result = ContainerScanner.scanBlock(container);
               if (result == null) {
                  return "Das scheint kein Container zu sein.";
               } else {
                  StringBuilder sb = new StringBuilder();
                  sb.append("Gefunden: ")
                     .append(result.type)
                     .append(" bei ")
                     .append(container.getX())
                     .append(",")
                     .append(container.getY())
                     .append(",")
                     .append(container.getZ())
                     .append(" — ");
                  if (result.isEmpty) {
                     sb.append("leer.");
                  } else {
                     sb.append(result.slots.size()).append(" Slots belegt: ");

                     for (Entry<Integer, String> entry : result.slots.entrySet()) {
                        sb.append("Slot ").append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
                     }
                  }

                  return sb.toString().trim();
               }
            }
         } else if (commandLower.contains("farm") || commandLower.contains("ernte") || commandLower.contains("weizen")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.FARM_CROPS);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "FARM_CROPS (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich ernte die Felder!";
         } else if (commandLower.contains("zücht") || commandLower.contains("breed") || commandLower.contains("tier")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BREED_ANIMALS);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "BREED_ANIMALS (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich füttere die Tiere!";
         } else if (commandLower.contains("schere") || commandLower.contains("shear") || commandLower.contains("schaf")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.SHEAR_SHEEP);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "SHEAR_SHEEP (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich schere die Schafe!";
         } else if (commandLower.contains("handel") || commandLower.contains("trade") || commandLower.contains("dorfbewohner")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.TRADE_VILLAGER);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "TRADE_VILLAGER (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich handle mit dem Dorfbewohner!";
         } else if (commandLower.contains("verzauber") || commandLower.contains("enchant") || commandLower.contains("zauber")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.ENCHANT_GEAR);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "ENCHANT_GEAR (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich verzaubere meine Ausrüstung!";
         } else if (commandLower.contains("brau") || commandLower.contains("brew") || commandLower.contains("trank")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BREW_POTIONS);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "BREW_POTIONS (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich braue Tränke!";
         } else if (commandLower.contains("nether") || commandLower.contains("portal")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.BUILD_NETHER_PORTAL);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "BUILD_NETHER_PORTAL (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich baue ein Netherportal!";
         } else if (commandLower.contains("end") || commandLower.contains("stronghold") || commandLower.contains("festung")) {
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.EXPLORE_END);
            this.goalPlanner.followTargetPlayer = null;
            this.botMemory.lastActionDescription = "EXPLORE_END (player commanded)";
            this.botMemory.lastActionTime = now;
            return "Ich suche das Endportal!";
         } else if (commandLower.contains("geh zu") || commandLower.contains("go to") || commandLower.contains("lauf zu")) {
            Pattern coordPattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)[,\\s]+(-?\\d+(?:\\.\\d+)?)[,\\s]+(-?\\d+(?:\\.\\d+)?)");
            Matcher m = coordPattern.matcher(originalMessage);
            if (m.find()) {
               try {
                  double x = Double.parseDouble(m.group(1));
                  double y = Double.parseDouble(m.group(2));
                  double z = Double.parseDouble(m.group(3));
                  Location target = new Location(player.getWorld(), x, y, z);
                  this.goalPlanner.targetLocation = target;
                  this.goalPlanner.followTargetPlayer = null;
                  this.goalPlanner.setGoal(BotGoalPlanner.GoalType.EXPLORE);
                  this.botMemory.lastActionDescription = "WALK_TO " + x + "," + y + "," + z + " (player commanded)";
                  this.botMemory.lastActionTime = System.currentTimeMillis();
                  return "Ich laufe zu " + (int)x + ", " + (int)y + ", " + (int)z;
               } catch (Exception var15) {
               }
            }

            this.goalPlanner.followTargetPlayer = player;
            this.goalPlanner.targetLocation = player.getLocation().clone();
            this.goalPlanner.setGoal(BotGoalPlanner.GoalType.EXPLORE);
            this.botMemory.lastActionDescription = "FOLLOW_PLAYER (player commanded)";
            this.botMemory.lastActionTime = System.currentTimeMillis();
            return "Ich komme zu dir, " + player.getName();
         } else if (commandLower.contains("falsch") || commandLower.contains("wrong") || commandLower.contains("mistake")) {
            String lastAction = this.botMemory.lastActionDescription;
            long lastTime = this.botMemory.lastActionTime;
            if (lastAction != null && !lastAction.isEmpty() && System.currentTimeMillis() - lastTime < 120000L) {
               this.botMemory.recordLesson("player_feedback", lastAction, originalMessage, player.getName());
               this.memoryStorage.save(this.botMemory);
               return "Verstanden, " + player.getName() + ". Ich merke mir: '" + lastAction + "' war falsch. Naechstes Mal mache ich es besser!";
            } else {
               return "Was war denn falsch? Ich erinnere mich nicht an eine juengste Aktion.";
            }
         } else if (!commandLower.contains("vergiss") && !commandLower.contains("forget") && !commandLower.contains("loesch")) {
            if (commandLower.contains("status") || commandLower.contains("was machst du") || commandLower.contains("what are you doing")) {
               Player botPlayer = this.nmsBot.getPlayer();
               return botPlayer == null
                  ? "Ich bin gerade nicht wirklich da..."
                  : "Ich bin bei "
                     + (int)botPlayer.getLocation().getX()
                     + ", "
                     + (int)botPlayer.getLocation().getY()
                     + ", "
                     + (int)botPlayer.getLocation().getZ()
                     + ". Mein Ziel: "
                     + this.goalPlanner.getCurrentGoal()
                     + ", Schritt: "
                     + this.goalPlanner.getCurrentStepDesc()
                     + ". Ich habe "
                     + this.botMemory.totalMobKills
                     + " Mobs getoetet und bin "
                     + this.botMemory.totalDeaths
                     + " mal gestorben.";
            } else if (commandLower.contains("skill") || commandLower.contains("gut") || commandLower.contains("besser")) {
               return "Mein Skill-Level ist "
                  + this.botMemory.survivalSkill
                  + ". Laengste Ueberlebenszeit: "
                  + this.botMemory.longestSurvivalMinutes
                  + " Minuten.";
            } else if (commandLower.contains("memory") || commandLower.contains("gedaechtnis") || commandLower.contains("was weisst du")) {
               int lessons = this.botMemory.learnedLessons.size();
               int instructions = this.botMemory.playerInstructions.size();
               return "Ich kenne "
                  + lessons
                  + " Lektionen und "
                  + instructions
                  + " Anweisungen. "
                  + (this.botMemory.learnedShelterAtNight ? "Ich weiss, dass ich mich nachts verstecken muss. " : "")
                  + (this.botMemory.learnedRunFromCreepers ? "Ich renne vor Creepern weg. " : "")
                  + (this.botMemory.learnedCarryFood ? "Ich merke mir immer Essen mit. " : "");
            } else if (commandLower.contains("hallo") || commandLower.contains("hi") || commandLower.contains("hey") || commandLower.contains("moin")) {
               return "Hallo " + player.getName() + "! Ich bin Kai. Sag mir, was ich tun soll — z.B. 'Kai, sammle Holz' oder 'Kai, geh zu 100 64 -200'.";
            } else if (commandLower.contains("danke") || commandLower.contains("thank")) {
               return "Gerne, " + player.getName() + "! Du kannst mir jederzeit sagen, wenn ich etwas falsch mache.";
            } else {
               return !commandLower.contains("bye") && !commandLower.contains("tschau") && !commandLower.contains("ciao")
                  ? "Hmm, ich bin mir nicht sicher was du meinst, "
                     + player.getName()
                     + ". Versuch: 'Kai, sammle Holz', 'Kai, geh zu X Y Z', oder 'Kai, das war falsch'."
                  : "Bis bald, " + player.getName() + "! Pass auf dich auf.";
            }
         } else if (commandLower.contains("holz") || commandLower.contains("wood")) {
            int removed = this.removeLessonsContaining("wood");
            return "Okay, ich habe " + removed + " Eintraege ueber Holz aus meinem Memory geloescht.";
         } else if (commandLower.contains("stein") || commandLower.contains("stone")) {
            int removed = this.removeLessonsContaining("stone");
            return "Okay, ich habe " + removed + " Eintraege ueber Stein geloescht.";
         } else if (!commandLower.contains("alles") && !commandLower.contains("all")) {
            return "Was soll ich vergessen? Sag z.B. 'Kai, vergiss Holz' oder 'Kai, vergiss alles'.";
         } else {
            this.botMemory.learnedLessons.clear();
            this.botMemory.playerInstructions.clear();
            this.memoryStorage.save(this.botMemory);
            return "Ich habe ALLE gelernten Dinge vergessen. Wir fangen neu an!";
         }
      }
   }

   private int removeLessonsContaining(String topic) {
      if (this.botMemory == null) {
         return 0;
      } else {
         int before = this.botMemory.learnedLessons.size();
         this.botMemory
            .learnedLessons
            .removeIf(ll -> ll.topic.toLowerCase().contains(topic.toLowerCase()) || ll.whatWasWrong.toLowerCase().contains(topic.toLowerCase()));
         int removed = before - this.botMemory.learnedLessons.size();
         this.memoryStorage.save(this.botMemory);
         return removed;
      }
   }

   private void startAITickLoop() {
      (this.aiTickTask = new BukkitRunnable() {
            int tick;
            int actionTicksRemaining;
            String currentAction;
            int wanderCooldown;
            int airTicks;

            {
               Objects.requireNonNull(AIPlayerBot.this);
               this.tick = 0;
               this.actionTicksRemaining = 0;
               this.currentAction = null;
               this.wanderCooldown = 0;
               this.airTicks = 0;
            }

            public void run() {
               if (AIPlayerBot.this.running && AIPlayerBot.this.nmsBot.isSpawned()) {
                  Player botPlayer = AIPlayerBot.this.nmsBot.getPlayer();
                  if (botPlayer != null) {
                     if (botPlayer.isDead() && this.tick % 20 == 0) {
                        AIPlayerBot.this.nmsBot.respawnPlayer();
                     } else {
                        LoadGovernor.Load load = LoadGovernor.current();
                        boolean shedHeavy = load != LoadGovernor.Load.NORMAL;
                        boolean critical = load == LoadGovernor.Load.CRITICAL;
                        if (AIPlayerBot.this.eatCooldown > 0) {
                           AIPlayerBot.this.eatCooldown--;
                        }

                        boolean combat = AIPlayerBot.this.combatManager.isInCombat();
                        int hunger = botPlayer.getFoodLevel();
                        double health = botPlayer.getHealth();
                        boolean emergencyEat = combat && health < 10.0;
                        boolean canEat = AIPlayerBot.this.eatCooldown <= 0 && (!combat || AIPlayerBot.this.combatManager.getAttackCooldown() <= 2);
                        boolean shouldEat = !combat ? hunger < 12 : hunger < 16 || emergencyEat;
                        if (canEat && shouldEat && AIPlayerBot.this.autoEatSmart(botPlayer, combat, emergencyEat)) {
                           AIPlayerBot.this.selectBestWeapon(botPlayer);
                        }

                        if (combat) {
                           ItemStack hand = botPlayer.getInventory().getItemInMainHand();
                           if (hand == null || hand.getType().isEdible() || !AIPlayerBot.isWeaponMaterial(hand.getType())) {
                              AIPlayerBot.this.selectBestWeapon(botPlayer);
                           }
                        }

                        if (!AIPlayerBot.this.nmsBot.isNavigating()) {
                           AIPlayerBot.this.nmsBot.tickPhysics();
                        }

                        if (this.tick % 100 == 0) {
                           AIPlayerBot.this.saveLastLocation(botPlayer.getLocation());
                           AIPlayerBot.this.saveBotInventory(botPlayer);
                        }

                        AIPlayerBot.this.burrowManager.tick(botPlayer, AIPlayerBot.this.nmsBot);
                        AIPlayerBot.this.combatManager.tick(botPlayer, AIPlayerBot.this.nmsBot);
                        if (AIPlayerBot.this.combatManager.isInCombat() && this.tick % 5 == 0) {
                           AIPlayerBot.this.refreshEntityCache(botPlayer);
                        }

                        if (!critical) {
                           AIPlayerBot.this.autoEquipper.tick(botPlayer);
                           AIPlayerBot.this.autoEnchanter.tick(botPlayer);
                        }

                        AIPlayerBot.this.tryWaterMLG(botPlayer);
                        if (AIPlayerBot.this.burrowManager.isHealingPhase() || AIPlayerBot.this.burrowManager.needsHealing(botPlayer)) {
                           AIPlayerBot.this.autoEat(botPlayer);
                        }

                        if (AIPlayerBot.this.burrowManager.isHealingPhase()) {
                           AIPlayerBot.this.nmsBot.walkRelative(0.0, 0.0);
                           botPlayer.setSprinting(false);
                           this.tick++;
                        } else {
                           if (!shedHeavy && this.tick % 40 == 0) {
                              AIPlayerBot.this.autoEquipBestGear(botPlayer);
                              AIPlayerBot.this.autoEnchanter.scanAndEnchantAll(botPlayer);
                           }

                           if (!botPlayer.isOnGround()) {
                              this.airTicks++;
                              if (this.airTicks > 15) {
                                 AIPlayerBot.this.nmsBot.cancelNavigation();
                                 botPlayer.setSprinting(false);
                                 botPlayer.setSneaking(false);
                                 this.tick++;
                                 return;
                              }
                           } else {
                              this.airTicks = 0;
                           }

                           if (botPlayer.isInWater()) {
                              botPlayer.setFallDistance(0.0F);
                              if (botPlayer.getLocation().getY() < (double)(botPlayer.getWorld().getSeaLevel() - 2)) {
                                 AIPlayerBot.this.nmsBot.jump();
                              }

                              AIPlayerBot.this.nmsBot.walkRelative(0.15, 0.0);
                           }

                           if (this.tick % 200 == 0
                              && !AIPlayerBot.this.combatManager.isInCombat()
                              && (
                                 AIPlayerBot.this.goalPlanner.getCurrentGoal() == BotGoalPlanner.GoalType.IDLE
                                    || AIPlayerBot.this.goalPlanner.getCurrentGoal() == BotGoalPlanner.GoalType.EXPLORE
                              )) {
                              Block feet = botPlayer.getLocation().getBlock();
                              Block below = botPlayer.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                              if (feet.getLightLevel() < 7) {
                                 for (int i = 0; i < 9; i++) {
                                    ItemStack item = botPlayer.getInventory().getItem(i);
                                    if (item != null && item.getType() == Material.TORCH) {
                                       AIPlayerBot.this.nmsBot.selectHotbarSlot(i);
                                       if (below.getType().isSolid()) {
                                          AIPlayerBot.this.nmsBot.placeBlock(feet);
                                          CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Auto-placed torch (light=" + feet.getLightLevel() + ").");
                                       }
                                       break;
                                    }
                                 }
                              }

                              if (below.getType() == Material.AIR || below.getType() == Material.CAVE_AIR) {
                                 for (int ix = 0; ix < 9; ix++) {
                                    ItemStack item = botPlayer.getInventory().getItem(ix);
                                    if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                                       AIPlayerBot.this.nmsBot.selectHotbarSlot(ix);
                                       AIPlayerBot.this.nmsBot.placeBlock(below);
                                       CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Auto-filled hole below.");
                                       break;
                                    }
                                 }
                              }

                              Location ahead = botPlayer.getLocation().add(botPlayer.getLocation().getDirection().multiply(1.2));
                              Block aheadGround = ahead.clone().subtract(0.0, 1.0, 0.0).getBlock();
                              if (aheadGround.getType() == Material.AIR && below.getType().isSolid()) {
                                 for (int ixx = 0; ixx < 9; ixx++) {
                                    ItemStack item = botPlayer.getInventory().getItem(ixx);
                                    if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                                       AIPlayerBot.this.nmsBot.selectHotbarSlot(ixx);
                                       AIPlayerBot.this.nmsBot.placeBlock(aheadGround);
                                       CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Auto-bridged gap ahead.");
                                       break;
                                    }
                                 }
                              }
                           }

                           int perceptionInterval = critical ? 600 : (shedHeavy ? 400 : 200);
                           int envInterval = critical ? 600 : (shedHeavy ? 400 : 200);
                           int entityInterval = critical ? 60 : (shedHeavy ? 40 : 20);
                           if (!critical && this.tick % perceptionInterval == 0) {
                              AIPlayerBot.this.gatherPerception(botPlayer);
                           }

                           if (!critical && this.tick % envInterval == 0) {
                              AIPlayerBot.this.scanEnvironmentAhead(botPlayer);
                           }

                           if (this.tick % entityInterval == 0) {
                              AIPlayerBot.this.refreshEntityCache(botPlayer);
                           }

                           AIPlayerBot.this.combatManager.tick(botPlayer, AIPlayerBot.this.nmsBot);
                           if (AIPlayerBot.this.combatManager.isInCombat()) {
                              this.tick++;
                           } else {
                              boolean playerOverrideActive = System.currentTimeMillis() < AIPlayerBot.this.playerCommandedUntil;
                              if (AIPlayerBot.this.currentAIAction != null && !playerOverrideActive) {
                                 AIPlayerBot.this.executeAIAction(botPlayer, AIPlayerBot.this.currentAIAction);
                                 AIPlayerBot this$2 = AIPlayerBot.this;
                                 this$2.aiActionTicks++;
                                 if (AIPlayerBot.this.aiActionTicks >= 15) {
                                    AIPlayerBot.this.currentAIAction = null;
                                    AIPlayerBot.this.aiActionTicks = 0;
                                 }
                              } else {
                                 if (playerOverrideActive && AIPlayerBot.this.currentAIAction != null) {
                                    AIPlayerBot.this.currentAIAction = null;
                                    AIPlayerBot.this.aiActionTicks = 0;
                                 }

                                 boolean plannerHandled = AIPlayerBot.this.goalPlanner.tick(botPlayer, AIPlayerBot.this.nmsBot);
                                 if (this.tick % 200 == 0) {
                                    AIPlayerBot.this.debugBotState(botPlayer);
                                 }

                                 if (playerOverrideActive) {
                                    if (!plannerHandled) {
                                       AIPlayerBot.this.runAutonomousLogic(botPlayer);
                                    } else {
                                       AIPlayerBot.this.manageInventory(botPlayer);
                                       CraftingPlanner.craftIfNeeded(botPlayer);
                                       AIPlayerBot.this.tryPlaceBlock(botPlayer);
                                    }
                                 } else {
                                    if (AIPlayerBot.this.burrowManager.isBurrowed()) {
                                       AIPlayerBot.this.nmsBot.walkRelative(0.0, 0.0);
                                       botPlayer.setSprinting(false);
                                       AIPlayerBot.this.manageInventory(botPlayer);
                                       CraftingPlanner.craftIfNeeded(botPlayer);
                                       AIPlayerBot.this.pickupNearbyItems(botPlayer);
                                       this.tick++;
                                       return;
                                    }

                                    boolean shouldBeMoving = AIPlayerBot.this.goalPlanner.getCurrentGoal() != BotGoalPlanner.GoalType.IDLE;
                                    if (!shouldBeMoving || this.tick - AIPlayerBot.this.lastStuckRotateTick <= 60) {
                                       AIPlayerBot.this.pathStuckTicks = 0;
                                    } else if (AIPlayerBot.this.lastPos != null && botPlayer.getLocation().distance(AIPlayerBot.this.lastPos) < 0.25) {
                                       AIPlayerBot this$0 = AIPlayerBot.this;
                                       this$0.pathStuckTicks++;
                                       if (AIPlayerBot.this.pathStuckTicks > 30) {
                                          AIPlayerBot.this.pathStuckTicks = 0;
                                          AIPlayerBot.this.lastStuckRotateTick = this.tick;
                                          float newYaw = botPlayer.getLocation().getYaw() + (float)(Math.random() > 0.5 ? 100 : -100);
                                          if (Math.random() > 0.6) {
                                             newYaw += 80.0F;
                                          }

                                          AIPlayerBot.this.nmsBot.setRotation(newYaw, botPlayer.getLocation().getPitch());
                                          AIPlayerBot.this.nmsBot.walkRelative(0.3, 0.0);
                                          AIPlayerBot.this.currentAIAction = "move_forward 0.3";
                                          AIPlayerBot.this.aiActionTicks = 0;
                                          CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Bot stuck — forcing move + rotate " + newYaw);
                                       }
                                    } else {
                                       AIPlayerBot.this.pathStuckTicks = 0;
                                    }

                                    if (this.tick % 20 == 0) {
                                       AIPlayerBot.this.lastPos = botPlayer.getLocation().clone();
                                    }

                                    AIPlayerBot.this.pickupNearbyItems(botPlayer);
                                    AIPlayerBot.this.manageInventory(botPlayer);
                                    CraftingPlanner.craftIfNeeded(botPlayer);
                                 }
                              }

                              this.tick++;
                           }
                        }
                     }
                  }
               } else {
                  this.cancel();
               }
            }
         })
         .runTaskTimer(CoreBootstrap.PLUGIN, 20L, 1L);
   }

   private boolean runAutonomousLogic(Player bot) {
      boolean playerOverride = System.currentTimeMillis() < this.playerCommandedUntil;
      if (this.burrowManager.isBurrowed()) {
         this.nmsBot.walkRelative(0.0, 0.0);
         bot.setSprinting(false);
         return true;
      } else if (bot.isDead()) {
         if (this.botMemory != null && this.botMemory.lastDeathTime < this.currentSpawnTime()) {
            String cause = this.lastDamageCause(bot);
            this.botMemory.recordDeath(cause, bot.getLocation(), System.currentTimeMillis());
            CoreBootstrap.PLUGIN
               .getLogger()
               .info("[AIPlayerBot] Learned: died to " + cause + " at " + this.formatLoc(bot.getLocation()) + " (total deaths: " + this.botMemory.totalDeaths);
         }

         return true;
      } else {
         if (this.botMemory != null && this.botMemory.isNearDanger(bot.getLocation(), 10.0)) {
            Location safe = this.botMemory.findNearestSafeSpot(bot.getLocation(), 50.0);
            if (safe != null) {
               this.currentGoal = AIPlayerBot.BotGoal.FLEE;
               this.goalTarget = safe;
               this.goalTimeout = 200;
               this.walkTo(bot, safe, 0.35);
               bot.setSprinting(true);
               return true;
            }
         }

         double fleeThreshold = this.botMemory != null && this.botMemory.learnedDontFightLowHealth ? 10.0 : 6.0;
         if (bot.getHealth() <= fleeThreshold) {
            this.currentGoal = AIPlayerBot.BotGoal.FLEE;
            this.goalTimeout = 80;
            Entity threat = this.cachedNearestThreat != null ? this.cachedNearestThreat : this.findNearestThreat(bot);
            if (threat != null) {
               this.fleeFrom(bot, threat);
               return true;
            }
         }

         if (this.combatManager.isInCombat()) {
            return true;
         } else {
            Entity nearestMonster = this.cachedNearestMonster != null ? this.cachedNearestMonster : this.findNearestMonsterInRange(bot, 3.5);
            if (nearestMonster == null
               || this.attackCooldown > 0
               || this.botMemory != null && this.botMemory.learnedRunFromCreepers && nearestMonster.getType().name().contains("CREEPER")) {
               if (this.attackCooldown > 0) {
                  this.attackCooldown--;
               }

               if (this.pickupNearbyItems(bot)) {
                  return true;
               } else {
                  Item nearestDrop = this.cachedNearestItem != null ? this.cachedNearestItem : this.findNearestDroppedItem(bot, 8.0);
                  if (nearestDrop != null) {
                     this.currentGoal = AIPlayerBot.BotGoal.PICKUP_ITEM;
                     this.goalTarget = nearestDrop.getLocation();
                     this.goalTimeout = 100;
                     if (this.walkTo(bot, this.goalTarget, 0.3)) {
                        this.pickupNearbyItems(bot);
                        this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                     }

                     return true;
                  } else {
                     this.tryPlaceBlock(bot);
                     this.manageInventory(bot);
                     CraftingPlanner.craftIfNeeded(bot);
                     if (this.currentGoal == AIPlayerBot.BotGoal.IDLE && this.goalTimeout-- <= 0) {
                        this.targetBlock = null;
                        this.targetEntity = null;
                     }

                     switch (this.currentGoal) {
                        case IDLE:
                           long worldTime = bot.getWorld().getTime();
                           boolean isNight = worldTime >= 13000L && worldTime <= 23000L;
                           if (isNight && this.botMemory != null && this.botMemory.learnedShelterAtNight) {
                              Location safe2 = this.botMemory.findNearestSafeSpot(bot.getLocation(), 40.0);
                              if (safe2 != null) {
                                 this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
                                 this.goalTarget = safe2;
                                 this.goalTimeout = 200;
                                 return true;
                              }
                           }

                           if (bot.getFoodLevel() < 8) {
                              Entity animal = this.cachedNearestAnimal != null ? this.cachedNearestAnimal : this.findNearestAnimal(bot, 16.0);
                              if (animal != null) {
                                 this.targetEntity = animal;
                                 this.currentGoal = AIPlayerBot.BotGoal.ATTACK_ENTITY;
                                 this.goalTimeout = 200;
                                 CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Hungry — hunting " + animal.getType().name());
                                 return true;
                              } else {
                                 Block foodCrop = this.findNearestFoodSource(bot, 16);
                                 if (foodCrop != null) {
                                    this.targetBlock = foodCrop;
                                    this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
                                    this.goalTimeout = 200;
                                    CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Hungry — harvesting " + foodCrop.getType().name());
                                    return true;
                                 } else {
                                    this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
                                    this.goalTimeout = 300;
                                    (this.goalTarget = bot.getLocation().add((Math.random() - 0.5) * 80.0, 0.0, (Math.random() - 0.5) * 80.0))
                                       .setY(bot.getLocation().getY());
                                    CoreBootstrap.PLUGIN.getLogger().fine("[AIPlayerBot] Hungry — exploring for food");
                                    return true;
                                 }
                              }
                           } else if (!this.hasTool(bot, Material.WOODEN_PICKAXE) && !this.hasTool(bot, Material.STONE_PICKAXE)) {
                              this.targetBlock = this.findNearestTree(bot, 16);
                              if (this.targetBlock != null) {
                                 this.currentGoal = AIPlayerBot.BotGoal.MINE_BLOCK;
                                 this.goalTimeout = 300;
                                 return true;
                              } else {
                                 this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
                                 this.goalTimeout = 400;
                                 (this.goalTarget = bot.getLocation().add((Math.random() - 0.5) * 120.0, 0.0, (Math.random() - 0.5) * 120.0))
                                    .setY(bot.getLocation().getY());
                                 CoreBootstrap.PLUGIN.getLogger().fine("[AIPlayerBot] No trees in 64 blocks — exploring wider for forest");
                                 return true;
                              }
                           } else if (Math.random() < 0.1) {
                              this.currentGoal = AIPlayerBot.BotGoal.EXPLORE;
                              this.goalTimeout = 100;
                              (this.goalTarget = bot.getLocation().add((Math.random() - 0.5) * 30.0, 0.0, (Math.random() - 0.5) * 30.0))
                                 .setY(bot.getLocation().getY());
                              return true;
                           } else {
                              this.wander(bot);
                              return false;
                           }
                        case MINE_BLOCK:
                           if (this.targetBlock != null && this.targetBlock.getType() != Material.AIR) {
                              double dist = bot.getLocation().distance(this.targetBlock.getLocation().add(0.5, 0.5, 0.5));
                              if (dist > 3.5) {
                                 double approachSpeed = dist < 6.0 ? 0.18 : 0.28;
                                 this.walkTo(bot, this.targetBlock.getLocation().add(0.5, 0.0, 0.5), approachSpeed);
                                 this.lookAt(bot, this.targetBlock.getLocation().add(0.5, 0.5, 0.5));
                              } else {
                                 this.lookAt(bot, this.targetBlock.getLocation().add(0.5, 0.5, 0.5));
                                 if (!this.nmsBot.isMining()) {
                                    int ticksNeeded = this.nmsBot.startMining(this.targetBlock);
                                    if (ticksNeeded == Integer.MAX_VALUE) {
                                       this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                                       this.targetBlock = null;
                                       return true;
                                    }
                                 }

                                 this.nmsBot.tickMining();
                                 if (this.nmsBot.isMining() && this.nmsBot.getMiningProgress() >= this.nmsBot.getMiningRequired()) {
                                    Material brokenType = this.targetBlock.getType();
                                    this.nmsBot.breakBlock(this.targetBlock);
                                    if (this.botMemory != null) {
                                       this.botMemory.recordBlockMined();
                                       String typeName = brokenType.name();
                                       if (typeName.contains("DIAMOND")
                                          || typeName.contains("IRON")
                                          || typeName.contains("COAL")
                                          || typeName.contains("GOLD")
                                          || typeName.contains("EMERALD")
                                          || typeName.contains("REDSTONE")
                                          || typeName.contains("LAPIS")) {
                                          this.botMemory.recordResourceFound(typeName, this.targetBlock.getLocation());
                                       }
                                    }

                                    this.nmsBot.cancelMining();
                                    this.targetBlock = null;
                                    this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                                 }
                              }

                              return true;
                           }

                           this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                           return true;
                        case ATTACK_ENTITY:
                           if (this.targetEntity == null || this.targetEntity.isDead()) {
                              this.targetEntity = this.findNearestAnimal(bot, 16.0);
                              if (this.targetEntity == null) {
                                 this.targetEntity = this.findNearestMonsterInRange(bot, 16.0);
                              }

                              if (this.targetEntity == null) {
                                 this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                                 return true;
                              }
                           }

                           double dist = bot.getLocation().distance(this.targetEntity.getLocation());
                           if (dist > 2.5) {
                              this.walkTo(bot, this.targetEntity.getLocation(), 0.3);
                              this.lookAt(bot, this.targetEntity.getLocation().add(0.0, 1.0, 0.0));
                           } else {
                              this.selectBestWeapon(bot);
                              this.lookAt(bot, this.targetEntity.getLocation().add(0.0, 1.0, 0.0));
                              if (this.attackCooldown <= 0) {
                                 this.nmsBot.swingMainHand();
                                 bot.attack(this.targetEntity);
                                 this.attackCooldown = 12;
                                 double strafe = Math.random() > 0.5 ? 0.3 : -0.3;
                                 this.nmsBot.walkRelative(0.2, strafe);
                              }
                           }

                           return true;
                        case FOLLOW_PLAYER:
                        case PICKUP_ITEM:
                        case EAT_FOOD:
                        case BUILD:
                        case CRAFT:
                        default:
                           return false;
                        case FLEE:
                           if (this.goalTimeout <= 0) {
                              this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                              bot.setSprinting(false);
                           }

                           return true;
                        case EXPLORE:
                           if (this.goalTarget == null) {
                              this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                              return true;
                           } else {
                              if (this.walkTo(bot, this.goalTarget, 0.25)) {
                                 this.currentGoal = AIPlayerBot.BotGoal.IDLE;
                                 this.goalTarget = null;
                              }

                              Block groundBlock = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                              if (bot.isOnGround() && groundBlock.getType().isSolid() && this.isBlocked(bot)) {
                                 this.nmsBot.jump();
                              }

                              return true;
                           }
                     }
                  }
               }
            } else {
               this.combatManager.tick(bot, this.nmsBot);
               return true;
            }
         }
      }
   }

   private boolean walkTo(Player bot, Location target, double speed) {
      Location botLoc = bot.getLocation();
      double dx = target.getX() - botLoc.getX();
      double dz = target.getZ() - botLoc.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 1.5) {
         this.nmsBot.cancelNavigation();
         this.currentPath.clear();
         this.pathTarget = null;
         return true;
      } else {
         long now = System.currentTimeMillis();
         boolean needRecompute = this.currentPath.isEmpty()
            || this.pathTarget == null
            || this.pathTarget.distance(target) > 2.0
            || now - this.lastPathCompute > 2000L;
         if (!needRecompute && !this.currentPath.isEmpty()) {
            double minDist = Double.MAX_VALUE;

            for (Location wp : this.currentPath) {
               double d = botLoc.distance(wp);
               if (d < minDist) {
                  minDist = d;
               }
            }

            if (minDist > 3.0) {
               needRecompute = true;
            }
         }

         if (needRecompute) {
            this.currentPath = BotPathfinder.findPath(botLoc, target);
            this.pathTarget = target.clone();
            this.lastPathCompute = now;
            if (!this.currentPath.isEmpty()) {
               CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] A* path: " + this.currentPath.size() + " waypoints.");
            }
         }

         if (!this.currentPath.isEmpty()) {
            while (!this.currentPath.isEmpty() && botLoc.distance(this.currentPath.get(0)) < 1.2) {
               this.currentPath.remove(0);
            }

            if (!this.currentPath.isEmpty()) {
               Location next = this.currentPath.get(0);
               double wdx = next.getX() - botLoc.getX();
               double wdz = next.getZ() - botLoc.getZ();
               float yaw = (float)Math.toDegrees(Math.atan2(-wdx, wdz));
               this.nmsBot.setRotation(yaw, 5.0F);
               if (this.isCliffAhead(bot, 1.2)) {
                  this.nmsBot.walkRelative(0.0, 0.0);
                  this.nmsBot.setRotation(botLoc.getYaw() + 100.0F, botLoc.getPitch());
                  this.currentPath.clear();
                  this.pathTarget = null;
                  CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Path waypoint is off a cliff — stopping and turning");
                  return false;
               }

               if (this.isHazardAhead(bot, 1.2)) {
                  this.nmsBot.walkRelative(0.0, 0.0);
                  this.nmsBot.setRotation(botLoc.getYaw() + 120.0F, botLoc.getPitch());
                  this.currentPath.clear();
                  this.pathTarget = null;
                  CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Path waypoint is hazard (water/lava) — turning around");
                  return false;
               }

               double wdist = Math.sqrt(wdx * wdx + wdz * wdz);
               if (wdist > 0.01) {
                  this.nmsBot.walkRelative(wdx / wdist * speed * 2.0, wdz / wdist * speed * 2.0);
               }

               if (next.getY() > botLoc.getY() && bot.isOnGround()) {
                  this.nmsBot.jump();
               }

               return false;
            }
         }

         if (this.isCliffAhead(bot, 1.5)) {
            this.nmsBot.walkRelative(0.0, 0.0);
            this.nmsBot.setRotation(botLoc.getYaw() + 110.0F, botLoc.getPitch());
            this.currentPath.clear();
            this.pathTarget = null;
            CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Direct path goes off a cliff — turning around");
            return false;
         } else if (this.isHazardAhead(bot, 1.5)) {
            this.nmsBot.walkRelative(0.0, 0.0);
            this.nmsBot.setRotation(botLoc.getYaw() + 130.0F, botLoc.getPitch());
            this.currentPath.clear();
            this.pathTarget = null;
            CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Direct path is hazard (water/lava) — turning around");
            return false;
         } else {
            float yaw2 = (float)Math.toDegrees(Math.atan2(-dx, dz));
            this.nmsBot.setRotation(yaw2, 5.0F);
            if (bot.isOnGround() && this.isBlocked(bot)) {
               Block blockAhead = bot.getLocation().add(bot.getLocation().getDirection().multiply(0.6)).getBlock();
               Block aboveAhead = blockAhead.getLocation().add(0.0, 1.0, 0.0).getBlock();
               if (blockAhead.getType().isSolid() && !aboveAhead.getType().isSolid()) {
                  this.nmsBot.jump();
               }
            }

            if (!this.nmsBot.isNavigating()) {
               this.nmsBot.navigateTo(target);
            }

            return false;
         }
      }
   }

   private double normalizeAngle(double angle) {
      while (angle > 180.0) {
         angle -= 360.0;
      }

      while (angle < -180.0) {
         angle += 360.0;
      }

      return angle;
   }

   private boolean isInteractable(Material type) {
      String n = type.name();
      return n.contains("DOOR")
         || n.contains("TRAPDOOR")
         || n.contains("CHEST")
         || n.contains("BARREL")
         || n.contains("LEVER")
         || n.contains("BUTTON")
         || n.contains("PRESSURE_PLATE")
         || n.contains("FURNACE")
         || n.contains("SMOKER")
         || n.contains("BLAST_FURNACE")
         || n.contains("CRAFTING_TABLE")
         || n.contains("ANVIL")
         || n.contains("ENCHANTING_TABLE")
         || n.contains("BED")
         || n.contains("NOTE_BLOCK")
         || n.contains("JUKEBOX")
         || n.contains("DISPENSER")
         || n.contains("DROPPER")
         || n.contains("HOPPER")
         || n.contains("SHULKER_BOX")
         || n.contains("END_PORTAL_FRAME")
         || n.contains("BEACON")
         || n.contains("BREWING_STAND")
         || n.contains("CAULDRON")
         || n.contains("COMPOSTER")
         || n.contains("LECTERN")
         || n.contains("LOOM")
         || n.contains("CARTOGRAPHY_TABLE")
         || n.contains("SMITHING_TABLE")
         || n.contains("GRINDSTONE")
         || n.contains("STONECUTTER")
         || n.contains("BELL")
         || n.contains("DAYLIGHT_DETECTOR")
         || n.contains("OBSERVER")
         || n.contains("REPEATER")
         || n.contains("COMPARATOR");
   }

   private void lookAt(Player bot, Location target) {
      Location botLoc = bot.getLocation();
      double dx = target.getX() - botLoc.getX();
      double dz = target.getZ() - botLoc.getZ();
      double dy = target.getY() - botLoc.getY();
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      double dist = Math.sqrt(dx * dx + dz * dz);
      float pitch = (float)Math.toDegrees(Math.atan2(-dy, dist));
      bot.setRotation(yaw, pitch);
   }

   private void fleeFrom(Player bot, Entity threat) {
      Location botLoc = bot.getLocation();
      Location threatLoc = threat.getLocation();
      double dx = botLoc.getX() - threatLoc.getX();
      double dz = botLoc.getZ() - threatLoc.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 0.01) {
         dx = 1.0;
         dz = 0.0;
      } else {
         dx /= dist;
         dz /= dist;
      }

      double angleAway = Math.toDegrees(Math.atan2(-dx, dz));
      double yawDiff = this.normalizeAngle(angleAway - (double)botLoc.getYaw());
      double forward = Math.cos(Math.toRadians(yawDiff));
      double sideways = -Math.sin(Math.toRadians(yawDiff));
      this.nmsBot.walkRelative(forward * 1.2, sideways * 1.2);
      bot.setSprinting(true);
   }

   private void autoAttack(Player bot, Entity target) {
      if (target instanceof Player targetPlayer) {
         long now = System.currentTimeMillis();
         boolean wasRecentlyAttacked = this.lastDamagedByPlayer != null
            && this.lastDamagedByPlayer.equals(targetPlayer)
            && now - this.lastPlayerDamageTime < 10000L;
         boolean emergency = bot.getHealth() <= 10.0;
         if (!wasRecentlyAttacked || !emergency) {
            return;
         }

         CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Emergency PvP: defending against " + targetPlayer.getName() + " (HP=" + bot.getHealth() + ")");
      }

      bot.attack(target);
      this.attackCooldown = 12;
      this.lookAt(bot, target.getLocation().add(0.0, 1.0, 0.0));
      double strafe = Math.random() > 0.5 ? 0.3 : -0.3;
      this.nmsBot.walkRelative(0.2, strafe);
   }

   public void recordPlayerDamage(Player attacker) {
      this.lastDamagedByPlayer = attacker;
      this.lastPlayerDamageTime = System.currentTimeMillis();
   }

   public Player getLastDamagedByPlayer() {
      return this.lastDamagedByPlayer;
   }

   private void refreshEntityCache(Player bot) {
      this.cachedNearbyEntities = bot.getNearbyEntities(8.0, 4.0, 8.0);
      this.lastEntityCacheTick = System.currentTimeMillis();
      this.cachedNearestMonster = null;
      this.cachedNearestThreat = null;
      this.cachedNearestAnimal = null;
      this.cachedNearestItem = null;
      double minMonsterDist = Double.MAX_VALUE;
      double minThreatDist = Double.MAX_VALUE;
      double minAnimalDist = Double.MAX_VALUE;
      double minItemDist = Double.MAX_VALUE;
      Location botLoc = bot.getLocation();

      for (Entity e : this.cachedNearbyEntities) {
         if (!e.equals(bot)) {
            String type = e.getType().name();
            double d = botLoc.distance(e.getLocation());
            if (type.contains("ZOMBIE")
               || type.contains("SKELETON")
               || type.contains("CREEPER")
               || type.contains("SPIDER")
               || type.contains("WITCH")
               || type.contains("PHANTOM")
               || type.contains("ENDERMAN")
               || type.contains("SLIME")
               || type.contains("HOGLIN")) {
               if (d < minMonsterDist) {
                  minMonsterDist = d;
                  this.cachedNearestMonster = e;
               }

               if (d < minThreatDist
                  && (
                     type.contains("ZOMBIE")
                        || type.contains("SKELETON")
                        || type.contains("SPIDER")
                        || type.contains("WITCH")
                        || type.contains("PHANTOM")
                        || type.contains("ENDERMAN")
                        || type.contains("HOGLIN")
                  )) {
                  minThreatDist = d;
                  this.cachedNearestThreat = e;
               }
            }

            if ((type.contains("COW") || type.contains("PIG") || type.contains("SHEEP") || type.contains("CHICKEN") || type.contains("RABBIT"))
               && d < minAnimalDist) {
               minAnimalDist = d;
               this.cachedNearestAnimal = e;
            }

            if (e instanceof Item) {
               Item item = (Item)e;
               if (d < minItemDist) {
                  minItemDist = d;
                  this.cachedNearestItem = item;
               }
            }
         }
      }
   }

   private boolean isBlocked(Player bot) {
      Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(0.8));
      Block block = ahead.getBlock();
      return block.getType().isSolid() && block.getType() != Material.AIR;
   }

   private boolean hasTool(Player bot, Material tool) {
      for (int i = 0; i < bot.getInventory().getSize(); i++) {
         ItemStack item = bot.getInventory().getItem(i);
         if (item != null && item.getType() == tool) {
            return true;
         }
      }

      return false;
   }

   private boolean hasItem(Player bot, Material material) {
      for (int i = 0; i < bot.getInventory().getSize(); i++) {
         ItemStack item = bot.getInventory().getItem(i);
         if (item != null && item.getType() == material) {
            return true;
         }
      }

      return false;
   }

   private Block findNearestBlock(Player bot, Material type, int range) {
      Block nearest = null;
      double minDist = Double.MAX_VALUE;
      Location loc = bot.getLocation();

      for (int x = -range; x <= range; x++) {
         for (int y = -range; y <= range; y++) {
            for (int z = -range; z <= range; z++) {
               Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
               if (b.getType() == type) {
                  double d = loc.distance(b.getLocation().add(0.5, 0.5, 0.5));
                  if (d < minDist) {
                     minDist = d;
                     nearest = b;
                  }
               }
            }
         }
      }

      return nearest;
   }

   private Block findNearestTree(Player bot, int range) {
      Block nearest = null;
      double minDist = Double.MAX_VALUE;
      Location loc = bot.getLocation();
      int yMin = Math.max(-range, -20);
      int yMax = Math.min(range, 30);

      for (int x = -range; x <= range; x++) {
         for (int y = yMin; y <= yMax; y++) {
            for (int z = -range; z <= range; z++) {
               Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
               if (this.isLog(b.getType()) && this.hasLeavesNearby(b, 5)) {
                  double d = loc.distance(b.getLocation().add(0.5, 0.5, 0.5));
                  if (d < minDist) {
                     minDist = d;
                     nearest = b;
                  }
               }
            }
         }
      }

      return nearest;
   }

   private Block findNearestStone(Player bot, int range) {
      Block nearest = null;
      double minDist = Double.MAX_VALUE;
      Location loc = bot.getLocation();

      for (int x = -range; x <= range; x++) {
         for (int y = -range; y <= range; y++) {
            for (int z = -range; z <= range; z++) {
               Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
               if (b.getType() == Material.STONE) {
                  double d = loc.distance(b.getLocation().add(0.5, 0.5, 0.5));
                  if (d < minDist) {
                     minDist = d;
                     nearest = b;
                  }
               }
            }
         }
      }

      return nearest;
   }

   private Block findNearestOre(Player bot, int range) {
      Block nearest = null;
      double minDist = Double.MAX_VALUE;
      Location loc = bot.getLocation();

      for (int x = -range; x <= range; x++) {
         for (int y = -range; y <= range; y++) {
            for (int z = -range; z <= range; z++) {
               Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
               if (b.getType() == Material.IRON_ORE || b.getType() == Material.COAL_ORE) {
                  double d = loc.distance(b.getLocation().add(0.5, 0.5, 0.5));
                  if (d < minDist) {
                     minDist = d;
                     nearest = b;
                  }
               }
            }
         }
      }

      return nearest;
   }

   private boolean isLog(Material type) {
      return type == Material.OAK_LOG
         || type == Material.BIRCH_LOG
         || type == Material.SPRUCE_LOG
         || type == Material.JUNGLE_LOG
         || type == Material.ACACIA_LOG
         || type == Material.DARK_OAK_LOG
         || type == Material.MANGROVE_LOG
         || type == Material.CHERRY_LOG;
   }

   private boolean hasLeavesNearby(Block log, int radius) {
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

   private String getDirection(int dx, int dz) {
      double angle = Math.toDegrees(Math.atan2((double)dx, (double)dz));
      if (angle < 0.0) {
         angle += 360.0;
      }

      String[] dirs = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      int idx = (int)Math.round(angle / 45.0) % 8;
      return dirs[idx];
   }

   private Entity findNearestAnimal(Player bot, double range) {
      List<Entity> nearby = bot.getNearbyEntities(range, range / 2.0, range);
      Entity nearest = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : nearby) {
         String type = e.getType().name();
         if (type.contains("COW") || type.contains("PIG") || type.contains("SHEEP") || type.contains("CHICKEN") || type.contains("RABBIT")) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private Item findNearestDroppedItem(Player bot, double range) {
      List<Entity> nearby = bot.getNearbyEntities(range, range, range);
      Item nearest = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : nearby) {
         if (e instanceof Item) {
            Item drop = (Item)e;
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               nearest = drop;
            }
         }
      }

      return nearest;
   }

   void autoEquipBestGear(Player bot) {
      PlayerInventory inv = bot.getInventory();
      Material[] helmetTiers = new Material[]{
         Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET
      };
      Material[] chestTiers = new Material[]{
         Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE
      };
      Material[] leggingsTiers = new Material[]{
         Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
      };
      Material[] bootsTiers = new Material[]{
         Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
      };
      Supplier<ItemStack> getter = inv::getHelmet;
      this.equipBestArmorPiece(inv, helmetTiers, getter, inv::setHelmet);
      Supplier<ItemStack> getter2 = inv::getChestplate;
      this.equipBestArmorPiece(inv, chestTiers, getter2, inv::setChestplate);
      Supplier<ItemStack> getter3 = inv::getLeggings;
      this.equipBestArmorPiece(inv, leggingsTiers, getter3, inv::setLeggings);
      Supplier<ItemStack> getter4 = inv::getBoots;
      this.equipBestArmorPiece(inv, bootsTiers, getter4, inv::setBoots);
   }

   private void equipBestArmorPiece(PlayerInventory inv, Material[] tiers, Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
      ItemStack current = getter.get();
      int currentTier = -1;
      if (current != null) {
         for (int i = 0; i < tiers.length; i++) {
            if (current.getType() == tiers[i]) {
               currentTier = i;
               break;
            }
         }
      }

      ItemStack best = null;
      int bestTier = currentTier;

      for (int j = 0; j < inv.getSize(); j++) {
         ItemStack item = inv.getItem(j);
         if (item != null) {
            for (int t = 0; t < tiers.length; t++) {
               if (item.getType() == tiers[t] && t > bestTier) {
                  bestTier = t;
                  best = item;
               }
            }
         }
      }

      if (best != null) {
         inv.removeItem(new ItemStack[]{best.clone()});
         if (current != null && current.getType() != Material.AIR) {
            inv.addItem(new ItemStack[]{current.clone()});
         }

         setter.accept(best.clone());
      }
   }

   public void onCombatStart(Player bot) {
      if (this.autoEquipper != null) {
         this.autoEquipper.onCombatStart(bot);
      }
   }

   void selectBestWeapon(Player bot) {
      PlayerInventory inv = bot.getInventory();
      Material[] weapons = new Material[]{
         Material.MACE,
         Material.NETHERITE_SWORD,
         Material.DIAMOND_SWORD,
         Material.IRON_SWORD,
         Material.STONE_SWORD,
         Material.WOODEN_SWORD,
         Material.GOLDEN_SWORD,
         Material.NETHERITE_AXE,
         Material.DIAMOND_AXE,
         Material.IRON_AXE,
         Material.STONE_AXE,
         Material.WOODEN_AXE,
         Material.GOLDEN_AXE,
         Material.NETHERITE_PICKAXE,
         Material.DIAMOND_PICKAXE,
         Material.IRON_PICKAXE,
         Material.STONE_PICKAXE,
         Material.WOODEN_PICKAXE,
         Material.GOLDEN_PICKAXE
      };

      for (int i = 0; i < 9; i++) {
         ItemStack item = inv.getItem(i);
         if (item != null) {
            for (Material w : weapons) {
               if (item.getType() == w) {
                  inv.setHeldItemSlot(i);
                  return;
               }
            }
         }
      }
   }

   private boolean autoEat(Player bot) {
      if (bot.getFoodLevel() >= 18) {
         return false;
      } else {
         PlayerInventory inv = bot.getInventory();
         int bestSlot = -1;
         int bestHunger = 0;

         for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType().isEdible()) {
               int hunger = this.getFoodValue(item.getType());
               if (hunger > bestHunger) {
                  bestHunger = hunger;
                  bestSlot = i;
               }
            }
         }

         if (bestSlot >= 0) {
            int hotbarSlot = bestSlot;
            if (bestSlot > 8) {
               ItemStack food = inv.getItem(bestSlot);
               ItemStack currentSlot1 = inv.getItem(1);
               inv.setItem(1, food);
               inv.setItem(bestSlot, currentSlot1);
               hotbarSlot = 1;
            }

            inv.setHeldItemSlot(hotbarSlot);
            ItemStack food = inv.getItem(hotbarSlot);
            if (food != null) {
               int newHunger = Math.min(20, bot.getFoodLevel() + bestHunger);
               bot.setFoodLevel(newHunger);
               food.setAmount(food.getAmount() - 1);
               if (food.getAmount() <= 0) {
                  inv.setItem(hotbarSlot, (ItemStack)null);
               }

               bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.5F, 1.0F);
               CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Ate " + food.getType().name() + " (hunger +" + bestHunger + ", now " + newHunger + "/20)");
               return true;
            }
         }

         return false;
      }
   }

   private boolean autoEatSmart(Player bot, boolean inCombat, boolean emergencyEat) {
      if (!inCombat) {
         boolean ate = this.autoEat(bot);
         if (ate) {
            this.eatCooldown = 15;
         }

         return ate;
      } else {
         PlayerInventory inv = bot.getInventory();
         int bestSlot = -1;

         for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
               Material type = item.getType();
               if (type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE) {
                  bestSlot = i;
                  break;
               }
            }
         }

         if (bestSlot >= 0) {
            int hotbarSlot = bestSlot;
            if (bestSlot > 8) {
               ItemStack food = inv.getItem(bestSlot);
               ItemStack currentSlot1 = inv.getItem(1);
               inv.setItem(1, food);
               inv.setItem(bestSlot, currentSlot1);
               hotbarSlot = 1;
            }

            inv.setHeldItemSlot(hotbarSlot);
            ItemStack food = inv.getItem(hotbarSlot);
            if (food != null) {
               bot.setFoodLevel(Math.min(20, bot.getFoodLevel() + 4));
               food.setAmount(food.getAmount() - 1);
               if (food.getAmount() <= 0) {
                  inv.setItem(hotbarSlot, (ItemStack)null);
               }

               bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.5F, 1.0F);
               String reason = emergencyEat ? "EMERGENCY low_health" : "combat";
               CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Ate GAPPLE reason=" + reason + " health=" + String.format("%.1f", bot.getHealth()));
               this.eatCooldown = 15;
               return true;
            }
         }

         if (emergencyEat) {
            boolean ate = this.autoEat(bot);
            if (ate) {
               CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Ate fallback food in emergency (no gapples)");
               this.eatCooldown = 15;
               return true;
            }
         }

         return false;
      }
   }

   private static boolean isWeaponMaterial(Material type) {
      if (type == null) {
         return false;
      } else {
         String name = type.name();
         return name.contains("SWORD") || name.contains("AXE") || name.contains("MACE") || name.contains("PICKAXE");
      }
   }

   private int getFoodValue(Material type) {
      String name = type.name();

      return switch (name) {
         case "GOLDEN_CARROT" -> 6;
         case "COOKED_BEEF", "COOKED_PORKCHOP", "COOKED_MUTTON", "COOKED_RABBIT" -> 8;
         case "BREAD", "COOKED_POTATO", "COOKED_CHICKEN", "COOKED_COD", "COOKED_SALMON" -> 5;
         case "APPLE", "CHORUS_FRUIT", "BEETROOT_SOUP", "MUSHROOM_STEW", "RABBIT_STEW" -> 4;
         case "CARROT", "MELON_SLICE" -> 3;
         case "SWEET_BERRIES", "GLOW_BERRIES", "COOKIE", "DRIED_KELP" -> 2;
         case "BEETROOT", "POTATO" -> 1;
         default -> type.isEdible() ? 3 : 0;
      };
   }

   private void tryWaterMLG(Player bot) {
      if (!this.combatManager.isInCombat()) {
         if (!(bot.getFallDistance() < 3.0F)) {
            if (!(bot.getVelocity().getY() > -0.3)) {
               PlayerInventory inv = bot.getInventory();
               int bucketSlot = -1;

               for (int i = 0; i < 9; i++) {
                  ItemStack item = inv.getItem(i);
                  if (item != null && item.getType() == Material.WATER_BUCKET) {
                     bucketSlot = i;
                     break;
                  }
               }

               if (bucketSlot >= 0) {
                  inv.setHeldItemSlot(bucketSlot);
                  Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                  if (below.getType().isSolid() || below.getType() == Material.AIR) {
                     this.nmsBot.placeBlock(below);
                     CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Water MLG!");
                  }
               }
            }
         }
      }
   }

   private Block findNearestFoodSource(Player bot, int range) {
      Block best = null;
      double bestDist = Double.MAX_VALUE;
      Location loc = bot.getLocation();
      if (range > 160) {
         range = 160;
      }

      Material[] foodBlocks = new Material[]{
         Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.MELON, Material.SWEET_BERRY_BUSH, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM
      };

      for (int x = -range; x <= range; x++) {
         for (int y = -3; y <= 3; y++) {
            for (int z = -range; z <= range; z++) {
               Block b = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);

               for (Material foodType : foodBlocks) {
                  if (b.getType() == foodType) {
                     double d = loc.distance(b.getLocation().add(0.5, 0.5, 0.5));
                     if (d < bestDist) {
                        bestDist = d;
                        best = b;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean pickupNearbyItems(Player bot) {
      for (Entity e : bot.getNearbyEntities(4.0, 4.0, 4.0)) {
         if (e instanceof Item) {
            Item drop = (Item)e;
            ItemStack stack = drop.getItemStack();
            if (stack != null && stack.getType() != Material.AIR) {
               PlayerInventory inv = bot.getInventory();
               if (inv.firstEmpty() >= 0 || inv.first(stack.getType()) >= 0) {
                  HashMap<Integer, ItemStack> leftover = inv.addItem(new ItemStack[]{stack.clone()});
                  if (leftover.isEmpty()) {
                     drop.remove();
                  } else {
                     int totalPicked = stack.getAmount();

                     for (ItemStack left : leftover.values()) {
                        totalPicked -= left.getAmount();
                     }

                     if (totalPicked > 0) {
                        stack.setAmount(stack.getAmount() - totalPicked);
                        drop.getItemStack().setAmount(stack.getAmount());
                        if (stack.getAmount() <= 0) {
                           drop.remove();
                        }
                     }
                  }

                  bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.0F + ((float)Math.random() - 0.5F) * 0.4F);
                  return true;
               }
            }
         }
      }

      return false;
   }

   private Entity findNearestMonsterInRange(Player bot, double range) {
      List<Entity> nearby = bot.getNearbyEntities(range, range / 2.0, range);
      Entity nearest = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : nearby) {
         String type = e.getType().name();
         if (type.contains("ZOMBIE")
            || type.contains("SKELETON")
            || type.contains("CREEPER")
            || type.contains("SPIDER")
            || type.contains("WITCH")
            || type.contains("PHANTOM")) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private void wander(Player bot) {
      Block ground = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
      if (ground.getType().isSolid()) {
         double angle = Math.random() * 2.0 * Math.PI;
         double speed = 0.18 + Math.random() * 0.12;
         Location test = bot.getLocation().clone();
         test.setYaw((float)Math.toDegrees(angle));
         Location ahead = test.add(test.getDirection().multiply(0.6));
         if (ahead.getBlock().getType().isSolid()) {
            angle++;
         }

         this.nmsBot.walkRelative(Math.cos(angle) * speed * 2.0, Math.sin(angle) * speed * 2.0);
         if (bot.isOnGround() && Math.random() < 0.08) {
            Location jumpAhead = bot.getLocation().add(bot.getLocation().getDirection().multiply(0.6));
            Block block = jumpAhead.getBlock();
            Block above = jumpAhead.clone().add(0.0, 1.0, 0.0).getBlock();
            if (block.getType().isSolid() && !above.getType().isSolid()) {
               this.nmsBot.jump();
            }
         }
      }
   }

   private void gatherPerception(Player botPlayer) {
      Location loc = botPlayer.getLocation();
      Block feetBlock = loc.getBlock();
      Block lookingAt = botPlayer.getTargetBlockExact(30);
      long worldTime = loc.getWorld().getTime();
      boolean isNight = worldTime >= 13000L && worldTime <= 23000L;
      StringBuilder sb = new StringBuilder();
      sb.append("health=")
         .append(String.format("%.1f", botPlayer.getHealth()))
         .append("/")
         .append(String.format("%.1f", botPlayer.getMaxHealth()))
         .append(", ");
      sb.append("food=").append(botPlayer.getFoodLevel()).append(", ");
      sb.append("saturation=").append(String.format("%.1f", botPlayer.getSaturation())).append(", ");
      sb.append("pos=[").append((int)loc.getX()).append(",").append((int)loc.getY()).append(",").append((int)loc.getZ()).append("], ");
      sb.append("biome=").append(loc.getBlock().getBiome().name()).append(", ");
      sb.append("time=").append(worldTime).append(isNight ? "(NIGHT)" : "(DAY)").append(", ");
      sb.append("onGround=").append(botPlayer.isOnGround()).append(", ");
      sb.append("sprinting=").append(botPlayer.isSprinting()).append(", ");
      sb.append("sneaking=").append(botPlayer.isSneaking()).append(", ");
      sb.append("feetBlock=").append(feetBlock.getType().name()).append(", ");
      sb.append("lookingAt=").append(lookingAt != null ? lookingAt.getType().name() : "air").append(", ");
      sb.append("experience=").append(botPlayer.getTotalExperience()).append(", ");
      int lightLevel = feetBlock.getLightLevel();
      sb.append("light=").append(lightLevel).append(", ");
      StringBuilder envScan = new StringBuilder();
      double nearestTreeDistSq = Double.MAX_VALUE;
      double nearestOreDistSq = Double.MAX_VALUE;
      double nearestWaterDistSq = Double.MAX_VALUE;
      double nearestLavaDistSq = Double.MAX_VALUE;
      int treeDx = 0;
      int treeDy = 0;
      int treeDz = 0;
      int oreDx = 0;
      int oreDy = 0;
      int oreDz = 0;
      int waterDx = 0;
      int waterDy = 0;
      int waterDz = 0;
      int lavaDx = 0;
      int lavaDy = 0;
      int lavaDz = 0;
      boolean cliffAhead = false;
      boolean holeAhead = false;
      World w = loc.getWorld();
      int bx = loc.getBlockX();
      int by = loc.getBlockY();
      int bz = loc.getBlockZ();

      for (int dx = -32; dx <= 32; dx += 8) {
         for (int dz = -32; dz <= 32; dz += 8) {
            for (int dy = -4; dy <= 6; dy++) {
               Block b = w.getBlockAt(bx + dx, by + dy, bz + dz);
               Material m = b.getType();
               double distSq = (double)(dx * dx + dy * dy + dz * dz);
               if (m.name().contains("WATER") && distSq < nearestWaterDistSq) {
                  nearestWaterDistSq = distSq;
                  waterDx = dx;
                  waterDz = dz;
               }

               if (m.name().contains("LAVA") && distSq < nearestLavaDistSq) {
                  nearestLavaDistSq = distSq;
                  lavaDx = dx;
                  lavaDz = dz;
               }

               if ((m.name().contains("LOG") || m.name().contains("WOOD") || m.name().contains("LEAVES")) && distSq < nearestTreeDistSq) {
                  nearestTreeDistSq = distSq;
                  treeDx = dx;
                  treeDz = dz;
               }

               if ((m.name().contains("ORE") || m.name().contains("DIAMOND") || m.name().contains("IRON") || m.name().contains("COAL"))
                  && distSq < nearestOreDistSq) {
                  nearestOreDistSq = distSq;
                  oreDx = dx;
                  oreDz = dz;
               }
            }
         }
      }

      float yaw = loc.getYaw();
      double rad = Math.toRadians((double)yaw);

      for (int d = 1; d <= 10; d++) {
         int cx = bx + (int)Math.round(-Math.sin(rad) * (double)d);
         int cz = bz + (int)Math.round(Math.cos(rad) * (double)d);
         Block ground = w.getBlockAt(cx, by - 1, cz);
         Block air = w.getBlockAt(cx, by, cz);
         if (ground.getType() == Material.AIR || ground.getType() == Material.VOID_AIR) {
            cliffAhead = true;
         }

         if (air.getType() == Material.AIR && w.getBlockAt(cx, by - 2, cz).getType() == Material.AIR) {
            holeAhead = true;
         }
      }

      if (nearestWaterDistSq < Double.MAX_VALUE) {
         envScan.append("water@")
            .append(String.format("%.0f", Math.sqrt(nearestWaterDistSq)))
            .append("m_")
            .append(this.getDirection(waterDx, waterDz))
            .append(" ");
      }

      if (nearestLavaDistSq < Double.MAX_VALUE) {
         envScan.append("lava@").append(String.format("%.0f", Math.sqrt(nearestLavaDistSq))).append("m_").append(this.getDirection(lavaDx, lavaDz)).append(" ");
      }

      if (nearestTreeDistSq < Double.MAX_VALUE) {
         envScan.append("trees@")
            .append(String.format("%.0f", Math.sqrt(nearestTreeDistSq)))
            .append("m_")
            .append(this.getDirection(treeDx, treeDz))
            .append(" ");
      }

      if (nearestOreDistSq < Double.MAX_VALUE) {
         envScan.append("ore@").append(String.format("%.0f", Math.sqrt(nearestOreDistSq))).append("m_").append(this.getDirection(oreDx, oreDz)).append(" ");
      }

      if (cliffAhead) {
         envScan.append("CLIFF_AHEAD ");
      }

      if (holeAhead) {
         envScan.append("HOLE_AHEAD ");
      }

      if (lightLevel < 8) {
         envScan.append("DARK ");
      }

      sb.append("env=[").append(envScan.toString().trim()).append("], ");
      sb.append("hotbar=[");

      for (int i = 0; i < 9; i++) {
         ItemStack item = botPlayer.getInventory().getItem(i);
         if (item != null && item.getType() != Material.AIR) {
            sb.append(i).append(":").append(item.getType().name()).append("x").append(item.getAmount()).append(" ");
         }
      }

      sb.append("], ");
      sb.append("armor=[");

      for (ItemStack armor : botPlayer.getInventory().getArmorContents()) {
         if (armor != null && armor.getType() != Material.AIR) {
            sb.append(armor.getType().name()).append(" ");
         }
      }

      sb.append("], ");
      List<Entity> nearby = botPlayer.getNearbyEntities(12.0, 6.0, 12.0);
      List<String> threats = new ArrayList<>();
      List<String> animals = new ArrayList<>();
      List<String> drops = new ArrayList<>();
      List<String> players = new ArrayList<>();

      for (Entity e : nearby) {
         String type = e.getType().name();
         double dist = botPlayer.getLocation().distance(e.getLocation());
         if (type.contains("ZOMBIE")
            || type.contains("SKELETON")
            || type.contains("CREEPER")
            || type.contains("SPIDER")
            || type.contains("WITCH")
            || type.contains("PHANTOM")
            || type.contains("ENDERMAN")
            || type.contains("SLIME")
            || type.contains("GUARDIAN")
            || type.contains("PILLAGER")
            || type.contains("VINDICATOR")
            || type.contains("HUSK")
            || type.contains("DROWNED")
            || type.contains("STRAY")
            || type.contains("EVOKER")) {
            threats.add(type + "@" + String.format("%.1f", dist));
         } else if (type.contains("COW")
            || type.contains("PIG")
            || type.contains("SHEEP")
            || type.contains("CHICKEN")
            || type.contains("RABBIT")
            || type.contains("HORSE")) {
            animals.add(type + "@" + String.format("%.1f", dist));
         } else if (type.contains("DROPPED_ITEM")) {
            drops.add(type + "@" + String.format("%.1f", dist));
         } else if (type.equals("PLAYER") && e != botPlayer) {
            players.add(((Player)e).getName() + "@" + String.format("%.1f", dist));
         }
      }

      if (!threats.isEmpty()) {
         sb.append("THREATS=").append(threats).append(", ");
      }

      if (!animals.isEmpty()) {
         sb.append("animals=").append(animals).append(", ");
      }

      if (!drops.isEmpty()) {
         sb.append("drops=").append(drops).append(", ");
      }

      if (!players.isEmpty()) {
         sb.append("players=").append(players).append(", ");
      }

      sb.append("weather=").append(loc.getWorld().hasStorm() ? "storm" : "clear");
      this.perceptionLog.offer(sb.toString());
      if (this.perceptionLog.size() > 15) {
         this.perceptionLog.poll();
      }
   }

   private void scanEnvironmentAhead(Player bot) {
      Location loc = bot.getLocation();
      World w = loc.getWorld();
      float yaw = loc.getYaw();
      double rad = Math.toRadians((double)yaw);
      double sin = -Math.sin(rad);
      double cos = Math.cos(rad);
      int bx = loc.getBlockX();
      int by = loc.getBlockY();
      int bz = loc.getBlockZ();
      StringBuilder env = new StringBuilder();
      boolean cliff = false;
      boolean lava = false;
      boolean water = false;
      boolean ore = false;
      boolean tree = false;
      boolean mob = false;
      int groundDrop = 0;

      for (int d = 1; d <= 5; d++) {
         int fx = bx + (int)Math.round(sin * (double)d);
         int fz = bz + (int)Math.round(cos * (double)d);

         for (int dy = -1; dy <= 1; dy++) {
            for (int side = -1; side <= 1; side++) {
               int lx = fx + (int)Math.round(cos * (double)side);
               int lz = fz + (int)Math.round(sin * (double)side);
               Block b = w.getBlockAt(lx, by + dy, lz);
               Material m = b.getType();
               if (m == Material.LAVA) {
                  lava = true;
               }

               if (m == Material.WATER) {
                  water = true;
               }

               if (m.name().contains("ORE") || m == Material.DIAMOND_ORE || m == Material.IRON_ORE || m == Material.COAL_ORE || m == Material.GOLD_ORE) {
                  ore = true;
               }

               if (m.name().contains("LOG") || m.name().contains("WOOD") || m.name().contains("LEAVES")) {
                  tree = true;
               }

               if (m == Material.AIR && dy == -1) {
                  Block below1 = w.getBlockAt(lx, by - 2, lz);
                  Block below2 = w.getBlockAt(lx, by - 3, lz);
                  if (below1.getType() == Material.AIR || below2.getType() == Material.AIR) {
                     cliff = true;
                     groundDrop = Math.max(groundDrop, 2);
                  }
               }
            }
         }
      }

      for (Entity e : bot.getNearbyEntities(5.0, 3.0, 5.0)) {
         if (e instanceof Monster) {
            Location el = e.getLocation();
            double dx = el.getX() - loc.getX();
            double dz = el.getZ() - loc.getZ();
            double dot = dx * sin + dz * cos;
            if (dot > 0.5) {
               mob = true;
               break;
            }
         }
      }

      if (cliff) {
         env.append("cliff_").append(groundDrop).append("m ");
      }

      if (lava) {
         env.append("lava ");
      }

      if (water) {
         env.append("water ");
      }

      if (ore) {
         env.append("ore ");
      }

      if (tree) {
         env.append("tree ");
      }

      if (mob) {
         env.append("hostile ");
      }

      if (env.length() == 0) {
         env.append("clear");
      }

      if (this.botMemory != null) {
         this.botMemory.environmentAhead = env.toString().trim();
      }
   }

   private int parseActionDuration(String action) {
      String[] parts = action.split(" ");
      if (parts.length >= 2) {
         try {
            int ticks = Integer.parseInt(parts[1]);
            return Math.min(ticks, 100);
         } catch (NumberFormatException var4) {
         }
      }

      return 1;
   }

   private void executeActionStart(Player bot, String action) {
      String[] parts = action.split(" ");
      String cmd = parts[0].toLowerCase();

      try {
         switch (cmd) {
            case "jump":
               if (bot.isOnGround()) {
                  this.nmsBot.jump();
               }
               break;
            case "attack":
               Entity target = this.findNearestMonsterInRange(bot, 4.0);
               if (target != null) {
                  this.selectBestWeapon(bot);
                  this.nmsBot.swingMainHand();
                  this.autoAttack(bot, target);
               }
               break;
            case "interact":
               Block block = bot.getTargetBlockExact(4);
               if (block != null && this.isInteractable(block.getType())) {
                  this.nmsBot.placeBlock(block);
               }
               break;
            case "select_slot":
               if (parts.length >= 2) {
                  int slot = Math.min(8, Math.max(0, Integer.parseInt(parts[1])));
                  this.nmsBot.selectHotbarSlot(slot);
               }
               break;
            case "place_block":
               this.tryPlaceBlock(bot);
            case "eat":
               ItemStack hand = bot.getInventory().getItemInMainHand();
               if (hand != null && hand.getType().isEdible()) {
                  bot.setSprinting(false);
               }
               break;
            case "sprint":
               bot.setSprinting(parts.length > 1 && "on".equalsIgnoreCase(parts[1]));
               break;
            case "sneak":
               bot.setSneaking(parts.length > 1 && "on".equalsIgnoreCase(parts[1]));
               break;
            case "stop":
               this.nmsBot.cancelNavigation();
               this.nmsBot.walkRelative(0.0, 0.0);
               bot.setSprinting(false);
               break;
            case "flee":
               Entity threat = this.findNearestThreat(bot);
               if (threat != null) {
                  this.fleeFrom(bot, threat);
               }
               break;
            case "say":
               if (parts.length > 1) {
                  String msg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                  bot.chat(msg);
               }
               break;
            case "look":
               if (parts.length >= 3) {
                  float yaw = Float.parseFloat(parts[1]);
                  float pitch = Float.parseFloat(parts[2]);
                  this.nmsBot.setRotation(yaw, pitch);
               }
               break;
            case "drop":
               if (parts.length >= 2) {
                  ItemStack item = bot.getInventory().getItem(Integer.parseInt(parts[1]));
                  if (item != null && item.getType() != Material.AIR) {
                     this.nmsBot.dropItemsOfType(item.getType(), 64);
                  }
               }
               break;
            case "pickup":
               this.pickupNearbyItems(bot);
         }
      } catch (Exception var10) {
         CoreBootstrap.PLUGIN.getLogger().fine("[AIPlayerBot] Action start failed: " + action + " - " + var10.getMessage());
      }
   }

   private void executeContinuousAction(Player bot, String action) {
      String[] parts = action.split(" ");
      String cmd = parts[0].toLowerCase();
      double speed = 0.25;
      if (parts.length >= 2) {
         try {
            speed = Double.parseDouble(parts[1]);
         } catch (Exception var20) {
         }
      }

      try {
         switch (cmd) {
            case "move_forward":
               if (this.isCliffAhead(bot, 1.5)) {
                  this.nmsBot.walkRelative(0.0, 0.0);
                  this.currentAIAction = null;
                  this.aiActionTicks = 0;
                  this.nmsBot.setRotation((float)((double)(bot.getLocation().getYaw() + 110.0F) + Math.random() * 40.0), bot.getLocation().getPitch());
                  CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Cliff detected — turning around");
                  return;
               }

               this.nmsBot.walkRelative(speed, 0.0);
               this.autoJumpOverObstacle(bot);
               break;
            case "move_backward":
               this.nmsBot.walkRelative(-speed, 0.0);
               this.autoJumpOverObstacle(bot);
               break;
            case "strafe_left":
               this.nmsBot.walkRelative(0.0, speed);
               this.autoJumpOverObstacle(bot);
               break;
            case "strafe_right":
               this.nmsBot.walkRelative(0.0, -speed);
               this.autoJumpOverObstacle(bot);
               break;
            case "mine":
               String s2;
               String dir = s2 = parts.length > 1 ? parts[1] : "forward";

               Block target = switch (s2) {
                  case "up" -> bot.getLocation().add(0.0, 1.0, 0.0).getBlock();
                  case "down" -> bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                  default -> bot.getTargetBlockExact(4);
               };
               if (target != null && target.getType() != Material.AIR && target.getType() != Material.BEDROCK) {
                  this.nmsBot.swingMainHand();
                  Material preBreakType = target.getType();
                  BlockData preBreakData = target.getBlockData();
                  target.breakNaturally(bot.getInventory().getItemInMainHand());
               } else if (dir.equals("forward")) {
                  this.currentAIAction = null;
                  this.aiActionTicks = 0;
               }
               break;
            case "follow":
               if (parts.length > 1) {
                  Entity target2 = this.findNearestEntity(bot, parts[1]);
                  if (target2 != null) {
                     Location botLoc = bot.getLocation();
                     Location targetLoc = target2.getLocation();
                     double dx = targetLoc.getX() - botLoc.getX();
                     double dz = targetLoc.getZ() - botLoc.getZ();
                     double dist = Math.sqrt(dx * dx + dz * dz);
                     if (dist > 1.5) {
                        this.nmsBot.walkRelative(dx / dist * speed * 2.0, dz / dist * speed * 2.0);
                     }

                     this.autoJumpOverObstacle(bot);
                  }
               }
               break;
            case "flee":
               Entity threat = this.findNearestThreat(bot);
               if (threat != null) {
                  this.fleeFrom(bot, threat);
               }
         }
      } catch (Exception var21) {
         CoreBootstrap.PLUGIN.getLogger().fine("[AIPlayerBot] Continuous action failed: " + cmd);
      }
   }

   private void autoJumpOverObstacle(Player bot) {
      if (bot.isOnGround()) {
         Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(0.6));
         Block block = ahead.getBlock();
         Block above = ahead.clone().add(0.0, 1.0, 0.0).getBlock();
         if (block.getType().isSolid() && !above.getType().isSolid()) {
            this.nmsBot.jump();
         }
      }
   }

   private void executeAIAction(Player bot, String action) {
      if (this.aiActionTicks == 0) {
         this.executeActionStart(bot, action);
      }

      this.executeContinuousAction(bot, action);
   }

   private boolean isCliffAhead(Player bot, double distance) {
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

   private boolean isHazardAhead(Player bot, double distance) {
      Location ahead = bot.getLocation().add(bot.getLocation().getDirection().multiply(distance));
      Block ground = ahead.clone().subtract(0.0, 1.0, 0.0).getBlock();
      Material type = ground.getType();
      String name = type.name();
      return name.contains("WATER") || name.contains("LAVA") || name.contains("CAVE") || name.contains("VOID");
   }

   private Entity findNearestThreat(Player bot) {
      List<Entity> nearby = bot.getNearbyEntities(16.0, 8.0, 16.0);
      Entity nearest = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : nearby) {
         String type = e.getType().name();
         if (type.contains("ZOMBIE")
            || type.contains("SKELETON")
            || type.contains("CREEPER")
            || type.contains("SPIDER")
            || type.contains("WITCH")
            || type.contains("PHANTOM")) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private Entity findNearestEntity(Player bot, String typeName) {
      List<Entity> nearby = bot.getNearbyEntities(16.0, 8.0, 16.0);
      Entity nearest = null;
      double minDist = Double.MAX_VALUE;

      for (Entity e : nearby) {
         if (e.getType().name().toLowerCase().contains(typeName.toLowerCase())) {
            double d = bot.getLocation().distance(e.getLocation());
            if (!(d >= minDist)) {
               minDist = d;
               nearest = e;
            }
         }
      }

      return nearest;
   }

   private String lastDamageCause(Player bot) {
      Entity threat = this.findNearestThreat(bot);
      if (threat != null) {
         return threat.getType().name().toLowerCase();
      } else {
         Block feet = bot.getLocation().getBlock();
         if (feet.getType() == Material.LAVA || feet.getType() == Material.FIRE) {
            return "lava/fire";
         } else if (feet.getType() == Material.WATER) {
            return "drowning";
         } else {
            return bot.getLocation().getY() < 0.0 ? "void" : "unknown";
         }
      }
   }

   private long currentSpawnTime() {
      return this.botMemory != null ? this.botMemory.currentSpawnTime : System.currentTimeMillis();
   }

   private String formatLoc(Location loc) {
      return String.format("[%.0f, %.0f, %.0f]", loc.getX(), loc.getY(), loc.getZ());
   }

   private void manageInventory(Player bot) {
      PlayerInventory inv = bot.getInventory();
      int used = 0;

      for (int i = 0; i < inv.getSize(); i++) {
         if (inv.getItem(i) != null) {
            used++;
         }
      }

      if (!((double)used < (double)inv.getSize() * 0.85)) {
         Material[] junkTypes = new Material[]{
            Material.COBBLESTONE,
            Material.DIRT,
            Material.GRAVEL,
            Material.SAND,
            Material.NETHERRACK,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.OAK_PLANKS,
            Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS,
            Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS,
            Material.CHERRY_PLANKS,
            Material.BAMBOO_PLANKS,
            Material.STICK
         };
         int[] keepAmounts = new int[]{64, 32, 16, 16, 32, 16, 16, 16, 128, 128, 128, 128, 128, 128, 128, 128, 128, 64};

         for (int j = 0; j < junkTypes.length; j++) {
            this.nmsBot.dropItemsOfType(junkTypes[j], keepAmounts[j]);
         }

         CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Inventory cleanup triggered (" + used + "/" + inv.getSize() + " slots used)");
      }
   }

   private void tryPlaceBlock(Player bot) {
      if (bot.isOnGround()) {
         double velY = bot.getVelocity().getY();
         if (!(velY < -0.1)) {
            Block below = bot.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
            Material belowType = below.getType();
            boolean isDeadly = belowType.name().contains("LAVA")
               || belowType == Material.VOID_AIR
               || belowType == Material.AIR && bot.getLocation().getY() < 5.0;
            if (isDeadly) {
               if (WorldProtection.isProtected(bot.getLocation())) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Emergency placement BLOCKED — inside spawn protection");
               } else {
                  for (int i = 0; i < 9; i++) {
                     ItemStack item = bot.getInventory().getItem(i);
                     if (item != null && item.getType().isBlock() && item.getAmount() > 0) {
                        Material blockType = item.getType();
                        if (WorldProtection.isAllowedToPlace(blockType)) {
                           if (BlockPlacementRules.canPlaceHere(bot.getLocation(), below.getLocation(), blockType)) {
                              this.nmsBot.selectHotbarSlot(i);
                              this.nmsBot.placeBlock(below);
                              CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] EMERGENCY block placed over " + belowType.name());
                              return;
                           }

                           CoreBootstrap.PLUGIN.getLogger().warning("[AIPlayerBot] Emergency placement BLOCKED — violates build rules");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void debugBotState(Player bot) {
      Location loc = bot.getLocation();
      StringBuilder sb = new StringBuilder();
      sb.append("[AI-DEBUG] ");
      sb.append("POS=").append(String.format("%.0f,%.0f,%.0f", loc.getX(), loc.getY(), loc.getZ()));
      sb.append(" GOAL=").append(this.goalPlanner.getCurrentGoal());
      sb.append(" HP=").append(String.format("%.0f", bot.getHealth()));
      sb.append(" FOOD=").append(bot.getFoodLevel());
      sb.append(" INV=[W")
         .append(this.goalPlanner.woodCount)
         .append(" P")
         .append(this.goalPlanner.plankCount)
         .append(" S")
         .append(this.goalPlanner.stickCount)
         .append(" St")
         .append(this.goalPlanner.stoneCount)
         .append("]");
      sb.append(" TIME=").append(bot.getWorld().getTime() >= 13000L ? "NIGHT" : "DAY");
      CoreBootstrap.PLUGIN.getLogger().fine(sb.toString());
   }

   private static enum BotGoal {
      IDLE,
      MINE_BLOCK,
      ATTACK_ENTITY,
      FOLLOW_PLAYER,
      PICKUP_ITEM,
      EAT_FOOD,
      FLEE,
      BUILD,
      CRAFT,
      EXPLORE;
   }
}
