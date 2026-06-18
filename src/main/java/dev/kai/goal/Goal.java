package dev.kai.goal;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * The single highest-priority intent Kai is acting on right now.
 *
 * @param type    what Kai wants to do
 * @param pursueTarget for {@link GoalType#PURSUE}, the last known location to move toward
 */
public record Goal(GoalType type, @Nullable Location pursueTarget) {

    public static final Goal IDLE = new Goal(GoalType.IDLE, null);

    public static Goal engage() {
        return new Goal(GoalType.ENGAGE, null);
    }

    public static Goal pursue(Location where) {
        return new Goal(GoalType.PURSUE, where);
    }
}
