package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * v0.49.0 — «Тюрьма и склад Элисара».
 *
 * <p>Клетка стоит на настоящей дороге города: страж, который поймал
 * преступника, приводит его сюда. Рядом стоит сундук склада, куда при
 * аресте складываются все вещи арестованного — чтобы ничего не пропало и
 * чтобы игрок мог забрать вещи обратно после срока.</p>
 *
 * <p>Клетка собрана из палочек железа и рифт-сланеца: тёмная каменная
 * база, железные прутья, светящаяся полоса наверху (маяк порядка). Сундук
 * склада стоит снаружи, у входа, чтобы к нему можно было подойти без риска.</p>
 */
public final class ElisarJail {

    private ElisarJail() {
    }

    /** Найденная тюрьма: позиция камеры и позиция сундука склада. */
    public record Site(BlockPos cell, BlockPos chest) {
    }

    /** Смещение клетки относительно точки дороги, на которой стоит тюрьма. */
    private static final int CELL_OFFSET = 4;
    /** Смещение сундука склада. */
    private static final int CHEST_OFFSET = 2;

    /**
     * Построить (или найти) тюрьму рядом с дорогой.
     *
     * @param roadX дорога, к которой привязывается тюрьма
     * @param roadZ
     * @param face  сторона дороги, на которую ставится клетка
     */
    public static Site build(ServerLevel level, double roadX, double roadZ, Direction face) {
        int baseX = net.minecraft.util.Mth.floor(roadX) + face.getStepX() * CELL_OFFSET;
        int baseZ = net.minecraft.util.Mth.floor(roadZ) + face.getStepZ() * CELL_OFFSET;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);

        BlockPos cell = new BlockPos(baseX, ground, baseZ);
        BlockPos chest = new BlockPos(baseX + face.getStepZ() * CHEST_OFFSET,
                ground,
                baseZ - face.getStepX() * CHEST_OFFSET);

        placeCell(level, cell, face);
        placeChest(level, chest);
        return new Site(cell, chest);
    }

    /** Клетка 5x5: каменный пол, железные прутья по периметру, маяк сверху. */
    private static void placeCell(ServerLevel level, BlockPos origin, Direction face) {
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState slate = ElisarBlocks.RIFT_SLATE.get().defaultBlockState();
        BlockState moss = ElisarBlocks.MOSS.get().defaultBlockState();
        BlockState glow = Blocks.SEA_LANTERN.defaultBlockState();

        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = origin.offset(dx, 0, dz);
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                // Пол: камень, по краю мох — чтобы клетка не выглядела чужой в городе.
                set(level, p.below(), (Math.abs(dx) == r || Math.abs(dz) == r) ? moss : slate);
                set(level, p, Blocks.AIR.defaultBlockState());
                if (edge) {
                    set(level, p, bars);
                    set(level, p.above(), bars);
                }
            }
        }
        // Крыша со светящимся маяком порядка — видно издалека.
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos p = origin.offset(dx, 2, dz);
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                set(level, p, edge ? bars : slate);
            }
        }
        set(level, origin.above(3), glow);
        set(level, origin.above(4), glow);

        // Проём в стене клетки со стороны дороги — заключённого надо впустить.
        Direction in = face.getOpposite();
        BlockPos door = origin.offset(in.getStepX(), 1, in.getStepZ());
        set(level, door, Blocks.AIR.defaultBlockState());
        set(level, door.above(), Blocks.AIR.defaultBlockState());
    }

    /** Сундук склада у входа в клетку. */
    private static void placeChest(ServerLevel level, BlockPos pos) {
        set(level, pos.below(), ElisarBlocks.RIFT_SLATE.get().defaultBlockState());
        set(level, pos, Blocks.CHEST.defaultBlockState());
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    /**
     * Найти ближайшую тюрьму к точке: ищем уже построенный сундук склада, у
     * которого рядом стоят железные прутья клетки.
     */
    public static Site findNear(ServerLevel level, double x, double z, double radius) {
        // В 1.20.1 getBlockStates не отдаёт Iterable<BlockState> с позицией,
        // поэтому обходим блоки сами. Но обход всего объёма (высота мира ×
        // радиус²) — это десятки миллионов проверок и подвисание сервера.
        // Поэтому ищем сундук ТОЛЬКО на уровне земли: тюрьма всегда стоит на
        // поверхности дороги, и сундук склада рядом с ней.
        int x0 = net.minecraft.util.Mth.floor(x - radius);
        int z0 = net.minecraft.util.Mth.floor(z - radius);
        int x1 = net.minecraft.util.Mth.floor(x + radius);
        int z1 = net.minecraft.util.Mth.floor(z + radius);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
        // Проверяем уровень земли и блоки вокруг него. Диапазон широкий:
        // игрок стоит ВНУТРИ клетки, и «земля» по его координатам — это
        // крыша клетки на пару блоков выше сундука склада.
        for (int dy = -5; dy <= 3; ++dy) {
            for (int bx = x0; bx <= x1; ++bx) {
                for (int bz = z0; bz <= z1; ++bz) {
                    BlockPos p = new BlockPos(bx, y + dy, bz);
                    if (!level.getBlockState(p).is(Blocks.CHEST)) {
                        continue;
                    }
                    // Сундук склада стоит у клетки: ищем железные прутья в радиусе 6.
                    for (int dx = -6; dx <= 6; dx++) {
                        for (int dz = -6; dz <= 6; dz++) {
                            BlockPos q = p.offset(dx, 0, dz);
                            if (level.getBlockState(q).is(Blocks.IRON_BARS)) {
                                return new Site(q, p);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
