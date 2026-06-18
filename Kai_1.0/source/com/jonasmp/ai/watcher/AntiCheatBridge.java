package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class AntiCheatBridge implements Listener {
   private final WatcherCore core;
   private boolean vulcanAvailable = false;
   private Class<?> vulcanFlagEventClass = null;
   private Class<?> vulbanBanEventClass = null;
   private Method getPlayerMethod = null;
   private Method getCheckMethod = null;
   private Method getVlMethod = null;
   private Method getInfoMethod = null;

   public AntiCheatBridge(WatcherCore core) {
      this.core = core;
      this.detectVulcan();
   }

   private void detectVulcan() {
      Plugin vulcan = Bukkit.getPluginManager().getPlugin("Vulcan");
      if (vulcan == null) {
         CoreBootstrap.PLUGIN.getLogger().info("[Watcher/AntiCheat] Vulcan not detected. Anti-cheat bridge disabled.");
      } else {
         try {
            this.vulcanFlagEventClass = Class.forName("me.frep.vulcan.api.event.VulcanFlagEvent");
         } catch (ClassNotFoundException var8) {
            try {
               this.vulcanFlagEventClass = Class.forName("me.frep.vulcan.api.VulcanFlagEvent");
            } catch (ClassNotFoundException var7) {
               try {
                  this.vulcanFlagEventClass = Class.forName("me.frep.vulcan.spigot.api.event.VulcanFlagEvent");
               } catch (ClassNotFoundException var6) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[Watcher/AntiCheat] Could not find Vulcan flag event class. Bridge partially disabled.");
                  this.vulcanAvailable = true;
                  return;
               }
            }
         }

         try {
            this.getPlayerMethod = this.findMethod(this.vulcanFlagEventClass, "getPlayer", Player.class);
            if (this.getPlayerMethod == null) {
               this.getPlayerMethod = this.findMethod(this.vulcanFlagEventClass, "getPlayer");
            }

            this.getCheckMethod = this.findMethod(this.vulcanFlagEventClass, "getCheck", String.class);
            if (this.getCheckMethod == null) {
               this.getCheckMethod = this.findMethod(this.vulcanFlagEventClass, "getCheckName", String.class);
            }

            if (this.getCheckMethod == null) {
               this.getCheckMethod = this.findMethod(this.vulcanFlagEventClass, "getCheck");
            }

            this.getVlMethod = this.findMethod(this.vulcanFlagEventClass, "getVl", double.class, Double.class, int.class, Integer.class);
            if (this.getVlMethod == null) {
               this.getVlMethod = this.findMethod(this.vulcanFlagEventClass, "getViolationLevel");
            }

            if (this.getVlMethod == null) {
               this.getVlMethod = this.findMethod(this.vulcanFlagEventClass, "getViolations");
            }

            this.getInfoMethod = this.findMethod(this.vulcanFlagEventClass, "getInfo", String.class);
            if (this.getInfoMethod == null) {
               this.getInfoMethod = this.findMethodAny(this.vulcanFlagEventClass, "getType", "getDetails", "getDescription");
            }

            this.vulcanAvailable = true;
            CoreBootstrap.PLUGIN.getLogger().info("[Watcher/AntiCheat] Vulcan bridge active. Flag event class: " + this.vulcanFlagEventClass.getName());
         } catch (Exception var5) {
            CoreBootstrap.PLUGIN.getLogger().warning("[Watcher/AntiCheat] Vulcan detected but API reflection failed: " + var5.getMessage());
            this.vulcanAvailable = true;
         }
      }
   }

   public void register() {
      if (this.vulcanAvailable) {
         try {
            if (this.vulcanFlagEventClass != null) {
               Bukkit.getPluginManager()
                  .registerEvent(
                     this.vulcanFlagEventClass, this, EventPriority.MONITOR, (listener, event) -> this.handleVulcanFlag(event), CoreBootstrap.PLUGIN
                  );
               CoreBootstrap.PLUGIN.getLogger().info("[Watcher/AntiCheat] Registered Vulcan flag listener.");
            }
         } catch (Exception var2) {
            CoreBootstrap.PLUGIN.getLogger().warning("[Watcher/AntiCheat] Could not register Vulcan event listener: " + var2.getMessage());
         }
      }
   }

   public void unregister() {
      HandlerList.unregisterAll(this);
   }

   private void handleVulcanFlag(Event event) {
      try {
         Player player = (Player)this.getPlayerMethod.invoke(event);
         String checkName = (String)(this.getCheckMethod != null ? this.getCheckMethod.invoke(event) : "unknown");
         Number vl = 0;
         if (this.getVlMethod != null && this.getVlMethod.invoke(event) instanceof Number n) {
            vl = n;
         }

         String info = "";
         if (this.getInfoMethod != null) {
            Object infoObj = this.getInfoMethod.invoke(event);
            if (infoObj != null) {
               info = infoObj.toString();
            }
         }

         if (player == null || !player.isOnline()) {
            return;
         }

         double violation = vl.doubleValue();
         CoreBootstrap.PLUGIN
            .getLogger()
            .info("[Watcher/AntiCheat] Vulcan flag: " + player.getName() + " | " + checkName + " | vl=" + violation + " | " + info);
         this.core.getObserver().onAntiCheatFlag(player, "Vulcan:" + checkName, violation);
         this.handleFlagResponse(player, checkName, violation);
      } catch (Exception var8) {
      }
   }

   private void handleFlagResponse(Player player, String checkName, double violation) {
      WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
      int totalStrikes = data.getTotalStrikes();
      if (!(violation >= 15.0)) {
         if (violation >= 5.0 && this.core.getState() == WatcherState.IDLE) {
            this.core.observePlayer(player.getUniqueId());
         }
      } else {
         if (this.core.getState() == WatcherState.IDLE || this.core.getState() == WatcherState.OBSERVING) {
            if (data.getAntiCheatStrikes() >= 2) {
               this.core.beginInvestigation(player.getUniqueId());
            } else {
               this.core.observePlayer(player.getUniqueId());
            }
         }
      }
   }

   private Method findMethod(Class<?> clazz, String name, Class<?>... returnTypes) {
      for (Method m : clazz.getMethods()) {
         if (m.getName().equals(name)) {
            for (Class<?> rt : returnTypes) {
               if (rt.isAssignableFrom(m.getReturnType())) {
                  m.setAccessible(true);
                  return m;
               }
            }
         }
      }

      return null;
   }

   private Method findMethodAny(Class<?> clazz, String... names) {
      for (String name : names) {
         Method m = this.findMethod(clazz, name, Object.class);
         if (m != null) {
            return m;
         }
      }

      return null;
   }

   public boolean isVulcanAvailable() {
      return this.vulcanAvailable;
   }

   public void reportFlag(Player player, String checkName, double violation, String source) {
      if (this.vulcanAvailable) {
         this.core.getObserver().onAntiCheatFlag(player, source + ":" + checkName, violation);
         WatcherPlayerData data = this.core.ensurePlayerData(player.getUniqueId(), player.getName());
         if (violation >= 10.0 && this.core.getState() == WatcherState.IDLE) {
            this.core.observePlayer(player.getUniqueId());
         } else if (violation >= 20.0 && (this.core.getState() == WatcherState.IDLE || this.core.getState() == WatcherState.OBSERVING)) {
            this.core.beginInvestigation(player.getUniqueId());
         }
      }
   }
}
