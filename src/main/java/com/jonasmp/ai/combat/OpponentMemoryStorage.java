package com.jonasmp.ai.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Gson-backed persistence for {@link OpponentProfile}s, one JSON file per opponent
 * UUID under {@code <dataFolder>/opponent_profiles/}. Same mechanism as
 * {@code BotMemoryStorage}; writes are infrequent (combat end), never per tick.
 */
public final class OpponentMemoryStorage {

   private final Gson gson = new GsonBuilder().create();
   private final File folder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "opponent_profiles");

   public OpponentMemoryStorage() {
      if (!this.folder.exists()) {
         this.folder.mkdirs();
      }
   }

   public OpponentProfile load(String uuid, String name) {
      File file = new File(this.folder, uuid + ".json");
      if (!file.exists()) {
         return new OpponentProfile(uuid, name);
      }
      try (FileReader reader = new FileReader(file)) {
         OpponentProfile profile = this.gson.fromJson(reader, OpponentProfile.class);
         if (profile == null) {
            return new OpponentProfile(uuid, name);
         }
         profile.normalise();
         profile.applyTimeDecay();
         if (name != null && !name.isEmpty()) {
            profile.name = name;
         }
         return profile;
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[OpponentMemory] Load failed for " + uuid + ": " + ex.getMessage());
         return new OpponentProfile(uuid, name);
      }
   }

   public void save(OpponentProfile profile) {
      if (profile == null || profile.uuid == null || profile.uuid.isEmpty()) {
         return;
      }
      try (FileWriter writer = new FileWriter(new File(this.folder, profile.uuid + ".json"))) {
         this.gson.toJson(profile, writer);
      } catch (Exception ex) {
         CoreBootstrap.PLUGIN.getLogger().warning("[OpponentMemory] Save failed for " + profile.uuid + ": " + ex.getMessage());
      }
   }
}
