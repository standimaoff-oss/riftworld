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
import ru.aiforge.riftworld.registry.ModEntities;

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

    public static net.minecraftforge.client.extensions.common.IClientItemExtensions geoItemExt() {
        return new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return itemRenderer();
            }
        };
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.MODEL.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.CRATE.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.LAUNCH_CORE.get(), LaunchCoreRenderer::new);
        e.registerBlockEntityRenderer(ru.aiforge.riftworld.registry.ArenaBlocks.COLUMN_BE.get(), BossColumnRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.BRIDGE_POST.get(), BridgePostRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.DEMON_GATE.get(), DemonGateRenderer::new);
        e.registerEntityRenderer(ModEntities.PAPER_DRAGON.get(), PaperDragonRenderer::new);
        e.registerEntityRenderer(ModEntities.SEA_FISH.get(), SeaFishRenderer::new);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.BOSS.get(), RiftMobRenderer::boss);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.GUARD.get(), RiftMobRenderer::guard);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.SCYTHE_ITEM.get(), ScytheDropRenderer::new);
        e.registerEntityRenderer(ModEntities.RIFT_OBELISK_BOSS.get(), CrystalGeoRenderers.Boss::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_MINION.get(), CrystalGeoRenderers.Minion::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_SHIELD.get(), CrystalGeoRenderers.Shield::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_SHARD.get(), CrystalGeoRenderers.Shard::new);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
        e.enqueueWork(ComboClient::init);
        e.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.SOURCE.get(), net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.FLOWING.get(), net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.GARDEN_SOURCE.get(), net.minecraft.client.renderer.RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ModFluids.GARDEN_FLOWING.get(), net.minecraft.client.renderer.RenderType.translucent());
            // Гарантируем ванильный cutoutMipped-рендер со сквозными просветами для всей листвы биомов и Dynamic Trees!
            net.minecraft.client.renderer.ItemBlockRenderTypes.setFancy(true);
            var cutout = net.minecraft.client.renderer.RenderType.cutoutMipped();
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_LEAVES.get(), cutout);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_LEAVES.get(), cutout);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.CRYSTAL_LEAVES.get(), cutout);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.SIZO_LEAVES.get(), cutout);
            for (String dtId : new String[]{"lilac_leaves", "garden_oak_leaves", "crystal_birch_leaves", "sizo_leaves"}) {
                var dtBlock = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("riftworlddt", dtId));
                if (dtBlock != null && dtBlock != net.minecraft.world.level.block.Blocks.AIR) {
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(dtBlock, cutout);
                }
            }
            if ("true".equalsIgnoreCase(System.getProperty("riftworld.fhd"))) {
                try {
                    long win = Minecraft.getInstance().getWindow().getWindow();
                    if (win != 0L) {
                        org.lwjgl.glfw.GLFW.glfwSetWindowPos(win, 0, 0);
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(win, 1920, 1080);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    @SubscribeEvent
    public static void particles(net.minecraftforge.client.event.RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ru.aiforge.riftworld.registry.ModParticles.VIOLET_LEAF.get(), FloatingLeafParticle.VioletProvider::new);
        e.registerSpriteSet(ru.aiforge.riftworld.registry.ModParticles.SIZO_LEAF.get(), FloatingLeafParticle.SizoProvider::new);
    }

    /** Biome-aware greys → per-biome grass tint (violet meadows, mint grove). */
    @SubscribeEvent
    public static void blockTints(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block e) {
        e.register((state, level, pos, idx) -> level != null && pos != null
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos)
                        : net.minecraft.world.level.GrassColor.get(0.5, 1.0),
                // листва и лесные васильково-фиолетовые цветы НЕ тонируются травой
                ru.aiforge.riftworld.registry.ElisarBlocks.GRASS.get(),
                ru.aiforge.riftworld.registry.ElisarBlocks.TALL_GRASS.get());
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((PreparableReloadListener) (ResourceManagerReloadListener) rm -> GlbAssets.clear());
    }
}
