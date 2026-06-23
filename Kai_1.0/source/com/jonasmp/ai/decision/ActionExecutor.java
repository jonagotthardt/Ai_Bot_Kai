package com.jonasmp.ai.decision;

import org.bukkit.entity.Player;

public class ActionExecutor {
   public void execute(Player player, DecisionResult result) {
      switch (result.getAction()) {
         case BLOCK:
            player.sendMessage("§cMessage blocked by AI");
            break;
         case WARN:
            player.sendMessage("§eWarning: " + result.getReason());
            break;
         case FLAG:
            player.sendMessage("§6Flagged for review");
      }
   }
}
