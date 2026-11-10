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
import ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/porcelain_crane.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class PorcelainCraneModel extends FaunaBaseModel<PorcelainCraneEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "porcelain_crane"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart neck_base;
	private final ModelPart head;
	private final ModelPart wing_l;
	private final ModelPart wing_r;
	private final ModelPart tail_base;
	private final ModelPart leg_l;
	private final ModelPart leg_r;

	public PorcelainCraneModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.neck_base = this.body.getChild("neck_base");
		this.head = this.neck_base.getChild("head");
		this.wing_l = this.body.getChild("wing_l");
		this.wing_r = this.body.getChild("wing_r");
		this.tail_base = this.body.getChild("tail_base");
		this.leg_l = this.root.getChild("leg_l");
		this.leg_r = this.root.getChild("leg_r");
		captureRestPoses(this.root, this.body, this.neck_base, this.head, this.wing_l, this.wing_r, this.tail_base, this.leg_l, this.leg_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -36.0F, 0.5F));

		PartDefinition torso_flank_r_r1 = body.addOrReplaceChild("torso_flank_r_r1", CubeListBuilder.create().texOffs(122, 2).addBox(-1.5F, -5.0F, -9.0F, 3.0F, 10.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.2F, -1.2F, 0.0F, -0.1047F, 0.0524F, 0.1047F));

		PartDefinition torso_flank_l_r1 = body.addOrReplaceChild("torso_flank_l_r1", CubeListBuilder.create().texOffs(76, 2).addBox(-1.5F, -5.0F, -9.0F, 3.0F, 10.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.2F, -1.2F, 0.0F, -0.1047F, -0.0524F, -0.1047F));

		PartDefinition rump_cone_r1 = body.addOrReplaceChild("rump_cone_r1", CubeListBuilder.create().texOffs(284, 41).addBox(-4.0F, -4.0F, -3.5F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, 11.3F, 0.2793F, 0.0F, 0.0F));

		PartDefinition belly_under_r1 = body.addOrReplaceChild("belly_under_r1", CubeListBuilder.create().texOffs(308, 2).addBox(-4.5F, -2.5F, -8.0F, 9.0F, 5.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.4F, 0.0F, -0.1396F, 0.0F, 0.0F));

		PartDefinition dorsal_rear_r1 = body.addOrReplaceChild("dorsal_rear_r1", CubeListBuilder.create().texOffs(38, 41).addBox(-4.5F, -2.0F, -6.0F, 9.0F, 4.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.6F, 6.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition dorsal_front_r1 = body.addOrReplaceChild("dorsal_front_r1", CubeListBuilder.create().texOffs(156, 41).addBox(-5.0F, -2.0F, -5.5F, 10.0F, 4.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.4F, -3.7F, 0.0524F, 0.0F, 0.0F));

		PartDefinition breast_lower_r1 = body.addOrReplaceChild("breast_lower_r1", CubeListBuilder.create().texOffs(202, 41).addBox(-5.0F, -3.0F, -4.5F, 10.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.4F, -8.7F, -0.4189F, 0.0F, 0.0F));

		PartDefinition breast_mid_r1 = body.addOrReplaceChild("breast_mid_r1", CubeListBuilder.create().texOffs(394, 41).addBox(-5.5F, -3.5F, -3.5F, 11.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -10.9F, -0.0698F, 0.0F, 0.0F));

		PartDefinition breast_top_r1 = body.addOrReplaceChild("breast_top_r1", CubeListBuilder.create().texOffs(244, 41).addBox(-5.0F, -3.5F, -4.0F, 10.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.4F, -9.1F, 0.2793F, 0.0F, 0.0F));

		PartDefinition torso_core_r1 = body.addOrReplaceChild("torso_core_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-6.5F, -6.5F, -11.0F, 13.0F, 13.0F, 22.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, -0.1047F, 0.0F, 0.0F));

		PartDefinition neck_base = body.addOrReplaceChild("neck_base", CubeListBuilder.create(), PartPose.offset(0.0F, -7.0F, -8.7F));

		PartDefinition neck_seg_6_r1 = neck_base.addOrReplaceChild("neck_seg_6_r1", CubeListBuilder.create().texOffs(226, 63).addBox(-2.0F, -4.7F, -2.5F, 4.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -26.2F, 1.2F, 0.7854F, 0.0F, 0.0F));

		PartDefinition neck_seg_5_r1 = neck_base.addOrReplaceChild("neck_seg_5_r1", CubeListBuilder.create().texOffs(72, 63).addBox(-2.0F, -5.7F, -2.5F, 4.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -21.8F, 2.8F, 0.3488F, 0.0F, 0.0F));

		PartDefinition neck_seg_4_r1 = neck_base.addOrReplaceChild("neck_seg_4_r1", CubeListBuilder.create().texOffs(20, 63).addBox(-2.5F, -6.7F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -16.2F, 1.2F, -0.2783F, 0.0F, 0.0F));

		PartDefinition neck_seg_3_r1 = neck_base.addOrReplaceChild("neck_seg_3_r1", CubeListBuilder.create().texOffs(350, 41).addBox(-2.5F, -7.7F, -3.0F, 5.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.2F, -2.0F, -0.49F, 0.0F, 0.0F));

		PartDefinition neck_seg_2_r1 = neck_base.addOrReplaceChild("neck_seg_2_r1", CubeListBuilder.create().texOffs(434, 41).addBox(-3.0F, -6.7F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.4F, -2.6F, -0.1031F, 0.0F, 0.0F));

		PartDefinition neck_seg_1_r1 = neck_base.addOrReplaceChild("neck_seg_1_r1", CubeListBuilder.create().texOffs(318, 41).addBox(-3.5F, -6.7F, -3.5F, 7.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8F, 0.0F, 0.4636F, 0.0F, 0.0F));

		PartDefinition head = neck_base.addOrReplaceChild("head", CubeListBuilder.create().texOffs(150, 81).addBox(-2.75F, -1.9F, -4.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(160, 81).addBox(1.75F, -1.9F, -4.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -28.6F, -1.2F));

		PartDefinition beak_lower_r1 = head.addOrReplaceChild("beak_lower_r1", CubeListBuilder.create().texOffs(248, 63).addBox(-1.0F, -9.4F, -0.5F, 2.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -6.0F, 1.6263F, 0.0F, 0.0F));

		PartDefinition beak_upper_tip_r1 = head.addOrReplaceChild("beak_upper_tip_r1", CubeListBuilder.create().texOffs(84, 81).addBox(-1.0F, -4.4F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -15.4F, 1.7016F, 0.0F, 0.0F));

		PartDefinition beak_upper_mid_r1 = head.addOrReplaceChild("beak_upper_mid_r1", CubeListBuilder.create().texOffs(452, 63).addBox(-1.0F, -5.4F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4F, -10.7F, 1.6902F, 0.0F, 0.0F));

		PartDefinition beak_upper_base_r1 = head.addOrReplaceChild("beak_upper_base_r1", CubeListBuilder.create().texOffs(438, 63).addBox(-1.5F, -5.2F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -6.0F, 1.6902F, 0.0F, 0.0F));

		PartDefinition head_lore_r1 = head.addOrReplaceChild("head_lore_r1", CubeListBuilder.create().texOffs(2, 81).addBox(-2.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -5.4F, -0.1047F, 0.0F, 0.0F));

		PartDefinition head_crown_r1 = head.addOrReplaceChild("head_crown_r1", CubeListBuilder.create().texOffs(374, 63).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.8F, -1.6F, 0.1047F, 0.0F, 0.0F));

		PartDefinition head_cranium_r1 = head.addOrReplaceChild("head_cranium_r1", CubeListBuilder.create().texOffs(44, 63).addBox(-2.5F, -2.5F, -3.5F, 5.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.6F, -2.0F, -0.0698F, 0.0F, 0.0F));

		PartDefinition wing_l = body.addOrReplaceChild("wing_l", CubeListBuilder.create(), PartPose.offset(-6.8F, -4.5F, -4.5F));

		PartDefinition wing_prim_l_4_r1 = wing_l.addOrReplaceChild("wing_prim_l_4_r1", CubeListBuilder.create().texOffs(276, 2).addBox(-1.0F, -16.8F, -2.0F, 2.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.05F, 6.9F, 8.0F, -1.4511F, 0.5269F, 1.6902F));

		PartDefinition wing_prim_l_3_r1 = wing_l.addOrReplaceChild("wing_prim_l_3_r1", CubeListBuilder.create().texOffs(186, 2).addBox(-1.0F, -17.8F, -2.5F, 2.0F, 19.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.35F, 5.5F, 8.2F, -1.4658F, 0.4838F, 1.6902F));

		PartDefinition wing_prim_l_2_r1 = wing_l.addOrReplaceChild("wing_prim_l_2_r1", CubeListBuilder.create().texOffs(168, 2).addBox(-1.0F, -17.8F, -2.5F, 2.0F, 19.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.55F, 4.0F, 8.0F, -1.4752F, 0.451F, 1.6902F));

		PartDefinition wing_prim_l_1_r1 = wing_l.addOrReplaceChild("wing_prim_l_1_r1", CubeListBuilder.create().texOffs(240, 2).addBox(-1.0F, -16.8F, -2.5F, 2.0F, 18.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.45F, 2.5F, 7.5F, -1.4931F, 0.4275F, 1.6902F));

		PartDefinition wing_mantle_l_2_r1 = wing_l.addOrReplaceChild("wing_mantle_l_2_r1", CubeListBuilder.create().texOffs(290, 63).addBox(-6.0F, -1.0F, -3.5F, 12.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.57F, 4.3F, 2.8F, -2.9571F, 1.1186F, 1.8783F));

		PartDefinition wing_mantle_l_1_r1 = wing_l.addOrReplaceChild("wing_mantle_l_1_r1", CubeListBuilder.create().texOffs(94, 63).addBox(-7.5F, -1.0F, -4.5F, 15.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 1.3F, 4.2F, -2.9571F, 1.1186F, 1.8783F));

		PartDefinition wing_r = body.addOrReplaceChild("wing_r", CubeListBuilder.create(), PartPose.offset(6.8F, -4.5F, -4.5F));

		PartDefinition wing_prim_r_4_r1 = wing_r.addOrReplaceChild("wing_prim_r_4_r1", CubeListBuilder.create().texOffs(292, 2).addBox(-1.0F, -16.8F, -2.0F, 2.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.05F, 6.9F, 8.0F, -1.4511F, -0.5269F, -1.6902F));

		PartDefinition wing_prim_r_3_r1 = wing_r.addOrReplaceChild("wing_prim_r_3_r1", CubeListBuilder.create().texOffs(222, 2).addBox(-1.0F, -17.8F, -2.5F, 2.0F, 19.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.35F, 5.5F, 8.2F, -1.4658F, -0.4838F, -1.6902F));

		PartDefinition wing_prim_r_2_r1 = wing_r.addOrReplaceChild("wing_prim_r_2_r1", CubeListBuilder.create().texOffs(204, 2).addBox(-1.0F, -17.8F, -2.5F, 2.0F, 19.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.55F, 4.0F, 8.0F, -1.4752F, -0.451F, -1.6902F));

		PartDefinition wing_prim_r_1_r1 = wing_r.addOrReplaceChild("wing_prim_r_1_r1", CubeListBuilder.create().texOffs(258, 2).addBox(-1.0F, -16.8F, -2.5F, 2.0F, 18.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.45F, 2.5F, 7.5F, -1.4931F, -0.4275F, -1.6902F));

		PartDefinition wing_mantle_r_2_r1 = wing_r.addOrReplaceChild("wing_mantle_r_2_r1", CubeListBuilder.create().texOffs(332, 63).addBox(-6.0F, -1.0F, -3.5F, 12.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.57F, 4.3F, 2.8F, -2.9571F, -1.1186F, -1.8783F));

		PartDefinition wing_mantle_r_1_r1 = wing_r.addOrReplaceChild("wing_mantle_r_1_r1", CubeListBuilder.create().texOffs(146, 63).addBox(-7.5F, -1.0F, -4.5F, 15.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 1.3F, 4.2F, -2.9571F, -1.1186F, -1.8783F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create(), PartPose.offset(0.0F, -0.2F, 12.0F));

		PartDefinition tail_low_r_2_r1 = tail_base.addOrReplaceChild("tail_low_r_2_r1", CubeListBuilder.create().texOffs(416, 2).addBox(-1.0F, -12.6F, -2.5F, 2.0F, 14.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8F, 2.6F, -0.3F, -0.8472F, -0.96F, -2.7049F));

		PartDefinition tail_low_r_1_r1 = tail_base.addOrReplaceChild("tail_low_r_1_r1", CubeListBuilder.create().texOffs(380, 2).addBox(-1.0F, -14.6F, -2.5F, 2.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 2.4F, 0.1F, -0.7495F, -0.8522F, -2.6832F));

		PartDefinition tail_mid_r_2_r1 = tail_base.addOrReplaceChild("tail_mid_r_2_r1", CubeListBuilder.create().texOffs(20, 41).addBox(-1.0F, -11.6F, -2.5F, 2.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6F, 1.0F, 0.5F, -1.6901F, -0.6212F, -2.2143F));

		PartDefinition tail_mid_r_1_r1 = tail_base.addOrReplaceChild("tail_mid_r_1_r1", CubeListBuilder.create().texOffs(2, 41).addBox(-1.0F, -11.6F, -2.5F, 2.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 0.4F, 1.0F, -1.5646F, -0.3718F, -2.3562F));

		PartDefinition tail_up_r_2_r1 = tail_base.addOrReplaceChild("tail_up_r_2_r1", CubeListBuilder.create().texOffs(138, 41).addBox(-1.0F, -9.6F, -2.5F, 2.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -0.6F, 0.5F, -2.2808F, -0.0664F, -2.2849F));

		PartDefinition tail_up_r_1_r1 = tail_base.addOrReplaceChild("tail_up_r_1_r1", CubeListBuilder.create().texOffs(120, 41).addBox(-1.0F, -9.6F, -2.5F, 2.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -1.2F, 0.7F, -2.2224F, 0.1964F, -2.4275F));

		PartDefinition tail_low_l_2_r1 = tail_base.addOrReplaceChild("tail_low_l_2_r1", CubeListBuilder.create().texOffs(398, 2).addBox(-1.0F, -12.6F, -2.5F, 2.0F, 14.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.8F, 2.6F, -0.3F, -0.8472F, 0.96F, 2.7049F));

		PartDefinition tail_low_l_1_r1 = tail_base.addOrReplaceChild("tail_low_l_1_r1", CubeListBuilder.create().texOffs(362, 2).addBox(-1.0F, -14.6F, -2.5F, 2.0F, 16.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, 2.4F, 0.1F, -0.7495F, 0.8522F, 2.6832F));

		PartDefinition tail_mid_l_2_r1 = tail_base.addOrReplaceChild("tail_mid_l_2_r1", CubeListBuilder.create().texOffs(494, 2).addBox(-1.0F, -11.6F, -2.5F, 2.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, 1.0F, 0.5F, -1.6901F, 0.6212F, 2.2143F));

		PartDefinition tail_mid_l_1_r1 = tail_base.addOrReplaceChild("tail_mid_l_1_r1", CubeListBuilder.create().texOffs(476, 2).addBox(-1.0F, -11.6F, -2.5F, 2.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, 0.4F, 1.0F, -1.5646F, 0.3718F, 2.3562F));

		PartDefinition tail_up_l_2_r1 = tail_base.addOrReplaceChild("tail_up_l_2_r1", CubeListBuilder.create().texOffs(102, 41).addBox(-1.0F, -9.6F, -2.5F, 2.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -0.6F, 0.5F, -2.2808F, 0.0664F, 2.2849F));

		PartDefinition tail_up_l_1_r1 = tail_base.addOrReplaceChild("tail_up_l_1_r1", CubeListBuilder.create().texOffs(84, 41).addBox(-1.0F, -9.6F, -2.5F, 2.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -1.2F, 0.7F, -2.2224F, -0.1964F, 2.4275F));

		PartDefinition tail_low_center_r1 = tail_base.addOrReplaceChild("tail_low_center_r1", CubeListBuilder.create().texOffs(458, 2).addBox(-2.5F, -14.6F, -1.0F, 5.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.4F, 0.3F, -0.4759F, 0.0F, 3.1416F));

		PartDefinition tail_mid_center_r1 = tail_base.addOrReplaceChild("tail_mid_center_r1", CubeListBuilder.create().texOffs(376, 41).addBox(-2.5F, -11.6F, -1.0F, 5.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 1.0F, -1.3734F, 0.0F, 3.1416F));

		PartDefinition tail_up_center_r1 = tail_base.addOrReplaceChild("tail_up_center_r1", CubeListBuilder.create().texOffs(2, 63).addBox(-2.5F, -10.6F, -1.0F, 5.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.4F, 0.7F, -2.2606F, 0.0F, 3.1416F));

		PartDefinition tail_pedestal_r1 = tail_base.addOrReplaceChild("tail_pedestal_r1", CubeListBuilder.create().texOffs(198, 63).addBox(-3.5F, -3.0F, -2.5F, 7.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.7F, 0.2443F, 0.0F, 0.0F));

		PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(398, 63).addBox(-2.0F, 9.0F, -1.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(434, 2).addBox(-1.0F, 12.0F, 0.0F, 2.0F, 17.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(94, 81).addBox(-1.5F, 28.0F, -0.7F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(464, 63).addBox(-0.5F, 29.5F, -5.6F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(126, 81).addBox(-0.5F, 29.5F, 1.3F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.6F, -30.5F, 0.0F));

		PartDefinition toe_outer_l_r1 = leg_l.addOrReplaceChild("toe_outer_l_r1", CubeListBuilder.create().texOffs(36, 81).addBox(-1.4F, -0.5F, -4.7F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, 30.0F, 0.2F, 0.0F, 0.4189F, 0.0F));

		PartDefinition toe_inner_l_r1 = leg_l.addOrReplaceChild("toe_inner_l_r1", CubeListBuilder.create().texOffs(20, 81).addBox(0.4F, -0.5F, -4.7F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, 30.0F, 0.2F, 0.0F, -0.4189F, 0.0F));

		PartDefinition tibia_upper_l_r1 = leg_l.addOrReplaceChild("tibia_upper_l_r1", CubeListBuilder.create().texOffs(258, 63).addBox(-1.5F, -6.2F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.3F, 0.7F, -0.0526F, 0.0F, 3.1416F));

		PartDefinition thigh_cuff_l_r1 = leg_l.addOrReplaceChild("thigh_cuff_l_r1", CubeListBuilder.create().texOffs(462, 41).addBox(-2.5F, -6.5F, -3.0F, 5.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, -0.2F, -0.1599F, 0.0F, 3.1416F));

		PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(418, 63).addBox(-2.0F, 9.0F, -1.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(446, 2).addBox(-1.0F, 12.0F, 0.0F, 2.0F, 17.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(110, 81).addBox(-1.5F, 28.0F, -0.7F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(482, 63).addBox(-0.5F, 29.5F, -5.6F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(138, 81).addBox(-0.5F, 29.5F, 1.3F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.6F, -30.5F, 0.0F));

		PartDefinition toe_outer_r_r1 = leg_r.addOrReplaceChild("toe_outer_r_r1", CubeListBuilder.create().texOffs(68, 81).addBox(0.4F, -0.5F, -4.7F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, 30.0F, 0.2F, 0.0F, -0.4189F, 0.0F));

		PartDefinition toe_inner_r_r1 = leg_r.addOrReplaceChild("toe_inner_r_r1", CubeListBuilder.create().texOffs(52, 81).addBox(-1.4F, -0.5F, -4.7F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, 30.0F, 0.2F, 0.0F, 0.4189F, 0.0F));

		PartDefinition tibia_upper_r_r1 = leg_r.addOrReplaceChild("tibia_upper_r_r1", CubeListBuilder.create().texOffs(274, 63).addBox(-1.5F, -6.2F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.3F, 0.7F, -0.0526F, 0.0F, -3.1416F));

		PartDefinition thigh_cuff_r_r1 = leg_r.addOrReplaceChild("thigh_cuff_r_r1", CubeListBuilder.create().texOffs(488, 41).addBox(-2.5F, -6.5F, -3.0F, 5.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, -0.2F, -0.1599F, 0.0F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(PorcelainCraneEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		float yaw = Mth.clamp(netHeadYaw, -50.0F, 50.0F);
		float pitch = Mth.clamp(headPitch, -30.0F, 30.0F);
		float dance = entity.getDanceProgress(pt);
		float flutter = entity.getFlutterProgress(pt);
		float breath = Mth.sin(ageInTicks * 0.07F);

		// Graceful crane stride
		float w = limbSwing * 0.62F;
		float legSwingL = Mth.cos(w) * 34.0F * move * (1.0F - 0.65F * flutter);
		float legSwingR = Mth.cos(w + (float) Math.PI) * 34.0F * move * (1.0F - 0.65F * flutter);
		addBbDeg(this.leg_l, legSwingL + flutter * 22.0F, 0.0F, 0.0F);
		addBbDeg(this.leg_r, legSwingR + flutter * 22.0F, 0.0F, 0.0F);
		addBbPos(this.body, 0.0F, Math.abs(Mth.sin(w)) * 1.1F * move, 0.0F);

		// S-neck idle poise, walking nod, and porcelain flower bow dance
		float walkNod = Mth.cos(w * 2.0F) * 4.5F * move;
		float bowNeck = dance * 42.0F;
		float bowHead = -dance * 28.0F + Mth.sin(ageInTicks * 0.35F) * 6.0F * dance;
		addBbDeg(this.neck_base, -pitch * 0.35F + breath * 1.6F + walkNod + bowNeck, yaw * 0.45F, 0.0F);
		addBbDeg(this.head, -pitch * 0.45F - walkNod * 0.7F + bowHead, yaw * 0.45F, 0.0F);

		// Wings: folded at rest, graceful half-fan in dance, broad crane beats in startled flutter
		float flap = Mth.sin(ageInTicks * 0.58F) * 44.0F * flutter;
		float danceSpread = dance * 26.0F;
		addBbDeg(this.wing_l, dance * -10.0F, danceSpread * 0.4F, -(danceSpread + flap + flutter * 24.0F));
		addBbDeg(this.wing_r, dance * -10.0F, -danceSpread * 0.4F, (danceSpread + flap + flutter * 24.0F));
		addBbDeg(this.tail_base, breath * 1.8F - dance * 14.0F, Mth.sin(ageInTicks * 0.08F) * 3.0F, 0.0F);
	}
}
