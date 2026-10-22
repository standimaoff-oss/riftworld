package ru.aiforge.riftworld.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/** Blocks of planet Elisar (all custom 16x16 textures). */
public final class ElisarBlocks {
    private ElisarBlocks() {
    }

    private static BlockBehaviour.Properties p(MapColor c, float h, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(h, h * 2).sound(s);
    }

    private static RegistryObject<Block> cube(String id, MapColor c, float h, SoundType s) {
        return ModBlocks.reg(id, () -> new Block(p(c, h, s)));
    }

    // terrain
    public static final RegistryObject<Block> GRASS = cube("elisar_grass", MapColor.WARPED_NYLIUM, 0.6F, SoundType.GRASS);
    public static final RegistryObject<Block> SOIL = cube("elisar_soil", MapColor.TERRACOTTA_ORANGE, 0.5F, SoundType.GRAVEL);
    public static final RegistryObject<Block> STONE = cube("elisar_stone", MapColor.TERRACOTTA_PURPLE, 1.5F, SoundType.STONE);
    public static final RegistryObject<Block> STONE_MOSSY = cube("elisar_stone_mossy", MapColor.TERRACOTTA_PURPLE, 1.5F, SoundType.STONE);
    public static final RegistryObject<Block> SEABED = cube("elisar_seabed", MapColor.SAND, 0.6F, SoundType.SAND);
    public static final RegistryObject<Block> COBBLE = cube("elisar_cobble", MapColor.STONE, 2.0F, SoundType.STONE);
    public static final RegistryObject<Block> PATH = cube("elisar_path", MapColor.DIRT, 0.6F, SoundType.GRAVEL);
    // building
    public static final RegistryObject<Block> BRICKS = cube("elisar_bricks", MapColor.TERRACOTTA_LIGHT_GRAY, 2.0F, SoundType.STONE);
    public static final RegistryObject<Block> BRICKS_MOSSY = cube("elisar_bricks_mossy", MapColor.TERRACOTTA_LIGHT_GRAY, 2.0F, SoundType.STONE);
    public static final RegistryObject<Block> PLASTER = cube("elisar_plaster", MapColor.SAND, 1.2F, SoundType.STONE);
    public static final RegistryObject<Block> TIMBER_CROSS = cube("timber_frame_cross", MapColor.SAND, 1.5F, SoundType.WOOD);
    public static final RegistryObject<Block> TIMBER_POST = cube("timber_frame_post", MapColor.SAND, 1.5F, SoundType.WOOD);
    public static final RegistryObject<Block> SIZO_PLANKS = cube("sizo_planks", MapColor.COLOR_LIGHT_BLUE, 2.0F, SoundType.WOOD);
    public static final RegistryObject<Block> AMBRA_PLANKS = cube("ambra_planks", MapColor.COLOR_ORANGE, 2.0F, SoundType.WOOD);
    public static final RegistryObject<Block> THATCH = cube("thatch", MapColor.COLOR_YELLOW, 0.5F, SoundType.GRASS);
    public static final RegistryObject<Block> ROOF_RED = cube("roof_red", MapColor.COLOR_RED, 1.5F, SoundType.STONE);
    public static final RegistryObject<Block> ROOF_BLUE = cube("roof_blue", MapColor.COLOR_BLUE, 1.5F, SoundType.STONE);
    // trees
    public static final RegistryObject<RotatedPillarBlock> SIZO_LOG = ModBlocks.reg("sizo_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_GRAY, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> AMBRA_LOG = ModBlocks.reg("ambra_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> SIZO_LEAVES = ModBlocks.reg("sizo_leaves", () -> new LeavesBlock(leaves(MapColor.COLOR_CYAN)));
    public static final RegistryObject<LeavesBlock> AMBRA_LEAVES = ModBlocks.reg("ambra_leaves", () -> new LeavesBlock(leaves(MapColor.COLOR_ORANGE)));
    // ---- Rev 7 part 2: new tree species (muted palettes: lilac / fern / mist pine / elisar oak)
    public static final RegistryObject<RotatedPillarBlock> LILAC_LOG = ModBlocks.reg("lilac_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_PURPLE, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> LILAC_LEAVES = ModBlocks.reg("lilac_leaves", () -> new LeavesBlock(leaves(MapColor.COLOR_PURPLE)));
    public static final RegistryObject<Block> LILAC_BLOSSOM = cube("lilac_blossom", MapColor.COLOR_PURPLE, 0.3F, SoundType.GRASS);
    public static final RegistryObject<RotatedPillarBlock> FERN_LOG = ModBlocks.reg("fern_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 1.6F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> FERN_LEAVES = ModBlocks.reg("fern_leaves", () -> new LeavesBlock(leaves(MapColor.PLANT)));
    public static final RegistryObject<RotatedPillarBlock> MIST_PINE_LOG = ModBlocks.reg("mist_pine_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_GRAY, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> MIST_PINE_LEAVES = ModBlocks.reg("mist_pine_leaves", () -> new LeavesBlock(leaves(MapColor.COLOR_GRAY)));
    public static final RegistryObject<RotatedPillarBlock> OAK_LOG = ModBlocks.reg("oak_log", () -> new RotatedPillarBlock(p(MapColor.WOOD, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> OAK_LEAVES = ModBlocks.reg("oak_leaves", () -> new LeavesBlock(leaves(MapColor.PLANT)));
    // shapes
    public static final RegistryObject<StairBlock> ROOF_RED_STAIRS = stairs("roof_red_stairs", ROOF_RED);
    public static final RegistryObject<StairBlock> ROOF_BLUE_STAIRS = stairs("roof_blue_stairs", ROOF_BLUE);
    public static final RegistryObject<StairBlock> THATCH_STAIRS = stairs("thatch_stairs", THATCH);
    public static final RegistryObject<StairBlock> SIZO_STAIRS = stairs("sizo_planks_stairs", SIZO_PLANKS);
    public static final RegistryObject<StairBlock> BRICKS_STAIRS = stairs("elisar_bricks_stairs", BRICKS);
    public static final RegistryObject<StairBlock> COBBLE_STAIRS = stairs("elisar_cobble_stairs", COBBLE);
    public static final RegistryObject<SlabBlock> ROOF_RED_SLAB = slab("roof_red_slab", MapColor.COLOR_RED, SoundType.STONE);
    public static final RegistryObject<SlabBlock> ROOF_BLUE_SLAB = slab("roof_blue_slab", MapColor.COLOR_BLUE, SoundType.STONE);
    public static final RegistryObject<SlabBlock> THATCH_SLAB = slab("thatch_slab", MapColor.COLOR_YELLOW, SoundType.GRASS);
    public static final RegistryObject<SlabBlock> SIZO_SLAB = slab("sizo_planks_slab", MapColor.COLOR_LIGHT_BLUE, SoundType.WOOD);
    public static final RegistryObject<SlabBlock> AMBRA_SLAB = slab("ambra_planks_slab", MapColor.COLOR_ORANGE, SoundType.WOOD);
    public static final RegistryObject<SlabBlock> COBBLE_SLAB = slab("elisar_cobble_slab", MapColor.STONE, SoundType.STONE);
    public static final RegistryObject<FenceBlock> FENCE = ModBlocks.reg("elisar_fence", () -> new FenceBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<FenceBlock> ROPE_POST = ModBlocks.reg("rope_post", () -> new FenceBlock(p(MapColor.COLOR_BROWN, 1.0F, SoundType.WOOD)));
    public static final RegistryObject<net.minecraft.world.level.block.ChainBlock> CHAIN = ModBlocks.reg("elisar_chain",
            () -> new net.minecraft.world.level.block.ChainBlock(p(MapColor.METAL, 2.0F, SoundType.CHAIN).noOcclusion()));
    public static final RegistryObject<IronBarsBlock> WINDOW = ModBlocks.reg("elisar_window", () -> new IronBarsBlock(p(MapColor.NONE, 0.3F, SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<DoorBlock> DOOR = ModBlocks.reg("elisar_door", () -> new DoorBlock(p(MapColor.COLOR_BROWN, 3.0F, SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY), BlockSetType.SPRUCE));
    // plants
    // ---- revision 2: reference-village style
    public static final RegistryObject<Block> CRUST = cube("elisar_crust", MapColor.SAND, 1.2F, SoundType.STONE);
    // улучшение биомов: монетоносная руда города - змеится прожилками в известняковых ярусах Висячих садов (биом 07)
    public static final RegistryObject<Block> AURITE_ORE = ModBlocks.reg("aurite_ore",
            () -> new Block(p(MapColor.GOLD, 3.0F, SoundType.STONE).requiresCorrectToolForDrops()));
    // мини-биом «Туманный данж» (рев 5): обломчатый серо-зелёный камень утёсов, частичные блоки для почти сломанной руины
    public static final RegistryObject<Block> KEEP_STONE = cube("keep_stone", MapColor.TERRACOTTA_LIGHT_GRAY, 2.2F, SoundType.STONE);
    public static final RegistryObject<Block> KEEP_STONE_CRACKED = cube("keep_stone_cracked", MapColor.TERRACOTTA_LIGHT_GRAY, 2.2F, SoundType.STONE);
    public static final RegistryObject<Block> KEEP_STONE_MOSSY = cube("keep_stone_mossy", MapColor.COLOR_LIGHT_GREEN, 2.2F, SoundType.STONE);
    public static final RegistryObject<StairBlock> KEEP_STAIRS = stairs("keep_stone_stairs", KEEP_STONE);
    public static final RegistryObject<SlabBlock> KEEP_SLAB = slab("keep_stone_slab", MapColor.TERRACOTTA_LIGHT_GRAY, SoundType.STONE);
    public static final RegistryObject<net.minecraft.world.level.block.LanternBlock> CAGE_LANTERN = ModBlocks.reg("keep_cage_lantern",
            () -> new net.minecraft.world.level.block.LanternBlock(p(MapColor.METAL, 1.0F, SoundType.LANTERN).lightLevel(s -> 13).noOcclusion()));
    public static final RegistryObject<Block> MOSS = cube("elisar_moss", MapColor.COLOR_LIGHT_GREEN, 0.6F, SoundType.MOSS);
    public static final RegistryObject<Block> TOWER_BRICKS = cube("tower_bricks", MapColor.COLOR_GRAY, 50.0F, SoundType.DEEPSLATE_BRICKS);
    public static final RegistryObject<Block> TOWER_BRICKS_CRACKED = cube("tower_bricks_cracked", MapColor.COLOR_GRAY, 50.0F, SoundType.DEEPSLATE_BRICKS);
    public static final RegistryObject<net.minecraft.world.level.block.LanternBlock> LANTERN = ModBlocks.reg("elisar_lantern",
            () -> new net.minecraft.world.level.block.LanternBlock(p(MapColor.METAL, 1.0F, SoundType.LANTERN).lightLevel(s -> 15).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.level.block.WallBlock> STONE_WALL = ModBlocks.reg("elisar_stone_wall",
            () -> new net.minecraft.world.level.block.WallBlock(p(MapColor.SAND, 2.0F, SoundType.STONE)));
    public static final RegistryObject<PlantBlock> PALE_WHEAT = ModBlocks.reg("pale_wheat", () -> new PlantBlock(plant()));
    public static final RegistryObject<PlantBlock> TALL_GRASS = ModBlocks.reg("elisar_tall_grass", () -> new PlantBlock(plant()));
    public static final RegistryObject<PlantBlock> FLOWERS = ModBlocks.reg("elisar_flowers", () -> new PlantBlock(plant()));
    // biome 04 «Фиолетовые холмы»: three violet bloom types, a tone apart from each other (Rev 4 item 8: "3 типа цветов")
    public static final RegistryObject<PlantBlock> VIOLET_BLOOM = ModBlocks.reg("violet_bloom", () -> new PlantBlock(plant()));
    public static final RegistryObject<PlantBlock> LILAC_BLOOM = ModBlocks.reg("lilac_bloom", () -> new PlantBlock(plant()));
    public static final RegistryObject<PlantBlock> PALE_BLOOM = ModBlocks.reg("pale_bloom", () -> new PlantBlock(plant()));
    public static final RegistryObject<VineBlock> IVY = ModBlocks.reg("elisar_ivy", () -> new VineBlock(plant().randomTicks()));
    // water (no item: the bucket is the item)
    // ВАЖНО: именно Fluid, а не Supplier. Forge-конструктор LiquidBlock(Supplier, ..) оставляет поле fluid = null,
    // а восемь методов (neighborChanged, onPlace, updateShape, skipRendering, isPathfindable, pickupBlock, ...) читают
    // это поле напрямую — первый же тик воды в мире падал с NPE и ронял сервер (поймано живым прогоном).
    public static final RegistryObject<LiquidBlock> WATER = ModBlocks.BLOCKS.register("elisar_water", () -> new LiquidBlock(ModFluids.SOURCE.get(),
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                    .noLootTable().liquid()));

    private static BlockBehaviour.Properties leaves(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(0.2F).sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn((s, l, pp, e) -> false).isSuffocating((s, l, pp) -> false).isViewBlocking((s, l, pp) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak()
                .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    private static RegistryObject<StairBlock> stairs(String id, RegistryObject<Block> base) {
        return ModBlocks.reg(id, () -> new StairBlock(() -> base.get().defaultBlockState(), BlockBehaviour.Properties.copy(base.get())));
    }

    private static RegistryObject<SlabBlock> slab(String id, MapColor c, SoundType s) {
        return ModBlocks.reg(id, () -> new SlabBlock(p(c, 1.5F, s)));
    }

    public static void init() {
    }

    /** Cross-model plant that only grows on Elisar ground. */
    public static class PlantBlock extends BushBlock {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

        public PlantBlock(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected boolean mayPlaceOn(BlockState s, BlockGetter level, BlockPos pos) {
            return s.is(GRASS.get()) || s.is(SOIL.get()) || s.is(PATH.get()) || s.is(MOSS.get());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE.move(s.getOffset(level, pos).x, 0, s.getOffset(level, pos).z);
        }
    }
}
