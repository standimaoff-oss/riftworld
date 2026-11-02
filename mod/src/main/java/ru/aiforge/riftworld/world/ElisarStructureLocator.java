package ru.aiforge.riftworld.world;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * v0.26.0: Мгновенный аналитический локатор ВСЕХ структур измерения Элисар (< 3 мс)
 * для стандартной команды {@code /locate structure riftworld:<id>}.
 */
public final class ElisarStructureLocator {
    private ElisarStructureLocator() {
    }

    @Nullable
    public static Pair<BlockPos, Holder<Structure>> findNearest(
            ServerLevel level, HolderSet<Structure> targets, BlockPos origin, int searchRadius) {
        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;
        int ox = origin.getX();
        int oz = origin.getZ();
        int maxDist = Math.max(4096, searchRadius * 16 * 3);

        BlockPos bestPos = null;
        Holder<Structure> bestHolder = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Holder<Structure> holder : targets) {
            String kind = null;
            if (holder.value() instanceof ElisarAnalyticalStructure eas) {
                kind = eas.kind();
            } else {
                var optKey = holder.unwrapKey();
                if (optKey.isPresent()) {
                    kind = optKey.get().location().getPath();
                }
            }
            if (kind == null) continue;

            BlockPos cand = locateSingleKind(kind, ox, oz, ocean, maxDist);
            if (cand == null) continue;
            double dx = cand.getX() - ox;
            double dz = cand.getZ() - oz;
            double d2 = dx * dx + dz * dz;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                bestPos = cand;
                bestHolder = holder;
            }
        }
        return (bestPos != null && bestHolder != null) ? Pair.of(bestPos, bestHolder) : null;
    }

    @Nullable
    public static BlockPos locateSingleKind(String kind, int ox, int oz, int ocean, int maxDist) {
        return switch (kind) {
            case "white_city" -> new BlockPos(0, 108, 0);
            case "city_sanctuary", "city_royal_castle" -> new BlockPos(0, 109, -4);

            case "inverted_tower" -> {
                int rMax = Math.max(5, (maxDist / InvertedTowers.SUPER) + 3);
                InvertedTowers.Site ts = InvertedTowers.nearest(ox, oz, true, rMax);
                yield ts == null ? null : new BlockPos(ts.cx(), ts.roostY(), ts.cz());
            }

            case "blacktide_citadel" -> {
                int sx0 = Math.floorDiv(ox, AshenLands.SUPER), sz0 = Math.floorDiv(oz, AshenLands.SUPER);
                int rMax = Math.max(4, (maxDist / AshenLands.SUPER) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                            if (rg == null || !rg.hasBlacktide || rg.blacktideSite == null) continue;
                            BlacktideCitadel.Site bs = rg.blacktideSite;
                            double d2 = distSq(bs.gateX, bs.gateZ, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(bs.gateX, bs.baseY + 48, bs.gateZ);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "ember_castle" -> {
                int sx0 = Math.floorDiv(ox, AshenLands.SUPER), sz0 = Math.floorDiv(oz, AshenLands.SUPER);
                int rMax = Math.max(4, (maxDist / AshenLands.SUPER) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                            if (rg == null || !rg.hasRedCastle) continue;
                            double d2 = distSq(rg.cx, rg.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(rg.cx, CastleSite.Y + 2, rg.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "mist_keep" -> {
                int sx0 = Math.floorDiv(ox, MistKeep.SUPER), sz0 = Math.floorDiv(oz, MistKeep.SUPER);
                int rMax = Math.max(5, (maxDist / MistKeep.SUPER) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            MistKeep.Region mk = MistKeep.region(sx0 + dx, sz0 + dz);
                            if (mk == null) continue;
                            double d2 = distSq(mk.cx, mk.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(mk.cx, ocean + 12, mk.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "ember_ruins" -> {
                int sx0 = Math.floorDiv(ox, EmberRuins.SUPER), sz0 = Math.floorDiv(oz, EmberRuins.SUPER);
                int rMax = Math.max(5, (maxDist / EmberRuins.SUPER) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            EmberRuins.Site s = EmberRuins.site(sx0 + dx, sz0 + dz);
                            if (s == null) continue;
                            double d2 = distSq(s.cx, s.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(s.cx, (int) Math.round(s.island.base) + 2, s.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "colonnade_arena" -> {
                int sx0 = Math.floorDiv(ox, CrystalRifts.SUPER), sz0 = Math.floorDiv(oz, CrystalRifts.SUPER);
                int rMax = Math.max(4, (maxDist / CrystalRifts.SUPER) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            CrystalRifts.Region cr = CrystalRifts.region(sx0 + dx, sz0 + dz);
                            if (cr == null) continue;
                            int ax = (int) Math.round(cr.astroX), az = (int) Math.round(cr.astroZ);
                            double d2 = distSq(ax, az, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(ax, cr.astroY + 1, az);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "white_village" -> {
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_VILLAGE), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_VILLAGE);
                int rMax = Math.max(5, (maxDist / WorldStructures.SUPER_VILLAGE) + 3);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= rMax; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            WorldStructures.Village v = WorldStructures.whiteVillage(sx0 + dx, sz0 + dz);
                            if (v == null) continue;
                            double d2 = distSq(v.cx, v.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(v.cx, v.baseY + 1, v.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "burned_village" -> {
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_BURNED), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_BURNED);
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            WorldStructures.Village bv = WorldStructures.burnedIslandVillage(sx0 + dx, sz0 + dz);
                            if (bv != null) {
                                double d2 = distSq(bv.cx, bv.cz, ox, oz);
                                if (d2 < bestD2) {
                                    bestD2 = d2;
                                    best = new BlockPos(bv.cx, bv.baseY + 1, bv.cz);
                                }
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                int ax0 = Math.floorDiv(ox, AshenLands.SUPER), az0 = Math.floorDiv(oz, AshenLands.SUPER);
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        AshenLands.Region rg = AshenLands.region(ax0 + dx, az0 + dz);
                        if (rg == null) continue;
                        WorldStructures.Village av = WorldStructures.ashenPlainVillage(rg);
                        if (av != null) {
                            double d2 = distSq(av.cx, av.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(av.cx, av.baseY + 1, av.cz);
                            }
                        }
                    }
                }
                yield best;
            }

            case "stilt_village" -> {
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                int gx0 = Math.floorDiv(ox, WaterfallGardens.SUPER), gz0 = Math.floorDiv(oz, WaterfallGardens.SUPER);
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        WaterfallGardens.Region gr = WaterfallGardens.region(gx0 + dx, gz0 + dz);
                        if (gr == null) continue;
                        List<WorldStructures.StiltVillage> list = WorldStructures.gardenStiltVillages(gr);
                        for (WorldStructures.StiltVillage sv : list) {
                            double d2 = distSq(sv.cx, sv.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(sv.cx, sv.deckY + 1, sv.cz);
                            }
                        }
                    }
                }
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_STILT), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_STILT);
                for (int dx = -4; dx <= 4; ++dx) {
                    for (int dz = -4; dz <= 4; ++dz) {
                        WorldStructures.StiltVillage sv = WorldStructures.seaStiltVillage(sx0 + dx, sz0 + dz);
                        if (sv != null) {
                            double d2 = distSq(sv.cx, sv.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(sv.cx, sv.deckY + 1, sv.cz);
                            }
                        }
                    }
                }
                yield best;
            }

            case "harbor_port" -> {
                int sx0 = Math.floorDiv(ox, LandmarkStructures.SUPER_HARBOR), sz0 = Math.floorDiv(oz, LandmarkStructures.SUPER_HARBOR);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            LandmarkStructures.HarborPort hp = LandmarkStructures.harborPort(sx0 + dx, sz0 + dz);
                            if (hp == null) continue;
                            double d2 = distSq(hp.cx, hp.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(hp.cx, hp.quayY + 1, hp.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "grand_theater" -> {
                int sx0 = Math.floorDiv(ox, LandmarkStructures.SUPER_THEATER), sz0 = Math.floorDiv(oz, LandmarkStructures.SUPER_THEATER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            LandmarkStructures.GrandTheater gt = LandmarkStructures.grandTheater(sx0 + dx, sz0 + dz);
                            if (gt == null) continue;
                            double d2 = distSq(gt.cx, gt.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(gt.cx, gt.baseY + 2, gt.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "sea_lighthouse" -> {
                int sx0 = Math.floorDiv(ox, LandmarkStructures.SUPER_LIGHTHOUSE), sz0 = Math.floorDiv(oz, LandmarkStructures.SUPER_LIGHTHOUSE);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            LandmarkStructures.SeaLighthouse sl = LandmarkStructures.seaLighthouse(sx0 + dx, sz0 + dz);
                            if (sl == null) continue;
                            double d2 = distSq(sl.cx, sl.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(sl.cx, sl.rockTopY + 1, sl.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "ancient_tree_altar" -> {
                int sx0 = Math.floorDiv(ox, SizoGrove.SUPER), sz0 = Math.floorDiv(oz, SizoGrove.SUPER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int dx = -4; dx <= 4; ++dx) {
                    for (int dz = -4; dz <= 4; ++dz) {
                        SizoGrove.Region sg = SizoGrove.region(sx0 + dx, sz0 + dz);
                        LandmarkStructures.BiomeLandmark bl = LandmarkStructures.ancientTreeAltar(sg);
                        if (bl == null) continue;
                        double d2 = distSq(bl.cx, bl.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            best = new BlockPos(bl.cx, bl.baseY + 2, bl.cz);
                        }
                    }
                }
                yield best;
            }

            case "ashen_belfry_barrow" -> {
                int sx0 = Math.floorDiv(ox, AshenLands.SUPER), sz0 = Math.floorDiv(oz, AshenLands.SUPER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int dx = -5; dx <= 5; ++dx) {
                    for (int dz = -5; dz <= 5; ++dz) {
                        AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                        LandmarkStructures.BiomeLandmark bl = LandmarkStructures.ashenBelfryBarrow(rg);
                        if (bl == null) continue;
                        double d2 = distSq(bl.cx, bl.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            best = new BlockPos(bl.cx, bl.baseY + 2, bl.cz);
                        }
                    }
                }
                yield best;
            }

            case "ashen_burned_church" -> {
                int sx0 = Math.floorDiv(ox, AshenLands.SUPER), sz0 = Math.floorDiv(oz, AshenLands.SUPER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int dx = -5; dx <= 5; ++dx) {
                    for (int dz = -5; dz <= 5; ++dz) {
                        AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                        LandmarkStructures.BiomeLandmark bl = LandmarkStructures.ashenBurnedChurch(rg);
                        if (bl == null) continue;
                        double d2 = distSq(bl.cx, bl.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            best = new BlockPos(bl.cx, bl.baseY + 2, bl.cz);
                        }
                    }
                }
                yield best;
            }

            case "shattered_shard_forge" -> {
                int sx0 = Math.floorDiv(ox, CrystalRifts.SUPER), sz0 = Math.floorDiv(oz, CrystalRifts.SUPER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int dx = -4; dx <= 4; ++dx) {
                    for (int dz = -4; dz <= 4; ++dz) {
                        CrystalRifts.Region cr = CrystalRifts.region(sx0 + dx, sz0 + dz);
                        LandmarkStructures.BiomeLandmark bl = LandmarkStructures.shatteredShardForge(cr);
                        if (bl == null) continue;
                        double d2 = distSq(bl.cx, bl.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            best = new BlockPos(bl.cx, bl.baseY + 2, bl.cz);
                        }
                    }
                }
                yield best;
            }

            case "violet_wind_observatory" -> {
                int sx0 = Math.floorDiv(ox, VioletHills.SUPER), sz0 = Math.floorDiv(oz, VioletHills.SUPER);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int dx = -4; dx <= 4; ++dx) {
                    for (int dz = -4; dz <= 4; ++dz) {
                        VioletHills.Region vh = VioletHills.region(sx0 + dx, sz0 + dz);
                        LandmarkStructures.BiomeLandmark bl = LandmarkStructures.violetWindObservatory(vh);
                        if (bl == null) continue;
                        double d2 = distSq(bl.cx, bl.cz, ox, oz);
                        if (d2 < bestD2) {
                            bestD2 = d2;
                            best = new BlockPos(bl.cx, bl.baseY + 2, bl.cz);
                        }
                    }
                }
                yield best;
            }

            case "crashed_ship" -> {
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_CRASH), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_CRASH);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            WorldStructures.ShipSite s = WorldStructures.crashedShip(sx0 + dx, sz0 + dz);
                            if (s == null) continue;
                            double d2 = distSq(s.cx, s.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(s.cx, s.rockTopY + 3, s.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "sunken_ship" -> {
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_SUNKEN), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_SUNKEN);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            WorldStructures.ShipSite s = WorldStructures.sunkenShip(sx0 + dx, sz0 + dz);
                            if (s == null) continue;
                            double d2 = distSq(s.cx, s.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(s.cx, s.baseY + 3, s.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "ocean_building" -> {
                int sx0 = Math.floorDiv(ox, WorldStructures.SUPER_OCEAN_BLDG), sz0 = Math.floorDiv(oz, WorldStructures.SUPER_OCEAN_BLDG);
                BlockPos best = null;
                double bestD2 = Double.MAX_VALUE;
                for (int r = 0; r <= 6; ++r) {
                    for (int dx = -r; dx <= r; ++dx) {
                        for (int dz = -r; dz <= r; ++dz) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                            WorldStructures.OceanBuilding ob = WorldStructures.oceanBuilding(sx0 + dx, sz0 + dz);
                            if (ob == null) continue;
                            double d2 = distSq(ob.cx, ob.cz, ox, oz);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = new BlockPos(ob.cx, ob.floorY + 3, ob.cz);
                            }
                        }
                    }
                    if (best != null && r >= 1) break;
                }
                yield best;
            }

            case "coral_grove" -> CoralGrove.nearestGrove(ox, oz);

            case "sea_cave_exit" -> {
                ElisarSea.SeaExit ex = ElisarSea.nearestSeaCaveExit(ox, oz, ocean, 16);
                yield ex == null ? null : new BlockPos(ex.cx(), ex.floorY() + 2, ex.cz());
            }

            default -> null;
        };
    }

    private static double distSq(int x1, int z1, int x2, int z2) {
        double dx = (double) x1 - x2;
        double dz = (double) z1 - z2;
        return dx * dx + dz * dz;
    }
}
