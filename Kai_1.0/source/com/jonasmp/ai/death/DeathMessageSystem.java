package com.jonasmp.ai.death;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class DeathMessageSystem implements Listener {
   private final Random random = new Random();
   private final Map<String, List<String>> deMessages = new HashMap<>();
   private final Map<String, List<String>> enMessages = new HashMap<>();

   public DeathMessageSystem() {
      this.loadMessages();
   }

   public void register() {
      Bukkit.getPluginManager().registerEvents(this, CoreBootstrap.PLUGIN);
      CoreBootstrap.PLUGIN.getLogger().info("[DeathMessages] System loaded — 20 death types, DE+EN.");
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      String victimName = victim.getName();
      Player killer = victim.getKiller();
      String category = this.getDeathCategory(victim, killer);
      event.setDeathMessage(null);

      for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
         String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(onlinePlayer.getUniqueId());
         String message = this.getRandomMessage(category, lang, victimName, killer);
         onlinePlayer.sendMessage(message);
      }
   }

   private String getDeathCategory(Player victim, Player killer) {
      EntityDamageEvent lastDamage = victim.getLastDamageCause();
      if (lastDamage == null) {
         return "generic";
      } else {
         DamageCause cause = lastDamage.getCause();
         if (killer != null && killer != victim) {
            return "pvp";
         } else {
            switch (cause) {
               case FALL:
                  return "fall";
               case FLY_INTO_WALL:
                  return "elytra";
               case FIRE:
               case FIRE_TICK:
                  return "fire";
               case LAVA:
                  return "lava";
               case ENTITY_EXPLOSION:
                  return "explosion";
               case ENTITY_ATTACK:
                  return "mob_melee";
               case PROJECTILE:
                  return "projectile";
               case DROWNING:
                  return "drowning";
               case SUFFOCATION:
                  return "suffocation";
               case CONTACT:
                  return "cactus";
               case FREEZE:
                  return "freeze";
               case STARVATION:
                  return "starvation";
               case POISON:
               case MAGIC:
                  return "poison";
               case WITHER:
                  return "wither";
               case LIGHTNING:
                  return "lightning";
               case VOID:
                  return "void";
               case BLOCK_EXPLOSION:
                  if (lastDamage.getDamage() >= 9000.0) {
                     return "bed_explosion";
                  }

                  return "block_explosion";
               case FALLING_BLOCK:
                  return "anvil";
               case THORNS:
                  return "thorns";
               case CRAMMING:
                  return "cramming";
               case SUICIDE:
                  return "suicide";
               case DRAGON_BREATH:
                  return "dragon_breath";
               case DRYOUT:
                  return "dryout";
               case HOT_FLOOR:
                  return "magma";
               case MELTING:
                  return "melting";
               case SONIC_BOOM:
                  return "sonic_boom";
               case KILL:
               case WORLD_BORDER:
               case CUSTOM:
               default:
                  return "generic";
            }
         }
      }
   }

   private String getRandomMessage(String category, String lang, String victimName, Player killer) {
      Map<String, List<String>> map = "en".equals(lang) ? this.enMessages : this.deMessages;
      List<String> messages = map.getOrDefault(category, map.get("generic"));
      String template = messages.get(this.random.nextInt(messages.size()));
      String killerName = killer != null ? killer.getName() : null;
      if (killerName != null) {
         template = template.replace("[Killer]", killerName);
      }

      return ChatColor.translateAlternateColorCodes('&', template.replace("[Spieler]", victimName));
   }

   private void loadMessages() {
      this.deMessages
         .put(
            "fall",
            Arrays.asList(
               "&c[Spieler] &7fand die Gravitation etwas zu interessant.",
               "&c[Spieler] &7hat die Schwerkraft unterschätzt. Die Schwerkraft nicht.",
               "&c[Spieler] &7wollte fliegen. Der Boden hatte Einwände.",
               "&c[Spieler] &7führte einen kontrollierten Absturz durch. Kontrolle nicht gefunden.",
               "&c[Spieler] &7sprang. Der Boden gewann.",
               "&c[Spieler] &7erreichte sein Ziel. Leider mit dem Gesicht zuerst."
            )
         );
      this.enMessages
         .put(
            "fall",
            Arrays.asList(
               "&c[Spieler] &7found gravity a bit too interesting.",
               "&c[Spieler] &7underestimated gravity. Gravity didn't.",
               "&c[Spieler] &7tried to fly. The ground objected.",
               "&c[Spieler] &7attempted a controlled crash. Control not found.",
               "&c[Spieler] &7jumped. The ground won.",
               "&c[Spieler] &7reached their target. Face first, unfortunately."
            )
         );
      this.deMessages
         .put(
            "elytra",
            Arrays.asList(
               "&c[Spieler] &7verwechselte Elytra mit Boeing.",
               "&c[Spieler] &7lernte die Bedeutung von 'Bremsweg'.",
               "&c[Spieler] &7traf die Wand mit Lichtgeschwindigkeit.",
               "&c[Spieler] &7führte einen ungeplanten Landeanflug durch."
            )
         );
      this.enMessages
         .put(
            "elytra",
            Arrays.asList(
               "&c[Spieler] &7confused Elytra with a Boeing.",
               "&c[Spieler] &7learned what 'braking distance' means.",
               "&c[Spieler] &7hit the wall at light speed.",
               "&c[Spieler] &7performed an unplanned landing approach."
            )
         );
      this.deMessages
         .put(
            "fire",
            Arrays.asList(
               "&c[Spieler] &7ist jetzt gut durch.",
               "&c[Spieler] &7blieb etwas zu lange im Ofen.",
               "&c[Spieler] &7wurde fachgerecht gegart.",
               "&c[Spieler] &7wurde zur knusprigen Version seiner selbst."
            )
         );
      this.enMessages
         .put(
            "fire",
            Arrays.asList(
               "&c[Spieler] &7is now well done.",
               "&c[Spieler] &7stayed in the oven a bit too long.",
               "&c[Spieler] &7was professionally roasted.",
               "&c[Spieler] &7became the crispy version of themselves."
            )
         );
      this.deMessages
         .put(
            "lava",
            Arrays.asList(
               "&c[Spieler] &7badete in verbotener Orangensaft.",
               "&c[Spieler] &7testete, ob Lava warm ist. Ergebnis: ja.",
               "&c[Spieler] &7wurde Teil des Erdmantels.",
               "&c[Spieler] &7hat das Lava-Spa nicht überlebt."
            )
         );
      this.enMessages
         .put(
            "lava",
            Arrays.asList(
               "&c[Spieler] &7bathed in forbidden orange juice.",
               "&c[Spieler] &7tested if lava is warm. Result: yes.",
               "&c[Spieler] &7became part of the Earth's mantle.",
               "&c[Spieler] &7didn't survive the lava spa."
            )
         );
      this.deMessages
         .put(
            "explosion",
            Arrays.asList(
               "&c[Spieler] &7wurde zu Minecraft-Konfetti verarbeitet.",
               "&c[Spieler] &7hat den Boom nicht kommen sehen.",
               "&c[Spieler] &7wurde in mehrere Chunks verteilt.",
               "&c[Spieler] &7verlor den Konflikt mit der Druckwelle.",
               "&c[Spieler] &7wurde dezent entmaterialisiert."
            )
         );
      this.enMessages
         .put(
            "explosion",
            Arrays.asList(
               "&c[Spieler] &7was processed into Minecraft confetti.",
               "&c[Spieler] &7didn't see the boom coming.",
               "&c[Spieler] &7was distributed across several chunks.",
               "&c[Spieler] &7lost the argument with the shockwave.",
               "&c[Spieler] &7was gently dematerialized."
            )
         );
      this.deMessages
         .put(
            "mob_melee",
            Arrays.asList(
               "&c[Spieler] &7wurde lokal überrannt.",
               "&c[Spieler] &7hat die Nachbarschaft unterschätzt.",
               "&c[Spieler] &7wurde vom PvE überzeugt.",
               "&c[Spieler] &7verlor die Diskussion mit einem Mob."
            )
         );
      this.enMessages
         .put(
            "mob_melee",
            Arrays.asList(
               "&c[Spieler] &7was locally overrun.",
               "&c[Spieler] &7underestimated the neighborhood.",
               "&c[Spieler] &7was convinced by PvE.",
               "&c[Spieler] &7lost the argument with a mob."
            )
         );
      this.deMessages
         .put(
            "projectile",
            Arrays.asList(
               "&c[Spieler] &7wurde aus sicherer Entfernung überzeugt.",
               "&c[Spieler] &7entdeckte Ballistik auf die harte Tour.",
               "&c[Spieler] &7traf einen Pfeil. Der Pfeil traf zurück.",
               "&c[Spieler] &7wurde von Aim Assist erwischt."
            )
         );
      this.enMessages
         .put(
            "projectile",
            Arrays.asList(
               "&c[Spieler] &7was convinced from a safe distance.",
               "&c[Spieler] &7discovered ballistics the hard way.",
               "&c[Spieler] &7hit an arrow. The arrow hit back.",
               "&c[Spieler] &7was caught by aim assist."
            )
         );
      this.deMessages
         .put(
            "pvp",
            Arrays.asList(
               "&c[Spieler] &7wurde demokratisch entfernt.",
               "&c[Spieler] &7verlor ein Duell um Lebenspunkte.",
               "&c[Spieler] &7wurde aus der Spielerliste aussortiert.",
               "&c[Spieler] &7fand heraus, warum PvP aktiviert ist."
            )
         );
      this.enMessages
         .put(
            "pvp",
            Arrays.asList(
               "&c[Spieler] &7was democratically removed.",
               "&c[Spieler] &7lost a duel for hit points.",
               "&c[Spieler] &7was removed from the player list.",
               "&c[Spieler] &7found out why PvP is enabled."
            )
         );
      this.deMessages
         .put(
            "drowning",
            Arrays.asList(
               "&c[Spieler] &7hat vergessen zu atmen.",
               "&c[Spieler] &7verlor gegen H₂O.",
               "&c[Spieler] &7wurde offiziell Teil des Meeres.",
               "&c[Spieler] &7atmete Wasser. Das Wasser gewann."
            )
         );
      this.enMessages
         .put(
            "drowning",
            Arrays.asList(
               "&c[Spieler] &7forgot to breathe.",
               "&c[Spieler] &7lost to H₂O.",
               "&c[Spieler] &7officially became part of the ocean.",
               "&c[Spieler] &7breathed water. The water won."
            )
         );
      this.deMessages
         .put(
            "suffocation",
            Arrays.asList(
               "&c[Spieler] &7verschmolz unfreiwillig mit der Architektur.",
               "&c[Spieler] &7lernte, dass Blöcke massiv sind.",
               "&c[Spieler] &7wurde von Geometrie besiegt.",
               "&c[Spieler] &7versuchte, mit Stein zu verschmelzen."
            )
         );
      this.enMessages
         .put(
            "suffocation",
            Arrays.asList(
               "&c[Spieler] &7involuntarily fused with architecture.",
               "&c[Spieler] &7learned that blocks are solid.",
               "&c[Spieler] &7was defeated by geometry.",
               "&c[Spieler] &7tried to merge with stone."
            )
         );
      this.deMessages
         .put(
            "cactus",
            Arrays.asList(
               "&c[Spieler] &7umarmte die Natur etwas zu fest.",
               "&c[Spieler] &7verlor gegen eine Pflanze.",
               "&c[Spieler] &7wurde von einem grünen Zylinder besiegt.",
               "&c[Spieler] &7fand Kakteen kuschelig. Fehler."
            )
         );
      this.enMessages
         .put(
            "cactus",
            Arrays.asList(
               "&c[Spieler] &7hugged nature a bit too hard.",
               "&c[Spieler] &7lost to a plant.",
               "&c[Spieler] &7was defeated by a green cylinder.",
               "&c[Spieler] &7found cacti cuddly. Mistake."
            )
         );
      this.deMessages
         .put(
            "freeze",
            Arrays.asList(
               "&c[Spieler] &7wurde auf Werkseinstellungen eingefroren.",
               "&c[Spieler] &7wurde zu Minecraft-Eis am Stiel.",
               "&c[Spieler] &7unterschätzte den Winter.",
               "&c[Spieler] &7erhielt ein dauerhaftes Cooldown."
            )
         );
      this.enMessages
         .put(
            "freeze",
            Arrays.asList(
               "&c[Spieler] &7was frozen to factory settings.",
               "&c[Spieler] &7became a Minecraft popsicle.",
               "&c[Spieler] &7underestimated winter.",
               "&c[Spieler] &7received a permanent cooldown."
            )
         );
      this.deMessages
         .put(
            "starvation",
            Arrays.asList(
               "&c[Spieler] &7hätte vielleicht essen sollen.",
               "&c[Spieler] &7entdeckte die Nachteile einer Diät.",
               "&c[Spieler] &7wurde Opfer der Inflation bei Brotpreisen.",
               "&c[Spieler] &7starb aus kulinarischen Gründen."
            )
         );
      this.enMessages
         .put(
            "starvation",
            Arrays.asList(
               "&c[Spieler] &7should have eaten maybe.",
               "&c[Spieler] &7discovered the downsides of a diet.",
               "&c[Spieler] &7fell victim to bread price inflation.",
               "&c[Spieler] &7died for culinary reasons."
            )
         );
      this.deMessages
         .put(
            "poison",
            Arrays.asList(
               "&c[Spieler] &7probierte die falschen Pilze.",
               "&c[Spieler] &7testete experimentelle Medizin.",
               "&c[Spieler] &7wurde biologisch optimiert. Rückwärts.",
               "&c[Spieler] &7hatte Nebenwirkungen."
            )
         );
      this.enMessages
         .put(
            "poison",
            Arrays.asList(
               "&c[Spieler] &7tried the wrong mushrooms.",
               "&c[Spieler] &7tested experimental medicine.",
               "&c[Spieler] &7was biologically optimized. In reverse.",
               "&c[Spieler] &7had side effects."
            )
         );
      this.deMessages
         .put(
            "wither",
            Arrays.asList(
               "&c[Spieler] &7wurde schrittweise deinstalliert.",
               "&c[Spieler] &7zerfiel in Echtzeit.",
               "&c[Spieler] &7wurde vom Wither-Abonnement gekündigt.",
               "&c[Spieler] &7verlor langsam die Existenz."
            )
         );
      this.enMessages
         .put(
            "wither",
            Arrays.asList(
               "&c[Spieler] &7was gradually uninstalled.",
               "&c[Spieler] &7decayed in real time.",
               "&c[Spieler] &7had their Wither subscription cancelled.",
               "&c[Spieler] &7slowly lost existence."
            )
         );
      this.deMessages
         .put(
            "lightning",
            Arrays.asList(
               "&c[Spieler] &7wurde vom Wetter persönlich begrüßt.",
               "&c[Spieler] &7lud sich versehentlich auf.",
               "&c[Spieler] &7hatte direkten Kontakt zur Cloud.",
               "&c[Spieler] &7wurde in 4K geröstet."
            )
         );
      this.enMessages
         .put(
            "lightning",
            Arrays.asList(
               "&c[Spieler] &7was personally greeted by the weather.",
               "&c[Spieler] &7accidentally charged themselves.",
               "&c[Spieler] &7had direct contact with the cloud.",
               "&c[Spieler] &7was roasted in 4K."
            )
         );
      this.deMessages
         .put(
            "void",
            Arrays.asList(
               "&c[Spieler] &7verließ die bekannte Realität.",
               "&c[Spieler] &7fiel aus der Welt. Wörtlich.",
               "&c[Spieler] &7entdeckte die Grenzen der Existenz.",
               "&c[Spieler] &7wurde vom Universum ausgespuckt."
            )
         );
      this.enMessages
         .put(
            "void",
            Arrays.asList(
               "&c[Spieler] &7left known reality.",
               "&c[Spieler] &7fell out of the world. Literally.",
               "&c[Spieler] &7discovered the limits of existence.",
               "&c[Spieler] &7was spat out by the universe."
            )
         );
      this.deMessages
         .put(
            "bed_explosion",
            Arrays.asList(
               "&c[Spieler] &7wurde Opfer absichtlichen Spieldesigns.",
               "&c[Spieler] &7versuchte im Nether zu schlafen. Mutig.",
               "&c[Spieler] &7erhielt eine spontane Bett-Detonation.",
               "&c[Spieler] &7wurde von Mojang persönlich korrigiert."
            )
         );
      this.enMessages
         .put(
            "bed_explosion",
            Arrays.asList(
               "&c[Spieler] &7fell victim to intentional game design.",
               "&c[Spieler] &7tried to sleep in the Nether. Brave.",
               "&c[Spieler] &7received a spontaneous bed detonation.",
               "&c[Spieler] &7was personally corrected by Mojang."
            )
         );
      this.deMessages
         .put(
            "block_explosion",
            Arrays.asList(
               "&c[Spieler] &7lud seinen Respawn etwas zu stark auf.",
               "&c[Spieler] &7spielte mit übernatürlicher Elektrik.",
               "&c[Spieler] &7überschätzte seine Ladefähigkeiten."
            )
         );
      this.enMessages
         .put(
            "block_explosion",
            Arrays.asList(
               "&c[Spieler] &7overloaded their respawn a bit too much.",
               "&c[Spieler] &7played with supernatural electricity.",
               "&c[Spieler] &7overestimated their charging capabilities."
            )
         );
      this.deMessages
         .put(
            "anvil",
            Arrays.asList(
               "&c[Spieler] &7wurde von Qualitätswerkzeug überzeugt.",
               "&c[Spieler] &7verlor gegen die Schwerkraft – erneut.",
               "&c[Spieler] &7wurde fachgerecht abgeflacht."
            )
         );
      this.enMessages
         .put(
            "anvil",
            Arrays.asList(
               "&c[Spieler] &7was convinced by quality tools.", "&c[Spieler] &7lost to gravity — again.", "&c[Spieler] &7was professionally flattened."
            )
         );
      this.deMessages.put("magma", Arrays.asList("&c[Spieler] &7tanzte auf heißen Fliesen.", "&c[Spieler] &7unterschätzte den Fußboden."));
      this.enMessages.put("magma", Arrays.asList("&c[Spieler] &7danced on hot tiles.", "&c[Spieler] &7underestimated the floor."));
      this.deMessages
         .put(
            "sonic_boom",
            Arrays.asList("&c[Spieler] &7hatte ein lautes Erlebnis.", "&c[Spieler] &7wurde akustisch dekonstruiert.", "&c[Spieler] &7verlor ein Schreiduelle.")
         );
      this.enMessages
         .put(
            "sonic_boom",
            Arrays.asList("&c[Spieler] &7had a loud experience.", "&c[Spieler] &7was acoustically deconstructed.", "&c[Spieler] &7lost a scream duel.")
         );
      this.deMessages
         .put(
            "generic",
            Arrays.asList(
               "&c[Spieler] &7hat die Realität verlassen.",
               "&c[Spieler] &7wurde dekompiliert.",
               "&c[Spieler] &7wurde vom Server zurückgerufen.",
               "&c[Spieler] &7existiert vorübergehend nicht mehr.",
               "&c[Spieler] &7traf eine irreversible Entscheidung."
            )
         );
      this.enMessages
         .put(
            "generic",
            Arrays.asList(
               "&c[Spieler] &7has left reality.",
               "&c[Spieler] &7was decompiled.",
               "&c[Spieler] &7was recalled by the server.",
               "&c[Spieler] &7temporarily no longer exists.",
               "&c[Spieler] &7made an irreversible decision."
            )
         );
   }
}
