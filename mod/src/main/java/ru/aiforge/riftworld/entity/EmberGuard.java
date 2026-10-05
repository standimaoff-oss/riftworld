package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * «Тлеющий страж» - gaunt guard of the hall with a bone mask and a hooked claw-sickle. Zombie-strength, but smarter: weaves
 * from side to side while closing in, runs up walls like a spider, crouches and trembles for a couple of seconds, then
 * dashes; close up it slashes. Summoned by Morgrath: appears on a balcony and leaps down into the hall.
 */
public class EmberGuard extends RiftMob {
    public static final int A_IDLE = 0, A_RUN = 1, A_CRAWL = 2, A_LEAP = 3, A_WINDUP = 4, A_DASH = 5, A_SLASH = 6, A_DEATH = 7, A_LAND = 8;
    private static final String[] CLIPS = {"idle", "run", "crawl", "leap", "windup", "dash", "slash", "death", "land"};
    enum S { WAIT, LEAP, CHASE, WINDUP, DASH, RECOVER, SLASH, MANTLE, PERCH }
    private static final int F_CLIMB = 1;

    S state = S.CHASE;
    int st, cooldown = 40, leapDir, leapDelay, windT;
    @Nullable Vec3 leapF;
    boolean dashHit, sentry;
    float phase;
    int stuck;
    double lastY;
    Vec3 m0, m1, m2;
    Vec3 dashDir = Vec3.ZERO;

