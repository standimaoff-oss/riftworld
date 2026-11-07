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
 * 31 articulated bones, 177 sculpted cuboids, 512x512 zero-overlap UV atlas.
 */
public class PegasusBlockbenchModel extends EntityModel<PegasusEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "pegasus_bb"), "main");

	private record RestPose(ModelPart part, float x, float y, float z) {}

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
	private final ModelPart wing_2_l;
	private final ModelPart wing_3_l;
	private final ModelPart wing_1_r;
	private final ModelPart wing_2_r;
	private final ModelPart wing_3_r;
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
		this.wing_2_l = this.wing_1_l.getChild("wing_2_l");
		this.wing_3_l = this.wing_2_l.getChild("wing_3_l");
		this.wing_1_r = this.body.getChild("wing_1_r");
		this.wing_2_r = this.wing_1_r.getChild("wing_2_r");
		this.wing_3_r = this.wing_2_r.getChild("wing_3_r");
		this.tail_1 = this.body.getChild("tail_1");
		this.tail_2 = this.tail_1.getChild("tail_2");
		this.tail_3 = this.tail_2.getChild("tail_3");
		this.restPoses = this.root.getAllParts()
				.map(p -> new RestPose(p, p.xRot, p.yRot, p.zRot))
				.toList();
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-5.5F, -4.5F, -10.5F, 11.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(68, 2).addBox(-4.5F, -5.5F, -7.5F, 9.0F, 2.0F, 17.0F, new CubeDeformation(0.08F))
		.texOffs(184, 2).addBox(-4.5F, 1.8F, -11.5F, 9.0F, 3.0F, 13.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition haunch_muscle_r_r1 = body.addOrReplaceChild("haunch_muscle_r_r1", CubeListBuilder.create().texOffs(68, 25).mirror().addBox(-2.0F, -4.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(4.0F, -0.5F, 10.5F, 0.0698F, 0.0F, -0.0524F));

		PartDefinition haunch_muscle_l_r1 = body.addOrReplaceChild("haunch_muscle_l_r1", CubeListBuilder.create().texOffs(150, 23).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 8.0F, 6.0F, new CubeDeformation(0.16F)), PartPose.offsetAndRotation(-4.0F, -0.5F, 10.5F, 0.0698F, 0.0F, 0.0524F));

		PartDefinition croup_core_r1 = body.addOrReplaceChild("croup_core_r1", CubeListBuilder.create().texOffs(360, 21).addBox(-5.0F, -4.0F, -3.0F, 10.0F, 8.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, -1.5F, 11.5F, 0.1396F, 0.0F, 0.0F));

		PartDefinition scapula_shoulder_r_r1 = body.addOrReplaceChild("scapula_shoulder_r_r1", CubeListBuilder.create().texOffs(400, 39).mirror().addBox(-1.0F, -3.5F, -3.0F, 2.0F, 7.0F, 6.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(5.0F, -1.5F, -8.8F, 0.2793F, 0.0F, 0.0698F));

		PartDefinition scapula_shoulder_l_r1 = body.addOrReplaceChild("scapula_shoulder_l_r1", CubeListBuilder.create().texOffs(380, 39).addBox(-1.0F, -3.5F, -3.0F, 2.0F, 7.0F, 6.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-5.0F, -1.5F, -8.8F, 0.2793F, 0.0F, -0.0698F));

		PartDefinition pectoral_muscle_r_r1 = body.addOrReplaceChild("pectoral_muscle_r_r1", CubeListBuilder.create().texOffs(272, 55).mirror().addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(3.0F, 0.5F, -11.4F, -0.1047F, 0.1222F, 0.0F));

		PartDefinition pectoral_muscle_l_r1 = body.addOrReplaceChild("pectoral_muscle_l_r1", CubeListBuilder.create().texOffs(44, 51).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-3.0F, 0.5F, -11.4F, -0.1047F, -0.1222F, 0.0F));

		PartDefinition chest_front_r1 = body.addOrReplaceChild("chest_front_r1", CubeListBuilder.create().texOffs(112, 41).addBox(-5.0F, -3.5F, -2.0F, 10.0F, 7.0F, 4.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, -0.3F, -11.0F, -0.1571F, 0.0F, 0.0F));

		PartDefinition tucked_flank_r1 = body.addOrReplaceChild("tucked_flank_r1", CubeListBuilder.create().texOffs(2, 34).addBox(-4.0F, -1.5F, -5.0F, 8.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.5F, 6.0F, -0.1222F, 0.0F, 0.0F));

		PartDefinition withers_arch_r1 = body.addOrReplaceChild("withers_arch_r1", CubeListBuilder.create().texOffs(472, 55).addBox(-4.0F, -1.5F, -3.0F, 8.0F, 3.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -5.3F, -8.5F, 0.1745F, 0.0F, 0.0F));

		PartDefinition saddle_tack = body.addOrReplaceChild("saddle_tack", CubeListBuilder.create().texOffs(312, 21).addBox(-5.0F, -2.4F, -5.0F, 10.0F, 2.0F, 12.0F, new CubeDeformation(0.18F))
		.texOffs(246, 40).addBox(-4.5F, -3.0F, -3.5F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.12F))
		.texOffs(254, 90).addBox(-4.5F, 7.1F, -3.5F, 9.0F, 2.0F, 3.0F, new CubeDeformation(0.22F))
		.texOffs(112, 25).addBox(-5.5F, -0.5F, -3.5F, 1.0F, 9.0F, 3.0F, new CubeDeformation(0.2F))
		.texOffs(234, 40).mirror().addBox(4.5F, -0.5F, -3.5F, 1.0F, 9.0F, 3.0F, new CubeDeformation(0.2F)).mirror(false)
		.texOffs(504, 2).addBox(-6.2F, -0.5F, -1.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.08F))
		.texOffs(504, 15).mirror().addBox(5.2F, -0.5F, -1.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.08F)).mirror(false)
		.texOffs(178, 107).addBox(-6.0F, 7.5F, -2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.14F))
		.texOffs(188, 107).mirror().addBox(5.0F, 7.5F, -2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offset(0.0F, -4.0F, -1.0F));

		PartDefinition chest_gem_r1 = saddle_tack.addOrReplaceChild("chest_gem_r1", CubeListBuilder.create().texOffs(238, 107).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.24F)), PartPose.offsetAndRotation(0.0F, 4.0F, -12.6F, -0.1571F, 0.0F, 0.0F));

		PartDefinition chest_medallion_r1 = saddle_tack.addOrReplaceChild("chest_medallion_r1", CubeListBuilder.create().texOffs(82, 100).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 4.0F, -12.4F, -0.1571F, 0.0F, 0.0F));

		PartDefinition breastcollar_r_r1 = saddle_tack.addOrReplaceChild("breastcollar_r_r1", CubeListBuilder.create().texOffs(112, 69).mirror().addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offsetAndRotation(3.5F, 3.8F, -9.6F, -0.2793F, 0.384F, 0.0F));

		PartDefinition breastcollar_l_r1 = saddle_tack.addOrReplaceChild("breastcollar_l_r1", CubeListBuilder.create().texOffs(488, 68).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.18F)), PartPose.offsetAndRotation(-3.5F, 3.8F, -9.6F, -0.2793F, -0.384F, 0.0F));

		PartDefinition saddle_cantle_r1 = saddle_tack.addOrReplaceChild("saddle_cantle_r1", CubeListBuilder.create().texOffs(86, 82).addBox(-4.0F, -1.5F, -1.0F, 8.0F, 3.0F, 2.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -3.0F, 5.5F, -0.3491F, 0.0F, 0.0F));

		PartDefinition saddle_pommel_r1 = saddle_tack.addOrReplaceChild("saddle_pommel_r1", CubeListBuilder.create().texOffs(282, 90).addBox(-3.5F, -1.5F, -1.0F, 7.0F, 3.0F, 2.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, 0.2793F, 0.0F, 0.0F));

		PartDefinition saddle_cloth_skirt_r_r1 = saddle_tack.addOrReplaceChild("saddle_cloth_skirt_r_r1", CubeListBuilder.create().texOffs(154, 2).mirror().addBox(-1.0F, -3.0F, -5.5F, 2.0F, 6.0F, 11.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(5.4F, 1.5F, 1.0F, 0.0F, 0.0F, 0.1222F));

		PartDefinition saddle_cloth_skirt_l_r1 = saddle_tack.addOrReplaceChild("saddle_cloth_skirt_l_r1", CubeListBuilder.create().texOffs(124, 2).addBox(-1.0F, -3.0F, -5.5F, 2.0F, 6.0F, 11.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-5.4F, 1.5F, 1.0F, 0.0F, 0.0F, -0.1222F));

		PartDefinition neck_lower = body.addOrReplaceChild("neck_lower", CubeListBuilder.create().texOffs(464, 2).addBox(-4.0F, -7.5F, -3.0F, 8.0F, 9.0F, 6.0F, new CubeDeformation(0.08F))
		.texOffs(124, 23).addBox(-3.0F, -8.5F, -2.0F, 6.0F, 9.0F, 5.0F, new CubeDeformation(0.04F))
		.texOffs(286, 40).addBox(-2.5F, -6.3F, -3.7F, 5.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(206, 40).addBox(-1.0F, -8.5F, 2.3F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -2.5F, -10.5F, 0.4189F, 0.0F, 0.0F));

		PartDefinition mane_low_lock2_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock2_r_r1", CubeListBuilder.create().texOffs(442, 80).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -5.8F, 3.2F, 0.1396F, 0.1745F, 0.3491F));

		PartDefinition mane_low_lock2_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock2_l_r1", CubeListBuilder.create().texOffs(428, 80).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -5.8F, 3.2F, 0.1396F, -0.1745F, -0.3491F));

		PartDefinition mane_low_lock1_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock1_r_r1", CubeListBuilder.create().texOffs(414, 80).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -3.4F, 3.2F, 0.1396F, 0.1745F, 0.2967F));

		PartDefinition mane_low_lock1_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock1_l_r1", CubeListBuilder.create().texOffs(400, 80).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -3.4F, 3.2F, 0.1396F, -0.1745F, -0.2967F));

		PartDefinition mane_low_lock0_r_r1 = neck_lower.addOrReplaceChild("mane_low_lock0_r_r1", CubeListBuilder.create().texOffs(386, 80).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, -1.0F, 3.2F, 0.1396F, 0.1745F, 0.2443F));

		PartDefinition mane_low_lock0_l_r1 = neck_lower.addOrReplaceChild("mane_low_lock0_l_r1", CubeListBuilder.create().texOffs(456, 69).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, -1.0F, 3.2F, 0.1396F, -0.1745F, -0.2443F));

		PartDefinition neck_upper = neck_lower.addOrReplaceChild("neck_upper", CubeListBuilder.create().texOffs(42, 34).addBox(-3.0F, -7.5F, -2.7F, 6.0F, 8.0F, 5.0F, new CubeDeformation(0.1F))
		.texOffs(136, 69).addBox(-2.5F, -8.5F, -2.4F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.06F))
		.texOffs(220, 40).addBox(-1.0F, -8.5F, 1.8F, 2.0F, 9.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -6.5F, -0.3F, -0.2793F, 0.0F, 0.0F));

		PartDefinition mane_up_lock2_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock2_r_r1", CubeListBuilder.create().texOffs(152, 81).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -6.6F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock2_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock2_l_r1", CubeListBuilder.create().texOffs(138, 81).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -6.6F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition mane_up_lock1_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock1_r_r1", CubeListBuilder.create().texOffs(124, 81).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -4.2F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock1_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock1_l_r1", CubeListBuilder.create().texOffs(110, 81).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -4.2F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition mane_up_lock0_r_r1 = neck_upper.addOrReplaceChild("mane_up_lock0_r_r1", CubeListBuilder.create().texOffs(488, 80).mirror().addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.6F, -1.8F, 2.9F, 0.1047F, 0.2094F, 0.2793F));

		PartDefinition mane_up_lock0_l_r1 = neck_upper.addOrReplaceChild("mane_up_lock0_l_r1", CubeListBuilder.create().texOffs(456, 80).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.6F, -1.8F, 2.9F, 0.1047F, -0.2094F, -0.2793F));

		PartDefinition head = neck_upper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 51).addBox(-3.0F, -3.2F, -5.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.12F))
		.texOffs(452, 56).addBox(-4.0F, -1.0F, -4.0F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.1F))
		.texOffs(470, 68).mirror().addBox(2.0F, -1.0F, -4.0F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.1F)).mirror(false)
		.texOffs(348, 80).addBox(-2.0F, -1.0F, -13.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.08F))
		.texOffs(198, 107).addBox(-2.2F, -0.6F, -12.8F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.16F))
		.texOffs(208, 107).mirror().addBox(1.2F, -0.6F, -12.8F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false)
		.texOffs(502, 80).addBox(-3.2F, -1.8F, -5.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.16F))
		.texOffs(336, 81).mirror().addBox(2.2F, -1.8F, -5.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, -6.0F, -0.4F, 0.2618F, 0.0F, 0.0F));

		PartDefinition forelock_tip_r1 = head.addOrReplaceChild("forelock_tip_r1", CubeListBuilder.create().texOffs(70, 91).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.8F, -6.5F, 0.384F, 0.0F, 0.0F));

		PartDefinition forelock_top_r1 = head.addOrReplaceChild("forelock_top_r1", CubeListBuilder.create().texOffs(368, 80).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, -2.8F, -3.1F, 0.2094F, 0.0F, 0.0F));

		PartDefinition brow_lash_r_r1 = head.addOrReplaceChild("brow_lash_r_r1", CubeListBuilder.create().texOffs(106, 100).mirror().addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.12F)).mirror(false), PartPose.offsetAndRotation(2.7F, -1.9F, -3.6F, 0.0F, 0.0F, 0.1396F));

		PartDefinition brow_lash_l_r1 = head.addOrReplaceChild("brow_lash_l_r1", CubeListBuilder.create().texOffs(94, 100).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(-2.7F, -1.9F, -3.6F, 0.0F, 0.0F, -0.1396F));

		PartDefinition nasal_bridge_r1 = head.addOrReplaceChild("nasal_bridge_r1", CubeListBuilder.create().texOffs(112, 56).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.22F)), PartPose.offsetAndRotation(0.0F, -0.1F, -7.0F, 0.0698F, 0.0F, 0.0F));

		PartDefinition head_tack = head.addOrReplaceChild("head_tack", CubeListBuilder.create().texOffs(68, 100).addBox(-2.0F, -1.0F, -10.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.3F))
		.texOffs(248, 107).addBox(-3.0F, -2.4F, -4.4F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.26F))
		.texOffs(266, 107).addBox(-1.0F, -2.4F, -5.1F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.28F))
		.texOffs(506, 103).addBox(-3.1F, 1.0F, -10.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.28F))
		.texOffs(276, 107).mirror().addBox(2.1F, 1.0F, -10.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.28F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 1.8F, -4.3F));

		PartDefinition mandible_equine_r1 = jaw.addOrReplaceChild("mandible_equine_r1", CubeListBuilder.create().texOffs(246, 55).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 2.0F, 8.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.6F, -4.1F, -0.0349F, 0.0F, 0.0F));

		PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(218, 107).addBox(-1.4F, -2.6F, -0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.06F))
		.texOffs(506, 89).addBox(-0.7F, -4.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-2.1F, -2.4F, 0.0F, -0.2094F, -0.2793F, 0.2793F));

		PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(228, 107).mirror().addBox(-0.6F, -2.6F, -0.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.06F)).mirror(false)
		.texOffs(506, 96).mirror().addBox(-0.3F, -4.2F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(2.1F, -2.4F, 0.0F, -0.2094F, 0.2793F, -0.2793F));

		PartDefinition leg_front_upper_l = body.addOrReplaceChild("leg_front_upper_l", CubeListBuilder.create().texOffs(470, 81).addBox(-1.5F, 8.3F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.28F)), PartPose.offset(-3.8F, 0.5F, -9.8F));

		PartDefinition fore_arm_l_r1 = leg_front_upper_l.addOrReplaceChild("fore_arm_l_r1", CubeListBuilder.create().texOffs(440, 39).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.22F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.0524F, 0.0F, 0.0F));

		PartDefinition leg_front_cannon_l = leg_front_upper_l.addOrReplaceChild("leg_front_cannon_l", CubeListBuilder.create().texOffs(304, 69).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.18F)), PartPose.offset(0.0F, 9.7F, 0.0F));

		PartDefinition leg_front_hoof_l = leg_front_cannon_l.addOrReplaceChild("leg_front_hoof_l", CubeListBuilder.create().texOffs(118, 100).addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F))
		.texOffs(320, 90).addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition fore_pastern_l_r1 = leg_front_hoof_l.addOrReplaceChild("fore_pastern_l_r1", CubeListBuilder.create().texOffs(142, 100).addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)), PartPose.offsetAndRotation(0.0F, 1.3F, -0.5F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_f_l_r1 = leg_front_hoof_l.addOrReplaceChild("fetlock_feather_f_l_r1", CubeListBuilder.create().texOffs(198, 81).addBox(-1.0F, -0.5F, -0.4F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.3F, 0.8F, -0.4538F, 0.0F, 0.0F));

		PartDefinition leg_front_upper_r = body.addOrReplaceChild("leg_front_upper_r", CubeListBuilder.create().texOffs(304, 90).mirror().addBox(-1.5F, 8.3F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.28F)).mirror(false), PartPose.offset(3.8F, 0.5F, -9.8F));

		PartDefinition fore_arm_r_r1 = leg_front_upper_r.addOrReplaceChild("fore_arm_r_r1", CubeListBuilder.create().texOffs(456, 39).mirror().addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.22F)).mirror(false), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.0524F, 0.0F, 0.0F));

		PartDefinition leg_front_cannon_r = leg_front_upper_r.addOrReplaceChild("leg_front_cannon_r", CubeListBuilder.create().texOffs(316, 69).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offset(0.0F, 9.7F, 0.0F));

		PartDefinition leg_front_hoof_r = leg_front_cannon_r.addOrReplaceChild("leg_front_hoof_r", CubeListBuilder.create().texOffs(130, 100).mirror().addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F)).mirror(false)
		.texOffs(470, 90).mirror().addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)).mirror(false), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition fore_pastern_r_r1 = leg_front_hoof_r.addOrReplaceChild("fore_pastern_r_r1", CubeListBuilder.create().texOffs(2, 107).mirror().addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, 1.3F, -0.5F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_f_r_r1 = leg_front_hoof_r.addOrReplaceChild("fetlock_feather_f_r_r1", CubeListBuilder.create().texOffs(212, 81).mirror().addBox(-1.0F, -0.5F, -0.4F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.3F, 0.8F, -0.4538F, 0.0F, 0.0F));

		PartDefinition leg_back_thigh_l = body.addOrReplaceChild("leg_back_thigh_l", CubeListBuilder.create().texOffs(166, 81).addBox(-1.5F, 7.4F, -0.2F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(-4.0F, 0.0F, 10.0F));

		PartDefinition hind_gaskin_l_r1 = leg_back_thigh_l.addOrReplaceChild("hind_gaskin_l_r1", CubeListBuilder.create().texOffs(292, 22).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-0.2F, 3.8F, 0.4F, 0.1222F, 0.0F, 0.0F));

		PartDefinition leg_back_cannon_l = leg_back_thigh_l.addOrReplaceChild("leg_back_cannon_l", CubeListBuilder.create(), PartPose.offset(0.0F, 9.6F, 1.2F));

		PartDefinition hind_cannon_l_r1 = leg_back_cannon_l.addOrReplaceChild("hind_cannon_l_r1", CubeListBuilder.create().texOffs(232, 56).addBox(-1.0F, -3.5F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.18F)), PartPose.offsetAndRotation(0.0F, 3.1F, -0.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition leg_back_hoof_l = leg_back_cannon_l.addOrReplaceChild("leg_back_hoof_l", CubeListBuilder.create().texOffs(14, 107).addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F))
		.texOffs(38, 91).addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)), PartPose.offset(0.0F, 6.6F, -0.4F));

		PartDefinition hind_pastern_l_r1 = leg_back_hoof_l.addOrReplaceChild("hind_pastern_l_r1", CubeListBuilder.create().texOffs(154, 107).addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)), PartPose.offsetAndRotation(0.0F, 1.3F, -0.4F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_b_l_r1 = leg_back_hoof_l.addOrReplaceChild("fetlock_feather_b_l_r1", CubeListBuilder.create().texOffs(226, 81).addBox(-1.0F, -0.5F, -0.3F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.3F, 0.7F, -0.4887F, 0.0F, 0.0F));

		PartDefinition leg_back_thigh_r = body.addOrReplaceChild("leg_back_thigh_r", CubeListBuilder.create().texOffs(182, 81).mirror().addBox(-1.5F, 7.4F, -0.2F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(4.0F, 0.0F, 10.0F));

		PartDefinition hind_gaskin_r_r1 = leg_back_thigh_r.addOrReplaceChild("hind_gaskin_r_r1", CubeListBuilder.create().texOffs(92, 25).mirror().addBox(-2.0F, -5.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(0.2F, 3.8F, 0.4F, 0.1222F, 0.0F, 0.0F));

		PartDefinition leg_back_cannon_r = leg_back_thigh_r.addOrReplaceChild("leg_back_cannon_r", CubeListBuilder.create(), PartPose.offset(0.0F, 9.6F, 1.2F));

		PartDefinition hind_cannon_r_r1 = leg_back_cannon_r.addOrReplaceChild("hind_cannon_r_r1", CubeListBuilder.create().texOffs(50, 65).mirror().addBox(-1.0F, -3.5F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offsetAndRotation(0.0F, 3.1F, -0.2F, -0.0698F, 0.0F, 0.0F));

		PartDefinition leg_back_hoof_r = leg_back_cannon_r.addOrReplaceChild("leg_back_hoof_r", CubeListBuilder.create().texOffs(26, 107).mirror().addBox(-1.0F, -0.6F, -1.1F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.26F)).mirror(false)
		.texOffs(54, 91).mirror().addBox(-1.5F, 1.8F, -1.8F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.04F)).mirror(false), PartPose.offset(0.0F, 6.6F, -0.4F));

		PartDefinition hind_pastern_r_r1 = leg_back_hoof_r.addOrReplaceChild("hind_pastern_r_r1", CubeListBuilder.create().texOffs(166, 107).mirror().addBox(-1.0F, -0.7F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.16F)).mirror(false), PartPose.offsetAndRotation(0.0F, 1.3F, -0.4F, 0.2443F, 0.0F, 0.0F));

		PartDefinition fetlock_feather_b_r_r1 = leg_back_hoof_r.addOrReplaceChild("fetlock_feather_b_r_r1", CubeListBuilder.create().texOffs(240, 81).mirror().addBox(-1.0F, -0.5F, -0.3F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.3F, 0.7F, -0.4887F, 0.0F, 0.0F));

		PartDefinition wing_1_l = body.addOrReplaceChild("wing_1_l", CubeListBuilder.create().texOffs(50, 82).addBox(-12.6F, -1.0F, -1.5F, 13.0F, 2.0F, 3.0F, new CubeDeformation(0.14F)), PartPose.offsetAndRotation(-5.2F, -3.8F, -6.5F, -0.0698F, 0.1396F, 0.2443F));

		PartDefinition secondary_inner_3_l_r1 = wing_1_l.addOrReplaceChild("secondary_inner_3_l_r1", CubeListBuilder.create().texOffs(396, 21).addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-10.8F, 1.1F, 11.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_3_l_r1 = wing_1_l.addOrReplaceChild("covert_inner_3_l_r1", CubeListBuilder.create().texOffs(184, 56).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-10.8F, 0.0F, 4.0F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_2_l_r1 = wing_1_l.addOrReplaceChild("secondary_inner_2_l_r1", CubeListBuilder.create().texOffs(312, 39).addBox(-1.5F, -0.5F, -6.0F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-7.8F, 1.1F, 10.5F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_2_l_r1 = wing_1_l.addOrReplaceChild("covert_inner_2_l_r1", CubeListBuilder.create().texOffs(204, 69).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-7.8F, 0.0F, 3.5F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_1_l_r1 = wing_1_l.addOrReplaceChild("secondary_inner_1_l_r1", CubeListBuilder.create().texOffs(472, 39).addBox(-1.5F, -0.5F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-4.8F, 1.1F, 10.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_1_l_r1 = wing_1_l.addOrReplaceChild("covert_inner_1_l_r1", CubeListBuilder.create().texOffs(136, 56).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-4.8F, 0.0F, 4.0F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition secondary_inner_0_l_r1 = wing_1_l.addOrReplaceChild("secondary_inner_0_l_r1", CubeListBuilder.create().texOffs(144, 41).addBox(-1.5F, -0.5F, -5.0F, 3.0F, 1.0F, 10.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-1.8F, 1.1F, 9.5F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_inner_0_l_r1 = wing_1_l.addOrReplaceChild("covert_inner_0_l_r1", CubeListBuilder.create().texOffs(160, 69).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-1.8F, 0.0F, 3.5F, 0.0873F, 0.0524F, 0.0F));

		PartDefinition covert_shoulder_cap_l_r1 = wing_1_l.addOrReplaceChild("covert_shoulder_cap_l_r1", CubeListBuilder.create().texOffs(348, 69).addBox(-4.0F, -1.0F, -2.5F, 8.0F, 2.0F, 5.0F, new CubeDeformation(0.18F)), PartPose.offsetAndRotation(-3.4F, -0.6F, 1.2F, 0.1047F, 0.0F, 0.0F));

		PartDefinition wing_2_l = wing_1_l.addOrReplaceChild("wing_2_l", CubeListBuilder.create().texOffs(254, 81).addBox(-14.5F, -1.0F, -1.5F, 15.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-11.8F, 0.0F, 0.0F, -0.0349F, 0.1047F, 0.1047F));

		PartDefinition secondary_mid_4_l_r1 = wing_2_l.addOrReplaceChild("secondary_mid_4_l_r1", CubeListBuilder.create().texOffs(220, 22).addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-12.7F, 1.1F, 11.5F, 0.0524F, 0.192F, 0.0F));

		PartDefinition secondary_mid_3_l_r1 = wing_2_l.addOrReplaceChild("secondary_mid_3_l_r1", CubeListBuilder.create().texOffs(388, 2).addBox(-1.5F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-9.9F, 1.1F, 12.0F, 0.0524F, 0.1658F, 0.0F));

		PartDefinition secondary_mid_2_l_r1 = wing_2_l.addOrReplaceChild("secondary_mid_2_l_r1", CubeListBuilder.create().texOffs(232, 2).addBox(-1.5F, -0.5F, -7.5F, 3.0F, 1.0F, 15.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-7.1F, 1.1F, 12.5F, 0.0524F, 0.1396F, 0.0F));

		PartDefinition secondary_mid_1_l_r1 = wing_2_l.addOrReplaceChild("secondary_mid_1_l_r1", CubeListBuilder.create().texOffs(312, 2).addBox(-1.5F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-4.3F, 1.1F, 12.0F, 0.0524F, 0.1134F, 0.0F));

		PartDefinition secondary_mid_0_l_r1 = wing_2_l.addOrReplaceChild("secondary_mid_0_l_r1", CubeListBuilder.create().texOffs(468, 21).addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-1.5F, 1.1F, 11.5F, 0.0524F, 0.0873F, 0.0F));

		PartDefinition covert_mid_4_l_r1 = wing_2_l.addOrReplaceChild("covert_mid_4_l_r1", CubeListBuilder.create().texOffs(2, 66).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-12.7F, 0.0F, 4.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_3_l_r1 = wing_2_l.addOrReplaceChild("covert_mid_3_l_r1", CubeListBuilder.create().texOffs(64, 58).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-9.9F, 0.0F, 4.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_2_l_r1 = wing_2_l.addOrReplaceChild("covert_mid_2_l_r1", CubeListBuilder.create().texOffs(404, 56).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-7.1F, 0.0F, 4.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_1_l_r1 = wing_2_l.addOrReplaceChild("covert_mid_1_l_r1", CubeListBuilder.create().texOffs(356, 56).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-4.3F, 0.0F, 4.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition covert_mid_0_l_r1 = wing_2_l.addOrReplaceChild("covert_mid_0_l_r1", CubeListBuilder.create().texOffs(308, 56).addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-1.5F, 0.0F, 4.0F, 0.0698F, 0.0698F, 0.0F));

		PartDefinition alula_tuft_l_r1 = wing_2_l.addOrReplaceChild("alula_tuft_l_r1", CubeListBuilder.create().texOffs(486, 91).addBox(-4.5F, -0.5F, -1.8F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-11.5F, 0.0F, -1.5F, -0.0698F, -0.2443F, 0.1047F));

		PartDefinition wing_3_l = wing_2_l.addOrReplaceChild("wing_3_l", CubeListBuilder.create(), PartPose.offsetAndRotation(-13.5F, 0.0F, 0.0F, 0.0F, 0.1396F, 0.0698F));

		PartDefinition primary_7_l_r1 = wing_3_l.addOrReplaceChild("primary_7_l_r1", CubeListBuilder.create().texOffs(388, 99).addBox(-11.0F, -0.5F, -1.0F, 12.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(0.3F, 1.0F, 3.5F, 0.0349F, 1.3788F, -0.0524F));

		PartDefinition primary_6_l_r1 = wing_3_l.addOrReplaceChild("primary_6_l_r1", CubeListBuilder.create().texOffs(316, 99).addBox(-12.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-0.9F, 1.0F, 3.1F, 0.0349F, 1.2043F, -0.0524F));

		PartDefinition primary_5_l_r1 = wing_3_l.addOrReplaceChild("primary_5_l_r1", CubeListBuilder.create().texOffs(204, 99).addBox(-13.0F, -0.5F, -1.0F, 14.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-2.5F, 1.0F, 2.7F, 0.0349F, 1.0123F, -0.0349F));

		PartDefinition primary_4_l_r1 = wing_3_l.addOrReplaceChild("primary_4_l_r1", CubeListBuilder.create().texOffs(124, 92).addBox(-14.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-4.3F, 1.0F, 2.3F, 0.0349F, 0.8203F, -0.0349F));

		PartDefinition primary_3_l_r1 = wing_3_l.addOrReplaceChild("primary_3_l_r1", CubeListBuilder.create().texOffs(336, 91).addBox(-15.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-6.0F, 1.0F, 1.7F, 0.0349F, 0.6283F, -0.0175F));

		PartDefinition primary_2_l_r1 = wing_3_l.addOrReplaceChild("primary_2_l_r1", CubeListBuilder.create().texOffs(166, 91).addBox(-15.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-7.5F, 1.0F, 0.9F, 0.0349F, 0.4363F, 0.0F));

		PartDefinition primary_1_l_r1 = wing_3_l.addOrReplaceChild("primary_1_l_r1", CubeListBuilder.create().texOffs(420, 91).addBox(-14.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-8.7F, 1.0F, 0.1F, 0.0349F, 0.2618F, 0.0175F));

		PartDefinition primary_0_l_r1 = wing_3_l.addOrReplaceChild("primary_0_l_r1", CubeListBuilder.create().texOffs(2, 99).addBox(-12.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)), PartPose.offsetAndRotation(-9.5F, 1.0F, -0.7F, 0.0349F, 0.1047F, 0.0349F));

		PartDefinition covert_outer_2_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_2_l_r1", CubeListBuilder.create().texOffs(2, 79).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-8.4F, 0.5F, 3.0F, 0.0524F, 0.3142F, 0.0F));

		PartDefinition covert_outer_1_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_1_l_r1", CubeListBuilder.create().texOffs(62, 71).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-5.2F, 0.5F, 3.0F, 0.0524F, 0.2443F, 0.0F));

		PartDefinition covert_outer_0_l_r1 = wing_3_l.addOrReplaceChild("covert_outer_0_l_r1", CubeListBuilder.create().texOffs(408, 69).addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(-2.0F, 0.5F, 3.0F, 0.0524F, 0.1745F, 0.0F));

		PartDefinition wing_spar_3_l_r1 = wing_3_l.addOrReplaceChild("wing_spar_3_l_r1", CubeListBuilder.create().texOffs(456, 99).addBox(-5.5F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-5.0F, -0.1F, -0.3F, 0.0F, 0.1396F, 0.0F));

		PartDefinition wing_1_r = body.addOrReplaceChild("wing_1_r", CubeListBuilder.create().texOffs(2, 90).mirror().addBox(-0.4F, -1.0F, -1.5F, 13.0F, 2.0F, 3.0F, new CubeDeformation(0.14F)).mirror(false), PartPose.offsetAndRotation(5.2F, -3.8F, -6.5F, -0.0698F, -0.1396F, -0.2443F));

		PartDefinition secondary_inner_3_r_r1 = wing_1_r.addOrReplaceChild("secondary_inner_3_r_r1", CubeListBuilder.create().texOffs(432, 21).mirror().addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(10.8F, 1.1F, 11.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_3_r_r1 = wing_1_r.addOrReplaceChild("covert_inner_3_r_r1", CubeListBuilder.create().texOffs(208, 56).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(10.8F, 0.0F, 4.0F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_2_r_r1 = wing_1_r.addOrReplaceChild("secondary_inner_2_r_r1", CubeListBuilder.create().texOffs(346, 39).mirror().addBox(-1.5F, -0.5F, -6.0F, 3.0F, 1.0F, 12.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(7.8F, 1.1F, 10.5F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_2_r_r1 = wing_1_r.addOrReplaceChild("covert_inner_2_r_r1", CubeListBuilder.create().texOffs(226, 69).mirror().addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(7.8F, 0.0F, 3.5F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_1_r_r1 = wing_1_r.addOrReplaceChild("secondary_inner_1_r_r1", CubeListBuilder.create().texOffs(174, 40).mirror().addBox(-1.5F, -0.5F, -5.5F, 3.0F, 1.0F, 11.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(4.8F, 1.1F, 10.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_1_r_r1 = wing_1_r.addOrReplaceChild("covert_inner_1_r_r1", CubeListBuilder.create().texOffs(160, 56).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(4.8F, 0.0F, 4.0F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition secondary_inner_0_r_r1 = wing_1_r.addOrReplaceChild("secondary_inner_0_r_r1", CubeListBuilder.create().texOffs(68, 43).mirror().addBox(-1.5F, -0.5F, -5.0F, 3.0F, 1.0F, 10.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(1.8F, 1.1F, 9.5F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_inner_0_r_r1 = wing_1_r.addOrReplaceChild("covert_inner_0_r_r1", CubeListBuilder.create().texOffs(182, 69).mirror().addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(1.8F, 0.0F, 3.5F, 0.0873F, -0.0524F, 0.0F));

		PartDefinition covert_shoulder_cap_r_r1 = wing_1_r.addOrReplaceChild("covert_shoulder_cap_r_r1", CubeListBuilder.create().texOffs(378, 69).mirror().addBox(-4.0F, -1.0F, -2.5F, 8.0F, 2.0F, 5.0F, new CubeDeformation(0.18F)).mirror(false), PartPose.offsetAndRotation(3.4F, -0.6F, 1.2F, 0.1047F, 0.0F, 0.0F));

		PartDefinition wing_2_r = wing_1_r.addOrReplaceChild("wing_2_r", CubeListBuilder.create().texOffs(294, 81).mirror().addBox(-0.5F, -1.0F, -1.5F, 15.0F, 2.0F, 3.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(11.8F, 0.0F, 0.0F, -0.0349F, -0.1047F, -0.1047F));

		PartDefinition secondary_mid_4_r_r1 = wing_2_r.addOrReplaceChild("secondary_mid_4_r_r1", CubeListBuilder.create().texOffs(256, 22).mirror().addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(12.7F, 1.1F, 11.5F, 0.0524F, -0.192F, 0.0F));

		PartDefinition secondary_mid_3_r_r1 = wing_2_r.addOrReplaceChild("secondary_mid_3_r_r1", CubeListBuilder.create().texOffs(426, 2).mirror().addBox(-1.5F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(9.9F, 1.1F, 12.0F, 0.0524F, -0.1658F, 0.0F));

		PartDefinition secondary_mid_2_r_r1 = wing_2_r.addOrReplaceChild("secondary_mid_2_r_r1", CubeListBuilder.create().texOffs(272, 2).mirror().addBox(-1.5F, -0.5F, -7.5F, 3.0F, 1.0F, 15.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(7.1F, 1.1F, 12.5F, 0.0524F, -0.1396F, 0.0F));

		PartDefinition secondary_mid_1_r_r1 = wing_2_r.addOrReplaceChild("secondary_mid_1_r_r1", CubeListBuilder.create().texOffs(350, 2).mirror().addBox(-1.5F, -0.5F, -7.0F, 3.0F, 1.0F, 14.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(4.3F, 1.1F, 12.0F, 0.0524F, -0.1134F, 0.0F));

		PartDefinition secondary_mid_0_r_r1 = wing_2_r.addOrReplaceChild("secondary_mid_0_r_r1", CubeListBuilder.create().texOffs(184, 22).mirror().addBox(-1.5F, -0.5F, -6.5F, 3.0F, 1.0F, 13.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(1.5F, 1.1F, 11.5F, 0.0524F, -0.0873F, 0.0F));

		PartDefinition covert_mid_4_r_r1 = wing_2_r.addOrReplaceChild("covert_mid_4_r_r1", CubeListBuilder.create().texOffs(26, 66).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(12.7F, 0.0F, 4.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_3_r_r1 = wing_2_r.addOrReplaceChild("covert_mid_3_r_r1", CubeListBuilder.create().texOffs(88, 58).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(9.9F, 0.0F, 4.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_2_r_r1 = wing_2_r.addOrReplaceChild("covert_mid_2_r_r1", CubeListBuilder.create().texOffs(428, 56).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(7.1F, 0.0F, 4.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_1_r_r1 = wing_2_r.addOrReplaceChild("covert_mid_1_r_r1", CubeListBuilder.create().texOffs(380, 56).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(4.3F, 0.0F, 4.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition covert_mid_0_r_r1 = wing_2_r.addOrReplaceChild("covert_mid_0_r_r1", CubeListBuilder.create().texOffs(332, 56).mirror().addBox(-1.5F, -1.0F, -3.5F, 3.0F, 2.0F, 7.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(1.5F, 0.0F, 4.0F, 0.0698F, -0.0698F, 0.0F));

		PartDefinition alula_tuft_r_r1 = wing_2_r.addOrReplaceChild("alula_tuft_r_r1", CubeListBuilder.create().texOffs(486, 99).mirror().addBox(-0.5F, -0.5F, -1.8F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(11.5F, 0.0F, -1.5F, -0.0698F, 0.2443F, -0.1047F));

		PartDefinition wing_3_r = wing_2_r.addOrReplaceChild("wing_3_r", CubeListBuilder.create(), PartPose.offsetAndRotation(13.5F, 0.0F, 0.0F, 0.0F, -0.1396F, -0.0698F));

		PartDefinition primary_7_r_r1 = wing_3_r.addOrReplaceChild("primary_7_r_r1", CubeListBuilder.create().texOffs(422, 99).mirror().addBox(-1.0F, -0.5F, -1.0F, 12.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(-0.3F, 1.0F, 3.5F, 0.0349F, -1.3788F, 0.0524F));

		PartDefinition primary_6_r_r1 = wing_3_r.addOrReplaceChild("primary_6_r_r1", CubeListBuilder.create().texOffs(352, 99).mirror().addBox(-1.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(0.9F, 1.0F, 3.1F, 0.0349F, -1.2043F, 0.0524F));

		PartDefinition primary_5_r_r1 = wing_3_r.addOrReplaceChild("primary_5_r_r1", CubeListBuilder.create().texOffs(242, 99).mirror().addBox(-1.0F, -0.5F, -1.0F, 14.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(2.5F, 1.0F, 2.7F, 0.0349F, -1.0123F, 0.0349F));

		PartDefinition primary_4_r_r1 = wing_3_r.addOrReplaceChild("primary_4_r_r1", CubeListBuilder.create().texOffs(164, 99).mirror().addBox(-1.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(4.3F, 1.0F, 2.3F, 0.0349F, -0.8203F, 0.0349F));

		PartDefinition primary_3_r_r1 = wing_3_r.addOrReplaceChild("primary_3_r_r1", CubeListBuilder.create().texOffs(378, 91).mirror().addBox(-1.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(6.0F, 1.0F, 1.7F, 0.0349F, -0.6283F, 0.0175F));

		PartDefinition primary_2_r_r1 = wing_3_r.addOrReplaceChild("primary_2_r_r1", CubeListBuilder.create().texOffs(208, 91).mirror().addBox(-1.0F, -0.5F, -1.0F, 16.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(7.5F, 1.0F, 0.9F, 0.0349F, -0.4363F, 0.0F));

		PartDefinition primary_1_r_r1 = wing_3_r.addOrReplaceChild("primary_1_r_r1", CubeListBuilder.create().texOffs(84, 92).mirror().addBox(-1.0F, -0.5F, -1.0F, 15.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(8.7F, 1.0F, 0.1F, 0.0349F, -0.2618F, -0.0175F));

		PartDefinition primary_0_r_r1 = wing_3_r.addOrReplaceChild("primary_0_r_r1", CubeListBuilder.create().texOffs(280, 99).mirror().addBox(-1.0F, -0.5F, -1.0F, 13.0F, 1.0F, 3.0F, new CubeDeformation(0.07F)).mirror(false), PartPose.offsetAndRotation(9.5F, 1.0F, -0.7F, 0.0349F, -0.1047F, -0.0349F));

		PartDefinition covert_outer_2_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_2_r_r1", CubeListBuilder.create().texOffs(26, 79).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(8.4F, 0.5F, 3.0F, 0.0524F, -0.3142F, 0.0F));

		PartDefinition covert_outer_1_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_1_r_r1", CubeListBuilder.create().texOffs(86, 71).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(5.2F, 0.5F, 3.0F, 0.0524F, -0.2443F, 0.0F));

		PartDefinition covert_outer_0_r_r1 = wing_3_r.addOrReplaceChild("covert_outer_0_r_r1", CubeListBuilder.create().texOffs(432, 69).mirror().addBox(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offsetAndRotation(2.0F, 0.5F, 3.0F, 0.0524F, -0.1745F, 0.0F));

		PartDefinition wing_spar_3_r_r1 = wing_3_r.addOrReplaceChild("wing_spar_3_r_r1", CubeListBuilder.create().texOffs(38, 100).mirror().addBox(-5.5F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(5.0F, -0.1F, -0.3F, 0.0F, -0.1396F, 0.0F));

		PartDefinition tail_1 = body.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(248, 69).addBox(-1.5F, -1.3F, -0.7F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, -3.2F, 13.5F, 0.3142F, 0.0F, 0.0F));

		PartDefinition tail_dock_lock_r_r1 = tail_1.addOrReplaceChild("tail_dock_lock_r_r1", CubeListBuilder.create().texOffs(286, 69).mirror().addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.0F, 0.7F, 2.5F, 0.1047F, 0.1745F, 0.1396F));

		PartDefinition tail_dock_lock_l_r1 = tail_1.addOrReplaceChild("tail_dock_lock_l_r1", CubeListBuilder.create().texOffs(268, 69).addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 0.7F, 2.5F, 0.1047F, -0.1745F, -0.1396F));

		PartDefinition tail_2 = tail_1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(420, 39).addBox(-2.0F, -0.7F, -1.2F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 2.7F, 3.7F, -0.2094F, 0.0F, 0.0F));

		PartDefinition tail_plume_splay_r_r1 = tail_2.addOrReplaceChild("tail_plume_splay_r_r1", CubeListBuilder.create().texOffs(30, 51).mirror().addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.08F)).mirror(false), PartPose.offsetAndRotation(2.2F, 4.0F, 0.5F, -0.0698F, 0.1396F, 0.1745F));

		PartDefinition tail_plume_splay_l_r1 = tail_2.addOrReplaceChild("tail_plume_splay_l_r1", CubeListBuilder.create().texOffs(98, 43).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(-2.2F, 4.0F, 0.5F, -0.0698F, -0.1396F, -0.1745F));

		PartDefinition tail_3 = tail_2.addOrReplaceChild("tail_3", CubeListBuilder.create().texOffs(292, 55).addBox(-1.5F, -0.5F, -1.6F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 8.0F, 1.6F, -0.1047F, 0.0F, 0.0F));

		PartDefinition tail_tip_wisp_r_r1 = tail_3.addOrReplaceChild("tail_tip_wisp_r_r1", CubeListBuilder.create().texOffs(338, 69).mirror().addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.06F)).mirror(false), PartPose.offsetAndRotation(1.3F, 2.7F, -0.3F, -0.0698F, 0.1047F, 0.1047F));

		PartDefinition tail_tip_wisp_l_r1 = tail_3.addOrReplaceChild("tail_tip_wisp_l_r1", CubeListBuilder.create().texOffs(328, 69).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(-1.3F, 2.7F, -0.3F, -0.0698F, -0.1047F, -0.1047F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	private static float rad(float deg) {
		return deg * ((float) Math.PI / 180.0F);
	}

	/** Applies Blockbench Y-up degree offsets (`-rx, -ry, +rz` in Mojang ModelPart space). */
	private static void addBbDeg(ModelPart p, float dx, float dy, float dz) {
		p.xRot -= rad(dx);
		p.yRot -= rad(dy);
		p.zRot += rad(dz);
	}

	@Override
	public void setupAnim(PegasusEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		for (RestPose rp : this.restPoses) {
			rp.part.xRot = rp.x;
			rp.part.yRot = rp.y;
			rp.part.zRot = rp.z;
		}

		// Toggle Royal Elisar saddle & golden bridle according to entity state!
		this.saddle_tack.visible = entity.hasSaddle();
		this.head_tack.visible = entity.hasSaddle() || entity.isTamed();

		boolean airborne = entity.isFlying() || !entity.onGround();

		// Noble head look & subtle ear flicks
		float clampedYaw = Mth.clamp(netHeadYaw, -45.0F, 45.0F);
		float clampedPitch = Mth.clamp(headPitch, -25.0F, 25.0F);
		addBbDeg(neck_lower, -clampedPitch * 0.20F + Mth.sin(ageInTicks * 0.06F) * 1.2F, -clampedYaw * 0.25F, 0.0F);
		addBbDeg(head, -clampedPitch * 0.45F, -clampedYaw * 0.45F, 0.0F);
		float earFlick = Mth.sin(ageInTicks * 0.09F) * 3.5F;
		addBbDeg(ear_l, earFlick, 0.0F, 0.0F);
		addBbDeg(ear_r, -earFlick * 0.7F, 0.0F, 0.0F);

		if (airborne) {
			// ---- IN FLIGHT: Full 5.3-block 3-tiered feathered wingspan + tucked galloping legs ----
			if (entity.isNoAi()) {
				// Majestic soaring showcase pose
				float soar = Mth.sin(ageInTicks * 0.06F) * 3.0F;
				addBbDeg(wing_1_l, 4.0F, 6.0F, 16.0F + soar);
				addBbDeg(wing_2_l, 2.0F, 4.0F, 10.0F + soar * 0.6F);
				addBbDeg(wing_3_l, 0.0F, 2.0F, 8.0F + soar * 0.4F);
				addBbDeg(wing_1_r, 4.0F, -6.0F, -16.0F - soar);
				addBbDeg(wing_2_r, 2.0F, -4.0F, -10.0F - soar * 0.6F);
				addBbDeg(wing_3_r, 0.0F, -2.0F, -8.0F - soar * 0.4F);
			} else {
				float wPhase = ageInTicks * 0.28F;
				float flap1 = Mth.sin(wPhase) * 26.0F;
				float flap2 = Mth.sin(wPhase - 0.55F) * 16.0F;
				float flap3 = Mth.sin(wPhase - 1.05F) * 13.0F;
				float twist = Mth.cos(wPhase) * 5.0F;
				addBbDeg(wing_1_l, twist, -twist * 0.6F, flap1);
				addBbDeg(wing_2_l, twist * 0.5F, 0.0F, flap2);
				addBbDeg(wing_3_l, 0.0F, 0.0F, flap3);
				addBbDeg(wing_1_r, twist, twist * 0.6F, -flap1);
				addBbDeg(wing_2_r, twist * 0.5F, 0.0F, -flap2);
				addBbDeg(wing_3_r, 0.0F, 0.0F, -flap3);
			}

			// Tucked airborne equine legs & streaming tail plume
			addBbDeg(leg_front_upper_l, -30.0F, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_l, 40.0F, 0.0F, 0.0F);
			addBbDeg(leg_front_upper_r, -24.0F, 0.0F, 0.0F);
			addBbDeg(leg_front_cannon_r, 34.0F, 0.0F, 0.0F);
			addBbDeg(leg_back_thigh_l, 28.0F, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_l, 22.0F, 0.0F, 0.0F);
			addBbDeg(leg_back_thigh_r, 34.0F, 0.0F, 0.0F);
			addBbDeg(leg_back_cannon_r, 26.0F, 0.0F, 0.0F);
			addBbDeg(tail_1, -12.0F, Mth.sin(ageInTicks * 0.12F) * 4.0F, 0.0F);
			addBbDeg(tail_2, -8.0F, Mth.cos(ageInTicks * 0.12F) * 5.0F, 0.0F);
		} else {
			// ---- ON GROUND: Regal swan-folded wings tucked neatly along flanks, clear of saddle ----
			float breath = Mth.sin(ageInTicks * 0.06F) * 1.2F;
			addBbDeg(wing_1_l, 62.0F, -78.0F, -6.0F + breath * 0.4F);
			addBbDeg(wing_2_l, -8.0F, 12.0F, 8.0F);
			addBbDeg(wing_3_l, -6.0F, 18.0F, 6.0F);

			addBbDeg(wing_1_r, 62.0F, 78.0F, 6.0F - breath * 0.4F);
			addBbDeg(wing_2_r, -8.0F, -12.0F, -8.0F);
			addBbDeg(wing_3_r, -6.0F, -18.0F, -6.0F);

			// 4-beat / diagonal equine leg walk with knee & hock flexion
			if (!entity.isNoAi() && limbSwingAmount > 0.005F) {
				float s1 = Mth.cos(limbSwing * 0.6662F) * 32.0F * limbSwingAmount;
				float s2 = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 32.0F * limbSwingAmount;
				float k1 = Math.max(0.0F, -Mth.sin(limbSwing * 0.6662F)) * 28.0F * limbSwingAmount;
				float k2 = Math.max(0.0F, -Mth.sin(limbSwing * 0.6662F + (float) Math.PI)) * 28.0F * limbSwingAmount;

				addBbDeg(leg_front_upper_l, s1, 0.0F, 0.0F);
				addBbDeg(leg_front_cannon_l, k1, 0.0F, 0.0F);
				addBbDeg(leg_front_upper_r, s2, 0.0F, 0.0F);
				addBbDeg(leg_front_cannon_r, k2, 0.0F, 0.0F);

				addBbDeg(leg_back_thigh_l, s2 * 0.88F, 0.0F, 0.0F);
				addBbDeg(leg_back_cannon_l, -k2 * 0.65F, 0.0F, 0.0F);
				addBbDeg(leg_back_thigh_r, s1 * 0.88F, 0.0F, 0.0F);
				addBbDeg(leg_back_cannon_r, -k1 * 0.65F, 0.0F, 0.0F);
			}

			// Flowing Arabian tail sway
			addBbDeg(tail_1, Mth.sin(ageInTicks * 0.05F) * 2.0F, Mth.cos(ageInTicks * 0.07F) * 4.5F, 0.0F);
			addBbDeg(tail_2, 0.0F, Mth.sin(ageInTicks * 0.07F) * 6.0F, 0.0F);
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
	                           int packedOverlay, float red, float green, float blue, float alpha) {
		this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
