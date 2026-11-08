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
import ru.aiforge.riftworld.entity.fauna.AzureFrogEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/azure_frog.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class AzureFrogModel extends FaunaBaseModel<AzureFrogEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "azure_frog"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart collar;
	private final ModelPart head;
	private final ModelPart leg_front_l;
	private final ModelPart leg_back_l;
	private final ModelPart leg_front_r;
	private final ModelPart leg_back_r;

	public AzureFrogModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.collar = this.body.getChild("collar");
		this.head = this.body.getChild("head");
		this.leg_front_l = this.root.getChild("leg_front_l");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_front_r = this.root.getChild("leg_front_r");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.collar, this.head, this.leg_front_l, this.leg_back_l, this.leg_front_r, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -9.5F, 1.5F));

		PartDefinition vocal_chest_r1 = body.addOrReplaceChild("vocal_chest_r1", CubeListBuilder.create().texOffs(234, 2).addBox(-6.5F, -5.5F, -3.0F, 13.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.7F, -7.3F, 0.3142F, 0.0F, 0.0F));

		PartDefinition rump_round_r1 = body.addOrReplaceChild("rump_round_r1", CubeListBuilder.create().texOffs(188, 2).addBox(-7.0F, -5.0F, -3.5F, 14.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.9F, 8.1F, 0.2793F, 0.0F, 0.0F));

		PartDefinition dorsal_hump_r1 = body.addOrReplaceChild("dorsal_hump_r1", CubeListBuilder.create().texOffs(74, 2).addBox(-7.0F, -2.5F, -6.5F, 14.0F, 5.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.3F, 1.3F, 0.4538F, 0.0F, 0.0F));

		PartDefinition torso_core_r1 = body.addOrReplaceChild("torso_core_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-8.5F, -6.5F, -8.5F, 17.0F, 13.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.3F, 0.1F, 0.384F, 0.0F, 0.0F));

		PartDefinition collar = body.addOrReplaceChild("collar", CubeListBuilder.create(), PartPose.offset(0.0F, -7.7F, -4.3F));

		PartDefinition bud_flower_r_5_r1 = collar.addOrReplaceChild("bud_flower_r_5_r1", CubeListBuilder.create().texOffs(38, 65).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.9931F, -4.8078F, 9.9879F, 0.3142F, -2.4086F, 0.0F));

		PartDefinition bud_stem_r_5_r1 = collar.addOrReplaceChild("bud_stem_r_5_r1", CubeListBuilder.create().texOffs(208, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0222F, -2.6078F, 6.6883F, -2.0132F, -0.733F, -3.1416F));

		PartDefinition bud_flower_r_4_r1 = collar.addOrReplaceChild("bud_flower_r_4_r1", CubeListBuilder.create().texOffs(26, 65).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.7822F, -3.4012F, 4.1532F, 0.3142F, -1.885F, 0.0F));

		PartDefinition bud_stem_r_4_r1 = collar.addOrReplaceChild("bud_stem_r_4_r1", CubeListBuilder.create().texOffs(200, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.5595F, -1.2012F, 2.7812F, -2.0132F, -1.2566F, -3.1416F));

		PartDefinition bud_flower_r_3_r1 = collar.addOrReplaceChild("bud_flower_r_3_r1", CubeListBuilder.create().texOffs(14, 65).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0408F, 3.2838F, -3.2514F, 0.3142F, -1.3265F, 0.0F));

		PartDefinition bud_stem_r_3_r1 = collar.addOrReplaceChild("bud_stem_r_3_r1", CubeListBuilder.create().texOffs(192, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.7327F, 0.5838F, -2.1773F, 2.0956F, -1.3265F, 0.0F));

		PartDefinition bud_flower_r_2_r1 = collar.addOrReplaceChild("bud_flower_r_2_r1", CubeListBuilder.create().texOffs(2, 65).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.3362F, 4.8307F, -9.6679F, 0.3142F, -0.7679F, 0.0F));

		PartDefinition bud_stem_r_2_r1 = collar.addOrReplaceChild("bud_stem_r_2_r1", CubeListBuilder.create().texOffs(184, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.2519F, 2.1307F, -6.4741F, 2.0956F, -0.7679F, 0.0F));

		PartDefinition bud_flower_r_1_r1 = collar.addOrReplaceChild("bud_flower_r_1_r1", CubeListBuilder.create().texOffs(492, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.7046F, 5.6145F, -12.9194F, 0.3142F, -0.2793F, 0.0F));

		PartDefinition bud_stem_r_1_r1 = collar.addOrReplaceChild("bud_stem_r_1_r1", CubeListBuilder.create().texOffs(176, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4807F, 2.9145F, -8.6514F, 2.0956F, -0.2793F, 0.0F));

		PartDefinition petal_r_5_r1 = collar.addOrReplaceChild("petal_r_5_r1", CubeListBuilder.create().texOffs(466, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.1313F, -3.1972F, 7.7699F, 0.2443F, -2.6529F, 0.0F));

		PartDefinition petal_r_4_r1 = collar.addOrReplaceChild("petal_r_4_r1", CubeListBuilder.create().texOffs(436, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4628F, -2.0788F, 4.6633F, 0.2443F, -2.1293F, 0.0F));

		PartDefinition petal_r_3_r1 = collar.addOrReplaceChild("petal_r_3_r1", CubeListBuilder.create().texOffs(406, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.8F, -0.4F, 0.0F, 0.384F, -1.5708F, 0.0F));

		PartDefinition petal_r_2_r1 = collar.addOrReplaceChild("petal_r_2_r1", CubeListBuilder.create().texOffs(376, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4628F, 1.2788F, -4.6633F, 0.5236F, -1.0123F, 0.0F));

		PartDefinition petal_r_1_r1 = collar.addOrReplaceChild("petal_r_1_r1", CubeListBuilder.create().texOffs(346, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.1313F, 2.3972F, -7.7699F, 0.5236F, -0.4887F, 0.0F));

		PartDefinition bud_flower_l_5_r1 = collar.addOrReplaceChild("bud_flower_l_5_r1", CubeListBuilder.create().texOffs(480, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.9931F, -4.8078F, 9.9879F, 0.3142F, 2.4086F, 0.0F));

		PartDefinition bud_stem_l_5_r1 = collar.addOrReplaceChild("bud_stem_l_5_r1", CubeListBuilder.create().texOffs(168, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0222F, -2.6078F, 6.6883F, -2.0132F, 0.733F, 3.1416F));

		PartDefinition bud_flower_l_4_r1 = collar.addOrReplaceChild("bud_flower_l_4_r1", CubeListBuilder.create().texOffs(468, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.7822F, -3.4012F, 4.1532F, 0.3142F, 1.885F, 0.0F));

		PartDefinition bud_stem_l_4_r1 = collar.addOrReplaceChild("bud_stem_l_4_r1", CubeListBuilder.create().texOffs(160, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5595F, -1.2012F, 2.7812F, -2.0132F, 1.2566F, 3.1416F));

		PartDefinition bud_flower_l_3_r1 = collar.addOrReplaceChild("bud_flower_l_3_r1", CubeListBuilder.create().texOffs(456, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0408F, 3.2838F, -3.2514F, 0.3142F, 1.3265F, 0.0F));

		PartDefinition bud_stem_l_3_r1 = collar.addOrReplaceChild("bud_stem_l_3_r1", CubeListBuilder.create().texOffs(152, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.7327F, 0.5838F, -2.1773F, 2.0956F, 1.3265F, 0.0F));

		PartDefinition bud_flower_l_2_r1 = collar.addOrReplaceChild("bud_flower_l_2_r1", CubeListBuilder.create().texOffs(444, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.3362F, 4.8307F, -9.6679F, 0.3142F, 0.7679F, 0.0F));

		PartDefinition bud_stem_l_2_r1 = collar.addOrReplaceChild("bud_stem_l_2_r1", CubeListBuilder.create().texOffs(144, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.2519F, 2.1307F, -6.4741F, 2.0956F, 0.7679F, 0.0F));

		PartDefinition bud_flower_l_1_r1 = collar.addOrReplaceChild("bud_flower_l_1_r1", CubeListBuilder.create().texOffs(432, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.7046F, 5.6145F, -12.9194F, 0.3142F, 0.2793F, 0.0F));

		PartDefinition bud_stem_l_1_r1 = collar.addOrReplaceChild("bud_stem_l_1_r1", CubeListBuilder.create().texOffs(136, 52).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4807F, 2.9145F, -8.6514F, 2.0956F, 0.2793F, 0.0F));

		PartDefinition petal_l_5_r1 = collar.addOrReplaceChild("petal_l_5_r1", CubeListBuilder.create().texOffs(316, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.1313F, -3.1972F, 7.7699F, 0.2443F, 2.6529F, 0.0F));

		PartDefinition petal_l_4_r1 = collar.addOrReplaceChild("petal_l_4_r1", CubeListBuilder.create().texOffs(286, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4628F, -2.0788F, 4.6633F, 0.2443F, 2.1293F, 0.0F));

		PartDefinition petal_l_3_r1 = collar.addOrReplaceChild("petal_l_3_r1", CubeListBuilder.create().texOffs(256, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.8F, -0.4F, 0.0F, 0.384F, 1.5708F, 0.0F));

		PartDefinition petal_l_2_r1 = collar.addOrReplaceChild("petal_l_2_r1", CubeListBuilder.create().texOffs(226, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4628F, 1.2788F, -4.6633F, 0.5236F, 1.0123F, 0.0F));

		PartDefinition petal_l_1_r1 = collar.addOrReplaceChild("petal_l_1_r1", CubeListBuilder.create().texOffs(196, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.1313F, 2.3972F, -7.7699F, 0.5236F, 0.4887F, 0.0F));

		PartDefinition petal_back_center_r1 = collar.addOrReplaceChild("petal_back_center_r1", CubeListBuilder.create().texOffs(166, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 8.0F, 0.384F, -3.1416F, 0.0F));

		PartDefinition petal_front_center_r1 = collar.addOrReplaceChild("petal_front_center_r1", CubeListBuilder.create().texOffs(136, 36).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.4F, -7.8F, 0.5585F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -9.3F, -5.0F));

		PartDefinition eye_globe_r_r1 = head.addOrReplaceChild("eye_globe_r_r1", CubeListBuilder.create().texOffs(84, 52).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8F, -6.4F, -3.7F, -0.0698F, -0.4538F, 0.0524F));

		PartDefinition eye_hood_r_r1 = head.addOrReplaceChild("eye_hood_r_r1", CubeListBuilder.create().texOffs(112, 36).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -6.6F, -2.3F, -0.1047F, -0.4189F, 0.1047F));

		PartDefinition eye_globe_l_r1 = head.addOrReplaceChild("eye_globe_l_r1", CubeListBuilder.create().texOffs(62, 52).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.8F, -6.4F, -3.7F, -0.0698F, 0.4538F, -0.0524F));

		PartDefinition eye_hood_l_r1 = head.addOrReplaceChild("eye_hood_l_r1", CubeListBuilder.create().texOffs(88, 36).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4F, -6.6F, -2.3F, -0.1047F, 0.4189F, -0.1047F));

		PartDefinition jaw_lower_r1 = head.addOrReplaceChild("jaw_lower_r1", CubeListBuilder.create().texOffs(2, 36).addBox(-6.0F, -1.5F, -4.5F, 12.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -4.3F, -0.0698F, 0.0F, 0.0F));

		PartDefinition snout_tip_r1 = head.addOrReplaceChild("snout_tip_r1", CubeListBuilder.create().texOffs(106, 52).addBox(-5.0F, -2.0F, -1.5F, 10.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.4F, -7.9F, -0.1047F, 0.0F, 0.0F));

		PartDefinition snout_mid_r1 = head.addOrReplaceChild("snout_mid_r1", CubeListBuilder.create().texOffs(48, 36).addBox(-6.5F, -2.5F, -2.5F, 13.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.6F, -6.3F, -0.1047F, 0.0F, 0.0F));

		PartDefinition head_cranium_r1 = head.addOrReplaceChild("head_cranium_r1", CubeListBuilder.create().texOffs(132, 2).addBox(-7.5F, -3.0F, -5.5F, 15.0F, 6.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -2.1F, -0.1047F, 0.0F, 0.0F));

		PartDefinition leg_front_l = root.addOrReplaceChild("leg_front_l", CubeListBuilder.create().texOffs(264, 52).addBox(2.0F, 10.2F, -8.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(278, 52).addBox(-0.6F, 10.2F, -9.1F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(292, 52).addBox(-3.2F, 10.2F, -8.1F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(216, 52).addBox(-2.4F, 10.2F, -7.2F, 6.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-7.2F, -12.2F, -5.0F));

		PartDefinition leg_front_l_fore_r1 = leg_front_l.addOrReplaceChild("leg_front_l_fore_r1", CubeListBuilder.create().texOffs(412, 2).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, 4.4F, -2.0F, 0.4251F, 0.0F, 2.8966F));

		PartDefinition leg_front_l_upper_r1 = leg_front_l.addOrReplaceChild("leg_front_l_upper_r1", CubeListBuilder.create().texOffs(460, 2).addBox(-2.5F, -6.6F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, -0.4F, 0.2F, 0.4399F, 0.0F, -2.9442F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(2, 52).addBox(-3.8F, 6.8F, -6.6F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(306, 52).addBox(0.4F, 6.8F, -8.6F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(320, 52).addBox(-2.0F, 6.8F, -9.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(334, 52).addBox(-4.4F, 6.8F, -8.2F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, -8.8F, 4.5F));

		PartDefinition leg_back_l_calf_r1 = leg_back_l.addOrReplaceChild("leg_back_l_calf_r1", CubeListBuilder.create().texOffs(344, 2).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, 4.4F, 0.7F, 0.2094F, -0.1396F, 0.0F));

		PartDefinition leg_back_l_thigh_r1 = leg_back_l.addOrReplaceChild("leg_back_l_thigh_r1", CubeListBuilder.create().texOffs(276, 2).addBox(-2.5F, -3.5F, -5.0F, 5.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8F, 0.6F, -0.7F, -0.4887F, -0.1745F, -0.1396F));

		PartDefinition leg_front_r = root.addOrReplaceChild("leg_front_r", CubeListBuilder.create().texOffs(348, 52).addBox(-4.0F, 10.2F, -8.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(362, 52).addBox(-1.4F, 10.2F, -9.1F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(376, 52).addBox(1.2F, 10.2F, -8.1F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(240, 52).addBox(-3.6F, 10.2F, -7.2F, 6.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(7.2F, -12.2F, -5.0F));

		PartDefinition leg_front_r_fore_r1 = leg_front_r.addOrReplaceChild("leg_front_r_fore_r1", CubeListBuilder.create().texOffs(436, 2).addBox(-2.5F, -8.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7F, 4.4F, -2.0F, 0.4251F, 0.0F, -2.8966F));

		PartDefinition leg_front_r_upper_r1 = leg_front_r.addOrReplaceChild("leg_front_r_upper_r1", CubeListBuilder.create().texOffs(484, 2).addBox(-2.5F, -6.6F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, -0.4F, 0.2F, 0.4399F, 0.0F, 2.9442F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(32, 52).addBox(-2.2F, 6.8F, -6.6F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(390, 52).addBox(-2.4F, 6.8F, -8.6F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(404, 52).addBox(0.0F, 6.8F, -9.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(418, 52).addBox(2.4F, 6.8F, -8.2F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, -8.8F, 4.5F));

		PartDefinition leg_back_r_calf_r1 = leg_back_r.addOrReplaceChild("leg_back_r_calf_r1", CubeListBuilder.create().texOffs(378, 2).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, 4.4F, 0.7F, 0.2094F, 0.1396F, 0.0F));

		PartDefinition leg_back_r_thigh_r1 = leg_back_r.addOrReplaceChild("leg_back_r_thigh_r1", CubeListBuilder.create().texOffs(310, 2).addBox(-2.5F, -3.5F, -5.0F, 5.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.8F, 0.6F, -0.7F, -0.4887F, 0.1745F, 0.1396F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(AzureFrogEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		boolean leaping = entity.isLeaping();
		float tongue = entity.getTongueProgress(pt);

		// Throat/collar vocal-sac breathing pulse
		float throatPulse = 0.5F + 0.5F * Mth.sin(ageInTicks * 0.16F);
		this.collar.xScale *= (1.0F + 0.06F * throatPulse + 0.10F * tongue);
		this.collar.yScale *= (1.0F + 0.08F * throatPulse + 0.12F * tongue);

		if (leaping) {
			addBbDeg(this.body, -24.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, 52.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, 52.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -34.0F, 0.0F, 0.0F);
		} else {
			float hop = limbSwing * 0.9F;
			float bounce = Math.abs(Mth.sin(hop)) * move;
			addBbPos(this.body, 0.0F, bounce * 2.2F, 0.0F);
			addBbDeg(this.body, -bounce * 12.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_l, bounce * 34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_back_r, bounce * 34.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_l, -bounce * 22.0F, 0.0F, 0.0F);
			addBbDeg(this.leg_front_r, -bounce * 22.0F, 0.0F, 0.0F);
		}

		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.45F - tongue * 18.0F,
				Mth.clamp(netHeadYaw, -35.0F, 35.0F) * 0.5F, 0.0F);
		addBbPos(this.head, 0.0F, 0.0F, -tongue * 1.8F);
	}
}
