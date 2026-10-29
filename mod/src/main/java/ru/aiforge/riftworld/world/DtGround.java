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
            // Рев 14.49 — ПОЧЕМУ ДЕРЕВЬЯ НЕ ПОЯВЛЯЛИСЬ В ХОЛМАХ, хотя вид находился.
            // Над каждой грядкой лаванды стоит блок-куст (он в теге replaceable_by_trees, поэтому
            // «место свободно»), DT начинал растить ствол в занятую клетку и молча возвращал
            // FAIL_GENERATION. Дереву нужна НАСТОЯЩАЯ пустая клетка над почвой, а не «заменяемая».
            // Плюс почва должна быть объявлена в DT (иначе FAIL_SOIL, как было у известняка):
            // проверяем это через SoilHelper, чтобы не полагаться на догадки.
            p.move(0, 1, 0);
            // Рев 15.4/15.10: в Фиолетовых холмах над клеткой луга стоят ДВА растения (лаванда и
            // цветок-макушка), а DT перед посадкой проверяет, что над грунтом свободны
            // `signal_energy` блоков (`Species.shouldGenerate` → `validTreePos`). Строгое «только
            // воздух» отбрасывало все колонки луга (`ground=-`), а снятие одной клетки оставляло
            // цветок выше — дерево всё равно не сажалось. Теперь снимаем до четырёх растений
            // подряд и требуем, чтобы над ними был настоящий воздух/вода.
            boolean free;
            int cleared = 0;
            if (sea) {
                BlockState above = level.getBlockState(p);
                free = above.isAir() || !above.getFluidState().isEmpty();
            } else {
                free = false;
                for (int i = 0; i < 4; ++i) {
                    BlockState above = level.getBlockState(p);
                    if (above.isAir()) { free = true; break; }
                    if (above.getCollisionShape(level, p).isEmpty() && above.canBeReplaced()) {
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        p.move(0, 1, 0);
                        ++cleared;
                        continue;
                    }
                    break;
                }
            }
            p.move(0, -1 - cleared, 0);                     // вернулись к блоку грунта
            if (free && isDtSoil(level, p)) return p.immutable();
        }
        return null;
    }


    // ------------------------------------------------------------------------------------------------
    // Рев 15.11 — САЖАЕМ ДЕРЕВЬЯ DT САМИ.
    //
    // Фича DT (`dynamictrees:dynamic_tree`) приходит в биом через биом-модификатор и физически
    // выполняется (мы вызываем `super.applyBiomeDecoration`), но её конвейер (диски Пуассона →
    // heightmap → GroundFinder → селектор) на нашем генераторе не даёт деревьев: см. `/riftdtdbg`.
    // Поэтому после ванильного прохода мы сажаем деревья аддона САМИ — тем же публичным API DT:
    // `LevelContext.create` → `TreeRegistry.findSpecies` → `GenerationContext` → `Species.generate`.
    // Дерево получается настоящим динамическим (растёт, даёт семена, листва — наши блоки).
    // ------------------------------------------------------------------------------------------------

    /** Кэш «вид по имени»: имена маленькие, а вызываем часто (по чанку на биом). */
    private static final java.util.Map<String, Object> SPECIES = new java.util.concurrent.ConcurrentHashMap<>();

    /** Вид DT по строке {@code namespace:path} или {@code null}, если DT/вид недоступны. */
    private static Object species(String id) {
        if (!ModList.get().isLoaded("dynamictrees")) return null;
        Object cached = SPECIES.get(id);
        if (cached != null) return cached;
        try {
            Class<?> treeRegistry = Class.forName("com.ferreusveritas.dynamictrees.api.TreeRegistry");
            Object sp = treeRegistry.getMethod("findSpecies", net.minecraft.resources.ResourceLocation.class)
                    .invoke(null, new net.minecraft.resources.ResourceLocation(id));
            if (sp == null) return null;
            SPECIES.put(id, sp);
            return sp;
        } catch (Throwable t) {
            SPECIES.put(id, NULL_SPECIES);
            return null;
        }
    }

    private static final Object NULL_SPECIES = new Object();

    /**
     * Посадить ОДНО динамическое дерево на готовом грунте. Возвращает {@code true}, если DT поставил
     * дерево (корни, ствол, листва — блоки аддона {@code riftworlddt}).
     *
     * @param radius радиус диска DT (2..8): под него подбирается JoCode-форма
     */
    public static boolean plant(LevelAccessor level, BlockPos ground, String speciesId, int radius) {
        Object sp = species(speciesId);
        if (sp == null || sp == NULL_SPECIES) return false;
        try {
            Class<?> lcC = Class.forName("com.ferreusveritas.dynamictrees.util.LevelContext");
            Object lc = lcC.getMethod("create", LevelAccessor.class).invoke(null, level);
            Class<?> spC = Class.forName("com.ferreusveritas.dynamictrees.tree.species.Species");
            Class<?> gcC = Class.forName("com.ferreusveritas.dynamictrees.worldgen.GenerationContext");
            Class<?> bndC = Class.forName("com.ferreusveritas.dynamictrees.util.SafeChunkBounds");
            Object bounds = bndC.getField("ANY_WG").get(null);
            Object ctx = gcC.getConstructor(lcC, spC, BlockPos.class, BlockPos.MutableBlockPos.class,
                            net.minecraft.core.Holder.class, net.minecraft.core.Direction.class, int.class, bndC)
                    .newInstance(lc, sp, ground, ground.mutable(), level.getBiome(ground),
                            net.minecraft.core.Direction.NORTH,
                            Math.max(2, Math.min(8, radius)), bounds);
            return Boolean.TRUE.equals(spC.getMethod("generate", gcC).invoke(sp, ctx));
        } catch (Throwable t) {
            return false;
        }
    }

    /** Вид для биома по его ключу; {@code null} — в этом биоме аддон ничего не сажает. */
    private static String speciesFor(String biome, java.util.Random r) {
        if (biome.endsWith(":violet_hills")) return "riftworlddt:violet";
        if (biome.endsWith(":sizo_grove")) return r.nextInt(5) == 0 ? "riftworlddt:giant_sizo" : "riftworlddt:sizo";
        if (biome.endsWith(":waterfall_gardens")) return "riftworlddt:sizo";
        if (biome.endsWith(":shallow_reef")) {
            int k = r.nextInt(10);
            return k < 5 ? "riftworlddt:coral_staghorn" : k < 8 ? "riftworlddt:coral_table" : "riftworlddt:coral_whip";
        }
        return null;
    }

    /**
     * Детерминированная посадка в чанке: до трёх попыток, вид и вероятность — по биому.
     * Вызывается из {@code ElisarChunkGenerator.applyBiomeDecoration} после ванильных фич.
     */
    public static int plantChunk(LevelAccessor level, net.minecraft.world.level.ChunkPos pos) {
        if (!ModList.get().isLoaded("dynamictrees")) return 0;
        java.util.Random r = new java.util.Random(pos.toLong() * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
        int planted = 0;
        for (int i = 0; i < 3; ++i) {
            // только внутренние 8x8 чанка: крона радиусом до 8 блоков не вылезет за соседний чанк,
            // иначе WorldGenRegion может бросить «setBlock in a far chunk»
            int x = pos.getMinBlockX() + 4 + r.nextInt(8), z = pos.getMinBlockZ() + 4 + r.nextInt(8);
            BlockPos start = new BlockPos(x, 0, z);
            var biome = level.getBiome(start);
            String id = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
            String sp = speciesFor(id, r);
            if (sp == null) continue;
            boolean sea = sp.startsWith("riftworlddt:coral");
            if (r.nextFloat() > (sea ? 0.30f : 0.55f)) continue;
            BlockPos ground = surfaceUnder(level, start, sea);
            if (ground == null) continue;
            if (plant(level, ground, sp, 3 + r.nextInt(4) * 2)) ++planted;
        }
        return planted;
    }
    /** Объявлен ли блок почвой в Dynamic Trees (реестр SoilHelper), т.е. дерево вообще имеет право тут встать. */
    private static boolean isDtSoil(LevelAccessor level, BlockPos p) {
        if (SOIL_CHECK == null) return true;                 // DT недоступен — не мешаем
        try {
            return (Boolean) SOIL_CHECK.invoke(null, level.getBlockState(p), DIRT_FLAGS);
        } catch (Throwable t) {
            return true;
        }
    }

    private static java.lang.reflect.Method SOIL_CHECK;
    private static Object DIRT_FLAGS;

    static {
        try {
            Class<?> helper = Class.forName("com.ferreusveritas.dynamictrees.block.rooty.SoilHelper");
            DIRT_FLAGS = helper.getMethod("getSoilFlags", String[].class).invoke(null, (Object) new String[]{"dirt_like"});
            SOIL_CHECK = helper.getMethod("isSoilAcceptable",
                    net.minecraft.world.level.block.state.BlockState.class, int.class);
        } catch (Throwable ignored) {
            SOIL_CHECK = null;                                // аддон собирается и без DT
        }
    }
}
