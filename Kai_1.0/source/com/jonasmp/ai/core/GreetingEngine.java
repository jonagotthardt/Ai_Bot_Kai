package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.rules.RuleAcceptanceGUI;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GreetingEngine implements Listener {
   private static final Random RANDOM = new Random();
   private static final long GREETING_DELAY_TICKS = 140L;
   private static final String[] GREETINGS_DE = new String[]{
      "Yo Digga, was geht alda? Willkommen aufm Server, Bruder!",
      "Ey %s, endlich bist du da! Hast du schon deine Diamanten gefarmt oder was?",
      "Moin moin, Diggi! Bereit für den Grind oder was?",
      "Alda, guck mal wer da ist! Willkommen, Bruder %s!",
      "Yo %s, willkommen! Vergiss nicht, deine Base zu claimen, sonst klau ich alles lol",
      "Eyyyy %s! Digga, wo warst du so lange? Alles gut?",
      "Willkommen %s! Bist du ready zu grinden oder willst du erstmal chillen?",
      "Alda %s, du bist back! Hast du neue Skins mitgebracht oder was?",
      "Yo %s, Digga! Der Server wurde langweilig ohne dich, endlich!",
      "Was geht ab %s? Bereit für den nächsten Raid?",
      "Digga %s, du siehst aus als hättest du gerade 10 Stunden geschlafen. Willkommen!",
      "Alda %s, willkommen zurück! Hast du Essen mitgebracht? Wir haben Hunger lol",
      "Yo %s! Endlich, dachte schon du hast uns verlassen, Bruder.",
      "Moin %s! Digga, der Creeper neben deiner Base wartet auf dich haha",
      "Willkommen %s! Bist du heute wieder der Mining-King oder was?",
      "Ey %s alda, hast du schon gehört? Es gibt neue Enchantments!",
      "Digga %s, willkommen! Deine Farms laufen übrigens noch, du Schlawiner.",
      "Alda %s, guck mal! Der Server ist heute extra schön für dich.",
      "Yo %s! Bereit für PvP oder farmst du erstmal weiter?",
      "Willkommen %s, Diggi! Vergiss nicht, /home zu setzen, sonst bist du lost.",
      "Alda %s, Digga! Lass mal eine Runde Bedwars zocken, was meinst du?",
      "Yo %s, was geht? Hast du schon deine Daily-Rewards abgeholt?",
      "Ey %s, endlich! Ich dachte schon, du spielst wieder Fortnite oder so lol",
      "Willkommen %s! Digga, deine Base ist übrigens noch nicht gegrieft. Noch nicht.",
      "Alda %s! Bist du heute wieder der G.O.A.T. oder nur ein Noob?",
      "Moin %s! Diggi, hast du schon von dem neuen Biom gehört? Krass!",
      "Yo %s, willkommen! Ich hoffe, du hast deine Lucky Blocks dabei!",
      "Eyyyy %s alda! Der Server hat auf dich gewartet, Bruder. Leg los!",
      "Digga %s, bist du bereit für den Endboss oder läufst du wieder weg?",
      "Willkommen %s! Alda, heute wird ein guter Tag zum Bauen, ich spüre es!"
   };
   private static final String[] GREETINGS_EN = new String[]{
      "Yo %s, what's good bro? Welcome to the server!",
      "Ey %s, finally you're here! Did you farm those diamonds yet or what?",
      "Yo yo %s! Ready to grind or what?",
      "Ayy look who's here! Welcome, brother %s!",
      "Yo %s, welcome! Don't forget to claim your base or I'm stealing everything lol",
      "Eyyyy %s! Bro where have you been? All good?",
      "Welcome %s! Are you ready to grind or wanna chill first?",
      "Ayy %s, you're back! Bring any new skins or what?",
      "Yo %s bro! The server was boring without you, finally!",
      "What's up %s? Ready for the next raid?",
      "Bro %s, you look like you just slept for 10 hours. Welcome!",
      "Ayy %s, welcome back! Bring any food? We're hungry lol",
      "Yo %s! Finally, thought you left us bro.",
      "Yo %s! Bro the Creeper next to your base is waiting for you haha",
      "Welcome %s! Are you the Mining King today or what?",
      "Ey %s, did you hear? There are new enchantments!",
      "Bro %s, welcome! Your farms are still running btw, you sneaky boy.",
      "Ayy %s, look! The server is extra beautiful today just for you.",
      "Yo %s! Ready for PvP or farming first?",
      "Welcome %s bro! Don't forget to /home or you're lost.",
      "Ayy %s bro! Let's play some Bedwars, what do you think?",
      "Yo %s, what's up? Did you claim your daily rewards yet?",
      "Ey %s, finally! Thought you were playing Fortnite again lol",
      "Welcome %s! Bro your base hasn't been griefed yet. Yet.",
      "Ayy %s! Are you the G.O.A.T. today or just a noob?",
      "Yo %s! Bro did you hear about the new biome? Insane!",
      "Yo %s, welcome! Hope you brought your lucky blocks!",
      "Eyyyy %s ayy! The server was waiting for you bro. Let's go!",
      "Bro %s, are you ready for the endboss or running away again?",
      "Welcome %s! Ayy today is gonna be a good day to build, I can feel it!"
   };

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      String playerName = player.getName();
      LanguagePrefixManager.updatePlayerPrefix(player);
      Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
         if (player.isOnline()) {
            boolean hasAccepted = CoreBootstrap.FIRST_JOIN_TRACKER.hasAccepted(player.getUniqueId());
            if (!hasAccepted) {
               player.sendMessage("§c§lWillkommen auf Yona SMP!");
               player.sendMessage("§7Bitte lies die Regeln und akzeptiere sie, um beizutreten.");
               player.sendMessage("§7Please read and accept the rules to join the server.");
               RuleAcceptanceGUI.open(player);
            } else {
               player.sendMessage("§e§lWillkommen! / Welcome!");
               player.sendMessage("§7Bitte wähle deine Sprache / Please select your language:");
               LanguageSelectionGUI.open(player);
            }
         }
      }, 40L);
      Bukkit.getScheduler().runTaskLater(CoreBootstrap.PLUGIN, () -> {
         if (player.isOnline() && player.hasPermission("jonasmpai.admin.greeting")) {
            String greeting = this.pickRandomGreeting(player);
            Bukkit.broadcastMessage("§b[KI] §f" + greeting);
         }
      }, 140L);
   }

   private String pickRandomGreeting(Player player) {
      String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId()) : "de";
      String[] pool = "en".equals(lang) ? GREETINGS_EN : GREETINGS_DE;
      String template = pool[RANDOM.nextInt(pool.length)];
      return template.contains("%s") ? String.format(template, player.getName()) : template;
   }
}
