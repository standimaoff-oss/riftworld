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
import ru.aiforge.riftworld.client.model.LiopleurodonBlockbenchModel;
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
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(LiopleurodonBlockbenchModel.LAYER_LOCATION, LiopleurodonBlockbenchModel::createBodyLayer);
        // v0.53.0: ванильная геометрия щита Элисара (plate + handle).
        e.registerLayerDefinition(ru.aiforge.riftworld.client.model.ShieldItemModel.LAYER_LOCATION,
                ru.aiforge.riftworld.client.model.ShieldItemModel::createBodyLayer);
        ru.aiforge.riftworld.client.model.ElisarFolkModels.registerLayers(e);
        ElisarFaunaRenderers.registerLayers(e);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.MODEL.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.CRATE.get(), ModelBER::new);
        e.registerBlockEntityRenderer(ModBlockEntities.LAUNCH_CORE.get(), LaunchCoreRenderer::new);
        e.registerBlockEntityRenderer(ru.aiforge.riftworld.registry.ArenaBlocks.COLUMN_BE.get(), BossColumnRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.BRIDGE_POST.get(), BridgePostRenderer::new);
        e.registerEntityRenderer(ModEntities.SPATIAL_RIFT.get(), SpatialRiftRenderer::new);
        e.registerEntityRenderer(ModEntities.PAPER_DRAGON.get(), PaperDragonRenderer::new);
        e.registerEntityRenderer(ModEntities.SEA_FISH.get(), SeaFishRenderer::new);
        e.registerEntityRenderer(ModEntities.LIOPLEURODON.get(), LiopleurodonRenderer::new);
        e.registerEntityRenderer(ModEntities.EL_GRAN_MAJA.get(), ElGranMajaRenderer::new);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.BOSS.get(), RiftMobRenderer::boss);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.GUARD.get(), RiftMobRenderer::guard);
        e.registerEntityRenderer(ru.aiforge.riftworld.registry.ModEntities.SCYTHE_ITEM.get(), ScytheDropRenderer::new);
        e.registerEntityRenderer(ModEntities.RIFT_OBELISK_BOSS.get(), CrystalGeoRenderers.Boss::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_MINION.get(), CrystalGeoRenderers.Minion::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_SHIELD.get(), CrystalGeoRenderers.Shield::new);
        e.registerEntityRenderer(ModEntities.CRYSTAL_SHARD.get(), CrystalGeoRenderers.Shard::new);
        e.registerEntityRenderer(ModEntities.ICE_PILLAR.get(), CrystalGeoRenderers.IcePillar::new);
        e.registerEntityRenderer(ModEntities.ICE_HAIL_BOULDER.get(), CrystalGeoRenderers.IceHailBoulder::new);
        e.registerEntityRenderer(ModEntities.SKY_DART_FISH.get(), PeacefulFaunaRenderers.SkyDartFish::new);
        e.registerEntityRenderer(ModEntities.BLOSSOM_STAG.get(), PeacefulFaunaRenderers.BlossomStag::new);
        // v0.39.0: Морок — геометрия morok + СВЕТЯЩИЕСЯ глаза (geo/morok_eyes.json, RenderType.eyes)
        e.registerEntityRenderer(ModEntities.MOROK.get(), MorokRenderer::new);
        e.registerEntityRenderer(ModEntities.HOLLOWWATCHER.get(), HollowwatcherRenderer::new);
        // v0.48.0: жители Элисара — житель, страж и лучник (референсы 05–07, собственные 128x128 модели и вооружение).
        e.registerEntityRenderer(ModEntities.ELISAR_VILLAGER.get(), ElisarFolkRenderers.Villager::new);
        e.registerEntityRenderer(ModEntities.ELISAR_GUARD.get(), ElisarFolkRenderers.Guard::new);
        e.registerEntityRenderer(ModEntities.ELISAR_ARCHER.get(), ElisarFolkRenderers.Archer::new);
        // v0.47.0: 15 существ биомной фауны Элисара (Blockbench MCP + многокостные анимации)
        ElisarFaunaRenderers.registerRenderers(e);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
        e.enqueueWork(ComboClient::init);
        e.enqueueWork(() -> {
            // v0.48.0: ItemProperties для натяжения рифт-лука и состояния блокирования щитов
            var bow = ru.aiforge.riftworld.registry.ModItems.ELISAR_BOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(bow,
                    new net.minecraft.resources.ResourceLocation("pull"),
                    (stack, lvl, ent, seed) -> {
                        if (ent == null) return 0.0F;
                        return ent.getUseItem() != stack ? 0.0F
                                : (float) (stack.getUseDuration() - ent.getUseItemRemainingTicks()) / 20.0F;
                    });
            net.minecraft.client.renderer.item.ItemProperties.register(bow,
                    new net.minecraft.resources.ResourceLocation("pulling"),
                    (stack, lvl, ent, seed) -> ent != null && ent.isUsingItem() && ent.getUseItem() == stack ? 1.0F : 0.0F);
            for (var shieldRo : java.util.List.of(
                    ru.aiforge.riftworld.registry.ModItems.ELISAR_SHIELD,
                    ru.aiforge.riftworld.registry.ModItems.ELISAR_GUARD_SHIELD)) {
                net.minecraft.client.renderer.item.ItemProperties.register(shieldRo.get(),
                        new net.minecraft.resources.ResourceLocation("blocking"),
                        (stack, lvl, ent, seed) -> ent != null && ent.isUsingItem() && ent.getUseItem() == stack ? 1.0F : 0.0F);
            }
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
            var plantCutout = net.minecraft.client.renderer.RenderType.cutout();
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.WHITE_LILY_CLUSTER.get(), plantCutout);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_FERN.get(), plantCutout);
            for (var ro : java.util.List.of(
                    ru.aiforge.riftworld.registry.ElisarBlocks.SIZO_DOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.SIZO_TRAPDOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.AMBRA_DOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.AMBRA_TRAPDOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_DOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_TRAPDOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.LILAC_DOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.LILAC_TRAPDOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.CRYSTAL_DOOR,
                    ru.aiforge.riftworld.registry.ElisarBlocks.CRYSTAL_TRAPDOOR,
                    ru.aiforge.riftworld.registry.CastleBlocks.DARK_DOOR,
                    ru.aiforge.riftworld.registry.CastleBlocks.DARK_TRAPDOOR,
                    ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_DOOR,
                    ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_TRAPDOOR)) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ro.get(), plantCutout);
            }
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
    public static void modifyBakedModels(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult e) {
        var models = e.getModels();
        wrapConnectedCarpet(models,
                ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get(),
                new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, "block/castle_carpet_center"),
                new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, "block/castle_carpet_trim"));
        wrapConnectedCarpet(models,
                ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.RUG).get(),
                new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, "block/rug_center"),
                new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, "block/rug_trim"));
    }

    private static void wrapConnectedCarpet(
            java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> models,
            net.minecraft.world.level.block.Block block,
            net.minecraft.resources.ResourceLocation centerLoc,
            net.minecraft.resources.ResourceLocation trimLoc) {
        for (var state : block.getStateDefinition().getPossibleStates()) {
            var mrl = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
            var orig = models.get(mrl);
            if (orig != null && !(orig instanceof ConnectedCarpetBakedModel)) {
                models.put(mrl, new ConnectedCarpetBakedModel(orig, centerLoc, trimLoc));
            }
        }
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((PreparableReloadListener) (ResourceManagerReloadListener) rm -> {
            GlbAssets.clear();
        });
    }
}
