package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.model.ElisarArmorModels;
import ru.aiforge.riftworld.item.ElisarArmorItem;
import ru.aiforge.riftworld.item.RiftArmorMaterials;

/**
 * v0.48.0: Emissive cyan + violet rift-vein overlay for Riftcrystal Strong Armor
 * (reference 02_riftcrystal_strong_armor.png).
 */
public final class RiftcrystalArmorGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation GLOW_TEX = new ResourceLocation(
            RiftWorld.MODID, "textures/models/armor/riftcrystal_armor_glow.png");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public RiftcrystalArmorGlowLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        M parentModel = this.getParentModel();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ElisarArmorItem armor) || !armor.isRiftcrystal()) {
                continue;
            }
            HumanoidModel<LivingEntity> glowModel = ElisarArmorModels.getForSlot(
                    RiftArmorMaterials.RIFTCRYSTAL, slot, parentModel);
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
            glowModel.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
