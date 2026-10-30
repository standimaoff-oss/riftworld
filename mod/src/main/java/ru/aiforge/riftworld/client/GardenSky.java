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
import ru.aiforge.riftworld.world.SizoGrove;
import ru.aiforge.riftworld.world.VioletHills;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * Атмосферные купола биомов Элисара по референсам:
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

    private GardenSky() {
    }

    private static long cacheKey = Long.MIN_VALUE;
    private static int cacheMode; // 0 = none, 1 = gardens, 2 = violet, 3 = sizo

    private static int skyMode(Vec3 cam) {
        long key = ((long) (int) Math.floor(cam.x / 16) << 20) ^ (long) (int) Math.floor(cam.z / 16);
        if (key == cacheKey) return cacheMode;
        int mode = 0;
        try {
            if (WaterfallGardens.skyInside(cam.x, cam.z)) mode = 1;
            else if (VioletHills.regionAt(cam.x, cam.z) != null) mode = 2;
            else if (SizoGrove.regionAt(cam.x, cam.z) != null) mode = 3;
        } catch (Throwable ignored) {
        }
        cacheKey = key;
        cacheMode = mode;
        return mode;
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
    public static final class Render {
        @SubscribeEvent
        public static void sky(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
            Vec3 cam = e.getCamera().getPosition();
            int mode = skyMode(cam);
            if (mode == 0) return;

            float zr, zg, zb, hr, hg, hb;
            if (mode == 1) {
                zr = G_ZEN_R; zg = G_ZEN_G; zb = G_ZEN_B;
                hr = G_HOR_R; hg = G_HOR_G; hb = G_HOR_B;
            } else if (mode == 2) {
                zr = V_ZEN_R; zg = V_ZEN_G; zb = V_ZEN_B;
                hr = V_HOR_R; hg = V_HOR_G; hb = V_HOR_B;
            } else {
                zr = S_ZEN_R; zg = S_ZEN_G; zb = S_ZEN_B;
                hr = S_HOR_R; hg = S_HOR_G; hb = S_HOR_B;
            }

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
                b.vertex(m, p0[0], -s * 0.35F, p0[1]).color(hr, hg, hb, 1F).endVertex();
                b.vertex(m, p1[0], -s * 0.35F, p1[1]).color(hr, hg, hb, 1F).endVertex();
                b.vertex(m, p1[0], s, p1[1]).color(zr, zg, zb, 1F).endVertex();
                b.vertex(m, p0[0], s, p0[1]).color(zr, zg, zb, 1F).endVertex();
                // нижняя полусфера купола
                b.vertex(m, p0[0], -s, p0[1]).color(hr, hg, hb, 1F).endVertex();
                b.vertex(m, p1[0], -s, p1[1]).color(hr, hg, hb, 1F).endVertex();
                b.vertex(m, p1[0], -s * 0.35F, p1[1]).color(hr, hg, hb, 1F).endVertex();
                b.vertex(m, p0[0], -s * 0.35F, p0[1]).color(hr, hg, hb, 1F).endVertex();
            }
            b.vertex(m, -s, s, -s).color(zr, zg, zb, 1F).endVertex();
            b.vertex(m, s, s, -s).color(zr, zg, zb, 1F).endVertex();
            b.vertex(m, s, s, s).color(zr, zg, zb, 1F).endVertex();
            b.vertex(m, -s, s, s).color(zr, zg, zb, 1F).endVertex();

            // Крупное тёплое низкое солнце над горизонтом Фиолетовых холмов (как на 04_violet_hills.png)
            if (mode == 2) {
                VioletHills.Region vr = VioletHills.regionAt(cam.x, cam.z);
                double ang = vr != null ? Math.atan2(vr.rbz - vr.raz, vr.rbx - vr.rax) : 0.7;
                float dist = s * 0.80F;
                float sx = (float) (Math.cos(ang) * dist);
                float sz = (float) (Math.sin(ang) * dist);
                float tx = (float) (-Math.sin(ang));
                float tz = (float) (Math.cos(ang));
                float sy = s * 0.19F;
                // мягкий ореол
                float hr2 = s * 0.22F;
                b.vertex(m, sx - tx * hr2, sy - hr2, sz - tz * hr2).color(1.0F, 0.91F, 0.80F, 0.42F).endVertex();
                b.vertex(m, sx + tx * hr2, sy - hr2, sz + tz * hr2).color(1.0F, 0.91F, 0.80F, 0.42F).endVertex();
                b.vertex(m, sx + tx * hr2, sy + hr2, sz + tz * hr2).color(1.0F, 0.91F, 0.80F, 0.42F).endVertex();
                b.vertex(m, sx - tx * hr2, sy + hr2, sz - tz * hr2).color(1.0F, 0.91F, 0.80F, 0.42F).endVertex();
                // яркий тёпло-сливочный диск солнца
                float sr = s * 0.14F;
                b.vertex(m, sx - tx * sr, sy - sr, sz - tz * sr).color(1.0F, 0.97F, 0.89F, 0.96F).endVertex();
                b.vertex(m, sx + tx * sr, sy - sr, sz + tz * sr).color(1.0F, 0.97F, 0.89F, 0.96F).endVertex();
                b.vertex(m, sx + tx * sr, sy + sr, sz + tz * sr).color(1.0F, 0.97F, 0.89F, 0.96F).endVertex();
                b.vertex(m, sx - tx * sr, sy + sr, sz - tz * sr).color(1.0F, 0.97F, 0.89F, 0.96F).endVertex();
            }

            Tesselator.getInstance().end();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
