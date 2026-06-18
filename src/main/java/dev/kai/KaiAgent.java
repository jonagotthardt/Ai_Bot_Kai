package dev.kai;

import dev.kai.bot.BotBackend;
import dev.kai.bot.KaiBot;
import dev.kai.goal.Goal;
import dev.kai.goal.GoalSystem;
import dev.kai.goal.GoalType;
import dev.kai.memory.MemorySystem;
import dev.kai.pvp.PvpController;
import dev.kai.radar.ChunkRadar;
import dev.kai.scheduler.LoadBalancer;
import dev.kai.task.MoveToTask;
import dev.kai.task.TaskSystem;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * The orchestrator wiring every Kai subsystem into one coherent per-tick brain.
 *
 * <p>Cadence is the core design idea. Cheap, responsive actions (goal choice, combat control) run on
 * the configurable control interval; perception (the chunk radar) runs on its own slower cadence and
 * feeds memory; bookkeeping (memory/cache expiry) is deferred onto the {@link LoadBalancer} so it is
 * spread across ticks under a fixed budget. The result is constant, low tick cost with no per-tick
 * world scans, while combat still feels immediate.
 */
public final class KaiAgent {

    private final KaiConfig config;
    private final BotBackend backend;
    private final LoadBalancer balancer;
    private final MemorySystem memory;
    private final ChunkRadar radar;
    private final GoalSystem goals;
    private final PvpController pvp;
    private final TaskSystem tasks = new TaskSystem();

    private Goal currentGoal = Goal.IDLE;

    public KaiAgent(@NotNull KaiConfig config, @NotNull BotBackend backend, @NotNull LoadBalancer balancer,
                    @NotNull MemorySystem memory, @NotNull ChunkRadar radar, @NotNull GoalSystem goals,
                    @NotNull PvpController pvp) {
        this.config = config;
        this.backend = backend;
        this.balancer = balancer;
        this.memory = memory;
        this.radar = radar;
        this.goals = goals;
        this.pvp = pvp;
    }

    /** Called once per server tick by {@link dev.kai.scheduler.TickService}. */
    public void tick(long nowTick) {
        KaiBot bot = backend.current().orElse(null);
        if (bot == null) {
            // Nothing to drive: keep memory tidy but skip all perception/combat work.
            if (nowTick % config.radarRefreshTicks == 0) {
                balancer.submit(() -> memory.maintain(nowTick));
            }
            balancer.drain(System.nanoTime());
            return;
        }

        if (nowTick % config.radarRefreshTicks == 0) {
            radar.refresh(bot.player().getLocation(), balancer, nowTick);
            balancer.submit(() -> memory.maintain(nowTick));
        }

        if (nowTick % config.controlIntervalTicks == 0) {
            currentGoal = goals.decide(bot, pvp, nowTick);
            act(bot, currentGoal, nowTick);
        }

        balancer.drain(System.nanoTime());
    }

    private void act(@NotNull KaiBot bot, @NotNull Goal goal, long nowTick) {
        switch (goal.type()) {
            case ENGAGE -> {
                tasks.clear();
                pvp.tick(bot, nowTick);
            }
            case PURSUE -> {
                Location destination = goal.pursueTarget();
                if (destination != null && !tasks.hasTask()) {
                    long timeout = config.pvpTargetRefreshTicks * 8L;
                    tasks.setTask(new MoveToTask(destination, config.pvpMoveSpeed,
                            config.pvpPreferredRange, nowTick, timeout));
                }
                tasks.tick(bot, nowTick);
            }
            case IDLE -> tasks.tick(bot, nowTick);
        }
    }

    public @NotNull GoalType currentGoalType() {
        return currentGoal.type();
    }

    public @NotNull MemorySystem memory() {
        return memory;
    }

    public @NotNull ChunkRadar radar() {
        return radar;
    }

    public @NotNull PvpController pvp() {
        return pvp;
    }

    public @NotNull LoadBalancer balancer() {
        return balancer;
    }

    public void reset() {
        tasks.clear();
        pvp.setTarget(null);
        currentGoal = Goal.IDLE;
    }
}
