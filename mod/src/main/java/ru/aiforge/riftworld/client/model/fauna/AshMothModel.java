package ru.aiforge.riftworld.client.model.fauna;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.fauna.AshMothEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/ash_moth.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class AshMothModel extends FaunaBaseModel<AshMothEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "ash_moth"), "main");

	private final ModelPart root;
	private final ModelPart thorax;
	private final ModelPart head;
	private final ModelPart antenna_l;
	private final ModelPart antenna_r;
	private final ModelPart abdomen_base;
	private final ModelPart forewing_l;
	private final ModelPart hindwing_l;
	private final ModelPart forewing_r;
	private final ModelPart hindwing_r;
	private final ModelPart ash_motes_l;
	private final ModelPart ash_motes_r;

	public AshMothModel(ModelPart root) {
		this.root = root.getChild("root");
		this.thorax = this.root.getChild("thorax");
		this.head = this.thorax.getChild("head");
		this.antenna_l = this.head.getChild("antenna_l");
		this.antenna_r = this.head.getChild("antenna_r");
		this.abdomen_base = this.thorax.getChild("abdomen_base");
		this.forewing_l = this.thorax.getChild("forewing_l");
		this.hindwing_l = this.thorax.getChild("hindwing_l");
		this.forewing_r = this.thorax.getChild("forewing_r");
		this.hindwing_r = this.thorax.getChild("hindwing_r");
		this.ash_motes_l = this.root.getChild("ash_motes_l");
		this.ash_motes_r = this.root.getChild("ash_motes_r");
		captureRestPoses(this.root, this.thorax, this.head, this.antenna_l, this.antenna_r, this.abdomen_base, this.forewing_l, this.hindwing_l, this.forewing_r, this.hindwing_r, this.ash_motes_l, this.ash_motes_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition thorax = root.addOrReplaceChild("thorax", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, -3.5F));

		PartDefinition moth_leg_back_r_paw_r1 = thorax.addOrReplaceChild("moth_leg_back_r_paw_r1", CubeListBuilder.create().texOffs(190, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 11.3F, 0.0F, 0.038F, 0.0F, -2.9889F));

		PartDefinition moth_leg_back_r_femur_r1 = thorax.addOrReplaceChild("moth_leg_back_r_femur_r1", CubeListBuilder.create().texOffs(86, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4F, 7.9F, 0.3F, 0.0874F, 0.0F, 3.0245F));

		PartDefinition moth_leg_mid_r_paw_r1 = thorax.addOrReplaceChild("moth_leg_mid_r_paw_r1", CubeListBuilder.create().texOffs(178, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 8.3F, -3.1F, 0.0759F, 0.0F, -2.9889F));

		PartDefinition moth_leg_mid_r_femur_r1 = thorax.addOrReplaceChild("moth_leg_mid_r_femur_r1", CubeListBuilder.create().texOffs(74, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, 4.9F, -2.7F, 0.1163F, 0.0F, 3.0245F));

		PartDefinition moth_leg_front_r_paw_r1 = thorax.addOrReplaceChild("moth_leg_front_r_paw_r1", CubeListBuilder.create().texOffs(166, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, 5.1F, -6.3F, 0.0759F, 0.0F, -2.9889F));

		PartDefinition moth_leg_front_r_femur_r1 = thorax.addOrReplaceChild("moth_leg_front_r_femur_r1", CubeListBuilder.create().texOffs(62, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.2F, 1.9F, -5.7F, 0.1839F, 0.0F, 3.0172F));

		PartDefinition moth_leg_back_l_paw_r1 = thorax.addOrReplaceChild("moth_leg_back_l_paw_r1", CubeListBuilder.create().texOffs(154, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 11.3F, 0.0F, 0.038F, 0.0F, 2.9889F));

		PartDefinition moth_leg_back_l_femur_r1 = thorax.addOrReplaceChild("moth_leg_back_l_femur_r1", CubeListBuilder.create().texOffs(50, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, 7.9F, 0.3F, 0.0874F, 0.0F, -3.0245F));

		PartDefinition moth_leg_mid_l_paw_r1 = thorax.addOrReplaceChild("moth_leg_mid_l_paw_r1", CubeListBuilder.create().texOffs(142, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 8.3F, -3.1F, 0.0759F, 0.0F, 2.9889F));

		PartDefinition moth_leg_mid_l_femur_r1 = thorax.addOrReplaceChild("moth_leg_mid_l_femur_r1", CubeListBuilder.create().texOffs(38, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, 4.9F, -2.7F, 0.1163F, 0.0F, -3.0245F));

		PartDefinition moth_leg_front_l_paw_r1 = thorax.addOrReplaceChild("moth_leg_front_l_paw_r1", CubeListBuilder.create().texOffs(130, 61).addBox(-1.0F, -3.2F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, 5.1F, -6.3F, 0.0759F, 0.0F, 2.9889F));

		PartDefinition moth_leg_front_l_femur_r1 = thorax.addOrReplaceChild("moth_leg_front_l_femur_r1", CubeListBuilder.create().texOffs(26, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.2F, 1.9F, -5.7F, 0.1839F, 0.0F, -3.0172F));

		PartDefinition collar_fluff_r_r1 = thorax.addOrReplaceChild("collar_fluff_r_r1", CubeListBuilder.create().texOffs(242, 2).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -1.9F, -3.0F, -0.733F, -0.1396F, 0.1745F));

		PartDefinition collar_fluff_l_r1 = thorax.addOrReplaceChild("collar_fluff_l_r1", CubeListBuilder.create().texOffs(214, 2).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.9F, -3.0F, -0.733F, 0.1396F, -0.1745F));

		PartDefinition collar_chest_r1 = thorax.addOrReplaceChild("collar_chest_r1", CubeListBuilder.create().texOffs(38, 28).addBox(-5.5F, -3.0F, -3.5F, 11.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.5F, -5.7F, -0.8378F, 0.0F, 0.0F));

		PartDefinition collar_dorsal_r1 = thorax.addOrReplaceChild("collar_dorsal_r1", CubeListBuilder.create().texOffs(402, 2).addBox(-6.5F, -3.0F, -4.0F, 13.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.7F, -3.3F, -0.6632F, 0.0F, 0.0F));

		PartDefinition thorax_core_r1 = thorax.addOrReplaceChild("thorax_core_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-6.0F, -5.5F, -5.5F, 12.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.8378F, 0.0F, 0.0F));

		PartDefinition head = thorax.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -4.1F, -6.7F));

		PartDefinition eye_r_r1 = head.addOrReplaceChild("eye_r_r1", CubeListBuilder.create().texOffs(114, 61).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(98, 61).addBox(-7.3F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.9F, 0.1F, -4.6F, -0.4538F, 0.0F, 0.0F));

		PartDefinition head_frons_ridge_r1 = head.addOrReplaceChild("head_frons_ridge_r1", CubeListBuilder.create().texOffs(492, 46).addBox(-1.5F, -2.5F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, -4.8F, -0.4538F, 0.0F, 0.0F));

		PartDefinition head_main_r1 = head.addOrReplaceChild("head_main_r1", CubeListBuilder.create().texOffs(2, 28).addBox(-4.5F, -3.5F, -3.5F, 9.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, -1.6F, -0.4538F, 0.0F, 0.0F));

		PartDefinition antenna_l = head.addOrReplaceChild("antenna_l", CubeListBuilder.create(), PartPose.offset(-2.4F, -3.6F, -2.2F));

		PartDefinition antenna_comb_l_6_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_6_r1", CubeListBuilder.create().texOffs(450, 61).addBox(-2.0F, -0.5F, -1.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9347F, -9.4016F, -2.7871F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_comb_l_5_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_5_r1", CubeListBuilder.create().texOffs(334, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.725F, -7.95F, -2.4F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_comb_l_4_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_4_r1", CubeListBuilder.create().texOffs(314, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.4685F, -6.3581F, -1.9661F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_comb_l_3_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_3_r1", CubeListBuilder.create().texOffs(294, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1621F, -4.6161F, -1.4823F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_comb_l_2_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_2_r1", CubeListBuilder.create().texOffs(274, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9161F, -2.8339F, -0.9581F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_comb_l_1_r1 = antenna_l.addOrReplaceChild("antenna_comb_l_1_r1", CubeListBuilder.create().texOffs(434, 61).addBox(-2.0F, -0.5F, -1.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7548F, -0.9952F, -0.3774F, -0.3142F, 0.1396F, -0.5585F));

		PartDefinition antenna_shaft_l_3_r1 = antenna_l.addOrReplaceChild("antenna_shaft_l_3_r1", CubeListBuilder.create().texOffs(2, 61).addBox(-1.0F, -4.2F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.1F, -7.2F, -2.2F, 0.2021F, 0.0F, -0.6947F));

		PartDefinition antenna_shaft_l_2_r1 = antenna_l.addOrReplaceChild("antenna_shaft_l_2_r1", CubeListBuilder.create().texOffs(456, 46).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -3.6F, -1.2F, 0.2187F, 0.0F, -0.6435F));

		PartDefinition antenna_shaft_l_1_r1 = antenna_l.addOrReplaceChild("antenna_shaft_l_1_r1", CubeListBuilder.create().texOffs(444, 46).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.2609F, 0.0F, -0.5633F));

		PartDefinition antenna_r = head.addOrReplaceChild("antenna_r", CubeListBuilder.create(), PartPose.offset(2.4F, -3.6F, -2.2F));

		PartDefinition antenna_comb_r_6_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_6_r1", CubeListBuilder.create().texOffs(482, 61).addBox(-2.0F, -0.5F, -1.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9347F, -9.4016F, -2.7871F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_comb_r_5_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_5_r1", CubeListBuilder.create().texOffs(414, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.725F, -7.95F, -2.4F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_comb_r_4_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_4_r1", CubeListBuilder.create().texOffs(394, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4685F, -6.3581F, -1.9661F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_comb_r_3_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_3_r1", CubeListBuilder.create().texOffs(374, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.1621F, -4.6161F, -1.4823F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_comb_r_2_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_2_r1", CubeListBuilder.create().texOffs(354, 61).addBox(-3.0F, -0.5F, -1.0F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9161F, -2.8339F, -0.9581F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_comb_r_1_r1 = antenna_r.addOrReplaceChild("antenna_comb_r_1_r1", CubeListBuilder.create().texOffs(466, 61).addBox(-2.0F, -0.5F, -1.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7548F, -0.9952F, -0.3774F, -0.3142F, -0.1396F, 0.5585F));

		PartDefinition antenna_shaft_r_3_r1 = antenna_r.addOrReplaceChild("antenna_shaft_r_3_r1", CubeListBuilder.create().texOffs(14, 61).addBox(-1.0F, -4.2F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.1F, -7.2F, -2.2F, 0.2021F, 0.0F, 0.6947F));

		PartDefinition antenna_shaft_r_2_r1 = antenna_r.addOrReplaceChild("antenna_shaft_r_2_r1", CubeListBuilder.create().texOffs(480, 46).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -3.6F, -1.2F, 0.2187F, 0.0F, 0.6435F));

		PartDefinition antenna_shaft_r_1_r1 = antenna_r.addOrReplaceChild("antenna_shaft_r_1_r1", CubeListBuilder.create().texOffs(468, 46).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.2609F, 0.0F, 0.5633F));

		PartDefinition abdomen_base = thorax.addOrReplaceChild("abdomen_base", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, 3.3F));

		PartDefinition abdomen_4_r1 = abdomen_base.addOrReplaceChild("abdomen_4_r1", CubeListBuilder.create().texOffs(110, 46).addBox(-3.0F, -2.5F, -2.5F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 13.7F, 8.6F, -1.0821F, 0.0F, 0.0F));

		PartDefinition abdomen_3_r1 = abdomen_base.addOrReplaceChild("abdomen_3_r1", CubeListBuilder.create().texOffs(286, 28).addBox(-4.0F, -3.0F, -3.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.3F, 6.6F, -1.0472F, 0.0F, 0.0F));

		PartDefinition abdomen_2_r1 = abdomen_base.addOrReplaceChild("abdomen_2_r1", CubeListBuilder.create().texOffs(448, 2).addBox(-5.0F, -3.5F, -3.5F, 10.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.3F, 4.2F, -1.0123F, 0.0F, 0.0F));

		PartDefinition abdomen_1_r1 = abdomen_base.addOrReplaceChild("abdomen_1_r1", CubeListBuilder.create().texOffs(172, 2).addBox(-5.5F, -4.0F, -4.0F, 11.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.9F, 1.4F, -0.9774F, 0.0F, 0.0F));

		PartDefinition forewing_l = thorax.addOrReplaceChild("forewing_l", CubeListBuilder.create(), PartPose.offset(-5.2F, -4.5F, -3.5F));

		PartDefinition fw_panel_l_4_r1 = forewing_l.addOrReplaceChild("fw_panel_l_4_r1", CubeListBuilder.create().texOffs(368, 46).addBox(-5.0F, -1.0F, -3.5F, 10.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-35.0675F, -12.2673F, -5.682F, -0.9699F, -0.1152F, 0.2804F));

		PartDefinition fw_panel_l_3_r1 = forewing_l.addOrReplaceChild("fw_panel_l_3_r1", CubeListBuilder.create().texOffs(136, 46).addBox(-13.0F, -1.0F, -3.5F, 26.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.7663F, 3.6862F, 3.8815F, -0.9699F, -0.1152F, 0.2804F));

		PartDefinition fw_panel_l_2_r1 = forewing_l.addOrReplaceChild("fw_panel_l_2_r1", CubeListBuilder.create().texOffs(318, 28).addBox(-17.0F, -1.0F, -4.5F, 34.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-19.4647F, -0.2598F, 1.3229F, -0.9699F, -0.1152F, 0.2804F));

		PartDefinition fw_panel_l_1_r1 = forewing_l.addOrReplaceChild("fw_panel_l_1_r1", CubeListBuilder.create().texOffs(78, 28).addBox(-20.0F, -1.0F, -5.0F, 40.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-19.9178F, -4.7055F, -1.6825F, -0.9699F, -0.1152F, 0.2804F));

		PartDefinition fw_costa_l_3_r1 = forewing_l.addOrReplaceChild("fw_costa_l_3_r1", CubeListBuilder.create().texOffs(132, 2).addBox(-1.5F, -14.0F, -2.5F, 3.0F, 15.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-26.1751F, -10.6592F, -5.078F, 0.3562F, -0.9603F, -1.4566F));

		PartDefinition fw_costa_l_2_r1 = forewing_l.addOrReplaceChild("fw_costa_l_2_r1", CubeListBuilder.create().texOffs(72, 2).addBox(-1.5F, -15.0F, -2.5F, 3.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.4795F, -5.0292F, -2.1769F, 0.3647F, -0.9603F, -1.4566F));

		PartDefinition fw_costa_l_1_r1 = forewing_l.addOrReplaceChild("fw_costa_l_1_r1", CubeListBuilder.create().texOffs(52, 2).addBox(-1.5F, -15.0F, -2.5F, 3.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2776F, 0.6607F, 0.7255F, 0.3525F, -0.9603F, -1.4566F));

		PartDefinition hindwing_l = thorax.addOrReplaceChild("hindwing_l", CubeListBuilder.create(), PartPose.offset(-10.5306F, 4.542F, 1.7458F));

		PartDefinition hw_panel_l_low_r1 = hindwing_l.addOrReplaceChild("hw_panel_l_low_r1", CubeListBuilder.create().texOffs(276, 46).addBox(-7.0F, -1.0F, -3.5F, 14.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.1751F, 6.2798F, 5.8271F, -0.9752F, -0.1607F, 0.0608F));

		PartDefinition hw_panel_l_outer_r1 = hindwing_l.addOrReplaceChild("hw_panel_l_outer_r1", CubeListBuilder.create().texOffs(2, 46).addBox(-8.0F, -1.0F, -4.5F, 16.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.6657F, 2.1495F, 2.4032F, -0.9752F, -0.1607F, 0.0608F));

		PartDefinition hw_panel_l_main_r1 = hindwing_l.addOrReplaceChild("hw_panel_l_main_r1", CubeListBuilder.create().texOffs(270, 2).addBox(-9.5F, -1.0F, -6.0F, 19.0F, 2.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0035F, 3.0872F, 3.4695F, -0.9752F, -0.1607F, 0.0608F));

		PartDefinition forewing_r = thorax.addOrReplaceChild("forewing_r", CubeListBuilder.create(), PartPose.offset(5.2F, -4.5F, -3.5F));

		PartDefinition fw_panel_r_4_r1 = forewing_r.addOrReplaceChild("fw_panel_r_4_r1", CubeListBuilder.create().texOffs(406, 46).addBox(-5.0F, -1.0F, -3.5F, 10.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(35.0675F, -12.2673F, -5.682F, -0.9699F, 0.1152F, -0.2804F));

		PartDefinition fw_panel_r_3_r1 = forewing_r.addOrReplaceChild("fw_panel_r_3_r1", CubeListBuilder.create().texOffs(206, 46).addBox(-13.0F, -1.0F, -3.5F, 26.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(17.7663F, 3.6862F, 3.8815F, -0.9699F, 0.1152F, -0.2804F));

		PartDefinition fw_panel_r_2_r1 = forewing_r.addOrReplaceChild("fw_panel_r_2_r1", CubeListBuilder.create().texOffs(408, 28).addBox(-17.0F, -1.0F, -4.5F, 34.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(19.4647F, -0.2598F, 1.3229F, -0.9699F, 0.1152F, -0.2804F));

		PartDefinition fw_panel_r_1_r1 = forewing_r.addOrReplaceChild("fw_panel_r_1_r1", CubeListBuilder.create().texOffs(182, 28).addBox(-20.0F, -1.0F, -5.0F, 40.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(19.9178F, -4.7055F, -1.6825F, -0.9699F, 0.1152F, -0.2804F));

		PartDefinition fw_costa_r_3_r1 = forewing_r.addOrReplaceChild("fw_costa_r_3_r1", CubeListBuilder.create().texOffs(152, 2).addBox(-1.5F, -14.0F, -2.5F, 3.0F, 15.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(26.1751F, -10.6592F, -5.078F, 0.3562F, 0.9603F, 1.4566F));

		PartDefinition fw_costa_r_2_r1 = forewing_r.addOrReplaceChild("fw_costa_r_2_r1", CubeListBuilder.create().texOffs(112, 2).addBox(-1.5F, -15.0F, -2.5F, 3.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.4795F, -5.0292F, -2.1769F, 0.3647F, 0.9603F, 1.4566F));

		PartDefinition fw_costa_r_1_r1 = forewing_r.addOrReplaceChild("fw_costa_r_1_r1", CubeListBuilder.create().texOffs(92, 2).addBox(-1.5F, -15.0F, -2.5F, 3.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2776F, 0.6607F, 0.7255F, 0.3525F, 0.9603F, 1.4566F));

		PartDefinition hindwing_r = thorax.addOrReplaceChild("hindwing_r", CubeListBuilder.create(), PartPose.offset(10.5306F, 4.542F, 1.7458F));

		PartDefinition hw_panel_r_low_r1 = hindwing_r.addOrReplaceChild("hw_panel_r_low_r1", CubeListBuilder.create().texOffs(322, 46).addBox(-7.0F, -1.0F, -3.5F, 14.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.1751F, 6.2798F, 5.8271F, -0.9752F, 0.1607F, -0.0608F));

		PartDefinition hw_panel_r_outer_r1 = hindwing_r.addOrReplaceChild("hw_panel_r_outer_r1", CubeListBuilder.create().texOffs(56, 46).addBox(-8.0F, -1.0F, -4.5F, 16.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.6657F, 2.1495F, 2.4032F, -0.9752F, 0.1607F, -0.0608F));

		PartDefinition hw_panel_r_main_r1 = hindwing_r.addOrReplaceChild("hw_panel_r_main_r1", CubeListBuilder.create().texOffs(336, 2).addBox(-9.5F, -1.0F, -6.0F, 19.0F, 2.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0035F, 3.0872F, 3.4695F, -0.9752F, 0.1607F, -0.0608F));

		PartDefinition ash_motes_l = root.addOrReplaceChild("ash_motes_l", CubeListBuilder.create(), PartPose.offset(-12.0F, 16.0F, 7.0F));

		PartDefinition ash_mote_l_3_r1 = ash_motes_l.addOrReplaceChild("ash_mote_l_3_r1", CubeListBuilder.create().texOffs(226, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2F, -0.6F, 4.4F, -0.4189F, 0.2793F, 0.2094F));

		PartDefinition ash_mote_l_2_r1 = ash_motes_l.addOrReplaceChild("ash_mote_l_2_r1", CubeListBuilder.create().texOffs(214, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 1.8F, 2.2F, 0.3142F, -0.2443F, 0.3491F));

		PartDefinition ash_mote_l_1_r1 = ash_motes_l.addOrReplaceChild("ash_mote_l_1_r1", CubeListBuilder.create().texOffs(202, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.8F, 0.5F, -2.5F, -0.2618F, -0.384F, -0.1745F));

		PartDefinition ash_motes_r = root.addOrReplaceChild("ash_motes_r", CubeListBuilder.create(), PartPose.offset(12.0F, 16.0F, 7.0F));

		PartDefinition ash_mote_r_3_r1 = ash_motes_r.addOrReplaceChild("ash_mote_r_3_r1", CubeListBuilder.create().texOffs(262, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2F, -0.6F, 4.4F, -0.4189F, -0.2793F, -0.2094F));

		PartDefinition ash_mote_r_2_r1 = ash_motes_r.addOrReplaceChild("ash_mote_r_2_r1", CubeListBuilder.create().texOffs(250, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 1.8F, 2.2F, 0.3142F, 0.2443F, -0.3491F));

		PartDefinition ash_mote_r_1_r1 = ash_motes_r.addOrReplaceChild("ash_mote_r_1_r1", CubeListBuilder.create().texOffs(238, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8F, 0.5F, -2.5F, -0.2618F, 0.384F, 0.1745F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(AshMothEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		boolean swoop = entity.getSwoopTicks() > 0;
		float freq = swoop ? 0.82F : 0.56F;
		float phase = ageInTicks * freq + (entity.getId() * 0.9F);
		float foreFlap = Mth.sin(phase) * (swoop ? 48.0F : 38.0F);
		float hindFlap = Mth.sin(phase - 0.48F) * (swoop ? 40.0F : 31.0F);

		addBbPos(this.thorax, 0.0F, Mth.cos(phase) * 1.25F, 0.0F);
		addBbDeg(this.thorax, (swoop ? 22.0F : 8.0F) + Mth.cos(phase) * 3.5F, 0.0F, 0.0F);
		addBbDeg(this.abdomen_base, -10.0F + Mth.sin(phase - 0.8F) * 7.5F, 0.0F, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.4F, Mth.clamp(netHeadYaw, -35.0F, 35.0F) * 0.5F, 0.0F);

		addBbDeg(this.forewing_l, swoop ? 8.0F : 0.0F, 0.0F, -foreFlap);
		addBbDeg(this.forewing_r, swoop ? 8.0F : 0.0F, 0.0F, foreFlap);
		addBbDeg(this.hindwing_l, 0.0F, 0.0F, -hindFlap);
		addBbDeg(this.hindwing_r, 0.0F, 0.0F, hindFlap);

		addBbDeg(this.antenna_l, Mth.sin(ageInTicks * 0.28F) * 5.5F, 0.0F, -Mth.cos(ageInTicks * 0.22F) * 3.5F);
		addBbDeg(this.antenna_r, Mth.cos(ageInTicks * 0.28F) * 5.5F, 0.0F, Mth.sin(ageInTicks * 0.22F) * 3.5F);
		addBbPos(this.ash_motes_l, 0.0F, Mth.sin(ageInTicks * 0.14F) * 1.4F, 0.0F);
		addBbPos(this.ash_motes_r, 0.0F, Mth.cos(ageInTicks * 0.14F) * 1.4F, 0.0F);
	}
}
