package ru.aiforge.riftworld.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.model.ElisarFolkModels;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;

/**
 * v0.48.0: Dedicated renderers for the three Elisar settlement residents (references 05, 06, 07):
 *  - Villager: peaceful lilac-coated citizen with folded arms;
 *  - Guard: golden-aurite & celadon armored sentinel wielding sword (left hand) + shield (right hand);
 *  - Archer: leather-vested ranger with asymmetric teal right pauldron, 3D back quiver, and recurve bow.
 */
public final class ElisarFolkRenderers {
    private static final ResourceLocation VILLAGER_TEX = new ResourceLocation(
            RiftWorld.MODID, "textures/entity/elisar_villager.png");
    private static final ResourceLocation GUARD_TEX = new ResourceLocation(
            RiftWorld.MODID, "textures/entity/elisar_guard.png");
    private static final ResourceLocation ARCHER_TEX = new ResourceLocation(
            RiftWorld.MODID, "textures/entity/elisar_archer.png");

    private ElisarFolkRenderers() {
    }

    public static final class Villager extends MobRenderer<ElisarVillagerEntity, ElisarFolkModels.Villager> {
        public Villager(EntityRendererProvider.Context ctx) {
            super(ctx, new ElisarFolkModels.Villager(ctx.bakeLayer(ElisarFolkModels.VILLAGER_LAYER)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(ElisarVillagerEntity entity) {
            return VILLAGER_TEX;
        }
    }

    public static final class Guard extends MobRenderer<ElisarGuardEntity, ElisarFolkModels.Guard> {
        public Guard(EntityRendererProvider.Context ctx) {
            super(ctx, new ElisarFolkModels.Guard(ctx.bakeLayer(ElisarFolkModels.GUARD_LAYER)), 0.5F);
            this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(ElisarGuardEntity entity) {
            return GUARD_TEX;
        }
    }

    public static final class Archer extends MobRenderer<ElisarArcherEntity, ElisarFolkModels.Archer> {
        public Archer(EntityRendererProvider.Context ctx) {
            super(ctx, new ElisarFolkModels.Archer(ctx.bakeLayer(ElisarFolkModels.ARCHER_LAYER)), 0.5F);
            this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(ElisarArcherEntity entity) {
            return ARCHER_TEX;
        }
    }
}
