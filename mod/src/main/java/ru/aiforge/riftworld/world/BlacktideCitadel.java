package ru.aiforge.riftworld.world;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * v0.29.0: Огромная океаническая крепость «Цитадель Чёрного утёса» ({@code data/riftworld/structures/blacktide.nbt},
 * {@code 248x176x228}).
 * <ul>
 *   <li>Спавнится <b>ТОЛЬКО В ОТКРЫТОМ ОКЕАНЕ</b> рандомно по всему морю Элисара (не привязана к Пепельному биому!);</li>
 *   <li>Арочный каменный мост выходит <b>прямо из-под воды / из морского дна</b> ({@code lz = 88..134}), без сухопутного острова;</li>
 *   <li>Снаружи замок <b>несимметричный и 2-уровневый на скалистом острове</b> (как на оригинальном референсе {@code 02}):
 *       нижний прибрежный бастион с морскими воротами, пирсом и портовыми домами ({@code Level 1}) + верхняя цитадель
 *       со смещёнными двухбашенными воротами ({@code lx = -16}) и высокой восточной колокольней с аркбутаном ({@code Level 2}),
 *       при этом внутри <b>Главный тронный зал Собора и купольная Ротонда строго симметричны</b>!</li>
 * </ul>
 */
public final class BlacktideCitadel {
    private static final Logger LOG = LoggerFactory.getLogger("RiftWorld/Blacktide");
    private static final String RESOURCE_PATH = "/data/riftworld/structures/blacktide.nbt";

    /** Размер супер-ячейки рандомной генерации Цитадели в открытом океане. */
    public static final int SUPER = 1320;
    private static final long SEED = 0x6B1AC71DE0CEA429L;

    public static final int W = 248;
    public static final int H = 176;
    public static final int D = 228;
    public static final int OX = -124;
    public static final int OZ = -104;
    public static final int SEA_BY = 24;

    // Ключевые локальные ориентиры v0.29.0 (смещённый мост и ворота lx = -16, симметричный Главный зал lx = 0)
    public static final int BRIDGE_SUB_LX = -16, BRIDGE_SUB_LZ = 112;
    public static final int BRIDGE_SURF_LX = -16, BRIDGE_SURF_LZ = 88;
    public static final int GATE_LX = -16, GATE_LZ = 36, GATE_BY = 48;
    public static final int KEEP_LX = 0, KEEP_LZ = -20, KEEP_BY = 57;

    /** Центры всех 28 башен (6 нижнего яруса + 15 верхнего периметра + 6 Собора + 1 Восточная колокольня) с винтовыми лестницами. */
    public static final int[][] TOWER_CENTERS = {
            { 78,  45}, { 95,  13}, { 95,  -1}, { 94, -38}, { 53,  46}, {-82,  -6},
            {-29,  36}, { -3,  36}, {-48,  35}, { 18,  35}, { 41,  35}, { 62,  35},
            {-48,   4}, {-48, -34}, {-48, -78}, { 62,   8}, { 62, -22}, { 62, -52},
            { 62, -78}, {-22, -78}, { 30, -78},
            { 26,   4}, {-26,   4}, { 26, -40}, {-26, -40}, { 16, -20}, {-16, -20},
            { 45, -18}
    };

    private static final ConcurrentHashMap<Long, Optional<Site>> CACHE = new ConcurrentHashMap<>();

    /** Экземпляр Цитадели Чёрного утёса в открытом океане Элисара. */
    public static final class Site {
        public final int cx, cz;             // мировая точка локального (lx=0, lz=0)
        public final int baseY;              // мировой Y для by=0 (ocean - 24 = 38)
        public final int rot;                // 0=NONE, 1=CW_90, 2=CW_180, 3=CCW_90
        public final int gateX, gateZ;       // мировая точка смещённых главных ворот (lx=-16, lz=36)
        public final int keepX, keepZ;       // мировая точка симметричного Главного зала Собора (lx=0, lz=-20)
        public final int bridgeSurfX, bridgeSurfZ; // точка выхода каменного моста из-под воды на поверхность моря (lz=88)
        public final int bridgeSubX, bridgeSubZ;   // подводная часть моста, уходящая в морское дно (lz=112)
        public final int minX, maxX, minZ, maxZ;

