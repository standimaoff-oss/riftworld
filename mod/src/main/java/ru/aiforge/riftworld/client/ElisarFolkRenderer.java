package ru.aiforge.riftworld.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import ru.aiforge.riftworld.RiftWorld;

/**
 * v0.40.0: жители, стражи и лучники Элисара — обычный гуманоидный скин 64x64
 * ({@code ModelLayers.PLAYER}) поверх текстур, снятых с референсов 05–07.
 */
public final class ElisarFolkRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {

    private final ResourceLocation texture;

    public ElisarFolkRenderer(EntityRendererProvider.Context ctx, String textureName) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.texture = new ResourceLocation(RiftWorld.MODID, "textures/entity/" + textureName + ".png");
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
