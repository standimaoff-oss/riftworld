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
import ru.aiforge.riftworld.entity.fauna.GhostSquidEntity;

/**
 * Generated from {@code blockbench/riftworld_fauna/ghost_squid.bbmodel} via Blockbench MCP
 * ({@code bbmodel_export_modded_entity}, Mojmap 1.17+ / 1.20.1, {@code flip_y: true})
 * with bespoke multi-state Riftworld v0.47.0 biomechanical animations.
 */
public class GhostSquidModel extends FaunaBaseModel<GhostSquidEntity> {
	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(new ResourceLocation(RiftWorld.MODID, "ghost_squid"), "main");

	private final ModelPart mantle_root;
	private final ModelPart head_collar;
	private final ModelPart arms_upper;
	private final ModelPart arms_lower;
	private final ModelPart tentacle_long_l;
	private final ModelPart tentacle_long_r;
	private final ModelPart fin_l;
	private final ModelPart fin_r;

	public GhostSquidModel(ModelPart root) {
		this.mantle_root = root.getChild("mantle_root");
		this.head_collar = this.mantle_root.getChild("head_collar");
		this.arms_upper = this.head_collar.getChild("arms_upper");
		this.arms_lower = this.head_collar.getChild("arms_lower");
		this.tentacle_long_l = this.head_collar.getChild("tentacle_long_l");
		this.tentacle_long_r = this.head_collar.getChild("tentacle_long_r");
		this.fin_l = this.mantle_root.getChild("fin_l");
		this.fin_r = this.mantle_root.getChild("fin_r");
		captureRestPoses(this.mantle_root, this.head_collar, this.arms_upper, this.arms_lower, this.tentacle_long_l, this.tentacle_long_r, this.fin_l, this.fin_r);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition mantle_root = partdefinition.addOrReplaceChild("mantle_root", CubeListBuilder.create(), PartPose.offset(0.0F, 7.0F, -4.0F));

		PartDefinition mantle_cone_flank_r_r1 = mantle_root.addOrReplaceChild("mantle_cone_flank_r_r1", CubeListBuilder.create().texOffs(68, 2).addBox(-1.5F, -14.6F, -3.0F, 3.0F, 15.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, -8.3F, -19.2F, -1.2347F, 0.1756F, -3.1416F));

		PartDefinition mantle_cone_flank_l_r1 = mantle_root.addOrReplaceChild("mantle_cone_flank_l_r1", CubeListBuilder.create().texOffs(46, 2).addBox(-1.5F, -14.6F, -3.0F, 3.0F, 15.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, -8.3F, -19.2F, -1.2347F, -0.1756F, 3.1416F));

		PartDefinition mantle_cone_ventral_r1 = mantle_root.addOrReplaceChild("mantle_cone_ventral_r1", CubeListBuilder.create().texOffs(110, 2).addBox(-3.0F, -15.6F, -1.5F, 6.0F, 16.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.6F, -19.2F, -1.0943F, 0.0F, 3.1416F));

		PartDefinition mantle_cone_dorsal_r1 = mantle_root.addOrReplaceChild("mantle_cone_dorsal_r1", CubeListBuilder.create().texOffs(168, 2).addBox(-3.0F, -13.6F, -1.5F, 6.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.0F, -19.2F, -1.3796F, 0.0F, 3.1416F));

		PartDefinition mantle_cone_core_r1 = mantle_root.addOrReplaceChild("mantle_cone_core_r1", CubeListBuilder.create().texOffs(90, 2).addBox(-2.0F, -15.6F, -2.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.4F, -19.8F, -1.2178F, 0.0F, 3.1416F));

		PartDefinition mantle_main_box_r1 = mantle_root.addOrReplaceChild("mantle_main_box_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-5.0F, -15.2F, -5.0F, 10.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.8F, -7.2F, -1.222F, 0.0F, 3.1416F));

		PartDefinition head_collar = mantle_root.addOrReplaceChild("head_collar", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 6.0F));

		PartDefinition squid_eye_r_r1 = head_collar.addOrReplaceChild("squid_eye_r_r1", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0F, -4.6F, -2.5F, 2.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6F, -0.6F, 0.8F, -1.2243F, 0.0F, -3.1416F));

		PartDefinition squid_eye_l_r1 = head_collar.addOrReplaceChild("squid_eye_l_r1", CubeListBuilder.create().texOffs(2, 32).addBox(-1.0F, -4.6F, -2.5F, 2.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, -0.6F, 0.8F, -1.2243F, 0.0F, 3.1416F));

