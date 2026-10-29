package ru.aiforge.riftworld.world;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Рев 14.41 — ПОЧЕМУ АДДОН DYNAMIC TREES «НИЧЕГО НЕ МЕНЯЛ».
 *
 * <p>Вид {@code riftworlddt:violet} регистрировался (это видно в пробах), но в мире не появлялось
 * ни одного дерева: DT ищет место посадки через {@code GroundFinder}, а по умолчанию берёт позицию
 * из {@code WORLD_SURFACE}-heightmap. У нас <b>свой</b> генератор: рельеф пишется в чанк напрямую,
 * и WG-heightmap на момент фич пустой/устаревший. DT «находил грунт» в воздухе, проверка почвы
 * проваливалась, и генерация молча возвращала {@code FAIL_SOIL} — ни деревьев, ни кораллов.
 * Ручная {@code /dynamictrees settree} работала, потому что там позиция задаётся игроком.
 *
 * <p>Лечение: регистрируем для измерения {@code riftworld:elisar} СВОЙ {@code GroundFinder},
 * который не верит heightmap, а честно сканирует колонку сверху вниз и отдаёт первый твёрдый блок
 * с двумя блоками воздуха над ним (лаванда, цветы и листва грунтом не считаются).
 *
 * <p>Делается это в ОСНОВНОМ моде, а не в аддоне: аддон собирается отдельной задачей {@code dtJar}
 * без реобфускации, поэтому любые обращения к классам Minecraft из него падают с
 * {@code NoSuchFieldError}/{@code NoSuchMethodError} (ловушка, на которой рев 14.40 и сгорел).
 * Здесь же обычный код мода — имена полей Minecraft корректные. Класс DT грузим рефлексией,
 * потому что Dynamic Trees подключён только как {@code runtimeOnly}.
 */
public final class DtGround {
    private static final Logger LOG = LoggerFactory.getLogger("riftworld");

    private DtGround() {
    }

    /** Вызывается из {@code RiftWorld}: ставит поиск грунта для нашего измерения, если DT загружен. */
    public static void install() {
        if (!ModList.get().isLoaded("dynamictrees")) return;
        try {
            Class<?> gf = Class.forName("com.ferreusveritas.dynamictrees.api.worldgen.GroundFinder");
            Object finder = Proxy.newProxyInstance(gf.getClassLoader(), new Class<?>[]{gf}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "findGround": {
                        LevelAccessor level = (LevelAccessor) args[0];
                        BlockPos start = (BlockPos) args[1];
                        Object hm = args.length > 2 ? args[2] : null;
                        boolean sea = hm != null && String.valueOf(hm).contains("OCEAN_FLOOR");
                        BlockPos ground = surfaceUnder(level, start, sea);
                        return ground == null ? List.<BlockPos>of() : List.of(ground);
                    }
                    case "toString":
                        return "riftworld:surface-scan";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        return null;
                }
            });
            Method reg = gf.getMethod("registerGroundFinder", net.minecraft.resources.ResourceKey.class, gf);
            reg.invoke(null, Flight.ELISAR_DIM, finder);
            LOG.info("[riftworld] DT GroundFinder для riftworld:elisar установлен: грунт ищется сканом колонки");
        } catch (Throwable t) {
            LOG.warn("[riftworld] DT GroundFinder не установлен ({}): деревья DT будут искать грунт по heightmap", t.toString());
        }
    }

    /**
     * Верхний твёрдый блок колонки, над которым есть место под ствол. Листва, цветы, лаванда и
     * «грядки» грунтом не считаются: дерево должно стоять на земле, а не на кусте.
     *
     * <p>{@code sea} — правило подводной посадки ({@code OCEAN_FLOOR_WG}): у кораллов DT над грунтом
     * законно стоит вода, поэтому «воздух сверху» там не требуется.
     */
    /** Публичная обёртка для проб: что наш поиск грунта считает землёй под этой точкой. */
    public static BlockPos groundFor(LevelAccessor level, BlockPos start) {
        return surfaceUnder(level, start, false);
    }

    private static BlockPos surfaceUnder(LevelAccessor level, BlockPos start, boolean sea) {
        if (start == null) return null;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(start.getX(), 0, start.getZ());
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); --y) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            if (!s.isCollisionShapeFullBlock(level, p)) continue;
            var id = ForgeRegistries.BLOCKS.getKey(s.getBlock());
            if (id != null) {
                String path = id.getPath();
                // цветы/лаванда/листва/высокая трава — не грунт (иначе дерево встаёт на куст)
                if (path.contains("leaves") || path.contains("bloom") || path.contains("lavender")
                        || path.endsWith("_bed") || path.contains("tall_grass") || path.contains("flowers")) continue;
            }
            // Рев 14.45: хватит ОДНОЙ свободной клетки над грунтом. Требование двух подряд
            // отбрасывало грядки лаванды (блок-куст + цветок сверху) и луговую траву: DT уходил
            // вниз по колонке и «находил грунт» в скале под рекой — почва не подходила, дерево
            // не ставилось (ровно то, на что жалуется владелец).
            p.move(0, 1, 0);
            if (fits(level, p, sea)) return p.below().immutable();
            p.move(0, -1, 0);
        }
        return null;
    }

    /** Годится ли клетка под ствол: воздух, растения из тега replaceable_by_trees, а под водой — ещё и вода. */
    private static boolean fits(LevelAccessor level, BlockPos p, boolean sea) {
        BlockState s = level.getBlockState(p);
        if (sea) return s.isAir() || !s.getFluidState().isEmpty();
        return s.isAir() || s.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES);
    }
}
