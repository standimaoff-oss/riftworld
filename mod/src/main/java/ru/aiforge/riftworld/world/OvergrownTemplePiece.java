package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.registry.ModBlocks;
import ru.aiforge.riftworld.registry.ModWorldgen;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * v0.24.0: Overworld Abandoned Portal Observatory ({@code reports/portal_hall/ref_abandoned_overworld.png}).
 * <ul>
 *   <li>Octagonal weathered stone-brick, mossy temple-brick &amp; oxidized-copper vaulted pavilion with
 *       timber rafters, collapsed skylight oculus, hanging iron chains/lanterns, shallow floor puddles,
 *       connected woven entrance rug, and faded banners;</li>
 *   <li>Central stepped ritual dais ({@code MOSAIC_CORE} + concentric {@code MOSAIC} &amp; {@code GOLD_RUNE} rings
 *       + 8 {@code RITUAL_CANDLE} pedestals + 4 fluted {@code GRAND_COLUMN} rune pillars);</li>
 *   <li>Two Sanctuary Reliquary Chests + Keeper's Lectern Tome containing <b>everything needed</b>
 *       ({@code ResonanceStone}, {@code Flint & Steel}, {@code Ritual Candles}, offerings, and instructions)
 *       to perform the full awakening of the White 3D Spatial Breach into Elisar!</li>
 * </ul>
 */
public class OvergrownTemplePiece extends StructurePiece {
    public static final int TW = 37, TD = 37, TH = 24;
    public static final ResourceLocation LOOT_OBSERVATORY =
            new ResourceLocation(RiftWorld.MODID, "chests/portal_observatory");

    private final BlockPos anchor;
    private final Rotation rot;

    public OvergrownTemplePiece(BlockPos anchor, Rotation rot) {
        super(ModWorldgen.OVERGROWN_TEMPLE_PIECE.get(), 0, envelope(anchor));
        this.anchor = anchor;
        this.rot = rot;
    }

