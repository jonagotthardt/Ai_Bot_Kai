package com.jonasmp.ai.watcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class WatcherCommands implements CommandExecutor, TabCompleter {
   private static final List<String> SUB_COMMANDS = Arrays.asList(
      "status",
      "spawn",
      "despawn",
      "reload",
      "observe",
      "investigate",
      "visible",
      "invisible",
      "freeze",
      "unfreeze",
      "idle",
      "lore",
      "tp",
      "patrol",
      "horror",
      "herobrine",
      "log",
      "help",
      "ambient",
      "stalk",
      "cave",
      "broken",
      "script",
      "toggle",
      "ai"
   );
   private static final List<String> AI_SUBS = Arrays.asList(
      "spawn", "despawn", "status", "goal", "info", "tp", "inv", "bridge", "debug", "config", "analyze", "reports", "reload", "memory", "forget"
   );
   private static final List<String> GOAL_TYPES = Arrays.asList(
      "IDLE",
      "GATHER_WOOD",
      "GATHER_STONE",
      "CRAFT_BASIC_TOOLS",
      "MINE_SURFACE_ORES",
      "DIG_FOR_DIAMONDS",
      "BUILD_SHELTER",
      "FIND_FOOD",
      "EXPLORE",
      "SURVIVE_NIGHT",
      "FLEE_DANGER",
      "SMELT_ORES",
      "DEEP_MINE",
      "COOK_FOOD",
      "BUILD_BASIC_HOUSE",
      "BRIDGE_GAP",
      "FILL_HOLE",
      "PLACE_TORCHES",
      "FARM_CROPS",
      "BREED_ANIMALS",
      "SHEAR_SHEEP",
      "TRADE_VILLAGER",
      "ENCHANT_GEAR",
      "BREW_POTIONS",
      "BUILD_NETHER_PORTAL",
      "EXPLORE_END"
   );
   private static final List<String> CONFIG_ACTIONS = Arrays.asList("reload", "show");

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length < 1) {
         this.sendHelp(sender);
         return true;
      } else {
         String sub = args[0].toLowerCase();
         WatcherCore core = WatcherCore.getInstance();
         switch (sub) {
            case "status":
               this.cmdStatus(sender, core);
               break;
            case "spawn":
               this.cmdSpawn(sender, core);
               break;
            case "despawn":
               this.cmdDespawn(sender, core);
               break;
            case "reload":
               this.cmdReload(sender, core);
               break;
            case "observe":
               this.cmdObserve(sender, args, core);
               break;
            case "investigate":
               this.cmdInvestigate(sender, args, core);
               break;
            case "visible":
               this.cmdVisible(sender, args, core);
               break;
            case "invisible":
               this.cmdInvisible(sender, core);
               break;
            case "freeze":
               this.cmdFreeze(sender, args, core);
               break;
            case "unfreeze":
               this.cmdUnfreeze(sender, args, core);
               break;
            case "idle":
               this.cmdIdle(sender, core);
               break;
            case "lore":
               this.cmdLore(sender, args, core);
               break;
            case "tp":
               this.cmdTp(sender, args, core);
               break;
            case "patrol":
               this.cmdPatrol(sender, core);
               break;
            case "horror":
               this.cmdHorror(sender, args, core);
               break;
            case "herobrine":
               this.cmdHerobrine(sender, args, core);
               break;
            case "log":
               this.cmdLog(sender, args, core);
               break;
            case "ambient":
               this.cmdAmbient(sender, args, core);
               break;
            case "stalk":
               this.cmdStalk(sender, args, core);
               break;
            case "cave":
               this.cmdCave(sender, args, core);
               break;
            case "broken":
               this.cmdBroken(sender, args, core);
               break;
            case "script":
               this.cmdScript(sender, args, core);
               break;
            case "toggle":
               this.cmdToggle(sender, args, core);
               break;
            case "ai":
               this.cmdAi(sender, args, core);
               break;
            case "help":
            case "?":
               this.sendHelp(sender);
               break;
            default:
               sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /watcher help");
         }

         return true;
      }
   }

   private void cmdStatus(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         sender.sendMessage(ChatColor.GOLD + "===== Watcher Status =====");
         sender.sendMessage(ChatColor.YELLOW + "State: " + ChatColor.WHITE + core.getState());
         sender.sendMessage(ChatColor.YELLOW + "Bot spawned: " + ChatColor.WHITE + core.getBot().isSpawned());
         sender.sendMessage(ChatColor.YELLOW + "Bot name: " + ChatColor.WHITE + core.getConfig().getWatcherName());
         sender.sendMessage(
            ChatColor.YELLOW + "Assigned player: " + ChatColor.WHITE + (core.getAssignedPlayer() != null ? Bukkit.getPlayer(core.getAssignedPlayer()) : "none")
         );
         sender.sendMessage(ChatColor.YELLOW + "Tracked players: " + ChatColor.WHITE + core.getAllPlayerData().size());
         sender.sendMessage(ChatColor.YELLOW + "Lore mode: " + ChatColor.WHITE + core.getConfig().isLoreModeEnabled());
      }
   }

   private void cmdSpawn(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (core.isRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "Watcher already active.");
         } else {
            core.init();
            sender.sendMessage(ChatColor.GREEN + "Watcher spawned and active.");
         }
      }
   }

   private void cmdDespawn(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         core.shutdown();
         sender.sendMessage(ChatColor.GREEN + "Watcher despawned.");
      }
   }

   private void cmdReload(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         core.reload();
         sender.sendMessage(ChatColor.GREEN + "Watcher config reloaded.");
      }
   }

   private void cmdObserve(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher observe <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.observePlayer(target.getUniqueId());
               sender.sendMessage(ChatColor.GREEN + "Now observing " + target.getName());
            }
         }
      }
   }

   private void cmdInvestigate(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher investigate <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.beginInvestigation(target.getUniqueId());
               sender.sendMessage(ChatColor.GREEN + "Investigation started on " + target.getName());
            }
         }
      }
   }

   private void cmdVisible(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher visible <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.showVisible(target.getUniqueId());
               sender.sendMessage(ChatColor.GREEN + "Watcher revealed to " + target.getName());
            }
         }
      }
   }

   private void cmdInvisible(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         core.getBot().vanish();
         sender.sendMessage(ChatColor.GREEN + "Watcher vanished.");
      }
   }

   private void cmdFreeze(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher freeze <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.freezePlayer(target.getUniqueId());
               sender.sendMessage(ChatColor.RED + "Froze " + target.getName());
            }
         }
      }
   }

   private void cmdUnfreeze(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher unfreeze <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.unfreezePlayer(target.getUniqueId());
               sender.sendMessage(ChatColor.GREEN + "Unfroze " + target.getName());
            }
         }
      }
   }

   private void cmdIdle(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         core.goIdle();
         sender.sendMessage(ChatColor.GREEN + "Watcher returned to idle/patrol.");
      }
   }

   private void cmdLore(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher lore <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.triggerLore(target);
               sender.sendMessage(ChatColor.DARK_PURPLE + "Triggered lore appearance for " + target.getName());
            }
         }
      }
   }

   private void cmdTp(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher tp <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getMovement().teleportNear(target, 5);
               sender.sendMessage(ChatColor.GREEN + "Teleported Watcher near " + target.getName());
            }
         }
      }
   }

   private void cmdPatrol(CommandSender sender, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         core.goIdle();
         sender.sendMessage(ChatColor.GREEN + "Watcher patrolling.");
      }
   }

   private void cmdHorror(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher horror <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.beginHorror(target);
               sender.sendMessage(ChatColor.DARK_RED + "§lHorror mode unleashed on " + target.getName());
            }
         }
      }
   }

   private void cmdHerobrine(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher herobrine <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.beginUltimateHerobrineMode(target);
               sender.sendMessage(ChatColor.DARK_RED + "§k...§l§4 HEROBRINE MODE ACTIVATED ON " + target.getName().toUpperCase() + " §4§l§k...");
            }
         }
      }
   }

   private void cmdLog(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher log <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               WatcherPlayerData data = core.getPlayerData(target.getUniqueId());
               if (data == null) {
                  sender.sendMessage(ChatColor.YELLOW + "No data for " + target.getName());
               } else {
                  sender.sendMessage(ChatColor.GOLD + "=== Watcher Log: " + target.getName() + " ===");
                  sender.sendMessage(ChatColor.YELLOW + "Total strikes: " + data.getTotalStrikes());
                  sender.sendMessage(ChatColor.YELLOW + "Investigation score: " + data.getInvestigationScore());
                  sender.sendMessage(ChatColor.YELLOW + "Under investigation: " + data.isUnderInvestigation());
                  sender.sendMessage(ChatColor.YELLOW + "Frozen count: " + data.getFreezeCount());
                  sender.sendMessage(ChatColor.YELLOW + "Recent observations:");
                  List<WatcherPlayerData.ObservationRecord> records = data.getObservationHistory();

                  for (int i = Math.max(0, records.size() - 10); i < records.size(); i++) {
                     WatcherPlayerData.ObservationRecord r = records.get(i);
                     sender.sendMessage(ChatColor.GRAY + " - [" + r.category + "] " + r.details + " (sev=" + r.severity);
                  }
               }
            }
         }
      }
   }

   private void cmdAmbient(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher ambient <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getAmbientHorror().triggerEventOnPlayer(target);
               sender.sendMessage(ChatColor.DARK_PURPLE + "Ambient event triggered on " + target.getName());
            }
         }
      }
   }

   private void cmdStalk(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher stalk <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getStalking().beginStalk(target);
               sender.sendMessage(ChatColor.DARK_GRAY + "Now stalking " + target.getName() + "...");
            }
         }
      }
   }

   private void cmdCave(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher cave <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getCaveDweller().triggerCaveHorror(target);
               sender.sendMessage(ChatColor.BLACK + "§lCave horror unleashed on " + target.getName());
            }
         }
      }
   }

   private void cmdBroken(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher broken <player>");
         } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getBrokenScript().triggerRandomEventOn(target);
               sender.sendMessage(ChatColor.DARK_RED + "§k...§r " + ChatColor.DARK_PURPLE + "Broken Script event triggered on " + target.getName());
            }
         }
      }
   }

   private void cmdScript(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /watcher script <event> <player>");
            sender.sendMessage(ChatColor.GRAY + "Events: block_corruption, item_vanish, door_anomaly, chest_anomaly,");
            sender.sendMessage(ChatColor.GRAY + "  torch_extinguish, fake_chat, health_glitch, camera_glitch,");
            sender.sendMessage(ChatColor.GRAY + "  entity_swap, sound_glitch, inventory_shuffle, sky_glitch,");
            sender.sendMessage(ChatColor.GRAY + "  block_under_foot, broken_script_end, temp_ban");
         } else {
            String eventType = args[1].toLowerCase();
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
               sender.sendMessage(ChatColor.RED + "Player not found.");
            } else {
               core.getBrokenScript().triggerSpecificEvent(target, eventType);
               sender.sendMessage(ChatColor.DARK_PURPLE + "TBS event '" + eventType + "' triggered on " + target.getName());
            }
         }
      }
   }

   private void cmdToggle(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            sender.sendMessage(ChatColor.GOLD + "===== Toggle Status =====");
            sender.sendMessage(core.getToggles().getToggleStatus());
            sender.sendMessage(ChatColor.YELLOW + "Usage: /watcher toggle <feature>");
            sender.sendMessage(ChatColor.GRAY + "Features: bot, horror, broken");
            sender.sendMessage(ChatColor.GRAY + "Events: block_corruption, item_vanish, door_anomaly, chest_anomaly,");
            sender.sendMessage(ChatColor.GRAY + "  torch_extinguish, fake_chat, health_glitch, camera_glitch,");
            sender.sendMessage(ChatColor.GRAY + "  entity_swap, sound_glitch, inventory_shuffle, sky_glitch,");
            sender.sendMessage(ChatColor.GRAY + "  block_under_foot, broken_script_end, temp_ban");
         } else {
            String feature = args[1].toLowerCase();
            WatcherToggles toggles = core.getToggles();
            switch (feature) {
               case "bot": {
                  boolean newState = toggles.toggleBot();
                  sender.sendMessage(ChatColor.YELLOW + "Bot toggle: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                  if (!newState && core.getBot().isSpawned()) {
                     core.getBot().despawn();
                     sender.sendMessage(ChatColor.RED + "Bot despawned because it was disabled.");
                  }
                  break;
               }
               case "horror": {
                  boolean newState = toggles.toggleHorrorGlobal();
                  sender.sendMessage(ChatColor.YELLOW + "Horror Global toggle: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                  break;
               }
               case "broken": {
                  boolean newState = toggles.toggleBrokenScript();
                  sender.sendMessage(ChatColor.YELLOW + "Broken Script toggle: " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                  break;
               }
               default:
                  if (toggles.getAllEventTypes().contains(feature)) {
                     boolean newState = toggles.toggleEvent(feature);
                     sender.sendMessage(ChatColor.YELLOW + "Event '" + feature + "': " + (newState ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                  } else {
                     sender.sendMessage(ChatColor.RED + "Unknown feature. Use /watcher toggle for list.");
                  }
            }
         }
      }
   }

   private void cmdAi(CommandSender sender, String[] args, WatcherCore core) {
      if (this.checkPerm(sender, "jonasmpai.watcher.admin")) {
         if (args.length < 2) {
            AIPlayerBot bot = core.getAIPlayerBot();
            sender.sendMessage(ChatColor.GOLD + "===== AI Bot =====");
            sender.sendMessage(ChatColor.YELLOW + "Spawned: " + ChatColor.WHITE + bot.isSpawned());
            sender.sendMessage(ChatColor.YELLOW + "Usage: /watcher ai <spawn|despawn|status|goal|info|tp|inv|debug|config>");
         } else {
            String action = args[1].toLowerCase();
            AIPlayerBot aiBot = core.getAIPlayerBot();
            switch (action) {
               case "spawn":
                  if (args.length < 3) {
                     sender.sendMessage(ChatColor.RED + "Usage: /watcher ai spawn <player>");
                     return;
                  }

                  Player target = Bukkit.getPlayer(args[2]);
                  if (target == null) {
                     sender.sendMessage(ChatColor.RED + "Player not found.");
                     return;
                  }

                  String name = "AI_" + (args.length > 3 ? args[3] : String.valueOf(new Random().nextInt(9999)));
                  aiBot.spawn(target.getLocation(), name);
                  sender.sendMessage(ChatColor.GREEN + "AI Bot '" + name + "' spawned near " + target.getName());
                  break;
               case "despawn":
                  aiBot.despawn();
                  sender.sendMessage(ChatColor.GREEN + "AI Bot despawned.");
                  break;
               case "status":
                  sender.sendMessage(ChatColor.GOLD + "AI Bot status: " + ChatColor.WHITE + (aiBot.isSpawned() ? "Spawned" : "Not spawned"));
                  break;
               case "goal":
                  if (args.length < 3) {
                     sender.sendMessage(ChatColor.RED + "Usage: /watcher ai goal <goaltype>");
                     sender.sendMessage(ChatColor.GRAY + "Goals: " + String.join(", ", GOAL_TYPES));
                     return;
                  }

                  try {
                     BotGoalPlanner.GoalType g = BotGoalPlanner.GoalType.valueOf(args[2].toUpperCase());
                     aiBot.getGoalPlanner().setGoal(g);
                     sender.sendMessage(ChatColor.GREEN + "AI Bot goal set to " + g);
                  } catch (IllegalArgumentException var21) {
                     sender.sendMessage(ChatColor.RED + "Unknown goal. Use: " + String.join(", ", GOAL_TYPES));
                  }
                  break;
               case "info":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  Player bot2 = aiBot.getNMSBot().getPlayer();
                  if (bot2 == null) {
                     sender.sendMessage(ChatColor.RED + "AI Bot player reference is null.");
                     return;
                  }

                  Location loc = bot2.getLocation();
                  BotGoalPlanner gp = aiBot.getGoalPlanner();
                  sender.sendMessage(ChatColor.GOLD + "===== AI Bot Info =====");
                  sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + bot2.getName());
                  sender.sendMessage(ChatColor.YELLOW + "Health: " + ChatColor.WHITE + String.format("%.1f/%.1f", bot2.getHealth(), bot2.getMaxHealth()));
                  sender.sendMessage(ChatColor.YELLOW + "Food: " + ChatColor.WHITE + bot2.getFoodLevel());
                  sender.sendMessage(
                     ChatColor.YELLOW
                        + "Pos: "
                        + ChatColor.WHITE
                        + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ())
                        + ChatColor.GRAY
                        + " in "
                        + loc.getWorld().getName()
                  );
                  sender.sendMessage(ChatColor.YELLOW + "Biome: " + ChatColor.WHITE + bot2.getLocation().getBlock().getBiome().toString());
                  sender.sendMessage(ChatColor.YELLOW + "Goal: " + ChatColor.WHITE + gp.getCurrentGoal());
                  sender.sendMessage(ChatColor.YELLOW + "Step: " + ChatColor.WHITE + gp.getCurrentStepDesc());
                  sender.sendMessage(
                     ChatColor.YELLOW
                        + "Inventory: "
                        + ChatColor.WHITE
                        + "Wood="
                        + gp.woodCount
                        + " Planks="
                        + gp.plankCount
                        + " Sticks="
                        + gp.stickCount
                        + " Stone="
                        + gp.stoneCount
                  );
                  break;
               case "tp":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  if (!(sender instanceof Player)) {
                     sender.sendMessage(ChatColor.RED + "Only players can teleport.");
                     return;
                  }

                  Player bot2 = aiBot.getNMSBot().getPlayer();
                  if (bot2 == null) {
                     sender.sendMessage(ChatColor.RED + "AI Bot player reference is null.");
                     return;
                  }

                  ((Player)sender).teleport(bot2.getLocation());
                  sender.sendMessage(
                     ChatColor.GREEN
                        + "Teleported to AI Bot at "
                        + String.format("%.1f, %.1f, %.1f", bot2.getLocation().getX(), bot2.getLocation().getY(), bot2.getLocation().getZ())
                  );
                  break;
               case "inv":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  Player bot2 = aiBot.getNMSBot().getPlayer();
                  if (bot2 == null) {
                     sender.sendMessage(ChatColor.RED + "AI Bot player reference is null.");
                     return;
                  }

                  sender.sendMessage(ChatColor.GOLD + "===== AI Bot Inventory =====");
                  PlayerInventory inv = bot2.getInventory();
                  Map<Material, Integer> totals = new HashMap<>();

                  for (int i = 0; i < inv.getSize(); i++) {
                     ItemStack item = inv.getItem(i);
                     if (item != null && item.getType() != Material.AIR) {
                        totals.merge(item.getType(), item.getAmount(), Integer::sum);
                     }
                  }

                  if (totals.isEmpty()) {
                     sender.sendMessage(ChatColor.GRAY + "(empty)");
                  } else {
                     List<String> lines = new ArrayList<>();

                     for (Entry<Material, Integer> entry : totals.entrySet()) {
                        lines.add("" + ChatColor.YELLOW + entry.getKey() + ChatColor.WHITE + " x" + entry.getValue());
                     }

                     Collections.sort(lines);

                     for (String line : lines) {
                        sender.sendMessage(line);
                     }
                  }

                  ItemStack off = inv.getItemInOffHand();
                  if (off != null && off.getType() != Material.AIR) {
                     sender.sendMessage(ChatColor.AQUA + "Offhand: " + ChatColor.WHITE + off.getType() + " x" + off.getAmount());
                  }

                  ItemStack[] armor = inv.getArmorContents();
                  boolean hasArmor = false;
                  StringBuilder armorSb = new StringBuilder();

                  for (int ix = 0; ix < armor.length; ix++) {
                     if (armor[ix] != null && armor[ix].getType() != Material.AIR) {
                        if (hasArmor) {
                           armorSb.append(ChatColor.GRAY + ", ");
                        }

                        armorSb.append(String.valueOf(ChatColor.WHITE)).append(armor[ix].getType());
                        hasArmor = true;
                     }
                  }

                  if (hasArmor) {
                     sender.sendMessage(ChatColor.AQUA + "Armor: " + armorSb.toString());
                  }
                  break;
               case "bridge":
                  if (args.length < 3) {
                     boolean active = core.getBridgeWriter().isEnabled();
                     sender.sendMessage(ChatColor.GOLD + "AI Bridge: " + ChatColor.WHITE + (active ? "ON" : "OFF"));
                     sender.sendMessage(ChatColor.YELLOW + "Usage: /watcher ai bridge <on|off>");
                     return;
                  }

                  String bridgeAction = args[2].toLowerCase();
                  if ("on".equals(bridgeAction)) {
                     core.getBridgeWriter().setEnabled(true);
                     sender.sendMessage(ChatColor.GREEN + "AI Bridge enabled. Writing to " + core.getBridgeWriter().toString());
                  } else if ("off".equals(bridgeAction)) {
                     core.getBridgeWriter().setEnabled(false);
                     sender.sendMessage(ChatColor.GREEN + "AI Bridge disabled.");
                  } else {
                     sender.sendMessage(ChatColor.RED + "Usage: /watcher ai bridge <on|off>");
                  }
                  break;
               case "debug":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  Player bot2 = aiBot.getNMSBot().getPlayer();
                  if (bot2 == null) {
                     sender.sendMessage(ChatColor.RED + "AI Bot player reference is null.");
                     return;
                  }

                  try {
                     Method m = AIPlayerBot.class.getDeclaredMethod("debugBotState", Player.class);
                     m.setAccessible(true);
                     m.invoke(aiBot, bot2);
                     sender.sendMessage(ChatColor.GREEN + "Debug output sent to console.");
                  } catch (Exception var20) {
                     sender.sendMessage(ChatColor.RED + "Debug error: " + var20.getMessage());
                  }
                  break;
               case "memory":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  BotMemory memx = aiBot.getBotMemory();
                  if (memx == null) {
                     sender.sendMessage(ChatColor.RED + "No memory loaded.");
                     return;
                  }

                  sender.sendMessage(ChatColor.GOLD + "===== Kai's Memory =====");
                  sender.sendMessage(
                     ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + memx.totalDeaths + "  Kills: " + memx.totalMobKills + "  Skill: " + memx.survivalSkill
                  );
                  sender.sendMessage(ChatColor.YELLOW + "Lessons learned: " + ChatColor.WHITE + memx.learnedLessons.size());

                  for (int ixx = 0; ixx < memx.learnedLessons.size(); ixx++) {
                     BotMemory.LearnedLesson ll = memx.learnedLessons.get(ixx);
                     sender.sendMessage(
                        ChatColor.GRAY
                           + " ["
                           + ixx
                           + "] "
                           + ChatColor.WHITE
                           + ll.topic
                           + ": "
                           + ll.whatWasWrong
                           + " -> "
                           + ll.whatToDoInstead
                           + " (by "
                           + ll.taughtBy
                     );
                  }

                  sender.sendMessage(ChatColor.YELLOW + "Player instructions: " + ChatColor.WHITE + memx.playerInstructions.size());

                  for (int ixx = 0; ixx < Math.min(10, memx.playerInstructions.size()); ixx++) {
                     BotMemory.PlayerInstruction pi = memx.playerInstructions.get(memx.playerInstructions.size() - 1 - ixx);
                     sender.sendMessage(
                        ChatColor.GRAY + " [" + (memx.playerInstructions.size() - 1 - ixx) + "] " + ChatColor.WHITE + pi.playerName + ": " + pi.instruction
                     );
                  }
                  break;
               case "forget":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  BotMemory mem = aiBot.getBotMemory();
                  if (mem == null) {
                     sender.sendMessage(ChatColor.RED + "No memory loaded.");
                     return;
                  }

                  if (args.length < 3) {
                     sender.sendMessage(ChatColor.RED + "Usage: /watcher ai forget <lesson|instruction|all> [index]");
                     return;
                  }

                  String lowerCase = args[2].toLowerCase();
                  switch (lowerCase) {
                     case "lesson":
                        if (args.length < 4) {
                           sender.sendMessage(ChatColor.RED + "Usage: /watcher ai forget lesson <index>");
                           return;
                        }

                        try {
                           int idx = Integer.parseInt(args[3]);
                           if (mem.removeLesson(idx)) {
                              sender.sendMessage(ChatColor.GREEN + "Lesson " + idx + " removed.");
                           } else {
                              sender.sendMessage(ChatColor.RED + "Invalid index.");
                           }
                        } catch (NumberFormatException var19) {
                           sender.sendMessage(ChatColor.RED + "Index must be a number.");
                        }
                        break;
                     case "instruction":
                        if (args.length < 4) {
                           sender.sendMessage(ChatColor.RED + "Usage: /watcher ai forget instruction <index>");
                           return;
                        }

                        try {
                           int idx = Integer.parseInt(args[3]);
                           if (mem.removeInstruction(idx)) {
                              sender.sendMessage(ChatColor.GREEN + "Instruction " + idx + " removed.");
                           } else {
                              sender.sendMessage(ChatColor.RED + "Invalid index.");
                           }
                        } catch (NumberFormatException var18) {
                           sender.sendMessage(ChatColor.RED + "Index must be a number.");
                        }
                        break;
                     case "all":
                        mem.learnedLessons.clear();
                        mem.playerInstructions.clear();
                        sender.sendMessage(ChatColor.GREEN + "All memory cleared.");
                        break;
                     default:
                        sender.sendMessage(ChatColor.RED + "Unknown type. Use lesson, instruction, or all.");
                  }

                  try {
                     Field f = AIPlayerBot.class.getDeclaredField("memoryStorage");
                     f.setAccessible(true);
                     BotMemoryStorage storage = (BotMemoryStorage)f.get(aiBot);
                     storage.save(mem);
                  } catch (Exception var17) {
                  }
                  break;
               case "analyze":
                  if (!aiBot.isSpawned()) {
                     sender.sendMessage(ChatColor.RED + "AI Bot is not spawned.");
                     return;
                  }

                  AIAnalyzer analyzer = aiBot.getAnalyzer();
                  if (analyzer == null) {
                     sender.sendMessage(ChatColor.RED + "Analyzer not initialized (no API key?).");
                     return;
                  }

                  if (args.length < 3) {
                     sender.sendMessage(ChatColor.YELLOW + "Starting full self-diagnosis... Kai will analyze her own code.");
                     analyzer.selfDiagnose();
                     sender.sendMessage(ChatColor.GREEN + "Self-diagnosis started. Check plugins/JonaSMP_AI/analysis/ for reports.");
                     return;
                  }

                  String targetFile = args[2];
                  String context = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "General code review";
                  analyzer.analyzeFile(targetFile, context);
                  sender.sendMessage(ChatColor.GREEN + "Analysis started for: " + targetFile);
                  break;
               case "reports":
                  AIAnalyzer analyzerx = aiBot.getAnalyzer();
                  if (analyzerx == null) {
                     sender.sendMessage(ChatColor.RED + "Analyzer not initialized.");
                     return;
                  }

                  List<String> reports = analyzerx.listReports();
                  if (reports.isEmpty()) {
                     sender.sendMessage(ChatColor.YELLOW + "No analysis reports yet. Use /watcher ai analyze to create one.");
                     return;
                  }

                  sender.sendMessage(ChatColor.GOLD + "===== Kai's Analysis Reports =====");

                  for (String r : reports) {
                     sender.sendMessage(ChatColor.GRAY + " - " + r);
                  }

                  sender.sendMessage(ChatColor.YELLOW + "Files saved in: plugins/JonaSMP_AI/analysis/ and suggestions/");
                  break;
               case "reload":
                  BotConfig.reload();
                  sender.sendMessage(ChatColor.GREEN + "Bot config reloaded from disk.");
                  if (aiBot.getAnalyzer() != null) {
                     sender.sendMessage(ChatColor.GREEN + "Analyzer ready.");
                  }
                  break;
               case "config":
                  if (args.length < 3) {
                     sender.sendMessage(ChatColor.RED + "Usage: /watcher ai config <reload|show>");
                     return;
                  }

                  String lowerCase2 = args[2].toLowerCase();
                  switch (lowerCase2) {
                     case "reload":
                        BotConfig.reload();
                        sender.sendMessage(ChatColor.GREEN + "Bot config reloaded.");
                        return;
                     case "show":
                        sender.sendMessage(ChatColor.GOLD + "===== Bot Config =====");
                        sender.sendMessage(ChatColor.YELLOW + "Movement speed: " + ChatColor.WHITE + BotConfig.getDouble("movement.speed", 0.28));
                        sender.sendMessage(ChatColor.YELLOW + "Pickup range: " + ChatColor.WHITE + BotConfig.getDouble("pickup.range", 3.0));
                        sender.sendMessage(ChatColor.YELLOW + "Stuck threshold: " + ChatColor.WHITE + BotConfig.getInt("movement.stuck_threshold_ticks", 10));
                        sender.sendMessage(ChatColor.YELLOW + "Auto-jump: " + ChatColor.WHITE + BotConfig.getBoolean("movement.auto_jump", true));
                        sender.sendMessage(ChatColor.YELLOW + "Debug interval: " + ChatColor.WHITE + BotConfig.getInt("debug.interval_ticks", 40));
                        sender.sendMessage(ChatColor.YELLOW + "Console output: " + ChatColor.WHITE + BotConfig.getBoolean("debug.console_output", true));
                        sender.sendMessage(ChatColor.YELLOW + "Default goal: " + ChatColor.WHITE + BotConfig.getString("goals.default", "IDLE"));
                        return;
                     default:
                        sender.sendMessage(ChatColor.RED + "Unknown config action. Use reload or show.");
                        return;
                  }
               default:
                  sender.sendMessage(ChatColor.RED + "Unknown action. Use /watcher ai for list.");
            }
         }
      }
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "===== /watcher Commands =====");
      sender.sendMessage(ChatColor.YELLOW + "/watcher status" + ChatColor.WHITE + " — Show Watcher status");
      sender.sendMessage(ChatColor.YELLOW + "/watcher spawn" + ChatColor.WHITE + " — Spawn the Watcher bot");
      sender.sendMessage(ChatColor.YELLOW + "/watcher despawn" + ChatColor.WHITE + " — Remove the Watcher bot");
      sender.sendMessage(ChatColor.YELLOW + "/watcher reload" + ChatColor.WHITE + " — Reload all Watcher configs");
      sender.sendMessage(ChatColor.YELLOW + "/watcher observe <player>" + ChatColor.WHITE + " — Observe a player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher investigate <player>" + ChatColor.WHITE + " — Investigate a player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher visible <player>" + ChatColor.WHITE + " — Reveal Watcher to player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher invisible" + ChatColor.WHITE + " — Hide Watcher");
      sender.sendMessage(ChatColor.YELLOW + "/watcher freeze <player>" + ChatColor.WHITE + " — Freeze a player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher unfreeze <player>" + ChatColor.WHITE + " — Unfreeze a player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher idle" + ChatColor.WHITE + " — Return to idle/patrol");
      sender.sendMessage(ChatColor.YELLOW + "/watcher lore <player>" + ChatColor.WHITE + " — Trigger lore appearance");
      sender.sendMessage(ChatColor.YELLOW + "/watcher tp <player>" + ChatColor.WHITE + " — Teleport Watcher near player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher patrol" + ChatColor.WHITE + " — Start patrolling");
      sender.sendMessage(ChatColor.YELLOW + "/watcher horror <player>" + ChatColor.WHITE + " — Unleash psychological horror");
      sender.sendMessage(ChatColor.YELLOW + "/watcher herobrine <player>" + ChatColor.WHITE + " — §c§lULTIMATE HEROBRINE MODE");
      sender.sendMessage(ChatColor.YELLOW + "/watcher log <player>" + ChatColor.WHITE + " — Show Watcher logs for player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher ambient <player>" + ChatColor.WHITE + " — Trigger ambient event on player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher stalk <player>" + ChatColor.WHITE + " — Begin stalking a player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher cave <player>" + ChatColor.WHITE + " — Trigger cave horror on player");
      sender.sendMessage(ChatColor.YELLOW + "/watcher broken <player>" + ChatColor.WHITE + " — Trigger Broken Script random event");
      sender.sendMessage(ChatColor.YELLOW + "/watcher script <event> <player>" + ChatColor.WHITE + " — Trigger specific TBS event");
      sender.sendMessage(ChatColor.YELLOW + "/watcher toggle [feature]" + ChatColor.WHITE + " — Toggle bot/horror/events");
      sender.sendMessage(
         ChatColor.YELLOW
            + "/watcher ai <spawn|despawn|status|goal|info|tp|inv|bridge|debug|config|analyze|reports|reload|memory|forget>"
            + ChatColor.WHITE
            + " — AI player bot"
      );
   }

   private boolean checkPerm(CommandSender sender, String perm) {
      if (!sender.hasPermission(perm)) {
         sender.sendMessage(ChatColor.RED + "You don't have permission.");
         return false;
      } else {
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return SUB_COMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
      } else {
         if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList(
                  "observe",
                  "investigate",
                  "visible",
                  "freeze",
                  "unfreeze",
                  "lore",
                  "tp",
                  "horror",
                  "herobrine",
                  "log",
                  "ambient",
                  "stalk",
                  "cave",
                  "broken",
                  "script"
               )
               .contains(sub)) {
               return Bukkit.getOnlinePlayers()
                  .stream()
                  .<String>map(Player::getName)
                  .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                  .collect(Collectors.toList());
            }

            if ("toggle".equals(sub)) {
               List<String> features = new ArrayList<>(Arrays.asList("bot", "horror", "broken"));
               features.addAll(WatcherCore.getInstance().getToggles().getAllEventTypes());
               return features.stream().filter(f -> f.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }

            if ("ai".equals(sub)) {
               return AI_SUBS.stream().filter(a -> a.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
         }

         if (args.length == 3) {
            String subx = args[0].toLowerCase();
            if ("script".equals(subx) || "toggle".equals(subx)) {
               return Bukkit.getOnlinePlayers()
                  .stream()
                  .<String>map(Player::getName)
                  .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                  .collect(Collectors.toList());
            }

            if ("ai".equals(subx) && "spawn".equalsIgnoreCase(args[1])) {
               return Bukkit.getOnlinePlayers()
                  .stream()
                  .<String>map(Player::getName)
                  .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                  .collect(Collectors.toList());
            }

            if ("ai".equals(subx) && "goal".equalsIgnoreCase(args[1])) {
               return GOAL_TYPES.stream().filter(g -> g.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }

            if ("ai".equals(subx) && "config".equalsIgnoreCase(args[1])) {
               return CONFIG_ACTIONS.stream().filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }

            if ("ai".equals(subx) && "forget".equalsIgnoreCase(args[1])) {
               return Arrays.asList("lesson", "instruction", "all")
                  .stream()
                  .filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase()))
                  .collect(Collectors.toList());
            }

            if ("ai".equals(subx) && "analyze".equalsIgnoreCase(args[1])) {
               return Collections.singletonList("<file-path-or-leave-empty>");
            }
         }

         return Collections.emptyList();
      }
   }
}
