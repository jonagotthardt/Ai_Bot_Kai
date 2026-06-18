package dev.kai.memory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kai's long(er)-term recollection of entities it has perceived.
 *
 * <p>This is the first tier of the canonical lookup order <b>Memory &rarr; Cache &rarr; Live</b>:
 * before paying for a fresh perception scan, a consumer asks memory what it already knows. Entries
 * are refreshed in place from live observations and expire after a configurable age, so memory never
 * grows without bound and stale ghosts don't drive behaviour.
 */
public final class MemorySystem {

    private final Map<UUID, EntityMemory> entries = new ConcurrentHashMap<>();
    private final long ttlTicks;

    public MemorySystem(long ttlTicks) {
        this.ttlTicks = Math.max(1L, ttlTicks);
    }

    /** Records (or refreshes) what Kai currently sees about {@code entity}. */
    public void remember(@NotNull LivingEntity entity, long nowTick) {
        UUID id = entity.getUniqueId();
        boolean isPlayer = entity instanceof Player;
        Location loc = entity.getLocation();
        Material held = heldItem(entity);
        double threat = threatOf(entity, held);

        entries.compute(id, (key, existing) -> {
            if (existing == null) {
                EntityMemory created = new EntityMemory(id, entity.getName(), loc, nowTick, isPlayer);
                created.update(entity.getName(), loc, nowTick, isPlayer, held, threat);
                return created;
            }
            existing.update(entity.getName(), loc, nowTick, isPlayer, held, threat);
            return existing;
        });
    }

    public @NotNull Optional<EntityMemory> recall(@NotNull UUID id) {
        return Optional.ofNullable(entries.get(id));
    }

    public @NotNull Collection<EntityMemory> all() {
        return entries.values();
    }

    /** Drops entries not refreshed within the TTL. Called from the load-balanced maintenance pass. */
    public int maintain(long nowTick) {
        int removed = 0;
        for (EntityMemory memory : entries.values()) {
            if (memory.ageTicks(nowTick) > ttlTicks && entries.remove(memory.id(), memory)) {
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    private static @Nullable Material heldItem(@NotNull LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return null;
        }
        ItemStack inHand = equipment.getItemInMainHand();
        return inHand.getType().isAir() ? null : inHand.getType();
    }

    /**
     * Coarse threat heuristic. Players outrank mobs; a weapon in hand and a healthy attacker raise
     * the score. Used only for prioritising targets, so a cheap approximation is appropriate.
     */
    private static double threatOf(@NotNull LivingEntity entity, @Nullable Material held) {
        double score = entity instanceof Player ? 10.0D : (entity instanceof Mob ? 4.0D : 1.0D);
        if (held != null) {
            String name = held.name();
            if (name.endsWith("_SWORD") || name.endsWith("_AXE")) {
                score += 6.0D;
            } else if (name.endsWith("BOW") || name.equals("TRIDENT")) {
                score += 4.0D;
            }
        }
        score += Math.max(0.0D, entity.getHealth()) * 0.1D;
        return score;
    }
}
