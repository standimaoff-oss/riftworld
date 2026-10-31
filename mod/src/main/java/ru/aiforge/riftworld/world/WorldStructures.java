package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModBlocks;

/**
 * Детерминированный планировщик и строитель мировых структур Элисара:
 * <ol>
 *   <li><b>Маленькие белые деревни ({@link Village}, {@code burned=false}):</b> 3–6 небольших домов
 *       в архитектуре Белого города ({@code city/house_f}, {@code house_a}, {@code fisher_house},
 *       {@code house_h}, {@code hunter_house}, {@code shop}, {@code forge}) с 3 вариациями планировки
 *       (колодезная площадь, ремесленная улочка со звонницей, приморская застава с беседкой).</li>
 *   <li><b>Сгоревшие уничтоженные деревни в адском биоме ({@link Village}, {@code burned=true}):</b>
 *       3–6 обугленных полуразрушенных домов ({@code ruins/burned_house_0..5}) с дымящимися головнями,
 *       пеплом и разрушенным фонтаном — как внутри каждого региона {@link AshenLands}, так и на отдельных
 *       выжженных островах архипелага.</li>
 *   <li><b>Большой парусный корабль, врезавшийся в скалу ({@link ShipSite}, {@code sunken=false}):</b>
 *       32-блочный галеон с мачтами, рваными парусами, каютой капитана и бушпритом, расколовшийся о
 *       морской скальный утёс с маяком (2 вариации).</li>
 *   <li><b>Такой же большой затонувший корабль на дне океана ({@link ShipSite}, {@code sunken=true}):</b>
 *       32-блочный галеон на глубоком морском дне, полностью затопленный без воздушных пузырей,
 *       заросший ламинарией и светящимися кораллами (3 вариации: накренившийся с якорем, разломанный
 *       пополам с россыпью груза, древний остов в кораллах).</li>
 *   <li><b>Редкие подводные здания на дне океана ({@link OceanBuilding}):</b> 4 вариации затопленных
 *       белокаменных построек (ротонда с куполом, двухэтажный архив, триумфальные морские врата с
 *       мозаичным двориком, затопленная улочка из малых домов) — полностью {@code waterlogged}.</li>
 * </ol>
 */
public final class WorldStructures {
    public static final int VILLAGE_KIND = 11;
    public static final int BURNED_VILLAGE_KIND = 12;

    public static final int SUPER_VILLAGE = 640;
    public static final int SUPER_BURNED = 820;
    public static final int SUPER_CRASH = 620;
    public static final int SUPER_SUNKEN = 580;
    public static final int SUPER_OCEAN_BLDG = 660;
    public static final int SUPER_STILT = 680;

    private static final long SEED_VILLAGE = 0x711A6E01L;
    private static final long SEED_BURNED = 0xB07ED911L;
    private static final long SEED_CRASH = 0xC7A5451FL;
    private static final long SEED_SUNKEN = 0x504CE451L;
    private static final long SEED_OBLDG = 0x0CEA4B1DL;
    private static final long SEED_STILT = 0x57117A9EL;

    /** Небольшие шаблоны Белого города: {имя, ширина_south, глубина_south}. */
    private static final String[][] SMALL_CITY_HOUSES = {
            {"house_f", "7", "7"},
            {"house_a", "9", "8"},
            {"fisher_house", "9", "8"},
            {"house_h", "11", "8"},
            {"hunter_house", "10", "9"},
            {"shop", "10", "9"},
            {"shop2", "10", "9"},
            {"forge", "11", "10"},
            {"bakery", "11", "9"},
            {"guardhouse", "10", "9"},
            {"manor", "12", "10"}
    };

    /** Сгоревшие шаблоны (из тех же малых домов Белого города): {имя, ширина_south, глубина_south}. */
    private static final String[][] BURNED_HOUSES = {
            {"burned_house_0", "7", "7"},
            {"burned_house_1", "9", "8"},
            {"burned_house_2", "9", "8"},
            {"burned_house_3", "11", "8"},
            {"burned_house_4", "10", "9"},
            {"burned_house_5", "11", "10"}
    };

    public record HousePlot(String nbtPath, int x0, int z0, int w, int d, String facing, int y, int doorX, int doorZ) {
    }

    public static final class Village {
        public final int cx, cz, baseY, variant;
        public final boolean burned;
        public final boolean onAshPlain;
        public final double flatR;
        public final CityPlan.Island island; // null когда деревня стоит прямо на пепельной равнине AshenLands
        public final List<HousePlot> houses;

        Village(int cx, int cz, int baseY, int variant, boolean burned, boolean onAshPlain,
                CityPlan.Island island, List<HousePlot> houses, double flatR) {
            this.cx = cx;
            this.cz = cz;
            this.baseY = baseY;
            this.variant = variant;
            this.burned = burned;
            this.onAshPlain = onAshPlain;
            this.island = island;
            this.houses = Collections.unmodifiableList(houses);
            this.flatR = flatR;
        }

        public boolean inFootprint(double x, double z, double margin) {
            if (Math.hypot(x - cx, z - cz) < flatR + margin) return true;
            for (HousePlot h : houses) {
                if (x >= h.x0 - margin && x <= h.x0 + h.w + margin
                        && z >= h.z0 - margin && z <= h.z0 + h.d + margin) return true;
            }
            return false;
        }
    }

    public static final class ShipSite {
        public final int cx, cz, variant;
        public final boolean sunken;
        public final double yaw;
        public final int baseY; // для crashed — ocean; для sunken — уровень дна под килем
        public final int rockX, rockZ, rockTopY;
        public final double rockR;

        ShipSite(int cx, int cz, int variant, boolean sunken, double yaw, int baseY,
                 int rockX, int rockZ, int rockTopY, double rockR) {
            this.cx = cx;
            this.cz = cz;
            this.variant = variant;
            this.sunken = sunken;
            this.yaw = yaw;
            this.baseY = baseY;
            this.rockX = rockX;
            this.rockZ = rockZ;
            this.rockTopY = rockTopY;
            this.rockR = rockR;
        }
    }

    public static final class OceanBuilding {
        public final int cx, cz, floorY, variant;
        public final double yaw;
        public final int radius;

        OceanBuilding(int cx, int cz, int floorY, int variant, double yaw, int radius) {
            this.cx = cx;
            this.cz = cz;
            this.floorY = floorY;
            this.variant = variant;
            this.yaw = yaw;
            this.radius = radius;
        }
    }

    /** Отдельная свайная постройка в надводной деревне (kind: 0=дом рыбака, 1=двухэтажный терем, 2=эллинг с лодкой, 3=лавка под навесом, 4=маячная башня). */
    public record StiltHut(int cx, int cz, int hw, int hd, int deckY, int kind, Direction facing) {
    }

    /** Надводная деревня на сваях: часто спавнится в лагунах возле «Висячих садов», а также на морских отмелях. */
    public static final class StiltVillage {
        public final int cx, cz, deckY, variant;
        public final boolean nearGardens;
        public final double reachR;
        public final List<StiltHut> huts;

        StiltVillage(int cx, int cz, int deckY, int variant, boolean nearGardens, double reachR, List<StiltHut> huts) {
            this.cx = cx;
            this.cz = cz;
            this.deckY = deckY;
            this.variant = variant;
            this.nearGardens = nearGardens;
            this.reachR = reachR;
            this.huts = Collections.unmodifiableList(huts);
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Village>> WHITE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<Village>> BURNED_ISLE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Village> ASHEN_PLAIN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<ShipSite>> CRASH_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<ShipSite>> SUNKEN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<OceanBuilding>> OBLDG_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, List<StiltVillage>> GARDEN_STILT_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Optional<StiltVillage>> SEA_STILT_CACHE = new ConcurrentHashMap<>();

    private WorldStructures() {
    }

    private static long key(int sx, int sz) {
        return ((long) sx << 32) ^ (sz & 0xFFFFFFFFL);
    }

    private static boolean nearProtectedZones(double x, double z, double extra) {
        // Городское кольцо + референсная морская точка проб (640, 690) / (696, 696)
        if (Math.hypot(x, z) < 560 + extra) return true;
        if (Math.hypot(x - 660, z - 660) < 210 + extra) return true;
        return false;
    }

    private static boolean overlapsSpecialBiomes(double x, double z, double clear) {
        if (nearProtectedZones(x, z, clear)) return true;
        if (AshenLands.blocks(x, z, clear + 50)) return true;
        if (EmberRuins.siteAt(x, z) != null) return true;
        if (MistKeep.inside(x, z)) return true;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                WaterfallGardens.Region gr = WaterfallGardens.region(
                        Math.floorDiv((int) x, WaterfallGardens.SUPER) + dx,
                        Math.floorDiv((int) z, WaterfallGardens.SUPER) + dz);
                if (gr != null && Math.hypot(x - gr.cx, z - gr.cz) < gr.Rr + clear + 40) return true;

                VioletHills.Region vr = VioletHills.region(
                        Math.floorDiv((int) x, VioletHills.SUPER) + dx,
                        Math.floorDiv((int) z, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(x - vr.cx, z - vr.cz) < vr.Rr + clear + 40) return true;

                SizoGrove.Region sr = SizoGrove.region(
                        Math.floorDiv((int) x, SizoGrove.SUPER) + dx,
                        Math.floorDiv((int) z, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(x - sr.cx, z - sr.cz) < sr.Rr + clear + 40) return true;

                CrystalRifts.Region cr = CrystalRifts.region(
                        Math.floorDiv((int) x, CrystalRifts.SUPER) + dx,
                        Math.floorDiv((int) z, CrystalRifts.SUPER) + dz);
                if (cr != null && Math.hypot(x - cr.cx, z - cr.cz) < cr.Rr + clear + 40) return true;
            }
        }
        return false;
    }

    // =========================================================================
    // 1. МАЛЕНЬКИЕ БЕЛЫЕ ДЕРЕВНИ (3..6 небольших домов Белого города + 3 вариации)
    // =========================================================================
    public static Village whiteVillage(int sx, int sz) {
        return WHITE_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genWhiteVillage(sx, sz))).orElse(null);
    }

    private static Village genWhiteVillage(int sx, int sz) {
        Random rnd = new Random(SEED_VILLAGE ^ (sx * 341873128712L + sz * 132897987541L));
        if (rnd.nextDouble() > 0.68) return null;
        int margin = 125;
        int cx = sx * SUPER_VILLAGE + margin + rnd.nextInt(SUPER_VILLAGE - 2 * margin);
        int cz = sz * SUPER_VILLAGE + margin + rnd.nextInt(SUPER_VILLAGE - 2 * margin);
        if (overlapsSpecialBiomes(cx, cz, 85)) return null;

        int variant = rnd.nextInt(3); // 0 = площадь с колодцем (5-6), 1 = ремесленный хутор (4-5), 2 = приморская застава (3-4)
        int houseCount = switch (variant) {
            case 0 -> 5 + rnd.nextInt(2); // 5..6
            case 1 -> 4 + rnd.nextInt(2); // 4..5
            default -> 3 + rnd.nextInt(2); // 3..4
        };
        int baseY = 92 + rnd.nextInt(12);
        double islR = 39.0 + houseCount * 1.4 + rnd.nextDouble() * 4.0;
        CityPlan.Island isl = new CityPlan.Island(cx, cz, islR, baseY, islR * 0.68 + 12.0,
                rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, VILLAGE_KIND, 0.42);

        List<HousePlot> houses = layoutHousesAroundCenter(cx, cz, baseY + 1, houseCount, variant, false, rnd);
        if (houses.size() < 3) return null;
        return new Village(cx, cz, baseY + 1, variant, false, false, isl, houses, 24.0 + houseCount * 1.5);
    }