		PartDefinition head_box_r1 = head_collar.addOrReplaceChild("head_box_r1", CubeListBuilder.create().texOffs(132, 2).addBox(-4.0F, -7.8F, -4.0F, 8.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.2F, -0.5F, -1.2171F, 0.0F, 3.1416F));

		PartDefinition arms_upper = head_collar.addOrReplaceChild("arms_upper", CubeListBuilder.create(), PartPose.offset(0.0F, 0.2F, 5.0F));

		PartDefinition arm_upper_lat_r_3_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_r_3_r1", CubeListBuilder.create().texOffs(90, 32).addBox(-0.5F, -7.2F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.8F, 4.0F, 13.8F, -1.3855F, 0.2526F, -3.1416F));

		PartDefinition arm_upper_lat_r_2_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_r_2_r1", CubeListBuilder.create().texOffs(470, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.6F, 2.0F, 7.0F, -1.2979F, 0.3129F, -3.1416F));

		PartDefinition arm_upper_lat_r_1_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_r_1_r1", CubeListBuilder.create().texOffs(458, 2).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 0.0F, 0.2F, -1.2968F, 0.2995F, -3.1416F));

		PartDefinition arm_dorsal_r_3_r1 = arms_upper.addOrReplaceChild("arm_dorsal_r_3_r1", CubeListBuilder.create().texOffs(82, 32).addBox(-0.5F, -7.2F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.2F, 1.0F, 14.4F, -1.4808F, 0.1206F, -3.1416F));

		PartDefinition arm_dorsal_r_2_r1 = arms_upper.addOrReplaceChild("arm_dorsal_r_2_r1", CubeListBuilder.create().texOffs(446, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1F, 0.0F, 7.2F, -1.4344F, 0.1516F, -3.1416F));

