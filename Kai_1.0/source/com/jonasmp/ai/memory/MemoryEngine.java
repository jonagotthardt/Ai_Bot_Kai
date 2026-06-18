package com.jonasmp.ai.memory;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class MemoryEngine {
   private final HashMap<UUID, PlayerMemory> cache = new HashMap<>();
   private final MemoryStorage storage = new MemoryStorage();
   private final ProfileExporter profileExporter = new ProfileExporter();

   public PlayerMemory get(UUID uuid) {
      HashMap<UUID, PlayerMemory> cache = this.cache;
      MemoryStorage storage = this.storage;
      return cache.computeIfAbsent(uuid, storage::load);
   }

   public void save(UUID uuid) {
      this.storage.save(this.get(uuid));
   }

   public void saveAll() {
      Collection<PlayerMemory> values = this.cache.values();
      MemoryStorage storage = this.storage;
      values.forEach(storage::save);
   }

   public void exportProfile(UUID uuid) {
      PlayerMemory mem = this.get(uuid);
      this.profileExporter.exportProfile(mem);
   }

   public void exportAllProfiles() {
      this.profileExporter.exportAll(this.cache.values());
   }

   public ProfileExporter getProfileExporter() {
      return this.profileExporter;
   }

   public void addScam(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.scamFlags++;
   }

   public void addRaid(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.raidFlags++;
   }

   public void addAiBlock(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.aiBlocks++;
   }

   public void addSpeedFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.speedFlags++;
      this.save(uuid);
   }

   public void addFlyFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.flyFlags++;
      this.save(uuid);
   }

   public void addXrayFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.xrayFlags++;
      this.save(uuid);
   }

   public void addNukerFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.nukerFlags++;
      this.save(uuid);
   }

   public void addKillauraFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.killauraFlags++;
      this.save(uuid);
   }

   public void addAutoclickFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.autoclickFlags++;
      this.save(uuid);
   }

   public void addContainerSpamFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.containerSpamFlags++;
      this.save(uuid);
   }

   public void addNoFallFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.noFallFlags++;
      this.save(uuid);
   }

   public void addReachFlag(UUID uuid) {
      PlayerMemory value = this.get(uuid);
      value.reachFlags++;
      this.save(uuid);
   }

   public int getRisk(UUID uuid) {
      return this.get(uuid).getRiskScore();
   }

   public void update(String playerId, String tag) {
      UUID uuid = UUID.fromString(playerId);
      switch (tag) {
         case "SCAM":
            this.addScam(uuid);
            break;
         case "RAID":
            this.addRaid(uuid);
            break;
         case "AI_BLOCK":
            this.addAiBlock(uuid);
            break;
         case "SPEED":
            this.addSpeedFlag(uuid);
            break;
         case "FLY":
            this.addFlyFlag(uuid);
            break;
         case "XRAY":
            this.addXrayFlag(uuid);
            break;
         case "NUKER":
            this.addNukerFlag(uuid);
            break;
         case "KILLAURA":
            this.addKillauraFlag(uuid);
            break;
         case "AUTOCLICK":
            this.addAutoclickFlag(uuid);
            break;
         case "CONTAINER_SPAM":
            this.addContainerSpamFlag(uuid);
            break;
         case "NOFALL":
            this.addNoFallFlag(uuid);
            break;
         case "REACH":
            this.addReachFlag(uuid);
      }
   }

   public void recordPunishment(String playerId, String type) {
      UUID uuid = UUID.fromString(playerId);
      this.get(uuid);
   }

   public void learnFromMessage(String playerId, String displayName, String message) {
      UUID uuid = UUID.fromString(playerId);
      PlayerMemory mem = this.get(uuid);
      if (!displayName.isEmpty()) {
         mem.displayName = displayName;
      }

      mem.learnFromMessage(message);
      this.save(uuid);
   }

   public String getPersonalityContext(String playerId) {
      UUID uuid = UUID.fromString(playerId);
      PlayerMemory mem = this.get(uuid);
      return mem.getPersonalityContext();
   }

   public String getDeepProfile(String playerId) {
      UUID uuid = UUID.fromString(playerId);
      PlayerMemory mem = this.get(uuid);
      return mem.getDeepProfile();
   }

   public void onJoin(UUID uuid) {
      PlayerMemory mem = this.get(uuid);
      if (mem.displayName == null || mem.displayName.isEmpty()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            mem.displayName = p.getName();
         } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline != null && offline.getName() != null) {
               mem.displayName = offline.getName();
            }
         }
      }

      mem.onJoin();
      this.save(uuid);
   }

   public void onQuit(UUID uuid) {
      this.get(uuid).onQuit();
      this.save(uuid);
   }

   public void onDeath(UUID uuid, String cause) {
      this.get(uuid).onDeath(cause);
      this.save(uuid);
   }

   public void onPlayerKill(UUID uuid, String victim) {
      this.get(uuid).onPlayerKill(victim);
      this.save(uuid);
   }

   public void onMobKill(UUID uuid, String mobType) {
      this.get(uuid).onMobKill(mobType);
      this.save(uuid);
   }

   public void onBlockPlaced(UUID uuid, String blockType) {
      this.get(uuid).onBlockPlaced(blockType);
      this.save(uuid);
   }

   public void onBlockBroken(UUID uuid, String blockType) {
      this.get(uuid).onBlockBroken(blockType);
      this.save(uuid);
   }

   public void onItemCrafted(UUID uuid, String itemType) {
      this.get(uuid).onItemCrafted(itemType);
      this.save(uuid);
   }

   public void onItemUsed(UUID uuid, String itemType) {
      this.get(uuid).onItemUsed(itemType);
      this.save(uuid);
   }

   public void onWorldChange(UUID uuid, String worldName) {
      this.get(uuid).onWorldChange(worldName);
      this.save(uuid);
   }

   public void onDistanceTraveled(UUID uuid, long blocks, boolean flying) {
      this.get(uuid).onDistanceTraveled(blocks, flying);
      this.save(uuid);
   }

   public void onCommand(UUID uuid, String cmd) {
      this.get(uuid).onCommand(cmd);
      this.save(uuid);
   }

   public void onTeleportRequest(UUID uuid) {
      this.get(uuid).onTeleportRequest();
      this.save(uuid);
   }

   public void onTeleportAccept(UUID uuid) {
      this.get(uuid).onTeleportAccept();
      this.save(uuid);
   }

   public void onMoneyEarned(UUID uuid, double amount) {
      this.get(uuid).onMoneyEarned(amount);
      this.save(uuid);
   }

   public void onMoneySpent(UUID uuid, double amount) {
      this.get(uuid).onMoneySpent(amount);
      this.save(uuid);
   }

   public void onShopBuy(UUID uuid, String item) {
      this.get(uuid).onShopBuy(item);
      this.save(uuid);
   }

   public void onShopSell(UUID uuid, String item) {
      this.get(uuid).onShopSell(item);
      this.save(uuid);
   }

   public void onNearbyPlayer(UUID uuid, String playerName, int seconds) {
      this.get(uuid).onNearbyPlayer(playerName, seconds);
      this.save(uuid);
   }

   public void onPlayerMention(UUID uuid, String playerName) {
      this.get(uuid).onPlayerMention(playerName);
      this.save(uuid);
   }

   public void setPeakBalance(UUID uuid, double balance) {
      this.get(uuid).setPeakBalance(balance);
      this.save(uuid);
   }

   public void setCurrentGoal(UUID uuid, String goal) {
      this.get(uuid).setCurrentGoal(goal);
      this.save(uuid);
   }

   public void setBuildStyle(UUID uuid, String style) {
      this.get(uuid).setBuildStyle(style);
      this.save(uuid);
   }

   public void setClanOrGuild(UUID uuid, String name) {
      this.get(uuid).setClanOrGuild(name);
      this.save(uuid);
   }

   public void setSocialRole(UUID uuid, String role) {
      this.get(uuid).setSocialRole(role);
      this.save(uuid);
   }

   public void setAiRelationship(UUID uuid, String rel) {
      this.get(uuid).setAiRelationship(rel);
      this.save(uuid);
   }

   public void recordMilestone(UUID uuid, String milestone) {
      this.get(uuid).recordMilestone(milestone);
      this.save(uuid);
   }

   public void recordAIConversation(UUID uuid, String topic) {
      this.get(uuid).recordAIConversation(topic);
      this.save(uuid);
   }

   public void recordPersonalFact(UUID uuid, String fact) {
      this.get(uuid).recordPersonalFact(fact);
      this.save(uuid);
   }

   public void recordFrustration(UUID uuid, String trigger) {
      this.get(uuid).recordFrustration(trigger);
      this.save(uuid);
   }

   public void recordLike(UUID uuid, String thing) {
      this.get(uuid).recordLike(thing);
      this.save(uuid);
   }

   public void recordDislike(UUID uuid, String thing) {
      this.get(uuid).recordDislike(thing);
      this.save(uuid);
   }

   public void logAction(UUID uuid, String type, String details, String world, int x, int y, int z) {
      this.get(uuid).logAction(type, details, world, x, y, z);
      this.save(uuid);
   }

   public void logSuspicious(UUID uuid, String type, String description, int severity) {
      this.get(uuid).logSuspicious(type, description, severity);
      this.save(uuid);
   }

   public void addAdminNote(UUID uuid, String note) {
      this.get(uuid).addAdminNote(note);
      this.save(uuid);
   }

   public void addAdminFlag(UUID uuid, String flag) {
      this.get(uuid).addAdminFlag(flag);
      this.save(uuid);
   }

   public void removeAdminFlag(UUID uuid, String flag) {
      this.get(uuid).removeAdminFlag(flag);
      this.save(uuid);
   }

   public void snapshotLocation(UUID uuid, String world, int x, int y, int z) {
      this.get(uuid).snapshotLocation(world, x, y, z);
      this.save(uuid);
   }

   public void logChat(UUID uuid, String message, String channel) {
      this.get(uuid).logChat(message, channel);
      this.save(uuid);
   }

   public void logConnection(UUID uuid, String type, long durationMs) {
      this.get(uuid).logConnection(type, durationMs);
      this.save(uuid);
   }

   public void logContainerAccess(UUID uuid, String containerType) {
      this.get(uuid).logContainerAccess(containerType);
      this.save(uuid);
   }

   public String getAdminReport(UUID uuid) {
      return this.get(uuid).getAdminReport();
   }

   public boolean wasAt(UUID uuid, String world, int x, int y, int z, long withinMs, int radius) {
      return this.get(uuid).wasAt(world, x, y, z, withinMs, radius);
   }
}
