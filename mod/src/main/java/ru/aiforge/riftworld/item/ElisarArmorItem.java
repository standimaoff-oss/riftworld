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
 */
public class ElisarArmorItem extends ArmorItem {
    private final boolean riftcrystal;

    public ElisarArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties props) {
        super(material, type, props);
        this.riftcrystal = material == RiftArmorMaterials.RIFTCRYSTAL;
    }

    public boolean isRiftcrystal() {
        return this.riftcrystal;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return RiftWorld.MODID + ":textures/models/armor/"
                + (this.riftcrystal ? "riftcrystal_armor.png" : "aurite_armor.png");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.armorExt(this.riftcrystal));
    }
}
