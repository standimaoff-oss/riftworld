package ru.aiforge.riftworld.world;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * v0.23.0: Огромная прибрежно-морская крепость «Цитадель Чёрного утёса» ({@code data/riftworld/structures/blacktide.nbt},
 * {@code 248x176x216}, {@code 1,662,983} блоков).
 * <ul>
 *   <li>Стриминговая бинарная загрузка сжатого NBT напрямую в {@code byte[248 * 176 * 216]} (~9.4 МБ ОЗУ, O(1) штамповка чанков без нагрузки на GC);</li>
 *   <li>Мост начинается прямо на суше Пепельного биома ({@link AshenLands}), плавно поднимаясь с вулканического мыса,
 *       перешагивает береговую черту арками над волнами, а сама огромная цитадель и кекуры стоят уже в открытом море!</li>
 *   <li>Подводные скалы и опоры моста доводятся до морского дна Элисара ({@code sub_air == 0}), а внутри цитадели ставятся скромные сундуки.</li>
 * </ul>
 */
public final class BlacktideCitadel {
    private static final Logger LOG = LoggerFactory.getLogger("RiftWorld/Blacktide");
    private static final String RESOURCE_PATH = "/data/riftworld/structures/blacktide.nbt";

    public static final int W = 248;
    public static final int H = 176;
    public static final int D = 216;
    public static final int OX = -124;
    public static final int OZ = -100;
    public static final int SEA_BY = 24;

    // Локальные координаты берегового устоя моста (на высоком плато биома), ворот и донжона (в бухте у берега) — строго по оси симметрии lx = 0!
    public static final int LAND_LX = 0, LAND_LZ = 96, LAND_BY = 46;
    public static final int BRIDGE_START_LX = 0, BRIDGE_START_LZ = 96;
    public static final int GATE_LX = 0, GATE_LZ = 35, GATE_BY = 48;
    public static final int KEEP_LX = 0, KEEP_LZ = -20, KEEP_BY = 57;

    // Единичный вектор от берегового устоя (0, 96) к морской цитадели (0, -20) в локальных координатах rot=0
    private static final double U0_X = 0.0;
    private static final double U0_Z = -1.0;

    /** Экземпляр Цитадели Чёрного утёса на побережье конкретного региона {@link AshenLands}. */
    public static final class Site {
        public final int cx, cz;       // мировая точка локального (lx=0, lz=0) цитадели (в открытом море!)
        public final int baseY;        // мировой Y для by=0 (ocean - 24 = 38)
        public final int rot;          // 0=NONE, 1=CW_90, 2=CW_180, 3=CCW_90
        public final int landX, landZ; // мировая точка начала моста на земле Пепельного биома!
        public final int gateX, gateZ; // мировая точка главных ворот над морем
        public final int keepX, keepZ; // мировая точка донжона в море
        public final double outUx, outUz; // мировой единичный вектор от берега Пепельного биома в море к замку
        public final int minX, maxX, minZ, maxZ;

        public Site(int cx, int cz, int baseY, int rot) {
            this.cx = cx;
            this.cz = cz;
            this.baseY = baseY;
            this.rot = rot & 3;
            this.landX = cx + rotX(LAND_LX, LAND_LZ, this.rot);
            this.landZ = cz + rotZ(LAND_LX, LAND_LZ, this.rot);
            this.gateX = cx + rotX(GATE_LX, GATE_LZ, this.rot);
            this.gateZ = cz + rotZ(GATE_LX, GATE_LZ, this.rot);
            this.keepX = cx + rotX(KEEP_LX, KEEP_LZ, this.rot);
            this.keepZ = cz + rotZ(KEEP_LX, KEEP_LZ, this.rot);
            this.outUx = rotDx(U0_X, U0_Z, this.rot);
            this.outUz = rotDz(U0_X, U0_Z, this.rot);

            int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
            int z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
            int[] cLx = {OX, OX + W - 1};
            int[] cLz = {OZ, OZ + D - 1};
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
            if (x >= minX - margin && x <= maxX + margin && z >= minZ - margin && z <= maxZ + margin) {
                return true;
            }
            // Береговое вулканическое плато и парные скальные мысы бухты Пепельного биома вокруг Цитадели
            double dx = x - landX, dz = z - landZ;
            double inland = -(dx * outUx + dz * outUz); // >=0 вглубь суши Пепельного биома, <0 в сторону бухты
            double perp = Math.abs(-dx * outUz + dz * outUx);
            return inland >= -110.0 && inland <= 96.0 && perp <= 195.0 + margin;
        }

