package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.client.event.EntityRenderersEvent;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.model.fauna.AmethystFoxModel;
import ru.aiforge.riftworld.client.model.fauna.AshMothModel;
import ru.aiforge.riftworld.client.model.fauna.AzureFrogModel;
import ru.aiforge.riftworld.client.model.fauna.BasaltBeetleModel;
import ru.aiforge.riftworld.client.model.fauna.BlossomSnailModel;
import ru.aiforge.riftworld.client.model.fauna.CoralHermitCrabModel;
import ru.aiforge.riftworld.client.model.fauna.CrystalGeckoModel;
import ru.aiforge.riftworld.client.model.fauna.FacetedOwlModel;
import ru.aiforge.riftworld.client.model.fauna.FaunaBaseModel;
import ru.aiforge.riftworld.client.model.fauna.BoneRavenModel;
import ru.aiforge.riftworld.client.model.fauna.GhostSquidModel;
import ru.aiforge.riftworld.client.model.fauna.PhantomWolfModel;
import ru.aiforge.riftworld.client.model.fauna.PorcelainCraneModel;
import ru.aiforge.riftworld.client.model.fauna.ReefSeahorseModel;
import ru.aiforge.riftworld.client.model.fauna.SilverSeaSerpentModel;
import ru.aiforge.riftworld.client.model.fauna.SizoLeafwingModel;
import ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity;
import ru.aiforge.riftworld.entity.fauna.AshMothEntity;
import ru.aiforge.riftworld.entity.fauna.AzureFrogEntity;
import ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity;
import ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity;
import ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity;
import ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity;
import ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity;
import ru.aiforge.riftworld.entity.fauna.GhostSquidEntity;
import ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity;
import ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity;
import ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity;
import ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity;
import ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * Client renderers & layer registration for all 15 Riftworld v0.47.0 Elisar fauna creatures.
 */
public final class ElisarFaunaRenderers {
    private ElisarFaunaRenderers() {
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(PorcelainCraneModel.LAYER_LOCATION, PorcelainCraneModel::createBodyLayer);
        e.registerLayerDefinition(BoneRavenModel.LAYER_LOCATION, BoneRavenModel::createBodyLayer);
        e.registerLayerDefinition(BasaltBeetleModel.LAYER_LOCATION, BasaltBeetleModel::createBodyLayer);
        e.registerLayerDefinition(AshMothModel.LAYER_LOCATION, AshMothModel::createBodyLayer);
        e.registerLayerDefinition(AmethystFoxModel.LAYER_LOCATION, AmethystFoxModel::createBodyLayer);
        e.registerLayerDefinition(SizoLeafwingModel.LAYER_LOCATION, SizoLeafwingModel::createBodyLayer);
        e.registerLayerDefinition(AzureFrogModel.LAYER_LOCATION, AzureFrogModel::createBodyLayer);
        e.registerLayerDefinition(BlossomSnailModel.LAYER_LOCATION, BlossomSnailModel::createBodyLayer);
        e.registerLayerDefinition(PhantomWolfModel.LAYER_LOCATION, PhantomWolfModel::createBodyLayer);
        e.registerLayerDefinition(CrystalGeckoModel.LAYER_LOCATION, CrystalGeckoModel::createBodyLayer);
        e.registerLayerDefinition(FacetedOwlModel.LAYER_LOCATION, FacetedOwlModel::createBodyLayer);
        e.registerLayerDefinition(CoralHermitCrabModel.LAYER_LOCATION, CoralHermitCrabModel::createBodyLayer);
        e.registerLayerDefinition(ReefSeahorseModel.LAYER_LOCATION, ReefSeahorseModel::createBodyLayer);
        e.registerLayerDefinition(GhostSquidModel.LAYER_LOCATION, GhostSquidModel::createBodyLayer);
        e.registerLayerDefinition(SilverSeaSerpentModel.LAYER_LOCATION, SilverSeaSerpentModel::createBodyLayer);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.PORCELAIN_CRANE.get(), c ->
                new BaseRenderer<>(c, new PorcelainCraneModel(c.bakeLayer(PorcelainCraneModel.LAYER_LOCATION)),
                        "porcelain_crane", 0.36F, 0.38F, false, false, false));

        e.registerEntityRenderer(ModEntities.BASALT_BEETLE.get(), c ->
                new BaseRenderer<>(c, new BasaltBeetleModel(c.bakeLayer(BasaltBeetleModel.LAYER_LOCATION)),
                        "basalt_beetle", 0.42F, 0.62F, false, true, false));

