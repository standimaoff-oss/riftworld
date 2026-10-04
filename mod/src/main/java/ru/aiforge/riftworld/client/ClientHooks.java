package ru.aiforge.riftworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.registry.ModBlockEntities;

@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientHooks {
    private static ModelItemRenderer itemRenderer;

    private ClientHooks() {
    }

    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer == null) {
            Minecraft mc = Minecraft.getInstance();
            itemRenderer = new ModelItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return itemRenderer;
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.MODEL.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.CRATE.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.LAUNCH_CORE.get(), LaunchCoreRenderer::new);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.SOURCE.get(), net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.FLOWING.get(), net.minecraft.client.renderer.RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((PreparableReloadListener) (ResourceManagerReloadListener) rm -> GlbAssets.clear());
    }
}
