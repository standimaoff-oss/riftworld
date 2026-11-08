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
import ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/crystal_gecko.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class CrystalGeckoModel extends FaunaBaseModel<CrystalGeckoEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "crystal_gecko"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tail_base;
	private final ModelPart leg_front_l;
	private final ModelPart leg_back_l;
	private final ModelPart leg_front_r;
	private final ModelPart leg_back_r;

	public CrystalGeckoModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.body.getChild("head");
		this.tail_base = this.body.getChild("tail_base");
		this.leg_front_l = this.root.getChild("leg_front_l");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_front_r = this.root.getChild("leg_front_r");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.head, this.tail_base, this.leg_front_l, this.leg_back_l, this.leg_front_r, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-4.5F, -3.8F, -9.2F, 9.0F, 7.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.5F, 1.0F));

		PartDefinition neck_frill_r_r1 = body.addOrReplaceChild("neck_frill_r_r1", CubeListBuilder.create().texOffs(476, 2).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -1.7F, -10.2F, -0.3142F, -0.2443F, 0.384F));

		PartDefinition neck_frill_l_r1 = body.addOrReplaceChild("neck_frill_l_r1", CubeListBuilder.create().texOffs(458, 2).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -1.7F, -10.2F, -0.3142F, 0.2443F, -0.384F));

		PartDefinition gecko_crys_6_r1 = body.addOrReplaceChild("gecko_crys_6_r1", CubeListBuilder.create().texOffs(174, 45).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.5F, 9.4F, -0.4366F, 0.0F, 0.0F));

		PartDefinition gecko_crys_5_r1 = body.addOrReplaceChild("gecko_crys_5_r1", CubeListBuilder.create().texOffs(204, 31).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.1F, 6.4F, -0.4049F, 0.0F, 0.0F));

		PartDefinition gecko_crys_4_peak_r1 = body.addOrReplaceChild("gecko_crys_4_peak_r1", CubeListBuilder.create().texOffs(286, 45).addBox(-0.5F, -2.2F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.7F, 5.2F, -0.3217F, 0.0F, 0.0F));

		PartDefinition gecko_crys_4_r1 = body.addOrReplaceChild("gecko_crys_4_r1", CubeListBuilder.create().texOffs(138, 31).addBox(-1.0F, -6.0F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.3F, 3.2F, -0.3547F, 0.0F, 0.0F));

		PartDefinition gecko_crys_3_peak_r1 = body.addOrReplaceChild("gecko_crys_3_peak_r1", CubeListBuilder.create().texOffs(276, 45).addBox(-0.5F, -2.2F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.7F, 1.8F, -0.3217F, 0.0F, 0.0F));

		PartDefinition gecko_crys_3_r1 = body.addOrReplaceChild("gecko_crys_3_r1", CubeListBuilder.create().texOffs(494, 2).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.3F, -0.2F, -0.3029F, 0.0F, 0.0F));

		PartDefinition gecko_crys_2_peak_r1 = body.addOrReplaceChild("gecko_crys_2_peak_r1", CubeListBuilder.create().texOffs(266, 45).addBox(-0.5F, -2.2F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.5F, -1.8F, -0.3217F, 0.0F, 0.0F));

		PartDefinition gecko_crys_2_r1 = body.addOrReplaceChild("gecko_crys_2_r1", CubeListBuilder.create().texOffs(124, 31).addBox(-1.0F, -6.0F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.3F, -3.6F, -0.3332F, 0.0F, 0.0F));

		PartDefinition gecko_crys_1_r1 = body.addOrReplaceChild("gecko_crys_1_r1", CubeListBuilder.create().texOffs(118, 45).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.3F, -6.8F, -0.353F, 0.0F, 0.0F));

		PartDefinition neck_joint_r1 = body.addOrReplaceChild("neck_joint_r1", CubeListBuilder.create().texOffs(336, 2).addBox(-4.0F, -3.0F, -2.5F, 8.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.1F, -10.2F, -0.2094F, 0.0F, 0.0F));

		PartDefinition pelvis_taper_r1 = body.addOrReplaceChild("pelvis_taper_r1", CubeListBuilder.create().texOffs(366, 2).addBox(-3.5F, -3.0F, -2.5F, 7.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.1F, 9.6F, 0.1047F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 2).addBox(-5.0F, -4.4F, -7.2F, 10.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(218, 31).addBox(-3.5F, -1.2F, -11.1F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(128, 2).addBox(-3.5F, 1.3F, -10.8F, 7.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(152, 31).addBox(-5.4F, -3.3F, -6.3F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(170, 31).addBox(3.4F, -3.3F, -6.3F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -12.0F));

		PartDefinition head_horn_r_r1 = head.addOrReplaceChild("head_horn_r_r1", CubeListBuilder.create().texOffs(146, 45).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -3.5F, -0.2F, -2.3044F, -0.588F, -3.1416F));

		PartDefinition brow_ridge_r_r1 = head.addOrReplaceChild("brow_ridge_r_r1", CubeListBuilder.create().texOffs(268, 31).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -4.3F, -3.8F, -0.1047F, 0.0F, 0.1396F));

		PartDefinition head_horn_l_r1 = head.addOrReplaceChild("head_horn_l_r1", CubeListBuilder.create().texOffs(132, 45).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -3.5F, -0.2F, -2.3044F, 0.588F, 3.1416F));

		PartDefinition brow_ridge_l_r1 = head.addOrReplaceChild("brow_ridge_l_r1", CubeListBuilder.create().texOffs(246, 31).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -4.3F, -3.8F, -0.1047F, 0.0F, -0.1396F));

		PartDefinition snout_bridge_r1 = head.addOrReplaceChild("snout_bridge_r1", CubeListBuilder.create().texOffs(2, 31).addBox(-3.0F, -5.8F, -1.5F, 6.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.7F, -5.8F, 2.0852F, 0.0F, 0.0F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create(), PartPose.offset(0.0F, 0.1F, 11.2F));

		PartDefinition tail_coil_12_r1 = tail_base.addOrReplaceChild("tail_coil_12_r1", CubeListBuilder.create().texOffs(160, 45).addBox(-1.0F, -3.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.0F, 7.2F, -1.5708F, 0.7854F, 1.5708F));

		PartDefinition tail_coil_11_r1 = tail_base.addOrReplaceChild("tail_coil_11_r1", CubeListBuilder.create().texOffs(102, 45).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.4F, 7.4F, 1.5708F, 1.512F, -1.5708F));

		PartDefinition tail_coil_10_r1 = tail_base.addOrReplaceChild("tail_coil_10_r1", CubeListBuilder.create().texOffs(188, 31).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.0F, 10.2F, 1.5708F, 0.7484F, -1.5708F));

		PartDefinition tail_coil_9_r1 = tail_base.addOrReplaceChild("tail_coil_9_r1", CubeListBuilder.create().texOffs(106, 31).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.2F, 14.2F, 1.5708F, 0.05F, -1.5708F));

		PartDefinition tail_coil_8_r1 = tail_base.addOrReplaceChild("tail_coil_8_r1", CubeListBuilder.create().texOffs(86, 31).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -13.0F, 17.8F, 1.5708F, -0.5486F, -1.5708F));

		PartDefinition tail_coil_7_r1 = tail_base.addOrReplaceChild("tail_coil_7_r1", CubeListBuilder.create().texOffs(66, 31).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.2F, 19.8F, 1.5708F, -1.0863F, -1.5708F));

		PartDefinition tail_coil_6_r1 = tail_base.addOrReplaceChild("tail_coil_6_r1", CubeListBuilder.create().texOffs(46, 31).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 19.6F, -1.5708F, -1.5232F, 1.5708F));

		PartDefinition tail_coil_5_r1 = tail_base.addOrReplaceChild("tail_coil_5_r1", CubeListBuilder.create().texOffs(24, 31).addBox(-2.5F, -5.0F, -2.0F, 5.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.2F, 17.4F, -1.5708F, -1.046F, 1.5708F));

		PartDefinition tail_coil_4_r1 = tail_base.addOrReplaceChild("tail_coil_4_r1", CubeListBuilder.create().texOffs(394, 2).addBox(-2.5F, -5.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.2F, 13.6F, -1.5708F, -0.5633F, 1.5708F));

		PartDefinition tail_coil_3_r1 = tail_base.addOrReplaceChild("tail_coil_3_r1", CubeListBuilder.create().texOffs(192, 2).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 9.0F, -1.5708F, -0.1722F, 1.5708F));

		PartDefinition tail_coil_2_r1 = tail_base.addOrReplaceChild("tail_coil_2_r1", CubeListBuilder.create().texOffs(166, 2).addBox(-3.0F, -6.0F, -2.5F, 6.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.4F, 4.3F, -1.5708F, 0.127F, 1.5708F));

		PartDefinition tail_coil_1_r1 = tail_base.addOrReplaceChild("tail_coil_1_r1", CubeListBuilder.create().texOffs(100, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.2F, -1.5708F, 0.3016F, 1.5708F));

		PartDefinition leg_front_l = root.addOrReplaceChild("leg_front_l", CubeListBuilder.create().texOffs(186, 45).addBox(-5.6F, 6.8F, -5.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.2F, -8.8F, -5.8F));

		PartDefinition leg_front_l_toe_4_r1 = leg_front_l.addOrReplaceChild("leg_front_l_toe_4_r1", CubeListBuilder.create().texOffs(350, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8174F, 7.8F, -5.6997F, 0.0F, 0.8727F, 0.0F));

		PartDefinition leg_front_l_toe_3_r1 = leg_front_l.addOrReplaceChild("leg_front_l_toe_3_r1", CubeListBuilder.create().texOffs(330, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.7577F, 7.8F, -7.0373F, 0.0F, 0.2793F, 0.0F));

		PartDefinition leg_front_l_toe_2_r1 = leg_front_l.addOrReplaceChild("leg_front_l_toe_2_r1", CubeListBuilder.create().texOffs(310, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.3021F, 7.8F, -6.9944F, 0.0F, -0.3142F, 0.0F));

		PartDefinition leg_front_l_toe_1_r1 = leg_front_l.addOrReplaceChild("leg_front_l_toe_1_r1", CubeListBuilder.create().texOffs(290, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2904F, 7.8F, -5.5858F, 0.0F, -0.9076F, 0.0F));

		PartDefinition leg_front_l_lower_r1 = leg_front_l.addOrReplaceChild("leg_front_l_lower_r1", CubeListBuilder.create().texOffs(236, 2).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 2.0F, 1.4F, 0.6138F, 0.0F, 2.9551F));

		PartDefinition leg_front_l_upper_r1 = leg_front_l.addOrReplaceChild("leg_front_l_upper_r1", CubeListBuilder.create().texOffs(216, 2).addBox(-2.0F, -6.6F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.1766F, 1.2754F, 3.1416F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(206, 45).addBox(-6.8F, 6.8F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -8.8F, 7.6F));

		PartDefinition leg_back_l_toe_4_r1 = leg_back_l.addOrReplaceChild("leg_back_l_toe_4_r1", CubeListBuilder.create().texOffs(430, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.282F, 7.8F, -1.3486F, 0.0F, 0.9774F, 0.0F));

		PartDefinition leg_back_l_toe_3_r1 = leg_back_l.addOrReplaceChild("leg_back_l_toe_3_r1", CubeListBuilder.create().texOffs(410, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.3733F, 7.8F, -2.8942F, 0.0F, 0.384F, 0.0F));

		PartDefinition leg_back_l_toe_2_r1 = leg_back_l.addOrReplaceChild("leg_back_l_toe_2_r1", CubeListBuilder.create().texOffs(390, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.9268F, 7.8F, -3.1082F, 0.0F, -0.2094F, 0.0F));

		PartDefinition leg_back_l_toe_1_r1 = leg_back_l.addOrReplaceChild("leg_back_l_toe_1_r1", CubeListBuilder.create().texOffs(370, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7788F, 7.8F, -1.9176F, 0.0F, -0.8029F, 0.0F));

		PartDefinition leg_back_l_lower_r1 = leg_back_l.addOrReplaceChild("leg_back_l_lower_r1", CubeListBuilder.create().texOffs(256, 2).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 1.6F, -1.4F, -0.4842F, 0.0F, -3.1065F));

		PartDefinition leg_back_l_upper_r1 = leg_back_l.addOrReplaceChild("leg_back_l_upper_r1", CubeListBuilder.create().texOffs(418, 2).addBox(-2.0F, -5.6F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.892F, 1.2754F, 0.0F));

		PartDefinition leg_front_r = root.addOrReplaceChild("leg_front_r", CubeListBuilder.create().texOffs(226, 45).addBox(1.6F, 6.8F, -5.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.2F, -8.8F, -5.8F));

		PartDefinition leg_front_r_toe_4_r1 = leg_front_r.addOrReplaceChild("leg_front_r_toe_4_r1", CubeListBuilder.create().texOffs(2, 45).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.8174F, 7.8F, -5.6997F, 0.0F, -0.8727F, 0.0F));

		PartDefinition leg_front_r_toe_3_r1 = leg_front_r.addOrReplaceChild("leg_front_r_toe_3_r1", CubeListBuilder.create().texOffs(490, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.7577F, 7.8F, -7.0373F, 0.0F, -0.2793F, 0.0F));

		PartDefinition leg_front_r_toe_2_r1 = leg_front_r.addOrReplaceChild("leg_front_r_toe_2_r1", CubeListBuilder.create().texOffs(470, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.3021F, 7.8F, -6.9944F, 0.0F, 0.3142F, 0.0F));

		PartDefinition leg_front_r_toe_1_r1 = leg_front_r.addOrReplaceChild("leg_front_r_toe_1_r1", CubeListBuilder.create().texOffs(450, 31).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2904F, 7.8F, -5.5858F, 0.0F, 0.9076F, 0.0F));

		PartDefinition leg_front_r_lower_r1 = leg_front_r.addOrReplaceChild("leg_front_r_lower_r1", CubeListBuilder.create().texOffs(296, 2).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 2.0F, 1.4F, 0.6138F, 0.0F, -2.9551F));

		PartDefinition leg_front_r_upper_r1 = leg_front_r.addOrReplaceChild("leg_front_r_upper_r1", CubeListBuilder.create().texOffs(276, 2).addBox(-2.0F, -6.6F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.1766F, -1.2754F, -3.1416F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(246, 45).addBox(2.8F, 6.8F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -8.8F, 7.6F));

		PartDefinition leg_back_r_toe_4_r1 = leg_back_r.addOrReplaceChild("leg_back_r_toe_4_r1", CubeListBuilder.create().texOffs(82, 45).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.282F, 7.8F, -1.3486F, 0.0F, -0.9774F, 0.0F));

		PartDefinition leg_back_r_toe_3_r1 = leg_back_r.addOrReplaceChild("leg_back_r_toe_3_r1", CubeListBuilder.create().texOffs(62, 45).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.3733F, 7.8F, -2.8942F, 0.0F, -0.384F, 0.0F));

		PartDefinition leg_back_r_toe_2_r1 = leg_back_r.addOrReplaceChild("leg_back_r_toe_2_r1", CubeListBuilder.create().texOffs(42, 45).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.9268F, 7.8F, -3.1082F, 0.0F, 0.2094F, 0.0F));

		PartDefinition leg_back_r_toe_1_r1 = leg_back_r.addOrReplaceChild("leg_back_r_toe_1_r1", CubeListBuilder.create().texOffs(22, 45).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7788F, 7.8F, -1.9176F, 0.0F, 0.8029F, 0.0F));

		PartDefinition leg_back_r_lower_r1 = leg_back_r.addOrReplaceChild("leg_back_r_lower_r1", CubeListBuilder.create().texOffs(316, 2).addBox(-2.0F, -7.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 1.6F, -1.4F, -0.4842F, 0.0F, 3.1065F));

		PartDefinition leg_back_r_upper_r1 = leg_back_r.addOrReplaceChild("leg_back_r_upper_r1", CubeListBuilder.create().texOffs(438, 2).addBox(-2.0F, -5.6F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.892F, -1.2754F, 0.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(CrystalGeckoEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		this.renderAlpha = entity.isCamouflaged() ? 0.36F : 0.96F;

		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean climbing = entity.isClimbingWall();
		boolean whip = entity.getTailWhipTicks() > 0;
		float w = limbSwing * 0.92F;

		if (climbing) {
			addBbDeg(this.root, 72.0F, 0.0F, 0.0F);
			addBbPos(this.root, 0.0F, 4.0F, -3.0F);
		}

		// Reptilian sprawling crawl & lateral S-body wave
		float bodyWave = Mth.sin(w) * 10.0F * move;
		addBbDeg(this.body, 0.0F, bodyWave, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.45F,
				Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.55F - bodyWave * 0.7F, 0.0F);

		float legA = Mth.cos(w) * 30.0F * move;
		float legB = Mth.cos(w + (float) Math.PI) * 30.0F * move;
		addBbDeg(this.leg_front_l, 0.0F, legA, -Math.max(0.0F, Mth.sin(w)) * 14.0F * move);
		addBbDeg(this.leg_back_r, 0.0F, -legA, Math.max(0.0F, Mth.sin(w)) * 14.0F * move);
		addBbDeg(this.leg_front_r, 0.0F, -legB, Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 14.0F * move);
		addBbDeg(this.leg_back_l, 0.0F, legB, -Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 14.0F * move);

		float whipSwing = whip ? Mth.sin(ageInTicks * 1.15F) * 48.0F : 0.0F;
		addBbDeg(this.tail_base, Mth.sin(ageInTicks * 0.07F) * 2.0F,
				-bodyWave * 1.6F + Mth.sin(ageInTicks * 0.09F) * 7.0F + whipSwing, 0.0F);
	}
}
