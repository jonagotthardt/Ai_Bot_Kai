package dev.kai.task;

import dev.kai.bot.KaiBot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs at most one {@link Task} at a time, advancing it one step per tick and clearing it when done.
 *
 * <p>Kept deliberately single-slot: the goal system decides <i>what</i> Kai should be doing, and the
 * task is simply the in-flight execution of that decision. Replacing the task is O(1) and never
 * leaves orphaned work running.
 */
public final class TaskSystem {

    private Task current;

    public void setTask(@Nullable Task task) {
        this.current = task;
    }

    public void clear() {
        this.current = null;
    }

    public boolean hasTask() {
        return current != null;
    }

    public @Nullable Task current() {
        return current;
    }

    public void tick(@NotNull KaiBot bot, long nowTick) {
        if (current == null) {
            return;
        }
        if (current.tick(bot, nowTick)) {
            current = null;
        }
    }
}