        /**
         * Полуширина открытой морской акватории бухты вокруг Цитадели и её 4-арочного виадука.
         * Внутри бухты — чистое глубокое море ({@code weight == 0.0}), а снаружи бухту с трёх сторон
         * обнимают высокие базальтовые утёсы и парные вулканические мысы Пепельного биома!
         */
        public static double bayHalfWidth(double seaward) {
            if (seaward <= 0.0) return 68.0;
            if (seaward <= 56.0) return 68.0 + (seaward / 56.0) * 29.0; // 68 м у берегового клифа -> 97 м у ворот замка
            if (seaward <= 110.0) return 97.0; // все морские пробы ±86 лежат с запасом в чистой воде бухты!
            return 97.0 + (seaward - 110.0) * 0.42;
        }

        /**
         * Высота широкого прибрежного вулканического плато и парных базальтовых мысов Пепельного биома:
         * мост Цитадели ({@code by = 46 -> worldY = baseY + 46 = 84}) выходит РОВНО И ГОРИЗОНТАЛЬНО
         * прямо на широкую (380 м!) скалистую береговую террасу Пепельного биома без бесконечной лестницы в море!
         */
        public int headlandTop(int wx, int wz, int ocean) {
            double dx = (wx + 0.5) - landX;
            double dz = (wz + 0.5) - landZ;
            double inland = -(dx * outUx + dz * outUz); // 0 у начала моста, >0 вглубь Пепельного биома, <0 в сторону бухты
            double seaward = -inland;
            double perp = Math.abs(-dx * outUz + dz * outUx);
            if (inland > 96.0 || seaward > 132.0 || perp > 205.0) return Integer.MIN_VALUE;

            double crag = (ElisarChunkGenerator.vnoise(wx * 0.045, wz * 0.045, 913) - 0.5) * 4.0
                    + (ElisarChunkGenerator.vnoise(wx * 0.11, wz * 0.11, 917) - 0.5) * 1.8;
            double sideEnv = perp <= 150.0 ? 1.0 : Math.max(0.0, 1.0 - (perp - 150.0) / 55.0);
            sideEnv = sideEnv * sideEnv * (3.0 - 2.0 * sideEnv);
            if (sideEnv <= 0.001) return Integer.MIN_VALUE;

            double plateauY = baseY + 46.0; // 84 — строго в уровень настила 4-арочного моста!
            double h = Integer.MIN_VALUE;

            // А) Главный широкий береговой клиф и высокое плато Пепельного биома позади моста (ширина ~380 м!)
            double cliffEdge = -3.5 + (perp > 12.0 ? crag * 0.9 : 0.0);
            if (inland >= cliffEdge - 4.5) {
                double seaRise = Math.max(0.0, Math.min(1.0, (inland - (cliffEdge - 4.5)) / 4.5));
                double inlandSlope = inland <= 28.0 ? 1.0 : Math.max(0.0, 1.0 - (inland - 28.0) / 66.0);
                inlandSlope = inlandSlope * inlandSlope * (3.0 - 2.0 * inlandSlope);
                double env = seaRise * inlandSlope * sideEnv;

                // В створе моста (perp <= 10, inland = -1..+22) держим идеально ровную площадку y = 84 без зазоров!
                double roadFlat = (perp <= 10.0 && inland >= -1.5 && inland <= 22.0)
                        ? 1.0
                        : Math.max(0.0, 1.0 - Math.max(0.0, perp - 10.0) / 14.0)
                          * Math.max(0.0, 1.0 - Math.max(0.0, inland - 22.0) / 14.0);
                double localTop = (plateauY + crag * (1.0 - roadFlat));
                h = (ocean + 1.0) + (localTop - (ocean + 1.0)) * env;
            }

            // Б) Парные скалистые вулканические мысы Пепельного биома по бокам морской бухты (perp ~ 99..205, seaward = 0..130):
            // замок стоит в настоящей морской бухте вплотную к мощным берегам Пепельного биома!
            if (seaward > -6.0 && seaward <= 132.0) {
                double bayW = bayHalfWidth(Math.max(0.0, seaward)) + 5.5 + crag * 1.1;
                if (perp > bayW) {
                    double bankIn = Math.min(1.0, (perp - bayW) / 9.0);
                    double capeLen = seaward <= 55.0 ? 1.0 : Math.max(0.0, 1.0 - (seaward - 55.0) / 77.0);
                    capeLen = capeLen * capeLen * (3.0 - 2.0 * capeLen);
                    double capeTop = (baseY + 42.0) + crag * 1.4;
                    double capeH = (ocean + 1.0) + (capeTop - (ocean + 1.0)) * bankIn * capeLen * sideEnv;
                    if (capeH > h) h = capeH;
                }
            }

            if (h <= ocean + 1.25) return Integer.MIN_VALUE;
            return (int) Math.round(h);
        }
    }

