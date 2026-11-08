package ru.aiforge.riftworld.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.EntityModel;
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
import ru.aiforge.riftworld.entity.PegasusEntity;

/**
 * Generated via Blockbench MCP headless 1.10.1 (`bbmodel_export_modded_entity`, Mojmap 1.17+ / 1.20.1):
 * 37 articulated bones (including 3-fan feather scissoring sub-bones for zero-clipping Z-folded wings),
 * 179 sculpted cuboids, 512x512 zero-overlap UV atlas, and full 9-state biomechanical animation blending:
 *   1. Living idle (breathing, asymmetric ear flicks, S-wave Arabian tail)
 *   2. Pasture grazing (arched neck reach + jaw chewing)
 *   3. 4-beat equine walk (anatomical carpus/tarsus swing flexion + 2x head nod)
 *   4. Asymmetric 3-beat canter/gallop (suspension bounce + lead/trailing stride + streaming tail)
 *   5. Heroic stallion rear (hindleg stand, foreleg pawing, neigh jaw, heraldic half-unfurl)
 *   6. Smooth takeoff / landing 3-stage wing unfurling & folding
 *   7. Powered avian figure-8 flap (wrist-flexed upstroke + body lift heave + tucked hooves)
 *   8. Thermal dihedral soar/glide
 *   9. Mid-air hover (nose-up posture + fanned tail brake + rapid hover wingbeat)
 */
