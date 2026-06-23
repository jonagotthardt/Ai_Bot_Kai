package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerWatcher implements Listener {
   private static final int MAX_ENTRIES_PER_PLAYER = 1000;
   private static final File DATA_DIR = new File(CoreBootstrap.PLUGIN.getDataFolder(), "ml-data");
   private final Map<UUID, Deque<PlayerWatcher.PlayerAction>> actionBuffers = new ConcurrentHashMap<>();
   private final Map<UUID, PlayerWatcher.PlayerAction> pendingActions = new ConcurrentHashMap<>();

   public PlayerWatcher() {
      if (!DATA_DIR.exists()) {
         DATA_DIR.mkdirs();
      }

      CoreBootstrap.PLUGIN.getLogger().info("[PlayerWatcher] ML data dir: " + DATA_DIR.getAbsolutePath());
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player p = event.getPlayer();
      Block block = event.getBlock();
      Location loc = block.getLocation();
      Chunk chunk = loc.getChunk();
      PlayerWatcher.PlayerAction action = new PlayerWatcher.PlayerAction(
         System.currentTimeMillis(),
         p.getUniqueId(),
         p.getName(),
         "BLOCK_BREAK",
         block.getType().name(),
         loc.getWorld().getName(),
         chunk.getX(),
         chunk.getZ(),
         loc.getBlockX(),
         loc.getBlockY(),
         loc.getBlockZ(),
         p.getInventory().getItemInMainHand().getType().name(),
         true,
         null
      );
      this.bufferAndWrite(action);
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player p = event.getPlayer();
      Block block = event.getBlock();
      Location loc = block.getLocation();
      Chunk chunk = loc.getChunk();
      PlayerWatcher.PlayerAction action = new PlayerWatcher.PlayerAction(
         System.currentTimeMillis(),
         p.getUniqueId(),
         p.getName(),
         "BLOCK_PLACE",
         block.getType().name(),
         loc.getWorld().getName(),
         chunk.getX(),
         chunk.getZ(),
         loc.getBlockX(),
         loc.getBlockY(),
         loc.getBlockZ(),
         p.getInventory().getItemInMainHand().getType().name(),
         true,
         null
      );
      this.bufferAndWrite(action);
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerMove(PlayerMoveEvent event) {
      Player p = event.getPlayer();
      UUID uuid = p.getUniqueId();
      long now = System.currentTimeMillis();
      Deque<PlayerWatcher.PlayerAction> buf = this.actionBuffers.get(uuid);
      if (buf != null && !buf.isEmpty()) {
         PlayerWatcher.PlayerAction last = buf.peekLast();
         if (last != null && last.actionType.equals("MOVE") && now - last.timestamp < 5000L) {
            return;
         }
      }

      Location to = event.getTo();
      Chunk chunk = to.getChunk();
      PlayerWatcher.PlayerAction action = new PlayerWatcher.PlayerAction(
         now,
         uuid,
         p.getName(),
         "MOVE",
         null,
         to.getWorld().getName(),
         chunk.getX(),
         chunk.getZ(),
         to.getBlockX(),
         to.getBlockY(),
         to.getBlockZ(),
         null,
         true,
         "distance=" + String.format("%.1f", event.getFrom().distance(to))
      );
      this.bufferAndWrite(action);
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player p = event.getEntity();
      Location loc = p.getLocation();
      Chunk chunk = loc.getChunk();
      PlayerWatcher.PlayerAction action = new PlayerWatcher.PlayerAction(
         System.currentTimeMillis(),
         p.getUniqueId(),
         p.getName(),
         "DEATH",
         null,
         loc.getWorld().getName(),
         chunk.getX(),
         chunk.getZ(),
         loc.getBlockX(),
         loc.getBlockY(),
         loc.getBlockZ(),
         null,
         false,
         event.getDeathMessage()
      );
      this.bufferAndWrite(action);
   }

   private void bufferAndWrite(final PlayerWatcher.PlayerAction action) {
      this.actionBuffers.computeIfAbsent(action.playerUuid, k -> new ConcurrentLinkedDeque<>()).addLast(action);
      Deque<PlayerWatcher.PlayerAction> buf = this.actionBuffers.get(action.playerUuid);

      while (buf.size() > 1000) {
         buf.pollFirst();
      }

      (new BukkitRunnable() {
         {
            Objects.requireNonNull(PlayerWatcher.this);
         }

         public void run() {
            PlayerWatcher.this.writeActionToFile(action);
         }
      }).runTaskAsynchronously(CoreBootstrap.PLUGIN);
   }

   private void writeActionToFile(PlayerWatcher.PlayerAction action) {
      File file = new File(DATA_DIR, action.playerName + "_actions.jsonl");

      try {
         Files.write(file.toPath(), Collections.singletonList(action.toJson()), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var4) {
         CoreBootstrap.PLUGIN.getLogger().fine("[PlayerWatcher] Write failed: " + var4.getMessage());
      }
   }

   public List<PlayerWatcher.PlayerAction> getRecentActions(UUID uuid, int limit) {
      Deque<PlayerWatcher.PlayerAction> buf = this.actionBuffers.get(uuid);
      if (buf == null) {
         return Collections.emptyList();
      } else {
         List<PlayerWatcher.PlayerAction> result = new ArrayList<>();
         Iterator<PlayerWatcher.PlayerAction> it = buf.descendingIterator();

         for (int count = 0; it.hasNext() && count < limit; count++) {
            result.add(it.next());
         }

         return result;
      }
   }

   public static final class PlayerAction {
      public final long timestamp;
      public final UUID playerUuid;
      public final String playerName;
      public final String actionType;
      public final String blockType;
      public final String world;
      public final int chunkX;
      public final int chunkZ;
      public final int blockX;
      public final int blockY;
      public final int blockZ;
      public final String toolUsed;
      public final boolean success;
      public final String outcome;

      public PlayerAction(
         long timestamp,
         UUID playerUuid,
         String playerName,
         String actionType,
         String blockType,
         String world,
         int chunkX,
         int chunkZ,
         int blockX,
         int blockY,
         int blockZ,
         String toolUsed,
         boolean success,
         String outcome
      ) {
         this.timestamp = timestamp;
         this.playerUuid = playerUuid;
         this.playerName = playerName;
         this.actionType = actionType;
         this.blockType = blockType;
         this.world = world;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
         this.blockX = blockX;
         this.blockY = blockY;
         this.blockZ = blockZ;
         this.toolUsed = toolUsed;
         this.success = success;
         this.outcome = outcome;
      }

      public String toJson() {
         return "{\"timestamp\":"
            + this.timestamp
            + ",\"playerUuid\":\""
            + this.playerUuid
            + "\",\"playerName\":\""
            + escape(this.playerName)
            + "\",\"actionType\":\""
            + this.actionType
            + "\""
            + (this.blockType != null ? ",\"blockType\":\"" + this.blockType + "\"" : "")
            + ",\"world\":\""
            + escape(this.world)
            + "\",\"chunkX\":"
            + this.chunkX
            + ",\"chunkZ\":"
            + this.chunkZ
            + ",\"blockX\":"
            + this.blockX
            + ",\"blockY\":"
            + this.blockY
            + ",\"blockZ\":"
            + this.blockZ
            + (this.toolUsed != null ? ",\"toolUsed\":\"" + this.toolUsed + "\"" : "")
            + ",\"success\":"
            + this.success
            + (this.outcome != null ? ",\"outcome\":\"" + escape(this.outcome) + "\"" : "")
            + "}";
      }

      private static String escape(String s) {
         return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
      }
   }
}
