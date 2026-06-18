package com.jonasmp.ai;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.command.KaiCommand;
import com.jonasmp.ai.radar.ChunkRadar;
import com.jonasmp.ai.watcher.AIPlayerBot;
import com.jonasmp.ai.watcher.BotRespawnListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kai 2.0 main plugin class.
 *
 * <p>Kai is a persistent, PvP-focused AI agent that runs entirely in-process.
 * The Kai 1.0 brain was an external OpenRouter LLM that requested one action per
 * second (the cause of the "one decision per tick" sluggishness). Kai 2.0
 * replaces it with the deterministic in-plugin systems ported from 1.0
 * ({@code BotGoalPlanner}, {@code BotCombatManager}, {@code BotPathfinder},
 * {@code ChunkRadar}, {@code BotMemory}) driven every tick by {@link AIPlayerBot}.
 *
 * <p>The class keeps the {@code com.jonasmp.ai.JonaSMP_AI} name/singleton because
 * the ported subsystems resolve the shared {@link ChunkRadar} through
 * {@link #getInstance()} / {@link #getChunkRadar()}.
 */
public final class JonaSMP_AI extends JavaPlugin {

   private static JonaSMP_AI instance;
   private ChunkRadar chunkRadar;
   private AIPlayerBot aiPlayerBot;

   public static JonaSMP_AI getInstance() {
      return instance;
   }

   public ChunkRadar getChunkRadar() {
      return this.chunkRadar;
   }

   public AIPlayerBot getAIPlayerBot() {
      return this.aiPlayerBot;
   }

   @Override
   public void onEnable() {
      instance = this;
      CoreBootstrap.init(this);
      this.saveDefaultConfig();

      this.chunkRadar = new ChunkRadar();
      Bukkit.getPluginManager().registerEvents(this.chunkRadar.getDeltaTracker(), this);

      this.aiPlayerBot = new AIPlayerBot();
      Bukkit.getPluginManager().registerEvents(new BotRespawnListener(this.aiPlayerBot), this);

      PluginCommand command = this.getCommand("kai");
      if (command != null) {
         KaiCommand executor = new KaiCommand(this.aiPlayerBot);
         command.setExecutor(executor);
         command.setTabCompleter(executor);
      } else {
         this.getLogger().warning("[Kai] Command 'kai' missing from plugin.yml.");
      }

      this.getLogger().info("[Kai] Kai 2.0 enabled — deterministic PvP brain, no external backend.");
   }

   @Override
   public void onDisable() {
      if (this.aiPlayerBot != null && this.aiPlayerBot.isSpawned()) {
         this.aiPlayerBot.despawn();
      }

      if (this.chunkRadar != null) {
         this.chunkRadar.stopRefreshTask();
      }

      instance = null;
   }
}
