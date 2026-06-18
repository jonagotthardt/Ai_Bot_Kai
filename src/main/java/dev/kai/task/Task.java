package dev.kai.task;

import dev.kai.bot.KaiBot;

/**
 * A unit of multi-tick behaviour that advances a little each tick.
 *
 * <p>Tasks let Kai carry out work that naturally spans many ticks (walking somewhere, etc.) without
 * blocking or recomputing everything each tick. The {@link TaskSystem} runs exactly one at a time.
 */
public interface Task {

    /**
     * Advances the task by one tick.
     *
     * @return {@code true} when the task is finished and should be cleared.
     */
    boolean tick(KaiBot bot, long nowTick);

    /** Short human-readable label for status output. */
    String name();
}
