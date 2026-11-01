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
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
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

        HarborPort(int cx, int cz, int quayY, int floorY, Direction seaDir, double reachR) {
            this.cx = cx;
            this.cz = cz;
            this.quayY = quayY;
            this.floorY = floorY;
            this.seaDir = seaDir;
            this.reachR = reachR;
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
    private static final ConcurrentHashMap<Long, BiomeLandmark> ASHEN_BELFRY_CACHE = new ConcurrentHashMap<>();
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

    /** #10 Пристань-Порт на морских просторах архипелага. */
    public static HarborPort harborPort(int sx, int sz) {
        return HARBOR_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genHarborPort(sx, sz))).orElse(null);
    }

    private static HarborPort genHarborPort(int sx, int sz) {
        Random rnd = new Random(SEED_HARBOR ^ (sx * 6364136223846793005L + sz * 1442695040888963407L));
        if (rnd.nextDouble() > 0.68) return null;
        int margin = 115;
        CityPlan plan = CityPlan.get();
        Direction[] dirs = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        for (int tries = 0; tries < 16; ++tries) {
            int cx = sx * SUPER_HARBOR + margin + rnd.nextInt(SUPER_HARBOR - 2 * margin);
            int cz = sz * SUPER_HARBOR + margin + rnd.nextInt(SUPER_HARBOR - 2 * margin);
            if (!openSeaClear(plan, cx, cz, 33)) continue;
            int floorY = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, cx, cz));
            Direction seaDir = dirs[rnd.nextInt(dirs.length)];
            return new HarborPort(cx, cz, plan.ocean + 2, floorY, seaDir, 28.0);
        }
        return null;
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
                int baseY = ElisarChunkGenerator.column(plan, ax, az).top + 1;
                if (baseY <= plan.ocean + 4) continue;
                return new BiomeLandmark(SiteKind.ANCIENT_TREE_ALTAR, ax, az, baseY, -nx * Math.signum(off), -nz * Math.signum(off), 15.0);
            }
        }
        int baseY = ElisarChunkGenerator.column(plan, sg.cx, sg.cz).top + 1;
        return new BiomeLandmark(SiteKind.ANCIENT_TREE_ALTAR, sg.cx, sg.cz, Math.max(plan.ocean + 6, baseY), 0.0, 1.0, 15.0);
    }

    /** #4 Пепельный Маяк-Колокольня и Курган Павших Стражей в каждом регионе Адских угодий. */
    public static BiomeLandmark ashenBelfryBarrow(AshenLands.Region rg) {
        if (rg == null) return null;
        return ASHEN_BELFRY_CACHE.computeIfAbsent(key(rg.cx, rg.cz), k -> {
            // Сектор (+124, +26) на пепельной равнине полностью свободен от замка, всех 3 курганов и сгоревшей деревни!
            int ax = rg.cx + 124;
            int az = rg.cz + 26;
            int baseY = CityPlan.get().ocean + 2;
            return new BiomeLandmark(SiteKind.ASHEN_BELFRY_BARROW, ax, az, baseY, -1.0, 0.0, 21.0);
        });
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
        int baseY = Math.max(plan.ocean + 8, ElisarChunkGenerator.column(plan, bestX, bestZ).top + 1);
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
        int baseY = Math.max(plan.ocean + 10, ElisarChunkGenerator.column(plan, ox, oz).top + 1);
        return new BiomeLandmark(SiteKind.VIOLET_WIND_OBSERVATORY, ox, oz, baseY, ux, uz, 22.0);
    }

    public static BiomeLandmark biomeLandmarkNear(double x, double z, double extraMargin) {
        SizoGrove.Region sg = SizoGrove.regionAt(x, z);
        if (sg != null) {
            BiomeLandmark lm = ancientTreeAltar(sg);
            if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) return lm;
        }
        AshenLands.Region ar = AshenLands.regionAt(x, z);
        if (ar != null) {
            BiomeLandmark lm = ashenBelfryBarrow(ar);
            if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) return lm;
        }
        int csx = Math.floorDiv((int) Math.floor(x), CrystalRifts.SUPER);
        int csz = Math.floorDiv((int) Math.floor(z), CrystalRifts.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                CrystalRifts.Region cr = CrystalRifts.region(csx + dx, csz + dz);
                if (cr == null) continue;
                BiomeLandmark lm = shatteredShardForge(cr);
                if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) return lm;
            }
        }
        int vsx = Math.floorDiv((int) Math.floor(x), VioletHills.SUPER);
        int vsz = Math.floorDiv((int) Math.floor(z), VioletHills.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                VioletHills.Region vh = VioletHills.region(vsx + dx, vsz + dz);
                if (vh == null) continue;
                BiomeLandmark lm = violetWindObservatory(vh);
                if (lm != null && Math.hypot(x - lm.cx, z - lm.cz) <= lm.reachR + extraMargin) return lm;
            }
        }
        return null;
    }

    public static boolean inFootprint(double x, double z, double margin) {
        GrandTheater th = theaterAt(x, z, 28 + margin);
        if (th != null && Math.hypot(x - th.cx, z - th.cz) <= th.flatR + margin) return true;
        HarborPort hp = harborNear(x, z, 32 + margin);
        if (hp != null && Math.hypot(x - hp.cx, z - hp.cz) <= hp.reachR + margin) return true;
        SeaLighthouse lh = lighthouseNear(x, z, 24 + margin);
        if (lh != null && Math.hypot(x - lh.cx, z - lh.cz) <= lh.reachR + margin) return true;
        return biomeLandmarkNear(x, z, margin) != null;
    }

    // =========================================================================
    // 2. ПОСТРОЙКА ВСЕХ 7 СТРУКТУР В ТЕКУЩЕМ ЧАНКЕ (0 ПОДВОДНЫХ ПУСТОТ sub_air=0!)
    // =========================================================================

    private static void put(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0,
                            int x, int y, int z, BlockState st) {
        if (x >= x0 && x <= x0 + 15 && z >= z0 && z <= z0 + 15
                && y >= chunk.getMinBuildHeight() + 2 && y < chunk.getMaxBuildHeight() - 1) {
            chunk.setBlockState(mp.set(x, y, z), st, false);
        }
    }

    private static void putLootChest(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                     RandomSource rnd, int x0, int z0,
                                     int x, int y, int z, Direction facing, net.minecraft.resources.ResourceLocation table) {
        if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState st = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
        level.setBlock(pos, st, 2);
        RandomizableContainerBlockEntity.setLootTable(level, rnd, pos, table);
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

        BiomeLandmark lm = biomeLandmarkNear(midX, midZ, 26);
        if (lm != null) {
            switch (lm.kind) {
                case ANCIENT_TREE_ALTAR -> buildAncientTreeAltar(level, chunk, mp, rnd, plan, lm, x0, z0);
                case ASHEN_BELFRY_BARROW -> buildAshenBelfryBarrow(level, chunk, mp, rnd, plan, lm, x0, z0);
                case SHATTERED_SHARD_FORGE -> buildShatteredShardForge(level, chunk, mp, rnd, plan, lm, x0, z0);
                case VIOLET_WIND_OBSERVATORY -> buildVioletWindObservatory(level, chunk, mp, rnd, plan, lm, x0, z0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // СТРУКТУРА #8: БЕЛОКАМЕННЫЙ АНТИЧНЫЙ ТЕАТР / АМФИТЕАТР
    // -------------------------------------------------------------------------
    private static void buildGrandTheater(WorldGenLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                          RandomSource rnd, CityPlan plan, GrandTheater t, int x0, int z0) {
        int cx = t.cx, cz = t.cz, by = t.baseY;
        BlockState marble = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState floor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState gold = TempleBlocks.GOLD.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState pillar = TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState rose = TempleBlocks.ROSE_WINDOW.get().defaultBlockState();
        BlockState stageWood = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState roofBlueSlab = ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState redBanner = Blocks.RED_WOOL.defaultBlockState();
        BlockState lantern = ElisarBlocks.LANTERN.get().defaultBlockState();
        BlockState lanternHang = lantern.setValue(LanternBlock.HANGING, true);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 22.5) continue;

                // Очищаем воздух над чашей театра и сценой
                for (int y = by; y <= by + 18; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                // Надёжное каменное основание до поверхности острова
                int colTop = ElisarChunkGenerator.column(plan, x, z).top;
                for (int y = Math.min(colTop, by - 2); y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, marble);
                }

                // 1) Круглая мозаичная Орхестра (r <= 6.5, dz >= -4)
                if (r <= 6.8 && dz >= -4) {
                    BlockState fl = (dx == 0 && dz == 0) ? mosaicCore
                            : (Math.abs(r - 4.8) < 0.75 || Math.abs(r - 2.3) < 0.55) ? mosaic
                            : (Math.abs(dx) == Math.abs(dz) && r < 4.5) ? goldRune : floor;
                    put(chunk, mp, x0, z0, x, by - 1, z, fl);
                }

                // 2) Полукруглая мраморная Кавея — 7 концентрических ступенчатых рядов скамей (dz >= -2, 6.8 < r <= 19.8)
                if (dz >= -2 && r > 6.8 && r <= 19.8) {
                    int tier = Mth.clamp((int) Math.floor((r - 6.8) / 1.8), 0, 6);
                    int seatY = by + tier;
                    for (int y = by - 1; y < seatY; ++y) {
                        put(chunk, mp, x0, z0, x, y, z, marble);
                    }
                    // Радиальные лестничные проходы (клинья)
                    double ang = Math.atan2(dz, dx);
                    boolean aisle = Math.abs(Math.sin(ang * 4.0)) < 0.11;
                    Direction towardCenter = Math.abs(dx) > Math.abs(dz)
                            ? (dx > 0 ? Direction.WEST : Direction.EAST)
                            : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                    BlockState seatStair = (aisle ? CastleBlocks.ASHLAR_STAIRS.get() : TempleBlocks.STAIRS.get())
                            .defaultBlockState()
                            .setValue(StairBlock.FACING, towardCenter.getOpposite())
                            .setValue(StairBlock.HALF, Half.BOTTOM);
                    put(chunk, mp, x0, z0, x, seatY, z, seatStair);

                    // Верхняя внешняя колоннада-галерея амфитеатра (r в [18.6 .. 19.8])
                    if (r >= 18.5) {
                        put(chunk, mp, x0, z0, x, by + 7, z, marble);
                        int arcStep = (int) Math.round((ang + Math.PI) * 10.0);
                        if (arcStep % 3 == 0 && r >= 18.8 && r <= 19.6) {
                            for (int py = by + 8; py <= by + 11; ++py) {
                                put(chunk, mp, x0, z0, x, py, z, pillar);
                            }
                            put(chunk, mp, x0, z0, x, by + 12, z, lantern);
                        }
                        put(chunk, mp, x0, z0, x, by + 12, z, TempleBlocks.SLAB.get().defaultBlockState());
                    }
                }

                // 3) Монументальная Скена (Scaenae Frons) и помост просцениума на севере (dz от -13 до -4, |dx| <= 15)
                if (dz >= -13 && dz <= -4 && Math.abs(dx) <= 15) {
                    // Помост сцены (высота by + 1)
                    put(chunk, mp, x0, z0, x, by, z, marble);
                    put(chunk, mp, x0, z0, x, by + 1, z, dz >= -8 ? stageWood : floor);
                    if (dz == -4 && Math.abs(dx) % 4 == 0) {
                        put(chunk, mp, x0, z0, x, by + 2, z, lantern);
                    }

                    // Задняя двухъярусная стена Скены (dz == -10 .. -9) с 3 порталами (центральные Царские врата + 2 боковых)
                    if (dz == -10 || dz == -9) {
                        boolean centralArch = Math.abs(dx) <= 2;
                        boolean sideArch = Math.abs(Math.abs(dx) - 8) <= 1;
                        for (int sy = by + 2; sy <= by + 12; ++sy) {
                            int relY = sy - (by + 2);
                            if (centralArch && relY <= 4) continue;
                            if (sideArch && relY <= 3) continue;
                            BlockState wallSt = (relY == 5 || relY == 10) ? gold
                                    : (relY == 7 && (dx == 0 || Math.abs(dx) == 8)) ? rose : marble;
                            put(chunk, mp, x0, z0, x, sy, z, wallSt);
                        }
                    }

                    // Резные каннелированные колонны двух ярусов перед фасадом Скены (dz == -8)
                    if (dz == -8 && (Math.abs(dx) == 3 || Math.abs(dx) == 6 || Math.abs(dx) == 10 || Math.abs(dx) == 13)) {
                        for (int cy = by + 2; cy <= by + 11; ++cy) {
                            put(chunk, mp, x0, z0, x, cy, z, (cy == by + 6 || cy == by + 11) ? gold : grandCol);
                        }
                    }
                    // Алые театральные знамёна между колоннами второго яруса
                    if (dz == -8 && (Math.abs(dx) == 4 || Math.abs(dx) == 5 || Math.abs(dx) == 11)) {
                        for (int cy = by + 8; cy <= by + 10; ++cy) {
                            put(chunk, mp, x0, z0, x, cy, z, redBanner);
                        }
                    }

                    // Боковые башни-параскении с лазурными куполами (|dx| от 12 до 15, dz от -12 до -6)
                    if (Math.abs(dx) >= 12 && dz >= -12 && dz <= -6) {
                        boolean edge = Math.abs(dx) == 12 || Math.abs(dx) == 15 || dz == -12 || dz == -6;
                        for (int ty = by + 2; ty <= by + 12; ++ty) {
                            if (edge) {
                                put(chunk, mp, x0, z0, x, ty, z, (ty == by + 7 || ty == by + 12) ? gold : marble);
                            }
                        }
                        int dCup = Math.max(Math.abs(Math.abs(dx) - 13), Math.abs(dz + 9));
                        put(chunk, mp, x0, z0, x, by + 13, z, roofBlue);
                        if (dCup <= 1) put(chunk, mp, x0, z0, x, by + 14, z, roofBlue);
                        if (dCup == 0) put(chunk, mp, x0, z0, x, by + 15, z, roofBlueSlab);
                    }
                }
            }
        }
        // Театральный сундук реквизита и сокровищ за Царскими вратами Скены
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 1, by + 2, cz - 12, Direction.SOUTH, BuiltInLootTables.SIMPLE_DUNGEON);
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

        // 2) Двухэтажное здание Портовой Таможни и арочного Склада на западной/центральной части набережной (lx: -11..+3, lz: -10..-4)
        for (int lx = -11; lx <= 3; ++lx) {
            for (int lz = -10; lz <= -4; ++lz) {
                int wx = cx + lx, wz = cz + lz;
                boolean wallX = (lx == -11 || lx == 3);
                boolean wallZ = (lz == -10 || lz == -4);
                boolean wall = wallX || wallZ;

                // 1-й этаж: каменная аркада склада (высота qy+1 .. qy+4)
                for (int dy = 1; dy <= 4; ++dy) {
                    if (wall) {
                        boolean archOpen = (lz == -4 && dy <= 3 && (Math.abs(lx + 7) <= 1 || Math.abs(lx + 1) <= 1));
                        if (!archOpen) {
                            BlockState st = (wallX && wallZ) ? ashlarPillar : brick;
                            put(chunk, mp, x0, z0, wx, qy + dy, wz, st);
                        }
                    }
                }
                // Межэтажное перекрытие + фахверковый 2-й этаж Портовой Таможни (qy+5 .. qy+8)
                put(chunk, mp, x0, z0, wx, qy + 5, wz, darkPlank);
                for (int dy = 6; dy <= 8; ++dy) {
                    if (wall) {
                        boolean win = (dy == 7 && !wallX && (lx == -8 || lx == -4 || lx == 0));
                        BlockState st = (wallX && wallZ) ? darkBeamY
                                : win ? ElisarBlocks.WINDOW.get().defaultBlockState()
                                : (dy == 7 ? timber : plaster);
                        put(chunk, mp, x0, z0, wx, qy + dy, wz, st);
                    }
                }
            }
        }
        // Двускатная синяя черепичная крыша Таможни
        for (int lx = -12; lx <= 4; ++lx) {
            for (int lz = -11; lz <= -3; ++lz) {
                int distMid = Math.abs(lz + 7);
                int ry = qy + 12 - distMid;
                put(chunk, mp, x0, z0, cx + lx, ry, cz + lz, roofBlue);
                if ((lx == -11 || lx == 3) && distMid < 3) {
                    for (int fy = qy + 9; fy < ry; ++fy) {
                        put(chunk, mp, x0, z0, cx + lx, fy, cz + lz, plaster);
                    }
                }
            }
        }
        put(chunk, mp, x0, z0, cx - 7, qy + 1, cz - 8, barrel);
        put(chunk, mp, x0, z0, cx - 6, qy + 1, cz - 8, barrel);
        put(chunk, mp, x0, z0, cx - 7, qy + 2, cz - 8, barrel);
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 2, qy + 6, cz - 9, Direction.SOUTH, BuiltInLootTables.SHIPWRECK_TREASURE);

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

        // 4) Пришвартованная в гавани между пирсами Торговая Каравелла (cx, cz + 8, плавает прямо на elisar_water!)
        int sx = cx, sz = cz + 8;
        for (int dz = -7; dz <= 7; ++dz) {
            int hw = dz <= -6 || dz >= 6 ? 0 : (Math.abs(dz) >= 4 ? 1 : 2);
            for (int dx = -hw; dx <= hw; ++dx) {
                // Киль и днище на уровне воды (ocean) и борта над водой (ocean+1 .. ocean+2) — 0 подводного воздуха!
                put(chunk, mp, x0, z0, sx + dx, ocean, sz + dz, darkPlank);
                boolean gunwale = (Math.abs(dx) == hw || Math.abs(dz) >= 6);
                put(chunk, mp, x0, z0, sx + dx, ocean + 1, sz + dz, gunwale ? darkPlank : ElisarBlocks.SIZO_PLANKS.get().defaultBlockState());
                if (gunwale) {
                    put(chunk, mp, x0, z0, sx + dx, ocean + 2, sz + dz, (Math.abs(dz) >= 5) ? darkPlank : CastleBlocks.DARK_SLAB.get().defaultBlockState());
                }
            }
        }
        // Грот-мачта и бело-лазурный парус каравеллы
        for (int my = ocean + 2; my <= ocean + 13; ++my) {
            put(chunk, mp, x0, z0, sx, my, sz, sizoLog);
        }
        for (int dx = -4; dx <= 4; ++dx) {
            put(chunk, mp, x0, z0, sx + dx, ocean + 11, sz, darkBeamX);
            put(chunk, mp, x0, z0, sx + dx, ocean + 6, sz, darkBeamX);
            for (int sy = ocean + 7; sy <= ocean + 10; ++sy) {
                int bulge = (Math.abs(dx) <= 2) ? 1 : 0;
                put(chunk, mp, x0, z0, sx + dx, sy, sz + bulge, (sy == ocean + 9) ? sailBlue : sailWhite);
            }
        }
        put(chunk, mp, x0, z0, sx, ocean + 14, sz, lantern);
        put(chunk, mp, x0, z0, sx, ocean + 2, sz - 3, barrel);
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

        // 3) Выносная смотровая галерея с коваными перилами и Сияющая Фонарная Камера (линза Френеля!)
        int galY = rTop + 1 + towerH + 1;
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

        // Сундук смотрителя маяка на 1-м этаже внутри башни
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 2, rTop + 1, cz, Direction.WEST, BuiltInLootTables.SHIPWRECK_TREASURE);
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

        // 1) Утопленное круглое замшелое святилище с руническим каменным полом (r <= 13.0)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 13.6) continue;

                int colTop = ElisarChunkGenerator.column(plan, x, z).top;
                for (int y = by; y <= by + 16; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 3); y < by - 1; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, mossyStone);
                }

                boolean outerRim = r >= 11.2 && r <= 13.0;
                int floorY = outerRim ? by : by - 1;
                BlockState tile = (Math.abs(r - 6.8) < 0.55 || (Math.abs(dx) == 0 && r < 10.5) || (Math.abs(dz) == 0 && r < 10.5))
                        ? goldRune
                        : ((dx * 7 + dz * 13) % 5 == 0 ? moss : ((dx + dz) % 2 == 0 ? mossyBrick : floor));
                put(chunk, mp, x0, z0, x, floorY, z, tile);
                if (!outerRim && r > 4.2 && r < 10.2 && ((dx * 31 ^ dz * 17) & 15) == 0) {
                    put(chunk, mp, x0, z0, x, floorY + 1, z, flowers);
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
        putLootChest(level, chunk, mp, rnd, x0, z0, cx, by, cz - 1, Direction.SOUTH, BuiltInLootTables.ANCIENT_CITY);

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

        // 1) Вымощенная обожжённым камнем мемориальная площадь + Курган Павших Стражей со склепом внутри!
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double dAll = Math.hypot(x - cx, z - cz);
                if (dAll > 20.5) continue;

                int colTop = ElisarChunkGenerator.column(plan, x, z).top;
                for (int y = Math.min(colTop, by - 3); y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, scorched);
                }
                for (int y = by; y <= by + 29; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                put(chunk, mp, x0, z0, x, by - 1, z, ((x + z) % 5 == 0) ? ash : scorched);

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
        putLootChest(level, chunk, mp, rnd, x0, z0, bxCenter, by, bzCenter - 2, Direction.SOUTH, BuiltInLootTables.BASTION_TREASURE);

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

        // 1) Круглая мраморно-сланцевая платформа мастерской (r <= 12.6), рассечённая посередине сияющим горном-разломом!
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 13.0) continue;

                int colTop = ElisarChunkGenerator.column(plan, x, z).top;
                for (int y = by; y <= by + 18; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 4); y < by - 1; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, marble);
                }

                // Светящаяся энергетическая расселина Горна Осколков вдоль оси Z (|dx| <= 1, |dz| <= 7)
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 7) {
                    put(chunk, mp, x0, z0, x, by - 3, z, crystalDeep);
                    put(chunk, mp, x0, z0, x, by - 2, z, (dx == 0) ? crystalBright : crystal);
                    if (Math.abs(dz) == 3 || dz == 0) {
                        // Каменные мостики-перемычки кузнецов над горном
                        put(chunk, mp, x0, z0, x, by - 1, z, ElisarBlocks.RIFT_MARBLE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                    } else {
                        put(chunk, mp, x0, z0, x, by - 1, z, (dx == 0 && Math.abs(dz) <= 5) ? crystalBright : air);
                    }
                } else {
                    boolean ring = Math.abs(r - 11.4) < 0.8 || Math.abs(r - 6.2) < 0.55;
                    BlockState fl = ring ? slate : ((Math.abs(dx) + Math.abs(dz)) % 6 == 0 ? gold : marble);
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
        putLootChest(level, chunk, mp, rnd, x0, z0, cx + 4, by, cz - 10, Direction.SOUTH, BuiltInLootTables.ANCIENT_CITY);
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

        // 1) Круглая белокаменно-аметистовая Ротонда Обсерватории (r <= 7.8) с открытым куполом и телескопом!
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                int dx = x - cx, dz = z - cz;
                double r = Math.hypot(dx, dz);
                if (r > 8.2) continue;

                int colTop = ElisarChunkGenerator.column(plan, x, z).top;
                for (int y = by; y <= by + 20; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, air);
                }
                for (int y = Math.min(colTop, by - 4); y < by; ++y) {
                    put(chunk, mp, x0, z0, x, y, z, marble);
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

        // Сундук звездочёта внутри ротонды обсерватории
        putLootChest(level, chunk, mp, rnd, x0, z0, cx - 2, by, cz, Direction.EAST, BuiltInLootTables.SIMPLE_DUNGEON);
    }
}
