package com.jonasmp.ai.memory;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class ProfileExporter {
   private final File profilesFolder = new File(CoreBootstrap.PLUGIN.getDataFolder(), "player_profiles");

   public ProfileExporter() {
      if (!this.profilesFolder.exists()) {
         this.profilesFolder.mkdirs();
      }
   }

   public void exportProfile(PlayerMemory memory) {
      if (memory != null) {
         String fileName = this.sanitizeFileName(memory.displayName) + "_profile.txt";
         File file = new File(this.profilesFolder, fileName);
         String report = memory.getAdminReport();

         try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(report);
         } catch (IOException var10) {
            CoreBootstrap.PLUGIN.getLogger().warning("[ProfileExporter] Failed to export profile for " + memory.displayName + ": " + var10.getMessage());
         }
      }
   }

   public void exportAll(Collection<PlayerMemory> memories) {
      for (PlayerMemory mem : memories) {
         this.exportProfile(mem);
      }

      CoreBootstrap.PLUGIN.getLogger().info("[ProfileExporter] Exported " + memories.size() + " player profiles to " + this.profilesFolder.getAbsolutePath());
   }

   private String sanitizeFileName(String name) {
      return name != null && !name.isBlank() ? name.replaceAll("[\\\\/:*?\"<>|]", "_").trim() : "unknown";
   }

   public File getProfilesFolder() {
      return this.profilesFolder;
   }
}
