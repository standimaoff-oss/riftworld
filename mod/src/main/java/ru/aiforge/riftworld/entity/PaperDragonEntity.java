package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;

/**
 * v0.24.0: Enlarged (~47.7m), ultra-smooth Modular 3D Origami Dragon.
 * <ul>
 *   <li><b>Strict White City Exclusion</b>: never enters the White City ({@link #isInsideCityNoFlyZone});</li>
 *   <li><b>Natural Island-Perimeter &amp; Vault Flight</b>: glides gracefully along the outer cliff rims of wild
 *       floating islands, beneath high island vaults, and across open sky/sea — NEVER threading between spires;</li>
 *   <li><b>Curvature-Limited C2 Motion &amp; Persistent Spine Chain</b>: 6-step client interpolation + arc-length-locked,
 *       joint-angle-clamped 72-node spine chain eliminates jerky turns and guarantees zero vanishing points/head!</li>
 * </ul>
 */
public class PaperDragonEntity extends FlyingMob {
    /** 72 spine nodes × 0.54m = 38.9m modular body + 4.3m Imperial Head + 4.5m Tail Fan ≈ 47.7m total length! */
    public static final int SEGS = 72;
    public static final float STEP = 0.54F;
    /** Maximum bend angle per spine link (6.5°) — guarantees zero kinking, folding, or inverted head/rings! */
    private static final float MAX_JOINT_COS = 0.9935F; // cos(6.5 deg)
    private static final float MAX_JOINT_SIN = 0.1132F; // sin(6.5 deg)

    private static final double BASE_SPEED = 0.42;
    private static final float MAX_YAW_STEP_DEG = 1.50F;
    private static final float MAX_PITCH_STEP_DEG = 1.05F;

    /** Flight modes strictly outside island building interiors and outside the White City. */
    public static final int FLIGHT_ALONG_ISLAND_RIM = 0;
    public static final int FLIGHT_UNDER_ISLAND     = 1;
    public static final int FLIGHT_OPEN_SKY_SEA     = 2;
    public static final int FLIGHT_OVER_ISLAND      = FLIGHT_ALONG_ISLAND_RIM;

    /** Minimum safe distance from (0,0) and around each White City island. */
    public static final double CITY_CENTER_EXCLUSION_R = 335.0;
    public static final double CITY_ISLAND_BUFFER      = 95.0;

