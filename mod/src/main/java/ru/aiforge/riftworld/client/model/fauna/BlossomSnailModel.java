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
import ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/blossom_snail.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class BlossomSnailModel extends FaunaBaseModel<BlossomSnailEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "blossom_snail"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart eyestalk_l;
	private final ModelPart eyestalk_r;
	private final ModelPart shell;

	public BlossomSnailModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
		this.eyestalk_l = this.head.getChild("eyestalk_l");
		this.eyestalk_r = this.head.getChild("eyestalk_r");
		this.shell = this.body.getChild("shell");
		captureRestPoses(this.root, this.body, this.neck, this.head, this.eyestalk_l, this.eyestalk_r, this.shell);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(64, 2).addBox(-5.0F, 1.0F, -12.5F, 10.0F, 3.0F, 27.0F, new CubeDeformation(0.0F))
		.texOffs(210, 2).addBox(-4.0F, -2.6F, -10.0F, 8.0F, 4.0F, 21.0F, new CubeDeformation(0.0F))
		.texOffs(308, 57).addBox(-3.5F, 1.0F, 14.0F, 7.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition neck_moss_r_r1 = body.addOrReplaceChild("neck_moss_r_r1", CubeListBuilder.create().texOffs(424, 57).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, -5.8F, -6.2F, -0.3142F, 0.0F, 0.2443F));

		PartDefinition neck_moss_l_r1 = body.addOrReplaceChild("neck_moss_l_r1", CubeListBuilder.create().texOffs(410, 57).addBox(-1.0F, -2.0F, -1.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, -5.8F, -6.2F, -0.3142F, 0.0F, -0.2443F));

		PartDefinition tail_moss_patch_r1 = body.addOrReplaceChild("tail_moss_patch_r1", CubeListBuilder.create().texOffs(362, 57).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4F, 15.0F, 0.1396F, 0.0F, 0.0F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -1.8F, -9.2F));

		PartDefinition neck_belly_plate_r1 = neck.addOrReplaceChild("neck_belly_plate_r1", CubeListBuilder.create().texOffs(300, 2).addBox(-2.5F, -10.2F, -1.0F, 5.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.6F, -2.4F, 0.2253F, 0.0F, 0.0F));

		PartDefinition neck_column_r1 = neck.addOrReplaceChild("neck_column_r1", CubeListBuilder.create().texOffs(272, 2).addBox(-3.0F, -10.6F, -3.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.4F, 0.2873F, 0.0F, 0.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(268, 42).addBox(-3.5F, -3.9F, -4.0F, 7.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(386, 57).addBox(-2.5F, -5.2F, -3.4F, 5.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, -2.4F));

		PartDefinition feeler_low_r_r1 = head.addOrReplaceChild("feeler_low_r_r1", CubeListBuilder.create().texOffs(482, 80).addBox(-0.5F, -2.2F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -0.6F, -3.0F, 1.815F, -0.8442F, 0.0F));

		PartDefinition feeler_low_l_r1 = head.addOrReplaceChild("feeler_low_l_r1", CubeListBuilder.create().texOffs(474, 80).addBox(-0.5F, -2.2F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -0.6F, -3.0F, 1.815F, 0.8442F, 0.0F));

		PartDefinition eyestalk_l = head.addOrReplaceChild("eyestalk_l", CubeListBuilder.create().texOffs(376, 71).addBox(-1.6F, -7.1F, -1.7F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.3F, -4.4F, -1.2F));

		PartDefinition eyestalk_shaft_l_r1 = eyestalk_l.addOrReplaceChild("eyestalk_shaft_l_r1", CubeListBuilder.create().texOffs(346, 57).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, 0.4F, 0.0F, 0.1148F, 0.0F, -0.0384F));

		PartDefinition eyestalk_r = head.addOrReplaceChild("eyestalk_r", CubeListBuilder.create().texOffs(390, 71).addBox(-1.4F, -7.1F, -1.7F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.3F, -4.4F, -1.2F));

		PartDefinition eyestalk_shaft_r_r1 = eyestalk_r.addOrReplaceChild("eyestalk_shaft_r_r1", CubeListBuilder.create().texOffs(354, 57).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, 0.4F, 0.0F, 0.1148F, 0.0F, 0.0384F));

		PartDefinition shell = body.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(2, 2).addBox(-5.5F, -9.0F, -9.0F, 11.0F, 18.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(318, 2).addBox(-5.0F, -12.8F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(276, 57).addBox(-3.5F, -13.6F, -3.5F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(142, 2).addBox(-6.8F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(244, 57).addBox(-6.9F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(490, 80).addBox(-9.5F, -11.0F, 4.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(498, 80).addBox(-10.1F, -3.5F, 11.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(506, 80).addBox(-9.3F, 4.5F, 8.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(176, 2).addBox(4.8F, -6.5F, -6.5F, 2.0F, 13.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(260, 57).addBox(5.9F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 88).addBox(8.5F, -11.0F, 4.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 88).addBox(9.1F, -3.5F, 11.8F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 88).addBox(8.3F, 4.5F, 8.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 3.5F));

		PartDefinition calyx_moss_r_1_r1 = shell.addOrReplaceChild("calyx_moss_r_1_r1", CubeListBuilder.create().texOffs(472, 57).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -7.4F, -5.9F, -0.4189F, 0.2094F, 0.3142F));

		PartDefinition rose_inner_r_7_r1 = shell.addOrReplaceChild("rose_inner_r_7_r1", CubeListBuilder.create().texOffs(456, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, -4.48F, -3.6837F, -2.5676F, -0.7071F, -1.3847F));

		PartDefinition rose_inner_r_6_r1 = shell.addOrReplaceChild("rose_inner_r_6_r1", CubeListBuilder.create().texOffs(438, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, 0.0868F, -5.7994F, -2.42F, 0.18F, -1.4273F));

		PartDefinition rose_inner_r_5_r1 = shell.addOrReplaceChild("rose_inner_r_5_r1", CubeListBuilder.create().texOffs(420, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, 4.5882F, -3.548F, -2.1888F, 1.0612F, -1.2783F));

		PartDefinition rose_inner_r_4_r1 = shell.addOrReplaceChild("rose_inner_r_4_r1", CubeListBuilder.create().texOffs(402, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, 5.6346F, 1.3751F, 0.3797F, 1.142F, 1.2257F));

		PartDefinition rose_inner_r_3_r1 = shell.addOrReplaceChild("rose_inner_r_3_r1", CubeListBuilder.create().texOffs(384, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, 2.4381F, 5.2627F, 0.6573F, 0.2642F, 1.4246F));

		PartDefinition rose_inner_r_2_r1 = shell.addOrReplaceChild("rose_inner_r_2_r1", CubeListBuilder.create().texOffs(366, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, -2.5944F, 5.1874F, 0.7981F, -0.6234F, 1.3967F));

		PartDefinition rose_inner_r_1_r1 = shell.addOrReplaceChild("rose_inner_r_1_r1", CubeListBuilder.create().texOffs(348, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.9F, -5.6733F, 1.2059F, 1.9725F, -1.4234F, 0.291F));

		PartDefinition rose_mid_r_8_r1 = shell.addOrReplaceChild("rose_mid_r_8_r1", CubeListBuilder.create().texOffs(202, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, -7.2805F, -2.5069F, -2.7466F, -1.0384F, -1.2989F));

		PartDefinition rose_mid_r_7_r1 = shell.addOrReplaceChild("rose_mid_r_7_r1", CubeListBuilder.create().texOffs(182, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, -3.3755F, -6.9207F, -2.5484F, -0.2667F, -1.429F));

		PartDefinition rose_mid_r_6_r1 = shell.addOrReplaceChild("rose_mid_r_6_r1", CubeListBuilder.create().texOffs(162, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, 2.5069F, -7.2805F, -2.4336F, 0.5109F, -1.4139F));

		PartDefinition rose_mid_r_5_r1 = shell.addOrReplaceChild("rose_mid_r_5_r1", CubeListBuilder.create().texOffs(142, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, 6.9207F, -3.3755F, -2.0519F, 1.2696F, -1.0933F));

		PartDefinition rose_mid_r_4_r1 = shell.addOrReplaceChild("rose_mid_r_4_r1", CubeListBuilder.create().texOffs(122, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, 7.2805F, 2.5069F, 0.395F, 1.0384F, 1.2989F));

		PartDefinition rose_mid_r_3_r1 = shell.addOrReplaceChild("rose_mid_r_3_r1", CubeListBuilder.create().texOffs(102, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, 3.3755F, 6.9207F, 0.5932F, 0.2667F, 1.429F));

		PartDefinition rose_mid_r_2_r1 = shell.addOrReplaceChild("rose_mid_r_2_r1", CubeListBuilder.create().texOffs(82, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, -2.5069F, 7.2805F, 0.708F, -0.5109F, 1.4139F));

		PartDefinition rose_mid_r_1_r1 = shell.addOrReplaceChild("rose_mid_r_1_r1", CubeListBuilder.create().texOffs(62, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4F, -6.9207F, 3.3755F, 1.0897F, -1.2696F, 1.0933F));

		PartDefinition rose_outer_r_9_r1 = shell.addOrReplaceChild("rose_outer_r_9_r1", CubeListBuilder.create().texOffs(354, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, -8.0417F, -5.4242F, -2.7087F, -0.7657F, -1.3911F));

		PartDefinition rose_outer_r_8_r1 = shell.addOrReplaceChild("rose_outer_r_8_r1", CubeListBuilder.create().texOffs(332, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, -2.6737F, -9.3242F, -2.5932F, -0.0751F, -1.4412F));

		PartDefinition rose_outer_r_7_r1 = shell.addOrReplaceChild("rose_outer_r_7_r1", CubeListBuilder.create().texOffs(310, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, 3.9453F, -8.8614F, -2.4913F, 0.6164F, -1.4122F));

		PartDefinition rose_outer_r_6_r1 = shell.addOrReplaceChild("rose_outer_r_6_r1", CubeListBuilder.create().texOffs(288, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, 8.7183F, -4.2522F, -2.1166F, 1.2898F, -1.0876F));

		PartDefinition rose_outer_r_5_r1 = shell.addOrReplaceChild("rose_outer_r_5_r1", CubeListBuilder.create().texOffs(266, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, 9.4119F, 2.3466F, 0.2961F, 1.1059F, 1.2793F));

		PartDefinition rose_outer_r_4_r1 = shell.addOrReplaceChild("rose_outer_r_4_r1", CubeListBuilder.create().texOffs(244, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, 5.7015F, 7.8475F, 0.4999F, 0.421F, 1.4291F));

		PartDefinition rose_outer_r_3_r1 = shell.addOrReplaceChild("rose_outer_r_3_r1", CubeListBuilder.create().texOffs(222, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, -0.6766F, 9.6764F, 0.5942F, -0.271F, 1.4367F));

		PartDefinition rose_outer_r_2_r1 = shell.addOrReplaceChild("rose_outer_r_2_r1", CubeListBuilder.create().texOffs(200, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, -6.7382F, 6.9776F, 0.7445F, -0.9594F, 1.3444F));

		PartDefinition rose_outer_r_1_r1 = shell.addOrReplaceChild("rose_outer_r_1_r1", CubeListBuilder.create().texOffs(178, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.9F, -9.6469F, 1.0139F, 2.7847F, -1.4083F, -0.6493F));

		PartDefinition calyx_seg_r_10_r1 = shell.addOrReplaceChild("calyx_seg_r_10_r1", CubeListBuilder.create().texOffs(222, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(2, 57).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -9.7008F, -3.152F, 5.969F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_9_r1 = shell.addOrReplaceChild("calyx_seg_r_9_r1", CubeListBuilder.create().texOffs(200, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(474, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -5.9954F, -8.252F, 5.3407F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_8_r1 = shell.addOrReplaceChild("calyx_seg_r_8_r1", CubeListBuilder.create().texOffs(178, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(452, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 0.0F, -10.2F, 4.7124F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_7_r1 = shell.addOrReplaceChild("calyx_seg_r_7_r1", CubeListBuilder.create().texOffs(156, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(430, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 5.9954F, -8.252F, 4.0841F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_6_r1 = shell.addOrReplaceChild("calyx_seg_r_6_r1", CubeListBuilder.create().texOffs(134, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(408, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 9.7008F, -3.152F, 3.4558F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_5_r1 = shell.addOrReplaceChild("calyx_seg_r_5_r1", CubeListBuilder.create().texOffs(112, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(386, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 9.7008F, 3.152F, 2.8274F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_4_r1 = shell.addOrReplaceChild("calyx_seg_r_4_r1", CubeListBuilder.create().texOffs(90, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(364, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 5.9954F, 8.252F, 2.1991F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_3_r1 = shell.addOrReplaceChild("calyx_seg_r_3_r1", CubeListBuilder.create().texOffs(68, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(342, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 0.0F, 10.2F, 1.5708F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_2_r1 = shell.addOrReplaceChild("calyx_seg_r_2_r1", CubeListBuilder.create().texOffs(46, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(320, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -5.9954F, 8.252F, 0.9425F, 0.0F, 0.0F));

		PartDefinition calyx_seg_r_1_r1 = shell.addOrReplaceChild("calyx_seg_r_1_r1", CubeListBuilder.create().texOffs(24, 57).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(298, 42).addBox(-11.8F, -1.5F, -3.5F, 2.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, -9.7008F, 3.152F, 0.3142F, 0.0F, 0.0F));

		PartDefinition calyx_moss_l_1_r1 = shell.addOrReplaceChild("calyx_moss_l_1_r1", CubeListBuilder.create().texOffs(458, 57).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -7.4F, -5.9F, -0.4189F, -0.2094F, -0.3142F));

		PartDefinition rose_inner_l_7_r1 = shell.addOrReplaceChild("rose_inner_l_7_r1", CubeListBuilder.create().texOffs(330, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, -4.48F, -3.6837F, -2.5676F, 0.7071F, 1.3847F));

		PartDefinition rose_inner_l_6_r1 = shell.addOrReplaceChild("rose_inner_l_6_r1", CubeListBuilder.create().texOffs(312, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, 0.0868F, -5.7994F, -2.42F, -0.18F, 1.4273F));

		PartDefinition rose_inner_l_5_r1 = shell.addOrReplaceChild("rose_inner_l_5_r1", CubeListBuilder.create().texOffs(294, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, 4.5882F, -3.548F, -2.1888F, -1.0612F, 1.2783F));

		PartDefinition rose_inner_l_4_r1 = shell.addOrReplaceChild("rose_inner_l_4_r1", CubeListBuilder.create().texOffs(276, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, 5.6346F, 1.3751F, 0.3797F, -1.142F, -1.2257F));

		PartDefinition rose_inner_l_3_r1 = shell.addOrReplaceChild("rose_inner_l_3_r1", CubeListBuilder.create().texOffs(258, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, 2.4381F, 5.2627F, 0.6573F, -0.2642F, -1.4246F));

		PartDefinition rose_inner_l_2_r1 = shell.addOrReplaceChild("rose_inner_l_2_r1", CubeListBuilder.create().texOffs(240, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, -2.5944F, 5.1874F, 0.7981F, 0.6234F, -1.3967F));

		PartDefinition rose_inner_l_1_r1 = shell.addOrReplaceChild("rose_inner_l_1_r1", CubeListBuilder.create().texOffs(222, 80).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9F, -5.6733F, 1.2059F, 1.9725F, 1.4234F, -0.291F));

		PartDefinition rose_mid_l_8_r1 = shell.addOrReplaceChild("rose_mid_l_8_r1", CubeListBuilder.create().texOffs(42, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, -7.2805F, -2.5069F, -2.7466F, 1.0384F, 1.2989F));

		PartDefinition rose_mid_l_7_r1 = shell.addOrReplaceChild("rose_mid_l_7_r1", CubeListBuilder.create().texOffs(22, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, -3.3755F, -6.9207F, -2.5484F, 0.2667F, 1.429F));

		PartDefinition rose_mid_l_6_r1 = shell.addOrReplaceChild("rose_mid_l_6_r1", CubeListBuilder.create().texOffs(2, 80).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, 2.5069F, -7.2805F, -2.4336F, -0.5109F, 1.4139F));

		PartDefinition rose_mid_l_5_r1 = shell.addOrReplaceChild("rose_mid_l_5_r1", CubeListBuilder.create().texOffs(484, 71).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, 6.9207F, -3.3755F, -2.0519F, -1.2696F, 1.0933F));

		PartDefinition rose_mid_l_4_r1 = shell.addOrReplaceChild("rose_mid_l_4_r1", CubeListBuilder.create().texOffs(464, 71).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, 7.2805F, 2.5069F, 0.395F, -1.0384F, -1.2989F));

		PartDefinition rose_mid_l_3_r1 = shell.addOrReplaceChild("rose_mid_l_3_r1", CubeListBuilder.create().texOffs(444, 71).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, 3.3755F, 6.9207F, 0.5932F, -0.2667F, -1.429F));

		PartDefinition rose_mid_l_2_r1 = shell.addOrReplaceChild("rose_mid_l_2_r1", CubeListBuilder.create().texOffs(424, 71).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, -2.5069F, 7.2805F, 0.708F, 0.5109F, -1.4139F));

		PartDefinition rose_mid_l_1_r1 = shell.addOrReplaceChild("rose_mid_l_1_r1", CubeListBuilder.create().texOffs(404, 71).addBox(-2.5F, -0.5F, -1.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.4F, -6.9207F, 3.3755F, 1.0897F, 1.2696F, -1.0933F));

		PartDefinition rose_outer_l_9_r1 = shell.addOrReplaceChild("rose_outer_l_9_r1", CubeListBuilder.create().texOffs(156, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -8.0417F, -5.4242F, -2.7087F, 0.7657F, 1.3911F));

		PartDefinition rose_outer_l_8_r1 = shell.addOrReplaceChild("rose_outer_l_8_r1", CubeListBuilder.create().texOffs(134, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -2.6737F, -9.3242F, -2.5932F, 0.0751F, 1.4412F));

		PartDefinition rose_outer_l_7_r1 = shell.addOrReplaceChild("rose_outer_l_7_r1", CubeListBuilder.create().texOffs(112, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, 3.9453F, -8.8614F, -2.4913F, -0.6164F, 1.4122F));

		PartDefinition rose_outer_l_6_r1 = shell.addOrReplaceChild("rose_outer_l_6_r1", CubeListBuilder.create().texOffs(90, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, 8.7183F, -4.2522F, -2.1166F, -1.2898F, 1.0876F));

		PartDefinition rose_outer_l_5_r1 = shell.addOrReplaceChild("rose_outer_l_5_r1", CubeListBuilder.create().texOffs(68, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, 9.4119F, 2.3466F, 0.2961F, -1.1059F, -1.2793F));

		PartDefinition rose_outer_l_4_r1 = shell.addOrReplaceChild("rose_outer_l_4_r1", CubeListBuilder.create().texOffs(46, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, 5.7015F, 7.8475F, 0.4999F, -0.421F, -1.4291F));

		PartDefinition rose_outer_l_3_r1 = shell.addOrReplaceChild("rose_outer_l_3_r1", CubeListBuilder.create().texOffs(24, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -0.6766F, 9.6764F, 0.5942F, 0.271F, -1.4367F));

		PartDefinition rose_outer_l_2_r1 = shell.addOrReplaceChild("rose_outer_l_2_r1", CubeListBuilder.create().texOffs(2, 71).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -6.7382F, 6.9776F, 0.7445F, 0.9594F, -1.3444F));

		PartDefinition rose_outer_l_1_r1 = shell.addOrReplaceChild("rose_outer_l_1_r1", CubeListBuilder.create().texOffs(486, 57).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.9F, -9.6469F, 1.0139F, 2.7847F, 1.4083F, 0.6493F));

		PartDefinition shell_sprout_back_2_r1 = shell.addOrReplaceChild("shell_sprout_back_2_r1", CubeListBuilder.create().texOffs(448, 57).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, 8.5F, -2.0344F, 0.0F, 3.1416F));

		PartDefinition shell_sprout_back_1_r1 = shell.addOrReplaceChild("shell_sprout_back_1_r1", CubeListBuilder.create().texOffs(336, 57).addBox(-1.0F, -6.0F, -0.5F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, 7.9F, -0.5984F, 0.0F, 0.0F));

		PartDefinition shell_sprout_front_1_r1 = shell.addOrReplaceChild("shell_sprout_front_1_r1", CubeListBuilder.create().texOffs(438, 57).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.8F, -7.3F, 0.4636F, 0.0F, 0.0F));

		PartDefinition shell_arch_12_r1 = shell.addOrReplaceChild("shell_arch_12_r1", CubeListBuilder.create().texOffs(230, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.3531F, -5.4F, 5.7596F, 0.0F, 0.0F));

		PartDefinition shell_arch_11_r1 = shell.addOrReplaceChild("shell_arch_11_r1", CubeListBuilder.create().texOffs(192, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.4F, -9.3531F, 5.236F, 0.0F, 0.0F));

		PartDefinition shell_arch_10_r1 = shell.addOrReplaceChild("shell_arch_10_r1", CubeListBuilder.create().texOffs(154, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -10.8F, 4.7124F, 0.0F, 0.0F));

		PartDefinition shell_arch_9_r1 = shell.addOrReplaceChild("shell_arch_9_r1", CubeListBuilder.create().texOffs(116, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.4F, -9.3531F, 4.1888F, 0.0F, 0.0F));

		PartDefinition shell_arch_8_r1 = shell.addOrReplaceChild("shell_arch_8_r1", CubeListBuilder.create().texOffs(78, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.3531F, -5.4F, 3.6652F, 0.0F, 0.0F));

		PartDefinition shell_arch_7_r1 = shell.addOrReplaceChild("shell_arch_7_r1", CubeListBuilder.create().texOffs(40, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.8F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition shell_arch_6_r1 = shell.addOrReplaceChild("shell_arch_6_r1", CubeListBuilder.create().texOffs(2, 42).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.3531F, 5.4F, 2.618F, 0.0F, 0.0F));

		PartDefinition shell_arch_5_r1 = shell.addOrReplaceChild("shell_arch_5_r1", CubeListBuilder.create().texOffs(470, 2).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.4F, 9.3531F, 2.0944F, 0.0F, 0.0F));

		PartDefinition shell_arch_4_r1 = shell.addOrReplaceChild("shell_arch_4_r1", CubeListBuilder.create().texOffs(432, 2).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 10.8F, 1.5708F, 0.0F, 0.0F));

		PartDefinition shell_arch_3_r1 = shell.addOrReplaceChild("shell_arch_3_r1", CubeListBuilder.create().texOffs(394, 2).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.4F, 9.3531F, 1.0472F, 0.0F, 0.0F));

		PartDefinition shell_arch_2_r1 = shell.addOrReplaceChild("shell_arch_2_r1", CubeListBuilder.create().texOffs(356, 2).addBox(-5.0F, -2.0F, -3.5F, 10.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.3531F, 5.4F, 0.5236F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(BlossomSnailEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float retract = entity.getRetractProgress(pt);
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * (1.0F - retract);

		// Gentle gastropod foot wave & shell rock
		float glide = ageInTicks * 0.12F;
		this.body.zScale *= (1.0F + Mth.sin(glide) * 0.035F * (0.4F + move) - 0.32F * retract);
		this.body.xScale *= (1.0F - Mth.sin(glide) * 0.025F * (0.4F + move) - 0.18F * retract);
		addBbDeg(this.shell, Mth.sin(glide * 0.8F) * 2.5F * (0.3F + move) + retract * 5.0F,
				0.0F, Mth.cos(glide * 0.7F) * 2.2F * move);
		addBbPos(this.shell, 0.0F, -retract * 1.8F, 0.0F);

		// Retract head, neck, and eyestalks cleanly into the blossom shell on hit
		float visScale = Math.max(0.08F, 1.0F - 0.88F * retract);
		this.neck.xScale *= visScale;
		this.neck.yScale *= visScale;
		this.neck.zScale *= visScale;
		addBbPos(this.neck, 0.0F, -retract * 2.5F, retract * 6.5F);
		addBbDeg(this.neck, retract * 24.0F + Mth.sin(glide) * 3.0F * (1.0F - retract),
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.35F * (1.0F - retract), 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -18.0F, 18.0F) * 0.35F * (1.0F - retract),
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.35F * (1.0F - retract), 0.0F);

		addBbDeg(this.eyestalk_l, Mth.sin(ageInTicks * 0.14F) * 8.0F * (1.0F - retract), 0.0F,
				-Mth.cos(ageInTicks * 0.11F) * 5.0F * (1.0F - retract));
		addBbDeg(this.eyestalk_r, Mth.cos(ageInTicks * 0.15F) * 8.0F * (1.0F - retract), 0.0F,
				Mth.sin(ageInTicks * 0.12F) * 5.0F * (1.0F - retract));
	}
}