        e.registerEntityRenderer(ModEntities.ASH_MOTH.get(), c ->
                new BaseRenderer<>(c, new AshMothModel(c.bakeLayer(AshMothModel.LAYER_LOCATION)),
                        "ash_moth", 0.32F, 0.35F, true, true, true));

        e.registerEntityRenderer(ModEntities.AMETHYST_FOX.get(), c ->
                new BaseRenderer<>(c, new AmethystFoxModel(c.bakeLayer(AmethystFoxModel.LAYER_LOCATION)),
                        "amethyst_fox", 0.38F, 0.36F, false, false, false));

        e.registerEntityRenderer(ModEntities.SIZO_LEAFWING.get(), c ->
                new BaseRenderer<>(c, new SizoLeafwingModel(c.bakeLayer(SizoLeafwingModel.LAYER_LOCATION)),
                        "sizo_leafwing", 0.42F, 0.46F, false, false, false));

        e.registerEntityRenderer(ModEntities.AZURE_FROG.get(), c ->
                new BaseRenderer<>(c, new AzureFrogModel(c.bakeLayer(AzureFrogModel.LAYER_LOCATION)),
                        "azure_frog", 0.32F, 0.25F, false, false, false));

        e.registerEntityRenderer(ModEntities.BLOSSOM_SNAIL.get(), c ->
                new BaseRenderer<>(c, new BlossomSnailModel(c.bakeLayer(BlossomSnailModel.LAYER_LOCATION)),
                        "blossom_snail", 0.42F, 0.35F, false, true, false));

        e.registerEntityRenderer(ModEntities.PHANTOM_WOLF.get(), c ->
                new BaseRenderer<>(c, new PhantomWolfModel(c.bakeLayer(PhantomWolfModel.LAYER_LOCATION)),
                        "phantom_wolf", 0.48F, 0.45F, true, true, false));

        e.registerEntityRenderer(ModEntities.CRYSTAL_GECKO.get(), c ->
                new BaseRenderer<>(c, new CrystalGeckoModel(c.bakeLayer(CrystalGeckoModel.LAYER_LOCATION)),
                        "crystal_gecko", 0.36F, 0.30F, true, false, false));

        e.registerEntityRenderer(ModEntities.FACETED_OWL.get(), c ->
                new BaseRenderer<>(c, new FacetedOwlModel(c.bakeLayer(FacetedOwlModel.LAYER_LOCATION)),
                        "faceted_owl", 0.38F, 0.32F, false, true, false));


        e.registerEntityRenderer(ModEntities.CORAL_HERMIT_CRAB.get(), c ->
                new BaseRenderer<>(c, new CoralHermitCrabModel(c.bakeLayer(CoralHermitCrabModel.LAYER_LOCATION)),
                        "coral_hermit_crab", 0.44F, 0.40F, false, false, false));

        e.registerEntityRenderer(ModEntities.REEF_SEAHORSE.get(), c ->
                new BaseRenderer<>(c, new ReefSeahorseModel(c.bakeLayer(ReefSeahorseModel.LAYER_LOCATION)),
                        "reef_seahorse", 0.42F, 0.20F, false, false, false));

        // Ворон вернулся в v0.49.0: костяной ворон Пади снова летает.
        e.registerEntityRenderer(ModEntities.BONE_RAVEN.get(), c ->
                new BaseRenderer<>(c, new BoneRavenModel(c.bakeLayer(BoneRavenModel.LAYER_LOCATION)),
                        // v0.50.0: ворон был микроскопическим (0,36 × 0,30) — в кадре
                        // он читался как несколько пикселей. Теперь модель в
                        // размер птицы, и ворон виден глазом в игре и на фото.
                        "bone_raven", 1.15F, 0.95F, false, true, true));
        e.registerEntityRenderer(ModEntities.GHOST_SQUID.get(), c ->
                new BaseRenderer<>(c, new GhostSquidModel(c.bakeLayer(GhostSquidModel.LAYER_LOCATION)),
                        "ghost_squid", 0.86F, 0.60F, true, true, true));

