package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * v0.21.0: 7 новых уникальных мировых структур Элисара в общей генерации мира:
 * <ol>
 *   <li><b>#10 Пристань-Порт</b> ({@link HarborPort}) — белокаменная гавань с таможенным домом, складом,
 *       поворотным портовым краном, деревянными причалами и пришвартованной торговой каравеллой;</li>
 *   <li><b>#8 Античный Театр</b> ({@link GrandTheater}) — парящий остров с полукруглым мраморным амфитеатром,
 *       мозаичной орхестрой и двухъярусной колоннадой скены ({@code Scaenae Frons});</li>
 *   <li><b>#7 Морской Маяк</b> ({@link SeaLighthouse}) — скалистый морской риф с домом смотрителя и высокой
 *       31-блочной конической башней маяка с винтовой полосой и хрустально-огненным фонарём;</li>
 *   <li><b>#5 Алтарь Векового Древа и Каменный Круг Корней</b> ({@link SiteKind#ANCIENT_TREE_ALTAR}) — в Роще Сизо;</li>
 *   <li><b>#4 Пепельный Маяк-Колокольня и Курган Павших Стражей</b> ({@link SiteKind#ASHEN_BELFRY_BARROW}) — в Адских угодьях;</li>
 *   <li><b>Расколотая Кузня Осколков (Резонаторная Мастерская)</b> ({@link SiteKind#SHATTERED_SHARD_FORGE}) — в Хрустальных разломах;</li>
 *   <li><b>Обсерватория Фиолетовых Ветров и Воздушная Пристань</b> ({@link SiteKind#VIOLET_WIND_OBSERVATORY}) — в Фиолетовых холмах.</li>
 * </ol>
 */
public final class LandmarkStructures {
    public static final int THEATER_KIND = 11;
    public static final int HARBOR_COAST_KIND = 12;

    public static final net.minecraft.resources.ResourceLocation LOOT_MODEST_WAYFARER =
            new net.minecraft.resources.ResourceLocation("riftworld", "chests/modest_wayfarer");
    public static final net.minecraft.resources.ResourceLocation LOOT_MODEST_MARITIME =
            new net.minecraft.resources.ResourceLocation("riftworld", "chests/modest_maritime");

    public static final int SUPER_THEATER = 920;
    public static final int SUPER_HARBOR = 820;
    public static final int SUPER_LIGHTHOUSE = 780;

    private static final long SEED_THEATER = 0x7EA7E11L;
    private static final long SEED_HARBOR = 0x4A2B010L;
    private static final long SEED_LIGHTHOUSE = 0x1164707L;
    private static final long SEED_BIOME_LANDMARK = 0xB103E77L;

    public enum SiteKind {
        ANCIENT_TREE_ALTAR,
        ASHEN_BELFRY_BARROW,
        ASHEN_BURNED_CHURCH,
        SHATTERED_SHARD_FORGE,
        VIOLET_WIND_OBSERVATORY
    }

    public static final class GrandTheater {
        public final int cx, cz, baseY;
        public final double flatR;
        public final CityPlan.Island island;

        GrandTheater(int cx, int cz, int baseY, double flatR, CityPlan.Island island) {
            this.cx = cx;
            this.cz = cz;
            this.baseY = baseY;
            this.flatR = flatR;
            this.island = island;
        }
    }

    public static final class HarborPort {
        public final int cx, cz, quayY, floorY;
        public final Direction seaDir;
        public final double reachR;
        public final CityPlan.Island island;

        HarborPort(int cx, int cz, int quayY, int floorY, Direction seaDir, double reachR, CityPlan.Island island) {
            this.cx = cx;
            this.cz = cz;
            this.quayY = quayY;
            this.floorY = floorY;
            this.seaDir = seaDir;
            this.reachR = reachR;
            this.island = island;
        }
    }

    public static final class SeaLighthouse {
        public final int cx, cz, rockTopY, lanternY, floorY;
        public final double reachR;

        SeaLighthouse(int cx, int cz, int rockTopY, int lanternY, int floorY, double reachR) {
            this.cx = cx;
            this.cz = cz;
            this.rockTopY = rockTopY;
            this.lanternY = lanternY;
            this.floorY = floorY;
            this.reachR = reachR;
        }
    }

    public static final class BiomeLandmark {
        public final SiteKind kind;
        public final int cx, cz, baseY;
        public final double dirX, dirZ;
        public final double reachR;

        BiomeLandmark(SiteKind kind, int cx, int cz, int baseY, double dirX, double dirZ, double reachR) {
            this.kind = kind;
            this.cx = cx;
            this.cz = cz;
            this.baseY = baseY;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.reachR = reachR;
        }
    }

    private static final ConcurrentHashMap<Long, Optional<GrandTheater>> THEATER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<HarborPort>> HARBOR_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<SeaLighthouse>> LIGHTHOUSE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<BiomeLandmark>> SIZO_ALTAR_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<BiomeLandmark>> ASHEN_BELFRY_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<BiomeLandmark>> ASHEN_CHURCH_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<BiomeLandmark>> CRYSTAL_FORGE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<BiomeLandmark>> VIOLET_OBS_CACHE = new ConcurrentHashMap<>();

    private LandmarkStructures() {
    }

    private static long key(int a, int b) {
        return ((long) a << 32) ^ (b & 0xFFFFFFFFL);
    }

    // =========================================================================
    // 1. ПЛАНИРОВЩИКИ КООРДИНАТ ВСЕХ 7 СТРУКТУР (СТРОГИЙ DAG БЕЗ ЦИКЛОВ КЭША!)
    // =========================================================================

    /** #8 Античный Театр на собственном парящем острове (НЕ обращается к WildIslands!). */
    public static GrandTheater grandTheater(int sx, int sz) {
        return THEATER_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genGrandTheater(sx, sz))).orElse(null);
    }

    private static GrandTheater genGrandTheater(int sx, int sz) {
        Random rnd = new Random(SEED_THEATER ^ (sx * 492876847L + sz * 918273645L));
        if (rnd.nextDouble() > 0.66) return null;
        int margin = 135;
        for (int tries = 0; tries < 10; ++tries) {
            int cx = sx * SUPER_THEATER + margin + rnd.nextInt(SUPER_THEATER - 2 * margin);
            int cz = sz * SUPER_THEATER + margin + rnd.nextInt(SUPER_THEATER - 2 * margin);
            if (WorldStructures.overlapsSpecialBiomes(cx, cz, 88)) continue;
            if (WorldStructures.anyVillageAt(cx, cz, 128) != null) continue;

            int baseY = 95 + rnd.nextInt(10);
            double islR = 41.0 + rnd.nextDouble() * 4.5;
            CityPlan.Island isl = new CityPlan.Island(
                    cx, cz, islR, baseY, islR * 0.66 + 13.0,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    THEATER_KIND, 0.36);
            return new GrandTheater(cx, cz, baseY + 1, 23.5, isl);
        }
        return null;
    }

    public static GrandTheater theaterAt(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_THEATER);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_THEATER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                GrandTheater t = grandTheater(sx + dx, sz + dz);
                if (t != null && Math.abs(x - t.cx) <= range && Math.abs(z - t.cz) <= range) return t;
            }
        }
        return null;
    }

    public static List<CityPlan.Island> theaterIslandsNear(double x, double z) {
        List<CityPlan.Island> out = null;
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_THEATER);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_THEATER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                GrandTheater t = grandTheater(sx + dx, sz + dz);
                if (t != null && Math.abs(x - t.island.cx) < t.island.maxReach()
                        && Math.abs(z - t.island.cz) < t.island.maxReach()) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(t.island);
                }
            }
        }
        return out == null ? Collections.emptyList() : out;
    }

    private static boolean openSeaClear(CityPlan plan, int cx, int cz, int radiusCheck) {
        if (WorldStructures.overlapsSpecialBiomes(cx, cz, 58)) return false;
        if (BlacktideCitadel.insideAnyFootprint(cx, cz, 145.0)) return false;
        if (WorldStructures.anyVillageAt(cx, cz, 105) != null) return false;
        if (theaterAt(cx, cz, 105) != null) return false;
        if (WorldStructures.crashedShipNear(cx, cz, 92) != null
                || WorldStructures.sunkenShipNear(cx, cz, 92) != null
                || WorldStructures.oceanBuildingNear(cx, cz, 92) != null
                || WorldStructures.stiltVillageNear(cx, cz, 92) != null) {
            return false;
        }
        for (int dx = -radiusCheck; dx <= radiusCheck; dx += 11) {
            for (int dz = -radiusCheck; dz <= radiusCheck; dz += 11) {
                if (plan.islandAt(cx + dx, cz + dz) != null) return false;
            }
        }
        int fl = ElisarChunkGenerator.oceanFloor(plan, cx, cz);
        return fl <= plan.ocean - 8 && fl >= 2;
    }

    /** #10 Пристань-Порт СТРОГО у берега реального прибрежного острова (НИКОГДА не просто посреди моря!). */
    public static HarborPort harborPort(int sx, int sz) {
        return HARBOR_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genHarborPort(sx, sz))).orElse(null);
    }

    private static HarborPort genHarborPort(int sx, int sz) {
        Random rnd = new Random(SEED_HARBOR ^ (sx * 6364136223846793005L + sz * 1442695040888963407L));
        if (rnd.nextDouble() > 0.68) return null;
        int margin = 135;
        CityPlan plan = CityPlan.get();
        for (int tries = 0; tries < 16; ++tries) {
            int cx = sx * SUPER_HARBOR + margin + rnd.nextInt(SUPER_HARBOR - 2 * margin);
            int cz = sz * SUPER_HARBOR + margin + rnd.nextInt(SUPER_HARBOR - 2 * margin);
            if (Math.hypot(cx, cz) < 560) continue;
            if (Math.hypot(cx - 640, cz - 690) < 200 || Math.hypot(cx - 696, cz - 696) < 200) continue;
            // Строгий DAG без вызова plan.islandAt / WildIslands внутри computeIfAbsent:
            if (WorldStructures.overlapsSpecialBiomes(cx, cz - 20, 105)) continue;
            if (WorldStructures.anyVillageAt(cx, cz - 18, 145) != null) continue;
            if (theaterAt(cx, cz - 18, 145) != null) continue;
            int floorY = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, cx, cz));
            if (floorY > plan.ocean - 6) continue;

            // Прибрежный зелёный остров, поднимающийся прямо со дна моря сразу за каменной набережной порта (cz - 34)!
            double islR = 32.5 + rnd.nextDouble() * 4.5;
            int islBaseY = plan.ocean + 7;
            CityPlan.Island coastIsland = new CityPlan.Island(
                    cx, cz - 34, islR, islBaseY, islR * 0.55 + 14.0,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    HARBOR_COAST_KIND, 0.34);
            return new HarborPort(cx, cz, plan.ocean + 2, floorY, Direction.SOUTH, 28.0, coastIsland);
        }
        return null;
    }

    public static List<CityPlan.Island> harborIslandsNear(double x, double z) {
        List<CityPlan.Island> out = null;
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_HARBOR);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_HARBOR);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                HarborPort h = harborPort(sx + dx, sz + dz);
                if (h != null && h.island != null
                        && Math.abs(x - h.island.cx) < h.island.maxReach()
                        && Math.abs(z - h.island.cz) < h.island.maxReach()) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(h.island);
                }
            }
        }
        return out == null ? Collections.emptyList() : out;
    }

    public static HarborPort harborNear(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_HARBOR);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_HARBOR);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                HarborPort h = harborPort(sx + dx, sz + dz);
                if (h != null && Math.abs(x - h.cx) <= range && Math.abs(z - h.cz) <= range) return h;
            }
        }
        return null;
    }

    /** #7 Высокий Морской Маяк на скалистом рифе. */
    public static SeaLighthouse seaLighthouse(int sx, int sz) {
        return LIGHTHOUSE_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genSeaLighthouse(sx, sz))).orElse(null);
    }

    private static SeaLighthouse genSeaLighthouse(int sx, int sz) {
        Random rnd = new Random(SEED_LIGHTHOUSE ^ (sx * 2862933555777941757L + sz * 7046029254386353131L));
        if (rnd.nextDouble() > 0.72) return null;
        int margin = 110;
        CityPlan plan = CityPlan.get();
        for (int tries = 0; tries < 16; ++tries) {
            int cx = sx * SUPER_LIGHTHOUSE + margin + rnd.nextInt(SUPER_LIGHTHOUSE - 2 * margin);
            int cz = sz * SUPER_LIGHTHOUSE + margin + rnd.nextInt(SUPER_LIGHTHOUSE - 2 * margin);
            if (!openSeaClear(plan, cx, cz, 26)) continue;
            if (harborNear(cx, cz, 110) != null) continue;
            int floorY = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, cx, cz));
            int rockTop = plan.ocean + 7;
            return new SeaLighthouse(cx, cz, rockTop, rockTop + 26, floorY, 20.0);
        }
        return null;
    }

    public static SeaLighthouse lighthouseNear(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_LIGHTHOUSE);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_LIGHTHOUSE);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                SeaLighthouse l = seaLighthouse(sx + dx, sz + dz);
                if (l != null && Math.abs(x - l.cx) <= range && Math.abs(z - l.cz) <= range) return l;
            }
        }
        return null;
    }

    /** #5 Алтарь Векового Древа и Каменный Круг Корней в каждом регионе Рощи Сизо. */
    public static BiomeLandmark ancientTreeAltar(SizoGrove.Region sg) {
        if (sg == null) return null;
        return SIZO_ALTAR_CACHE.computeIfAbsent(key(sg.cx, sg.cz), k -> Optional.ofNullable(genAncientTreeAltar(sg))).orElse(null);
    }

    private static BiomeLandmark genAncientTreeAltar(SizoGrove.Region sg) {
        Random rnd = new Random(0x5120A17A4L ^ (sg.cx * 0x9E3779B97F4A7C15L + sg.cz * 0xBF58476D1CE4E5B9L));
        if (rnd.nextDouble() > 0.54) return null; // v0.30.0: случайный шанс < 100% в биоме Рощи Сизо!
        CityPlan plan = CityPlan.get();
        double dxTrail = sg.tbx - sg.tax, dzTrail = sg.tbz - sg.taz;
        double len = Math.max(1.0, Math.hypot(dxTrail, dzTrail));
        double nx = -dzTrail / len, nz = dxTrail / len;

        // Ищем уютную лесную поляну на главном острове в стороне от тропы и башни
        double[] uCands = {0.28, 0.36, 0.22, 0.50};
        double[] sideCands = {22.0, -22.0, 18.0, -18.0, 26.0, -26.0};
        for (double u : uCands) {
            double bx = sg.tax + dxTrail * u;
            double bz = sg.taz + dzTrail * u;
            for (double off : sideCands) {
                int ax = (int) Math.round(bx + nx * off);
                int az = (int) Math.round(bz + nz * off);
                if (sg.hub.t(ax, az) < 0.34) continue;
                if (sg.temple && Math.hypot(ax - sg.tx, az - sg.tz) < 25.0) continue;
                int baseY = (int) Math.floor(sg.hub.raw(ax + 0.5, az + 0.5)) + 1;
                if (baseY <= plan.ocean + 4) continue;
                return new BiomeLandmark(SiteKind.ANCIENT_TREE_ALTAR, ax, az, baseY, -nx * Math.signum(off), -nz * Math.signum(off), 15.0);
            }
        }
        int baseY = (int) Math.floor(sg.hub.raw(sg.cx + 0.5, sg.cz + 0.5)) + 1;
        return new BiomeLandmark(SiteKind.ANCIENT_TREE_ALTAR, sg.cx, sg.cz, Math.max(plan.ocean + 6, baseY), 0.0, 1.0, 15.0);
    }

    /** #4 Пепельный Маяк-Колокольня и Курган Павших Стражей в регионе Адских угодий (если выпал в comboMode!). */
    public static BiomeLandmark ashenBelfryBarrow(AshenLands.Region rg) {
        if (rg == null || !rg.hasBelfry) return null;
        return ASHEN_BELFRY_CACHE.computeIfAbsent(key(rg.cx, rg.cz), k -> {
            double baseAng = rg.hasBurnedChurch
                    ? Math.atan2(rg.churchZ - rg.cz, rg.churchX - rg.cx) + 2.15
                    : 0.42;
            int bestX = rg.cx + 148, bestZ = rg.cz + 34;
            for (int step = 0; step < 16; ++step) {
                double a = baseAng + step * 0.39;
                int ax = (int) Math.round(rg.cx + Math.cos(a) * (rg.R * 0.44));
                int az = (int) Math.round(rg.cz + Math.sin(a) * (rg.R * 0.44));
                if (rg.hasBurnedChurch && Math.hypot(ax - rg.churchX, az - rg.churchZ) < 95.0) continue;
                boolean hitMound = false;
                for (AshenLands.Mound m : rg.mounds) {
                    if (Math.hypot(ax - m.x(), az - m.z()) < m.r() + 26.0) { hitMound = true; break; }
                }
                if (hitMound) continue;
                bestX = ax;
                bestZ = az;
                break;
            }
            int baseY = CityPlan.get().ocean + 2;
            return Optional.of(new BiomeLandmark(SiteKind.ASHEN_BELFRY_BARROW, bestX, bestZ, baseY, -1.0, 0.0, 24.0));
        }).orElse(null);
    }

    /** v0.23.0: Готическая Сгоревшая Пепельная Церковь в регионе Адских угодий (если rg.hasBurnedChurch == true!). */
    public static BiomeLandmark ashenBurnedChurch(AshenLands.Region rg) {
        if (rg == null || !rg.hasBurnedChurch) return null;
        return ASHEN_CHURCH_CACHE.computeIfAbsent(key(rg.cx, rg.cz), k ->
                Optional.of(new BiomeLandmark(SiteKind.ASHEN_BURNED_CHURCH,
                        rg.churchX, rg.churchZ, rg.churchY, 1.0, 0.0, 25.0))
        ).orElse(null);
    }

    /** Расколотая Кузня Осколков (Резонаторная Мастерская) на 3-м острове каждого региона Хрустальных разломов. */
    public static BiomeLandmark shatteredShardForge(CrystalRifts.Region cr) {
        if (cr == null || cr.isles.isEmpty()) return null;
        return CRYSTAL_FORGE_CACHE.computeIfAbsent(key(cr.cx, cr.cz), k -> Optional.ofNullable(genShatteredForge(cr))).orElse(null);
    }

    private static BiomeLandmark genShatteredForge(CrystalRifts.Region cr) {
        Random rnd = new Random(0xC295F026EL ^ (cr.cx * 0x9E3779B97F4A7C15L + cr.cz * 0xBF58476D1CE4E5B9L));
        if (rnd.nextDouble() > 0.54) return null; // v0.30.0: случайный шанс < 100% в биоме Хрустальных разломов!
        // Остров 0 = Колоннада Босса, Остров 1 = чистый кристальный остров (isl1_trees=0), поэтому ставим на Остров 2!
        CityPlan.Island isl = cr.isles.get(Math.min(2, cr.isles.size() - 1));
        CityPlan plan = CityPlan.get();
        int bestX = (int) Math.round(isl.cx);
        int bestZ = (int) Math.round(isl.cz);
        // Выбираем ровную площадку на плато острова 2 в стороне от гигантских наклонных кристаллов
        outer:
        for (int r = 0; r <= 18; r += 6) {
            int steps = r == 0 ? 1 : 8;
            for (int i = 0; i < steps; ++i) {
                double a = (Math.PI * 2.0 * i) / steps;
                int tx = (int) Math.round(isl.cx + Math.cos(a) * r);
                int tz = (int) Math.round(isl.cz + Math.sin(a) * r);
                if (isl.t(tx, tz) < 0.36) continue;
                boolean hitSpire = false;
                for (CrystalRifts.Spire sp : cr.spires) {
                    if (Math.hypot(tx - sp.ox, tz - sp.oz) < 11.0) {
                        hitSpire = true;
                        break;
                    }
                }
                if (!hitSpire) {
                    bestX = tx;
                    bestZ = tz;
                    break outer;
                }
            }
        }
        int rawTop = (int) Math.floor(isl.raw(bestX + 0.5, bestZ + 0.5));
        int baseY = Math.max(plan.ocean + 8, rawTop + 1);
        return new BiomeLandmark(SiteKind.SHATTERED_SHARD_FORGE, bestX, bestZ, baseY, 0.0, 1.0, 14.5);
    }

    /** Обсерватория Фиолетовых Ветров и Воздушная Пристань у обрыва 3-го острова Фиолетовых холмов. */
    public static BiomeLandmark violetWindObservatory(VioletHills.Region vh) {
        if (vh == null || vh.isles.isEmpty()) return null;
        return VIOLET_OBS_CACHE.computeIfAbsent(key(vh.cx, vh.cz), k -> Optional.ofNullable(genVioletObservatory(vh))).orElse(null);
    }

    private static BiomeLandmark genVioletObservatory(VioletHills.Region vh) {
        Random rnd = new Random(0x7101E70B5L ^ (vh.cx * 0x9E3779B97F4A7C15L + vh.cz * 0xBF58476D1CE4E5B9L));
        if (rnd.nextDouble() > 0.54) return null; // v0.30.0: случайный шанс < 100% в биоме Фиолетовых холмов!
        CityPlan.Island isl = vh.isles.get(Math.min(2, vh.isles.size() - 1));
        CityPlan plan = CityPlan.get();
        // Направляем воздушную пристань наружу от центра архипелага к открытому небу над морем!
        double ux = isl.cx - vh.cx, uz = isl.cz - vh.cz;
        double ulen = Math.hypot(ux, uz);
        if (ulen < 1.0) {
            ux = 1.0;
            uz = 0.0;
        } else {
            ux /= ulen;
            uz /= ulen;
        }
        // Находим кромку обрыва острова вдоль луча (ux, uz) и ставим ротонду обсерватории в 11 блоках от края,
        // чтобы деревянная Воздушная Пристань выходила прямо за край скалы в открытое небо!
        double rimD = isl.R * 0.65;
        for (double d = 10.0; d <= isl.maxReach(); d += 2.0) {
            if (isl.t(isl.cx + ux * d, isl.cz + uz * d) > 0.06) {
                rimD = d;
            }
        }
        double obsD = Math.max(8.0, rimD - 11.5);
        int ox = (int) Math.round(isl.cx + ux * obsD);
        int oz = (int) Math.round(isl.cz + uz * obsD);
        int baseY = Math.max(plan.ocean + 10, ElisarChunkGenerator.violetTop(isl, ox + 0.5, oz + 0.5, plan.ocean) + 1);
        return new BiomeLandmark(SiteKind.VIOLET_WIND_OBSERVATORY, ox, oz, baseY, ux, uz, 22.0);
    }

    public static List<BiomeLandmark> biomeLandmarksNear(double x, double z, double extraMargin) {
        List<BiomeLandmark> out = null;
        SizoGrove.Region sg = SizoGrove.regionAt(x, z);
        if (sg != null) {
            BiomeLandmark lm = ancientTreeAltar(sg);
            if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) {
                out = new ArrayList<>(2);
                out.add(lm);
            }
        }
        AshenLands.Region ar = AshenLands.regionAt(x, z);
        if (ar != null) {
            BiomeLandmark belfry = ashenBelfryBarrow(ar);
            if (belfry != null && Math.hypot(x - belfry.cx, z - belfry.cz) <= belfry.reachR + extraMargin) {
                if (out == null) out = new ArrayList<>(2);
                out.add(belfry);
            }
            BiomeLandmark church = ashenBurnedChurch(ar);
            if (church != null && Math.hypot(x - church.cx, z - church.cz) <= church.reachR + extraMargin) {
                if (out == null) out = new ArrayList<>(2);
                out.add(church);
            }
        }
        int csx = Math.floorDiv((int) Math.floor(x), CrystalRifts.SUPER);
        int csz = Math.floorDiv((int) Math.floor(z), CrystalRifts.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                CrystalRifts.Region cr = CrystalRifts.region(csx + dx, csz + dz);
                if (cr == null) continue;
                BiomeLandmark lm = shatteredShardForge(cr);
                if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(lm);
                }
            }
        }
        int vsx = Math.floorDiv((int) Math.floor(x), VioletHills.SUPER);
        int vsz = Math.floorDiv((int) Math.floor(z), VioletHills.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                VioletHills.Region vh = VioletHills.region(vsx + dx, vsz + dz);
                if (vh == null) continue;
                BiomeLandmark lm = violetWindObservatory(vh);
                if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(lm);
                }
            }
        }
        return out == null ? Collections.emptyList() : out;
    }

    public static BiomeLandmark biomeLandmarkNear(double x, double z, double extraMargin) {
        List<BiomeLandmark> list = biomeLandmarksNear(x, z, extraMargin);
        return list.isEmpty() ? null : list.get(0);
    }

    public static boolean inFootprint(double x, double z, double margin) {
        GrandTheater th = theaterAt(x, z, 28 + margin);
        if (th != null && Math.hypot(x - th.cx, z - th.cz) <= th.flatR + margin) return true;
        HarborPort hp = harborNear(x, z, 32 + margin);
        if (hp != null && Math.abs(x - hp.cx) <= 15.5 + margin
                && z >= hp.cz - 20.5 - margin && z <= hp.cz + 18.0 + margin) return true;
        SeaLighthouse lh = lighthouseNear(x, z, 24 + margin);
        if (lh != null && Math.hypot(x - lh.cx, z - lh.cz) <= lh.reachR + margin) return true;
        return biomeLandmarkNear(x, z, margin) != null;
    }

    // =========================================================================
    // 2. ПОСТРОЙКА ВСЕХ 7 СТРУКТУР В ТЕКУЩЕМ ЧАНКЕ (0 ПОДВОДНЫХ ПУСТОТ sub_air=0!)
    // =========================================================================

    /**
     * Безопасная оценка высоты грунта колонки (НИКОГДА не возвращает {@code Integer.MIN_VALUE} на воде или пепельной равнине!).
     */
    private static int safeGroundY(CityPlan plan, int x, int z, int fallback) {
        ElisarChunkGenerator.Col c = ElisarChunkGenerator.column(plan, x, z);
        if (c.top == Integer.MIN_VALUE || c.top < plan.ocean - 16) return fallback;
        return Math.max(fallback - 12, Math.min(fallback, c.top));
    }

    private static void put(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0,
                            int x, int y, int z, BlockState st) {
        if (x >= x0 && x <= x0 + 15 && z >= z0 && z <= z0 + 15
                && y >= chunk.getMinBuildHeight() + 2 && y < chunk.getMaxBuildHeight() - 1) {
            chunk.setBlockState(mp.set(x, y, z), st, false);
        }
    }

    public static void putLootChest(ChunkAccess chunk, int x0, int z0,
                                    int x, int y, int z, Direction facing, net.minecraft.resources.ResourceLocation table) {
        if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) return;
        BlockPos pos = new BlockPos(x, y, z);
        boolean underwater = y <= CityPlan.get().ocean && !chunk.getFluidState(pos).isEmpty();
        BlockState st = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, facing)
                .setValue(ChestBlock.WATERLOGGED, underwater);
        chunk.setBlockState(pos, st, false);
        ChestBlockEntity be = new ChestBlockEntity(pos, st);
        long lootSeed = (x * 341873128712L) ^ (z * 132897987541L) ^ (y * 42317861L) ^ 0x5DEECE66DL;
        be.setLootTable(table, lootSeed);
        chunk.setBlockEntity(be);
    }

    private static BlockState stair(BlockState base, Direction facing, Half half) {
        return base.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    private static BlockState slabTop(BlockState base) {
        return base.setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static void putLootChest(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                     RandomSource rnd, int x0, int z0,
                                     int x, int y, int z, Direction facing, net.minecraft.resources.ResourceLocation table) {
        putLootChest(chunk, x0, z0, x, y, z, facing, table);
    }

    public static void buildInChunk(WorldGenLevel level, ChunkAccess chunk, RandomSource rnd,
                                    CityPlan plan, int x0, int z0) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        double midX = x0 + 8.0, midZ = z0 + 8.0;

        GrandTheater th = theaterAt(midX, midZ, 40);
        if (th != null) {
            buildGrandTheater(level, chunk, mp, rnd, plan, th, x0, z0);
        }

        HarborPort hp = harborNear(midX, midZ, 44);
        if (hp != null) {
            buildHarborPort(level, chunk, mp, rnd, plan, hp, x0, z0);
        }

        SeaLighthouse lh = lighthouseNear(midX, midZ, 34);
        if (lh != null) {
            buildSeaLighthouse(level, chunk, mp, rnd, plan, lh, x0, z0);
        }

        List<BiomeLandmark> lms = biomeLandmarksNear(midX, midZ, 30);
        for (BiomeLandmark lm : lms) {
            switch (lm.kind) {
                case ANCIENT_TREE_ALTAR -> buildAncientTreeAltar(level, chunk, mp, rnd, plan, lm, x0, z0);
                case ASHEN_BELFRY_BARROW -> buildAshenBelfryBarrow(level, chunk, mp, rnd, plan, lm, x0, z0);
                case ASHEN_BURNED_CHURCH -> buildAshenBurnedChurch(level, chunk, mp, rnd, plan, lm, x0, z0);
                case SHATTERED_SHARD_FORGE -> buildShatteredShardForge(level, chunk, mp, rnd, plan, lm, x0, z0);
                case VIOLET_WIND_OBSERVATORY -> buildVioletWindObservatory(level, chunk, mp, rnd, plan, lm, x0, z0);
            }
        }
    }

    // =========================================================================
    // 3. v0.25.0: ПОЛНОСТЬЮ ПЕРЕСТРОЕННЫЕ С НУЛЯ 8 МИРОВЫХ СТРУКТУР ЭЛИСАРА
    //    (Массивные пилястры и арки, чистые карнизы и своды, 0 блок-шума, sub_air = 0)
    // =========================================================================

    private static BlockState slabBot(BlockState base) {
        return base.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    // -------------------------------------------------------------------------
    // #8 АНТИЧНЫЙ МРАМОРНЫЙ АМФИТЕАТР (Cavea + Мозаичная Орхестра + Scaenae Frons)
    // -------------------------------------------------------------------------
    private static void buildGrandTheater(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                          RandomSource rnd, CityPlan plan, GrandTheater t, int x0, int z0) {
        int cx = t.cx, cz = t.cz, by = t.baseY;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState cracked = TempleBlocks.CRACKED.get().defaultBlockState();
        BlockState ivyBrick = TempleBlocks.IVY.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState pillarY = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tStairs = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState stageWood = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState roofBlueSlab = slabBot(ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState());
        BlockState keepStone = ElisarBlocks.KEEP_STONE.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 25.5) continue;

                // Парковое кольцо у края чаши (r = 21.6 .. 25.5)
                if (r > 21.6) {
                    int gTop = safeGroundY(plan, x, z, by - 1);
                    if (gTop > plan.ocean + 2 && Math.abs(r - 23.6) < 0.7 && ((dx * 19 + dz * 31) & 15) == 0 && dz > -4) {
                        for (int cy = 1; cy <= 6; ++cy) {
                            put(chunk, mp, x0, z0, x, gTop + cy, z,
                                    cy <= 2 ? ElisarBlocks.SIZO_LOG.get().defaultBlockState()
                                            : ElisarBlocks.SIZO_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                        }
                    }
                    continue;
                }

                // Сплошной каменный фундамент чаши до тела острова
                int gBot = safeGroundY(plan, x, z, by - 4);
                for (int y = gBot; y <= by - 1; ++y) {
                    if (y > plan.ocean) {
                        put(chunk, mp, x0, z0, x, y, z, y >= by - 2 ? keepStone : keepMossy);
                    }
                }
                for (int y = by; y <= by + 17; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                }

                // 1) Полукруглая мраморная чаша Cavea (dz >= -2, r = 8.4 .. 21.4) + внешняя аркадная галерея Summa Cavea
                if (dz >= -2 && r >= 8.2) {
                    int tier = Mth.clamp((int) Math.floor((r - 8.2) / 1.9), 0, 6);
                    int seatY = by + 1 + tier;
                    for (int y = by; y < seatY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, (y == by && ((dx + dz) & 7) == 0) ? ivyBrick : marble);
                    }

                    // 5 радиальных лестниц-климаксов (0°, ±45°, ±82°)
                    double ang = Math.atan2(dz, dx); // [0..pi]
                    boolean radialAisle = Math.abs(ang - Math.PI * 0.5) < 0.08
                            || Math.abs(ang - Math.PI * 0.25) < 0.08
                            || Math.abs(ang - Math.PI * 0.75) < 0.08
                            || Math.abs(dz) <= 1;
                    Direction inDir = Math.abs(dx) > dz
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : Direction.NORTH;

                    if (r <= 19.6) {
                        put(chunk, mp, x0, z0, x, seatY, z,
                                radialAisle ? stair(tStairs, inDir, Half.BOTTOM) : floor);
                        // Промежуточный балюстрадный парапет Diazoma на 3-м ярусе
                        if (tier == 3 && !radialAisle && Math.abs(r - 14.1) < 0.55) {
                            put(chunk, mp, x0, z0, x, seatY + 1, z, slabBot(tSlab));
                        }
                    } else {
                        // Внешний венчающий портик Summa Cavea (r = 19.6 .. 21.4) с ритмичными колоннами и карнизом!
                        int baseRimY = by + 7;
                        for (int y = by; y <= baseRimY; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, marble);
                        }
                        // 13 стройных античных колонн по гребню амфитеатра (с поэтичным проломом на восточном краю dx > 14)
                        boolean brokenRim = (dx > 14 && dz < 11);
                        int sect = (int) Math.round(ang / (Math.PI / 12.0));
                        double colAng = sect * (Math.PI / 12.0);
                        double colX = Math.cos(colAng) * 20.4, colZ = Math.sin(colAng) * 20.4;
                        boolean onCol = Math.hypot(dx - colX, dz - colZ) <= 0.78;
                        if (!brokenRim) {
                            if (onCol) {
                                for (int y = baseRimY + 1; y <= baseRimY + 4; ++y) {
                                    put(chunk, mp, x0, z0, x, y, z, grandCol);
                                }
                            }
                            if (Math.abs(r - 20.4) <= 0.95) {
                                put(chunk, mp, x0, z0, x, baseRimY + 5, z, marble);
                                put(chunk, mp, x0, z0, x, baseRimY + 6, z, slabBot(tSlab));
                            }
                        } else if (onCol) {
                            put(chunk, mp, x0, z0, x, baseRimY + 1, z, cracked);
                            put(chunk, mp, x0, z0, x, baseRimY + 2, z, ivyBrick);
                        }
                    }
                    continue;
                }

                // 2) Чистая концентрическая мозаичная Орхестра (dz >= -3, r < 8.2) — 0 случайного мусора!
                if (dz >= -3 && r < 8.2) {
                    BlockState orch;
                    if (dx == 0 && dz == 1) {
                        orch = mosaicCore;
                    } else if (Math.abs(dx) <= 1 && Math.abs(dz - 1) <= 1) {
                        orch = goldRune;
                    } else if (Math.abs(r - 5.2) < 0.65 || (Math.abs(dx) == 0 || Math.abs(dz - 1) == 0) && r < 5.5) {
                        orch = mosaic;
                    } else if (Math.abs(r - 7.4) < 0.6) {
                        orch = goldRune;
                    } else {
                        orch = floor;
                    }
                    put(chunk, mp, x0, z0, x, by, z, orch);
                    if (dx == 0 && dz == 1) {
                        // Невысокий резной мраморный жертвенник Фимела в центре орхестры
                        put(chunk, mp, x0, z0, x, by + 1, z, slabBot(tSlab));
                    }
                    continue;
                }

                // 3) Приподнятый античный просцениум Pulpitum (dz = -9 .. -4, |dx| <= 14)
                if (dz >= -9 && dz <= -4 && Math.abs(dx) <= 14) {
                    boolean frontFace = (dz == -4);
                    boolean sideWing = (Math.abs(dx) >= 13);
                    put(chunk, mp, x0, z0, x, by, z, marble);
                    if (frontFace) {
                        // Рельефный мраморный фасад просцениума с двумя боковыми лестницами из орхестры
                        if (Math.abs(dx) == 6 || Math.abs(dx) == 7) {
                            put(chunk, mp, x0, z0, x, by + 1, z, stair(tStairs, Direction.SOUTH, Half.BOTTOM));
                        } else {
                            put(chunk, mp, x0, z0, x, by + 1, z, (Math.abs(dx) % 3 == 0) ? pillarY : mosaic);
                            put(chunk, mp, x0, z0, x, by + 2, z, slabBot(tSlab));
                        }
                    } else if (sideWing) {
                        for (int y = by + 1; y <= by + 7; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y == by + 7 ? slabBot(tSlab) : marble);
                        }
                    } else {
                        put(chunk, mp, x0, z0, x, by + 1, z, stageWood);
                    }
                    continue;
                }

                // 4) Монументальная двухъярусная мраморная скена Scaenae Frons (dz = -14 .. -10, |dx| <= 14)
                if (dz >= -14 && dz <= -10 && Math.abs(dx) <= 14) {
                    for (int y = by; y <= by + 1; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, marble);
                    }
                    boolean portaRegia = (Math.abs(dx) <= 2);
                    boolean portaHosp = (Math.abs(Math.abs(dx) - 8) <= 1);
                    // Живописный облом верхнего карниза лишь на самом правом крыле скены
                    int maxWallY = (dx >= 10) ? (by + 9 - (dx - 9)) : (by + 12);

                    if (dz == -10) {
                        // Выступающая колоннада скены (8 парных канелюрных колонн + антаблемент + центральный фронтон)
                        boolean onFrontCol = (Math.abs(dx) == 3 || Math.abs(dx) == 6 || Math.abs(dx) == 10 || Math.abs(dx) == 13);
                        if (onFrontCol) {
                            for (int y = by + 2; y <= Math.min(maxWallY, by + 11); ++y) {
                                put(chunk, mp, x0, z0, x, y, z, (y == by + 7 || y == by + 11) ? marble : grandCol);
                            }
                        }
                        if (by + 7 <= maxWallY && !portaRegia) {
                            put(chunk, mp, x0, z0, x, by + 7, z, marble);
                        }
                        if (by + 11 <= maxWallY) {
                            put(chunk, mp, x0, z0, x, by + 11, z, marble);
                            put(chunk, mp, x0, z0, x, by + 12, z, roofBlueSlab);
                        }
                        // Классический треугольный мраморный фронтон над царским порталом Porta Regia (|dx| <= 5)
                        if (Math.abs(dx) <= 5) {
                            int pedTop = by + 15 - Math.abs(dx) / 2;
                            for (int y = by + 12; y <= pedTop; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, y == pedTop ? roofBlue : (y == by + 13 && dx == 0 ? goldRune : marble));
                            }
                        }
                    } else if (dz >= -13) {
                        // Основная массивная стена Scaenae с тремя сводчатыми римскими порталами
                        for (int y = by + 2; y <= maxWallY; ++y) {
                            if (portaRegia && y <= by + 6 - (dx * dx) / 3) continue;
                            if (portaHosp && y <= by + 5 - Math.abs(Math.abs(dx) - 8)) continue;
                            // Верхние арочные ниши второго яруса
                            if (dz == -11 && y >= by + 8 && y <= by + 10 && (portaRegia || portaHosp)) {
                                if (y == by + 8 && dx == 0) {
                                    put(chunk, mp, x0, z0, x, y, z, lantern);
                                }
                                continue;
                            }
                            BlockState ws = (y == maxWallY && dx >= 9) ? ivyBrick
                                    : (y == by + 7 || y == by + 11) ? mosaic
                                    : marble;
                            put(chunk, mp, x0, z0, x, y, z, ws);
                        }
                        if (maxWallY >= by + 12) {
                            put(chunk, mp, x0, z0, x, by + 13, z, dz == -12 ? roofBlue : roofBlueSlab);
                        }
                    } else {
                        // Задняя контрфорсная стена скены (dz = -14)
                        boolean buttress = (Math.abs(dx) % 4 == 0);
                        if (buttress) {
                            for (int y = by + 2; y <= Math.min(maxWallY, by + 10); ++y) {
                                put(chunk, mp, x0, z0, x, y, z, y <= by + 3 ? ivyBrick : marble);
                            }
                        }
                    }
                }
            }
        }

        // 2 скромных сундука за кулисами скены (Postscaenium)
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 5, by + 2, cz - 12, Direction.SOUTH, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 5, by + 2, cz - 12, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // #10 БЕЛОКАМЕННАЯ ПРИСТАНЬ-ПОРТ У ОСТРОВА (Таможня + Склад + 2 Пирса + Каравелла)
    // -------------------------------------------------------------------------
    private static void buildHarborPort(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                        RandomSource rnd, CityPlan plan, HarborPort hp, int x0, int z0) {
        int cx = hp.cx, cz = hp.cz, qy = hp.quayY, fy = hp.floorY;
        int ocean = plan.ocean;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState keepStone = ElisarBlocks.KEEP_STONE.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState pillarY = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tStairs = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState darkLogY = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState sizoPlank = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState roofBlueSlab = slabBot(ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState());
        BlockState glass = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        BlockState whiteSail = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState redTrim = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int lx = x - cx, lz = z - cz;
                if (Math.abs(lx) > 19 || lz < -29 || lz > 18) continue;

                // Расчищаем ТОЛЬКО воздух выше уровня моря (y >= ocean + 1), чтобы sub_air СТРОГО равнялся 0!
                if (Math.abs(lx) <= 17 && lz >= -24 && lz <= 17) {
                    for (int y = Math.max(ocean + 1, qy + 1); y <= qy + 16; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, air);
                    }
                }

                // 1) Монументальная каменная набережная (lx = -16..16, lz = -22..-4), монолитная от дна моря fy до qy!
                if (Math.abs(lx) <= 16 && lz >= -22 && lz <= -4) {
                    for (int y = fy; y <= qy - 1; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y <= ocean ? keepMossy : keepStone);
                    }
                    // Набережная мостовая с чистыми каменными бордюрами и швартовными тумбами
                    boolean seaEdge = (lz == -4);
                    boolean paveGrid = ((lx + 16) % 4 == 0 || (lz + 22) % 4 == 0);
                    put(chunk, mp, x0, z0, x, qy, z, seaEdge ? marble : (paveGrid ? keepStone : floor));
                    if (seaEdge && Math.abs(lx) % 6 == 0) {
                        put(chunk, mp, x0, z0, x, qy + 1, z, pillarY);
                        if (Math.abs(lx) == 12) put(chunk, mp, x0, z0, x, qy + 2, z, lantern);
                    }
                }

                // Широкая парадная каменная лестница с набережной прямо на зелёный прибрежный остров (lz = -28..-23, |lx| <= 4)
                if (Math.abs(lx) <= 4 && lz >= -28 && lz <= -23) {
                    int stepY = qy + (-22 - lz);
                    for (int y = fy; y < stepY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y <= ocean ? keepMossy : keepStone);
                    }
                    if (Math.abs(lx) == 4) {
                        put(chunk, mp, x0, z0, x, stepY, z, marble);
                        put(chunk, mp, x0, z0, x, stepY + 1, z, slabBot(tSlab));
                    } else {
                        put(chunk, mp, x0, z0, x, stepY, z, stair(tStairs, Direction.NORTH, Half.BOTTOM));
                    }
                    for (int y = stepY + 2; y <= stepY + 7; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, air);
                    }
                }

                // 2) Здание Таможни и Дома Капитана Порта (lx = -15..-3, lz = -20..-9): 2 этажа, каменная аркада + синяя вальмовая крыша
                if (lx >= -15 && lx <= -3 && lz >= -20 && lz <= -9) {
                    boolean wWall = (lx == -15 || lx == -3 || lz == -20 || lz == -9);
                    boolean corner = (lx == -15 || lx == -3) && (lz == -20 || lz == -9);
                    for (int y = qy + 1; y <= qy + 8; ++y) {
                        if (!wWall) {
                            if (y == qy + 5) put(chunk, mp, x0, z0, x, y, z, sizoPlank);
                            continue;
                        }
                        // Открытая стрельчатая аркада 1-го этажа на южном фасаде к гавани (lz == -9)
                        if (lz == -9 && y <= qy + 3 && (Math.abs(lx + 9) <= 1 || Math.abs(lx + 12) == 0 || Math.abs(lx + 6) == 0)) {
                            continue;
                        }
                        // Высокие остеклённые окна 2-го этажа
                        if (!corner && y >= qy + 6 && y <= qy + 7 && ((lx + 9) % 3 == 0 || (lz + 14) % 3 == 0)) {
                            put(chunk, mp, x0, z0, x, y, z, glass);
                            continue;
                        }
                        BlockState bs = corner ? pillarY : (y == qy + 5 || y == qy + 8 ? keepStone : marble);
                        put(chunk, mp, x0, z0, x, y, z, bs);
                    }
                    // Четырёхскатная синяя черепичная крыша (qy + 9 .. qy + 12)
                    int inset = Math.min(Math.min(lx - (-15), (-3) - lx), Math.min(lz - (-20), (-9) - lz));
                    int roofY = qy + 9 + Math.min(3, inset / 2);
                    for (int y = qy + 9; y <= roofY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y == roofY ? roofBlue : sizoPlank);
                    }
                    if (inset % 2 == 1 && inset < 6) {
                        put(chunk, mp, x0, z0, x, roofY + 1, z, roofBlueSlab);
                    }
                }

                // 3) Каменный Портовый Пакгауз и Сигнальная Башня Гавани (lx = 4..15, lz = -20..-10)
                if (lx >= 4 && lx <= 15 && lz >= -20 && lz <= -10) {
                    boolean inTower = (lx >= 11 && lz >= -14);
                    if (inTower) {
                        boolean tWall = (lx == 11 || lx == 15 || lz == -14 || lz == -10);
                        boolean tCorner = (lx == 11 || lx == 15) && (lz == -14 || lz == -10);
                        for (int y = qy + 1; y <= qy + 12; ++y) {
                            if (tWall) {
                                if (!tCorner && y >= qy + 9 && y <= qy + 10) {
                                    if (y == qy + 9 && lx == 13 && lz == -12) put(chunk, mp, x0, z0, x, y, z, lantern);
                                    continue;
                                }
                                put(chunk, mp, x0, z0, x, y, z, tCorner ? pillarY : marble);
                            } else if (y == qy + 8 || y == qy + 12) {
                                put(chunk, mp, x0, z0, x, y, z, darkPlank);
                            }
                        }
                        int tDist = Math.max(Math.abs(lx - 13), Math.abs(lz + 12));
                        put(chunk, mp, x0, z0, x, qy + 13 + (2 - tDist), z, roofBlue);
                    } else {
                        boolean wWall = (lx == 4 || lx == 15 || lz == -20 || lz == -10);
                        for (int y = qy + 1; y <= qy + 6; ++y) {
                            if (!wWall) continue;
                            if (lz == -10 && lx >= 6 && lx <= 9 && y <= qy + 4) continue; // Широкие грузовые ворота пакгауза
                            put(chunk, mp, x0, z0, x, y, z, (lx == 4 || lx == 15) && (lz == -20 || lz == -10) ? pillarY : keepStone);
                        }
                        int gInset = Math.min(lz - (-20), (-10) - lz);
                        int rY = qy + 7 + gInset / 2;
                        for (int y = qy + 7; y <= rY; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y == rY ? roofBlue : darkPlank);
                        }
                    }
                }

                // Аккуратные бочки на грузовом дворе у пакгауза (без разбросанного шума!)
                if ((lx == 5 || lx == 6) && (lz == -7 || lz == -8)) {
                    put(chunk, mp, x0, z0, x, qy + 1, z, barrel);
                }

                // 4) Два капитальных деревянных пирса на монолитных каменных быках (lx = -9..-6 и lx = 6..9, lz = -3..14)
                boolean westPier = (lx >= -9 && lx <= -6 && lz >= -3 && lz <= 14);
                boolean eastPier = (lx >= 6 && lx <= 9 && lz >= -3 && lz <= 12);
                if (westPier || eastPier) {
                    int maxZ = westPier ? 14 : 12;
                    boolean stonePiling = ((lx == -9 || lx == -6 || lx == 6 || lx == 9) && ((lz + 3) % 5 == 0 || lz == maxZ));
                    if (stonePiling) {
                        for (int y = fy; y <= qy - 1; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y <= ocean ? keepMossy : keepStone);
                        }
                        put(chunk, mp, x0, z0, x, qy + 1, z, darkLogY);
                        if (lz == maxZ) put(chunk, mp, x0, z0, x, qy + 2, z, lantern);
                    } else {
                        for (int y = fy; y <= ocean; ++y) {
                            if (chunk.getBlockState(mp.set(x, y, z)).isAir()) {
                                put(chunk, mp, x0, z0, x, y, z, seaWater);
                            }
                        }
                    }
                    put(chunk, mp, x0, z0, x, qy, z, darkPlank);
                }

                // А-образный деревянный портовый кран на корне Восточного пирса (lx = 7..8, lz = -5..-1)
                if (lx == 7 && lz == -4) {
                    for (int y = qy + 1; y <= qy + 8; ++y) put(chunk, mp, x0, z0, x, y, z, darkLogY);
                }
                if (lx == 7 && lz >= -4 && lz <= 0) {
                    int boomY = qy + 8 + (lz + 4) / 2;
                    put(chunk, mp, x0, z0, x, boomY, z, darkPlank);
                    if (lz == 0) {
                        put(chunk, mp, x0, z0, x, boomY - 1, z, Blocks.CHAIN.defaultBlockState());
                        put(chunk, mp, x0, z0, x, boomY - 2, z, lanternHang);
                    }
                }

                // 5) Изящная двухмачтовая торговая Каравелла у Западного пирса (lx = -16..-11, lz = -1..15, sub_air = 0!)
                if (lx >= -16 && lx <= -10 && lz >= -1 && lz <= 15) {
                    double shipZ = lz - 7.0; // -8..+8
                    double shipX = lx + 13.0; // центр борта lx = -13
                    double halfBeam = 2.6 * Math.sqrt(Math.max(0.0, 1.0 - (shipZ * shipZ) / 68.0));
                    if (Math.abs(shipX) <= halfBeam + 0.15) {
                        // Монолитный подводный корпус от ocean - 2 до палубы qy (0 пустот под водой!)
                        int keelBot = Math.max(fy + 1, ocean - 2);
                        for (int y = fy; y < keelBot; ++y) {
                            if (chunk.getBlockState(mp.set(x, y, z)).isAir()) {
                                put(chunk, mp, x0, z0, x, y, z, seaWater);
                            }
                        }
                        for (int y = keelBot; y <= qy; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y == qy ? sizoPlank : darkPlank);
                        }
                        // Плавная седловатость борта (sheer) и кормовая/носовая надстройки
                        boolean gunwale = Math.abs(shipX) >= halfBeam - 0.75 || Math.abs(shipZ) >= 6.8;
                        int sheerTop = (lz <= 2 || lz >= 12) ? qy + 2 : qy + 1;
                        if (gunwale) {
                            for (int y = qy + 1; y <= sheerTop; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, y == sheerTop && (lz == 0 || lz == 14) ? redTrim : darkPlank);
                            }
                        }
                        // Две стройные мачты (lz = 4 и lz = 10) с наполненными ветром белыми парусами
                        if (lx == -13 && (lz == 4 || lz == 10)) {
                            int mastTop = (lz == 4) ? qy + 12 : qy + 10;
                            for (int y = qy + 1; y <= mastTop; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, darkLogY);
                            }
                            put(chunk, mp, x0, z0, x, mastTop + 1, z, redTrim);
                        }
                    }
                    // Наполненные ветром белые паруса на обеих мачтах (без затирания соседней колонкой z!)
                    boolean belly = Math.abs(shipX) <= 1.2;
                    boolean onForeSail = (belly && lz == 5) || (!belly && lz == 4);
                    boolean onMainSail = (belly && lz == 11) || (!belly && lz == 10);
                    if ((onForeSail || onMainSail) && Math.abs(shipX) <= 3.1 && !(lx == -13 && (lz == 4 || lz == 10))) {
                        int sLow = qy + 4, sHigh = onForeSail ? qy + 10 : qy + 9;
                        for (int y = sLow; y <= sHigh; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, whiteSail);
                        }
                    }
                }
            }
        }

        // 2 скромных морских сундука в Таможне и Пакгаузе
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 9, qy + 1, cz - 18, Direction.SOUTH, LOOT_MODEST_MARITIME);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 8, qy + 1, cz - 18, Direction.SOUTH, LOOT_MODEST_MARITIME);
    }

    // -------------------------------------------------------------------------
    // #7 ВЫСОКИЙ МОРСКОЙ МАЯК ФАРОС И ДОМ СМОТРИТЕЛЯ НА СКАЛИСТОМ РИФЕ (sub_air = 0)
    // -------------------------------------------------------------------------
    private static void buildSeaLighthouse(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                           RandomSource rnd, CityPlan plan, SeaLighthouse lh, int x0, int z0) {
        int cx = lh.cx, cz = lh.cz, rTop = lh.rockTopY, fy = lh.floorY;
        int ocean = plan.ocean;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState keepStone = ElisarBlocks.KEEP_STONE.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState basalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState pillarY = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tStairs = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState crimsonBand = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState oxCopper = Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
        BlockState oxSlab = slabBot(Blocks.OXIDIZED_CUT_COPPER_SLAB.defaultBlockState());
        BlockState glass = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        BlockState beaconCore = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 19.5) continue;

                // Расчищаем ТОЛЬКО воздух над водой (y >= ocean + 1), sub_air == 0!
                for (int y = ocean + 1; y <= rTop + 36; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }

                // 1) Скалистый базальтовый риф с каменным волноломом (монолитный от дна fy до rTop!)
                double ang = Math.atan2(dz, dx);
                double reefRim = 13.8 + 1.8 * Math.sin(ang * 3.0 + 0.7) + 1.1 * Math.cos(ang * 5.0);
                boolean onJetty = (dx >= 5 && dx <= 15 && dz >= 3 && dz <= 6);

                if (r <= reefRim) {
                    double u = Mth.clamp((reefRim - r) / 5.2, 0.0, 1.0);
                    int surfY = (int) Math.round(Mth.lerp(u, ocean - 1.0, rTop));
                    if (r <= 10.6) surfY = rTop;
                    for (int y = fy; y <= surfY; ++y) {
                        BlockState rs = (y <= ocean) ? (y <= ocean - 2 ? basalt : keepMossy)
                                : (r <= 10.6 && y == rTop ? floor : keepStone);
                        put(chunk, mp, x0, z0, x, y, z, rs);
                    }
                    // Каменный парапет террасы маяка (r ~ 10.2)
                    if (Math.abs(r - 10.2) < 0.55 && !(dx >= 3 && dz >= 2 && dz <= 6)) {
                        put(chunk, mp, x0, z0, x, rTop + 1, z, slabBot(tSlab));
                    }
                } else if (!onJetty) {
                    for (int y = fy; y <= ocean; ++y) {
                        if (chunk.getBlockState(mp.set(x, y, z)).isAir()) {
                            put(chunk, mp, x0, z0, x, y, z, seaWater);
                        }
                    }
                }

                // Каменная пристань смотрителя и ступени подъёма с воды на террасу (dx = 5..15, dz = 3..6)
                if (onJetty) {
                    int stepY = (dx >= 11) ? (ocean + 1) : Mth.clamp(rTop - (dx - 6), ocean + 1, rTop);
                    for (int y = fy; y <= stepY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y <= ocean ? keepMossy : keepStone);
                    }
                    if (dx >= 7 && dx <= 10 && dz >= 4 && dz <= 5) {
                        put(chunk, mp, x0, z0, x, stepY, z, stair(tStairs, Direction.WEST, Half.BOTTOM));
                    }
                    if (dx == 15 && (dz == 3 || dz == 6)) {
                        put(chunk, mp, x0, z0, x, stepY + 1, z, pillarY);
                        put(chunk, mp, x0, z0, x, stepY + 2, z, lantern);
                    }
                }

                // 2) Каменный Дом Смотрителя с двускатной синей кровлей (dx = -10..-5, dz = -3..4)
                if (dx >= -10 && dx <= -5 && dz >= -3 && dz <= 4) {
                    for (int y = fy; y <= rTop; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y <= ocean ? basalt : keepStone);
                    }
                    boolean cWall = (dx == -10 || dx == -5 || dz == -3 || dz == 4);
                    boolean cCorner = (dx == -10 || dx == -5) && (dz == -3 || dz == 4);
                    for (int y = rTop + 1; y <= rTop + 5; ++y) {
                        if (!cWall) continue;
                        if (dz == 4 && dx == -7 && y <= rTop + 2) continue;
                        if (!cCorner && y == rTop + 3 && (dx == -8 || dz == 0 || dz == 1)) {
                            put(chunk, mp, x0, z0, x, y, z, glass);
                            continue;
                        }
                        put(chunk, mp, x0, z0, x, y, z, cCorner ? pillarY : marble);
                    }
                    int gH = 3 - Math.abs(dx + 7);
                    for (int y = rTop + 6; y <= rTop + 6 + Math.max(0, gH); ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y == rTop + 6 + Math.max(0, gH) ? roofBlue : darkPlank);
                    }
                }

                // 3) Монументальная коническая башня Маяка (центр cx, cz, высота 34 м до rTop + 34!)
                //    Восьмигранный контрфорсный цоколь (dy = 1..5) -> конический ствол с чёткими сигнальными поясами (dy = 6..23)
                //    -> консольная смотровая галерея (dy = 24..25) -> хрустальный фонарь (dy = 26..30) -> медный купол и шпиль (dy = 31..34)!
                if (r <= 6.2) {
                    for (int dy = 1; dy <= 34; ++dy) {
                        int y = rTop + dy;
                        if (dy <= 5) {
                            double baseR = 5.4 - dy * 0.14;
                            boolean buttress = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && r <= baseR + 0.5;
                            if (r <= baseR || buttress) {
                                if (r < 2.4) {
                                    // Внутренняя винтовая лестница башни
                                    if (dx == 0 && dz == 0) {
                                        put(chunk, mp, x0, z0, x, y, z, pillarY);
                                    } else if (((dy + (dx > 0 ? 0 : 2) + (dz > 0 ? 1 : 0)) & 3) == 0) {
                                        put(chunk, mp, x0, z0, x, y, z, slabBot(tSlab));
                                    }
                                } else if (dz > 0 && Math.abs(dx) <= 1 && dy <= 3) {
                                    // Парадный арочный вход с юга
                                } else {
                                    put(chunk, mp, x0, z0, x, y, z, dy == 5 ? floor : keepStone);
                                }
                            }
                        } else if (dy <= 23) {
                            double shaftR = Mth.lerp((dy - 6) / 17.0, 4.5, 3.25);
                            if (r <= shaftR) {
                                if (r < shaftR - 1.25) {
                                    if (dx == 0 && dz == 0) {
                                        put(chunk, mp, x0, z0, x, y, z, pillarY);
                                    } else if (((dy + (dx >= 0 ? 0 : 2) + (dz >= 0 ? 1 : 0)) & 3) == 0) {
                                        put(chunk, mp, x0, z0, x, y, z, slabBot(tSlab));
                                    }
                                } else {
                                    // Узкие арочные окна с тёплым светом по сторонам света
                                    boolean slitWin = ((dx == 0 || dz == 0) && (dy == 10 || dy == 11 || dy == 17 || dy == 18));
                                    if (slitWin) {
                                        put(chunk, mp, x0, z0, x, y, z, glass);
                                    } else {
                                        // Чистые горизонтальные морские сигнальные пояса (белый камень + карминовые пояса)
                                        boolean redCourse = (dy >= 11 && dy <= 13) || (dy >= 19 && dy <= 21);
                                        put(chunk, mp, x0, z0, x, y, z, redCourse ? crimsonBand : marble);
                                    }
                                }
                            }
                        } else if (dy == 24) {
                            // Выступающая консольная каменная смотровая площадка (r <= 4.6)
                            if (r <= 4.6) {
                                put(chunk, mp, x0, z0, x, y, z, r > 3.6 ? slabTop(tSlab) : keepStone);
                            }
                        } else if (dy == 25) {
                            if (r >= 3.6 && r <= 4.6) {
                                put(chunk, mp, x0, z0, x, y, z, slabBot(tSlab));
                            } else if (r <= 3.2) {
                                put(chunk, mp, x0, z0, x, y, z, floor);
                            }
                        } else if (dy <= 29) {
                            // Высокая остеклённая фонарная камера с сияющим морским ядром
                            if (r <= 1.6) {
                                put(chunk, mp, x0, z0, x, y, z, beaconCore);
                            } else if (r <= 3.1) {
                                boolean mullion = (Math.abs(dx) == 2 && Math.abs(dz) == 2) || ((dx == 0 || dz == 0) && dy == 29);
                                put(chunk, mp, x0, z0, x, y, z, mullion ? pillarY : glass);
                            }
                        } else if (dy <= 32) {
                            // Ступенчатый патинированный медный купол фонаря
                            double domeR = 3.3 - (dy - 30) * 1.05;
                            if (r <= domeR) {
                                put(chunk, mp, x0, z0, x, y, z, oxCopper);
                            } else if (r <= domeR + 0.65 && dy == 30) {
                                put(chunk, mp, x0, z0, x, y, z, oxSlab);
                            }
                        } else if (dx == 0 && dz == 0) {
                            // Громоотводный шпиль маяка (dy = 33..34)
                            put(chunk, mp, x0, z0, x, y, z, dy == 34 ? Blocks.LIGHTNING_ROD.defaultBlockState() : pillarY);
                        }
                    }
                }
            }
        }

        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 8, rTop + 1, cz - 1, Direction.SOUTH, LOOT_MODEST_MARITIME);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 1, rTop + 1, cz - 1, Direction.SOUTH, LOOT_MODEST_MARITIME);
    }

    // -------------------------------------------------------------------------
    // #5 СВЯТИЛИЩЕ ВЕКОВОГО ДРЕВА И МРАМОРНЫЙ КРУГ ТРИЛИТОВ В РОЩЕ СИЗО
    //    (Плавные органичные корни и диагональные ветви + резной алтарь без веток-палок!)
    // -------------------------------------------------------------------------
    private static void buildAncientTreeAltar(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                              RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState ivyBrick = TempleBlocks.IVY.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState pillarY = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tStairs = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState moss = ElisarBlocks.MOSS.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState sizoBark = ElisarBlocks.SIZO_LOG.get().defaultBlockState();
        BlockState sizoLeaves = ElisarBlocks.SIZO_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState crystalGlow = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState lanternHang = ElisarBlocks.LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, true);

        // Древо смещено на (-2, -2), чтобы перед ним на (+2, +2) открывался чистый парадный вид на резной Алтарь!
        int tx = cx - 2, tz = cz - 2;

        // 1) Круглая каменная терраса святилища (r <= 12.6) и 8 резных арочных трилитов по кольцу r = 10.6
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 13.2) continue;

                int gBot = safeGroundY(plan, x, z, by - 3);
                for (int y = gBot; y <= by - 1; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, y == by - 1 ? keepMossy : ElisarBlocks.KEEP_STONE.get().defaultBlockState());
                }
                for (int y = by; y <= by + 23; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                }

                if (r <= 11.8) {
                    BlockState fs;
                    if (Math.hypot(dx - 2, dz - 2) <= 3.2) {
                        fs = Math.hypot(dx - 2, dz - 2) <= 1.5 ? goldRune : mosaic;
                    } else if (Math.abs(r - 8.2) < 0.65 || Math.abs(dx - dz) <= 1 && dx >= -1 && dz >= -1) {
                        fs = floor;
                    } else if (((dx * 3 + dz * 5) & 7) == 0) {
                        fs = moss;
                    } else {
                        fs = ivyBrick;
                    }
                    put(chunk, mp, x0, z0, x, by - 1, z, fs);
                } else {
                    Direction inDir = Math.abs(dx) >= Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, x, by - 1, z, stair(tStairs, inDir, Half.BOTTOM));
                }
            }
        }

        // 6 стройных мраморных трилитов-арок по внешнему кольцу (с открытым просветом со стороны Юго-Восточной камеры!)
        double[] archAngles = {Math.PI * 0.55, Math.PI * 0.85, Math.PI * 1.15, Math.PI * 1.45, Math.PI * 1.75, Math.PI * 0.02};
        for (double a : archAngles) {
            double px = Math.cos(a) * 10.2, pz = Math.sin(a) * 10.2;
            double txv = -Math.sin(a), tzv = Math.cos(a);
            int p1x = cx + (int) Math.round(px - txv * 1.2), p1z = cz + (int) Math.round(pz - tzv * 1.2);
            int p2x = cx + (int) Math.round(px + txv * 1.2), p2z = cz + (int) Math.round(pz + tzv * 1.2);
            int midX = cx + (int) Math.round(px), midZ = cz + (int) Math.round(pz);
            for (int dy = 0; dy <= 4; ++dy) {
                put(chunk, mp, x0, z0, p1x, by + dy, p1z, dy == 0 ? ivyBrick : pillarY);
                put(chunk, mp, x0, z0, p2x, by + dy, p2z, dy == 0 ? ivyBrick : pillarY);
            }
            for (int st = 0; st <= 6; ++st) {
                double u = st / 6.0;
                int lx = (int) Math.round(Mth.lerp(u, p1x, p2x));
                int lz = (int) Math.round(Mth.lerp(u, p1z, p2z));
                put(chunk, mp, x0, z0, lx, by + 5, lz, marble);
                if (st >= 1 && st <= 5) {
                    put(chunk, mp, x0, z0, lx, by + 6, lz, slabBot(tSlab));
                }
            }
            put(chunk, mp, x0, z0, midX, by + 6, midZ, slabBot(tSlab));
        }

        // 2) Резной Мраморный Алтарь Корней перед Вековым Древом (cx + 2, cz + 2)
        for (int ax = cx + 1; ax <= cx + 3; ++ax) {
            for (int az = cz + 1; az <= cz + 3; ++az) {
                boolean center = (ax == cx + 2 && az == cz + 2);
                if (center) {
                    put(chunk, mp, x0, z0, ax, by, az, goldRune);
                    put(chunk, mp, x0, z0, ax, by + 1, az, crystalGlow);
                    put(chunk, mp, x0, z0, ax, by + 2, az, slabBot(tSlab));
                } else {
                    Direction inDir = Math.abs(ax - (cx + 2)) >= Math.abs(az - (cz + 2))
                            ? (ax > cx + 2 ? Direction.WEST : Direction.EAST)
                            : (az > cz + 2 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, ax, by, az, stair(tStairs, inDir, Half.BOTTOM));
                }
            }
        }

        // 3) Органическое Вековое Древо Сизо (tx, tz): изогнутые корни, витой ствол и 6 плавных дугообразных ветвей с пышной кроной!
        double[] rootAngles = {0.4, 1.35, 2.35, 3.3, 4.3, 5.35};
        for (double ra : rootAngles) {
            for (int step = 0; step <= 12; ++step) {
                double u = step / 12.0;
                double rad = 1.2 + u * 5.8;
                double rx = tx + Math.cos(ra + u * 0.22) * rad;
                double rz = tz + Math.sin(ra + u * 0.22) * rad;
                double ry = by + (1.0 - u) * (1.0 - u) * 3.4 - u * 0.4;
                put(chunk, mp, x0, z0, (int) Math.round(rx), (int) Math.round(ry), (int) Math.round(rz), sizoBark);
            }
        }

        // Витой живой ствол (высота 14 блоков с плавным изгибом S-кривой)
        for (int dy = 0; dy <= 13; ++dy) {
            double u = dy / 13.0;
            double sx = tx + Math.sin(u * Math.PI) * 0.8;
            double sz = tz - Math.cos(u * Math.PI * 0.7) * 0.6;
            double tr = Mth.lerp(u, 2.05, 1.15);
            for (int ox = -3; ox <= 3; ++ox) {
                for (int oz = -3; oz <= 3; ++oz) {
                    if (Math.hypot(ox, oz) <= tr) {
                        put(chunk, mp, x0, z0, (int) Math.round(sx) + ox, by + dy, (int) Math.round(sz) + oz,
                                (dy >= 4 && dy <= 7 && ox == 0 && oz == 0) ? crystalGlow : sizoBark);
                    }
                }
            }
        }

        // 6 плавных диагонально восходящих ветвей (кривые Безье) + объёмные эллипсоидальные облака листвы Sizo!
        double[][] boughs = {
                { 6.5, -1.5, 15.0, 4.8, 3.0},
                {-6.0,  2.5, 15.5, 4.6, 3.0},
                { 1.5, -6.5, 16.0, 4.8, 3.1},
                {-4.5, -5.0, 14.5, 4.4, 2.8},
                { 4.5,  5.0, 14.5, 4.2, 2.7},
                { 0.0,  0.0, 18.5, 5.6, 3.6}
        };
        for (double[] b : boughs) {
            double ex = tx + b[0], ez = tz + b[1], ey = by + b[2];
            double leafRx = b[3], leafRy = b[4];
            // Сначала пышная многослойная крона (чтобы ветви врастали прямо внутрь свода листвы!)
            int minLx = (int) Math.floor(ex - leafRx - 1), maxLx = (int) Math.ceil(ex + leafRx + 1);
            int minLz = (int) Math.floor(ez - leafRx - 1), maxLz = (int) Math.ceil(ez + leafRx + 1);
            int minLy = (int) Math.floor(ey - leafRy), maxLy = (int) Math.ceil(ey + leafRy);
            for (int lx = minLx; lx <= maxLx; ++lx) {
                if (lx < x0 || lx > x0 + 15) continue;
                for (int lz = minLz; lz <= maxLz; ++lz) {
                    if (lz < z0 || lz > z0 + 15) continue;
                    for (int ly = minLy; ly <= maxLy; ++ly) {
                        double nx = (lx - ex) / leafRx;
                        double ny = (ly - ey) / leafRy;
                        double nz = (lz - ez) / leafRx;
                        double d2 = nx * nx + ny * ny + nz * nz;
                        double wave = 0.14 * Math.sin(lx * 0.9 + lz * 0.7 + ly * 0.6);
                        if (d2 + wave <= 1.0) {
                            put(chunk, mp, x0, z0, lx, ly, lz, sizoLeaves);
                        }
                    }
                }
            }
            // Дугообразная древесная ветвь от ствола (by + 10) к центру кроны (ex, ey, ez)
            for (int step = 0; step <= 18; ++step) {
                double u = step / 18.0;
                double bx = Mth.lerp(u, tx, ex);
                double bz = Mth.lerp(u, tz, ez);
                double byc = Mth.lerp(u, by + 10.0, ey - 0.5) + Math.sin(u * Math.PI) * 1.4;
                put(chunk, mp, x0, z0, (int) Math.round(bx), (int) Math.round(byc), (int) Math.round(bz), sizoBark);
            }
            // Подвесной светящийся фонарь под крайними сводами кроны
            int lx = (int) Math.round(ex * 0.7 + tx * 0.3);
            int lz = (int) Math.round(ez * 0.7 + tz * 0.3);
            int ly = (int) Math.floor(ey - leafRy);
            put(chunk, mp, x0, z0, lx, ly, lz, lanternHang);
        }

        putLootChest(level, chunk, mp, rnd, x0, z0, tx + 2, by, tz - 1, Direction.EAST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, tx - 1, by, tz + 2, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // #4 ПЕПЕЛЬНАЯ ГОТИЧЕСКАЯ КОЛОКОЛЬНЯ И КУРГАН ПАВШИХ СТРАЖЕЙ (0 кубов магмы!)
    // -------------------------------------------------------------------------
    private static void buildAshenBelfryBarrow(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                               RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState dsBrick = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState dsTile = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState crackedDs = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState pbBrick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chiseledPb = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState dsStair = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
        BlockState dsSlab = Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState();
        BlockState basaltY = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState ashTurf = CastleBlocks.BURNT_SOIL.get().defaultBlockState();
        BlockState ashStone = CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState soulHang = soulLantern.setValue(LanternBlock.HANGING, true);
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState bellBronze = Blocks.ANVIL.defaultBlockState();

        int bxCenter = cx - 5, bzCenter = cz; // 27-метровая готическая колокольня слева
        int mxCenter = cx + 6, mzCenter = cz; // Каменный курган с южным порталом склепа справа

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int tdx = x - bxCenter, tdz = z - bzCenter;
                int tCheb = Math.max(Math.abs(tdx), Math.abs(tdz));
                boolean inTowerFoot = (tCheb <= 4);

                int mdx = x - mxCenter, mdz = z - mzCenter;
                double mr = Math.hypot(mdx, mdz);
                boolean inPortalFoot = (mdz >= 6 && mdz <= 8 && Math.abs(mdx) <= 2);
                boolean inBarrowFoot = (mr <= 8.2) || inPortalFoot;

                // Узкая неровная тропа строго между южными входами башни и кургана (БЕЗ искусственного круга вокруг!)
                boolean causeway = (Math.abs(z - (cz + 5)) <= 1 && x >= bxCenter - 1 && x <= mxCenter + 1)
                        || (Math.abs(x - bxCenter) <= 1 && z >= bzCenter + 3 && z <= cz + 6)
                        || (Math.abs(x - mxCenter) <= 1 && z >= mzCenter + 5 && z <= cz + 9);

                if (!inTowerFoot && !inBarrowFoot && !causeway) {
                    continue;
                }

                int gBot = safeGroundY(plan, x, z, by - 3);
                for (int y = gBot; y <= by - 1; ++y) {
                    if (y > plan.ocean) {
                        BlockState baseBlk = (y == by - 1)
                                ? (inTowerFoot ? dsTile : (causeway && ((x + z) & 1) == 0 ? pbBrick : ashStone))
                                : ashStone;
                        put(chunk, mp, x0, z0, x, y, z, baseBlk);
                    }
                }
                int clearTop = inTowerFoot ? (by + 27) : (inBarrowFoot ? (by + 9) : (by + 4));
                for (int y = by; y <= clearTop; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                }

                // 1) 4-ярусная готическая Колокольня-Кампанила (bxCenter, bzCenter, высота 27 м)
                if (tCheb <= 5) {
                    for (int dy = 0; dy <= 26; ++dy) {
                        int y = by + dy;
                        if (dy <= 3) {
                            // Ступенчатый контрфорсный цоколь (9x9 + угловые контрфорсы)
                            boolean buttress = (Math.abs(tdx) == 4 && Math.abs(tdz) >= 3) || (Math.abs(tdz) == 4 && Math.abs(tdx) >= 3);
                            if (tCheb <= 3 || buttress) {
                                if (tdz > 0 && Math.abs(tdx) <= 1 && dy <= 2) continue; // Южный портал башни
                                if (tCheb <= 2) {
                                    if (tdx == 0 && tdz == 0) put(chunk, mp, x0, z0, x, y, z, basaltY);
                                    else if (dy == 0) put(chunk, mp, x0, z0, x, y, z, dsTile);
                                } else {
                                    put(chunk, mp, x0, z0, x, y, z, dy == 3 ? chiseledPb : pbBrick);
                                }
                            }
                        } else if (dy <= 14) {
                            // Стройный ствол колокольни 7x7 с угловыми пилястрами и высокими стрельчатыми нишами
                            boolean cornerPilaster = (Math.abs(tdx) == 3 && Math.abs(tdz) == 3);
                            if (tCheb == 3) {
                                boolean slit = (tdx == 0 || tdz == 0) && (dy >= 7 && dy <= 11);
                                if (slit) {
                                    if (dy == 7) put(chunk, mp, x0, z0, x, y, z, soulLantern);
                                } else {
                                    put(chunk, mp, x0, z0, x, y, z, cornerPilaster ? pbBrick : (dy == 14 ? chiseledPb : dsBrick));
                                }
                            } else if (tCheb < 3) {
                                if (tdx == 0 && tdz == 0) {
                                    put(chunk, mp, x0, z0, x, y, z, basaltY);
                                } else if (((dy + (tdx >= 0 ? 0 : 2) + (tdz >= 0 ? 1 : 0)) & 3) == 0) {
                                    put(chunk, mp, x0, z0, x, y, z, slabBot(dsSlab));
                                }
                            }
                        } else if (dy <= 20) {
                            // Открытый арочный ярус звона (7x7) с подвешенным Великим Колоколом!
                            if (dy == 15 && tCheb <= 3) {
                                put(chunk, mp, x0, z0, x, y, z, dsTile);
                            } else if (tCheb == 3) {
                                boolean pier = Math.abs(tdx) >= 2 && Math.abs(tdz) >= 2;
                                boolean archTop = (dy == 20) || (dy == 19 && (Math.abs(tdx) == 1 || Math.abs(tdz) == 1));
                                if (pier || archTop) {
                                    put(chunk, mp, x0, z0, x, y, z, dy == 20 ? chiseledPb : pbBrick);
                                }
                            } else if (tdx == 0 && tdz == 0) {
                                if (dy >= 18 && dy <= 20) put(chunk, mp, x0, z0, x, y, z, chain);
                                else if (dy == 17) put(chunk, mp, x0, z0, x, y, z, bellBronze);
                                else if (dy == 16) put(chunk, mp, x0, z0, x, y, z, soulHang);
                            }
                        } else {
                            // Остроконечный готический шпиль с 4 угловыми пинаклями (dy = 21..26)
                            if (dy <= 22 && Math.abs(tdx) == 3 && Math.abs(tdz) == 3) {
                                put(chunk, mp, x0, z0, x, y, z, dy == 22 ? slabBot(dsSlab) : pbBrick);
                            }
                            int spireR = Math.max(0, 3 - (dy - 20) / 2);
                            if (Math.abs(tdx) + Math.abs(tdz) <= spireR + 1 && tCheb <= spireR) {
                                put(chunk, mp, x0, z0, x, y, z, dsTile);
                            } else if (dy == 26 && tdx == 0 && tdz == 0) {
                                put(chunk, mp, x0, z0, x, y, z, Blocks.LIGHTNING_ROD.defaultBlockState());
                            }
                        }
                    }
                }

                // 2) Каменный Курган Павших Стражей с резным южным порталом склепа (mxCenter, mzCenter)
                if (mr <= 8.2) {
                    boolean entranceCorridor = (mdz >= 3 && Math.abs(mdx) <= 1);
                    boolean innerVault = (mr <= 4.2);
                    int domeTop = by + (int) Math.round(Math.max(0.0, (1.0 - (mr * mr) / 67.0) * 6.5));

                    for (int y = by; y <= domeTop; ++y) {
                        if (innerVault && y <= by + 3) {
                            if (y == by && Math.abs(mdx) <= 1 && Math.abs(mdz) <= 1) {
                                // Резной каменный саркофаг в центре склепа
                                put(chunk, mp, x0, z0, x, y, z, mdz == 0 && mdx == 0 ? chiseledPb : pbBrick);
                            }
                            continue;
                        }
                        if (entranceCorridor && y <= by + 2) continue;
                        BlockState ms = (y == domeTop && mr > 2.2) ? ashTurf
                                : (mr >= 6.8 && y <= by + 2) ? pbBrick
                                : crackedDs;
                        put(chunk, mp, x0, z0, x, y, z, ms);
                    }
                    // Выступающий мегалитический южный портал кургана (mdz = 6..8, |mdx| <= 2), обращённый прямо к камере!
                    if (mdz >= 6 && mdz <= 8 && Math.abs(mdx) <= 2) {
                        for (int y = by; y <= by + 4; ++y) {
                            if (Math.abs(mdx) <= 1 && y <= by + 2) continue;
                            if (y == by + 4 && Math.abs(mdx) == 2) {
                                put(chunk, mp, x0, z0, x, y, z, stair(dsStair, mdx < 0 ? Direction.EAST : Direction.WEST, Half.BOTTOM));
                            } else {
                                put(chunk, mp, x0, z0, x, y, z, y == by + 3 ? chiseledPb : pbBrick);
                            }
                        }
                        if (mdz == 8 && mdx == 0) {
                            put(chunk, mp, x0, z0, x, by + 3, z, soulHang);
                        }
                    }
                    // Каменная стела на вершине кургана
                    if (mdx == 0 && mdz == 0) {
                        put(chunk, mp, x0, z0, x, domeTop + 1, z, chiseledPb);
                        put(chunk, mp, x0, z0, x, domeTop + 2, z, slabBot(dsSlab));
                    }
                }
            }
        }

        putLootChest(level, chunk, mp, rnd, x0, z0, mxCenter - 2, by, mzCenter - 1, Direction.EAST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, bxCenter + 1, by, bzCenter - 1, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // #6 ГОТИЧЕСКАЯ РУИНИРОВАННАЯ ПЕПЕЛЬНАЯ ЦЕРКОВЬ (Западный фасад с Розой + 5 травей нефа + Апсида)
    // -------------------------------------------------------------------------
    private static void buildAshenBurnedChurch(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                               RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState dsBrick = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState dsTile = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState crackedDs = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState pbBrick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chiseledPb = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState dsStair = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
        BlockState dsSlab = Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState();
        BlockState dsWall = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState basaltY = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState darkBeamX = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState grayGlass = Blocks.GRAY_STAINED_GLASS.defaultBlockState();
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState soulHang = soulLantern.setValue(LanternBlock.HANGING, true);

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                if (dx < -17 || dx > 16 || Math.abs(dz) > 11) continue;

                // Сплошной цоколь церкви
                int gBot = safeGroundY(plan, x, z, by - 3);
                if (dx >= -15 && dx <= 15 && Math.abs(dz) <= 9) {
                    for (int y = gBot; y <= by - 1; ++y) {
                        if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, y == by - 1 ? pbBrick : dsBrick);
                    }
                    for (int y = by; y <= by + 23; ++y) {
                        if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                    }
                }

                // Парадное западное крыльцо (dx = -17..-15, |dz| <= 3), обращённое прямо к ЮЗ камере!
                if (dx >= -17 && dx <= -15 && Math.abs(dz) <= 3) {
                    int stepY = by - 1 - (-16 - dx);
                    for (int y = gBot; y <= stepY; ++y) {
                        if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, pbBrick);
                    }
                    if (dx < -15 && Math.abs(dz) <= 4) {
                        put(chunk, mp, x0, z0, x, stepY + 1, z, stair(dsStair, Direction.EAST, Half.BOTTOM));
                    }
                }

                // 1) Монументальный Западный Готический Фасад с Окном-Розой и двумя квадратными 3x3 башенками-пинаклями (dx = -15..-13, |dz| <= 8)
                if (dx >= -15 && dx <= -13 && Math.abs(dz) <= 8) {
                    boolean inTurret = (Math.abs(dz) >= 6);
                    if (inTurret) {
                        for (int y = by; y <= by + 17; ++y) {
                            boolean edge = (dx == -15 || dx == -13 || Math.abs(dz) == 8 || Math.abs(dz) == 6);
                            put(chunk, mp, x0, z0, x, y, z, (y == by + 11 || y == by + 16) ? chiseledPb : (edge ? pbBrick : dsBrick));
                        }
                        // Ступенчатые готические пирамидальные шпили 3x3 -> 1x1 (by + 18 .. by + 22)
                        put(chunk, mp, x0, z0, x, by + 18, z, (dx == -14 && Math.abs(dz) == 7) ? pbBrick : dsSlab);
                        if (dx == -14 && Math.abs(dz) == 7) {
                            put(chunk, mp, x0, z0, x, by + 19, z, dsTile);
                            put(chunk, mp, x0, z0, x, by + 20, z, dsTile);
                            put(chunk, mp, x0, z0, x, by + 21, z, dsWall);
                            put(chunk, mp, x0, z0, x, by + 22, z, dsWall);
                        }
                        continue;
                    }
                    if (dx == -15) continue;
                    // Центральный щипец Западного фасада с глубоким стрельчатым порталом и круглой Готической Розой!
                    int gableTop = by + 18 - Math.abs(dz);
                    for (int y = by; y <= gableTop; ++y) {
                        // Глубокий стрельчатый Западный портал (|dz| <= 2, dy <= 5)
                        int portalClear = 5 - Math.abs(dz);
                        if (Math.abs(dz) <= 2 && y <= by + portalClear) {
                            if (dx == -14 && y == by + portalClear && Math.abs(dz) >= 1) {
                                put(chunk, mp, x0, z0, x, y, z, stair(dsStair, dz < 0 ? Direction.SOUTH : Direction.NORTH, Half.TOP));
                            }
                            continue;
                        }
                        // Круглое готическое окно-роза (центр dz = 0, dy = 10, радиус 2.4)
                        double roseR = Math.hypot(dz, (y - (by + 10)));
                        if (roseR <= 2.3) {
                            if (dx == -13) {
                                boolean spoke = (dz == 0 || y == by + 10);
                                put(chunk, mp, x0, z0, x, y, z, spoke ? chiseledPb : grayGlass);
                            }
                            continue;
                        }
                        put(chunk, mp, x0, z0, x, y, z, (y == gableTop) ? dsTile : (Math.abs(roseR - 2.8) < 0.55 ? pbBrick : dsBrick));
                    }
                    continue;
                }

                // 2) Пятипролётный готический Неф с контрфорсами, стрельчатыми окнами и сохранившимися каменными нервюрами свода (dx = -12..9, |dz| <= 9)
                if (dx >= -12 && dx <= 9 && Math.abs(dz) <= 9) {
                    boolean bayPier = ((dx + 12) % 5 == 0); // Пилоны на dx = -12, -7, -2, +3, +8
                    if (Math.abs(dz) == 9) {
                        // Наружные ступенчатые контрфорсы нефа
                        if (bayPier) {
                            for (int y = by; y <= by + 11; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, y == by + 11 ? stair(dsStair, dz > 0 ? Direction.NORTH : Direction.SOUTH, Half.BOTTOM) : pbBrick);
                            }
                        }
                        continue;
                    }
                    // Мозаичный пол нефа с центральным проходом и аккуратными скамьями
                    if (Math.abs(dz) <= 7) {
                        put(chunk, mp, x0, z0, x, by, z, Math.abs(dz) <= 1 ? pbBrick : (((dx + dz) & 3) == 0 ? crackedDs : dsTile));
                        if (Math.abs(dz) >= 3 && Math.abs(dz) <= 5 && ((dx + 12) % 5 == 2 || (dx + 12) % 5 == 3) && dx < 4) {
                            put(chunk, mp, x0, z0, x, by + 1, z, stair(dsStair, Direction.EAST, Half.BOTTOM));
                        }
                    }

                    // Продольные стены нефа (|dz| == 8) со стрельчатыми готическими окнами в каждом прясле
                    if (Math.abs(dz) == 8) {
                        int bayOff = Math.abs(((dx + 12) % 5) - 2); // 0 в центре окна, 1 по краям окна, 2 на пилоне
                        // Руинированный пролом кровли в средних пряслах, но стены и стрельчатые арки целы и читаемы!
                        int wallTop = bayPier ? (by + 13) : (dx >= -4 && dx <= 2 && dz > 0 ? by + 10 : by + 12);
                        for (int y = by; y <= wallTop; ++y) {
                            boolean inLancet = (bayOff <= 1 && y >= by + 3 && y <= by + 9 - bayOff);
                            if (inLancet) {
                                if (bayOff == 0 && y <= by + 5) {
                                    put(chunk, mp, x0, z0, x, y, z, grayGlass);
                                }
                                continue;
                            }
                            put(chunk, mp, x0, z0, x, y, z, bayPier ? pbBrick : (y == wallTop ? crackedDs : dsBrick));
                        }
                    }

                    // Внутренние пристенные колонны (|dz| == 7 на bayPier) и поперечные каменные стрельчатые арки-нервюры свода!
                    if (bayPier && Math.abs(dz) == 7) {
                        for (int y = by + 1; y <= by + 11; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, basaltY);
                        }
                        put(chunk, mp, x0, z0, x, by + 7, z - Integer.signum(dz), soulHang);
                    }
                    if (bayPier && Math.abs(dz) <= 7) {
                        // Каменная стрельчатая подпружная арка свода над нефом (уцелела на всех пилонах, кроме одного обрушенного центрального пролёта dx = -2!)
                        if (dx != -2 || Math.abs(dz) >= 5) {
                            int archY = by + 12 + (7 - Math.abs(dz)) / 2;
                            put(chunk, mp, x0, z0, x, archY, z, Math.abs(dz) == 0 ? chiseledPb : pbBrick);
                            put(chunk, mp, x0, z0, x, archY + 1, z, slabBot(dsSlab));
                        }
                    } else if ((Math.abs(dz) == 6 || Math.abs(dz) == 3) && dx < -4) {
                        // Уцелевшие обугленные продольные прогоны крыши в западной части нефа
                        int purlinY = by + 12 + (7 - Math.abs(dz)) / 2;
                        put(chunk, mp, x0, z0, x, purlinY, z, darkBeamX);
                    }
                    continue;
                }

                // 3) Полукруглая сводчатая Восточная Алтарная Апсида (dx = 10..15) с уцелевшим каменным конховым сводом!
                if (dx >= 10 && dx <= 15) {
                    double ar = Math.hypot((dx - 9.5) * 1.15, dz);
                    if (ar <= 7.8) {
                        put(chunk, mp, x0, z0, x, by, z, pbBrick);
                        put(chunk, mp, x0, z0, x, by + 1, z, ar <= 4.5 ? chiseledPb : dsTile);
                        boolean apseWall = (ar >= 6.5);
                        int conchaTop = by + 14 - (int) Math.round(ar * 0.45);
                        if (apseWall) {
                            for (int y = by + 2; y <= by + 11; ++y) {
                                boolean apseLancet = (Math.abs(dz) <= 1 || Math.abs(Math.abs(dz) - 4) == 0) && y >= by + 4 && y <= by + 9;
                                put(chunk, mp, x0, z0, x, y, z, apseLancet ? grayGlass : dsBrick);
                            }
                        }
                        // Каменный полукупол конхи над алтарём (by + 11 .. conchaTop)
                        for (int y = Math.max(by + 11, conchaTop - 1); y <= conchaTop; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y == conchaTop ? dsTile : pbBrick);
                        }
                        // Главный каменный престол в центре апсиды (dx = 12, |dz| <= 1)
                        if (dx == 12 && Math.abs(dz) <= 1) {
                            put(chunk, mp, x0, z0, x, by + 2, z, chiseledPb);
                            if (dz == 0) put(chunk, mp, x0, z0, x, by + 3, z, soulLantern);
                        }
                    }
                }
            }
        }

        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 11, by + 2, cz - 3, Direction.EAST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 11, by + 2, cz + 3, Direction.EAST, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // #9 РЕЗОНАТОРНАЯ КУЗНЯ ОСКОЛКОВ В ХРУСТАЛЬНЫХ РАЗЛОМАХ
    //    (Сводчатый павильон на 4 массивных устоях + парящая призма + парные горны, 0 кубов золота!)
    // -------------------------------------------------------------------------
    private static void buildShatteredShardForge(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                 RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState pillarY = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tStairs = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState dsTile = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState pbBrick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chiseledPb = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState dsSlab = slabBot(Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        BlockState crystalPale = ElisarBlocks.CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState crystalBright = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState cyanGlass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        BlockState lanternHang = ElisarBlocks.LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, true);

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double oct = Math.max(Math.abs(dx), Math.abs(dz)) + 0.414 * Math.min(Math.abs(dx), Math.abs(dz));
                if (oct > 12.6) continue;

                int gBot = safeGroundY(plan, x, z, by - 3);
                for (int y = gBot; y <= by - 1; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, y == by - 1 ? pbBrick : ElisarBlocks.KEEP_STONE.get().defaultBlockState());
                }
                for (int y = by; y <= by + 16; ++y) {
                    if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                }

                // 1) Ступенчатая восьмигранная платформа мастерской (oct <= 12.6) с чистым геометрическим инкрустированным полом
                if (oct > 11.2) {
                    Direction inDir = Math.abs(dx) >= Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, x, by - 1, z, stair(tStairs, inDir, Half.BOTTOM));
                    continue;
                }

                double r = Math.hypot(dx, dz);
                BlockState fs;
                if (r <= 2.4) {
                    fs = r <= 1.1 ? crystalBright : goldRune;
                } else if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
                    fs = dsTile;
                } else if (Math.abs(r - 6.2) < 0.65) {
                    fs = mosaic;
                } else {
                    fs = floor;
                }
                put(chunk, mp, x0, z0, x, by - 1, z, fs);

                // 2) Центральное резонаторное горнило с наковальней (r <= 2.2) и парящая гранёная Хрустальная Призма (dy = 5..10)!
                if (dx == 0 && dz == 0) {
                    put(chunk, mp, x0, z0, x, by, z, chiseledPb);
                    put(chunk, mp, x0, z0, x, by + 1, z, Blocks.ANVIL.defaultBlockState());
                } else if (Math.abs(dx) + Math.abs(dz) == 2) {
                    put(chunk, mp, x0, z0, x, by, z, dsSlab);
                }
                for (int dy = 5; dy <= 10; ++dy) {
                    double pr = (dy == 5 || dy == 10) ? 0.5 : (dy == 7 || dy == 8 ? 1.5 : 1.05);
                    if (Math.abs(dx) + Math.abs(dz) <= pr) {
                        put(chunk, mp, x0, z0, x, by + dy, z,
                                (dx == 0 && dz == 0) ? crystalBright : (dy == 7 || dy == 8 ? cyanGlass : crystalPale));
                    }
                }

                // 3) 4 массивных угловых контрфорсных пилона 2x2 на (±7..±8, ±7..±8) и плавные диагональные каменные арки к верхнему кольцу!
                boolean inCornerPier = (Math.abs(dx) >= 6 && Math.abs(dx) <= 7 && Math.abs(dz) >= 6 && Math.abs(dz) <= 7);
                if (inCornerPier) {
                    boolean innerShaft = (Math.abs(dx) == 6 && Math.abs(dz) == 6);
                    for (int y = by; y <= by + 7; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, innerShaft ? pillarY : (y == by || y == by + 7 ? chiseledPb : marble));
                    }
                }
                // Четыре диагональные стрельчатые арки от угловых пилонов к световому кольцу (r = 3.2 .. 8.8)
                if (Math.abs(Math.abs(dx) - Math.abs(dz)) <= 0 && Math.abs(dx) >= 3 && Math.abs(dx) <= 6) {
                    int step = 6 - Math.abs(dx); // 0 у пилона -> 3 у короны
                    int archY = by + 8 + step;
                    put(chunk, mp, x0, z0, x, archY, z, marble);
                    put(chunk, mp, x0, z0, x, archY + 1, z, slabBot(tSlab));
                    if (Math.abs(dx) == 4) {
                        put(chunk, mp, x0, z0, x, archY - 1, z, lanternHang);
                    }
                }
                // Верхнее мраморно-хрустальное резонаторное кольцо (r = 2.6 .. 4.0, dy = 11..12)
                if (r >= 2.5 && r <= 4.1) {
                    put(chunk, mp, x0, z0, x, by + 11, z, r <= 3.2 ? chiseledPb : marble);
                    put(chunk, mp, x0, z0, x, by + 12, z, (dx == 0 || dz == 0) ? crystalPale : slabBot(tSlab));
                }

                // 4) Два симметричных сужающихся каменных плавильных горна на заднем плане (dz = -8..-6, dx = ±4), не закрывающих вид камере!
                for (int side : new int[]{-4, 4}) {
                    int fdx = dx - side, fdz = dz + 7;
                    if (Math.abs(fdx) <= 1 && Math.abs(fdz) <= 1) {
                        for (int dy = 0; dy <= 11; ++dy) {
                            if (dy >= 5 && (fdx != 0 || fdz != 0)) {
                                if (dy == 5) put(chunk, mp, x0, z0, x, by + dy, z, dsSlab);
                                continue;
                            }
                            if (fdx == 0 && fdz == 1 && dy == 1) {
                                put(chunk, mp, x0, z0, x, by + dy, z, Blocks.BLAST_FURNACE.defaultBlockState());
                            } else if (fdx == 0 && fdz == 0 && dy <= 2) {
                                put(chunk, mp, x0, z0, x, by + dy, z, crystalBright);
                            } else {
                                put(chunk, mp, x0, z0, x, by + dy, z, dy == 11 ? chiseledPb : pbBrick);
                            }
                        }
                    }
                }
            }
        }

        // Каменное закалочное корыто и 2 сундука мастеров-кристальщиков
        put(chunk, mp, x0, z0, cx - 4, by, cz + 2, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        put(chunk, mp, x0, z0, cx + 4, by, cz + 2, Blocks.SMITHING_TABLE.defaultBlockState());
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 2, by, cz - 7, Direction.SOUTH, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 2, by, cz - 7, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // #11 ОБСЕРВАТОРИЯ ФИОЛЕТОВЫХ ВЕТРОВ И АРОЧНАЯ ВОЗДУШНАЯ ПРИСТАНЬ С НЕБЕСНОЙ ЯХТОЙ
    // -------------------------------------------------------------------------
    private static void buildVioletWindObservatory(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                   RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        double ux = lm.dirX, uz = lm.dirZ;
        double vx = -uz, vz = ux;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState tSlab = TempleBlocks.SLAB.get().defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState purpurPillar = Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState purpurSlab = slabBot(Blocks.PURPUR_SLAB.defaultBlockState());
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState darkBeamY = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState sizoPlank = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState copper = Blocks.COPPER_BLOCK.defaultBlockState();
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState crystalBright = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState whiteSail = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState lilacSail = Blocks.MAGENTA_TERRACOTTA.defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);

        // 1) Двухъярусная бело-мраморная Ротонда Обсерватории (r <= 8.8) с ребристым сиреневым астрономическим куполом!
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r <= 9.6) {
                    int gBot = safeGroundY(plan, x, z, by - 4);
                    for (int y = gBot; y <= by - 1; ++y) {
                        if (y > plan.ocean) {
                            put(chunk, mp, x0, z0, x, y, z, y == by - 1 ? marble : ElisarBlocks.KEEP_STONE.get().defaultBlockState());
                        }
                    }
                    for (int y = by; y <= by + 20; ++y) {
                        if (y > plan.ocean) put(chunk, mp, x0, z0, x, y, z, air);
                    }

                    if (r <= 8.6) {
                        // Мозаичный пол ротонды с компасной звездой
                        put(chunk, mp, x0, z0, x, by - 1, z,
                                r <= 1.6 ? goldRune : (Math.abs(r - 5.2) < 0.6 ? mosaic : floor));
                    } else {
                        put(chunk, mp, x0, z0, x, by - 1, z, slabBot(tSlab));
                    }

                    // Стена и 8 массивных сдвоенных пилястр ротонды (r = 7.2 .. 8.5, dy = 0..9)
                    double sProj = dx * ux + dz * uz;
                    double wProj = dx * vx + dz * vz;
                    boolean pierPortal = (sProj > 5.0 && Math.abs(wProj) <= 1.6);
                    boolean landPortal = (sProj < -5.0 && Math.abs(wProj) <= 1.6);
                    if (r >= 7.1 && r <= 8.5) {
                        double ang = Math.atan2(dz, dx);
                        int nearestOct = (int) Math.round(ang / (Math.PI * 0.25));
                        double pAng = nearestOct * (Math.PI * 0.25);
                        boolean onPilaster = Math.hypot(dx - Math.cos(pAng) * 7.8, dz - Math.sin(pAng) * 7.8) <= 0.95;

                        for (int dy = 0; dy <= 9; ++dy) {
                            if ((pierPortal || landPortal) && dy <= 3) continue;
                            if (!onPilaster && dy >= 5 && dy <= 7) {
                                put(chunk, mp, x0, z0, x, by + dy, z, glass);
                                continue;
                            }
                            put(chunk, mp, x0, z0, x, by + dy, z,
                                    (dy == 0 || dy == 4 || dy == 9) ? marble : (onPilaster ? grandCol : floor));
                        }
                    }
                    // Верхний балкон-антаблемент (dy = 9..10) и сиреневый купол со щелью телескопа (dy = 10..16)!
                    if (r >= 7.8 && r <= 9.1) {
                        put(chunk, mp, x0, z0, x, by + 9, z, slabTop(tSlab));
                        put(chunk, mp, x0, z0, x, by + 10, z, slabBot(tSlab));
                    }
                    if (r <= 8.1) {
                        if (r >= 3.2 && r <= 7.4) {
                            // Галерея второго этажа внутри обсерватории
                            put(chunk, mp, x0, z0, x, by + 4, z, darkPlank);
                        }
                        double domeH = Math.sqrt(Math.max(0.0, 66.0 - r * r)) * 0.80;
                        int domeY = by + 10 + (int) Math.floor(domeH);
                        double outerH = Math.sqrt(Math.max(0.0, 66.0 - (r + 0.9) * (r + 0.9))) * 0.80;
                        int lowY = Math.max(by + 10, by + 10 + (int) Math.floor(outerH));
                        boolean inSlit = (sProj > 0.0 && Math.abs(wProj) <= 1.15);
                        if (!inSlit && r <= 8.0) {
                            boolean rib = (dx == 0 || dz == 0);
                            for (int y = lowY; y <= domeY; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, rib ? marble : purpur);
                            }
                            if (domeH - Math.floor(domeH) > 0.5 && !rib) {
                                put(chunk, mp, x0, z0, x, domeY + 1, z, purpurSlab);
                            }
                        } else if (inSlit && Math.abs(wProj) >= 0.75 && r <= 7.9) {
                            // Обрамление открытой створки купола
                            for (int y = lowY; y <= domeY; ++y) {
                                put(chunk, mp, x0, z0, x, y, z, marble);
                            }
                        }
                    }
                }
            }
        }

        // Медно-кварцевая труба Большого Рефрактора на центральной колонне (выходит из щели купола к небу!)
        for (int py = 0; py <= 9; ++py) {
            put(chunk, mp, x0, z0, cx, by + py, cz, purpurPillar);
        }
        for (int step = -2; step <= 8; ++step) {
            int tx = cx + (int) Math.round(ux * step);
            int tz = cz + (int) Math.round(uz * step);
            int ty = by + 10 + (step + 2) / 2;
            put(chunk, mp, x0, z0, tx, ty, tz, (step == 8) ? crystalBright : (step % 3 == 0 ? copper : marble));
        }

        // 2) Изящная консольная Воздушная Пристань на каменной подпружной арке (s = 7.5 .. 20.0, ширина 5 м)
        for (double s = 7.5; s <= 20.0; s += 0.5) {
            // Плавная параболическая подпружная арка под пристанью вместо частокола вертикальных палок!
            double archDrop = Math.max(0.0, (16.5 - s) * 0.58);
            int archY = by - 1 - (int) Math.round(archDrop);
            for (double w = -2.2; w <= 2.2; w += 0.5) {
                int px = cx + (int) Math.round(ux * s + vx * w);
                int pz = cz + (int) Math.round(uz * s + vz * w);
                boolean edge = Math.abs(w) >= 1.85;
                put(chunk, mp, x0, z0, px, by, pz, edge ? darkPlank : sizoPlank);
                if (edge && archDrop > 0.4) {
                    put(chunk, mp, x0, z0, px, archY, pz, s <= 12.5 ? marble : darkPlank);
                    put(chunk, mp, x0, z0, px, archY + 1, pz, darkPlank);
                }
                int sInt = (int) Math.round(s);
                if (edge && (sInt == 10 || sInt == 15 || s >= 19.6)) {
                    put(chunk, mp, x0, z0, px, by + 1, pz, darkBeamY);
                    if (s >= 19.6) {
                        put(chunk, mp, x0, z0, px, by + 2, pz, lantern);
                        put(chunk, mp, x0, z0, px, by - 1, pz, lanternHang);
                    }
                }
            }
        }

        // 3) Изящная парящая Небесная Яхта с бушпритом и косым сиренево-белым парусом у края пристани!
        double skiffS = 15.0, skiffW = 5.4;
        for (int ls = -6; ls <= 6; ++ls) {
            double halfB = 1.8 * Math.sqrt(Math.max(0.0, 1.0 - (ls * ls) / 40.0));
            for (double lw = -2.0; lw <= 2.0; lw += 0.5) {
                if (Math.abs(lw) > halfB + 0.15) continue;
                int sx = cx + (int) Math.round(ux * (skiffS + ls) + vx * (skiffW + lw));
                int sz = cz + (int) Math.round(uz * (skiffS + ls) + vz * (skiffW + lw));
                put(chunk, mp, x0, z0, sx, by - 1, sz, (ls == 0 && Math.abs(lw) < 0.6) ? crystalBright : darkPlank);
                boolean rail = Math.abs(lw) >= halfB - 0.55 || Math.abs(ls) >= 5;
                put(chunk, mp, x0, z0, sx, by, sz, rail ? darkPlank : sizoPlank);
                if (Math.abs(ls) >= 5) {
                    put(chunk, mp, x0, z0, sx, by + 1, sz, darkPlank);
                }
            }
        }
        int mastX = cx + (int) Math.round(ux * skiffS + vx * skiffW);
        int mastZ = cz + (int) Math.round(uz * skiffS + vz * skiffW);
        for (int my = 1; my <= 9; ++my) {
            put(chunk, mp, x0, z0, mastX, by + my, mastZ, darkBeamY);
        }
        // Изогнутый гафельный парус вдоль курса небесной яхты
        for (int ls = -3; ls <= 2; ++ls) {
            int sailTop = by + 8 - Math.max(0, -ls) / 2;
            for (int sy = by + 3; sy <= sailTop; ++sy) {
                double camber = (sy >= by + 5 && sy <= by + 6) ? 0.6 : 0.0;
                int sx = cx + (int) Math.round(ux * (skiffS + ls) + vx * (skiffW + camber));
                int sz = cz + (int) Math.round(uz * (skiffS + ls) + vz * (skiffW + camber));
                put(chunk, mp, x0, z0, sx, sy, sz, (sy == by + 4 || sy == by + 7) ? lilacSail : whiteSail);
            }
        }

        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 3, by, cz, Direction.EAST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 3, by, cz, Direction.WEST, LOOT_MODEST_WAYFARER);
    }
}
