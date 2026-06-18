package com.jonasmp.ai.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lean bootstrap holder for Kai 2.0.
 *
 * <p>Kai 1.0 used this class as a god-object wiring chat-moderation, an external
 * OpenRouter gateway, a shop bridge and more. Kai 2.0 is a PvP/survival agent
 * that runs entirely in-process, so the only shared state the ported subsystems
 * still need is a reference to the owning plugin (for logger, config, data
 * folder and scheduler access).
 */
public final class CoreBootstrap {

   /** The active plugin instance. Assigned once in {@link #init(JavaPlugin)}. */
   public static JavaPlugin PLUGIN;

   private CoreBootstrap() {
   }

   public static void init(JavaPlugin plugin) {
      PLUGIN = plugin;
   }
}
