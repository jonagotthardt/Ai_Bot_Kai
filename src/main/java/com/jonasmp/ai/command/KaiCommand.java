package com.jonasmp.ai.command;

import com.jonasmp.ai.watcher.AIPlayerBot;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandExecutor;

/**
 * Operator command surface for Kai 2.0.
 *
 * <ul>
 *   <li>{@code /kai spawn [name]} — spawn the bot at the sender's location.</li>
 *   <li>{@code /kai despawn} — remove the bot.</li>
 *   <li>{@code /kai come} — order the bot to move to the sender.</li>
 *   <li>{@code /kai say <text>} — issue a natural-language order routed to the
 *       deterministic goal planner.</li>
 * </ul>
 */
public final class KaiCommand implements CommandExecutor, TabCompleter {

   private static final List<String> SUBCOMMANDS = Arrays.asList("spawn", "despawn", "come", "say");

   private final AIPlayerBot bot;

   public KaiCommand(AIPlayerBot bot) {
      this.bot = bot;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         sender.sendMessage("§e[Kai] /kai <spawn|despawn|come|say> — status: " + (this.bot.isSpawned() ? "§aspawned" : "§cnot spawned"));
         return true;
      }

      switch (args[0].toLowerCase()) {
         case "spawn": {
            if (!(sender instanceof Player player)) {
               sender.sendMessage("§c[Kai] Only a player can spawn the bot (needs a location).");
               return true;
            }
            if (this.bot.isSpawned()) {
               sender.sendMessage("§c[Kai] Bot is already spawned.");
               return true;
            }
            String name = args.length > 1
               ? args[1]
               : com.jonasmp.ai.JonaSMP_AI.getInstance().getConfig().getString("bot.name", "Kai");
            this.bot.spawn(player.getLocation(), name);
            sender.sendMessage("§a[Kai] Spawning bot '" + name + "'.");
            return true;
         }
         case "despawn": {
            if (!this.bot.isSpawned()) {
               sender.sendMessage("§c[Kai] Bot is not spawned.");
               return true;
            }
            this.bot.despawn();
            sender.sendMessage("§a[Kai] Bot despawned.");
            return true;
         }
         case "come": {
            if (!(sender instanceof Player player)) {
               sender.sendMessage("§c[Kai] Only a player can be followed.");
               return true;
            }
            if (!this.bot.isSpawned()) {
               sender.sendMessage("§c[Kai] Bot is not spawned.");
               return true;
            }
            String reply = this.bot.handlePlayerChat(player, "komm zu mir", "komm zu mir");
            sender.sendMessage("§b[Kai] " + reply);
            return true;
         }
         case "say": {
            if (!(sender instanceof Player player)) {
               sender.sendMessage("§c[Kai] Only a player can talk to the bot.");
               return true;
            }
            if (!this.bot.isSpawned()) {
               sender.sendMessage("§c[Kai] Bot is not spawned.");
               return true;
            }
            if (args.length < 2) {
               sender.sendMessage("§c[Kai] Usage: /kai say <text>");
               return true;
            }
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String reply = this.bot.handlePlayerChat(player, message.toLowerCase(), message);
            sender.sendMessage("§b[Kai] " + reply);
            return true;
         }
         default:
            sender.sendMessage("§c[Kai] Unknown subcommand. Use /kai <spawn|despawn|come|say>.");
            return true;
      }
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         String prefix = args[0].toLowerCase();
         return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
      }
      return Collections.emptyList();
   }
}
