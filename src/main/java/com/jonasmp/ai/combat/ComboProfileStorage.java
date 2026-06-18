package com.jonasmp.ai.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Gson persistence for {@link ComboProfile}s, split into two human-readable folders:
 * <ul>
 *   <li>{@code combo_profiles/players/<playerName>.json} — one file per player,
 *       named after the player for readability (UUID stored inside).</li>
 *   <li>{@code combo_profiles/mobs/<mobType>.json} — one file per mob type
 *       (all zombies share a profile, etc.).</li>
 * </ul>
 * Mob profiles are saved less aggressively than player profiles (see
 * {@link #MOB_MIN_SAVE_INTERVAL_MS}) because mob attack patterns barely change.
 */
public final class ComboProfileStorage {

   /** Don't rewrite a mob profile more often than this. */
   public static final long MOB_MIN_SAVE_INTERVAL_MS = 120_000L;

   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final File playersFolder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "combo_profiles/players");
   private final File mobsFolder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "combo_profiles/mobs");

   public ComboProfileStorage() {
      if (!this.playersFolder.exists()) {
         this.playersFolder.mkdirs();
      }
      if (!this.mobsFolder.exists()) {
         this.mobsFolder.mkdirs();
      }
   }

   public ComboProfile loadPlayer(String uuid, String name) {
      ComboProfile p = this.read(this.fileFor(false, name), uuid, name, false);
      p.id = uuid;
      if (name != null && !name.isEmpty()) {
         p.name = name;
      }
      return p;
   }

   public ComboProfile loadMob(String typeName) {
      return this.read(this.fileFor(true, typeName), typeName, typeName, true);
   }

   /**
    * Persists a profile. Mob profiles are throttled and skipped if saved recently;
    * returns true when a write actually happened.
    */
   public boolean save(ComboProfile profile) {
      if (profile == null || profile.name == null || profile.name.isEmpty()) {
         return false;
      }
      long now = System.currentTimeMillis();
      if (profile.mob && now - profile.lastSavedAt < MOB_MIN_SAVE_INTERVAL_MS) {
         return false;
      }
      File file = this.fileFor(profile.mob, profile.name);
      try (FileWriter writer = new FileWriter(file)) {
         profile.lastSavedAt = now;
         this.gson.toJson(profile, writer);
         return true;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[ComboProfile] Save failed for " + profile.name + ": " + ex.getMessage());
         return false;
      }
   }

   private ComboProfile read(File file, String id, String name, boolean mob) {
      if (!file.exists()) {
         return new ComboProfile(id, name, mob);
      }
      try (FileReader reader = new FileReader(file)) {
         ComboProfile p = this.gson.fromJson(reader, ComboProfile.class);
         if (p == null) {
            return new ComboProfile(id, name, mob);
         }
         p.mob = mob;
         p.normalise();
         return p;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[ComboProfile] Load failed for " + name + ": " + ex.getMessage());
         return new ComboProfile(id, name, mob);
      }
   }

   private File fileFor(boolean mob, String name) {
      File folder = mob ? this.mobsFolder : this.playersFolder;
      return new File(folder, sanitize(name) + ".json");
   }

   private static String sanitize(String name) {
      String s = name == null ? "unknown" : name.replaceAll("[^a-zA-Z0-9_.-]", "_");
      return s.isEmpty() ? "unknown" : s;
   }
}
