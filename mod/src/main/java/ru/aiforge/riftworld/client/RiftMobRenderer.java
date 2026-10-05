package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.EmberGuard;
import ru.aiforge.riftworld.entity.MorgrathBoss;
import ru.aiforge.riftworld.entity.RiftMob;
import ru.aiforge.riftworld.glb.GlbModel;
import ru.aiforge.riftworld.glb.Pose;

/**
 * Draws the boss / guards from their animated .glb: current clip (+ walk cycle blended by speed) cross-faded from the
 * previous clip over 4 ticks, head tracking split over neck and head, hurt flash, wall-crawl tilt for the guards.
 */
public class RiftMobRenderer<T extends RiftMob> extends EntityRenderer<T> {
    private static final float FADE = 4.0F;
    private final ResourceLocation glb;
    private final float scale;

    public RiftMobRenderer(EntityRendererProvider.Context ctx, String model, float shadow, float scale) {
        super(ctx);
        this.glb = new ResourceLocation(RiftWorld.MODID, "models/glb/" + model + ".glb");
        this.shadowRadius = shadow;
        this.scale = scale;
    }

    public static RiftMobRenderer<MorgrathBoss> boss(EntityRendererProvider.Context c) {
        return new RiftMobRenderer<>(c, "boss", 1.0F, 1.0F);
    }

    public static RiftMobRenderer<EmberGuard> guard(EntityRendererProvider.Context c) {
        return new RiftMobRenderer<>(c, "guard", 0.5F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return glb;
    }

    private GlbModel.Animation clip(GlbModel m, T e, int a) {
        String[] c = e.clips();
        return a >= 0 && a < c.length ? m.animation(c[a]) : null;
    }

    private void layer(Pose pose, GlbModel m, T e, int a, int start, float now, float w, float partial) {
        if (a < 0 || w <= 0) return;
        pose.sampleBlend(clip(m, e, a), (now - start) / 20.0F, w, e.loops(a));
        int loc = e.locomotion(a);
        if (loc >= 0) {
            float sp = Mth.clamp(e.walkAnimation.speed(partial) * 2.2F, 0.0F, 1.0F);
            if (sp > 0.02F) pose.sampleBlend(clip(m, e, loc), e.walkAnimation.position(partial) * e.strideScale(), sp * w, true);
        }
    }

    /** Ease-out with a small overshoot (~6%) - reads as a snap that "lands". */
    private static float snapEase(float u) {
        float c = 1.9F;
        float v = u - 1.0F;
        return 1.0F + (c + 1.0F) * v * v * v + c * v * v;
    }

    @Override
    public void render(T e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        GlbAssets.Loaded lm = GlbAssets.get(glb);
        if (lm == null) return;
        GlbModel m = lm.model;
        float now = (float) e.level().getGameTime() + partial;
        boolean crawl = e instanceof EmberGuard g && g.climbing();
        int a = e.anim();
        if (crawl && (a == EmberGuard.A_IDLE)) a = EmberGuard.A_CRAWL;
        int start = crawl && a == EmberGuard.A_CRAWL ? 0 : e.animStart();
        if (a != e.cLast || start != e.cLastStart) {
            if (e.cLast >= 0) {
                e.cPrev = e.cLast;
                e.cPrevStart = e.cLastStart;
                e.cSwitch = now;
            }
            e.cLast = a;
            e.cLastStart = start;
        }
        Pose pose = lm.scratch();
        float fade = e.cPrev < 0 ? 1.0F : Mth.clamp((now - e.cSwitch) / FADE, 0.0F, 1.0F);
        if (fade < 1.0F) {
            layer(pose, m, e, e.cPrev, e.cPrevStart, now, 1.0F, partial);
            layer(pose, m, e, a, start, now, fade, partial);
        } else {
            e.cPrev = -1;
            layer(pose, m, e, a, start, now, 1.0F, partial);
        }
        if (e instanceof MorgrathBoss b && b.scytheGone()) pose.hide("scythe", true);
        pose.compute();
        if (e.headLook() && e.isAlive()) {
            float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
            float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
            float want = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -70.0F, 70.0F);
            float wantPitch = Mth.clamp(Mth.lerp(partial, e.xRotO, e.getXRot()), -40.0F, 40.0F);
            // sharp, predatory head turns: the head keeps its direction until the gaze drifts past a threshold, then
            // snaps to the new one in ~2 ticks (ease-out), with a tiny overshoot - never a smooth glide
            float snapT = e instanceof EmberGuard ? 1.6F : 2.4F;
            float u0 = Mth.clamp((now - e.cHeadT0) / snapT, 0.0F, 1.0F);
            float curNow = Mth.lerp(snapEase(u0), e.cHeadFrom, e.cHeadTo);
            if (Math.abs(want - e.cHeadTo) > (e instanceof EmberGuard ? 9.0F : 13.0F) || now < e.cHeadT0 - 1) {
                e.cHeadFrom = curNow;
                e.cHeadTo = want;
                e.cHeadT0 = now;
            }
            if (Math.abs(wantPitch - e.cHeadPitch) > 10.0F) e.cHeadPitch = wantPitch;
            float u = Mth.clamp((now - e.cHeadT0) / snapT, 0.0F, 1.0F);
            float relDeg = Mth.lerp(snapEase(u), e.cHeadFrom, e.cHeadTo);
            // a quick involuntary twitch every few seconds (seeded per mob, so all viewers see the same jerk)
            float roll = 0.0F;
            int period = e instanceof EmberGuard ? 46 : 70;
            int slot = (int) Math.floor(now / period);
            long seed = (long) e.getId() * 341873128712L + slot * 132897987541L;
            java.util.Random rnd = new java.util.Random(seed);
            float tStart = slot * period + rnd.nextInt(period - 10);
            float k = now - tStart;
            if (k >= 0 && k < 6) {
                float amp = (rnd.nextBoolean() ? 1 : -1) * (14 + rnd.nextInt(16));
                float env = k < 1 ? k : k < 3.5F ? 1.0F : Math.max(0.0F, 1.0F - (k - 3.5F) / 2.5F);
                relDeg += amp * env;
                roll = amp * 0.6F * env;
            }
            float rel = relDeg * Mth.DEG_TO_RAD;
            float pitch = e.cHeadPitch * Mth.DEG_TO_RAD;
            int neck = m.nodeIndex("neck"), head = m.nodeIndex("head");
            pose.rotateInModel(neck, new Quaternionf().rotationY(-rel * 0.4F));
            pose.compute();
            pose.rotateInModel(head, new Quaternionf().rotationY(-rel * 0.6F).rotateX(pitch * 0.8F).rotateZ(roll * Mth.DEG_TO_RAD));
            pose.compute();
        }
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        if (crawl) {
            // belly to the wall: tip the whole body up 80 degrees around the hips and press it against the wall
            ps.translate(0, 0.9, 0.15);
            ps.mulPose(Axis.XP.rotationDegrees(-80));
            ps.translate(0, -0.9, 0);
        }
        ps.scale(scale, scale, scale);
        int overlay = e.deathTime > 0 ? net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY : LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        GlbRenderer.render(lm, pose, ps, buffers, light, overlay);
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }
}
