package dev.kai.memory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * What Kai remembers about a single entity it has perceived.
 *
 * <p>Mutable on purpose: entries are updated in place every time the entity is seen, which avoids
 * allocating a fresh object per observation in the hot path. The remembered {@link #lastLocation}
 * is what lets Kai keep pursuing a target that just broke line of sight instead of re-scanning.
 */
public final class EntityMemory {

    private final UUID id;
    private String name;
    private final Location lastLocation;
    private long lastSeenTick;
    private double threat;
    private boolean player;
    private Material heldItem;

    EntityMemory(@NotNull UUID id, @NotNull String name, @NotNull Location lastLocation, long seenTick,
                 boolean player) {
        this.id = id;
        this.name = name;
        this.lastLocation = lastLocation.clone();
        this.lastSeenTick = seenTick;
        this.player = player;
    }

    void update(@NotNull String name, @NotNull Location location, long seenTick, boolean player,
                @Nullable Material heldItem, double threat) {
        this.name = name;
        this.lastLocation.setWorld(location.getWorld());
        this.lastLocation.set(location.getX(), location.getY(), location.getZ());
        this.lastLocation.setYaw(location.getYaw());
        this.lastLocation.setPitch(location.getPitch());
        this.lastSeenTick = seenTick;
        this.player = player;
        this.heldItem = heldItem;
        this.threat = threat;
    }

    public @NotNull UUID id() {
        return id;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull Location lastLocation() {
        return lastLocation.clone();
    }

    public long lastSeenTick() {
        return lastSeenTick;
    }

    public double threat() {
        return threat;
    }

    public boolean isPlayer() {
        return player;
    }

    public @Nullable Material heldItem() {
        return heldItem;
    }

    public long ageTicks(long nowTick) {
        return nowTick - lastSeenTick;
    }
}
