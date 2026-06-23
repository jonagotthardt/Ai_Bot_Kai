package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

public class GriefingDetector implements Listener {
   private static final int CHECK_RADIUS = 8;
   private static final int SUSPICIOUS_THRESHOLD = 5;
   private static final long COOLDOWN_MS = 300000L;
   private final Map<UUID, Integer> suspiciousCount;
   private final Map<UUID, Long> lastWarningTime;
   private final Map<UUID, Location> lastCheckedArea;
   private final Random random;
   private Object coreProtectAPI;
   private Method blockLookupMethod;
   private Method parseResultMethod;
   private boolean coreProtectAvailable;
   private Object logBlockConsumer;
   private Method lbGetHistoryMethod;
   private Method lbGetPlayerMethod;
   private boolean logBlockAvailable;
   private volatile boolean initAttempted;
   private static GriefingDetector INSTANCE;
   private Method logRemovalMethod;

   public GriefingDetector() {
      INSTANCE = this;
      this.suspiciousCount = new HashMap<>();
      this.lastWarningTime = new HashMap<>();
      this.lastCheckedArea = new HashMap<>();
      this.random = new Random();
      this.coreProtectAvailable = false;
      this.logBlockAvailable = false;
      this.initAttempted = false;
   }

   private synchronized void initLogging() {
      if (!this.initAttempted) {
         this.initAttempted = true;

         try {
            Plugin cp = Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (cp != null) {
               Method getAPI = cp.getClass().getMethod("getAPI");
               this.coreProtectAPI = getAPI.invoke(cp);
               if (this.coreProtectAPI != null) {
                  this.blockLookupMethod = this.coreProtectAPI.getClass().getMethod("blockLookup", Block.class, int.class);

                  try {
                     this.parseResultMethod = this.coreProtectAPI.getClass().getMethod("parseResult", String[].class);
                  } catch (NoSuchMethodException var10) {
                     this.parseResultMethod = this.coreProtectAPI.getClass().getMethod("parseResult", Object.class);
                  }

                  this.coreProtectAvailable = true;

                  try {
                     this.logRemovalMethod = this.coreProtectAPI.getClass().getMethod("logRemoval", String.class, BlockData.class, Location.class);
                  } catch (NoSuchMethodException var9) {
                     try {
                        this.logRemovalMethod = this.coreProtectAPI.getClass().getMethod("logRemoval", String.class, Material.class, Location.class);
                     } catch (NoSuchMethodException var8) {
                        CoreBootstrap.PLUGIN.getLogger().warning("[AI] CoreProtect logRemoval not found, using BlockBreakEvent fallback");
                     }
                  }

                  CoreBootstrap.PLUGIN.getLogger().info("[AI] CoreProtect API connected — griefing detection active");
                  return;
               }
            }
         } catch (Exception var11) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not connect to CoreProtect API: " + var11.getMessage());
         }

         try {
            Plugin lb = Bukkit.getPluginManager().getPlugin("LogBlock");
            if (lb != null) {
               Method getInstance = lb.getClass().getMethod("getInstance");
               Object logBlockInstance = getInstance.invoke(null);
               if (logBlockInstance != null) {
                  Method getConsumer = logBlockInstance.getClass().getMethod("getConsumer");
                  this.logBlockConsumer = getConsumer.invoke(logBlockInstance);
                  if (this.logBlockConsumer != null) {
                     try {
                        this.lbGetHistoryMethod = this.logBlockConsumer.getClass().getMethod("getHistory", Block.class);
                        this.lbGetPlayerMethod = Class.forName("de.diddiz.LogBlock.BlockChange").getMethod("getPlayerName");
                     } catch (NoSuchMethodException var6) {
                        this.lbGetHistoryMethod = this.logBlockConsumer.getClass().getMethod("getBlockHistory", Block.class);
                     }

                     this.logBlockAvailable = true;
                     CoreBootstrap.PLUGIN.getLogger().info("[AI] LogBlock API connected — griefing detection active (fallback)");
                     return;
                  }
               }
            }
         } catch (Exception var7) {
            CoreBootstrap.PLUGIN.getLogger().warning("[AI] Could not connect to LogBlock API: " + var7.getMessage());
         }

         CoreBootstrap.PLUGIN.getLogger().info("[AI] No logging plugin found (CoreProtect/LogBlock) — griefing detection disabled");
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (!this.initAttempted) {
         this.initLogging();
      }

      if (this.coreProtectAvailable || this.logBlockAvailable) {
         this.checkBlockAction(event.getPlayer(), event.getBlock());
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      if (!this.initAttempted) {
         this.initLogging();
      }

      if (this.coreProtectAvailable || this.logBlockAvailable) {
         this.checkBlockAction(event.getPlayer(), event.getBlock());
      }
   }

   private void checkBlockAction(Player player, Block block) {
      if (player.hasPermission("jonasmpai.chat.spontaneous")) {
         UUID uuid = player.getUniqueId();
         Location blockLoc = block.getLocation();
         String playerName = player.getName();
         Location lastArea = this.lastCheckedArea.get(uuid);
         if (lastArea == null || !lastArea.getWorld().equals(blockLoc.getWorld()) || !(lastArea.distance(blockLoc) < 8.0)) {
            boolean foreignBlocks = this.hasForeignBlocksNearby(playerName, blockLoc);
            if (foreignBlocks) {
               int count = this.suspiciousCount.getOrDefault(uuid, 0) + 1;
               this.suspiciousCount.put(uuid, count);
               this.lastCheckedArea.put(uuid, blockLoc);
               if (count >= 5) {
                  long now = System.currentTimeMillis();
                  Long lastWarn = this.lastWarningTime.get(uuid);
                  if (lastWarn == null || now - lastWarn > 300000L) {
                     this.lastWarningTime.put(uuid, now);
                     this.suspiciousCount.put(uuid, 0);
                     String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(uuid) : "de";
                     String msg = this.buildGriefingMessage(playerName, lang);
                     String prefix = "en".equals(lang) ? "§b[AI] §f" : "§b[KI] §f";
                     Bukkit.broadcastMessage(prefix + msg);
                  }
               }
            }
         }
      }
   }

   private boolean hasForeignBlocksNearby(String playerName, Location center) {
      World world = center.getWorld();
      int cx = center.getBlockX();
      int cy = center.getBlockY();
      int cz = center.getBlockZ();
      Set<String> otherBuilders = new HashSet<>();
      int checked = 0;

      for (int dx = -8; dx <= 8; dx += 2) {
         for (int dy = -8; dy <= 8; dy += 2) {
            for (int dz = -8; dz <= 8 && checked++ <= 50; dz += 2) {
               Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
               if (!b.getType().isAir()) {
                  String builder = this.getBlockPlacer(b);
                  if (builder != null && !builder.equalsIgnoreCase(playerName)) {
                     otherBuilders.add(builder);
                     if (otherBuilders.size() >= 2) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private String getBlockPlacer(Block block) {
      if (this.coreProtectAvailable) {
         return this.getBlockPlacerCoreProtect(block);
      } else {
         return this.logBlockAvailable ? this.getBlockPlacerLogBlock(block) : null;
      }
   }

   private String getBlockPlacerCoreProtect(Block block) {
      try {
         Object resultList = this.blockLookupMethod.invoke(this.coreProtectAPI, block, 0);
         if (resultList == null) {
            return null;
         }

         for (Object entry : (Iterable)resultList) {
            String who = null;
            int actionId = -1;
            if (entry instanceof String[] result) {
               if (result.length > 2) {
                  who = result[1];

                  try {
                     actionId = Integer.parseInt(result[2]);
                  } catch (NumberFormatException var12) {
                  }
               }
            } else {
               Object parseResult = this.parseResultMethod.invoke(this.coreProtectAPI, entry);
               if (parseResult == null) {
                  continue;
               }

               Method getPlayer = parseResult.getClass().getMethod("getPlayer");
               Method getActionId = parseResult.getClass().getMethod("getActionId");
               who = (String)getPlayer.invoke(parseResult);
               actionId = (Integer)getActionId.invoke(parseResult);
            }

            if (actionId == 1 && who != null && !who.equals("#tnt") && !who.equals("#fire")) {
               return who;
            }
         }
      } catch (Exception var13) {
      }

      return null;
   }

   private String getBlockPlacerLogBlock(Block block) {
      try {
         if (this.lbGetHistoryMethod == null) {
            return null;
         } else {
            Object history = this.lbGetHistoryMethod.invoke(this.logBlockConsumer, block);
            if (history == null) {
               return null;
            } else {
               Iterable<?> iterable = (Iterable<?>)history;
               Iterator var4 = iterable.iterator();

               String who;
               while (true) {
                  if (!var4.hasNext()) {
                     return null;
                  }

                  Object entry = var4.next();
                  if (entry != null) {
                     who = null;
                     if (this.lbGetPlayerMethod != null) {
                        who = (String)this.lbGetPlayerMethod.invoke(entry);
                     } else {
                        try {
                           Method getPlayer = entry.getClass().getMethod("getPlayerName");
                           who = (String)getPlayer.invoke(entry);
                        } catch (NoSuchMethodException var10) {
                           try {
                              Method getWho = entry.getClass().getMethod("getPlayer");
                              who = (String)getWho.invoke(entry);
                           } catch (NoSuchMethodException var9) {
                           }
                        }
                     }

                     if (who != null && !who.isEmpty()) {
                        try {
                           Method getType = entry.getClass().getMethod("getType");
                           Object type = getType.invoke(entry);
                           if (type == null || !type.toString().toLowerCase().contains("break")) {
                              break;
                           }
                        } catch (NoSuchMethodException var11) {
                           break;
                        }
                     }
                  }
               }

               return who;
            }
         }
      } catch (Exception var12) {
         return null;
      }
   }

   public static void logBlockBreak(Player player, Block block, Material type, BlockData data) {
      if (INSTANCE != null && INSTANCE.initAttempted) {
         if (INSTANCE.coreProtectAvailable) {
            try {
               if (INSTANCE.logRemovalMethod != null && data != null) {
                  INSTANCE.logRemovalMethod.invoke(INSTANCE.coreProtectAPI, player.getName(), data, block.getLocation());
                  return;
               }

               BlockBreakEvent event = new BlockBreakEvent(block, player);
               Bukkit.getPluginManager().callEvent(event);
            } catch (Exception var5) {
               CoreBootstrap.PLUGIN.getLogger().fine("[GriefingDetector] Failed to log block break: " + var5.getMessage());
            }
         }
      }
   }

   private String buildGriefingMessage(String name, String lang) {
      boolean en = "en".equals(lang);
      String[] msgs = en
         ? new String[]{
            "Ey " + name + ", that doesn't belong to you, right? Ask first!",
            "Bro " + name + ", do you have permission to build here? Looks like someone else's base!",
            "Yo " + name + ", who's building wild here? This isn't your terrain, brother!",
            "Hey " + name + ", someone else placed these blocks. Better leave it, bro!",
            "Eyyyy " + name + ", I think that belongs to another player. Hands off!",
            "Bro " + name + ", I have eyes everywhere! That's someone else's property!",
            "Yo " + name + ", did you ask the owner? Because this doesn't look like your stuff!",
            "Welcome to griefing prevention, " + name + "! That belongs to someone else, bro!"
         }
         : new String[]{
            "Ey " + name + ", das da gehört doch nicht dir, oder? Alda, frag erstmal!",
            "Digga " + name + ", hast du Permission hier zu bauen? Sieht nach fremder Base aus!",
            "Alda " + name + ", wer baut denn hier wild rum? Das ist nicht dein Terrain, Bruder!",
            "Yo " + name + ", die Blöcke hier hat wer anders hingesetzt. Lass das lieber, Digga!",
            "Eyyyy " + name + ", ich glaub das gehört nem anderen Spieler. Finger weg, alda!",
            "Digga " + name + ", ich hab Augen überall! Das ist fremdes Eigentum, Bruder!",
            "Moin " + name + ", hast du den Owner gefragt? Weil das hier sieht nicht nach deinem Zeug aus!",
            "Willkommen zur Griefing-Prävention, " + name + "! Das gehört wem anderem, Digga!"
         };
      return msgs[this.random.nextInt(msgs.length)];
   }
}