    // =========================================================================
    // 2. СГОРЕВШИЕ УНИЧТОЖЕННЫЕ ДЕРЕВНИ В АДСКОМ БИОМЕ (3..6 домов)
    // =========================================================================
    public static Village burnedIslandVillage(int sx, int sz) {
        return BURNED_ISLE_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genBurnedIsleVillage(sx, sz))).orElse(null);
    }

    private static Village genBurnedIsleVillage(int sx, int sz) {
        Random rnd = new Random(SEED_BURNED ^ (sx * 912931421L + sz * 734287613L));
        if (rnd.nextDouble() > 0.52) return null;
        int margin = 150;
        int cx = sx * SUPER_BURNED + margin + rnd.nextInt(SUPER_BURNED - 2 * margin);
        int cz = sz * SUPER_BURNED + margin + rnd.nextInt(SUPER_BURNED - 2 * margin);
        if (overlapsSpecialBiomes(cx, cz, 95)) return null;
        // Не пересекаемся с обычной белой деревней
        if (whiteVillageAt(cx, cz, 120) != null) return null;

        int variant = rnd.nextInt(3);
        int houseCount = 3 + rnd.nextInt(4); // строго 3..6 домов!
        int baseY = 95 + rnd.nextInt(12);
        double islR = 38.0 + houseCount * 1.3 + rnd.nextDouble() * 4.0;
        CityPlan.Island isl = new CityPlan.Island(cx, cz, islR, baseY, islR * 0.65 + 12.0,
                rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, BURNED_VILLAGE_KIND, 0.38);

        List<HousePlot> houses = layoutHousesAroundCenter(cx, cz, baseY + 1, houseCount, variant, true, rnd);
        if (houses.size() < 3) return null;
        return new Village(cx, cz, baseY + 1, variant, true, false, isl, houses, 23.0 + houseCount * 1.5);
    }

    /** Гарантированная сгоревшая уничтоженная деревня на пепельной равнине каждого региона AshenLands! */
    public static Village ashenPlainVillage(AshenLands.Region rg) {
        if (rg == null) return null;
        long k = key(rg.cx, rg.cz);
        return ASHEN_PLAIN_CACHE.computeIfAbsent(k, unused -> {
            Random rnd = new Random(SEED_BURNED ^ k ^ 0xA54E4L);
            // Квадрант (+X, -Z) свободен от замка (cx, cz) и всех трёх курганов AshenLands!
            int vx = rg.cx + 120;
            int vz = rg.cz - 112;
            int vy = CityPlan.get().ocean + 2;
            int count = 4 + rnd.nextInt(3); // 4..6 сгоревших домов
            int variant = rnd.nextInt(3);
            List<HousePlot> houses = layoutHousesAroundCenter(vx, vz, vy, count, variant, true, rnd);
            return new Village(vx, vz, vy, variant, true, true, null, houses, 26.0);
        });
    }

    private static List<HousePlot> layoutHousesAroundCenter(int cx, int cz, int y, int targetCount,
                                                            int variant, boolean burned, Random rnd) {
        String[][] catalog = burned ? BURNED_HOUSES : SMALL_CITY_HOUSES;
        String prefix = burned ? "ruins/" : "city/";
        List<HousePlot> placed = new ArrayList<>();
        int count = Math.max(3, Math.min(6, targetCount));

        // 6 аккуратных слотов вокруг центральной площади/улочки (все двери смотрят к центру деревни!)
        // Формат слота: {dx_center, dz_center, facing_code(0=south/+Z, 1=north/-Z, 2=east/+X, 3=west/-X)}
        int[][][] layouts = {
                // Вариация 0: Круглая колодезная площадь
                {
                        {-6, -15, 0}, { 7, -15, 0},
                        {-17, -2, 2}, {17,  -2, 3},
                        {-6,  15, 1}, { 7,  15, 1}
                },
                // Вариация 1: Вдоль ремесленной улочки с запада на восток
                {
                        {-13, -13, 0}, { 0, -13, 0}, {13, -13, 0},
                        {-12,  13, 1}, { 2,  13, 1}, {14,  12, 1}
                },
                // Вариация 2: Уютный полукруг у приморской смотровой беседки (беседка на +Z)
                {
                        {-14, -10, 0}, { 0, -14, 0}, {14, -10, 0},
                        {-16,   4, 2}, {16,   4, 3}, { 0,  15, 1}
                }
        };
        int[][] slots = layouts[Math.floorMod(variant, layouts.length)];

        for (int i = 0; i < slots.length && placed.size() < count; ++i) {
            int[] sl = slots[i];
            int idx;
            if (!burned && variant == 1 && i == 0) idx = 7; // кузница (forge) в ремесленном хуторе
            else if (!burned && variant == 0 && i == 1) idx = 5; // лавка (shop) у площади
            else idx = rnd.nextInt(burned ? catalog.length : 6);

            String[] entry = catalog[idx];
            String name = entry[0];
            int sw = Integer.parseInt(entry[1]);
            int sd = Integer.parseInt(entry[2]);
            String facing = switch (sl[2]) {
                case 1 -> "north";
                case 2 -> "east";
                case 3 -> "west";
                default -> "south";
            };
            int w = (facing.equals("east") || facing.equals("west")) ? sd : sw;
            int d = (facing.equals("east") || facing.equals("west")) ? sw : sd;
            int hx0 = cx + sl[0] - w / 2;
            int hz0 = cz + sl[1] - d / 2;

            boolean overlap = false;
            for (HousePlot o : placed) {
                if (hx0 < o.x0 + o.w + 2 && hx0 + w + 2 > o.x0
                        && hz0 < o.z0 + o.d + 2 && hz0 + d + 2 > o.z0) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) continue;

            int doorX = switch (facing) {
                case "east" -> hx0 + w;
                case "west" -> hx0 - 1;
                default -> hx0 + w / 2;
            };
            int doorZ = switch (facing) {
                case "south" -> hz0 + d;
                case "north" -> hz0 - 1;
                default -> hz0 + d / 2;
            };
            placed.add(new HousePlot(prefix + name, hx0, hz0, w, d, facing, y, doorX, doorZ));
        }
        return placed;
    }

    public static Village whiteVillageAt(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_VILLAGE);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_VILLAGE);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Village v = whiteVillage(sx + dx, sz + dz);
                if (v != null && Math.abs(x - v.cx) <= range && Math.abs(z - v.cz) <= range) return v;
            }
        }
        return null;
    }

    public static Village anyVillageAt(double x, double z, double range) {
        Village w = whiteVillageAt(x, z, range);
        if (w != null) return w;
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_BURNED);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_BURNED);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Village b = burnedIslandVillage(sx + dx, sz + dz);
                if (b != null && Math.abs(x - b.cx) <= range && Math.abs(z - b.cz) <= range) return b;
            }
        }
        AshenLands.Region ar = AshenLands.regionAt(x, z);
        if (ar != null) {
            Village av = ashenPlainVillage(ar);
            if (av != null && Math.abs(x - av.cx) <= range && Math.abs(z - av.cz) <= range) return av;
        }
        return null;
    }

    public static List<CityPlan.Island> islandsNear(double x, double z) {
        List<CityPlan.Island> out = null;
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_VILLAGE);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_VILLAGE);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Village v = whiteVillage(sx + dx, sz + dz);
                if (v != null && v.island != null
                        && Math.abs(x - v.island.cx) < v.island.maxReach()
                        && Math.abs(z - v.island.cz) < v.island.maxReach()) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(v.island);
                }
            }
        }
        int bsx = Math.floorDiv((int) Math.floor(x), SUPER_BURNED);
        int bsz = Math.floorDiv((int) Math.floor(z), SUPER_BURNED);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Village b = burnedIslandVillage(bsx + dx, bsz + dz);
                if (b != null && b.island != null
                        && Math.abs(x - b.island.cx) < b.island.maxReach()
                        && Math.abs(z - b.island.cz) < b.island.maxReach()) {
                    if (out == null) out = new ArrayList<>(2);
                    out.add(b.island);
                }
            }
        }
        return out == null ? Collections.emptyList() : out;
    }

    public static boolean inFootprint(double x, double z, double margin) {
        Village v = anyVillageAt(x, z, 52 + margin);
        if (v != null && v.inFootprint(x, z, margin)) return true;
        StiltVillage sv = stiltVillageNear(x, z, 36 + margin);
        return sv != null && Math.hypot(x - sv.cx, z - sv.cz) <= sv.reachR + margin;
    }

    // =========================================================================
    // 6. НАДВОДНЫЕ ДЕРЕВНИ НА СВАЯХ (ЧАСТО ВОЗЛЕ «ВИСЯЧИХ САДОВ» + В МОРЕ)
    // =========================================================================
    public static List<StiltVillage> gardenStiltVillages(WaterfallGardens.Region rg) {
        if (rg == null) return Collections.emptyList();
        return GARDEN_STILT_CACHE.computeIfAbsent(key(rg.cx, rg.cz), k -> genGardenStiltVillages(rg));
    }

    private static List<StiltVillage> genGardenStiltVillages(WaterfallGardens.Region rg) {
        Random rnd = new Random(SEED_STILT ^ (rg.cx * 4827149287L + rg.cz * 9182736451L));
        int want = rnd.nextDouble() < 0.72 ? 2 : 1; // 1..2 свайные деревни в лагуне у каждого архипелага Висячих садов!
        List<StiltVillage> res = new ArrayList<>(want);
        int ocean = CityPlan.get().ocean;
        double baseAng = rnd.nextDouble() * Math.PI * 2.0;

        for (int slot = 0; slot < want; ++slot) {
            for (int attempt = 0; attempt < 36; ++attempt) {
                double ang = baseAng + slot * Math.PI + (attempt * 0.21) * (attempt % 2 == 0 ? 1 : -1);
                // Ищем внешний берег архипелага в направлении ang и отступаем 25..34 блока в чистую лагуну
                CityPlan.Island outer = rg.hub;
                double bestProj = -1e9;
                for (CityPlan.Island gi : rg.isles) {
                    double proj = (gi.cx - rg.cx) * Math.cos(ang) + (gi.cz - rg.cz) * Math.sin(ang) + gi.maxReach();
                    if (proj > bestProj) {
                        bestProj = proj;
                        outer = gi;
                    }
                }
                double offshore = outer.maxReach() + 25.0 + (attempt % 4) * 3.0;
                int vx = (int) Math.round(outer.cx + Math.cos(ang) * offshore);
                int vz = (int) Math.round(outer.cz + Math.sin(ang) * offshore);

                if (nearProtectedZones(vx, vz, 55)) continue;
                if (AshenLands.blocks(vx, vz, 95) || EmberRuins.siteAt(vx, vz) != null || MistKeep.inside(vx, vz)) continue;
                if (VioletHills.nearAny(vx, vz, 34, 30) || CrystalRifts.nearAny(vx, vz, 34, 30) || SizoGrove.regionAt(vx, vz) != null) continue;
                if (anyVillageAt(vx, vz, 85) != null) continue;

                // Проверяем, что вся акватория деревни (±24 блока) лежит в воде и НЕ налезает на сушу садов
                boolean clearLagoon = true;
                for (int dx = -24; dx <= 24 && clearLagoon; dx += 8) {
                    for (int dz = -24; dz <= 24; dz += 8) {
                        if (WaterfallGardens.landAt(vx + dx, vz + dz) != null) {
                            clearLagoon = false;
                            break;
                        }
                        for (CityPlan.Island gi : rg.isles) {
                            if (gi.t(vx + dx, vz + dz) > 0.06) {
                                clearLagoon = false;
                                break;
                            }
                        }
                    }
                }
                if (!clearLagoon) continue;
                boolean tooClose = false;
                for (StiltVillage prev : res) {
                    if (Math.hypot(vx - prev.cx, vz - prev.cz) < 95.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                res.add(makeStiltLayout(vx, vz, ocean + 3, rnd.nextInt(3), true, rnd));
                break;
            }
        }
        return res.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(res);
    }

    public static StiltVillage seaStiltVillage(int sx, int sz) {
        return SEA_STILT_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genSeaStiltVillage(sx, sz))).orElse(null);
    }

    private static StiltVillage genSeaStiltVillage(int sx, int sz) {
        Random rnd = new Random(SEED_STILT ^ 0x33AA55CCL ^ (sx * 1928374651L + sz * 5647382913L));
        if (rnd.nextDouble() > 0.45) return null;
        int margin = 115;
        int ocean = CityPlan.get().ocean;
        for (int tries = 0; tries < 14; ++tries) {
            int cx = sx * SUPER_STILT + margin + rnd.nextInt(SUPER_STILT - 2 * margin);
            int cz = sz * SUPER_STILT + margin + rnd.nextInt(SUPER_STILT - 2 * margin);
            if (!openDeepSea(cx, cz, 8, true)) continue;
            if (crashedShipNear(cx, cz, 85) != null || sunkenShipNear(cx, cz, 85) != null || oceanBuildingNear(cx, cz, 85) != null) continue;
            return makeStiltLayout(cx, cz, ocean + 3, rnd.nextInt(3), false, rnd);
        }
        return null;
    }

    private static StiltVillage makeStiltLayout(int cx, int cz, int deckY, int variant, boolean nearGardens, Random rnd) {
        // 4..6 свайных построек вокруг центральной деревянной площади-пристани на мостиках:
        //  kind 0 = рыбацкий дом с сушильней сетей, 1 = двухэтажный свайный терем,
        //  2 = крытый эллинг с лодкой, 3 = торговая лавка под навесом, 4 = маячная башня лагуны
        int count = switch (variant) {
            case 0 -> 5 + rnd.nextInt(2); // 5..6: круговая лагунная слобода с эллингом и теремом
            case 1 -> 5;                  // 5: крестообразная пристань с маячной башней
            default -> 4 + rnd.nextInt(2); // 4..5: уютный рыбацкий посад на воде
        };
        int[][] templates = {
                {4, 3, 0}, // 9x7 рыбацкий дом
                {5, 4, 1}, // 11x9 двухэтажный свайный терем
                {5, 4, 2}, // 11x9 крытый эллинг с лодкой на воде
                {4, 4, 3}, // 9x9 лавка торговца под полосатым навесом
                {3, 3, 4}  // 7x7 высокая дозорно-маячная вышка
        };
        List<StiltHut> huts = new ArrayList<>(count);
        double a0 = rnd.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < count; ++i) {
            double ang = a0 + (Math.PI * 2.0 * i) / count + (rnd.nextDouble() - 0.5) * 0.18;
            double dist = 15.0 + (i % 2) * 2.5;
            int hx = cx + (int) Math.round(Math.cos(ang) * dist);
            int hz = cz + (int) Math.round(Math.sin(ang) * dist);
            int pick = (i == 0) ? 1 : (i == 1 ? 2 : (i == 2 && variant == 1 ? 4 : (i == 3 ? 3 : 0)));
            int hw = templates[pick][0];
            int hd = templates[pick][1];
            int kind = templates[pick][2];
            Direction face;
            double dxToHub = cx - hx, dzToHub = cz - hz;
            if (Math.abs(dxToHub) > Math.abs(dzToHub)) {
                face = dxToHub > 0 ? Direction.EAST : Direction.WEST;
            } else {
                face = dzToHub > 0 ? Direction.SOUTH : Direction.NORTH;
            }
            if (face == Direction.EAST || face == Direction.WEST) {
                int tmp = hw;
                hw = hd;
                hd = tmp;
            }
            int hDeck = deckY + ((i % 2 == 1 && kind != 2) ? 1 : 0);
            huts.add(new StiltHut(hx, hz, hw, hd, hDeck, kind, face));
        }
        return new StiltVillage(cx, cz, deckY, variant, nearGardens, 26.0, huts);
    }

    public static StiltVillage stiltVillageNear(double x, double z, double range) {
        int gsx = Math.floorDiv((int) Math.floor(x), WaterfallGardens.SUPER);
        int gsz = Math.floorDiv((int) Math.floor(z), WaterfallGardens.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                WaterfallGardens.Region rg = WaterfallGardens.region(gsx + dx, gsz + dz);
                if (rg == null) continue;
                for (StiltVillage sv : gardenStiltVillages(rg)) {
                    if (Math.abs(x - sv.cx) <= range && Math.abs(z - sv.cz) <= range) return sv;
                }
            }
        }
        int ssx = Math.floorDiv((int) Math.floor(x), SUPER_STILT);
        int ssz = Math.floorDiv((int) Math.floor(z), SUPER_STILT);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                StiltVillage sv = seaStiltVillage(ssx + dx, ssz + dz);
                if (sv != null && Math.abs(x - sv.cx) <= range && Math.abs(z - sv.cz) <= range) return sv;
            }
        }
        return null;
    }

    // =========================================================================
    // 3 & 4. КОРАБЛИ: ВРЕЗАВШИЙСЯ В СКАЛУ И ЗАТОНУВШИЙ НА ДНЕ ОКЕАНА
    // =========================================================================
    private static boolean openDeepSea(int cx, int cz, int minDepthBelowSea, boolean surfaceSkyClear) {
        if (overlapsSpecialBiomes(cx, cz, 55)) return false;
        if (anyVillageAt(cx, cz, 95) != null) return false;
        // Проверяем лагунные свайные деревни у Висячих садов (без рекурсии в seaStiltVillage)
        int gsx = Math.floorDiv(cx, WaterfallGardens.SUPER), gsz = Math.floorDiv(cz, WaterfallGardens.SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                WaterfallGardens.Region rg = WaterfallGardens.region(gsx + dx, gsz + dz);
                if (rg == null) continue;
                for (StiltVillage sv : gardenStiltVillages(rg)) {
                    if (Math.hypot(cx - sv.cx, cz - sv.cz) < 95.0) return false;
                }
            }
        }
        CityPlan plan = CityPlan.get();
        for (int dx = -24; dx <= 24; dx += 12) {
            for (int dz = -24; dz <= 24; dz += 12) {
                if (plan.islandAt(cx + dx, cz + dz) != null) return false;
            }
        }
        if (surfaceSkyClear) {
            for (int dx = -44; dx <= 44; dx += 22) {
                for (int dz = -44; dz <= 44; dz += 22) {
                    CityPlan.Island isl = plan.islandAt(cx + dx, cz + dz);
                    if (isl != null && isl.R > 16.0) return false;
                }
            }
        }
        int fl = ElisarChunkGenerator.oceanFloor(plan, cx, cz);
        return fl <= plan.ocean - minDepthBelowSea && fl >= 2;
    }

    public static ShipSite crashedShip(int sx, int sz) {
        return CRASH_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genCrashedShip(sx, sz))).orElse(null);
    }

    private static ShipSite genCrashedShip(int sx, int sz) {
        Random rnd = new Random(SEED_CRASH ^ (sx * 492876847L + sz * 715225739L));
        if (rnd.nextDouble() > 0.72) return null;
        int margin = 95;
        for (int tries = 0; tries < 18; ++tries) {
            int cx = sx * SUPER_CRASH + margin + rnd.nextInt(SUPER_CRASH - 2 * margin);
            int cz = sz * SUPER_CRASH + margin + rnd.nextInt(SUPER_CRASH - 2 * margin);
            if (!openDeepSea(cx, cz, 24, true)) continue;

            int ocean = CityPlan.get().ocean;
            double yaw = rnd.nextDouble() * Math.PI * 2.0;
            int variant = rnd.nextInt(2); // 0 = скала с руиной маяка, 1 = двойной рифовый клык
            double ux = Math.cos(yaw), uz = Math.sin(yaw);
            int rockX = (int) Math.round(cx + ux * 17.5);
            int rockZ = (int) Math.round(cz + uz * 17.5);
            int rockTopY = ocean + 20 + rnd.nextInt(7);
            double rockR = 10.5 + rnd.nextDouble() * 2.5;
            return new ShipSite(cx, cz, variant, false, yaw, ocean, rockX, rockZ, rockTopY, rockR);
        }
        return null;
    }

    public static ShipSite sunkenShip(int sx, int sz) {
        return SUNKEN_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genSunkenShip(sx, sz))).orElse(null);
    }

    private static ShipSite genSunkenShip(int sx, int sz) {
        Random rnd = new Random(SEED_SUNKEN ^ (sx * 6364136223846793005L + sz * 1442695040888963407L));
        if (rnd.nextDouble() > 0.75) return null;
        int margin = 90;
        CityPlan plan = CityPlan.get();
        for (int tries = 0; tries < 18; ++tries) {
            int cx = sx * SUPER_SUNKEN + margin + rnd.nextInt(SUPER_SUNKEN - 2 * margin);
            int cz = sz * SUPER_SUNKEN + margin + rnd.nextInt(SUPER_SUNKEN - 2 * margin);
            if (!openDeepSea(cx, cz, 32, false)) continue;
            if (crashedShipNear(cx, cz, 90) != null) continue;

            int floorY = Math.max(5, ElisarChunkGenerator.oceanFloor(plan, cx, cz));
            double yaw = rnd.nextDouble() * Math.PI * 2.0;
            int variant = rnd.nextInt(3); // 0 = целый накренившийся с якорем, 1 = разломанный пополам, 2 = коралловый остов
            return new ShipSite(cx, cz, variant, true, yaw, floorY, cx, cz, floorY, 0.0);
        }
        return null;
    }

    public static ShipSite crashedShipNear(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_CRASH);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_CRASH);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                ShipSite s = crashedShip(sx + dx, sz + dz);
                if (s != null && Math.abs(x - s.cx) <= range && Math.abs(z - s.cz) <= range) return s;
            }
        }
        return null;
    }

    public static ShipSite sunkenShipNear(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_SUNKEN);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_SUNKEN);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                ShipSite s = sunkenShip(sx + dx, sz + dz);
                if (s != null && Math.abs(x - s.cx) <= range && Math.abs(z - s.cz) <= range) return s;
            }
        }
        return null;
    }

    // =========================================================================
    // 5. РЕДКИЕ ПОДВОДНЫЕ ЗДАНИЯ НА ДНЕ ОКЕАНА (4 вариации)
    // =========================================================================
    public static OceanBuilding oceanBuilding(int sx, int sz) {
        return OBLDG_CACHE.computeIfAbsent(key(sx, sz), k -> Optional.ofNullable(genOceanBuilding(sx, sz))).orElse(null);
    }

    private static OceanBuilding genOceanBuilding(int sx, int sz) {
        Random rnd = new Random(SEED_OBLDG ^ (sx * 2862933555777941757L + sz * 3037000493L));
        if (rnd.nextDouble() > 0.68) return null;
        int margin = 100;
        CityPlan plan = CityPlan.get();
        for (int tries = 0; tries < 18; ++tries) {
            int cx = sx * SUPER_OCEAN_BLDG + margin + rnd.nextInt(SUPER_OCEAN_BLDG - 2 * margin);
            int cz = sz * SUPER_OCEAN_BLDG + margin + rnd.nextInt(SUPER_OCEAN_BLDG - 2 * margin);
            if (!openDeepSea(cx, cz, 30, false)) continue;
            if (crashedShipNear(cx, cz, 85) != null || sunkenShipNear(cx, cz, 85) != null) continue;

            int floorY = Math.max(6, ElisarChunkGenerator.oceanFloor(plan, cx, cz));
            int variant = rnd.nextInt(4); // 0=ротонда, 1=двухэтажный дом-архив, 2=подводные врата, 3=затопленная улочка
            int rad = variant == 3 ? 16 : 12;
            return new OceanBuilding(cx, cz, floorY, variant, rnd.nextDouble() * Math.PI * 2.0, rad);
        }
        return null;
    }

    public static OceanBuilding oceanBuildingNear(double x, double z, double range) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER_OCEAN_BLDG);
        int sz = Math.floorDiv((int) Math.floor(z), SUPER_OCEAN_BLDG);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                OceanBuilding b = oceanBuilding(sx + dx, sz + dz);
                if (b != null && Math.abs(x - b.cx) <= range && Math.abs(z - b.cz) <= range) return b;
            }
        }
        return null;
    }

    // =========================================================================
    // ПОСТРОЙКА ВСЕХ НОВЫХ СТРУКТУР В ЧАНКЕ (вызывается из applyBiomeDecoration)
    // =========================================================================
    private static void putChunk(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0,
                                 int x, int y, int z, BlockState st) {
        if (x >= x0 && x <= x0 + 15 && z >= z0 && z <= z0 + 15
                && y >= chunk.getMinBuildHeight() + 2 && y < chunk.getMaxBuildHeight() - 1) {
            chunk.setBlockState(mp.set(x, y, z), st, false);
        }
    }

    private static BlockState waterlogged(BlockState st, boolean sub) {
        if (sub && st.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return st.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return st;
    }

    public static void buildInChunk(WorldGenLevel level, ChunkAccess chunk, BoundingBox box,
                                    StructureTemplateManager stm, RandomSource rnd,
                                    CityPlan plan, int x0, int z0) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        double midX = x0 + 8.0, midZ = z0 + 8.0;

        // 1 & 2) Белые и сгоревшие деревни (островные + на пепельной равнине AshenLands)
        Village vil = anyVillageAt(midX, midZ, 56);
        if (vil != null) {
            buildVillage(level, chunk, box, mp, stm, rnd, plan, vil, x0, z0);
        }

        // 3) Большой корабль, врезавшийся в морскую скалу
        ShipSite crash = crashedShipNear(midX, midZ, 44);
        if (crash != null) {
            buildCrashedShipAndRock(chunk, mp, plan, crash, x0, z0);
        }

        // 4) Большой затонувший корабль на дне океана
        ShipSite sunk = sunkenShipNear(midX, midZ, 40);
        if (sunk != null) {
            buildSunkenShip(chunk, mp, plan, sunk, x0, z0);
        }

        // 5) Редкое подводное здание на дне океана
        OceanBuilding ob = oceanBuildingNear(midX, midZ, 34);
        if (ob != null) {
            buildOceanBuilding(chunk, mp, plan, ob, x0, z0);
        }

        // 6) Надводная деревня на сваях (часто в лагунах возле Висячих садов + на отмелях)
        StiltVillage sv = stiltVillageNear(midX, midZ, 42);
        if (sv != null) {
            buildStiltVillage(chunk, mp, plan, sv, x0, z0);
        }
    }

    // -------------------------------------------------------------------------
    // Постройка Белой деревни / Сгоревшей деревни (дорожки, колодец/фонтан и 3..6 NBT-домов)
    // -------------------------------------------------------------------------
    private static void buildVillage(WorldGenLevel level, ChunkAccess chunk, BoundingBox box,
                                     BlockPos.MutableBlockPos mp, StructureTemplateManager stm,
                                     RandomSource rnd, CityPlan plan, Village v, int x0, int z0) {
        int vy = v.baseY;
        BlockState cobble = v.burned ? CastleBlocks.SCORCHED_STONE.get().defaultBlockState() : ElisarBlocks.COBBLE.get().defaultBlockState();
        BlockState brick = v.burned ? CastleBlocks.BRICKS_CRACKED.get().defaultBlockState() : ElisarBlocks.BRICKS.get().defaultBlockState();
        BlockState path = v.burned ? CastleBlocks.BURNT_SOIL.get().defaultBlockState() : ElisarBlocks.PATH.get().defaultBlockState();
        BlockState ash = CastleBlocks.ASH.get().defaultBlockState();
        BlockState stone = v.burned ? CastleBlocks.SCORCHED_STONE.get().defaultBlockState() : ElisarBlocks.STONE.get().defaultBlockState();
        BlockState grass = v.burned ? CastleBlocks.BURNT_SOIL.get().defaultBlockState() : ElisarBlocks.GRASS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // А) Выравниваем каменное основание на пепельной равнине (или под домами и площадью на острове)
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double dc = Math.hypot(x - v.cx, z - v.cz);
                if (dc > v.flatR + 7.0) continue;
                if (v.onAshPlain) {
                    int rTop = vy - 1;
                    if (dc > v.flatR) {
                        double u = (dc - v.flatR) / 7.0;
                        rTop = (int) Math.round((vy - 1) * (1.0 - u) + (plan.ocean - 1) * u);
                    }
                    for (int y = plan.ocean - 2; y <= rTop; ++y) {
                        BlockState bs = (y == rTop)
                                ? (ElisarChunkGenerator.hash(x, z, 811) < 0.45 ? ash : grass)
                                : stone;
                        putChunk(chunk, mp, x0, z0, x, y, z, bs);
                    }
                }
                // Центральная площадь (r <= 6.2) и дорожки к дверям каждого дома
                boolean onPlaza = dc <= 6.2;
                boolean onLane = false;
                if (!onPlaza) {
                    for (HousePlot hp : v.houses) {
                        double segD = distToSegment(x + 0.5, z + 0.5, v.cx, v.cz, hp.doorX + 0.5, hp.doorZ + 0.5);
                        if (segD <= 1.85) { onLane = true; break; }
                    }
                }
                if (onPlaza || onLane) {
                    double h = ElisarChunkGenerator.hash(x, z, 817);
                    BlockState surf = onPlaza
                            ? (h < 0.48 ? brick : (h < 0.85 ? cobble : (v.burned ? ash : path)))
                            : (h < 0.62 ? path : cobble);
                    putChunk(chunk, mp, x0, z0, x, vy - 1, z, surf);
                    // Очищаем траву над дорожкой/площадью
                    if (dc > 3.2) {
                        putChunk(chunk, mp, x0, z0, x, vy, z,
                                (v.burned && h < 0.14) ? ModBlocks.DEBRIS.get().defaultBlockState() : air);
                    }
                }
                // Каменный фундамент строго под каждым домом (чтобы ни один угол дома не висел в воздухе)
                for (HousePlot hp : v.houses) {
                    if (x >= hp.x0 - 1 && x <= hp.x0 + hp.w && z >= hp.z0 - 1 && z <= hp.z0 + hp.d) {
                        for (int fy = vy - 5; fy <= vy - 1; ++fy) {
                            putChunk(chunk, mp, x0, z0, x, fy, z, fy == vy - 1 ? cobble : stone);
                        }
                    }
                }
            }
        }

        // Б) Центральный архитектурный акцент деревни (по вариациям!)
        buildVillageCenterpiece(chunk, mp, v, x0, z0);

        // В) Размещаем 3..6 NBT-домов деревни
        for (HousePlot hp : v.houses) {
            if (hp.x0 > x0 + 15 || hp.x0 + hp.w - 1 < x0 || hp.z0 > z0 + 15 || hp.z0 + hp.d - 1 < z0) continue;
            Optional<StructureTemplate> tpl = stm.get(new ResourceLocation("riftworld", hp.nbtPath));
            if (tpl.isEmpty()) continue;
            Rotation rot = switch (hp.facing) {
                case "west" -> Rotation.CLOCKWISE_90;
                case "north" -> Rotation.CLOCKWISE_180;
                case "east" -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
            BoundingBox bb = tpl.get().getBoundingBox(s, BlockPos.ZERO);
            BlockPos pos = new BlockPos(hp.x0 - bb.minX(), hp.y, hp.z0 - bb.minZ());
            tpl.get().placeInWorld(level, pos, pos, s, rnd, 2);
        }
    }

    private static void buildVillageCenterpiece(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                Village v, int x0, int z0) {
        int cx = v.cx, cz = v.cz, y = v.baseY;
        if (v.burned) {
            // Разрушенный почерневший фонтан с пеплом, тлеющей магмой и дымом костра + обгоревшие столбы
            BlockState rim = CastleBlocks.BRICKS_CRACKED.get().defaultBlockState();
            BlockState scorch = CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
            BlockState magma = ModBlocks.MAGMA_MEANDER.get().defaultBlockState();
            BlockState charPost = CastleBlocks.DARK_FENCE.get().defaultBlockState();
            for (int dx = -2; dx <= 2; ++dx) {
                for (int dz = -2; dz <= 2; ++dz) {
                    int ad = Math.max(Math.abs(dx), Math.abs(dz));
                    if (ad == 2) {
                        if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                        putChunk(chunk, mp, x0, z0, cx + dx, y, cz + dz, (dx + dz) % 2 == 0 ? rim : scorch);
                    } else {
                        putChunk(chunk, mp, x0, z0, cx + dx, y - 1, cz + dz, (dx == 0 && dz == 0) ? scorch : magma);
                    }
                }
            }
            putChunk(chunk, mp, x0, z0, cx, y, cz, scorch);
            putChunk(chunk, mp, x0, z0, cx, y + 1, cz, Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
            int[][] posts = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
            for (int[] p : posts) {
                putChunk(chunk, mp, x0, z0, cx + p[0], y, cz + p[1], scorch);
                putChunk(chunk, mp, x0, z0, cx + p[0], y + 1, cz + p[1], charPost);
            }
            return;
        }

        // Живая Белая деревня: 3 вариации центрального ансамбля
        BlockState brick = ElisarBlocks.BRICKS.get().defaultBlockState();
        BlockState plaster = ElisarBlocks.PLASTER.get().defaultBlockState();
        BlockState post = ElisarBlocks.FENCE.get().defaultBlockState();
        BlockState timber = ElisarBlocks.TIMBER_POST.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE_SLAB.get().defaultBlockState();
        BlockState roofRed = ElisarBlocks.ROOF_RED_SLAB.get().defaultBlockState();
        BlockState gWater = ElisarBlocks.GARDEN_WATER.get().defaultBlockState();
        BlockState lanternHang = ElisarBlocks.LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState lanternStand = ElisarBlocks.LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, false);

        if (v.variant == 0) {
            // Вариация 0: Белокаменный колодец под синим шатром + фонарные столбы площади
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dz == 0) {
                        putChunk(chunk, mp, x0, z0, cx, y - 1, cz, gWater);
                        putChunk(chunk, mp, x0, z0, cx, y, cz, Blocks.AIR.defaultBlockState());
                    } else {
                        putChunk(chunk, mp, x0, z0, cx + dx, y, cz + dz, brick);
                    }
                    putChunk(chunk, mp, x0, z0, cx + dx, y + 3, cz + dz, roofBlue);
                }
            }
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    putChunk(chunk, mp, x0, z0, cx + dx, y + 1, cz + dz, post);
                    putChunk(chunk, mp, x0, z0, cx + dx, y + 2, cz + dz, post);
                }
            }
            putChunk(chunk, mp, x0, z0, cx, y + 3, cz, ElisarBlocks.ROOF_BLUE.get().defaultBlockState());
            putChunk(chunk, mp, x0, z0, cx, y + 2, cz, lanternHang);
        } else if (v.variant == 1) {
            // Вариация 1: Белокаменная арка-звонница над ремесленной улочкой + бочки и пшеничная грядка
            for (int dz = -2; dz <= 2; dz += 4) {
                for (int dy = 0; dy <= 3; ++dy) {
                    putChunk(chunk, mp, x0, z0, cx, y + dy, cz + dz, dy == 0 ? brick : plaster);
                }
            }
            for (int dz = -2; dz <= 2; ++dz) {
                putChunk(chunk, mp, x0, z0, cx, y + 4, cz + dz, brick);
                putChunk(chunk, mp, x0, z0, cx, y + 5, cz + dz, roofRed);
            }
            putChunk(chunk, mp, x0, z0, cx, y + 3, cz, lanternHang);
            putChunk(chunk, mp, x0, z0, cx + 3, y, cz - 2, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
            putChunk(chunk, mp, x0, z0, cx + 4, y, cz - 2, Blocks.HAY_BLOCK.defaultBlockState());
        } else {
            // Вариация 2: Белокаменная смотровая ротонда-маячок в центре заставы
            for (int dx = -2; dx <= 2; ++dx) {
                for (int dz = -2; dz <= 2; ++dz) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    putChunk(chunk, mp, x0, z0, cx + dx, y, cz + dz, brick);
                    putChunk(chunk, mp, x0, z0, cx + dx, y + 4, cz + dz, roofBlue);
                }
            }
            int[][] cols = {{-2, -1}, {2, -1}, {-1, 2}, {1, 2}};
            for (int[] c : cols) {
                for (int dy = 1; dy <= 3; ++dy) {
                    putChunk(chunk, mp, x0, z0, cx + c[0], y + dy, cz + c[1], timber);
                }
            }
            putChunk(chunk, mp, x0, z0, cx, y + 1, cz, brick);
            putChunk(chunk, mp, x0, z0, cx, y + 2, cz, lanternStand);
            putChunk(chunk, mp, x0, z0, cx, y + 5, cz, ElisarBlocks.ROOF_BLUE.get().defaultBlockState());
        }

        // Уличные фонари Белого города по углам деревенской площади
        int[][] lamps = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] lp : lamps) {
            putChunk(chunk, mp, x0, z0, cx + lp[0], y, cz + lp[1], brick);
            putChunk(chunk, mp, x0, z0, cx + lp[0], y + 1, cz + lp[1], post);
            putChunk(chunk, mp, x0, z0, cx + lp[0], y + 2, cz + lp[1], post);
            putChunk(chunk, mp, x0, z0, cx + lp[0], y + 3, cz + lp[1], lanternStand);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Постройка Большого корабля, врезавшегося в морскую скалу (32 блока + утёс)
    // -------------------------------------------------------------------------
    private static void buildCrashedShipAndRock(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                                CityPlan plan, ShipSite s, int x0, int z0) {
        int ocean = plan.ocean;
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState reefRock = ElisarBlocks.REEF_STONE.get().defaultBlockState();
        BlockState keepStone = ElisarBlocks.KEEP_STONE.get().defaultBlockState();
        BlockState keepMossy = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState elisarStone = ElisarBlocks.STONE.get().defaultBlockState();
        BlockState elisarMossy = ElisarBlocks.STONE_MOSSY.get().defaultBlockState();
        BlockState moss = ElisarBlocks.MOSS.get().defaultBlockState();

        // А) Зубчатый морской скальный пик (Sea Stack), в который врезался нос корабля
        double ux = Math.cos(s.yaw), uz = Math.sin(s.yaw);
        double vx = -uz, vz = ux;
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double dx = x - s.rockX, dz = z - s.rockZ;
                double dMain = Math.hypot(dx, dz);
                // Для вариации 1 — второй острый скальный клык сбоку от борта корабля
                double dFang = s.variant == 1
                        ? Math.hypot(x - (s.cx + ux * 4.0 + vx * 6.5), z - (s.cz + uz * 4.0 + vz * 6.5))
                        : 999.0;
                if (dMain > s.rockR + 5.0 && dFang > 7.5) continue;

                int floor = ElisarChunkGenerator.oceanFloor(plan, x, z);
                int topY = Integer.MIN_VALUE;
                if (dMain <= s.rockR + 3.5) {
                    double th = Math.atan2(dz, dx);
                    double rShape = s.rockR * (0.86 + 0.18 * Math.sin(3 * th + 1.1) + 0.12 * Math.cos(5 * th + 0.4));
                    if (dMain < rShape) {
                        double u = 1.0 - dMain / rShape;
                        int h = (int) Math.round((s.rockTopY - floor) * Math.pow(u, 0.58)
                                + (ElisarChunkGenerator.vnoise(x * 0.22, z * 0.22, 931) - 0.5) * 5.0);
                        topY = Math.max(topY, floor + h);
                    }
                }
                if (dFang < 6.2) {
                    double uf = 1.0 - dFang / 6.2;
                    topY = Math.max(topY, floor + (int) Math.round((ocean + 15 - floor) * Math.pow(uf, 0.52)));
                }
                if (topY > floor) {
                    for (int y = floor; y <= topY; ++y) {
                        double hv = ElisarChunkGenerator.hash(x, y, z);
                        BlockState bs;
                        if (y == topY && y > ocean + 3 && hv < 0.55) bs = moss;
                        else if (y < ocean - 3) bs = hv < 0.65 ? reefRock : keepMossy;
                        else if (y <= ocean + 3) bs = hv < 0.60 ? keepMossy : elisarMossy;
                        else bs = hv < 0.58 ? elisarStone : (hv < 0.84 ? keepStone : elisarMossy);
                        putChunk(chunk, mp, x0, z0, x, y, z, bs);
                    }
                }
            }
        }
        // На вершине главного скального пика (вариация 0) — полуразрушенный каменный маяк-жаровня
        if (s.variant == 0 && s.rockX >= x0 - 2 && s.rockX <= x0 + 17 && s.rockZ >= z0 - 2 && s.rockZ <= z0 + 17) {
            int by = s.rockTopY;
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    putChunk(chunk, mp, x0, z0, s.rockX + dx, by, s.rockZ + dz, ElisarBlocks.BRICKS.get().defaultBlockState());
                }
            }
            putChunk(chunk, mp, x0, z0, s.rockX - 1, by + 1, s.rockZ - 1, ElisarBlocks.BRICKS.get().defaultBlockState());
            putChunk(chunk, mp, x0, z0, s.rockX + 1, by + 1, s.rockZ - 1, ElisarBlocks.BRICKS.get().defaultBlockState());
            putChunk(chunk, mp, x0, z0, s.rockX - 1, by + 2, s.rockZ - 1, ElisarBlocks.BRICKS.get().defaultBlockState());
            putChunk(chunk, mp, x0, z0, s.rockX, by + 1, s.rockZ, Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
        }

        // Б) Сам 32-блочный парусный галеон, наклонно врезавшийся носом в скалу!
        buildGalleonVoxelHull(chunk, mp, x0, z0, s.cx, s.baseY + 1, s.cz, s.yaw,
                0.17, s.variant == 1 ? 0.18 : -0.09, false, s.variant, ocean, seaWater);
    }

    // -------------------------------------------------------------------------
    // 4. Постройка Большого затонувшего корабля на дне океана (32 блока, 3 вариации)
    // -------------------------------------------------------------------------
    private static void buildSunkenShip(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                        CityPlan plan, ShipSite s, int x0, int z0) {
        int ocean = plan.ocean;
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState seabed = ElisarBlocks.SEABED.get().defaultBlockState();
        BlockState reefRock = ElisarBlocks.REEF_STONE.get().defaultBlockState();

        // Песчаная подушка под килем на морском дне (чтобы корабль лежал на дне, а не висел над промоиной)
        double ux = Math.cos(s.yaw), uz = Math.sin(s.yaw);
        double vx = -uz, vz = ux;
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double dx = x - s.cx, dz = z - s.cz;
                double ls = dx * ux + dz * uz;
                double lw = dx * vx + dz * vz;
                if (Math.abs(ls) <= 18.0 && Math.abs(lw) <= 7.0) {
                    int curFloor = ElisarChunkGenerator.oceanFloor(plan, x, z);
                    int targetFloor = s.baseY - 1;
                    for (int y = Math.min(curFloor, targetFloor - 3); y <= targetFloor; ++y) {
                        putChunk(chunk, mp, x0, z0, x, y, z, y == targetFloor ? seabed : reefRock);
                    }
                }
            }
        }

        double pitch = s.variant == 1 ? -0.06 : 0.05;
        double roll = s.variant == 0 ? 0.24 : (s.variant == 2 ? -0.20 : 0.12);
        buildGalleonVoxelHull(chunk, mp, x0, z0, s.cx, s.baseY + 2, s.cz, s.yaw,
                pitch, roll, true, s.variant, ocean, seaWater);

        // Якорная цепь и каменный/железный якорь на песке у носа затонувшего корабля (вариация 0 и 2)
        if (s.variant != 1) {
            for (int step = 0; step <= 8; ++step) {
                int ax = (int) Math.round(s.cx + ux * (15.0 + step * 0.9) + vx * (step * 0.4));
                int az = (int) Math.round(s.cz + uz * (15.0 + step * 0.9) + vz * (step * 0.4));
                int ay = Math.max(4, s.baseY + (step < 3 ? 2 - step : 0));
                BlockState ch = CastleBlocks.DARK_BEAM.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, step < 3 ? Direction.Axis.Y : Direction.Axis.X);
                putChunk(chunk, mp, x0, z0, ax, ay, az, ch);
                if (step == 8) {
                    putChunk(chunk, mp, x0, z0, ax, ay, az, Blocks.IRON_BLOCK.defaultBlockState());
                    putChunk(chunk, mp, x0, z0, ax + 1, ay, az, Blocks.IRON_BLOCK.defaultBlockState());
                    putChunk(chunk, mp, x0, z0, ax - 1, ay, az, Blocks.IRON_BLOCK.defaultBlockState());
                }
            }
        }
    }

    /**
     * Универсальный 3D-воксельный построитель 32-блочного галеона (сухопутно-скальный ИЛИ глубоководный затонувший).
     * Проходит по плотной локальной сетке {@code (ls, lw, lh)} с шагом 0.5 блока, чтобы при любом угле {@code yaw},
     * {@code pitch} и {@code roll} обшивка бортов и палуб не имела дыр!
     */
    private static void buildGalleonVoxelHull(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                              int x0, int z0, double cx, double cy, double cz,
                                              double yaw, double pitch, double roll,
                                              boolean sunken, int variant, int ocean, BlockState seaWater) {
        double ux = Math.cos(yaw), uz = Math.sin(yaw);
        double vx = -uz, vz = ux;

        BlockState hullDark = CastleBlocks.DARK_PLANKS.get().defaultBlockState();
        BlockState hullStripe = ElisarBlocks.AMBRA_PLANKS.get().defaultBlockState();
        BlockState keelLog = CastleBlocks.DARK_BEAM.get().defaultBlockState();
        BlockState deckPlank = sunken ? CastleBlocks.DARK_PLANKS.get().defaultBlockState() : ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState mastLog = CastleBlocks.DARK_BEAM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState yardLog = ElisarBlocks.SIZO_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState sailMain = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState sailTrim = ElisarBlocks.PLASTER.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Корпус галеона: ls от -15.0 (корма) до +15.5 (нос)
        for (double ls = -15.0; ls <= 15.5; ls += 0.5) {
            // Вариация 1 затонувшего корабля: корабль разломан пополам посредине корпуса!
            if (sunken && variant == 1 && ls > -2.2 && ls < 2.2) {
                if (Math.abs(ls) < 1.2) {
                    int bx = (int) Math.round(cx + ux * ls + vx * ((ls * 3.0) % 3.0));
                    int bz = (int) Math.round(cz + uz * ls + vz * ((ls * 3.0) % 3.0));
                    int by = (int) Math.round(cy - 2);
                    putChunk(chunk, mp, x0, z0, bx, by, bz,
                            ls < 0 ? Blocks.BARREL.defaultBlockState() : ElisarBlocks.AURITE_ORE.get().defaultBlockState());
                }
                continue;
            }

            double norm = ls / 15.5; // -0.97 .. +1.0
            double halfW;
            if (norm >= 0) {
                halfW = 4.7 * Math.pow(Math.max(0.0, 1.0 - norm * norm * 0.92), 0.62) + 0.55;
            } else {
                double b = -norm;
                halfW = 4.7 * (1.0 - 0.24 * b * b);
            }
            double sheer = (norm > 0 ? norm * norm * 2.2 : norm * norm * 3.2);
            int cabinExtra = (ls <= -8.5) ? 3 : (ls >= 9.5 ? 1 : 0);
            double deckH = 4.0 + sheer;
            double topH = deckH + cabinExtra;

            double sectionShift = (sunken && variant == 1 && ls >= 2.2) ? 2.0 : 0.0;

            for (double lw = -halfW - 0.5; lw <= halfW + 0.5; lw += 0.5) {
                double aw = Math.abs(lw);
                if (aw > halfW) continue;
                double keelFloor = (aw / Math.max(0.8, halfW));
                keelFloor = keelFloor * keelFloor * 2.6; // V-образный изогнутый киль

                for (double lh = keelFloor - 0.5; lh <= topH + 1.0; lh += 0.5) {
                    double wy = cy + lh + ls * Math.sin(pitch) + lw * Math.sin(roll);
                    double wx = cx + ux * ls + vx * (lw + sectionShift);
                    double wz = cz + uz * ls + vz * (lw + sectionShift);
                    int ix = (int) Math.floor(wx), iy = (int) Math.floor(wy), iz = (int) Math.floor(wz);
                    if (ix < x0 || ix > x0 + 15 || iz < z0 || iz > z0 + 15) continue;

                    boolean sub = sunken || iy <= ocean;
                    boolean isShell = aw >= halfW - 0.85 || lh <= keelFloor + 0.75
                            || ls <= -14.3 || ls >= 14.8;
                    boolean isDeck = Math.abs(lh - deckH) <= 0.45
                            || (cabinExtra > 0 && Math.abs(lh - topH) <= 0.45);
                    boolean isCabinBulkhead = (cabinExtra == 3 && Math.abs(ls - (-8.5)) <= 0.5 && lh >= deckH && lh <= topH);
                    boolean isRail = Math.abs(lh - (topH + 0.85)) <= 0.42 && aw >= halfW - 0.85;

                    if (isRail) {
                        if (sunken && variant == 2 && ElisarChunkGenerator.hash(ix, iy, iz) < 0.55) continue;
                        // Под водой в elisar_water ванильный WATERLOGGED ставит minecraft:water с видимыми гранями —
                        // поэтому на затонувшем корабле делаем зубчатый деревянный фальшборт из цельных блоков без рамок воды!
                        BlockState rail = sub
                                ? (((ix + iz) & 1) == 0 ? hullDark : seaWater)
                                : CastleBlocks.DARK_FENCE.get().defaultBlockState();
                        putChunk(chunk, mp, x0, z0, ix, iy, iz, rail);
                    } else if (isShell || isCabinBulkhead) {
                        // В древнем коралловом остове (sunken variant 2) часть досок сгнила, обнажая шпангоуты и кораллы
                        if (sunken && variant == 2 && lh > 2.0 && ((int) Math.round(ls) % 3 != 0)
                                && ElisarChunkGenerator.hash(ix, iy, iz) < 0.46) {
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, seaWater);
                            continue;
                        }
                        // Кормовые окна каюты капитана
                        if (ls <= -14.3 && lh >= deckH + 1.0 && lh <= deckH + 2.1 && aw <= 2.2) {
                            BlockState win = sub
                                    ? Blocks.SEA_LANTERN.defaultBlockState()
                                    : ElisarBlocks.WINDOW.get().defaultBlockState();
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, win);
                        } else if (isCabinBulkhead && aw <= 0.8 && lh <= deckH + 2.0) {
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, sub ? seaWater : air); // вход в каюту капитана
                        } else if (aw <= 0.7 && lh <= 1.2) {
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, keelLog);
                        } else {
                            boolean stripe = Math.abs(lh - (deckH - 0.8)) <= 0.55;
                            BlockState plank = (sunken && ElisarChunkGenerator.hash(ix, iy, iz) < 0.14)
                                    ? ElisarBlocks.CORAL_CAP_TEAL.get().defaultBlockState()
                                    : (stripe ? hullStripe : hullDark);
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, plank);
                        }
                    } else if (isDeck) {
                        // Грузовой люк в центре палубы
                        if (Math.abs(ls - 2.0) <= 1.4 && aw <= 1.3 && lh <= deckH + 0.5) {
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, sub ? seaWater : air);
                        } else {
                            putChunk(chunk, mp, x0, z0, ix, iy, iz, deckPlank);
                        }
                    } else if (lh < topH) {
                        // Внутренний трюм и каюта: под водой СТРОГО вода (0 пузырей воздуха!), над водой — воздух
                        putChunk(chunk, mp, x0, z0, ix, iy, iz, sub ? seaWater : air);
                    }
                }
            }
        }

        // Бушприт на носу (ls = +14..+21)
        for (int b = 0; b <= 14; ++b) {
            double ls = 14.0 + b * 0.5;
            double wy = cy + 6.4 + b * 0.22 + ls * Math.sin(pitch);
            int ix = (int) Math.floor(cx + ux * ls);
            int iy = (int) Math.floor(wy);
            int iz = (int) Math.floor(cz + uz * ls);
            putChunk(chunk, mp, x0, z0, ix, iy, iz, keelLog);
        }

        // Трюмные и палубные детали: сундук/бочки с грузом и морские/обычные фонари
        placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -12.0, 0.0, 5.2,
                (sunken || (cy + 5.2 <= ocean))
                        ? Blocks.BARREL.defaultBlockState()
                        : Blocks.CHEST.defaultBlockState());
        placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -11.5, 2.0, 5.2,
                sunken ? Blocks.SEA_LANTERN.defaultBlockState()
                        : ElisarBlocks.LANTERN.get().defaultBlockState());
        placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -4.0, -2.2, 4.3,
                Blocks.BARREL.defaultBlockState());
        placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -3.0, -2.2, 4.3,
                Blocks.BARREL.defaultBlockState());
        if (sunken) {
            // Светящиеся подводные акценты на палубе затонувшего корабля (чтобы он сиял в толще воды!)
            placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, 0.0, 0.0, 4.5,
                    Blocks.SEA_LANTERN.defaultBlockState());
            placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, 6.5, 1.2, 5.0,
                    ElisarBlocks.CORAL_GLOW.get().defaultBlockState());
            placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -9.5, -1.5, 8.2,
                    ElisarBlocks.CORAL_GLOW.get().defaultBlockState());
        } else {
            // Кормовые фонари над водой
            placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -15.0, -2.4, 9.2,
                    ElisarBlocks.LANTERN.get().defaultBlockState());
            placeShipProp(chunk, mp, x0, z0, cx, cy, cz, ux, uz, vx, vz, pitch, roll, -15.0, 2.4, 9.2,
                    ElisarBlocks.LANTERN.get().defaultBlockState());
        }

        // Грот-мачта (ls = -1.5) и фок-мачта (ls = +7.5) + реи и рваные паруса!
        double[][] masts = {{-1.5, sunken ? 12.0 : 19.0}, {7.5, sunken ? 8.5 : 12.5}};
        for (int mi = 0; mi < masts.length; ++mi) {
            double mls = masts[mi][0];
            if (sunken && variant == 1 && Math.abs(mls) < 3.0) continue;
            double mHeight = masts[mi][1];
            for (double mh = 2.0; mh <= mHeight; mh += 0.5) {
                // У врезавшегося корабля фок-мачта надломлена на высоте 8 блоков и наклонена вперёд к скале!
                double fwdLean = (!sunken && mi == 1 && mh > 8.0) ? (mh - 8.0) * 0.65 : 0.0;
                double wx = cx + ux * (mls + fwdLean) + vx * (mh * Math.sin(roll) * 0.45);
                double wz = cz + uz * (mls + fwdLean) + vz * (mh * Math.sin(roll) * 0.45);
                double wy = cy + 4.0 + mh + mls * Math.sin(pitch);
                int ix = (int) Math.floor(wx), iy = (int) Math.floor(wy), iz = (int) Math.floor(wz);
                if (sunken && iy >= ocean - 2) break;
                putChunk(chunk, mp, x0, z0, ix, iy, iz, mastLog);
            }

            // Поперечная рея и надутый ветром рваный парус (только на надводном врезавшемся корабле или остатки реи под водой)
            double yardH = mi == 0 ? (sunken ? 9.5 : 14.5) : (sunken ? 6.5 : 8.0);
            double yardHalfW = mi == 0 ? 5.5 : 4.0;
            for (double yw = -yardHalfW; yw <= yardHalfW; yw += 0.5) {
                double wx = cx + ux * mls + vx * yw;
                double wz = cz + uz * mls + vz * yw;
                double wy = cy + 4.0 + yardH + mls * Math.sin(pitch);
                int ix = (int) Math.floor(wx), iy = (int) Math.floor(wy), iz = (int) Math.floor(wz);
                if (sunken && iy >= ocean - 2) continue;
                putChunk(chunk, mp, x0, z0, ix, iy, iz, yardLog);

                if (!sunken) {
                    // Рваное полотнище паруса ниже реи с изгибом по ветру
                    double tear = 2.8 + 2.6 * Math.sin(yw * 1.1 + mi * 2.0);
                    int sailDrop = (int) Math.round(mi == 0 ? tear + 2.5 : tear);
                    for (int sd = 1; sd <= sailDrop; ++sd) {
                        double belly = Math.sin((sd / 6.0) * Math.PI) * 1.35 * (1.0 - (yw * yw) / (yardHalfW * yardHalfW + 4.0));
                        int sx2 = (int) Math.floor(wx + ux * belly);
                        int sz2 = (int) Math.floor(wz + uz * belly);
                        int sy2 = iy - sd;
                        putChunk(chunk, mp, x0, z0, sx2, sy2, sz2, (sd % 3 == 0) ? sailTrim : sailMain);
                    }
                } else if (Math.abs(yw) <= 2.5 && ElisarChunkGenerator.hash(ix, iy, iz) < 0.45) {
                    // На затонувшем корабле с рей свисают пряди ламинарии!
                    for (int kd = 1; kd <= 3; ++kd) {
                        if (iy - kd > s_baseYSafe(cy)) {
                            putChunk(chunk, mp, x0, z0, ix, iy - kd, iz, ElisarBlocks.SEA_KELP.get().defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static double s_baseYSafe(double cy) {
        return cy + 4.5;
    }

    private static void placeShipProp(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0,
                                      double cx, double cy, double cz,
                                      double ux, double uz, double vx, double vz,
                                      double pitch, double roll,
                                      double ls, double lw, double lh, BlockState st) {
        int ix = (int) Math.floor(cx + ux * ls + vx * lw);
        int iy = (int) Math.floor(cy + lh + ls * Math.sin(pitch) + lw * Math.sin(roll));
        int iz = (int) Math.floor(cz + uz * ls + vz * lw);
        putChunk(chunk, mp, x0, z0, ix, iy, iz, st);
    }

    // -------------------------------------------------------------------------
    // 5. Редкие подводные здания на дне океана (4 вариации, 100% waterlogged!)
    // -------------------------------------------------------------------------
    private static void buildOceanBuilding(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                           CityPlan plan, OceanBuilding b, int x0, int z0) {
        int fy = b.floorY;
        int ocean = plan.ocean;
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();
        BlockState lime = ElisarBlocks.GARDEN_LIMESTONE.get().defaultBlockState();
        BlockState limeM = ElisarBlocks.GARDEN_LIMESTONE_MOSSY.get().defaultBlockState();
        BlockState brick = ElisarBlocks.BRICKS.get().defaultBlockState();
        BlockState brickM = ElisarBlocks.BRICKS_MOSSY.get().defaultBlockState();
        BlockState keepM = ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState();
        BlockState mosaic = CastleBlocks.MOSAIC.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState cageLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState chest = Blocks.BARREL.defaultBlockState();
        BlockState glowCoral = ElisarBlocks.CORAL_GLOW.get().defaultBlockState();

        // Каменный цоколь до рельефа дна, чтобы подводное здание стояло монолитно
        for (int x = x0; x <= x0 + 15; ++x) {
            for (int z = z0; z <= z0 + 15; ++z) {
                double d = Math.hypot(x - b.cx, z - b.cz);
                if (d > b.radius + 1.5) continue;
                int natFloor = ElisarChunkGenerator.oceanFloor(plan, x, z);
                for (int y = Math.min(natFloor, fy - 4); y <= fy; ++y) {
                    BlockState fbs = (y == fy)
                            ? ((d < 4.5 && b.variant != 3) ? mosaic : (ElisarChunkGenerator.hash(x, y, z) < 0.55 ? brickM : limeM))
                            : keepM;
                    putChunk(chunk, mp, x0, z0, x, y, z, fbs);
                }
                // Очищаем внутренний объём водой от возможных рифовых холмиков
                for (int y = fy + 1; y <= Math.min(ocean - 2, fy + 15); ++y) {
                    putChunk(chunk, mp, x0, z0, x, y, z, seaWater);
                }
            }
        }

        if (b.variant == 0) {
            // Вариация 0: Затопленная белокаменная ротонда с полуразрушенным синим куполом
            for (int k = 0; k < 8; ++k) {
                double a = k * (Math.PI / 4.0);
                int px = (int) Math.round(b.cx + Math.cos(a) * 7.5);
                int pz = (int) Math.round(b.cz + Math.sin(a) * 7.5);
                int colH = (k == 2 || k == 5) ? 4 : 7;
                for (int dy = 1; dy <= colH; ++dy) {
                    putChunk(chunk, mp, x0, z0, px, fy + dy, pz, dy % 3 == 0 ? limeM : lime);
                }
            }
            // Кольцевой архитрав и проломленный купол
            for (int dx = -8; dx <= 8; ++dx) {
                for (int dz = -8; dz <= 8; ++dz) {
                    double r = Math.hypot(dx, dz);
                    if (r >= 6.8 && r <= 8.3 && !(dx > 2 && dz > 2)) {
                        putChunk(chunk, mp, x0, z0, b.cx + dx, fy + 8, b.cz + dz, brickM);
                    }
                    if (r <= 7.5 && !(dx > 1 && dz > 0)) {
                        int domeY = fy + 9 + (int) Math.round(Math.sqrt(Math.max(0.0, 56.25 - r * r)) * 0.48);
                        if (domeY < ocean - 3 && r >= 2.2) {
                            putChunk(chunk, mp, x0, z0, b.cx + dx, domeY, b.cz + dz,
                                    ElisarChunkGenerator.hash(b.cx + dx, domeY, b.cz + dz) < 0.72 ? roofBlue : brickM);
                        }
                    }
                }
            }
            putChunk(chunk, mp, x0, z0, b.cx, fy + 1, b.cz, lime);
            putChunk(chunk, mp, x0, z0, b.cx, fy + 2, b.cz, seaLantern);
            putChunk(chunk, mp, x0, z0, b.cx + 2, fy + 1, b.cz, chest);
            putChunk(chunk, mp, x0, z0, b.cx - 3, fy + 1, b.cz + 2, glowCoral);

        } else if (b.variant == 1) {
            // Вариация 1: Двухэтажный затопленный белокаменный дом-архив Элисара (13 x 11)
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    boolean wall = Math.abs(dx) == 6 || Math.abs(dz) == 5;
                    if (wall) {
                        for (int dy = 1; dy <= 8; ++dy) {
                            // Пролом в верхнем углу + арочные проёмы окон и дверей
                            if (dx > 2 && dz > 1 && dy >= 6) continue;
                            boolean door = dz == 5 && Math.abs(dx) <= 1 && dy <= 3;
                            boolean win = (dy == 3 || dy == 6) && (Math.abs(dx) == 3 || dz == 0);
                            if (door || win) continue;
                            BlockState wb = (dy == 4) ? lime : (ElisarChunkGenerator.hash(b.cx + dx, fy + dy, b.cz + dz) < 0.45 ? brickM : brick);
                            putChunk(chunk, mp, x0, z0, b.cx + dx, fy + dy, b.cz + dz, wb);
                        }
                    } else if (Math.abs(dx) <= 5 && Math.abs(dz) <= 4) {
                        // Межэтажное перекрытие с проломом посредине
                        if (Math.hypot(dx, dz) > 2.4) {
                            putChunk(chunk, mp, x0, z0, b.cx + dx, fy + 4, b.cz + dz, limeM);
                        }
                    }
                    // Двускатная синяя крыша с пробоиной
                    int roofH = 9 + (5 - Math.abs(dz)) / 2;
                    if (Math.abs(dx) <= 6 && !(dx > 1 && dz > 0) && roofH < ocean - 3) {
                        putChunk(chunk, mp, x0, z0, b.cx + dx, fy + roofH, b.cz + dz, roofBlue);
                    }
                }
            }
            putChunk(chunk, mp, x0, z0, b.cx, fy + 7, b.cz, seaLantern);
            putChunk(chunk, mp, x0, z0, b.cx - 4, fy + 1, b.cz - 3, seaLantern);
            putChunk(chunk, mp, x0, z0, b.cx + 4, fy + 1, b.cz - 3, chest);

        } else if (b.variant == 2) {
            // Вариация 2: Подводные монументальные триумфальные врата и мозаичный дворик
            for (int az = -5; az <= 5; az += 10) {
                for (int dx = -5; dx <= 5; ++dx) {
                    if (Math.abs(dx) >= 3) {
                        for (int dy = 1; dy <= 10; ++dy) {
                            putChunk(chunk, mp, x0, z0, b.cx + dx, fy + dy, b.cz + az, dy % 2 == 0 ? lime : brickM);
                        }
                    }
                    if (Math.abs(dx) <= 5) {
                        int archY = fy + 10 + (int) Math.round((1.0 - (dx * dx) / 30.0) * 2.5);
                        if (archY < ocean - 3) {
                            putChunk(chunk, mp, x0, z0, b.cx + dx, archY, b.cz + az, lime);
                            putChunk(chunk, mp, x0, z0, b.cx + dx, archY + 1, b.cz + az, roofBlue);
                        }
                    }
                }
            }
            putChunk(chunk, mp, x0, z0, b.cx - 4, fy + 2, b.cz, seaLantern);
            putChunk(chunk, mp, x0, z0, b.cx + 4, fy + 2, b.cz, seaLantern);
            putChunk(chunk, mp, x0, z0, b.cx, fy + 1, b.cz, chest);
            putChunk(chunk, mp, x0, z0, b.cx + 2, fy + 1, b.cz + 2, glowCoral);

        } else {
            // Вариация 3: Малая затопленная белокаменная улочка (3 небольших подводных дома вдоль мостовой)
            int[][] cottages = {{-9, -6, 6, 5}, {3, -6, 6, 5}, {-3, 4, 7, 5}};
            for (int ci = 0; ci < cottages.length; ++ci) {
                int x1 = b.cx + cottages[ci][0], z1 = b.cz + cottages[ci][1];
                int cw = cottages[ci][2], cd = cottages[ci][3];
                for (int dx = 0; dx <= cw; ++dx) {
                    for (int dz = 0; dz <= cd; ++dz) {
                        boolean wall = dx == 0 || dx == cw || dz == 0 || dz == cd;
                        if (wall) {
                            for (int dy = 1; dy <= 4; ++dy) {
                                if (dz == (ci < 2 ? cd : 0) && dx == cw / 2 && dy <= 2) continue; // дверь к улочке
                                if (dy == 4 && dx > cw - 2 && dz > cd - 2) continue; // скол стены
                                putChunk(chunk, mp, x0, z0, x1 + dx, fy + dy, z1 + dz,
                                        ElisarChunkGenerator.hash(x1 + dx, fy + dy, z1 + dz) < 0.5 ? brickM : lime);
                            }
                        }
                        if (dx >= 1 && dx <= cw - 1 && dz >= 1 && dz <= cd - 1 && !(dx == 1 && dz == 1)) {
                            putChunk(chunk, mp, x0, z0, x1 + dx, fy + 5, z1 + dz, roofBlue);
                        }
                    }
                }
                putChunk(chunk, mp, x0, z0, x1 + cw / 2, fy + 1, z1 + cd / 2, ci == 0 ? chest : seaLantern);
            }
            // Подводные фонарные столбы вдоль улочки
            for (int lx = -6; lx <= 6; lx += 6) {
                putChunk(chunk, mp, x0, z0, b.cx + lx, fy + 1, b.cz, keepM);
                putChunk(chunk, mp, x0, z0, b.cx + lx, fy + 2, b.cz, cageLantern);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 6. Постройка надводной деревни на сваях (StiltVillage: сваи до дна, мостки, лодки и 5 типов домов)
    // -------------------------------------------------------------------------
    private static void drivePiling(ChunkAccess chunk, BlockPos.MutableBlockPos mp, CityPlan plan,
                                    int x0, int z0, int px, int pz, int topY,
                                    BlockState logY, BlockState footing) {
        if (px < x0 || px > x0 + 15 || pz < z0 || pz > z0 + 15) return;
        int floorY = Math.max(4, ElisarChunkGenerator.oceanFloor(plan, px, pz) - 1);
        for (int y = topY; y >= floorY; --y) {
            BlockState cur = chunk.getBlockState(mp.set(px, y, pz));
            // Если ниже дна уже твёрдая скала (например подводная терраса садов), опираемся прямо на неё
            if (y < plan.ocean - 2 && cur.isSolidRender(chunk, mp) && y < topY - 4) {
                putChunk(chunk, mp, x0, z0, px, y + 1, pz, footing);
                break;
            }
            putChunk(chunk, mp, x0, z0, px, y, pz, y <= floorY + 2 ? footing : logY);
        }
    }

    private static void buildStiltVillage(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                          CityPlan plan, StiltVillage sv, int x0, int z0) {
        int ocean = plan.ocean;
        int dy0 = sv.deckY; // ocean + 3 (y = 67)
        BlockState logY = (sv.nearGardens ? ElisarBlocks.GARDEN_LOG.get() : ElisarBlocks.AMBRA_LOG.get()).defaultBlockState();
        BlockState sizoLogY = ElisarBlocks.SIZO_LOG.get().defaultBlockState();
        BlockState footing = ElisarBlocks.GARDEN_LIMESTONE_MOSSY.get().defaultBlockState();
        BlockState deck = ElisarBlocks.AMBRA_PLANKS.get().defaultBlockState();
        BlockState deckAlt = ElisarBlocks.SIZO_PLANKS.get().defaultBlockState();
        BlockState deckSlabTop = ElisarBlocks.AMBRA_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState deckSlabBot = ElisarBlocks.AMBRA_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState plaster = ElisarBlocks.PLASTER.get().defaultBlockState();
        BlockState timber = ElisarBlocks.TIMBER_CROSS.get().defaultBlockState();
        BlockState thatch = ElisarBlocks.THATCH.get().defaultBlockState();
        BlockState roofBlue = ElisarBlocks.ROOF_BLUE.get().defaultBlockState();
        BlockState roofRed = ElisarBlocks.ROOF_RED.get().defaultBlockState();
        BlockState gold = ru.aiforge.riftworld.registry.TempleBlocks.GOLD.get().defaultBlockState();
        BlockState fence = ElisarBlocks.FENCE.get().defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        BlockState lanternH = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        BlockState chest = Blocks.CHEST.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState campfire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        BlockState air = Blocks.AIR.defaultBlockState();

        // А) Центральная восьмигранная свайная площадь-пристань (R = 5.5) с беседкой-фонарём и лодочным пирсом
        for (int dx = -6; dx <= 6; ++dx) {
            for (int dz = -6; dz <= 6; ++dz) {
                double d = Math.hypot(dx, dz);
                if (d <= 5.6) {
                    putChunk(chunk, mp, x0, z0, sv.cx + dx, dy0, sv.cz + dz,
                            (Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? deckAlt : deck);
                    for (int ay = dy0 + 1; ay <= dy0 + 5; ++ay) {
                        putChunk(chunk, mp, x0, z0, sv.cx + dx, ay, sv.cz + dz, air);
                    }
                    // Перила по краю кроме выходов на мостики и спуска к воде
                    if (d >= 4.6 && Math.abs(dx) > 1 && Math.abs(dz) > 1) {
                        putChunk(chunk, mp, x0, z0, sv.cx + dx, dy0 + 1, sv.cz + dz, fence);
                    }
                }
            }
        }
        int[][] hubPilings = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {0, -5}, {0, 5}, {-5, 0}, {5, 0}, {0, 0}};
        for (int[] hp : hubPilings) {
            drivePiling(chunk, mp, plan, x0, z0, sv.cx + hp[0], sv.cz + hp[1], dy0, logY, footing);
        }
        // Центральная крытая беседка-маячок на площади
        for (int sx : new int[]{-2, 2}) {
            for (int sz : new int[]{-2, 2}) {
                for (int y = dy0 + 1; y <= dy0 + 4; ++y) {
                    putChunk(chunk, mp, x0, z0, sv.cx + sx, y, sv.cz + sz, sizoLogY);
                }
            }
        }
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                int distM = Math.max(Math.abs(dx), Math.abs(dz));
                if (distM <= 3) putChunk(chunk, mp, x0, z0, sv.cx + dx, dy0 + 5 + (3 - distM) / 2, sv.cz + dz, roofBlue);
            }
        }
        putChunk(chunk, mp, x0, z0, sv.cx, dy0 + 6, sv.cz, gold);
        putChunk(chunk, mp, x0, z0, sv.cx, dy0 + 4, sv.cz, lanternH);
        putChunk(chunk, mp, x0, z0, sv.cx + 2, dy0 + 1, sv.cz, barrel);
        putChunk(chunk, mp, x0, z0, sv.cx - 2, dy0 + 1, sv.cz + 1, chest);

        // Низкий плавучий причал у южного края площади (ocean + 1) + деревянная рыбацкая лодка на воде!
        for (int dz = 6; dz <= 10; ++dz) {
            int wy = dz == 6 ? dy0 - 1 : (dz == 7 ? ocean + 1 : ocean + 1);
            for (int dx = -1; dx <= 1; ++dx) {
                putChunk(chunk, mp, x0, z0, sv.cx + dx, wy, sv.cz + dz, deck);
            }
            if (dz == 10) {
                drivePiling(chunk, mp, plan, x0, z0, sv.cx - 1, sv.cz + dz, ocean + 2, logY, footing);
                drivePiling(chunk, mp, plan, x0, z0, sv.cx + 1, sv.cz + dz, ocean + 2, logY, footing);
                putChunk(chunk, mp, x0, z0, sv.cx + 1, ocean + 3, sv.cz + dz, lantern);
            }
        }
        buildMooredSkiff(chunk, mp, x0, z0, sv.cx + 4, ocean + 1, sv.cz + 9, deckAlt, logY, lantern);

        // Б) Поднятые деревянные мостики (ширина 3, с перилами и сваями каждые 5 блоков) от площади к каждому дому
        for (StiltHut h : sv.huts) {
            double vx = h.cx - sv.cx, vz = h.cz - sv.cz;
            double len = Math.max(1.0, Math.hypot(vx, vz));
            int steps = (int) Math.ceil(len);
            for (int s = 4; s <= steps - 3; ++s) {
                double u = s / len;
                int bx = (int) Math.round(sv.cx + vx * u);
                int bz = (int) Math.round(sv.cz + vz * u);
                int by = u > 0.72 ? h.deckY : dy0;
                double nx = -vz / len, nz = vx / len;
                for (int w = -1; w <= 1; ++w) {
                    int wx = (int) Math.round(bx + nx * w);
                    int wz = (int) Math.round(bz + nz * w);
                    putChunk(chunk, mp, x0, z0, wx, by, wz, s % 4 == 0 ? deckAlt : deck);
                    putChunk(chunk, mp, x0, z0, wx, by + 1, wz, air);
                    putChunk(chunk, mp, x0, z0, wx, by + 2, wz, air);
                }
                int rx1 = (int) Math.round(bx + nx * 1.65), rz1 = (int) Math.round(bz + nz * 1.65);
                int rx2 = (int) Math.round(bx - nx * 1.65), rz2 = (int) Math.round(bz - nz * 1.65);
                if (s % 5 == 0) {
                    drivePiling(chunk, mp, plan, x0, z0, rx1, rz1, by + 1, logY, footing);
                    drivePiling(chunk, mp, plan, x0, z0, rx2, rz2, by + 1, logY, footing);
                    if (s == 10) putChunk(chunk, mp, x0, z0, rx1, by + 2, rz1, lantern);
                } else {
                    putChunk(chunk, mp, x0, z0, rx1, by, rz1, deckSlabTop);
                    putChunk(chunk, mp, x0, z0, rx2, by, rz2, deckSlabTop);
                    putChunk(chunk, mp, x0, z0, rx1, by + 1, rz1, fence);
                    putChunk(chunk, mp, x0, z0, rx2, by + 1, rz2, fence);
                }
            }
        }

        // В) Постройка каждого из 5 уникальных свайных домов
        for (StiltHut h : sv.huts) {
            buildSingleStiltHut(chunk, mp, plan, x0, z0, h, ocean, logY, sizoLogY, footing,
                    deck, deckAlt, deckSlabBot, plaster, timber, thatch, roofBlue, roofRed,
                    gold, fence, glass, lantern, lanternH, seaLantern, barrel, chest, chain, campfire, air);
        }
    }

    private static void buildMooredSkiff(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                         int x0, int z0, int bx, int wy, int bz,
                                         BlockState hull, BlockState gunwale, BlockState lantern) {
        // Изящная 7-блочная деревянная лодка-пирога на воде (wy = ocean + 1)
        for (int dz = -3; dz <= 3; ++dz) {
            int hw = Math.abs(dz) >= 3 ? 0 : 1;
            for (int dx = -hw; dx <= hw; ++dx) {
                putChunk(chunk, mp, x0, z0, bx + dx, wy, bz + dz, hull);
                if (Math.abs(dx) == hw && hw > 0 && dz % 2 == 0) {
                    putChunk(chunk, mp, x0, z0, bx + dx, wy + 1, bz + dz, gunwale);
                }
            }
        }
        putChunk(chunk, mp, x0, z0, bx, wy + 1, bz - 3, lantern);
    }

    private static void buildSingleStiltHut(ChunkAccess chunk, BlockPos.MutableBlockPos mp, CityPlan plan,
                                            int x0, int z0, StiltHut h, int ocean,
                                            BlockState logY, BlockState sizoLogY, BlockState footing,
                                            BlockState deck, BlockState deckAlt, BlockState deckSlabBot,
                                            BlockState plaster, BlockState timber, BlockState thatch,
                                            BlockState roofBlue, BlockState roofRed, BlockState gold,
                                            BlockState fence, BlockState glass, BlockState lantern,
                                            BlockState lanternH, BlockState seaLantern, BlockState barrel,
                                            BlockState chest, BlockState chain, BlockState campfire, BlockState air) {
        int hy = h.deckY;
        int hw = h.hw, hd = h.hd;

        // 1) Свайное основание по углам и центрам сторон каждого дома (строго до самого морского дна!)
        int[] xs = {-hw, 0, hw};
        int[] zs = {-hd, 0, hd};
        for (int sx : xs) {
            for (int sz : zs) {
                if (h.kind == 2 && sx == 0 && sz == 0) continue; // у эллинга в центре — водный канал под лодку!
                drivePiling(chunk, mp, plan, x0, z0, h.cx + sx, h.cz + sz, hy, logY, footing);
            }
        }

        // 2) Широкая деревянная терраса-палуба вокруг дома (выступ +1 блок с перилами)
        for (int dx = -hw - 1; dx <= hw + 1; ++dx) {
            for (int dz = -hd - 1; dz <= hd + 1; ++dz) {
                // В эллинге (kind == 2) оставляем открытый водный слип по центру с внешней стороны!
                boolean slipway = (h.kind == 2) && Math.abs(dx) <= 1 && Math.abs(dz) <= hd;
                if (slipway) continue;
                putChunk(chunk, mp, x0, z0, h.cx + dx, hy, h.cz + dz,
                        (Math.abs(dx) == hw + 1 || Math.abs(dz) == hd + 1) ? deckAlt : deck);
                for (int ay = hy + 1; ay <= hy + 8; ++ay) {
                    putChunk(chunk, mp, x0, z0, h.cx + dx, ay, h.cz + dz, air);
                }
                boolean outerEdge = Math.abs(dx) == hw + 1 || Math.abs(dz) == hd + 1;
                boolean doorGap = (h.facing == Direction.SOUTH && dz == hd + 1 && Math.abs(dx) <= 1)
                        || (h.facing == Direction.NORTH && dz == -hd - 1 && Math.abs(dx) <= 1)
                        || (h.facing == Direction.EAST && dx == hw + 1 && Math.abs(dz) <= 1)
                        || (h.facing == Direction.WEST && dx == -hw - 1 && Math.abs(dz) <= 1);
                if (outerEdge && !doorGap) {
                    putChunk(chunk, mp, x0, z0, h.cx + dx, hy + 1, h.cz + dz, fence);
                }
            }
        }

        if (h.kind == 3) {
            // KIND 3: Открытая лагунная торговая лавка под полосатым красно-соломенным навесом!
            for (int sx : new int[]{-hw, hw}) {
                for (int sz : new int[]{-hd, hd}) {
                    for (int y = hy + 1; y <= hy + 4; ++y) {
                        putChunk(chunk, mp, x0, z0, h.cx + sx, y, h.cz + sz, sizoLogY);
                    }
                }
            }
            for (int dx = -hw - 1; dx <= hw + 1; ++dx) {
                for (int dz = -hd - 1; dz <= hd + 1; ++dz) {
                    int m = Math.max(Math.abs(dx), Math.abs(dz));
                    int ry = hy + 5 + Math.max(0, (hw + 1 - m) / 2);
                    putChunk(chunk, mp, x0, z0, h.cx + dx, ry, h.cz + dz, (dx + dz) % 2 == 0 ? roofRed : thatch);
                }
            }
            for (int dx = -2; dx <= 2; ++dx) {
                putChunk(chunk, mp, x0, z0, h.cx + dx, hy + 1, h.cz, deckAlt);
            }
            putChunk(chunk, mp, x0, z0, h.cx - 2, hy + 1, h.cz - 2, barrel);
            putChunk(chunk, mp, x0, z0, h.cx - 2, hy + 2, h.cz - 2, barrel);
            putChunk(chunk, mp, x0, z0, h.cx + 2, hy + 1, h.cz - 2, chest);
            putChunk(chunk, mp, x0, z0, h.cx, hy + 4, h.cz, lanternH);
            return;
        }

        if (h.kind == 4) {
            // KIND 4: Высокая дозорно-маячная башня лагуны на сваях (высота 15 блоков над палубой + синий шпиль!)
            for (int y = hy + 1; y <= hy + 11; ++y) {
                for (int dx = -hw + 1; dx <= hw - 1; ++dx) {
                    for (int dz = -hd + 1; dz <= hd - 1; ++dz) {
                        boolean corner = Math.abs(dx) == hw - 1 && Math.abs(dz) == hd - 1;
                        boolean wall = Math.abs(dx) == hw - 1 || Math.abs(dz) == hd - 1;
                        if (corner) {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, y, h.cz + dz, sizoLogY);
                        } else if (wall && y <= hy + 8) {
                            if (y == hy + 5 || (y <= hy + 2 && (dx == 0 || dz == 0))) {
                                putChunk(chunk, mp, x0, z0, h.cx + dx, y, h.cz + dz, y <= hy + 2 ? air : glass);
                            } else {
                                putChunk(chunk, mp, x0, z0, h.cx + dx, y, h.cz + dz, y % 3 == 0 ? timber : plaster);
                            }
                        } else if (y == hy + 9) {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, y, h.cz + dz, deck);
                        }
                    }
                }
            }
            putChunk(chunk, mp, x0, z0, h.cx, hy + 10, h.cz, seaLantern);
            putChunk(chunk, mp, x0, z0, h.cx, hy + 11, h.cz, campfire);
            for (int sy = 0; sy <= 4; ++sy) {
                int r = Math.max(0, hw - sy);
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        putChunk(chunk, mp, x0, z0, h.cx + dx, hy + 12 + sy, h.cz + dz, sy == 4 ? gold : roofBlue);
                    }
                }
            }
            return;
        }

        // KIND 0 (рыбацкий дом), KIND 1 (двухэтажный свайный терем), KIND 2 (крытый эллинг с лодкой под сводом)
        int wallH = (h.kind == 1) ? 8 : 4;
        BlockState roofMat = (h.kind == 1) ? roofBlue : (h.kind == 2 ? roofRed : thatch);
        boolean ridgeAlongX = hw >= hd;

        for (int dx = -hw; dx <= hw; ++dx) {
            for (int dz = -hd; dz <= hd; ++dz) {
                boolean corner = Math.abs(dx) == hw && Math.abs(dz) == hd;
                boolean wall = Math.abs(dx) == hw || Math.abs(dz) == hd;
                for (int dy = 1; dy <= wallH; ++dy) {
                    if (corner) {
                        putChunk(chunk, mp, x0, z0, h.cx + dx, hy + dy, h.cz + dz, sizoLogY);
                    } else if (wall) {
                        boolean isDoor = dy <= 2 && ((h.facing == Direction.SOUTH && dz == hd && dx == 0)
                                || (h.facing == Direction.NORTH && dz == -hd && dx == 0)
                                || (h.facing == Direction.EAST && dx == hw && dz == 0)
                                || (h.facing == Direction.WEST && dx == -hw && dz == 0));
                        // В эллинге (kind == 2) — широкие лодочные ворота к воде
                        boolean boatArch = (h.kind == 2) && dy <= 2 && Math.abs(dx) <= 1 && Math.abs(dz) == hd;
                        boolean isWin = (dy == 2 || dy == 6) && ((Math.abs(dx) == hw && dz == 0) || (Math.abs(dz) == hd && dx == 0)) && !isDoor;
                        if (isDoor || boatArch) {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, hy + dy, h.cz + dz, air);
                        } else if (isWin) {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, hy + dy, h.cz + dz, glass);
                        } else {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, hy + dy, h.cz + dz,
                                    (dy == 4 || dy == wallH) ? timber : plaster);
                        }
                    } else if (h.kind == 1 && dy == 4) {
                        putChunk(chunk, mp, x0, z0, h.cx + dx, hy + dy, h.cz + dz, deck);
                    }
                }
            }
        }

        // Высокая азиатско-островная крыша с изогнутым свесом и золотыми коньками у терема
        int span = ridgeAlongX ? hd + 1 : hw + 1;
        for (int k = 0; k <= span; ++k) {
            int ry = hy + wallH + 1 + k;
            if (ridgeAlongX) {
                int zL = -hd - 1 + k, zR = hd + 1 - k;
                if (zL > zR) break;
                for (int dx = -hw - 1; dx <= hw + 1; ++dx) {
                    BlockState rb = (zL == zR && h.kind == 1 && Math.abs(dx) == hw + 1) ? gold : roofMat;
                    putChunk(chunk, mp, x0, z0, h.cx + dx, ry, h.cz + zL, rb);
                    putChunk(chunk, mp, x0, z0, h.cx + dx, ry, h.cz + zR, rb);
                    if (Math.abs(dx) == hw && zL + 1 <= zR - 1) {
                        for (int gz = zL + 1; gz <= zR - 1; ++gz) {
                            putChunk(chunk, mp, x0, z0, h.cx + dx, ry, h.cz + gz, plaster);
                        }
                    }
                }
            } else {
                int xL = -hw - 1 + k, xR = hw + 1 - k;
                if (xL > xR) break;
                for (int dz = -hd - 1; dz <= hd + 1; ++dz) {
                    BlockState rb = (xL == xR && h.kind == 1 && Math.abs(dz) == hd + 1) ? gold : roofMat;
                    putChunk(chunk, mp, x0, z0, h.cx + xL, ry, h.cz + dz, rb);
                    putChunk(chunk, mp, x0, z0, h.cx + xR, ry, h.cz + dz, rb);
                    if (Math.abs(dz) == hd && xL + 1 <= xR - 1) {
                        for (int gx = xL + 1; gx <= xR - 1; ++gx) {
                            putChunk(chunk, mp, x0, z0, h.cx + gx, ry, h.cz + dz, plaster);
                        }
                    }
                }
            }
        }

        // Обстановка внутри и детали снаружи
        putChunk(chunk, mp, x0, z0, h.cx, hy + wallH, h.cz, lanternH);
        if (h.kind == 2) {
            // Внутри водного канала эллинга стоит лодка прямо на воде (ocean + 1), а сверху висит цепь-лебёдка!
            buildMooredSkiff(chunk, mp, x0, z0, h.cx, ocean + 1, h.cz, deckAlt, logY, lantern);
            for (int cy = ocean + 3; cy <= hy + 3; ++cy) {
                putChunk(chunk, mp, x0, z0, h.cx, cy, h.cz, cy == ocean + 3 ? lanternH : chain);
            }
            putChunk(chunk, mp, x0, z0, h.cx - hw + 1, hy + 1, h.cz, barrel);
            putChunk(chunk, mp, x0, z0, h.cx + hw - 1, hy + 1, h.cz, chest);
        } else {
            putChunk(chunk, mp, x0, z0, h.cx - hw + 1, hy + 1, h.cz - hd + 1, chest);
            putChunk(chunk, mp, x0, z0, h.cx + hw - 1, hy + 1, h.cz - hd + 1, barrel);
            if (h.kind == 0) {
                // Сушильня рыболовных сетей и фонарь на боковой террасе рыбацкого дома
                putChunk(chunk, mp, x0, z0, h.cx + hw + 1, hy + 2, h.cz, fence);
                putChunk(chunk, mp, x0, z0, h.cx + hw + 1, hy + 3, h.cz, lanternH);
            }
        }
    }

    private static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double len2 = vx * vx + vz * vz;
        if (len2 < 1e-6) return Math.hypot(px - ax, pz - az);
        double u = Math.max(0.0, Math.min(1.0, ((px - ax) * vx + (pz - az) * vz) / len2));
        return Math.hypot(px - (ax + u * vx), pz - (az + u * vz));
    }
}
