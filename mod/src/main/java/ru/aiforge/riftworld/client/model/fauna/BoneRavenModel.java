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
import ru.aiforge.riftworld.entity.fauna.BoneRavenEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/bone_raven.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class BoneRavenModel extends FaunaBaseModel<BoneRavenEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "bone_raven"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart wing_l;
	private final ModelPart wing_r;
	private final ModelPart tail;
	private final ModelPart leg_l;
	private final ModelPart leg_r;

	public BoneRavenModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.wing_l = this.body.getChild("wing_l");
		this.wing_r = this.body.getChild("wing_r");
		this.tail = this.body.getChild("tail");
		this.leg_l = this.root.getChild("leg_l");
		this.leg_r = this.root.getChild("leg_r");
		captureRestPoses(this.root, this.body, this.neck, this.head, this.wing_l, this.wing_r, this.tail, this.leg_l, this.leg_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(404, 32).addBox(-4.0F, -6.1F, -6.1F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(100, 32).addBox(-3.5F, 0.7F, 0.0F, 7.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -36.0F, 1.0F));

		PartDefinition rib_low_r_4_r1 = body.addOrReplaceChild("rib_low_r_4_r1", CubeListBuilder.create().texOffs(470, 32).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 2.0F, 2.2F, 2.0246F, 0.9151F, 0.0F));

		PartDefinition rib_up_r_4_r1 = body.addOrReplaceChild("rib_up_r_4_r1", CubeListBuilder.create().texOffs(396, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -0.8F, 2.4F, 0.0523F, 0.0F, 2.3932F));

		PartDefinition rib_low_r_3_r1 = body.addOrReplaceChild("rib_low_r_3_r1", CubeListBuilder.create().texOffs(388, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4F, 1.2F, 0.4F, 2.046F, 1.0304F, 0.0F));

		PartDefinition rib_up_r_3_r1 = body.addOrReplaceChild("rib_up_r_3_r1", CubeListBuilder.create().texOffs(380, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -2.0F, 0.6F, 0.0456F, 0.0F, 2.3884F));

		PartDefinition rib_low_r_2_r1 = body.addOrReplaceChild("rib_low_r_2_r1", CubeListBuilder.create().texOffs(372, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 0.2F, -1.4F, 2.0266F, 1.0584F, 0.0F));

		PartDefinition rib_up_r_2_r1 = body.addOrReplaceChild("rib_up_r_2_r1", CubeListBuilder.create().texOffs(364, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -3.0F, -1.2F, 0.0442F, 0.0F, 2.3562F));

		PartDefinition rib_low_r_1_r1 = body.addOrReplaceChild("rib_low_r_1_r1", CubeListBuilder.create().texOffs(356, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4F, -1.0F, -3.2F, 2.0577F, 1.0808F, 0.0F));

		PartDefinition rib_up_r_1_r1 = body.addOrReplaceChild("rib_up_r_1_r1", CubeListBuilder.create().texOffs(348, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -4.0F, -3.0F, 0.0471F, 0.0F, 2.3562F));

		PartDefinition rib_low_l_4_r1 = body.addOrReplaceChild("rib_low_l_4_r1", CubeListBuilder.create().texOffs(462, 32).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 2.0F, 2.2F, 2.0246F, -0.9151F, 0.0F));

		PartDefinition rib_up_l_4_r1 = body.addOrReplaceChild("rib_up_l_4_r1", CubeListBuilder.create().texOffs(340, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -0.8F, 2.4F, 0.0523F, 0.0F, -2.3932F));

		PartDefinition rib_low_l_3_r1 = body.addOrReplaceChild("rib_low_l_3_r1", CubeListBuilder.create().texOffs(332, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.4F, 1.2F, 0.4F, 2.046F, -1.0304F, 0.0F));

		PartDefinition rib_up_l_3_r1 = body.addOrReplaceChild("rib_up_l_3_r1", CubeListBuilder.create().texOffs(324, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -2.0F, 0.6F, 0.0456F, 0.0F, -2.3884F));

		PartDefinition rib_low_l_2_r1 = body.addOrReplaceChild("rib_low_l_2_r1", CubeListBuilder.create().texOffs(316, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 0.2F, -1.4F, 2.0266F, -1.0584F, 0.0F));

		PartDefinition rib_up_l_2_r1 = body.addOrReplaceChild("rib_up_l_2_r1", CubeListBuilder.create().texOffs(308, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -3.0F, -1.2F, 0.0442F, 0.0F, -2.3562F));

		PartDefinition rib_low_l_1_r1 = body.addOrReplaceChild("rib_low_l_1_r1", CubeListBuilder.create().texOffs(300, 32).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.4F, -1.0F, -3.2F, 2.0577F, -1.0808F, 0.0F));

		PartDefinition rib_up_l_1_r1 = body.addOrReplaceChild("rib_up_l_1_r1", CubeListBuilder.create().texOffs(292, 32).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -4.0F, -3.0F, 0.0471F, 0.0F, -2.3562F));

		PartDefinition sternum_keel_r1 = body.addOrReplaceChild("sternum_keel_r1", CubeListBuilder.create().texOffs(400, 2).addBox(-1.5F, -8.0F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8F, -5.8F, -1.0447F, 0.0F, 3.1416F));

		PartDefinition sternum_front_r1 = body.addOrReplaceChild("sternum_front_r1", CubeListBuilder.create().texOffs(496, 2).addBox(-2.0F, -6.0F, -1.5F, 4.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.2F, -5.4F, 0.1149F, 0.0F, -3.1416F));

		PartDefinition spine_rear_r1 = body.addOrReplaceChild("spine_rear_r1", CubeListBuilder.create().texOffs(384, 2).addBox(-1.5F, -8.2F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.6F, 0.8F, -0.8871F, 0.0F, 3.1416F));

		PartDefinition spine_front_r1 = body.addOrReplaceChild("spine_front_r1", CubeListBuilder.create().texOffs(368, 2).addBox(-1.5F, -8.0F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.2F, -4.8F, -0.9908F, 0.0F, 3.1416F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -5.2F, -5.0F));

		PartDefinition vert_ring_2_r1 = neck.addOrReplaceChild("vert_ring_2_r1", CubeListBuilder.create().texOffs(2, 46).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -2.4F, -0.4189F, 0.0F, 0.0F));

		PartDefinition vert_ring_1_r1 = neck.addOrReplaceChild("vert_ring_1_r1", CubeListBuilder.create().texOffs(478, 32).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.4F, -1.2F, -0.4189F, 0.0F, 0.0F));

		PartDefinition neck_column_r1 = neck.addOrReplaceChild("neck_column_r1", CubeListBuilder.create().texOffs(436, 2).addBox(-1.5F, -6.8F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.6288F, 0.0F, 0.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -4.2F, -3.4F));

		PartDefinition eye_socket_r_r1 = head.addOrReplaceChild("eye_socket_r_r1", CubeListBuilder.create().texOffs(250, 32).addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(234, 32).addBox(-6.6F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8F, -1.0F, -3.0F, -0.1396F, 0.0F, 0.0F));

		PartDefinition jaw_lower_r1 = head.addOrReplaceChild("jaw_lower_r1", CubeListBuilder.create().texOffs(20, 32).addBox(-1.5F, -7.8F, -0.5F, 3.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.6F, -5.0F, 1.9863F, 0.0F, 0.0F));

		PartDefinition beak_upper_tip_r1 = head.addOrReplaceChild("beak_upper_tip_r1", CubeListBuilder.create().texOffs(266, 32).addBox(-1.5F, -4.2F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.9F, -9.4F, 1.9727F, 0.0F, 0.0F));

		PartDefinition beak_upper_base_r1 = head.addOrReplaceChild("beak_upper_base_r1", CubeListBuilder.create().texOffs(2, 32).addBox(-2.0F, -5.8F, -1.5F, 4.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, -5.2F, 1.9591F, 0.0F, 0.0F));

		PartDefinition skull_cranium_r1 = head.addOrReplaceChild("skull_cranium_r1", CubeListBuilder.create().texOffs(302, 2).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, -2.4F, -0.1396F, 0.0F, 0.0F));

		PartDefinition wing_l = body.addOrReplaceChild("wing_l", CubeListBuilder.create(), PartPose.offset(-4.0F, -4.6F, -4.6F));

		PartDefinition wing_feather_l_12_r1 = wing_l.addOrReplaceChild("wing_feather_l_12_r1", CubeListBuilder.create().texOffs(80, 32).addBox(-0.5F, -6.1F, -1.0F, 1.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-23.8F, -1.8F, 0.2F, -1.3136F, 1.4056F, 3.1416F));

		PartDefinition wing_feather_l_11_r1 = wing_l.addOrReplaceChild("wing_feather_l_11_r1", CubeListBuilder.create().texOffs(282, 2).addBox(-0.5F, -11.1F, -1.0F, 1.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.2F, -2.6F, 0.2F, -1.197F, 1.3734F, 3.1416F));

		PartDefinition wing_feather_l_10_r1 = wing_l.addOrReplaceChild("wing_feather_l_10_r1", CubeListBuilder.create().texOffs(192, 2).addBox(-0.5F, -16.1F, -1.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-20.0F, -3.6F, 0.1F, -1.0956F, 1.3393F, 3.1416F));

		PartDefinition wing_feather_l_9_r1 = wing_l.addOrReplaceChild("wing_feather_l_9_r1", CubeListBuilder.create().texOffs(114, 2).addBox(-0.5F, -17.1F, -1.0F, 1.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.6F, -4.8F, 0.0F, -0.9971F, 1.249F, 3.1416F));

		PartDefinition wing_feather_l_8_r1 = wing_l.addOrReplaceChild("wing_feather_l_8_r1", CubeListBuilder.create().texOffs(26, 2).addBox(-0.5F, -20.1F, -1.5F, 1.0F, 21.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.2F, -6.0F, -0.2F, -0.8803F, 1.1591F, 3.1416F));

		PartDefinition wing_feather_l_7_r1 = wing_l.addOrReplaceChild("wing_feather_l_7_r1", CubeListBuilder.create().texOffs(50, 2).addBox(-0.5F, -21.1F, -1.0F, 1.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.8F, -7.2F, -0.4F, -0.7951F, 1.035F, 3.1416F));

		PartDefinition wing_feather_l_6_r1 = wing_l.addOrReplaceChild("wing_feather_l_6_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-0.5F, -22.1F, -1.5F, 1.0F, 23.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.2F, -6.6F, -0.6F, -0.7466F, 0.9003F, 3.1416F));

		PartDefinition wing_feather_l_5_r1 = wing_l.addOrReplaceChild("wing_feather_l_5_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-0.5F, -19.1F, -1.0F, 1.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.2F, -4.4F, -0.2F, -0.8175F, 0.7749F, 3.1416F));

		PartDefinition wing_feather_l_4_r1 = wing_l.addOrReplaceChild("wing_feather_l_4_r1", CubeListBuilder.create().texOffs(90, 2).addBox(-0.5F, -17.1F, -1.5F, 1.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -2.0F, 0.2F, -0.8052F, 0.6147F, 3.1416F));

		PartDefinition wing_feather_l_3_r1 = wing_l.addOrReplaceChild("wing_feather_l_3_r1", CubeListBuilder.create().texOffs(232, 2).addBox(-0.5F, -15.1F, -1.0F, 1.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 0.2F, 0.4F, -0.902F, 0.5344F, 3.1416F));

		PartDefinition wing_feather_l_2_r1 = wing_l.addOrReplaceChild("wing_feather_l_2_r1", CubeListBuilder.create().texOffs(146, 2).addBox(-0.5F, -15.1F, -1.5F, 1.0F, 16.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 0.4F, 0.6F, -0.8055F, 0.4389F, 3.1416F));

		PartDefinition wing_feather_l_1_r1 = wing_l.addOrReplaceChild("wing_feather_l_1_r1", CubeListBuilder.create().texOffs(262, 2).addBox(-0.5F, -12.1F, -1.0F, 1.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, 0.6F, 0.6F, -0.8088F, 0.3721F, 3.1416F));

		PartDefinition wing_finger_l_3_r1 = wing_l.addOrReplaceChild("wing_finger_l_3_r1", CubeListBuilder.create().texOffs(222, 2).addBox(-0.5F, -15.0F, -1.0F, 1.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -6.6F, -0.4F, -0.6769F, 0.9404F, 3.1416F));

		PartDefinition wing_finger_l_2_r1 = wing_l.addOrReplaceChild("wing_finger_l_2_r1", CubeListBuilder.create().texOffs(182, 2).addBox(-0.5F, -16.0F, -1.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.8F, -8.2F, -1.0F, -0.8026F, 1.249F, 3.1416F));

		PartDefinition wing_finger_l_1_r1 = wing_l.addOrReplaceChild("wing_finger_l_1_r1", CubeListBuilder.create().texOffs(134, 2).addBox(-1.0F, -16.0F, -1.0F, 2.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.8F, -9.2F, -1.6F, -1.1196F, 1.4995F, 3.1416F));

		PartDefinition wing_carpal_l_r1 = wing_l.addOrReplaceChild("wing_carpal_l_r1", CubeListBuilder.create().texOffs(430, 32).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.8F, -9.4F, -1.8F, -0.2094F, 0.1745F, -0.4189F));

		PartDefinition wing_ulna_l_r1 = wing_l.addOrReplaceChild("wing_ulna_l_r1", CubeListBuilder.create().texOffs(334, 2).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -0.6F, -0.2F, 0.1507F, 0.0F, -0.4248F));

		PartDefinition wing_humerus_l_r1 = wing_l.addOrReplaceChild("wing_humerus_l_r1", CubeListBuilder.create().texOffs(452, 2).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, -0.2F, -0.2F, 1.492F, 1.5708F, 0.0F));

		PartDefinition wing_r = body.addOrReplaceChild("wing_r", CubeListBuilder.create(), PartPose.offset(4.0F, -4.6F, -4.6F));

		PartDefinition wing_feather_r_12_r1 = wing_r.addOrReplaceChild("wing_feather_r_12_r1", CubeListBuilder.create().texOffs(90, 32).addBox(-0.5F, -6.1F, -1.0F, 1.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.8F, -1.8F, 0.2F, -1.3136F, -1.4056F, -3.1416F));

		PartDefinition wing_feather_r_11_r1 = wing_r.addOrReplaceChild("wing_feather_r_11_r1", CubeListBuilder.create().texOffs(292, 2).addBox(-0.5F, -11.1F, -1.0F, 1.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(22.2F, -2.6F, 0.2F, -1.197F, -1.3734F, -3.1416F));

		PartDefinition wing_feather_r_10_r1 = wing_r.addOrReplaceChild("wing_feather_r_10_r1", CubeListBuilder.create().texOffs(212, 2).addBox(-0.5F, -16.1F, -1.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.0F, -3.6F, 0.1F, -1.0956F, -1.3393F, -3.1416F));

		PartDefinition wing_feather_r_9_r1 = wing_r.addOrReplaceChild("wing_feather_r_9_r1", CubeListBuilder.create().texOffs(124, 2).addBox(-0.5F, -17.1F, -1.0F, 1.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(17.6F, -4.8F, 0.0F, -0.9971F, -1.249F, -3.1416F));

		PartDefinition wing_feather_r_8_r1 = wing_r.addOrReplaceChild("wing_feather_r_8_r1", CubeListBuilder.create().texOffs(38, 2).addBox(-0.5F, -20.1F, -1.5F, 1.0F, 21.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(15.2F, -6.0F, -0.2F, -0.8803F, -1.1591F, -3.1416F));

		PartDefinition wing_feather_r_7_r1 = wing_r.addOrReplaceChild("wing_feather_r_7_r1", CubeListBuilder.create().texOffs(60, 2).addBox(-0.5F, -21.1F, -1.0F, 1.0F, 22.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.8F, -7.2F, -0.4F, -0.7951F, -1.035F, -3.1416F));

		PartDefinition wing_feather_r_6_r1 = wing_r.addOrReplaceChild("wing_feather_r_6_r1", CubeListBuilder.create().texOffs(14, 2).addBox(-0.5F, -22.1F, -1.5F, 1.0F, 23.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.2F, -6.6F, -0.6F, -0.7466F, -0.9003F, -3.1416F));

		PartDefinition wing_feather_r_5_r1 = wing_r.addOrReplaceChild("wing_feather_r_5_r1", CubeListBuilder.create().texOffs(80, 2).addBox(-0.5F, -19.1F, -1.0F, 1.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.2F, -4.4F, -0.2F, -0.8175F, -0.7749F, -3.1416F));

		PartDefinition wing_feather_r_4_r1 = wing_r.addOrReplaceChild("wing_feather_r_4_r1", CubeListBuilder.create().texOffs(102, 2).addBox(-0.5F, -17.1F, -1.5F, 1.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -2.0F, 0.2F, -0.8052F, -0.6147F, -3.1416F));

		PartDefinition wing_feather_r_3_r1 = wing_r.addOrReplaceChild("wing_feather_r_3_r1", CubeListBuilder.create().texOffs(252, 2).addBox(-0.5F, -15.1F, -1.0F, 1.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 0.2F, 0.4F, -0.902F, -0.5344F, -3.1416F));

		PartDefinition wing_feather_r_2_r1 = wing_r.addOrReplaceChild("wing_feather_r_2_r1", CubeListBuilder.create().texOffs(170, 2).addBox(-0.5F, -15.1F, -1.5F, 1.0F, 16.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 0.4F, 0.6F, -0.8055F, -0.4389F, -3.1416F));

		PartDefinition wing_feather_r_1_r1 = wing_r.addOrReplaceChild("wing_feather_r_1_r1", CubeListBuilder.create().texOffs(272, 2).addBox(-0.5F, -12.1F, -1.0F, 1.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 0.6F, 0.6F, -0.8088F, -0.3721F, -3.1416F));

		PartDefinition wing_finger_r_3_r1 = wing_r.addOrReplaceChild("wing_finger_r_3_r1", CubeListBuilder.create().texOffs(242, 2).addBox(-0.5F, -15.0F, -1.0F, 1.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, -6.6F, -0.4F, -0.6769F, -0.9404F, -3.1416F));

		PartDefinition wing_finger_r_2_r1 = wing_r.addOrReplaceChild("wing_finger_r_2_r1", CubeListBuilder.create().texOffs(202, 2).addBox(-0.5F, -16.0F, -1.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8F, -8.2F, -1.0F, -0.8026F, -1.249F, -3.1416F));

		PartDefinition wing_finger_r_1_r1 = wing_r.addOrReplaceChild("wing_finger_r_1_r1", CubeListBuilder.create().texOffs(158, 2).addBox(-1.0F, -16.0F, -1.0F, 2.0F, 17.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8F, -9.2F, -1.6F, -1.1196F, -1.4995F, -3.1416F));

		PartDefinition wing_carpal_r_r1 = wing_r.addOrReplaceChild("wing_carpal_r_r1", CubeListBuilder.create().texOffs(446, 32).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.8F, -9.4F, -1.8F, -0.2094F, -0.1745F, 0.4189F));

		PartDefinition wing_ulna_r_r1 = wing_r.addOrReplaceChild("wing_ulna_r_r1", CubeListBuilder.create().texOffs(346, 2).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -0.6F, -0.2F, 0.1507F, 0.0F, 0.4248F));

		PartDefinition wing_humerus_r_r1 = wing_r.addOrReplaceChild("wing_humerus_r_r1", CubeListBuilder.create().texOffs(464, 2).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, -0.2F, -0.2F, 1.492F, -1.5708F, 0.0F));

		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 2.8F, 6.0F));

		PartDefinition tail_feather_r_2_r1 = tail.addOrReplaceChild("tail_feather_r_2_r1", CubeListBuilder.create().texOffs(486, 2).addBox(-1.0F, -8.8F, -0.5F, 2.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, 1.0F, 1.8F, -1.1096F, -0.4636F, -3.1416F));

		PartDefinition tail_feather_r_1_r1 = tail.addOrReplaceChild("tail_feather_r_1_r1", CubeListBuilder.create().texOffs(426, 2).addBox(-1.0F, -9.8F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8F, 1.2F, 2.2F, -1.0708F, -0.2063F, -3.1416F));

		PartDefinition tail_feather_l_2_r1 = tail.addOrReplaceChild("tail_feather_l_2_r1", CubeListBuilder.create().texOffs(476, 2).addBox(-1.0F, -8.8F, -0.5F, 2.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 1.0F, 1.8F, -1.1096F, 0.4636F, 3.1416F));

		PartDefinition tail_feather_l_1_r1 = tail.addOrReplaceChild("tail_feather_l_1_r1", CubeListBuilder.create().texOffs(416, 2).addBox(-1.0F, -9.8F, -0.5F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8F, 1.2F, 2.2F, -1.0708F, 0.2063F, 3.1416F));

		PartDefinition tail_feather_center_r1 = tail.addOrReplaceChild("tail_feather_center_r1", CubeListBuilder.create().texOffs(358, 2).addBox(-1.0F, -10.8F, -0.5F, 2.0F, 12.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.4F, 2.4F, -1.0819F, 0.0F, 3.1416F));

		PartDefinition tail_pygostyle_r1 = tail.addOrReplaceChild("tail_pygostyle_r1", CubeListBuilder.create().texOffs(280, 32).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -0.2F, -1.0839F, 0.0F, 3.1416F));

		PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(22, 46).addBox(-2.1F, 10.2F, -3.8F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(144, 32).addBox(-1.1F, 11.2F, -8.8F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(50, 46).addBox(-1.1F, 11.2F, -2.3F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -16.0F, 3.0F));

		PartDefinition leg_l_toe_3_r1 = leg_l.addOrReplaceChild("leg_l_toe_3_r1", CubeListBuilder.create().texOffs(162, 32).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8679F, 11.7F, -5.5189F, 0.0F, 0.4363F, 0.0F));

		PartDefinition leg_l_toe_1_r1 = leg_l.addOrReplaceChild("leg_l_toe_1_r1", CubeListBuilder.create().texOffs(126, 32).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6679F, 11.7F, -5.5189F, 0.0F, -0.4363F, 0.0F));

		PartDefinition leg_l_tarsus_r1 = leg_l.addOrReplaceChild("leg_l_tarsus_r1", CubeListBuilder.create().texOffs(32, 32).addBox(-1.0F, -7.2F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 5.0F, 1.2F, 0.58F, 0.0F, -3.1416F));

		PartDefinition leg_l_femur_r1 = leg_l.addOrReplaceChild("leg_l_femur_r1", CubeListBuilder.create().texOffs(56, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -0.2F, -0.2864F, 0.0F, -3.0309F));

		PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(36, 46).addBox(-0.9F, 10.2F, -3.8F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(198, 32).addBox(0.1F, 11.2F, -8.8F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(62, 46).addBox(0.1F, 11.2F, -2.3F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -16.0F, 3.0F));

		PartDefinition leg_r_toe_3_r1 = leg_r.addOrReplaceChild("leg_r_toe_3_r1", CubeListBuilder.create().texOffs(216, 32).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8679F, 11.7F, -5.5189F, 0.0F, -0.4363F, 0.0F));

		PartDefinition leg_r_toe_1_r1 = leg_r.addOrReplaceChild("leg_r_toe_1_r1", CubeListBuilder.create().texOffs(180, 32).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6679F, 11.7F, -5.5189F, 0.0F, 0.4363F, 0.0F));

		PartDefinition leg_r_tarsus_r1 = leg_r.addOrReplaceChild("leg_r_tarsus_r1", CubeListBuilder.create().texOffs(44, 32).addBox(-1.0F, -7.2F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 5.0F, 1.2F, 0.58F, 0.0F, 3.1416F));

		PartDefinition leg_r_femur_r1 = leg_r.addOrReplaceChild("leg_r_femur_r1", CubeListBuilder.create().texOffs(68, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -0.2F, -0.2864F, 0.0F, 3.0309F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(BoneRavenEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		boolean diving = entity.getDiveTicks() > 0;
		boolean deferring = entity.isDeferringToApex();

		if (diving) {
			// Teardrop raptor stoop: wings swept back, nose pitched sharply down
			addBbDeg(this.body, 36.0F, 0.0F, 0.0F);
			addBbDeg(this.neck, -14.0F, 0.0F, 0.0F);
			addBbDeg(this.head, -12.0F, Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
			addBbDeg(this.wing_l, 18.0F, -24.0F, -12.0F);
			addBbDeg(this.wing_r, 18.0F, 24.0F, 12.0F);
			addBbDeg(this.leg_l, 38.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 38.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, 10.0F, 0.0F, 0.0F);
		} else {
			// Slow ominous corvid wingbeats alternating with high circling glides
			float speed = deferring ? 0.34F : 0.48F;
			float phase = ageInTicks * speed + entity.getId();
			float flap = Mth.sin(phase) * (deferring ? 24.0F : 38.0F);
			addBbPos(this.body, 0.0F, Mth.cos(phase) * 1.1F, 0.0F);
			addBbDeg(this.body, 10.0F, 0.0F, Mth.sin(ageInTicks * 0.06F) * 4.5F);
			addBbDeg(this.neck, Mth.sin(phase) * 3.5F, Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.35F, 0.0F);
			addBbDeg(this.head, -Mth.clamp(headPitch, -25.0F, 25.0F) * 0.45F,
					Mth.clamp(netHeadYaw, -40.0F, 40.0F) * 0.5F, 0.0F);
			addBbDeg(this.wing_l, 0.0F, 0.0F, -(18.0F + flap));
			addBbDeg(this.wing_r, 0.0F, 0.0F, (18.0F + flap));
			addBbDeg(this.leg_l, 28.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_r, 28.0F, 0.0F, 0.0F);
			addBbDeg(this.tail, -8.0F + Mth.cos(phase) * 4.0F, Mth.sin(ageInTicks * 0.09F) * 4.0F, 0.0F);
		}
	}
}
