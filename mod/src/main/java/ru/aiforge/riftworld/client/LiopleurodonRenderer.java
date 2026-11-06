package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.LiopleurodonEntity;

/**
 * v0.41.1 — рендер Лиоплевродона настоящей JSON-моделью ({@link GeoModel}):
 * 12 костей, 554 кубоида, атлас 1024x1024 (`assets/riftworld/geo/liopleurodon.json`).
 * Модель пересобрана в `tools/rift/gen_v041_liopleurodon_model.py`: фасеточная
 * торпеда-корпус, отдельная подвижная челюсть с оскалом, трёхслойные глаза, четыре
 * веслообразных ласта с лучами, боковая линия и серповидный хвостовой плавник.
 * Честные UV-островки упакованы skyline-алгоритмом без наложений; ласты,
 * челюсть и три сегмента хвоста анимируются на клиенте, пакетов это не тратит.
 *
 * <p>Как и {@link GeoMobRenderer}, явно применяет {@code scale(-s,-s,s)}:
 * geo выгружен в системе +Y вниз, а кастомный {@code EntityRenderer} не делает
 * ванильного отражения. Без него зверь в игре показывал брюхо вместо спины.
 */
public class LiopleurodonRenderer extends EntityRenderer<LiopleurodonEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation("riftworld", "textures/entity/liopleurodon.png");

    public LiopleurodonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2F;
    }

    @Override
    public ResourceLocation getTextureLocation(LiopleurodonEntity e) {
        return TEX;
    }

    @Override
    public void render(LiopleurodonEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel model = GeoModel.of("liopleurodon");
        if (model == null) {
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float time = e.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));   // модель смотрит носом в -Z
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = model.renderScale;
        pose.scale(-s, -s, s);          // +Y модели вниз → +Y мира вверх
        model.animate(time);

        VertexConsumer vc = buf.getBuffer(model.translucent
                ? RenderType.entityTranslucent(TEX) : RenderType.entityCutoutNoCull(TEX));
        model.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