        e.registerEntityRenderer(ModEntities.SILVER_SEA_SERPENT.get(), c ->
                new SerpentRenderer(c));
    }

    private static final class EmissiveGlowLayer<T extends Mob, M extends FaunaBaseModel<T>> extends RenderLayer<T, M> {
        private final RenderType glowType;

        EmissiveGlowLayer(MobRenderer<T, M> parent, ResourceLocation glowTex) {
            super(parent);
            this.glowType = RenderType.eyes(glowTex);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) return;
            VertexConsumer vc = buffer.getBuffer(this.glowType);
            this.getParentModel().renderToBuffer(poseStack, vc, 15728640, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static class BaseRenderer<T extends Mob, M extends FaunaBaseModel<T>> extends MobRenderer<T, M> {
        private final ResourceLocation tex;
        private final float modelScale;
        private final boolean translucent;
        private final boolean bankAndPitch;

        BaseRenderer(EntityRendererProvider.Context ctx, M model, String id, float scale, float shadow,
                     boolean translucent, boolean emissiveGlow, boolean bankAndPitch) {
            super(ctx, model, shadow);
            this.tex = new ResourceLocation(RiftWorld.MODID, "textures/entity/" + id + ".png");
            this.modelScale = scale;
            this.translucent = translucent;
            this.bankAndPitch = bankAndPitch;
            if (emissiveGlow) {
                this.addLayer(new EmissiveGlowLayer<>(this,
                        new ResourceLocation(RiftWorld.MODID, "textures/entity/" + id + "_glow.png")));
            }
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return this.tex;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return this.tex;
        }

        /** Отладочный куб: рисуется вместо модели, если у сущности взведён флаг. */
        @Override
        public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                           MultiBufferSource buffer, int packedLight) {
            if (entity instanceof ru.aiforge.riftworld.entity.fauna.BoneRavenEntity raven
                    && raven.isShowcaseDebugCube()) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.5F, 0.0F);
                PoseStack cubePose = poseStack;
                com.mojang.blaze3d.vertex.VertexConsumer vc =
                        buffer.getBuffer(RenderType.entitySolid(new ResourceLocation(RiftWorld.MODID,
                                "textures/entity/bone_raven.png")));
                // Отладочный квад: четыре вершины вручную, без ModelPart —
                // так видно, доходит ли сущность до рендера вообще.
                float q = 0.5F;
                com.mojang.blaze3d.vertex.PoseStack qp = cubePose;
                com.mojang.blaze3d.vertex.VertexConsumer qv = vc;
                qp.pushPose();
                qp.translate(0.0F, 0.5F, 0.0F);
                org.joml.Matrix4f mtx = qp.last().pose();
                // передняя грань
                qv.vertex(mtx, -q, -q, 0.0F).color(1.0F, 0.15F, 0.15F, 1.0F).uv(0.0F, 1.0F)
                        .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight).endVertex();
                qv.vertex(mtx,  q, -q, 0.0F).color(1.0F, 0.15F, 0.15F, 1.0F).uv(1.0F, 1.0F)
                        .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight).endVertex();
                qv.vertex(mtx,  q,  q, 0.0F).color(1.0F, 0.15F, 0.15F, 1.0F).uv(1.0F, 0.0F)
                        .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight).endVertex();
                qv.vertex(mtx, -q,  q, 0.0F).color(1.0F, 0.15F, 0.15F, 1.0F).uv(0.0F, 0.0F)
                        .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight).endVertex();
                qp.popPose();
                poseStack.popPose();
                return;
            }
            super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
        }

        @Override
        protected void scale(T entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(this.modelScale, this.modelScale, this.modelScale);
        }

        @Override
        protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
            if (this.bankAndPitch) {
                float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
                float yawDelta = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
                poseStack.mulPose(Axis.XP.rotationDegrees(-pitch * 0.6F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(yawDelta * 1.4F, -16.0F, 16.0F)));
            }
        }

        @Nullable
        @Override
        protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucentFlag, boolean glowing) {
            // Внимание: LivingEntityRenderer вызывает getRenderType с
            // bodyVisible = false, поэтому проверка "if (!bodyVisible) return null"
            // отключала рендер ВСЕХ существ на BaseRenderer с непрозрачной
            // текстурой — в том числе костяного ворона. В кадре было пусто.
            // Теперь ведём себя как ванильный MobRenderer.
            if (glowing) return RenderType.entityTranslucent(this.tex);
            return (this.translucent || translucentFlag)
                    ? RenderType.entityTranslucent(this.tex)
                    : RenderType.entityCutoutNoCull(this.tex);
        }
    }

    private static final class SerpentRenderer extends BaseRenderer<SilverSeaSerpentEntity, SilverSeaSerpentModel> {
        SerpentRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new SilverSeaSerpentModel(ctx.bakeLayer(SilverSeaSerpentModel.LAYER_LOCATION)),
                    "silver_sea_serpent", 1.45F, 1.35F, false, true, true);
        }

        @Override
        public boolean shouldRender(SilverSeaSerpentEntity e, Frustum frustum, double x, double y, double z) {
            return e.shouldRender(x, y, z)
                    && (e.noCulling || frustum.isVisible(e.getBoundingBoxForCulling().inflate(4.5D, 2.0D, 4.5D)));
        }
    }
}
