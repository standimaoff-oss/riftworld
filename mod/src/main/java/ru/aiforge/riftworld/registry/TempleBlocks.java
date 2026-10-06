package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.LightBridgeBlock;
import ru.aiforge.riftworld.block.MosaicBlock;
import ru.aiforge.riftworld.block.MosaicCoreBlock;
import ru.aiforge.riftworld.block.RitualCandleBlock;
import ru.aiforge.riftworld.block.TemplePlantBlock;
import ru.aiforge.riftworld.block.TempleTrapBlock;

/** Own stone, soil, flora, light and mechanisms used by the Great Temple and its undercroft. */
public final class TempleBlocks {
    private TempleBlocks() {
    }

    private static BlockBehaviour.Properties p(MapColor color, float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(strength, strength * 3).sound(sound);
    }

    public static final RegistryObject<Block> BRICKS = ModBlocks.reg("temple_bricks", () -> new Block(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<StairBlock> STAIRS = ModBlocks.reg("temple_bricks_stairs",
            () -> new StairBlock(() -> BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(BRICKS.get())));
    public static final RegistryObject<SlabBlock> SLAB = ModBlocks.reg("temple_bricks_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(BRICKS.get())));
    public static final RegistryObject<Block> GOLD = ModBlocks.reg("temple_gold", () -> new Block(p(MapColor.GOLD, 2.0F, SoundType.METAL)));
    public static final RegistryObject<RotatedPillarBlock> PILLAR = ModBlocks.reg("temple_pillar",
            () -> new RotatedPillarBlock(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<Block> FLOOR = ModBlocks.reg("temple_floor", () -> new Block(p(MapColor.QUARTZ, 2.0F, SoundType.STONE)));
    public static final RegistryObject<Block> UNDERSTONE = ModBlocks.reg("temple_understone", () -> new Block(p(MapColor.COLOR_BLACK, 3.0F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> MAZE_FLOOR = ModBlocks.reg("temple_maze_floor", () -> new Block(p(MapColor.COLOR_GRAY, 2.0F, SoundType.DEEPSLATE_TILES)));
    public static final RegistryObject<Block> GARDEN_SOIL = ModBlocks.reg("temple_garden_soil", () -> new Block(p(MapColor.TERRACOTTA_BROWN, 0.7F, SoundType.ROOTED_DIRT)));
    public static final RegistryObject<Block> MOSS = ModBlocks.reg("temple_moss", () -> new CarpetBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.MOSS_CARPET).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<TemplePlantBlock> FERN = ModBlocks.reg("temple_fern", TemplePlantBlock::new);
    public static final RegistryObject<TemplePlantBlock> BLOOM = ModBlocks.reg("temple_bloom", TemplePlantBlock::new);
    public static final RegistryObject<TemplePlantBlock> ROOT_VINE = ModBlocks.reg("temple_root_vine", TemplePlantBlock::new);
    public static final RegistryObject<Block> LUMEN_CRYSTAL = ModBlocks.reg("temple_lumen_crystal", () -> new GlassBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.4F).sound(SoundType.AMETHYST_CLUSTER)
                    .noOcclusion().lightLevel(s -> 12).isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false)));
    public static final RegistryObject<TempleTrapBlock> TRAP_PLATE = ModBlocks.reg("temple_trap_plate", TempleTrapBlock::new);

    public static final RegistryObject<MosaicBlock> MOSAIC = ModBlocks.reg("temple_mosaic",
            () -> new MosaicBlock(p(MapColor.COLOR_BLUE, 3.0F, SoundType.STONE)));
    public static final RegistryObject<MosaicCoreBlock> MOSAIC_CORE = ModBlocks.reg("mosaic_core",
            () -> new MosaicCoreBlock(p(MapColor.COLOR_BLUE, 3.0F, SoundType.STONE)));
    public static final RegistryObject<RitualCandleBlock> RITUAL_CANDLE = ModBlocks.reg("ritual_candle",
            () -> new RitualCandleBlock(p(MapColor.QUARTZ, 0.5F, SoundType.CANDLE).noOcclusion()));
    public static final RegistryObject<LightBridgeBlock> LIGHT_BRIDGE = ModBlocks.reg("light_bridge", LightBridgeBlock::new);

    /** Forces this registry group to initialize before the DeferredRegister is attached to the mod bus. */
    public static void init() {
    }
}
