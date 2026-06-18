package dev.kai.bot;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction over whatever creates and owns Kai's underlying player entity.
 *
 * <p>The default implementation ({@link FppBotBackend}) bridges to the FakePlayer plugin, but the
 * rest of Kai only ever talks to this interface and {@link KaiBot}. That keeps the AI decoupled from
 * how the bot is actually spawned, and lets the spawn mechanism evolve (or be swapped) without
 * touching the goal/task/PvP systems.
 */
public interface BotBackend {

    /** Whether the backend can currently spawn a bot (e.g. FakePlayer present and enabled). */
    boolean isAvailable();

    /**
     * Spawns the bot at {@code where}. Completes with the controllable handle once the player exists
     * on the server, or completes exceptionally if spawning fails.
     */
    @NotNull CompletableFuture<KaiBot> spawn(@NotNull Location where);

    /** The currently live bot, if any. */
    @NotNull Optional<KaiBot> current();

    /** Removes the bot from the world. No-op if none is alive. */
    void despawn();
}
