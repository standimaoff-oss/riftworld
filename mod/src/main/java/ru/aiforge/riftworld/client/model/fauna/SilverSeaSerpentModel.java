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
import ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/silver_sea_serpent.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class SilverSeaSerpentModel extends FaunaBaseModel<SilverSeaSerpentEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "silver_sea_serpent"), "main");

	private final ModelPart serpent_root;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart body_arch;
	private final ModelPart tail_dip;
	private final ModelPart tail_tip;
	private final ModelPart hind_flipper_l;
	private final ModelPart hind_flipper_r;
	private final ModelPart flipper_l;
	private final ModelPart flipper_r;

	public SilverSeaSerpentModel(ModelPart root) {
		this.serpent_root = root.getChild("serpent_root");
		this.neck = this.serpent_root.getChild("neck");
		this.head = this.neck.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.body_arch = this.serpent_root.getChild("body_arch");
		this.tail_dip = this.body_arch.getChild("tail_dip");
		this.tail_tip = this.tail_dip.getChild("tail_tip");
		this.hind_flipper_l = this.tail_dip.getChild("hind_flipper_l");
		this.hind_flipper_r = this.tail_dip.getChild("hind_flipper_r");
		this.flipper_l = this.serpent_root.getChild("flipper_l");
		this.flipper_r = this.serpent_root.getChild("flipper_r");
		captureRestPoses(this.serpent_root, this.neck, this.head, this.jaw, this.body_arch, this.tail_dip, this.tail_tip, this.hind_flipper_l, this.hind_flipper_r, this.flipper_l, this.flipper_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition serpent_root = partdefinition.addOrReplaceChild("serpent_root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.8F, -5.0F));

		PartDefinition dorsal_sail_06_r1 = serpent_root.addOrReplaceChild("dorsal_sail_06_r1", CubeListBuilder.create().texOffs(286, 20).addBox(-0.5F, -4.3F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.9208F, 0.1396F, -2.0344F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_06_r1 = serpent_root.addOrReplaceChild("dorsal_scute_06_r1", CubeListBuilder.create().texOffs(434, 34).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.9174F, 2.3913F, 0.4636F, 0.0F, 0.0F));

		PartDefinition spine_seg_06_r1 = serpent_root.addOrReplaceChild("spine_seg_06_r1", CubeListBuilder.create().texOffs(30, 2).addBox(-3.5F, -4.85F, -3.5F, 7.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8F, 2.0F, -2.0344F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_05_r1 = serpent_root.addOrReplaceChild("dorsal_ray_05_r1", CubeListBuilder.create().texOffs(160, 34).addBox(-0.5F, -5.6F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.263F, 0.9119F, -1.5708F, -0.7285F, 1.5708F));

		PartDefinition dorsal_sail_05_r1 = serpent_root.addOrReplaceChild("dorsal_sail_05_r1", CubeListBuilder.create().texOffs(274, 20).addBox(-0.5F, -4.8F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.2082F, -0.4866F, -1.2998F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_05_r1 = serpent_root.addOrReplaceChild("dorsal_scute_05_r1", CubeListBuilder.create().texOffs(416, 34).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.7351F, 1.0431F, -0.2709F, 0.0F, 0.0F));

		PartDefinition spine_seg_05_r1 = serpent_root.addOrReplaceChild("spine_seg_05_r1", CubeListBuilder.create().texOffs(176, 2).addBox(-3.5F, -4.45F, -3.5F, 7.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, -1.6F, -1.2998F, 0.0F, 3.1416F));

		PartDefinition neck = serpent_root.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, -2.2F));

		PartDefinition dorsal_sail_04_r1 = neck.addOrReplaceChild("dorsal_sail_04_r1", CubeListBuilder.create().texOffs(262, 20).addBox(-0.5F, -4.3F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.3644F, 1.3514F, -0.6327F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_04_r1 = neck.addOrReplaceChild("dorsal_scute_04_r1", CubeListBuilder.create().texOffs(398, 34).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.2967F, 1.6773F, -0.938F, 0.0F, 0.0F));

		PartDefinition spine_seg_04_r1 = neck.addOrReplaceChild("spine_seg_04_r1", CubeListBuilder.create().texOffs(320, 2).addBox(-3.5F, -3.85F, -3.0F, 7.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.6F, -0.6327F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_03_r1 = neck.addOrReplaceChild("dorsal_ray_03_r1", CubeListBuilder.create().texOffs(402, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.1117F, -0.2694F, -1.5708F, 0.2074F, 1.5708F));

		PartDefinition dorsal_sail_03_r1 = neck.addOrReplaceChild("dorsal_sail_03_r1", CubeListBuilder.create().texOffs(78, 20).addBox(-0.5F, -5.3F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.8525F, 0.6127F, -0.364F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_03_r1 = neck.addOrReplaceChild("dorsal_scute_03_r1", CubeListBuilder.create().texOffs(2, 46).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.2612F, 0.1231F, -1.2068F, 0.0F, 0.0F));

		PartDefinition spine_seg_03_r1 = neck.addOrReplaceChild("spine_seg_03_r1", CubeListBuilder.create().texOffs(120, 2).addBox(-3.0F, -5.85F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.4F, -3.2F, -0.364F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_02_r1 = neck.addOrReplaceChild("dorsal_ray_02_r1", CubeListBuilder.create().texOffs(394, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.2468F, -1.8831F, -1.5708F, 0.1908F, 1.5708F));

		PartDefinition dorsal_sail_02_r1 = neck.addOrReplaceChild("dorsal_sail_02_r1", CubeListBuilder.create().texOffs(64, 20).addBox(-0.5F, -5.3F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.9153F, -1.0118F, -0.3805F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_02_r1 = neck.addOrReplaceChild("dorsal_scute_02_r1", CubeListBuilder.create().texOffs(486, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.4028F, -1.4931F, -1.1903F, 0.0F, 0.0F));

		PartDefinition spine_seg_02_r1 = neck.addOrReplaceChild("spine_seg_02_r1", CubeListBuilder.create().texOffs(92, 2).addBox(-3.0F, -5.85F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.4F, -4.8F, -0.3805F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_01_r1 = neck.addOrReplaceChild("dorsal_ray_01_r1", CubeListBuilder.create().texOffs(152, 34).addBox(-0.5F, -5.6F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.8839F, -4.4994F, -1.5708F, -0.1706F, 1.5708F));

		PartDefinition dorsal_sail_01_r1 = neck.addOrReplaceChild("dorsal_sail_01_r1", CubeListBuilder.create().texOffs(248, 20).addBox(-0.5F, -3.8F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.2191F, -4.361F, -0.7419F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_01_r1 = neck.addOrReplaceChild("dorsal_scute_01_r1", CubeListBuilder.create().texOffs(470, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -13.1204F, -4.2414F, -0.8288F, 0.0F, 0.0F));

		PartDefinition spine_seg_01_r1 = neck.addOrReplaceChild("spine_seg_01_r1", CubeListBuilder.create().texOffs(486, 2).addBox(-3.0F, -4.45F, -2.5F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.8F, -7.0F, -0.7419F, 0.0F, 3.1416F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(288, 2).addBox(-3.5F, -2.4F, -6.1F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(134, 20).addBox(-2.5F, -1.0F, -11.3F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(66, 46).addBox(-1.5F, -0.6F, -12.6F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(66, 34).addBox(-2.0F, 1.2F, -10.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(164, 46).addBox(0.4F, -1.3F, -11.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(370, 34).addBox(2.0F, -1.2F, -5.7F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(188, 46).addBox(1.2F, 1.2F, -10.3F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 46).addBox(1.3F, 1.1F, -7.8F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(176, 46).addBox(-2.4F, -1.3F, -11.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(384, 34).addBox(-4.0F, -1.2F, -5.7F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(212, 46).addBox(-2.2F, 1.2F, -10.3F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(220, 46).addBox(-2.3F, 1.1F, -7.8F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, -7.0F));

		PartDefinition barbel_r_tip_r1 = head.addOrReplaceChild("barbel_r_tip_r1", CubeListBuilder.create().texOffs(318, 34).addBox(-0.5F, -4.4F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.8F, 4.8F, -7.0F, -0.7721F, 0.0F, -2.9124F));

		PartDefinition barbel_r_base_r1 = head.addOrReplaceChild("barbel_r_base_r1", CubeListBuilder.create().texOffs(310, 34).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.9F, 1.4F, -9.0F, -0.517F, 0.0F, -2.8828F));

		PartDefinition gill_ray_lower_r_r1 = head.addOrReplaceChild("gill_ray_lower_r_r1", CubeListBuilder.create().texOffs(144, 34).addBox(-0.5F, -5.4F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.3F, 1.6F, -0.6F, -1.372F, 0.4817F, -3.1416F));

		PartDefinition gill_ray_mid_r_r1 = head.addOrReplaceChild("gill_ray_mid_r_r1", CubeListBuilder.create().texOffs(386, 20).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 0.6F, -0.6F, -1.6227F, 0.5256F, -3.1416F));

		PartDefinition gill_ray_upper_r_r1 = head.addOrReplaceChild("gill_ray_upper_r_r1", CubeListBuilder.create().texOffs(378, 20).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, -0.4F, -0.6F, -1.8893F, 0.4964F, -3.1416F));

		PartDefinition gill_web_r_r1 = head.addOrReplaceChild("gill_web_r_r1", CubeListBuilder.create().texOffs(348, 34).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 0.4F, 1.4F, 1.2789F, -1.0565F, -2.8093F));

		PartDefinition horn_r_tip_r1 = head.addOrReplaceChild("horn_r_tip_r1", CubeListBuilder.create().texOffs(370, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -4.6F, 2.2F, -2.118F, 0.2253F, -3.1416F));

		PartDefinition horn_r_base_r1 = head.addOrReplaceChild("horn_r_base_r1", CubeListBuilder.create().texOffs(220, 20).addBox(-1.0F, -6.2F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -1.8F, -2.0F, -2.1347F, 0.3217F, -3.1416F));

		PartDefinition brow_ridge_r_r1 = head.addOrReplaceChild("brow_ridge_r_r1", CubeListBuilder.create().texOffs(284, 34).addBox(-1.0F, -4.6F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -1.4F, -5.8F, -1.7444F, 0.2663F, -3.1416F));

		PartDefinition cheek_armor_r_r1 = head.addOrReplaceChild("cheek_armor_r_r1", CubeListBuilder.create().texOffs(258, 34).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.3F, 1.2F, -1.4F, 0.0F, -0.1396F, 0.0F));

		PartDefinition barbel_l_tip_r1 = head.addOrReplaceChild("barbel_l_tip_r1", CubeListBuilder.create().texOffs(302, 34).addBox(-0.5F, -4.4F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8F, 4.8F, -7.0F, -0.7721F, 0.0F, 2.9124F));

		PartDefinition barbel_l_base_r1 = head.addOrReplaceChild("barbel_l_base_r1", CubeListBuilder.create().texOffs(294, 34).addBox(-0.5F, -4.2F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.9F, 1.4F, -9.0F, -0.517F, 0.0F, 2.8828F));

		PartDefinition gill_ray_lower_l_r1 = head.addOrReplaceChild("gill_ray_lower_l_r1", CubeListBuilder.create().texOffs(136, 34).addBox(-0.5F, -5.4F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.3F, 1.6F, -0.6F, -1.372F, -0.4817F, 3.1416F));

		PartDefinition gill_ray_mid_l_r1 = head.addOrReplaceChild("gill_ray_mid_l_r1", CubeListBuilder.create().texOffs(362, 20).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 0.6F, -0.6F, -1.6227F, -0.5256F, 3.1416F));

		PartDefinition gill_ray_upper_l_r1 = head.addOrReplaceChild("gill_ray_upper_l_r1", CubeListBuilder.create().texOffs(354, 20).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4F, -0.4F, -0.6F, -1.8893F, -0.4964F, 3.1416F));

		PartDefinition gill_web_l_r1 = head.addOrReplaceChild("gill_web_l_r1", CubeListBuilder.create().texOffs(326, 34).addBox(-2.5F, -0.5F, -2.0F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 0.4F, 1.4F, 1.2789F, 1.0565F, 2.8093F));

		PartDefinition horn_l_tip_r1 = head.addOrReplaceChild("horn_l_tip_r1", CubeListBuilder.create().texOffs(346, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -4.6F, 2.2F, -2.118F, -0.2253F, 3.1416F));

		PartDefinition horn_l_base_r1 = head.addOrReplaceChild("horn_l_base_r1", CubeListBuilder.create().texOffs(208, 20).addBox(-1.0F, -6.2F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -1.8F, -2.0F, -2.1347F, -0.3217F, 3.1416F));

		PartDefinition brow_ridge_l_r1 = head.addOrReplaceChild("brow_ridge_l_r1", CubeListBuilder.create().texOffs(274, 34).addBox(-1.0F, -4.6F, -0.5F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -1.4F, -5.8F, -1.7444F, -0.2663F, 3.1416F));

		PartDefinition cheek_armor_l_r1 = head.addOrReplaceChild("cheek_armor_l_r1", CubeListBuilder.create().texOffs(242, 34).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.3F, 1.2F, -1.4F, 0.0F, 0.1396F, 0.0F));

		PartDefinition head_nasal_bridge_r1 = head.addOrReplaceChild("head_nasal_bridge_r1", CubeListBuilder.create().texOffs(50, 20).addBox(-1.5F, -7.4F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -3.6F, 1.7955F, 0.0F, 0.0F));

		PartDefinition head_occiput_crest_r1 = head.addOrReplaceChild("head_occiput_crest_r1", CubeListBuilder.create().texOffs(176, 34).addBox(-2.5F, -1.0F, -2.0F, 5.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.2F, -0.8F, -0.2443F, 0.0F, 0.0F));

		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(204, 46).addBox(0.8F, -0.2F, -6.4F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(228, 46).addBox(-1.8F, -0.2F, -6.4F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, -3.3F));

		PartDefinition jaw_chin_keel_r1 = jaw.addOrReplaceChild("jaw_chin_keel_r1", CubeListBuilder.create().texOffs(126, 34).addBox(-1.0F, -5.6F, -0.5F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.2F, -0.5F, 1.7793F, 0.0F, 0.0F));

		PartDefinition jaw_mandible_r1 = jaw.addOrReplaceChild("jaw_mandible_r1", CubeListBuilder.create().texOffs(418, 2).addBox(-2.0F, -8.4F, -1.0F, 4.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.3F, 1.7578F, 0.0F, 0.0F));

		PartDefinition body_arch = serpent_root.addOrReplaceChild("body_arch", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 5.4F));

		PartDefinition dorsal_ray_11_r1 = body_arch.addOrReplaceChild("dorsal_ray_11_r1", CubeListBuilder.create().texOffs(240, 20).addBox(-0.5F, -7.6F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.9247F, 16.1571F, -1.5708F, -0.2765F, 1.5708F));

		PartDefinition dorsal_sail_11_r1 = body_arch.addOrReplaceChild("dorsal_sail_11_r1", CubeListBuilder.create().texOffs(18, 20).addBox(-0.5F, -4.3F, -2.5F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.9994F, 15.8465F, -0.8478F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_11_r1 = body_arch.addOrReplaceChild("dorsal_scute_11_r1", CubeListBuilder.create().texOffs(80, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.1871F, 16.3886F, -0.723F, 0.0F, 0.0F));

		PartDefinition spine_seg_11_r1 = body_arch.addOrReplaceChild("spine_seg_11_r1", CubeListBuilder.create().texOffs(350, 2).addBox(-2.5F, -4.85F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.0F, 13.2F, -0.8478F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_10_r1 = body_arch.addOrReplaceChild("dorsal_ray_10_r1", CubeListBuilder.create().texOffs(426, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.1765F, 11.2735F, -1.5708F, -1.055F, 1.5708F));

		PartDefinition dorsal_sail_10_r1 = body_arch.addOrReplaceChild("dorsal_sail_10_r1", CubeListBuilder.create().texOffs(2, 20).addBox(-0.5F, -4.3F, -2.5F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -16.2931F, 9.3504F, -1.6263F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_10_r1 = body_arch.addOrReplaceChild("dorsal_scute_10_r1", CubeListBuilder.create().texOffs(50, 46).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.5958F, 11.2502F, 0.0555F, 0.0F, 0.0F));

		PartDefinition spine_seg_10_r1 = body_arch.addOrReplaceChild("spine_seg_10_r1", CubeListBuilder.create().texOffs(208, 2).addBox(-3.0F, -4.85F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.8F, 9.6F, -1.6263F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_09_r1 = body_arch.addOrReplaceChild("dorsal_ray_09_r1", CubeListBuilder.create().texOffs(232, 20).addBox(-0.5F, -7.6F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.8122F, 6.3878F, 1.5708F, -1.3567F, -1.5708F));

		PartDefinition dorsal_sail_09_r1 = body_arch.addOrReplaceChild("dorsal_sail_09_r1", CubeListBuilder.create().texOffs(434, 2).addBox(-0.5F, -5.3F, -2.5F, 1.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -11.782F, 3.218F, -2.3562F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_09_r1 = body_arch.addOrReplaceChild("dorsal_scute_09_r1", CubeListBuilder.create().texOffs(34, 46).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.1092F, 6.0908F, 0.7854F, 0.0F, 0.0F));

		PartDefinition spine_seg_09_r1 = body_arch.addOrReplaceChild("spine_seg_09_r1", CubeListBuilder.create().texOffs(148, 2).addBox(-3.0F, -5.85F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.6F, 6.4F, -2.3562F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_08_r1 = body_arch.addOrReplaceChild("dorsal_ray_08_r1", CubeListBuilder.create().texOffs(418, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.5455F, 2.9902F, 1.5708F, -1.1492F, -1.5708F));

		PartDefinition dorsal_sail_08_r1 = body_arch.addOrReplaceChild("dorsal_sail_08_r1", CubeListBuilder.create().texOffs(260, 2).addBox(-0.5F, -6.3F, -2.5F, 1.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.4582F, -0.3692F, -2.5637F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_08_r1 = body_arch.addOrReplaceChild("dorsal_scute_08_r1", CubeListBuilder.create().texOffs(18, 46).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.7749F, 2.6385F, 0.9929F, 0.0F, 0.0F));

		PartDefinition spine_seg_08_r1 = body_arch.addOrReplaceChild("spine_seg_08_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-3.0F, -6.85F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 3.4F, -2.5637F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_07_r1 = body_arch.addOrReplaceChild("dorsal_ray_07_r1", CubeListBuilder.create().texOffs(410, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.4243F, 0.0196F, 1.5708F, -1.2461F, -1.5708F));

		PartDefinition dorsal_sail_07_r1 = body_arch.addOrReplaceChild("dorsal_sail_07_r1", CubeListBuilder.create().texOffs(92, 20).addBox(-0.5F, -5.8F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.5488F, -2.9859F, -2.4669F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_07_r1 = body_arch.addOrReplaceChild("dorsal_scute_07_r1", CubeListBuilder.create().texOffs(452, 34).addBox(-2.0F, -0.5F, -1.5F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.6867F, -0.3083F, 0.8961F, 0.0F, 0.0F));

		PartDefinition spine_seg_07_r1 = body_arch.addOrReplaceChild("spine_seg_07_r1", CubeListBuilder.create().texOffs(62, 2).addBox(-3.5F, -6.45F, -3.0F, 7.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.2F, -2.4669F, 0.0F, 3.1416F));

		PartDefinition tail_dip = body_arch.addOrReplaceChild("tail_dip", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 16.6F));

		PartDefinition dorsal_sail_13_r1 = tail_dip.addOrReplaceChild("dorsal_sail_13_r1", CubeListBuilder.create().texOffs(298, 20).addBox(-0.5F, -4.3F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.7607F, 5.0088F, -0.8819F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_13_r1 = tail_dip.addOrReplaceChild("dorsal_scute_13_r1", CubeListBuilder.create().texOffs(108, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.8632F, 6.1303F, -0.6889F, 0.0F, 0.0F));

		PartDefinition spine_seg_13_r1 = tail_dip.addOrReplaceChild("spine_seg_13_r1", CubeListBuilder.create().texOffs(374, 2).addBox(-2.5F, -4.85F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.2F, 3.0F, -0.8819F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_12_r1 = tail_dip.addOrReplaceChild("dorsal_ray_12_r1", CubeListBuilder.create().texOffs(434, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.9956F, 3.0461F, -1.5708F, -0.0489F, 1.5708F));

		PartDefinition dorsal_sail_12_r1 = tail_dip.addOrReplaceChild("dorsal_sail_12_r1", CubeListBuilder.create().texOffs(106, 20).addBox(-0.5F, -5.8F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0808F, 2.9132F, -0.6202F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_12_r1 = tail_dip.addOrReplaceChild("dorsal_scute_12_r1", CubeListBuilder.create().texOffs(94, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.7922F, 3.3309F, -0.9505F, 0.0F, 0.0F));

		PartDefinition spine_seg_12_r1 = tail_dip.addOrReplaceChild("spine_seg_12_r1", CubeListBuilder.create().texOffs(236, 2).addBox(-2.5F, -6.45F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6202F, 0.0F, 3.1416F));

		PartDefinition tail_tip = tail_dip.addOrReplaceChild("tail_tip", CubeListBuilder.create(), PartPose.offset(0.0F, 7.0F, 6.2F));

		PartDefinition tail_fan_lobe_lower_r1 = tail_tip.addOrReplaceChild("tail_fan_lobe_lower_r1", CubeListBuilder.create().texOffs(464, 2).addBox(-0.5F, -7.2F, -1.5F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.0F, 10.0F, -2.0344F, 0.0F, 3.1416F));

		PartDefinition tail_fan_lobe_upper_r1 = tail_tip.addOrReplaceChild("tail_fan_lobe_upper_r1", CubeListBuilder.create().texOffs(276, 2).addBox(-0.5F, -8.2F, -1.5F, 1.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, 9.6F, -2.5536F, 0.0F, 3.1416F));

		PartDefinition tail_fan_spine_center_r1 = tail_tip.addOrReplaceChild("tail_fan_spine_center_r1", CubeListBuilder.create().texOffs(476, 2).addBox(-0.5F, -8.2F, -1.0F, 1.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.4F, 9.8F, -2.3373F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_16_r1 = tail_tip.addOrReplaceChild("dorsal_ray_16_r1", CubeListBuilder.create().texOffs(450, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.7122F, 7.9098F, 1.5708F, -1.2461F, -1.5708F));

		PartDefinition dorsal_sail_16_r1 = tail_tip.addOrReplaceChild("dorsal_sail_16_r1", CubeListBuilder.create().texOffs(450, 2).addBox(-0.5F, -6.3F, -2.0F, 1.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.6117F, 5.1854F, -2.4669F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_16_r1 = tail_tip.addOrReplaceChild("dorsal_scute_16_r1", CubeListBuilder.create().texOffs(150, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.8433F, 7.7458F, 0.8961F, 0.0F, 0.0F));

		PartDefinition spine_seg_16_r1 = tail_tip.addOrReplaceChild("spine_seg_16_r1", CubeListBuilder.create().texOffs(34, 20).addBox(-1.5F, -5.85F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 7.2F, -2.4669F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_15_r1 = tail_tip.addOrReplaceChild("dorsal_ray_15_r1", CubeListBuilder.create().texOffs(442, 20).addBox(-0.5F, -6.6F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.5069F, 4.4582F, 1.5708F, -1.387F, -1.5708F));

		PartDefinition dorsal_sail_15_r1 = tail_tip.addOrReplaceChild("dorsal_sail_15_r1", CubeListBuilder.create().texOffs(120, 20).addBox(-0.5F, -5.3F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0429F, 1.6891F, -2.3259F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_15_r1 = tail_tip.addOrReplaceChild("dorsal_scute_15_r1", CubeListBuilder.create().texOffs(136, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.7108F, 4.2663F, 0.7551F, 0.0F, 0.0F));

		PartDefinition spine_seg_15_r1 = tail_tip.addOrReplaceChild("spine_seg_15_r1", CubeListBuilder.create().texOffs(398, 2).addBox(-2.0F, -5.85F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.8F, 3.8F, -2.3259F, 0.0F, 3.1416F));

		PartDefinition dorsal_ray_14_r1 = tail_tip.addOrReplaceChild("dorsal_ray_14_r1", CubeListBuilder.create().texOffs(168, 34).addBox(-0.5F, -5.6F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.8838F, 1.6703F, -1.5708F, -1.2181F, 1.5708F));

		PartDefinition dorsal_sail_14_r1 = tail_tip.addOrReplaceChild("dorsal_sail_14_r1", CubeListBuilder.create().texOffs(310, 20).addBox(-0.5F, -4.8F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.5967F, -0.377F, -1.7895F, 0.0F, 3.1416F));

		PartDefinition dorsal_scute_14_r1 = tail_tip.addOrReplaceChild("dorsal_scute_14_r1", CubeListBuilder.create().texOffs(122, 46).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.1571F, 1.6095F, 0.2187F, 0.0F, 0.0F));

		PartDefinition spine_seg_14_r1 = tail_tip.addOrReplaceChild("spine_seg_14_r1", CubeListBuilder.create().texOffs(160, 20).addBox(-2.0F, -4.45F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.2F, -1.7895F, 0.0F, 3.1416F));

		PartDefinition hind_flipper_l = tail_dip.addOrReplaceChild("hind_flipper_l", CubeListBuilder.create(), PartPose.offset(2.4F, 5.6F, 3.8F));

		PartDefinition hind_flipper_l_ray_r1 = hind_flipper_l.addOrReplaceChild("hind_flipper_l_ray_r1", CubeListBuilder.create().texOffs(482, 20).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.2F, 1.8F, 2.0F, -1.215F, -0.7328F, 3.1416F));

		PartDefinition hind_flipper_l_web_r1 = hind_flipper_l.addOrReplaceChild("hind_flipper_l_web_r1", CubeListBuilder.create().texOffs(198, 34).addBox(-2.0F, -0.5F, -2.5F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4F, 2.8F, 4.0F, -2.8644F, 0.63F, -2.7255F));

		PartDefinition hind_flipper_l_arm_r1 = hind_flipper_l.addOrReplaceChild("hind_flipper_l_arm_r1", CubeListBuilder.create().texOffs(322, 20).addBox(-1.0F, -5.4F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, 0.0F, -0.2F, -1.0701F, -0.8176F, 3.1416F));

		PartDefinition hind_flipper_r = tail_dip.addOrReplaceChild("hind_flipper_r", CubeListBuilder.create(), PartPose.offset(-2.4F, 5.6F, 3.8F));

		PartDefinition hind_flipper_r_ray_r1 = hind_flipper_r.addOrReplaceChild("hind_flipper_r_ray_r1", CubeListBuilder.create().texOffs(2, 34).addBox(-0.5F, -6.4F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2F, 1.8F, 2.0F, -1.215F, 0.7328F, -3.1416F));

		PartDefinition hind_flipper_r_web_r1 = hind_flipper_r.addOrReplaceChild("hind_flipper_r_web_r1", CubeListBuilder.create().texOffs(220, 34).addBox(-2.0F, -0.5F, -2.5F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, 2.8F, 4.0F, -2.8644F, -0.63F, 2.7255F));

		PartDefinition hind_flipper_r_arm_r1 = hind_flipper_r.addOrReplaceChild("hind_flipper_r_arm_r1", CubeListBuilder.create().texOffs(334, 20).addBox(-1.0F, -5.4F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, 0.0F, -0.2F, -1.0701F, 0.8176F, -3.1416F));

		PartDefinition flipper_l = serpent_root.addOrReplaceChild("flipper_l", CubeListBuilder.create().texOffs(90, 34).addBox(-0.9F, -1.2F, -2.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(3.4F, 0.7F, 0.6F));

		PartDefinition flipper_l_ray_3_r1 = flipper_l.addOrReplaceChild("flipper_l_ray_3_r1", CubeListBuilder.create().texOffs(474, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6F, 2.7F, 3.0F, -1.3896F, -0.4964F, 3.1416F));

		PartDefinition flipper_l_ray_2_r1 = flipper_l.addOrReplaceChild("flipper_l_ray_2_r1", CubeListBuilder.create().texOffs(466, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.4F, 2.7F, 2.4F, -1.3503F, -0.761F, 3.1416F));

		PartDefinition flipper_l_ray_1_r1 = flipper_l.addOrReplaceChild("flipper_l_ray_1_r1", CubeListBuilder.create().texOffs(458, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, 2.5F, 1.6F, -1.3678F, -1.0304F, 3.1416F));

		PartDefinition flipper_l_web_r1 = flipper_l.addOrReplaceChild("flipper_l_web_r1", CubeListBuilder.create().texOffs(10, 34).addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.4F, 3.3F, 4.0F, -2.9714F, 0.5922F, -2.8625F));

		PartDefinition flipper_l_arm_r1 = flipper_l.addOrReplaceChild("flipper_l_arm_r1", CubeListBuilder.create().texOffs(180, 20).addBox(-1.5F, -6.2F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, 0.5F, -0.2F, -1.0884F, -0.9716F, 3.1416F));

		PartDefinition flipper_r = serpent_root.addOrReplaceChild("flipper_r", CubeListBuilder.create().texOffs(108, 34).addBox(-2.1F, -1.2F, -2.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.4F, 0.7F, 0.6F));

		PartDefinition flipper_r_ray_3_r1 = flipper_r.addOrReplaceChild("flipper_r_ray_3_r1", CubeListBuilder.create().texOffs(506, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, 2.7F, 3.0F, -1.3896F, 0.4964F, -3.1416F));

		PartDefinition flipper_r_ray_2_r1 = flipper_r.addOrReplaceChild("flipper_r_ray_2_r1", CubeListBuilder.create().texOffs(498, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.4F, 2.7F, 2.4F, -1.3503F, 0.761F, -3.1416F));

		PartDefinition flipper_r_ray_1_r1 = flipper_r.addOrReplaceChild("flipper_r_ray_1_r1", CubeListBuilder.create().texOffs(490, 20).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, 2.5F, 1.6F, -1.3678F, 1.0304F, -3.1416F));

		PartDefinition flipper_r_web_r1 = flipper_r.addOrReplaceChild("flipper_r_web_r1", CubeListBuilder.create().texOffs(38, 34).addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.4F, 3.3F, 4.0F, -2.9714F, -0.5922F, 2.8625F));

		PartDefinition flipper_r_arm_r1 = flipper_r.addOrReplaceChild("flipper_r_arm_r1", CubeListBuilder.create().texOffs(194, 20).addBox(-1.5F, -6.2F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, 0.5F, -0.2F, -1.0884F, 0.9716F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.serpent_root;
	}

	@Override
	public void setupAnim(SilverSeaSerpentEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float speed = (float) entity.getDeltaMovement().horizontalDistance();
		boolean biting = entity.getBiteTicks() > 0;
		boolean slapping = entity.getTailSlapTicks() > 0;
		boolean retreating = entity.getRetreatTicks() > 0;

		float effort = Mth.clamp(0.45F + speed * 3.2F + (retreating ? 0.55F : 0.0F), 0.45F, 1.65F);
		float wave = (ageInTicks + entity.getId() * 1.9F) * (0.16F + 0.08F * effort);

		// Travelling pelagic S-wave from neck through tail fluke
		float slapBoost = slapping ? Mth.sin(ageInTicks * 0.95F) * 34.0F : 0.0F;
		addBbDeg(this.serpent_root, Mth.sin(wave * 0.5F) * 2.5F, Mth.sin(wave) * 6.0F * effort, Mth.cos(wave * 0.5F) * 3.5F);
		addBbDeg(this.neck, Mth.sin(wave + 0.5F) * 3.0F - (biting ? 10.0F : 0.0F),
				-Mth.sin(wave - 0.4F) * 7.5F * effort + Mth.clamp(netHeadYaw, -30.0F, 30.0F) * 0.35F, 0.0F);
		addBbDeg(this.head, -Mth.clamp(headPitch, -22.0F, 22.0F) * 0.45F - (biting ? 8.0F : 0.0F),
				-Mth.sin(wave + 0.3F) * 5.0F + Mth.clamp(netHeadYaw, -30.0F, 30.0F) * 0.4F, 0.0F);
		addBbDeg(this.jaw, biting ? (28.0F + Mth.sin(ageInTicks * 0.8F) * 6.0F) : (2.5F + Mth.sin(ageInTicks * 0.07F) * 1.8F), 0.0F, 0.0F);

		addBbDeg(this.body_arch, Mth.sin(wave - 0.9F) * 3.0F, Mth.sin(wave - 0.95F) * 12.0F * effort + slapBoost * 0.4F, 0.0F);
		addBbDeg(this.tail_dip, Mth.sin(wave - 1.8F) * 4.0F, Mth.sin(wave - 1.90F) * 18.0F * effort + slapBoost * 0.75F, 0.0F);
		addBbDeg(this.tail_tip, Mth.sin(wave - 2.6F) * 5.0F, Mth.sin(wave - 2.85F) * 25.0F * effort + slapBoost, 0.0F);

		// 4-flipper hydrofoil rowing: fore-flippers lead, hind-flippers follow with phase lag
		float foreLift = Mth.sin(wave) * 24.0F * effort;
		float hindLift = Mth.sin(wave - 1.6F) * 19.0F * effort;
		addBbDeg(this.flipper_l, Mth.cos(wave) * 9.0F, 0.0F, -(12.0F + foreLift));
		addBbDeg(this.flipper_r, Mth.cos(wave) * 9.0F, 0.0F, (12.0F + foreLift));
		addBbDeg(this.hind_flipper_l, Mth.cos(wave - 1.6F) * 7.0F, 0.0F, -(8.0F + hindLift));
		addBbDeg(this.hind_flipper_r, Mth.cos(wave - 1.6F) * 7.0F, 0.0F, (8.0F + hindLift));
	}
}
