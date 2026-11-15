package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;

import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;
import ru.aiforge.riftworld.registry.ModBlocks;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.55.0 — рынок Элисара: ряд прилавков, у каждого стоит свой торговец.
 *
 * <p>Раньше в городе было всего три обычных прилавка и четыре рыбных, и
 * никто за ними не стоял: торговля существовала, но «рынка» не было.</p>
 *
 * <p>Теперь у каждой площади-рынка разворачивается ряд прилавков, и за
 * каждым прилавком стоит житель с точной профессией: у мясного — Рыбак,
 * у ювелирного — Ювелир, у стола с монетами — Банкир и так далее. Профессия
 * торговца совпадает с прилавком, поэтому видно, кто чем торгует.</p>
 */
public final class ElisarMarketKeeper {

    private ElisarMarketKeeper() {
    }

    /** Прилавок + профессия торговца, который за ним стоит. */
    private record Stall(ModelKind kind, ElisarProfession profession, int offsetX, int offsetZ, Direction facing) {
    }

    /**
     * Рынок у площади: 8 прилавков полукругом, лицом к центру площади.
     *
     * <p>Порядок professions повторяет круг: так торговцы стоят лицом к
     * прохожим, а покупатель видит, что за прилавком — Охотник или Банкир.</p>
     */
    private static final Stall[] STALLS = {
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.BAKER, 0, -3, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.MASON, 3, -3, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.SMITH, 5, 0, Direction.WEST),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.ARMOURER, 3, 3, Direction.NORTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.SCULPTOR, 0, 3, Direction.NORTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.TANNER, -3, 3, Direction.NORTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.CARTER, -5, 0, Direction.EAST),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.BREWER, -3, -3, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL_FISH, ElisarProfession.FISHER, 0, -6, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL_FISH, ElisarProfession.HUNTER, -6, 0, Direction.EAST),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.HERBALIST, 6, 3, Direction.NORTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.JEWELLER, 6, -3, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.BANKER, -6, -3, Direction.SOUTH),
            new Stall(ModelKind.MARKET_STALL, ElisarProfession.MERCHANT, 0, 6, Direction.NORTH),
    };

    /**
     * Развернуть рынок в площади.
     *
     * @param level  мир
     * @param plazaX центр площади (X)
     * @param plazaZ центр площади (Z)
     * @param baseY  высота пола площади
     * @return число поставленных прилавков
     */
    public static int build(ServerLevelAccessor level, int plazaX, int plazaZ, int baseY) {
        int placed = 0;
        for (Stall stall : STALLS) {
            int x = plazaX + stall.offsetX();
            int z = plazaZ + stall.offsetZ();
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            if (placeStall(level, stall, x, z, baseY)) {
                ++placed;
            }
        }
        return placed;
    }

    /** Ставит прилавок и торговца за ним. Возвращает true, если всё удалось. */
    private static boolean placeStall(ServerLevelAccessor level, Stall stall, int x, int z, int baseY) {
        ModelBlock block = ModBlocks.MODELS.get(stall.kind()).get();
        if (block == null) return false;
        BlockPos master = new BlockPos(x, baseY, z);
        // Пол прилавка должен быть твёрдым — иначе он повиснет в воздухе.
        if (level.getBlockState(master.below()).isAir()) return false;
        BlockState state = block.defaultBlockState()
                .setValue(ModelBlock.FACING, stall.facing())
                .setValue(ModelBlock.POWERED, false);
        level.setBlock(master, state, 3);
        BlockState part = ModBlocks.PART.get().defaultBlockState()
                .setValue(ru.aiforge.riftworld.block.PartBlock.OPEN, true);
        for (BlockPos off : stall.kind().partOffsets(stall.facing())) {
            level.setBlock(master.offset(off), part, 3);
        }
        spawnTrader(level, stall.profession(), x, z, baseY, stall.facing());
        return true;
    }

    /** Ставит торговца за прилавком — он стоит лицом к покупателю. */
    private static void spawnTrader(ServerLevelAccessor level, ElisarProfession profession,
                                    int x, int z, int baseY, Direction facing) {
        ServerLevel sl = level.getLevel();
        if (sl == null) return;
        // Торговец стоит с изнанки прилавка, лицом к его фронту.
        int dx = facing.getStepX();
        int dz = facing.getStepZ();
        int vx = x - dx;
        int vz = z - dz;
        int y = baseY;
        BlockPos pos = new BlockPos(vx, y, vz);
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            // Прилавок занят — тогда торговец встаёт рядом.
            vx += dx;
            vz += dz;
            pos = new BlockPos(vx, y, vz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return;
        }
        ElisarVillagerEntity villager = ModEntities.ELISAR_VILLAGER.get().create(sl);
        if (villager == null) return;
        villager.moveTo(vx + 0.5D, y, vz + 0.5D, facing.toYRot(), 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        villager.setProfession(profession);
        villager.restrictTo(pos, 24);
        if (level.addFreshEntity(villager)) {
            villager.setPersistenceRequired();
        }
    }

    /** Все профессии, которые задействованы на рынке. */
    public static List<ElisarProfession> marketProfessions() {
        List<ElisarProfession> out = new ArrayList<>(STALLS.length);
        for (Stall s : STALLS) out.add(s.profession());
        return out;
    }
}
