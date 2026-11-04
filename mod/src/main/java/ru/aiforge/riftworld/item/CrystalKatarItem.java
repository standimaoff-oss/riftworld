package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * «Длань Осколка» ({@code crystal_katar}) — многоразовый боевой кристальный клинок-наруч,
 * повторяющий оружие на руке Гранёного Прислужника ({@code 02_crystal_minions_ref.png}).
 * Быстрые выпады + каждый 3-й удар вызывает кристальный раскол перед владельцем.
 */
public class CrystalKatarItem extends SwordItem implements CrystalGeoItem {
    public CrystalKatarItem(Properties props) {
        super(RiftTiers.RIFT_CRYSTAL, 5, -2.0F, props); // 1 + 1.5 + 5 = 7.5 урона, скорость 2.0 уд/с
    }

    @Override
    public String geoModel() {
        return "crystal_katar";
    }

    @Override
    public String geoTexture() {
        return "crystal_minion";
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean res = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && attacker.level() instanceof ServerLevel sl) {
            int step = (stack.getOrCreateTag().getInt("RiftStrike") + 1) % 3;
            stack.getOrCreateTag().putInt("RiftStrike", step);
            sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.2F + step * 0.15F);
            sl.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    6, 0.25, 0.25, 0.25, 0.04);

            if (step == 0) {
                // Каждый 3-й удар — «Рассечение Грани»: резонансный веер по врагам перед бойцом
                sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.35F);
                sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.9, target.getZ(),
                        14, 0.45, 0.35, 0.45, 0.07);
                Vec3 fwd = attacker.getLookAngle();
                AABB arc = target.getBoundingBox().inflate(1.8, 0.8, 1.8);
                for (LivingEntity near : sl.getEntitiesOfClass(LivingEntity.class, arc, e -> e.isAlive() && e != attacker)) {
                    near.hurt(sl.damageSources().indirectMagic(attacker, attacker), 3.0F);
                    near.push(fwd.x * 0.28, 0.1, fwd.z * 0.28);
                }
            }
        }
        return res;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.crystal_katar.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.riftworld.crystal_katar.desc2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
