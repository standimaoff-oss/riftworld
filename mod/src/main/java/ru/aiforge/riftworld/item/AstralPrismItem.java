package ru.aiforge.riftworld.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.entity.CrystalCombatHandler;
import ru.aiforge.riftworld.entity.CrystalShardEntity;
import ru.aiforge.riftworld.entity.IceHailBoulderEntity;
import ru.aiforge.riftworld.entity.IcePillarEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.26.0: Могущественная мифическая реликвия «Призма Архонта Разлома» ({@code astral_prism}):
 * <ul>
 *   <li><b>ЛКМ — «Копьё Сингулярности» (КД 0.85 с):</b> прожигает 36-метровый астральный луч Разлома
 *       сквозь всех врагов на линии взгляда (14 ед. урона) и выпускает сходящийся залп из 3 осколков Архонта;</li>
 *   <li><b>ПКМ — «Разломный Рывок Архонта» (КД 8.5 с):</b> рассекает пространство на 18 блоков вперёд
 *       (16 ед. урона всем врагам вдоль прорыва), даёт Сопротивление II, Скорость II и Поглощение II,
 *       поднимает 4 ледяных столба Разлома и раскрывает 8-секундную Астролябию Сингулярности с финальной Сверхновой;</li>
 *   <li><b>Shift + ПКМ — «Катаклизм Расколотого Обелиска» (КД 15 с):</b> обрушивает на указанную область
 *       кольцо из 8 столбов Разлома и 4 гигантские небесные ледяные глыбы по очереди!</li>
 * </ul>
 */
public class AstralPrismItem extends Item implements CrystalGeoItem {
    public AstralPrismItem(Properties props) {
        super(props);
    }

    @Override
    public String geoModel() {
        return "astral_prism";
    }

    /** ЛКМ: «Копьё Сингулярности» — мгновенный пронзающий луч + веер из 3 сходящихся кристаллов Разлома! */
    public void fireSingularityLance(ServerPlayer sp) {
        if (sp.getCooldowns().isOnCooldown(this)) return;
        ServerLevel sl = sp.serverLevel();
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        else right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        double maxRange = 36.0;
        BlockHitResult hit = sl.clip(new ClipContext(eye, eye.add(look.scale(maxRange)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(maxRange)) : hit.getLocation();
        double len = eye.distanceTo(end);

        // Двойная астральная спираль луча + пронзающий урон вдоль всей линии
        Set<Integer> struck = new HashSet<>();
        for (double d = 1.2; d <= len; d += 0.85) {
            Vec3 p = eye.add(look.scale(d));
            double ang = d * 1.15;
            Vec3 h1 = p.add(right.scale(Math.cos(ang) * 0.28)).add(up.scale(Math.sin(ang) * 0.28));
            sl.sendParticles(ParticleTypes.END_ROD, h1.x, h1.y, h1.z, 1, 0.0, 0.0, 0.0, 0.0);
            if (((int) (d * 2)) % 3 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.005);
            }
            AABB segBox = new AABB(p.x - 1.15, p.y - 1.15, p.z - 1.15, p.x + 1.15, p.y + 1.15, p.z + 1.15);
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, segBox, e -> e != sp && e.isAlive())) {
                if (le instanceof Player || !struck.add(le.getId())) continue;
                le.hurt(sl.damageSources().indirectMagic(sp, sp), 14.0F);
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                Vec3 kb = look.scale(0.65).add(0.0, 0.22, 0.0);
                le.setDeltaMovement(le.getDeltaMovement().add(kb));
                sl.sendParticles(ParticleTypes.FLASH, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 1, 0, 0, 0, 0);
            }
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, eye.x + look.x * 2.2, eye.y + look.y * 2.2, eye.z + look.z * 2.2, 1, 0, 0, 0, 0);

        // Тройной сходящийся залп кристаллов Разлома в точку прицела
        Vec3 aimTarget = eye.add(look.scale(Math.max(12.0, len)));
        for (int i = -1; i <= 1; ++i) {
            Vec3 spawnPos = eye.add(look.scale(1.1)).add(right.scale(i * 0.75)).add(up.scale(i == 0 ? 0.45 : -0.10));
            Vec3 shardDir = aimTarget.subtract(spawnPos).normalize();
            CrystalShardEntity shard = new CrystalShardEntity(ModEntities.CRYSTAL_SHARD.get(), sl, sp, 10.5F);
            shard.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            shard.setDeltaMovement(shardDir.scale(2.25));
            sl.addFreshEntity(shard);
        }

