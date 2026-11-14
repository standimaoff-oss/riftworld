package ru.aiforge.riftworld.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import ru.aiforge.riftworld.RiftWorld;

/**
 * v0.53.0 — 3D-модель щита Элисара.
 *
 * <p>Геометрия — ровно ванильная: та же коробка «plate» (12×22×1) и та же
 * рукоять «handle» (2×6×6), что у ванильного {@code ShieldEntityModel}, с теми
 * же UV-координатами в текстуре 64×64. Модель собрана вручную через
 * {@link ModelPart}, потому что ванильный рендерер щита рисует только
 * {@code minecraft:shield} — свой щит в Forge 1.20.1 нужно рисовать самому.
 *
 * <p>Используется {@link ru.aiforge.riftworld.client.ShieldItemRenderer}.
 */
public final class ShieldItemModel {

    /** Слой модели щита (нужен, чтобы Forge собрал меш). */
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "shield"), "main");

    private final ModelPart root;
    private final ModelPart plate;
    private final ModelPart handle;

    public ShieldItemModel(ModelPart root) {
        this.root = root;
        this.plate = root.getChild("plate");
        this.handle = root.getChild("handle");
    }

    /** Лицевая сторона щита (на ней рисуется герб/узор). */
    public ModelPart plate() {
        return this.plate;
    }

    /** Рукоять щита. */
    public ModelPart handle() {
        return this.handle;
    }

    public ModelPart root() {
        return this.root;
    }

    /**
     * Ванильная геометрия щита: «plate» 12×22×1 в UV (0,0) и
     * «handle» 2×6×6 в UV (26,0) — как в {@code ShieldEntityModel}.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plate", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -11.0F, -1.0F, 12.0F, 22.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("handle", CubeListBuilder.create()
                .texOffs(26, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