        public Site(int cx, int cz, int baseY, int rot) {
            this.cx = cx;
            this.cz = cz;
            this.baseY = baseY;
            this.rot = rot & 3;
            this.gateX = cx + rotX(GATE_LX, GATE_LZ, this.rot);
            this.gateZ = cz + rotZ(GATE_LX, GATE_LZ, this.rot);
            this.keepX = cx + rotX(KEEP_LX, KEEP_LZ, this.rot);
            this.keepZ = cz + rotZ(KEEP_LX, KEEP_LZ, this.rot);
            this.bridgeSurfX = cx + rotX(BRIDGE_SURF_LX, BRIDGE_SURF_LZ, this.rot);
            this.bridgeSurfZ = cz + rotZ(BRIDGE_SURF_LX, BRIDGE_SURF_LZ, this.rot);
            this.bridgeSubX = cx + rotX(BRIDGE_SUB_LX, BRIDGE_SUB_LZ, this.rot);
            this.bridgeSubZ = cz + rotZ(BRIDGE_SUB_LX, BRIDGE_SUB_LZ, this.rot);

            int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
            int z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
            int[] cLx = {OX, OX + W - 1};
            int[] cLz = {OZ, OZ + D + 14}; // включает подводный уход моста в морское дно (lz <= 136)
            for (int lx : cLx) {
                for (int lz : cLz) {
                    int wx = cx + rotX(lx, lz, this.rot);
                    int wz = cz + rotZ(lx, lz, this.rot);
                    if (wx < x0) x0 = wx;
                    if (wx > x1) x1 = wx;
                    if (wz < z0) z0 = wz;
                    if (wz > z1) z1 = wz;
                }
            }
            this.minX = x0;
            this.maxX = x1;
            this.minZ = z0;
            this.maxZ = z1;
        }

        public boolean intersectsChunk(int x0, int z0, int pad) {
            return x0 + 15 >= minX - pad && x0 <= maxX + pad
                    && z0 + 15 >= minZ - pad && z0 <= maxZ + pad;
        }

        public boolean inFootprint(double x, double z, double margin) {
            return x >= minX - margin && x <= maxX + margin
                    && z >= minZ - margin && z <= maxZ + margin;
        }
    }

    // =========================================================================
    // РАНДОМНАЯ ГЕНЕРАЦИЯ В ОТКРЫТОМ ОКЕАНЕ (НЕ ПРИВЯЗАНА К ПЕПЕЛЬНОМУ БИОМУ!)
    // =========================================================================

    @Nullable
    public static Site siteInCell(int sx, int sz) {
        long k = WildIslands.key(sx, sz);
        Optional<Site> cached = CACHE.get(k);
        if (cached != null) {
            return cached.orElse(null);
        }
        Optional<Site> built = Optional.ofNullable(gen(sx, sz));
        CACHE.putIfAbsent(k, built);
        return built.orElse(null);
    }

    @Nullable
    private static Site gen(int sx, int sz) {
        if (sx == 0 && sz == 0) {
            return null; // Центральная акватория Белого Города
        }
        long cellSeed = SEED ^ (sx * 0x9E3779B97F4A7C15L + sz * 0xBF58476D1CE4E5B9L);
        Random r = new Random(cellSeed);

        // Гарантируем эталонные морские экземпляры в открытых секторах океана (вдали от Пепельного биома!) + рандом по всему морю
        boolean pinned = (sx == 1 && sz == -1) || (sx == -1 && sz == -1) || (sx == 1 && sz == 1);
        if (!pinned && r.nextDouble() > 0.54) {
            return null;
        }

        int ocean = CityPlan.get().ocean;
        int margin = 210;
        for (int attempt = 0; attempt < 28; ++attempt) {
            int cx = sx * SUPER + margin + r.nextInt(SUPER - 2 * margin);
            int cz = sz * SUPER + margin + r.nextInt(SUPER - 2 * margin);
            int rot = r.nextInt(4);

            if (Math.hypot(cx, cz) < 840.0) continue;                     // вдали от Белого Города и внешнего кольца
            if (Math.hypot(cx - 648.0, cz - 693.0) < 420.0) continue;     // вдали от морских проб spots.json

            if (!isPureOpenOceanSite(cx, cz, rot)) continue;
            return new Site(cx, cz, ocean - SEA_BY, rot);
        }
        return null;
    }

