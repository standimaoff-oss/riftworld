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
 * v0.48.0: 3D multi-layered armor item for Aurite (medium) and Riftcrystal (strong, emissive).
 * Routes client armor rendering to {@code ElisarArmorModels} (128x128 non-overlapping Box-UV)
 * while staying 100% dedicated-server classload safe.
 *
 * Note: {@link #initializeClient} runs inside {@code super(material, type, props)} before subclass
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
