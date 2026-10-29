package ru.aiforge.riftworld.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/**
 * Elisar atmosphere: bleached white-cream haze, no clouds (the islands float inside the cloud layer).
 * The sky colour comes from the biome; here the fog is pulled towards cream and brought closer so distant islands fade to white.
 */
public final class ElisarSky extends DimensionSpecialEffects {
    public static final ResourceLocation ID = new ResourceLocation(RiftWorld.MODID, "elisar");
    private static final Vec3 CREAM = new Vec3(0.93, 0.915, 0.875);

    public ElisarSky() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        float k = brightness * 0.94F + 0.06F;
        return new Vec3(Mth.lerp(0.45, fog.x, CREAM.x) * k, Mth.lerp(0.45, fog.y, CREAM.y) * k, Mth.lerp(0.45, fog.z, CREAM.z) * k);
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
        private static long gardenKey = Long.MIN_VALUE;
        private static boolean gardenHit;

        /** Кешированная проверка «мы над Висячими садами» (регион считается из сида). */
        private static boolean gardenAt(Vec3 p) {
            long key = ((long) (int) Math.floor(p.x / 16) << 20) ^ (long) (int) Math.floor(p.z / 16);
            if (key == gardenKey) return gardenHit;
            boolean hit = false;
            try {
                hit = ru.aiforge.riftworld.world.WaterfallGardens.inside(p.x, p.z);
            } catch (Throwable ignored) {
            }
            gardenKey = key;
            gardenHit = hit;
            return hit;
        }

        private static long violetKey = Long.MIN_VALUE;
        private static boolean violetHit;

        /** Кешированная проверка «мы над Фиолетовыми холмами» (рев 14.29: тёплая розовая дымка). */
        private static boolean violetAt(Vec3 p) {
            long key = ((long) (int) Math.floor(p.x / 16) << 20) ^ (long) (int) Math.floor(p.z / 16);
            if (key == violetKey) return violetHit;
            boolean hit = false;
            try {
                hit = ru.aiforge.riftworld.world.VioletHills.inside(p.x, p.z);
            } catch (Throwable ignored) {
            }
            violetKey = key;
            violetHit = hit;
            return hit;
        }

        private static boolean inElisar() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.level != null && mc.level.dimension().location().equals(ID);
        }

        @SubscribeEvent
        public static void fog(ViewportEvent.RenderFog e) {
            if (!inElisar() || e.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
            if (e.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
            float far = e.getFarPlaneDistance();
            Vec3 pos = e.getCamera().getPosition();
            float g = gloom(pos);
            float m = mist(pos);
            float nearF = far * Mth.lerp(g, 0.45F, 0.55F);
            float farF = far * Mth.lerp(g, 1.05F, 1.0F);
            // the sea breathes: close over the water the horizon dissolves in a milky band (Rev 4: light optimised sea mist)
            e.setNearPlaneDistance(nearF * Mth.lerp(m, 1.0F, 0.42F));
            e.setFarPlaneDistance(farF * Mth.lerp(m, 1.0F, 0.62F));
            e.setCanceled(true);
        }

        /** 1 on the rolling crest, fading out ~26 blocks above the sea; 0 underwater (the fluid fog owns that space). */
        public static float mist(Vec3 cam) {
            double sea = ru.aiforge.riftworld.world.CityPlan.get().ocean + 1;
            double dy = cam.y - sea;
            if (dy < -2) return 0.0F;
            return (float) Mth.clamp(1.0 - dy / 26.0, 0.0, 1.0);
        }

        /** 0 in the white Elisar haze, 1 over the castle's scorched island (ash-brown, gloomy air). */
        public static float gloom(Vec3 cam) {
            double d = Math.hypot(cam.x - ru.aiforge.riftworld.world.CastleSite.CX, cam.z - ru.aiforge.riftworld.world.CastleSite.CZ);
            double t = Mth.clamp((170 - d) / 80.0, 0, 1);
            t = Math.max(t, ru.aiforge.riftworld.world.AshenLands.weight(cam.x, cam.z) * 0.85);   // the whole Ashen Lands are gloomy
            return (float) (t * t * (3 - 2 * t));
        }

        private static final Vec3 ASH = new Vec3(0.34, 0.235, 0.20);
        private static final Vec3 SEA_MIST = new Vec3(0.965, 0.945, 0.885);   // warm milk of the sea haze (TZ_atmosphere_fx palette)

        private static final Vec3 GARDEN_HAZE = new Vec3(0.80, 0.89, 0.96);   // светло-голубая дымка садов (референс)

        @SubscribeEvent
        public static void fogColor(ViewportEvent.ComputeFogColor e) {
            if (!inElisar()) return;
            var pos = e.getCamera().getPosition();
            // Рев 14.18: над садами кремовая дымка Элисара гасит голубое небо — кадры выходили
            // жёлто-зелёными. В регионе садов туман светлый и голубой, как на референсе.
            if (gardenAt(pos)) {
                e.setRed((float) Mth.lerp(0.92F, e.getRed(), GARDEN_HAZE.x));
                e.setGreen((float) Mth.lerp(0.92F, e.getGreen(), GARDEN_HAZE.y));
                e.setBlue((float) Mth.lerp(0.92F, e.getBlue(), GARDEN_HAZE.z));
                return;
            }
            // Рев 14.29: у Фиолетовых холмов на референсе тёплый закатный воздух (розово-персиковый):
            // кремовая дымка Элисара гасила лаванду в серое. Красим туман под биом.
            if (violetAt(pos)) {
                e.setRed((float) Mth.lerp(0.85F, e.getRed(), 0.98F));
                e.setGreen((float) Mth.lerp(0.85F, e.getGreen(), 0.80F));
                e.setBlue((float) Mth.lerp(0.85F, e.getBlue(), 0.84F));
                return;
            }
            float g = gloom(e.getCamera().getPosition());
            float m = mist(e.getCamera().getPosition()) * 0.62F;
            if (m > 0) {
                e.setRed((float) Mth.lerp(m, e.getRed(), SEA_MIST.x));
                e.setGreen((float) Mth.lerp(m, e.getGreen(), SEA_MIST.y));
                e.setBlue((float) Mth.lerp(m, e.getBlue(), SEA_MIST.z));
            }
            if (g <= 0) return;
            e.setRed((float) Mth.lerp(g, e.getRed(), ASH.x));
            e.setGreen((float) Mth.lerp(g, e.getGreen(), ASH.y));
            e.setBlue((float) Mth.lerp(g, e.getBlue(), ASH.z));
        }
    }
}