    /** Создаёт размещение Цитадели в прибрежной бухте Пепельного биома для выбранного поворота {@code rot}. */
    public static Site createForCoast(int ashCx, int ashCz, double ashR, int ocean, int rot) {
        int r = rot & 3;
        double ux = rotDx(U0_X, U0_Z, r);
        double uz = rotDz(U0_X, U0_Z, r);
        // Придвигаем береговой устой моста (LAND_LZ=96) на 150 блоков ВГЛУБЬ радиуса Пепельного биома:
        // замок стоит в скалистой морской бухте вплотную к высоким утесам Пепельного биома (всего в 3.5 чанках от берега!),
        // а 4-арочный горизонтальный виадук выходит прямо на высокое пепельное плато без спуска в воду!
        double landDist = Math.max(225.0, ashR - 150.0);
        double landWx = ashCx + ux * landDist;
        double landWz = ashCz + uz * landDist;
        int siteCx = (int) Math.round(landWx - rotX(LAND_LX, LAND_LZ, r));
        int siteCz = (int) Math.round(landWz - rotZ(LAND_LX, LAND_LZ, r));
        return new Site(siteCx, siteCz, ocean - SEA_BY, r);
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

    private static double rotDx(double x, double z, int rot) {
        return switch (rot & 3) {
            case 1 -> -z;
            case 2 -> -x;
            case 3 -> z;
            default -> x;
        };
    }

    private static double rotDz(double x, double z, int rot) {
        return switch (rot & 3) {
            case 1 -> x;
            case 2 -> -z;
            case 3 -> -x;
            default -> z;
        };
    }

    // =========================================================================
    // СТРИМИНГОВЫЙ ЗАГРУЗЧИК СЖАТОГО NBT В КОМПАКТНЫЙ ВОКСЕЛЬНЫЙ МАССИВ (9.4 МБ)
    // =========================================================================

    private static final class LoadedTemplate {
        final byte[] voxels;          // [by * D * W + bz * W + bx], 0 = пусто, 1..N = индекс палитры + 1
        final BlockState[][] states;  // [v][rot]
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
                dis.readUTF(); // root name

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
        } catch ( Exception ex) {
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
    // ШТАМПОВКА В ЧАНК (МОСТ НА ЗЕМЛЕ ПЕПЕЛЬНОГО БИОМА -> ЗАМОК В МОРЕ, sub_air=0!)
    // =========================================================================

    public static void buildInChunk(ChunkAccess chunk, CityPlan plan, Site site, int x0, int z0) {
        if (site == null || !site.intersectsChunk(x0, z0, 48)) return;
        LoadedTemplate tpl = template();
        if (tpl.states.length <= 1) return;

        int ocean = plan.ocean;
        int baseY = site.baseY;
        int rot = site.rot;
        int maxBuildY = chunk.getMaxBuildHeight() - 2;

        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState basalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState tuff = Blocks.TUFF.defaultBlockState();
        BlockState burntSoil = CastleBlocks.BURNT_SOIL.get().defaultBlockState();
        BlockState ash = CastleBlocks.ASH.get().defaultBlockState();
        BlockState scorched = CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
        BlockState ashenCobble = CastleBlocks.ASHEN_COBBLESTONE.get().defaultBlockState();
        BlockState obsBricks = CastleBlocks.OBSIDIAN_BRICKS.get().defaultBlockState();
        BlockState runeBasalt = CastleBlocks.CARVED_BASALT_RUNES.get().defaultBlockState();
        BlockState brazier = CastleBlocks.EMBER_BRAZIER.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        // 1) Короткая ровная пепельно-булыжная тропа прямо по высокому плато Пепельного биома (y=84) от моста вглубь суши!
        for (int wx = x0; wx <= x0 + 15; ++wx) {
            for (int wz = z0; wz <= z0 + 15; ++wz) {
                int ht = site.headlandTop(wx, wz, ocean);
                if (ht != Integer.MIN_VALUE) {
                    double dx = (wx + 0.5) - site.landX;
                    double dz = (wz + 0.5) - site.landZ;
                    double inland = -(dx * site.outUx + dz * site.outUz);
                    double perp = Math.abs(-dx * site.outUz + dz * site.outUx);
                    if (inland >= 10.0 && inland <= 34.0 && perp <= 4.2) {
                        double h2 = ElisarChunkGenerator.hash(wx, wz, 912);
                        BlockState pave = perp <= 2.6
                                ? (h2 < 0.48 ? ashenCobble : scorched)
                                : (h2 < 0.5 ? ash : burntSoil);
                        chunk.setBlockState(mp.set(wx, ht, wz), pave, false);
                        for (int cy = ht + 1; cy <= ht + 6; ++cy) {
                            chunk.setBlockState(mp.set(wx, cy, wz), air, false);
                        }
                    } else if (Math.abs(inland - 16.0) < 0.6 && Math.abs(perp - 5.5) < 0.6) {
                        // Парные пограничные рунные тумбы с коваными жаровнями на плато Пепельного биома!
                        chunk.setBlockState(mp.set(wx, ht + 1, wz), obsBricks, false);
                        chunk.setBlockState(mp.set(wx, ht + 2, wz), runeBasalt, false);
                        chunk.setBlockState(mp.set(wx, ht + 3, wz), brazier, false);
                    }
                }
            }
        }

        if (!site.intersectsChunk(x0, z0, 0)) return;

        // 2) Штамповка вокселей Цитадели Чёрного утёса и доведение морских скал/опор до самого дна океана!
        for (int wx = x0; wx <= x0 + 15; ++wx) {
            for (int wz = z0; wz <= z0 + 15; ++wz) {
                int dx = wx - site.cx;
                int dz = wz - site.cz;
                int lx = invLx(dx, dz, rot);
                int lz = invLz(dx, dz, rot);
                int bx = lx - OX;
                int bz = lz - OZ;
                if (bx < 0 || bx >= W || bz < 0 || bz >= D) continue;

                int colStride = bz * W + bx;
                int baseVoxel = tpl.voxels[colStride] & 0xFF;

                // Если основание колонки на by=0 (worldY=38) твёрдое — укореняем скалу/бык моста до морского дна!
                if (baseVoxel != 0) {
                    int floorY = Math.max(3, ElisarChunkGenerator.oceanFloor(plan, wx, wz) - 1);
                    double hCol = ElisarChunkGenerator.hash(wx, wz, 947);
                    BlockState rootRock = hCol < 0.45 ? deepslate : (hCol < 0.78 ? basalt : blackstone);
                    for (int y = floorY; y < baseY; ++y) {
                        chunk.setBlockState(mp.set(wx, y, wz), rootRock, false);
                    }
                }

                boolean onShoreLanding = (Math.abs(lx) <= 22 && lz >= 88);
                boolean bridgeApproachDeck = (Math.abs(lx) <= 7 && lz >= 92 && lz <= 108);

                int maxBy = Math.min(H - 1, maxBuildY - baseY);
                for (int by = 0; by <= maxBy; ++by) {
                    int v = tpl.voxels[by * (D * W) + colStride] & 0xFF;
                    int wy = baseY + by;
                    if (v != 0) {
                        BlockState st;
                        if (v == tpl.grassId) {
                            double hg = ElisarChunkGenerator.hash(wx, wy, wz);
                            if (onShoreLanding) {
                                // На берегу Пепельного биома трава устоя органично переходит в пепел и выжженную почву!
                                st = hg < 0.48 ? burntSoil : (hg < 0.82 ? ash : scorched);
                            } else {
                                // Во дворах морской цитадели — ровная архитектурная кладка из тёмносланцевой плитки и чёрного кирпича без пятен!
                                boolean courtRib = (Math.floorMod(lx, 6) == 0 || Math.floorMod(lz, 6) == 0);
                                st = courtRib ? roadBrick : Blocks.DEEPSLATE_TILES.defaultBlockState();
                            }
                        } else if (v == tpl.dirtId) {
                            st = onShoreLanding ? scorched : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                        } else {
                            st = tpl.states[v][rot];
                        }
                        chunk.setBlockState(mp.set(wx, wy, wz), st, false);
                    } else if (wy <= ocean) {
                        // Гарантия sub_air == 0: в морской зоне на уровне <= ocean не оставляем воздушных карманов!
                        mp.set(wx, wy, wz);
                        if (chunk.getBlockState(mp).isAir()) {
                            chunk.setBlockState(mp, seaWater, false);
                        }
                    } else if (bridgeApproachDeck && by >= 42 && by <= 49) {
                        // Над входным раструбом моста на берегу расчищаем возможный наплыв мыса
                        chunk.setBlockState(mp.set(wx, wy, wz), air, false);
                    }
                }
            }
        }

        // 3) Строго симметрично расставленные скромные сундуки с припасами на 1-м и 2-м этажах Собора и Ротонды!
        placeCitadelChest(chunk, site, x0, z0, -22, 57, -8, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  22, 57, -8, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -22, 57, -32, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  22, 57, -32, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -21, 71, -22, Direction.EAST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0,  21, 71, -22, Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        placeCitadelChest(chunk, site, x0, z0, -14, 57, -58, Direction.EAST, LandmarkStructures.LOOT_MODEST_MARITIME);
        placeCitadelChest(chunk, site, x0, z0,  14, 57, -58, Direction.WEST, LandmarkStructures.LOOT_MODEST_MARITIME);
    }

    private static void placeCitadelChest(ChunkAccess chunk, Site site, int x0, int z0,
                                          int lx, int by, int lz, Direction localFacing,
                                          net.minecraft.resources.ResourceLocation table) {
        int wx = site.cx + rotX(lx, lz, site.rot);
        int wz = site.cz + rotZ(lx, lz, site.rot);
        int wy = site.baseY + by;
        if (wx < x0 || wx > x0 + 15 || wz < z0 || wz > z0 + 15) return;

        // Находим ближайший твёрдый пол в пределах ±2 по высоте на случай локального рельефа этажа
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
