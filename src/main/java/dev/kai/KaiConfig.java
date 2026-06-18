package dev.kai;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable, typed snapshot of {@code config.yml}.
 *
 * <p>Reading config values through Bukkit's {@link FileConfiguration} on every tick would mean
 * repeated map lookups and string parsing in the hot path. Instead we parse once into primitive
 * fields and hand this snapshot to every subsystem, so the control loop only touches plain fields.
 */
public final class KaiConfig {

    public final String botName;
    public final boolean autoSpawn;

    public final long schedulerBudgetMicros;
    public final int controlIntervalTicks;

    public final int radarRefreshTicks;
    public final int radarRadiusChunks;
    public final long radarEntryTtlTicks;

    public final long memoryTtlTicks;

    public final double pvpEngageRange;
    public final double pvpPreferredRange;
    public final double pvpReach;
    public final int pvpTargetRefreshTicks;
    public final int pvpLosRefreshTicks;
    public final int pvpGearRefreshTicks;
    public final double pvpMoveSpeed;
    public final boolean pvpTargetMobs;

    private KaiConfig(FileConfiguration c) {
        this.botName = c.getString("bot.name", "Kai");
        this.autoSpawn = c.getBoolean("bot.auto-spawn", false);

        this.schedulerBudgetMicros = Math.max(50L, c.getLong("scheduler.budget-micros", 500L));
        this.controlIntervalTicks = Math.max(1, c.getInt("control.interval-ticks", 1));

        this.radarRefreshTicks = Math.max(1, c.getInt("radar.refresh-ticks", 20));
        this.radarRadiusChunks = Math.max(1, c.getInt("radar.radius-chunks", 4));
        this.radarEntryTtlTicks = Math.max(1L, c.getLong("radar.entry-ttl-ticks", 200L));

        this.memoryTtlTicks = Math.max(1L, c.getLong("memory.ttl-ticks", 1200L));

        this.pvpEngageRange = Math.max(1.0D, c.getDouble("pvp.engage-range", 16.0D));
        this.pvpPreferredRange = Math.max(0.5D, c.getDouble("pvp.preferred-range", 2.6D));
        this.pvpReach = Math.max(1.0D, c.getDouble("pvp.reach", 3.0D));
        this.pvpTargetRefreshTicks = Math.max(1, c.getInt("pvp.target-refresh-ticks", 10));
        this.pvpLosRefreshTicks = Math.max(1, c.getInt("pvp.los-refresh-ticks", 5));
        this.pvpGearRefreshTicks = Math.max(1, c.getInt("pvp.gear-refresh-ticks", 40));
        this.pvpMoveSpeed = Math.max(0.0D, c.getDouble("pvp.move-speed", 0.22D));
        this.pvpTargetMobs = c.getBoolean("pvp.target-mobs", true);
    }

    public static KaiConfig from(FileConfiguration configuration) {
        return new KaiConfig(configuration);
    }
}
