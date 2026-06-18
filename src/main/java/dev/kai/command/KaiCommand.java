package dev.kai.command;

import dev.kai.KaiAgent;
import dev.kai.KaiConfig;
import dev.kai.bot.BotBackend;
import dev.kai.bot.KaiBot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * {@code /kai <spawn|despawn|status|engage|stop>} — operator control surface for the agent.
 */
public final class KaiCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("spawn", "despawn", "status", "engage", "stop");

    private final KaiConfig config;
    private final BotBackend backend;
    private final KaiAgent agent;

    public KaiCommand(@NotNull KaiConfig config, @NotNull BotBackend backend, @NotNull KaiAgent agent) {
        this.config = config;
        this.backend = backend;
        this.agent = agent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /kai <spawn|despawn|status|engage|stop>", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender);
            case "despawn" -> handleDespawn(sender);
            case "status" -> handleStatus(sender);
            case "engage" -> handleEngage(sender, args);
            case "stop" -> handleStop(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleSpawn(@NotNull CommandSender sender) {
        if (!backend.isAvailable()) {
            sender.sendMessage(Component.text("FakePlayer plugin is not available; cannot spawn Kai.", NamedTextColor.RED));
            return;
        }
        Location where = sender instanceof Player p
                ? p.getLocation()
                : org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation();
        sender.sendMessage(Component.text("Spawning Kai...", NamedTextColor.GRAY));
        backend.spawn(where).whenComplete((bot, error) -> {
            if (error != null) {
                sender.sendMessage(Component.text("Spawn failed: " + error.getMessage(), NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text("Kai is online as '" + bot.player().getName() + "'.", NamedTextColor.GREEN));
            }
        });
    }

    private void handleDespawn(@NotNull CommandSender sender) {
        backend.despawn();
        agent.reset();
        sender.sendMessage(Component.text("Kai despawned.", NamedTextColor.GRAY));
    }

    private void handleStop(@NotNull CommandSender sender) {
        agent.reset();
        sender.sendMessage(Component.text("Kai cleared its target and current task.", NamedTextColor.GRAY));
    }

    private void handleEngage(@NotNull CommandSender sender, @NotNull String[] args) {
        KaiBot bot = backend.current().orElse(null);
        if (bot == null) {
            sender.sendMessage(Component.text("Kai is not spawned.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /kai engage <player>", NamedTextColor.YELLOW));
            return;
        }
        Player victim = org.bukkit.Bukkit.getPlayerExact(args[1]);
        if (victim == null || victim.equals(bot.player())) {
            sender.sendMessage(Component.text("No such online player to engage.", NamedTextColor.RED));
            return;
        }
        agent.pvp().setTarget(victim);
        sender.sendMessage(Component.text("Kai is now engaging " + victim.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleStatus(@NotNull CommandSender sender) {
        KaiBot bot = backend.current().orElse(null);
        sender.sendMessage(Component.text("== Kai status ==", NamedTextColor.AQUA));
        sender.sendMessage(line("Bot", bot == null ? "offline" : bot.player().getName()));
        sender.sendMessage(line("Goal", agent.currentGoalType().name()));
        LivingEntity target = agent.pvp().target();
        sender.sendMessage(line("Target", target == null ? "none" : target.getName()));
        sender.sendMessage(line("Radar", agent.radar().cachedChunks() + " chunks ~"
                + formatMb(agent.radar().approxCacheBytes()) + " MB ("
                + agent.radar().knownHostiles() + " hostiles)"));
        sender.sendMessage(line("Memory", agent.memory().size() + " entries"));
        sender.sendMessage(line("Queue", agent.balancer().pending() + " pending units"));
    }

    private static Component line(@NotNull String key, @NotNull String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
    }

    private static String formatMb(long bytes) {
        return String.format("%.2f", bytes / (1024.0D * 1024.0D));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("engage")) {
            String prefix = args[1].toLowerCase();
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return Stream.<String>of().toList();
    }
}
