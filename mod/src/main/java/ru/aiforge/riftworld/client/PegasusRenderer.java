package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.PegasusEntity;

/**
 * v0.45.0 — крылатый конь на новой 27-костяной оснастке.
 *
 * <p>Анимация целиком клиентская (пакеты не тратятся): диагональная рысь с
 * подгибанием бабок, покачивание корпуса, кивок шеи, трепет гривы и хвоста,
 * редкое подёргивание ушей, складывание крыла в покое и полный взмах в полёте
 * с запаздыванием кисти относительно плеча. Корпус кренится в поворот, а по
 * тангажу наклона головы всадника пегас задирает или опускает нос.
 */
public final class PegasusRenderer extends EntityRenderer<PegasusEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("riftworld", "textures/entity/pegasus.png");
    private static final float DEG = (float) (Math.PI / 180.0);

    public PegasusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.15F;
    }

    @Override
    public ResourceLocation getTextureLocation(PegasusEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(PegasusEntity e, Frustum frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z)
                && (e.noCulling || frustum.isVisible(e.getBoundingBoxForCulling().inflate(4.0, 3.0, 4.0)));
    }

    private static ModelPart child(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (RuntimeException missing) {
            return null;
        }
    }

    @Override
    public void render(PegasusEntity e, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        GeoModel model = GeoModel.of("pegasus");
        if (model == null) {
            super.render(e, yaw, partial, pose, buffers, light);
            return;
        }
        model.animate(e.tickCount + partial);   // reset every joint to its bind pose
        ModelPart body = model.root.getChild("body");
        ModelPart saddle = child(body, "saddle");
        if (saddle != null) saddle.visible = e.hasSaddle();

        float time = e.tickCount + partial;
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float yawRate = Mth.wrapDegrees(e.getYRot() - e.yRotO);
        boolean air = e.isFlying() || (!e.onGround() && !e.isInWater());
        float speed = Math.min(1.35F, e.walkAnimation.speed(partial));
        float stride = e.walkAnimation.position(partial) * 0.62F;
        float gait = Mth.cos(stride) * Math.min(1.0F, speed * 1.5F);

        // ---------------------------------------------------------------- legs
        // Diagonal pairs move together (trot); the lower joint only bends while
        // the leg is travelling forward, which is what makes a walk read.
        animateLeg(body, "leg_front_l", gait, 1.0F, air, speed);
        animateLeg(body, "leg_rear_r", gait, 1.0F, air, speed);
        animateLeg(body, "leg_front_r", -gait, 1.0F, air, speed);
        animateLeg(body, "leg_rear_l", -gait, 1.0F, air, speed);

        // ------------------------------------------------------------- torso/joints
        float bob = Mth.cos(stride * 2.0F) * 0.035F * Math.min(1.0F, speed);
        ModelPart neckLow = child(body, "neck_low");
        ModelPart neckUp = child(body, "neck_up");
        ModelPart head = child(body, "head");
        if (neckLow != null) neckLow.xRot = -0.10F + bob + 0.06F * Mth.cos(stride) * Math.min(1.0F, speed);
        if (neckUp != null) neckUp.xRot = -0.06F + 0.05F * Mth.cos(stride + 0.5F) * Math.min(1.0F, speed);
        if (head != null) head.xRot = 0.12F - 0.08F * Mth.cos(stride + 0.8F) * Math.min(1.0F, speed);

        // ears flick a few times per minute instead of wobbling constantly
        float flick = (float) Math.pow(Math.max(0.0F, Mth.sin(time * 0.045F)), 12.0F);
        ModelPart earL = child(body, "ear_l");
        ModelPart earR = child(body, "ear_r");
        if (earL != null) earL.zRot = -0.35F * flick;
        if (earR != null) earR.zRot = 0.35F * flick;

        // ---------------------------------------------------------------- mane
        for (int i = 1; i <= 3; i++) {
            ModelPart mane = child(body, "mane_" + i);
            if (mane == null) continue;
            float phase = time * (0.10F + i * 0.02F) + i * 0.7F;
            mane.zRot = 0.16F * Mth.sin(phase) + (air ? 0.10F * Mth.cos(phase * 1.6F) : 0.0F);
            mane.xRot = -0.10F - (air ? 0.18F : 0.04F) * Mth.sin(phase * 1.3F);
        }

        // ---------------------------------------------------------------- tail
        ModelPart tail1 = child(body, "tail_1");
        ModelPart tail2 = child(body, "tail_2");
        ModelPart tail3 = child(body, "tail_3");
        if (tail1 != null) {
            tail1.xRot = -0.22F - (air ? 0.35F : 0.06F);
            tail1.yRot = 0.12F * Mth.sin(time * 0.09F);
        }
        if (tail2 != null) {
            tail2.xRot = 0.10F;
            tail2.yRot = 0.16F * Mth.sin(time * 0.09F - 0.7F);
        }
        if (tail3 != null) tail3.yRot = 0.22F * Mth.sin(time * 0.09F - 1.4F);

        // ---------------------------------------------------------------- wings
        float flapSpeed = air ? 0.30F : 0.085F;
        float flap = time * flapSpeed;
        float stroke = Mth.sin(flap);
        if (air) {
            float effort = 0.85F + 0.25F * Mth.sin(time * 0.035F);   // slow breathing of the rhythm
            float shoulder = (12.0F + 48.0F * stroke) * effort;
            float elbow = (-14.0F - 18.0F * Mth.sin(flap - 0.55F)) * effort;
            float wrist = (8.0F + 16.0F * Mth.sin(flap - 1.05F)) * effort;
            setWing(body, "l", shoulder, elbow, wrist, -8.0F);
            setWing(body, "r", shoulder, elbow, wrist, -8.0F);
        } else {
            // folded: the wrist swings back along the flank (yaw), and only a
            // small lift/elbow tuck keeps the primaries clear of the barrel
            float breathe = 4.0F * Mth.sin(time * 0.045F);
            float shoulder = 38.0F + breathe + (speed > 0.05F ? 3.0F * Mth.cos(stride) : 0.0F);
            setWing(body, "l", shoulder, -12.0F, -6.0F, 14.0F + breathe * 0.5F);
            setWing(body, "r", shoulder, -12.0F, -6.0F, 14.0F + breathe * 0.5F);
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(yawRate * 2.4F, -20.0F, 20.0F)));   // banking
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.scale(model.renderScale, -model.renderScale, model.renderScale);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** Upper joint swings, lower joint folds; both tuck in flight. */
    private static void animateLeg(ModelPart body, String name, float gait, float scale,
                                   boolean air, float speed) {
        ModelPart upper = child(body, name);
        ModelPart lower = child(body, name + "_low");
        if (air) {
            if (upper != null) {
                upper.xRot = (name.startsWith("leg_front") ? 0.55F : -0.42F) + 0.06F * Mth.sin(speed * 3.0F);
            }
            if (lower != null) lower.xRot = name.startsWith("leg_front") ? -1.05F : 0.85F;
            return;
        }
        float swing = gait * scale * 0.55F;
        if (upper != null) upper.xRot = swing;
        if (lower != null) {
            // knee/hock flexes while the hoof is behind the body
            float bend = Math.max(0.0F, -swing) * 1.1F + Math.max(0.0F, swing) * 0.15F;
            lower.xRot = name.startsWith("leg_front") ? bend : -bend * 0.85F;
        }
    }

    private static void setWing(ModelPart body, String side, float shoulderDeg, float elbowDeg,
                                float wristDeg, float sweepDeg) {
        ModelPart wing = child(body, "wing_" + side);
        ModelPart mid = child(body, "wing_" + side + "_mid");
        ModelPart tip = child(body, "wing_" + side + "_tip");
        float s = side.equals("l") ? -1.0F : 1.0F;
        if (wing != null) {
            wing.zRot = s * shoulderDeg * DEG;
            wing.yRot = -s * sweepDeg * DEG;   // + = tucked backwards along the flank
        }
        if (mid != null) mid.zRot = s * elbowDeg * DEG;
        if (tip != null) tip.zRot = s * wristDeg * DEG;
    }
}
