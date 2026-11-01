package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.flight.PortalRitual;
import ru.aiforge.riftworld.world.CityPlan;

/**
 * v0.20.0: Древние врата Разлома в заросшем храме Обычного мира.
 *
 * <p>Вместо мгновенного телепорта с земляным экраном загрузки требуют действия игрока
 * (поднести кристалл/предмет ПКМ, нажать ПКМ на полотно врат или расколоть багровую печать ударом ЛКМ)
 * и запускают бесшовный ритуал {@link PortalRitual}: плавное выбеливание мира от центра портала
 * (без изменения блоков!), парящие смотрящие на игрока глаза и резкая вспышка в Элисар!
 */
public class DemonGateBlock extends Block implements EntityBlock {
    private static final Map<UUID, Long> HINT_CD = new HashMap<>();

    public DemonGateBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_RED).noCollission().strength(-1.0F, 3600000.0F)
                .sound(SoundType.SCULK).lightLevel(s -> 13).noLootTable());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemonGateBE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> tick((ServerLevel) l, p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.block();
    }

    /** ПКМ по порталу: поднести кристалл/предмет («отдать») или нажать («нажать») для пробуждения Разлома. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.dimension() == Flight.ELISAR_DIM) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            ItemStack held = sp.getItemInHand(hand);
            boolean offered = PortalRitual.isOfferingItem(held);
            if (PortalRitual.start(sp, pos, offered ? "offer" : "press")) {
                if (offered && !sp.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    /** ЛКМ по порталу: расколоть дремлющую багровую печать («сломать»), не ломая сами блоки в мире. */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.dimension() == Flight.ELISAR_DIM || level.isClientSide) return;
        if (player instanceof ServerPlayer sp) {
            PortalRitual.start(sp, pos, "break");
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (rnd.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE, x + rnd.nextGaussian() * 0.3, y + rnd.nextDouble(), z + rnd.nextGaussian() * 0.3, 0, -0.02, 0);
        }
        if (rnd.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, rnd.nextGaussian() * 0.05, 0.03, rnd.nextGaussian() * 0.05);
        }
    }

    private static void tick(ServerLevel sl, BlockPos pos) {
        if (sl.getGameTime() % 15 != 0 || sl.dimension() == Flight.ELISAR_DIM) return;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(0.35))) {
            if (!p.isAlive() || p.isSpectator() || PortalRitual.isActive(p.getUUID())) continue;
            Long last = HINT_CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 60) continue;
            HINT_CD.put(p.getUUID(), sl.getGameTime());
            p.displayClientMessage(Component.literal(
                    "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u041f\u041a\u041c, \u0440\u0430\u0441\u043a\u043e\u043b\u0438\u0442\u0435 \u043f\u0435\u0447\u0430\u0442\u044c \u041b\u041a\u041c \u0438\u043b\u0438 \u043f\u043e\u0434\u043d\u0435\u0441\u0438\u0442\u0435 \u043a\u0440\u0438\u0441\u0442\u0430\u043b\u043b, \u0447\u0442\u043e\u0431\u044b \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0435\u0440\u0435\u0445\u043e\u0434 \u0432 \u042d\u043b\u0438\u0441\u0430\u0440"),
                    true);
        }
    }

    /** Парадная площадь перед Великим Храмом / Замком в Элисаре. */
    public static BlockPos templeApron(ServerLevel elisar) {
        int fx = 0, fz = 0;
        for (CityPlan.Plot pl : CityPlan.get().plots) {
            if (!pl.template().equals("temple_great") && !pl.template().equals("city_castle")) continue;
            fx = pl.x0() + pl.w() / 2;
            fz = pl.z0() + pl.d() - 3;
            break;
        }
        BlockPos mid = new BlockPos(fx, 64, fz);
        return elisar.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, mid).above();
    }
}
