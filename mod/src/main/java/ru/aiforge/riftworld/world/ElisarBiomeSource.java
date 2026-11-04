package ru.aiforge.riftworld.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Elisar biomes: Ashen Lands, Violet Hills, Sizo Grove, Waterfall Gardens, Crystal Rifts, Mist Keep, Sea, and City. */
public class ElisarBiomeSource extends BiomeSource {
    // ВНИМАНИЕ: порядок полей в RecordCodecBuilder обязан строго 1:1 совпадать с порядком аргументов конструктора!
    public static final Codec<ElisarBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("default").forGetter(s -> s.def),
            Biome.CODEC.fieldOf("ashen").forGetter(s -> s.ashen),
            Biome.CODEC.fieldOf("violet").forGetter(s -> s.violet),
            Biome.CODEC.fieldOf("sizo").forGetter(s -> s.sizo),
            Biome.CODEC.fieldOf("gardens").forGetter(s -> s.gardens),
            Biome.CODEC.fieldOf("mist").forGetter(s -> s.mist),
            Biome.CODEC.fieldOf("reef").forGetter(s -> s.reef),
            Biome.CODEC.fieldOf("abyss").forGetter(s -> s.abyss),
            Biome.CODEC.fieldOf("crystal").forGetter(s -> s.crystal),
            Biome.CODEC.fieldOf("morok").forGetter(s -> s.morok),
            // Old saved dimension codecs do not have this field: keep them readable.
            Biome.CODEC.optionalFieldOf("deep_ocean").forGetter(s -> s.deepOceanSetting)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal, morok, deepOcean;
    private final Optional<Holder<Biome>> deepOceanSetting;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen, Holder<Biome> violet, Holder<Biome> sizo, Holder<Biome> gardens,
                             Holder<Biome> mist, Holder<Biome> reef, Holder<Biome> abyss, Holder<Biome> crystal,
                             Holder<Biome> morok, Optional<Holder<Biome>> deepOcean) {
        this.def = def;
        this.ashen = ashen;
        this.violet = prosec(violet);
        this.sizo = sizo;
        this.gardens = gardens;
        this.mist = mist;
        this.reef = reef;
        this.abyss = abyss;
        this.crystal = crystal;
        this.morok = morok;
        this.deepOceanSetting = deepOcean;
        this.deepOcean = deepOcean.orElse(abyss);
    }

    private static Holder<Biome> prosec(Holder<Biome> h) {
        return h;
    }

    /** Saved pre-0.40 dimension codecs legitimately lack the new biome holder. */
    public boolean hasDeepOceanBiome() { return deepOceanSetting.isPresent(); }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal, morok, deepOcean).distinct();
    }

    /**
     * v0.28.0: Потокобезопасный ThreadLocal-кэш колонок (qx, qz) на 256 слотов.
     * Minecraft запрашивает {@code getNoiseBiome(qx, qy, qz)} 96 раз по высоте для ОДНОЙ И ТОЙ ЖЕ колонки (qx, qz)
     * при генерации каждого чанка и тысячи раз при смешивании цветов биомов на клиенте!
     * Предрасчёт 4 высотных диапазонов на (qx, qz) ускоряет {@code getNoiseBiome} более чем в 80 раз при 100% совпадении результата.
     */
    private static final class ColCache {
        final long[] keys = new long[256];
        final Object[] skyAbove70 = new Object[256];   // y > ocean + 8
        final Object[] surf63to70 = new Object[256];   // ocean < y <= ocean + 8
        final Object[] mid39to62 = new Object[256];    // ocean - 24 < y <= ocean
        final Object[] deepLe38 = new Object[256];     // y <= ocean - 24

        ColCache() {
            java.util.Arrays.fill(keys, Long.MIN_VALUE);
        }
    }

    private final ThreadLocal<ColCache> colCache = ThreadLocal.withInitial(ColCache::new);

    @Override
    @SuppressWarnings("unchecked")
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int ocean = CityPlan.get().ocean;
        int y = qy * 4;
        long key = (((long) qx) << 32) ^ (qz & 0xFFFFFFFFL);
        if (key == Long.MIN_VALUE) key = Long.MIN_VALUE + 1L;
        ColCache cc = colCache.get();
        int slot = ((qx * 73428767) ^ (qz * 912931)) & 255;
        if (cc.keys[slot] != key) {
            fillColumnBiomes(qx, qz, ocean, cc, slot, key);
        }
        if (y > ocean + 8) return (Holder<Biome>) cc.skyAbove70[slot];
        if (y > ocean) return (Holder<Biome>) cc.surf63to70[slot];
        if (y > ocean - 24) return (Holder<Biome>) cc.mid39to62[slot];
        return (Holder<Biome>) cc.deepLe38[slot];
    }

    private void fillColumnBiomes(int qx, int qz, int ocean, ColCache cc, int slot, long key) {
        double x = qx * 4 + 2, z = qz * 4 + 2;
        if (DeepOcean.openCoreChunk((int) Math.floor(x), (int) Math.floor(z)) && deepOceanSetting.isPresent()) {
            cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = deepOcean;
            cc.keys[slot] = key;
            return;
        }
        CityPlan plan = CityPlan.get();
        CityPlan.Island at = plan.islandAt(x, z);
        // v0.36.0: «Мороковая Падь» — сухопутный биом поверх диких островов (туман + бледный бор)
        if (at != null && MorokHollow.inside(x, z)) {
            cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = morok;
            cc.keys[slot] = key;
            return;
        }
        if (at != null) {
            if (at.kind == WaterfallGardens.KIND && WaterfallGardens.landAt(x, z) != null) {
                cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = gardens;
                cc.keys[slot] = key;
                return;
            }
            if (at.kind == SizoGrove.KIND && SizoGrove.inside(x, z)) {
                cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = sizo;
                cc.keys[slot] = key;
                return;
            }
            if (at.kind == 7 && VioletHills.inside(x, z)) {
                cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = violet;
                cc.keys[slot] = key;
                return;
            }
            if (at.kind == CrystalRifts.KIND && CrystalRifts.landAt(x, z) != null) {
                cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = crystal;
                cc.keys[slot] = key;
                return;
            }
            if (at.kind == 5) {
                cc.skyAbove70[slot] = cc.surf63to70[slot] = cc.mid39to62[slot] = cc.deepLe38[slot] = ashen;
                cc.keys[slot] = key;
                return;
            }
        }

        // Высотный диапазон dry (y > ocean), когда at != null, но не сработал ранний возврат выше
        Holder<Biome> dryBiome = null;
        if (at != null) {
            dryBiome = switch (at.kind) {
                case WaterfallGardens.KIND -> reef;
                case SizoGrove.KIND -> sizo;
                case 7 -> violet;
                case CrystalRifts.KIND -> crystal;
                case 5 -> ashen;
                default -> def;
            };
        }

        boolean inAshen = AshenLands.inside(x, z);
        boolean inMist = !inAshen && MistKeep.inside(x, z);
        Holder<Biome> seaBiome = def;
        if (inMist) {
            seaBiome = mist;
        } else {
            int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
            int floor = ElisarChunkGenerator.oceanFloor(plan, ix, iz);
            if (DeepOcean.contains(ix, iz) && deepOceanSetting.isPresent()) seaBiome = deepOcean;
            else if (ElisarSea.reefness(ix, iz) > 0.45) seaBiome = reef;
            else if (ocean - floor > 46) seaBiome = abyss;
        }

        if (dryBiome != null) {
            cc.skyAbove70[slot] = dryBiome;
            cc.surf63to70[slot] = dryBiome;
        } else {
            cc.skyAbove70[slot] = inAshen ? ashen : (inMist ? mist : def);
            cc.surf63to70[slot] = inAshen ? ashen : seaBiome;
        }
        cc.mid39to62[slot] = inAshen ? ashen : seaBiome;
        cc.deepLe38[slot] = seaBiome;
        cc.keys[slot] = key;
    }

    /**
     * v0.26.0: Мгновенный аналитический поиск ВСЕХ 9 биомов Элисара для команды {@code /locate biome riftworld:<id>}
     * с любой высоты Y (включая морские биомы shallow_reef и abyss при полёте высоко в небе!).
     */
    @Nullable
    @Override
    public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
            BlockPos origin, int radius, int horizontalStep, int verticalStep,
            Predicate<Holder<Biome>> predicate, Climate.Sampler sampler, LevelReader level) {
        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;
        int ox = origin.getX();
        int oz = origin.getZ();

        BlockPos bestPos = null;
        Holder<Biome> bestHolder = null;
        double bestD2 = (double) radius * (double) radius;

        // 1. elisar_city (default)
        if (predicate.test(def)) {
            if (getNoiseBiome(ox >> 2, Math.max(ocean + 20, origin.getY()) >> 2, oz >> 2, sampler) == def) {
                return Pair.of(new BlockPos(ox, Math.max(ocean + 20, origin.getY()), oz), def);
            }
            double d2 = distSq(0, 0, ox, oz);
            if (d2 <= bestD2) {
                bestD2 = d2;
                bestPos = new BlockPos(0, 108, 0);
                bestHolder = def;
            }
        }

        // 2. ashen_lands
        if (predicate.test(ashen)) {
            int sx0 = Math.floorDiv(ox, AshenLands.SUPER), sz0 = Math.floorDiv(oz, AshenLands.SUPER);
            int rCells = Math.max(3, (radius / AshenLands.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                        if (rg == null) continue;
                        double d2 = distSq(rg.cx, rg.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(rg.cx, ocean + 4, rg.cz);
                            bestHolder = ashen;
                        }
                    }
                }
            }
        }

        // 3. violet_hills
        if (predicate.test(violet)) {
            int sx0 = Math.floorDiv(ox, VioletHills.SUPER), sz0 = Math.floorDiv(oz, VioletHills.SUPER);
            int rCells = Math.max(3, (radius / VioletHills.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        VioletHills.Region vh = VioletHills.region(sx0 + dx, sz0 + dz);
                        if (vh == null || vh.isles.isEmpty()) continue;
                        CityPlan.Island hub = vh.isles.get(0);
                        int bx = (int) Math.round(hub.cx), bz = (int) Math.round(hub.cz);
                        double d2 = distSq(bx, bz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(bx, (int) Math.round(hub.base) + 4, bz);
                            bestHolder = violet;
                        }
                    }
                }
            }
        }

        // 4. sizo_grove
        if (predicate.test(sizo)) {
            int sx0 = Math.floorDiv(ox, SizoGrove.SUPER), sz0 = Math.floorDiv(oz, SizoGrove.SUPER);
            int rCells = Math.max(3, (radius / SizoGrove.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        SizoGrove.Region sg = SizoGrove.region(sx0 + dx, sz0 + dz);
                        if (sg == null || sg.hub == null) continue;
                        int bx = (int) Math.round(sg.hub.cx), bz = (int) Math.round(sg.hub.cz);
                        double d2 = distSq(bx, bz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(bx, (int) Math.round(sg.hub.base) + 3, bz);
                            bestHolder = sizo;
                        }
                    }
                }
            }
        }

        // 5. waterfall_gardens
        if (predicate.test(gardens)) {
            int sx0 = Math.floorDiv(ox, WaterfallGardens.SUPER), sz0 = Math.floorDiv(oz, WaterfallGardens.SUPER);
            int rCells = Math.max(3, (radius / WaterfallGardens.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        WaterfallGardens.Region wg = WaterfallGardens.region(sx0 + dx, sz0 + dz);
                        if (wg == null || wg.isles.isEmpty()) continue;
                        CityPlan.Island hub = wg.isles.get(0);
                        int bx = (int) Math.round(hub.cx), bz = (int) Math.round(hub.cz);
                        double d2 = distSq(bx, bz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(bx, (int) Math.round(hub.base) + 6, bz);
                            bestHolder = gardens;
                        }
                    }
                }
            }
        }

        // 6. crystal_rifts
        if (predicate.test(crystal)) {
            int sx0 = Math.floorDiv(ox, CrystalRifts.SUPER), sz0 = Math.floorDiv(oz, CrystalRifts.SUPER);
            int rCells = Math.max(3, (radius / CrystalRifts.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        CrystalRifts.Region cr = CrystalRifts.region(sx0 + dx, sz0 + dz);
                        if (cr == null) continue;
                        int bx = (int) Math.round(cr.astroX), bz = (int) Math.round(cr.astroZ);
                        double d2 = distSq(bx, bz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(bx, cr.astroY + 2, bz);
                            bestHolder = crystal;
                        }
                    }
                }
            }
        }

        // 7. mist_keep
        if (predicate.test(mist)) {
            int sx0 = Math.floorDiv(ox, MistKeep.SUPER), sz0 = Math.floorDiv(oz, MistKeep.SUPER);
            int rCells = Math.max(4, (radius / MistKeep.SUPER) + 2);
            for (int r = 0; r <= rCells; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        MistKeep.Region mk = MistKeep.region(sx0 + dx, sz0 + dz);
                        if (mk == null) continue;
                        double d2 = distSq(mk.cx, mk.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            bestPos = new BlockPos(mk.cx, ocean + 10, mk.cz);
                            bestHolder = mist;
                        }
                    }
                }
            }
        }

        // 10. Deep ocean: regions have known centres, so /locate works from any altitude.
        if (deepOceanSetting.isPresent() && predicate.test(deepOcean)) {
            int qy = (ocean - 8) >> 2;
            if (getNoiseBiome(ox >> 2, qy, oz >> 2, sampler) == deepOcean) {
                return Pair.of(new BlockPos(ox, ocean - 8, oz), deepOcean);
            }
            int cx0 = Math.floorDiv(ox, DeepOcean.CELL), cz0 = Math.floorDiv(oz, DeepOcean.CELL);
            int cells = Math.max(1, radius / DeepOcean.CELL + 2);
            for (int dx = -cells; dx <= cells; ++dx) for (int dz = -cells; dz <= cells; ++dz) {
                DeepOcean.Region rg = DeepOcean.region(cx0 + dx, cz0 + dz);
                // A land/structure biome may cover the centre: sample a small cross too.
                for (int sx = -600; sx <= 600; sx += 300) for (int sz = -600; sz <= 600; sz += 300) {
                    int x = rg.x() + sx, z = rg.z() + sz;
                    double d2 = distSq(x, z, ox, oz);
                    if (d2 < bestD2 && getNoiseBiome(x >> 2, qy, z >> 2, sampler) == deepOcean) {
                        bestD2 = d2;
                        bestPos = new BlockPos(x, ocean - 8, z);
                        bestHolder = deepOcean;
                    }
                }
            }
        }

        // 8 & 9. shallow_reef & abyss (сканируем морской ярус y = ocean - 8 с любой высоты полёта!)
        boolean wantReef = predicate.test(reef);
        boolean wantAbyss = predicate.test(abyss);
        if (wantReef || wantAbyss) {
            int step = 16;
            int maxSteps = Math.min(200, Math.max(48, radius / step));
            int seaSampleY = ocean - 8;
            outerSea:
            for (int r = 0; r <= maxSteps; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        int sx = ox + dx * step;
                        int sz = oz + dz * step;
                        Holder<Biome> hb = getNoiseBiome(sx >> 2, seaSampleY >> 2, sz >> 2, sampler);
                        if ((wantReef && hb == reef) || (wantAbyss && hb == abyss)) {
                            double d2 = distSq(sx, sz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                bestPos = new BlockPos(sx, ocean - 4, sz);
                                bestHolder = hb;
                                break outerSea;
                            }
                        }
                    }
                }
            }
        }

        if (bestPos != null && bestHolder != null) {
            return Pair.of(bestPos, bestHolder);
        }
        return super.findClosestBiome3d(origin, radius, horizontalStep, verticalStep, predicate, sampler, level);
    }

    private static double distSq(int x1, int z1, int x2, int z2) {
        double dx = (double) x1 - x2;
        double dz = (double) z1 - z2;
        return dx * dx + dz * dz;
    }
}
