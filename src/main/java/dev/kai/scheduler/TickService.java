package dev.kai.scheduler;

import dev.kai.KaiAgent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongConsumer;

/**
 * The single main-thread heartbeat driving Kai.
 *
 * <p>Exactly one repeating task ticks the whole agent. Centralising the tick here (instead of many
 * independent timers) keeps all Bukkit/Paper access on the main thread and makes the per-tick cost
 * one obvious, measurable thing. The agent itself decides what actually runs on any given tick via
 * its cadences.
 */
public final class TickService {

    private final Plugin plugin;
    private final LongConsumer onTick;

    private BukkitTask task;
    private long tickCounter;

    public TickService(@NotNull Plugin plugin, @NotNull KaiAgent agent) {
        this(plugin, agent::tick);
    }

    TickService(@NotNull Plugin plugin, @NotNull LongConsumer onTick) {
        this.plugin = plugin;
        this.onTick = onTick;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> onTick.accept(tickCounter++), 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public long tick() {
        return tickCounter;
    }
}
