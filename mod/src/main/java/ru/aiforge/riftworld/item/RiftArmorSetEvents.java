package ru.aiforge.riftworld.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/** Small, visible bonuses for wearing all four matching pieces; no hidden attribute accumulation. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class RiftArmorSetEvents {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 100 != 0) return;
        RiftArmorMaterials material = fullSetMaterial(entity);
        if (material == RiftArmorMaterials.AURITE) {
            entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 220, 0, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 220, 0, true, false, true));
        } else if (material == RiftArmorMaterials.RIFTCRYSTAL) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, true, false, true));
        }
    }

    private static RiftArmorMaterials fullSetMaterial(LivingEntity entity) {
        RiftArmorMaterials result = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor)
                    || !(armor.getMaterial() instanceof RiftArmorMaterials current)) return null;
            if (result == null) result = current;
            else if (result != current) return null;
        }
        return result;
    }

    private RiftArmorSetEvents() {
    }
}
