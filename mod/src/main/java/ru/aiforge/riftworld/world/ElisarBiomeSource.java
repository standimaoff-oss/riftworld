package ru.aiforge.riftworld.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
            Biome.CODEC.fieldOf("crystal").forGetter(s -> s.crystal)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen, Holder<Biome> violet, Holder<Biome> sizo, Holder<Biome> gardens,
                             Holder<Biome> mist, Holder<Biome> reef, Holder<Biome> abyss, Holder<Biome> crystal) {
        this.def = def;
        this.ashen = ashen;
        this.violet = prosec(violet);
        this.sizo = sizo;
        this.gardens = gardens;
        this.mist = mist;
        this.reef = reef;
        this.abyss = abyss;
        this.crystal = crystal;
    }

    private static Holder<Biome> prosec(Holder<Biome> h) {
        return h;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        double x = qx * 4 + 2, z = qz * 4 + 2;
        CityPlan plan = CityPlan.get();
        CityPlan.Island at = plan.islandAt(x, z);
        if (at != null) {
            if (at.kind == WaterfallGardens.KIND && WaterfallGardens.landAt(x, z) != null) return gardens;
            if (at.kind == SizoGrove.KIND && SizoGrove.inside(x, z)) return sizo;
            if (at.kind == 7 && VioletHills.inside(x, z)) return violet;
            if (at.kind == CrystalRifts.KIND && CrystalRifts.landAt(x, z) != null) return crystal;
            if (at.kind == 5) return ashen;
        }
        boolean dry = at != null && qy * 4 > plan.ocean;
        if (dry) {
            if (at.kind == WaterfallGardens.KIND)
                return WaterfallGardens.landAt(x, z) != null ? gardens : reef;
            if (at.kind == SizoGrove.KIND) return sizo;
            if (at.kind == 7) return violet;
            if (at.kind == CrystalRifts.KIND) return crystal;
            if (at.kind == 5) return ashen;
        }
        if (qy * 4 > plan.ocean - 24 && AshenLands.inside(x, z)) return ashen;
        if (dry) return def;
        if (MistKeep.inside(x, z)) return mist;
        if (qy * 4 <= plan.ocean + 8) {
            int floor = ElisarChunkGenerator.oceanFloor(plan, (int) Math.floor(x), (int) Math.floor(z));
            if (ElisarSea.reefness((int) Math.floor(x), (int) Math.floor(z)) > 0.45) return reef;
            if (plan.ocean - floor > 46) return abyss;
        }
        return def;
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
