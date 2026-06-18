package dev.kai.task;

import dev.kai.bot.KaiBot;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Walks Kai toward a target location using velocity-based motion, finishing when close enough or
 * after a timeout.
 *
 * <p>Used for {@code PURSUE}: heading to a target's last known position once it has slipped out of
 * sight, rather than re-pathfinding every tick. Vertical motion is left to gravity; the task only
 * nudges horizontal velocity and hops when it stalls against a step.
 */
public final class MoveToTask implements Task {

    private final Location destination;
    private final double speed;
    private final double arriveRadius;
    private final long deadlineTick;

    private Location lastPosition;
    private long stuckSince;

    public MoveToTask(@NotNull Location destination, double speed, double arriveRadius,
                      long nowTick, long maxTicks) {
        this.destination = destination.clone();
        this.speed = speed;
        this.arriveRadius = Math.max(0.5D, arriveRadius);
        this.deadlineTick = nowTick + Math.max(1L, maxTicks);
    }

    @Override
    public boolean tick(KaiBot bot, long nowTick) {
        if (nowTick >= deadlineTick) {
            return true;
        }
        Location loc = bot.player().getLocation();
        if (loc.getWorld() == null || !loc.getWorld().equals(destination.getWorld())) {
            return true;
        }
        Vector flat = new Vector(
                destination.getX() - loc.getX(), 0.0D, destination.getZ() - loc.getZ());
        if (flat.lengthSquared() <= arriveRadius * arriveRadius) {
            return true;
        }

        bot.lookAt(destination);
        bot.moveHorizontally(flat, speed);

        if (lastPosition != null && lastPosition.getWorld() == loc.getWorld()
                && lastPosition.distanceSquared(loc) < 0.0025D) {
            if (nowTick - stuckSince > 5L) {
                bot.jumpIfGrounded();
            }
        } else {
            stuckSince = nowTick;
        }
        lastPosition = loc;
        return false;
    }

    @Override
    public String name() {
        return "move-to";
    }
}
