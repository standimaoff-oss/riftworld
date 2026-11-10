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
import ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/faceted_owl.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class FacetedOwlModel extends FaunaBaseModel<FacetedOwlEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "faceted_owl"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart wing_l;
	private final ModelPart wing_r;
	private final ModelPart tail;
	private final ModelPart head;
	private final ModelPart leg_l;
	private final ModelPart leg_r;

	public FacetedOwlModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.wing_l = this.body.getChild("wing_l");
		this.wing_r = this.body.getChild("wing_r");
		this.tail = this.body.getChild("tail");
		this.head = this.body.getChild("head");
		this.leg_l = this.root.getChild("leg_l");
		this.leg_r = this.root.getChild("leg_r");
		captureRestPoses(this.root, this.body, this.wing_l, this.wing_r, this.tail, this.head, this.leg_l, this.leg_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-5.5F, -8.7F, -5.1F, 11.0F, 13.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(260, 2).addBox(-4.5F, -7.7F, -6.2F, 9.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, -0.5F));

		PartDefinition belly_diamond_low_r_r1 = body.addOrReplaceChild("belly_diamond_low_r_r1", CubeListBuilder.create().texOffs(434, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6F, 2.6F, -3.9F, 1.9911F, -0.0676F, -0.7386F));

		PartDefinition belly_diamond_side_r_r1 = body.addOrReplaceChild("belly_diamond_side_r_r1", CubeListBuilder.create().texOffs(414, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8F, -1.4F, -5.3F, 1.8869F, -0.1236F, -0.7737F));

		PartDefinition chest_diamond_outer_r_r1 = body.addOrReplaceChild("chest_diamond_outer_r_r1", CubeListBuilder.create().texOffs(54, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -6.0F, -4.3F, 1.9377F, -0.493F, -0.9313F));

		PartDefinition chest_diamond_inner_r_r1 = body.addOrReplaceChild("chest_diamond_inner_r_r1", CubeListBuilder.create().texOffs(394, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.2F, -5.2F, -5.8F, 1.6915F, -0.2866F, -0.8103F));

		PartDefinition belly_diamond_low_l_r1 = body.addOrReplaceChild("belly_diamond_low_l_r1", CubeListBuilder.create().texOffs(374, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, 2.6F, -3.9F, 1.9911F, 0.0676F, 0.7386F));

		PartDefinition belly_diamond_side_l_r1 = body.addOrReplaceChild("belly_diamond_side_l_r1", CubeListBuilder.create().texOffs(354, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.8F, -1.4F, -5.3F, 1.8869F, 0.1236F, 0.7737F));

		PartDefinition chest_diamond_outer_l_r1 = body.addOrReplaceChild("chest_diamond_outer_l_r1", CubeListBuilder.create().texOffs(38, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4F, -6.0F, -4.3F, 1.9377F, 0.493F, 0.9313F));

		PartDefinition chest_diamond_inner_l_r1 = body.addOrReplaceChild("chest_diamond_inner_l_r1", CubeListBuilder.create().texOffs(334, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.2F, -5.2F, -5.8F, 1.6915F, 0.2866F, 0.8103F));

		PartDefinition belly_diamond_low_center_r1 = body.addOrReplaceChild("belly_diamond_low_center_r1", CubeListBuilder.create().texOffs(314, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.8F, -4.1F, 1.7782F, -0.1951F, 0.7446F));

		PartDefinition belly_diamond_center_r1 = body.addOrReplaceChild("belly_diamond_center_r1", CubeListBuilder.create().texOffs(294, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, -5.7F, 1.6709F, -0.0987F, 0.7755F));

		PartDefinition chest_diamond_center_r1 = body.addOrReplaceChild("chest_diamond_center_r1", CubeListBuilder.create().texOffs(274, 29).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.6F, -6.3F, 1.4706F, 0.0987F, 0.7755F));

		PartDefinition torso_undertail_r1 = body.addOrReplaceChild("torso_undertail_r1", CubeListBuilder.create().texOffs(302, 2).addBox(-3.5F, -2.5F, -3.0F, 7.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.4F, 3.7F, -0.4189F, 0.0F, 0.0F));

		PartDefinition torso_belly_taper_r1 = body.addOrReplaceChild("torso_belly_taper_r1", CubeListBuilder.create().texOffs(140, 2).addBox(-4.5F, -3.5F, -4.5F, 9.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.4F, 1.3F, -0.3142F, 0.0F, 0.0F));

		PartDefinition wing_l = body.addOrReplaceChild("wing_l", CubeListBuilder.create(), PartPose.offset(-5.5F, -7.2F, -0.5F));

		PartDefinition wing_prim_l_3_r1 = wing_l.addOrReplaceChild("wing_prim_l_3_r1", CubeListBuilder.create().texOffs(236, 2).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, 10.6F, 5.8F, -0.6947F, 0.0F, -3.1277F));

		PartDefinition wing_prim_l_2_r1 = wing_l.addOrReplaceChild("wing_prim_l_2_r1", CubeListBuilder.create().texOffs(194, 2).addBox(-1.0F, -10.0F, -1.5F, 2.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.3F, 9.6F, 4.0F, -0.6841F, 0.0F, -3.1153F));

		PartDefinition wing_prim_l_1_r1 = wing_l.addOrReplaceChild("wing_prim_l_1_r1", CubeListBuilder.create().texOffs(180, 2).addBox(-1.0F, -10.0F, -1.5F, 2.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 8.4F, 1.8F, -0.6434F, 0.0F, -3.1166F));

		PartDefinition wing_cov_l_4_r1 = wing_l.addOrReplaceChild("wing_cov_l_4_r1", CubeListBuilder.create().texOffs(446, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 7.8F, 3.4F, -0.6543F, 0.0F, -3.0648F));

		PartDefinition wing_cov_l_3_r1 = wing_l.addOrReplaceChild("wing_cov_l_3_r1", CubeListBuilder.create().texOffs(432, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.9F, 3.4F, 3.8F, -0.6346F, 0.0F, -3.0309F));

		PartDefinition wing_cov_l_2_r1 = wing_l.addOrReplaceChild("wing_cov_l_2_r1", CubeListBuilder.create().texOffs(418, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 4.6F, 1.6F, -0.6112F, 0.0F, -3.0493F));

		PartDefinition wing_cov_l_1_r1 = wing_l.addOrReplaceChild("wing_cov_l_1_r1", CubeListBuilder.create().texOffs(404, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.9F, 0.0F, 1.4F, -0.5935F, 0.0F, -3.0349F));

		PartDefinition wing_shoulder_diamond_l_r1 = wing_l.addOrReplaceChild("wing_shoulder_diamond_l_r1", CubeListBuilder.create().texOffs(202, 29).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 2.6F, -0.2F, 2.8681F, 0.7337F, 1.5705F));

		PartDefinition wing_core_l_r1 = wing_l.addOrReplaceChild("wing_core_l_r1", CubeListBuilder.create().texOffs(48, 2).addBox(-1.0F, -14.0F, -3.5F, 2.0F, 15.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -0.2F, -0.8F, -0.5099F, 0.0F, -3.1246F));

		PartDefinition wing_r = body.addOrReplaceChild("wing_r", CubeListBuilder.create(), PartPose.offset(5.5F, -7.2F, -0.5F));

		PartDefinition wing_prim_r_3_r1 = wing_r.addOrReplaceChild("wing_prim_r_3_r1", CubeListBuilder.create().texOffs(248, 2).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 10.6F, 5.8F, -0.6947F, 0.0F, 3.1277F));

		PartDefinition wing_prim_r_2_r1 = wing_r.addOrReplaceChild("wing_prim_r_2_r1", CubeListBuilder.create().texOffs(222, 2).addBox(-1.0F, -10.0F, -1.5F, 2.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.3F, 9.6F, 4.0F, -0.6841F, 0.0F, 3.1153F));

		PartDefinition wing_prim_r_1_r1 = wing_r.addOrReplaceChild("wing_prim_r_1_r1", CubeListBuilder.create().texOffs(208, 2).addBox(-1.0F, -10.0F, -1.5F, 2.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 8.4F, 1.8F, -0.6434F, 0.0F, 3.1166F));

		PartDefinition wing_cov_r_4_r1 = wing_r.addOrReplaceChild("wing_cov_r_4_r1", CubeListBuilder.create().texOffs(2, 29).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 7.8F, 3.4F, -0.6543F, 0.0F, 3.0648F));

		PartDefinition wing_cov_r_3_r1 = wing_r.addOrReplaceChild("wing_cov_r_3_r1", CubeListBuilder.create().texOffs(488, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.9F, 3.4F, 3.8F, -0.6346F, 0.0F, 3.0309F));

		PartDefinition wing_cov_r_2_r1 = wing_r.addOrReplaceChild("wing_cov_r_2_r1", CubeListBuilder.create().texOffs(474, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, 4.6F, 1.6F, -0.6112F, 0.0F, 3.0493F));

		PartDefinition wing_cov_r_1_r1 = wing_r.addOrReplaceChild("wing_cov_r_1_r1", CubeListBuilder.create().texOffs(460, 2).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.9F, 0.0F, 1.4F, -0.5935F, 0.0F, 3.0349F));

		PartDefinition wing_shoulder_diamond_r_r1 = wing_r.addOrReplaceChild("wing_shoulder_diamond_r_r1", CubeListBuilder.create().texOffs(226, 29).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, 2.6F, -0.2F, 2.8681F, -0.7337F, -1.5705F));

		PartDefinition wing_core_r_r1 = wing_r.addOrReplaceChild("wing_core_r_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-1.0F, -14.0F, -3.5F, 2.0F, 15.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -0.2F, -0.8F, -0.5099F, 0.0F, 3.1246F));

		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 7.8F, 4.9F));

		PartDefinition tail_side_r_r1 = tail.addOrReplaceChild("tail_side_r_r1", CubeListBuilder.create().texOffs(30, 29).addBox(-1.5F, -7.8F, -1.0F, 3.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8F, -0.6F, -0.4F, -0.9533F, -0.155F, -3.1416F));

		PartDefinition tail_side_l_r1 = tail.addOrReplaceChild("tail_side_l_r1", CubeListBuilder.create().texOffs(16, 29).addBox(-1.5F, -7.8F, -1.0F, 3.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8F, -0.6F, -0.4F, -0.9533F, 0.155F, 3.1416F));

		PartDefinition tail_center_r1 = tail.addOrReplaceChild("tail_center_r1", CubeListBuilder.create().texOffs(288, 2).addBox(-1.5F, -8.8F, -1.0F, 3.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, -0.2F, -0.9368F, 0.0F, 3.1416F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(92, 2).addBox(-6.5F, -9.3F, -5.3F, 13.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(122, 29).addBox(-4.5F, -10.8F, -4.1F, 9.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(96, 29).addBox(-4.5F, -8.8F, 3.2F, 9.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(178, 44).addBox(-0.5F, -2.6F, -7.6F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(454, 29).addBox(-5.2F, -6.6F, -5.8F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(470, 29).addBox(1.2F, -6.6F, -5.8F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.6F, -0.7F));

		PartDefinition cheek_ruff_r_1_r1 = head.addOrReplaceChild("cheek_ruff_r_1_r1", CubeListBuilder.create().texOffs(350, 2).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.8F, -4.0F, -0.8F, -0.1396F, 0.0F, 0.1745F));

		PartDefinition ear_plume_sub_r_r1 = head.addOrReplaceChild("ear_plume_sub_r_r1", CubeListBuilder.create().texOffs(192, 29).addBox(-0.5F, -6.0F, -1.0F, 1.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8F, -6.2F, -0.8F, -2.2418F, -0.723F, -3.1416F));

		PartDefinition ear_plume_cyan_r_r1 = head.addOrReplaceChild("ear_plume_cyan_r_r1", CubeListBuilder.create().texOffs(84, 29).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -7.8F, -1.8F, -2.3899F, -0.8124F, -3.1416F));

		PartDefinition ear_crest_main_r_r1 = head.addOrReplaceChild("ear_crest_main_r_r1", CubeListBuilder.create().texOffs(58, 29).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2F, -8.0F, -4.2F, -0.332F, 0.0F, 0.5619F));

		PartDefinition brow_ridge_r_r1 = head.addOrReplaceChild("brow_ridge_r_r1", CubeListBuilder.create().texOffs(170, 29).addBox(-1.0F, -6.2F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8F, -5.8F, -5.6F, -0.1346F, 0.0F, 1.0769F));

		PartDefinition cheek_ruff_l_1_r1 = head.addOrReplaceChild("cheek_ruff_l_1_r1", CubeListBuilder.create().texOffs(332, 2).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8F, -4.0F, -0.8F, -0.1396F, 0.0F, -0.1745F));

		PartDefinition ear_plume_sub_l_r1 = head.addOrReplaceChild("ear_plume_sub_l_r1", CubeListBuilder.create().texOffs(182, 29).addBox(-0.5F, -6.0F, -1.0F, 1.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.8F, -6.2F, -0.8F, -2.2418F, 0.723F, 3.1416F));

		PartDefinition ear_plume_cyan_l_r1 = head.addOrReplaceChild("ear_plume_cyan_l_r1", CubeListBuilder.create().texOffs(72, 29).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4F, -7.8F, -1.8F, -2.3899F, 0.8124F, 3.1416F));

		PartDefinition ear_crest_main_l_r1 = head.addOrReplaceChild("ear_crest_main_l_r1", CubeListBuilder.create().texOffs(44, 29).addBox(-1.0F, -7.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, -8.0F, -4.2F, -0.332F, 0.0F, -0.5619F));

		PartDefinition brow_ridge_l_r1 = head.addOrReplaceChild("brow_ridge_l_r1", CubeListBuilder.create().texOffs(158, 29).addBox(-1.0F, -6.2F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8F, -5.8F, -5.6F, -0.1346F, 0.0F, -1.0769F));

		PartDefinition beak_upper_r1 = head.addOrReplaceChild("beak_upper_r1", CubeListBuilder.create().texOffs(486, 29).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.2F, -6.0F, -0.2793F, 0.0F, 0.0F));

		PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 44).addBox(-2.3F, 6.3F, -4.7F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(84, 44).addBox(-0.8F, 6.8F, -7.4F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(198, 44).addBox(-0.8F, 7.8F, -8.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(154, 44).addBox(-0.8F, 7.8F, -2.7F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.2F, -8.8F, -0.2F));

		PartDefinition leg_l_claw_3_r1 = leg_l.addOrReplaceChild("leg_l_claw_3_r1", CubeListBuilder.create().texOffs(208, 44).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7984F, 8.3F, -6.9087F, 0.0F, 0.384F, 0.0F));

		PartDefinition leg_l_toe_3_r1 = leg_l.addOrReplaceChild("leg_l_toe_3_r1", CubeListBuilder.create().texOffs(98, 44).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1241F, 7.3F, -5.2398F, 0.0F, 0.384F, 0.0F));

		PartDefinition leg_l_claw_1_r1 = leg_l.addOrReplaceChild("leg_l_claw_1_r1", CubeListBuilder.create().texOffs(188, 44).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1984F, 8.3F, -6.9087F, 0.0F, -0.384F, 0.0F));

		PartDefinition leg_l_toe_1_r1 = leg_l.addOrReplaceChild("leg_l_toe_1_r1", CubeListBuilder.create().texOffs(70, 44).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5241F, 7.3F, -5.2398F, 0.0F, -0.384F, 0.0F));

		PartDefinition leg_l_tarsus_r1 = leg_l.addOrReplaceChild("leg_l_tarsus_r1", CubeListBuilder.create().texOffs(250, 29).addBox(-1.0F, -4.2F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, 3.4F, -1.3F, 0.4635F, 0.0F, -3.1153F));

		PartDefinition leg_l_thigh_r1 = leg_l.addOrReplaceChild("leg_l_thigh_r1", CubeListBuilder.create().texOffs(368, 2).addBox(-1.5F, -5.8F, -2.0F, 3.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, 0.0F, 0.3078F, 0.0F, -3.0962F));

		PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(20, 44).addBox(-1.7F, 6.3F, -4.7F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(126, 44).addBox(-0.2F, 6.8F, -7.4F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(228, 44).addBox(-0.2F, 7.8F, -8.2F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(166, 44).addBox(-0.2F, 7.8F, -2.7F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.2F, -8.8F, -0.2F));

		PartDefinition leg_r_claw_3_r1 = leg_r.addOrReplaceChild("leg_r_claw_3_r1", CubeListBuilder.create().texOffs(238, 44).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7984F, 8.3F, -6.9087F, 0.0F, -0.384F, 0.0F));

		PartDefinition leg_r_toe_3_r1 = leg_r.addOrReplaceChild("leg_r_toe_3_r1", CubeListBuilder.create().texOffs(140, 44).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1241F, 7.3F, -5.2398F, 0.0F, -0.384F, 0.0F));

		PartDefinition leg_r_claw_1_r1 = leg_r.addOrReplaceChild("leg_r_claw_1_r1", CubeListBuilder.create().texOffs(218, 44).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1984F, 8.3F, -6.9087F, 0.0F, 0.384F, 0.0F));

		PartDefinition leg_r_toe_1_r1 = leg_r.addOrReplaceChild("leg_r_toe_1_r1", CubeListBuilder.create().texOffs(112, 44).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5241F, 7.3F, -5.2398F, 0.0F, 0.384F, 0.0F));

		PartDefinition leg_r_tarsus_r1 = leg_r.addOrReplaceChild("leg_r_tarsus_r1", CubeListBuilder.create().texOffs(262, 29).addBox(-1.0F, -4.2F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, 3.4F, -1.3F, 0.4635F, 0.0F, 3.1153F));

		PartDefinition leg_r_thigh_r1 = leg_r.addOrReplaceChild("leg_r_thigh_r1", CubeListBuilder.create().texOffs(386, 2).addBox(-1.5F, -5.8F, -2.0F, 3.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, 0.0F, 0.3078F, 0.0F, 3.0962F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(FacetedOwlEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		boolean flying = entity.isOwlFlying();
		boolean signaling = entity.getSignalTicks() > 0;

		// Iconic wide-angle owl head tracking + curious head-tilt
		float owlYaw = Mth.clamp(netHeadYaw, -75.0F, 75.0F);
		float owlPitch = Mth.clamp(headPitch, -30.0F, 30.0F);
		float curiousTilt = Mth.sin(ageInTicks * 0.05F + entity.getId()) * 4.5F;
		addBbDeg(this.head, -owlPitch * 0.65F + (signaling ? Mth.sin(ageInTicks * 0.6F) * 6.0F : 0.0F),
				owlYaw * 0.85F, curiousTilt);

		if (flying) {
			float flap = Mth.sin(ageInTicks * 0.52F) * 44.0F;
			addBbPos(this.body, 0.0F, Mth.cos(ageInTicks * 0.52F) * 1.2F, 0.0F);
			addBbDeg(this.body, 18.0F, 0.0F, 0.0F);
			addBbDeg(this.wing_l, -8.0F, 12.0F, -(28.0F + flap));
			addBbDeg(this.wing_r, -8.0F, -12.0F, (28.0F + flap));
			addBbDeg(this.leg_l, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, -12.0F, 0.0F, 0.0F);
		} else {
			float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
			float w = limbSwing * 0.75F;
			addBbDeg(this.leg_l, Mth.cos(w) * 22.0F * move, 0.0F, 0.0F);
			addBbDeg(this.leg_r, Mth.cos(w + (float) Math.PI) * 22.0F * move, 0.0F, 0.0F);
			addBbDeg(this.body, Mth.sin(ageInTicks * 0.06F) * 1.5F, 0.0F, 0.0F);
			float mantle = signaling ? (22.0F + Mth.sin(ageInTicks * 0.5F) * 10.0F) : 0.0F;
			addBbDeg(this.wing_l, 0.0F, 0.0F, -mantle);
			addBbDeg(this.wing_r, 0.0F, 0.0F, mantle);
			addBbDeg(this.tail, Mth.sin(ageInTicks * 0.07F) * 2.5F, 0.0F, 0.0F);
		}
	}
}
