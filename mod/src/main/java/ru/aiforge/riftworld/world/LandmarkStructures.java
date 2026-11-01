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
                if (rg.blacktideSite != null && rg.blacktideSite.inFootprint(ax, az, 54.0)) continue;
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

    // -------------------------------------------------------------------------
    // СТРУКТУРА #8: ЗАБРОШЕННЫЙ АНТИЧНЫЙ ТЕАТР / АМФИТЕАТР (РУИНЫ В ПЛЮЩЕ И МХУ)
    // -------------------------------------------------------------------------
    private static void buildGrandTheater(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                          RandomSource rnd, CityPlan plan, GrandTheater t, int x0, int z0) {
        int cx = t.cx, cz = t.cz, by = t.baseY;
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState cracked = TempleBlocks.CRACKED.get().defaultBlockState();
        BlockState ivyBrick = TempleBlocks.IVY.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState mossBlock = ElisarBlocks.MOSS.get().defaultBlockState();
        BlockState flowers = ElisarBlocks.FLOWERS.get().defaultBlockState();
        BlockState tallGrass = ElisarBlocks.TALL_GRASS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState gold = TempleBlocks.GOLD.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState fallenColX = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState fallenColZ = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState pillar = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState rose = TempleBlocks.ROSE_WINDOW.get().defaultBlockState();
        BlockState stageWood = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState darkWood = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState roofBlueSlab = ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState redBanner = Blocks.RED_WOOL.defaultBlockState();
        BlockState cobweb = Blocks.COBWEB.defaultBlockState();
        BlockState vineSouth = Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.NORTH, true);
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 26.8) continue;

                double h2d = ElisarChunkGenerator.hash(x, z, 601);

                // v0.23.0: Естественный парковый пояс совместимости с островом (r = 22.2 .. 26.8) —
                // кипарисы Сизо, замшелые валуны, цветущие лужайки и плавный спуск к траве острова!
                if (r > 22.2) {
                    int gTop = safeGroundY(plan, x, z, by - 2);
                    if (gTop > plan.ocean + 2) {
                        double blend = (26.8 - r) / 4.6;
                        if (h2d < blend * 0.72) {
                            put(chunk, mp, x0, z0, x, gTop, z, h2d < 0.28 ? mossBlock : ElisarBlocks.GRASS.get().defaultBlockState());
                            if (h2d < 0.14) {
                                put(chunk, mp, x0, z0, x, gTop + 1, z, h2d < 0.06 ? flowers : tallGrass);
                            } else if (Math.abs(r - 24.2) < 0.65 && ((dx * 17 + dz * 31) & 15) == 0 && dz > -8) {
                                // Стройные парковые кипарисы вокруг чаши античного театра
                                for (int cy = 1; cy <= 6; ++cy) {
                                    put(chunk, mp, x0, z0, x, gTop + cy, z,
                                            cy <= 2 ? ElisarBlocks.SIZO_LOG.get().defaultBlockState()
                                                    : ElisarBlocks.GARDEN_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                                }
                            }
                        }
                    }
                    continue;
                }

                // Очищаем воздух над чашей театра и сценой
                for (int y = by; y <= by + 18; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                // Надёжное каменное основание до поверхности острова
                int colTop = safeGroundY(plan, x, z, by - 3);
                for (int y = Math.min(colTop, by - 2); y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, h2d < 0.35 ? ivyBrick : (h2d < 0.65 ? cracked : marble));
                }

                // 1) Потрескавшаяся и поросшая мхом Орхестра (r <= 6.8, dz >= -4)
                if (r <= 6.8 && dz >= -4) {
                    BlockState fl;
                    if (dx == 0 && dz == 0) {
                        fl = mosaicCore;
                    } else if (h2d < 0.19) {
                        fl = mossBlock;
                    } else if (h2d < 0.36) {
                        fl = ivyBrick;
                    } else if (Math.abs(r - 4.8) < 0.75 || Math.abs(r - 2.3) < 0.55) {
                        fl = h2d < 0.52 ? cracked : mosaic;
                    } else {
                        fl = (Math.abs(dx) == Math.abs(dz) && r < 4.5) ? goldRune : (h2d < 0.62 ? cracked : floor);
                    }
                    put(chunk, mp, x0, z0, x, by - 1, z, fl);
                    // Пробивающиеся сквозь плиты полевые цветы и трава в заброшенной чаше
                    if (r > 1.8 && r < 6.2 && h2d < 0.12) {
                        put(chunk, mp, x0, z0, x, by, z, h2d < 0.05 ? flowers : tallGrass);
                    }
                }

                // 2) Полукруглая разрушенная Кавея — ступенчатые скамьи со сколами, мхом и обрушенной колоннадой
                if (dz >= -2 && r > 6.8 && r <= 19.8) {
                    int tier = Mth.clamp((int) Math.floor((r - 6.8) / 1.8), 0, 6);
                    int seatY = by + tier;
                    // Местами верхние ряды амфитеатра обвалились (живописный рваный пролом на юго-востоке и западе)
                    boolean collapsedSector = (dx > 8 && dz > 8 && tier >= 4) || (dx < -11 && dz > 3 && dz < 10 && tier >= 5);
                    int buildTop = collapsedSector ? Math.max(by, seatY - 2) : seatY;
                    for (int y = by - 1; y < buildTop; ++y) {
                        double hy = ElisarChunkGenerator.hash(x, y, z);
                        BlockState sub = hy < 0.28 ? ivyBrick : (hy < 0.58 ? cracked : (hy < 0.72 ? keepMossy : marble));
                        put(chunk, mp, x0, z0, x, y, z, sub);
                    }
                    double ang = Math.atan2(dz, dx);
                    boolean aisle = Math.abs(Math.sin(ang * 4.0)) < 0.11;
                    Direction towardCenter = Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    if (collapsedSector || h2d < 0.16) {
                        put(chunk, mp, x0, z0, x, buildTop, z, h2d < 0.09 ? mossBlock : TempleBlocks.SLAB.get().defaultBlockState());
                        if (h2d < 0.05) put(chunk, mp, x0, z0, x, buildTop + 1, z, tallGrass);
                    } else {
                        BlockState seatStair = (aisle ? CastleBlocks.ASHLAR_STAIRS.get() : TempleBlocks.STAIRS.get())
                                .defaultBlockState()
                                .setValue(StairBlock.FACING, towardCenter.getOpposite())
                                .setValue(StairBlock.HALF, Half.BOTTOM);
                        put(chunk, mp, x0, z0, x, seatY, z, seatStair);
                    }

                    // Разрушенная верхняя колоннада амфитеатра с контрфорсами, карнизами и рельефным архитравом (v0.23.0!)
                    if (r >= 18.5 && !collapsedSector) {
                        boolean buttress = Math.abs(Math.sin(ang * 5.0)) < 0.14 && r >= 19.2;
                        if (buttress) {
                            for (int by2 = by; by2 <= by + 6; ++by2) {
                                put(chunk, mp, x0, z0, x, by2, z, by2 % 3 == 0 ? keepMossy : marble);
                            }
                        }
                        put(chunk, mp, x0, z0, x, by + 7, z, h2d < 0.45 ? ivyBrick : cracked);
                        int arcStep = (int) Math.round((ang + Math.PI) * 10.0);
                        boolean intactSpan = Math.sin(ang * 2.3 + 0.6) > -0.15;
                        if (arcStep % 3 == 0 && r >= 18.8 && r <= 19.6) {
                            int colTopY = intactSpan ? (by + 11) : (by + 8 + (Math.abs(arcStep) % 3));
                            for (int py = by + 8; py <= colTopY; ++py) {
                                put(chunk, mp, x0, z0, x, py, z, py == colTopY && !intactSpan ? cracked : pillar);
                            }
                        }
                        if (intactSpan && h2d > 0.18) {
                            put(chunk, mp, x0, z0, x, by + 12, z,
                                    arcStep % 3 == 1 ? slabTop(TempleBlocks.SLAB.get().defaultBlockState()) : marble);
                            put(chunk, mp, x0, z0, x, by + 13, z,
                                    h2d < 0.48 ? mossBlock : TempleBlocks.SLAB.get().defaultBlockState());
                        }
                    }
                }

                // 3) Полуразрушенная Скена (Scaenae Frons) в трещинах и лозах плюща на севере (dz от -13 до -4, |dx| <= 15)
                if (dz >= -13 && dz <= -4 && Math.abs(dx) <= 15) {
                    put(chunk, mp, x0, z0, x, by, z, h2d < 0.40 ? cracked : ivyBrick);
                    // Прогнивший деревянный настил сцены с проломами и мхом
                    BlockState stg = dz >= -8
                            ? (h2d < 0.14 ? mossBlock : (h2d < 0.34 ? darkWood : stageWood))
                            : (h2d < 0.42 ? cracked : floor);
                    put(chunk, mp, x0, z0, x, by + 1, z, stg);
                    if (dz == -4 && (dx == -8 || dx == 4)) {
                        put(chunk, mp, x0, z0, x, by + 2, z, lantern); // часть рамповых фонарей уцелела, часть погасла/утрачена
                    }

                    // Рваный верхний контур задней стены Скены (в центре и справа верх обрушен!)
                    int skeneMaxRel = 10 - (dx >= -3 && dx <= 5 ? (3 - Math.min(3, Math.abs(dx - 1))) : 0)
                            - (h2d < 0.28 ? 1 : 0);
                    if (dz == -10 || dz == -9) {
                        boolean centralArch = Math.abs(dx) <= 2;
                        boolean sideArch = Math.abs(Math.abs(dx) - 8) <= 1;
                        for (int sy = by + 2; sy <= by + 2 + skeneMaxRel; ++sy) {
                            int relY = sy - (by + 2);
                            if (centralArch && relY <= 4) continue;
                            if (sideArch && relY <= 3) continue;
                            double hs = ElisarChunkGenerator.hash(x, sy, z);
                            BlockState wallSt = (relY == 5 && hs > 0.35) ? gold
                                    : (relY == 7 && Math.abs(dx) == 8) ? rose
                                    : (hs < 0.36 ? ivyBrick : (hs < 0.72 ? cracked : marble));
                            put(chunk, mp, x0, z0, x, sy, z, wallSt);
                        }
                    }
                    // Свисающие с фасада Скены плети лоз/плюща (dz == -8)
                    if (dz == -8 && Math.abs(dx) <= 11 && h2d < 0.34 && Math.abs(dx) != 3 && Math.abs(dx) != 6 && Math.abs(dx) != 10) {
                        int vTop = by + 6 + (int) (h2d * 12.0) % 4;
                        int vBot = by + 3 + (Math.abs(dx) % 2);
                        for (int vy = vBot; vy <= vTop; ++vy) {
                            put(chunk, mp, x0, z0, x, vy, z, vineSouth);
                        }
                    }

                    // Колонны перед Скеной: часть стоит целиком, а колонна dx == 3 рухнула на сцену и в орхестру!
                    if (dz == -8 && (Math.abs(dx) == 3 || Math.abs(dx) == 6 || Math.abs(dx) == 10 || Math.abs(dx) == 13)) {
                        int cTop = (dx == 3) ? (by + 4) : (dx == -10 ? by + 8 : by + 10);
                        for (int cy = by + 2; cy <= cTop; ++cy) {
                            put(chunk, mp, x0, z0, x, cy, z, (cy == by + 6 && cTop >= by + 8) ? gold : grandCol);
                        }
                    }
                    // Истлевшие неровные обрывки театральных знамён
                    if (dz == -8 && (dx == -5 || dx == -4 || dx == 11)) {
                        int bBot = (dx == -4) ? by + 9 : by + 8;
                        for (int cy = bBot; cy <= by + 10; ++cy) {
                            put(chunk, mp, x0, z0, x, cy, z, redBanner);
                        }
                    }

                    // Полуразрушенные боковые башни-параскении (западная сохранила проломленный купол, восточная без верхушки в плюще)
                    if (Math.abs(dx) >= 12 && dz >= -12 && dz <= -6) {
                        boolean eastRuined = dx > 0;
                        int towerTop = eastRuined ? (by + 9 - ((Math.abs(dx + dz)) % 2)) : (by + 12);
                        boolean edge = Math.abs(dx) == 12 || Math.abs(dx) == 15 || dz == -12 || dz == -6;
                        for (int ty = by + 2; ty <= towerTop; ++ty) {
                            if (edge) {
                                double ht = ElisarChunkGenerator.hash(x, ty, z);
                                put(chunk, mp, x0, z0, x, ty, z, ht < 0.42 ? ivyBrick : (ht < 0.78 ? cracked : marble));
                            }
                        }
                        if (!eastRuined) {
                            int dCup = Math.max(Math.abs(Math.abs(dx) - 13), Math.abs(dz + 9));
                            if (h2d > 0.20) put(chunk, mp, x0, z0, x, by + 13, z, h2d < 0.45 ? keepMossy : roofBlue);
                            if (dCup <= 1 && h2d > 0.28) put(chunk, mp, x0, z0, x, by + 14, z, roofBlue);
                            if (dCup == 0) put(chunk, mp, x0, z0, x, by + 15, z, roofBlueSlab);
                        }
                    }
                }
            }
        }
        // Обломки рухнувшей колонны на орхестре и сцене + паутина в кулисах
        for (int s = 0; s < 3; ++s) {
            put(chunk, mp, x0, z0, cx + 3, by, cz - 2 + s, fallenColZ);
        }
        put(chunk, mp, x0, z0, cx - 4, by, cz + 2, fallenColX);
        put(chunk, mp, x0, z0, cx - 3, by, cz + 2, cracked);
        put(chunk, mp, x0, z0, cx - 14, by + 3, cz - 11, cobweb);
        put(chunk, mp, x0, z0, cx + 13, by + 3, cz - 11, cobweb);

        // 2 скромных сундука с припасами странников в заброшенных кулисах и западной башне театра
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 1, by + 2, cz - 11, Direction.SOUTH, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 13, by + 2, cz - 9, Direction.EAST, LOOT_MODEST_WAYFARER);
        put(chunk, mp, x0, z0, cx, by + 6, cz - 9, lanternHang);
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #10: ПРИСТАНЬ-ПОРТ С ТАМОЖНЕЙ, ПОРТОВЫМ КРАНОМ И КАРАВЕЛЛОЙ
    // -------------------------------------------------------------------------
    private static void buildHarborPort(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                        RandomSource rnd, CityPlan plan, HarborPort h, int x0, int z0) {
        int cx = h.cx, cz = h.cz, qy = h.quayY, ocean = plan.ocean;
        BlockState stone = ElisarBlocks.STONE.get().defaultBlockState();
        BlockState mossyBrick = ElisarBlocks.BRICKS_MOSSY.get().defaultBlockState();
        BlockState brick = ElisarBlocks.BRICKS.get().defaultBlockState();
        BlockState ashlar = CastleBlocks.ASHLAR.get().defaultBlockState();
        BlockState ashlarPillar = CastleBlocks.ASHLAR_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState plaster = ElisarBlocks.PLASTER.get().defaultBlockState();
        BlockState timber = ElisarBlocks.TIMBER_CROSS.get().defaultBlockState();
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState darkBeamY = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState darkBeamX = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState darkBeamZ = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState sizoLog = ElisarBlocks.SIZO_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState sailWhite = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState sailBlue = Blocks.BLUE_WOOL.defaultBlockState();
        BlockState chain = ElisarBlocks.CHAIN.get().defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        BlockState fence = ElisarBlocks.FENCE.get().defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);

        // 1) Главная каменная набережная порта (lx: -13..+13, lz: -11..-2) на массивных каменных быках до самого дна океана (0 sub_air!)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int lx = x - cx, lz = z - cz;
                if (Math.abs(lx) > 24 || Math.abs(lz) > 24) continue;

                boolean mainQuay = (lx >= -13 && lx <= 13 && lz >= -11 && lz <= -2);
                boolean westPier = (lx >= -12 && lx <= -8 && lz >= -1 && lz <= 14);
                boolean eastPier = (lx >= 8 && lx <= 12 && lz >= -1 && lz <= 14);

                if (mainQuay) {
                    int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, x, z));
                    boolean caisson = (Math.abs(lx) % 5 <= 1 && Math.abs(lz) % 4 <= 1)
                            || lx == -13 || lx == 13 || lz == -11 || lz == -2;
                    if (caisson) {
                        for (int y = fl; y < qy - 1; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, y <= ocean ? mossyBrick : brick);
                        }
                    }
                    put(chunk, mp, x0, z0, x, qy - 1, z, stone);
                    boolean border = lx == -13 || lx == 13 || lz == -11 || lz == -2;
                    put(chunk, mp, x0, z0, x, qy, z, border ? ashlar : ((lx + lz) % 3 == 0 ? brick : ElisarBlocks.COBBLE.get().defaultBlockState()));

                    // Каменные швартовые тумбы и портовые фонари вдоль кромки воды
                    if (lz == -2 && (lx == -12 || lx == -4 || lx == 4 || lx == 12)) {
                        put(chunk, mp, x0, z0, x, qy + 1, z, ashlarPillar);
                        if (Math.abs(lx) == 4) {
                            put(chunk, mp, x0, z0, x, qy + 2, z, fence);
                            put(chunk, mp, x0, z0, x, qy + 3, z, lantern);
                        }
                    }
                } else if (westPier || eastPier) {
                    // Деревянные пирсы на сваях, уходящих в морское дно (без единого блока воздуха под водой!)
                    boolean piling = ((lx == -12 || lx == -8 || lx == 8 || lx == 12) && (lz % 4 == 0 || lz == 14));
                    if (piling) {
                        int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, x, z));
                        for (int y = fl; y <= qy; ++y) {
                            put(chunk, mp, x0, z0, x, y, z, darkBeamY);
                        }
                        if (lz == 14 || lz == 6) {
                            put(chunk, mp, x0, z0, x, qy + 1, z, fence);
                            put(chunk, mp, x0, z0, x, qy + 2, z, lantern);
                        }
                    } else {
                        put(chunk, mp, x0, z0, x, qy, z, darkPlank);
                    }
                }
            }
        }

        // 2) Двухэтажное здание Портовой Таможни и арочного Склада с эркером, кронштейнами, ставнями и трубой (v0.23.0!)
        BlockState ashlarStair = CastleBlocks.ASHLAR_STAIRS.get().defaultBlockState();
        BlockState roofBlueStairN = stair(ElisarBlocks.ROOF_BLUE_STAIRS.get().defaultBlockState(), Direction.NORTH, Half.BOTTOM);
        BlockState roofBlueStairS = stair(ElisarBlocks.ROOF_BLUE_STAIRS.get().defaultBlockState(), Direction.SOUTH, Half.BOTTOM);
        BlockState darkStair = CastleBlocks.DARK_STAIRS.get().defaultBlockState();
        for (int lx = -11; lx <= 3; ++lx) {
            for (int lz = -10; lz <= -4; ++lz) {
                int wx = cx + lx, wz = cz + lz;
                boolean wallX = (lx == -11 || lx == 3);
                boolean wallZ = (lz == -10 || lz == -4);
                boolean wall = wallX || wallZ;

                // 1-й этаж: каменная аркада склада со ступенчатыми пятами арок и рустованными углами (qy+1 .. qy+4)
                for (int dy = 1; dy <= 4; ++dy) {
                    if (wall) {
                        boolean archCenter = (lz == -4 && (lx == -7 || lx == -1));
                        boolean archSide = (lz == -4 && (Math.abs(lx + 7) == 1 || Math.abs(lx + 1) == 1));
                        if (archCenter && dy <= 3) continue;
                        if (archSide && dy <= 2) continue;
                        if (archSide && dy == 3) {
                            Direction inDir = (lx == -8 || lx == -2) ? Direction.WEST : Direction.EAST;
                            put(chunk, mp, x0, z0, wx, qy + dy, wz, stair(ashlarStair, inDir, Half.TOP));
                            continue;
                        }
                        BlockState st = (wallX && wallZ) ? ashlarPillar
                                : (dy == 1 ? ashlar : ((lx + dy) % 4 == 0 ? ElisarBlocks.BRICKS_MOSSY.get().defaultBlockState() : brick));
                        put(chunk, mp, x0, z0, wx, qy + dy, wz, st);
                    }
                }
                // Межэтажное перекрытие + нависающий фахверковый 2-й этаж Портовой Таможни (qy+5 .. qy+8)
                put(chunk, mp, x0, z0, wx, qy + 5, wz, darkPlank);
                for (int dy = 6; dy <= 8; ++dy) {
                    if (wall) {
                        boolean win = (dy == 7 && !wallX && (lx == -8 || lx == -4 || lx == 0));
                        BlockState st = (wallX && wallZ) ? darkBeamY
                                : (lx == -4 && !wallZ) ? darkBeamY
                                : win ? ElisarBlocks.WINDOW.get().defaultBlockState()
                                : (dy == 7 || (dy == 6 && (lx + lz) % 3 == 0) ? timber : plaster);
                        put(chunk, mp, x0, z0, wx, qy + dy, wz, st);
                    }
                }
            }
            // Консольные деревянные кронштейны нависающего карниза и цветочницы под окнами со стороны гавани (lz == -3)
            put(chunk, mp, x0, z0, cx + lx, qy + 5, cz - 3,
                    (lx == -11 || lx == -7 || lx == -4 || lx == -1 || lx == 3)
                            ? stair(darkStair, Direction.NORTH, Half.TOP)
                            : slabTop(CastleBlocks.DARK_SLAB.get().defaultBlockState()));
            if (lx == -8 || lx == 0) {
                put(chunk, mp, x0, z0, cx + lx, qy + 6, cz - 3, Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
            }
        }
        // Рельефная ступенчатая синяя черепичная крыша Таможни + фронтон дормера + каменная печная труба с дымком!
        for (int lx = -12; lx <= 4; ++lx) {
            for (int lz = -11; lz <= -3; ++lz) {
                int distMid = Math.abs(lz + 7);
                int ry = qy + 12 - distMid;
                BlockState rSt = distMid == 0 ? ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState()
                        : (lz < -7 ? roofBlueStairS : roofBlueStairN);
                if (lx == -12 || lx == 4) {
                    rSt = stair(darkStair, lz < -7 ? Direction.SOUTH : Direction.NORTH, Half.BOTTOM);
                }
                put(chunk, mp, x0, z0, cx + lx, ry, cz + lz, rSt);
                if (distMid > 0 && distMid < 4 && lx > -12 && lx < 4) {
                    put(chunk, mp, x0, z0, cx + lx, ry - 1, cz + lz, roofBlue);
                }
                if ((lx == -11 || lx == 3) && distMid < 3) {
                    for (int fy = qy + 9; fy < ry; ++fy) {
                        put(chunk, mp, x0, z0, cx + lx, fy, cz + lz, fy == qy + 10 && lx == -11 ? ElisarBlocks.WINDOW.get().defaultBlockState() : plaster);
                    }
                }
            }
        }
        // Каменная каминная труба на западном крыле Таможни
        for (int cy = qy + 9; cy <= qy + 14; ++cy) {
            put(chunk, mp, x0, z0, cx - 10, cy, cz - 9, cy == qy + 14 ? CastleBlocks.ASHLAR_SLAB.get().defaultBlockState() : ashlar);
        }
        put(chunk, mp, x0, z0, cx - 7, qy + 1, cz - 8, barrel);
        put(chunk, mp, x0, z0, cx - 6, qy + 1, cz - 8, barrel);
        put(chunk, mp, x0, z0, cx - 7, qy + 2, cz - 8, barrel);
        // 2 скромных портовых сундука с морскими припасами (на складе 1-го этажа и в конторе Таможни на 2-м этаже)
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 9, qy + 1, cz - 8, Direction.SOUTH, LOOT_MODEST_MARITIME);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 2, qy + 6, cz - 9, Direction.SOUTH, LOOT_MODEST_MARITIME);

        // Каменная береговая лестница и подпорная набережная, соединяющие причал (qy) прямо с зелёным берегом острова (cz - 12 .. -18)!
        BlockState cobbleStairN = ElisarBlocks.COBBLE_STAIRS.get().defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.BOTTOM);
        for (int lx = -13; lx <= 13; ++lx) {
            for (int lz = -17; lz <= -12; ++lz) {
                int wx = cx + lx, wz = cz + lz;
                boolean stairRun = (lx >= 5 && lx <= 9);
                if (stairRun) {
                    int stepRise = Mth.clamp((-12 - lz) + 1, 0, 5);
                    int sy = qy + stepRise;
                    int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz));
                    for (int y = fl; y < sy; ++y) {
                        put(chunk, mp, x0, z0, wx, y, wz, y <= ocean ? mossyBrick : stone);
                    }
                    for (int y = sy + 1; y <= sy + 6; ++y) {
                        put(chunk, mp, x0, z0, wx, y, wz, Blocks.AIR.defaultBlockState());
                    }
                    if (lx == 5 || lx == 9) {
                        put(chunk, mp, x0, z0, wx, sy, wz, ashlar);
                        if (lz == -12 || lz == -17) {
                            put(chunk, mp, x0, z0, wx, sy + 1, wz, ashlarPillar);
                            put(chunk, mp, x0, z0, wx, sy + 2, wz, lantern);
                        }
                    } else {
                        put(chunk, mp, x0, z0, wx, sy, wz, cobbleStairN);
                    }
                } else if (lz >= -14) {
                    // Каменное сопряжение тыла набережной со склоном прибрежного острова (0 разрывов и 0 sub_air!)
                    int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz));
                    for (int y = fl; y <= qy; ++y) {
                        put(chunk, mp, x0, z0, wx, y, wz, y <= ocean ? mossyBrick : (y == qy ? ElisarBlocks.COBBLE.get().defaultBlockState() : stone));
                    }
                }
            }
        }

        // 3) Большой деревянный портовый кран с висящим на цепи грузом на восточном крыле набережной (cx + 8, cz - 4)
        int kx = cx + 6, kz = cz - 4;
        for (int dy = 1; dy <= 10; ++dy) {
            put(chunk, mp, x0, z0, kx, qy + dy, kz, darkBeamY);
        }
        put(chunk, mp, x0, z0, kx - 1, qy + 1, kz, ashlar);
        put(chunk, mp, x0, z0, kx + 1, qy + 1, kz, ashlar);
        for (int s = 0; s <= 5; ++s) {
            put(chunk, mp, x0, z0, kx, qy + 8 + s / 2, kz + s, darkBeamZ);
        }
        for (int cy = qy + 5; cy <= qy + 9; ++cy) {
            put(chunk, mp, x0, z0, kx, cy, kz + 5, chain);
        }
        put(chunk, mp, x0, z0, kx, qy + 4, kz + 5, barrel);
        put(chunk, mp, x0, z0, kx, qy + 3, kz + 5, lanternHang);

        // 4) Детализированная Торговая Каравелла с кормовым кастлем, бушпритом и двумя мачтами (v0.23.0, 0 sub_air!)
        int sx = cx, sz = cz + 8;
        for (int dz = -8; dz <= 8; ++dz) {
            int hw = Math.abs(dz) >= 7 ? 0 : (Math.abs(dz) >= 5 ? 1 : (Math.abs(dz) >= 3 ? 2 : 3));
            for (int dx = -hw; dx <= hw; ++dx) {
                put(chunk, mp, x0, z0, sx + dx, ocean, sz + dz, darkPlank);
                boolean gunwale = (Math.abs(dx) == hw || Math.abs(dz) >= 7);
                put(chunk, mp, x0, z0, sx + dx, ocean + 1, sz + dz, gunwale ? darkPlank : ElisarBlocks.SIZO_PLANKS.get().defaultBlockState());
                if (gunwale) {
                    put(chunk, mp, x0, z0, sx + dx, ocean + 2, sz + dz,
                            (dz <= -4 || dz >= 6) ? darkPlank : CastleBlocks.DARK_SLAB.get().defaultBlockState());
                }
                // Возвышенный кормовой квартердек (каюта капитана на dz = -7 .. -4)
                if (dz >= -7 && dz <= -4 && Math.abs(dx) <= hw) {
                    if (gunwale || dz == -4) {
                        put(chunk, mp, x0, z0, sx + dx, ocean + 3, sz + dz,
                                (dz == -7 && dx == 0) ? ElisarBlocks.WINDOW.get().defaultBlockState() : darkPlank);
                    }
                    put(chunk, mp, x0, z0, sx + dx, ocean + 4, sz + dz, CastleBlocks.DARK_SLAB.get().defaultBlockState());
                    if (gunwale && dz <= -5) {
                        put(chunk, mp, x0, z0, sx + dx, ocean + 5, sz + dz, fence);
                    }
                }
            }
        }
        // Бушприт на носу каравеллы (dz = +8 .. +12) и кормовой фонарь
        for (int b = 8; b <= 12; ++b) {
            put(chunk, mp, x0, z0, sx, ocean + 2 + (b - 8) / 2, sz + b, darkBeamZ);
        }
        put(chunk, mp, x0, z0, sx, ocean + 6, sz - 7, lantern);
        // Грот-мачта и фок-мачта с пузатыми бело-лазурными парусами и вантами
        for (int my = ocean + 2; my <= ocean + 15; ++my) {
            put(chunk, mp, x0, z0, sx, my, sz, sizoLog);
        }
        for (int my = ocean + 4; my <= ocean + 11; ++my) {
            put(chunk, mp, x0, z0, sx, my, sz - 5, sizoLog);
        }
        for (int dx = -4; dx <= 4; ++dx) {
            put(chunk, mp, x0, z0, sx + dx, ocean + 12, sz, darkBeamX);
            put(chunk, mp, x0, z0, sx + dx, ocean + 6, sz, darkBeamX);
            for (int sy = ocean + 7; sy <= ocean + 11; ++sy) {
                int bulge = (Math.abs(dx) <= 2 && sy >= ocean + 8 && sy <= ocean + 10) ? 1 : 0;
                put(chunk, mp, x0, z0, sx + dx, sy, sz + bulge, (sy == ocean + 9) ? sailBlue : sailWhite);
            }
        }
        for (int dx = -2; dx <= 2; ++dx) {
            put(chunk, mp, x0, z0, sx + dx, ocean + 9, sz - 5, darkBeamX);
            for (int sy = ocean + 6; sy <= ocean + 8; ++sy) {
                put(chunk, mp, x0, z0, sx + dx, sy, sz - 5, sy == ocean + 7 ? sailBlue : sailWhite);
            }
        }
        for (int wy = ocean + 3; wy <= ocean + 7; ++wy) {
            put(chunk, mp, x0, z0, sx - 2, wy, sz, chain);
            put(chunk, mp, x0, z0, sx + 2, wy, sz, chain);
        }
        put(chunk, mp, x0, z0, sx, ocean + 16, sz, lantern);
        put(chunk, mp, x0, z0, sx, ocean + 2, sz - 2, barrel);
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #7: ВЫСОКИЙ МОРСКОЙ МАЯК НА СКАЛИСТОМ РИФЕ
    // -------------------------------------------------------------------------
    private static void buildSeaLighthouse(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                           RandomSource rnd, CityPlan plan, SeaLighthouse l, int x0, int z0) {
        int cx = l.cx, cz = l.cz, rTop = l.rockTopY, ocean = plan.ocean;
        BlockState stone = ElisarBlocks.STONE.get().defaultBlockState();
        BlockState mossy = ElisarBlocks.STONE_MOSSY.get().defaultBlockState();
        BlockState cobble = ElisarBlocks.COBBLE.get().defaultBlockState();
        BlockState whiteWall = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState stripeRed = ElisarBlocks.ROOF_RED.get().defaultBlockState();
        BlockState brass = CastleBlocks.BRASS.get().defaultBlockState();
        BlockState bars = CastleBlocks.BARS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState beaconCrystal = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();

        // 1) Массивный скалистый островок-риф от морского дна до rTop (сплошной камень — 0 sub_air!)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double d = Math.hypot(dx, dz);
                double ang = Math.atan2(dz, dx);
                double rockR = 12.2 + 1.8 * Math.sin(ang * 3.0 + cx * 0.1) + 1.1 * Math.cos(ang * 5.0 - cz * 0.1);
                if (d <= rockR) {
                    int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, x, z));
                    double u = d / rockR;
                    int topY = (u < 0.52) ? rTop : (int) Math.round(Mth.lerp(1.0 - (u - 0.52) / 0.48, ocean - 3.0, rTop));
                    for (int y = fl; y <= topY; ++y) {
                        BlockState st = (y == topY && y > ocean + 1) ? ((dx + dz) % 3 == 0 ? mossy : cobble)
                                : (y <= ocean + 1 ? mossy : stone);
                        put(chunk, mp, x0, z0, x, y, z, st);
                    }
                }
                // Каменная лодочная пристань и ступени с юга (dx: -2..2, dz: 6..15)
                if (Math.abs(dx) <= 2 && dz >= 6 && dz <= 15) {
                    int stepY = dz >= 12 ? ocean + 2 : Mth.clamp(rTop - (dz - 6), ocean + 2, rTop);
                    int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, x, z));
                    for (int y = fl; y <= stepY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, y == stepY ? CastleBlocks.ASHLAR.get().defaultBlockState() : mossy);
                    }
                    if (Math.abs(dx) == 2 && dz == 15) {
                        put(chunk, mp, x0, z0, x, stepY + 1, z, ElisarBlocks.FENCE.get().defaultBlockState());
                        put(chunk, mp, x0, z0, x, stepY + 2, z, lantern);
                    }
                }
            }
        }

        // 2) Высокая 31-блочная коническая башня Маяка (y = rTop + 1 .. rTop + 31) со спиральной красной полосой!
        int towerH = 25;
        for (int dy = 0; dy <= towerH; ++dy) {
            int y = rTop + 1 + dy;
            double rOut = Mth.lerp(dy / (double) towerH, 5.1, 3.35);
            double rIn = rOut - 1.25;
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dz = -6; dz <= 6; ++dz) {
                    double d = Math.hypot(dx, dz);
                    if (d > rOut) continue;
                    int wx = cx + dx, wz = cz + dz;
                    if (d >= rIn) {
                        // Входная дверь на южной стороне у подножия башни
                        if (dy <= 2 && Math.abs(dx) <= 0 && dz > 0) {
                            put(chunk, mp, x0, z0, wx, y, wz, Blocks.AIR.defaultBlockState());
                            continue;
                        }
                        // Узкие арочные бойницы-окна вдоль винтовой лестницы
                        boolean slitWin = (dy % 6 == 4) && ((dx == 0 && Math.abs(dz) >= 3) || (dz == 0 && Math.abs(dx) >= 3));
                        if (slitWin) {
                            put(chunk, mp, x0, z0, wx, y, wz, ElisarBlocks.WINDOW.get().defaultBlockState());
                            continue;
                        }
                        double theta = Math.atan2(dz, dx);
                        double spiral = ((theta / (Math.PI * 2.0)) + dy * 0.085 + 2.0) % 0.5;
                        boolean redBand = spiral < 0.16 || dy == 0 || dy == towerH;
                        put(chunk, mp, x0, z0, wx, y, wz, redBand ? stripeRed : whiteWall);
                    } else {
                        // Внутренность башни + центральный опорный столб и винтовая лестница
                        if (dx == 0 && dz == 0) {
                            put(chunk, mp, x0, z0, wx, y, wz, CastleBlocks.ASHLAR_PILLAR.get().defaultBlockState());
                        } else {
                            double stepAng = (dy * 0.55) % (Math.PI * 2.0);
                            int sx = (int) Math.round(Math.cos(stepAng) * Math.min(2.2, rIn - 0.5));
                            int sz = (int) Math.round(Math.sin(stepAng) * Math.min(2.2, rIn - 0.5));
                            if (dx == sx && dz == sz) {
                                put(chunk, mp, x0, z0, wx, y, wz, darkPlank);
                            } else {
                                put(chunk, mp, x0, z0, wx, y, wz, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

        // 3) Выносная смотровая галерея на резных каменных кронштейнах и Сияющая Фонарная Камера (v0.23.0!)
        BlockState ashlarStair = CastleBlocks.ASHLAR_STAIRS.get().defaultBlockState();
        int galY = rTop + 1 + towerH + 1;
        for (Direction d4 : Direction.Plane.HORIZONTAL) {
            put(chunk, mp, x0, z0, cx + d4.getStepX() * 4, galY - 1, cz + d4.getStepZ() * 4,
                    stair(ashlarStair, d4.getOpposite(), Half.TOP));
            put(chunk, mp, x0, z0, cx + d4.getStepX() * 6, rTop + 1, cz + d4.getStepZ() * 6,
                    stair(ashlarStair, d4.getOpposite(), Half.BOTTOM));
        }
        // Уютный каменный Домик Смотрителя Маяка с синей двускатной крышей и каминной трубой на западном плече рифа (dx: -9..-4, dz: -3..+3)
        for (int kx = -9; kx <= -4; ++kx) {
            for (int kz = -3; kz <= 3; ++kz) {
                int wx = cx + kx, wz = cz + kz;
                int fl = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz));
                for (int y = fl; y <= rTop; ++y) {
                    put(chunk, mp, x0, z0, wx, y, wz, y <= ocean + 1 ? mossy : stone);
                }
                boolean kwX = (kx == -9 || kx == -4);
                boolean kwZ = (kz == -3 || kz == 3);
                for (int dy = 1; dy <= 4; ++dy) {
                    if (kwX || kwZ) {
                        if (kx == -4 && kz == 0 && dy <= 2) continue; // проход к башне маяка
                        boolean win = dy == 2 && ((kwZ && kx == -7) || (kx == -9 && kz == 0));
                        BlockState kst = (kwX && kwZ) ? CastleBlocks.ASHLAR_PILLAR.get().defaultBlockState()
                                : win ? ElisarBlocks.WINDOW.get().defaultBlockState()
                                : (dy == 1 ? CastleBlocks.ASHLAR.get().defaultBlockState() : whiteWall);
                        put(chunk, mp, x0, z0, wx, rTop + dy, wz, kst);
                    } else {
                        put(chunk, mp, x0, z0, wx, rTop + dy, wz, dy == 1 ? darkPlank : Blocks.AIR.defaultBlockState());
                    }
                }
                int distK = Math.abs(kz);
                int ry = rTop + 8 - distK;
                put(chunk, mp, x0, z0, wx, ry, wz,
                        distK == 0 ? ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState()
                                : stair(ElisarBlocks.ROOF_BLUE_STAIRS.get().defaultBlockState(),
                                kz < 0 ? Direction.SOUTH : Direction.NORTH, Half.BOTTOM));
                if (kwX && distK < 3) {
                    for (int fy = rTop + 5; fy < ry; ++fy) {
                        put(chunk, mp, x0, z0, wx, fy, wz, whiteWall);
                    }
                }
            }
        }
        for (int cy = rTop + 5; cy <= rTop + 10; ++cy) {
            put(chunk, mp, x0, z0, cx - 8, cy, cz - 2, CastleBlocks.ASHLAR.get().defaultBlockState());
        }
        for (int dx = -5; dx <= 5; ++dx) {
            for (int dz = -5; dz <= 5; ++dz) {
                double d = Math.hypot(dx, dz);
                int wx = cx + dx, wz = cz + dz;
                if (d <= 4.7) {
                    put(chunk, mp, x0, z0, wx, galY, wz, CastleBlocks.ASHLAR.get().defaultBlockState());
                    if (d >= 3.8) {
                        put(chunk, mp, x0, z0, wx, galY + 1, wz, bars);
                    }
                }
                // Стеклянный барабан фонаря с пылающим ядром морского фонаря и яркого кристалла Элисара
                if (d <= 3.1) {
                    for (int ly = 1; ly <= 4; ++ly) {
                        if (d <= 1.45) {
                            put(chunk, mp, x0, z0, wx, galY + ly, wz, (ly == 2 || ly == 3) ? beaconCrystal : seaLantern);
                        } else if (d >= 2.2) {
                            boolean mullion = (dx == 0 || dz == 0);
                            put(chunk, mp, x0, z0, wx, galY + ly, wz, mullion ? brass : glass);
                        } else {
                            put(chunk, mp, x0, z0, wx, galY + ly, wz, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                // Остроконечный синий купол и громоотвод на вершине маяка
                for (int cy = 5; cy <= 8; ++cy) {
                    double cupR = 3.6 - (cy - 5) * 1.05;
                    if (d <= cupR) {
                        put(chunk, mp, x0, z0, wx, galY + cy, wz, roofBlue);
                    }
                }
            }
        }
        put(chunk, mp, x0, z0, cx, galY + 9, cz, brass);
        put(chunk, mp, x0, z0, cx, galY + 10, cz, Blocks.LIGHTNING_ROD.defaultBlockState());

        // Скромные сундуки смотрителя маяка (внизу и на смотровом ярусе) + в каюте каравеллы
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 2, rTop + 1, cz, Direction.WEST, LOOT_MODEST_MARITIME);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 1, galY + 1, cz, Direction.EAST, LOOT_MODEST_MARITIME);
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #5: АЛТАРЬ ВЕКОВОГО ДРЕВА И КАМЕННЫЙ КРУГ КОРНЕЙ (Роща Сизо)
    // -------------------------------------------------------------------------
    private static void buildAncientTreeAltar(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                              RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState mossyStone = ElisarBlocks.STONE_MOSSY.get().defaultBlockState();
        BlockState mossyBrick = ElisarBlocks.BRICKS_MOSSY.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState sizoLogY = ElisarBlocks.SIZO_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState sizoLeaves = ElisarBlocks.SIZO_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState moss = ElisarBlocks.MOSS.get().defaultBlockState();
        BlockState flowers = ElisarBlocks.FLOWERS.get().defaultBlockState();
        BlockState candle = TempleBlocks.RITUAL_CANDLE.get().defaultBlockState();
        BlockState lanternHang = ElisarBlocks.LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState chain = ElisarBlocks.CHAIN.get().defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Утопленное круглое замшелое святилище с мягким лесным переходом в Рощу Сизо (r <= 17.5, v0.23.0!)
        BlockState cobbleStair = ElisarBlocks.COBBLE_STAIRS.get().defaultBlockState();
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 17.5) continue;
                if (r > 13.4) {
                    // Мягкий мшисто-корневой пояс с папоротниками и цветами, сливающий круг святилища с лесом Сизо!
                    int gTop = safeGroundY(plan, x, z, by - 1);
                    double h = ElisarChunkGenerator.hash(x, z, 631);
                    if (gTop > plan.ocean + 3 && h < (17.5 - r) / 4.1) {
                        put(chunk, mp, x0, z0, x, gTop, z, h < 0.45 ? moss : mossyStone);
                        if (h < 0.22) {
                            put(chunk, mp, x0, z0, x, gTop + 1, z, h < 0.10 ? flowers : Blocks.FERN.defaultBlockState());
                        }
                    }
                    continue;
                }

                int colTop = safeGroundY(plan, x, z, by - 4);
                for (int y = by; y <= by + 16; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 3); y < by - 1; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, mossyStone);
                }

                boolean stepRing = r >= 10.6 && r < 11.4;
                boolean outerRim = r >= 11.4 && r <= 13.4;
                int floorY = outerRim ? by : by - 1;
                if (stepRing) {
                    Direction inDir = Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, x, by - 1, z, mossyStone);
                    put(chunk, mp, x0, z0, x, by, z, stair(cobbleStair, inDir.getOpposite(), Half.BOTTOM));
                } else {
                    BlockState tile = (Math.abs(r - 6.8) < 0.55 || (Math.abs(dx) == 0 && r < 10.5) || (Math.abs(dz) == 0 && r < 10.5))
                            ? goldRune
                            : ((dx * 7 + dz * 13) % 5 == 0 ? moss : ((dx + dz) % 2 == 0 ? mossyBrick : floor));
                    put(chunk, mp, x0, z0, x, floorY, z, tile);
                    if (!outerRim && r > 4.2 && r < 10.2 && ((dx * 31 ^ dz * 17) & 15) == 0) {
                        put(chunk, mp, x0, z0, x, floorY + 1, z, flowers);
                    }
                }
            }
        }

        // 2) Каменный Круг из 8 резных замшелых менгиров и переплетённых корневых арок по кольцу r = 11.4
        for (int i = 0; i < 8; ++i) {
            double ang = (Math.PI * 2.0 * i) / 8.0;
            int mx = cx + (int) Math.round(Math.cos(ang) * 11.4);
            int mz = cz + (int) Math.round(Math.sin(ang) * 11.4);
            int mh = 5 + (i % 2) * 2;
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (Math.abs(dx) == 1 && Math.abs(dz) == 1) continue;
                    int hHere = (dx == 0 && dz == 0) ? mh : mh - 2;
                    for (int dy = 1; dy <= hHere; ++dy) {
                        BlockState st = (dx == 0 && dz == 0 && dy == mh - 1) ? goldRune
                                : (dy % 2 == 0 ? keepMossy : mossyBrick);
                        put(chunk, mp, x0, z0, mx + dx, by + dy, mz + dz, st);
                    }
                }
            }
            // Изогнутые живые корни, тянущиеся от Векового Древа к каждому менгиру круга!
            for (int s = 3; s <= 10; ++s) {
                double u = s / 11.4;
                int rx = cx + (int) Math.round(Math.cos(ang) * s);
                int rz = cz + (int) Math.round(Math.sin(ang) * s);
                int archY = by + (int) Math.round(2.4 * Math.sin(u * Math.PI) + (1.0 - u) * 2.2);
                put(chunk, mp, x0, z0, rx, archY, rz, sizoLogY);
                if (s == 6) {
                    put(chunk, mp, x0, z0, rx, archY - 1, rz, lanternHang);
                }
            }
        }

        // 3) Исполинское Вековое Материнское Древо Сизо в центре с полым корневым гротом и Живым Алтарём!
        for (int dy = 0; dy <= 21; ++dy) {
            double tr = dy < 4 ? (3.0 - dy * 0.30) : (dy < 14 ? 1.65 : 1.15);
            for (int dx = -3; dx <= 3; ++dx) {
                for (int dz = -3; dz <= 3; ++dz) {
                    double d = Math.hypot(dx, dz);
                    if (d > tr) continue;
                    // Арочная ниша Живого Алтаря внутри комля с южной и северной сторон (dy = 0..3)
                    if (dy <= 3 && (d < 1.45 || (Math.abs(dx) <= 1 && Math.abs(dz) >= 1 && dy <= 2))) {
                        continue;
                    }
                    put(chunk, mp, x0, z0, cx + dx, by + dy, cz + dz, sizoLogY);
                }
            }
        }

        // Алтарь в сердце корней Векового Древа
        put(chunk, mp, x0, z0, cx, by, cz, Blocks.SEA_LANTERN.defaultBlockState());
        put(chunk, mp, x0, z0, cx, by + 1, cz, mosaicCore);
        put(chunk, mp, x0, z0, cx - 1, by, cz, goldRune);
        put(chunk, mp, x0, z0, cx + 1, by, cz, goldRune);
        put(chunk, mp, x0, z0, cx - 1, by + 1, cz, candle);
        put(chunk, mp, x0, z0, cx + 1, by + 1, cz, candle);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx, by, cz - 1, Direction.SOUTH, LOOT_MODEST_WAYFARER);

        // 3 широких плоских яруса листвы Векового Древа + свисающие на цепях фонари Рощи Сизо
        int[][] tiers = {
                {15, 9},
                {19, 11},
                {22, 7}
        };
        for (int[] tier : tiers) {
            int cy = by + tier[0];
            int cr = tier[1];
            // Толстые ветви к кроне
            for (int b = 0; b < 6; ++b) {
                double ba = (Math.PI * 2.0 * b) / 6.0 + tier[0] * 0.3;
                for (int s = 2; s <= cr - 2; ++s) {
                    int bx = cx + (int) Math.round(Math.cos(ba) * s);
                    int bz = cz + (int) Math.round(Math.sin(ba) * s);
                    put(chunk, mp, x0, z0, bx, cy - 1, bz, sizoLogY);
                }
                int lx = cx + (int) Math.round(Math.cos(ba) * (cr - 2));
                int lz = cz + (int) Math.round(Math.sin(ba) * (cr - 2));
                put(chunk, mp, x0, z0, lx, cy - 2, lz, chain);
                put(chunk, mp, x0, z0, lx, cy - 3, lz, lanternHang);
            }
            for (int dx = -cr; dx <= cr; ++dx) {
                for (int dz = -cr; dz <= cr; ++dz) {
                    double d = Math.hypot(dx, dz);
                    if (d <= cr + 0.4) {
                        put(chunk, mp, x0, z0, cx + dx, cy, cz + dz, sizoLeaves);
                        if (d <= cr * 0.72) {
                            put(chunk, mp, x0, z0, cx + dx, cy + 1, cz + dz, sizoLeaves);
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #4: ПЕПЕЛЬНЫЙ МАЯК-КОЛОКОЛЬНЯ И КУРГАН ПАВШИХ СТРАЖЕЙ (Адские угодья)
    // -------------------------------------------------------------------------
    private static void buildAshenBelfryBarrow(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                               RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState scorched = CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
        BlockState burntSoil = CastleBlocks.BURNT_SOIL.get().defaultBlockState();
        BlockState ash = CastleBlocks.ASH.get().defaultBlockState();
        BlockState castleBrick = CastleBlocks.BRICKS.get().defaultBlockState();
        BlockState crackedBrick = CastleBlocks.BRICKS_CRACKED.get().defaultBlockState();
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState ashlarPillar = CastleBlocks.ASHLAR_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState brass = CastleBlocks.BRASS.get().defaultBlockState();
        BlockState gold = TempleBlocks.GOLD.get().defaultBlockState();
        BlockState bars = CastleBlocks.BARS.get().defaultBlockState();
        BlockState chain = ElisarBlocks.CHAIN.get().defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        BlockState soulLantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState soulStand = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Центры двух частей ансамбля: Курган Павших Стражей (cx - 7, cz) и Пепельная Колокольня (cx + 8, cz)
        int bxCenter = cx - 7, bzCenter = cz;
        int txCenter = cx + 8, tzCenter = cz;

        // 1) Вымощенная обожжённым камнем мемориальная площадь с плавной пепельно-базальтовой каймой (r <= 24.5, v0.23.0!)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double dAll = Math.hypot(x - cx, z - cz);
                if (dAll > 24.5) continue;
                double hAsh = ElisarChunkGenerator.hash(x, z, 645);
                if (dAll > 20.2) {
                    if (AshenLands.weight(x + 0.5, z + 0.5) > 0.45 && hAsh < (24.5 - dAll) / 4.3) {
                        put(chunk, mp, x0, z0, x, by - 1, z, hAsh < 0.35 ? ash : (hAsh < 0.7 ? burntSoil : scorched));
                    }
                    continue;
                }

                int seaFloor = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, x, z));
                int baseStart = AshenLands.weight(x + 0.5, z + 0.5) > 0.45 ? (by - 4) : seaFloor;
                for (int y = baseStart; y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, scorched);
                }
                for (int y = by; y <= by + 29; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                put(chunk, mp, x0, z0, x, by - 1, z,
                        hAsh < 0.18 ? ash : (hAsh < 0.36 ? blackstone : (hAsh < 0.55 ? crackedBrick : scorched)));

                // Купол Кургана (r <= 9.6 вокруг bxCenter, bzCenter) с подземно-купольным склепом внутри!
                int dxB = x - bxCenter, dzB = z - bzCenter;
                double rB = Math.hypot(dxB, dzB);
                if (rB <= 9.6) {
                    int moundH = (int) Math.round(6.8 * Math.cos((rB / 9.6) * (Math.PI * 0.5)));
                    for (int dy = 0; dy <= moundH; ++dy) {
                        // Внутренний сводчатый склеп (|dxB| <= 3, |dzB| <= 3, dy <= 3) + восточный входной дромос к колокольне!
                        boolean cryptRoom = Math.abs(dxB) <= 3 && Math.abs(dzB) <= 3 && dy <= 3;
                        boolean dromos = dxB >= 3 && Math.abs(dzB) <= 1 && dy <= 2;
                        if (cryptRoom || dromos) {
                            put(chunk, mp, x0, z0, x, by + dy, z, air);
                        } else {
                            BlockState mSt = (dy == moundH) ? ((dxB + dzB) % 3 == 0 ? ash : burntSoil)
                                    : (dy <= 1 ? blackstone : crackedBrick);
                            put(chunk, mp, x0, z0, x, by + dy, z, mSt);
                        }
                    }
                    // Навершие кургана: вечный жертвенник с огнём душ
                    if (dxB == 0 && dzB == 0) {
                        put(chunk, mp, x0, z0, x, by + 7, z, blackstone);
                        put(chunk, mp, x0, z0, x, by + 8, z, Blocks.SOUL_CAMPFIRE.defaultBlockState());
                    }
                }
            }
        }

        // Саркофаг Первого Стража и реликвии в центре склепа кургана
        for (int dx = -1; dx <= 1; ++dx) {
            put(chunk, mp, x0, z0, bxCenter + dx, by, bzCenter, CastleBlocks.ASHLAR.get().defaultBlockState());
            put(chunk, mp, x0, z0, bxCenter + dx, by + 1, bzCenter, CastleBlocks.ASHLAR_SLAB.get().defaultBlockState());
        }
        put(chunk, mp, x0, z0, bxCenter, by + 3, bzCenter, soulLantern);
        putLootChest(level, chunk, mp, rnd, x0, z0, bxCenter, by, bzCenter - 2, Direction.SOUTH, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx, by + 1, cz + 1, Direction.NORTH, LOOT_MODEST_WAYFARER);

        // Кольцо из 10 каменных кенотафов-мечей Павших Стражей вокруг кургана (r = 10.8)
        for (int i = 0; i < 10; ++i) {
            double ang = (Math.PI * 2.0 * i) / 10.0;
            int sx = bxCenter + (int) Math.round(Math.cos(ang) * 10.8);
            int sz = bzCenter + (int) Math.round(Math.sin(ang) * 10.8);
            if (Math.abs(sz - bzCenter) <= 1 && sx > bxCenter) continue; // не загораживаем вход в склеп
            put(chunk, mp, x0, z0, sx, by - 1, sz, magma);
            put(chunk, mp, x0, z0, sx, by, sz, blackstone);
            put(chunk, mp, x0, z0, sx, by + 1, sz, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            put(chunk, mp, x0, z0, sx, by + 2, sz, bars);
            put(chunk, mp, x0, z0, sx, by + 3, sz, (i % 2 == 0) ? soulStand : bars);
        }

        // 2) Высокая 27-блочная готическая Пепельная Маяк-Колокольня (txCenter, tzCenter)
        for (int dy = 0; dy <= 26; ++dy) {
            int y = by + dy;
            int hw = dy < 6 ? 4 : (dy < 22 ? 3 : 2);
            for (int dx = -hw; dx <= hw; ++dx) {
                for (int dz = -hw; dz <= hw; ++dz) {
                    int wx = txCenter + dx, wz = tzCenter + dz;
                    boolean corner = Math.abs(dx) == hw && Math.abs(dz) == hw;
                    boolean edge = Math.abs(dx) == hw || Math.abs(dz) == hw;

                    if (dy <= 14) {
                        // Каменный ствол колокольни с контрфорсами и готическими бойницами
                        if (!edge) continue;
                        if (dy <= 3 && dx == -hw && Math.abs(dz) <= 1) continue; // западный портал к кургану
                        if (dy >= 7 && dy <= 11 && ((dx == 0 && Math.abs(dz) == hw) || (dz == 0 && Math.abs(dx) == hw))) {
                            put(chunk, mp, x0, z0, wx, y, wz, bars);
                        } else {
                            put(chunk, mp, x0, z0, wx, y, wz, corner ? ashlarPillar : (dy % 4 == 0 ? blackstone : castleBrick));
                        }
                    } else if (dy <= 21) {
                        // Открытый арочный ярус Звонницы (только угловые устои и стрельчатое завершение)
                        if (corner || (dy == 15 && edge) || (dy == 21 && edge)) {
                            put(chunk, mp, x0, z0, wx, y, wz, corner ? ashlarPillar : blackstone);
                        }
                    } else {
                        // Огненная чаша-маяк на вершине колокольни (dy = 22 .. 26)
                        if (dy == 22) {
                            put(chunk, mp, x0, z0, wx, y, wz, blackstone);
                        } else if (dy <= 24 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                            put(chunk, mp, x0, z0, wx, y, wz, dy == 23 ? magma : Blocks.SHROOMLIGHT.defaultBlockState());
                        } else if (dy <= 25 && corner) {
                            put(chunk, mp, x0, z0, wx, y, wz, bars);
                        } else if (dy == 25 && dx == 0 && dz == 0) {
                            put(chunk, mp, x0, z0, wx, y, wz, Blocks.CAMPFIRE.defaultBlockState());
                        }
                    }
                }
            }
        }
        // Огромный подвешенный бронзово-золотой Вечевой Колокол внутри звонницы (by + 16 .. by + 20)!
        put(chunk, mp, x0, z0, txCenter, by + 20, tzCenter, chain);
        put(chunk, mp, x0, z0, txCenter, by + 19, tzCenter, brass);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                put(chunk, mp, x0, z0, txCenter + dx, by + 18, tzCenter + dz, (dx == 0 && dz == 0) ? gold : brass);
                if (Math.abs(dx) + Math.abs(dz) >= 1) {
                    put(chunk, mp, x0, z0, txCenter + dx, by + 17, tzCenter + dz, gold);
                }
            }
        }
        put(chunk, mp, x0, z0, txCenter, by + 16, tzCenter, soulLantern);
        // v0.23.0: Рельефные ступенчатые готические контрфорсы и стрельчатые фронтоны у Пепельной Колокольни!
        BlockState bsStair = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState();
        BlockState bsWall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        for (Direction d4 : Direction.Plane.HORIZONTAL) {
            int sx = d4.getStepX(), sz = d4.getStepZ();
            for (int oy = 0; oy <= 6; ++oy) {
                put(chunk, mp, x0, z0, txCenter + sx * 5, by + oy, tzCenter + sz * 4, oy == 6 ? stair(bsStair, d4.getOpposite(), Half.BOTTOM) : blackstone);
                put(chunk, mp, x0, z0, txCenter + sx * 4, by + oy, tzCenter + sz * 5, oy == 6 ? stair(bsStair, d4.getOpposite(), Half.BOTTOM) : blackstone);
            }
            for (int oy = 7; oy <= 13; ++oy) {
                put(chunk, mp, x0, z0, txCenter + sx * 4, by + oy, tzCenter + sz * 3, oy == 13 ? stair(bsStair, d4.getOpposite(), Half.BOTTOM) : bsWall);
                put(chunk, mp, x0, z0, txCenter + sx * 3, by + oy, tzCenter + sz * 4, oy == 13 ? stair(bsStair, d4.getOpposite(), Half.BOTTOM) : bsWall);
            }
            put(chunk, mp, x0, z0, txCenter + sx * 4, by + 14, tzCenter, stair(bsStair, d4.getOpposite(), Half.TOP));
            put(chunk, mp, x0, z0, txCenter, by + 14, tzCenter + sz * 4, stair(bsStair, d4.getOpposite(), Half.TOP));
        }
    }

    // -------------------------------------------------------------------------
    // НОВАЯ СТРУКТУРА v0.23.0: ГОТИЧЕСКАЯ СГОРЕВШАЯ ПЕПЕЛЬНАЯ ЦЕРКОВЬ (Адские угодья)
    // -------------------------------------------------------------------------
    private static void buildAshenBurnedChurch(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                               RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState scorched = CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
        BlockState burntSoil = CastleBlocks.BURNT_SOIL.get().defaultBlockState();
        BlockState ash = CastleBlocks.ASH.get().defaultBlockState();
        BlockState brick = CastleBlocks.BRICKS.get().defaultBlockState();
        BlockState cracked = CastleBlocks.BRICKS_CRACKED.get().defaultBlockState();
        BlockState brickWall = CastleBlocks.BRICKS_WALL.get().defaultBlockState();
        BlockState brickStair = CastleBlocks.BRICKS_STAIRS.get().defaultBlockState();
        BlockState ashlar = CastleBlocks.ASHLAR.get().defaultBlockState();
        BlockState ashlarStair = CastleBlocks.ASHLAR_STAIRS.get().defaultBlockState();
        BlockState ashlarPillar = CastleBlocks.ASHLAR_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState bsCracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState bsStair = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState();
        BlockState bsWall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        BlockState charredY = CastleBlocks.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState charredX = CastleBlocks.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState charredZ = CastleBlocks.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState darkStair = CastleBlocks.DARK_STAIRS.get().defaultBlockState();
        BlockState darkSlab = CastleBlocks.DARK_SLAB.get().defaultBlockState();
        BlockState stainedRed = CastleBlocks.STAINED_GLASS.get().defaultBlockState();
        BlockState bars = CastleBlocks.BARS.get().defaultBlockState();
        BlockState chain = ElisarBlocks.CHAIN.get().defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        BlockState soulHang = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState soulStand = Blocks.SOUL_LANTERN.defaultBlockState();
        BlockState candle = TempleBlocks.RITUAL_CANDLE.get().defaultBlockState();
        BlockState carpet = CastleBlocks.CARPET.get().defaultBlockState();
        BlockState cobweb = CastleBlocks.COBWEB.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Погост и пепельный двор базилики с плавным переходом в рельеф Адских угодий (r <= 24.5)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx * 0.85, dz * 1.05);
                if (r > 24.5) continue;
                double h = ElisarChunkGenerator.hash(x, z, 677);
                if (r > 19.8) {
                    if (AshenLands.weight(x + 0.5, z + 0.5) > 0.40 && h < (24.5 - r) / 4.7) {
                        put(chunk, mp, x0, z0, x, by - 1, z, h < 0.35 ? ash : (h < 0.72 ? burntSoil : scorched));
                    }
                    continue;
                }
                int seaFloor = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, x, z));
                int baseStart = AshenLands.weight(x + 0.5, z + 0.5) > 0.45 ? (by - 4) : seaFloor;
                for (int y = baseStart; y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, scorched);
                }
                for (int y = by; y <= by + 25; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                // Каменная процессионная дорожка с запада к порталу (dz: -2..2, dx: -19..-13) и пепельный погост вокруг
                boolean walk = (dx >= -19 && dx <= -13 && Math.abs(dz) <= 2);
                BlockState yard = walk ? (h < 0.45 ? bsCracked : blackstone)
                        : (h < 0.25 ? ash : (h < 0.58 ? burntSoil : scorched));
                put(chunk, mp, x0, z0, x, by - 1, z, yard);

                // Кованая ограда погоста с обрушенными пролётами по периметру двора
                boolean fenceRing = (Math.abs(dx) == 18 && Math.abs(dz) <= 13 && Math.abs(dz) > 2)
                        || (Math.abs(dz) == 13 && Math.abs(dx) <= 18);
                if (fenceRing && h > 0.22) {
                    boolean post = (dx % 4 == 0 && dz % 4 == 0) || (Math.abs(dx) == 18 && Math.abs(dz) == 3);
                    put(chunk, mp, x0, z0, x, by, z, post ? blackstone : bsWall);
                    if (post) {
                        put(chunk, mp, x0, z0, x, by + 1, z, bsWall);
                        if (Math.abs(dx) == 18 && Math.abs(dz) == 3) {
                            put(chunk, mp, x0, z0, x, by + 2, z, soulStand);
                        }
                    } else if (h > 0.42) {
                        put(chunk, mp, x0, z0, x, by + 1, z, bars);
                    }
                }
                // Покосившиеся надгробия на северном и южном кладбищах церкви (|dz| >= 9 .. 11, |dx| <= 11)
                if (Math.abs(dz) >= 9 && Math.abs(dz) <= 11 && Math.abs(dx) <= 10 && (dx % 4 == 0) && (dz % 2 != 0) && h > 0.30) {
                    put(chunk, mp, x0, z0, x, by, z, bsWall);
                    if (h > 0.62) put(chunk, mp, x0, z0, x, by + 1, z, CastleBlocks.ASHLAR_SLAB.get().defaultBlockState());
                }
            }
        }

        // 2) Крестообразный план Готической Базилики: Неф (dx: -12..+8, |dz| <= 6), Трансепт (dx: +1..+6, |dz| <= 9), Восточная Апсида (dx: +8..+13)
        for (int dx = -13; dx <= 14; ++dx) {
            for (int dz = -10; dz <= 10; ++dz) {
                int wx = cx + dx, wz = cz + dz;
                boolean inNave = dx >= -12 && dx <= 8 && Math.abs(dz) <= 6;
                boolean inTransept = dx >= 1 && dx <= 6 && Math.abs(dz) <= 9;
                double dApse = Math.hypot(dx - 8.0, dz);
                boolean inApse = dx >= 8 && dApse <= 5.6;
                if (!inNave && !inTransept && !inApse) {
                    // Внешние ступенчатые контрфорсы вдоль стен нефа (dx = -10, -6, -2, +7; |dz| == 7)
                    if ((dx == -10 || dx == -6 || dx == -2 || dx == 7) && Math.abs(dz) == 7) {
                        Direction outZ = dz > 0 ? Direction.SOUTH : Direction.NORTH;
                        for (int dy = 0; dy <= 8; ++dy) {
                            BlockState bst = dy == 8 ? stair(bsStair, outZ.getOpposite(), Half.BOTTOM)
                                    : (dy >= 5 ? bsWall : blackstone);
                            put(chunk, mp, x0, z0, wx, by + dy, wz, bst);
                        }
                    }
                    continue;
                }

                // Пол храма: возвышенная алтарная солея (dx >= 7 -> by+1), узорчатый чёрно-серый пол и ковровая дорожка с проплешинами пепла!
                int floorY = dx >= 7 ? by + 1 : by;
                double hf = ElisarChunkGenerator.hash(wx, wz, 681);
                BlockState floorSt = (dx == 7 && Math.abs(dz) <= 4)
                        ? stair(ashlarStair, Direction.EAST, Half.BOTTOM)
                        : (hf < 0.16 ? ash : (((dx + dz) & 1) == 0 ? blackstone : ashlar));
                for (int fy = by; fy <= floorY; ++fy) {
                    put(chunk, mp, x0, z0, wx, fy, wz, fy == floorY ? floorSt : scorched);
                }
                if (Math.abs(dz) <= 1 && dx >= -11 && dx <= 6 && hf > 0.35) {
                    put(chunk, mp, x0, z0, wx, floorY + 1, wz, carpet);
                }

                // Определяем наружную стену базилики
                boolean naveWall = inNave && !inTransept && Math.abs(dz) == 6;
                boolean westWall = dx == -12 && Math.abs(dz) <= 6;
                boolean transWall = inTransept && (Math.abs(dz) == 9 || ((dx == 1 || dx == 6) && Math.abs(dz) > 6));
                boolean apseWall = inApse && dApse >= 4.45;
                boolean extWall = naveWall || westWall || transWall || apseWall;

                if (extWall) {
                    // Захватывающий рваный профиль обрушения стен в центре нефа и северном трансепте!
                    int wallTop = westWall ? (14 - Math.abs(dz) / 2)
                            : apseWall ? 11
                            : (dz < 0 && dx >= -5 && dx <= 0) ? (7 + Math.abs(dx + 2))
                            : 11;
                    for (int dy = 1; dy <= wallTop; ++dy) {
                        // Высокий стрельчатый западный портал (dx == -12, |dz| <= 1, dy <= 4)
                        if (westWall && Math.abs(dz) <= 1 && dy <= 4) {
                            if (dy == 4 && Math.abs(dz) == 1) {
                                put(chunk, mp, x0, z0, wx, by + dy, wz, stair(bsStair, dz < 0 ? Direction.SOUTH : Direction.NORTH, Half.TOP));
                            }
                            continue;
                        }
                        // Круглое готическое окно-роза на западном фасаде (dx == -12, |dz| <= 2, dy = 7..11)
                        if (westWall && Math.hypot(dz, dy - 9.0) <= 2.3) {
                            put(chunk, mp, x0, z0, wx, by + dy, wz, (dz == 0 || dy == 9) ? bars : stainedRed);
                            continue;
                        }
                        // Узкие стрельчатые витражные окна-ланцеты между контрфорсами нефа и апсиды
                        boolean lancet = (naveWall && (dx == -8 || dx == -4 || dx == 0) && dy >= 4 && dy <= 8)
                                || (transWall && Math.abs(dz) == 9 && (dx == 3 || dx == 4) && dy >= 4 && dy <= 9)
                                || (apseWall && Math.abs(dz) <= 1 && dy >= 4 && dy <= 8);
                        if (lancet) {
                            double hw = ElisarChunkGenerator.hash(wx, by + dy, wz);
                            put(chunk, mp, x0, z0, wx, by + dy, wz, hw < 0.35 ? air : (hw < 0.72 ? stainedRed : bars));
                            continue;
                        }
                        double hm = ElisarChunkGenerator.hash(wx, by + dy, wz);
                        BlockState wSt = (dy == 1 || dy == wallTop) ? bsCracked
                                : (hm < 0.44 ? cracked : (hm < 0.78 ? brick : scorched));
                        put(chunk, mp, x0, z0, wx, by + dy, wz, wSt);
                    }
                } else {
                    // Внутренние колонны нефа (|dz| == 3, dx = -8, -4, 0) и обугленные скамьи прихожан!
                    if (Math.abs(dz) == 3 && (dx == -8 || dx == -4 || dx == 0 || dx == 4)) {
                        int cTop = (dx == -4 && dz < 0) ? 5 : 9;
                        for (int dy = 1; dy <= cTop; ++dy) {
                            put(chunk, mp, x0, z0, wx, by + dy, wz, dy == cTop && cTop < 9 ? cracked : ashlarPillar);
                        }
                    } else if (dx >= -9 && dx <= -1 && (dx % 3 != 0) && Math.abs(dz) >= 2 && Math.abs(dz) <= 4 && Math.abs(dz) != 3) {
                        if (ElisarChunkGenerator.hash(wx, wz, 689) > 0.28) {
                            put(chunk, mp, x0, z0, wx, by + 1, wz, stair(darkStair, Direction.WEST, Half.BOTTOM));
                        }
                    }
                }

                // Обугленные стропила и полуобрушенная сводчатая крыша базилики (пролом неба над средокрестием dx = -3..+4!)
                boolean roofHole = (dx >= -3 && dx <= 4 && Math.abs(dz) <= 3);
                if (inNave && !roofHole && Math.abs(dz) <= 6) {
                    int ry = by + 15 - Math.abs(dz) / 2;
                    boolean rafter = (dx % 4 == 0);
                    if (rafter || ElisarChunkGenerator.hash(wx, wz, 693) > 0.42) {
                        BlockState rSt = rafter ? (dz == 0 ? charredX : charredZ)
                                : (dz == 0 ? darkSlab : stair(darkStair, dz < 0 ? Direction.SOUTH : Direction.NORTH, Half.BOTTOM));
                        put(chunk, mp, x0, z0, wx, ry, wz, rSt);
                    }
                }
            }
        }

        // 3) Две готические угловые башни западного фасада (южная уцелела со шпилем до by+22, северная обломлена на by+14!)
        for (int side = -1; side <= 1; side += 2) {
            int tzBase = cz + side * 6;
            int txBase = cx - 12;
            int tMax = (side > 0) ? 18 : 13;
            for (int dy = 0; dy <= tMax; ++dy) {
                for (int tx = -2; tx <= 2; ++tx) {
                    for (int tz = -2; tz <= 2; ++tz) {
                        boolean corner = Math.abs(tx) == 2 && Math.abs(tz) == 2;
                        boolean edge = Math.abs(tx) == 2 || Math.abs(tz) == 2;
                        if (!edge) continue;
                        int wx = txBase + tx, wz = tzBase + tz;
                        if (side < 0 && dy >= 12 && ElisarChunkGenerator.hash(wx, by + dy, wz) < 0.45) continue;
                        if (dy >= 13 && dy <= 16 && ((tx == 0 && Math.abs(tz) == 2) || (tz == 0 && Math.abs(tx) == 2))) {
                            put(chunk, mp, x0, z0, wx, by + dy, wz, bars);
                            continue;
                        }
                        put(chunk, mp, x0, z0, wx, by + dy, wz, corner ? bsWall : (dy % 4 == 0 ? blackstone : cracked));
                    }
                }
            }
            if (side > 0) {
                // Остроконечный обугленный шатёр южной колокольни (by + 19 .. by + 23) + церковный колокол!
                for (int sy = 19; sy <= 23; ++sy) {
                    int sr = sy <= 20 ? 1 : 0;
                    for (int tx = -sr; tx <= sr; ++tx) {
                        for (int tz = -sr; tz <= sr; ++tz) {
                            put(chunk, mp, x0, z0, txBase + tx, by + sy, tzBase + tz, sy == 23 ? bars : charredY);
                        }
                    }
                }
                put(chunk, mp, x0, z0, txBase, by + 17, tzBase, chain);
                put(chunk, mp, x0, z0, txBase, by + 16, tzBase, CastleBlocks.BRASS.get().defaultBlockState());
                put(chunk, mp, x0, z0, txBase, by + 15, tzBase, soulHang);
            }
        }
        // Резной портальный вимперг перед западным входом (dx == -13, |dz| <= 2)
        for (int dz = -2; dz <= 2; ++dz) {
            if (Math.abs(dz) == 2) {
                for (int dy = 1; dy <= 5; ++dy) put(chunk, mp, x0, z0, cx - 13, by + dy, cz + dz, ashlarPillar);
            }
            put(chunk, mp, x0, z0, cx - 13, by + 6 - Math.abs(dz), cz + dz,
                    dz == 0 ? blackstone : stair(bsStair, dz < 0 ? Direction.SOUTH : Direction.NORTH, Half.BOTTOM));
        }

        // Главный осквернённый Алтарь в восточной апсиде + канделябры душ + 2 скромных сундука (в ризнице и за алтарём)
        for (int dz = -1; dz <= 1; ++dz) {
            put(chunk, mp, x0, z0, cx + 10, by + 2, cz + dz, dz == 0 ? CastleBlocks.MOSAIC.get().defaultBlockState() : blackstone);
        }
        put(chunk, mp, x0, z0, cx + 10, by + 3, cz - 1, candle);
        put(chunk, mp, x0, z0, cx + 10, by + 3, cz + 1, candle);
        put(chunk, mp, x0, z0, cx + 10, by + 3, cz, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        put(chunk, mp, x0, z0, cx + 4, by + 8, cz, chain);
        put(chunk, mp, x0, z0, cx + 4, by + 7, cz, soulHang);
        put(chunk, mp, x0, z0, cx - 10, by + 6, cz + 4, cobweb);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 11, by + 2, cz, Direction.WEST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 4, by + 1, cz - 8, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #6: РАСКОЛОТАЯ КУЗНЯ ОСКОЛКОВ (Резонаторная Мастерская в Хрустальных разломах)
    // -------------------------------------------------------------------------
    private static void buildShatteredShardForge(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                 RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        BlockState marble = ElisarBlocks.RIFT_MARBLE.get().defaultBlockState();
        BlockState slate = ElisarBlocks.RIFT_SLATE.get().defaultBlockState();
        BlockState crystalDeep = ElisarBlocks.CRYSTAL_BLOCK_DEEP.get().defaultBlockState();
        BlockState crystal = ElisarBlocks.CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState crystalBright = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState brass = CastleBlocks.BRASS.get().defaultBlockState();
        BlockState gold = TempleBlocks.GOLD.get().defaultBlockState();
        BlockState pillar = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState water = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Мраморно-сланцевая платформа мастерской с естественными хрустальными террасами и друзами (r <= 16.8, v0.23.0!)
        BlockState riftMossy = ElisarBlocks.RIFT_MARBLE_MOSSY.get().defaultBlockState();
        BlockState riftTurf = ElisarBlocks.RIFT_TURF.get().defaultBlockState();
        BlockState riftStair = ElisarBlocks.RIFT_MARBLE_STAIRS.get().defaultBlockState();
        BlockState cluster = ElisarBlocks.CRYSTAL_CLUSTER.get().defaultBlockState();
        BlockState shroom = ElisarBlocks.CRYSTAL_SHROOM.get().defaultBlockState();
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 16.8) continue;
                double hc = ElisarChunkGenerator.hash(x, z, 709);
                if (r > 12.8) {
                    // Ступенчатый мраморно-кристальный пояс совместимости с Хрустальными разломами!
                    int gTop = safeGroundY(plan, x, z, by - 2);
                    if (gTop > plan.ocean + 4 && hc < (16.8 - r) / 4.0) {
                        put(chunk, mp, x0, z0, x, gTop, z, hc < 0.42 ? riftTurf : riftMossy);
                        if (hc < 0.20) {
                            put(chunk, mp, x0, z0, x, gTop + 1, z, hc < 0.11 ? cluster : shroom);
                        }
                    }
                    continue;
                }

                int colTop = safeGroundY(plan, x, z, by - 5);
                for (int y = by; y <= by + 18; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 4); y < by - 1; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, y % 3 == 0 ? slate : marble);
                }

                // Светящаяся энергетическая расселина Горна Осколков вдоль оси Z (|dx| <= 1, |dz| <= 7)
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 7) {
                    put(chunk, mp, x0, z0, x, by - 3, z, crystalDeep);
                    put(chunk, mp, x0, z0, x, by - 2, z, (dx == 0) ? crystalBright : crystal);
                    if (Math.abs(dz) == 3 || dz == 0) {
                        put(chunk, mp, x0, z0, x, by - 1, z, ElisarBlocks.RIFT_MARBLE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                    } else {
                        put(chunk, mp, x0, z0, x, by - 1, z, (dx == 0 && Math.abs(dz) <= 5) ? crystalBright : air);
                    }
                } else if (r >= 11.8) {
                    Direction inDir = Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, x, by - 1, z, stair(riftStair, inDir, Half.BOTTOM));
                } else {
                    boolean ring = Math.abs(r - 10.8) < 0.7 || Math.abs(r - 6.2) < 0.55;
                    BlockState fl = ring ? slate : ((Math.abs(dx) + Math.abs(dz)) % 6 == 0 ? gold : (hc < 0.25 ? riftMossy : marble));
                    put(chunk, mp, x0, z0, x, by - 1, z, fl);
                }
            }
        }

        // 2) Два высоких изогнутых Пилона-Камертона Резонатора (dx = -7 и +7), сходящихся к парящему Астральному Ядру!
        for (int side = -1; side <= 1; side += 2) {
            for (int dy = 0; dy <= 14; ++dy) {
                int shiftIn = dy < 6 ? 0 : (dy - 5) / 2;
                int px = cx + side * (7 - shiftIn);
                for (int dz = -1; dz <= 1; ++dz) {
                    BlockState st = (dz == 0 && dy % 3 == 2) ? brass
                            : (Math.abs(dz) == 1 && dy > 9) ? air
                            : (dy % 4 == 0 ? slate : marble);
                    if (!st.isAir()) {
                        put(chunk, mp, x0, z0, px, by + dy, cz + dz, st);
                    }
                }
                if (dy == 10) {
                    // Горизонтальные резонаторные лучи к парящему кристаллу
                    for (int bx = Math.min(px, cx + side * 2); bx <= Math.max(px, cx + side * 2); ++bx) {
                        put(chunk, mp, x0, z0, bx, by + 10, cz, Blocks.END_ROD.defaultBlockState());
                    }
                }
            }
        }

        // Парящий в воздухе на высоте by+7..by+12 необработанный Осколок Астрального Кристалла!
        for (int dy = 7; dy <= 13; ++dy) {
            int cr = (dy == 7 || dy == 13) ? 0 : ((dy == 9 || dy == 10 || dy == 11) ? 1 : 0);
            for (int dx = -cr; dx <= cr; ++dx) {
                for (int dz = -cr; dz <= cr; ++dz) {
                    if (Math.abs(dx) + Math.abs(dz) > cr + (dy == 10 ? 1 : 0)) continue;
                    BlockState cst = (dx == 0 && dz == 0) ? crystalBright : (dy <= 9 ? crystalDeep : crystal);
                    put(chunk, mp, x0, z0, cx + dx, by + dy, cz + dz, cst);
                }
            }
        }

        // 3) Крытая аркада Мастерской Кузнеца на севере платформы (dz = -11 .. -8, dx = -5 .. +5)
        for (int dx = -5; dx <= 5; ++dx) {
            for (int dz = -11; dz <= -8; ++dz) {
                put(chunk, mp, x0, z0, cx + dx, by + 5, cz + dz, (Math.abs(dx) == 5 || dz == -11) ? slate : marble);
            }
            if (Math.abs(dx) == 5 || Math.abs(dx) == 2) {
                for (int dy = 0; dy <= 4; ++dy) {
                    put(chunk, mp, x0, z0, cx + dx, by + dy, cz - 11, pillar);
                    if (Math.abs(dx) == 5) put(chunk, mp, x0, z0, cx + dx, by + dy, cz - 8, pillar);
                }
            }
        }
        // Наковальня, плавильные горны, точило, закалочная чаша с elisar_water и сундук мастера осколков!
        put(chunk, mp, x0, z0, cx - 3, by, cz - 10, Blocks.BLAST_FURNACE.defaultBlockState());
        put(chunk, mp, x0, z0, cx - 2, by, cz - 10, Blocks.BLAST_FURNACE.defaultBlockState());
        put(chunk, mp, x0, z0, cx, by, cz - 9, Blocks.ANVIL.defaultBlockState());
        put(chunk, mp, x0, z0, cx + 2, by, cz - 9, Blocks.SMITHING_TABLE.defaultBlockState());
        put(chunk, mp, x0, z0, cx + 3, by, cz - 9, Blocks.GRINDSTONE.defaultBlockState());
        // Закалочная купель воды Элисара в каменных бортах
        put(chunk, mp, x0, z0, cx - 4, by - 1, cz - 9, slate);
        put(chunk, mp, x0, z0, cx - 4, by, cz - 9, water);
        put(chunk, mp, x0, z0, cx - 5, by, cz - 9, slate);
        put(chunk, mp, x0, z0, cx - 3, by, cz - 9, slate);
        put(chunk, mp, x0, z0, cx - 4, by, cz - 8, slate);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 4, by, cz - 10, Direction.SOUTH, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 5, by, cz - 10, Direction.SOUTH, LOOT_MODEST_WAYFARER);
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #7: ОБСЕРВАТОРИЯ ФИОЛЕТОВЫХ ВЕТРОВ И ВОЗДУШНАЯ ПРИСТАНЬ (Фиолетовые холмы)
    // -------------------------------------------------------------------------
    private static void buildVioletWindObservatory(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                   RandomSource rnd, CityPlan plan, BiomeLandmark lm, int x0, int z0) {
        int cx = lm.cx, cz = lm.cz, by = lm.baseY;
        double ux = lm.dirX, uz = lm.dirZ;
        double vx = -uz, vz = ux;

        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState purpur = Blocks.PURPUR_PILLAR.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState gold = TempleBlocks.GOLD.get().defaultBlockState();
        BlockState brass = CastleBlocks.BRASS.get().defaultBlockState();
        BlockState pillar = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState darkPlank = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState darkBeamY = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState fence = ElisarBlocks.FENCE.get().defaultBlockState();
        BlockState crystalBright = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);
        BlockState lilacWool = Blocks.MAGENTA_WOOL.defaultBlockState();
        BlockState purpleWool = Blocks.PURPLE_WOOL.defaultBlockState();
        BlockState whiteWool = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Белокаменно-аметистовая Ротонда Обсерватории с цветущими лавандовыми террасами на склоне холма (r <= 14.2, v0.23.0!)
        BlockState templeStair = TempleBlocks.STAIRS.get().defaultBlockState();
        BlockState violetLeaves = ElisarBlocks.VIOLET_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState lavender = ElisarBlocks.LAVENDER_BED.get().defaultBlockState();
        BlockState lilacBloom = ElisarBlocks.LILAC_BLOOM.get().defaultBlockState();
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 14.2) continue;
                double hv = ElisarChunkGenerator.hash(x, z, 733);
                if (r > 8.6) {
                    // Мягкие лавандово-каменные террасы и стриженые фиолетовые кусты вокруг ротонды (не перекрывая Воздушную Пристань!)
                    double fwd = dx * ux + dz * uz;
                    if (fwd < 6.0) {
                        int gTop = safeGroundY(plan, x, z, by - 2);
                        if (gTop > plan.ocean + 5 && hv < (14.2 - r) / 5.6) {
                            put(chunk, mp, x0, z0, x, gTop, z, hv < 0.65 ? ElisarBlocks.GRASS.get().defaultBlockState() : ElisarBlocks.MOSS.get().defaultBlockState());
                            if (Math.abs(r - 10.2) < 0.55 && ((dx + dz) & 3) == 0) {
                                put(chunk, mp, x0, z0, x, gTop + 1, z, violetLeaves);
                            } else if (hv < 0.36) {
                                put(chunk, mp, x0, z0, x, gTop + 1, z, hv < 0.22 ? lavender : lilacBloom);
                            }
                        }
                    }
                    continue;
                }

                int colTop = safeGroundY(plan, x, z, by - 6);
                for (int y = by; y <= by + 20; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 4); y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, y == by - 1 && r > 7.8 ? ElisarBlocks.STONE_MOSSY.get().defaultBlockState() : marble);
                }
                if (r > 7.8) {
                    Direction inDir = Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(chunk, mp, x0, z0, x, by - 1, z, stair(templeStair, inDir, Half.BOTTOM));
                    if (Math.abs(Math.sin(Math.atan2(dz, dx) * 4.0)) < 0.16) {
                        // Выступающие пилястры-контрфорсы вокруг ротонды!
                        for (int py = 0; py <= 5; ++py) {
                            put(chunk, mp, x0, z0, x, by + py, z, py == 5 ? stair(templeStair, inDir, Half.BOTTOM) : pillar);
                        }
                    }
                    continue;
                }
                put(chunk, mp, x0, z0, x, by - 1, z, (Math.abs(r - 4.2) < 0.6) ? amethyst : ((dx == 0 || dz == 0) ? gold : floor));

                // Цилиндрическая колоннада обсерватории (by .. by + 9)
                if (r >= 6.4 && r <= 7.6) {
                    double projOut = (dx * ux + dz * uz) / Math.max(0.1, r);
                    boolean archDoor = projOut > 0.85 || projOut < -0.85;
                    for (int dy = 0; dy <= 9; ++dy) {
                        if (archDoor && dy <= 3) continue;
                        if (dy == 0 || dy == 5 || dy == 9) {
                            put(chunk, mp, x0, z0, x, by + dy, z, dy == 5 ? purpur : marble);
                        } else {
                            double ang = Math.atan2(dz, dx);
                            boolean colPos = Math.abs(Math.sin(ang * 6.0)) < 0.22;
                            if (colPos) {
                                put(chunk, mp, x0, z0, x, by + dy, z, pillar);
                            } else if (dy >= 2 && dy <= 4) {
                                put(chunk, mp, x0, z0, x, by + dy, z, ElisarBlocks.WINDOW.get().defaultBlockState());
                            }
                        }
                    }
                }
                // Перекрытие 2-го астрономического зала (by + 5) с центральным окулярным колодцем
                if (r < 6.5 && r > 2.2) {
                    put(chunk, mp, x0, z0, x, by + 5, z, floor);
                }

                // Полусферический астрономический купол (by + 10 .. by + 16) с широкой смотровой щелью под телескоп!
                for (int dy = 10; dy <= 16; ++dy) {
                    double domeH = (dy - 9.5) / 6.8;
                    double domeR = 7.4 * Math.sqrt(Math.max(0.0, 1.0 - domeH * domeH));
                    if (Math.abs(r - domeR) <= 0.78) {
                        // Открытая небесная щель купола в направлении ux, uz
                        double latOff = Math.abs(dx * vx + dz * vz);
                        double fwd = dx * ux + dz * uz;
                        if (fwd > -1.0 && latOff <= 1.35) continue;
                        boolean meridian = Math.abs(dx) <= 0 || Math.abs(dz) <= 0;
                        put(chunk, mp, x0, z0, x, by + dy, z, meridian ? gold : roofBlue);
                    }
                }
            }
        }

        // Гигантская латунно-хрустальная Труба Небесного Телескопа (наклонена на 35° вверх к небу Элисара!)
        put(chunk, mp, x0, z0, cx, by + 6, cz, brass);
        put(chunk, mp, x0, z0, cx, by + 7, cz, brass);
        for (int s = -2; s <= 7; ++s) {
            int tx = cx + (int) Math.round(ux * s);
            int tz = cz + (int) Math.round(uz * s);
            int ty = by + 8 + (int) Math.round(s * 0.68);
            BlockState tubeSt = (s == 7) ? crystalBright : (s == -2 ? gold : (s % 2 == 0 ? brass : purpur));
            put(chunk, mp, x0, z0, tx, ty, tz, tubeSt);
        }

        // Высокий шпиль Флюгера Фиолетовых Ветров с развевающимися лилово-белыми лентами!
        int wxBase = cx - (int) Math.round(ux * 5.0);
        int wzBase = cz - (int) Math.round(uz * 5.0);
        for (int dy = 10; dy <= 21; ++dy) {
            put(chunk, mp, x0, z0, wxBase, by + dy, wzBase, dy == 21 ? crystalBright : fence);
        }
        for (int s = 1; s <= 5; ++s) {
            int bx = wxBase + (int) Math.round(ux * s);
            int bz = wzBase + (int) Math.round(uz * s);
            put(chunk, mp, x0, z0, bx, by + 20 - s / 3, bz, (s % 3 == 0) ? whiteWool : (s % 2 == 0 ? lilacWool : purpleWool));
        }

        // 2) Деревянная консольная ВОЗДУШНАЯ ПРИСТАНЬ (длина 14 блоков прямо над пропастью в открытое небо!)
        for (double s = 7.0; s <= 20.5; s += 0.5) {
            for (double w = -2.5; w <= 2.5; w += 0.5) {
                int px = cx + (int) Math.round(ux * s + vx * w);
                int pz = cz + (int) Math.round(uz * s + vz * w);
                boolean rail = Math.abs(w) >= 2.1;
                put(chunk, mp, x0, z0, px, by, pz, darkPlank);
                // Арочные подкосы под консолью воздушного пирса
                if (s <= 13.0 && Math.abs(w) >= 1.8) {
                    int braceDepth = (int) Math.round((13.5 - s) * 0.65);
                    for (int bd = 1; bd <= braceDepth; ++bd) {
                        put(chunk, mp, x0, z0, px, by - bd, pz, darkBeamY);
                    }
                }
                if (rail) {
                    int step = (int) Math.round(s);
                    if (step % 4 == 0 || s >= 20.0) {
                        put(chunk, mp, x0, z0, px, by + 1, pz, fence);
                        if (s >= 20.0) {
                            put(chunk, mp, x0, z0, px, by + 2, pz, lantern);
                            put(chunk, mp, x0, z0, px, by - 1, pz, lanternHang);
                        }
                    }
                }
            }
        }

        // 3) Пришвартованная у Воздушной Пристани парящая Небесная Ладья с сиреневым парусом!
        double skiffCenterS = 15.5;
        double skiffSideW = 5.2;
        for (int ls = -5; ls <= 5; ++ls) {
            int hw = Math.abs(ls) >= 4 ? 0 : 1;
            for (int lw = -hw; lw <= hw; ++lw) {
                int sx = cx + (int) Math.round(ux * (skiffCenterS + ls) + vx * (skiffSideW + lw));
                int sz = cz + (int) Math.round(uz * (skiffCenterS + ls) + vz * (skiffSideW + lw));
                put(chunk, mp, x0, z0, sx, by - 1, sz, (ls == 0 && lw == 0) ? crystalBright : darkPlank);
                if (Math.abs(lw) == hw || Math.abs(ls) >= 4) {
                    put(chunk, mp, x0, z0, sx, by, sz, darkPlank);
                }
            }
        }
        int mastX = cx + (int) Math.round(ux * skiffCenterS + vx * skiffSideW);
        int mastZ = cz + (int) Math.round(uz * skiffCenterS + vz * skiffSideW);
        for (int my = 0; my <= 7; ++my) {
            put(chunk, mp, x0, z0, mastX, by + my, mastZ, darkBeamY);
        }
        for (int sw = -2; sw <= 2; ++sw) {
            int sx = mastX + (int) Math.round(vx * sw);
            int sz = mastZ + (int) Math.round(vz * sw);
            for (int sy = 3; sy <= 6; ++sy) {
                put(chunk, mp, x0, z0, sx, by + sy, sz, (sy == 4 || sy == 5) ? lilacWool : whiteWool);
            }
        }

        // Скромные сундуки звездочёта внутри ротонды обсерватории и у воздушного причала
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 2, by, cz, Direction.EAST, LOOT_MODEST_WAYFARER);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 2, by, cz, Direction.WEST, LOOT_MODEST_WAYFARER);
    }
}
