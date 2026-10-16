package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import ru.aiforge.riftworld.world.RiftTravel;

/**
 * ЧЕРНЫЙ ПРОЛОМ ПРОСТРАНСТВА (Black Rift in Space):
 * The portal torn between dimensions.
 * Found in the ancient overgrown temple in the Overworld and opened through the ritual in the City's Great Temple.
 * Walk-through, unmineable, emits deep dark void particles and ambient whispers.
 */
public class DemonGateBlock extends Block implements EntityBlock {
    private static final Map<UUID, Long> CD = new HashMap<>();

    public DemonGateBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_BLACK).noCollission().strength(-1.0F, 3600000.0F)
                .sound(SoundType.SCULK).lightLevel(s -> 11).noLootTable());
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
        // Deep black void ink smoke billowing from the tear
        if (rnd.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SQUID_INK,
                    x + rnd.nextGaussian() * 0.28, y + rnd.nextGaussian() * 0.35, z + rnd.nextGaussian() * 0.28,
                    0, 0.015, 0);
        }
        // Spatial distortion particles spiraling inward
        if (rnd.nextInt(3) == 0) {
            double dx = (rnd.nextDouble() - 0.5) * 1.4, dy = (rnd.nextDouble() - 0.5) * 1.4, dz = (rnd.nextDouble() - 0.5) * 1.4;
            level.addParticle(ParticleTypes.PORTAL, x + dx, y + dy, z + dz, -dx * 0.3, -dy * 0.3, -dz * 0.3);
        }
        // Burning event-horizon sparks
        if (rnd.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + rnd.nextGaussian() * 0.2, y + rnd.nextGaussian() * 0.2, z + rnd.nextGaussian() * 0.2,
                    rnd.nextGaussian() * 0.02, 0.03, rnd.nextGaussian() * 0.02);
        }
        // Occasional dimensional hum
        if (rnd.nextInt(80) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.5F + rnd.nextFloat() * 0.2F, false);
        }
    }

    private static void tick(ServerLevel sl, BlockPos pos, DemonGateBE be) {
        if (sl.getGameTime() % 10 != 0) return;
        // Spatial tear collision envelope (covers the 4.5m tall free-floating 3D rift)
        AABB riftBounds = new AABB(pos).inflate(1.6, 2.2, 1.2);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, riftBounds)) {
            if (!p.isAlive() || p.isSpectator()) continue;
            Long last = CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 120) continue;
            CD.put(p.getUUID(), sl.getGameTime());

            if (sl.dimension().equals(Level.OVERWORLD)) {
                // OVERWORLD -> ELISAR
                RiftTravel.remember(sl, pos);
                RiftTravel.travelToElisar(p, sl);
            } else {
                // ELISAR -> OVERWORLD
                RiftTravel.travelToOverworld(p, sl);
            }
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