    public EmberGuard(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        this.setMaxUpStep(1.0F);
        this.phase = this.random.nextFloat() * 6.28F;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 3.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public String[] clips() {
        return CLIPS;
    }

    @Override
    public boolean loops(int a) {
        return a == A_IDLE || a == A_RUN || a == A_CRAWL;
    }

    @Override
    public int locomotion(int a) {
        return a == A_IDLE ? A_RUN : -1;
    }

    @Override
    public float strideScale() {
        return 0.075F;
    }

    @Override
    public boolean headLook() {
        return anim() == A_IDLE && !climbing();
    }

    public boolean climbing() {
        return flag(F_CLIMB);
    }

    @Override
    public boolean onClimbable() {
        return climbing();
    }

    /** Set by the boss before spawning: wait `delay` ticks on the balcony, then leap towards the hall (dir = +-1 on X, 0 = in place). */
    public void prepareLeap(int dir, int delay, @Nullable BlockPos core) {
        leapDir = dir;
        leapDelay = delay;
        state = S.WAIT;
        st = 0;
        setAnim(A_IDLE, true);
    }

    /** Ruins sentry: crouches motionless on a wall / roof until a player comes close, then leaps down at him. */
    public void perch() {
        sentry = true;
        state = S.PERCH;
        st = 0;
        setAnim(A_IDLE, true);
    }

    private void go(S s) {
        state = s;
        st = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setFlag(F_CLIMB, horizontalCollision && (state == S.CHASE));
            if (state != S.MANTLE && isNoGravity()) {            // e.g. reloaded in the middle of a vault
                setNoGravity(false);
                noPhysics = false;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isDeadOrDying()) return;
        ServerLevel sl = (ServerLevel) level();
        ++st;
        if (cooldown > 0) --cooldown;
        LivingEntity tgt = target(sl);
        switch (state) {
            case PERCH -> {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                Player near = sl.getNearestPlayer(getX(), getY(), getZ(), 24, e -> e instanceof Player pl && !pl.isSpectator() && !pl.isCreative());
                if (near != null && (hasLineOfSight(near) || Math.hypot(near.getX() - getX(), near.getZ() - getZ()) < 16)) {
                    face(near, 360);
                    setTarget(near);
                    prepareLeap(0, 6 + random.nextInt(24), null);
                } else if (st % 80 == 0 && random.nextInt(2) == 0) {
                    setYRot(yBodyRot = yHeadRot = getYRot() + (random.nextFloat() - 0.5F) * 90.0F);   // looks around
                }
            }
            case WAIT -> {
                getNavigation().stop();
                if (st == 1) sound(SoundEvents.SKELETON_AMBIENT, 1.0F, 0.5F);
                if (st >= leapDelay) {
                    setAnim(A_LEAP, true);
                    if (st >= leapDelay + 7) {
                        float yaw = getYRot();
                        Vec3 f = leapDir != 0 ? new Vec3(leapDir, 0, 0) : MorgrathBoss.forward(yaw);
                        leapF = f;
                        setDeltaMovement(f.x * 0.12, 0.6, f.z * 0.12);   // up over the railing first, then out (LEAP st 3)
                        hasImpulse = true;
                        sound(SoundEvents.PHANTOM_FLAP, 1.0F, 0.6F);
                        go(S.LEAP);
                    }
                }
            }
            case LEAP -> {
                if (st == 3 && leapF != null) {
                    // the arc out into the hall: ~5-6 blocks from the balcony, slightly spread so they do not land in a heap
                    double sp = 0.6 + random.nextDouble() * 0.08, side = (random.nextDouble() - 0.5) * 0.14;
                    setDeltaMovement(leapF.x * sp - leapF.z * side, getDeltaMovement().y, leapF.z * sp + leapF.x * side);
                    hasImpulse = true;
                }
                if (st > 3 && st < 40 && !onGround()) {
                    Vec3 v = getDeltaMovement();
                    if (v.horizontalDistanceSqr() > 1.0E-4) setYRot(yBodyRot = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F);
                }
                if (st > 4 && onGround() || st > 80) {
                    sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.6F, 1.4F);
                    sound(SoundEvents.RAVAGER_STEP, 0.8F, 1.3F);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 8, 0.5, 0.05, 0.5, 0.015);
                    sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.1, getZ(), 3, 0.3, 0.0, 0.3, 0.0);
                    setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
                    go(S.RECOVER);
                    setAnim(A_LAND, true);
                    cooldown = 30;
                }
            }
            case CHASE -> chase(tgt);
            case MANTLE -> {
                // vault over an overhang (gallery floor, balcony): out from the wall, up over the railing, onto the floor
                getNavigation().stop();
                double u = Math.min(1.0, st / 13.0), a = 1 - u;
                Vec3 p = m0.scale(a * a).add(m1.scale(2 * a * u)).add(m2.scale(u * u));
                setDeltaMovement(Vec3.ZERO);
                setPos(p.x, p.y, p.z);
                if (tgt != null) face(tgt, 30);
                if (st >= 13) {
                    noPhysics = false;
                    setNoGravity(false);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 3, 0.3, 0.05, 0.3, 0.01);
                    sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.4F, 1.7F);
                    go(S.RECOVER);
                    setAnim(A_IDLE);
                    cooldown = 20;
                }
            }
            case WINDUP -> {
                getNavigation().stop();
                setAnim(A_WINDUP);
                if (tgt != null) face(tgt, 25);
                if (st % 9 == 0) sound(SoundEvents.SKELETON_AMBIENT, 0.8F, 0.4F + random.nextFloat() * 0.2F);
                if (st >= windT) {
                    Vec3 d = tgt != null ? new Vec3(tgt.getX() - getX(), 0, tgt.getZ() - getZ()) : MorgrathBoss.forward(getYRot());
                    dashDir = d.lengthSqr() > 1e-4 ? d.normalize() : MorgrathBoss.forward(getYRot());
                    dashHit = false;
                    go(S.DASH);
                    setAnim(A_DASH, true);
                    sound(SoundEvents.PHANTOM_SWOOP, 1.2F, 0.7F);
                }
            }
            case DASH -> {
                if (st <= 8) setDeltaMovement(dashDir.x * 0.85, Math.min(getDeltaMovement().y, 0.1) + (st == 1 ? 0.18 : 0), dashDir.z * 0.85);
                hasImpulse = true;
                if (st % 2 == 0) sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 2, 0.2, 0.3, 0.2, 0.01);
                if (!dashHit && tgt != null && getBoundingBox().inflate(0.7).intersects(tgt.getBoundingBox())) {
                    dashHit = true;
                    if (tgt.hurt(damageSources().mobAttack(this), 5.0F)) {
                        tgt.knockback(0.6, -dashDir.x, -dashDir.z);
                        sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
                    }
                }
                if (st >= 12) {
                    go(S.RECOVER);
                    setAnim(A_IDLE);
                    cooldown = 50 + random.nextInt(30);
                }
            }
            case RECOVER -> {
                getNavigation().stop();
                if (tgt != null) face(tgt, 15);
                if (st >= (anim() == A_LAND ? 18 : 16)) go(S.CHASE);
            }
            case SLASH -> {
                getNavigation().stop();
                if (tgt != null) face(tgt, 20);
                if (st == 8 && tgt != null && distanceTo(tgt) < 2.8) {
                    if (tgt.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.3F);
                }
                if (st >= 16) {
                    go(S.CHASE);
                    setAnim(A_IDLE);
                    cooldown = 14;
                }
            }
        }
    }

    private void chase(@Nullable LivingEntity tgt) {
        setAnim(A_IDLE);
        if (tgt == null) {
            getNavigation().stop();
            return;
        }
        // stuck on a wall under an overhang while the target stands above -> vault onto its floor
        double hd = Math.hypot(tgt.getX() - getX(), tgt.getZ() - getZ());
        if (climbing() && tgt.getY() - getY() > 1.5 && hd < 7.5) {
            stuck = Math.abs(getY() - lastY) < 0.05 ? stuck + 1 : 0;
            if (stuck > 10) {
                stuck = 0;
                if (startMantle(tgt)) return;
            }
        } else stuck = 0;
        lastY = getY();
        double d = distanceTo(tgt);
        if (d < 2.3 && cooldown <= 0) {
            go(S.SLASH);
            setAnim(A_SLASH, true);
            return;
        }
        if (d > 3.5 && d < 8.5 && cooldown <= 0 && hasLineOfSight(tgt) && onGround() && random.nextInt(8) == 0) {
            windT = 30 + random.nextInt(16);                       // "a couple of seconds" before the dash
            go(S.WINDUP);
            setAnim(A_WINDUP, true);
            sound(SoundEvents.WOLF_GROWL, 0.9F, 0.5F);
            return;
        }
        if (tickCount % 4 == 0) {
            // weave: aim at a point swinging from side to side of the target while closing in
            Vec3 to = new Vec3(tgt.getX() - getX(), 0, tgt.getZ() - getZ());
            Vec3 side = to.lengthSqr() > 1e-4 ? new Vec3(-to.z, 0, to.x).normalize() : Vec3.ZERO;
            double sw = d > 3.5 ? Math.sin(tickCount * 0.09 + phase) * Math.min(3.0, d * 0.4) : 0;
            getNavigation().moveTo(tgt.getX() + side.x * sw, tgt.getY(), tgt.getZ() + side.z * sw, 1.0);
        }
        getLookControl().setLookAt(tgt, 40, 40);
    }

    private boolean startMantle(LivingEntity tgt) {
        Vec3 tp = tgt.position();
        Vec3 toMe = new Vec3(getX() - tp.x, 0, getZ() - tp.z);
        Vec3 land = null;
        for (double o : new double[] {1.3, 0.8, 2.0}) {
            for (int k = 0; k < 9 && land == null; ++k) {
                Vec3 dir = k == 0 ? (toMe.lengthSqr() > 1e-3 ? toMe.normalize() : MorgrathBoss.forward(0)) : MorgrathBoss.forward(k * 40);
                Vec3 c = tp.add(dir.scale(o));
                net.minecraft.world.phys.AABB bb = getDimensions(getPose()).makeBoundingBox(c);
                if (level().noCollision(this, bb) && !level().noCollision(this, bb.move(0, -0.25, 0))) land = c;
            }
            if (land != null) break;
        }
        if (land == null) return false;
        Vec3 out = Vec3.ZERO;
        BlockPos bp = blockPosition().above();
        for (net.minecraft.core.Direction dd : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos q = bp.relative(dd);
            if (!level().getBlockState(q).getCollisionShape(level(), q).isEmpty()) out = out.add(-dd.getStepX(), 0, -dd.getStepZ());
        }
        if (out.lengthSqr() < 1e-3) out = new Vec3(getX() - land.x, 0, getZ() - land.z);
        out = out.lengthSqr() > 1e-3 ? out.normalize() : Vec3.ZERO;
        m0 = position();
        m2 = land;
        m1 = new Vec3(m0.x + out.x * 3.0, Math.max(m0.y, land.y) + 2.6, m0.z + out.z * 3.0);
        go(S.MANTLE);
        setAnim(A_LEAP, true);
        noPhysics = true;
        setNoGravity(true);
        sound(SoundEvents.PHANTOM_FLAP, 1.0F, 0.7F);
        return true;
    }

    private void face(LivingEntity e, float step) {
        float want = (float) (Mth.atan2(e.getZ() - getZ(), e.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float y = Mth.approachDegrees(getYRot(), want, step);
        setYRot(y);
        yBodyRot = y;
        yHeadRot = y;
    }

    @Nullable
    private LivingEntity target(ServerLevel sl) {
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && !(t instanceof Player p && (p.isSpectator() || p.isCreative()))) return t;
        Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 36, e -> e instanceof Player pl && !pl.isSpectator() && !pl.isCreative());
        setTarget(p);
        return p;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (state == S.WAIT && st < leapDelay && !sentry) return false;        // balcony guards (boss summon) only
        if (state == S.PERCH && src.getEntity() instanceof LivingEntity le && !level().isClientSide) {
            setTarget(le);
            go(S.CHASE);
        }
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide) setAnim(A_DEATH, true);
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (deathTime >= 32 && level() instanceof ServerLevel sl && !isRemoved()) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 12, 0.5, 0.2, 0.5, 0.02);
            sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.4, getZ(), 20, 0.5, 0.3, 0.5, 0.02);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hit) {
        super.dropCustomDeathLoot(src, looting, hit);
        if (random.nextFloat() < 0.22F + looting * 0.08F) spawnAtLocation(new ItemStack(ModItems.GUARD_SICKLE.get()));
        if (random.nextFloat() < 0.6F) spawnAtLocation(new ItemStack(Items.BONE, 1 + random.nextInt(2)));
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    private void sound(SoundEvent ev, float vol, float pitch) {
        level().playSound(null, getX(), getY() + 1, getZ(), ev, SoundSource.HOSTILE, vol, pitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + random.nextFloat() * 0.1F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (state == S.PERCH) tag.putBoolean("Perch", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        state = tag.getBoolean("Perch") ? S.PERCH : S.CHASE;
        sentry = tag.getBoolean("Perch");
    }
}
