package ru.aiforge.riftworld.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rev 4: three-hit combos of the boss / guard weapons in the player's hands (left click with a charged swing).
 * The client picks the step (it plays the matching first / third person animation) and reports it; the server checks
 * the charge, the weapon and the order, and adds the step's own effect on top of the vanilla hit:
 * <pre>
 * Morgrath's scythe: 0 «Косой мах»       - wide horizontal sweep, also cuts everything in a 140° arc
 *                    1 «Рубящий удар»     - overhead chop: a strip of 4.5 blocks in front, foes are tossed up
 *                    2 «Пепельная жатва»  - full spin: ring of embers around the player, sets on fire, throws back
 * Guard sickle:      0 «Рассечение»       - quick diagonal slash, cuts a 110° arc
 *                    1 «Крюк»             - backhand hook: pulls the nearest foe in front to the player
 *                    2 «Крестовый разрез» - X-thrust: cone of cuts, slowness + bleeding (wither)
 * </pre>
 * The directly hit target gets the vanilla damage; the step's extra damage only hits the others (no double hits).
 */
public final class WeaponCombo {
    private static final Map<UUID, long[]> LAST = new HashMap<>();      // {game time, step}

    private WeaponCombo() {
    }

    public static boolean isComboWeapon(ItemStack s) {
        return s.getItem() instanceof ScytheItem || s.getItem() instanceof GuardSickleItem;
    }

    public static void onStep(ServerPlayer sp, int step) {
        ItemStack st = sp.getMainHandItem();
        if (!isComboWeapon(st) || sp.isSpectator() || step < 0 || step > 2) return;
        if (sp.getAttackStrengthScale(0.5F) < 0.8F) return;
        boolean scythe = st.getItem() instanceof ScytheItem;
        long now = sp.level().getGameTime();
        long[] last = LAST.computeIfAbsent(sp.getUUID(), u -> new long[] {-1000, 2});
        if (now - last[0] < (scythe ? 10 : 6)) return;                  // rate limit (swing cooldown is longer)
        if (step != 0 && (now - last[0] > 50 || (last[1] + 1) % 3 != step)) step = 0;
        last[0] = now;
        last[1] = step;
        ServerLevel sl = sp.serverLevel();
        double base = sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Entity primary = primary(sp);
        if (scythe) {
            switch (step) {
                case 0 -> arc(sl, sp, primary, 3.9, 70, base * 0.6, 0.45, ParticleTypes.SWEEP_ATTACK, 1.0F);
                case 1 -> chop(sl, sp, primary, base);
                default -> ring(sl, sp, primary, base);
            }
        } else {
            switch (step) {
                case 0 -> arc(sl, sp, primary, 3.1, 55, base * 0.5, 0.25, ParticleTypes.SWEEP_ATTACK, 1.45F);
                case 1 -> hook(sl, sp, primary, base);
                default -> cross(sl, sp, primary, base);
            }
        }
        if (step == 2) st.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }

    /** The entity under the crosshair within reach - it already takes the vanilla hit. */
    private static Entity primary(ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        double reach = sp.getEntityReach();
        Vec3 end = eye.add(sp.getLookAngle().scale(reach));
        EntityHitResult r = ProjectileUtil.getEntityHitResult(sp.level(), sp, eye, end, sp.getBoundingBox().expandTowards(sp.getLookAngle().scale(reach)).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && e != sp);
        return r == null ? null : r.getEntity();
    }

