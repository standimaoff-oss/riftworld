package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.world.SizoGrove;
import ru.aiforge.riftworld.world.VioletHills;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * Атмосферные купола биомов Элисара с плавным пространственно-временным переходом (без резких скачков):
 * - Висячие сады (07_waterfall_gardens.png): чистое голубое небо со светлым горизонтом;
 * - Фиолетовые холмы (04_violet_hills.png): нежно-сиреневый зенит + тёплое розово-персиковое зарево у горизонта с крупным тёплым диском солнца;
 * - Роща Сизо (05_biome_sizo_grove.png): мягкое бледно-лазурное небо вверху и тёплая солнечная дымка между стволами у горизонта.
 */
public final class GardenSky {
    private static final float G_ZEN_R = 0.31F, G_ZEN_G = 0.55F, G_ZEN_B = 0.86F;
    private static final float G_HOR_R = 0.80F, G_HOR_G = 0.90F, G_HOR_B = 0.97F;

    // 04_violet_hills.png: пастельно-лавандовый верх и тёплое персиково-розовое зарево у горизонта
    private static final float V_ZEN_R = 0.78F, V_ZEN_G = 0.72F, V_ZEN_B = 0.88F;
    private static final float V_HOR_R = 0.96F, V_HOR_G = 0.82F, V_HOR_B = 0.76F;

    // 05_biome_sizo_grove.png: светло-голубая прохлада в зените и тёплая молочно-золотистая лесная дымка
    private static final float S_ZEN_R = 0.77F, S_ZEN_G = 0.85F, S_ZEN_B = 0.90F;
    private static final float S_HOR_R = 0.95F, S_HOR_G = 0.93F, S_HOR_B = 0.88F;

    private static Vec3 lastCam;
    private static long lastNs;
    private static float curGarden, curViolet, curSizo;
    private static double sunAng = 0.7;
    private static boolean sunInit;

    private GardenSky() {
    }

