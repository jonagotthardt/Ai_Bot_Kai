package dev.kai.pvp;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

/**
 * Picks the best melee weapon currently in Kai's hotbar.
 *
 * <p>Pure, allocation-free scan over the nine hotbar slots. The PvP controller calls this only on a
 * cadence and caches the chosen slot, so weapon selection never runs every tick.
 */
public final class EquipmentSelector {

    private EquipmentSelector() {
    }

    /**
     * @return the hotbar slot (0-8) holding the strongest melee weapon, or the current slot if no
     * weapon is found.
     */
    public static int bestMeleeSlot(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        int bestSlot = inv.getHeldItemSlot();
        int bestScore = Integer.MIN_VALUE;
        boolean foundWeapon = false;

        for (int slot = 0; slot <= 8; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            int score = meleeScore(item.getType());
            if (score <= 0) {
                continue;
            }
            if (!foundWeapon || score > bestScore) {
                foundWeapon = true;
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static int meleeScore(@NotNull Material type) {
        String name = type.name();
        int tier = tierScore(name);
        if (name.endsWith("_SWORD")) {
            return 100 + tier;
        }
        if (name.endsWith("_AXE")) {
            return 80 + tier;
        }
        if (name.equals("TRIDENT")) {
            return 90;
        }
        return 0;
    }

    private static int tierScore(@NotNull String name) {
        if (name.startsWith("NETHERITE_")) {
            return 6;
        }
        if (name.startsWith("DIAMOND_")) {
            return 5;
        }
        if (name.startsWith("IRON_")) {
            return 4;
        }
        if (name.startsWith("STONE_")) {
            return 3;
        }
        if (name.startsWith("GOLDEN_")) {
            return 2;
        }
        if (name.startsWith("WOODEN_")) {
            return 1;
        }
        return 0;
    }
}
