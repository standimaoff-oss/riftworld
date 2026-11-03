package ru.aiforge.riftworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.world.AshenLands;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.MorokHollow;

/**
 * Небо и туман Элисара с универсальной совместимостью со ВСЕМИ шейдерпаками
 * (Oculus / Iris / OptiFine: IterationT, Complementary, Bliss, Photon, BSL, SEUS и др.):
 * <ul>
 *   <li>{@code hasGround = false} убирает тёмный нижний полукупол {@code darkBuffer} под парящими островами,
 *       который в шейдерпаках давал резкий разлом неба у горизонта;</li>
 *   <li>Стандартный небесный конвейер {@code SkyType.NORMAL} остаётся штатным (не перехватывает {@code renderSky}),
 *       поэтому {@code gbuffers_skybasic} в IterationT и других deferred-шейдерах всегда корректно заполняет
 *       G-буфер неба без «белых кусков»!</li>
 *   <li>Плавное пространственно-временное сглаживание цветов тумана без ступенек между биомами.</li>
 * </ul>
 */
public final class ElisarSky extends DimensionSpecialEffects {
    public static final ResourceLocation ID = new ResourceLocation(RiftWorld.MODID, "elisar");
    private static final Vec3 CREAM = new Vec3(0.93, 0.915, 0.875);

