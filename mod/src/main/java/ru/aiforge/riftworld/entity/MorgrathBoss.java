package ru.aiforge.riftworld.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.arena.ArenaCoreBlockEntity;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * «Морграт, Хозяин Чертога» - 3 m hunched beast with a long canine skull and bison horns, 200 HP, war scythe.
 * Sequence: lies as if dead (DORMANT) -> jerks and rises (RISING) -> waits by the chair (WAITING) -> roars and calls four
 * guards from the balconies (ROAR) -> fight. Every swing is telegraphed: a readable wind-up, the direction locks 0.5 s before
 * the blow so the player can step out; overhead and diagonal swings bury the scythe in the floor (vulnerable while he tugs
 * it out, +50% damage), the horizontal sweep never sticks. He climbs walls only when shot with arrows from the gallery.
 * Death: falls, lies 17 s, crumbles to ash; the scythe drops immediately.
 */
public class MorgrathBoss extends RiftMob {
    public static final int A_IDLE = 0, A_WALK = 1, A_AWAKEN = 2, A_OVER = 3, A_PULL = 4, A_SIDE = 5, A_DIAG = 6, A_PULL_DIAG = 7,
            A_ROAR = 8, A_CLIMB = 9, A_DEATH = 10, A_DEAD = 11;
    private static final String[] CLIPS = {"idle", "walk", "awaken", "swing_over", "pull", "swing_side", "swing_diag", "pull_diag",
            "roar", "climb", "death", "dead"};
    enum S { DORMANT, RISING, WAITING, ROAR, FIGHT, ATTACK, PULL, CLIMB_GO, CLIMB, HOP }

    static final int RISE_T = 92, ROAR_T = 44, SUMMON_AT = 20, LIE_T = 340;
    static final int OVER_HIT = 21, OVER_END = 48, SIDE_HIT = 18, SIDE_END = 36, DIAG_HIT = 20, DIAG_END = 44, PULL_END = 30, LOCK_BEFORE = 9;
    private static final int F_SCYTHE_GONE = 0;

