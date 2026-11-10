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
import ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/amethyst_fox.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class AmethystFoxModel extends FaunaBaseModel<AmethystFoxEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "amethyst_fox"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart ear_l;
	private final ModelPart ear_r;
	private final ModelPart tail_base;
	private final ModelPart leg_front_l;
	private final ModelPart leg_back_l;
	private final ModelPart leg_front_r;
	private final ModelPart leg_back_r;

	public AmethystFoxModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.body.getChild("head");
		this.ear_l = this.head.getChild("ear_l");
		this.ear_r = this.head.getChild("ear_r");
		this.tail_base = this.body.getChild("tail_base");
		this.leg_front_l = this.root.getChild("leg_front_l");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_front_r = this.root.getChild("leg_front_r");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.head, this.ear_l, this.ear_r, this.tail_base, this.leg_front_l, this.leg_back_l, this.leg_front_r, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -19.0F, 1.0F));

		PartDefinition dorsal_shard_r_2_r1 = body.addOrReplaceChild("dorsal_shard_r_2_r1", CubeListBuilder.create().texOffs(362, 27).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -4.0F, 4.4F, -2.3856F, -0.7854F, -3.1416F));

		PartDefinition dorsal_shard_r_1_r1 = body.addOrReplaceChild("dorsal_shard_r_1_r1", CubeListBuilder.create().texOffs(266, 27).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8F, -3.8F, -0.6F, -2.4007F, -0.8961F, -3.1416F));

		PartDefinition dorsal_shard_l_2_r1 = body.addOrReplaceChild("dorsal_shard_l_2_r1", CubeListBuilder.create().texOffs(350, 27).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -4.0F, 4.4F, -2.3856F, 0.7854F, 3.1416F));

		PartDefinition dorsal_shard_l_1_r1 = body.addOrReplaceChild("dorsal_shard_l_1_r1", CubeListBuilder.create().texOffs(250, 27).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.8F, -3.8F, -0.6F, -2.4007F, 0.8961F, 3.1416F));

		PartDefinition dorsal_crys_3_r1 = body.addOrReplaceChild("dorsal_crys_3_r1", CubeListBuilder.create().texOffs(174, 27).addBox(-1.5F, -6.8F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.6F, 4.0F, -0.535F, 0.0F, 0.0F));

		PartDefinition dorsal_crys_2_r1 = body.addOrReplaceChild("dorsal_crys_2_r1", CubeListBuilder.create().texOffs(464, 2).addBox(-2.0F, -8.8F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.6F, -0.5F, -0.4736F, 0.0F, 0.0F));

		PartDefinition dorsal_crys_1_r1 = body.addOrReplaceChild("dorsal_crys_1_r1", CubeListBuilder.create().texOffs(234, 27).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.4F, -4.6F, -0.3217F, 0.0F, 0.0F));

		PartDefinition chest_bib_r1 = body.addOrReplaceChild("chest_bib_r1", CubeListBuilder.create().texOffs(286, 2).addBox(-3.5F, -12.0F, -1.5F, 7.0F, 13.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.2F, -8.8F, 0.4475F, 0.0F, 0.0F));

		PartDefinition neck_column_r1 = body.addOrReplaceChild("neck_column_r1", CubeListBuilder.create().texOffs(94, 2).addBox(-4.0F, -10.6F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, -7.8F, 0.5136F, 0.0F, 0.0F));

		PartDefinition torso_rear_r1 = body.addOrReplaceChild("torso_rear_r1", CubeListBuilder.create().texOffs(50, 2).addBox(-4.5F, -4.5F, -5.5F, 9.0F, 9.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 5.8F, 0.0349F, 0.0F, 0.0F));

		PartDefinition torso_front_r1 = body.addOrReplaceChild("torso_front_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-5.0F, -4.5F, -6.0F, 10.0F, 9.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, -4.2F, -0.0349F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(206, 2).addBox(-5.5F, -5.3F, -6.8F, 11.0F, 8.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(58, 45).addBox(-4.7F, -2.0F, -7.4F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(72, 45).addBox(1.7F, -2.0F, -7.4F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(374, 27).addBox(-2.5F, -0.1F, -10.8F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(398, 27).addBox(-2.5F, 1.5F, -10.6F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.5F, -12.5F));

		PartDefinition cheek_ruff_r_r1 = head.addOrReplaceChild("cheek_ruff_r_r1", CubeListBuilder.create().texOffs(212, 27).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.2F, 1.3F, -1.9F, 0.0F, -0.0698F, 0.1396F));

		PartDefinition cheek_ruff_l_r1 = head.addOrReplaceChild("cheek_ruff_l_r1", CubeListBuilder.create().texOffs(190, 27).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.2F, 1.3F, -1.9F, 0.0F, 0.0698F, -0.1396F));

		PartDefinition ear_l = head.addOrReplaceChild("ear_l", CubeListBuilder.create(), PartPose.offset(-4.0F, -4.9F, -1.7F));

		PartDefinition ear_crystal_l_r1 = ear_l.addOrReplaceChild("ear_crystal_l_r1", CubeListBuilder.create().texOffs(34, 45).addBox(-1.0F, -4.1F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.3F, -7.0F, 0.4F, -0.0554F, 0.0F, -0.3393F));

		PartDefinition ear_mid_l_r1 = ear_l.addOrReplaceChild("ear_mid_l_r1", CubeListBuilder.create().texOffs(2, 45).addBox(-2.0F, -4.2F, -1.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, -4.0F, 0.2F, -0.0549F, 0.0F, -0.3652F));

		PartDefinition ear_base_l_r1 = ear_l.addOrReplaceChild("ear_base_l_r1", CubeListBuilder.create().texOffs(314, 27).addBox(-2.5F, -5.8F, -1.0F, 5.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, 0.4F, 0.0F, -0.039F, 0.0F, -0.3588F));

		PartDefinition ear_r = head.addOrReplaceChild("ear_r", CubeListBuilder.create(), PartPose.offset(4.0F, -4.9F, -1.7F));

		PartDefinition ear_crystal_r_r1 = ear_r.addOrReplaceChild("ear_crystal_r_r1", CubeListBuilder.create().texOffs(46, 45).addBox(-1.0F, -4.1F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.3F, -7.0F, 0.4F, -0.0554F, 0.0F, 0.3393F));

		PartDefinition ear_mid_r_r1 = ear_r.addOrReplaceChild("ear_mid_r_r1", CubeListBuilder.create().texOffs(18, 45).addBox(-2.0F, -4.2F, -1.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, -4.0F, 0.2F, -0.0549F, 0.0F, 0.3652F));

		PartDefinition ear_base_r_r1 = ear_r.addOrReplaceChild("ear_base_r_r1", CubeListBuilder.create().texOffs(332, 27).addBox(-2.5F, -5.8F, -1.0F, 5.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, 0.4F, 0.0F, -0.039F, 0.0F, 0.3588F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create(), PartPose.offset(0.0F, -1.6F, 10.2F));

		PartDefinition tail_geode_r_4_r1 = tail_base.addOrReplaceChild("tail_geode_r_4_r1", CubeListBuilder.create().texOffs(158, 27).addBox(-1.5F, -7.6F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, 9.2F, 16.0F, -2.3514F, -0.7045F, -3.1416F));

		PartDefinition tail_geode_r_3_r1 = tail_base.addOrReplaceChild("tail_geode_r_3_r1", CubeListBuilder.create().texOffs(50, 27).addBox(-1.5F, -8.6F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8F, 11.8F, 16.6F, -1.1686F, -0.5533F, -3.1416F));

		PartDefinition tail_geode_r_2_r1 = tail_base.addOrReplaceChild("tail_geode_r_2_r1", CubeListBuilder.create().texOffs(328, 2).addBox(-1.5F, -10.6F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 10.8F, 16.8F, -1.6512F, -0.5992F, -3.1416F));

		PartDefinition tail_geode_r_1_r1 = tail_base.addOrReplaceChild("tail_geode_r_1_r1", CubeListBuilder.create().texOffs(448, 2).addBox(-1.5F, -10.6F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, 9.8F, 16.6F, -2.1194F, -0.5162F, -3.1416F));

		PartDefinition tail_geode_l_4_r1 = tail_base.addOrReplaceChild("tail_geode_l_4_r1", CubeListBuilder.create().texOffs(142, 27).addBox(-1.5F, -7.6F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, 9.2F, 16.0F, -2.3514F, 0.7045F, 3.1416F));

		PartDefinition tail_geode_l_3_r1 = tail_base.addOrReplaceChild("tail_geode_l_3_r1", CubeListBuilder.create().texOffs(34, 27).addBox(-1.5F, -8.6F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8F, 11.8F, 16.6F, -1.1686F, 0.5533F, 3.1416F));

		PartDefinition tail_geode_l_2_r1 = tail_base.addOrReplaceChild("tail_geode_l_2_r1", CubeListBuilder.create().texOffs(310, 2).addBox(-1.5F, -10.6F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 10.8F, 16.8F, -1.6512F, 0.5992F, 3.1416F));

		PartDefinition tail_geode_l_1_r1 = tail_base.addOrReplaceChild("tail_geode_l_1_r1", CubeListBuilder.create().texOffs(432, 2).addBox(-1.5F, -10.6F, -1.5F, 3.0F, 12.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, 9.8F, 16.6F, -2.1194F, 0.5162F, 3.1416F));

		PartDefinition tail_geode_down_r1 = tail_base.addOrReplaceChild("tail_geode_down_r1", CubeListBuilder.create().texOffs(18, 27).addBox(-1.5F, -8.5F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.8F, 16.8F, -0.976F, 0.0F, 3.1416F));

		PartDefinition tail_geode_up_r1 = tail_base.addOrReplaceChild("tail_geode_up_r1", CubeListBuilder.create().texOffs(414, 2).addBox(-1.5F, -9.5F, -2.0F, 3.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.0F, 16.8F, -2.1815F, 0.0F, 3.1416F));

		PartDefinition tail_geode_center_r1 = tail_base.addOrReplaceChild("tail_geode_center_r1", CubeListBuilder.create().texOffs(394, 2).addBox(-2.0F, -9.5F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.8F, 17.2F, -1.3986F, 0.0F, 3.1416F));

		PartDefinition tail_top_crys_2_r1 = tail_base.addOrReplaceChild("tail_top_crys_2_r1", CubeListBuilder.create().texOffs(298, 27).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.4F, 9.6F, -2.4292F, 0.0F, 3.1416F));

		PartDefinition tail_top_crys_1_r1 = tail_base.addOrReplaceChild("tail_top_crys_1_r1", CubeListBuilder.create().texOffs(282, 27).addBox(-1.5F, -5.8F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 4.0F, -2.4981F, 0.0F, 3.1416F));

		PartDefinition tail_seg_3_r1 = tail_base.addOrReplaceChild("tail_seg_3_r1", CubeListBuilder.create().texOffs(250, 2).addBox(-4.0F, -6.8F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.6F, 12.4F, -1.0516F, 0.0F, 3.1416F));

		PartDefinition tail_seg_2_r1 = tail_base.addOrReplaceChild("tail_seg_2_r1", CubeListBuilder.create().texOffs(130, 2).addBox(-4.5F, -8.8F, -4.5F, 9.0F, 10.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.8F, 6.0F, -1.0612F, 0.0F, 3.1416F));

		PartDefinition tail_seg_1_r1 = tail_base.addOrReplaceChild("tail_seg_1_r1", CubeListBuilder.create().texOffs(170, 2).addBox(-4.0F, -8.6F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.1F, -0.4F, -1.0725F, 0.0F, 3.1416F));

		PartDefinition leg_front_l = root.addOrReplaceChild("leg_front_l", CubeListBuilder.create().texOffs(66, 27).addBox(-2.2F, -2.4F, -2.5F, 4.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(484, 2).addBox(-1.5F, 4.0F, -1.7F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(422, 27).addBox(-1.5F, 14.5F, -3.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.2F, -16.5F, -6.2F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(110, 27).addBox(-1.4F, 6.0F, -0.7F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(442, 27).addBox(-1.4F, 15.0F, -2.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.1F, -17.0F, 8.2F));

		PartDefinition leg_back_l_thigh_r1 = leg_back_l.addOrReplaceChild("leg_back_l_thigh_r1", CubeListBuilder.create().texOffs(346, 2).addBox(-2.0F, -8.0F, -3.0F, 4.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -1.2F, -0.8F, -0.2517F, 0.0F, 3.1416F));

		PartDefinition leg_front_r = root.addOrReplaceChild("leg_front_r", CubeListBuilder.create().texOffs(88, 27).addBox(-1.8F, -2.4F, -2.5F, 4.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 27).addBox(-1.5F, 4.0F, -1.7F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(462, 27).addBox(-1.5F, 14.5F, -3.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.2F, -16.5F, -6.2F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(126, 27).addBox(-1.6F, 6.0F, -0.7F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(482, 27).addBox(-1.6F, 15.0F, -2.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.1F, -17.0F, 8.2F));

		PartDefinition leg_back_r_thigh_r1 = leg_back_r.addOrReplaceChild("leg_back_r_thigh_r1", CubeListBuilder.create().texOffs(370, 2).addBox(-2.0F, -8.0F, -3.0F, 4.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -1.2F, -0.8F, -0.2517F, 0.0F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(AmethystFoxEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean sniffing = entity.getSniffTicks() > 0;
		float w = limbSwing * 0.72F;

		// Agile diagonal fox trot
		float dA = Mth.cos(w) * 34.0F * move;
		float dB = Mth.cos(w + (float) Math.PI) * 34.0F * move;
		addBbDeg(this.leg_front_l, dA, 0.0F, 0.0F);
		addBbDeg(this.leg_back_r, dA * 0.92F, 0.0F, 0.0F);
		addBbDeg(this.leg_front_r, dB, 0.0F, 0.0F);
		addBbDeg(this.leg_back_l, dB * 0.92F, 0.0F, 0.0F);
		addBbDeg(this.body, Mth.sin(w * 2.0F) * 2.2F * move, 0.0F, 0.0F);

		// Head look vs crystal-vein sniffing sweep
		float yaw = Mth.clamp(netHeadYaw, -45.0F, 45.0F);
		float pitch = Mth.clamp(headPitch, -25.0F, 25.0F);
		if (sniffing) {
			addBbDeg(this.head, 24.0F + Mth.sin(ageInTicks * 0.7F) * 4.5F, Mth.sin(ageInTicks * 0.42F) * 18.0F, 0.0F);
		} else {
			addBbDeg(this.head, -pitch * 0.55F + Mth.sin(ageInTicks * 0.06F) * 1.5F, yaw * 0.65F, 0.0F);
		}

		// Independent asymmetric ear flicks & crystal-plume tail sway
		addBbDeg(this.ear_l, Mth.sin(ageInTicks * 0.11F) * 4.0F, 0.0F, -Math.max(0.0F, Mth.sin(ageInTicks * 0.19F)) * 8.0F);
		addBbDeg(this.ear_r, Mth.cos(ageInTicks * 0.13F) * 4.0F, 0.0F, Math.max(0.0F, Mth.sin(ageInTicks * 0.23F + 1.4F)) * 8.0F);

		float tailFreq = sniffing ? 0.45F : (entity.hasTrustedPlayer() ? 0.20F : 0.10F);
		float tailAmp = sniffing ? 20.0F : 10.0F + move * 8.0F;
		addBbDeg(this.tail_base, -move * 12.0F + Mth.sin(ageInTicks * 0.07F) * 3.0F, Mth.sin(ageInTicks * tailFreq) * tailAmp, 0.0F);
	}
}
