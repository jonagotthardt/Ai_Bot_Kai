package com.jonasmp.ai.shop;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopCommandExecutor implements CommandExecutor, TabCompleter {
   private final DynamicShopBridge bridge;
   private static final int ITEMS_PER_PAGE = 15;

   public ShopCommandExecutor(DynamicShopBridge bridge) {
      this.bridge = bridge;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         String cmd = command.getName().toLowerCase();

         return switch (cmd) {
            case "shopbuy" -> this.handleBuy(player, args);
            case "shopsell" -> this.handleSell(player, args);
            case "shopprice" -> this.handlePrice(player, args);
            case "shoplist" -> this.handleList(player, args);
            case "shopsearch" -> this.handleSearch(player, args);
            default -> false;
         };
      } else {
         sender.sendMessage(ChatColor.RED + "Nur für Spieler!");
         return true;
      }
   }

   private boolean handleBuy(Player player, String[] args) {
      if (args.length < 1) {
         player.sendMessage("§cVerwendung: §7/shopbuy <item> [menge]");
         return true;
      } else {
         String itemName = args[0].toUpperCase();
         int amount = 1;
         if (args.length >= 2) {
            try {
               amount = Integer.parseInt(args[1]);
               if (amount < 1) {
                  amount = 1;
               }

               if (amount > 64) {
                  amount = 64;
               }
            } catch (NumberFormatException var7) {
               player.sendMessage("§cUngültige Menge: §7" + args[1]);
               return true;
            }
         }

         int finalAmount = amount;
         Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("JonaSMP_AI_v1_Fixed"), () -> {
            DynamicShopBridge.BuySellResult result = this.bridge.buyItem(player, itemName, finalAmount);
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JonaSMP_AI_v1_Fixed"), () -> player.sendMessage(result.message));
         });
         return true;
      }
   }

   private boolean handleSell(Player player, String[] args) {
      if (args.length < 1) {
         player.sendMessage("§cVerwendung: §7/shopsell <item> [menge|all]");
         return true;
      } else {
         String itemName = args[0].toUpperCase();
         int amount = 1;
         boolean sellAll = false;
         if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
               sellAll = true;
            } else {
               try {
                  amount = Integer.parseInt(args[1]);
                  if (amount < 1) {
                     amount = 1;
                  }
               } catch (NumberFormatException var12) {
                  player.sendMessage("§cUngültige Menge: §7" + args[1]);
                  return true;
               }
            }
         }

         if (sellAll) {
            Material mat = Material.getMaterial(itemName);
            if (mat == null) {
               player.sendMessage("§cItem nicht gefunden: §7" + itemName);
               return true;
            }

            int has = 0;

            for (ItemStack stack : player.getInventory().getContents()) {
               if (stack != null && stack.getType() == mat) {
                  has += stack.getAmount();
               }
            }

            if (has == 0) {
               player.sendMessage("§cDu hast keine §7" + itemName + " §cim Inventar.");
               return true;
            }

            amount = has;
         }

         int finalAmount = amount;
         Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("JonaSMP_AI_v1_Fixed"), () -> {
            DynamicShopBridge.BuySellResult result = this.bridge.sellItem(player, itemName, finalAmount);
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JonaSMP_AI_v1_Fixed"), () -> player.sendMessage(result.message));
         });
         return true;
      }
   }

   private boolean handlePrice(Player player, String[] args) {
      if (args.length < 1) {
         player.sendMessage("§cVerwendung: §7/shopprice <item>");
         return true;
      } else {
         String itemName = args[0].toUpperCase();
         double buyPrice = this.bridge.getBuyPrice(itemName);
         double sellPrice = this.bridge.getSellPrice(itemName);
         if (buyPrice < 0.0) {
            player.sendMessage("§cItem nicht im Shop: §7" + itemName);
            return true;
         } else {
            player.sendMessage("");
            player.sendMessage("§2§lJona§a§lSMP §7— §eShop Preise");
            player.sendMessage("§8§m                   ");
            player.sendMessage("§7Item: §e" + itemName);
            player.sendMessage("§7Kauf: §c" + String.format("%.2f", buyPrice) + "§7$");
            player.sendMessage("§7Verkauf: §a" + String.format("%.2f", sellPrice) + "§7$");
            player.sendMessage("§8§m                   ");
            return true;
         }
      }
   }

   private boolean handleList(Player player, String[] args) {
      int page = 1;
      if (args.length >= 1) {
         try {
            page = Integer.parseInt(args[0]);
            if (page < 1) {
               page = 1;
            }
         } catch (NumberFormatException var12) {
         }
      }

      List<String> items = this.bridge.getAllItems();
      int totalPages = (int)Math.ceil((double)items.size() / 15.0);
      if (page > totalPages) {
         page = totalPages;
      }

      if (totalPages == 0) {
         player.sendMessage("§cKeine Items im Shop geladen.");
         return true;
      } else {
         int start = (page - 1) * 15;
         int end = Math.min(start + 15, items.size());
         player.sendMessage("");
         player.sendMessage("§2§lJona§a§lSMP §7— §eShop Items §7(Seite " + page + "/" + totalPages + ")");
         player.sendMessage("§8§m                   ");

         for (int i = start; i < end; i++) {
            String item = items.get(i);
            double price = this.bridge.getBuyPrice(item);
            player.sendMessage(" §7• §e" + item + " §7— §c" + String.format("%.2f", price) + "§7$");
         }

         player.sendMessage("§8§m                   ");
         if (page < totalPages) {
            player.sendMessage("§7Nächste Seite: §e/shoplist " + (page + 1));
         }

         return true;
      }
   }

   private boolean handleSearch(Player player, String[] args) {
      if (args.length < 1) {
         player.sendMessage("§cVerwendung: §7/shopsearch <begriff>");
         return true;
      } else {
         String query = String.join(" ", args);
         List<String> results = this.bridge.searchItems(query);
         if (results.isEmpty()) {
            player.sendMessage("§cKeine Items gefunden für: §7" + query);
            return true;
         } else {
            player.sendMessage("");
            player.sendMessage("§2§lJona§a§lSMP §7— §eSuchergebnisse §7für '" + query + "'");
            player.sendMessage("§8§m                   ");

            for (String item : results) {
               double price = this.bridge.getBuyPrice(item);
               player.sendMessage(" §7• §e" + item + " §7— §c" + String.format("%.2f", price) + "§7$");
            }

            player.sendMessage("§8§m                   ");
            player.sendMessage("§7" + results.size() + " Ergebnisse. Kaufen: §e/shopbuy <item> [menge]");
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!(sender instanceof Player)) {
         return Collections.emptyList();
      } else {
         String cmd = command.getName().toLowerCase();
         List<String> items = this.bridge.getAllItems();
         if (args.length == 1) {
            String partial = args[0].toUpperCase();
            return items.stream().filter(i -> i.contains(partial)).limit(50L).collect(Collectors.toList());
         } else {
            return args.length != 2 || !cmd.equals("shopbuy") && !cmd.equals("shopsell") ? Collections.emptyList() : List.of("1", "8", "16", "32", "64", "all");
         }
      }
   }
}