    public ElisarSky() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        float k = brightness * 0.94F + 0.06F;
        return new Vec3(
                Mth.lerp(0.30, fog.x, CREAM.x) * k,
                Mth.lerp(0.30, fog.y, CREAM.y) * k,
                Mth.lerp(0.30, fog.z, CREAM.z) * k);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Reg {
        @SubscribeEvent
        public static void effects(RegisterDimensionSpecialEffectsEvent e) {
            e.register(ID, new ElisarSky());
            e.register(FlightFx.TRANSIT_FX, new FlightFx.TransitEffects());
        }
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
    public static final class Fog {
        private static final Vec3 ASH = new Vec3(0.34, 0.235, 0.20);
        private static final Vec3 SEA_MIST = new Vec3(0.965, 0.945, 0.885);
        private static final Vec3 GARDEN_HAZE = new Vec3(0.80, 0.89, 0.96);
        /** v0.37.0: мгла «Мороковой Пади» — холодный костяной серо-зелёный ({@code #C9D6D8}). */
        private static final Vec3 MOROK_MIST = new Vec3(0.788, 0.839, 0.847);
        private static final Vec3 SIZO_MIST = new Vec3(0.95, 0.94, 0.90);
        private static final Vec3 CRYSTAL_HAZE = new Vec3(0.84, 0.95, 0.97);

        private static Vec3 lastFogCam;
        private static long lastFogNs;
        private static float smoothR = -1.0F, smoothG = -1.0F, smoothB = -1.0F;
        private static float curMorok = 0.0F;

        /**
         * v0.37.0: вес мглы Пади со сглаживанием по времени и по камере — на кромке биома
         * туман подъезжает плавно, без «щелчка» при переходе чанка.
         */
        public static float morokWeight(Vec3 cam) {
            float w = 0.0F;
            try {
                w = (float) MorokHollow.weight(cam.x, cam.z);
            } catch (Throwable ignored) {
            }
            curMorok = curMorok < 0 ? w : curMorok + (w - curMorok) * 0.08F;
            return Mth.clamp(curMorok, 0.0F, 1.0F);
        }

        private static boolean inElisar() {
            Minecraft mc = Minecraft.getInstance();
            return mc.level != null && mc.level.dimension().location().equals(ID);
        }

        @SubscribeEvent
        public static void fog(ViewportEvent.RenderFog e) {
            if (!inElisar() || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
            if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
            if (ShaderCompat.isShaderPackActive()) {
                // При активном шейдерпаке не сжимаем ванильный линейный туман у горизонта,
                // чтобы объёмная атмосфера шейдера (IterationT / Bliss / Complementary) работала без швов.
                return;
            }
            float far = e.getFarPlaneDistance();
            Vec3 pos = e.getCamera().getPosition();
            float[] w = GardenSky.smoothWeights(pos);
            float wg = w[0], wv = w[1], ws = w[2], wc = w[3];
            float g = gloom(pos);
            float m = mist(pos) * (1.0F - Mth.clamp(wg + wv + ws + wc, 0.0F, 1.0F));

            float nearDist = far * Mth.lerp(g, 0.45F, 0.55F) * Mth.lerp(m, 1.0F, 0.42F);
            float farDist = far * Mth.lerp(g, 1.05F, 1.00F) * Mth.lerp(m, 1.0F, 0.62F);

            if (wg > 0.001F) {
                nearDist = Mth.lerp(wg, nearDist, far * 0.60F);
                farDist = Mth.lerp(wg, farDist, far * 1.08F);
            }
            if (wv > 0.001F) {
                nearDist = Mth.lerp(wv, nearDist, far * 0.72F);
                farDist = Mth.lerp(wv, farDist, far * 1.12F);
            }
            if (ws > 0.001F) {
                nearDist = Mth.lerp(ws, nearDist, Math.min(far * 0.38F, 46.0F));
                farDist = Mth.lerp(ws, farDist, Math.min(far * 0.96F, 136.0F));
            }
            if (wc > 0.001F) {
                nearDist = Mth.lerp(wc, nearDist, far * 0.68F);
                farDist = Mth.lerp(wc, farDist, far * 1.14F);
            }
            // v0.37.0: «Мороковая Падь» — мгла сжимает обзор до 6–12 блоков, как в концепте
            float wm = morokWeight(pos);
            if (wm > 0.001F) {
                nearDist = Mth.lerp(wm, nearDist, 6.5F);
                farDist = Mth.lerp(wm, farDist, Math.min(far, 54.0F));
            }
            e.setNearPlaneDistance(nearDist);
            e.setFarPlaneDistance(farDist);
            e.setCanceled(true);
        }

        public static float mist(Vec3 cam) {
            double sea = CityPlan.get().ocean + 1;
            double dy = cam.y - sea;
            if (dy < -2) return 0.0F;
            return (float) Mth.clamp(1.0 - dy / 26.0, 0.0, 1.0);
        }

        public static float gloom(Vec3 cam) {
            AshenLands.Region rg = AshenLands.regionAt(cam.x, cam.z);
            double d = rg != null ? Math.hypot(cam.x - rg.cx, cam.z - rg.cz) : 1e9;
            double t = Mth.clamp((170 - d) / 80.0, 0, 1);
            t = Math.max(t, AshenLands.weight(cam.x, cam.z) * 0.85);
            return (float) (t * t * (3 - 2 * t));
        }

        @SubscribeEvent
        public static void fogColor(ViewportEvent.ComputeFogColor e) {
            if (!inElisar()) return;
            Vec3 pos = e.getCamera().getPosition();
            float[] w = GardenSky.smoothWeights(pos);
            float wg = w[0], wv = w[1], ws = w[2], wc = w[3];
            float customCover = Mth.clamp(wg + wv + ws + wc, 0.0F, 1.0F);

            float r = e.getRed();
            float gr = e.getGreen();
            float bl = e.getBlue();

            float m = mist(pos) * 0.62F * (1.0F - customCover);
            if (m > 0.001F) {
                r = (float) Mth.lerp(m, r, SEA_MIST.x);
                gr = (float) Mth.lerp(m, gr, SEA_MIST.y);
                bl = (float) Mth.lerp(m, bl, SEA_MIST.z);
            }
            float wm = morokWeight(pos);
            if (wm > 0.001F) {
                float km = 0.94F * wm;
                r = (float) Mth.lerp(km, r, MOROK_MIST.x);
                gr = (float) Mth.lerp(km, gr, MOROK_MIST.y);
                bl = (float) Mth.lerp(km, bl, MOROK_MIST.z);
            }
            if (wg > 0.001F) {
                float kg = 0.92F * wg;
                r = (float) Mth.lerp(kg, r, GARDEN_HAZE.x);
                gr = (float) Mth.lerp(kg, gr, GARDEN_HAZE.y);
                bl = (float) Mth.lerp(kg, bl, GARDEN_HAZE.z);
            }
            if (wv > 0.001F) {
                r = Mth.lerp(0.28F * wv, r, 0.99F);
                gr = Mth.lerp(0.24F * wv, gr, 0.86F);
                bl = Mth.lerp(0.20F * wv, bl, 0.90F);
            }
            if (ws > 0.001F) {
                float ks = 0.24F * ws;
                r = (float) Mth.lerp(ks, r, SIZO_MIST.x);
                gr = (float) Mth.lerp(ks, gr, SIZO_MIST.y);
                bl = (float) Mth.lerp(ks, bl, SIZO_MIST.z);
            }
            if (wc > 0.001F) {
                float kc = 0.64F * wc;
                r = (float) Mth.lerp(kc, r, CRYSTAL_HAZE.x);
                gr = (float) Mth.lerp(kc, gr, CRYSTAL_HAZE.y);
                bl = (float) Mth.lerp(kc, bl, CRYSTAL_HAZE.z);
            }
            float g = gloom(pos);
            if (g > 0.001F) {
                r = (float) Mth.lerp(g, r, ASH.x);
                gr = (float) Mth.lerp(g, gr, ASH.y);
                bl = (float) Mth.lerp(g, bl, ASH.z);
            }

            long now = System.nanoTime();
            boolean teleported = lastFogCam == null || smoothR < 0.0F || pos.distanceToSqr(lastFogCam) > 64.0;
            if (teleported) {
                smoothR = r;
                smoothG = gr;
                smoothB = bl;
            } else {
                float dt = Mth.clamp((now - lastFogNs) * 1.0e-9F, 0.0F, 0.25F);
                if (dt > 1.0e-4F) {
                    float kf = 1.0F - (float) Math.exp(-dt * 2.5F);
                    smoothR = Mth.lerp(kf, smoothR, r);
                    smoothG = Mth.lerp(kf, smoothG, gr);
                    smoothB = Mth.lerp(kf, smoothB, bl);
                }
            }
            lastFogCam = pos;
            lastFogNs = now;

            e.setRed(smoothR);
            e.setGreen(smoothG);
            e.setBlue(smoothB);
        }
    }
}
