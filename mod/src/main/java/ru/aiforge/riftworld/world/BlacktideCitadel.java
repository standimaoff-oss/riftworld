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

    // Локальные координаты берегового устоя моста (на суше биома), ворот и донжона (в открытом море)
    public static final int LAND_LX = -109, LAND_LZ = 94, LAND_BY = 41;
    public static final int BRIDGE_START_LX = -108, BRIDGE_START_LZ = 94;
    public static final int GATE_LX = -65, GATE_LZ = 35, GATE_BY = 48;
    public static final int KEEP_LX = 12, KEEP_LZ = -25, KEEP_BY = 63;

    // Единичный вектор от берегового устоя (-109, 94) к морской цитадели (+5, -2) в локальных координатах rot=0
    private static final double U0_X;
    private static final double U0_Z;
    static {
        double vx = 5.0 - LAND_LX;
        double vz = -2.0 - LAND_LZ;
        double len = Math.hypot(vx, vz);
        U0_X = vx / len;
        U0_Z = vz / len;
    }

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
            // Подходная дорога-мыс на суше Пепельного биома к началу моста
            double dx = x - landX, dz = z - landZ;
            double along = -(dx * outUx + dz * outUz); // положительно вглубь суши Пепельного биома
            double perp = Math.abs(-dx * outUz + dz * outUx);
            return along >= -8.0 && along <= 52.0 && perp <= 24.0 + margin;
        }

        /**
         * Высота пологого вулканического мыса на суше Пепельного биома, поднимающегося от равнины (ocean+1)
         * прямо к отметке берегового устоя моста ({@code baseY + 40 = 78}), чтобы на мост заходили пешком с земли биома!
         */
        public int headlandTop(int wx, int wz, int ocean) {
            double dx = (wx + 0.5) - landX;
            double dz = (wz + 0.5) - landZ;
            double inland = -(dx * outUx + dz * outUz); // 0 у начала моста, >0 вглубь Пепельного биома
            double perp = Math.abs(-dx * outUz + dz * outUx);
            if (inland < -12.0 || inland > 62.0 || perp > 64.0) return Integer.MIN_VALUE;

            // Широкий скалистый береговой обрыв Пепельного биома по обе стороны от начала моста (ocean+3..ocean+8)
            double coastAlong = inland < -12.0 ? 0.0 : (inland <= 18.0 ? Math.min(1.0, (inland + 12.0) / 10.0)
                    : Math.max(0.0, 1.0 - (inland - 18.0) / 44.0));
            double coastSide = perp <= 42.0 ? 1.0 : Math.max(0.0, 1.0 - (perp - 42.0) / 22.0);
            double crag = 3.5 + 4.5 * ElisarChunkGenerator.vnoise(wx * 0.06, wz * 0.06, 913);
            double bluffH = (ocean + 1.0) + crag * coastAlong * coastSide;

            // Центральный плавный пандус-мыс прямо к настилу берегового устоя моста (baseY + 40 = 78)
            double rampH = Integer.MIN_VALUE;
            if (inland >= -6.0 && inland <= 54.0 && perp <= 26.0) {
                double targetY = baseY + 40.0;
                double uAlong = inland <= 6.0 ? 1.0 : Math.max(0.0, 1.0 - (inland - 6.0) / 46.0);
                double sAlong = uAlong * uAlong * (3.0 - 2.0 * uAlong);
                double halfW = 10.5 + (1.0 - sAlong) * 12.0;
                if (perp < halfW) {
                    double uSide = perp <= 4.5 ? 1.0 : Math.max(0.0, 1.0 - (perp - 4.5) / Math.max(1.0, halfW - 4.5));
                    double sSide = uSide * uSide * (3.0 - 2.0 * uSide);
                    rampH = (ocean + 1.0) + (targetY - (ocean + 1.0)) * sAlong * sSide;
                }
            }
            double h = Math.max(bluffH, rampH);
            if (h <= ocean + 1.4) return Integer.MIN_VALUE;
            return (int) Math.round(h);
        }
    }

    /** Создаёт размещение Цитадели на побережье Пепельного биома для выбранного поворота {@code rot}. */
    public static Site createForCoast(int ashCx, int ashCz, double ashR, int ocean, int rot) {
        int r = rot & 3;
        double ux = rotDx(U0_X, U0_Z, r);
        double uz = rotDz(U0_X, U0_Z, r);
        // Береговой устой моста стоит на 36 блоков ВНУТРИ суши Пепельного биома, мост перешагивает берег, а сам замок — в открытом море!
        double landDist = ashR - 36.0;
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
        BlockState roadBrick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState crackedRoad = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState seaWater = ElisarBlocks.WATER.get().defaultBlockState();

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        // 1) Мощёная базальтово-чернокаменная дорога по вулканическому мысу Пепельного биома прямо к началу моста!
        for (int wx = x0; wx <= x0 + 15; ++wx) {
            for (int wz = z0; wz <= z0 + 15; ++wz) {
                int ht = site.headlandTop(wx, wz, ocean);
                if (ht != Integer.MIN_VALUE) {
                    double dx = (wx + 0.5) - site.landX;
                    double dz = (wz + 0.5) - site.landZ;
                    double inland = -(dx * site.outUx + dz * site.outUz);
                    double perp = Math.abs(-dx * site.outUz + dz * site.outUx);
                    if (inland >= 2.0 && inland <= 50.0 && perp <= 3.6) {
                        double h2 = ElisarChunkGenerator.hash(wx, wz, 912);
                        BlockState pave = perp <= 2.3
                                ? (h2 < 0.35 ? crackedRoad : roadBrick)
                                : (h2 < 0.5 ? ash : burntSoil);
                        chunk.setBlockState(mp.set(wx, ht, wz), pave, false);
                        // Расчищаем воздух над дорогой подъёма к мосту
                        for (int cy = ht + 1; cy <= ht + 6; ++cy) {
                            chunk.setBlockState(mp.set(wx, cy, wz), air, false);
                        }
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

                boolean onShoreLanding = (lx <= -86 && lz >= 70);
                boolean bridgeApproachDeck = (lx >= -120 && lx <= -98 && lz >= 88 && lz <= 102);

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
                                // Во дворах морской цитадели — суровый вулканический туф, мшистый камень и пепельный дёрн
                                st = hg < 0.44 ? burntSoil : (hg < 0.78 ? tuff : ElisarBlocks.KEEP_STONE_MOSSY.get().defaultBlockState());
                            }
                        } else if (v == tpl.dirtId) {
                            st = onShoreLanding ? scorched : tuff;
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

        // 3) Скромные сундуки с припасами внутри надвратной башни, башни нижнего двора и зала Донжона!
        placeCitadelChest(chunk, site, x0, z0, -54, 48, 36, Direction.NORTH, LandmarkStructures.LOOT_MODEST_MARITIME);
        placeCitadelChest(chunk, site, x0, z0, -76, 49, -43, Direction.EAST, LandmarkStructures.LOOT_MODEST_MARITIME);
        placeCitadelChest(chunk, site, x0, z0, 12, 64, -14, Direction.SOUTH, LandmarkStructures.LOOT_MODEST_WAYFARER);
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
