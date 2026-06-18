package dev.kai.goal;

import dev.kai.KaiConfig;
import dev.kai.bot.KaiBot;
import dev.kai.memory.EntityMemory;
import dev.kai.memory.MemorySystem;
import dev.kai.pvp.PvpController;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Chooses Kai's current {@link Goal} by priority.
 *
 * <p>Selection is cheap and deterministic: a live combat target always wins (ENGAGE); otherwise the
 * freshest remembered threat that is still worth chasing yields a PURSUE toward its last known
 * location; otherwise Kai is IDLE. Because the heavy perception already lives in the radar/memory
 * tiers, this only reads what is already known — no scanning happens here.
 */
public final class GoalSystem {

    private final KaiConfig config;
    private final MemorySystem memory;

    public GoalSystem(@NotNull KaiConfig config, @NotNull MemorySystem memory) {
        this.config = config;
        this.memory = memory;
    }

    public @NotNull Goal decide(@NotNull KaiBot bot, @NotNull PvpController pvp, long nowTick) {
        if (pvp.target() != null) {
            return Goal.engage();
        }

        Location botLoc = bot.player().getLocation();
        double engageSq = config.pvpEngageRange * config.pvpEngageRange * 9.0D; // pursue from farther than engage
        EntityMemory best = null;
        double bestThreat = 0.0D;
        for (EntityMemory mem : memory.all()) {
            Location last = mem.lastLocation();
            if (last.getWorld() == null || !last.getWorld().equals(botLoc.getWorld())) {
                continue;
            }
            if (last.distanceSquared(botLoc) > engageSq) {
                continue;
            }
            if (mem.threat() > bestThreat) {
                bestThreat = mem.threat();
                best = mem;
            }
        }
        if (best != null) {
            return Goal.pursue(best.lastLocation());
        }
        return Goal.IDLE;
    }
}