    /**
     * Проверяет, что весь остров Цитадели И весь подводный мост стоят СТРОГО В ЧИСТОМ ОТКРЫТОМ ОКЕАНЕ
     * вдали от любых сухопутных/архипелаговых биомов, пристаней и структур (включая Пепельный биом!).
     */
    private static boolean isPureOpenOceanSite(int cx, int cz, int rot) {
        int[][] checkOffsets = {
                {0, 0}, {0, -95}, {-16, 95}, {-16, 134},
                {105, 0}, {-95, 0}, {95, 55}, {95, -55}, {-65, 55}, {-95, -55}
        };
        for (int[] off : checkOffsets) {
            double wx = cx + rotX(off[0], off[1], rot);
            double wz = cz + rotZ(off[0], off[1], rot);

            if (AshenLands.blocks(wx, wz, 110.0) || AshenLands.weight(wx, wz) > 0.0) return false;
            EmberRuins.Site es = EmberRuins.siteAt(wx, wz);
            if (es != null && Math.hypot(wx - es.cx, wz - es.cz) < es.island.maxReach() + 120.0) return false;
            if (VioletHills.nearAny(wx, wz, 40.0, 110.0)) return false;
            if (CrystalRifts.nearAny(wx, wz, 40.0, 110.0)) return false;
            SizoGrove.Region sr = SizoGrove.regionAt(wx, wz);
            if (sr != null && Math.hypot(wx - sr.hub.cx, wz - sr.hub.cz) < sr.hub.maxReach() + 120.0) return false;
            if (WaterfallGardens.nearAny(wx, wz, 40.0, 125.0)) return false;
            MistKeep.Region mk = MistKeep.regionAt(wx, wz);
            if (mk != null && Math.hypot(wx - mk.cx, wz - mk.cz) < mk.Rr + 120.0) return false;
            if (WorldStructures.anyVillageAt(wx, wz, 145.0) != null) return false;
            if (LandmarkStructures.theaterAt(wx, wz, 145.0) != null) return false;
            if (LandmarkStructures.harborNear(wx, wz, 175.0) != null) return false;
        }
        return true;
    }

