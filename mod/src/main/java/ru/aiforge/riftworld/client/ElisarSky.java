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
        private static boolean inElisar() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.level != null && mc.level.dimension().location().equals(ID);
        }

        @SubscribeEvent
        public static void fog(ViewportEvent.RenderFog e) {
            if (!inElisar() || e.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
            if (e.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
            float far = e.getFarPlaneDistance();
            e.setNearPlaneDistance(far * 0.45F);
            e.setFarPlaneDistance(far * 1.05F);
            e.setCanceled(true);
        }
    }
}
