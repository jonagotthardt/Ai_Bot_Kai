package com.jonasmp.ai.punishment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishmentEngine {
   public void execute(Player player, PunishmentResult result) {
      switch (result.getType()) {
         case KICK:
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + " " + result.getReason());
            break;
         case BAN: {
            String command = "ban " + player.getName() + " " + result.getReason();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            break;
         }
         case MUTE: {
            String command = "mute " + player.getName() + " " + result.getReason();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            break;
         }
         case WARN:
            player.sendMessage("§eWarning: " + result.getReason());
      }
   }
}