    /** Возвращает экземпляр Цитадели Чёрного утёса, пересекающий чанк {@code (x0, z0)}, либо {@code null}. */
    @Nullable
    public static Site siteForChunk(int x0, int z0) {
        int sx0 = Math.floorDiv(x0 + 8, SUPER);
        int sz0 = Math.floorDiv(z0 + 8, SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Site s = siteInCell(sx0 + dx, sz0 + dz);
                if (s != null && s.intersectsChunk(x0, z0, 24)) {
                    return s;
                }
            }
        }
        return null;
    }

    /** Проверяет, попадает ли точка {@code (x, z)} внутрь акватории любой морской Цитадели Чёрного утёса. */
    public static boolean insideAnyFootprint(double x, double z, double margin) {
        int sx0 = Math.floorDiv((int) Math.floor(x), SUPER);
        int sz0 = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Site s = siteInCell(sx0 + dx, sz0 + dz);
                if (s != null && s.inFootprint(x, z, margin)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Находит ближайшую океаническую Цитадель Чёрного утёса в радиусе {@code maxCellRadius} супер-ячеек. */
    @Nullable
    public static Site nearest(double x, double z, int maxCellRadius) {
        int sx0 = Math.floorDiv((int) Math.floor(x), SUPER);
        int sz0 = Math.floorDiv((int) Math.floor(z), SUPER);
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (int rad = 0; rad <= maxCellRadius; ++rad) {
            for (int dx = -rad; dx <= rad; ++dx) {
                for (int dz = -rad; dz <= rad; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
                    Site s = siteInCell(sx0 + dx, sz0 + dz);
                    if (s == null) continue;
                    double ddx = s.gateX - x;
                    double ddz = s.gateZ - z;
                    double d2 = ddx * ddx + ddz * ddz;
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = s;
                    }
                }
            }
            if (best != null && rad >= 1) break;
        }
        return best;
    }

    public static int rotX(int lx, int lz, int rot) {
        return switch (rot & 3) {
            case 1 -> -lz;
            case 2 -> -lx;
            case 3 -> lz;
            default -> lx;
        };
    }

    public static int rotZ(int lx, int lz, int rot) {
        return switch (rot & 3) {
            case 1 -> lx;
            case 2 -> -lz;
            case 3 -> -lx;
            default -> lz;
        };
    }

    public static int invLx(int dx, int dz, int rot) {
        return switch (rot & 3) {
            case 1 -> dz;
            case 2 -> -dx;
            case 3 -> -dz;
            default -> dx;
        };
    }

    public static int invLz(int dx, int dz, int rot) {
        return switch (rot & 3) {
            case 1 -> -dx;
            case 2 -> -dz;
            case 3 -> dx;
            default -> dz;
        };
    }

    // =========================================================================
    // СТРИМИНГОВЫЙ ЗАГРУЗЧИК СЖАТОГО NBT В КОМПАКТНЫЙ ВОКСЕЛЬНЫЙ МАССИВ
    // =========================================================================

    private static final class LoadedTemplate {
        final byte[] voxels;
        final BlockState[][] states;
        final int grassId;
        final int dirtId;

        LoadedTemplate(byte[] voxels, BlockState[][] states, int grassId, int dirtId) {
            this.voxels = voxels;
            this.states = states;
            this.grassId = grassId;
            this.dirtId = dirtId;
        }
    }

    private static volatile LoadedTemplate LOADED;

    private static LoadedTemplate template() {
        LoadedTemplate cur = LOADED;
        if (cur != null) return cur;
        synchronized (BlacktideCitadel.class) {
            if (LOADED == null) {
                LOADED = loadFromResource();
            }
            return LOADED;
        }
    }

    private static LoadedTemplate loadFromResource() {
        long t0 = System.currentTimeMillis();
        byte[] vox = new byte[W * H * D];
        BlockState[][] rotStates = new BlockState[1][4];
        int grassIdx = -1, dirtIdx = -1;

        try (InputStream rawIn = BlacktideCitadel.class.getResourceAsStream(RESOURCE_PATH)) {
            if (rawIn == null) {
                LOG.error("Missing structure resource {}", RESOURCE_PATH);
                return new LoadedTemplate(vox, rotStates, -1, -1);
            }
            try (DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(new GZIPInputStream(rawIn, 65536), 65536))) {
                int rootType = dis.readUnsignedByte();
                if (rootType != 10) throw new IOException("Expected root TAG_Compound, got " + rootType);
                dis.readUTF();

                Rotation[] rots = {
                        Rotation.NONE,
                        Rotation.CLOCKWISE_90,
                        Rotation.CLOCKWISE_180,
                        Rotation.COUNTERCLOCKWISE_90
                };

                while (true) {
                    int tagType = dis.readUnsignedByte();
                    if (tagType == 0) break;
                    String name = dis.readUTF();
                    if ("palette".equals(name) && tagType == 9) {
                        int elemType = dis.readUnsignedByte();
                        int count = dis.readInt();
                        if (elemType != 10) {
                            for (int i = 0; i < count; ++i) skipPayload(dis, elemType);
                            continue;
                        }
                        rotStates = new BlockState[count + 1][4];
                        for (int i = 0; i < count; ++i) {
                            CompoundTag entry = new CompoundTag();
                            String blockName = "minecraft:stone";
                            CompoundTag props = null;
                            while (true) {
                                int ft = dis.readUnsignedByte();
                                if (ft == 0) break;
                                String fn = dis.readUTF();
                                if ("Name".equals(fn) && ft == 8) {
                                    blockName = dis.readUTF();
                                } else if ("Properties".equals(fn) && ft == 10) {
                                    props = new CompoundTag();
                                    while (true) {
                                        int pt = dis.readUnsignedByte();
                                        if (pt == 0) break;
                                        String pk = dis.readUTF();
                                        if (pt == 8) props.putString(pk, dis.readUTF());
                                        else skipPayload(dis, pt);
                                    }
                                } else {
                                    skipPayload(dis, ft);
                                }
                            }
                            entry.putString("Name", blockName);
                            if (props != null) entry.put("Properties", props);
                            BlockState base = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry);
                            if ("minecraft:grass_block".equals(blockName)) grassIdx = i + 1;
                            if ("minecraft:dirt".equals(blockName)) dirtIdx = i + 1;
                            for (int r = 0; r < 4; ++r) {
                                rotStates[i + 1][r] = base.rotate(rots[r]);
                            }
                        }
                    } else if ("blocks".equals(name) && tagType == 9) {
                        int elemType = dis.readUnsignedByte();
                        int count = dis.readInt();
                        if (elemType != 10) {
                            for (int i = 0; i < count; ++i) skipPayload(dis, elemType);
                            continue;
                        }
                        for (int i = 0; i < count; ++i) {
                            int bx = 0, by = 0, bz = 0, bst = 0;
                            while (true) {
                                int ft = dis.readUnsignedByte();
                                if (ft == 0) break;
                                String fn = dis.readUTF();
                                if ("pos".equals(fn) && ft == 9) {
                                    int lt = dis.readUnsignedByte();
                                    int ln = dis.readInt();
                                    if (lt == 3 && ln == 3) {
                                        bx = dis.readInt();
                                        by = dis.readInt();
                                        bz = dis.readInt();
                                    } else {
                                        for (int k = 0; k < ln; ++k) skipPayload(dis, lt);
                                    }
                                } else if ("state".equals(fn) && ft == 3) {
                                    bst = dis.readInt();
                                } else {
                                    skipPayload(dis, ft);
                                }
                            }
                            if (bx >= 0 && bx < W && by >= 0 && by < H && bz >= 0 && bz < D) {
                                vox[(by * D + bz) * W + bx] = (byte) (bst + 1);
                            }
                        }
                    } else {
                        skipPayload(dis, tagType);
                    }
                }
            }
            LOG.info("Loaded blacktide.nbt ({}x{}x{}) in {} ms", W, H, D, System.currentTimeMillis() - t0);
        } catch (Exception ex) {
            LOG.error("Failed to stream blacktide.nbt", ex);
        }
        return new LoadedTemplate(vox, rotStates, grassIdx, dirtIdx);
    }

    private static void skipPayload(DataInputStream dis, int tagType) throws IOException {
        switch (tagType) {
            case 1 -> skipFully(dis, 1);
            case 2 -> skipFully(dis, 2);
            case 3, 5 -> skipFully(dis, 4);
            case 4, 6 -> skipFully(dis, 8);
            case 7 -> skipFully(dis, dis.readInt());
            case 8 -> dis.readUTF();
            case 9 -> {
                int et = dis.readUnsignedByte();
                int n = dis.readInt();
                for (int i = 0; i < n; ++i) skipPayload(dis, et);
            }
            case 10 -> {
                while (true) {
                    int tt = dis.readUnsignedByte();
                    if (tt == 0) break;
                    dis.readUTF();
                    skipPayload(dis, tt);
                }
            }
            case 11 -> skipFully(dis, (long) dis.readInt() * 4L);
            case 12 -> skipFully(dis, (long) dis.readInt() * 8L);
            default -> throw new IOException("Unknown NBT tag type: " + tagType);
        }
    }

    private static void skipFully(DataInputStream dis, long bytes) throws IOException {
        long rem = bytes;
        while (rem > 0) {
            long sk = dis.skip(rem);
            if (sk <= 0) {
                if (dis.read() < 0) throw new IOException("Unexpected EOF in blacktide.nbt");
                rem--;
            } else {
                rem -= sk;
            }
        }
    }

    // =========================================================================
    // ШТАМПОВКА В ЧАНК (ОСТРОВ В ОКЕАНЕ + МОСТ ИЗ МОРСКОГО ДНА, sub_air == 0!)
    // =========================================================================

    public static void buildInChunk(ChunkAccess chunk, CityPlan plan, Site site, int x0, int z0) {
        if (site == null || !site.intersectsChunk(x0, z0, 16)) return;
        LoadedTemplate tpl = template();
        if (tpl.states.length <= 1) return;

        int ocean = plan.ocean;
        int baseY = site.baseY;
        int rot = site.rot;
        int maxBuildY = chunk.getMaxBuildHeight() - 2;

        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState basalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState pbb = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState dsb = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState obsBricks = CastleBlocks.OBSIDIAN_BRICKS.get().defaultBlockState();
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        for (int wx = x0; wx <= x0 + 15; ++wx) {
            for (int wz = z0; wz <= z0 + 15; ++wz) {
                int dx = wx - site.cx;
                int dz = wz - site.cz;
                int lx = invLx(dx, dz, rot);
                int lz = invLz(dx, dz, rot);

                // Продолжение затопленного каменного моста (lz = 122..136) плавным пандусом прямо в морское дно (oceanFloor)!
                if (lz >= OZ + D && lz <= OZ + D + 14 && Math.abs(lx - GATE_LX) <= 5) {
                    int floorY = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz));
                    double u = (lz - (OZ + D - 1)) / 15.0; // 0 у края NBT (by=2 -> baseY+2), 1 на самом морском дне!
                    int startRoadY = Math.max(floorY, baseY + 2);
                    int roadY = (int) Math.round(startRoadY + (floorY - startRoadY) * u);
                    boolean parapet = Math.abs(lx - GATE_LX) == 5 && u < 0.72;
                    int topSubY = Math.min(ocean - 2, parapet ? roadY + 1 : roadY);
                    for (int y = floorY - 1; y <= topSubY; ++y) {
                        double hm = ElisarChunkGenerator.hash(wx, y, wz);
                        BlockState subBlk = (y >= roadY - 1)
                                ? (hm < 0.38 ? mossy : (parapet ? pbb : dsb))
                                : (hm < 0.5 ? deepslate : blackstone);
                        chunk.setBlockState(mp.set(wx, y, wz), subBlk, false);
                    }
                    for (int y = topSubY + 1; y <= ocean; ++y) {
                        mp.set(wx, y, wz);
                        if (chunk.getBlockState(mp).isAir()) {
                            chunk.setBlockState(mp, seaWater, false);
                        }
                    }
                    continue;
                }

                int bx = lx - OX;
                int bz = lz - OZ;
                if (bx < 0 || bx >= W || bz < 0 || bz >= D) continue;

                int colStride = bz * W + bx;
                int baseVoxel = tpl.voxels[colStride] & 0xFF;

                // Укореняем скалистый базальтовый остров и подводные опоры/арки моста прямо в морское дно (sub_air == 0!)
                if (baseVoxel != 0) {
                    int floorY = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz) - 1);
                    double hCol = ElisarChunkGenerator.hash(wx, wz, 947);
                    boolean isSubBridge = (lz >= 80 && Math.abs(lx - GATE_LX) <= 7);
                    BlockState rootRock = isSubBridge
                            ? (hCol < 0.35 ? mossy : (hCol < 0.72 ? dsb : pbb))
                            : (hCol < 0.45 ? deepslate : (hCol < 0.78 ? basalt : blackstone));
                    for (int y = floorY; y < baseY; ++y) {
                        chunk.setBlockState(mp.set(wx, y, wz), rootRock, false);
                    }
                }

                int maxBy = Math.min(H - 1, maxBuildY - baseY);
                for (int by = 0; by <= maxBy; ++by) {
                    int v = tpl.voxels[by * (D * W) + colStride] & 0xFF;
                    int wy = baseY + by;
                    if (v != 0) {
                        BlockState st;
                        if (v == tpl.grassId) {
                            boolean courtRib = (Math.floorMod(lx, 6) == 0 || Math.floorMod(lz, 6) == 0);
                            st = courtRib ? obsBricks : Blocks.DEEPSLATE_TILES.defaultBlockState();
                        } else if (v == tpl.dirtId) {
                            st = dsb;
                        } else {
                            st = tpl.states[v][rot];
                        }
                        chunk.setBlockState(mp.set(wx, wy, wz), st, false);
                    } else if (wy <= ocean) {
                        // Вся подводная часть (включая подводные готические арки моста на lz = 88..121!) заполнена морской водой!
                        mp.set(wx, wy, wz);
                        if (chunk.getBlockState(mp).isAir()) {
                            chunk.setBlockState(mp, seaWater, false);
                        }
                    }
                }
            }
        }

        // Симметричные скромные сундуки в Главном зале Собора и Ротонде + сундук в Нижнем портовом бастионе (Level 1)!
        placeCitadelChest(chunk, site, x0, z0, -22, 57, -8, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  22, 57, -8, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -22, 57, -32, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  22, 57, -32, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -21, 71, -22, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  21, 71, -22, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -14, 57, -58, Direction.EAST, LandmarkStructures.LOOT_MODEST_MARITIME);
        placeCitadelChest(chunk, site, x0, z0,  14, 57, -58, Direction.WEST, LandmarkStructures.LOOT_MODEST_MARITIME);
        placeCitadelChest(chunk, site, x0, z0,  77, 33, -20, Direction.WEST, LandmarkStructures.LOOT_MODEST_MARITIME);
    }

    private static void placeCitadelChest(ChunkAccess chunk, Site site, int x0, int z0,
                                          int lx, int by, int lz, Direction localFacing,
                                          net.minecraft.resources.ResourceLocation table) {
        int wx = site.cx + rotX(lx, lz, site.rot);
        int wz = site.cz + rotZ(lx, lz, site.rot);
        int wy = site.baseY + by;
        if (wx < x0 || wx > x0 + 15 || wz < z0 || wz > z0 + 15) return;

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int placeY = -1;
        for (int dy = 0; dy <= 3; ++dy) {
            int testY = wy + (dy == 0 ? 0 : (dy == 1 ? 1 : (dy == 2 ? -1 : 2)));
            if (!chunk.getBlockState(mp.set(wx, testY - 1, wz)).isAir()
                    && chunk.getBlockState(mp.set(wx, testY, wz)).isAir()) {
                placeY = testY;
                break;
            }
        }
        if (placeY < 0) return;

        Rotation mcRot = switch (site.rot & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        LandmarkStructures.putLootChest(chunk, x0, z0, wx, placeY, wz, mcRot.rotate(localFacing), table);
    }

    /** Фоновая предзагрузка {@code blacktide.nbt} в память при старте игры — 0 задержки на первом чанке. */
    public static void warmupAsync() {
        Thread t = new Thread(() -> {
            try {
                template();
            } catch (Throwable ex) {
                LOG.warn("BlacktideCitadel warmup skipped: {}", ex.toString());
            }
        }, "riftworld-blacktide-warmup");
        t.setDaemon(true);
        t.start();
    }

    private BlacktideCitadel() {
    }
}
