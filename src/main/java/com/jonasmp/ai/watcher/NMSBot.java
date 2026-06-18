package com.jonasmp.ai.watcher;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class NMSBot {
   private Object serverPlayer;
   private Player craftPlayer;
   private UUID uuid;
   private String name;
   private boolean spawned = false;
   private Location currentLoc;
   private long lastRespawnTime = 0L;
   private static final long RESPAWN_COOLDOWN_MS = 3000L;
   private Object fppBot;
   private static final String HEROBRINE_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc3NjA0Njk1NDQwOCwKICAicHJvZmlsZUlkIiA6ICJhYzY1NDYwOWVkZjM0ODhmOTM0ZWNhMDRmNjlkNGIwMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFjZUd1cmxTa3kiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTVmYTQyY2ZmODI4MjA3MTllYTg3MWM5MWQ0ZWQ2ODE4ZGQ5ZDM0Yzk4NGQ1NDA3M2I1ZTlmOWM4ZTU4NjA1MyIKICAgIH0KICB9Cn0=";
   private static final String HEROBRINE_SKIN_SIGNATURE = "DVAgcUUBrgLNYx1UF5cbM+UeEKNUB0UfqfgfaemLcLJ4dtshNRcvzBxG395Z6xqbvf/yKIwrGDqngau+kWF7NHH4zmSNxNfPw5AZcHL7qRR9uhzWpfOPiGz3Fxxxfh9TWDnf0g6sunC7A1ub2zCne/BsSqi+2IgsN6kkiWMmqBIXkGA9QQkGHeAVl4gU7mDO1kdRRvPhBRKw9v6tRF5wuXgD8Z1XuvRiNE6JSsLLjl9no/1XrXtRSOuIQx+wM52koVKgsOOnwbUO/kf49cTK79rX98xvW6VwV2xxDKFIfAni8vp8+38KPORWx1Hg/D7XnushwgBg5qHXRECB4K0lKgLyMZLUPEqRFv2BUCxJo2vvYfqZtwFKWm/g0Y7MLExP1nQYfj89uEpjCYfT0Bdm/Xsbp2uR2xQQidIpTd493rUG/t7VXHFlggt7JSuS+r7l6Frl65pP04og8hnoSuv2l5iByG1RSIk0Qbjt1K/kkPhRvqIrWcUBPjzfNX9piLXEV3KhWhgS7fBYox2uSTydIwjSW6RZzuOZVu4FevPd86JY9MPWqKTS1uofrhIXPEpK/riIx2DN6g3wdu5G9cUB4LKDKyPJyF5r1lJhr2yz6QlWeo4KDhcoom6RA7UXjjoI6hRUUuAT3Ps4WE9OrQrioOzPadP4wz4lV7n0m40VdqU=";
   private static Object fppApi = null;
   private Method cachedSetYRot;
   private Method cachedSetXRot;
   private long lastJumpTime = 0L;
   private static final long JUMP_COOLDOWN_MS = 600L;
   private Block miningTarget;
   private int miningProgress = 0;
   private int miningRequired = 0;
   private boolean isMiningActive = false;

   public void spawn(Location loc, String botName) {
      this.spawn(loc, botName, null);
   }

   public void spawn(Location loc, String botName, UUID fixedUuid) {
      if (!this.spawned) {
         this.currentLoc = loc;
         this.name = botName;
         this.uuid = fixedUuid != null ? fixedUuid : UUID.randomUUID();
         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Spawning bot '" + botName + "' at " + loc + " UUID=" + this.uuid);
         this.preloadLuckPermsUser(this.uuid, botName);
         this.fetchSkinAndSpawn(loc, botName);
      }
   }

   private void preloadLuckPermsUser(UUID uuid, String botName) {
      try {
         LuckPerms api = LuckPermsProvider.get();
         UserManager um = api.getUserManager();
         CompletableFuture<User> future = um.loadUser(uuid);
         User user = future.get(3L, TimeUnit.SECONDS);
         if (user != null) {
            CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] LuckPerms user preloaded for '" + botName + "'.");
         }
      } catch (Exception var7) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] LuckPerms preload failed (ignored): " + var7.getClass().getSimpleName() + " " + var7.getMessage());
      }
   }

   private void fetchSkinAndSpawn(Location loc, String botName) {
      Bukkit.getScheduler()
         .runTask(
            CoreBootstrap.PLUGIN,
            () -> this.createEntity(
                  loc,
                  botName,
                  "ewogICJ0aW1lc3RhbXAiIDogMTc3NjA0Njk1NDQwOCwKICAicHJvZmlsZUlkIiA6ICJhYzY1NDYwOWVkZjM0ODhmOTM0ZWNhMDRmNjlkNGIwMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFjZUd1cmxTa3kiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTVmYTQyY2ZmODI4MjA3MTllYTg3MWM5MWQ0ZWQ2ODE4ZGQ5ZDM0Yzk4NGQ1NDA3M2I1ZTlmOWM4ZTU4NjA1MyIKICAgIH0KICB9Cn0=",
                  "DVAgcUUBrgLNYx1UF5cbM+UeEKNUB0UfqfgfaemLcLJ4dtshNRcvzBxG395Z6xqbvf/yKIwrGDqngau+kWF7NHH4zmSNxNfPw5AZcHL7qRR9uhzWpfOPiGz3Fxxxfh9TWDnf0g6sunC7A1ub2zCne/BsSqi+2IgsN6kkiWMmqBIXkGA9QQkGHeAVl4gU7mDO1kdRRvPhBRKw9v6tRF5wuXgD8Z1XuvRiNE6JSsLLjl9no/1XrXtRSOuIQx+wM52koVKgsOOnwbUO/kf49cTK79rX98xvW6VwV2xxDKFIfAni8vp8+38KPORWx1Hg/D7XnushwgBg5qHXRECB4K0lKgLyMZLUPEqRFv2BUCxJo2vvYfqZtwFKWm/g0Y7MLExP1nQYfj89uEpjCYfT0Bdm/Xsbp2uR2xQQidIpTd493rUG/t7VXHFlggt7JSuS+r7l6Frl65pP04og8hnoSuv2l5iByG1RSIk0Qbjt1K/kkPhRvqIrWcUBPjzfNX9piLXEV3KhWhgS7fBYox2uSTydIwjSW6RZzuOZVu4FevPd86JY9MPWqKTS1uofrhIXPEpK/riIx2DN6g3wdu5G9cUB4LKDKyPJyF5r1lJhr2yz6QlWeo4KDhcoom6RA7UXjjoI6hRUUuAT3Ps4WE9OrQrioOzPadP4wz4lV7n0m40VdqU="
               )
         );
   }

   private Object getFppApi() {
      if (fppApi != null) {
         return fppApi;
      } else {
         try {
            Plugin fpp = Bukkit.getPluginManager().getPlugin("FakePlayerPlugin");
            if (fpp == null) {
               CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] FakePlayerPlugin not found. Cannot spawn bot.");
               return null;
            } else {
               Method getApi = fpp.getClass().getMethod("getFppApi");
               fppApi = getApi.invoke(fpp);
               CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] FPP API connected.");
               return fppApi;
            }
         } catch (Exception var3) {
            CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] FPP API connect failed: " + var3.getMessage());
            return null;
         }
      }
   }

   private void createEntity(Location loc, final String botName, String skinValue, String skinSignature) {
      try {
         Object api = this.getFppApi();
         if (api == null) {
            CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] FPP not available. Aborting spawn.");
            return;
         }

         Method spawnBot = api.getClass().getMethod("spawnBot", Location.class, Player.class, String.class);
         Object optBot = spawnBot.invoke(api, loc, null, botName);
         Method isPresent = optBot.getClass().getMethod("isPresent");
         boolean present = (Boolean)isPresent.invoke(optBot);
         if (present) {
            Method getMethod = optBot.getClass().getMethod("get");
            this.fppBot = getMethod.invoke(optBot);
         } else {
            CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] FPP spawnBot returned empty Optional (bot may already exist from persistence).");
            this.craftPlayer = Bukkit.getPlayerExact(botName);
            if (this.craftPlayer != null) {
               CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Found existing bot player via Bukkit.");

               try {
                  for (Method m : api.getClass().getMethods()) {
                     if (m.getName().equals("getBot") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        this.fppBot = m.invoke(api, botName);
                        if (this.fppBot != null) {
                           break;
                        }
                     }
                  }
               } catch (Exception var14) {
               }
            }

            if (this.fppBot == null && this.craftPlayer == null) {
               CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] Bot not found after empty Optional.");
               return;
            }
         }

         if (this.tryInitializeBot(botName)) {
            return;
         }

         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Bot login pending, scheduling delayed init...");
         (new BukkitRunnable() {
            int attempts;

            {
               Objects.requireNonNull(NMSBot.this);
               this.attempts = 0;
            }

            public void run() {
               this.attempts++;
               if (NMSBot.this.tryInitializeBot(botName)) {
                  this.cancel();
               } else if (this.attempts >= 40) {
                  CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] Bot init timed out after 10s.");
                  this.cancel();
               }
            }
         }).runTaskTimer(CoreBootstrap.PLUGIN, 5L, 5L);
      } catch (Exception var15) {
         CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] Spawn failed: " + var15.getMessage());
         var15.printStackTrace();
      }
   }

   private boolean tryInitializeBot(String botName) {
      if (this.spawned && this.craftPlayer != null) {
         return true;
      } else {
         if (this.craftPlayer == null && this.fppBot != null) {
            String[] array = new String[]{"getPlayer", "getBukkitPlayer", "player", "getEntity", "getCraftPlayer"};

            for (String cand : array) {
               try {
                  Method m = this.fppBot.getClass().getMethod(cand);
                  if (Player.class.isAssignableFrom(m.getReturnType())) {
                     this.craftPlayer = (Player)m.invoke(this.fppBot);
                     if (this.craftPlayer != null) {
                        CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Found player via " + cand + "()");
                        break;
                     }
                  }
               } catch (Exception var26) {
               }
            }
         }

         if (this.craftPlayer == null) {
            this.craftPlayer = Bukkit.getPlayerExact(botName);
            if (this.craftPlayer != null) {
               CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Found player via Bukkit.getPlayerExact()");
            }
         }

         if (this.craftPlayer == null) {
            return false;
         } else {
            try {
               this.uuid = this.craftPlayer.getUniqueId();
               Object craftServer = Bukkit.getServer();
               Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
               ClassLoader cl = minecraftServer.getClass().getClassLoader();
               Class<?> spClass = cl.loadClass("net.minecraft.server.level.ServerPlayer");
               Method getHandle = this.craftPlayer.getClass().getMethod("getHandle");
               this.serverPlayer = getHandle.invoke(this.craftPlayer);

               try {
                  Class<?> gameTypeClass = cl.loadClass("net.minecraft.world.level.GameType");
                  Object survival = gameTypeClass.getField("SURVIVAL").get(null);
                  Field gameModeField = spClass.getDeclaredField("gameMode");
                  gameModeField.setAccessible(true);
                  Object spgm = gameModeField.get(this.serverPlayer);
                  if (spgm != null) {
                     String[] array2 = new String[]{"gameModeForPlayer", "gameMode", "k", "l"};

                     for (String cand2 : array2) {
                        try {
                           Field f = spgm.getClass().getDeclaredField(cand2);
                           if (f.getType() == gameTypeClass) {
                              f.setAccessible(true);
                              f.set(spgm, survival);
                              break;
                           }
                        } catch (NoSuchFieldException var23) {
                        }
                     }
                  }
               } catch (Exception var24) {
                  CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] gameMode set skipped: " + var24.getMessage());
               }

               try {
                  Field noPhysics = this.findField(spClass, "noPhysics");
                  if (noPhysics != null) {
                     noPhysics.setBoolean(this.serverPlayer, false);
                  }
               } catch (Exception var22) {
               }

               try {
                  Field invulnerable = this.findField(spClass, "invulnerable");
                  if (invulnerable != null) {
                     invulnerable.setBoolean(this.serverPlayer, false);
                  }
               } catch (Exception var21) {
               }

               Location ploc = this.craftPlayer.getLocation();
               this.initPreviousPosition(this.serverPlayer, ploc.getX(), ploc.getY(), ploc.getZ());

               try {
                  Field listed = this.findField(spClass, "listed");
                  if (listed != null) {
                     listed.setBoolean(this.serverPlayer, true);
                  }
               } catch (Exception var20) {
               }

               if (this.craftPlayer != null) {
                  try {
                     this.craftPlayer.setViewDistance(2);
                  } catch (Exception var19) {
                  }

                  try {
                     this.craftPlayer.setSimulationDistance(2);
                  } catch (Exception var18) {
                  }
               }

               this.spawned = true;
               CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Bot '" + botName + "' initialized via FPP. UUID=" + this.uuid);
               return true;
            } catch (Exception var25) {
               CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] Bot init failed: " + var25.getMessage());
               var25.printStackTrace();
               return false;
            }
         }
      }
   }

   private void injectSkin(ClassLoader cl, Object profile, String value, String signature) throws Exception {
      Class<?> propClass = cl.loadClass("com.mojang.authlib.properties.Property");

      Object property;
      try {
         Constructor<?> ctor = propClass.getDeclaredConstructor(String.class, String.class, String.class);
         ctor.setAccessible(true);
         property = ctor.newInstance("textures", value, signature);
      } catch (NoSuchMethodException var12) {
         Constructor<?> ctor2 = propClass.getDeclaredConstructor(String.class, String.class);
         ctor2.setAccessible(true);
         property = ctor2.newInstance("textures", value);
      }

      Object pm = this.resolvePropertyMap(profile);
      if (pm != null) {
         this.clearExistingTextures(pm);

         for (Method m : this.getAllMethods(pm.getClass())) {
            if (m.getName().equals("put") && m.getParameterCount() == 2) {
               m.setAccessible(true);

               try {
                  m.invoke(pm, "textures", property);
               } catch (Exception var11) {
               }
            }
         }
      }
   }

   private Object resolvePropertyMap(Object gameProfile) {
      for (Method m : this.getAllMethods(gameProfile.getClass())) {
         if (m.getName().equals("getProperties") && m.getParameterCount() == 0) {
            try {
               m.setAccessible(true);
               return m.invoke(gameProfile);
            } catch (Exception var7) {
            }
         }
      }

      for (Method mx : this.getAllMethods(gameProfile.getClass())) {
         if (mx.getName().equals("properties") && mx.getParameterCount() == 0) {
            try {
               mx.setAccessible(true);
               return mx.invoke(gameProfile);
            } catch (Exception var6) {
            }
         }
      }

      for (Field f : this.getAllFields(gameProfile.getClass())) {
         if ("properties".equals(f.getName())) {
            try {
               f.setAccessible(true);
               return f.get(gameProfile);
            } catch (Exception var5) {
            }
         }
      }

      return null;
   }

   private void clearExistingTextures(Object propertyMap) {
      for (Method m : this.getAllMethods(propertyMap.getClass())) {
         if (m.getName().equals("removeAll") && m.getParameterCount() == 1) {
            try {
               m.setAccessible(true);
               m.invoke(propertyMap, "textures");
               return;
            } catch (Exception var6) {
            }
         }
      }

      for (Method mx : this.getAllMethods(propertyMap.getClass())) {
         if (mx.getName().equals("remove") && mx.getParameterCount() == 1) {
            try {
               mx.setAccessible(true);
               mx.invoke(propertyMap, "textures");
            } catch (Exception var5) {
            }
         }
      }
   }

   private Object createFakeConnection(ClassLoader cl) {
      try {
         Class<?> connClass = cl.loadClass("net.minecraft.network.Connection");
         Class<?> pfClass = cl.loadClass("net.minecraft.network.protocol.PacketFlow");
         Object serverbound = pfClass.getField("SERVERBOUND").get(null);
         Object conn = connClass.getConstructor(pfClass).newInstance(serverbound);
         InetSocketAddress fakeAddress = new InetSocketAddress("127.0.0.1", 25565);
         Field addressField = connClass.getDeclaredField("address");
         addressField.setAccessible(true);
         addressField.set(conn, fakeAddress);
         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] FakeConnection created (channel=null, packets dropped).");
         return conn;
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] FakeConnection failed: " + var8.getMessage());
         var8.printStackTrace();
         return null;
      }
   }

   private Object createNullChannel(ClassLoader cl) {
      try {
         Class<?> channelClass = cl.loadClass("io.netty.channel.Channel");
         Class<?> pipelineClass = cl.loadClass("io.netty.channel.ChannelPipeline");
         Class<?> futureClass = cl.loadClass("io.netty.channel.ChannelFuture");
         Class<?> promiseClass = cl.loadClass("io.netty.channel.ChannelPromise");
         Class<?> eventLoopClass = cl.loadClass("io.netty.channel.EventLoop");
         Class<?> unsafeClass = cl.loadClass("io.netty.channel.Channel$Unsafe");
         Object nullFuture = Proxy.newProxyInstance(cl, new Class[]{futureClass}, (proxy, method, args) -> {
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) {
               return true;
            } else if (rt == boolean.class || rt == Boolean.class) {
               return true;
            } else if (rt == int.class || rt == Integer.class) {
               return 0;
            } else if (rt == long.class || rt == Long.class) {
               return 0L;
            } else if (rt == void.class || rt == Void.class) {
               return null;
            } else if (rt == Throwable.class) {
               return null;
            } else {
               return rt == channelClass ? null : null;
            }
         });
         Object nullPipeline = Proxy.newProxyInstance(cl, new Class[]{pipelineClass}, (proxy, method, args) -> {
            Class<?> rt2 = method.getReturnType();
            if (rt2 == boolean.class || rt2 == Boolean.class) {
               return false;
            } else if (rt2 == int.class || rt2 == Integer.class) {
               return 0;
            } else if (rt2 == void.class || rt2 == Void.class) {
               return null;
            } else if (rt2 == channelClass) {
               return null;
            } else if (rt2 == futureClass || rt2 == promiseClass) {
               return nullFuture;
            } else {
               return rt2.isAssignableFrom(pipelineClass) ? proxy : null;
            }
         });
         return Proxy.newProxyInstance(cl, new Class[]{channelClass}, (proxy, method, args) -> {
            Class<?> rt3 = method.getReturnType();
            if (rt3 == boolean.class || rt3 == Boolean.class) {
               return false;
            } else if (rt3 == int.class || rt3 == Integer.class) {
               return 0;
            } else if (rt3 == long.class || rt3 == Long.class) {
               return 0L;
            } else if (rt3 == void.class || rt3 == Void.class) {
               return null;
            } else if (rt3 == String.class) {
               return "";
            } else if (rt3 == channelClass) {
               return proxy;
            } else if (rt3 == pipelineClass) {
               return nullPipeline;
            } else if (rt3 == futureClass || rt3 == promiseClass) {
               return nullFuture;
            } else if (rt3 == eventLoopClass) {
               return null;
            } else {
               return rt3 == unsafeClass ? null : null;
            }
         });
      } catch (Exception var10) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] NullChannel creation failed: " + var10.getMessage());
         return null;
      }
   }

   private boolean placePlayer(Object minecraftServer, Object conn, Object serverPlayer, Object gameProfile, Object clientInfo) {
      try {
         Method getPL = minecraftServer.getClass().getMethod("getPlayerList");
         Object playerList = getPL.invoke(minecraftServer);
         if (playerList == null) {
            return false;
         }

         Object cookie = this.createCookie(gameProfile, clientInfo);
         if (cookie == null) {
            return false;
         }

         Method placeMethod = this.findMethod(playerList.getClass(), "placeNewPlayer", 3);
         if (placeMethod == null) {
            CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] placeNewPlayer not found on PlayerList.");
            return false;
         }

         placeMethod.setAccessible(true);

         for (int attempt = 0; attempt < 5; attempt++) {
            try {
               placeMethod.invoke(playerList, conn, serverPlayer, cookie);
               return true;
            } catch (Exception var14) {
               Throwable cause = (Throwable)(var14.getCause() != null ? var14.getCause() : var14);
               if (cause instanceof NullPointerException && attempt < 4) {
                  String msg = cause.getMessage();
                  if (msg != null
                     && (msg.toLowerCase().contains("worlddata") || msg.toLowerCase().contains("serverlevel") || msg.toLowerCase().contains("connections"))) {
                     Thread.sleep(50L);
                     continue;
                  }
               }

               CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] placeNewPlayer failed: " + cause);
               return false;
            }
         }
      } catch (Exception var15) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] placePlayer setup failed: " + var15.getMessage());
      }

      return false;
   }

   private Object createCookie(Object gameProfile, Object clientInfo) {
      try {
         Class<?> cookieClass = Class.forName("net.minecraft.server.network.CommonListenerCookie");

         for (Constructor<?> c : cookieClass.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length <= 0 || !p[p.length - 1].getSimpleName().contains("DefaultConstructorMarker")) {
               try {
                  Object result = null;
                  switch (p.length) {
                     case 1:
                        result = c.newInstance(gameProfile);
                        break;
                     case 2:
                        result = c.newInstance(gameProfile, 0);
                        break;
                     case 3:
                        result = c.newInstance(gameProfile, 0, clientInfo);
                        break;
                     case 4:
                        result = c.newInstance(gameProfile, 0, clientInfo, false);
                        break;
                     case 5:
                        result = c.newInstance(gameProfile, 0, clientInfo, false, false);
                     case 6:
                     default:
                        break;
                     case 7:
                        result = c.newInstance(gameProfile, 0, clientInfo, false, null, Collections.emptySet(), null);
                  }

                  if (result != null) {
                     return result;
                  }
               } catch (Exception var10) {
               }
            }
         }
      } catch (Exception var11) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] CommonListenerCookie creation failed: " + var11.getMessage());
      }

      return null;
   }

   private void ensurePlayerData(Object minecraftServer, Object serverPlayer, UUID uuid) {
      try {
         Method getPL = minecraftServer.getClass().getMethod("getPlayerList");
         Object playerList = getPL.invoke(minecraftServer);
         if (playerList == null) {
            return;
         }

         Field storageField = null;

         for (Field f : this.getAllFields(playerList.getClass())) {
            String typeName = f.getType().getSimpleName();
            if (typeName.contains("WorldNBTStorage") || typeName.contains("PlayerDataStorage")) {
               storageField = f;
               break;
            }
         }

         if (storageField == null) {
            return;
         }

         storageField.setAccessible(true);
         Object storage = storageField.get(playerList);
         if (storage == null) {
            return;
         }

         Method getDir = null;

         for (Method m : storage.getClass().getMethods()) {
            if (m.getName().equals("getPlayerDir") && m.getParameterCount() == 0) {
               getDir = m;
               break;
            }
         }

         File playerDir = null;
         if (getDir != null) {
            getDir.setAccessible(true);
            playerDir = (File)getDir.invoke(storage);
         }

         if (playerDir != null) {
            File datFile = new File(playerDir, uuid.toString() + ".dat");
            if (!datFile.exists()) {
               try {
                  ClassLoader nmsLoader = Bukkit.getServer().getClass().getClassLoader();
                  Class<?> compoundClass = nmsLoader.loadClass("net.minecraft.nbt.CompoundTag");
                  Object compound = compoundClass.getConstructor().newInstance();
                  Class<?> nbtIoClass = nmsLoader.loadClass("net.minecraft.nbt.NbtIo");
                  boolean written = false;

                  try {
                     Method writeCompressed = nbtIoClass.getMethod("writeCompressed", compoundClass, File.class);
                     writeCompressed.invoke(null, compound, datFile);
                     written = true;
                  } catch (NoSuchMethodException var25) {
                  }

                  if (!written) {
                     try {
                        Class<?> pathClass = Class.forName("java.nio.file.Path");
                        Method writeCompressed2 = nbtIoClass.getMethod("writeCompressed", compoundClass, pathClass);
                        Method toPath = File.class.getMethod("toPath");
                        Object path = toPath.invoke(datFile);
                        writeCompressed2.invoke(null, compound, path);
                        written = true;
                     } catch (NoSuchMethodException var24) {
                     }
                  }

                  if (!written) {
                     try {
                        Method writeCompressed = nbtIoClass.getMethod("writeCompressed", compoundClass, OutputStream.class);

                        try (FileOutputStream fos = new FileOutputStream(datFile)) {
                           writeCompressed.invoke(null, compound, fos);
                        }

                        written = true;
                     } catch (NoSuchMethodException var23) {
                     }
                  }

                  if (written) {
                     CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Created empty playerdata for bot.");
                  } else {
                     CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] No matching NbtIo.writeCompressed signature found.");
                  }
               } catch (Exception var26) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Could not create valid playerdata: " + var26.getMessage());
               }
            }
         }

         for (Method i : storage.getClass().getDeclaredMethods()) {
            if ("a".equals(i.getName()) && i.getParameterCount() == 1 && i.getReturnType() == void.class) {
               i.setAccessible(true);

               try {
                  i.invoke(storage, serverPlayer);
               } catch (Exception var20) {
               }
               break;
            }
         }
      } catch (Exception var27) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] PlayerData init skipped: " + var27.getMessage());
      }
   }

   private Object[] injectMinimalListener(ClassLoader cl, Object minecraftServer, Object serverPlayer, Object gameProfile, Object clientInfo) {
      Object[] result = new Object[2];

      try {
         Class<?> spClass = serverPlayer.getClass();
         Class<?> listenerClass = cl.loadClass("net.minecraft.server.network.ServerGamePacketListenerImpl");
         Class<?> connClass = cl.loadClass("net.minecraft.network.Connection");
         Class<?> mcClass = cl.loadClass("net.minecraft.server.MinecraftServer");
         Class<?> gpClass = cl.loadClass("com.mojang.authlib.GameProfile");
         Class<?> ciClass = cl.loadClass("net.minecraft.server.level.ClientInformation");
         Class<?> pfClass = cl.loadClass("net.minecraft.network.protocol.PacketFlow");
         Object serverbound = pfClass.getField("SERVERBOUND").get(null);
         Object conn = connClass.getConstructor(pfClass).newInstance(serverbound);
         result[0] = conn;
         InetSocketAddress fakeAddress = new InetSocketAddress("127.0.0.1", 25565);
         Object channel = this.createNullChannel(cl);
         if (channel != null) {
            Field channelField = connClass.getDeclaredField("channel");
            channelField.setAccessible(true);
            channelField.set(conn, channel);
         }

         Field addressField = connClass.getDeclaredField("address");
         addressField.setAccessible(true);
         addressField.set(conn, fakeAddress);
         Object listener = null;
         Object cookie = null;
         Constructor<?>[] constructors = listenerClass.getConstructors();
         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Found " + constructors.length + " ServerGamePacketListenerImpl constructors.");

         for (Constructor<?> c : constructors) {
            try {
               Class<?>[] p = c.getParameterTypes();
               if (p.length >= 3 && p[0] == mcClass && p[1] == connClass && p[2] == spClass) {
                  Object[] args = new Object[p.length];
                  args[0] = minecraftServer;
                  args[1] = conn;
                  args[2] = serverPlayer;

                  for (int i = 3; i < p.length; i++) {
                     Class<?> paramType = p[i];
                     if (paramType == gpClass) {
                        args[i] = gameProfile;
                     } else if (paramType == ciClass) {
                        args[i] = clientInfo;
                     } else if (paramType.getSimpleName().equals("CommonListenerCookie")) {
                        boolean created = false;

                        try {
                           Method createInitial = paramType.getMethod("createInitial", ciClass);
                           cookie = createInitial.invoke(null, clientInfo);
                           created = true;
                           CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] CommonListenerCookie created via createInitial.");
                        } catch (Exception var38) {
                        }

                        if (!created) {
                           for (Constructor<?> cc : paramType.getConstructors()) {
                              try {
                                 Class<?>[] cp = cc.getParameterTypes();
                                 Object[] cArgs = new Object[cp.length];

                                 for (int j = 0; j < cp.length; j++) {
                                    if (cp[j] == ciClass) {
                                       cArgs[j] = clientInfo;
                                    } else if (cp[j] == gpClass) {
                                       cArgs[j] = gameProfile;
                                    } else if (cp[j] == boolean.class) {
                                       cArgs[j] = false;
                                    } else if (cp[j] == int.class) {
                                       cArgs[j] = 0;
                                    } else {
                                       cArgs[j] = null;
                                    }
                                 }

                                 cookie = cc.newInstance(cArgs);
                                 CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] CommonListenerCookie created via constructor.");
                                 break;
                              } catch (Exception var39) {
                              }
                           }
                        }

                        if (cookie == null) {
                           CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] CommonListenerCookie could not be created.");
                        }

                        args[i] = cookie;
                     } else if (paramType == String.class) {
                        args[i] = "";
                     } else if (paramType.isPrimitive()) {
                        if (paramType == boolean.class) {
                           args[i] = false;
                        } else if (paramType == int.class) {
                           args[i] = 0;
                        } else if (paramType == long.class) {
                           args[i] = 0L;
                        } else {
                           args[i] = 0;
                        }
                     } else {
                        args[i] = null;
                     }
                  }

                  listener = c.newInstance(args);
                  CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Created ServerGamePacketListenerImpl with " + p.length + " params.");
                  break;
               }
            } catch (Exception var40) {
               String causeInfo = var40.getCause() != null ? var40.getCause().getClass().getSimpleName() + ": " + var40.getCause().getMessage() : "no cause";
               CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Constructor failed: " + var40.getClass().getSimpleName() + " -> " + causeInfo);
               if (var40.getCause() != null) {
                  var40.getCause().printStackTrace();
               }
            }
         }

         if (listener != null) {
            Field connField = spClass.getDeclaredField("connection");
            connField.setAccessible(true);
            connField.set(serverPlayer, listener);
            CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Injected minimal ServerGamePacketListenerImpl with EmbeddedChannel.");
         } else {
            CoreBootstrap.PLUGIN.getLogger().severe("[NMSBot] FAILED to create ServerGamePacketListenerImpl — no matching constructor!");
         }

         result[1] = cookie;
      } catch (Exception var41) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Minimal listener injection skipped: " + var41.getMessage());
         var41.printStackTrace();
      }

      return result;
   }

   private void addToPlayerList(Object minecraftServer, Object serverPlayer) {
      try {
         Method getPL = minecraftServer.getClass().getMethod("getPlayerList");
         Object playerList = getPL.invoke(minecraftServer);
         if (playerList == null) {
            return;
         }

         for (Field f : this.getAllFields(playerList.getClass())) {
            if (f.getType() == List.class) {
               f.setAccessible(true);
               List<Object> list = (List<Object>)f.get(playerList);
               if (list != null && !list.contains(serverPlayer)) {
                  list.add(serverPlayer);
                  CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Added bot to PlayerList.players.");
                  break;
               }
            }
         }

         for (Field fx : this.getAllFields(playerList.getClass())) {
            if (Map.class.isAssignableFrom(fx.getType())) {
               fx.setAccessible(true);
               Map<Object, Object> map = (Map<Object, Object>)fx.get(playerList);
               if (map != null && !map.containsKey(this.uuid)) {
                  map.put(this.uuid, serverPlayer);
                  CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Added bot to PlayerList.playersByUUID.");
                  break;
               }
            }
         }
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Could not add to PlayerList: " + var8.getMessage());
      }
   }

   private void initPreviousPosition(Object nmsEntity, double x, double y, double z) {
      if (nmsEntity != null) {
         try {
            Class<?> entityClass = nmsEntity.getClass();

            while (entityClass != null && !entityClass.getName().contains("Entity")) {
               entityClass = entityClass.getSuperclass();
            }

            if (entityClass == null) {
               entityClass = nmsEntity.getClass();
            }

            Field xo = this.findField(entityClass, "xo");
            Field yo = this.findField(entityClass, "yo");
            Field zo = this.findField(entityClass, "zo");
            if (xo != null) {
               xo.setAccessible(true);
               xo.setDouble(nmsEntity, x);
            }

            if (yo != null) {
               yo.setAccessible(true);
               yo.setDouble(nmsEntity, y);
            }

            if (zo != null) {
               zo.setAccessible(true);
               zo.setDouble(nmsEntity, z);
            }

            Field yRot = this.findField(entityClass, "yRot");
            Field yRotO = this.findField(entityClass, "yRotO");
            if (yRot != null && yRotO != null) {
               yRot.setAccessible(true);
               yRotO.setAccessible(true);
               float yaw = this.currentLoc != null ? this.currentLoc.getYaw() : 0.0F;
               yRot.setFloat(nmsEntity, yaw);
               yRotO.setFloat(nmsEntity, yaw);
            }
         } catch (Exception var15) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] initPreviousPosition failed: " + var15.getMessage());
         }
      }
   }

   private void broadcastSpawnPackets(Object serverPlayer) {
      try {
         Method getBukkit = serverPlayer.getClass().getMethod("getBukkitEntity");
         Player bukkit = (Player)getBukkit.invoke(serverPlayer);
         String joinMsg = "§e" + this.name + " joined the game";

         for (Player p : Bukkit.getOnlinePlayers()) {
            try {
               p.showPlayer(CoreBootstrap.PLUGIN, bukkit);
            } catch (Exception var9) {
            }
         }

         try {
            Bukkit.broadcastMessage(joinMsg);
         } catch (Exception var8) {
         }

         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Spawn packets broadcasted. Join message sent.");
      } catch (Exception var10) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] showPlayer broadcast failed: " + var10.getMessage());
      }
   }

   public void despawn() {
      try {
         Object api = this.getFppApi();
         if (api != null && this.name != null) {
            try {
               Method despawnBot = api.getClass().getMethod("despawnBot", String.class);
               despawnBot.invoke(api, this.name);
               CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Bot despawned via FPP.");
            } catch (Exception var7) {
               CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] FPP despawn call failed: " + var7.getMessage());
            }
         }
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Despawn error: " + var8.getMessage());
      } finally {
         this.spawned = false;
         this.serverPlayer = null;
         this.craftPlayer = null;
         this.fppBot = null;
      }
   }

   private void removeFromPlayerList() {
      try {
         Object craftServer = Bukkit.getServer();
         Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
         Method getPL = minecraftServer.getClass().getMethod("getPlayerList");
         Object playerList = getPL.invoke(minecraftServer);
         if (playerList == null) {
            return;
         }

         for (Field f : this.getAllFields(playerList.getClass())) {
            if (f.getType() == List.class) {
               f.setAccessible(true);
               List<Object> list = (List<Object>)f.get(playerList);
               if (list != null) {
                  list.remove(this.serverPlayer);
               }
            }
         }

         for (Field fx : this.getAllFields(playerList.getClass())) {
            if (Map.class.isAssignableFrom(fx.getType())) {
               fx.setAccessible(true);
               Map<Object, Object> map = (Map<Object, Object>)fx.get(playerList);
               if (map != null) {
                  map.remove(this.uuid);
               }
            }
         }

         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Removed bot from PlayerList.");
      } catch (Exception var8) {
         CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Could not remove from PlayerList: " + var8.getMessage());
      }
   }

   public void teleport(Location loc) {
      if (this.spawned && this.serverPlayer != null) {
         this.currentLoc = loc;

         try {
            Method setPos = this.findMethod(this.serverPlayer.getClass(), "setPos", 3, double.class, double.class, double.class);
            if (setPos != null) {
               setPos.invoke(this.serverPlayer, loc.getX(), loc.getY(), loc.getZ());
            }

            if (this.craftPlayer != null) {
               this.craftPlayer.teleport(loc);
               this.craftPlayer.setFallDistance(0.0F);
            }
         } catch (Exception var3) {
            CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Teleport failed: " + var3.getMessage());
         }
      }
   }

   public void setRotation(float yaw, float pitch) {
      if (this.spawned && this.serverPlayer != null) {
         try {
            if (this.cachedSetYRot == null) {
               this.cachedSetYRot = this.findMethod(this.serverPlayer.getClass(), "setYRot", 1, float.class);
            }

            if (this.cachedSetXRot == null) {
               this.cachedSetXRot = this.findMethod(this.serverPlayer.getClass(), "setXRot", 1, float.class);
            }

            if (this.cachedSetYRot != null) {
               this.cachedSetYRot.invoke(this.serverPlayer, yaw);
            }

            if (this.cachedSetXRot != null) {
               this.cachedSetXRot.invoke(this.serverPlayer, Math.max(-90.0F, Math.min(90.0F, pitch)));
            }

            if (this.currentLoc != null) {
               this.currentLoc.setYaw(yaw);
               this.currentLoc.setPitch(pitch);
            }
         } catch (Exception var4) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] setRotation failed: " + var4.getMessage());
         }
      }
   }

   public void lookAt(Location target) {
      if (this.spawned && this.currentLoc != null) {
         double dx = target.getX() - this.currentLoc.getX();
         double dy = target.getY() - this.currentLoc.getY() - 1.62;
         double dz = target.getZ() - this.currentLoc.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
         float pitch = (float)Math.toDegrees(Math.atan2(-dy, dist));
         this.setRotation(yaw, pitch);
      }
   }

   public void moveNMS(double forward, double strafe, double jump) {
      if (this.spawned && this.serverPlayer != null) {
         try {
            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Class<?> vec3Class = cl.loadClass("net.minecraft.world.phys.Vec3");
            Object movement = vec3Class.getConstructor(double.class, double.class, double.class).newInstance(forward, jump, strafe);
            Method travel = this.findMethod(this.serverPlayer.getClass(), "travel", 1, vec3Class);
            if (travel != null) {
               travel.invoke(this.serverPlayer, movement);
               this.updateLocationFromNMS();
            }
         } catch (Exception var11) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] NMS travel failed: " + var11.getMessage());
            if (this.craftPlayer != null) {
               Vector dir = new Vector(forward, jump, strafe).normalize().multiply(0.25);
               this.craftPlayer.setVelocity(dir);
            }
         }
      }
   }

   public void walkRelative(double forward, double sideways) {
      if (this.spawned && this.serverPlayer != null) {
         try {
            Class<?> spClass = this.serverPlayer.getClass();
            Field zza = this.findField(spClass, "zza");
            if (zza != null) {
               zza.setAccessible(true);
               zza.setFloat(this.serverPlayer, (float)Math.max(-1.0, Math.min(1.0, forward)));
            }

            Field xxa = this.findField(spClass, "xxa");
            if (xxa != null) {
               xxa.setAccessible(true);
               xxa.setFloat(this.serverPlayer, (float)Math.max(-1.0, Math.min(1.0, sideways)));
            }
         } catch (Exception var13) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] walkRelative input failed: " + var13.getMessage());
            if (this.craftPlayer != null) {
               float yaw = this.currentLoc != null ? this.currentLoc.getYaw() : this.craftPlayer.getLocation().getYaw();
               double yawRad = Math.toRadians((double)yaw);
               double mx = -Math.sin(yawRad) * forward + Math.cos(yawRad) * sideways;
               double mz = Math.cos(yawRad) * forward + Math.sin(yawRad) * sideways;
               this.craftPlayer.setVelocity(new Vector(mx * 0.25, this.craftPlayer.getVelocity().getY(), mz * 0.25));
            }
         }
      }
   }

   public void jump() {
      if (this.spawned && this.serverPlayer != null) {
         if (this.craftPlayer != null && this.craftPlayer.isOnGround()) {
            long now = System.currentTimeMillis();
            if (now - this.lastJumpTime >= 600L) {
               this.lastJumpTime = now;

               try {
                  Class<?> spClass = this.serverPlayer.getClass();
                  Field jumpingField = this.findField(spClass, "jumping");
                  if (jumpingField != null) {
                     jumpingField.setAccessible(true);
                     jumpingField.setBoolean(this.serverPlayer, true);
                  } else {
                     this.craftPlayer.setVelocity(this.craftPlayer.getVelocity().setY(0.42));
                  }
               } catch (Exception var5) {
                  CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] jump failed: " + var5.getMessage());
                  this.craftPlayer.setVelocity(this.craftPlayer.getVelocity().setY(0.42));
               }
            }
         }
      }
   }

   public void navigateTo(Location target) {
      if (this.spawned && this.fppBot != null) {
         try {
            Object api = this.getFppApi();
            if (api == null) {
               return;
            }

            for (Method m : api.getClass().getMethods()) {
               if (m.getName().equals("navigateTo")) {
                  Class<?>[] params = m.getParameterTypes();
                  if (params.length >= 2 && params[0].isAssignableFrom(this.fppBot.getClass()) && params[1] == Location.class) {
                     Object[] args = new Object[params.length];
                     args[0] = this.fppBot;
                     args[1] = target;

                     for (int i = 2; i < params.length; i++) {
                        args[i] = null;
                     }

                     m.invoke(api, args);
                     return;
                  }
               }
            }

            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] No suitable navigateTo found on FPP API.");
         } catch (Exception var10) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] navigateTo failed: " + var10.getMessage());
         }
      }
   }

   public void cancelNavigation() {
      if (this.spawned && this.fppBot != null) {
         try {
            Object api = this.getFppApi();
            if (api == null) {
               return;
            }

            for (Method m : api.getClass().getMethods()) {
               if (m.getName().equals("cancelNavigation") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(this.fppBot.getClass())) {
                  m.invoke(api, this.fppBot);
                  return;
               }
            }
         } catch (Exception var6) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] cancelNavigation failed: " + var6.getMessage());
         }
      }
   }

   public boolean isNavigating() {
      if (this.spawned && this.fppBot != null) {
         try {
            Object api = this.getFppApi();
            if (api == null) {
               return false;
            } else {
               for (Method m : api.getClass().getMethods()) {
                  if (m.getName().equals("isNavigating") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(this.fppBot.getClass())) {
                     Object r = m.invoke(api, this.fppBot);
                     return r instanceof Boolean && (Boolean)r;
                  }
               }

               return false;
            }
         } catch (Exception var7) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void tickPhysics() {
      if (this.spawned && this.serverPlayer != null && this.craftPlayer != null) {
         if (this.craftPlayer.isOnline() && !this.craftPlayer.isDead()) {
            try {
               Class<?> spClass = this.serverPlayer.getClass();
               Field jumpingField = this.findField(spClass, "jumping");
               if (jumpingField != null) {
                  jumpingField.setBoolean(this.serverPlayer, false);
               }

               Method tickMethod = this.findMethod(spClass, "tick", 0);
               if (tickMethod != null) {
                  tickMethod.invoke(this.serverPlayer);
               } else {
                  Method doTick = this.findMethod(spClass, "doTick", 0);
                  if (doTick != null) {
                     doTick.invoke(this.serverPlayer);
                  }
               }

               this.updateLocationFromNMS();
            } catch (Exception var5) {
            }
         }
      }
   }

   private void updateLocationFromNMS() {
      if (this.serverPlayer != null && this.craftPlayer != null) {
         try {
            Method getX = this.findMethod(this.serverPlayer.getClass(), "getX", 0);
            Method getY = this.findMethod(this.serverPlayer.getClass(), "getY", 0);
            Method getZ = this.findMethod(this.serverPlayer.getClass(), "getZ", 0);
            if (getX != null && getY != null && getZ != null) {
               double x = (Double)getX.invoke(this.serverPlayer);
               double y = (Double)getY.invoke(this.serverPlayer);
               double z = (Double)getZ.invoke(this.serverPlayer);
               if (this.currentLoc == null) {
                  this.currentLoc = this.craftPlayer.getLocation();
               }

               this.currentLoc.setX(x);
               this.currentLoc.setY(y);
               this.currentLoc.setZ(z);
            }
         } catch (Exception var10) {
         }
      }
   }

   public void respawnPlayer() {
      if (this.spawned && this.serverPlayer != null && this.craftPlayer != null) {
         if (this.craftPlayer.isDead()) {
            long now = System.currentTimeMillis();
            if (now - this.lastRespawnTime >= 3000L) {
               this.lastRespawnTime = now;

               try {
                  try {
                     Class<?> spigotClass = Class.forName("org.bukkit.entity.Player$Spigot");
                     Object spigot = this.craftPlayer.getClass().getMethod("spigot").invoke(this.craftPlayer);
                     Method respawn = spigotClass.getMethod("respawn");
                     respawn.invoke(spigot);
                     if (!this.craftPlayer.isDead()) {
                        CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Bot respawned via Spigot API.");
                        return;
                     }
                  } catch (Exception var9) {
                     CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] Spigot respawn failed: " + var9.getMessage());
                  }

                  ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
                  Class<?> spClass = this.serverPlayer.getClass();
                  Field deathTime = this.findField(spClass, "deathTime");
                  if (deathTime != null) {
                     deathTime.setInt(this.serverPlayer, 0);
                  }

                  Field dead = this.findField(spClass, "dead");
                  if (dead != null) {
                     dead.setBoolean(this.serverPlayer, false);
                  }

                  Method setHealth = this.findMethod(spClass, "setHealth", 1, float.class);
                  if (setHealth != null) {
                     setHealth.invoke(this.serverPlayer, 20.0F);
                  }

                  if (this.craftPlayer != null) {
                     this.craftPlayer.setFoodLevel(20);
                     this.craftPlayer.setFireTicks(0);
                     this.craftPlayer.setFallDistance(0.0F);
                  }

                  Location respawnLoc = AIPlayerBot.loadLastLocation();
                  if (respawnLoc == null || respawnLoc.getWorld() == null) {
                     respawnLoc = this.craftPlayer.getWorld().getSpawnLocation();
                     respawnLoc.setY((double)(this.craftPlayer.getWorld().getHighestBlockYAt(respawnLoc) + 1));
                  }

                  this.teleport(respawnLoc);
                  CoreBootstrap.PLUGIN
                     .getLogger()
                     .info("[NMSBot] Bot respawned via NMS fallback at " + respawnLoc.getBlockX() + "," + respawnLoc.getBlockY() + "," + respawnLoc.getBlockZ());
               } catch (Exception var10) {
                  CoreBootstrap.PLUGIN.getLogger().warning("[NMSBot] Respawn failed: " + var10.getMessage());
                  var10.printStackTrace();
               }
            }
         }
      }
   }

   public boolean placeBlock(Block targetBlock) {
      if (this.spawned && this.serverPlayer != null && targetBlock != null) {
         try {
            Object gameMode = this.getFieldValue(this.serverPlayer, "gameMode");
            if (gameMode == null) {
               return false;
            }

            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Class<?> blockPosClass = cl.loadClass("net.minecraft.core.BlockPos");
            Object blockPos = blockPosClass.getConstructor(int.class, int.class, int.class)
               .newInstance(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
            Class<?> handClass = cl.loadClass("net.minecraft.world.InteractionHand");
            Object mainHand = handClass.getField("MAIN_HAND").get(null);

            for (Method m : this.getAllMethods(gameMode.getClass())) {
               if (m.getName().equals("useItemOn") || m.getName().equals("a")) {
                  Class<?>[] params = m.getParameterTypes();
                  if (params.length >= 2 && params[0] == blockPosClass && params[1] == handClass) {
                     m.setAccessible(true);
                     Object result = m.invoke(gameMode, blockPos, mainHand);
                     return result != null && result.toString().contains("SUCCESS");
                  }
               }
            }
         } catch (Exception var12) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] NMS placeBlock failed: " + var12.getMessage());
         }

         if (targetBlock.getType() == Material.AIR && this.craftPlayer != null) {
            ItemStack hand = this.craftPlayer.getInventory().getItemInMainHand();
            if (hand.getType().isBlock()) {
               targetBlock.setType(hand.getType());
               hand.setAmount(hand.getAmount() - 1);
               this.swingMainHand();
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void dropItem(int slot) {
      if (this.spawned && this.craftPlayer != null) {
         ItemStack item = this.craftPlayer.getInventory().getItem(slot);
         if (item != null && item.getType() != Material.AIR) {
            try {
               ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
               Class<?> itemStackClass = cl.loadClass("net.minecraft.world.item.ItemStack");
               Method asNMSCopy = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
               Object nmsItem = asNMSCopy.invoke(null, item);
               Method drop = this.findMethod(this.serverPlayer.getClass(), "drop", 2, itemStackClass, boolean.class);
               if (drop != null) {
                  drop.invoke(this.serverPlayer, nmsItem, false);
                  this.craftPlayer.getInventory().setItem(slot, (ItemStack)null);
                  CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] Dropped item from slot " + slot);
               }
            } catch (Exception var8) {
               this.craftPlayer.getWorld().dropItemNaturally(this.craftPlayer.getLocation(), item);
               this.craftPlayer.getInventory().setItem(slot, (ItemStack)null);
            }
         }
      }
   }

   public void dropItemsOfType(Material type, int keepAmount) {
      if (this.spawned && this.craftPlayer != null) {
         PlayerInventory inv = this.craftPlayer.getInventory();

         for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == type && item.getAmount() > keepAmount) {
               int dropAmount = item.getAmount() - keepAmount;
               ItemStack drop = item.clone();
               drop.setAmount(dropAmount);
               if (keepAmount <= 0) {
                  inv.setItem(i, (ItemStack)null);
               } else {
                  item.setAmount(keepAmount);
               }

               this.craftPlayer.getWorld().dropItemNaturally(this.craftPlayer.getLocation(), drop);
            }
         }
      }
   }

   public void openCrafting() {
      if (this.spawned && this.craftPlayer != null) {
         for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
               for (int z = -3; z <= 3; z++) {
                  Block b = this.craftPlayer.getLocation().add((double)x, (double)y, (double)z).getBlock();
                  if (b.getType().name().contains("CRAFTING_TABLE") || b.getType().name().contains("WORKBENCH")) {
                     this.placeBlock(b);
                     CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] Opened crafting table.");
                     return;
                  }
               }
            }
         }

         CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] No crafting table nearby, using 2x2 grid.");
      }
   }

   public void vanish() {
      if (this.spawned && this.craftPlayer != null) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(CoreBootstrap.PLUGIN, this.craftPlayer);
         }
      }
   }

   public void unvanish() {
      if (this.spawned && this.craftPlayer != null) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            p.showPlayer(CoreBootstrap.PLUGIN, this.craftPlayer);
         }
      }
   }

   public void setGlowing(boolean glowing) {
      if (this.spawned && this.craftPlayer != null) {
         if (glowing) {
            this.craftPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
         } else {
            this.craftPlayer.removePotionEffect(PotionEffectType.GLOWING);
         }
      }
   }

   public void setInvisible(boolean invisible) {
      if (this.spawned && this.craftPlayer != null) {
         if (invisible) {
            this.craftPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
         } else {
            this.craftPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
         }
      }
   }

   public void swingMainHand() {
      if (this.spawned && this.serverPlayer != null) {
         try {
            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Class<?> handClass = cl.loadClass("net.minecraft.world.InteractionHand");
            Object mainHand = handClass.getField("MAIN_HAND").get(null);
            Method swing = this.findMethod(this.serverPlayer.getClass(), "swing", 1, handClass);
            if (swing != null) {
               swing.invoke(this.serverPlayer, mainHand);
            }
         } catch (Exception var8) {
            try {
               ClassLoader cl2 = this.serverPlayer.getClass().getClassLoader();
               Class<?> packetClass = cl2.loadClass("net.minecraft.network.protocol.game.ServerboundSwingPacket");
               Class<?> handClass2 = cl2.loadClass("net.minecraft.world.InteractionHand");
               Object mainHand2 = handClass2.getField("MAIN_HAND").get(null);
               Object packet = packetClass.getConstructor(handClass2).newInstance(mainHand2);
               this.broadcastPacketToSelf(packet);
            } catch (Exception var7) {
            }
         }
      }
   }

   public void useItem() {
      if (this.spawned && this.serverPlayer != null) {
         try {
            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Class<?> handClass = cl.loadClass("net.minecraft.world.InteractionHand");
            Object mainHand = handClass.getField("MAIN_HAND").get(null);
            Class<?> packetClass = cl.loadClass("net.minecraft.network.protocol.game.ServerboundUseItemPacket");
            Object packet = packetClass.getConstructor(handClass).newInstance(mainHand);
            this.broadcastPacketToSelf(packet);
         } catch (Exception var7) {
            if (this.craftPlayer != null) {
               try {
                  this.craftPlayer.swingMainHand();
               } catch (Exception var6) {
               }
            }
         }
      }
   }

   public void useOffhandItem() {
      if (this.spawned && this.serverPlayer != null) {
         try {
            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Class<?> handClass = cl.loadClass("net.minecraft.world.InteractionHand");
            Object offHand = handClass.getField("OFF_HAND").get(null);
            Class<?> packetClass = cl.loadClass("net.minecraft.network.protocol.game.ServerboundUseItemPacket");
            Object packet = packetClass.getConstructor(handClass).newInstance(offHand);
            this.broadcastPacketToSelf(packet);
         } catch (Exception var6) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] useOffhandItem failed: " + var6.getMessage());
         }
      }
   }

   public void selectHotbarSlot(int slot) {
      if (this.spawned && this.serverPlayer != null) {
         slot = Math.max(0, Math.min(8, slot));

         try {
            Object inventory = this.getFieldValue(this.serverPlayer, "inventory");
            if (inventory != null) {
               Field selectedField = this.findField(inventory.getClass(), "selected");
               if (selectedField != null) {
                  selectedField.setInt(inventory, slot);
               }
            }

            if (this.craftPlayer != null) {
               this.craftPlayer.getInventory().setHeldItemSlot(slot);
            }
         } catch (Exception var4) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] selectHotbarSlot failed: " + var4.getMessage());
         }
      }
   }

   public void sendChat(String message) {
      if (this.spawned && this.craftPlayer != null) {
         try {
            this.craftPlayer.chat(message);
         } catch (Exception var3) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] sendChat failed: " + var3.getMessage());
         }
      }
   }

   public void performCommand(String command) {
      if (this.spawned && this.craftPlayer != null) {
         try {
            Bukkit.dispatchCommand(this.craftPlayer, command);
         } catch (Exception var3) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] performCommand failed: " + var3.getMessage());
         }
      }
   }

   private boolean breakBlockInternal(Block block) {
      if (this.spawned && this.serverPlayer != null && block != null) {
         try {
            ClassLoader cl = this.serverPlayer.getClass().getClassLoader();
            Object gameMode = this.getFieldValue(this.serverPlayer, "gameMode");
            if (gameMode == null) {
               return false;
            }

            Class<?> blockPosClass = cl.loadClass("net.minecraft.core.BlockPos");
            Object blockPos = blockPosClass.getConstructor(int.class, int.class, int.class).newInstance(block.getX(), block.getY(), block.getZ());
            Method destroyBlock = this.findMethod(gameMode.getClass(), "destroyBlock", 1, blockPosClass);
            if (destroyBlock == null) {
               for (Method m : this.getAllMethods(gameMode.getClass())) {
                  if (m.getName().equals("destroyBlock") && m.getParameterCount() == 1) {
                     destroyBlock = m;
                     break;
                  }
               }
            }

            if (destroyBlock != null) {
               Object result = destroyBlock.invoke(gameMode, blockPos);
               if (result instanceof Boolean && (Boolean)result) {
                  return true;
               }
            }
         } catch (Exception var9) {
            CoreBootstrap.PLUGIN.getLogger().fine("[NMSBot] breakBlock NMS failed: " + var9.getMessage());
         }

         if (block.getType() != Material.AIR) {
            block.breakNaturally(this.craftPlayer != null ? this.craftPlayer.getInventory().getItemInMainHand() : null);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean breakBlock(Block block) {
      if (this.isMiningActive && this.miningTarget != null) {
         Location mt = this.miningTarget.getLocation();
         Location b = block.getLocation();
         return mt.getBlockX() == b.getBlockX() && mt.getBlockY() == b.getBlockY() && mt.getBlockZ() == b.getBlockZ() && mt.getWorld().equals(b.getWorld())
            ? this.finishMining()
            : false;
      } else {
         return false;
      }
   }

   public int startMining(Block block) {
      if (this.spawned && this.serverPlayer != null && block != null) {
         this.miningTarget = block;
         this.miningProgress = 0;
         this.isMiningActive = true;
         this.equipBestTool(block.getType());
         float hardness = block.getType().getHardness();
         if (hardness < 0.0F) {
            this.miningRequired = Integer.MAX_VALUE;
            return this.miningRequired;
         } else if (hardness == 0.0F) {
            this.miningRequired = 1;
            return 1;
         } else {
            int speed = 1;
            if (this.craftPlayer != null) {
               ItemStack hand = this.craftPlayer.getInventory().getItemInMainHand();
               if (hand != null) {
                  speed = this.getToolSpeed(hand.getType(), block.getType());
               }
            }

            if (speed <= 0) {
               this.miningRequired = Integer.MAX_VALUE;
               return Integer.MAX_VALUE;
            } else {
               this.miningRequired = Math.max(1, (int)(hardness * 30.0F / (float)speed * 2.0F));
               return this.miningRequired;
            }
         }
      } else {
         return 20;
      }
   }

   public boolean tickMining() {
      if (this.isMiningActive && this.miningTarget != null) {
         if (this.miningTarget.getType() == Material.AIR) {
            this.cancelMining();
            return false;
         } else {
            this.swingMainHand();
            this.miningProgress++;
            return this.miningProgress < this.miningRequired;
         }
      } else {
         return false;
      }
   }

   public boolean finishMining() {
      if (!this.isMiningActive || this.miningTarget == null) {
         return false;
      } else if (this.miningProgress < this.miningRequired) {
         return false;
      } else {
         Block block = this.miningTarget;
         Material blockType = block.getType();
         BlockData blockData = block.getBlockData();
         CoreBootstrap.PLUGIN
            .getLogger()
            .info("[NMSBot] finishMining called for " + blockType + " at " + block.getX() + "," + block.getY() + "," + block.getZ());
         boolean result = this.breakBlockInternal(block);
         CoreBootstrap.PLUGIN.getLogger().info("[NMSBot] breakBlockInternal result=" + result + " for " + blockType);
         this.cancelMining();
         return result;
      }
   }

   public void cancelMining() {
      this.miningTarget = null;
      this.miningProgress = 0;
      this.miningRequired = 0;
      this.isMiningActive = false;
   }

   public boolean isMining() {
      return this.isMiningActive;
   }

   public Block getMiningTarget() {
      return this.miningTarget;
   }

   public int getMiningProgress() {
      return this.miningProgress;
   }

   public int getMiningRequired() {
      return this.miningRequired;
   }

   public int equipBestTool(Material blockType) {
      if (this.spawned && this.craftPlayer != null) {
         PlayerInventory inv = this.craftPlayer.getInventory();
         int bestSlot = -1;
         int bestSpeed = 0;

         for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
               int speed = this.getToolSpeed(item.getType(), blockType);
               if (speed > bestSpeed) {
                  bestSpeed = speed;
                  bestSlot = i;
               }
            }
         }

         if (bestSlot >= 0) {
            this.selectHotbarSlot(bestSlot);
         }

         return bestSlot;
      } else {
         return -1;
      }
   }

   private int getToolSpeed(Material tool, Material block) {
      String t = tool.name().toLowerCase();
      String b = block.name().toLowerCase();
      if (!b.contains("stone") && !b.contains("ore") && !b.contains("block") && !b.contains("deepslate") && !b.contains("netherite")) {
         if (!b.contains("log") && !b.contains("wood") && !b.contains("planks")) {
            if (!b.contains("dirt") && !b.contains("sand") && !b.contains("gravel") && !b.contains("grass") && !b.contains("snow")) {
               if ((b.contains("web") || b.contains("bamboo")) && t.contains("sword")) {
                  return 5;
               } else {
                  return (b.contains("wool") || b.contains("leaves") || b.contains("vine")) && t.contains("shear") ? 5 : 1;
               }
            } else if (!t.contains("shovel") && !t.contains("spade")) {
               return 0;
            } else if (t.contains("netherite")) {
               return 10;
            } else if (t.contains("diamond")) {
               return 9;
            } else if (t.contains("iron")) {
               return 7;
            } else if (t.contains("stone")) {
               return 5;
            } else {
               return t.contains("wooden") ? 3 : 1;
            }
         } else if (!t.contains("axe")) {
            return 0;
         } else if (t.contains("netherite")) {
            return 10;
         } else if (t.contains("diamond")) {
            return 9;
         } else if (t.contains("iron")) {
            return 7;
         } else if (t.contains("stone")) {
            return 5;
         } else {
            return t.contains("wooden") ? 3 : 1;
         }
      } else if (!t.contains("pickaxe")) {
         return 0;
      } else if (t.contains("netherite")) {
         return 10;
      } else if (t.contains("diamond")) {
         return 9;
      } else if (t.contains("iron")) {
         return 7;
      } else if (t.contains("stone")) {
         return 5;
      } else {
         return t.contains("wooden") ? 3 : 1;
      }
   }

   private void broadcastPacketToSelf(Object packet) {
      try {
         Object connection = this.getFieldValue(this.serverPlayer, "connection");
         if (connection == null) {
            return;
         }

         Method send = this.findMethod(connection.getClass(), "send", 1);
         if (send != null) {
            send.invoke(connection, packet);
         }
      } catch (Exception var4) {
      }
   }

   private Object getFieldValue(Object obj, String fieldName) {
      try {
         Field f = this.findField(obj.getClass(), fieldName);
         if (f != null) {
            return f.get(obj);
         }
      } catch (Exception var4) {
      }

      return null;
   }

   public Player getPlayer() {
      return this.craftPlayer;
   }

   public boolean isSpawned() {
      return this.spawned;
   }

   public String getName() {
      return this.name;
   }

   private Method findMethod(Class<?> clazz, String name, int paramCount, Class<?>... paramTypes) {
      Class<?> c = clazz;

      label47:
      while (c != null && c != Object.class) {
         Method[] var6 = c.getDeclaredMethods();
         int var7 = var6.length;
         int var8 = 0;

         Method m;
         while (true) {
            if (var8 >= var7) {
               c = c.getSuperclass();
               continue label47;
            }

            m = var6[var8];
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
               if (paramTypes.length <= 0) {
                  break;
               }

               Class<?>[] actual = m.getParameterTypes();
               boolean match = true;

               for (int i = 0; i < paramTypes.length; i++) {
                  if (!paramTypes[i].equals(actual[i])) {
                     match = false;
                     break;
                  }
               }

               if (match) {
                  break;
               }
            }

            var8++;
         }

         return m;
      }

      return null;
   }

   private Field findField(Class<?> clazz, String name) {
      for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Field f : c.getDeclaredFields()) {
            if (f.getName().equals(name)) {
               f.setAccessible(true);
               return f;
            }
         }
      }

      return null;
   }

   private List<Method> getAllMethods(Class<?> type) {
      List<Method> list = new ArrayList<>();

      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         list.addAll(Arrays.asList(c.getDeclaredMethods()));
      }

      list.addAll(Arrays.asList(type.getMethods()));
      return list;
   }

   private List<Field> getAllFields(Class<?> type) {
      List<Field> list = new ArrayList<>();

      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
         list.addAll(Arrays.asList(c.getDeclaredFields()));
      }

      return list;
   }

   private String extractJson(String json, String key) {
      int idx = json.indexOf(key);
      if (idx == -1) {
         return null;
      } else {
         int start = idx + key.length();
         int end = json.indexOf("\"", start);
         return end == -1 ? null : json.substring(start, end);
      }
   }
}