    /**
     * Синхронное плавное состояние весов атмосферы {gardens, violet, sizo} в [0..1].
     * При ходьбе и полёте веса плавно интерполируются и по расстоянию (smoothstep ~90 блоков),
     * и по времени кадра; при далёком телепорте (/tp) сбрасываются мгновенно в целевую точку.
     */
    public static float[] smoothWeights(Vec3 cam) {
        float tg = 0.0F, tv = 0.0F, ts = 0.0F;
        try {
            tg = WaterfallGardens.skyWeight(cam.x, cam.z);
            tv = VioletHills.skyWeight(cam.x, cam.z);
            ts = SizoGrove.skyWeight(cam.x, cam.z);
        } catch (Throwable ignored) {
        }
        long now = System.nanoTime();
        boolean teleported = lastCam == null || cam.distanceToSqr(lastCam) > 64.0;
        if (teleported) {
            curGarden = tg;
            curViolet = tv;
            curSizo = ts;
        } else {
            float dt = Mth.clamp((now - lastNs) * 1.0e-9F, 0.0F, 0.25F);
            if (dt > 1.0e-4F) {
                float k = 1.0F - (float) Math.exp(-dt * 2.2F);
                curGarden = Mth.lerp(k, curGarden, tg);
                curViolet = Mth.lerp(k, curViolet, tv);
                curSizo = Mth.lerp(k, curSizo, ts);
            }
        }
        lastCam = cam;
        lastNs = now;
        return new float[]{curGarden, curViolet, curSizo};
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
    public static final class Render {
        @SubscribeEvent
        public static void sky(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
            Vec3 cam = e.getCamera().getPosition();
            float[] w = smoothWeights(cam);
            float wg = w[0], wv = w[1], ws = w[2];
            float wSum = wg + wv + ws;
            if (wSum < 0.002F) return;

            float inv = 1.0F / wSum;
            float ng = wg * inv, nv = wv * inv, ns = ws * inv;
            float zr = G_ZEN_R * ng + V_ZEN_R * nv + S_ZEN_R * ns;
            float zg = G_ZEN_G * ng + V_ZEN_G * nv + S_ZEN_G * ns;
            float zb = G_ZEN_B * ng + V_ZEN_B * nv + S_ZEN_B * ns;
            float hr = G_HOR_R * ng + V_HOR_R * nv + S_HOR_R * ns;
            float hg = G_HOR_G * ng + V_HOR_G * nv + S_HOR_G * ns;
            float hb = G_HOR_B * ng + V_HOR_B * nv + S_HOR_B * ns;
            float alpha = Mth.clamp(wSum, 0.0F, 1.0F);

            Matrix4f m = e.getPoseStack().last().pose();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            final float s = Math.max(96F, mc.options.getEffectiveRenderDistance() * 16F * 0.85F);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float[][] corners = {{-s, -s}, {s, -s}, {s, s}, {-s, s}};
            for (int i = 0; i < 4; i++) {
                float[] p0 = corners[i], p1 = corners[(i + 1) % 4];
                b.vertex(m, p0[0], -s * 0.35F, p0[1]).color(hr, hg, hb, alpha).endVertex();
                b.vertex(m, p1[0], -s * 0.35F, p1[1]).color(hr, hg, hb, alpha).endVertex();
                b.vertex(m, p1[0], s, p1[1]).color(zr, zg, zb, alpha).endVertex();
                b.vertex(m, p0[0], s, p0[1]).color(zr, zg, zb, alpha).endVertex();
                // нижняя полусфера купола
                b.vertex(m, p0[0], -s, p0[1]).color(hr, hg, hb, alpha).endVertex();
                b.vertex(m, p1[0], -s, p1[1]).color(hr, hg, hb, alpha).endVertex();
                b.vertex(m, p1[0], -s * 0.35F, p1[1]).color(hr, hg, hb, alpha).endVertex();
                b.vertex(m, p0[0], -s * 0.35F, p0[1]).color(hr, hg, hb, alpha).endVertex();
            }
            b.vertex(m, -s, s, -s).color(zr, zg, zb, alpha).endVertex();
            b.vertex(m, s, s, -s).color(zr, zg, zb, alpha).endVertex();
            b.vertex(m, s, s, s).color(zr, zg, zb, alpha).endVertex();
            b.vertex(m, -s, s, s).color(zr, zg, zb, alpha).endVertex();

            // Крупное тёплое низкое солнце над горизонтом Фиолетовых холмов с плавным проявлением
            if (wv > 0.005F) {
                VioletHills.Region vr = VioletHills.regionAt(cam.x, cam.z);
                if (vr != null) {
                    double targetAng = Math.atan2(vr.rbz - vr.raz, vr.rbx - vr.rax);
                    if (!sunInit) {
                        sunAng = targetAng;
                        sunInit = true;
                    } else {
                        double diff = Math.atan2(Math.sin(targetAng - sunAng), Math.cos(targetAng - sunAng));
                        sunAng += diff * 0.08;
                    }
                }
                float dist = s * 0.80F;
                float sx = (float) (Math.cos(sunAng) * dist);
                float sz = (float) (Math.sin(sunAng) * dist);
                float tx = (float) (-Math.sin(sunAng));
                float tz = (float) (Math.cos(sunAng));
                float sy = s * 0.19F;
                float haloA = 0.42F * wv;
                float diskA = 0.96F * wv;
                float hr2 = s * 0.22F;
                b.vertex(m, sx - tx * hr2, sy - hr2, sz - tz * hr2).color(1.0F, 0.91F, 0.80F, haloA).endVertex();
                b.vertex(m, sx + tx * hr2, sy - hr2, sz + tz * hr2).color(1.0F, 0.91F, 0.80F, haloA).endVertex();
                b.vertex(m, sx + tx * hr2, sy + hr2, sz + tz * hr2).color(1.0F, 0.91F, 0.80F, haloA).endVertex();
                b.vertex(m, sx - tx * hr2, sy + hr2, sz - tz * hr2).color(1.0F, 0.91F, 0.80F, haloA).endVertex();
                float sr = s * 0.14F;
                b.vertex(m, sx - tx * sr, sy - sr, sz - tz * sr).color(1.0F, 0.97F, 0.89F, diskA).endVertex();
                b.vertex(m, sx + tx * sr, sy - sr, sz + tz * sr).color(1.0F, 0.97F, 0.89F, diskA).endVertex();
                b.vertex(m, sx + tx * sr, sy + sr, sz + tz * sr).color(1.0F, 0.97F, 0.89F, diskA).endVertex();
                b.vertex(m, sx - tx * sr, sy + sr, sz - tz * sr).color(1.0F, 0.97F, 0.89F, diskA).endVertex();
            }

            Tesselator.getInstance().end();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
