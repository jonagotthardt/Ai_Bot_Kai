package com.jonasmp.ai.config;

public class DebugConfig {
   private static boolean debugEnabled = true;
   private static boolean voiceDebugEnabled = false;

   public static boolean isDebugEnabled() {
      return debugEnabled;
   }

   public static void setDebugEnabled(boolean enabled) {
      debugEnabled = enabled;
   }

   public static boolean isVoiceDebugEnabled() {
      return voiceDebugEnabled;
   }

   public static void setVoiceDebugEnabled(boolean enabled) {
      voiceDebugEnabled = enabled;
   }
}
