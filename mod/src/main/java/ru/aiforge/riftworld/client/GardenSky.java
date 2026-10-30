package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * Рев 14.14: небо над Висячими садами.
 *
 * <p>На референсе {@code reports/refs_rev4/07_waterfall_gardens.png} небо голубое с редкими
 * облаками, а над Элисаром висит общая кремово-жёлтая дымка: в кадрах садов фон выходил
 * жёлто-зелёным и биом «не читался» как тот же самый. Поэтому над регионом садов рисуем
 * собственный небесный купол — вертикальный градиент от голубого к светлому у горизонта.
 *
 * <p>Куб рисуется на стадии AFTER_SKY (то есть ДО всей геометрии мира): всё, что ближе,
 * просто перекрывает его обычным тестом глубины, поэтому острова, деревья и вода остаются
 * как есть. Работает только внутри биома {@code riftworld:waterfall_gardens}.
 */
public final class GardenSky {
    /** Верх зенита и цвет у горизонта — по референсу. */
    private static final float ZENITH_R = 0.31F, ZENITH_G = 0.55F, ZENITH_B = 0.86F;
    private static final float HORIZ_R = 0.80F, HORIZ_G = 0.90F, HORIZ_B = 0.97F;

    private GardenSky() {
    }

    /** Результат проверки кешируем: расчёт региона нетривиален, а кадров в секунду много. */
    private static long cacheKey = Long.MIN_VALUE;
    private static boolean cacheHit;

    private static boolean inGardens(Vec3 cam) {
        long key = ((long) (int) Math.floor(cam.x / 16) << 20) ^ (long) (int) Math.floor(cam.z / 16);
        if (key == cacheKey) return cacheHit;
        boolean hit = false;
        try {
            hit = WaterfallGardens.skyInside(cam.x, cam.z);
        } catch (Throwable ignored) {
            // клиент не должен падать из-за неба: при любой ошибке просто не рисуем купол
        }
        cacheKey = key;
        cacheHit = hit;
        return hit;
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
    public static final class Render {
        @SubscribeEvent
        public static void sky(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
            Vec3 cam = e.getCamera().getPosition();
            // Проверяем не биом (камера висит высоко над островом, там биом уже другой), а сам
            // регион садов: он считается чистой математикой из сида, без данных сервера.
            if (!inGardens(cam)) return;

            Matrix4f m = e.getPoseStack().last().pose();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            // Рев 14.16: купол обязан целиком влезать в дальнюю плоскость отсечения, иначе он
            // обрезается и в небе остаются жёлтые клинья (кадр 42 итерации 16). Берём 85 % от
            // дальности прорисовки — этого хватает, чтобы закрыть весь горизонт.
            final float s = Math.max(96F, mc.options.getEffectiveRenderDistance() * 16F * 0.85F);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // четыре стены: у горизонта светлее, к зениту — насыщенный голубой
            float[][] corners = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
            for (int i = 0; i < 4; i++) {
                float[] p0 = corners[i], p1 = corners[(i + 1) % 4];
                b.vertex(m, p0[0], -s, p0[1]).color(HORIZ_R, HORIZ_G, HORIZ_B, 1F).endVertex();
                b.vertex(m, p1[0], -s, p1[1]).color(HORIZ_R, HORIZ_G, HORIZ_B, 1F).endVertex();
                b.vertex(m, p1[0], s, p1[1]).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
                b.vertex(m, p0[0], s, p0[1]).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
            }
            // потолок купола
            b.vertex(m, -s, s, -s).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
            b.vertex(m, s, s, -s).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
            b.vertex(m, s, s, s).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
            b.vertex(m, -s, s, s).color(ZENITH_R, ZENITH_G, ZENITH_B, 1F).endVertex();
            Tesselator.getInstance().end();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
