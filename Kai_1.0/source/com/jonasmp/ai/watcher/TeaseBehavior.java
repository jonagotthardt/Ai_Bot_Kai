package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeaseBehavior {
   private static final String TARGET = "mka197";
   private static final long MESSAGE_COOLDOWN_TICKS = 600L;
   private static final long STARE_COOLDOWN_TICKS = 200L;
   private static final long CHECK_INTERVAL = 40L;
   private final Random random = new Random();
   private final WatcherCore core;
   private BukkitRunnable task;
   private long lastMessageTick = 0L;
   private long lastStareTick = 0L;
   private String lastAction = "";
   private static final String[] TEASE_MESSAGES = new String[]{
      "Hey mka197, ich beobachte dich...",
      "mka197, baust du da was Verbotenes?",
      "mka197, dein Inventar sieht... interessant aus.",
      "Hast du Angst vor mir, mka197?",
      "mka197, ich weiß wo dein Haus ist...",
      "Du gruselst mich, mka197. Ehrlich.",
      "mka197, soll ich was fuer dich abbauen?",
      "Hey mka197, rate mal was ich denke...",
      "mka197, du wirkst heute besonders... lebendig.",
      "Ich folge dir, mka197. Immer."
   };
   private static final String[] ACTION_REACTIONS = new String[]{
      "Nice Block, mka197!", "mka197, das sieht gefaehrlich aus...", "Bergbau ist Leben, oder mka197?", "mka197, pass auf deine Gesundheit auf!"
   };

   public TeaseBehavior(WatcherCore core) {
      this.core = core;
   }

   public void start() {
      CoreBootstrap.PLUGIN.getLogger().info("[TeaseBehavior] Disabled — not starting.");
   }

   public void stop() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }

   private void tick() {
      Player target = Bukkit.getPlayerExact("mka197");
      if (target != null && target.isOnline()) {
         try {
            if ((Boolean)target.getClass().getMethod("isAfk").invoke(target)) {
               return;
            }
         } catch (Exception var16) {
         }

         AIPlayerBot bot = this.core.getAIPlayerBot();
         if (bot.isSpawned()) {
            Player kai = bot.getNMSBot().getPlayer();
            if (kai != null) {
               long now = kai.getWorld().getFullTime();
               if (now - this.lastMessageTick > 600L && this.random.nextDouble() < 0.2) {
                  String msg = TEASE_MESSAGES[this.random.nextInt(TEASE_MESSAGES.length)];
                  kai.chat(msg);
                  this.lastMessageTick = now;
               }

               if (now - this.lastStareTick > 200L && this.random.nextDouble() < 0.15) {
                  Location targetLoc = target.getLocation();
                  Location stareLoc = targetLoc.clone().add((double)(this.random.nextInt(10) - 5), 0.0, (double)(this.random.nextInt(10) - 5));
                  stareLoc.setY((double)targetLoc.getWorld().getHighestBlockYAt(stareLoc));
                  kai.teleport(stareLoc);
                  double dx = targetLoc.getX() - stareLoc.getX();
                  double dz = targetLoc.getZ() - stareLoc.getZ();
                  double dy = targetLoc.getY() - stareLoc.getY();
                  float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
                  float pitch = (float)Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
                  kai.setRotation(yaw, pitch);
                  this.lastStareTick = now;
               }

               if (this.random.nextDouble() < 0.05) {
                  String reaction = ACTION_REACTIONS[this.random.nextInt(ACTION_REACTIONS.length)];
                  kai.chat(reaction);
               }
            }
         }
      }
   }
}
