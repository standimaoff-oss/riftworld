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
import ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/coral_hermit_crab.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class CoralHermitCrabModel extends FaunaBaseModel<CoralHermitCrabEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "coral_hermit_crab"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart eyestalk_l;
	private final ModelPart eyestalk_r;
	private final ModelPart claw_major_r;
	private final ModelPart claw_minor_l;
	private final ModelPart shell;
	private final ModelPart leg_l_1;
	private final ModelPart leg_l_2;
	private final ModelPart leg_l_3;
	private final ModelPart leg_r_1;
	private final ModelPart leg_r_2;
	private final ModelPart leg_r_3;

	public CoralHermitCrabModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.eyestalk_l = this.body.getChild("eyestalk_l");
		this.eyestalk_r = this.body.getChild("eyestalk_r");
		this.claw_major_r = this.body.getChild("claw_major_r");
		this.claw_minor_l = this.body.getChild("claw_minor_l");
		this.shell = this.body.getChild("shell");
		this.leg_l_1 = this.root.getChild("leg_l_1");
		this.leg_l_2 = this.root.getChild("leg_l_2");
		this.leg_l_3 = this.root.getChild("leg_l_3");
		this.leg_r_1 = this.root.getChild("leg_r_1");
		this.leg_r_2 = this.root.getChild("leg_r_2");
		this.leg_r_3 = this.root.getChild("leg_r_3");
		captureRestPoses(this.root, this.body, this.eyestalk_l, this.eyestalk_r, this.claw_major_r, this.claw_minor_l, this.shell, this.leg_l_1, this.leg_l_2, this.leg_l_3, this.leg_r_1, this.leg_r_2, this.leg_r_3);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(106, 2).addBox(-5.5F, -4.6F, -5.2F, 11.0F, 8.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.0F, -5.5F));

		PartDefinition eyestalk_l = body.addOrReplaceChild("eyestalk_l", CubeListBuilder.create().texOffs(44, 28).addBox(-1.7F, -7.1F, -2.7F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.6F, -4.0F, -3.7F));

		PartDefinition eyestalk_shaft_l_r1 = eyestalk_l.addOrReplaceChild("eyestalk_shaft_l_r1", CubeListBuilder.create().texOffs(416, 2).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.2052F, 0.0F, -0.0416F));

		PartDefinition eyestalk_r = body.addOrReplaceChild("eyestalk_r", CubeListBuilder.create().texOffs(60, 28).addBox(-1.3F, -7.1F, -2.7F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.6F, -4.0F, -3.7F));

		PartDefinition eyestalk_shaft_r_r1 = eyestalk_r.addOrReplaceChild("eyestalk_shaft_r_r1", CubeListBuilder.create().texOffs(428, 2).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.2052F, 0.0F, 0.0416F));

		PartDefinition claw_major_r = body.addOrReplaceChild("claw_major_r", CubeListBuilder.create(), PartPose.offset(4.6F, 0.2F, -2.7F));

		PartDefinition claw_major_thumb_r1 = claw_major_r.addOrReplaceChild("claw_major_thumb_r1", CubeListBuilder.create().texOffs(440, 2).addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 2.2F, -7.2F, -0.384F, -0.2793F, -0.1745F));

		PartDefinition claw_major_palm_r1 = claw_major_r.addOrReplaceChild("claw_major_palm_r1", CubeListBuilder.create().texOffs(186, 2).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 2.6F, -6.6F, -0.384F, -0.2793F, -0.1745F));

		PartDefinition claw_major_arm_r1 = claw_major_r.addOrReplaceChild("claw_major_arm_r1", CubeListBuilder.create().texOffs(400, 2).addBox(-1.5F, -4.8F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, 0.0F, 0.2F, 1.8543F, -0.5071F, 0.0F));

		PartDefinition claw_minor_l = body.addOrReplaceChild("claw_minor_l", CubeListBuilder.create(), PartPose.offset(-4.6F, 0.0F, -2.7F));

		PartDefinition claw_minor_pincer_r1 = claw_minor_l.addOrReplaceChild("claw_minor_pincer_r1", CubeListBuilder.create().texOffs(26, 28).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8F, 2.0F, -5.8F, -0.2793F, 0.4887F, 0.1396F));

		PartDefinition claw_minor_palm_r1 = claw_minor_l.addOrReplaceChild("claw_minor_palm_r1", CubeListBuilder.create().texOffs(220, 2).addBox(-2.5F, -2.5F, -4.5F, 5.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, 1.8F, -6.0F, -0.2793F, 0.4887F, 0.1396F));

		PartDefinition claw_minor_arm_r1 = claw_minor_l.addOrReplaceChild("claw_minor_arm_r1", CubeListBuilder.create().texOffs(276, 2).addBox(-1.5F, -6.8F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, 0.0F, 0.2F, 1.706F, 0.9545F, 0.0F));

		PartDefinition shell = body.addOrReplaceChild("shell", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 4.0F));

		PartDefinition anemone_tent_8_r1 = shell.addOrReplaceChild("anemone_tent_8_r1", CubeListBuilder.create().texOffs(464, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.4189F, -2.0003F, 4.2128F, -2.4181F, -0.8939F, 3.1416F));

		PartDefinition anemone_tent_7_r1 = shell.addOrReplaceChild("anemone_tent_7_r1", CubeListBuilder.create().texOffs(456, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.9651F, -2.3448F, 3.2161F, 0.3073F, -0.9838F, 0.0F));

		PartDefinition anemone_tent_6_r1 = shell.addOrReplaceChild("anemone_tent_6_r1", CubeListBuilder.create().texOffs(98, 40).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.6997F, -1.8311F, 2.2243F, 0.8576F, -0.1284F, 0.0F));

		PartDefinition anemone_tent_5_r1 = shell.addOrReplaceChild("anemone_tent_5_r1", CubeListBuilder.create().texOffs(448, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.7781F, -0.7601F, 1.8184F, 1.5451F, -0.1435F, 0.0F));

		PartDefinition anemone_tent_4_r1 = shell.addOrReplaceChild("anemone_tent_4_r1", CubeListBuilder.create().texOffs(440, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.1545F, 0.2409F, 2.2362F, 2.1672F, -0.4552F, 0.0F));

		PartDefinition anemone_tent_3_r1 = shell.addOrReplaceChild("anemone_tent_3_r1", CubeListBuilder.create().texOffs(432, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.6083F, 0.5854F, 3.2328F, 2.4369F, -1.3248F, 0.0F));

		PartDefinition anemone_tent_2_r1 = shell.addOrReplaceChild("anemone_tent_2_r1", CubeListBuilder.create().texOffs(424, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.8737F, 0.0716F, 4.2246F, -1.0929F, -1.0761F, 3.1416F));

		PartDefinition anemone_tent_1_r1 = shell.addOrReplaceChild("anemone_tent_1_r1", CubeListBuilder.create().texOffs(416, 28).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.7953F, -0.9994F, 4.6305F, -1.7366F, -0.8408F, 3.1416F));

		PartDefinition anemone_column_r1 = shell.addOrReplaceChild("anemone_column_r1", CubeListBuilder.create().texOffs(376, 2).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -0.6F, 3.7F, 1.5544F, 1.2242F, 2.9101F));

		PartDefinition coral_pink_6_br_3_r1 = shell.addOrReplaceChild("coral_pink_6_br_3_r1", CubeListBuilder.create().texOffs(90, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8377F, 1.6866F, 10.2925F, -1.983F, -0.3437F, 3.1416F));

		PartDefinition coral_pink_6_br_2_r1 = shell.addOrReplaceChild("coral_pink_6_br_2_r1", CubeListBuilder.create().texOffs(82, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8377F, 1.6866F, 10.2925F, -0.8375F, -0.0804F, 3.1416F));

		PartDefinition coral_pink_6_br_1_r1 = shell.addOrReplaceChild("coral_pink_6_br_1_r1", CubeListBuilder.create().texOffs(408, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8377F, 1.6866F, 10.2925F, -1.4104F, -1.511F, 3.1416F));

		PartDefinition coral_pink_6_stem_r1 = shell.addOrReplaceChild("coral_pink_6_stem_r1", CubeListBuilder.create().texOffs(400, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4F, 1.2F, 8.7F, -1.3478F, -0.7343F, 3.1416F));

		PartDefinition coral_pink_5_br_3_r1 = shell.addOrReplaceChild("coral_pink_5_br_3_r1", CubeListBuilder.create().texOffs(392, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8105F, -8.4598F, 9.9624F, 0.5317F, 1.1626F, 0.0F));

		PartDefinition coral_pink_5_br_2_r1 = shell.addOrReplaceChild("coral_pink_5_br_2_r1", CubeListBuilder.create().texOffs(384, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8105F, -8.4598F, 9.9624F, 1.6803F, 1.4495F, 0.0F));

		PartDefinition coral_pink_5_br_1_r1 = shell.addOrReplaceChild("coral_pink_5_br_1_r1", CubeListBuilder.create().texOffs(376, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8105F, -8.4598F, 9.9624F, -1.926F, 0.39F, 3.1416F));

		PartDefinition coral_pink_5_stem_r1 = shell.addOrReplaceChild("coral_pink_5_stem_r1", CubeListBuilder.create().texOffs(368, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -7.4F, 9.3F, -2.0734F, 1.2201F, 3.1416F));

		PartDefinition coral_lav_4_br_3_r1 = shell.addOrReplaceChild("coral_lav_4_br_3_r1", CubeListBuilder.create().texOffs(360, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0818F, -6.5962F, 4.5465F, 0.5558F, 0.5623F, 0.0F));

		PartDefinition coral_lav_4_br_2_r1 = shell.addOrReplaceChild("coral_lav_4_br_2_r1", CubeListBuilder.create().texOffs(74, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0818F, -6.5962F, 4.5465F, 1.7094F, 0.8109F, 0.0F));

		PartDefinition coral_lav_4_br_1_r1 = shell.addOrReplaceChild("coral_lav_4_br_1_r1", CubeListBuilder.create().texOffs(352, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0818F, -6.5962F, 4.5465F, -1.9039F, 1.0354F, 3.1416F));

		PartDefinition coral_lav_4_stem_r1 = shell.addOrReplaceChild("coral_lav_4_stem_r1", CubeListBuilder.create().texOffs(344, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.2F, -5.6F, 5.1F, 1.1008F, 1.2847F, 0.0F));

		PartDefinition coral_lav_3_br_3_r1 = shell.addOrReplaceChild("coral_lav_3_br_3_r1", CubeListBuilder.create().texOffs(66, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.3861F, -12.0116F, 13.6252F, -2.6655F, -0.1776F, 3.1416F));

		PartDefinition coral_lav_3_br_2_r1 = shell.addOrReplaceChild("coral_lav_3_br_2_r1", CubeListBuilder.create().texOffs(336, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.3861F, -12.0116F, 13.6252F, -1.5331F, -0.5734F, 3.1416F));

		PartDefinition coral_lav_3_br_1_r1 = shell.addOrReplaceChild("coral_lav_3_br_1_r1", CubeListBuilder.create().texOffs(328, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.3861F, -12.0116F, 13.6252F, 1.1619F, -1.2446F, 0.0F));

		PartDefinition coral_lav_3_stem_r1 = shell.addOrReplaceChild("coral_lav_3_stem_r1", CubeListBuilder.create().texOffs(320, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -10.8F, 12.7F, -2.1541F, -1.0427F, 3.1416F));

		PartDefinition coral_lav_2_br_3_r1 = shell.addOrReplaceChild("coral_lav_2_br_3_r1", CubeListBuilder.create().texOffs(312, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.015F, -16.4208F, 12.715F, -2.3899F, 0.5362F, 3.1416F));

		PartDefinition coral_lav_2_br_2_r1 = shell.addOrReplaceChild("coral_lav_2_br_2_r1", CubeListBuilder.create().texOffs(304, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.015F, -16.4208F, 12.715F, -1.9746F, -0.8713F, 3.1416F));

		PartDefinition coral_lav_2_br_1_r1 = shell.addOrReplaceChild("coral_lav_2_br_1_r1", CubeListBuilder.create().texOffs(296, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.015F, -16.4208F, 12.715F, 0.5264F, -0.4134F, 0.0F));

		PartDefinition coral_lav_2_stem_r1 = shell.addOrReplaceChild("coral_lav_2_stem_r1", CubeListBuilder.create().texOffs(288, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -14.4F, 12.1F, -2.7351F, -0.7854F, 3.1416F));

		PartDefinition coral_pink_4_br_3_r1 = shell.addOrReplaceChild("coral_pink_4_br_3_r1", CubeListBuilder.create().texOffs(58, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1514F, -14.785F, 7.3281F, -2.2875F, 1.4125F, 3.1416F));

		PartDefinition coral_pink_4_br_2_r1 = shell.addOrReplaceChild("coral_pink_4_br_2_r1", CubeListBuilder.create().texOffs(280, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1514F, -14.785F, 7.3281F, -2.7884F, -1.2163F, 3.1416F));

		PartDefinition coral_pink_4_br_1_r1 = shell.addOrReplaceChild("coral_pink_4_br_1_r1", CubeListBuilder.create().texOffs(50, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1514F, -14.785F, 7.3281F, 1.1586F, 0.1988F, 0.0F));

		PartDefinition coral_pink_4_stem_r1 = shell.addOrReplaceChild("coral_pink_4_stem_r1", CubeListBuilder.create().texOffs(272, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, -12.8F, 8.1F, 0.4458F, 0.6202F, 0.0F));

		PartDefinition coral_pink_3_br_3_r1 = shell.addOrReplaceChild("coral_pink_3_br_3_r1", CubeListBuilder.create().texOffs(42, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0591F, -5.9746F, 10.9853F, -2.4994F, -0.5046F, 3.1416F));

		PartDefinition coral_pink_3_br_2_r1 = shell.addOrReplaceChild("coral_pink_3_br_2_r1", CubeListBuilder.create().texOffs(34, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0591F, -5.9746F, 10.9853F, -1.3344F, -0.6429F, 3.1416F));

		PartDefinition coral_pink_3_br_1_r1 = shell.addOrReplaceChild("coral_pink_3_br_1_r1", CubeListBuilder.create().texOffs(264, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0591F, -5.9746F, 10.9853F, 1.3138F, -1.218F, 0.0F));

		PartDefinition coral_pink_3_stem_r1 = shell.addOrReplaceChild("coral_pink_3_stem_r1", CubeListBuilder.create().texOffs(256, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.2F, -5.2F, 10.1F, -1.9306F, -1.1264F, 3.1416F));

		PartDefinition coral_mint_3_br_3_r1 = shell.addOrReplaceChild("coral_mint_3_br_3_r1", CubeListBuilder.create().texOffs(248, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.3219F, -9.8804F, 8.7857F, -2.6892F, -0.3717F, 3.1416F));

		PartDefinition coral_mint_3_br_2_r1 = shell.addOrReplaceChild("coral_mint_3_br_2_r1", CubeListBuilder.create().texOffs(26, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.3219F, -9.8804F, 8.7857F, -1.5669F, -0.8269F, 3.1416F));

		PartDefinition coral_mint_3_br_1_r1 = shell.addOrReplaceChild("coral_mint_3_br_1_r1", CubeListBuilder.create().texOffs(18, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.3219F, -9.8804F, 8.7857F, 1.1371F, -0.9786F, 0.0F));

		PartDefinition coral_mint_3_stem_r1 = shell.addOrReplaceChild("coral_mint_3_stem_r1", CubeListBuilder.create().texOffs(240, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.6F, -8.6F, 8.3F, -2.192F, -1.2959F, 3.1416F));

		PartDefinition coral_lav_1_br_3_r1 = shell.addOrReplaceChild("coral_lav_1_br_3_r1", CubeListBuilder.create().texOffs(10, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0521F, -6.8142F, 4.6826F, -2.5926F, -1.1602F, 3.1416F));

		PartDefinition coral_lav_1_br_2_r1 = shell.addOrReplaceChild("coral_lav_1_br_2_r1", CubeListBuilder.create().texOffs(232, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0521F, -6.8142F, 4.6826F, -1.4403F, -1.4192F, 3.1416F));

		PartDefinition coral_lav_1_br_1_r1 = shell.addOrReplaceChild("coral_lav_1_br_1_r1", CubeListBuilder.create().texOffs(224, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0521F, -6.8142F, 4.6826F, 1.2315F, -0.4252F, 0.0F));

		PartDefinition coral_lav_1_stem_r1 = shell.addOrReplaceChild("coral_lav_1_stem_r1", CubeListBuilder.create().texOffs(216, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.2F, -5.8F, 5.3F, 1.0917F, -1.249F, 0.0F));

		PartDefinition coral_mint_2_br_3_r1 = shell.addOrReplaceChild("coral_mint_2_br_3_r1", CubeListBuilder.create().texOffs(2, 40).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.9433F, -10.9149F, 2.1305F, 0.3566F, -0.5598F, 0.0F));

		PartDefinition coral_mint_2_br_2_r1 = shell.addOrReplaceChild("coral_mint_2_br_2_r1", CubeListBuilder.create().texOffs(208, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.9433F, -10.9149F, 2.1305F, 1.332F, 0.4945F, 0.0F));

		PartDefinition coral_mint_2_br_1_r1 = shell.addOrReplaceChild("coral_mint_2_br_1_r1", CubeListBuilder.create().texOffs(504, 28).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.9433F, -10.9149F, 2.1305F, -2.1685F, 1.1714F, 3.1416F));

		PartDefinition coral_mint_2_stem_r1 = shell.addOrReplaceChild("coral_mint_2_stem_r1", CubeListBuilder.create().texOffs(200, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -9.2F, 2.9F, 0.6769F, 0.9784F, 0.0F));

		PartDefinition coral_mint_1_br_3_r1 = shell.addOrReplaceChild("coral_mint_1_br_3_r1", CubeListBuilder.create().texOffs(496, 28).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6842F, -11.7368F, 1.7756F, 0.4166F, 1.4472F, 0.0F));

		PartDefinition coral_mint_1_br_2_r1 = shell.addOrReplaceChild("coral_mint_1_br_2_r1", CubeListBuilder.create().texOffs(488, 28).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6842F, -11.7368F, 1.7756F, -2.3856F, -1.345F, 3.1416F));

		PartDefinition coral_mint_1_br_1_r1 = shell.addOrReplaceChild("coral_mint_1_br_1_r1", CubeListBuilder.create().texOffs(192, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6842F, -11.7368F, 1.7756F, 1.2265F, -0.1696F, 0.0F));

		PartDefinition coral_mint_1_stem_r1 = shell.addOrReplaceChild("coral_mint_1_stem_r1", CubeListBuilder.create().texOffs(184, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2F, -9.8F, 2.7F, 0.4942F, -0.4825F, 0.0F));

		PartDefinition coral_pink_2_br_3_r1 = shell.addOrReplaceChild("coral_pink_2_br_3_r1", CubeListBuilder.create().texOffs(176, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0994F, -7.5047F, -3.302F, 0.3569F, -0.9915F, 0.0F));

		PartDefinition coral_pink_2_br_2_r1 = shell.addOrReplaceChild("coral_pink_2_br_2_r1", CubeListBuilder.create().texOffs(168, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0994F, -7.5047F, -3.302F, 1.3385F, 0.0439F, 0.0F));

		PartDefinition coral_pink_2_br_1_r1 = shell.addOrReplaceChild("coral_pink_2_br_1_r1", CubeListBuilder.create().texOffs(160, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0994F, -7.5047F, -3.302F, 0.9771F, 1.5146F, 0.0F));

		PartDefinition coral_pink_2_stem_r1 = shell.addOrReplaceChild("coral_pink_2_stem_r1", CubeListBuilder.create().texOffs(152, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, -5.8F, -2.1F, 0.6843F, 0.5269F, 0.0F));

		PartDefinition coral_pink_1_br_3_r1 = shell.addOrReplaceChild("coral_pink_1_br_3_r1", CubeListBuilder.create().texOffs(480, 28).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2597F, -7.5152F, -3.3095F, -2.785F, -1.1034F, 3.1416F));

		PartDefinition coral_pink_1_br_2_r1 = shell.addOrReplaceChild("coral_pink_1_br_2_r1", CubeListBuilder.create().texOffs(144, 28).addBox(-0.5F, -3.1F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2597F, -7.5152F, -3.3095F, 1.3318F, -0.9832F, 0.0F));

		PartDefinition coral_pink_1_br_1_r1 = shell.addOrReplaceChild("coral_pink_1_br_1_r1", CubeListBuilder.create().texOffs(472, 28).addBox(-0.5F, -2.1F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2597F, -7.5152F, -3.3095F, 0.9729F, 0.4927F, 0.0F));

		PartDefinition coral_pink_1_stem_r1 = shell.addOrReplaceChild("coral_pink_1_stem_r1", CubeListBuilder.create().texOffs(136, 28).addBox(-0.5F, -2.6F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, -5.8F, -2.1F, 0.6767F, -0.4993F, 0.0F));

		PartDefinition shell_tier_4_r1 = shell.addOrReplaceChild("shell_tier_4_r1", CubeListBuilder.create().texOffs(252, 2).addBox(-2.5F, -3.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.4F, 14.5F, -0.4189F, 0.0F, 0.0F));

		PartDefinition shell_tier_3_r1 = shell.addOrReplaceChild("shell_tier_3_r1", CubeListBuilder.create().texOffs(150, 2).addBox(-4.5F, -4.5F, -3.5F, 9.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.4F, 11.1F, -0.4189F, 0.0F, 0.0F));

		PartDefinition shell_tier_2_r1 = shell.addOrReplaceChild("shell_tier_2_r1", CubeListBuilder.create().texOffs(58, 2).addBox(-6.5F, -5.5F, -4.5F, 13.0F, 11.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.8F, 6.7F, -0.4189F, 0.0F, 0.0F));

		PartDefinition shell_tier_1_r1 = shell.addOrReplaceChild("shell_tier_1_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-8.0F, -6.0F, -5.0F, 16.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.8F, 1.9F, -0.4189F, 0.0F, 0.0F));

		PartDefinition leg_l_1 = root.addOrReplaceChild("leg_l_1", CubeListBuilder.create().texOffs(76, 28).addBox(-6.9F, 5.2F, -2.4F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.8F, -8.2F, -5.6F));

		PartDefinition leg_l_1_carpus_r1 = leg_l_1.addOrReplaceChild("leg_l_1_carpus_r1", CubeListBuilder.create().texOffs(458, 2).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 1.6F, -0.8F, 0.1306F, 0.0F, -2.7367F));

		PartDefinition leg_l_1_merus_r1 = leg_l_1.addOrReplaceChild("leg_l_1_merus_r1", CubeListBuilder.create().texOffs(320, 2).addBox(-1.0F, -5.8F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.9009F, 1.3986F, 0.0F));

		PartDefinition leg_l_2 = root.addOrReplaceChild("leg_l_2", CubeListBuilder.create().texOffs(86, 28).addBox(-7.7F, 5.2F, 0.2F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -8.2F, -3.2F));

		PartDefinition leg_l_2_carpus_r1 = leg_l_2.addOrReplaceChild("leg_l_2_carpus_r1", CubeListBuilder.create().texOffs(470, 2).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2F, 1.6F, 0.8F, -0.0858F, 0.0F, -2.6972F));

		PartDefinition leg_l_2_merus_r1 = leg_l_2.addOrReplaceChild("leg_l_2_merus_r1", CubeListBuilder.create().texOffs(334, 2).addBox(-1.0F, -5.8F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.2756F, 1.4181F, 3.1416F));

		PartDefinition leg_l_3 = root.addOrReplaceChild("leg_l_3", CubeListBuilder.create().texOffs(96, 28).addBox(-7.3F, 5.4F, 2.6F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, -8.4F, -0.8F));

		PartDefinition leg_l_3_carpus_r1 = leg_l_3.addOrReplaceChild("leg_l_3_carpus_r1", CubeListBuilder.create().texOffs(482, 2).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 1.6F, 2.4F, -0.2473F, 0.0F, -2.7533F));

		PartDefinition leg_l_3_merus_r1 = leg_l_3.addOrReplaceChild("leg_l_3_merus_r1", CubeListBuilder.create().texOffs(292, 2).addBox(-1.0F, -6.8F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.2899F, 1.1233F, 3.1416F));

		PartDefinition leg_r_1 = root.addOrReplaceChild("leg_r_1", CubeListBuilder.create().texOffs(106, 28).addBox(5.9F, 5.2F, -2.4F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(4.8F, -8.2F, -5.6F));

		PartDefinition leg_r_1_carpus_r1 = leg_r_1.addOrReplaceChild("leg_r_1_carpus_r1", CubeListBuilder.create().texOffs(494, 2).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 1.6F, -0.8F, 0.1306F, 0.0F, 2.7367F));

		PartDefinition leg_r_1_merus_r1 = leg_r_1.addOrReplaceChild("leg_r_1_merus_r1", CubeListBuilder.create().texOffs(348, 2).addBox(-1.0F, -5.8F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.9009F, -1.3986F, 0.0F));

		PartDefinition leg_r_2 = root.addOrReplaceChild("leg_r_2", CubeListBuilder.create().texOffs(116, 28).addBox(6.7F, 5.2F, 0.2F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -8.2F, -3.2F));

		PartDefinition leg_r_2_carpus_r1 = leg_r_2.addOrReplaceChild("leg_r_2_carpus_r1", CubeListBuilder.create().texOffs(2, 28).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.2F, 1.6F, 0.8F, -0.0858F, 0.0F, 2.6972F));

		PartDefinition leg_r_2_merus_r1 = leg_r_2.addOrReplaceChild("leg_r_2_merus_r1", CubeListBuilder.create().texOffs(362, 2).addBox(-1.0F, -5.8F, -1.5F, 2.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.2756F, -1.4181F, -3.1416F));

		PartDefinition leg_r_3 = root.addOrReplaceChild("leg_r_3", CubeListBuilder.create().texOffs(126, 28).addBox(6.3F, 5.4F, 2.6F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -8.4F, -0.8F));

		PartDefinition leg_r_3_carpus_r1 = leg_r_3.addOrReplaceChild("leg_r_3_carpus_r1", CubeListBuilder.create().texOffs(14, 28).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 1.6F, 2.4F, -0.2473F, 0.0F, 2.7533F));

		PartDefinition leg_r_3_merus_r1 = leg_r_3.addOrReplaceChild("leg_r_3_merus_r1", CubeListBuilder.create().texOffs(306, 2).addBox(-1.0F, -6.8F, -1.5F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.2899F, -1.1233F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(CoralHermitCrabEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean sifting = entity.getSiftTicks() > 0;
		boolean hunkered = entity.getHunkerTicks() > 0;
		boolean pinching = entity.getPinchTicks() > 0;
		float w = limbSwing * 1.15F;

		// 6-legged crustacean scuttle wave
		ModelPart[] leftLegs = { this.leg_l_1, this.leg_l_2, this.leg_l_3 };
		ModelPart[] rightLegs = { this.leg_r_1, this.leg_r_2, this.leg_r_3 };
		for (int i = 0; i < 3; ++i) {
			float ph = w + i * 1.05F;
			addBbDeg(leftLegs[i], 0.0F, Mth.cos(ph) * 20.0F * move, -Math.max(0.0F, Mth.sin(ph)) * 18.0F * move);
			addBbDeg(rightLegs[i], 0.0F, -Mth.cos(ph + (float) Math.PI) * 20.0F * move,
					Math.max(0.0F, Mth.sin(ph + (float) Math.PI)) * 18.0F * move);
		}

		if (hunkered) {
			addBbPos(this.body, 0.0F, -2.6F, 1.5F);
			addBbPos(this.shell, 0.0F, -1.8F, 0.0F);
			addBbDeg(this.claw_major_r, -18.0F, -26.0F, 0.0F);
			addBbDeg(this.claw_minor_l, -16.0F, 24.0F, 0.0F);
			addBbDeg(this.eyestalk_l, 26.0F, 0.0F, 0.0F);
			addBbDeg(this.eyestalk_r, 26.0F, 0.0F, 0.0F);
		} else {
			addBbDeg(this.shell, Mth.sin(w) * 3.5F * move + Mth.sin(ageInTicks * 0.06F) * 1.2F,
					0.0F, Mth.cos(w) * 3.0F * move);
			addBbDeg(this.eyestalk_l, Mth.sin(ageInTicks * 0.13F) * 5.0F, 0.0F, 0.0F);
			addBbDeg(this.eyestalk_r, Mth.cos(ageInTicks * 0.15F) * 5.0F, 0.0F, 0.0F);

			if (sifting) {
				addBbDeg(this.claw_minor_l, 18.0F + Mth.sin(ageInTicks * 0.65F) * 16.0F, 10.0F, 0.0F);
				addBbDeg(this.claw_major_r, 14.0F + Mth.cos(ageInTicks * 0.65F) * 12.0F, -8.0F, 0.0F);
			} else if (pinching) {
				addBbDeg(this.claw_major_r, -28.0F, 22.0F, -14.0F);
			} else {
				addBbDeg(this.claw_major_r, Mth.sin(ageInTicks * 0.08F) * 4.0F, -Mth.cos(ageInTicks * 0.07F) * 3.0F, 0.0F);
				addBbDeg(this.claw_minor_l, Mth.cos(ageInTicks * 0.09F) * 4.0F, Mth.sin(ageInTicks * 0.08F) * 3.0F, 0.0F);
			}
		}
	}
}
