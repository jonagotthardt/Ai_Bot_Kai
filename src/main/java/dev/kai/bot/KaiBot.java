package dev.kai.bot;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A thin, main-thread-only control surface over the FakePlayer that Kai drives.
 *
 * <p>Kai is a real server-side {@link Player} (created by the FakePlayer plugin), so every primitive
 * here is plain Paper API: aiming via {@link Player#lookAt}, movement via velocity, melee via
 * {@link LivingEntity#attack}. Keeping control on the public API (instead of raw NMS) is what makes
 * Kai survive Paper updates without remapping.
 */
public final class KaiBot {

    private final Player player;

    public KaiBot(@NotNull Player player) {
        this.player = player;
    }

    public @NotNull Player player() {
        return player;
    }

    public boolean isValid() {
        return player.isOnline() && !player.isDead();
    }

    public @NotNull Location eyeLocation() {
        return player.getEyeLocation();
    }

    /** Aims head + body at a target's eyes. Cheap; safe to call every control tick. */
    public void lookAt(@NotNull Entity target) {
        if (target instanceof LivingEntity living) {
            player.lookAt(living, LookAnchor.EYES, LookAnchor.EYES);
        } else {
            player.lookAt(target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(), LookAnchor.EYES);
        }
    }

    public void lookAt(@NotNull Location location) {
        player.lookAt(location.getX(), location.getY(), location.getZ(), LookAnchor.EYES);
    }

    /**
     * Drives horizontal motion in {@code direction} at {@code speed}, preserving vertical velocity
     * so gravity and jumps are untouched.
     */
    public void moveHorizontally(@NotNull Vector direction, double speed) {
        Vector flat = new Vector(direction.getX(), 0.0D, direction.getZ());
        if (flat.lengthSquared() < 1.0e-6D) {
            return;
        }
        flat.normalize().multiply(speed);
        Vector current = player.getVelocity();
        player.setVelocity(new Vector(flat.getX(), current.getY(), flat.getZ()));
    }

    public void jumpIfGrounded() {
        if (player.isOnGround()) {
            Vector v = player.getVelocity();
            player.setVelocity(new Vector(v.getX(), 0.42D, v.getZ()));
        }
    }

    /**
     * Returns how charged the next melee hit is, in {@code [0, 1]}. A full-strength vanilla hit needs
     * this to be at (or very near) {@code 1.0}; attacking early wastes the cooldown.
     */
    public float attackCharge() {
        return player.getCooledAttackStrength(0.0F);
    }

    public void attack(@NotNull Entity target) {
        player.attack(target);
        player.swingMainHand();
    }

    public void holdSlot(int hotbarSlot) {
        if (hotbarSlot >= 0 && hotbarSlot <= 8 && player.getInventory().getHeldItemSlot() != hotbarSlot) {
            player.getInventory().setHeldItemSlot(hotbarSlot);
        }
    }

    public @NotNull ItemStack mainHand() {
        return player.getInventory().getItemInMainHand();
    }
}
