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
import ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/basalt_beetle.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class BasaltBeetleModel extends FaunaBaseModel<BasaltBeetleEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "basalt_beetle"), "main");

	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart mandible_l;
	private final ModelPart antenna_l;
	private final ModelPart mandible_r;
	private final ModelPart antenna_r;
	private final ModelPart leg_front_l;
	private final ModelPart leg_mid_l;
	private final ModelPart leg_back_l;
	private final ModelPart leg_front_r;
	private final ModelPart leg_mid_r;
	private final ModelPart leg_back_r;

	public BasaltBeetleModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.body.getChild("head");
		this.mandible_l = this.head.getChild("mandible_l");
		this.antenna_l = this.head.getChild("antenna_l");
		this.mandible_r = this.head.getChild("mandible_r");
		this.antenna_r = this.head.getChild("antenna_r");
		this.leg_front_l = this.root.getChild("leg_front_l");
		this.leg_mid_l = this.root.getChild("leg_mid_l");
		this.leg_back_l = this.root.getChild("leg_back_l");
		this.leg_front_r = this.root.getChild("leg_front_r");
		this.leg_mid_r = this.root.getChild("leg_mid_r");
		this.leg_back_r = this.root.getChild("leg_back_r");
		captureRestPoses(this.root, this.body, this.head, this.mandible_l, this.antenna_l, this.mandible_r, this.antenna_r, this.leg_front_l, this.leg_mid_l, this.leg_back_l, this.leg_front_r, this.leg_mid_r, this.leg_back_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-13.5F, -10.0F, -15.5F, 27.0F, 15.0F, 33.0F, new CubeDeformation(0.0F))
		.texOffs(374, 2).addBox(-10.5F, -14.0F, -12.0F, 21.0F, 6.0F, 26.0F, new CubeDeformation(0.0F))
		.texOffs(126, 2).addBox(-16.5F, -8.0F, -12.5F, 4.0F, 12.0F, 27.0F, new CubeDeformation(0.0F))
		.texOffs(192, 2).addBox(12.5F, -8.0F, -12.5F, 4.0F, 12.0F, 27.0F, new CubeDeformation(0.0F))
		.texOffs(258, 2).addBox(-11.5F, 4.5F, -17.0F, 23.0F, 3.0F, 33.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 2.0F));

		PartDefinition rear_pygidium_r_r1 = body.addOrReplaceChild("rear_pygidium_r_r1", CubeListBuilder.create().texOffs(2, 101).addBox(-4.0F, -4.5F, -2.0F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.6F, -2.2F, 21.2F, 0.3142F, 0.384F, 0.1396F));

		PartDefinition elytra_skirt_r_4_r1 = body.addOrReplaceChild("elytra_skirt_r_4_r1", CubeListBuilder.create().texOffs(134, 79).addBox(-2.0F, -5.0F, -3.5F, 4.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.4F, -1.0F, 17.0F, 0.1396F, 0.2094F, 0.1745F));

		PartDefinition elytra_skirt_r_3_r1 = body.addOrReplaceChild("elytra_skirt_r_3_r1", CubeListBuilder.create().texOffs(468, 54).addBox(-2.0F, -5.5F, -4.0F, 4.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.2F, -2.2F, 9.8F, 0.0698F, 0.0524F, 0.1745F));

		PartDefinition elytra_skirt_r_2_r1 = body.addOrReplaceChild("elytra_skirt_r_2_r1", CubeListBuilder.create().texOffs(80, 79).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.8F, -1.6F, 1.6F, 0.0F, 0.0F, 0.1745F));

		PartDefinition elytra_skirt_r_1_r1 = body.addOrReplaceChild("elytra_skirt_r_1_r1", CubeListBuilder.create().texOffs(440, 54).addBox(-2.0F, -5.5F, -4.0F, 4.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.2F, -2.0F, -6.8F, -0.0698F, -0.0524F, 0.1745F));

		PartDefinition elytra_mid_r_4_r1 = body.addOrReplaceChild("elytra_mid_r_4_r1", CubeListBuilder.create().texOffs(104, 118).addBox(-4.5F, -2.0F, -3.5F, 9.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.6F, -7.2F, 17.0F, 0.4887F, 0.1571F, 0.5934F));

		PartDefinition elytra_mid_r_3_r1 = body.addOrReplaceChild("elytra_mid_r_3_r1", CubeListBuilder.create().texOffs(418, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.4F, -10.4F, 9.8F, 0.2269F, 0.0698F, 0.6283F));

		PartDefinition elytra_mid_r_2_r1 = body.addOrReplaceChild("elytra_mid_r_2_r1", CubeListBuilder.create().texOffs(378, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.2F, -11.6F, 1.6F, -0.0349F, 0.0F, 0.6632F));

		PartDefinition elytra_mid_r_1_r1 = body.addOrReplaceChild("elytra_mid_r_1_r1", CubeListBuilder.create().texOffs(338, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.4F, -10.4F, -6.6F, -0.2443F, -0.0698F, 0.6283F));

		PartDefinition rear_pygidium_l_r1 = body.addOrReplaceChild("rear_pygidium_l_r1", CubeListBuilder.create().texOffs(476, 79).addBox(-4.0F, -4.5F, -2.0F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.6F, -2.2F, 21.2F, 0.3142F, -0.384F, -0.1396F));

		PartDefinition elytra_skirt_l_4_r1 = body.addOrReplaceChild("elytra_skirt_l_4_r1", CubeListBuilder.create().texOffs(108, 79).addBox(-2.0F, -5.0F, -3.5F, 4.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.4F, -1.0F, 17.0F, 0.1396F, -0.2094F, -0.1745F));

		PartDefinition elytra_skirt_l_3_r1 = body.addOrReplaceChild("elytra_skirt_l_3_r1", CubeListBuilder.create().texOffs(412, 54).addBox(-2.0F, -5.5F, -4.0F, 4.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.2F, -2.2F, 9.8F, 0.0698F, -0.0524F, -0.1745F));

		PartDefinition elytra_skirt_l_2_r1 = body.addOrReplaceChild("elytra_skirt_l_2_r1", CubeListBuilder.create().texOffs(52, 79).addBox(-2.0F, -5.0F, -4.0F, 4.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.8F, -1.6F, 1.6F, 0.0F, 0.0F, -0.1745F));

		PartDefinition elytra_skirt_l_1_r1 = body.addOrReplaceChild("elytra_skirt_l_1_r1", CubeListBuilder.create().texOffs(384, 54).addBox(-2.0F, -5.5F, -4.0F, 4.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.2F, -2.0F, -6.8F, -0.0698F, 0.0524F, -0.1745F));

		PartDefinition elytra_mid_l_4_r1 = body.addOrReplaceChild("elytra_mid_l_4_r1", CubeListBuilder.create().texOffs(68, 118).addBox(-4.5F, -2.0F, -3.5F, 9.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.6F, -7.2F, 17.0F, 0.4887F, -0.1571F, -0.5934F));

		PartDefinition elytra_mid_l_3_r1 = body.addOrReplaceChild("elytra_mid_l_3_r1", CubeListBuilder.create().texOffs(298, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.4F, -10.4F, 9.8F, 0.2269F, -0.0698F, -0.6283F));

		PartDefinition elytra_mid_l_2_r1 = body.addOrReplaceChild("elytra_mid_l_2_r1", CubeListBuilder.create().texOffs(258, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.2F, -11.6F, 1.6F, -0.0349F, 0.0F, -0.6632F));

		PartDefinition elytra_mid_l_1_r1 = body.addOrReplaceChild("elytra_mid_l_1_r1", CubeListBuilder.create().texOffs(218, 101).addBox(-5.0F, -2.0F, -4.0F, 10.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.4F, -10.4F, -6.6F, -0.2443F, 0.0698F, -0.6283F));

		PartDefinition rear_pygidium_center_r1 = body.addOrReplaceChild("rear_pygidium_center_r1", CubeListBuilder.create().texOffs(442, 79).addBox(-5.5F, -4.5F, -2.0F, 11.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.2F, 21.8F, 0.384F, 0.0F, 0.0F));

		PartDefinition elytra_dorsal_4_r1 = body.addOrReplaceChild("elytra_dorsal_4_r1", CubeListBuilder.create().texOffs(26, 118).addBox(-6.0F, -2.0F, -3.5F, 12.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.2F, 17.2F, 0.5585F, 0.0F, 0.0F));

		PartDefinition elytra_dorsal_3_r1 = body.addOrReplaceChild("elytra_dorsal_3_r1", CubeListBuilder.create().texOffs(128, 101).addBox(-7.0F, -2.0F, -4.0F, 14.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -13.8F, 9.8F, 0.2443F, 0.0F, 0.0F));

		PartDefinition elytra_dorsal_2_r1 = body.addOrReplaceChild("elytra_dorsal_2_r1", CubeListBuilder.create().texOffs(30, 101).addBox(-7.5F, -2.0F, -4.0F, 15.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.2F, 1.6F, -0.0349F, 0.0F, 0.0F));

		PartDefinition elytra_dorsal_1_r1 = body.addOrReplaceChild("elytra_dorsal_1_r1", CubeListBuilder.create().texOffs(80, 101).addBox(-7.0F, -2.0F, -4.0F, 14.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -13.8F, -6.6F, -0.2618F, 0.0F, 0.0F));

		PartDefinition pronotum_cheek_r_r1 = body.addOrReplaceChild("pronotum_cheek_r_r1", CubeListBuilder.create().texOffs(160, 54).addBox(-2.0F, -4.5F, -5.0F, 4.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.6F, -0.8F, -14.8F, -0.0873F, -0.0873F, 0.1745F));

		PartDefinition pronotum_shoulder_r_r1 = body.addOrReplaceChild("pronotum_shoulder_r_r1", CubeListBuilder.create().texOffs(256, 79).addBox(-4.5F, -2.0F, -5.0F, 9.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.6F, -7.0F, -14.6F, -0.3142F, -0.1047F, 0.6283F));

		PartDefinition pronotum_cheek_l_r1 = body.addOrReplaceChild("pronotum_cheek_l_r1", CubeListBuilder.create().texOffs(128, 54).addBox(-2.0F, -4.5F, -5.0F, 4.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.6F, -0.8F, -14.8F, -0.0873F, 0.0873F, -0.1745F));

		PartDefinition pronotum_shoulder_l_r1 = body.addOrReplaceChild("pronotum_shoulder_l_r1", CubeListBuilder.create().texOffs(214, 79).addBox(-4.5F, -2.0F, -5.0F, 9.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.6F, -7.0F, -14.6F, -0.3142F, 0.1047F, -0.6283F));

		PartDefinition pronotum_top_r1 = body.addOrReplaceChild("pronotum_top_r1", CubeListBuilder.create().texOffs(160, 79).addBox(-7.5F, -2.0F, -5.0F, 15.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.0F, -15.0F, -0.384F, 0.0F, 0.0F));

		PartDefinition magma_core_rear_r1 = body.addOrReplaceChild("magma_core_rear_r1", CubeListBuilder.create().texOffs(66, 54).addBox(-10.5F, -6.0F, -4.0F, 21.0F, 12.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 18.5F, 0.384F, 0.0F, 0.0F));

		PartDefinition magma_core_neck_r1 = body.addOrReplaceChild("magma_core_neck_r1", CubeListBuilder.create().texOffs(2, 54).addBox(-10.5F, -6.0F, -4.5F, 21.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -16.8F, -0.3142F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 79).addBox(-7.0F, -4.8F, -8.1F, 14.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(74, 147).addBox(-7.9F, -1.8F, -7.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(88, 147).addBox(5.9F, -1.8F, -7.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.5F, -20.2F));

		PartDefinition molten_horn_r_3_r1 = head.addOrReplaceChild("molten_horn_r_3_r1", CubeListBuilder.create().texOffs(230, 118).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.6F, -7.1F, -9.2F, 0.147F, 0.0F, -0.037F));

		PartDefinition molten_horn_r_2_r1 = head.addOrReplaceChild("molten_horn_r_2_r1", CubeListBuilder.create().texOffs(210, 118).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.8F, -3.3F, -6.4F, 0.5875F, 0.0F, 0.4424F));

		PartDefinition molten_horn_r_1_r1 = head.addOrReplaceChild("molten_horn_r_1_r1", CubeListBuilder.create().texOffs(2, 118).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.4F, -1.9F, -2.8F, 0.7748F, 0.0F, 1.1802F));

		PartDefinition brow_ridge_r_r1 = head.addOrReplaceChild("brow_ridge_r_r1", CubeListBuilder.create().texOffs(146, 134).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.4F, -2.9F, -4.8F, -0.1396F, -0.1396F, 0.2793F));

		PartDefinition molten_horn_l_3_r1 = head.addOrReplaceChild("molten_horn_l_3_r1", CubeListBuilder.create().texOffs(190, 118).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.6F, -7.1F, -9.2F, 0.147F, 0.0F, 0.037F));

		PartDefinition molten_horn_l_2_r1 = head.addOrReplaceChild("molten_horn_l_2_r1", CubeListBuilder.create().texOffs(170, 118).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.8F, -3.3F, -6.4F, 0.5875F, 0.0F, -0.4424F));

		PartDefinition molten_horn_l_1_r1 = head.addOrReplaceChild("molten_horn_l_1_r1", CubeListBuilder.create().texOffs(484, 101).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.4F, -1.9F, -2.8F, 0.7748F, 0.0F, -1.1802F));

		PartDefinition brow_ridge_l_r1 = head.addOrReplaceChild("brow_ridge_l_r1", CubeListBuilder.create().texOffs(122, 134).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.4F, -2.9F, -4.8F, -0.1396F, 0.1396F, -0.2793F));

		PartDefinition rhino_horn_3_r1 = head.addOrReplaceChild("rhino_horn_3_r1", CubeListBuilder.create().texOffs(170, 134).addBox(-2.0F, -4.2F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.7F, -16.8F, 0.5317F, 0.0F, 0.0F));

		PartDefinition rhino_horn_2_r1 = head.addOrReplaceChild("rhino_horn_2_r1", CubeListBuilder.create().texOffs(394, 118).addBox(-2.5F, -5.2F, -2.0F, 5.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.1F, -13.8F, 0.8789F, 0.0F, 0.0F));

		PartDefinition rhino_horn_1_r1 = head.addOrReplaceChild("rhino_horn_1_r1", CubeListBuilder.create().texOffs(458, 101).addBox(-3.0F, -6.0F, -2.5F, 6.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.1F, -9.8F, 1.2692F, 0.0F, 0.0F));

		PartDefinition snout_bridge_r1 = head.addOrReplaceChild("snout_bridge_r1", CubeListBuilder.create().texOffs(140, 118).addBox(-4.0F, -3.0F, -2.5F, 8.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.7F, -8.6F, -0.2094F, 0.0F, 0.0F));

		PartDefinition head_frons_shield_r1 = head.addOrReplaceChild("head_frons_shield_r1", CubeListBuilder.create().texOffs(176, 101).addBox(-5.5F, -2.0F, -4.0F, 11.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.5F, -3.0F, -0.4189F, 0.0F, 0.0F));

		PartDefinition mandible_l = head.addOrReplaceChild("mandible_l", CubeListBuilder.create(), PartPose.offset(-5.2F, 3.3F, -6.6F));

		PartDefinition mandible_tip_l_r1 = mandible_l.addOrReplaceChild("mandible_tip_l_r1", CubeListBuilder.create().texOffs(326, 134).addBox(-1.5F, -5.2F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, 1.0F, -4.0F, 1.6548F, -0.7109F, 0.0F));

		PartDefinition mandible_base_l_r1 = mandible_l.addOrReplaceChild("mandible_base_l_r1", CubeListBuilder.create().texOffs(416, 118).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.3F, 1.7731F, 0.2698F, 0.0F));

		PartDefinition antenna_l = head.addOrReplaceChild("antenna_l", CubeListBuilder.create(), PartPose.offset(-4.6F, -5.7F, -0.8F));

		PartDefinition antenna_seg_l_4_r1 = antenna_l.addOrReplaceChild("antenna_seg_l_4_r1", CubeListBuilder.create().texOffs(190, 134).addBox(-2.0F, -4.1F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.8F, -10.4F, -7.4F, 0.6976F, 0.0F, -0.4324F));

		PartDefinition antenna_seg_l_3_r1 = antenna_l.addOrReplaceChild("antenna_seg_l_3_r1", CubeListBuilder.create().texOffs(262, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.2F, -7.2F, -4.8F, 0.6284F, 0.0F, -0.4636F));

		PartDefinition antenna_seg_l_2_r1 = antenna_l.addOrReplaceChild("antenna_seg_l_2_r1", CubeListBuilder.create().texOffs(246, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, -3.6F, -2.2F, 0.5834F, 0.0F, -0.4182F));

		PartDefinition antenna_seg_l_1_r1 = antenna_l.addOrReplaceChild("antenna_seg_l_1_r1", CubeListBuilder.create().texOffs(230, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.4901F, 0.0F, -0.3985F));

		PartDefinition mandible_r = head.addOrReplaceChild("mandible_r", CubeListBuilder.create(), PartPose.offset(5.2F, 3.3F, -6.6F));

		PartDefinition mandible_tip_r_r1 = mandible_r.addOrReplaceChild("mandible_tip_r_r1", CubeListBuilder.create().texOffs(340, 134).addBox(-1.5F, -5.2F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, 1.0F, -4.0F, 1.6548F, 0.7109F, 0.0F));

		PartDefinition mandible_base_r_r1 = mandible_r.addOrReplaceChild("mandible_base_r_r1", CubeListBuilder.create().texOffs(432, 118).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.3F, 1.7731F, -0.2698F, 0.0F));

		PartDefinition antenna_r = head.addOrReplaceChild("antenna_r", CubeListBuilder.create(), PartPose.offset(4.6F, -5.7F, -0.8F));

		PartDefinition antenna_seg_r_4_r1 = antenna_r.addOrReplaceChild("antenna_seg_r_4_r1", CubeListBuilder.create().texOffs(210, 134).addBox(-2.0F, -4.1F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.8F, -10.4F, -7.4F, 0.6976F, 0.0F, 0.4324F));

		PartDefinition antenna_seg_r_3_r1 = antenna_r.addOrReplaceChild("antenna_seg_r_3_r1", CubeListBuilder.create().texOffs(310, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.2F, -7.2F, -4.8F, 0.6284F, 0.0F, 0.4636F));

		PartDefinition antenna_seg_r_2_r1 = antenna_r.addOrReplaceChild("antenna_seg_r_2_r1", CubeListBuilder.create().texOffs(294, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, -3.6F, -2.2F, 0.5834F, 0.0F, 0.4182F));

		PartDefinition antenna_seg_r_1_r1 = antenna_r.addOrReplaceChild("antenna_seg_r_1_r1", CubeListBuilder.create().texOffs(278, 134).addBox(-1.5F, -5.1F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, 0.0F, 0.4901F, 0.0F, 0.3985F));

		PartDefinition leg_front_l = root.addOrReplaceChild("leg_front_l", CubeListBuilder.create().texOffs(250, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(448, 118).addBox(-12.9F, 10.2F, -10.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(354, 134).addBox(-9.0F, 10.2F, -11.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(372, 134).addBox(-12.8F, 10.2F, -11.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-12.0F, -13.2F, -11.8F));

		PartDefinition leg_front_l_tibia_r1 = leg_front_l.addOrReplaceChild("leg_front_l_tibia_r1", CubeListBuilder.create().texOffs(192, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 1.8F, -3.7F, 0.3334F, 0.0F, -2.7767F));

		PartDefinition leg_front_l_femur_r1 = leg_front_l.addOrReplaceChild("leg_front_l_femur_r1", CubeListBuilder.create().texOffs(298, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.8208F, 1.0182F, 0.0F));

		PartDefinition leg_mid_l = root.addOrReplaceChild("leg_mid_l", CubeListBuilder.create().texOffs(274, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(478, 118).addBox(-13.3F, 10.2F, -3.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(390, 134).addBox(-9.4F, 10.2F, -4.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(408, 134).addBox(-13.2F, 10.2F, -4.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-12.8F, -13.2F, 1.5F));

		PartDefinition leg_mid_l_tibia_r1 = leg_mid_l.addOrReplaceChild("leg_mid_l_tibia_r1", CubeListBuilder.create().texOffs(224, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.6F, 1.8F, 0.0F, 0.0F, 0.0F, -2.7964F));

		PartDefinition leg_mid_l_femur_r1 = leg_mid_l.addOrReplaceChild("leg_mid_l_femur_r1", CubeListBuilder.create().texOffs(322, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.837F, 1.5708F, 0.0F));

		PartDefinition leg_back_l = root.addOrReplaceChild("leg_back_l", CubeListBuilder.create().texOffs(298, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 134).addBox(-13.3F, 10.2F, 4.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(426, 134).addBox(-9.4F, 10.2F, 3.2F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(444, 134).addBox(-13.2F, 10.2F, 3.2F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-12.0F, -13.2F, 13.8F));

		PartDefinition leg_back_l_tibia_r1 = leg_back_l.addOrReplaceChild("leg_back_l_tibia_r1", CubeListBuilder.create().texOffs(256, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.2F, 1.8F, 3.4F, -0.3588F, 0.0F, -2.7572F));

		PartDefinition leg_back_l_femur_r1 = leg_back_l.addOrReplaceChild("leg_back_l_femur_r1", CubeListBuilder.create().texOffs(346, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3215F, 1.0692F, 3.1416F));

		PartDefinition leg_front_r = root.addOrReplaceChild("leg_front_r", CubeListBuilder.create().texOffs(322, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(32, 134).addBox(5.9F, 10.2F, -10.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(462, 134).addBox(6.0F, 10.2F, -11.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(480, 134).addBox(9.8F, 10.2F, -11.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(12.0F, -13.2F, -11.8F));

		PartDefinition leg_front_r_tibia_r1 = leg_front_r.addOrReplaceChild("leg_front_r_tibia_r1", CubeListBuilder.create().texOffs(288, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 1.8F, -3.7F, 0.3334F, 0.0F, 2.7767F));

		PartDefinition leg_front_r_femur_r1 = leg_front_r.addOrReplaceChild("leg_front_r_femur_r1", CubeListBuilder.create().texOffs(370, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.8208F, -1.0182F, 0.0F));

		PartDefinition leg_mid_r = root.addOrReplaceChild("leg_mid_r", CubeListBuilder.create().texOffs(346, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(62, 134).addBox(6.3F, 10.2F, -3.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(2, 147).addBox(6.4F, 10.2F, -4.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(20, 147).addBox(10.2F, 10.2F, -4.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(12.8F, -13.2F, 1.5F));

		PartDefinition leg_mid_r_tibia_r1 = leg_mid_r.addOrReplaceChild("leg_mid_r_tibia_r1", CubeListBuilder.create().texOffs(320, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.6F, 1.8F, 0.0F, 0.0F, 0.0F, 2.7964F));

		PartDefinition leg_mid_r_femur_r1 = leg_mid_r.addOrReplaceChild("leg_mid_r_femur_r1", CubeListBuilder.create().texOffs(394, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.837F, -1.5708F, 0.0F));

		PartDefinition leg_back_r = root.addOrReplaceChild("leg_back_r", CubeListBuilder.create().texOffs(370, 118).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(92, 134).addBox(6.3F, 10.2F, 4.0F, 7.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(38, 147).addBox(6.4F, 10.2F, 3.2F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(56, 147).addBox(10.2F, 10.2F, 3.2F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(12.0F, -13.2F, 13.8F));

		PartDefinition leg_back_r_tibia_r1 = leg_back_r.addOrReplaceChild("leg_back_r_tibia_r1", CubeListBuilder.create().texOffs(352, 54).addBox(-3.5F, -10.6F, -3.5F, 7.0F, 12.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.2F, 1.8F, 3.4F, -0.3588F, 0.0F, 2.7572F));

		PartDefinition leg_back_r_femur_r1 = leg_back_r.addOrReplaceChild("leg_back_r_femur_r1", CubeListBuilder.create().texOffs(418, 79).addBox(-2.5F, -7.8F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3215F, -1.0692F, -3.1416F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(BasaltBeetleEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		float pt = ageInTicks - entity.tickCount;
		float move = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
		float ram = entity.getRamProgress(pt);
		float munch = entity.getMunchTicks() > 0 ? 1.0F : 0.0F;

		// Alternating insect tripod gait: (FL, MR, BL) vs (FR, ML, BR)
		float w = limbSwing * 0.95F;
		float sA = Mth.cos(w) * 24.0F * move;
		float sB = Mth.cos(w + (float) Math.PI) * 24.0F * move;
		float lA = Math.max(0.0F, Mth.sin(w)) * 16.0F * move;
		float lB = Math.max(0.0F, Mth.sin(w + (float) Math.PI)) * 16.0F * move;

		addBbDeg(this.leg_front_l, 0.0F, sA, -lA);
		addBbDeg(this.leg_mid_r, 0.0F, -sA, lA);
		addBbDeg(this.leg_back_l, 0.0F, sA, -lA);

		addBbDeg(this.leg_front_r, 0.0F, -sB, lB);
		addBbDeg(this.leg_mid_l, 0.0F, sB, -lB);
		addBbDeg(this.leg_back_r, 0.0F, -sB, lB);

		addBbDeg(this.body, Mth.sin(ageInTicks * 0.06F) * 0.8F - ram * 6.0F, 0.0F, Mth.cos(w) * 2.2F * move);

		// Rhino-horn head ram + coal munching mandibles + feeler antennae
		float chew = munch * Mth.sin(ageInTicks * 0.85F) * 14.0F;
		float idlePinch = Mth.sin(ageInTicks * 0.09F) * 3.5F;
		addBbDeg(this.head, -Mth.clamp(headPitch, -20.0F, 20.0F) * 0.35F + ram * 26.0F + munch * Mth.sin(ageInTicks * 0.45F) * 4.0F,
				Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
		addBbDeg(this.mandible_l, 0.0F, idlePinch + chew + ram * 12.0F, 0.0F);
		addBbDeg(this.mandible_r, 0.0F, -(idlePinch + chew + ram * 12.0F), 0.0F);
		addBbDeg(this.antenna_l, Mth.sin(ageInTicks * 0.16F) * 6.0F, Mth.cos(ageInTicks * 0.12F) * 5.0F, 0.0F);
		addBbDeg(this.antenna_r, Mth.cos(ageInTicks * 0.16F) * 6.0F, -Mth.sin(ageInTicks * 0.12F) * 5.0F, 0.0F);
	}
}