public class PegasusBlockbenchModel extends EntityModel<PegasusEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "pegasus_bb"), "main");

	private record RestPose(ModelPart part, float px, float py, float pz, float rx, float ry, float rz) {}

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart saddle_tack;
	private final ModelPart neck_lower;
	private final ModelPart neck_upper;
	private final ModelPart head;
	private final ModelPart head_tack;
	private final ModelPart jaw;
	private final ModelPart ear_l;
	private final ModelPart ear_r;
	private final ModelPart leg_front_upper_l;
	private final ModelPart leg_front_cannon_l;
	private final ModelPart leg_front_hoof_l;
	private final ModelPart leg_front_upper_r;
	private final ModelPart leg_front_cannon_r;
	private final ModelPart leg_front_hoof_r;
	private final ModelPart leg_back_thigh_l;
	private final ModelPart leg_back_cannon_l;
	private final ModelPart leg_back_hoof_l;
	private final ModelPart leg_back_thigh_r;
	private final ModelPart leg_back_cannon_r;
	private final ModelPart leg_back_hoof_r;
	private final ModelPart wing_1_l;
	private final ModelPart wing_1_feathers_l;
	private final ModelPart wing_2_l;
	private final ModelPart wing_2_feathers_l;
	private final ModelPart wing_3_l;
	private final ModelPart wing_3_fan_l;
	private final ModelPart wing_1_r;
	private final ModelPart wing_1_feathers_r;
	private final ModelPart wing_2_r;
	private final ModelPart wing_2_feathers_r;
	private final ModelPart wing_3_r;
	private final ModelPart wing_3_fan_r;
	private final ModelPart tail_1;
	private final ModelPart tail_2;
	private final ModelPart tail_3;
	private final List<RestPose> restPoses;

	public PegasusBlockbenchModel(ModelPart bakedRoot) {
		this.root = bakedRoot.getChild("root");
		this.body = this.root.getChild("body");
		this.saddle_tack = this.body.getChild("saddle_tack");
		this.neck_lower = this.body.getChild("neck_lower");
		this.neck_upper = this.neck_lower.getChild("neck_upper");
		this.head = this.neck_upper.getChild("head");
		this.head_tack = this.head.getChild("head_tack");
		this.jaw = this.head.getChild("jaw");
		this.ear_l = this.head.getChild("ear_l");
		this.ear_r = this.head.getChild("ear_r");
		this.leg_front_upper_l = this.body.getChild("leg_front_upper_l");
		this.leg_front_cannon_l = this.leg_front_upper_l.getChild("leg_front_cannon_l");
		this.leg_front_hoof_l = this.leg_front_cannon_l.getChild("leg_front_hoof_l");
		this.leg_front_upper_r = this.body.getChild("leg_front_upper_r");
		this.leg_front_cannon_r = this.leg_front_upper_r.getChild("leg_front_cannon_r");
		this.leg_front_hoof_r = this.leg_front_cannon_r.getChild("leg_front_hoof_r");
		this.leg_back_thigh_l = this.body.getChild("leg_back_thigh_l");
		this.leg_back_cannon_l = this.leg_back_thigh_l.getChild("leg_back_cannon_l");
		this.leg_back_hoof_l = this.leg_back_cannon_l.getChild("leg_back_hoof_l");
		this.leg_back_thigh_r = this.body.getChild("leg_back_thigh_r");
		this.leg_back_cannon_r = this.leg_back_thigh_r.getChild("leg_back_cannon_r");
		this.leg_back_hoof_r = this.leg_back_cannon_r.getChild("leg_back_hoof_r");
		this.wing_1_l = this.body.getChild("wing_1_l");
		this.wing_1_feathers_l = this.wing_1_l.getChild("wing_1_feathers_l");
		this.wing_2_l = this.wing_1_l.getChild("wing_2_l");
		this.wing_2_feathers_l = this.wing_2_l.getChild("wing_2_feathers_l");
		this.wing_3_l = this.wing_2_l.getChild("wing_3_l");
		this.wing_3_fan_l = this.wing_3_l.getChild("wing_3_fan_l");
		this.wing_1_r = this.body.getChild("wing_1_r");
		this.wing_1_feathers_r = this.wing_1_r.getChild("wing_1_feathers_r");
		this.wing_2_r = this.wing_1_r.getChild("wing_2_r");
		this.wing_2_feathers_r = this.wing_2_r.getChild("wing_2_feathers_r");
		this.wing_3_r = this.wing_2_r.getChild("wing_3_r");
		this.wing_3_fan_r = this.wing_3_r.getChild("wing_3_fan_r");
		this.tail_1 = this.body.getChild("tail_1");
		this.tail_2 = this.tail_1.getChild("tail_2");
		this.tail_3 = this.tail_2.getChild("tail_3");
		this.restPoses = this.root.getAllParts()
				.map(p -> new RestPose(p, p.x, p.y, p.z, p.xRot, p.yRot, p.zRot))
				.toList();
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-5.5F, -4.5F, -10.5F, 11.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(68, 2).addBox(-4.5F, -5.5F, -7.5F, 9.0F, 2.0F, 17.0F, new CubeDeformation(0.08F))
		.texOffs(184, 2).addBox(-4.5F, 1.8F, -11.5F, 9.0F, 3.0F, 13.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition haunch_muscle_r_r1 = body.addOrReplaceChild("haunch_muscle_r_r1", CubeListBuilder.create().texOffs(470, 2).mirror().addBox(-2.0F, -4.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(4.0F, -0.5F, 10.5F, 0.0698F, 0.0F, -0.0524F));

		PartDefinition haunch_muscle_l_r1 = body.addOrReplaceChild("haunch_muscle_l_r1", CubeListBuilder.create().texOffs(446, 2).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-4.0F, -0.5F, 10.5F, 0.0698F, 0.0F, 0.0524F));

		PartDefinition croup_core_r1 = body.addOrReplaceChild("croup_core_r1", CubeListBuilder.create().texOffs(312, 2).addBox(-5.0F, -4.0F, -3.0F, 10.0F, 8.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, -1.5F, 11.5F, 0.1396F, 0.0F, 0.0F));

		PartDefinition covert_wing_socket_r_r1 = body.addOrReplaceChild("covert_wing_socket_r_r1", CubeListBuilder.create().texOffs(124, 68).mirror().addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(6.0F, -2.8F, -6.8F, 0.1396F, 0.0F, 0.1047F));

		PartDefinition covert_wing_socket_l_r1 = body.addOrReplaceChild("covert_wing_socket_l_r1", CubeListBuilder.create().texOffs(188, 67).addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-6.0F, -2.8F, -6.8F, 0.1396F, 0.0F, -0.1047F));

		PartDefinition scapula_shoulder_r_r1 = body.addOrReplaceChild("scapula_shoulder_r_r1", CubeListBuilder.create().texOffs(124, 23).mirror().addBox(-1.0F, -3.5F, -3.0F, 2.0F, 7.0F, 6.0F, new CubeDeformation(0.12F)).mirror(false), PartPose.offsetAndRotation(5.0F, -1.5F, -8.8F, 0.2793F, 0.0F, 0.0698F));

		PartDefinition scapula_shoulder_l_r1 = body.addOrReplaceChild("scapula_shoulder_l_r1", CubeListBuilder.create().texOffs(494, 20).addBox(-1.0F, -3.5F, -3.0F, 2.0F, 7.0F, 6.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-5.0F, -1.5F, -8.8F, 0.2793F, 0.0F, -0.0698F));

		PartDefinition pectoral_muscle_r_r1 = body.addOrReplaceChild("pectoral_muscle_r_r1", CubeListBuilder.create().texOffs(48, 50).mirror().addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(3.0F, 0.5F, -11.4F, -0.1047F, 0.1222F, 0.0F));

		PartDefinition pectoral_muscle_l_r1 = body.addOrReplaceChild("pectoral_muscle_l_r1", CubeListBuilder.create().texOffs(28, 50).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-3.0F, 0.5F, -11.4F, -0.1047F, -0.1222F, 0.0F));

		PartDefinition chest_front_r1 = body.addOrReplaceChild("chest_front_r1", CubeListBuilder.create().texOffs(458, 37).addBox(-5.0F, -3.5F, -2.0F, 10.0F, 7.0F, 4.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, -0.3F, -11.0F, -0.1571F, 0.0F, 0.0F));

		PartDefinition tucked_flank_r1 = body.addOrReplaceChild("tucked_flank_r1", CubeListBuilder.create().texOffs(284, 20).addBox(-4.0F, -1.5F, -5.0F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.5F, 6.0F, -0.1222F, 0.0F, 0.0F));

		PartDefinition withers_arch_r1 = body.addOrReplaceChild("withers_arch_r1", CubeListBuilder.create().texOffs(418, 52).addBox(-4.0F, -1.5F, -3.0F, 8.0F, 3.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -5.3F, -8.5F, 0.1745F, 0.0F, 0.0F));

		PartDefinition saddle_tack = body.addOrReplaceChild("saddle_tack", CubeListBuilder.create().texOffs(264, 2).addBox(-5.0F, -2.4F, -5.0F, 10.0F, 2.0F, 12.0F, new CubeDeformation(0.18F))
		.texOffs(418, 37).addBox(-4.5F, -3.0F, -3.5F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.12F))
		.texOffs(110, 79).addBox(-4.5F, 7.1F, -3.5F, 9.0F, 2.0F, 3.0F, new CubeDeformation(0.22F))
		.texOffs(394, 37).addBox(-5.5F, -0.5F, -3.5F, 1.0F, 9.0F, 3.0F, new CubeDeformation(0.2F))
		.texOffs(406, 37).mirror().addBox(4.5F, -0.5F, -3.5F, 1.0F, 9.0F, 3.0F, new CubeDeformation(0.2F)).mirror(false)
		.texOffs(132, 55).addBox(-6.2F, -0.5F, -1.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.08F))
		.texOffs(68, 56).mirror().addBox(5.2F, -0.5F, -1.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.08F)).mirror(false)
		.texOffs(50, 104).addBox(-6.0F, 7.5F, -2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.14F))
		.texOffs(60, 104).mirror().addBox(5.0F, 7.5F, -2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offset(0.0F, -4.0F, -1.0F));

		PartDefinition chest_gem_r1 = saddle_tack.addOrReplaceChild("chest_gem_r1", CubeListBuilder.create().texOffs(110, 104).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.24F)), PartPose.offsetAndRotation(0.0F, 4.0F, -12.6F, -0.1571F, 0.0F, 0.0F));

		PartDefinition chest_medallion_r1 = saddle_tack.addOrReplaceChild("chest_medallion_r1", CubeListBuilder.create().texOffs(498, 95).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 4.0F, -12.4F, -0.1571F, 0.0F, 0.0F));

		PartDefinition breastcollar_r_r1 = saddle_tack.addOrReplaceChild("breastcollar_r_r1", CubeListBuilder.create().texOffs(418, 65).mirror().addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offsetAndRotation(3.5F, 3.8F, -9.6F, -0.2793F, 0.384F, 0.0F));

		PartDefinition breastcollar_l_r1 = saddle_tack.addOrReplaceChild("breastcollar_l_r1", CubeListBuilder.create().texOffs(44, 64).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.18F)), PartPose.offsetAndRotation(-3.5F, 3.8F, -9.6F, -0.2793F, -0.384F, 0.0F));

		PartDefinition saddle_cantle_r1 = saddle_tack.addOrReplaceChild("saddle_cantle_r1", CubeListBuilder.create().texOffs(44, 87).addBox(-4.0F, -1.5F, -1.0F, 8.0F, 3.0F, 2.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -3.0F, 5.5F, -0.3491F, 0.0F, 0.0F));

		PartDefinition saddle_pommel_r1 = saddle_tack.addOrReplaceChild("saddle_pommel_r1", CubeListBuilder.create().texOffs(228, 87).addBox(-3.5F, -1.5F, -1.0F, 7.0F, 3.0F, 2.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, 0.2793F, 0.0F, 0.0F));

		PartDefinition saddle_cloth_skirt_r_r1 = saddle_tack.addOrReplaceChild("saddle_cloth_skirt_r_r1", CubeListBuilder.create().texOffs(154, 2).mirror().addBox(-1.0F, -3.0F, -5.5F, 2.0F, 6.0F, 11.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(5.2F, 1.5F, 1.0F, 0.0F, 0.0F, 0.0698F));

		PartDefinition saddle_cloth_skirt_l_r1 = saddle_tack.addOrReplaceChild("saddle_cloth_skirt_l_r1", CubeListBuilder.create().texOffs(124, 2).addBox(-1.0F, -3.0F, -5.5F, 2.0F, 6.0F, 11.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-5.2F, 1.5F, 1.0F, 0.0F, 0.0F, -0.0698F));

		PartDefinition neck_lower = body.addOrReplaceChild("neck_lower", CubeListBuilder.create().texOffs(232, 2).addBox(-4.0F, -7.5F, -3.0F, 8.0F, 9.0F, 6.0F, new CubeDeformation(0.08F))
		.texOffs(420, 2).addBox(-3.0F, -8.5F, -2.0F, 6.0F, 9.0F, 5.0F, new CubeDeformation(0.04F))
		.texOffs(490, 37).addBox(-2.5F, -6.3F, -3.7F, 5.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(218, 22).addBox(-1.0F, -8.5F, 2.3F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -2.5F, -10.5F, 0.4189F, 0.0F, 0.0F));

		PartDefinition mane_low_lock2_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock2_r_r1", CubeListBuilder.create().texOffs(362, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -5.8F, 3.2F, 0.1396F, 0.1745F, 0.3491F));

		PartDefinition mane_low_lock2_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock2_l_r1", CubeListBuilder.create().texOffs(348, 77).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -5.8F, 3.2F, 0.1396F, -0.1745F, -0.3491F));

		PartDefinition mane_low_lock1_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock1_r_r1", CubeListBuilder.create().texOffs(30, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -3.4F, 3.2F, 0.1396F, 0.1745F, 0.2967F));

		PartDefinition mane_low_lock1_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock1_l_r1", CubeListBuilder.create().texOffs(16, 77).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -3.4F, 3.2F, 0.1396F, -0.1745F, -0.2967F));

		PartDefinition mane_low_lock0_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock0_r_r1", CubeListBuilder.create().texOffs(2, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -1.0F, 3.2F, 0.1396F, 0.1745F, 0.2443F));

		PartDefinition mane_low_lock0_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock0_l_r1", CubeListBuilder.create().texOffs(62, 76).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -1.0F, 3.2F, 0.1396F, -0.1745F, -0.2443F));

		PartDefinition neck_upper = neck_lower.addOrReplaceChild("neck_upper", CubeListBuilder.create().texOffs(232, 21).addBox(-3.0F, -7.5F, -2.7F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.1F))
		.texOffs(442, 65).addBox(-2.5F, -8.5F, -2.4F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.06F))
		.texOffs(380, 37).addBox(-1.0F, -8.5F, 1.8F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -6.5F, -0.3F, -0.2793F, 0.0F, 0.0F));

		PartDefinition mane_up_lock2_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock2_r_r1", CubeListBuilder.create().texOffs(446, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -6.6F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock2_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock2_l_r1", CubeListBuilder.create().texOffs(432, 77).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -6.6F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition mane_up_lock1_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock1_r_r1", CubeListBuilder.create().texOffs(418, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -4.2F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock1_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock1_l_r1", CubeListBuilder.create().texOffs(404, 77).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -4.2F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition mane_up_lock0_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock0_r_r1", CubeListBuilder.create().texOffs(390, 77).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -1.8F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock0_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock0_l_r1", CubeListBuilder.create().texOffs(376, 77).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -1.8F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition head = neck_upper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(180, 39).addBox(-3.0F, -3.2F, -5.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.12F))
		.texOffs(400, 53).addBox(-4.0F, -1.0F, -4.0F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.1F))
		.texOffs(26, 64).mirror().addBox(2.0F, -1.0F, -4.0F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.1F)).mirror(false)
		.texOffs(396, 66).addBox(-2.0F, -1.0F, -13.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.08F))
		.texOffs(70, 104).addBox(-2.2F, -0.6F, -12.8F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.16F))
		.texOffs(80, 104).mirror().addBox(1.2F, -0.6F, -12.8F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false)
		.texOffs(474, 87).addBox(-3.2F, -1.8F, -5.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.16F))
		.texOffs(486, 87).mirror().addBox(2.2F, -1.8F, -5.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, -6.0F, -0.4F, 0.2618F, 0.0F, 0.0F));

		PartDefinition forelock_tip_r1 = head.addOrReplaceChild("forelock_tip_r1", CubeListBuilder.create().texOffs(460, 87).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.8F, -6.5F, 0.384F, 0.0F, 0.0F));

		PartDefinition forelock_top_r1 = head.addOrReplaceChild("forelock_top_r1", CubeListBuilder.create().texOffs(44, 76).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -2.8F, -3.1F, 0.2094F, 0.0F, 0.0F));

		PartDefinition brow_lash_r_r1 = head.addOrReplaceChild("brow_lash_r_r1", CubeListBuilder.create().texOffs(448, 96).mirror().addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.12F)).mirror(false), PartPose.offsetAndRotation(2.7F, -1.9F, -3.6F, 0.0F, 0.0F, 0.1396F));

		PartDefinition brow_lash_l_r1 = head.addOrReplaceChild("brow_lash_l_r1", CubeListBuilder.create().texOffs(436, 96).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-2.7F, -1.9F, -3.6F, 0.0F, 0.0F, -0.1396F));

		PartDefinition nasal_bridge_r1 = head.addOrReplaceChild("nasal_bridge_r1", CubeListBuilder.create().texOffs(450, 52).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.22F)), PartPose.offsetAndRotation(0.0F, -0.1F, -7.0F, 0.0698F, 0.0F, 0.0F));

		PartDefinition head_tack = head.addOrReplaceChild("head_tack", CubeListBuilder.create().texOffs(498, 87).addBox(-2.0F, -1.0F, -10.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.3F))
		.texOffs(136, 104).addBox(-3.0F, -2.4F, -4.4F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.26F))
		.texOffs(154, 104).addBox(-1.0F, -2.4F, -5.1F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.28F))
		.texOffs(164, 104).addBox(-3.1F, 1.0F, -10.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.28F))
		.texOffs(172, 104).mirror().addBox(2.1F, 1.0F, -10.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.28F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 1.8F, -4.3F));

		PartDefinition mandible_equine_r1 = jaw.addOrReplaceChild("mandible_equine_r1", CubeListBuilder.create().texOffs(2, 50).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.6F, -4.1F, -0.0349F, 0.0F, 0.0F));

		PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(90, 104).addBox(-1.4F, -2.6F, -0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.06F))
		.texOffs(120, 104).addBox(-0.7F, -4.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-2.1F, -2.4F, 0.0F, -0.2094F, -0.2793F, 0.2793F));

		PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(100, 104).mirror().addBox(-0.6F, -2.6F, -0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.06F)).mirror(false)
		.texOffs(128, 104).mirror().addBox(-0.3F, -4.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(2.1F, -2.4F, 0.0F, -0.2094F, 0.2793F, -0.2793F));

		PartDefinition leg_front_upper_l = body.addOrReplaceChild("leg_front_upper_l", CubeListBuilder.create().texOffs(250, 87).addBox(-1.5F, 8.3F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.28F)), PartPose.offset(-3.8F, 0.5F, -9.8F));

		PartDefinition fore_arm_l_r1 = leg_front_upper_l.addOrReplaceChild("fore_arm_l_r1", CubeListBuilder.create().texOffs(164, 23).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.22F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.0524F, 0.0F, 0.0F));

		PartDefinition leg_front_cannon_l = leg_front_upper_l.addOrReplaceChild("leg_front_cannon_l", CubeListBuilder.create().texOffs(304, 66).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.18F)), PartPose.offset(0.0F, 9.7F, 0.0F));

		PartDefinition leg_front_hoof_l = leg_front_cannon_l.addOrReplaceChild("leg_front_hoof_l", CubeListBuilder.create().texOffs(460, 96).addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F))
		.texOffs(282, 87).addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition fore_pastern_l_r1 = leg_front_hoof_l.addOrReplaceChild("fore_pastern_l_r1", CubeListBuilder.create().texOffs(484, 96).addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)), PartPose.offsetAndRotation(0.0F, 1.3F, -0.5F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_f_l_r1 = leg_front_hoof_l.addOrReplaceChild("fetlock_feather_f_l_r1", CubeListBuilder.create().texOffs(172, 78).addBox(-1.0F, -0.5F, -0.4F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.3F, 0.8F, -0.4538F, 0.0F, 0.0F));

		PartDefinition leg_front_upper_r = body.addOrReplaceChild("leg_front_upper_r", CubeListBuilder.create().texOffs(266, 87).mirror().addBox(-1.5F, 8.3F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.28F)).mirror(false), PartPose.offset(3.8F, 0.5F, -9.8F));

		PartDefinition fore_arm_r_r1 = leg_front_upper_r.addOrReplaceChild("fore_arm_r_r1", CubeListBuilder.create().texOffs(68, 25).mirror().addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.22F)).mirror(false), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.0524F, 0.0F, 0.0F));

		PartDefinition leg_front_cannon_r = leg_front_upper_r.addOrReplaceChild("leg_front_cannon_r", CubeListBuilder.create().texOffs(316, 66).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offset(0.0F, 9.7F, 0.0F));

		PartDefinition leg_front_hoof_r = leg_front_cannon_r.addOrReplaceChild("leg_front_hoof_r", CubeListBuilder.create().texOffs(472, 96).mirror().addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F)).mirror(false)
		.texOffs(298, 87).mirror().addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)).mirror(false), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition fore_pastern_r_r1 = leg_front_hoof_r.addOrReplaceChild("fore_pastern_r_r1", CubeListBuilder.create().texOffs(496, 103).mirror().addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, 1.3F, -0.5F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_f_r_r1 = leg_front_hoof_r.addOrReplaceChild("fetlock_feather_f_r_r1", CubeListBuilder.create().texOffs(186, 78).mirror().addBox(-1.0F, -0.5F, -0.4F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.3F, 0.8F, -0.4538F, 0.0F, 0.0F));

		PartDefinition leg_back_thigh_l = body.addOrReplaceChild("leg_back_thigh_l", CubeListBuilder.create().texOffs(140, 78).addBox(-1.5F, 7.4F, -0.2F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(-4.0F, 0.0F, 10.0F));

		PartDefinition hind_gaskin_l_r1 = leg_back_thigh_l.addOrReplaceChild("hind_gaskin_l_r1", CubeListBuilder.create().texOffs(494, 2).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-0.2F, 3.8F, 0.4F, 0.1222F, 0.0F, 0.0F));

		PartDefinition leg_back_cannon_l = leg_back_thigh_l.addOrReplaceChild("leg_back_cannon_l", CubeListBuilder.create(), PartPose.offset(0.0F, 9.6F, 1.2F));

		PartDefinition hind_cannon_l_r1 = leg_back_cannon_l.addOrReplaceChild("hind_cannon_l_r1", CubeListBuilder.create().texOffs(498, 52).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.18F)), PartPose.offsetAndRotation(0.0F, 3.1F, -0.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition leg_back_hoof_l = leg_back_cannon_l.addOrReplaceChild("leg_back_hoof_l", CubeListBuilder.create().texOffs(2, 104).addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F))
		.texOffs(314, 87).addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)), PartPose.offset(0.0F, 6.6F, -0.4F));

		PartDefinition hind_pastern_l_r1 = leg_back_hoof_l.addOrReplaceChild("hind_pastern_l_r1", CubeListBuilder.create().texOffs(26, 104).addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)), PartPose.offsetAndRotation(0.0F, 1.3F, -0.4F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_b_l_r1 = leg_back_hoof_l.addOrReplaceChild("fetlock_feather_b_l_r1", CubeListBuilder.create().texOffs(200, 78).addBox(-1.0F, -0.5F, -0.3F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.3F, 0.7F, -0.4887F, 0.0F, 0.0F));

		PartDefinition leg_back_thigh_r = body.addOrReplaceChild("leg_back_thigh_r", CubeListBuilder.create().texOffs(156, 78).mirror().addBox(-1.5F, 7.4F, -0.2F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(4.0F, 0.0F, 10.0F));

		PartDefinition hind_gaskin_r_r1 = leg_back_thigh_r.addOrReplaceChild("hind_gaskin_r_r1", CubeListBuilder.create().texOffs(264, 20).mirror().addBox(-2.0F, -5.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(0.2F, 3.8F, 0.4F, 0.1222F, 0.0F, 0.0F));

		PartDefinition leg_back_cannon_r = leg_back_thigh_r.addOrReplaceChild("leg_back_cannon_r", CubeListBuilder.create(), PartPose.offset(0.0F, 9.6F, 1.2F));

		PartDefinition hind_cannon_r_r1 = leg_back_cannon_r.addOrReplaceChild("hind_cannon_r_r1", CubeListBuilder.create().texOffs(192, 54).mirror().addBox(-1.0F, -3.5F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offsetAndRotation(0.0F, 3.1F, -0.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition leg_back_hoof_r = leg_back_cannon_r.addOrReplaceChild("leg_back_hoof_r", CubeListBuilder.create().texOffs(14, 104).mirror().addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F)).mirror(false)
		.texOffs(330, 87).mirror().addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)).mirror(false), PartPose.offset(0.0F, 6.6F, -0.4F));

		PartDefinition hind_pastern_r_r1 = leg_back_hoof_r.addOrReplaceChild("hind_pastern_r_r1", CubeListBuilder.create().texOffs(38, 104).mirror().addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, 1.3F, -0.4F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_b_r_r1 = leg_back_hoof_r.addOrReplaceChild("fetlock_feather_b_r_r1", CubeListBuilder.create().texOffs(214, 78).mirror().addBox(-1.0F, -0.5F, -0.3F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.3F, 0.7F, -0.4887F, 0.0F, 0.0F));

		PartDefinition wing_1_l = body.addOrReplaceChild("wing_1_l", CubeListBuilder.create().texOffs(304, 78).addBox(-12.0F, -1.2F, -1.2F, 12.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-6.5F, -2.4F, -7.2F, -0.0698F, 0.1396F, 0.2443F));

		PartDefinition covert_shoulder_cap_l_r1 = wing_1_l.addOrReplaceChild("covert_shoulder_cap_l_r1", CubeListBuilder.create().texOffs(460, 77).addBox(-3.5F, -1.0F, -2.0F, 7.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-3.5F, -0.6F, 1.1F, 0.1047F, 0.0F, 0.0F));

		PartDefinition wing_1_feathers_l = wing_1_l.addOrReplaceChild("wing_1_feathers_l", CubeListBuilder.create(), PartPose.offset(-3.0F, 0.4F, 1.4F));

		PartDefinition secondary_inner_3_l_r1 = wing_1_feathers_l.addOrReplaceChild("secondary_inner_3_l_r1", CubeListBuilder.create().texOffs(324, 20).addBox(-1.7F, -0.6F, -4.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-6.8F, 0.4F, 7.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_3_l_r1 = wing_1_feathers_l.addOrReplaceChild("covert_inner_3_l_r1", CubeListBuilder.create().texOffs(232, 53).addBox(-1.7F, -1.1F, -2.9F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-6.8F, -0.5F, 2.3F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_2_l_r1 = wing_1_feathers_l.addOrReplaceChild("secondary_inner_2_l_r1", CubeListBuilder.create().texOffs(84, 25).addBox(-1.7F, -0.6F, -4.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.1F, 0.4F, 7.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_2_l_r1 = wing_1_feathers_l.addOrReplaceChild("covert_inner_2_l_r1", CubeListBuilder.create().texOffs(204, 66).addBox(-1.7F, -1.1F, -2.9F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-4.1F, -0.5F, 2.3F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_1_l_r1 = wing_1_feathers_l.addOrReplaceChild("secondary_inner_1_l_r1", CubeListBuilder.create().texOffs(218, 38).addBox(-1.7F, -0.6F, -4.5F, 3.0F, 1.0F, 10.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-1.4F, 0.4F, 7.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_1_l_r1 = wing_1_feathers_l.addOrReplaceChild("covert_inner_1_l_r1", CubeListBuilder.create().texOffs(474, 52).addBox(-1.7F, -1.1F, -2.9F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-1.4F, -0.5F, 2.3F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_0_l_r1 = wing_1_feathers_l.addOrReplaceChild("secondary_inner_0_l_r1", CubeListBuilder.create().texOffs(144, 40).addBox(-1.7F, -0.6F, -4.5F, 3.0F, 1.0F, 9.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(1.3F, 0.4F, 7.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_0_l_r1 = wing_1_feathers_l.addOrReplaceChild("covert_inner_0_l_r1", CubeListBuilder.create().texOffs(466, 65).addBox(-1.7F, -1.1F, -2.9F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(1.3F, -0.5F, 2.3F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition wing_2_l = wing_1_l.addOrReplaceChild("wing_2_l", CubeListBuilder.create().texOffs(228, 78).addBox(-13.6F, -1.2F, -1.2F, 14.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-11.5F, 0.0F, 0.0F, -0.0349F, 0.1047F, 0.1047F));

		PartDefinition alula_tuft_l_r1 = wing_2_l.addOrReplaceChild("alula_tuft_l_r1", CubeListBuilder.create().texOffs(396, 96).addBox(-4.5F, -0.5F, -1.3F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-11.3F, -0.1F, -1.3F, -0.0698F, -0.2443F, 0.1047F));

		PartDefinition wing_2_feathers_l = wing_2_l.addOrReplaceChild("wing_2_feathers_l", CubeListBuilder.create(), PartPose.offset(-4.5F, 0.4F, 1.4F));

		PartDefinition secondary_mid_4_l_r1 = wing_2_feathers_l.addOrReplaceChild("secondary_mid_4_l_r1", CubeListBuilder.create().texOffs(316, 37).addBox(-1.7F, -0.6F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-7.4F, 0.4F, 9.3F, 0.0524F, 0.192F, 0.0F));

		PartDefinition secondary_mid_3_l_r1 = wing_2_feathers_l.addOrReplaceChild("secondary_mid_3_l_r1", CubeListBuilder.create().texOffs(460, 20).addBox(-1.7F, -0.6F, -5.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.8F, 0.4F, 9.3F, 0.0524F, 0.1658F, 0.0F));

		PartDefinition secondary_mid_2_l_r1 = wing_2_feathers_l.addOrReplaceChild("secondary_mid_2_l_r1", CubeListBuilder.create().texOffs(348, 2).addBox(-1.7F, -0.6F, -5.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-2.2F, 0.4F, 9.3F, 0.0524F, 0.1396F, 0.0F));

		PartDefinition secondary_mid_1_l_r1 = wing_2_feathers_l.addOrReplaceChild("secondary_mid_1_l_r1", CubeListBuilder.create().texOffs(392, 20).addBox(-1.7F, -0.6F, -5.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.4F, 0.4F, 9.3F, 0.0524F, 0.1134F, 0.0F));

		PartDefinition secondary_mid_0_l_r1 = wing_2_feathers_l.addOrReplaceChild("secondary_mid_0_l_r1", CubeListBuilder.create().texOffs(34, 34).addBox(-1.7F, -0.6F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(3.0F, 0.4F, 9.3F, 0.0524F, 0.0873F, 0.0F));

		PartDefinition covert_mid_4_l_r1 = wing_2_feathers_l.addOrReplaceChild("covert_mid_4_l_r1", CubeListBuilder.create().texOffs(108, 55).addBox(-1.7F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-7.4F, -0.5F, 2.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_3_l_r1 = wing_2_feathers_l.addOrReplaceChild("covert_mid_3_l_r1", CubeListBuilder.create().texOffs(168, 54).addBox(-1.7F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-4.8F, -0.5F, 2.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_2_l_r1 = wing_2_feathers_l.addOrReplaceChild("covert_mid_2_l_r1", CubeListBuilder.create().texOffs(376, 53).addBox(-1.7F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-2.2F, -0.5F, 2.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_1_l_r1 = wing_2_feathers_l.addOrReplaceChild("covert_mid_1_l_r1", CubeListBuilder.create().texOffs(328, 53).addBox(-1.7F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.4F, -0.5F, 2.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_0_l_r1 = wing_2_feathers_l.addOrReplaceChild("covert_mid_0_l_r1", CubeListBuilder.create().texOffs(280, 53).addBox(-1.7F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(3.0F, -0.5F, 2.8F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition wing_3_l = wing_2_l.addOrReplaceChild("wing_3_l", CubeListBuilder.create(), PartPose.offsetAndRotation(-13.2F, 0.0F, 0.0F, 0.0F, 0.1396F, 0.0698F));

		PartDefinition primary_3_l_r1 = wing_3_l.addOrReplaceChild("primary_3_l_r1", CubeListBuilder.create().texOffs(110, 88).addBox(-15.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-6.1F, 0.7F, 1.9F, 0.0349F, 0.6283F, -0.0175F));

		PartDefinition primary_2_l_r1 = wing_3_l.addOrReplaceChild("primary_2_l_r1", CubeListBuilder.create().texOffs(2, 88).addBox(-15.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-7.6F, 0.7F, 1.1F, 0.0349F, 0.4363F, 0.0F));

		PartDefinition primary_1_l_r1 = wing_3_l.addOrReplaceChild("primary_1_l_r1", CubeListBuilder.create().texOffs(346, 88).addBox(-14.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-8.8F, 0.7F, 0.3F, 0.0349F, 0.2618F, 0.0175F));

		PartDefinition primary_0_l_r1 = wing_3_l.addOrReplaceChild("primary_0_l_r1", CubeListBuilder.create().texOffs(158, 96).addBox(-12.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-9.6F, 0.7F, -0.5F, 0.0349F, 0.1047F, 0.0349F));

		PartDefinition covert_outer_2_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_2_l_r1", CubeListBuilder.create().texOffs(76, 68).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-8.3F, 0.4F, 3.2F, 0.0524F, 0.3142F, 0.0F));

		PartDefinition covert_outer_1_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_1_l_r1", CubeListBuilder.create().texOffs(140, 67).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-5.2F, 0.4F, 3.2F, 0.0524F, 0.2443F, 0.0F));

		PartDefinition covert_outer_0_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_0_l_r1", CubeListBuilder.create().texOffs(348, 66).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-2.1F, 0.4F, 3.2F, 0.0524F, 0.1745F, 0.0F));

		PartDefinition wing_spar_3_l_r1 = wing_3_l.addOrReplaceChild("wing_spar_3_l_r1", CubeListBuilder.create().texOffs(336, 96).addBox(-5.5F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-5.1F, -0.2F, -0.1F, 0.0F, 0.1396F, 0.0F));

		PartDefinition wing_3_fan_l = wing_3_l.addOrReplaceChild("wing_3_fan_l", CubeListBuilder.create(), PartPose.offset(-2.3F, 0.4F, 3.0F));

		PartDefinition primary_7_l_r1 = wing_3_fan_l.addOrReplaceChild("primary_7_l_r1", CubeListBuilder.create().texOffs(426, 88).addBox(-11.0F, -0.5F, -1.0F, 12.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(2.5F, 0.3F, 0.7F, 0.0349F, 1.3788F, -0.0524F));

		PartDefinition primary_6_l_r1 = wing_3_fan_l.addOrReplaceChild("primary_6_l_r1", CubeListBuilder.create().texOffs(230, 96).addBox(-12.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(1.3F, 0.3F, 0.3F, 0.0349F, 1.2043F, -0.0524F));

		PartDefinition primary_5_l_r1 = wing_3_fan_l.addOrReplaceChild("primary_5_l_r1", CubeListBuilder.create().texOffs(82, 96).addBox(-13.0F, -0.5F, -1.0F, 14.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-0.3F, 0.3F, -0.1F, 0.0349F, 1.0123F, -0.0349F));

		PartDefinition primary_4_l_r1 = wing_3_fan_l.addOrReplaceChild("primary_4_l_r1", CubeListBuilder.create().texOffs(2, 96).addBox(-14.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-2.1F, 0.3F, -0.5F, 0.0349F, 0.8203F, -0.0349F));

		PartDefinition wing_1_r = body.addOrReplaceChild("wing_1_r", CubeListBuilder.create().texOffs(76, 79).mirror().addBox(0.0F, -1.2F, -1.2F, 12.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(6.5F, -2.4F, -7.2F, -0.0698F, -0.1396F, -0.2443F));

		PartDefinition covert_shoulder_cap_r_r1 = wing_1_r.addOrReplaceChild("covert_shoulder_cap_r_r1", CubeListBuilder.create().texOffs(486, 77).mirror().addBox(-3.5F, -1.0F, -2.0F, 7.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(3.5F, -0.6F, 1.1F, 0.1047F, 0.0F, 0.0F));

		PartDefinition wing_1_feathers_r = wing_1_r.addOrReplaceChild("wing_1_feathers_r", CubeListBuilder.create(), PartPose.offset(3.0F, 0.4F, 1.4F));

		PartDefinition secondary_inner_3_r_r1 = wing_1_feathers_r.addOrReplaceChild("secondary_inner_3_r_r1", CubeListBuilder.create().texOffs(358, 20).mirror().addBox(-1.3F, -0.6F, -4.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(6.8F, 0.4F, 7.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_3_r_r1 = wing_1_feathers_r.addOrReplaceChild("covert_inner_3_r_r1", CubeListBuilder.create().texOffs(256, 53).mirror().addBox(-1.3F, -1.1F, -2.9F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(6.8F, -0.5F, 2.3F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_2_r_r1 = wing_1_feathers_r.addOrReplaceChild("secondary_inner_2_r_r1", CubeListBuilder.create().texOffs(2, 34).mirror().addBox(-1.3F, -0.6F, -4.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(4.1F, 0.4F, 7.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_2_r_r1 = wing_1_feathers_r.addOrReplaceChild("covert_inner_2_r_r1", CubeListBuilder.create().texOffs(226, 66).mirror().addBox(-1.3F, -1.1F, -2.9F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(4.1F, -0.5F, 2.3F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_1_r_r1 = wing_1_feathers_r.addOrReplaceChild("secondary_inner_1_r_r1", CubeListBuilder.create().texOffs(248, 38).mirror().addBox(-1.3F, -0.6F, -4.5F, 3.0F, 1.0F, 10.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(1.4F, 0.4F, 7.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_1_r_r1 = wing_1_feathers_r.addOrReplaceChild("covert_inner_1_r_r1", CubeListBuilder.create().texOffs(208, 53).mirror().addBox(-1.3F, -1.1F, -2.9F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(1.4F, -0.5F, 2.3F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_0_r_r1 = wing_1_feathers_r.addOrReplaceChild("secondary_inner_0_r_r1", CubeListBuilder.create().texOffs(84, 41).mirror().addBox(-1.3F, -0.6F, -4.5F, 3.0F, 1.0F, 9.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(-1.3F, 0.4F, 7.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_0_r_r1 = wing_1_feathers_r.addOrReplaceChild("covert_inner_0_r_r1", CubeListBuilder.create().texOffs(488, 65).mirror().addBox(-1.3F, -1.1F, -2.9F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(-1.3F, -0.5F, 2.3F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition wing_2_r = wing_1_r.addOrReplaceChild("wing_2_r", CubeListBuilder.create().texOffs(266, 78).mirror().addBox(-0.4F, -1.2F, -1.2F, 14.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(11.5F, 0.0F, 0.0F, -0.0349F, -0.1047F, -0.1047F));

		PartDefinition alula_tuft_r_r1 = wing_2_r.addOrReplaceChild("alula_tuft_r_r1", CubeListBuilder.create().texOffs(416, 96).mirror().addBox(-0.5F, -0.5F, -1.3F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(11.3F, -0.1F, -1.3F, -0.0698F, 0.2443F, -0.1047F));

		PartDefinition wing_2_feathers_r = wing_2_r.addOrReplaceChild("wing_2_feathers_r", CubeListBuilder.create(), PartPose.offset(4.5F, 0.4F, 1.4F));

		PartDefinition secondary_mid_4_r_r1 = wing_2_feathers_r.addOrReplaceChild("secondary_mid_4_r_r1", CubeListBuilder.create().texOffs(348, 37).mirror().addBox(-1.3F, -0.6F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(7.4F, 0.4F, 9.3F, 0.0524F, -0.192F, 0.0F));

		PartDefinition secondary_mid_3_r_r1 = wing_2_feathers_r.addOrReplaceChild("secondary_mid_3_r_r1", CubeListBuilder.create().texOffs(184, 22).mirror().addBox(-1.3F, -0.6F, -5.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(4.8F, 0.4F, 9.3F, 0.0524F, -0.1658F, 0.0F));

		PartDefinition secondary_mid_2_r_r1 = wing_2_feathers_r.addOrReplaceChild("secondary_mid_2_r_r1", CubeListBuilder.create().texOffs(384, 2).mirror().addBox(-1.3F, -0.6F, -5.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(2.2F, 0.4F, 9.3F, 0.0524F, -0.1396F, 0.0F));

		PartDefinition secondary_mid_1_r_r1 = wing_2_feathers_r.addOrReplaceChild("secondary_mid_1_r_r1", CubeListBuilder.create().texOffs(426, 20).mirror().addBox(-1.3F, -0.6F, -5.5F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(-0.4F, 0.4F, 9.3F, 0.0524F, -0.1134F, 0.0F));

		PartDefinition secondary_mid_0_r_r1 = wing_2_feathers_r.addOrReplaceChild("secondary_mid_0_r_r1", CubeListBuilder.create().texOffs(284, 37).mirror().addBox(-1.3F, -0.6F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.05F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 0.4F, 9.3F, 0.0524F, -0.0873F, 0.0F));

		PartDefinition covert_mid_4_r_r1 = wing_2_feathers_r.addOrReplaceChild("covert_mid_4_r_r1", CubeListBuilder.create().texOffs(2, 64).mirror().addBox(-1.3F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(7.4F, -0.5F, 2.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_3_r_r1 = wing_2_feathers_r.addOrReplaceChild("covert_mid_3_r_r1", CubeListBuilder.create().texOffs(84, 55).mirror().addBox(-1.3F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(4.8F, -0.5F, 2.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_2_r_r1 = wing_2_feathers_r.addOrReplaceChild("covert_mid_2_r_r1", CubeListBuilder.create().texOffs(144, 54).mirror().addBox(-1.3F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(2.2F, -0.5F, 2.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_1_r_r1 = wing_2_feathers_r.addOrReplaceChild("covert_mid_1_r_r1", CubeListBuilder.create().texOffs(352, 53).mirror().addBox(-1.3F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(-0.4F, -0.5F, 2.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_0_r_r1 = wing_2_feathers_r.addOrReplaceChild("covert_mid_0_r_r1", CubeListBuilder.create().texOffs(304, 53).mirror().addBox(-1.3F, -1.1F, -3.4F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(-3.0F, -0.5F, 2.8F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition wing_3_r = wing_2_r.addOrReplaceChild("wing_3_r", CubeListBuilder.create(), PartPose.offsetAndRotation(13.2F, 0.0F, 0.0F, 0.0F, -0.1396F, -0.0698F));

		PartDefinition primary_3_r_r1 = wing_3_r.addOrReplaceChild("primary_3_r_r1", CubeListBuilder.create().texOffs(152, 88).mirror().addBox(-1.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(6.1F, 0.7F, 1.9F, 0.0349F, -0.6283F, 0.0175F));

		PartDefinition primary_2_r_r1 = wing_3_r.addOrReplaceChild("primary_2_r_r1", CubeListBuilder.create().texOffs(68, 88).mirror().addBox(-1.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(7.6F, 0.7F, 1.1F, 0.0349F, -0.4363F, 0.0F));

		PartDefinition primary_1_r_r1 = wing_3_r.addOrReplaceChild("primary_1_r_r1", CubeListBuilder.create().texOffs(386, 88).mirror().addBox(-1.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(8.8F, 0.7F, 0.3F, 0.0349F, -0.2618F, -0.0175F));

		PartDefinition primary_0_r_r1 = wing_3_r.addOrReplaceChild("primary_0_r_r1", CubeListBuilder.create().texOffs(194, 96).mirror().addBox(-1.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(9.6F, 0.7F, -0.5F, 0.0349F, -0.1047F, -0.0349F));

		PartDefinition covert_outer_2_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_2_r_r1", CubeListBuilder.create().texOffs(100, 68).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(8.3F, 0.4F, 3.2F, 0.0524F, -0.3142F, 0.0F));

		PartDefinition covert_outer_1_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_1_r_r1", CubeListBuilder.create().texOffs(164, 67).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(5.2F, 0.4F, 3.2F, 0.0524F, -0.2443F, 0.0F));

		PartDefinition covert_outer_0_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_0_r_r1", CubeListBuilder.create().texOffs(372, 66).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(2.1F, 0.4F, 3.2F, 0.0524F, -0.1745F, 0.0F));

		PartDefinition wing_spar_3_r_r1 = wing_3_r.addOrReplaceChild("wing_spar_3_r_r1", CubeListBuilder.create().texOffs(366, 96).mirror().addBox(-5.5F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(5.1F, -0.2F, -0.1F, 0.0F, -0.1396F, 0.0F));

		PartDefinition wing_3_fan_r = wing_3_r.addOrReplaceChild("wing_3_fan_r", CubeListBuilder.create(), PartPose.offset(2.3F, 0.4F, 3.0F));

		PartDefinition primary_7_r_r1 = wing_3_fan_r.addOrReplaceChild("primary_7_r_r1", CubeListBuilder.create().texOffs(302, 96).mirror().addBox(-1.0F, -0.5F, -1.0F, 12.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(-2.5F, 0.3F, 0.7F, 0.0349F, -1.3788F, 0.0524F));

		PartDefinition primary_6_r_r1 = wing_3_fan_r.addOrReplaceChild("primary_6_r_r1", CubeListBuilder.create().texOffs(266, 96).mirror().addBox(-1.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(-1.3F, 0.3F, 0.3F, 0.0349F, -1.2043F, 0.0524F));

		PartDefinition primary_5_r_r1 = wing_3_fan_r.addOrReplaceChild("primary_5_r_r1", CubeListBuilder.create().texOffs(120, 96).mirror().addBox(-1.0F, -0.5F, -1.0F, 14.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(0.3F, 0.3F, -0.1F, 0.0349F, -1.0123F, 0.0349F));

		PartDefinition primary_4_r_r1 = wing_3_fan_r.addOrReplaceChild("primary_4_r_r1", CubeListBuilder.create().texOffs(42, 96).mirror().addBox(-1.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(2.1F, 0.3F, -0.5F, 0.0349F, -0.8203F, 0.0349F));

		PartDefinition tail_1 = body.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(248, 66).addBox(-1.5F, -1.3F, -0.7F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -3.2F, 13.5F, 0.3142F, 0.0F, 0.0F));

		PartDefinition tail_dock_lock_r_r1 = tail_1.addOrReplaceChild("tail_dock_lock_r_r1", CubeListBuilder.create().texOffs(286, 66).mirror().addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.0F, 0.7F, 2.5F, 0.1047F, 0.1745F, 0.1396F));

		PartDefinition tail_dock_lock_l_r1 = tail_1.addOrReplaceChild("tail_dock_lock_l_r1", CubeListBuilder.create().texOffs(268, 66).addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 0.7F, 2.5F, 0.1047F, -0.1745F, -0.1396F));

		PartDefinition tail_2 = tail_1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(144, 23).addBox(-2.0F, -0.7F, -1.2F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 2.7F, 3.7F, -0.2094F, 0.0F, 0.0F));

		PartDefinition tail_plume_splay_r_r1 = tail_2.addOrReplaceChild("tail_plume_splay_r_r1", CubeListBuilder.create().texOffs(130, 40).mirror().addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(2.2F, 4.0F, 0.5F, -0.0698F, 0.1396F, 0.1745F));

		PartDefinition tail_plume_splay_l_r1 = tail_2.addOrReplaceChild("tail_plume_splay_l_r1", CubeListBuilder.create().texOffs(116, 40).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-2.2F, 4.0F, 0.5F, -0.0698F, -0.1396F, -0.1745F));

		PartDefinition tail_3 = tail_2.addOrReplaceChild("tail_3", CubeListBuilder.create().texOffs(68, 42).addBox(-1.5F, -0.5F, -1.6F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 8.0F, 1.6F, -0.1047F, 0.0F, 0.0F));

		PartDefinition tail_tip_wisp_r_r1 = tail_3.addOrReplaceChild("tail_tip_wisp_r_r1", CubeListBuilder.create().texOffs(338, 66).mirror().addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(1.3F, 2.7F, -0.3F, -0.0698F, 0.1047F, 0.1047F));

		PartDefinition tail_tip_wisp_l_r1 = tail_3.addOrReplaceChild("tail_tip_wisp_l_r1", CubeListBuilder.create().texOffs(328, 66).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-1.3F, 2.7F, -0.3F, -0.0698F, -0.1047F, -0.1047F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	private static float rad(float deg) {
		return deg * ((float) Math.PI / 180.0F);
	}

	private static float smooth01(float x) {
		float t = Mth.clamp(x, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** Exact Blockbench `flip_y: true` rotation delta (`-rx, -ry, +rz` in Mojang ModelPart space). */
	private static void addBbDeg(ModelPart p, float dx, float dy, float dz) {
		p.xRot -= rad(dx);
		p.yRot -= rad(dy);
		p.zRot += rad(dz);
	}

	/** Exact Blockbench `flip_y: true` translation delta (`-dx, -dy, +dz` in Mojang ModelPart space). */
	private static void addBbPos(ModelPart p, float dx, float dy, float dz) {
		p.x -= dx;
		p.y -= dy;
		p.z += dz;
	}

	@Override
	public void setupAnim(PegasusEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		for (RestPose rp : this.restPoses) {
			rp.part.x = rp.px;
			rp.part.y = rp.py;
			rp.part.z = rp.pz;
			rp.part.xRot = rp.rx;
			rp.part.yRot = rp.ry;
			rp.part.zRot = rp.rz;
		}

		// Toggle Royal Elisar saddle & golden bridle according to entity state
		this.saddle_tack.visible = entity.hasSaddle();
		this.head_tack.visible = entity.hasSaddle() || entity.isTamed();

		float pt = entity.getRenderPartialTick();
		int showcase = entity.getShowcasePose();
		if (showcase == PegasusEntity.POSE_WALK) {
			limbSwing = ageInTicks * 0.45F + 1.1F;
			limbSwingAmount = 0.34F;
		} else if (showcase == PegasusEntity.POSE_GALLOP) {
			limbSwing = ageInTicks * 0.65F + 0.75F;
			limbSwingAmount = 0.88F;
		}

		float rawFly = entity.getFlyProgress(pt);
		float uFly = smooth01(rawFly);
		float uGlide = smooth01(entity.getGlideProgress(pt));
		float uHover = smooth01(entity.getHoverProgress(pt));
		float uRear = smooth01(entity.getRearProgress(pt)) * (1.0F - uFly);
		float uGraze = smooth01(entity.getGrazeProgress(pt)) * (1.0F - uFly) * (1.0F - uRear);
		float uGround = (1.0F - uFly) * (1.0F - uRear);

		// =========================================================================
		// 1. IDLE, LOOK, HURT/NEIGH & GRAZE HEAD-NECK ARTICULATION
		// =========================================================================
		float clampedYaw = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (1.0F - 0.65F * uGraze);
		float clampedPitch = Mth.clamp(headPitch, -25.0F, 25.0F) * (1.0F - 0.80F * uGraze);
		float breath = Mth.sin(ageInTicks * 0.065F);

		float neckLowPitch = -clampedPitch * 0.22F + breath * 1.4F;
		float neckUpPitch = -clampedPitch * 0.16F - breath * 0.9F;
		float headLocPitch = -clampedPitch * 0.42F + breath * 0.5F;
		float jawOpen = 0.0F;

		// Pasture grazing: arched neck reaches down to grass + rhythmic chewing
		if (uGraze > 0.001F) {
			float chew = 0.5F + 0.5F * Mth.sin(ageInTicks * 0.52F);
			neckLowPitch += -38.0F * uGraze;
			neckUpPitch += -24.0F * uGraze;
			headLocPitch += (20.0F - chew * 3.5F) * uGraze;
			jawOpen += (-1.5F - chew * 7.5F) * uGraze;
		}

		// Hurt / startle head-toss & whinny
		float hurtFrac = entity.hurtTime > 0 ? Mth.clamp((entity.hurtTime - pt) / 10.0F, 0.0F, 1.0F) : 0.0F;
		if (hurtFrac > 0.001F) {
			float toss = Mth.sin(hurtFrac * (float) Math.PI);
			neckLowPitch += 14.0F * toss;
			headLocPitch += -18.0F * toss;
			jawOpen += -14.0F * toss;
		}

		// Asymmetric living ear flicks + pinned ears when hurt/rearing
		float earFlickL = Mth.sin(ageInTicks * 0.09F) * 3.2F + Math.max(0.0F, Mth.sin(ageInTicks * 0.23F)) * 5.0F;
		float earFlickR = -Mth.sin(ageInTicks * 0.083F + 1.1F) * 3.0F + Math.max(0.0F, Mth.sin(ageInTicks * 0.19F + 2.4F)) * 4.5F;
		float earPin = -18.0F * Math.max(hurtFrac, uRear * 0.55F);
		addBbDeg(ear_l, earFlickL + earPin, 3.0F * uGraze, 0.0F);
		addBbDeg(ear_r, earFlickR + earPin, -3.0F * uGraze, 0.0F);

		// =========================================================================
		// 2. GROUND LOCOMOTION: 4-BEAT WALK <-> ASYMMETRIC 3-BEAT CANTER/GALLOP
		// =========================================================================
		float moveAmt = Mth.clamp(limbSwingAmount, 0.0F, 1.15F) * uGround * (1.0F - 0.85F * uGraze);
		float gallopBlend = smooth01((moveAmt - 0.38F) / 0.34F);
		float walkBlend = 1.0F - gallopBlend;

		float bodyBounceY = 0.0F;
		float bodyPitch = 0.0F;

		if (moveAmt > 0.004F) {
			float w = limbSwing * 0.6662F;

			// 4-beat equine walk phases (BL -> FL -> BR -> FR) with anatomical swing flexion
			float wFL = Mth.cos(w) * 28.0F * moveAmt;
			float wFR = Mth.cos(w + (float) Math.PI) * 28.0F * moveAmt;
			float wBL = Mth.cos(w + (float) Math.PI * 0.75F) * 25.0F * moveAmt;
			float wBR = Mth.cos(w - (float) Math.PI * 0.25F) * 25.0F * moveAmt;

			// Fore-carpus folds backward (-Rx) on forward lift; hind-hock folds smoothly on swing
			float kFL = -Math.max(0.0F, -Mth.sin(w)) * 34.0F * moveAmt;
			float kFR = -Math.max(0.0F, -Mth.sin(w + (float) Math.PI)) * 34.0F * moveAmt;
			float kBL = Math.max(0.0F, -Mth.sin(w + (float) Math.PI * 0.75F)) * 24.0F * moveAmt;
			float kBR = Math.max(0.0F, -Mth.sin(w - (float) Math.PI * 0.25F)) * 24.0F * moveAmt;

			// Asymmetric 3-beat gallop phases (lead foreleg vs trailing foreleg, suspension bounce)
			float g = limbSwing * 0.78F;
			float gFL = Mth.cos(g) * 40.0F * moveAmt;
			float gFR = Mth.cos(g + 0.65F) * 38.0F * moveAmt;
			float gBL = Mth.cos(g + (float) Math.PI + 0.20F) * 36.0F * moveAmt;
			float gBR = Mth.cos(g + (float) Math.PI + 0.78F) * 34.0F * moveAmt;

			float gkFL = -(0.25F + 0.75F * Math.max(0.0F, -Mth.sin(g))) * 46.0F * moveAmt;
			float gkFR = -(0.25F + 0.75F * Math.max(0.0F, -Mth.sin(g + 0.65F))) * 44.0F * moveAmt;
			float gkBL = (0.20F + 0.80F * Math.max(0.0F, -Mth.sin(g + (float) Math.PI + 0.20F))) * 30.0F * moveAmt;
			float gkBR = (0.20F + 0.80F * Math.max(0.0F, -Mth.sin(g + (float) Math.PI + 0.78F))) * 28.0F * moveAmt;

			addBbDeg(leg_front_upper_l, Mth.lerp(gallopBlend, wFL, gFL), 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_l, Mth.lerp(gallopBlend, kFL, gkFL), 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_l, Mth.lerp(gallopBlend, kFL * 0.30F, gkFL * 0.35F), 0.0F, 0.0F);

			addBbDeg(leg_front_upper_r, Mth.lerp(gallopBlend, wFR, gFR), 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_r, Mth.lerp(gallopBlend, kFR, gkFR), 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_r, Mth.lerp(gallopBlend, kFR * 0.30F, gkFR * 0.35F), 0.0F, 0.0F);

			addBbDeg(leg_back_thigh_l, Mth.lerp(gallopBlend, wBL, gBL), 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_l, Mth.lerp(gallopBlend, kBL, gkBL), 0.0F, 0.0F);
			addBbDeg(leg_back_hoof_l, Mth.lerp(gallopBlend, -kBL * 0.25F, -gkBL * 0.30F), 0.0F, 0.0F);

			addBbDeg(leg_back_thigh_r, Mth.lerp(gallopBlend, wBR, gBR), 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_r, Mth.lerp(gallopBlend, kBR, gkBR), 0.0F, 0.0F);
			addBbDeg(leg_back_hoof_r, Mth.lerp(gallopBlend, -kBR * 0.25F, -gkBR * 0.30F), 0.0F, 0.0F);

			// 2x walk head-neck nod vs 1x gallop suspension rock & bounce
			float walkNod = Mth.cos(w * 2.0F) * 3.2F * moveAmt * walkBlend;
			float gallopRock = Mth.cos(g) * 6.5F * moveAmt * gallopBlend;
			bodyBounceY += Mth.sin(g) * 1.15F * moveAmt * gallopBlend;
			bodyPitch += Mth.cos(g) * 4.5F * moveAmt * gallopBlend;
			neckLowPitch += walkNod - gallopRock * 1.15F - 6.0F * gallopBlend;
			headLocPitch += -walkNod * 0.8F + gallopRock * 0.85F + 5.0F * gallopBlend;
		}

		// =========================================================================
		// 3. HEROIC STALLION REAR (animation.pegasus.rear)
		// =========================================================================
		if (uRear > 0.001F) {
			float paw = Mth.sin(ageInTicks * 0.42F);
			bodyBounceY += 3.0F * uRear;
			addBbPos(body, 0.0F, 0.0F, 2.5F * uRear);
			bodyPitch += 36.0F * uRear;
			neckLowPitch += -18.0F * uRear;
			neckUpPitch += -10.0F * uRear;
			headLocPitch += -14.0F * uRear;
			jawOpen += (-12.0F - 3.0F * Math.abs(paw)) * uRear;

			// Alternating foreleg pawing in the air + braced hindlegs planted on ground
			addBbDeg(leg_front_upper_l, (42.0F + 14.0F * paw) * uRear, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_l, (-48.0F - 12.0F * paw) * uRear, 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_l, -16.0F * uRear, 0.0F, 0.0F);

			addBbDeg(leg_front_upper_r, (42.0F - 14.0F * paw) * uRear, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_r, (-48.0F + 12.0F * paw) * uRear, 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_r, -16.0F * uRear, 0.0F, 0.0F);

			addBbDeg(leg_back_thigh_l, -35.0F * uRear, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_l, -4.0F * uRear, 0.0F, 0.0F);
			addBbDeg(leg_back_thigh_r, -36.0F * uRear, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_r, -4.0F * uRear, 0.0F, 0.0F);
		}

		// =========================================================================
		// 4. AIRBORNE BODY, LEGS & TAIL (FLAP / GLIDE / HOVER)
		// =========================================================================
		float flapRate = Mth.lerp(uHover, 0.30F, 0.46F);
		float wPhase = (showcase == PegasusEntity.POSE_FLY_FLAP) ? (ageInTicks * 0.30F + 0.85F) : (ageInTicks * flapRate);

		if (uFly > 0.001F) {
			float activeBeat = 1.0F - uGlide * 0.85F;
			bodyBounceY += Mth.cos(wPhase) * 0.85F * activeBeat * uFly;
			bodyPitch += (11.0F * uHover - 2.5F * uGlide) * uFly;
			neckLowPitch += (Mth.sin(wPhase) * 3.0F * activeBeat - 5.0F * uHover) * uFly;
			headLocPitch += (-Mth.sin(wPhase) * 2.2F * activeBeat - 4.0F * uHover) * uFly;

			// Sleekly tucked equine legs in flight (slightly lower dangle during hover)
			float hoverDrop = 1.0F - 0.35F * uHover;
			addBbDeg(leg_front_upper_l, -24.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_l, -42.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_l, -14.0F * hoverDrop * uFly, 0.0F, 0.0F);

			addBbDeg(leg_front_upper_r, -20.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_r, -38.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_front_hoof_r, -12.0F * hoverDrop * uFly, 0.0F, 0.0F);

			addBbDeg(leg_back_thigh_l, -28.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_l, 18.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_back_thigh_r, -32.0F * hoverDrop * uFly, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_r, 20.0F * hoverDrop * uFly, 0.0F, 0.0F);
		}

		// Apply accumulated body & head-neck posture
		addBbPos(body, 0.0F, bodyBounceY, 0.0F);
		addBbDeg(body, bodyPitch, 0.0F, 0.0F);
		addBbDeg(neck_lower, neckLowPitch, -clampedYaw * 0.26F, 0.0F);
		addBbDeg(neck_upper, neckUpPitch, -clampedYaw * 0.22F, 0.0F);
		addBbDeg(head, headLocPitch, -clampedYaw * 0.44F, 0.0F);
		addBbDeg(jaw, jawOpen, 0.0F, 0.0F);

		// =========================================================================
		// 5. 37-BONE ZERO-CLIPPING Z-FOLD <-> TAKEOFF <-> FLAP / GLIDE / HOVER WINGS
		// =========================================================================
		// Ground Z-fold state (0 OBB penetrations against torso, saddle, legs & tail)
		float foldBreath = breath * 1.2F + hurtFrac * 4.0F;
		float gW1x = -9.2F, gW1y = -69.1F, gW1z = -97.2F + foldBreath;
		float gF1y = 78.0F;
		float gW2x = 0.0F, gW2y = 178.0F, gW2z = -6.0F;
		float gF2y = -115.0F;
		float gW3x = -2.0F, gW3y = -157.0F, gW3z = -4.0F;
		float gF3y = 54.0F;

		// Blend toward heraldic half-unfurl when rearing on ground
		if (uRear > 0.001F) {
			gW1x = Mth.lerp(uRear, gW1x, 8.0F);
			gW1y = Mth.lerp(uRear, gW1y, -12.0F);
			gW1z = Mth.lerp(uRear, gW1z, 24.0F);
			gF1y = Mth.lerp(uRear, gF1y, 0.0F);
			gW2x = Mth.lerp(uRear, gW2x, 4.0F);
			gW2y = Mth.lerp(uRear, gW2y, 10.0F);
			gW2z = Mth.lerp(uRear, gW2z, 14.0F);
			gF2y = Mth.lerp(uRear, gF2y, 0.0F);
			gW3x = Mth.lerp(uRear, gW3x, 0.0F);
			gW3y = Mth.lerp(uRear, gW3y, 4.0F);
			gW3z = Mth.lerp(uRear, gW3z, 10.0F);
			gF3y = Mth.lerp(uRear, gF3y, 0.0F);
		}

		// Airborne target state: powered figure-8 flap <-> thermal glide <-> hover + takeoff burst
		float takeoffBurst = Mth.sin(rawFly * (float) Math.PI) * 16.0F;
		float s1 = Mth.sin(wPhase);
		float s2 = Mth.sin(wPhase - 0.55F);
		float s3 = Mth.sin(wPhase - 1.05F);
		float upstrokeFlex = Math.max(0.0F, -s1); // avian wrist/hand flexion on recovery upstroke!

		// Powered flap angles (with hover amplitude boost)
		float ampBoost = 1.0F + 0.18F * uHover;
		float flpW1x = Mth.cos(wPhase) * 6.0F * ampBoost + 6.0F * uHover;
		float flpW1y = -Mth.cos(wPhase) * 5.0F + 8.0F * uHover * s1;
		float flpW1z = s1 * 28.0F * ampBoost + takeoffBurst;
		float flpW2x = Mth.cos(wPhase - 0.4F) * 4.0F;
		float flpW2y = upstrokeFlex * 18.0F;
		float flpW2z = s2 * 18.0F * ampBoost + takeoffBurst * 0.5F;
		float flpW3x = 0.0F;
		float flpW3y = upstrokeFlex * 16.0F;
		float flpW3z = s3 * 14.0F * ampBoost + takeoffBurst * 0.35F;

		// Soaring dihedral glide angles with subtle primary wind-flutter
		float tremor = Mth.sin(ageInTicks * 0.24F) * 2.0F;
		float fW1x = Mth.lerp(uGlide, flpW1x, 3.0F);
		float fW1y = Mth.lerp(uGlide, flpW1y, 5.0F);
		float fW1z = Mth.lerp(uGlide, flpW1z, 12.0F + tremor);
		float fW2x = Mth.lerp(uGlide, flpW2x, 2.0F);
		float fW2y = Mth.lerp(uGlide, flpW2y, 3.0F);
		float fW2z = Mth.lerp(uGlide, flpW2z, 8.0F + tremor * 0.7F);
		float fW3x = Mth.lerp(uGlide, flpW3x, 0.0F);
		float fW3y = Mth.lerp(uGlide, flpW3y, 2.0F);
		float fW3z = Mth.lerp(uGlide, flpW3z, 6.0F + tremor * 1.1F);

		// Final continuous blend between ground/rear wing state and airborne wing state
		float posScale = (1.0F - uFly) * (1.0F - uRear);
		addBbPos(wing_1_l, 1.48F * posScale, -0.3F * posScale, 0.4F * posScale);
		addBbPos(wing_1_r, -1.48F * posScale, -0.3F * posScale, 0.4F * posScale);

		float w1x = Mth.lerp(uFly, gW1x, fW1x);
		float w1y = Mth.lerp(uFly, gW1y, fW1y);
		float w1z = Mth.lerp(uFly, gW1z, fW1z);
		float f1y = Mth.lerp(uFly, gF1y, 0.0F);
		float w2x = Mth.lerp(uFly, gW2x, fW2x);
		float w2y = Mth.lerp(uFly, gW2y, fW2y);
		float w2z = Mth.lerp(uFly, gW2z, fW2z);
		float f2y = Mth.lerp(uFly, gF2y, 0.0F);
		float w3x = Mth.lerp(uFly, gW3x, fW3x);
		float w3y = Mth.lerp(uFly, gW3y, fW3y);
		float w3z = Mth.lerp(uFly, gW3z, fW3z);
		float f3y = Mth.lerp(uFly, gF3y, 0.0F);

		addBbDeg(wing_1_l, w1x, w1y, w1z);
		addBbDeg(wing_1_feathers_l, 0.0F, f1y, 0.0F);
		addBbDeg(wing_2_l, w2x, w2y, w2z);
		addBbDeg(wing_2_feathers_l, 0.0F, f2y, 0.0F);
		addBbDeg(wing_3_l, w3x, w3y, w3z);
		addBbDeg(wing_3_fan_l, 0.0F, f3y, 0.0F);

		addBbDeg(wing_1_r, w1x, -w1y, -w1z);
		addBbDeg(wing_1_feathers_r, 0.0F, -f1y, 0.0F);
		addBbDeg(wing_2_r, w2x, -w2y, -w2z);
		addBbDeg(wing_2_feathers_r, 0.0F, -f2y, 0.0F);
		addBbDeg(wing_3_r, w3x, -w3y, -w3z);
		addBbDeg(wing_3_fan_r, 0.0F, -f3y, 0.0F);

		// =========================================================================
		// 6. CASCADING 3-SEGMENT ARABIAN TAIL PLUME
		// =========================================================================
		float tailStream = -18.0F * gallopBlend * moveAmt - 14.0F * uFly * (1.0F - uHover) + 12.0F * uHover * uFly - 20.0F * uRear;
		float tailSwayFreq = Mth.lerp(uFly, 0.07F, 0.14F);
		float tailSway1 = Mth.cos(ageInTicks * tailSwayFreq) * (4.5F + 3.0F * moveAmt);
		float tailSway2 = Mth.sin(ageInTicks * tailSwayFreq) * (6.0F + 4.0F * moveAmt);
		float tailSway3 = Mth.sin(ageInTicks * tailSwayFreq - 0.7F) * (7.0F + 4.5F * moveAmt);

		addBbDeg(tail_1, Mth.sin(ageInTicks * 0.055F) * 2.0F + tailStream, tailSway1, 0.0F);
		addBbDeg(tail_2, tailStream * 0.55F, tailSway2, 0.0F);
		addBbDeg(tail_3, tailStream * 0.35F, tailSway3, 0.0F);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
	                           int packedOverlay, float red, float green, float blue, float alpha) {
		this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