    public OvergrownTemplePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModWorldgen.OVERGROWN_TEMPLE_PIECE.get(), tag);
        this.anchor = NbtUtils.readBlockPos(tag.getCompound("anchor"));
        this.rot = Rotation.valueOf(tag.getString("rot"));
    }

    private static BoundingBox envelope(BlockPos center) {
        int r = 20;
        return new BoundingBox(center.getX() - r, center.getY() - 18, center.getZ() - r,
                center.getX() + r, center.getY() + TH + 4, center.getZ() + r);
    }

    /** World position of the central MOSAIC_CORE seal for this Observatory piece. */
    public BlockPos sealWorldPos() {
        return new BlockPos(anchor.getX(), anchor.getY() + 1, anchor.getZ());
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.put("anchor", NbtUtils.writeBlockPos(anchor));
        tag.putString("rot", rot.name());
    }

    private BlockPos wpos(int lx, int dy, int lz) {
        int rx = switch (rot) {
            case CLOCKWISE_90 -> -lz;
            case CLOCKWISE_180 -> -lx;
            case COUNTERCLOCKWISE_90 -> lz;
            default -> lx;
        };
        int rz = switch (rot) {
            case CLOCKWISE_90 -> lx;
            case CLOCKWISE_180 -> -lz;
            case COUNTERCLOCKWISE_90 -> -lx;
            default -> lz;
        };
        return new BlockPos(anchor.getX() + rx, anchor.getY() + dy, anchor.getZ() + rz);
    }

    private void put(WorldGenLevel level, BoundingBox box, int lx, int dy, int lz, BlockState state) {
        BlockPos p = wpos(lx, dy, lz);
        if (!box.isInside(p)) return;
        level.setBlock(p, state.rotate(rot), 2);
    }

    private static double detHash(int x, int y, int z) {
        long h = x * 374761393L + y * 668265263L + z * 1274126177L;
        h = (h ^ (h >> 13)) * 1103515245L;
        return ((h ^ (h >> 16)) & 0xFFFFL) / 65535.0;
    }

    private BlockState weatheredWall(int lx, int dy, int lz) {
        double h = detHash(lx + anchor.getX(), dy, lz + anchor.getZ());
        if (h < 0.24) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (h < 0.44) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (h < 0.66) return Blocks.STONE_BRICKS.defaultBlockState();
        if (h < 0.80) return TempleBlocks.CRACKED.get().defaultBlockState();
        if (h < 0.92) return TempleBlocks.IVY.get().defaultBlockState();
        return TempleBlocks.BRICKS.get().defaultBlockState();
    }

    private BlockState oxidizedRoof(int lx, int dy, int lz) {
        double h = detHash(lx * 3, dy * 5, lz * 7);
        if (h < 0.46) return Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
        if (h < 0.82) return Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
        return Blocks.EXPOSED_CUT_COPPER.defaultBlockState();
    }

    private BlockState oxidizedSlab(int lx, int dy, int lz, SlabType type) {
        double h = detHash(lx * 7, dy * 3, lz * 5);
        BlockState base = h < 0.55 ? Blocks.OXIDIZED_CUT_COPPER_SLAB.defaultBlockState()
                : Blocks.WEATHERED_CUT_COPPER_SLAB.defaultBlockState();
        return base.setValue(SlabBlock.TYPE, type);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox box, ChunkPos cp, BlockPos pos) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState templeFloor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState darkBeamX = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState darkBeamZ = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState rug = ModBlocks.MODELS.get(ModelKind.RUG).get().defaultBlockState();
        BlockState waterPuddle = Blocks.STONE_BRICK_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, true);

        // 1) Octagonal pavilion foundation, cleared interior air volume, and sunken rain-puddle stone floor
        for (int lx = -16; lx <= 16; ++lx) {
            for (int lz = -16; lz <= 18; ++lz) {
                double oct = Math.max(Math.abs(lx), Math.abs(lz)) + 0.42 * Math.min(Math.abs(lx), Math.abs(lz));
                boolean inPorch = (Math.abs(lx) <= 4 && lz >= 12 && lz <= 18);
                if (oct > 16.6 && !inPorch) continue;

                // Clear trees/brush inside & above the pavilion up to skylight height
                for (int dy = 1; dy <= 21; ++dy) {
                    put(level, box, lx, dy, lz, air);
                }

                // Foundation skirt down to solid Overworld ground so zero corners ever float!
                for (int dy = -1; dy >= -14; --dy) {
                    BlockPos wp = wpos(lx, dy, lz);
                    if (!box.isInside(wp)) break;
                    BlockState cur = level.getBlockState(wp);
                    if (cur.isSolidRender(level, wp) && dy < -3) break;
                    level.setBlock(wp, dy >= -2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 2);
                }

                // Main floor at dy = 0 (with shallow rain puddles in cracked depressions, ref_abandoned_overworld.png!)
                double distCenter = Math.hypot(lx, lz);
                if (inPorch && lz >= 15) {
                    // Stepped South Approach Stairs leading up from ground to porch
                    int stepDy = (lz == 18) ? -2 : (lz == 17 ? -1 : 0);
                    for (int fillY = -3; fillY < stepDy; ++fillY) {
                        put(level, box, lx, fillY, lz, Blocks.STONE_BRICKS.defaultBlockState());
                    }
                    BlockState st = (lz >= 17 && Math.abs(lx) <= 3)
                            ? Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH)
                            : weatheredWall(lx, 0, lz);
                    put(level, box, lx, stepDy, lz, st);
                } else {
                    double hp = detHash(lx + 31, 0, lz - 19);
                    boolean isPuddle = distCenter > 6.8 && distCenter < 12.2
                            && Math.abs(lx) > 2 && !(lz > 6 && Math.abs(lx) <= 3)
                            && hp < 0.14;
                    if (isPuddle) {
                        put(level, box, lx, -1, lz, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                        put(level, box, lx, 0, lz, waterPuddle);
                    } else if (distCenter <= 8.6) {
                        put(level, box, lx, 0, lz, (hp < 0.22 && distCenter > 6.2) ? goldRune : templeFloor);
                    } else {
                        put(level, box, lx, 0, lz, hp < 0.30 ? TempleBlocks.FLOOR.get().defaultBlockState() : weatheredWall(lx, 0, lz));
                        if (hp > 0.88 && distCenter > 10.2 && Math.abs(lx) > 3 && Math.abs(lz) > 3) {
                            put(level, box, lx, 1, lz, hp > 0.94 ? Blocks.FERN.defaultBlockState() : Blocks.MOSS_CARPET.defaultBlockState());
                        }
                    }
                }
            }
        }

        // 2) Raised Circular Ritual Seal Dais (dy = +1) at the center — ready for full Spatial Breach opening!
        for (int lx = -6; lx <= 6; ++lx) {
            for (int lz = -6; lz <= 6; ++lz) {
                double r = Math.hypot(lx, lz);
                if (r <= 4.6) {
                    BlockState daisBlock;
                    if (lx == 0 && lz == 0) {
                        daisBlock = mosaicCore;
                    } else if (Math.abs(lx) <= 1 && Math.abs(lz) <= 1) {
                        daisBlock = goldRune;
                    } else if (Math.abs(lx) <= 2 && Math.abs(lz) <= 2) {
                        daisBlock = mosaic;
                    } else if (r >= 3.3 && r <= 4.3 && ((Math.abs(lx) + Math.abs(lz)) % 2 == 0)) {
                        daisBlock = goldRune;
                    } else {
                        daisBlock = templeFloor;
                    }
                    put(level, box, lx, 1, lz, daisBlock);
                } else if (r <= 5.7) {
                    // Gently stepped circular stone-brick rim around the dais
                    Direction outDir = Math.abs(lx) >= Math.abs(lz)
                            ? (lx > 0 ? Direction.WEST : Direction.EAST)
                            : (lz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(level, box, lx, 1, lz, TempleBlocks.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, outDir));
                }
            }
        }

        // 3) Connected Woven Entrance Rug Runner (3x7 seamless connected rug from South Porch to Dais!)
        for (int lz = 6; lz <= 12; ++lz) {
            for (int lx = -1; lx <= 1; ++lx) {
                put(level, box, lx, 1, lz, rug);
            }
        }
        // Cross-wings at entrance forming a T-shaped connected rug!
        put(level, box, -2, 1, 11, rug);
        put(level, box,  2, 1, 11, rug);
        put(level, box, -2, 1, 12, rug);
        put(level, box,  2, 1, 12, rug);

        // 4) 8 Carved Stone Pedestals with Ritual Candles around the Seal (for candle-ritual path!)
        int[][] candleOffsets = {
                {-5, -2}, {-5, 2}, {5, -2}, {5, 2},
                {-2, -5}, {2, -5}, {-2, 5}, {2, 5}
        };
        for (int[] co : candleOffsets) {
            put(level, box, co[0], 1, co[1], chiseled);
            put(level, box, co[0], 2, co[1], TempleBlocks.RITUAL_CANDLE.get().defaultBlockState());
        }

        // 5) 4 Towering Inner Rune Columns framing the Skylight Oculus
        int[][] innerCols = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] ic : innerCols) {
            put(level, box, ic[0], 1, ic[1], goldRune);
            for (int dy = 2; dy <= 11; ++dy) {
                put(level, box, ic[0], dy, ic[1], dy == 7 ? Blocks.OXIDIZED_CUT_COPPER.defaultBlockState() : grandCol);
            }
            put(level, box, ic[0], 12, ic[1], TempleBlocks.GOLD.get().defaultBlockState());
        }

        // 6) Octagonal Perimeter Arched Colonnade, Semi-Ruined Walls, Rose Windows & Faded Banners
        int[][] outerPiers = {
                {-13, -6}, {-13, 6}, {13, -6}, {13, 6},
                {-6, -13}, {6, -13}, {-6, 13}, {6, 13}
        };
        for (int[] op : outerPiers) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (Math.abs(dx) == 1 && Math.abs(dz) == 1) continue;
                    for (int dy = 1; dy <= 11; ++dy) {
                        put(level, box, op[0] + dx, dy, op[1] + dz,
                                (dx == 0 && dz == 0) ? grandCol : weatheredWall(op[0] + dx, dy, op[1] + dz));
                    }
                }
            }
        }

        // Perimeter arched curtain walls (open grand South archway + semi-ruined upper courses + rose windows)
        for (int lx = -14; lx <= 14; ++lx) {
            for (int lz = -14; lz <= 14; ++lz) {
                double oct = Math.max(Math.abs(lx), Math.abs(lz)) + 0.42 * Math.min(Math.abs(lx), Math.abs(lz));
                if (oct < 13.6 || oct > 15.1) continue;

                // Keep South Entrance Archway open (|lx| <= 2, lz > 10, dy = 1..7)
                boolean southGate = (lz > 10 && Math.abs(lx) <= 2);
                // Tall arched window bays on N, W, E and flanking S bays
                boolean northBay = (lz < -10 && Math.abs(lx) <= 2);
                boolean sideBay = (Math.abs(lx) > 10 && Math.abs(lz) <= 2);

                int ruinTop = 10 - (int) (detHash(lx, 17, lz) * 2.4);
                for (int dy = 1; dy <= ruinTop; ++dy) {
                    if (southGate && dy <= 7) continue;
                    if ((northBay || sideBay) && dy >= 3 && dy <= 7) {
                        if (dy == 7 && (lx == 0 || lz == 0)) {
                            put(level, box, lx, dy, lz, TempleBlocks.ROSE_WINDOW.get().defaultBlockState());
                        } else if (dy == 3) {
                            put(level, box, lx, dy, lz, Blocks.IRON_BARS.defaultBlockState());
                        }
                        continue;
                    }
                    put(level, box, lx, dy, lz, weatheredWall(lx, dy, lz));
                }

                // Exterior hanging ivy / vines on weathered stone piers
                if (detHash(lx, 99, lz) < 0.20 && Math.abs(lx) == 14) {
                    Direction vineFace = lx < 0 ? Direction.EAST : Direction.WEST;
                    BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(vineFace), true);
                    for (int dy = 5; dy <= 9; ++dy) {
                        put(level, box, lx + (lx < 0 ? -1 : 1), dy, lz, vine);
                    }
                }
            }
        }

        // Whispering sleeping TempleSpirit relief above the North Sanctum Arch
        put(level, box, 0, 8, -13, TempleBlocks.SPIRIT.get().defaultBlockState());

        // Faded cyan/white banners hanging inward from the 4 inner piers
        put(level, box, -5, 8, -4, Blocks.WHITE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
        put(level, box,  5, 8, -4, Blocks.WHITE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
        put(level, box, -5, 8,  4, Blocks.LIGHT_BLUE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.NORTH));
        put(level, box,  5, 8,  4, Blocks.LIGHT_BLUE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.NORTH));

        // 7) Oxidized-Copper & Dark-Oak Timber Vaulted Dome (dy = 11..19) with Open Central Skylight Oculus (r < 4.8)!
        for (int lx = -14; lx <= 14; ++lx) {
            for (int lz = -14; lz <= 14; ++lz) {
                double r = Math.hypot(lx, lz);
                if (r < 4.8 || r > 14.8) continue;

                // Semi-ruined jagged edges near the skylight oculus & outer eaves (ref_abandoned_overworld.png!)
                double gapNoise = detHash(lx + 7, 41, lz - 13);
                if (r < 5.7 && gapNoise < 0.35) continue;

                double u = (14.8 - r) / (14.8 - 4.8); // 0 at outer eaves -> 1 at oculus collar
                int domeY = 11 + (int) Math.round(Math.sin(u * Math.PI * 0.5) * 6.5);
                boolean rib = (Math.abs(lx) <= 1 || Math.abs(lz) <= 1 || Math.abs(Math.abs(lx) - Math.abs(lz)) <= 1);
                if (rib) {
                    put(level, box, lx, domeY, lz, Blocks.STONE_BRICKS.defaultBlockState());
                    put(level, box, lx, domeY + 1, lz, oxidizedSlab(lx, domeY + 1, lz, SlabType.BOTTOM));
                } else {
                    put(level, box, lx, domeY, lz, oxidizedRoof(lx, domeY, lz));
                    // Dark-oak timber rafter purlins underneath the copper vault
                    if (domeY >= 12 && ((lx + lz) & 3) == 0) {
                        put(level, box, lx, domeY - 1, lz, Math.abs(lx) > Math.abs(lz) ? darkBeamZ : darkBeamX);
                    }
                }
            }
        }

        // Skylight Oculus Stone Collar + Hanging Wrought-Iron Chains & Lanterns!
        int[][] chainSpots = {{-5, 0}, {5, 0}, {0, -5}, {0, 5}, {-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        for (int[] cs : chainSpots) {
            int topY = (Math.abs(cs[0]) == 7) ? 14 : 16;
            int chainLen = (Math.abs(cs[0]) == 7) ? 4 : 5;
            for (int k = 1; k <= chainLen; ++k) {
                put(level, box, cs[0], topY - k, cs[1], Blocks.CHAIN.defaultBlockState());
            }
            put(level, box, cs[0], topY - chainLen - 1, cs[1],
                    Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        // 8) COMPLETE PORTAL-OPENING RELIQUARY & KEEPER'S SCRIPTORIUM ALCOVES!
        // West Reliquary Alcove (lx = -10, lz = -2..+2): Altar Table + Chest #1 (Resonance Stones + Flint & Steel + Offerings!) + Lit Candles
        for (int lz = -2; lz <= 2; ++lz) {
            put(level, box, -10, 1, lz, TempleBlocks.BRICKS.get().defaultBlockState());
        }
        put(level, box, -10, 2, -2, ModBlocks.MODELS.get(ModelKind.CANDLE_STAND).get().defaultBlockState()
                .setValue(ModelBlock.FACING, Direction.EAST).setValue(ModelBlock.POWERED, true));
        put(level, box, -10, 2,  2, ModBlocks.MODELS.get(ModelKind.CANDLE_STAND).get().defaultBlockState()
                .setValue(ModelBlock.FACING, Direction.EAST).setValue(ModelBlock.POWERED, true));
        placeObservatoryChest(level, box, -10, 2, 0, Direction.EAST, 101L);

        // East Scriptorium Alcove (lx = +10, lz = -2..+2): Keeper's Lectern with Ritual Journal + Chest #2 + Bookshelves
        put(level, box, 11, 1, -2, Blocks.BOOKSHELF.defaultBlockState());
        put(level, box, 11, 2, -2, Blocks.BOOKSHELF.defaultBlockState());
        put(level, box, 11, 1,  2, Blocks.BOOKSHELF.defaultBlockState());
        put(level, box, 11, 2,  2, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        placeObservatoryChest(level, box, 10, 1, -1, Direction.WEST, 202L);
        placeKeeperLectern(level, box, 10, 1, 1, Direction.WEST);
    }

    private void placeObservatoryChest(WorldGenLevel level, BoundingBox box, int lx, int dy, int lz,
                                       Direction localFacing, long seedSalt) {
        BlockPos wp = wpos(lx, dy, lz);
        if (!box.isInside(wp)) return;
        Direction worldFacing = rot.rotate(localFacing);
        level.setBlock(wp, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, worldFacing), 2);
        if (level.getBlockEntity(wp) instanceof ChestBlockEntity chest) {
            long lootSeed = (wp.asLong() * 6364136223846793005L) ^ seedSalt;
            chest.setLootTable(LOOT_OBSERVATORY, lootSeed);
        }
    }

    private void placeKeeperLectern(WorldGenLevel level, BoundingBox box, int lx, int dy, int lz, Direction localFacing) {
        BlockPos wp = wpos(lx, dy, lz);
        if (!box.isInside(wp)) return;
        Direction worldFacing = rot.rotate(localFacing);
        BlockState st = Blocks.LECTERN.defaultBlockState()
                .setValue(LecternBlock.FACING, worldFacing)
                .setValue(LecternBlock.HAS_BOOK, true);
        level.setBlock(wp, st, 2);
        if (level.getBlockEntity(wp) instanceof LecternBlockEntity lectern) {
            lectern.setBook(buildKeeperRitualBook());
        }
    }

    private static ItemStack buildKeeperRitualBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Обсерватория Белого Пролома");
        tag.putString("author", "Хранитель Элисара");
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "§lПечать Белого Города§r\n\n"
                        + "Под куполом этой забытой обсерватории покоится древняя мозаичная печать, "
                        + "связывающая Обычный мир с парящими островами Элисара.\n\n"
                        + "В ковчегах у западной и восточной стен оставлено всё необходимое для полного пробуждения Пролома."))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "§lДва пути Открытия§r\n\n"
                        + "1. Возьми §6Камень Резонанса§r из сундука и приложи его к центральной печати (или брось подношение на золотые руны).\n\n"
                        + "2. Или зажги огнивом все §68 ритуальных свечей§r вокруг помоста и встань в центр печати.\n\n"
                        + "Когда мир побелеет и вырастет Белый Пролом — шагните внутрь!"))));
        tag.put("pages", pages);
        return book;
    }
}
