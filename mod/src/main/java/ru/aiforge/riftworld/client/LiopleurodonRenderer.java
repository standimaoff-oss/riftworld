package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.LiopleurodonEntity;

/**
 * v0.45.0 — Лиоплевродон на новой 16-костяной оснастке.
 *
 * <p>Тело изогнуто бегущей волной (каждая кость отстаёт по фазе, амплитуда
 * растёт к хвосту), четыре ласта гребут в противофазе с опережением передней
 * пары, кисть ласта запаздывает; голова слегка противо-качается, челюсть
 * приоткрывается на укусе. Всё считается на клиенте, пакеты не тратятся.
 * Корпус кренится в поворот, нос идёт по тангажу сущности.
 */
public class LiopleurodonRenderer extends EntityRenderer<LiopleurodonEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation("riftworld", "textures/entity/liopleurodon.png");
    private static final float DEG = (float) (Math.PI / 180.0);

    public LiopleurodonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2F;
    }

    @Override
    public ResourceLocation getTextureLocation(LiopleurodonEntity e) {
        return TEX;
    }

    @Override
    public boolean shouldRender(LiopleurodonEntity e, Frustum frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z)
                && (e.noCulling || frustum.isVisible(e.getBoundingBoxForCulling().inflate(6.0, 2.0, 6.0)));
    }

    /** root -> bone chain lookup that never throws when a rig changes. */
    private static ModelPart part(ModelPart from, String... path) {
        ModelPart p = from;
        for (String name : path) {
            if (p == null) return null;
            try {
                p = p.getChild(name);
            } catch (RuntimeException missing) {
                return null;
            }
        }
        return p;
    }

    @Override
    public void render(LiopleurodonEntity e, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buf, int light) {
        GeoModel model = GeoModel.of("liopleurodon");
        if (model == null) {
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        model.animate(e.tickCount + partial);   // reset every joint to the bind pose
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float yawRate = Mth.wrapDegrees(e.getYRot() - e.yRotO);
        float time = e.tickCount + partial;
        Vec3 motion = e.getDeltaMovement();
        float effort = Mth.clamp((float) motion.horizontalDistance() * 3.0F + (e.isInWater() ? 0.55F : 0.25F),
                0.25F, 1.35F);
        float wave = time * (0.16F + 0.06F * effort) * effort;

        ModelPart front = part(model.root, "body_front");
        ModelPart mid = part(model.root, "body_front", "body_mid");
        ModelPart rear = part(model.root, "body_front", "body_mid", "body_rear");
        ModelPart tail1 = part(model.root, "body_front", "body_mid", "body_rear", "tail_1");
        ModelPart tail2 = part(model.root, "body_front", "body_mid", "body_rear", "tail_1", "tail_2");
        ModelPart neck = part(model.root, "body_front", "neck");
        ModelPart head = part(model.root, "body_front", "neck", "head");
        ModelPart jaw = part(model.root, "body_front", "neck", "head", "jaw");

        // travelling wave: larger amplitude and later phase toward the tail
        if (mid != null) mid.yRot = 7.0F * DEG * Mth.sin(wave - 0.9F);
        if (rear != null) rear.yRot = 11.0F * DEG * Mth.sin(wave - 1.8F);
        if (tail1 != null) {
            tail1.yRot = 16.0F * DEG * Mth.sin(wave - 2.7F);
            tail1.xRot = 1.5F * DEG * Mth.sin(wave - 2.5F);
        }
        if (tail2 != null) {
            tail2.yRot = 22.0F * DEG * Mth.sin(wave - 3.5F);
            tail2.xRot = 2.0F * DEG * Mth.sin(wave - 3.2F);
        }
        if (neck != null) neck.yRot = -4.0F * DEG * Mth.sin(wave - 0.3F);
        if (head != null) {
            head.yRot = -5.0F * DEG * Mth.sin(wave + 0.4F);
            head.xRot = 1.5F * DEG * Mth.sin(wave * 0.6F + 1.0F);
        }
        if (front != null) front.zRot = 3.5F * DEG * Mth.sin(wave * 0.5F);

        // four flippers: front pair leads, rear pair follows half a beat later
        for (int i = 0; i < 2; i++) {
            boolean isFront = i == 0;
            float phase = wave * (isFront ? 1.0F : 0.85F) + (isFront ? 0.0F : 2.1F);
            float lift = Mth.sin(phase);
            float feather = Mth.cos(phase - 0.6F);
            String stem = isFront ? "flipper_front_" : "flipper_rear_";
            for (String side : new String[]{"l", "r"}) {
                float s = side.equals("l") ? -1.0F : 1.0F;
                ModelPart base = part(front, stem + side);
                if (base == null) continue;
                float baseLift = (isFront ? 14.0F : 10.0F) + lift * (isFront ? 34.0F : 26.0F);
                base.zRot = s * baseLift * DEG;
                base.xRot = s * feather * 12.0F * DEG;
                ModelPart tip = part(base, stem + side + "_tip");
                if (tip != null) tip.zRot = s * (-lift * 18.0F) * DEG;
            }
        }

        if (jaw != null) {
            jaw.xRot = (e.isBiting() ? 12.0F : 1.5F + 1.2F * Mth.sin(time * 0.05F)) * DEG;
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));   // snout authored toward -Z
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(yawRate * 1.6F, -14.0F, 14.0F)));
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = model.renderScale;
        pose.scale(s, -s, s);   // Y-up source rig
        VertexConsumer vc = buf.getBuffer(model.translucent
                ? RenderType.entityTranslucent(TEX) : RenderType.entityCutoutNoCull(TEX));
        model.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