    private static float yawTo(ServerPlayer sp, Entity e) {
        return (float) (Mth.atan2(e.getZ() - sp.getZ(), e.getX() - sp.getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private static Vec3 dir(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private static void hit(ServerPlayer sp, LivingEntity e, Entity primary, double dmg) {
        if (e != primary && dmg > 0) e.hurt(sp.damageSources().playerAttack(sp), (float) dmg);
    }

    private static void arc(ServerLevel sl, ServerPlayer sp, Entity primary, double r, float half, double dmg, double kb, ParticleOptions fx, float pitch) {
        for (LivingEntity e : ScytheFx.targets(sp, sp.getBoundingBox().inflate(r, 1.2, r))) {
            double dx = e.getX() - sp.getX(), dz = e.getZ() - sp.getZ(), d = Math.sqrt(dx * dx + dz * dz);
            if (d > r + e.getBbWidth() * 0.5 || Math.abs(Mth.wrapDegrees(yawTo(sp, e) - sp.getYRot())) > half && d > 1.0) continue;
            hit(sp, e, primary, dmg);
            e.knockback(kb, -dx / Math.max(d, 0.1), -dz / Math.max(d, 0.1));
        }
        double y = sp.getY() + 1.0;
        for (int k = -2; k <= 2; ++k) {
            Vec3 p = sp.position().add(dir(sp.getYRot() + k * half / 2.2F).scale(r * 0.7));
            sl.sendParticles(fx, p.x, y, p.z, 1, 0, 0, 0, 0);
        }
        ScytheFx.sound(sp, sp.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, pitch * 0.8F);
    }

    private static void chop(ServerLevel sl, ServerPlayer sp, Entity primary, double base) {
        Vec3 f = dir(sp.getYRot());
        Vec3 o = sp.position();
        for (LivingEntity e : ScytheFx.targets(sp, sp.getBoundingBox().inflate(5, 1.5, 5))) {
            Vec3 rel = e.position().subtract(o);
            double along = rel.x * f.x + rel.z * f.z, side = Math.abs(rel.x * f.z - rel.z * f.x);
            if (along < -0.3 || along > 4.8 || side > 1.3 + e.getBbWidth() * 0.5) continue;
            hit(sp, e, primary, base * 0.9);
            e.setDeltaMovement(e.getDeltaMovement().add(f.scale(0.2)).add(0, 0.5, 0));
            e.hurtMarked = true;
        }
        for (int k = 1; k <= 9; ++k) {
            Vec3 p = o.add(f.scale(k * 0.5));
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.15, p.z, 4, 0.15, 0.05, 0.15, 0.15);
            sl.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.1, p.z, k % 3 == 0 ? 1 : 0, 0.1, 0, 0.1, 0);
        }
        Vec3 e = o.add(f.scale(4.5));
        sl.sendParticles(ParticleTypes.EXPLOSION, e.x, e.y + 0.3, e.z, 1, 0, 0, 0, 0);
        ScytheFx.sound(sp, e, SoundEvents.ANVIL_LAND, 0.6F, 0.55F);
        ScytheFx.sound(sp, e, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.5F);
    }

    private static void ring(ServerLevel sl, ServerPlayer sp, Entity primary, double base) {
        double r = 4.6;
        for (LivingEntity e : ScytheFx.targets(sp, sp.getBoundingBox().inflate(r, 1.5, r))) {
            double dx = e.getX() - sp.getX(), dz = e.getZ() - sp.getZ(), d = Math.sqrt(dx * dx + dz * dz);
            if (d > r + e.getBbWidth() * 0.5) continue;
            hit(sp, e, primary, base * 0.9);
            e.setSecondsOnFire(3);
            e.knockback(0.9, -dx / Math.max(d, 0.1), -dz / Math.max(d, 0.1));
        }
        for (int k = 0; k < 40; ++k) {
            double a = k * Math.PI * 2 / 40;
            double x = sp.getX() + Math.cos(a) * r * 0.85, z = sp.getZ() + Math.sin(a) * r * 0.85;
            sl.sendParticles(ParticleTypes.FLAME, x, sp.getY() + 0.3, z, 2, 0.05, 0.15, 0.05, 0.02);
            if (k % 4 == 0) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, x, sp.getY() + 1.0, z, 1, 0, 0, 0, 0);
            if (k % 3 == 0) sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sp.getX() + Math.cos(a) * 2, sp.getY() + 0.2, sp.getZ() + Math.sin(a) * 2, 1, 0, 0.05, 0, 0.01);
        }
        ScytheFx.sound(sp, sp.position(), SoundEvents.BLAZE_SHOOT, 0.9F, 0.6F);
        ScytheFx.sound(sp, sp.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
    }

    private static void hook(ServerLevel sl, ServerPlayer sp, Entity primary, double base) {
        LivingEntity best = primary instanceof LivingEntity l ? l : null;
        if (best == null) {
            double bd = 99;
            for (LivingEntity e : ScytheFx.targets(sp, sp.getBoundingBox().inflate(5.5, 1.5, 5.5))) {
                double d = e.distanceTo(sp);
                if (d < bd && d < 5.5 && Math.abs(Mth.wrapDegrees(yawTo(sp, e) - sp.getYRot())) < 35) {
                    bd = d;
                    best = e;
                }
            }
        }
        if (best == null) {
            ScytheFx.sound(sp, sp.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 1.2F);
            return;
        }
        hit(sp, best, primary, base * 0.6);
        Vec3 pull = sp.position().subtract(best.position());
        double d = Math.max(pull.length(), 0.1);
        double k = Math.min(1.0, (d - 1.2) / 3.0);
        if (k > 0) {
            best.setDeltaMovement(pull.scale(0.55 * k / d).add(0, 0.28, 0));
            best.hurtMarked = true;
        }
        for (int i = 1; i < 8; ++i) {
            Vec3 p = best.position().add(pull.scale(i / 8.0));
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1.0, p.z, 1, 0, 0, 0, 0);
        }
        ScytheFx.sound(sp, best.position(), SoundEvents.CHAIN_BREAK, 1.0F, 1.3F);
        ScytheFx.sound(sp, best.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 1.1F);
    }

    private static void cross(ServerLevel sl, ServerPlayer sp, Entity primary, double base) {
        double r = 4.0;
        List<LivingEntity> ts = ScytheFx.targets(sp, sp.getBoundingBox().inflate(r, 1.5, r));
        for (LivingEntity e : ts) {
            double d = e.distanceTo(sp);
            if (d > r + e.getBbWidth() * 0.5 || Math.abs(Mth.wrapDegrees(yawTo(sp, e) - sp.getYRot())) > 40 && d > 1.0) continue;
            hit(sp, e, primary, base * 0.8);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), sp);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), sp);
        }
        Vec3 f = dir(sp.getYRot()), s = new Vec3(f.z, 0, -f.x);
        Vec3 c = sp.position().add(f.scale(2.2)).add(0, 1.1, 0);
        for (int i = -4; i <= 4; ++i) {
            double t = i * 0.22;
            for (int sg = -1; sg <= 1; sg += 2) {
                Vec3 p = c.add(s.scale(t)).add(0, t * sg, 0);
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.x, p.y, p.z, 3, 0.02, 0.02, 0.02, 0);
                sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0.05);
            }
        }
        ScytheFx.sound(sp, c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
        ScytheFx.sound(sp, c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
    }
}
