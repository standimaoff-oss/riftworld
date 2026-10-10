package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Part 7 «Храм полностью»: the DEMONIC TELEPORT found roaring in the overgrown temple of the common world.
 * A churning crimson pane - step in and it drags you across the Rift to the Great Temple of the floating city.
 * Walk-through, unmineable, glows like an ember.
 */
public class DemonGateBlock extends Block implements EntityBlock {
    private static final Map<UUID, Long> CD = new HashMap<>();

    public DemonGateBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_RED).noCollission().strength(-1.0F, 3600000.0F)
                .sound(SoundType.SCULK).lightLevel(s -> 12).noLootTable());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemonGateBE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> tick((ServerLevel) l, p, (DemonGateBE) be);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.block();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (rnd.nextInt(3) == 0) level.addParticle(ParticleTypes.CRIMSON_SPORE, x + rnd.nextGaussian() * 0.3, y + rnd.nextDouble(), z + rnd.nextGaussian() * 0.3, 0, -0.02, 0);
        if (rnd.nextInt(6) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, rnd.nextGaussian() * 0.05, 0.03, rnd.nextGaussian() * 0.05);
    }

    private static void tick(ServerLevel sl, BlockPos pos, DemonGateBE be) {
        if (sl.getGameTime() % 10 != 0) return;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(0.35))) {
            if (!p.isAlive() || p.isSpectator()) continue;
            Long last = CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 120) continue;
            ServerLevel elisar = sl.getServer().getLevel(Flight.ELISAR_DIM);
            if (elisar == null) continue;
            CD.put(p.getUUID(), sl.getGameTime());
            BlockPos target = templeApron(elisar);
            sl.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.9F, 0.5F);
            sl.playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.2F, 0.6F);
            elisar.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.4, 0.6, 0.4, 0.01);
            p.teleportTo(elisar, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, p.getYRot(), p.getXRot());
            elisar.playSound(null, target, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7F, 1.4F);
            // arrival burst (Part 12): a soul-spray, a heartbeat of darkness, then the temple light
            elisar.playSound(null, target, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.8F);
            elisar.sendParticles(ParticleTypes.CRIMSON_SPORE, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 40, 1.2, 1.0, 1.2, 0.02);
            elisar.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 24, 1.0, 0.6, 1.0, 0.02);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 50, 0, false, false));
        }
    }

    /** The front apron of the Great Temple in Elisar (falls back to the island centre if the plot is ungenerated). */
    public static BlockPos templeApron(ServerLevel elisar) {
        int fx = 0, fz = 0;
        for (CityPlan.Plot pl : CityPlan.get().plots) {
            if (!pl.template().equals("temple_great")) continue;
            fx = pl.x0() + pl.w() / 2;
            fz = pl.z0() + pl.d() - 6;
            break;
        }
        BlockPos mid = new BlockPos(fx, 64, fz);
        return elisar.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, mid).above();
    }
}
