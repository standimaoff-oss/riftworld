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
import ru.aiforge.riftworld.entity.SeaFishEntity;

/**
 * Рев 11: рыбы рисуются НАСТОЯЩИМИ моделями из JSON ({@link GeoModel}) с атласом, UV, освещением
 * и скелетной анимацией. Раньше здесь были дебаг-квады с белой текстурой — «поломанные рыбы без текстур».
 *
 * <p>Каждый из 11 видов — свой файл {@code assets/riftworld/geo/<вид>.json} и своя текстура
 * {@code assets/riftworld/textures/entity/<вид>.png}. Проверка в CI снимает каждый вид отдельным кадром.
 */
public class SeaFishRenderer extends EntityRenderer<SeaFishEntity> {
    /** Индексы совпадают с константами {@link SeaFishEntity}: SILVER..STING. */
    private static final String[] MODELS = {
            "silver", "gold", "striped", "jelly", "ray", "angler", "eel", "surgeon", "turtle", "gloom", "sting"
    };

    public SeaFishRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.25F;
    }

    public static String modelName(int variant) {
        return MODELS[Math.max(0, Math.min(MODELS.length - 1, variant))];
    }

    @Override
    public ResourceLocation getTextureLocation(SeaFishEntity e) {
        return new ResourceLocation("riftworld", "textures/entity/" + modelName(e.variant()) + ".png");
    }

    @Override
    public void render(SeaFishEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel model = GeoModel.of(modelName(e.variant()));
        if (model == null) {                       // файла нет — молча пропускаем, но не роняем кадр
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float time = e.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));      // модель смотрит носом в -Z
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = model.renderScale;
        pose.scale(s, s, s);
        model.animate(time);

        ResourceLocation tex = getTextureLocation(e);
        VertexConsumer vc = buf.getBuffer(model.translucent
                ? RenderType.entityTranslucent(tex) : RenderType.entityCutoutNoCull(tex));
        model.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