    S state = S.FIGHT;
    int st, atk, cooldown = 30, twitch, twitchYaw, climbTicks;
    boolean second;
    float lockYaw;
    @Nullable BlockPos core;
    @Nullable Player climbTarget;
    Vec3 climbBase, climbTop;
    final List<UUID> guards = new ArrayList<>();
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.riftworld.morgrath"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public MorgrathBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
        this.xpReward = 300;
        this.setPersistenceRequired();
        this.bar.setVisible(false);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ARMOR, 8.0).add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    // ------------------------------------------------------------------------------------------------ RiftMob
    @Override
    public String[] clips() {
        return CLIPS;
    }

    @Override
    public boolean loops(int a) {
        return a == A_IDLE || a == A_WALK || a == A_CLIMB || a == A_DEAD;
    }

    @Override
    public int locomotion(int a) {
        return a == A_IDLE ? A_WALK : -1;
    }

    @Override
    public float strideScale() {
        return 0.125F;
    }

    @Override
    public boolean headLook() {
        int a = anim();
        return a == A_IDLE || a == A_WALK;
    }

    public boolean scytheGone() {
        return flag(F_SCYTHE_GONE);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor lvl, net.minecraft.world.DifficultyInstance diff,
            MobSpawnType type, @Nullable net.minecraft.world.entity.SpawnGroupData data, @Nullable CompoundTag tag) {
        var r = super.finalizeSpawn(lvl, diff, type, data, tag);
        go(S.ROAR);                       // egg / command: wakes at once (the arena re-sets him to DORMANT)
        setAnim(A_ROAR, true);
        return r;
    }

    // ------------------------------------------------------------------------------------------------ arena control
    /** Placed by the arena at the start of the entry scene: lies as if dead in front of the chair. */
    public void makeDormant(BlockPos corePos) {
        this.core = corePos;
        go(S.DORMANT);
        setAnim(A_DEAD, true);
    }

    /** Candles are lit: the body jerks and rises, then waits by the chair. */
    public void beginRise() {
        if (state != S.DORMANT) return;
        go(S.RISING);
        setAnim(A_AWAKEN, true);
    }

    /** A player came to the chair (or struck him): roar, guards, fight. */
    public void beginFight() {
        if (state == S.DORMANT || state == S.RISING || state == S.WAITING) {
            go(S.ROAR);
            setAnim(A_ROAR, true);
        }
    }

    public boolean isWaiting() {
        return state == S.DORMANT || state == S.RISING || state == S.WAITING;
    }

    private void go(S s) {
        state = s;
        st = 0;
    }

    @Nullable
    private ArenaCoreBlockEntity arena() {
        if (core == null) return null;
        return level().getBlockEntity(core) instanceof ArenaCoreBlockEntity c ? c : null;
    }

    // ------------------------------------------------------------------------------------------------ tick
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (!isDeadOrDying() && random.nextInt(3) == 0 && anim() != A_DEAD) {
                Vec3 f = forward(yBodyRot);
                level().addParticle(ParticleTypes.SMALL_FLAME, getX() + f.x * 0.8 + (random.nextDouble() - 0.5) * 0.3, getY() + 2.45,
                        getZ() + f.z * 0.8 + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
            }
            return;
        }
        if (isDeadOrDying()) return;
        ServerLevel sl = (ServerLevel) level();
        ++st;
        if (cooldown > 0) --cooldown;
        if (tickCount % 10 == 0) updateBar(sl);
        LivingEntity tgt = target(sl);
        switch (state) {
            case DORMANT -> {
                getNavigation().stop();
                setAnim(A_DEAD);
            }
            case RISING -> {
                getNavigation().stop();
                if (st == 14 || st == 30) sound(SoundEvents.SKELETON_STEP, 1.2F, 0.5F);
                if (st == 30) sound(SoundEvents.WARDEN_AGITATED, 1.6F, 0.6F);
                if (st == 62) sound(SoundEvents.RAVAGER_STEP, 1.6F, 0.5F);
                if (st == 80) sound(SoundEvents.WARDEN_SNIFF, 2.0F, 0.5F);
                if (st >= RISE_T) {
                    go(S.WAITING);
                    setAnim(A_IDLE);
                }
            }
            case WAITING -> {
                getNavigation().stop();
                if (tgt != null) getLookControl().setLookAt(tgt, 40, 40);
                if (st % 80 == 40) sound(SoundEvents.WARDEN_LISTENING, 1.4F, 0.6F);
                if (core == null && tgt != null && distanceTo(tgt) < 8) beginFight();
            }
            case ROAR -> {
                getNavigation().stop();
                if (tgt != null) faceTo(tgt, 20);
                if (st == 16) {
                    sound(SoundEvents.WARDEN_ROAR, 3.0F, 0.55F);
                    sound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.5F);
                }
                if (st == SUMMON_AT) summon(sl);
                if (st >= ROAR_T) {
                    go(S.FIGHT);
                    setAnim(A_IDLE);
                    cooldown = 20;
                }
            }
            case FIGHT -> fight(sl, tgt);
            case ATTACK -> attack(sl, tgt);
            case PULL -> {
                getNavigation().stop();
                setYRot(lockYaw);
                yBodyRot = lockYaw;
                if (st == 6 || st == 11) sound(SoundEvents.GRINDSTONE_USE, 1.2F, 0.5F);
                if (st == 18) {
                    sound(SoundEvents.ANVIL_LAND, 0.8F, 0.4F);
                    Vec3 p = blade(2.4);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, getY() + 0.2, p.z, 10, 0.4, 0.1, 0.4, 0.02);
                }
                if (st >= PULL_END) {
                    go(S.FIGHT);
                    setAnim(A_IDLE);
                    cooldown = 16 + random.nextInt(14);
                }
            }
            case CLIMB_GO -> climbGo(tgt);
            case CLIMB -> climb();
            case HOP -> hop();
        }
        // sharp head twitches while idle/walking (the head snaps, then returns)
        if (state == S.WAITING || state == S.FIGHT) {
            if (twitch > 0) {
                --twitch;
                yHeadRot = yBodyRot + twitchYaw;
            } else if (random.nextInt(state == S.WAITING ? 60 : 110) == 0) {
                twitch = 5 + random.nextInt(4);
                twitchYaw = (random.nextBoolean() ? 1 : -1) * (35 + random.nextInt(25));
                if (random.nextInt(3) == 0) sound(SoundEvents.WOLF_GROWL, 1.0F, 0.45F);
            }
        }
    }

    private void fight(ServerLevel sl, @Nullable LivingEntity tgt) {
        setAnim(A_IDLE);
        ArenaCoreBlockEntity ar = arena();
        if (ar != null && tgt != null && !ar.hallBox().inflate(0.5).contains(tgt.position())) tgt = null;   // never follows out of the hall
        if (tgt == null) {
            if (core != null && distanceToSqr(core.getX() + 0.5, core.getY(), core.getZ() - 4.5) > 25) {
                if (tickCount % 20 == 0) getNavigation().moveTo(core.getX() + 0.5, core.getY(), core.getZ() - 4.5, 0.9);
            } else getNavigation().stop();
            return;
        }
        if (!second && getHealth() < getMaxHealth() * 0.5F) {
            second = true;
            if (aliveGuards(sl) < 2) {
                go(S.ROAR);
                setAnim(A_ROAR, true);
                return;
            }
        }
        if (tgt.getY() > getY() + 5.0) {
            // the player is up on the gallery: he does not take the stairs - waits below, glaring (climbs only when shot)
            getNavigation().stop();
            faceTo(tgt, 10);
            getLookControl().setLookAt(tgt, 30, 30);
            return;
        }
        double d = distanceTo(tgt);
        float want = yawTo(tgt);
        float diff = Mth.wrapDegrees(want - getYRot());
        if (d < 4.1 && cooldown <= 0 && Math.abs(diff) < 55 && Math.abs(tgt.getY() - getY()) < 2.5) {
            int r = random.nextInt(100);
            atk = r < 36 ? A_OVER : r < 72 ? A_SIDE : A_DIAG;
            go(S.ATTACK);
            setAnim(atk, true);
            lockYaw = want;
            getNavigation().stop();
            sound(SoundEvents.RAVAGER_ATTACK, 1.4F, 0.55F);
            return;
        }
        if (d < 3.0) {
            getNavigation().stop();
            faceTo(tgt, 12);
        } else if (tickCount % 5 == 0) {
            getNavigation().moveTo(tgt, 1.0);
        }
        getLookControl().setLookAt(tgt, 30, 30);
    }

    private void attack(ServerLevel sl, @Nullable LivingEntity tgt) {
        getNavigation().stop();
        int hit = atk == A_OVER ? OVER_HIT : atk == A_SIDE ? SIDE_HIT : DIAG_HIT;
        int end = atk == A_OVER ? OVER_END : atk == A_SIDE ? SIDE_END : DIAG_END;
        if (st < hit - LOCK_BEFORE && tgt != null) lockYaw = Mth.approachDegrees(lockYaw, yawTo(tgt), 8);
        setYRot(lockYaw);
        yBodyRot = lockYaw;
        yHeadRot = lockYaw;
        if (st == hit - LOCK_BEFORE) {
            // telegraph: the blade flares up a moment before the blow
            Vec3 p = blade(1.5).add(0, 2.2, 0);
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.02);
            sound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.5F);
        }
        if (st == hit) {
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
            if (atk == A_OVER) {
                hitLine(sl, 0.6, 4.6, 1.05, 15.0F);
                Vec3 p = blade(2.6);
                impact(sl, p);
            } else if (atk == A_SIDE) {
                hitArc(sl, 4.4, 85, 11.0F, 1.2);
                for (int k = -6; k <= 6; ++k) {
                    Vec3 p = position().add(forward(lockYaw + k * 13).scale(3.2));
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, getY() + 1.2, p.z, 1, 0, 0, 0, 0);
                }
            } else {
                hitArc(sl, 4.2, 50, 13.0F, 0.6);
                Vec3 p = position().add(forward(lockYaw - 20).scale(2.6));
                impact(sl, p);
            }
        }
        if (st >= end) {
            if (atk == A_SIDE) {
                go(S.FIGHT);
                setAnim(A_IDLE);
                cooldown = 18 + random.nextInt(16);
            } else {
                go(S.PULL);
                setAnim(atk == A_OVER ? A_PULL : A_PULL_DIAG, true);
            }
        }
    }

    private void impact(ServerLevel sl, Vec3 p) {
        sound(SoundEvents.ANVIL_LAND, 1.4F, 0.45F);
        sound(SoundEvents.GENERIC_EXPLODE, 0.6F, 0.5F);
        sl.sendParticles(ParticleTypes.LAVA, p.x, getY() + 0.1, p.z, 8, 0.3, 0.05, 0.3, 0.1);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, getY() + 0.2, p.z, 6, 0.4, 0.1, 0.4, 0.01);
        sl.sendParticles(ParticleTypes.FLAME, p.x, getY() + 0.1, p.z, 20, 0.6, 0.05, 0.6, 0.03);
    }

    /** Blade tip position on the floor `dist` blocks ahead along the locked swing direction. */
    private Vec3 blade(double dist) {
        return position().add(forward(lockYaw).scale(dist));
    }

    static Vec3 forward(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private float yawTo(Entity e) {
        return (float) (Mth.atan2(e.getZ() - getZ(), e.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void faceTo(Entity e, float step) {
        float y = Mth.approachDegrees(getYRot(), yawTo(e), step);
        setYRot(y);
        yBodyRot = y;
    }

    private List<LivingEntity> victims(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2.0, r),
                e -> e != this && e.isAlive() && !(e instanceof EmberGuard) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    private void hitLine(ServerLevel sl, double from, double to, double half, float dmg) {
        Vec3 f = forward(lockYaw);
        for (LivingEntity e : victims(to)) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double along = dx * f.x + dz * f.z, side = Math.abs(dx * f.z - dz * f.x);
            if (along >= from && along <= to + e.getBbWidth() * 0.5 && side <= half + e.getBbWidth() * 0.5 && Math.abs(e.getY() - getY()) < 2.5) {
                e.hurt(damageSources().mobAttack(this), dmg);
            }
        }
    }

    private void hitArc(ServerLevel sl, double r, double halfDeg, float dmg, double knock) {
        for (LivingEntity e : victims(r)) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > r + e.getBbWidth() * 0.5 || Math.abs(e.getY() - getY()) > 2.5) continue;
            float a = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            if (Math.abs(Mth.wrapDegrees(a - lockYaw)) > halfDeg && dist > 1.2) continue;
            if (e.hurt(damageSources().mobAttack(this), dmg) && knock > 0) e.knockback(knock, -dx / Math.max(dist, 0.1), -dz / Math.max(dist, 0.1));
        }
    }

    // ------------------------------------------------------------------------------------------------ guards
    private void summon(ServerLevel sl) {
        List<Vec3> spots = new ArrayList<>();
        List<Integer> dirs = new ArrayList<>();
        if (core != null) {
            int[][] rel = {{-10, 9, -20}, {-10, 9, -12}, {10, 9, -20}, {10, 9, -12}};
            for (int[] r : rel) {
                spots.add(new Vec3(core.getX() + r[0] + 0.5, core.getY() + r[1], core.getZ() + r[2] + 0.5));
                dirs.add(r[0] < 0 ? 1 : -1);
            }
        } else {
            for (int k = 0; k < 4; ++k) {
                Vec3 f = forward(getYRot() + 45 + k * 90).scale(5);
                spots.add(position().add(f).add(0, 0.5, 0));
                dirs.add(0);
            }
        }
        for (int k = 0; k < spots.size(); ++k) {
            EmberGuard g = ModEntities.GUARD.get().create(sl);
            if (g == null) continue;
            Vec3 p = spots.get(k);
            g.moveTo(p.x, p.y, p.z, dirs.get(k) > 0 ? -90 : dirs.get(k) < 0 ? 90 : getYRot(), 0);
            g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(p)), MobSpawnType.MOB_SUMMONED, null, null);
            g.prepareLeap(dirs.get(k), 6 + k * 7, core);
            sl.addFreshEntity(g);
            guards.add(g.getUUID());
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 16, 0.3, 0.6, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1, p.z, 10, 0.3, 0.6, 0.3, 0.02);
        }
        sound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.5F);
    }

    private int aliveGuards(ServerLevel sl) {
        int n = 0;
        for (UUID u : guards) if (sl.getEntity(u) instanceof EmberGuard g && g.isAlive()) ++n;
        return n;
    }

    public void discardGuards(ServerLevel sl) {
        for (UUID u : guards) {
            Entity e = sl.getEntity(u);
            if (e != null) e.discard();
        }
        guards.clear();
    }

    // ------------------------------------------------------------------------------------------------ wall climbing
    private void startClimb(Player p) {
        if (core == null || !(state == S.FIGHT || state == S.ATTACK && st > 30)) return;
        climbTarget = p;
        boolean west = p.getX() < core.getX() + 0.5;
        double bx = core.getX() + (west ? -7.5 : 8.5);
        double bz = Mth.clamp(p.getZ(), core.getZ() - 26.5, core.getZ() + 1.5);
        if (bz > core.getZ() - 9 && bz < core.getZ() - 3 && !west) bz = core.getZ() - 10.5;    // east gallery is fallen there
        for (int sz : new int[] {-23, -16, -9}) {                 // keep clear of the column sockets along the walls
            double c = core.getZ() + sz + 0.5;
            if (Math.abs(bz - c) < 2.2) bz = c + (bz >= c ? 2.6 : -2.6);
        }
        if (bz > core.getZ() - 9 && bz < core.getZ() - 3 && !west) bz = core.getZ() - 11.6;
        climbBase = new Vec3(bx, getY(), bz);
        climbTop = new Vec3(core.getX() + (west ? -9.5 : 10.5), core.getY() + 9, bz);
        go(S.CLIMB_GO);
        setAnim(A_IDLE);
        sound(SoundEvents.WARDEN_ANGRY, 2.0F, 0.6F);
    }

    private void climbGo(@Nullable LivingEntity tgt) {
        if (climbBase == null) {
            go(S.FIGHT);
            return;
        }
        double dx = climbBase.x - getX(), dz = climbBase.z - getZ();
        if (dx * dx + dz * dz < 1.0 || st > 30 && dx * dx + dz * dz < 12.0 || st > 140) {   // close enough (sockets, rubble) -> jump on the wall
            go(S.CLIMB);
            setAnim(A_CLIMB, true);
            setNoGravity(true);
            noPhysics = true;
            getNavigation().stop();
            setPos(climbBase.x, getY(), climbBase.z);
            return;
        }
        if (st % 10 == 1) getNavigation().moveTo(climbBase.x, climbBase.y, climbBase.z, 1.15);
    }

    private void climb() {
        float yaw = climbTop.x < getX() ? 90 : -90;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        setDeltaMovement(Vec3.ZERO);
        setPos(getX(), getY() + 0.16, getZ());
        if (st % 6 == 0) sound(SoundEvents.STONE_HIT, 1.0F, 0.6F);
        if (st % 3 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX() + (climbTop.x < getX() ? -0.7 : 0.7), getY() + 1.5, getZ(), 2, 0.2, 0.3, 0.2, 0.01);
        }
        if (getY() >= climbTop.y + 0.2 || st > 120) {
            go(S.HOP);
            setAnim(A_IDLE);
        }
    }

    private void hop() {
        Vec3 d = new Vec3(climbTop.x - getX(), 0, climbTop.z - getZ());
        if (d.length() > 0.25 && st < 20) {
            Vec3 m = d.normalize().scale(0.22);
            setPos(getX() + m.x, Math.max(getY(), climbTop.y + 0.05), getZ() + m.z);
            return;
        }
        noPhysics = false;
        setNoGravity(false);
        go(S.FIGHT);
        cooldown = 6;
        sound(SoundEvents.RAVAGER_STEP, 2.0F, 0.5F);
    }

    // ------------------------------------------------------------------------------------------------ damage / death
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (state == S.DORMANT || state == S.RISING || state == S.ROAR && st < SUMMON_AT) return false;
        if (src.getEntity() instanceof Player p && !level().isClientSide) {
            if (state == S.WAITING) {
                ArenaCoreBlockEntity a = arena();
                if (a != null) a.trigger((ServerLevel) level());
                else beginFight();
            }
            if (src.getDirectEntity() instanceof AbstractArrow && core != null && p.getY() > core.getY() + 6 && getY() < core.getY() + 4) {
                startClimb(p);
            }
        }
        if (state == S.PULL || state == S.ATTACK && atk != A_SIDE && st > (atk == A_OVER ? OVER_HIT : DIAG_HIT)) amount *= 1.5F;   // scythe stuck
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            setFlag(F_SCYTHE_GONE, true);
            setAnim(A_DEATH, true);
            noPhysics = false;
            setNoGravity(false);
            Vec3 f = forward(yBodyRot);
            spawnAtLocation(new ItemStack(ModItems.MORGRATH_SCYTHE.get()), 0.2F);
            sound(SoundEvents.WARDEN_DEATH, 3.0F, 0.5F);
            sound(SoundEvents.WITHER_DEATH, 0.8F, 0.6F);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + f.x, getY() + 2.0, getZ() + f.z, 30, 0.5, 0.6, 0.5, 0.03);
            discardGuards(sl);
            ArenaCoreBlockEntity a = arena();
            if (a != null) a.bossDefeated(sl);
            bar.removeAllPlayers();
        }
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof ServerLevel sl) {
            if (deathTime > 60 && deathTime % 7 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 3, 0.8, 0.2, 0.8, 0.01);
            }
            if (deathTime >= LIE_T) {
                Vec3 f = forward(yBodyRot);
                for (int k = 0; k < 5; ++k) {
                    Vec3 p = position().add(f.scale(k * 0.5));
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, getY() + 0.4, p.z, 10, 0.4, 0.3, 0.4, 0.02);
                    sl.sendParticles(ParticleTypes.WHITE_ASH, p.x, getY() + 0.5, p.z, 20, 0.5, 0.4, 0.5, 0.02);
                }
                sound(SoundEvents.SAND_BREAK, 2.0F, 0.5F);
                dropExperience();
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return !isWaiting() && !isDeadOrDying();
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Nullable
    private LivingEntity target(ServerLevel sl) {
        if (climbTarget != null && (!climbTarget.isAlive() || climbTarget.isSpectator() || climbTarget.isCreative())) climbTarget = null;
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && !(t instanceof Player p && (p.isSpectator() || p.isCreative())) && distanceToSqr(t) < 48 * 48) return t;
        Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 40, e -> e instanceof Player pl && !pl.isSpectator() && !pl.isCreative());
        setTarget(p);
        return p;
    }

    private void updateBar(ServerLevel sl) {
        bar.setProgress(getHealth() / getMaxHealth());
        boolean show = !isWaiting();
        bar.setVisible(show);
        AABB box = getBoundingBox().inflate(40);
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) if (!box.contains(p.position()) || !p.isAlive()) bar.removePlayer(p);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public void remove(RemovalReason r) {
        bar.removeAllPlayers();
        super.remove(r);
    }

    private void sound(SoundEvent ev, float vol, float pitch) {
        level().playSound(null, getX(), getY() + 1.5, getZ(), ev, SoundSource.HOSTILE, vol, pitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }

    // ------------------------------------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BossState", state.ordinal());
        tag.putBoolean("Second", second);
        if (core != null) tag.put("Core", NbtUtils.writeBlockPos(core));
        ListTag l = new ListTag();
        for (UUID u : guards) l.add(NbtUtils.createUUID(u));
        tag.put("Guards", l);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int s = tag.getInt("BossState");
        state = S.values()[Mth.clamp(s, 0, S.values().length - 1)];
        if (state == S.ATTACK || state == S.PULL || state == S.CLIMB_GO || state == S.CLIMB || state == S.HOP || state == S.ROAR) state = S.FIGHT;
        if (state == S.RISING) state = S.WAITING;
        second = tag.getBoolean("Second");
        core = tag.contains("Core") ? NbtUtils.readBlockPos(tag.getCompound("Core")) : null;
        guards.clear();
        for (Tag t : tag.getList("Guards", Tag.TAG_INT_ARRAY)) guards.add(NbtUtils.loadUUID(t));
        setAnim(state == S.DORMANT ? A_DEAD : A_IDLE, true);
        if (hasCustomName()) bar.setName(getDisplayName());
    }
}
