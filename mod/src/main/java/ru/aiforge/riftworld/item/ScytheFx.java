package ru.aiforge.riftworld.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/** Server side of the scythe's super attacks; staged effects (furrow, dash, slam) advance on the server tick. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ScytheFx {
    private static final List<Job> JOBS = new ArrayList<>();

    private ScytheFx() {
    }

    private abstract static class Job {
        final ServerPlayer p;
        int t;
        final Set<Integer> hit = new HashSet<>();

        Job(ServerPlayer p) {
            this.p = p;
        }

        abstract boolean step(ServerLevel sl);   // true = finished
    }

    static List<LivingEntity> targets(ServerPlayer p, AABB box) {
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !(e instanceof ArmorStand)
                && !(e instanceof Player o && (o.isCreative() || o.isSpectator() || !p.canHarmPlayer(o))));
    }

    static void sound(ServerPlayer p, Vec3 at, SoundEvent ev, float vol, float pitch) {
        p.level().playSound(null, at.x, at.y, at.z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    /** «Жатва»: a full reaping circle around the player. */
    public static int spin(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        double r = 4.5;
        for (LivingEntity e : targets(p, p.getBoundingBox().inflate(r, 1.5, r))) {
            double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ(), d = Math.sqrt(dx * dx + dz * dz);
            if (d > r + e.getBbWidth() * 0.5) continue;
            if (e.hurt(p.damageSources().playerAttack(p), 11.0F)) e.knockback(0.9, -dx / Math.max(d, 0.1), -dz / Math.max(d, 0.1));
        }
        for (int k = 0; k < 20; ++k) {
            double a = k / 20.0 * Math.PI * 2;
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 2.6, p.getY() + 1.0, p.getZ() + Math.sin(a) * 2.6, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.FLAME, p.getX() + Math.cos(a) * 3.6, p.getY() + 0.9, p.getZ() + Math.sin(a) * 3.6, 2, 0.1, 0.1, 0.1, 0.01);
        }
        sound(p, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.55F);
        sound(p, p.position(), SoundEvents.FIRECHARGE_USE, 0.8F, 0.7F);
        return 45;
    }

    /** «Огненная борозда»: fire erupts from the ground in a line ahead, one block per tick, 14 blocks. */
    public static int furrow(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        final Vec3 d = dir;
        final Vec3 o = p.position();
        JOBS.add(new Job(p) {
            double y = o.y;

            @Override
            boolean step(ServerLevel sl) {
                ++t;
                if (t > 14) return true;
                Vec3 c = o.add(d.scale(t + 0.5));
                // follow the ground a little (steps up / down)
                BlockPos bp = BlockPos.containing(c.x, y, c.z);
                if (!sl.getBlockState(bp).getCollisionShape(sl, bp).isEmpty()) y += 1;
                else if (sl.getBlockState(bp.below()).getCollisionShape(sl, bp.below()).isEmpty() && !sl.getBlockState(bp.below(2)).getCollisionShape(sl, bp.below(2)).isEmpty()) y -= 1;
                sl.sendParticles(ParticleTypes.FLAME, c.x, y + 0.3, c.z, 14, 0.35, 0.5, 0.35, 0.04);
                sl.sendParticles(ParticleTypes.LAVA, c.x, y + 0.2, c.z, 3, 0.3, 0.1, 0.3, 0.1);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, y + 1.0, c.z, 3, 0.3, 0.3, 0.3, 0.02);
                if (t % 3 == 1) sound(p, c, SoundEvents.BLAZE_SHOOT, 0.8F, 0.6F);
                for (LivingEntity e : targets(p, new AABB(c.x - 1.3, y - 0.5, c.z - 1.3, c.x + 1.3, y + 2.5, c.z + 1.3))) {
                    if (hit.add(e.getId()) && e.hurt(p.damageSources().playerAttack(p), 10.0F)) {
                        e.setSecondsOnFire(4);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
                    }
                }
                return false;
            }
        });
        sound(p, p.position(), SoundEvents.RAVAGER_ROAR, 0.5F, 1.4F);
        return 80;
    }

    /** «Рывок жнеца»: a forward dash that cuts everything on the way. */
    public static int dash(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 d = new Vec3(look.x, 0, look.z);
        d = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        p.setDeltaMovement(d.x * 2.1, 0.2, d.z * 2.1);
        p.hurtMarked = true;
        noFall(p);
        sound(p, p.position(), SoundEvents.PHANTOM_SWOOP, 1.2F, 0.6F);
        JOBS.add(new Job(p) {
            @Override
            boolean step(ServerLevel sl) {
                ++t;
                p.fallDistance = 0;
                sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 4, 0.2, 0.4, 0.2, 0.01);
                sl.sendParticles(ParticleTypes.SMALL_FLAME, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.4, 0.2, 0.01);
                for (LivingEntity e : targets(p, p.getBoundingBox().inflate(1.6, 0.8, 1.6))) {
                    if (hit.add(e.getId()) && e.hurt(p.damageSources().playerAttack(p), 13.0F)) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                        sound(p, e.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
                    }
                }
                return t >= 9;
            }
        });
        return 60;
    }

    /** «Падение кометы»: dive down and smash the ground - shock wave around the landing point. */
    public static int slam(ServerPlayer p) {
        p.setDeltaMovement(p.getDeltaMovement().x * 0.3, -2.4, p.getDeltaMovement().z * 0.3);
        p.hurtMarked = true;
        noFall(p);
        sound(p, p.position(), SoundEvents.PHANTOM_SWOOP, 1.0F, 0.5F);
        JOBS.add(new Job(p) {
            @Override
            boolean step(ServerLevel sl) {
                ++t;
                p.fallDistance = 0;
                sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 4, 0.2, 0.4, 0.2, 0.01);
                if (!p.onGround() && t < 50) return false;
                Vec3 c = p.position();
                double r = 5.5;
                for (LivingEntity e : targets(p, p.getBoundingBox().inflate(r, 2, r))) {
                    double dx = e.getX() - c.x, dz = e.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r) continue;
                    float dmg = (float) (15.0 * (1.0 - d / (r + 1.5)));
                    if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                        e.knockback(1.0, -dx / Math.max(d, 0.1), -dz / Math.max(d, 0.1));
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
                    }
                }
                for (int k = 0; k < 28; ++k) {
                    double a = k / 28.0 * Math.PI * 2;
                    for (double rr = 1.5; rr <= 5.0; rr += 1.75) {
                        sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * rr, c.y + 0.15, c.z + Math.sin(a) * rr, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x + Math.cos(a) * 2.5, c.y + 0.2, c.z + Math.sin(a) * 2.5, 1, 0.1, 0.1, 0.1, 0.01);
                }
                sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.2, c.z, 20, 1.2, 0.1, 1.2, 0.2);
                sound(p, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.6F);
                sound(p, c, SoundEvents.ANVIL_LAND, 0.8F, 0.5F);
                return true;
            }
        });
        return 100;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job j = it.next();
            if (j.p.isRemoved() || !j.p.isAlive() || j.step(j.p.serverLevel())) it.remove();
        }
    }

    private static final java.util.Map<java.util.UUID, Long> NOFALL = new java.util.HashMap<>();

    static void noFall(ServerPlayer p) {
        NOFALL.put(p.getUUID(), p.level().getGameTime() + 40);
    }

    @SubscribeEvent
    public static void fall(net.minecraftforge.event.entity.living.LivingFallEvent e) {
        Long until = NOFALL.get(e.getEntity().getUUID());
        if (until != null && e.getEntity().level().getGameTime() <= until) {
            e.setCanceled(true);
            NOFALL.remove(e.getEntity().getUUID());
        }
    }
}
