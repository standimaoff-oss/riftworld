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
import ru.aiforge.riftworld.world.CrystalRifts;
import ru.aiforge.riftworld.world.SizoGrove;
import ru.aiforge.riftworld.world.VioletHills;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * Атмосферные купола биомов Элисара с плавным переходом и полной совместимостью со ВСЕМИ шейдерпаками:
 * <ul>
 *   <li>{@link #smoothWeights(Vec3)} питает плавные цвета тумана в {@link ElisarSky.Fog} и в ванили, и в шейдерпаках;</li>
 *   <li>При активном шейдерпаке ({@link ShaderCompat#isShaderPackActive()}) мы НЕ перекрываем небесный G-буфер
 *       коробкой в {@code AFTER_SKY} (именно это рвало небо на белые куски в IterationT и других шейдерах!),
 *       а в ванили рисуем гладкий сферический купол через {@code POSITION_TEX_COLOR} внутри {@code depthFar}.</li>
 * </ul>
 */
public final class GardenSky {
    private static final float G_ZEN_R = 0.31F, G_ZEN_G = 0.55F, G_ZEN_B = 0.86F;
    private static final float G_HOR_R = 0.80F, G_HOR_G = 0.90F, G_HOR_B = 0.97F;

    private static final float V_ZEN_R = 0.78F, V_ZEN_G = 0.72F, V_ZEN_B = 0.88F;
    private static final float V_HOR_R = 0.96F, V_HOR_G = 0.82F, V_HOR_B = 0.76F;

    private static final float S_ZEN_R = 0.77F, S_ZEN_G = 0.85F, S_ZEN_B = 0.90F;
    private static final float S_HOR_R = 0.95F, S_HOR_G = 0.93F, S_HOR_B = 0.88F;

    private static final float C_ZEN_R = 0.62F, C_ZEN_G = 0.85F, C_ZEN_B = 0.95F;
    private static final float C_HOR_R = 0.88F, C_HOR_G = 0.97F, C_HOR_B = 0.99F;

    private static Vec3 lastCam;
    private static long lastNs;
    private static float curGarden, curViolet, curSizo, curCrystal;
    private static double sunAng = 0.7;
    private static boolean sunInit;

    private GardenSky() {
    }

    public static float[] smoothWeights(Vec3 cam) {
        float tg = 0.0F, tv = 0.0F, ts = 0.0F, tc = 0.0F;
        try {
            tg = WaterfallGardens.skyWeight(cam.x, cam.z);
            tv = VioletHills.skyWeight(cam.x, cam.z);
            ts = SizoGrove.skyWeight(cam.x, cam.z);
            tc = CrystalRifts.skyWeight(cam.x, cam.z);
        } catch (Throwable ignored) {
        }
        long now = System.nanoTime();
        boolean teleported = lastCam == null || cam.distanceToSqr(lastCam) > 64.0;
        if (teleported) {
            curGarden = tg;
            curViolet = tv;
            curSizo = ts;
            curCrystal = tc;
        } else {
            float dt = Mth.clamp((now - lastNs) * 1.0e-9F, 0.0F, 0.25F);
            if (dt > 1.0e-4F) {
                float k = 1.0F - (float) Math.exp(-dt * 2.2F);
                curGarden = Mth.lerp(k, curGarden, tg);
                curViolet = Mth.lerp(k, curViolet, tv);
                curSizo = Mth.lerp(k, curSizo, ts);
                curCrystal = Mth.lerp(k, curCrystal, tc);
            }
        }
        lastCam = cam;
        lastNs = now;
        return new float[]{curGarden, curViolet, curSizo, curCrystal};
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
    public static final class Render {
        @SubscribeEvent
        public static void sky(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
            if (ShaderCompat.isRenderingShadowPass() || ShaderCompat.isShaderPackActive()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
            Vec3 cam = e.getCamera().getPosition();
            float[] w = smoothWeights(cam);
            float wg = w[0], wv = w[1], ws = w[2], wc = w[3];
            float wSum = wg + wv + ws + wc;
            if (wSum < 0.002F) return;

            float inv = 1.0F / wSum;
            float ng = wg * inv, nv = wv * inv, ns = ws * inv, nc = wc * inv;
            int zr = Mth.clamp(Math.round((G_ZEN_R * ng + V_ZEN_R * nv + S_ZEN_R * ns + C_ZEN_R * nc) * 255.0F), 0, 255);
            int zg = Mth.clamp(Math.round((G_ZEN_G * ng + V_ZEN_G * nv + S_ZEN_G * ns + C_ZEN_G * nc) * 255.0F), 0, 255);
            int zb = Mth.clamp(Math.round((G_ZEN_B * ng + V_ZEN_B * nv + S_ZEN_B * ns + C_ZEN_B * nc) * 255.0F), 0, 255);
            int hr = Mth.clamp(Math.round((G_HOR_R * ng + V_HOR_R * nv + S_HOR_R * ns + C_HOR_R * nc) * 255.0F), 0, 255);
            int hg = Mth.clamp(Math.round((G_HOR_G * ng + V_HOR_G * nv + S_HOR_G * ns + C_HOR_G * nc) * 255.0F), 0, 255);
            int hb = Mth.clamp(Math.round((G_HOR_B * ng + V_HOR_B * nv + S_HOR_B * ns + C_HOR_B * nc) * 255.0F), 0, 255);
            int alpha = Mth.clamp(Math.round(Mth.clamp(wSum, 0.0F, 1.0F) * 255.0F), 0, 255);

            float radius = Math.min(mc.gameRenderer.getDepthFar() * 0.52F, 180.0F);
            Matrix4f m = e.getPoseStack().last().pose();
            ShaderCompat.drawSphericalSkyDome(m, radius,
                    zr, zg, zb, alpha,
                    hr, hg, hb, alpha,
                    hr, hg, hb, alpha);

            // Тёплый низкий солнечный диск Фиолетовых холмов (внутри радиуса сферы, без выхода за depthFar)
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
                float dist = radius * 0.90F;
                float sx = (float) (Math.cos(sunAng) * dist);
                float sz = (float) (Math.sin(sunAng) * dist);
                float tx = (float) (-Math.sin(sunAng));
                float tz = (float) (Math.cos(sunAng));
                float sy = radius * 0.21F;
                int haloA = Mth.clamp(Math.round(0.42F * wv * 255.0F), 0, 255);
                int diskA = Mth.clamp(Math.round(0.96F * wv * 255.0F), 0, 255);
                float hr2 = radius * 0.24F;
                float sr = radius * 0.15F;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);

                BufferBuilder b = Tesselator.getInstance().getBuilder();
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                b.vertex(m, sx - tx * hr2, sy - hr2, sz - tz * hr2).uv(0.5F, 0.5F).color(255, 232, 204, haloA).endVertex();
                b.vertex(m, sx + tx * hr2, sy - hr2, sz + tz * hr2).uv(0.5F, 0.5F).color(255, 232, 204, haloA).endVertex();
                b.vertex(m, sx + tx * hr2, sy + hr2, sz + tz * hr2).uv(0.5F, 0.5F).color(255, 232, 204, haloA).endVertex();
                b.vertex(m, sx - tx * hr2, sy + hr2, sz - tz * hr2).uv(0.5F, 0.5F).color(255, 232, 204, haloA).endVertex();

                b.vertex(m, sx - tx * sr, sy - sr, sz - tz * sr).uv(0.5F, 0.5F).color(255, 247, 227, diskA).endVertex();
                b.vertex(m, sx + tx * sr, sy - sr, sz + tz * sr).uv(0.5F, 0.5F).color(255, 247, 227, diskA).endVertex();
                b.vertex(m, sx + tx * sr, sy + sr, sz + tz * sr).uv(0.5F, 0.5F).color(255, 247, 227, diskA).endVertex();
                b.vertex(m, sx - tx * sr, sy + sr, sz - tz * sr).uv(0.5F, 0.5F).color(255, 247, 227, diskA).endVertex();
                Tesselator.getInstance().end();

                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
        }
    }
}
