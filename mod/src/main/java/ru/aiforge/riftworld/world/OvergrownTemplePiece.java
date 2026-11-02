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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
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
 * v0.25.0: Rebuilt-from-scratch Overworld Portal Sanctuary Rotunda.
 * <ul>
 *   <li>Monumental classical stone-brick &amp; patinated-copper Pantheon rotunda on a stepped circular crepidoma:
 *       8 solid 2x2 buttressed piers, true semicircular stone arches, ordered horizontal masonry courses,
 *       and an 8-ribbed copper dome crowned by a sculpted stone Oculus Collar open to the sky;</li>
 *   <li>Clean radial pavement, stepped central Ritual Seal Dais ({@code MOSAIC_CORE} + concentric {@code GOLD_RUNE}
 *       &amp; {@code MOSAIC} rings), 8 carved stone candle pedestals, and a seamless connected {@code RUG} runner;</li>
 *   <li>Recessed West Reliquary &amp; East Scriptorium alcoves containing 2 Reliquary Chests + Keeper's Lectern Tome
 *       with everything needed to awaken the White 3D Spatial Breach into Elisar.</li>
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

    private static BlockState sbStair(Direction facing, Half half) {
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, half);
    }

    private static BlockState sbSlab(SlabType type) {
        return Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox box, ChunkPos cp, BlockPos pos) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState sb = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossySb = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState chiseledSb = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState templeFloor = TempleBlocks.FLOOR.get().defaultBlockState();
        BlockState templeBrick = TempleBlocks.BRICKS.get().defaultBlockState();
        BlockState grandCol = TempleBlocks.GRAND_COLUMN.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState mosaic = TempleBlocks.MOSAIC.get().defaultBlockState();
        BlockState mosaicCore = TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
        BlockState goldRune = TempleBlocks.GOLD_RUNE.get().defaultBlockState();
        BlockState rug = ModBlocks.MODELS.get(ModelKind.RUG).get().defaultBlockState();
        BlockState oxCopper = Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
        BlockState weathCopper = Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
        BlockState oxSlabBot = Blocks.OXIDIZED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState oxSlabTop = Blocks.OXIDIZED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

        // 1) Stepped Circular Crepidoma Foundation, Ground Skirt & Clean Radial Sanctuary Floor
        for (int lx = -17; lx <= 17; ++lx) {
            for (int lz = -17; lz <= 19; ++lz) {
                double r = Math.hypot(lx, lz);
                boolean inSouthPortico = (Math.abs(lx) <= 4 && lz >= 12 && lz <= 19);
                if (r > 16.4 && !inSouthPortico) continue;

                for (int dy = 1; dy <= 22; ++dy) {
                    put(level, box, lx, dy, lz, air);
                }

                // Deep stone foundation skirt down into natural terrain (0 floating corners!)
                int topStylobate = (r <= 14.6 || (Math.abs(lx) <= 4 && lz <= 15)) ? 0 : (r <= 15.5 ? -1 : -2);
                if (inSouthPortico && lz >= 16) {
                    topStylobate = 15 - lz; // lz=16 -> -1, lz=17 -> -2, lz=18 -> -3
                }
                for (int dy = topStylobate - 1; dy >= -15; --dy) {
                    BlockPos wp = wpos(lx, dy, lz);
                    if (!box.isInside(wp)) break;
                    if (dy < topStylobate - 3 && level.getBlockState(wp).isSolidRender(level, wp)) break;
                    level.setBlock(wp, dy >= topStylobate - 2 ? mossySb : Blocks.COBBLESTONE.defaultBlockState(), 2);
                }

                if (inSouthPortico && lz >= 16) {
                    if (Math.abs(lx) == 4) {
                        // Sculpted cheek walls flanking the grand entrance steps
                        for (int wy = topStylobate; wy <= 1; ++wy) {
                            put(level, box, lx, wy, lz, wy == 1 ? chiseledSb : sb);
                        }
                        if (lz == 18) {
                            put(level, box, lx, 2, lz, Blocks.LANTERN.defaultBlockState());
                        }
                    } else if (lz <= 18) {
                        put(level, box, lx, topStylobate, lz, sbStair(Direction.NORTH, Half.BOTTOM));
                    } else {
                        put(level, box, lx, topStylobate, lz, sb);
                    }
                    continue;
                }

                if (r > 14.6) {
                    // Outer stepped stone ring
                    Direction inDir = Math.abs(lx) >= Math.abs(lz)
                            ? (lx > 0 ? Direction.WEST : Direction.EAST)
                            : (lz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(level, box, lx, topStylobate, lz, r <= 15.5 ? sbStair(inDir, Half.BOTTOM) : mossySb);
                    continue;
                }

                // Clean architectural radial pavement at dy = 0
                boolean cardinalRib = (Math.abs(lx) <= 1 || Math.abs(lz) <= 1 || Math.abs(Math.abs(lx) - Math.abs(lz)) == 0);
                BlockState floorSt;
                if (r <= 6.2) {
                    floorSt = templeFloor;
                } else if (r >= 12.6) {
                    floorSt = ((lx + lz) & 3) == 0 ? mossySb : sb;
                } else if (cardinalRib) {
                    floorSt = sb;
                } else if (Math.abs(r - 9.4) < 0.7) {
                    floorSt = goldRune;
                } else {
                    floorSt = templeFloor;
                }
                put(level, box, lx, 0, lz, floorSt);
            }
        }

        // 2) Raised Central Ritual Seal Dais (dy = +1) with Concentric Gold Runes & Stepped Rim
        for (int lx = -6; lx <= 6; ++lx) {
            for (int lz = -6; lz <= 6; ++lz) {
                double r = Math.hypot(lx, lz);
                if (r <= 4.5) {
                    BlockState st;
                    if (lx == 0 && lz == 0) {
                        st = mosaicCore;
                    } else if (Math.abs(lx) <= 1 && Math.abs(lz) <= 1) {
                        st = goldRune;
                    } else if (Math.abs(lx) <= 2 && Math.abs(lz) <= 2) {
                        st = mosaic;
                    } else if (r >= 3.2 && r <= 4.3 && ((Math.abs(lx) + Math.abs(lz)) % 2 == 0)) {
                        st = goldRune;
                    } else {
                        st = templeFloor;
                    }
                    put(level, box, lx, 1, lz, st);
                } else if (r <= 5.5) {
                    Direction inDir = Math.abs(lx) >= Math.abs(lz)
                            ? (lx > 0 ? Direction.WEST : Direction.EAST)
                            : (lz > 0 ? Direction.NORTH : Direction.SOUTH);
                    put(level, box, lx, 1, lz, TempleBlocks.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, inDir));
                }
            }
        }

        // 3) Seamless Connected Woven Entrance Rug (T-shaped 25-block runner from South Portico to Seal Dais!)
        for (int lz = 6; lz <= 12; ++lz) {
            for (int lx = -1; lx <= 1; ++lx) {
                put(level, box, lx, 1, lz, rug);
            }
        }
        for (int lz = 11; lz <= 12; ++lz) {
            put(level, box, -2, 1, lz, rug);
            put(level, box,  2, 1, lz, rug);
        }

        // 4) 8 Symmetrical Carved Stone Pedestals with Ritual Candles around the Seal
        int[][] candleSpots = {
                {-6, -2}, {-6, 2}, {6, -2}, {6, 2},
                {-2, -6}, {2, -6}, {-2, 6}, {2, 6}
        };
        for (int[] cs : candleSpots) {
            put(level, box, cs[0], 1, cs[1], chiseledSb);
            put(level, box, cs[0], 2, cs[1], TempleBlocks.RITUAL_CANDLE.get().defaultBlockState());
        }

        // 5) 8 Solid 2x2 Buttressed Classical Piers & True Stepped Stone Arches around the Rotunda!
        //    Placed at octagonal vertices (±12, ±5) and (±5, ±12) -> solid monumental proportions, 0 skinny sticks!
        int[][] piers = {
                {-12, -5}, {-12, 4}, {11, -5}, {11, 4},
                {-5, -12}, {4, -12}, {-5, 11}, {4, 11}
        };
        for (int[] p : piers) {
            for (int dx = 0; dx <= 1; ++dx) {
                for (int dz = 0; dz <= 1; ++dz) {
                    int px = p[0] + dx, pz = p[1] + dz;
                    // Molded plinth (dy = 1..2), fluted marble/stone shaft (dy = 3..9), classical capital (dy = 10)
                    put(level, box, px, 1, pz, chiseledSb);
                    put(level, box, px, 2, pz, sb);
                    boolean innerFlute = (Math.abs(px) <= 11 && Math.abs(pz) <= 11);
                    for (int dy = 3; dy <= 9; ++dy) {
                        put(level, box, px, dy, pz, innerFlute ? grandCol : (dy == 6 ? chiseledSb : templeBrick));
                    }
                    put(level, box, px, 10, pz, chiseledSb);
                }
            }
        }

        // Perimeter Ashlar Drum & Arched Window Bays (r = 12.6..14.1, dy = 1..11)
        for (int lx = -14; lx <= 14; ++lx) {
            for (int lz = -14; lz <= 14; ++lz) {
                double oct = Math.max(Math.abs(lx), Math.abs(lz)) + 0.414 * Math.min(Math.abs(lx), Math.abs(lz));
                if (oct < 13.0 || oct > 14.3) continue;

                boolean southArch = (lz > 10 && Math.abs(lx) <= 3);
                boolean northWindow = (lz < -10 && Math.abs(lx) <= 2);
                boolean westAlcoveBack = (lx < -11 && Math.abs(lz) <= 2);
                boolean eastAlcoveBack = (lx > 11 && Math.abs(lz) <= 2);

                for (int dy = 1; dy <= 11; ++dy) {
                    if (southArch) {
                        // Grand 7-wide Stepped Classical Entrance Archway (open up to dy = 8 at center!)
                        int archClear = 8 - (Math.abs(lx) * Math.abs(lx)) / 3;
                        if (dy <= archClear) {
                            if (dy == archClear && Math.abs(lx) >= 2) {
                                put(level, box, lx, dy, lz, sbStair(lx < 0 ? Direction.EAST : Direction.WEST, Half.TOP));
                            }
                            continue;
                        }
                    }
                    if (northWindow && dy >= 4 && dy <= 8) {
                        if (dy == 8 && lx == 0) {
                            put(level, box, lx, dy, lz, TempleBlocks.ROSE_WINDOW.get().defaultBlockState());
                        } else if (Math.abs(lx) <= 1 && dy <= 7) {
                            continue; // Clean tall arched lancet opening without floating clutter!
                        }
                    }
                    // Ordered horizontal masonry courses: mossy base -> crisp stone brick body -> chiseled frieze -> entablature!
                    BlockState course = (dy == 1) ? mossySb
                            : (dy == 2 || dy == 10) ? chiseledSb
                            : (dy == 11) ? smoothStone
                            : ((westAlcoveBack || eastAlcoveBack) && dy <= 7 ? templeBrick : sb);
                    put(level, box, lx, dy, lz, course);
                }
            }
        }

        // Classical Triangular Pediment crowning the South Entrance Portico (lz = 13..15, lx = -5..5, dy = 10..14)
        for (int lz = 13; lz <= 15; ++lz) {
            for (int lx = -5; lx <= 5; ++lx) {
                put(level, box, lx, 10, lz, chiseledSb);
                int pedTop = 13 - Math.abs(lx) / 2;
                for (int dy = 11; dy <= pedTop; ++dy) {
                    put(level, box, lx, dy, lz, (lz == 15 && dy < pedTop) ? templeBrick : sb);
                }
                if (Math.abs(lx) >= 1 && Math.abs(lx) % 2 == 1) {
                    put(level, box, lx, pedTop + 1, lz, sbStair(lx < 0 ? Direction.EAST : Direction.WEST, Half.BOTTOM));
                }
            }
            put(level, box, 0, 14, lz, sbSlab(SlabType.BOTTOM));
        }

        // 6) Ribbed Patinated-Copper & Stone Pantheon Dome (dy = 11..18) with Sculpted Open Oculus Collar (r = 4.6)!
        for (int lx = -14; lx <= 14; ++lx) {
            for (int lz = -14; lz <= 14; ++lz) {
                double r = Math.hypot(lx, lz);
                if (r < 4.5 || r > 14.2) continue;

                double u = (14.2 - r) / (14.2 - 4.5); // 0 at cornice -> 1 at oculus collar
                double exactH = 11.2 + Math.sin(u * Math.PI * 0.5) * 6.4;
                int domeY = (int) Math.floor(exactH);
                boolean upperHalf = (exactH - domeY) >= 0.52;

                boolean oculusCollar = (r <= 5.3);
                boolean meridianRib = (Math.abs(lx) <= 1 || Math.abs(lz) <= 1 || Math.abs(Math.abs(lx) - Math.abs(lz)) <= 1);

                if (oculusCollar) {
                    put(level, box, lx, domeY, lz, chiseledSb);
                    put(level, box, lx, domeY + 1, lz, sbSlab(SlabType.BOTTOM));
                } else if (meridianRib) {
                    put(level, box, lx, domeY, lz, sb);
                    if (upperHalf) {
                        put(level, box, lx, domeY + 1, lz, sbSlab(SlabType.BOTTOM));
                    }
                } else {
                    // Clean horizontal copper patina gradient (weathered lower eaves -> oxidized upper dome, 0 checkerboard!)
                    BlockState cop = (domeY <= 13) ? weathCopper : oxCopper;
                    put(level, box, lx, domeY, lz, cop);
                    put(level, box, lx, domeY - 1, lz, sbSlab(SlabType.TOP));
                    if (upperHalf) {
                        put(level, box, lx, domeY + 1, lz, oxSlabBot);
                    }
                }
            }
        }

        // 8 Symmetrical Hanging Sanctuary Lanterns suspended from the Oculus Collar & Dome Ribs
        int[][] lanternSpots = {{-5, 0}, {5, 0}, {0, -5}, {0, 5}, {-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
        for (int[] ls : lanternSpots) {
            boolean inner = Math.abs(ls[0]) <= 5 && Math.abs(ls[1]) <= 5;
            int attachY = inner ? 17 : 14;
            int len = inner ? 4 : 3;
            for (int k = 1; k <= len; ++k) {
                put(level, box, ls[0], attachY - k, ls[1], Blocks.CHAIN.defaultBlockState());
            }
            put(level, box, ls[0], attachY - len - 1, ls[1],
                    Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        // 7) Symmetrical Recessed West Reliquary & East Scriptorium Alcoves (Everything for Full Breach Awakening!)
        // West Reliquary Altar (lx = -11, lz = -2..2): Stepped Stone Table + Chest #1 + Lit Candle Stands
        for (int lz = -2; lz <= 2; ++lz) {
            put(level, box, -11, 1, lz, Math.abs(lz) == 2 ? chiseledSb : templeBrick);
        }
        put(level, box, -11, 2, -2, ModBlocks.MODELS.get(ModelKind.CANDLE_STAND).get().defaultBlockState()
                .setValue(ModelBlock.FACING, Direction.EAST).setValue(ModelBlock.POWERED, true));
        put(level, box, -11, 2,  2, ModBlocks.MODELS.get(ModelKind.CANDLE_STAND).get().defaultBlockState()
                .setValue(ModelBlock.FACING, Direction.EAST).setValue(ModelBlock.POWERED, true));
        placeObservatoryChest(level, box, -11, 2, 0, Direction.EAST, 101L);

        // East Scriptorium (lx = 11, lz = -2..2): Framed Bookshelf Niche + Chest #2 + Keeper's Lectern Tome
        for (int lz : new int[]{-2, 2}) {
            put(level, box, 11, 1, lz, Blocks.BOOKSHELF.defaultBlockState());
            put(level, box, 11, 2, lz, Blocks.BOOKSHELF.defaultBlockState());
            put(level, box, 11, 3, lz, sbStair(Direction.WEST, Half.BOTTOM));
        }
        put(level, box, 11, 1, 0, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        put(level, box, 11, 2, 0, chiseledSb);
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
                        + "Под куполом этой ротонды покоится древняя мозаичная печать, "
                        + "связывающая Обычный мир с парящими островами Элисара.\n\n"
                        + "В ковчегах у западной и восточной ниш оставлено всё необходимое для полного пробуждения Пролома."))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(
                "§lДва пути Открытия§r\n\n"
                        + "1. Возьми §6Камень Резонанса§r из сундука и приложи его к центральной печати (или брось подношение на золотые руны).\n\n"
                        + "2. Или зажги огнивом все §68 ритуальных свечей§r вокруг помоста и встань в центр печати.\n\n"
                        + "Когда вокруг на 50 шагов разольётся белизна и вырастет Белый Пролом — шагните внутрь!"))));
        tag.put("pages", pages);
        return book;
    }
}
