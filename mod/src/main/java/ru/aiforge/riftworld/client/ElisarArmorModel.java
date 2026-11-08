package ru.aiforge.riftworld.client;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Real worn-armour geometry, not a flat player skin. Extra UV space lives in a 128x128 atlas.
 * The vanilla armour layer still controls part visibility, glint and animation for every slot.
 */
public final class ElisarArmorModel extends HumanoidModel<LivingEntity> {
    private static final Map<EquipmentSlot, ElisarArmorModel> AURITE = new EnumMap<>(EquipmentSlot.class);
    private static final Map<EquipmentSlot, ElisarArmorModel> RIFTCRYSTAL = new EnumMap<>(EquipmentSlot.class);

    private ElisarArmorModel(ModelPart root) {
        super(root);
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot, boolean riftcrystal) {
        Map<EquipmentSlot, ElisarArmorModel> cache = riftcrystal ? RIFTCRYSTAL : AURITE;
        return cache.computeIfAbsent(slot, key -> new ElisarArmorModel(createLayer(key, riftcrystal).bakeRoot()));
    }

    private static LayerDefinition createLayer(EquipmentSlot slot, boolean riftcrystal) {
        float inflation = slot == EquipmentSlot.LEGS ? 0.35F : 0.65F;
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inflation), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        PartDefinition rightArm = root.getChild("right_arm");
        PartDefinition leftArm = root.getChild("left_arm");
        PartDefinition rightLeg = root.getChild("right_leg");
        PartDefinition leftLeg = root.getChild("left_leg");

        // Open-faced helmet: separate brow, cheek plates and a raised central crest.
        box(head, "helmet_brow", 64, 0, -4.5F, -7.7F, -4.8F, 9.0F, 2.0F, 1.0F);
        box(head, "right_cheek_guard", 88, 0, -4.8F, -5.7F, -4.65F, 1.0F, 5.0F, 1.0F);
        box(head, "left_cheek_guard", 88, 0, 3.8F, -5.7F, -4.65F, 1.0F, 5.0F, 1.0F);
        box(head, "helmet_crest", 96, 0, -1.0F, riftcrystal ? -12.0F : -10.8F, -1.0F,
                2.0F, riftcrystal ? 4.0F : 3.0F, 2.0F);
        if (riftcrystal) {
            box(head, "right_head_crystal", 112, 80, -5.0F, -9.7F, -1.0F, 2.0F, 4.0F, 2.0F);
            box(head, "left_head_crystal", 112, 80, 3.0F, -9.7F, -1.0F, 2.0F, 4.0F, 2.0F);
        }

        // Floating breast/back plates and three visibly separate lamellar abdomen bands.
        box(body, "breastplate", 64, 16, -4.5F, 0.1F, -2.85F, 9.0F, 7.0F, 1.0F);
        box(body, "backplate", 88, 16, -4.5F, 0.1F, 1.85F, 9.0F, 7.0F, 1.0F);
        for (int band = 0; band < 3; ++band) {
            box(body, "abdomen_band_" + band, 64, 32, -4.35F, 7.0F + band * 1.65F,
                    -2.65F, 8.7F, 1.45F, 0.8F);
        }
        box(body, "belt_buckle", 96, 0, -1.0F, 9.0F, -3.0F, 2.0F, 2.0F, 1.0F);

        float padWidth = riftcrystal ? 6.0F : 5.2F;
        float padHeight = riftcrystal ? 4.5F : 3.7F;
        // Right and left shoulder shells have real depth and project beyond the vanilla arms.
        box(rightArm, "right_shoulder_shell", 64, 48, -padWidth + 0.4F, -3.3F, -3.2F,
                padWidth, padHeight, 6.4F);
        box(leftArm, "left_shoulder_shell", 64, 48, -0.4F, -3.3F, -3.2F,
                padWidth, padHeight, 6.4F);
        box(rightArm, "right_outer_pauldron", 96, 48, -4.7F, -2.2F, -2.6F, 1.0F, 5.0F, 5.2F);
        box(leftArm, "left_outer_pauldron", 96, 48, 3.7F, -2.2F, -2.6F, 1.0F, 5.0F, 5.2F);
        box(rightArm, "right_bracer", 64, 64, -3.45F, 4.0F, -2.55F, 5.0F, 4.0F, 5.1F);
        box(leftArm, "left_bracer", 64, 64, -1.55F, 4.0F, -2.55F, 5.0F, 4.0F, 5.1F);
        if (riftcrystal) {
            box(rightArm, "right_shoulder_crystal", 112, 80, -5.6F, -5.4F, -1.0F, 2.0F, 4.0F, 2.0F);
            box(leftArm, "left_shoulder_crystal", 112, 80, 3.6F, -5.4F, -1.0F, 2.0F, 4.0F, 2.0F);
        }

        for (PartDefinition leg : new PartDefinition[]{rightLeg, leftLeg}) {
            String side = leg == rightLeg ? "right" : "left";
            box(leg, side + "_thigh_plate", 88, 64, -2.45F, 1.8F, -2.65F, 4.9F, 5.0F, 0.9F);
            box(leg, side + "_knee_guard", 104, 64, -2.55F, 5.9F, -2.9F, 5.1F, 3.0F, 1.0F);
            box(leg, side + "_greave", 64, 80, -2.5F, 8.0F, -2.55F, 5.0F, 4.0F, 5.1F);
            box(leg, side + "_toe_cap", 88, 80, -2.5F, 10.0F, -3.6F, 5.0F, 2.0F, 1.4F);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void box(PartDefinition part, String name, int u, int v, float x, float y, float z,
                            float width, float height, float depth) {
        part.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, width, height, depth), PartPose.ZERO);
    }
}
