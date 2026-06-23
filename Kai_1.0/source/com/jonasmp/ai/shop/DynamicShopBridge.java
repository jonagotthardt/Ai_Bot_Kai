package com.jonasmp.ai.shop;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class DynamicShopBridge {
   private final Logger logger;
   private Plugin dsPlugin;
   private Economy vaultEconomy;
   private boolean reflectionReady = false;
   private final Map<String, Double> basePrices = new HashMap<>();
   private final List<String> itemList = new ArrayList<>();
   private final Map<String, String> searchIndex = new HashMap<>();
   private Method dsBuyMethod;
   private Method dsSellMethod;
   private Method dsGetBuyPrice;
   private Method dsGetSellPrice;
   private Object dsShopDataManager;
   private Object dsEconomyManager;
   private static final double SELL_TAX = 0.7;

   public DynamicShopBridge(Logger logger) {
      this.logger = logger;
      this.loadConfigPrices();
      this.addFallbackPrices();
      this.initReflection();
      this.initVault();
   }

   private void addFallbackPrices() {
      Map<String, Double> fallback = new HashMap<>();
      fallback.put("MACE", 5000.0);
      fallback.put("WIND_CHARGE", 100.0);
      fallback.put("NETHERITE_SWORD", 3000.0);
      fallback.put("DIAMOND_SWORD", 1500.0);
      fallback.put("IRON_SWORD", 500.0);
      fallback.put("NETHERITE_AXE", 2500.0);
      fallback.put("DIAMOND_AXE", 1200.0);
      fallback.put("IRON_AXE", 400.0);
      fallback.put("STONE_AXE", 200.0);
      fallback.put("NETHERITE_CHESTPLATE", 4000.0);
      fallback.put("DIAMOND_CHESTPLATE", 2000.0);
      fallback.put("IRON_CHESTPLATE", 600.0);
      fallback.put("NETHERITE_HELMET", 3000.0);
      fallback.put("DIAMOND_HELMET", 1500.0);
      fallback.put("IRON_HELMET", 400.0);
      fallback.put("NETHERITE_LEGGINGS", 3500.0);
      fallback.put("DIAMOND_LEGGINGS", 1800.0);
      fallback.put("IRON_LEGGINGS", 500.0);
      fallback.put("NETHERITE_BOOTS", 2500.0);
      fallback.put("DIAMOND_BOOTS", 1200.0);
      fallback.put("IRON_BOOTS", 400.0);
      fallback.put("SHIELD", 300.0);
      fallback.put("GOLDEN_APPLE", 50.0);
      fallback.put("ENCHANTED_GOLDEN_APPLE", 500.0);
      fallback.put("COOKED_BEEF", 10.0);

      for (Entry<String, Double> entry : fallback.entrySet()) {
         if (!this.basePrices.containsKey(entry.getKey())) {
            this.basePrices.put(entry.getKey(), entry.getValue());
            this.itemList.add(entry.getKey());
            this.searchIndex.put(entry.getKey().replace("_", "").toLowerCase(), entry.getKey());
         }
      }

      this.logger.info("[ShopBridge] Loaded " + fallback.size() + " fallback prices for bot gear.");
   }

   private void loadConfigPrices() {
      File configFile = new File(((World)Bukkit.getWorlds().get(0)).getWorldFolder().getParentFile(), "plugins/DynamicShop/config.yml");
      if (!configFile.exists()) {
         configFile = new File("plugins/DynamicShop/config.yml");
      }

      if (!configFile.exists()) {
         this.logger.warning("[ShopBridge] DynamicShop config.yml not found at expected paths.");
      } else {
         try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
            ConfigurationSection items = cfg.getConfigurationSection("items");
            if (items == null) {
               this.logger.warning("[ShopBridge] No 'items' section in DynamicShop config.");
               return;
            }

            for (String key : items.getKeys(false)) {
               double base = items.getDouble(key + ".base", -1.0);
               if (base >= 0.0) {
                  String mat = key.toUpperCase();
                  this.basePrices.put(mat, base);
                  this.itemList.add(mat);
                  this.searchIndex.put(mat.replace("_", "").toLowerCase(), mat);
                  String[] parts = mat.split("_");

                  for (String part : parts) {
                     if (part.length() > 2) {
                        this.searchIndex.put(part.toLowerCase(), mat);
                     }
                  }
               }
            }

            Collections.sort(this.itemList);
            this.logger.info("[ShopBridge] Loaded " + this.itemList.size() + " items from DynamicShop config.");
         } catch (Exception var14) {
            this.logger.warning("[ShopBridge] Failed to parse config: " + var14.getMessage());
         }
      }
   }

   private void initReflection() {
      this.dsPlugin = Bukkit.getPluginManager().getPlugin("DynamicShop");
      if (this.dsPlugin == null) {
         this.logger.info("[ShopBridge] DynamicShop plugin not found, using Vault fallback.");
      } else {
         try {
            Class<?> dsClass = this.dsPlugin.getClass();

            for (Method m : dsClass.getDeclaredMethods()) {
               m.setAccessible(true);
               String name = m.getName().toLowerCase();
               if (name.contains("get") && (name.contains("shop") || name.contains("data"))) {
                  try {
                     Object result = m.invoke(this.dsPlugin);
                     if (result != null) {
                        this.exploreMethods(result);
                     }
                  } catch (Exception var10) {
                  }
               }

               if (name.contains("get") && name.contains("economy")) {
                  try {
                     Object result = m.invoke(this.dsPlugin);
                     if (result != null) {
                        this.dsEconomyManager = result;
                        this.exploreMethods(result);
                     }
                  } catch (Exception var9) {
                  }
               }
            }

            for (Field f : dsClass.getDeclaredFields()) {
               f.setAccessible(true);
               String namex = f.getName().toLowerCase();
               if (namex.contains("shop") || namex.contains("data") || namex.contains("economy")) {
                  try {
                     Object result = f.get(this.dsPlugin);
                     if (result != null) {
                        this.exploreMethods(result);
                     }
                  } catch (Exception var8) {
                  }
               }
            }

            if (this.dsBuyMethod == null && this.dsGetBuyPrice == null) {
               this.logger.info("[ShopBridge] No suitable DynamicShop API found via reflection. Using Vault fallback.");
            } else {
               this.reflectionReady = true;
               this.logger.info("[ShopBridge] DynamicShop reflection bridge active.");
            }
         } catch (Throwable var11) {
            this.logger.warning("[ShopBridge] Reflection init failed: " + var11.getMessage());
         }
      }
   }

   private void exploreMethods(Object obj) {
      try {
         Class<?> clazz = obj.getClass();

         for (Method m : clazz.getDeclaredMethods()) {
            try {
               m.setAccessible(true);
               String name = m.getName().toLowerCase();
               Class<?>[] params = m.getParameterTypes();
               if (name.contains("buy") && !name.contains("price") && params.length >= 2) {
                  this.dsBuyMethod = m;
               } else if (name.contains("sell") && !name.contains("price") && params.length >= 2) {
                  this.dsSellMethod = m;
               } else if ((name.contains("buyprice") || name.contains("pricebuy")) && params.length >= 1) {
                  this.dsGetBuyPrice = m;
               } else if ((name.contains("sellprice") || name.contains("pricesell")) && params.length >= 1) {
                  this.dsGetSellPrice = m;
               }
            } catch (Throwable var9) {
            }
         }

         if (this.dsShopDataManager == null && (this.dsGetBuyPrice != null || this.dsGetSellPrice != null)) {
            this.dsShopDataManager = obj;
         }
      } catch (Throwable var10) {
         this.logger.fine("[ShopBridge] Could not explore methods of " + obj.getClass().getName() + ": " + var10.getMessage());
      }
   }

   private void initVault() {
      RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
      if (rsp != null) {
         this.vaultEconomy = (Economy)rsp.getProvider();
         this.logger.info("[ShopBridge] Vault economy: " + this.vaultEconomy.getName());
      } else {
         this.logger.warning("[ShopBridge] Vault economy not available!");
      }
   }

   public boolean isReady() {
      return !this.itemList.isEmpty() && (this.reflectionReady || this.vaultEconomy != null);
   }

   public int getItemCount() {
      return this.itemList.size();
   }

   public List<String> getAllItems() {
      return Collections.unmodifiableList(this.itemList);
   }

   public double getBuyPrice(String material) {
      material = this.normalizeMaterial(material);
      if (material == null) {
         return -1.0;
      } else {
         if (this.reflectionReady && this.dsGetBuyPrice != null) {
            try {
               Object result = this.dsGetBuyPrice.invoke(this.dsShopDataManager, material);
               if (result instanceof Number) {
                  return ((Number)result).doubleValue();
               }
            } catch (Exception var3) {
            }
         }

         return this.basePrices.getOrDefault(material, -1.0);
      }
   }

   public double getSellPrice(String material) {
      double buy = this.getBuyPrice(material);
      return buy < 0.0 ? -1.0 : buy * 0.7;
   }

   public DynamicShopBridge.BuySellResult buyItem(Player player, String material, int amount) {
      material = this.normalizeMaterial(material);
      if (material == null) {
         return DynamicShopBridge.BuySellResult.fail("Item nicht gefunden: " + material);
      } else {
         double price = this.getBuyPrice(material);
         if (price < 0.0) {
            return DynamicShopBridge.BuySellResult.fail("Kein Preis für " + material + " verfügbar.");
         } else {
            double total = price * (double)amount;
            if (this.reflectionReady && this.dsBuyMethod != null) {
               try {
                  Object result = this.dsBuyMethod.invoke(this.dsShopDataManager, player, material, amount);
                  if (result instanceof Boolean && (Boolean)result) {
                     return DynamicShopBridge.BuySellResult.success(material, amount, total, true);
                  }
               } catch (Exception var16) {
                  this.logger.warning("[ShopBridge] Reflection buy failed: " + var16.getMessage());
               }
            }

            if (this.vaultEconomy == null) {
               return DynamicShopBridge.BuySellResult.fail("Kein Economy-System verfügbar.");
            } else if (!this.vaultEconomy.has(player, total)) {
               return DynamicShopBridge.BuySellResult.fail("Nicht genug Geld! Benötigt: " + String.format("%.2f", total) + "$");
            } else {
               Material mat = Material.getMaterial(material);
               if (mat == null) {
                  return DynamicShopBridge.BuySellResult.fail("Ungültiges Material: " + material);
               } else {
                  PlayerInventory inv = player.getInventory();
                  int maxStack = mat.getMaxStackSize();
                  int remaining = amount;

                  for (int i = 0; i < 36 && remaining > 0; i++) {
                     ItemStack existing = inv.getItem(i);
                     if (existing == null || existing.getType() == Material.AIR) {
                        remaining -= Math.min(remaining, maxStack);
                     } else if (existing.getType() == mat && existing.getAmount() < maxStack) {
                        remaining -= Math.min(remaining, maxStack - existing.getAmount());
                     }
                  }

                  if (remaining > 0) {
                     return DynamicShopBridge.BuySellResult.fail("Nicht genug Inventarplatz!");
                  } else {
                     ItemStack stack = new ItemStack(mat, Math.min(amount, maxStack));
                     HashMap<Integer, ItemStack> leftover = inv.addItem(new ItemStack[]{stack});
                     int given = amount;

                     while (!leftover.isEmpty() && given < amount) {
                        int nextAmount = Math.min(amount - given, maxStack);
                        if (nextAmount <= 0) {
                           break;
                        }

                        stack = new ItemStack(mat, nextAmount);
                        leftover = inv.addItem(new ItemStack[]{stack});
                        given += nextAmount;
                     }

                     this.vaultEconomy.withdrawPlayer(player, total);
                     return DynamicShopBridge.BuySellResult.success(material, amount, total, false);
                  }
               }
            }
         }
      }
   }

   public DynamicShopBridge.BuySellResult sellItem(Player player, String material, int amount) {
      material = this.normalizeMaterial(material);
      if (material == null) {
         return DynamicShopBridge.BuySellResult.fail("Item nicht gefunden: " + material);
      } else {
         double price = this.getSellPrice(material);
         if (price < 0.0) {
            return DynamicShopBridge.BuySellResult.fail("Kein Verkaufspreis für " + material + " verfügbar.");
         } else {
            double total = price * (double)amount;
            Material mat = Material.getMaterial(material);
            if (mat == null) {
               return DynamicShopBridge.BuySellResult.fail("Ungültiges Material: " + material);
            } else {
               int has = this.countItems(player, mat);
               if (has < amount) {
                  return DynamicShopBridge.BuySellResult.fail("Nicht genug Items! Du hast " + has + "x " + material);
               } else {
                  if (this.reflectionReady && this.dsSellMethod != null) {
                     try {
                        Object result = this.dsSellMethod.invoke(this.dsShopDataManager, player, material, amount);
                        if (result instanceof Boolean && (Boolean)result) {
                           return DynamicShopBridge.BuySellResult.success(material, amount, total, true);
                        }
                     } catch (Exception var11) {
                        this.logger.warning("[ShopBridge] Reflection sell failed: " + var11.getMessage());
                     }
                  }

                  if (this.vaultEconomy == null) {
                     return DynamicShopBridge.BuySellResult.fail("Kein Economy-System verfügbar.");
                  } else {
                     this.removeItems(player, mat, amount);
                     this.vaultEconomy.depositPlayer(player, total);
                     return DynamicShopBridge.BuySellResult.success(material, amount, total, false);
                  }
               }
            }
         }
      }
   }

   public List<String> searchItems(String query) {
      query = query.toLowerCase().replace("_", "").replace(" ", "");
      List<String> results = new ArrayList<>();

      for (String item : this.itemList) {
         String clean = item.toLowerCase().replace("_", "");
         if (clean.contains(query) || query.contains(clean)) {
            results.add(item);
            if (results.size() >= 20) {
               break;
            }
         }
      }

      if (results.isEmpty()) {
         String[] queryWords = query.split("[\\s_]+");

         for (String itemx : this.itemList) {
            String[] itemWords = itemx.toLowerCase().split("_");
            boolean allMatch = true;

            for (String qw : queryWords) {
               if (qw.length() >= 2) {
                  boolean found = false;

                  for (String iw : itemWords) {
                     if (iw.startsWith(qw)) {
                        found = true;
                        break;
                     }
                  }

                  if (!found) {
                     allMatch = false;
                     break;
                  }
               }
            }

            if (allMatch) {
               results.add(itemx);
               if (results.size() >= 20) {
                  break;
               }
            }
         }
      }

      return results;
   }

   private String normalizeMaterial(String input) {
      if (input != null && !input.isBlank()) {
         String normalized = input.toUpperCase().replace(" ", "_").trim();
         if (this.basePrices.containsKey(normalized)) {
            return normalized;
         } else {
            return this.basePrices.containsKey("MINECRAFT_" + normalized) ? "MINECRAFT_" + normalized : null;
         }
      } else {
         return null;
      }
   }

   private int countItems(Player player, Material mat) {
      int count = 0;

      for (ItemStack stack : player.getInventory().getContents()) {
         if (stack != null && stack.getType() == mat) {
            count += stack.getAmount();
         }
      }

      return count;
   }

   private void removeItems(Player player, Material mat, int amount) {
      PlayerInventory inv = player.getInventory();
      int remaining = amount;

      for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
         ItemStack stack = inv.getItem(i);
         if (stack != null && stack.getType() == mat) {
            int remove = Math.min(remaining, stack.getAmount());
            if (remove == stack.getAmount()) {
               inv.setItem(i, null);
            } else {
               stack.setAmount(stack.getAmount() - remove);
            }

            remaining -= remove;
         }
      }
   }

   public static class BuySellResult {
      public final boolean success;
      public final String material;
      public final int amount;
      public final double totalPrice;
      public final String message;
      public final boolean usedReflection;

      private BuySellResult(boolean success, String material, int amount, double totalPrice, String message, boolean usedReflection) {
         this.success = success;
         this.material = material;
         this.amount = amount;
         this.totalPrice = totalPrice;
         this.message = message;
         this.usedReflection = usedReflection;
      }

      static DynamicShopBridge.BuySellResult success(String material, int amount, double price, boolean usedReflection) {
         return new DynamicShopBridge.BuySellResult(
            true, material, amount, price, "§aErfolg: §7" + amount + "x §e" + material + " §7für §6" + String.format("%.2f", price) + "§7$", usedReflection
         );
      }

      static DynamicShopBridge.BuySellResult fail(String message) {
         return new DynamicShopBridge.BuySellResult(false, null, 0, 0.0, "§cFehler: §7" + message, false);
      }
   }
}
