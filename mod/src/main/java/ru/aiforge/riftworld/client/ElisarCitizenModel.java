package ru.aiforge.riftworld.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarCitizenEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;

/** Player-compatible rig with the long villager nose, folded arms and an archer's geometric quiver. */
public final class ElisarCitizenModel<T extends ElisarCitizenEntity> extends HumanoidModel<T> {
    public ElisarCitizenModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        mesh.getRoot().getChild("head").addOrReplaceChild("elisar_nose",
                CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F),
                PartPose.ZERO);
        // A rigid block quiver and three arrow shafts on the back; transparent in the two other skins.
        ModelPart quiver = mesh.getRoot().getChild("body");
        quiver.addOrReplaceChild("quiver",
                CubeListBuilder.create().texOffs(48, 32).addBox(1.0F, -1.0F, 2.4F, 4.0F, 10.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.12F, 0.0F, -0.18F));
        quiver.addOrReplaceChild("quiver_arrows",
                CubeListBuilder.create().texOffs(48, 46)
                        .addBox(1.5F, -5.0F, 3.0F, 0.7F, 6.0F, 0.7F)
                        .addBox(2.7F, -6.0F, 3.4F, 0.7F, 7.0F, 0.7F)
                        .addBox(3.9F, -4.5F, 3.8F, 0.7F, 5.5F, 0.7F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.12F, 0.0F, -0.18F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        if (entity instanceof ElisarArcherEntity && entity.isAggressive()) {
            this.rightArmPose = ArmPose.BOW_AND_ARROW;
        } else if (entity instanceof ElisarGuardEntity) {
            this.rightArmPose = ArmPose.ITEM;
            this.leftArmPose = ArmPose.BLOCK;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof ElisarVillagerEntity) {
            // The familiar crossed-arm villager silhouette from the references.
            this.rightArm.xRot = -0.78F;
            this.rightArm.yRot = -0.18F;
            this.rightArm.zRot = -0.58F;
            this.leftArm.xRot = -0.78F;
            this.leftArm.yRot = 0.18F;
            this.leftArm.zRot = 0.58F;
        }
    }
}