    private static final EntityDataAccessor<Float> DATA_PHASE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);

    // Server flight state (smooth tangential orbit + low-pass steering)
    private Vec3 targetPoint;
    private Vec3 smoothAimDir;
    private int waypointTimer;
    private int flightMode = FLIGHT_ALONG_ISLAND_RIM;
    private double orbitCenterX, orbitCenterZ, orbitRadius = 90.0, orbitAltY = 115.0;
    private double orbitAngle;
    private int orbitSign = 1;

    // Client-side persistent arc-length-locked spine chain (interpolated cleanly at any FPS!)
    public boolean chainInit;
    public final double[] chainX     = new double[SEGS];
    public final double[] chainY     = new double[SEGS];
    public final double[] chainZ     = new double[SEGS];
    public final double[] prevChainX = new double[SEGS];
    public final double[] prevChainY = new double[SEGS];
    public final double[] prevChainZ = new double[SEGS];
    public final float[]  chainRoll     = new float[SEGS];
    public final float[]  prevChainRoll = new float[SEGS];

    // Smooth unit head forward vector on client (never flips or collapses!)
    public double headFx = 0.0, headFy = 0.0, headFz = 1.0;
    public double prevHeadFx = 0.0, prevHeadFy = 0.0, prevHeadFz = 1.0;
    public float bankRoll, bankRollPrev;
    public float jawAnim, jawAnimPrev;
    private float prevSmoothYaw;

    public PaperDragonEntity(EntityType<? extends PaperDragonEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.FLYING_SPEED, BASE_SPEED);
    }

    /** Returns true if (x, z) lies inside or near the White City no-fly zone. */
    public static boolean isInsideCityNoFlyZone(CityPlan plan, double x, double z, double extraBuffer) {
        double r0 = CITY_CENTER_EXCLUSION_R + extraBuffer;
        if (x * x + z * z < r0 * r0) return true;
        if (plan == null) return false;
        for (CityPlan.Island ci : plan.islands) {
            double lim = ci.R * 1.18 + CITY_ISLAND_BUFFER + extraBuffer;
            if (lim <= 8.0) continue;
            double dx = x - ci.cx;
            double dz = z - ci.cz;
            if (dx * dx + dz * dz < lim * lim) return true;
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0.0F);
    }

    public float wavePhase() {
        return this.entityData.get(DATA_PHASE);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.entityData.set(DATA_PHASE, this.random.nextFloat() * Mth.TWO_PI);
        this.orbitSign = this.random.nextBoolean() ? 1 : -1;
        this.orbitAngle = this.random.nextDouble() * Mth.TWO_PI;
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        // 6-step client glide completely eliminates server-packet sawtooth jitter!
        super.lerpTo(x, y, z, yRot, xRot, teleport ? 1 : Math.max(steps, 6), teleport);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return distSq > 360.0 * 360.0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 384.0 * 384.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // Covers the entire ~48m dragon + tail fan from any camera angle so zero segments ever cull!
        return super.getBoundingBoxForCulling().inflate(96.0, 64.0, 96.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientSpineTick();
        } else {
            serverFlightTick();
        }
    }

    // =========================================================================
    // CLIENT: PERSISTENT, ARC-LENGTH-LOCKED, JOINT-CLAMPED 72-NODE SPINE CHAIN
    // =========================================================================

    private void clientSpineTick() {
        float curYaw = this.getYRot();
        float curPitch = Mth.clamp(this.getXRot(), -34.0F, 34.0F);
        float yawRad = curYaw * Mth.DEG_TO_RAD;
        float pitchRad = curPitch * Mth.DEG_TO_RAD;
        double rawFx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double rawFy = -Mth.sin(pitchRad);
        double rawFz =  Mth.cos(yawRad) * Mth.cos(pitchRad);

        if (!chainInit) {
            chainInit = true;
            prevSmoothYaw = curYaw;
            headFx = prevHeadFx = rawFx;
            headFy = prevHeadFy = rawFy;
            headFz = prevHeadFz = rawFz;
            for (int i = 0; i < SEGS; ++i) {
                double d = i * STEP;
                double px = this.getX() - rawFx * d;
                double py = this.getY() - rawFy * d;
                double pz = this.getZ() - rawFz * d;
                chainX[i] = prevChainX[i] = px;
                chainY[i] = prevChainY[i] = py;
                chainZ[i] = prevChainZ[i] = pz;
                chainRoll[i] = prevChainRoll[i] = 0.0F;
            }
            return;
        }

        // 1) Snapshot previous tick state for butter-smooth partialTick interpolation
        System.arraycopy(chainX, 0, prevChainX, 0, SEGS);
        System.arraycopy(chainY, 0, prevChainY, 0, SEGS);
        System.arraycopy(chainZ, 0, prevChainZ, 0, SEGS);
        System.arraycopy(chainRoll, 0, prevChainRoll, 0, SEGS);
        prevHeadFx = headFx;
        prevHeadFy = headFy;
        prevHeadFz = headFz;
        bankRollPrev = bankRoll;
        jawAnimPrev = jawAnim;

        // 2) Smooth head forward vector blended with actual motion + gentle slow serpentine sway
        double vx = this.getX() - this.xo;
        double vy = this.getY() - this.yo;
        double vz = this.getZ() - this.zo;
        double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);

        double desFx = rawFx, desFy = rawFy, desFz = rawFz;
        if (vLen > 0.035) {
            double mvx = vx / vLen, mvy = Mth.clamp(vy / vLen, -0.52, 0.52), mvz = vz / vLen;
            desFx = Mth.lerp(0.55, rawFx, mvx);
            desFy = Mth.lerp(0.55, rawFy, mvy);
            desFz = Mth.lerp(0.55, rawFz, mvz);
        }
        float phase = wavePhase();
        float t = this.tickCount;
        // Subtle slow imperial head undulation
        double sideHx = desFz, sideHz = -desFx;
        double sideHLen = Math.hypot(sideHx, sideHz);
        if (sideHLen > 1.0E-4) {
            sideHx /= sideHLen;
            sideHz /= sideHLen;
            double headSway = Math.sin(t * 0.042F + phase) * 0.075;
            desFx += sideHx * headSway;
            desFz += sideHz * headSway;
        }
        desFy += Math.cos(t * 0.034F + phase * 0.7F) * 0.035;
        double dLen = Math.sqrt(desFx * desFx + desFy * desFy + desFz * desFz);
        if (dLen > 1.0E-5) {
            desFx /= dLen;
            desFy /= dLen;
            desFz /= dLen;
        }

        // Low-pass filter head forward vector (max ~2.8 deg/tick -> zero head snapping!)
        headFx = Mth.lerp(0.14, headFx, desFx);
        headFy = Mth.lerp(0.14, headFy, desFy);
        headFz = Mth.lerp(0.14, headFz, desFz);
        double hLen = Math.sqrt(headFx * headFx + headFy * headFy + headFz * headFz);
        if (hLen > 1.0E-6) {
            headFx /= hLen;
            headFy /= hLen;
            headFz /= hLen;
        } else {
            headFx = 0.0;
            headFy = 0.0;
            headFz = 1.0;
        }

        // Smooth banking roll & subtle breathing jaw
        float smoothYaw = Mth.rotLerp(0.12F, prevSmoothYaw, curYaw);
        float yawRate = Mth.wrapDegrees(smoothYaw - prevSmoothYaw);
        prevSmoothYaw = smoothYaw;
        float targetRoll = Mth.clamp(-yawRate * 11.0F, -24.0F, 24.0F);
        bankRoll = Mth.lerp(0.08F, bankRoll, targetRoll);

        float targetJaw = 0.05F + 0.05F * Mth.sin(t * 0.048F + phase)
                + (Mth.sin(t * 0.017F + phase * 1.9F) > 0.90F ? 0.14F : 0.0F);
        jawAnim = Mth.lerp(0.10F, jawAnim, targetJaw);

        // 3) Advance node 0 and solve follow-the-leader + gentle traveling wave + 6.5° max joint angle!
        chainX[0] = this.getX();
        chainY[0] = this.getY();
        chainZ[0] = this.getZ();
        chainRoll[0] = bankRoll;

        double refBx = -headFx;
        double refBy = -headFy;
        double refBz = -headFz;

        for (int i = 1; i < SEGS; ++i) {
            float u = (float) i / (float) (SEGS - 1);
            float env = Mth.clamp(u * 2.2F, 0.0F, 1.0F) * (0.35F + 0.65F * u);

            // Gentle traveling aerial wave nudges each link smoothly before distance & angle locking
            double latSX = -refBz, latSZ = refBx;
            double latSL = Math.hypot(latSX, latSZ);
            if (latSL > 1.0E-4) {
                latSX /= latSL;
                latSZ /= latSL;
            } else {
                latSX = 1.0;
                latSZ = 0.0;
            }
            double wavePushLat  = Math.sin(t * 0.068F - i * 0.16F + phase) * 0.022 * env;
            double wavePushVert = Math.cos(t * 0.052F - i * 0.13F + phase * 0.8F) * 0.011 * env;

            double dx = (chainX[i] + latSX * wavePushLat) - chainX[i - 1];
            double dy = (chainY[i] + wavePushVert)        - chainY[i - 1];
            double dz = (chainZ[i] + latSZ * wavePushLat) - chainZ[i - 1];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1.0E-5) {
                dx = refBx;
                dy = refBy;
                dz = refBz;
            } else {
                dx /= dist;
                dy /= dist;
                dz /= dist;
            }

            // Clamp joint bend angle relative to refB to <= 6.5° so spine is ALWAYS a smooth ribbon!
            double dot = dx * refBx + dy * refBy + dz * refBz;
            if (dot < MAX_JOINT_COS) {
                double px = dx - dot * refBx;
                double py = dy - dot * refBy;
                double pz = dz - dot * refBz;
                double pLen = Math.sqrt(px * px + py * py + pz * pz);
                if (pLen > 1.0E-6) {
                    px /= pLen;
                    py /= pLen;
                    pz /= pLen;
                    dx = refBx * MAX_JOINT_COS + px * MAX_JOINT_SIN;
                    dy = refBy * MAX_JOINT_COS + py * MAX_JOINT_SIN;
                    dz = refBz * MAX_JOINT_COS + pz * MAX_JOINT_SIN;
                } else {
                    dx = refBx;
                    dy = refBy;
                    dz = refBz;
                }
            }

            chainX[i] = chainX[i - 1] + dx * STEP;
            chainY[i] = chainY[i - 1] + dy * STEP;
            chainZ[i] = chainZ[i - 1] + dz * STEP;
            chainRoll[i] = Mth.lerp(0.18F, chainRoll[i], chainRoll[i - 1] * 0.965F
                    + Mth.sin(t * 0.068F - i * 0.16F + phase) * 1.1F * env);

            refBx = dx;
            refBy = dy;
            refBz = dz;
        }
    }

    // =========================================================================
    // SERVER: SMOOTH ISLAND-PERIMETER, UNDER-VAULT & OPEN-SEA FLIGHT (0 CITY!)
    // =========================================================================

    private void serverFlightTick() {
        boolean isElisar = this.level().dimension().equals(Flight.ELISAR_DIM);
        CityPlan plan = isElisar ? CityPlan.get() : null;
        int ocean = plan != null ? plan.ocean : this.level().getSeaLevel();

        // 1) Strict White City exclusion: if anywhere near the White City, glide smoothly outward to open sea!
        if (isElisar && isInsideCityNoFlyZone(plan, this.getX(), this.getZ(), 15.0)) {
            double rx = this.getX();
            double rz = this.getZ();
            double rLen = Math.hypot(rx, rz);
            if (rLen < 1.0) {
                rx = 1.0;
                rz = 0.0;
                rLen = 1.0;
            }
            rx /= rLen;
            rz /= rLen;
            targetPoint = new Vec3(
                    this.getX() + rx * 95.0,
                    Math.max(this.getY(), ocean + 44.0),
                    this.getZ() + rz * 95.0);
            waypointTimer = 40;
            flightMode = FLIGHT_OPEN_SKY_SEA;
        } else if (targetPoint == null || --waypointTimer <= 0 || this.position().distanceToSqr(targetPoint) < 196.0) {
            advanceSmoothFlightTarget(plan, ocean, isElisar);
        }

        // 2) Smooth low-pass desired direction + gentle 5-ray lookahead terrain clearance
        Vec3 toTarget = targetPoint.subtract(this.position());
        double dist = toTarget.length();
        Vec3 rawDir = dist > 0.01 ? toTarget.scale(1.0 / dist) : Vec3.directionFromRotation(this.getXRot(), this.getYRot());

        // Gentle slow breathing wave along macro trajectory (unhurried, majestic)
        float phase = wavePhase();
        float slowT = this.tickCount * 0.028F + phase;
        Vec3 right = new Vec3(-rawDir.z, 0.0, rawDir.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        Vec3 desired = rawDir
                .add(right.scale(Math.sin(slowT) * 0.10))
                .add(0.0, Math.cos(slowT * 0.75F) * 0.04, 0.0);

        desired = blendMultiRayClearance(desired, ocean);
        if (desired.lengthSqr() > 1.0E-6) desired = desired.normalize();

        if (smoothAimDir == null || smoothAimDir.lengthSqr() < 1.0E-4) {
            smoothAimDir = desired;
        } else {
            smoothAimDir = smoothAimDir.scale(0.945).add(desired.scale(0.055));
            if (smoothAimDir.lengthSqr() > 1.0E-6) smoothAimDir = smoothAimDir.normalize();
        }

        // 3) Curvature-clamped yaw & pitch integration (<= 1.50°/tick yaw, <= 1.05°/tick pitch)
        double horiz = Math.hypot(smoothAimDir.x, smoothAimDir.z);
        float wantYaw = (float) (Mth.atan2(smoothAimDir.z, smoothAimDir.x) * (180.0 / Math.PI)) - 90.0F;
        float wantPitch = Mth.clamp((float) -(Mth.atan2(smoothAimDir.y, Math.max(0.05, horiz)) * (180.0 / Math.PI)), -26.0F, 26.0F);

        float dYaw = Mth.clamp(Mth.wrapDegrees(wantYaw - this.getYRot()), -MAX_YAW_STEP_DEG, MAX_YAW_STEP_DEG);
        float dPitch = Mth.clamp(wantPitch - this.getXRot(), -MAX_PITCH_STEP_DEG, MAX_PITCH_STEP_DEG);
        float nextYaw = this.getYRot() + dYaw;
        float nextPitch = Mth.clamp(this.getXRot() + dPitch, -26.0F, 26.0F);
        this.setYRot(nextYaw);
        this.yBodyRot = nextYaw;
        this.yHeadRot = nextYaw;
        this.setXRot(nextPitch);

        // 4) Move purely along the dragon's smooth forward tangent (zero sideways crabbing or snaps!)
        Vec3 forward = Vec3.directionFromRotation(nextPitch, nextYaw);
        double targetSpeed = BASE_SPEED * (1.0 - 0.10 * (Math.abs(dYaw) / MAX_YAW_STEP_DEG));
        Vec3 prevVel = this.getDeltaMovement();
        Vec3 nextVel = prevVel.scale(0.93).add(forward.scale(targetSpeed * 0.07));
        if (nextVel.length() < BASE_SPEED * 0.72) {
            nextVel = forward.scale(BASE_SPEED * 0.72);
        }

        // Soft sea-surface floor cushion
        if (this.getY() < ocean + 11.0 && nextVel.y < 0.05) {
            nextVel = new Vec3(nextVel.x, Mth.lerp(0.10, nextVel.y, 0.09), nextVel.z);
        }
        int maxSkyY = this.level().getMaxBuildHeight() - 30;
        if (this.getY() > maxSkyY && nextVel.y > -0.04) {
            nextVel = new Vec3(nextVel.x, Mth.lerp(0.10, nextVel.y, -0.08), nextVel.z);
        }

        this.setDeltaMovement(nextVel);
        this.move(net.minecraft.world.entity.MoverType.SELF, nextVel);

        if (this.horizontalCollision || this.verticalCollision) {
            // Gentle tangential deflection without resetting timer abruptly
            orbitSign = -orbitSign;
            orbitAngle += orbitSign * 0.45;
        }
    }

    /**
     * Advances a smooth tangential waypoint along wild island perimeters, beneath high island vaults,
     * or across open sea/sky — NEVER over island building centers and NEVER near the White City!
     */
    private void advanceSmoothFlightTarget(@Nullable CityPlan plan, int ocean, boolean isElisar) {
        CityPlan.Island wildIsl = isElisar ? findWildFloatingIsland(plan, this.getX(), this.getZ()) : null;

        if (wildIsl != null) {
            double vaultBottom = wildIsl.base - wildIsl.depth;
            boolean tallUnderVault = (vaultBottom - ocean) >= 30.0;

            if (this.random.nextFloat() < 0.14F) {
                float roll = this.random.nextFloat();
                if (tallUnderVault && roll < 0.30F) {
                    flightMode = FLIGHT_UNDER_ISLAND;
                } else if (roll < 0.82F) {
                    flightMode = FLIGHT_ALONG_ISLAND_RIM;
                } else {
                    flightMode = FLIGHT_OPEN_SKY_SEA;
                }
            }
            if (flightMode == FLIGHT_UNDER_ISLAND && !tallUnderVault) {
                flightMode = FLIGHT_ALONG_ISLAND_RIM;
            }

            orbitCenterX = wildIsl.cx;
            orbitCenterZ = wildIsl.cz;
            double curAng = Math.atan2(this.getZ() - orbitCenterZ, this.getX() - orbitCenterX);
            // Advance 32°..48° tangentially along the perimeter arc
            orbitAngle = curAng + orbitSign * (0.56 + this.random.nextDouble() * 0.28);

            if (flightMode == FLIGHT_UNDER_ISLAND) {
                // Wide arc deep beneath the floating island's stone vault
                orbitRadius = wildIsl.R * (0.45 + this.random.nextDouble() * 0.45);
                orbitAltY = Mth.lerp(0.46 + this.random.nextDouble() * 0.20, ocean + 12.0, vaultBottom - 11.0);
            } else if (flightMode == FLIGHT_ALONG_ISLAND_RIM) {
                // Strictly OUTSIDE the island's outer cliff reach (+28..52m) so it NEVER threads between buildings/spires!
                orbitRadius = wildIsl.maxReach() + 28.0 + this.random.nextDouble() * 24.0;
                orbitAltY = wildIsl.base - 4.0 + this.random.nextDouble() * 24.0;
            } else {
                // High open sky well above all peaks/spires, or wide sea crossing
                orbitRadius = wildIsl.maxReach() + 45.0 + this.random.nextDouble() * 45.0;
                orbitAltY = wildIsl.base + 38.0 + this.random.nextDouble() * 24.0;
            }

            double wx = orbitCenterX + Math.cos(orbitAngle) * orbitRadius;
            double wz = orbitCenterZ + Math.sin(orbitAngle) * orbitRadius;
            if (isElisar && isInsideCityNoFlyZone(plan, wx, wz, 25.0)) {
                orbitSign = -orbitSign;
                orbitAngle = curAng + orbitSign * 0.85;
                wx = orbitCenterX + Math.cos(orbitAngle) * orbitRadius;
                wz = orbitCenterZ + Math.sin(orbitAngle) * orbitRadius;
            }
            double wy = Mth.clamp(orbitAltY, ocean + 13.0, this.level().getMaxBuildHeight() - 34.0);
            targetPoint = new Vec3(wx, wy, wz);
            waypointTimer = 140 + this.random.nextInt(90);
            return;
        }

        // Open sea / Overworld high-sky sweeping arc
        float curYawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double baseAng = Math.atan2(Mth.cos(curYawRad), -Mth.sin(curYawRad));
        double nextAng = baseAng + (this.random.nextDouble() - 0.5) * 0.85;
        double dist = 70.0 + this.random.nextDouble() * 55.0;
        double wx = this.getX() + Math.cos(nextAng) * dist;
        double wz = this.getZ() + Math.sin(nextAng) * dist;

        if (isElisar && isInsideCityNoFlyZone(plan, wx, wz, 25.0)) {
            double outAng = Math.atan2(this.getZ(), this.getX());
            wx = this.getX() + Math.cos(outAng) * 90.0;
            wz = this.getZ() + Math.sin(outAng) * 90.0;
        }

        int ix = Mth.floor(wx), iz = Mth.floor(wz);
        int ground = this.level().hasChunk(ix >> 4, iz >> 4)
                ? this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz)
                : ocean;
        // Stay >= 34m above any ground/structures so the dragon glides freely in open sky!
        double wy = Math.max(ground, ocean) + 34.0 + this.random.nextDouble() * 28.0;
        wy = Mth.clamp(wy, ocean + 16.0, this.level().getMaxBuildHeight() - 34.0);
        targetPoint = new Vec3(wx, wy, wz);
        waypointTimer = 150 + this.random.nextInt(90);
    }

    /** Multi-ray fan clearance: gently steers around cliffs/spires 30m ahead without any sharp spikes. */
    private Vec3 blendMultiRayClearance(Vec3 desired, int ocean) {
        Vec3 start = this.position();
        Vec3 d = desired.lengthSqr() > 1.0E-5 ? desired.normalize() : new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-d.z, 0.0, d.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        else right = new Vec3(1, 0, 0);

        double pushRight = 0.0;
        double pushUp = 0.0;

        double[] angles = {-0.48, -0.24, 0.0, 0.24, 0.48};
        for (double a : angles) {
            Vec3 rayDir = d.scale(Math.cos(a)).add(right.scale(Math.sin(a))).normalize();
            var hit = this.level().clip(new ClipContext(
                    start, start.add(rayDir.scale(30.0)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            if (hit.getType() != HitResult.Type.MISS) {
                double frac = 1.0 - Math.min(1.0, Math.sqrt(hit.getLocation().distanceToSqr(start)) / 30.0);
                if (a < -0.05) pushRight += frac * 0.32;
                else if (a > 0.05) pushRight -= frac * 0.32;
                else {
                    pushRight += orbitSign * frac * 0.38;
                    pushUp += (flightMode == FLIGHT_UNDER_ISLAND ? -0.16 : 0.28) * frac;
                }
            }
        }

        // Ground clearance directly below when not intentionally flying deep under a high vault
        if (flightMode != FLIGHT_UNDER_ISLAND) {
            int gx = this.getBlockX(), gz = this.getBlockZ();
            if (this.level().hasChunk(gx >> 4, gz >> 4)) {
                int topY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
                if (topY > ocean + 4 && this.getY() < topY + 22.0 && this.getY() > topY - 4.0) {
                    // Gently lift well above rooftops/trees if ever passing near terrain
                    pushUp += Mth.clamp((topY + 24.0 - this.getY()) * 0.025, 0.0, 0.35);
                }
            }
        } else {
            // Under-island ceiling cushion
            var ceilHit = this.level().clip(new ClipContext(
                    start, start.add(0.0, 13.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (ceilHit.getType() != HitResult.Type.MISS) {
                pushUp -= 0.24;
            }
        }

        return d.add(right.scale(Mth.clamp(pushRight, -0.55, 0.55)))
                .add(0.0, Mth.clamp(pushUp, -0.35, 0.40), 0.0);
    }

    public void setHomeAnchor(Vec3 anchor, int mode) {
        this.orbitCenterX = anchor.x;
        this.orbitAltY = anchor.y;
        this.orbitCenterZ = anchor.z;
        this.flightMode = mode;
        this.orbitAngle = Math.atan2(this.getZ() - anchor.z, this.getX() - anchor.x);
        this.orbitRadius = Math.max(40.0, Math.hypot(this.getX() - anchor.x, this.getZ() - anchor.z));
        this.waypointTimer = 240;
    }

    public static double lowestSolidInColumn(Level level, double x, double z, int yBottom, int yTop) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int ix = Mth.floor(x), iz = Mth.floor(z);
        for (int y = yBottom + 1; y <= yTop; ++y) {
            BlockState bs = level.getBlockState(mp.set(ix, y, iz));
            if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                return y;
            }
        }
        return yTop + 1;
    }

    @Nullable
    private static CityPlan.Island findWildFloatingIsland(@Nullable CityPlan plan, double x, double z) {
        CityPlan.Island best = null;
        double bestScore = Double.MAX_VALUE;
        // includeCity = false -> NEVER considers White City islands!
        for (CityPlan.Island isl : FloatingIslands.gather(x, z, 280.0, false)) {
            if (isl.kind == 0 || isl.R < 22.0) continue;
            if (isInsideCityNoFlyZone(plan, isl.cx, isl.cz, 55.0)) continue;
            double d = Math.hypot(x - isl.cx, z - isl.cz);
            double score = Math.abs(d - (isl.maxReach() + 36.0)) - isl.R * 0.35;
            if (score < bestScore) {
                bestScore = score;
                best = isl;
            }
        }
        return best;
    }
}
