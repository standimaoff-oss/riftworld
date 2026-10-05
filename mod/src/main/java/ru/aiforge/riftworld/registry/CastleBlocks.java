package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/**
 * Rev 3 / C-1: building blocks of the castle «Чертог Тлеющего Рога» (red brick, grey ashlar, dark oak, rare brass/mosaic,
 * stained glass) and the scorched ground of its site (ash, burnt soil, scorched stone, charred wood). All custom 16x16.
 */
public final class CastleBlocks {
    private CastleBlocks() {
    }

    private static BlockBehaviour.Properties p(MapColor c, float h, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(h, h * 3).sound(s);
    }

    private static RegistryObject<Block> cube(String id, BlockBehaviour.Properties p) {
        return ModBlocks.reg(id, () -> new Block(p));
    }

    private static RegistryObject<StairBlock> stairs(String id, RegistryObject<Block> base) {
        return ModBlocks.reg(id, () -> new StairBlock(() -> base.get().defaultBlockState(), BlockBehaviour.Properties.copy(base.get())));
    }

    private static RegistryObject<SlabBlock> slab(String id, RegistryObject<Block> base) {
        return ModBlocks.reg(id, () -> new SlabBlock(BlockBehaviour.Properties.copy(base.get())));
    }

    // masonry
    public static final RegistryObject<Block> BRICKS = cube("castle_bricks", p(MapColor.TERRACOTTA_RED, 2.5F, SoundType.STONE));
    public static final RegistryObject<Block> BRICKS_CRACKED = cube("castle_bricks_cracked", p(MapColor.TERRACOTTA_RED, 2.0F, SoundType.STONE));
    public static final RegistryObject<Block> BRICKS_MOSSY = cube("castle_bricks_mossy", p(MapColor.TERRACOTTA_RED, 2.5F, SoundType.STONE));
    public static final RegistryObject<StairBlock> BRICKS_STAIRS = stairs("castle_bricks_stairs", BRICKS);
    public static final RegistryObject<SlabBlock> BRICKS_SLAB = slab("castle_bricks_slab", BRICKS);
    public static final RegistryObject<WallBlock> BRICKS_WALL = ModBlocks.reg("castle_bricks_wall",
            () -> new WallBlock(p(MapColor.TERRACOTTA_RED, 2.5F, SoundType.STONE)));
    public static final RegistryObject<Block> ASHLAR = cube("ashlar", p(MapColor.STONE, 3.0F, SoundType.STONE));
    public static final RegistryObject<StairBlock> ASHLAR_STAIRS = stairs("ashlar_stairs", ASHLAR);
    public static final RegistryObject<SlabBlock> ASHLAR_SLAB = slab("ashlar_slab", ASHLAR);
    public static final RegistryObject<RotatedPillarBlock> ASHLAR_PILLAR = ModBlocks.reg("ashlar_pillar",
            () -> new RotatedPillarBlock(p(MapColor.STONE, 3.0F, SoundType.STONE)));
    // wood
    public static final RegistryObject<Block> DARK_PLANKS = cube("dark_planks", p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD));
    public static final RegistryObject<StairBlock> DARK_STAIRS = stairs("dark_planks_stairs", DARK_PLANKS);
    public static final RegistryObject<SlabBlock> DARK_SLAB = slab("dark_planks_slab", DARK_PLANKS);
    public static final RegistryObject<RotatedPillarBlock> DARK_BEAM = ModBlocks.reg("dark_beam",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<FenceBlock> DARK_FENCE = ModBlocks.reg("dark_fence",
            () -> new FenceBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    // accents
    public static final RegistryObject<Block> BRASS = cube("brass_block", p(MapColor.GOLD, 4.0F, SoundType.METAL));
    public static final RegistryObject<Block> MOSAIC = cube("castle_mosaic", p(MapColor.TERRACOTTA_YELLOW, 2.0F, SoundType.STONE));
    public static final RegistryObject<IronBarsBlock> STAINED_GLASS = ModBlocks.reg("stained_glass",
            () -> new IronBarsBlock(p(MapColor.COLOR_RED, 0.3F, SoundType.GLASS).noOcclusion()
                    .lightLevel(st -> 10).emissiveRendering((st, l, pos) -> true)));   // rev3: the glass glows and lights the room
    public static final RegistryObject<Block> ROOF = cube("castle_roof", p(MapColor.TERRACOTTA_BROWN, 2.0F, SoundType.STONE));
    public static final RegistryObject<StairBlock> ROOF_STAIRS = stairs("castle_roof_stairs", ROOF);
    public static final RegistryObject<SlabBlock> ROOF_SLAB = slab("castle_roof_slab", ROOF);
    // scorched site
    public static final RegistryObject<Block> ASH = cube("ash_block", p(MapColor.COLOR_LIGHT_GRAY, 0.5F, SoundType.SAND));
    public static final RegistryObject<Block> BURNT_SOIL = cube("burnt_soil", p(MapColor.COLOR_BLACK, 0.6F, SoundType.GRAVEL));
    public static final RegistryObject<Block> SCORCHED_STONE = cube("scorched_stone", p(MapColor.COLOR_GRAY, 2.0F, SoundType.BASALT));
    public static final RegistryObject<RotatedPillarBlock> CHARRED_LOG = ModBlocks.reg("charred_log",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BLACK, 1.5F, SoundType.WOOD)));

    // C-2: room furnishing
    public static final RegistryObject<RotatedPillarBlock> BOOKSHELF = ModBlocks.reg("castle_bookshelf",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 1.5F, SoundType.WOOD)));
    public static final RegistryObject<IronBarsBlock> BARS = ModBlocks.reg("castle_bars",
            () -> new IronBarsBlock(p(MapColor.METAL, 5.0F, SoundType.METAL).noOcclusion()));
    public static final RegistryObject<CarpetBlock> CARPET = ModBlocks.reg("castle_carpet",
            () -> new ru.aiforge.riftworld.block.MergingCarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1F).sound(SoundType.WOOL)));
    public static final RegistryObject<WebBlock> COBWEB = ModBlocks.reg("castle_cobweb",
            () -> new WebBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollission().strength(4.0F).noOcclusion()));

    public static void init() {
    }
}
