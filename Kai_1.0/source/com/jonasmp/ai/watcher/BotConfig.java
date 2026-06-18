package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class BotConfig {
   private static final File CONFIG_DIR = new File(CoreBootstrap.PLUGIN.getDataFolder(), "bot_config");
   private static FileConfiguration behavior;
   private static FileConfiguration goals;
   private static FileConfiguration debug;
   private static FileConfiguration inventory;
   private static FileConfiguration prompts;

   public static void reload() {
      if (!CONFIG_DIR.exists()) {
         CONFIG_DIR.mkdirs();
      }

      behavior = loadOrCreate(
         "behavior.yml",
         "# AI Bot Movement & Behavior Config\nmovement:\n  speed: 0.28          # walk speed multiplier\n  sprint_speed: 0.5   # sprint speed multiplier\n  auto_jump: true     # auto-jump over 1-block obstacles\n  stuck_threshold_ticks: 10  # stuck detection threshold\n  stuck_random_turn: true    # try sideways when stuck\npickup:\n  range: 3.0          # item pickup radius\n  check_interval_ticks: 5   # how often to check for items\n  prioritize_drops: true     # walk toward drops before continuing goal\npathfinding:\n  max_tree_distance: 30     # max range to search for trees\n  max_stone_distance: 25    # max range to search for stone\n  max_animal_distance: 25  # max range to search for food\n  wander_range: 40         # max wander target distance\n"
      );
      goals = loadOrCreate(
         "goals.yml",
         "# AI Bot Goal Config\ndefault: IDLE\nnight_behavior: SURVIVE_NIGHT  # auto-switch to this at night\nlow_health_behavior: FLEE_DANGER\nhealth_threshold: 0.35         # health % to trigger flee\nallowed:\n  - GATHER_WOOD\n  - GATHER_STONE\n  - CRAFT_BASIC_TOOLS\n  - MINE_SURFACE_ORES\n  - DIG_FOR_DIAMONDS\n  - BUILD_SHELTER\n  - FIND_FOOD\n  - EXPLORE\n  - SURVIVE_NIGHT\n  - FLEE_DANGER\n  - SMELT_ORES\n  - DEEP_MINE\n  - COOK_FOOD\n  - BUILD_BASIC_HOUSE\n  - BRIDGE_GAP\n  - FILL_HOLE\n  - PLACE_TORCHES\n  - FARM_CROPS\n  - BREED_ANIMALS\n  - SHEAR_SHEEP\n  - TRADE_VILLAGER\n  - ENCHANT_GEAR\n  - BREW_POTIONS\n  - BUILD_NETHER_PORTAL\n  - EXPLORE_END\npriority:\n  flee: 100\n  survive_night: 90\n  find_food: 80\n  gather_wood: 70\n  craft_tools: 60\n  gather_stone: 50\n  mine_ores: 40\n  build_shelter: 30\n  explore: 10\n"
      );
      debug = loadOrCreate(
         "debug.yml",
         "# AI Bot Debug Config\ninterval_ticks: 40          # how often to print debug block (2 sec)\nconsole_output: true        # print to server console\nlog_to_file: false          # write debug to bot_config/debug.log\nperception_range: 5         # block range for perception scan\n"
      );
      inventory = loadOrCreate(
         "inventory.yml",
         "# AI Bot Inventory Config\nkeep_inventory_on_death: true   # KeepInventory (no grave drops)\nauto_drop_excess: false         # auto drop items if full\ncrafting:\n  auto_craft_pickaxe: true      # auto craft wooden/stone pickaxe\n  auto_craft_sword: true         # auto craft sword when possible\n  auto_craft_axe: true           # auto craft axe when possible\nhotbar:\n  slot_0: weapon    # sword/axe/pickaxe priority\n  slot_1: blocks    # placeable blocks\n  slot_2: food      # food items\n"
      );
      prompts = loadOrCreate(
         "prompts.yml",
         "# AI Bot Prompts & Personality Config (editable at runtime, reload with /watcher ai reload)\nsystem:\n  identity: \"You are Kai, a sentient Minecraft AI companion.\"\n  personality: \"Friendly, cautious, eager to learn.\"\n  language: \"de\"  # de or en\n\nbehavior_rules:\n  - \"Priority 1: SURVIVE (health > food > shelter)\"\n  - \"Priority 2: Gather resources (wood -> stone -> iron -> diamond)\"\n  - \"Priority 3: Build a safe base\"\n  - \"Night = DANGER. Dig down 2 blocks and cover head.\"\n  - \"Low health (< 6): EAT FOOD immediately or FLEE\"\n  - \"Hunger < 6: Find food NOW\"\n  - \"Monsters nearby: Equip weapon (slot 0) or RUN if low health\"\n\nlearning:\n  reinforce_on_death: true\n  reinforce_on_kill: true\n  max_lessons: 30\n  max_instructions: 50\n"
      );
   }

   private static FileConfiguration loadOrCreate(String fileName, String defaultContent) {
      File file = new File(CONFIG_DIR, fileName);
      if (!file.exists()) {
         try {
            Files.write(file.toPath(), defaultContent.getBytes(StandardCharsets.UTF_8));
         } catch (IOException var4) {
            CoreBootstrap.PLUGIN.getLogger().warning("[BotConfig] Failed to create " + fileName + ": " + var4.getMessage());
         }
      }

      return YamlConfiguration.loadConfiguration(file);
   }

   public static String getString(String path, String def) {
      return behavior.getString(path, goals.getString(path, debug.getString(path, inventory.getString(path, prompts.getString(path, def)))));
   }

   public static int getInt(String path, int def) {
      return behavior.getInt(path, goals.getInt(path, debug.getInt(path, inventory.getInt(path, prompts.getInt(path, def)))));
   }

   public static double getDouble(String path, double def) {
      return behavior.getDouble(path, goals.getDouble(path, debug.getDouble(path, inventory.getDouble(path, prompts.getDouble(path, def)))));
   }

   public static boolean getBoolean(String path, boolean def) {
      return behavior.getBoolean(path, goals.getBoolean(path, debug.getBoolean(path, inventory.getBoolean(path, prompts.getBoolean(path, def)))));
   }

   public static List<String> getStringList(String path) {
      if (prompts.contains(path)) {
         return prompts.getStringList(path);
      } else if (behavior.contains(path)) {
         return behavior.getStringList(path);
      } else if (goals.contains(path)) {
         return goals.getStringList(path);
      } else if (debug.contains(path)) {
         return debug.getStringList(path);
      } else {
         return (List<String>)(inventory.contains(path) ? inventory.getStringList(path) : new ArrayList<>());
      }
   }

   static {
      reload();
   }
}
