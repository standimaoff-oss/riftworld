package ru.aiforge.riftworld.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base of the GLB-animated castle mobs. The server picks a clip (index into {@link #clips()}) and stamps the game time it
 * started; the client samples the clip from the boss/guard .glb at (gameTime - start) and cross-fades on every change.
 */
public abstract class RiftMob extends Monster {
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(RiftMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM_T = SynchedEntityData.defineId(RiftMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(RiftMob.class, EntityDataSerializers.BYTE);

    /** Client-side cross-fade bookkeeping (renderer only). */
    public int cLast = -1, cPrev = -1, cLastStart, cPrevStart;
    public float cSwitch;
    /** Client-side head snap state (renderer only): the head holds a direction and jerks to a new one, never glides. */
    public float cHeadFrom, cHeadTo, cHeadT0 = -100, cHeadPitch;

    protected RiftMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM, 0);
        this.entityData.define(ANIM_T, 0);
        this.entityData.define(FLAGS, (byte) 0);
    }

    /** glTF clip names by animation id. */
    public abstract String[] clips();

    /** Whether the clip loops (one-shots hold their last frame). */
    public abstract boolean loops(int anim);

    /** Clip blended in by walking speed while the mob is in a locomotion clip (-1 = none). */
    public abstract int locomotion(int anim);

    /** Distance-to-cycle factor for the locomotion clip (seconds of clip per unit of walkAnimation.position). */
    public abstract float strideScale();

    public void setAnim(int a) {
        setAnim(a, false);
    }

    public void setAnim(int a, boolean restart) {
        if (!restart && anim() == a) return;
        this.entityData.set(ANIM_T, (int) level().getGameTime());
        this.entityData.set(ANIM, a);
    }

    /** Start a clip as if it had begun `ticksAgo` ticks earlier (skips into the middle, e.g. combo follow-ups). */
    public void setAnimAt(int a, int ticksAgo) {
        this.entityData.set(ANIM_T, (int) level().getGameTime() - ticksAgo);
        this.entityData.set(ANIM, a);
    }

    public int anim() {
        return this.entityData.get(ANIM);
    }

    public int animStart() {
        return this.entityData.get(ANIM_T);
    }

    /** Seconds since the current clip started. */
    public float animTime(float partial) {
        return ((int) level().getGameTime() - animStart() + partial) / 20.0F;
    }

    protected boolean flag(int bit) {
        return (this.entityData.get(FLAGS) & (1 << bit)) != 0;
    }

    protected void setFlag(int bit, boolean on) {
        byte b = this.entityData.get(FLAGS);
        byte n = (byte) (on ? b | (1 << bit) : b & ~(1 << bit));
        if (n != b) this.entityData.set(FLAGS, n);
    }

    /** Extra hide/rotate tweaks after sampling (client). */
    public boolean headLook() {
        return true;
    }
}
