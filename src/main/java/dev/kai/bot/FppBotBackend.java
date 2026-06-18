package dev.kai.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * {@link BotBackend} that drives the <a href="https://github.com/tanyaofei/minecraft-fakeplayer">
 * FakePlayer (FPP)</a> plugin.
 *
 * <p>FPP does not expose its internal {@code FakeplayerManager} through Bukkit's service registry,
 * so the stable, version-independent integration point is its command surface. Kai dispatches
 * {@code /fp spawn <name> <world> <x> <y> <z>} as the console and then resolves the resulting,
 * real {@link Player} by name. From that point on Kai controls it purely through {@link KaiBot}.
 *
 * <p>This keeps the integration entirely in-process (no external backend) and means a Paper update
 * that changes NMS internals only requires FPP itself to update, not Kai.
 */
public final class FppBotBackend implements BotBackend {

    private static final String FPP_PLUGIN = "fakeplayer";
    private static final long POLL_INTERVAL_TICKS = 2L;
    private static final long SPAWN_TIMEOUT_TICKS = 200L; // 10s

    private final Plugin plugin;
    private final Logger log;
    private final String botName;

    private KaiBot bot;

    public FppBotBackend(@NotNull Plugin plugin, @NotNull String botName) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.botName = botName;
    }

    @Override
    public boolean isAvailable() {
        Plugin fpp = Bukkit.getPluginManager().getPlugin(FPP_PLUGIN);
        return fpp != null && fpp.isEnabled();
    }

    @Override
    public @NotNull CompletableFuture<KaiBot> spawn(@NotNull Location where) {
        CompletableFuture<KaiBot> future = new CompletableFuture<>();

        if (!isAvailable()) {
            future.completeExceptionally(new IllegalStateException(
                    "FakePlayer plugin is not installed/enabled; cannot spawn Kai."));
            return future;
        }
        if (where.getWorld() == null) {
            future.completeExceptionally(new IllegalStateException("Spawn location has no world."));
            return future;
        }
        Player existing = Bukkit.getPlayerExact(botName);
        if (existing != null) {
            this.bot = new KaiBot(existing);
            future.complete(this.bot);
            return future;
        }

        String command = "fp spawn %s %s %.2f %.2f %.2f".formatted(
                botName, where.getWorld().getName(), where.getX(), where.getY(), where.getZ());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        pollForJoin(future);
        return future;
    }

    private void pollForJoin(@NotNull CompletableFuture<KaiBot> future) {
        final long[] waited = {0L};
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player player = Bukkit.getPlayerExact(botName);
            if (player != null) {
                this.bot = new KaiBot(player);
                holder[0].cancel();
                future.complete(this.bot);
                return;
            }
            waited[0] += POLL_INTERVAL_TICKS;
            if (waited[0] >= SPAWN_TIMEOUT_TICKS) {
                holder[0].cancel();
                future.completeExceptionally(new IllegalStateException(
                        "Timed out waiting for FakePlayer '" + botName + "' to join."));
            }
        }, POLL_INTERVAL_TICKS, POLL_INTERVAL_TICKS);
    }

    @Override
    public @NotNull Optional<KaiBot> current() {
        if (bot != null && bot.isValid()) {
            return Optional.of(bot);
        }
        bot = null;
        return Optional.empty();
    }

    @Override
    public void despawn() {
        if (!isAvailable()) {
            bot = null;
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fp kill " + botName);
        log.info("Requested despawn of Kai bot '" + botName + "'.");
        bot = null;
    }
}