		PartDefinition arm_dorsal_r_1_r1 = arms_upper.addOrReplaceChild("arm_dorsal_r_1_r1", CubeListBuilder.create().texOffs(434, 2).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.3F, -1.0F, 0.2F, -1.4298F, 0.1138F, -3.1416F));

		PartDefinition arm_upper_lat_l_3_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_l_3_r1", CubeListBuilder.create().texOffs(74, 32).addBox(-0.5F, -7.2F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.8F, 4.0F, 13.8F, -1.3855F, -0.2526F, 3.1416F));

		PartDefinition arm_upper_lat_l_2_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_l_2_r1", CubeListBuilder.create().texOffs(398, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.6F, 2.0F, 7.0F, -1.2979F, -0.3129F, 3.1416F));

		PartDefinition arm_upper_lat_l_1_r1 = arms_upper.addOrReplaceChild("arm_upper_lat_l_1_r1", CubeListBuilder.create().texOffs(386, 2).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 0.0F, 0.2F, -1.2968F, -0.2995F, 3.1416F));

		PartDefinition arm_dorsal_l_3_r1 = arms_upper.addOrReplaceChild("arm_dorsal_l_3_r1", CubeListBuilder.create().texOffs(66, 32).addBox(-0.5F, -7.2F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.2F, 1.0F, 14.4F, -1.4808F, -0.1206F, 3.1416F));

		PartDefinition arm_dorsal_l_2_r1 = arms_upper.addOrReplaceChild("arm_dorsal_l_2_r1", CubeListBuilder.create().texOffs(374, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1F, 0.0F, 7.2F, -1.4344F, -0.1516F, 3.1416F));

		PartDefinition arm_dorsal_l_1_r1 = arms_upper.addOrReplaceChild("arm_dorsal_l_1_r1", CubeListBuilder.create().texOffs(362, 2).addBox(-1.0F, -7.8F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.3F, -1.0F, 0.2F, -1.4298F, -0.1138F, 3.1416F));

		PartDefinition arms_lower = head_collar.addOrReplaceChild("arms_lower", CubeListBuilder.create(), PartPose.offset(0.0F, 3.2F, 5.0F));

		PartDefinition arm_ventral_r_3_r1 = arms_lower.addOrReplaceChild("arm_ventral_r_3_r1", CubeListBuilder.create().texOffs(110, 32).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 9.4F, 12.2F, -1.2507F, 0.1038F, -3.1416F));

		PartDefinition arm_ventral_r_2_r1 = arms_lower.addOrReplaceChild("arm_ventral_r_2_r1", CubeListBuilder.create().texOffs(494, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8F, 5.0F, 6.4F, -0.9253F, 0.1201F, -3.1416F));

		PartDefinition arm_ventral_r_1_r1 = arms_lower.addOrReplaceChild("arm_ventral_r_1_r1", CubeListBuilder.create().texOffs(274, 2).addBox(-1.0F, -8.8F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, 0.4F, 0.2F, -0.9347F, 0.0965F, -3.1416F));

		PartDefinition arm_lower_lat_r_3_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_r_3_r1", CubeListBuilder.create().texOffs(130, 32).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.8F, 7.6F, 12.8F, -1.0993F, 0.1831F, -3.1416F));

		PartDefinition arm_lower_lat_r_2_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_r_2_r1", CubeListBuilder.create().texOffs(482, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.2F, 3.6F, 6.8F, -0.9985F, 0.2606F, -3.1416F));

		PartDefinition arm_lower_lat_r_1_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_r_1_r1", CubeListBuilder.create().texOffs(262, 2).addBox(-1.0F, -8.8F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.4F, -0.6F, 0.2F, -1.0202F, 0.2663F, -3.1416F));

		PartDefinition arm_ventral_l_3_r1 = arms_lower.addOrReplaceChild("arm_ventral_l_3_r1", CubeListBuilder.create().texOffs(98, 32).addBox(-1.0F, -5.2F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 9.4F, 12.2F, -1.2507F, -0.1038F, 3.1416F));

		PartDefinition arm_ventral_l_2_r1 = arms_lower.addOrReplaceChild("arm_ventral_l_2_r1", CubeListBuilder.create().texOffs(422, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8F, 5.0F, 6.4F, -0.9253F, -0.1201F, 3.1416F));

		PartDefinition arm_ventral_l_1_r1 = arms_lower.addOrReplaceChild("arm_ventral_l_1_r1", CubeListBuilder.create().texOffs(250, 2).addBox(-1.0F, -8.8F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2F, 0.4F, 0.2F, -0.9347F, -0.0965F, 3.1416F));

		PartDefinition arm_lower_lat_l_3_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_l_3_r1", CubeListBuilder.create().texOffs(122, 32).addBox(-0.5F, -6.2F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.8F, 7.6F, 12.8F, -1.0993F, -0.1831F, 3.1416F));

		PartDefinition arm_lower_lat_l_2_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_l_2_r1", CubeListBuilder.create().texOffs(410, 2).addBox(-1.0F, -8.2F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.2F, 3.6F, 6.8F, -0.9985F, -0.2606F, 3.1416F));

		PartDefinition arm_lower_lat_l_1_r1 = arms_lower.addOrReplaceChild("arm_lower_lat_l_1_r1", CubeListBuilder.create().texOffs(238, 2).addBox(-1.0F, -8.8F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, -0.6F, 0.2F, -1.0202F, -0.2663F, 3.1416F));

		PartDefinition tentacle_long_l = head_collar.addOrReplaceChild("tentacle_long_l", CubeListBuilder.create(), PartPose.offset(2.4F, 2.0F, 5.0F));

		PartDefinition tentacle_l_club_r1 = tentacle_long_l.addOrReplaceChild("tentacle_l_club_r1", CubeListBuilder.create().texOffs(38, 32).addBox(-1.5F, -6.4F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.7F, 6.5F, 18.4F, -1.2594F, -0.1874F, 3.1416F));

		PartDefinition tentacle_l_stalk_2_r1 = tentacle_long_l.addOrReplaceChild("tentacle_l_stalk_2_r1", CubeListBuilder.create().texOffs(214, 2).addBox(-1.0F, -10.2F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, 3.2F, 9.8F, -1.2135F, -0.1543F, 3.1416F));

		PartDefinition tentacle_l_stalk_1_r1 = tentacle_long_l.addOrReplaceChild("tentacle_l_stalk_1_r1", CubeListBuilder.create().texOffs(190, 2).addBox(-1.0F, -10.8F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2F, -0.4F, 0.2F, -1.2165F, -0.1651F, 3.1416F));

		PartDefinition tentacle_long_r = head_collar.addOrReplaceChild("tentacle_long_r", CubeListBuilder.create(), PartPose.offset(-2.4F, 2.0F, 5.0F));

		PartDefinition tentacle_r_club_r1 = tentacle_long_r.addOrReplaceChild("tentacle_r_club_r1", CubeListBuilder.create().texOffs(52, 32).addBox(-1.5F, -6.4F, -1.0F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.7F, 6.5F, 18.4F, -1.2594F, 0.1874F, -3.1416F));

		PartDefinition tentacle_r_stalk_2_r1 = tentacle_long_r.addOrReplaceChild("tentacle_r_stalk_2_r1", CubeListBuilder.create().texOffs(226, 2).addBox(-1.0F, -10.2F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, 3.2F, 9.8F, -1.2135F, 0.1543F, -3.1416F));

		PartDefinition tentacle_r_stalk_1_r1 = tentacle_long_r.addOrReplaceChild("tentacle_r_stalk_1_r1", CubeListBuilder.create().texOffs(202, 2).addBox(-1.0F, -10.8F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2F, -0.4F, 0.2F, -1.2165F, 0.1651F, -3.1416F));

		PartDefinition fin_l = mantle_root.addOrReplaceChild("fin_l", CubeListBuilder.create(), PartPose.offset(4.5F, -5.5F, -12.0F));

		PartDefinition fin_l_wing_r1 = fin_l.addOrReplaceChild("fin_l_wing_r1", CubeListBuilder.create().texOffs(286, 2).addBox(-3.5F, -0.5F, -5.0F, 7.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.7F, -0.9F, -0.8F, -3.073F, 0.5F, 2.9928F));

		PartDefinition fin_r = mantle_root.addOrReplaceChild("fin_r", CubeListBuilder.create(), PartPose.offset(-4.5F, -5.5F, -12.0F));

		PartDefinition fin_r_wing_r1 = fin_r.addOrReplaceChild("fin_r_wing_r1", CubeListBuilder.create().texOffs(324, 2).addBox(-3.5F, -0.5F, -5.0F, 7.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.7F, -0.9F, -0.8F, -3.073F, -0.5F, -2.9928F));

		return LayerDefinition.create(meshdefinition, 512, 512);
	}

	@Override
	protected ModelPart rootPart() {
		return this.mantle_root;
	}

	@Override
	public void setupAnim(GhostSquidEntity entity, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		resetToBindPose();
		this.renderAlpha = 0.86F;
		float t = ageInTicks + entity.getId() * 2.3F;
		boolean jetting = entity.getJetTicks() > 0;
		boolean lashing = entity.getLashTicks() > 0;

		// Rhythmic mantle-jet pulse & graceful abyssal ear-fin undulation
		float pulse = jetting ? Mth.sin(t * 0.55F) : Mth.sin(t * 0.13F);
		float finWave = Mth.sin(t * (jetting ? 0.42F : 0.18F)) * 28.0F;
		addBbDeg(this.mantle_root, (jetting ? 12.0F : 4.0F) + pulse * 3.5F, 0.0F, Mth.cos(t * 0.07F) * 3.0F);
		this.mantle_root.xScale *= (1.0F - 0.045F * pulse);
		this.mantle_root.yScale *= (1.0F - 0.045F * pulse);
		addBbDeg(this.fin_l, Mth.cos(t * 0.18F) * 8.0F, 0.0F, -finWave);
		addBbDeg(this.fin_r, Mth.cos(t * 0.18F) * 8.0F, 0.0F, finWave);

		// Bell-opening intake vs streamlined jet-thrust tentacle cone + cornered whip lash
		float spread = jetting ? -8.0F : (10.0F + Math.max(0.0F, -pulse) * 16.0F);
		addBbDeg(this.arms_upper, -spread, 0.0F, 0.0F);
		addBbDeg(this.arms_lower, spread, 0.0F, 0.0F);
		if (lashing) {
			float whip = Mth.sin(t * 0.95F) * 38.0F;
			addBbDeg(this.tentacle_long_l, -24.0F, whip, 0.0F);
			addBbDeg(this.tentacle_long_r, 24.0F, -whip, 0.0F);
		} else {
			addBbDeg(this.tentacle_long_l, Mth.sin(t * 0.14F - 0.8F) * 10.0F, -spread * 0.7F + Mth.cos(t * 0.11F) * 8.0F, 0.0F);
			addBbDeg(this.tentacle_long_r, Mth.cos(t * 0.14F - 0.8F) * 10.0F, spread * 0.7F - Mth.sin(t * 0.11F) * 8.0F, 0.0F);
		}
	}
}
