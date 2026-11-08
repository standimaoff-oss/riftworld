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
import ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/phantom_wolf.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class PhantomWolfModel extends FaunaBaseModel<PhantomWolfEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "phantom_wolf"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart tail_base;
	private final ModelPart leg_front_l;
	private final ModelPart leg_back_l;
	private final ModelPart leg_front_r;
	private final ModelPart leg_back_r;

	public PhantomWolfModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.tail_base = this.body.getChild("tail_base");
		this.leg_front_l = this.root.getChild("leg_front_l");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_front_r = this.root.getChild("leg_front_r");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.neck, this.head, this.tail_base, this.leg_front_l, this.leg_back_l, this.leg_front_r, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(52, 46).addBox(-5.5F, -13.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(60, 46).addBox(-5.1F, -28.7F, 5.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(68, 46).addBox(-5.5F, -9.7F, 16.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(76, 46).addBox(4.5F, -13.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(84, 46).addBox(4.1F, -28.7F, 5.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(92, 46).addBox(4.5F, -9.7F, 16.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(64, 2).addBox(-2.5F, -3.3F, -6.8F, 5.0F, 7.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(2, 2).addBox(-4.0F, -5.1F, -10.5F, 8.0F, 3.0F, 21.0F, new CubeDeformation(0.0F))
		.texOffs(370, 2).addBox(-4.5F, -3.6F, -10.8F, 9.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 30).addBox(-4.0F, -3.7F, 5.8F, 8.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -22.0F, 1.5F));

		PartDefinition dorsal_wisp_r_1_r1 = body.addOrReplaceChild("dorsal_wisp_r_1_r1", CubeListBuilder.create().texOffs(166, 30).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -4.8F, -4.7F, -1.6781F, -0.0809F, -3.1416F));

		PartDefinition rib_r_5_r1 = body.addOrReplaceChild("rib_r_5_r1", CubeListBuilder.create().texOffs(288, 30).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -0.1F, 5.3F, -0.0698F, 0.0F, 0.1047F));

		PartDefinition rib_r_4_r1 = body.addOrReplaceChild("rib_r_4_r1", CubeListBuilder.create().texOffs(278, 30).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -0.1F, 2.7F, -0.0698F, 0.0F, 0.1047F));

		PartDefinition rib_r_3_r1 = body.addOrReplaceChild("rib_r_3_r1", CubeListBuilder.create().texOffs(224, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 0.4F, 0.1F, -0.0698F, 0.0F, 0.1047F));

		PartDefinition rib_r_2_r1 = body.addOrReplaceChild("rib_r_2_r1", CubeListBuilder.create().texOffs(214, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 0.4F, -2.5F, -0.0698F, 0.0F, 0.1047F));

		PartDefinition rib_r_1_r1 = body.addOrReplaceChild("rib_r_1_r1", CubeListBuilder.create().texOffs(204, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 0.4F, -5.1F, -0.0698F, 0.0F, 0.1047F));

		PartDefinition dorsal_wisp_l_1_r1 = body.addOrReplaceChild("dorsal_wisp_l_1_r1", CubeListBuilder.create().texOffs(158, 30).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -4.8F, -4.7F, -1.6781F, 0.0809F, 3.1416F));

		PartDefinition rib_l_5_r1 = body.addOrReplaceChild("rib_l_5_r1", CubeListBuilder.create().texOffs(268, 30).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -0.1F, 5.3F, -0.0698F, 0.0F, -0.1047F));

		PartDefinition rib_l_4_r1 = body.addOrReplaceChild("rib_l_4_r1", CubeListBuilder.create().texOffs(258, 30).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -0.1F, 2.7F, -0.0698F, 0.0F, -0.1047F));

		PartDefinition rib_l_3_r1 = body.addOrReplaceChild("rib_l_3_r1", CubeListBuilder.create().texOffs(194, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 0.4F, 0.1F, -0.0698F, 0.0F, -0.1047F));

		PartDefinition rib_l_2_r1 = body.addOrReplaceChild("rib_l_2_r1", CubeListBuilder.create().texOffs(184, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 0.4F, -2.5F, -0.0698F, 0.0F, -0.1047F));

		PartDefinition rib_l_1_r1 = body.addOrReplaceChild("rib_l_1_r1", CubeListBuilder.create().texOffs(174, 30).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 0.4F, -5.1F, -0.0698F, 0.0F, -0.1047F));

		PartDefinition sternum_keel_r1 = body.addOrReplaceChild("sternum_keel_r1", CubeListBuilder.create().texOffs(172, 2).addBox(-2.5F, -1.0F, -7.5F, 5.0F, 2.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.4F, -1.3F, 0.0698F, 0.0F, 0.0F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -1.8F, -9.7F));

		PartDefinition neck_mane_wisp_r_2_r1 = neck.addOrReplaceChild("neck_mane_wisp_r_2_r1", CubeListBuilder.create().texOffs(478, 30).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.7F, -6.2F, 1.2F, -2.0492F, -0.203F, -3.1416F));

		PartDefinition neck_mane_wisp_r_1_r1 = neck.addOrReplaceChild("neck_mane_wisp_r_1_r1", CubeListBuilder.create().texOffs(430, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, -4.6F, -2.6F, -1.9778F, -0.2915F, -3.1416F));

		PartDefinition neck_mane_wisp_l_2_r1 = neck.addOrReplaceChild("neck_mane_wisp_l_2_r1", CubeListBuilder.create().texOffs(470, 30).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.7F, -6.2F, 1.2F, -2.0492F, 0.203F, 3.1416F));

		PartDefinition neck_mane_wisp_l_1_r1 = neck.addOrReplaceChild("neck_mane_wisp_l_1_r1", CubeListBuilder.create().texOffs(422, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, -4.6F, -2.6F, -1.9778F, 0.2915F, 3.1416F));

		PartDefinition neck_core_r1 = neck.addOrReplaceChild("neck_core_r1", CubeListBuilder.create().texOffs(104, 2).addBox(-4.0F, -9.6F, -4.0F, 8.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.4F, 0.4F, 0.5754F, 0.0F, 0.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(216, 2).addBox(-4.5F, -5.2F, -6.4F, 9.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(234, 30).addBox(-2.0F, 1.8F, -11.2F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(14, 46).addBox(-1.5F, -1.0F, -12.8F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(28, 46).addBox(-3.7F, -2.8F, -6.8F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(40, 46).addBox(1.7F, -2.8F, -6.8F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, -4.4F));

		PartDefinition cheek_wisp_r_2_r1 = head.addOrReplaceChild("cheek_wisp_r_2_r1", CubeListBuilder.create().texOffs(494, 30).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.3F, -2.2F, 3.2F, -2.0533F, -0.146F, -3.1416F));

		PartDefinition cheek_wisp_r_1_r1 = head.addOrReplaceChild("cheek_wisp_r_1_r1", CubeListBuilder.create().texOffs(446, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2F, -1.0F, -0.6F, -1.8943F, -0.2915F, -3.1416F));

		PartDefinition ear_tip_r_r1 = head.addOrReplaceChild("ear_tip_r_r1", CubeListBuilder.create().texOffs(2, 46).addBox(-1.0F, -2.3F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.3F, -8.2F, -1.0F, -0.0898F, 0.0F, 0.1355F));

		PartDefinition ear_base_r_r1 = head.addOrReplaceChild("ear_base_r_r1", CubeListBuilder.create().texOffs(408, 30).addBox(-1.5F, -4.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -4.8F, -1.4F, -0.1043F, 0.0F, 0.1049F));

		PartDefinition cheek_wisp_l_2_r1 = head.addOrReplaceChild("cheek_wisp_l_2_r1", CubeListBuilder.create().texOffs(486, 30).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.3F, -2.2F, 3.2F, -2.0533F, 0.146F, 3.1416F));

		PartDefinition cheek_wisp_l_1_r1 = head.addOrReplaceChild("cheek_wisp_l_1_r1", CubeListBuilder.create().texOffs(438, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, -1.0F, -0.6F, -1.8943F, 0.2915F, 3.1416F));

		PartDefinition ear_tip_l_r1 = head.addOrReplaceChild("ear_tip_l_r1", CubeListBuilder.create().texOffs(502, 30).addBox(-1.0F, -2.3F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.3F, -8.2F, -1.0F, -0.0898F, 0.0F, -0.1355F));

		PartDefinition ear_base_l_r1 = head.addOrReplaceChild("ear_base_l_r1", CubeListBuilder.create().texOffs(394, 30).addBox(-1.5F, -4.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -4.8F, -1.4F, -0.1043F, 0.0F, -0.1049F));

		PartDefinition snout_upper_r1 = head.addOrReplaceChild("snout_upper_r1", CubeListBuilder.create().texOffs(64, 30).addBox(-2.5F, -2.0F, -3.5F, 5.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8F, -8.8F, -0.0698F, 0.0F, 0.0F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create(), PartPose.offset(0.0F, -1.4F, 10.5F));

		PartDefinition tail_plume_low_r_r1 = tail_base.addOrReplaceChild("tail_plume_low_r_r1", CubeListBuilder.create().texOffs(476, 2).addBox(-0.5F, -10.0F, -1.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 8.0F, 9.0F, -1.0389F, -0.0907F, -3.1416F));

		PartDefinition tail_plume_curl_r_r1 = tail_base.addOrReplaceChild("tail_plume_curl_r_r1", CubeListBuilder.create().texOffs(148, 30).addBox(-0.5F, -8.2F, -1.0F, 1.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6F, 6.6F, 12.4F, -1.7572F, -0.0809F, -3.1416F));

		PartDefinition tail_plume_up_r_r1 = tail_base.addOrReplaceChild("tail_plume_up_r_r1", CubeListBuilder.create().texOffs(358, 2).addBox(-1.0F, -10.8F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8F, 2.4F, 3.2F, -1.1604F, -0.1038F, -3.1416F));

		PartDefinition tail_plume_low_l_r1 = tail_base.addOrReplaceChild("tail_plume_low_l_r1", CubeListBuilder.create().texOffs(466, 2).addBox(-0.5F, -10.0F, -1.0F, 1.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, 8.0F, 9.0F, -1.0389F, 0.0907F, 3.1416F));

		PartDefinition tail_plume_curl_l_r1 = tail_base.addOrReplaceChild("tail_plume_curl_l_r1", CubeListBuilder.create().texOffs(138, 30).addBox(-0.5F, -8.2F, -1.0F, 1.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, 6.6F, 12.4F, -1.7572F, 0.0809F, 3.1416F));

		PartDefinition tail_plume_up_l_r1 = tail_base.addOrReplaceChild("tail_plume_up_l_r1", CubeListBuilder.create().texOffs(346, 2).addBox(-1.0F, -10.8F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8F, 2.4F, 3.2F, -1.1604F, 0.1038F, 3.1416F));

		PartDefinition tail_core_4_r1 = tail_base.addOrReplaceChild("tail_core_4_r1", CubeListBuilder.create().texOffs(124, 30).addBox(-1.0F, -6.8F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.8F, 18.5F, -2.1312F, 0.0F, 3.1416F));

		PartDefinition tail_core_3_r1 = tail_base.addOrReplaceChild("tail_core_3_r1", CubeListBuilder.create().texOffs(446, 2).addBox(-2.0F, -7.8F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.2F, 12.5F, -1.3294F, 0.0F, 3.1416F));

		PartDefinition tail_core_2_r1 = tail_base.addOrReplaceChild("tail_core_2_r1", CubeListBuilder.create().texOffs(278, 2).addBox(-2.5F, -9.8F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 6.0F, -0.9137F, 0.0F, 3.1416F));

		PartDefinition tail_core_1_r1 = tail_base.addOrReplaceChild("tail_core_1_r1", CubeListBuilder.create().texOffs(254, 2).addBox(-2.5F, -9.8F, -2.5F, 5.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.4F, -0.9179F, 0.0F, 3.1416F));

		PartDefinition leg_front_l = root.addOrReplaceChild("leg_front_l", CubeListBuilder.create().texOffs(402, 2).addBox(-2.2F, -3.6F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(140, 2).addBox(-1.4F, 3.5F, -1.7F, 3.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(314, 30).addBox(-1.4F, 18.0F, -3.7F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.6F, -20.0F, -6.8F));

		PartDefinition leg_front_l_wisp_1_r1 = leg_front_l.addOrReplaceChild("leg_front_l_wisp_1_r1", CubeListBuilder.create().texOffs(454, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, 17.2F, 1.0F, -2.4915F, 0.1651F, 3.1416F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(334, 30).addBox(-1.4F, 19.0F, -1.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -21.0F, 9.6F));

		PartDefinition leg_back_l_wisp_1_r1 = leg_back_l.addOrReplaceChild("leg_back_l_wisp_1_r1", CubeListBuilder.create().texOffs(298, 30).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, 17.8F, 3.2F, -2.402F, 0.1651F, 3.1416F));

		PartDefinition leg_back_l_tarsus_r1 = leg_back_l.addOrReplaceChild("leg_back_l_tarsus_r1", CubeListBuilder.create().texOffs(32, 30).addBox(-1.5F, -8.2F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, 12.0F, 4.0F, 0.2386F, 0.0F, -3.1416F));

		PartDefinition leg_back_l_hock_r1 = leg_back_l.addOrReplaceChild("leg_back_l_hock_r1", CubeListBuilder.create().texOffs(92, 30).addBox(-1.5F, -7.2F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 1.6F, -0.4487F, 0.0F, 3.1231F));

		PartDefinition leg_back_l_thigh_r1 = leg_back_l.addOrReplaceChild("leg_back_l_thigh_r1", CubeListBuilder.create().texOffs(302, 2).addBox(-2.0F, -8.8F, -2.5F, 4.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -0.6F, -0.6F, -0.2915F, 0.0F, 3.1416F));

		PartDefinition leg_front_r = root.addOrReplaceChild("leg_front_r", CubeListBuilder.create().texOffs(424, 2).addBox(-1.8F, -3.6F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(156, 2).addBox(-1.6F, 3.5F, -1.7F, 3.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(354, 30).addBox(-1.6F, 18.0F, -3.7F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.6F, -20.0F, -6.8F));

		PartDefinition leg_front_r_wisp_1_r1 = leg_front_r.addOrReplaceChild("leg_front_r_wisp_1_r1", CubeListBuilder.create().texOffs(462, 30).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 17.2F, 1.0F, -2.4915F, -0.1651F, -3.1416F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(374, 30).addBox(-1.6F, 19.0F, -1.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, -21.0F, 9.6F));

		PartDefinition leg_back_r_wisp_1_r1 = leg_back_r.addOrReplaceChild("leg_back_r_wisp_1_r1", CubeListBuilder.create().texOffs(306, 30).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 17.8F, 3.2F, -2.402F, -0.1651F, -3.1416F));

		PartDefinition leg_back_r_tarsus_r1 = leg_back_r.addOrReplaceChild("leg_back_r_tarsus_r1", CubeListBuilder.create().texOffs(48, 30).addBox(-1.5F, -8.2F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 12.0F, 4.0F, 0.2386F, 0.0F, 3.1416F));

		PartDefinition leg_back_r_hock_r1 = leg_back_r.addOrReplaceChild("leg_back_r_hock_r1", CubeListBuilder.create().texOffs(108, 30).addBox(-1.5F, -7.2F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 1.6F, -0.4487F, 0.0F, -3.1231F));

		PartDefinition leg_back_r_thigh_r1 = leg_back_r.addOrReplaceChild("leg_back_r_thigh_r1", CubeListBuilder.create().texOffs(324, 2).addBox(-2.0F, -8.8F, -2.5F, 4.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -0.6F, -0.6F, -0.2915F, 0.0F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(PhantomWolfEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		// Spectral translucency: ghostly in mist/dark, solidified in bright lantern light
		this.renderAlpha = entity.isRevealedByLight() ? 0.90F : 0.48F;

		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean leaping = entity.getLeapTicks() > 0;
		boolean howling = entity.getHowlTicks() > 0;
		float w = limbSwing * 0.68F;

		if (leaping) {
			addBbDeg(this.body, -16.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -42.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -38.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, 44.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 40.0F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -18.0F, 0.0F, 0.0F);
		} else {
			float dA = Mth.cos(w) * 36.0F * move;
			float dB = Mth.cos(w + (float) Math.PI) * 36.0F * move;
			addBbDeg(this.leg_front_l, dA, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, dA * 0.9F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, dB, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, dB * 0.9F, 0.0F, 0.0F);
			addBbDeg(this.tail_base, -8.0F * move + Mth.sin(ageInTicks * 0.08F) * 3.0F,
					Mth.sin(ageInTicks * 0.12F) * (7.0F + 6.0F * move), 0.0F);
		}

		if (howling) {
			addBbDeg(this.neck, -26.0F, 0.0F, 0.0F);
			addBbDeg(this.head, -34.0F + Mth.sin(ageInTicks * 0.55F) * 3.0F, 0.0F, 0.0F);
		} else {
			addBbDeg(this.neck, 6.0F * move + Mth.sin(ageInTicks * 0.07F) * 1.5F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.35F, 0.0F);
			addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.55F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.45F, 0.0F);
		}
	}
}
