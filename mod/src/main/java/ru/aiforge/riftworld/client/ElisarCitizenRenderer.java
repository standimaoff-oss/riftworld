package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarCitizenEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;

/** One animation rig, three independent high-detail Minecraft skins. */
public final class ElisarCitizenRenderer extends MobRenderer<ElisarCitizenEntity, ElisarCitizenModel<ElisarCitizenEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "elisar_citizen"), "main");
    private static final ResourceLocation VILLAGER = new ResourceLocation(RiftWorld.MODID, "textures/entity/elisar_villager.png");
    private static final ResourceLocation GUARD = new ResourceLocation(RiftWorld.MODID, "textures/entity/elisar_guard.png");
    private static final ResourceLocation ARCHER = new ResourceLocation(RiftWorld.MODID, "textures/entity/elisar_archer.png");

    public ElisarCitizenRenderer(EntityRendererProvider.Context context) {
        super(context, new ElisarCitizenModel<>(context.bakeLayer(LAYER)), 0.45F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ElisarCitizenEntity entity) {
        if (entity instanceof ElisarGuardEntity) return GUARD;
        if (entity instanceof ElisarArcherEntity) return ARCHER;
        return VILLAGER;
    }

    @Override
    protected void scale(ElisarCitizenEntity entity, PoseStack pose, float partialTick) {
        if (entity.isBaby()) pose.scale(0.52F, 0.52F, 0.52F);
    }
}
