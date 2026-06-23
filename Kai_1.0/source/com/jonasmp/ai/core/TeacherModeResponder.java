package com.jonasmp.ai.core;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import com.jonasmp.ai.decision.DecisionResult;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TeacherModeResponder {
   private final Random random = new Random();
   private static final String[] WARN_MESSAGES_DE = new String[]{
      "Ey ey! So redet man hier nicht, mein Freund. Pass auf deine Worte auf!",
      "Moment mal! Was war das gerade? Hier wird respektvoll miteinander umgegangen.",
      "Aha, da hat wohl jemand vergessen, wie man sich benehmt. Zurückhaltung, bitte!",
      "So nicht, Kamerad. Wir sind hier im virtuellen Klassenzimmer, nicht im Raucherkeller!",
      "Pst pst! Die Sprache bitte! Denk an die kleinen Pixelohren hier."
   };
   private static final String[] BLOCK_MESSAGES_DE = new String[]{
      "Das war's! Das reicht! Du kriegst jetzt ein Denkzettel. Aufpassen!",
      "Na warte! Solche Wörter fliegen hier raus wie ungeliebte Hausaufgaben!",
      "Aufgepasst! Ich bin nicht umsonst hier. Nächste Beleidigung = ernste Konsequenzen!",
      "Hör mal, Jungchen/Älterchen — wir machen hier keine Kneipenrunde. Anstand!",
      "Das ist ein Ort der Kultur und des feinen Umgangs! ...Okay, vielleicht nicht FEIN, aber respektvoll!"
   };
   private static final String[] SEVERE_MESSAGES_DE = new String[]{
      "DAS WAR DER LETZTE FEHLER! Ich habe es notiert! In ROT!",
      "Unverschämtheit hat hier keinen Platz! Geh dich erstmal in der Ecke besinnen!",
      "Na bravo! So spricht man also auf diesem Server? Dann lernen wir das jetzt BESSER!",
      "Das ist nicht nur unhöflich, das ist PEINLICH! Für dich und für alle, die das lesen müssen!",
      "Stopp! Sofort! Dieses Benehmen akzeptiere ich nicht! Nächstes Mal folgt eine richtige Strafe!"
   };
   private static final String[] WARN_MESSAGES_EN = new String[]{
      "Hey hey! We don't talk like that here, my friend. Watch your words!",
      "Hold on! What was that just now? We treat each other with RESPECT here.",
      "Aha, someone seems to have forgotten their manners. Restrain yourself, please!",
      "Not like that, buddy. This is a virtual classroom, not a bar!",
      "Psst psst! Mind your language! Think of the little pixel ears here."
   };
   private static final String[] BLOCK_MESSAGES_EN = new String[]{
      "That's it! Enough! You're getting a warning now. Pay attention!",
      "Oh just you wait! Those words fly out of here like unloved homework!",
      "Listen up! I'm not here for nothing. Next insult = serious consequences!",
      "Listen here, kiddo/old-timer — this isn't a pub. Decency, please!",
      "This is a place of culture and fine manners! ...Okay, maybe not FINE, but respectful!"
   };
   private static final String[] SEVERE_MESSAGES_EN = new String[]{
      "THAT WAS THE LAST STRAW! I have noted it! In RED!",
      "Insolence has no place here! Go stand in the corner and think about what you did!",
      "Well done! So THAT'S how we talk on this server? Then we'll learn BETTER now!",
      "That is not just rude, it is EMBARRASSING! For you and for everyone who has to read it!",
      "Stop! Immediately! I will not accept this behavior! Next time there will be real punishment!"
   };
   private static final String[] TITLE_WARN = new String[]{"§c§lATTENTION!", "§e§lWATCH YOUR TONGUE!", "§6§lBEHAVE!", "§c§lNO NO NO!", "§4§lEXCUSE ME?!"};
   private static final String[] TITLE_BLOCK = new String[]{
      "§4§lTHAT'S IT!", "§c§lSTOP RIGHT NOW!", "§4§lUNACCEPTABLE!", "§c§lBEHAVE OR LEAVE!", "§4§lLAST WARNING!"
   };
   private static final String[] TITLE_SEVERE = new String[]{
      "§4§lOUTRAGEOUS!", "§4§lHOW DARE YOU!", "§c§lUNFORGIVABLE!", "§4§lDISRESPECTFUL!", "§c§lSHAME ON YOU!"
   };
   private static final String[] SUBTITLE_WARN = new String[]{
      "§fWatch your language, please", "§fWe don't talk like that here", "§fRespect is key", "§fMind your manners", "§fThis is a family-friendly zone"
   };
   private static final String[] SUBTITLE_BLOCK = new String[]{
      "§fYour message was blocked", "§fThat word is not welcome here", "§fClean it up or ship out", "§fTeacher is watching you", "§fNo second chances left"
   };
   private static final String[] SUBTITLE_SEVERE = new String[]{
      "§fYou have been noted", "§fYour behavior is recorded", "§fFinal warning issued", "§fConsequences will follow", "§fThe teacher is VERY disappointed"
   };

   public void respond(Player player, DecisionResult.Action action, String message, double score) {
      if (CoreBootstrap.CONFIG.isTeacherModeEnabled()) {
         Bukkit.getScheduler()
            .runTask(
               CoreBootstrap.PLUGIN,
               () -> {
                  String lang = CoreBootstrap.PLAYER_LANGUAGE_STORE != null ? CoreBootstrap.PLAYER_LANGUAGE_STORE.getLanguage(player.getUniqueId()) : "de";
                  TeacherModeResponder.Severity severity = this.determineSeverity(action, score);
                  this.sendTitle(player, severity);
                  String teacherMsg = this.pickMessage(severity, lang);
                  String label = "en".equals(lang) ? "[AI Teacher]" : "[AI Lehrer]";
                  player.sendMessage("");
                  player.sendMessage("§8§m----------------------------");
                  player.sendMessage("§c§l" + label + " §f" + teacherMsg);
                  player.sendMessage("§8§m----------------------------");
                  player.sendMessage("");
                  if (severity == TeacherModeResponder.Severity.SEVERE && CoreBootstrap.CONFIG.isPublicShamingEnabled()) {
                     for (Player nearby : Bukkit.getOnlinePlayers()) {
                        if (!nearby.equals(player) && nearby.hasPermission("jonasmpai.moderation.notify")) {
                           String shame = "en".equals(lang)
                              ? "§8[AI] §7" + player.getName() + " was reprimanded by the teacher."
                              : "§8[AI] §7" + player.getName() + " wurde vom Lehrer zurechtgewiesen.";
                           nearby.sendMessage(shame);
                        }
                     }
                  }

                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.5F);
               }
            );
      }
   }

   private TeacherModeResponder.Severity determineSeverity(DecisionResult.Action action, double score) {
      return switch (action) {
         case BAN, KICK -> TeacherModeResponder.Severity.SEVERE;
         case BLOCK -> score >= 0.7 ? TeacherModeResponder.Severity.SEVERE : TeacherModeResponder.Severity.BLOCK;
         case WARN -> score >= 0.4 ? TeacherModeResponder.Severity.BLOCK : TeacherModeResponder.Severity.WARN;
         default -> TeacherModeResponder.Severity.WARN;
      };
   }

   private void sendTitle(Player player, TeacherModeResponder.Severity severity) {
      String title = null;
      String subtitle = null;
      switch (severity) {
         case BLOCK:
            title = TITLE_BLOCK[this.random.nextInt(TITLE_BLOCK.length)];
            subtitle = SUBTITLE_BLOCK[this.random.nextInt(SUBTITLE_BLOCK.length)];
            break;
         case SEVERE:
            title = TITLE_SEVERE[this.random.nextInt(TITLE_SEVERE.length)];
            subtitle = SUBTITLE_SEVERE[this.random.nextInt(SUBTITLE_SEVERE.length)];
            break;
         default:
            title = TITLE_WARN[this.random.nextInt(TITLE_WARN.length)];
            subtitle = SUBTITLE_WARN[this.random.nextInt(SUBTITLE_WARN.length)];
      }

      player.sendTitle(title, subtitle, 10, 70, 20);
   }

   private String pickMessage(TeacherModeResponder.Severity severity, String lang) {
      boolean en = "en".equals(lang);

      return switch (severity) {
         case BLOCK -> en ? BLOCK_MESSAGES_EN[this.random.nextInt(BLOCK_MESSAGES_EN.length)] : BLOCK_MESSAGES_DE[this.random.nextInt(BLOCK_MESSAGES_DE.length)];
         case SEVERE -> en
         ? SEVERE_MESSAGES_EN[this.random.nextInt(SEVERE_MESSAGES_EN.length)]
         : SEVERE_MESSAGES_DE[this.random.nextInt(SEVERE_MESSAGES_DE.length)];
         default -> en ? WARN_MESSAGES_EN[this.random.nextInt(WARN_MESSAGES_EN.length)] : WARN_MESSAGES_DE[this.random.nextInt(WARN_MESSAGES_DE.length)];
      };
   }

   private static enum Severity {
      WARN,
      BLOCK,
      SEVERE;
   }
}
