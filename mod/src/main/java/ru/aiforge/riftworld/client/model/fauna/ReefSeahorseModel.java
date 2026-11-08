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
import ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/reef_seahorse.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class ReefSeahorseModel extends FaunaBaseModel<ReefSeahorseEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "reef_seahorse"), "main");

	private final ModelPart root;
	private final ModelPart trunk;
	private final ModelPart dorsal_fin;
	private final ModelPart head;
	private final ModelPart cheek_fin_l;
	private final ModelPart cheek_fin_r;
	private final ModelPart trunk_fin_l;
	private final ModelPart trunk_fin_r;
	private final ModelPart tail_curl;

	public ReefSeahorseModel(ModelPart root) {
		this.root = root.getChild("root");
		this.trunk = this.root.getChild("trunk");
		this.dorsal_fin = this.trunk.getChild("dorsal_fin");
		this.head = this.trunk.getChild("head");
		this.cheek_fin_l = this.head.getChild("cheek_fin_l");
		this.cheek_fin_r = this.head.getChild("cheek_fin_r");
		this.trunk_fin_l = this.trunk.getChild("trunk_fin_l");
		this.trunk_fin_r = this.trunk.getChild("trunk_fin_r");
		this.tail_curl = this.root.getChild("tail_curl");
		captureRestPoses(this.root, this.trunk, this.dorsal_fin, this.head, this.cheek_fin_l, this.cheek_fin_r, this.trunk_fin_l, this.trunk_fin_r, this.tail_curl);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition trunk = root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(170, 2).addBox(-2.5F, 1.2F, -1.7F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(36, 2).addBox(-3.0F, -4.7F, -2.8F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(194, 2).addBox(-2.5F, -8.0F, -2.3F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(372, 2).addBox(-2.0F, 1.0F, 2.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(260, 2).addBox(-2.5F, -4.5F, 2.0F, 5.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(330, 2).addBox(-2.0F, -8.7F, 1.5F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(20, 19).addBox(-2.0F, 1.3F, -3.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(278, 2).addBox(-2.0F, -4.5F, -4.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(404, 2).addBox(-1.5F, -7.8F, -3.2F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 0.0F));

		PartDefinition dorsal_ridge_waist_r1 = trunk.addOrReplaceChild("dorsal_ridge_waist_r1", CubeListBuilder.create().texOffs(48, 19).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.4F, 3.8F, -0.2793F, 0.0F, 0.0F));

		PartDefinition dorsal_ridge_nape_r1 = trunk.addOrReplaceChild("dorsal_ridge_nape_r1", CubeListBuilder.create().texOffs(36, 19).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.6F, 3.3F, 0.3142F, 0.0F, 0.0F));

		PartDefinition dorsal_fin = trunk.addOrReplaceChild("dorsal_fin", CubeListBuilder.create().texOffs(248, 2).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(64, 2).addBox(-0.5F, -5.2F, 1.5F, 1.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(92, 2).addBox(-0.5F, -4.2F, 3.5F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(346, 2).addBox(-0.5F, -2.7F, 5.7F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 3.5F));

		PartDefinition head = trunk.addOrReplaceChild("head", CubeListBuilder.create().texOffs(104, 2).addBox(-3.0F, -5.0F, -2.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 19).addBox(-2.0F, -6.3F, -1.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(128, 19).addBox(-1.0F, -7.7F, -0.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(60, 19).addBox(1.8F, -4.0F, -1.3F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(72, 19).addBox(-3.8F, -4.0F, -1.3F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.5F, -0.5F));

		PartDefinition antler_rear_r_tine_r1 = head.addOrReplaceChild("antler_rear_r_tine_r1", CubeListBuilder.create().texOffs(120, 19).addBox(-0.5F, -3.3F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1F, -5.7F, 3.9F, -0.0697F, 0.0F, -0.4324F));

		PartDefinition antler_rear_r_main_r1 = head.addOrReplaceChild("antler_rear_r_main_r1", CubeListBuilder.create().texOffs(364, 2).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -4.3F, 2.3F, -2.2265F, 0.3948F, -3.1416F));

		PartDefinition antler_upper_r_tine_r1 = head.addOrReplaceChild("antler_upper_r_tine_r1", CubeListBuilder.create().texOffs(112, 19).addBox(-0.5F, -3.3F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6F, -7.7F, 1.6F, 0.4625F, 0.0F, -0.3743F));

		PartDefinition antler_upper_r_main_r1 = head.addOrReplaceChild("antler_upper_r_main_r1", CubeListBuilder.create().texOffs(302, 2).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1F, -5.5F, 0.7F, -0.391F, 0.0F, -0.2268F));

		PartDefinition antler_rear_l_tine_r1 = head.addOrReplaceChild("antler_rear_l_tine_r1", CubeListBuilder.create().texOffs(104, 19).addBox(-0.5F, -3.3F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1F, -5.7F, 3.9F, -0.0697F, 0.0F, 0.4324F));

		PartDefinition antler_rear_l_main_r1 = head.addOrReplaceChild("antler_rear_l_main_r1", CubeListBuilder.create().texOffs(356, 2).addBox(-0.5F, -5.2F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -4.3F, 2.3F, -2.2265F, -0.3948F, 3.1416F));

		PartDefinition antler_upper_l_tine_r1 = head.addOrReplaceChild("antler_upper_l_tine_r1", CubeListBuilder.create().texOffs(96, 19).addBox(-0.5F, -3.3F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6F, -7.7F, 1.6F, 0.4625F, 0.0F, 0.3743F));

		PartDefinition antler_upper_l_main_r1 = head.addOrReplaceChild("antler_upper_l_main_r1", CubeListBuilder.create().texOffs(294, 2).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1F, -5.5F, 0.7F, -0.391F, 0.0F, 0.2268F));

		PartDefinition snout_coral_tip_r1 = head.addOrReplaceChild("snout_coral_tip_r1", CubeListBuilder.create().texOffs(388, 2).addBox(-1.5F, -2.9F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.2F, -6.1F, 2.0152F, 0.0F, 0.0F));

		PartDefinition snout_tube_r1 = head.addOrReplaceChild("snout_tube_r1", CubeListBuilder.create().texOffs(218, 2).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -1.5F, 2.0169F, 0.0F, 0.0F));

		PartDefinition cheek_fin_l = head.addOrReplaceChild("cheek_fin_l", CubeListBuilder.create(), PartPose.offset(2.8F, -2.0F, 2.5F));

		PartDefinition cheek_fin_l_blade_r1 = cheek_fin_l.addOrReplaceChild("cheek_fin_l_blade_r1", CubeListBuilder.create().texOffs(468, 2).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 0.2F, 1.2F, 1.7468F, 1.049F, -2.9393F));

		PartDefinition cheek_fin_r = head.addOrReplaceChild("cheek_fin_r", CubeListBuilder.create(), PartPose.offset(-2.8F, -2.0F, 2.5F));

		PartDefinition cheek_fin_r_blade_r1 = cheek_fin_r.addOrReplaceChild("cheek_fin_r_blade_r1", CubeListBuilder.create().texOffs(488, 2).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 0.2F, 1.2F, 1.7468F, -1.049F, 2.9393F));

		PartDefinition trunk_fin_l = trunk.addOrReplaceChild("trunk_fin_l", CubeListBuilder.create(), PartPose.offset(2.8F, -1.0F, 1.5F));

		PartDefinition trunk_fin_l_blade_r1 = trunk_fin_l.addOrReplaceChild("trunk_fin_l_blade_r1", CubeListBuilder.create().texOffs(428, 2).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.6F, 0.2F, 1.3F, 1.5708F, 1.1384F, 3.1416F));

		PartDefinition trunk_fin_r = trunk.addOrReplaceChild("trunk_fin_r", CubeListBuilder.create(), PartPose.offset(-2.8F, -1.0F, 1.5F));

		PartDefinition trunk_fin_r_blade_r1 = trunk_fin_r.addOrReplaceChild("trunk_fin_r_blade_r1", CubeListBuilder.create().texOffs(448, 2).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.6F, 0.2F, 1.3F, 1.5708F, -1.1384F, -3.1416F));

		PartDefinition tail_curl = root.addOrReplaceChild("tail_curl", CubeListBuilder.create().texOffs(76, 2).addBox(-2.0F, -0.5F, 1.0F, 4.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(156, 2).addBox(-1.5F, -0.3F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(2, 2).addBox(-2.0F, 8.5F, -8.0F, 4.0F, 2.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(130, 2).addBox(-1.5F, 7.0F, -6.7F, 3.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(234, 2).addBox(-1.5F, 1.5F, -8.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(418, 2).addBox(-1.0F, 2.5F, -6.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(310, 2).addBox(-1.5F, 1.5F, -6.8F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(84, 19).addBox(-1.0F, 3.0F, -3.7F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(140, 19).addBox(-1.0F, 4.3F, -4.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 3.5F, 2.5F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.root;
	}

	@Override
	public void setupAnim(ReefSeahorseEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		boolean anchored = entity.isAnchoredToCoral();
		float t = ageInTicks + entity.getId() * 3.1F;

		// Delicate hummingbird-fast dorsal & pectoral cheek-fin flutter
		float dorsalFlutter = Mth.sin(t * 0.85F) * (anchored ? 14.0F : 26.0F);
		float cheekFlutter = Mth.cos(t * 0.78F) * (anchored ? 12.0F : 22.0F);
		addBbDeg(this.dorsal_fin, 0.0F, dorsalFlutter, 0.0F);
		addBbDeg(this.cheek_fin_l, 0.0F, cheekFlutter, -8.0F);
		addBbDeg(this.cheek_fin_r, 0.0F, -cheekFlutter, 8.0F);
		addBbDeg(this.trunk_fin_l, 0.0F, Mth.sin(t * 0.45F) * 12.0F, 0.0F);
		addBbDeg(this.trunk_fin_r, 0.0F, -Mth.sin(t * 0.45F) * 12.0F, 0.0F);

		// Upright seahorse hover bob & prehensile tail curl
		addBbPos(this.trunk, 0.0F, Mth.sin(t * 0.09F) * (anchored ? 0.4F : 1.3F), 0.0F);
		addBbDeg(this.trunk, Mth.sin(t * 0.07F) * 4.5F, 0.0F, Mth.cos(t * 0.06F) * 2.5F);
		addBbDeg(this.head, 4.0F + Mth.sin(t * 0.08F) * 3.5F, Mth.clamp(netHeadYaw, -25.0F, 25.0F) * 0.4F, 0.0F);
		addBbDeg(this.tail_curl, (anchored ? 26.0F : 8.0F) + Mth.sin(t * 0.10F - 0.9F) * 10.0F, 0.0F, 0.0F);
	}
}
