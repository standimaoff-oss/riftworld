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
import ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/sizo_leafwing.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class SizoLeafwingModel extends FaunaBaseModel<SizoLeafwingEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "sizo_leafwing"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart antler_l;
	private final ModelPart antler_r;
	private final ModelPart arm_l;
	private final ModelPart arm_r;
	private final ModelPart wing_l;
	private final ModelPart wing_r;
	private final ModelPart tail_base;
	private final ModelPart leg_back_l;
	private final ModelPart leg_back_r;

	public SizoLeafwingModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.body.getChild("head");
		this.antler_l = this.head.getChild("antler_l");
		this.antler_r = this.head.getChild("antler_r");
		this.arm_l = this.body.getChild("arm_l");
		this.arm_r = this.body.getChild("arm_r");
		this.wing_l = this.body.getChild("wing_l");
		this.wing_r = this.body.getChild("wing_r");
		this.tail_base = this.body.getChild("tail_base");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.head, this.antler_l, this.antler_r, this.arm_l, this.arm_r, this.wing_l, this.wing_r, this.tail_base, this.leg_back_l, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -17.5F, 1.5F));

		PartDefinition neck_vine_r_r1 = body.addOrReplaceChild("neck_vine_r_r1", CubeListBuilder.create().texOffs(438, 46).addBox(-0.5F, -7.4F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, -13.9F, -10.1F, 1.4784F, -1.4158F, 1.5708F));

		PartDefinition neck_flower_r_3_r1 = body.addOrReplaceChild("neck_flower_r_3_r1", CubeListBuilder.create().texOffs(268, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2F, -7.7F, -11.3F, -0.3491F, 0.2094F, 0.2793F));

		PartDefinition neck_flower_r_2_r1 = body.addOrReplaceChild("neck_flower_r_2_r1", CubeListBuilder.create().texOffs(256, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, -10.5F, -10.9F, -0.2793F, 0.3142F, 0.384F));

		PartDefinition neck_flower_r_1_r1 = body.addOrReplaceChild("neck_flower_r_1_r1", CubeListBuilder.create().texOffs(244, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -13.5F, -10.3F, -0.2094F, 0.4189F, 0.3142F));

		PartDefinition neck_vine_l_r1 = body.addOrReplaceChild("neck_vine_l_r1", CubeListBuilder.create().texOffs(430, 46).addBox(-0.5F, -7.4F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, -13.9F, -10.1F, 1.4784F, 1.4158F, -1.5708F));

		PartDefinition neck_flower_l_3_r1 = body.addOrReplaceChild("neck_flower_l_3_r1", CubeListBuilder.create().texOffs(232, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, -7.7F, -11.3F, -0.3491F, -0.2094F, -0.2793F));

		PartDefinition neck_flower_l_2_r1 = body.addOrReplaceChild("neck_flower_l_2_r1", CubeListBuilder.create().texOffs(220, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, -10.5F, -10.9F, -0.2793F, -0.3142F, -0.384F));

		PartDefinition neck_flower_l_1_r1 = body.addOrReplaceChild("neck_flower_l_1_r1", CubeListBuilder.create().texOffs(208, 61).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -13.5F, -10.3F, -0.2094F, -0.4189F, -0.3142F));

		PartDefinition chest_mane_lower_r1 = body.addOrReplaceChild("chest_mane_lower_r1", CubeListBuilder.create().texOffs(270, 29).addBox(-4.0F, -8.2F, -1.5F, 8.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.9F, -13.1F, -0.3805F, 0.0F, 3.1416F));

		PartDefinition chest_mane_upper_r1 = body.addOrReplaceChild("chest_mane_upper_r1", CubeListBuilder.create().texOffs(296, 29).addBox(-3.5F, -8.2F, -1.5F, 7.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.9F, -12.9F, 0.0526F, 0.0F, -3.1416F));

		PartDefinition neck_upper_r1 = body.addOrReplaceChild("neck_upper_r1", CubeListBuilder.create().texOffs(2, 46).addBox(-3.0F, -3.8F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.7F, -11.1F, 0.245F, 0.0F, 0.0F));

		PartDefinition neck_lower_r1 = body.addOrReplaceChild("neck_lower_r1", CubeListBuilder.create().texOffs(398, 2).addBox(-3.5F, -5.8F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.5F, -9.8F, 0.3029F, 0.0F, 0.0F));

		PartDefinition chest_core_r1 = body.addOrReplaceChild("chest_core_r1", CubeListBuilder.create().texOffs(126, 2).addBox(-5.0F, -7.8F, -4.5F, 10.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, -7.0F, 0.4838F, 0.0F, 0.0F));

		PartDefinition waist_core_r1 = body.addOrReplaceChild("waist_core_r1", CubeListBuilder.create().texOffs(86, 2).addBox(-4.5F, -8.8F, -4.5F, 9.0F, 10.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.5F, -1.7F, 0.8622F, 0.0F, 0.0F));

		PartDefinition pelvis_core_r1 = body.addOrReplaceChild("pelvis_core_r1", CubeListBuilder.create().texOffs(168, 2).addBox(-4.5F, -7.8F, -4.0F, 9.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 4.0F, 1.1479F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -17.7F, -11.9F));

		PartDefinition horn_bud_r_r1 = head.addOrReplaceChild("horn_bud_r_r1", CubeListBuilder.create().texOffs(140, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8F, -2.8F, 0.2F, -0.4612F, 0.0F, 0.1107F));

		PartDefinition ear_lower_r_r1 = head.addOrReplaceChild("ear_lower_r_r1", CubeListBuilder.create().texOffs(70, 46).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -0.6F, 0.8F, -1.8462F, -0.7454F, -3.1416F));

		PartDefinition ear_main_r_r1 = head.addOrReplaceChild("ear_main_r_r1", CubeListBuilder.create().texOffs(346, 2).addBox(-1.0F, -9.8F, -2.0F, 2.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, -1.8F, 0.6F, -2.2171F, -0.6626F, -2.8357F));

		PartDefinition cheek_tuft_r_1_r1 = head.addOrReplaceChild("cheek_tuft_r_1_r1", CubeListBuilder.create().texOffs(460, 46).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, 1.2F, -0.8F, -1.3758F, -0.5743F, -3.1416F));

		PartDefinition eye_r_r1 = head.addOrReplaceChild("eye_r_r1", CubeListBuilder.create().texOffs(84, 61).addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4F, -0.3F, -3.2F, -0.1047F, -0.3142F, 0.0F));

		PartDefinition horn_bud_l_r1 = head.addOrReplaceChild("horn_bud_l_r1", CubeListBuilder.create().texOffs(128, 61).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8F, -2.8F, 0.2F, -0.4612F, 0.0F, -0.1107F));

		PartDefinition ear_lower_l_r1 = head.addOrReplaceChild("ear_lower_l_r1", CubeListBuilder.create().texOffs(46, 46).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -0.6F, 0.8F, -1.8462F, 0.7454F, 3.1416F));

		PartDefinition ear_main_l_r1 = head.addOrReplaceChild("ear_main_l_r1", CubeListBuilder.create().texOffs(330, 2).addBox(-1.0F, -9.8F, -2.0F, 2.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, -1.8F, 0.6F, -2.2171F, 0.6626F, 2.8357F));

		PartDefinition cheek_tuft_l_1_r1 = head.addOrReplaceChild("cheek_tuft_l_1_r1", CubeListBuilder.create().texOffs(446, 46).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, 1.2F, -0.8F, -1.3758F, 0.5743F, 3.1416F));

		PartDefinition eye_l_r1 = head.addOrReplaceChild("eye_l_r1", CubeListBuilder.create().texOffs(68, 61).addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, -0.3F, -3.2F, -0.1047F, 0.3142F, 0.0F));

		PartDefinition snout_lower_r1 = head.addOrReplaceChild("snout_lower_r1", CubeListBuilder.create().texOffs(2, 61).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.4F, -5.6F, -0.1047F, 0.0F, 0.0F));

		PartDefinition snout_upper_r1 = head.addOrReplaceChild("snout_upper_r1", CubeListBuilder.create().texOffs(404, 46).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8F, -6.4F, -0.1745F, 0.0F, 0.0F));

		PartDefinition head_cranium_r1 = head.addOrReplaceChild("head_cranium_r1", CubeListBuilder.create().texOffs(362, 2).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, -1.6F, -0.1047F, 0.0F, 0.0F));

		PartDefinition antler_l = head.addOrReplaceChild("antler_l", CubeListBuilder.create(), PartPose.offset(-2.6F, -3.0F, -1.0F));

		PartDefinition antler_tine_upper_l_r1 = antler_l.addOrReplaceChild("antler_tine_upper_l_r1", CubeListBuilder.create().texOffs(160, 61).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -8.0F, 4.8F, 1.4416F, -1.2627F, -1.5708F));

		PartDefinition antler_tine_front_l_r1 = antler_l.addOrReplaceChild("antler_tine_front_l_r1", CubeListBuilder.create().texOffs(152, 61).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, -3.2F, 1.9F, 1.4091F, -1.2168F, -1.5708F));

		PartDefinition antler_beam_l_2_r1 = antler_l.addOrReplaceChild("antler_beam_l_2_r1", CubeListBuilder.create().texOffs(94, 46).addBox(-0.5F, -8.2F, -1.0F, 1.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9F, -5.2F, 3.0F, -0.5317F, 0.0F, -0.345F));

		PartDefinition antler_beam_l_1_r1 = antler_l.addOrReplaceChild("antler_beam_l_1_r1", CubeListBuilder.create().texOffs(58, 46).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4F, 0.0F, -0.4684F, 0.0F, -0.3217F));

		PartDefinition antler_r = head.addOrReplaceChild("antler_r", CubeListBuilder.create(), PartPose.offset(2.6F, -3.0F, -1.0F));

		PartDefinition antler_tine_upper_r_r1 = antler_r.addOrReplaceChild("antler_tine_upper_r_r1", CubeListBuilder.create().texOffs(176, 61).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -8.0F, 4.8F, 1.4416F, 1.2627F, 1.5708F));

		PartDefinition antler_tine_front_r_r1 = antler_r.addOrReplaceChild("antler_tine_front_r_r1", CubeListBuilder.create().texOffs(168, 61).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, -3.2F, 1.9F, 1.4091F, 1.2168F, 1.5708F));

		PartDefinition antler_beam_r_2_r1 = antler_r.addOrReplaceChild("antler_beam_r_2_r1", CubeListBuilder.create().texOffs(104, 46).addBox(-0.5F, -8.2F, -1.0F, 1.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9F, -5.2F, 3.0F, -0.5317F, 0.0F, 0.345F));

		PartDefinition antler_beam_r_1_r1 = antler_r.addOrReplaceChild("antler_beam_r_1_r1", CubeListBuilder.create().texOffs(82, 46).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4F, 0.0F, -0.4684F, 0.0F, 0.3217F));

		PartDefinition arm_l = body.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offset(-4.8F, -6.5F, -8.3F));

		PartDefinition arm_l_claw_3_r1 = arm_l.addOrReplaceChild("arm_l_claw_3_r1", CubeListBuilder.create().texOffs(300, 61).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(290, 61).addBox(0.4F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(280, 61).addBox(1.3F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 15.4F, -6.4F, -0.384F, 0.0F, 0.0F));

		PartDefinition arm_l_hand_r1 = arm_l.addOrReplaceChild("arm_l_hand_r1", CubeListBuilder.create().texOffs(100, 61).addBox(-1.5F, -4.2F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, 10.8F, -4.8F, 0.3213F, 0.0F, 3.0861F));

		PartDefinition arm_l_fore_r1 = arm_l.addOrReplaceChild("arm_l_fore_r1", CubeListBuilder.create().texOffs(74, 29).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, 6.2F, 2.0F, 2.1681F, -0.1279F, 0.0F));

		PartDefinition arm_l_upper_r1 = arm_l.addOrReplaceChild("arm_l_upper_r1", CubeListBuilder.create().texOffs(2, 29).addBox(-1.5F, -7.8F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, -0.2F, 0.0F, -0.314F, 0.0F, -2.9934F));

		PartDefinition arm_r = body.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offset(4.8F, -6.5F, -8.3F));

		PartDefinition arm_r_claw_3_r1 = arm_r.addOrReplaceChild("arm_r_claw_3_r1", CubeListBuilder.create().texOffs(330, 61).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(320, 61).addBox(-1.4F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(310, 61).addBox(-2.3F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 15.4F, -6.4F, -0.384F, 0.0F, 0.0F));

		PartDefinition arm_r_hand_r1 = arm_r.addOrReplaceChild("arm_r_hand_r1", CubeListBuilder.create().texOffs(114, 61).addBox(-1.5F, -4.2F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, 10.8F, -4.8F, 0.3213F, 0.0F, -3.0861F));

		PartDefinition arm_r_fore_r1 = arm_r.addOrReplaceChild("arm_r_fore_r1", CubeListBuilder.create().texOffs(90, 29).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7F, 6.2F, 2.0F, 2.1681F, 0.1279F, 0.0F));

		PartDefinition arm_r_upper_r1 = arm_r.addOrReplaceChild("arm_r_upper_r1", CubeListBuilder.create().texOffs(38, 29).addBox(-1.5F, -7.8F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, -0.2F, 0.0F, -0.314F, 0.0F, 2.9934F));

		PartDefinition wing_l = body.addOrReplaceChild("wing_l", CubeListBuilder.create(), PartPose.offset(-4.6F, -8.7F, -6.0F));

		PartDefinition wing_leaf_l_4_r1 = wing_l.addOrReplaceChild("wing_leaf_l_4_r1", CubeListBuilder.create().texOffs(242, 46).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.0564F, -4.8409F, 4.3498F, -1.2012F, 0.1406F, 0.4275F));

		PartDefinition wing_leaf_l_3_r1 = wing_l.addOrReplaceChild("wing_leaf_l_3_r1", CubeListBuilder.create().texOffs(114, 46).addBox(-11.0F, -1.0F, -4.0F, 22.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.1638F, -5.3387F, 7.1217F, -1.2012F, 0.1406F, 0.4275F));

		PartDefinition wing_leaf_l_2_r1 = wing_l.addOrReplaceChild("wing_leaf_l_2_r1", CubeListBuilder.create().texOffs(106, 29).addBox(-14.5F, -1.0F, -5.0F, 29.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-24.8877F, -11.9237F, 5.9994F, -1.2012F, 0.1406F, 0.4275F));

		PartDefinition wing_leaf_l_1_r1 = wing_l.addOrReplaceChild("wing_leaf_l_1_r1", CubeListBuilder.create().texOffs(336, 29).addBox(-16.0F, -1.0F, -4.5F, 32.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-28.9972F, -19.0375F, 5.123F, -1.2012F, 0.1406F, 0.4275F));

		PartDefinition wing_costa_l_2_r1 = wing_l.addOrReplaceChild("wing_costa_l_2_r1", CubeListBuilder.create().texOffs(62, 2).addBox(-1.0F, -17.2F, -1.0F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-30.4F, -20.4F, 3.6F, -1.847F, 1.4036F, 3.1416F));

		PartDefinition wing_costa_l_1_r1 = wing_l.addOrReplaceChild("wing_costa_l_1_r1", CubeListBuilder.create().texOffs(34, 2).addBox(-1.0F, -17.0F, -1.5F, 2.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.6F, -15.4F, 1.0F, -1.8938F, 1.3961F, 3.1416F));

		PartDefinition wing_claw_l_r1 = wing_l.addOrReplaceChild("wing_claw_l_r1", CubeListBuilder.create().texOffs(184, 61).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.0F, -17.4F, 0.3F, -0.384F, 0.0F, -0.3142F));

		PartDefinition wing_forearm_l_r1 = wing_l.addOrReplaceChild("wing_forearm_l_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, -18.8F, -1.5F, 3.0F, 20.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -0.1F, 1.6F, 0.0336F, 0.0F, -0.5164F));

		PartDefinition wing_humerus_l_r1 = wing_l.addOrReplaceChild("wing_humerus_l_r1", CubeListBuilder.create().texOffs(450, 2).addBox(-1.5F, -8.8F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, 0.0F, 0.0F, -1.6093F, 1.3507F, 3.1416F));

		PartDefinition wing_r = body.addOrReplaceChild("wing_r", CubeListBuilder.create(), PartPose.offset(4.6F, -8.7F, -6.0F));

		PartDefinition wing_leaf_r_4_r1 = wing_r.addOrReplaceChild("wing_leaf_r_4_r1", CubeListBuilder.create().texOffs(286, 46).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.0564F, -4.8409F, 4.3498F, -1.2012F, -0.1406F, -0.4275F));

		PartDefinition wing_leaf_r_3_r1 = wing_r.addOrReplaceChild("wing_leaf_r_3_r1", CubeListBuilder.create().texOffs(178, 46).addBox(-11.0F, -1.0F, -4.0F, 22.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(22.1638F, -5.3387F, 7.1217F, -1.2012F, -0.1406F, -0.4275F));

		PartDefinition wing_leaf_r_2_r1 = wing_r.addOrReplaceChild("wing_leaf_r_2_r1", CubeListBuilder.create().texOffs(188, 29).addBox(-14.5F, -1.0F, -5.0F, 29.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.8877F, -11.9237F, 5.9994F, -1.2012F, -0.1406F, -0.4275F));

		PartDefinition wing_leaf_r_1_r1 = wing_r.addOrReplaceChild("wing_leaf_r_1_r1", CubeListBuilder.create().texOffs(422, 29).addBox(-16.0F, -1.0F, -4.5F, 32.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(28.9972F, -19.0375F, 5.123F, -1.2012F, -0.1406F, -0.4275F));

		PartDefinition wing_costa_r_2_r1 = wing_r.addOrReplaceChild("wing_costa_r_2_r1", CubeListBuilder.create().texOffs(74, 2).addBox(-1.0F, -17.2F, -1.0F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(30.4F, -20.4F, 3.6F, -1.847F, -1.4036F, -3.1416F));

		PartDefinition wing_costa_r_1_r1 = wing_r.addOrReplaceChild("wing_costa_r_1_r1", CubeListBuilder.create().texOffs(48, 2).addBox(-1.0F, -17.0F, -1.5F, 2.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(15.6F, -15.4F, 1.0F, -1.8938F, -1.3961F, -3.1416F));

		PartDefinition wing_claw_r_r1 = wing_r.addOrReplaceChild("wing_claw_r_r1", CubeListBuilder.create().texOffs(196, 61).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, -17.4F, 0.3F, -0.384F, 0.0F, 0.3142F));

		PartDefinition wing_forearm_r_r1 = wing_r.addOrReplaceChild("wing_forearm_r_r1", CubeListBuilder.create().texOffs(18, 2).addBox(-1.5F, -18.8F, -1.5F, 3.0F, 20.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -0.1F, 1.6F, 0.0336F, 0.0F, 0.5164F));

		PartDefinition wing_humerus_r_r1 = wing_r.addOrReplaceChild("wing_humerus_r_r1", CubeListBuilder.create().texOffs(468, 2).addBox(-1.5F, -8.8F, -2.0F, 3.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, 0.0F, 0.0F, -1.6093F, -1.3507F, -3.1416F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create(), PartPose.offset(0.0F, 1.7F, 4.5F));

		PartDefinition tail_leaf_tip_r1 = tail_base.addOrReplaceChild("tail_leaf_tip_r1", CubeListBuilder.create().texOffs(474, 46).addBox(-3.0F, -1.0F, -2.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.7F, 14.8F, 0.5616F, -0.7766F, -1.8778F));

		PartDefinition tail_leaf_main_r1 = tail_base.addOrReplaceChild("tail_leaf_main_r1", CubeListBuilder.create().texOffs(330, 46).addBox(-5.5F, -1.0F, -4.0F, 11.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.2F, 18.6F, 0.5616F, -0.7766F, -1.8778F));

		PartDefinition tail_seg_7_r1 = tail_base.addOrReplaceChild("tail_seg_7_r1", CubeListBuilder.create().texOffs(30, 46).addBox(-1.5F, -6.8F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, 28.5F, 1.5708F, -0.2292F, -1.5708F));

		PartDefinition tail_seg_6_r1 = tail_base.addOrReplaceChild("tail_seg_6_r1", CubeListBuilder.create().texOffs(320, 29).addBox(-1.5F, -7.8F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.7F, 31.5F, 1.5708F, -1.1264F, -1.5708F));

		PartDefinition tail_seg_5_r1 = tail_base.addOrReplaceChild("tail_seg_5_r1", CubeListBuilder.create().texOffs(486, 2).addBox(-2.0F, -7.8F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.0F, 28.8F, -1.5708F, -1.1877F, 1.5708F));

		PartDefinition tail_seg_4_r1 = tail_base.addOrReplaceChild("tail_seg_4_r1", CubeListBuilder.create().texOffs(430, 2).addBox(-2.0F, -8.8F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.8F, 22.0F, -1.5708F, -0.5096F, 1.5708F));

		PartDefinition tail_seg_3_r1 = tail_base.addOrReplaceChild("tail_seg_3_r1", CubeListBuilder.create().texOffs(306, 2).addBox(-2.5F, -8.8F, -2.5F, 5.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.6F, 13.8F, -1.5708F, 0.0244F, 1.5708F));

		PartDefinition tail_seg_2_r1 = tail_base.addOrReplaceChild("tail_seg_2_r1", CubeListBuilder.create().texOffs(282, 2).addBox(-2.5F, -9.8F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.8F, 6.0F, -1.5708F, 0.4533F, 1.5708F));

		PartDefinition tail_seg_1_r1 = tail_base.addOrReplaceChild("tail_seg_1_r1", CubeListBuilder.create().texOffs(206, 2).addBox(-3.0F, -9.8F, -3.0F, 6.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, -0.4F, -1.5708F, 0.7188F, 1.5708F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(24, 61).addBox(-2.4F, 14.5F, -7.9F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(340, 61).addBox(0.3F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(350, 61).addBox(-0.9F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(360, 61).addBox(-2.1F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.4F, -16.5F, 4.2F));

		PartDefinition leg_back_l_ankle_r1 = leg_back_l.addOrReplaceChild("leg_back_l_ankle_r1", CubeListBuilder.create().texOffs(372, 46).addBox(-1.5F, -6.1F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 10.1F, -0.2F, 0.6074F, 0.0F, 3.0981F));

		PartDefinition leg_back_l_shin_r1 = leg_back_l.addOrReplaceChild("leg_back_l_shin_r1", CubeListBuilder.create().texOffs(20, 29).addBox(-1.5F, -8.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8F, 5.7F, -5.2F, -0.8469F, -0.0384F, 3.1416F));

		PartDefinition leg_back_l_thigh_r1 = leg_back_l.addOrReplaceChild("leg_back_l_thigh_r1", CubeListBuilder.create().texOffs(234, 2).addBox(-2.0F, -9.6F, -3.0F, 4.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 0.0F, 2.4197F, 0.1471F, 0.0F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(46, 61).addBox(-1.6F, 14.5F, -7.9F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(370, 61).addBox(-1.3F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(380, 61).addBox(-0.1F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(390, 61).addBox(1.1F, 15.5F, -9.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.4F, -16.5F, 4.2F));

		PartDefinition leg_back_r_ankle_r1 = leg_back_r.addOrReplaceChild("leg_back_r_ankle_r1", CubeListBuilder.create().texOffs(388, 46).addBox(-1.5F, -6.1F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 10.1F, -0.2F, 0.6074F, 0.0F, -3.0981F));

		PartDefinition leg_back_r_shin_r1 = leg_back_r.addOrReplaceChild("leg_back_r_shin_r1", CubeListBuilder.create().texOffs(56, 29).addBox(-1.5F, -8.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8F, 5.7F, -5.2F, -0.8469F, 0.0384F, -3.1416F));

		PartDefinition leg_back_r_thigh_r1 = leg_back_r.addOrReplaceChild("leg_back_r_thigh_r1", CubeListBuilder.create().texOffs(258, 2).addBox(-2.0F, -9.6F, -3.0F, 4.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.3F, 0.0F, 2.4197F, -0.1471F, 0.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(SizoLeafwingEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean gliding = entity.isGliding();
		boolean sitting = entity.isInSittingPose();
		boolean gust = entity.getGustTicks() > 0;
		float w = limbSwing * 0.68F;

		if (sitting) {
			addBbPos(this.body, 0.0F, -5.0F, 0.0F);
			addBbDeg(this.body, -18.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, 16.0F, Mth.sin(ageInTicks * 0.08F) * 5.0F, 0.0F);
		} else if (gliding) {
			float soarTremble = Mth.sin(ageInTicks * 0.28F) * 5.5F;
			addBbDeg(this.body, 14.0F, 0.0F, Mth.sin(ageInTicks * 0.10F) * 2.5F);
			addBbDeg(this.wing_l, 0.0F, 8.0F, -32.0F + soarTremble);
			addBbDeg(this.wing_r, 0.0F, -8.0F, 32.0F - soarTremble);
			addBbDeg(this.arm_l, -12.0F, 0.0F, -18.0F);
			addBbDeg(this.arm_r, -12.0F, 0.0F, 18.0F);
			addBbDeg(this.leg_back_l, 32.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 32.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -10.0F, Mth.sin(ageInTicks * 0.14F) * 6.0F, 0.0F);
		} else {
			addBbDeg(this.leg_back_l, Mth.cos(w) * 32.0F * move, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, Mth.cos(w + (float) Math.PI) * 32.0F * move, 0.0F, 0.0F);
			addBbPos(this.body, 0.0F, Math.abs(Mth.sin(w)) * 1.0F * move, 0.0F);
			addBbDeg(this.wing_l, 0.0F, -6.0F, 18.0F - move * 8.0F);
			addBbDeg(this.wing_r, 0.0F, 6.0F, -18.0F + move * 8.0F);
			addBbDeg(this.tail_base, -move * 10.0F, Mth.sin(ageInTicks * 0.09F) * 6.5F, 0.0F);
		}

		if (gust) {
			float clap = Mth.sin(ageInTicks * 0.9F) * 46.0F;
			addBbDeg(this.wing_l, -14.0F, 0.0F, -clap);
			addBbDeg(this.wing_r, -14.0F, 0.0F, clap);
		}

		addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.5F + Mth.sin(ageInTicks * 0.07F) * 1.5F,
				Mth.clamp(netHeadYaw, -45.0F, 45.0F) * 0.6F, 0.0F);
		addBbDeg(this.antler_l, Mth.sin(ageInTicks * 0.08F) * 2.0F, 0.0F, 0.0F);
		addBbDeg(this.antler_r, Mth.sin(ageInTicks * 0.08F) * 2.0F, 0.0F, 0.0F);
	}
}
