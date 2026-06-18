package dev.kai.pvp;

import dev.kai.KaiConfig;
import dev.kai.bot.KaiBot;
import dev.kai.cache.TtlCache;
import dev.kai.memory.MemorySystem;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Kai's combat brain: target acquisition, distance control, strafing, line-of-sight and melee.
 *
 * <p>Responsiveness is the whole point here. The lesson from Kai 1.x was that doing roughly one
 * movement decision per tick felt sluggish, so the cheap, player-facing actions — aiming, moving,
 * deciding whether to swing — run on <b>every</b> control tick. Only the genuinely expensive
 * decisions are throttled and cached:
 * <ul>
 *   <li>target selection runs on {@code pvp.target-refresh-ticks};</li>
 *   <li>line-of-sight is evaluated on {@code pvp.los-refresh-ticks} and cached in between;</li>
 *   <li>weapon choice runs on {@code pvp.gear-refresh-ticks}.</li>
 * </ul>
 * Nothing scans the whole world; target search is a bounded {@code getNearbyEntities} around Kai.
 */
public final class PvpController {

    private final KaiConfig config;
    private final MemorySystem memory;
    private final TtlCache<UUID, Boolean> losCache;

    private LivingEntity target;
    private long lastTargetTick = Long.MIN_VALUE;
    private long lastGearTick = Long.MIN_VALUE;
    private int strafeSign = 1;
    private long strafeFlipTick;

    public PvpController(@NotNull KaiConfig config, @NotNull MemorySystem memory) {
        this.config = config;
        this.memory = memory;
        this.losCache = new TtlCache<>(config.pvpLosRefreshTicks);
    }

    /** Forces (or clears) the current target, bypassing automatic acquisition for one cycle. */
    public void setTarget(@Nullable LivingEntity forced) {
        this.target = forced;
        this.lastTargetTick = Long.MIN_VALUE;
    }

    public @Nullable LivingEntity target() {
        return target;
    }

    /**
     * One combat control step. Runs every control tick while Kai is alive.
     */
    public void tick(@NotNull KaiBot bot, long nowTick) {
        Player self = bot.player();

        if (nowTick - lastTargetTick >= config.pvpTargetRefreshTicks || !isValidTarget(self, target)) {
            target = acquireTarget(self);
            lastTargetTick = nowTick;
        }
        if (target == null) {
            return;
        }

        memory.remember(target, nowTick);
        maybeUpdateGear(bot, nowTick);

        Location botLoc = self.getLocation();
        Location targetLoc = target.getLocation();
        Vector flatToTarget = new Vector(
                targetLoc.getX() - botLoc.getX(), 0.0D, targetLoc.getZ() - botLoc.getZ());
        double horizontalDist = flatToTarget.length();

        bot.lookAt(target);
        steer(bot, flatToTarget, horizontalDist, nowTick);

        if (targetLoc.getY() > botLoc.getY() + 1.0D) {
            bot.jumpIfGrounded();
        }

        boolean los = hasLineOfSight(self, target, nowTick);
        if (los && horizontalDist <= config.pvpReach && bot.attackCharge() >= 0.9F) {
            bot.attack(target);
        }
    }

    private void steer(@NotNull KaiBot bot, @NotNull Vector flatToTarget, double dist, long nowTick) {
        if (dist < 1.0e-4D) {
            return;
        }
        Vector approach = flatToTarget.clone().normalize();
        Vector strafe = new Vector(-approach.getZ(), 0.0D, approach.getX()).multiply(strafeSign);

        if (nowTick - strafeFlipTick > 25L) {
            strafeSign = -strafeSign;
            strafeFlipTick = nowTick;
        }

        Vector move;
        double preferred = config.pvpPreferredRange;
        if (dist > preferred + 0.2D) {
            move = approach.multiply(1.0D).add(strafe.multiply(0.35D));
        } else if (dist < preferred - 0.4D) {
            move = approach.multiply(-1.0D).add(strafe.multiply(0.5D));
        } else {
            move = strafe; // orbit at preferred range
        }
        bot.moveHorizontally(move, config.pvpMoveSpeed);
    }

    private void maybeUpdateGear(@NotNull KaiBot bot, long nowTick) {
        if (nowTick - lastGearTick < config.pvpGearRefreshTicks) {
            return;
        }
        lastGearTick = nowTick;
        bot.holdSlot(EquipmentSelector.bestMeleeSlot(bot.player()));
    }

    private boolean hasLineOfSight(@NotNull Player self, @NotNull LivingEntity target, long nowTick) {
        Boolean cached = losCache.get(target.getUniqueId(), nowTick);
        if (cached != null) {
            return cached;
        }
        boolean los = self.hasLineOfSight(target);
        losCache.put(target.getUniqueId(), los, nowTick, config.pvpLosRefreshTicks);
        return los;
    }

    private @Nullable LivingEntity acquireTarget(@NotNull Player self) {
        double range = config.pvpEngageRange;
        double bestDistSq = range * range;
        LivingEntity best = null;
        for (Entity entity : self.getNearbyEntities(range, range, range)) {
            if (!isValidTarget(self, entity)) {
                continue;
            }
            double distSq = entity.getLocation().distanceSquared(self.getLocation());
            if (distSq <= bestDistSq) {
                bestDistSq = distSq;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    private boolean isValidTarget(@NotNull Player self, @Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living) || living.isDead() || living.equals(self)) {
            return false;
        }
        if (living instanceof ArmorStand) {
            return false;
        }
        if (living instanceof Player) {
            return true;
        }
        return config.pvpTargetMobs && living instanceof Monster;
    }
}
