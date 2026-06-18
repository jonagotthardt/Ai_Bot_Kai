package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ActivityTracker implements Listener {
   private static final long CHECK_INTERVAL_TICKS = 2400L;
   private static final int MONOTONY_THRESHOLD = 6;
   private static final long MESSAGE_COOLDOWN_MS = 180000L;
   private final Map<UUID, ActivityTracker.ActivityWindow> playerActivities = new HashMap<>();
   private final Map<UUID, Long> lastMessageTime = new HashMap<>();
   private final Random random = new Random();

   public void start() {
      Bukkit.getScheduler().runTaskTimer(CoreBootstrap.PLUGIN, this::checkAllPlayers, 2400L, 2400L);
      CoreBootstrap.PLUGIN.getLogger().info("[AI] ActivityTracker started (2min interval)");
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Material type = event.getBlock().getType();
      ActivityTracker.ActivityType act = this.classifyBreak(type);
      this.recordActivity(player.getUniqueId(), act);
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      this.recordActivity(event.getPlayer().getUniqueId(), ActivityTracker.ActivityType.BUILDING);
   }

   @EventHandler
   public void onCombat(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player player) {
         this.recordActivity(player.getUniqueId(), ActivityTracker.ActivityType.COMBAT);
      }
   }

   private ActivityTracker.ActivityType classifyBreak(Material type) {
      String name = type.name().toLowerCase();
      if (name.contains("ore")
         || name.contains("stone")
         || name.contains("deepslate")
         || name.contains("dirt")
         || name.contains("sand")
         || name.contains("gravel")
         || name.contains("netherrack")
         || name.contains("end_stone")) {
         return ActivityTracker.ActivityType.MINING;
      } else if (name.contains("crop")
         || name.contains("wheat")
         || name.contains("carrot")
         || name.contains("potato")
         || name.contains("beetroot")
         || name.contains("melon")
         || name.contains("pumpkin")
         || name.contains("sugarcane")
         || name.contains("bamboo")) {
         return ActivityTracker.ActivityType.FARMING;
      } else {
         return !name.contains("wood") && !name.contains("log") && !name.contains("plank")
            ? ActivityTracker.ActivityType.MINING
            : ActivityTracker.ActivityType.BUILDING;
      }
   }

   private void recordActivity(UUID uuid, ActivityTracker.ActivityType type) {
      ActivityTracker.ActivityWindow window = this.playerActivities.computeIfAbsent(uuid, k -> new ActivityTracker.ActivityWindow());
      long now = System.currentTimeMillis();
      if (now - window.lastUpdate > 60000L) {
         window.current = type;
         window.streak = 1;
      } else if (window.current == type) {
         window.streak++;
      } else {
         window.current = type;
         window.streak = 1;
      }

      window.lastUpdate = now;
   }

   private void checkAllPlayers() {
      long now = System.currentTimeMillis();

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (player.hasPermission("jonasmpai.chat.spontaneous")) {
            UUID uuid = player.getUniqueId();
            ActivityTracker.ActivityWindow window = this.playerActivities.get(uuid);
            if (window != null && window.streak >= 6) {
               Long lastMsg = this.lastMessageTime.get(uuid);
               if (lastMsg == null || now - lastMsg >= 180000L) {
                  String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(uuid) : "de";
                  String message = this.buildMonotonyMessage(player.getName(), window.current, lang);
                  if (message != null) {
                     this.lastMessageTime.put(uuid, now);
                     String prefix = "en".equals(lang) ? "§b[AI] §f" : "§b[KI] §f";
                     player.sendMessage(prefix + message);
                  }
               }
            }
         }
      }
   }

   private String buildMonotonyMessage(String name, ActivityTracker.ActivityType type, String lang) {
      boolean en = "en".equals(lang);
      switch (type) {
         case MINING: {
            String[] msgs = en
               ? new String[]{
                  "Ey " + name + ", you've been mining forever! Do something else for a change?",
                  "Bro " + name + ", if you mine one more block you'll turn to stone yourself!",
                  name + ", there's more to life than mining! Look, the sun is shining!",
                  "Yo " + name + ", your pickaxe is already burnt out. Take a break?",
                  "Hey " + name + ", you probably won't find any more diamonds since you already emptied everything!"
               }
               : new String[]{
                  "Ey " + name + ", du gräbst schon ewig! Mal was anderes machen, alda?",
                  "Digga " + name + ", wenn du noch einen Block abbhaust, wirst du selbst zu Stein!",
                  name + ", es gibt mehr im Leben als nur Mining! Guck mal, die Sonne scheint!",
                  "Alda " + name + ", deine Pickaxe ist schon durchgebrannt. Pause?",
                  "Yo " + name + ", du findest bestimmt keine Diamanten mehr, weil du schon alles leer gemacht hast!"
               };
            return msgs[this.random.nextInt(msgs.length)];
         }
         case FARMING: {
            String[] msgs = en
               ? new String[]{
                  name + ", you're no farmer, bro! Leave the carrots alone!",
                  "Ey " + name + ", 10 minutes of farming only. Get a hobby!",
                  "Bro " + name + ", your farm is already bigger than my whole house! Chill!",
                  name + ", wheat isn't currency. Do something else!",
                  "Yo " + name + ", the cows are already looking weird. Time for something new!"
               }
               : new String[]{
                  name + ", du bist kein Bauer, Digga! Lass die Karotten in Ruhe!",
                  "Ey " + name + ", seit 10 Minuten nur Farmen. Such dir ein Hobby, alda!",
                  "Digga " + name + ", deine Farm ist schon größer als mein ganzes Haus! Chill mal!",
                  name + ", Weizen ist keine Währung. Mach was anderes!",
                  "Alda " + name + ", die Kühe gucken schon komisch. Zeit für was Neues!"
               };
            return msgs[this.random.nextInt(msgs.length)];
         }
         case BUILDING: {
            String[] msgs = en
               ? new String[]{
                  name + ", are you building a new empire or what? Take a break, brother!",
                  "Ey " + name + ", since when are you an architect? Drop the blocks!",
                  "Bro " + name + ", your house is already bigger than the White House. Chill!",
                  "Yo " + name + ", if you place one more block the server will collapse!",
                  name + ", building is nice but food is important too. Go touch grass!"
               }
               : new String[]{
                  name + ", baust du ein neues Reich oder was? Mach mal Pause, Bruder!",
                  "Ey " + name + ", seit wann bist du Architekt? Lass mal die Blöcke!",
                  "Digga " + name + ", dein Haus ist schon größer als das Weiße Haus. Chill!",
                  "Alda " + name + ", wenn du noch einen Block platzierst, kollabiert der Server!",
                  name + ", Bauen ist schön, aber Essen ist auch wichtig. Go touch grass!"
               };
            return msgs[this.random.nextInt(msgs.length)];
         }
         case COMBAT: {
            String[] msgs = en
               ? new String[]{
                  name + ", you're no warrior, you're a farm animal murderer! Chill!",
                  "Ey " + name + ", the mobs have families! Leave the poor zombies alone!",
                  "Bro " + name + ", if you kill one more Creeper it'll explode in revenge!",
                  "Yo " + name + ", PvP is cool but you're only farming mobs. Where's the skill?",
                  name + ", your sword is dull from all that bashing. Take a break?"
               }
               : new String[]{
                  name + ", du bist kein Krieger, du bist ein Farmtier-Mörder! Chill mal!",
                  "Ey " + name + ", die Mobs haben Familien! Lass die armen Zombies in Ruhe!",
                  "Digga " + name + ", wenn du noch einen Creeper killst, explodiert er aus Rache!",
                  "Alda " + name + ", PvP ist cool, aber du farmst nur Mobs. Wo ist der Skill?",
                  name + ", dein Schwert ist stumpf von all dem Geklopfe. Pause?"
               };
            return msgs[this.random.nextInt(msgs.length)];
         }
         default:
            return null;
      }
   }

   private static enum ActivityType {
      MINING,
      FARMING,
      BUILDING,
      COMBAT,
      EXPLORING;
   }

   private static class ActivityWindow {
      ActivityTracker.ActivityType current = ActivityTracker.ActivityType.EXPLORING;
      int streak = 0;
      long lastUpdate = System.currentTimeMillis();
   }
}
