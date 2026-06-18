package dev.kai;

import dev.kai.bot.BotBackend;
import dev.kai.bot.FppBotBackend;
import dev.kai.command.KaiCommand;
import dev.kai.goal.GoalSystem;
import dev.kai.memory.MemorySystem;
import dev.kai.pvp.PvpController;
import dev.kai.radar.ChunkRadar;
import dev.kai.scheduler.LoadBalancer;
import dev.kai.scheduler.TickService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kai 2.0 entry point.
 *
 * <p>A persistent, PvP-focused AI agent that drives a FakePlayer bot entirely in-process — no
 * external backend. This class only wires the subsystems together and owns their lifecycle; all
 * behaviour lives in the dedicated systems (scheduler, cache, memory, radar, goal, task, pvp).
 */
public final class KaiPlugin extends JavaPlugin {

    private TickService tickService;
    private BotBackend backend;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        KaiConfig config = KaiConfig.from(getConfig());

        LoadBalancer balancer = new LoadBalancer(config.schedulerBudgetMicros);
        MemorySystem memory = new MemorySystem(config.memoryTtlTicks);
        ChunkRadar radar = new ChunkRadar(memory, config.radarRadiusChunks, config.radarEntryTtlTicks);
        GoalSystem goals = new GoalSystem(config, memory);
        PvpController pvp = new PvpController(config, memory);

        this.backend = new FppBotBackend(this, config.botName);
        KaiAgent agent = new KaiAgent(config, backend, balancer, memory, radar, goals, pvp);

        this.tickService = new TickService(this, agent);
        this.tickService.start();

        PluginCommand command = getCommand("kai");
        if (command != null) {
            KaiCommand handler = new KaiCommand(config, backend, agent);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        if (config.autoSpawn) {
            if (backend.isAvailable()) {
                backend.spawn(getServer().getWorlds().get(0).getSpawnLocation())
                        .whenComplete((bot, error) -> {
                            if (error != null) {
                                getLogger().warning("Auto-spawn of Kai failed: " + error.getMessage());
                            } else {
                                getLogger().info("Kai auto-spawned as '" + bot.player().getName() + "'.");
                            }
                        });
            } else {
                getLogger().warning("auto-spawn is enabled but the FakePlayer plugin is not available.");
            }
        }

        getLogger().info("Kai 2.0 enabled (Paper " + getServer().getMinecraftVersion() + ").");
    }

    @Override
    public void onDisable() {
        if (tickService != null) {
            tickService.stop();
        }
        if (backend != null) {
            backend.despawn();
        }
        getLogger().info("Kai 2.0 disabled.");
    }
}
