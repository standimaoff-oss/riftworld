package ru.aiforge.riftworld.item;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.RiftWorld;

/**
 * v0.49.0: 3D multi-layered armor item for Aurite (medium) and Riftcrystal (strong, emissive).
 *
 * <p>The worn armor is 2x HD: {@code ElisarArmorModels} bakes a 256x256 non-overlapping
 * Box-UV atlas with a nested {@code "hd"} child bone scaled to 0.5 units per texel, so the
 * plates keep the reference silhouette instead of the v0.48 blockiness.
 *
 * <p>The DROPPED / inventory / GUI representation is a TRUE volumetric 3D item model
 * (a standalone {@code models/item/<id>.json} element model with a 32x32 sprite and a
 * 64x64 palette atlas) — not a flat {@code item/generated} sprite — so the helmet,
 * cuirass, greaves and sabatons read as real objects in the hotbar and on the ground.
 *
 * <p>Rendering routes through {@code ClientHooks.armorExt()} while staying 100%
 * dedicated-server classload safe.
 *
 * <p>Note: {@link #initializeClient} runs inside {@code super(material, type, props)} before subclass
 * fields are initialized, so {@link #isRiftcrystal()} derives directly from {@link #getMaterial()}
 * and {@code ClientHooks.armorExt()} inspects the live {@link ItemStack} at render time.
 */
public class ElisarArmorItem extends ArmorItem {
    public ElisarArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties props) {
        super(material, type, props);
    }

    public boolean isRiftcrystal() {
        return this.getMaterial() == RiftArmorMaterials.RIFTCRYSTAL;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return RiftWorld.MODID + ":textures/models/armor/"
                + (this.isRiftcrystal() ? "riftcrystal_armor.png" : "aurite_armor.png");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.armorExt());
    }
}