        sl.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55F, 1.65F);
        sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 1.35F);
        sp.getCooldowns().addCooldown(this, 17); // 0.85 секунды!
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            if (sp.isSecondaryUseActive()) {
                castObeliskCataclysm(sl, sp);
                sp.getCooldowns().addCooldown(this, 300); // 15 с ультимейт Катаклизма
            } else {
                castArchonRiftBlink(sl, sp);
                sp.getCooldowns().addCooldown(this, 170); // 8.5 с Разломный Рывок + Астролябия
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void castArchonRiftBlink(ServerLevel sl, ServerPlayer sp) {
        Vec3 start = sp.getEyePosition();
        Vec3 look = sp.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(18.0));
        BlockHitResult hit = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 dest = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.95));

        BlockPos bp = BlockPos.containing(dest.x, dest.y - 1.0, dest.z);
        double targetY = dest.y - 1.0;
        if (sl.getBlockState(bp).isSolid() && sl.getBlockState(bp.above()).isAir() && sl.getBlockState(bp.above(2)).isAir()) {
            targetY = bp.getY() + 1.05;
        }

        Vec3 origin = sp.position();
        Vec3 destFeet = new Vec3(dest.x, targetY, dest.z);

        // Рассекающий прорыв сквозь пространство: 16 ед. урона всем врагам вдоль траектории рывка!
        Set<Integer> slashed = new HashSet<>();
        int steps = 20;
        for (int i = 0; i <= steps; ++i) {
            double u = (double) i / steps;
            Vec3 p = origin.lerp(destFeet, u).add(0.0, 1.0, 0.0);
            sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.25, 0.35, 0.25, 0.04);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.20, 0.30, 0.20, 0.02);
            AABB slashBox = new AABB(p.x - 1.8, p.y - 1.5, p.z - 1.8, p.x + 1.8, p.y + 1.8, p.z + 1.8);
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, slashBox, e -> e != sp && e.isAlive())) {
                if (le instanceof Player || !slashed.add(le.getId())) continue;
                le.hurt(sl.damageSources().indirectMagic(sp, sp), 16.0F);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
            }
        }

        sl.playSound(null, sp.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.2F, 1.25F);
        sp.teleportTo(dest.x, targetY, dest.z);
        sp.fallDistance = 0.0F;

        // Благословение Архонта: Сопротивление II, Скорость II и Поглощение II (4 золотых сердца)!
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true));

        sl.sendParticles(ParticleTypes.FLASH, dest.x, targetY + 1.0, dest.z, 2, 0.1, 0.1, 0.1, 0.0);
        sl.sendParticles(ParticleTypes.END_ROD, dest.x, targetY + 1.0, dest.z, 40, 0.7, 0.9, 0.7, 0.12);
        sl.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1F, 1.45F);

        // 4 растущих кристально-ледяных столба Разлома вокруг точки прибытия!
        for (int k = 0; k < 4; ++k) {
            double ang = (Math.PI * 0.5) * k + Math.PI * 0.25;
            double px = dest.x + Math.cos(ang) * 3.4;
            double pz = dest.z + Math.sin(ang) * 3.4;
            int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(px), Mth.floor(pz));
            double py = Math.abs(gy - targetY) <= 6.0 ? gy : targetY;
            IcePillarEntity pillar = ModEntities.ICE_PILLAR.get().create(sl);
            if (pillar != null) {
                pillar.setPos(px, py, pz);
                pillar.configure(sp, 5.2F, false, -k * 2);
                sl.addFreshEntity(pillar);
            }
        }

        // 8-секундная Астролябия Сингулярности (вихрь + самонаводящиеся осколки + финальная Сверхновая!)
        CrystalCombatHandler.spawnPrismBeacon(sl, sp.getUUID(), new Vec3(dest.x, targetY + 1.1, dest.z));
    }

    private static void castObeliskCataclysm(ServerLevel sl, ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle().normalize();
        BlockHitResult hit = sl.clip(new ClipContext(eye, eye.add(look.scale(28.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 center = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(20.0)) : hit.getLocation();
        int groundY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(center.x), Mth.floor(center.z));
        double baseY = Math.abs(groundY - center.y) <= 12.0 ? groundY : center.y;
        Vec3 groundCenter = new Vec3(center.x, baseY, center.z);

        sl.playSound(null, BlockPos.containing(groundCenter), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.6F, 0.85F);
        sl.playSound(null, BlockPos.containing(groundCenter), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.1F, 1.35F);
        sl.sendParticles(ParticleTypes.END_ROD, groundCenter.x, groundCenter.y + 1.0, groundCenter.z, 55, 3.2, 0.6, 3.2, 0.08);

        // Кольцо из 8 столбов Разлома вокруг эпицентра Катаклизма!
        for (int i = 0; i < 8; ++i) {
            double ang = (Mth.TWO_PI * i) / 8.0;
            double px = groundCenter.x + Math.cos(ang) * 5.6;
            double pz = groundCenter.z + Math.sin(ang) * 5.6;
            int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(px), Mth.floor(pz));
            double py = Math.abs(gy - baseY) <= 7.0 ? gy : baseY;
            IcePillarEntity pillar = ModEntities.ICE_PILLAR.get().create(sl);
            if (pillar != null) {
                pillar.setPos(px, py, pz);
                pillar.configure(sp, 6.4F, true, -i * 2);
                sl.addFreshEntity(pillar);
            }
        }

        // Небесный Град из 4 гигантских ледяных глыб Обелиска, обрушивающихся в цель по очереди!
        for (int i = 0; i < 4; ++i) {
            double ang = (Mth.TWO_PI * i) / 4.0 + 0.4;
            double rad = i == 0 ? 0.0 : 3.2;
            Vec3 impact = new Vec3(groundCenter.x + Math.cos(ang) * rad, baseY, groundCenter.z + Math.sin(ang) * rad);
            IceHailBoulderEntity boulder = ModEntities.ICE_HAIL_BOULDER.get().create(sl);
            if (boulder != null) {
                boulder.setPos(impact.x + (i - 1.5) * 2.4, baseY + 18.5 + i * 1.8, impact.z + (1.5 - i) * 2.0);
                boulder.configure(sp, impact, 14 + i * 9);
                sl.addFreshEntity(boulder);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.astral_prism.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.riftworld.astral_prism.desc2").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("item.riftworld.astral_prism.desc3").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
