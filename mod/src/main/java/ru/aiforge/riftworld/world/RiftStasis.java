package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rev 5 «the space inside the building is slowed down»: while the ritual of the Rift runs (or the rift is open),
 * everything inside the hall moves as if through honey - mobs, animals and monsters drag their feet, arrows hang in the
 * air, dropped items and falling sand crawl. The player walks untouched, exactly as asked.
 * <p>
 * Server side only: movement of a living entity is produced by its AI every tick, so instead of scaling the velocity
 * (which the AI would overwrite) the stasis leans on a heavy slowness effect; everything else (arrows, items, falling
 * blocks, boats, minecarts) simply has its velocity damped each tick.
 */
public final class RiftStasis {
    private static final int THROTTLE = 4;
    private static final java.util.Map<Long, Long> LAST = new java.util.HashMap<>();

    private RiftStasis() {
    }

    public static void apply(ServerLevel level, BlockPos altar) {
        long key = altar.asLong() * 31L + level.dimension().location().hashCode();
        long now = level.getGameTime();
        Long last = LAST.get(key);
        if (last != null && now - last < THROTTLE) return;
        LAST.put(key, now);

        AABB hall = RiftZone.box(altar);
        for (Entity e : level.getEntitiesOfClass(Entity.class, hall)) {
            if (e instanceof Player) continue;                     // the player is never slowed
            if (e instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, true, false, false));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, true, false, false));
            }
            Vec3 d = e.getDeltaMovement();
            if (d.lengthSqr() > 1.0E-7) e.setDeltaMovement(d.scale(0.25));
            e.hurtMarked = true;
        }
    }
}
