package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BotRespawnListener implements Listener {
   private final AIPlayerBot aiPlayerBot;
   private ItemStack[] savedInventory = null;
   private ItemStack[] savedArmor = null;
   private ItemStack savedOffHand = null;
   private int savedLevel = 0;
   private float savedExp = 0.0F;

   public BotRespawnListener(AIPlayerBot aiPlayerBot) {
      this.aiPlayerBot = aiPlayerBot;
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      if (this.aiPlayerBot.isSpawned()) {
         Player bot = this.aiPlayerBot.getNMSBot().getPlayer();
         if (bot != null) {
            if (player.getUniqueId().equals(bot.getUniqueId())) {
               CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Bot '" + player.getName() + "' died. Saving inventory before respawn...");
               PlayerInventory inv = bot.getInventory();
               this.savedInventory = new ItemStack[36];

               for (int i = 0; i < 36; i++) {
                  ItemStack item = inv.getItem(i);
                  this.savedInventory[i] = item != null ? item.clone() : null;
               }

               this.savedArmor = (ItemStack[])inv.getArmorContents().clone();
               this.savedOffHand = inv.getItemInOffHand() != null ? inv.getItemInOffHand().clone() : null;
               this.savedLevel = bot.getLevel();
               this.savedExp = bot.getExp();
               CoreBootstrap.PLUGIN
                  .getLogger()
                  .info(
                     "[AIPlayerBot] Saved inventory: "
                        + countItems(this.savedInventory)
                        + " items, armor: "
                        + (this.savedArmor != null ? this.savedArmor.length : 0)
                        + " slots"
                  );
               this.aiPlayerBot.saveBotInventory(bot);
               this.aiPlayerBot.getCombatManager().reset();
               event.getDrops().clear();
               event.setKeepInventory(true);
               event.setKeepLevel(true);
               event.setDroppedExp(0);
               if (this.aiPlayerBot.getBotMemory() != null) {
                  String cause = event.getDeathMessage() != null ? event.getDeathMessage() : "unknown";
                  this.aiPlayerBot.getBotMemory().recordDeath(cause, player.getLocation(), System.currentTimeMillis());
                  BotMemory mem = this.aiPlayerBot.getBotMemory();
                  if (cause.toLowerCase().contains("creeper")) {
                     mem.learnedRunFromCreepers = true;
                     mem.recordLesson("creepers", "Got blown up by creeper", "Run away from creepers immediately", "self");
                  }

                  if (cause.toLowerCase().contains("zombie") || cause.toLowerCase().contains("skeleton")) {
                     mem.learnedDontFightLowHealth = true;
                     mem.recordLesson("low_health_combat", "Died fighting while low health", "Flee when health is below 50%", "self");
                  }

                  if (cause.toLowerCase().contains("fall") || cause.toLowerCase().contains("high place")) {
                     mem.recordLesson("fall_damage", "Died from fall damage", "Avoid jumping off cliffs", "self");
                  }

                  if (cause.toLowerCase().contains("drown")) {
                     mem.recordLesson("drowning", "Died underwater", "Swim up immediately when underwater", "self");
                  }

                  if (cause.toLowerCase().contains("lava") || cause.toLowerCase().contains("fire")) {
                     mem.recordLesson("lava", "Died in lava/fire", "Never walk near lava without blocks", "self");
                  }

                  mem.survivalSkill = Math.max(0, mem.survivalSkill);
                  CoreBootstrap.PLUGIN
                     .getLogger()
                     .info("[AIPlayerBot] Learned from death. Skill=" + mem.survivalSkill + " Lessons=" + mem.learnedLessons.size());
               }

               Bukkit.getScheduler()
                  .runTaskLater(
                     CoreBootstrap.PLUGIN,
                     () -> {
                        this.aiPlayerBot.getNMSBot().respawnPlayer();
                        Bukkit.getScheduler()
                           .runTaskLater(
                              CoreBootstrap.PLUGIN,
                              () -> {
                                 Player respawned = this.aiPlayerBot.getNMSBot().getPlayer();
                                 if (respawned != null) {
                                    if (this.savedInventory != null) {
                                       PlayerInventory newInv = respawned.getInventory();

                                       for (int j = 0; j < 36 && j < newInv.getSize(); j++) {
                                          newInv.setItem(j, this.savedInventory[j] != null ? this.savedInventory[j].clone() : null);
                                       }

                                       this.savedInventory = null;
                                       CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Main inventory restored (36 slots)");
                                    }

                                    if (this.savedArmor != null) {
                                       respawned.getInventory().setArmorContents((ItemStack[])this.savedArmor.clone());
                                       this.savedArmor = null;
                                       CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Armor restored");
                                    }

                                    if (this.savedOffHand != null) {
                                       respawned.getInventory().setItemInOffHand(this.savedOffHand.clone());
                                       this.savedOffHand = null;
                                       CoreBootstrap.PLUGIN.getLogger().info("[AIPlayerBot] Offhand restored");
                                    }

                                    respawned.setLevel(this.savedLevel);
                                    respawned.setExp(this.savedExp);
                                    this.aiPlayerBot.loadBotInventory(respawned);
                                    AIPlayerBot aiPlayerBot = this.aiPlayerBot;
                                    Location lastLoc = AIPlayerBot.loadLastLocation();
                                    if (lastLoc != null && lastLoc.getWorld() != null) {
                                       respawned.teleport(lastLoc);
                                       CoreBootstrap.PLUGIN
                                          .getLogger()
                                          .info(
                                             "[AIPlayerBot] Respawned at last location: "
                                                + lastLoc.getBlockX()
                                                + ","
                                                + lastLoc.getBlockY()
                                                + ","
                                                + lastLoc.getBlockZ()
                                          );
                                    }

                                    this.aiPlayerBot.getGoalPlanner().setGoal(BotGoalPlanner.GoalType.IDLE);
                                    long overrideTime = System.currentTimeMillis() + 30000L;
                                    this.aiPlayerBot.getGoalPlanner().setPlayerOverride(overrideTime);
                                    this.aiPlayerBot.setPlayerCommandedUntil(overrideTime);
                                 }
                              },
                              2L
                           );
                     },
                     5L
                  );
            }
         }
      }
   }

   private static int countItems(ItemStack[] items) {
      int count = 0;

      for (ItemStack item : items) {
         if (item != null && item.getType() != Material.AIR) {
            count++;
         }
      }

      return count;
   }
}
