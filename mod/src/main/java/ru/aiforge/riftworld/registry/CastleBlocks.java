package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.StrippablePillarBlock;

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
            () -> new StrippablePillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD), () -> CastleBlocks.STRIPPED_DARK_BEAM.get()));
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
            () -> new StrippablePillarBlock(p(MapColor.COLOR_BLACK, 1.5F, SoundType.WOOD), () -> CastleBlocks.STRIPPED_CHARRED_LOG.get()));

    // C-2: room furnishing
    public static final RegistryObject<RotatedPillarBlock> BOOKSHELF = ModBlocks.reg("castle_bookshelf",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 1.5F, SoundType.WOOD)));
    public static final RegistryObject<IronBarsBlock> BARS = ModBlocks.reg("castle_bars",
            () -> new IronBarsBlock(p(MapColor.METAL, 5.0F, SoundType.METAL).noOcclusion()));
    public static final RegistryObject<CarpetBlock> CARPET = ModBlocks.reg("castle_carpet",
            () -> new ru.aiforge.riftworld.block.MergingCarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1F).sound(SoundType.WOOL)));
    public static final RegistryObject<WebBlock> COBWEB = ModBlocks.reg("castle_cobweb",
            () -> new WebBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).noCollission().strength(4.0F).noOcclusion()));

    // v0.27.0: Черноводные обсидианово-базальтовые блоки, рунный базальт, кованые жаровни и окованные доски
    public static final RegistryObject<Block> OBSIDIAN_BRICKS = cube("obsidian_bricks", p(MapColor.COLOR_BLACK, 4.0F, SoundType.DEEPSLATE_BRICKS));
    public static final RegistryObject<StairBlock> OBSIDIAN_BRICKS_STAIRS = stairs("obsidian_bricks_stairs", OBSIDIAN_BRICKS);
    public static final RegistryObject<SlabBlock> OBSIDIAN_BRICKS_SLAB = slab("obsidian_bricks_slab", OBSIDIAN_BRICKS);
    public static final RegistryObject<WallBlock> OBSIDIAN_BRICKS_WALL = ModBlocks.reg("obsidian_bricks_wall",
            () -> new WallBlock(p(MapColor.COLOR_BLACK, 4.0F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> CARVED_BASALT_RUNES = ModBlocks.reg("carved_basalt_runes",
            () -> new Block(p(MapColor.COLOR_BLACK, 3.5F, SoundType.BASALT).lightLevel(st -> 7)));
    public static final RegistryObject<Block> EMBER_BRAZIER = ModBlocks.reg("ember_brazier_block",
            () -> new Block(p(MapColor.COLOR_ORANGE, 3.0F, SoundType.LANTERN).lightLevel(st -> 15)));
    public static final RegistryObject<Block> IRONBOUND_DARK_PLANKS = cube("ironbound_dark_planks", p(MapColor.COLOR_BROWN, 3.0F, SoundType.WOOD));
    public static final RegistryObject<Block> ASHEN_COBBLESTONE = cube("ashen_cobblestone", p(MapColor.COLOR_GRAY, 2.2F, SoundType.BASALT));

    // v0.28.0: Полные ванильные наборы Тёмного дуба замка (+8), Обугленного дерева (+12) и Пепельного булыжника (+3)
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_DARK_BEAM = ModBlocks.reg("stripped_dark_beam",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_DARK_WOOD = ModBlocks.reg("stripped_dark_wood",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> DARK_WOOD = ModBlocks.reg("dark_wood",
            () -> new StrippablePillarBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD), STRIPPED_DARK_WOOD));
    public static final RegistryObject<FenceGateBlock> DARK_FENCE_GATE = ModBlocks.reg("dark_fence_gate",
            () -> new FenceGateBlock(p(MapColor.COLOR_BROWN, 2.0F, SoundType.WOOD), WoodType.DARK_OAK));
    public static final RegistryObject<DoorBlock> DARK_DOOR = ModBlocks.reg("dark_door",
            () -> new DoorBlock(p(MapColor.COLOR_BROWN, 3.0F, SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY), BlockSetType.DARK_OAK));
    public static final RegistryObject<TrapDoorBlock> DARK_TRAPDOOR = ModBlocks.reg("dark_trapdoor",
            () -> new TrapDoorBlock(p(MapColor.COLOR_BROWN, 3.0F, SoundType.WOOD).noOcclusion(), BlockSetType.DARK_OAK));
    public static final RegistryObject<PressurePlateBlock> DARK_PRESSURE_PLATE = ModBlocks.reg("dark_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    p(MapColor.COLOR_BROWN, 0.5F, SoundType.WOOD).noCollission().pushReaction(PushReaction.DESTROY), BlockSetType.DARK_OAK));
    public static final RegistryObject<ButtonBlock> DARK_BUTTON = ModBlocks.reg("dark_button",
            () -> new ButtonBlock(p(MapColor.COLOR_BROWN, 0.5F, SoundType.WOOD).noCollission().pushReaction(PushReaction.DESTROY),
                    BlockSetType.DARK_OAK, 30, true));

    public static final RegistryObject<RotatedPillarBlock> STRIPPED_CHARRED_LOG = ModBlocks.reg("stripped_charred_log",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BLACK, 1.5F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_CHARRED_WOOD = ModBlocks.reg("stripped_charred_wood",
            () -> new RotatedPillarBlock(p(MapColor.COLOR_BLACK, 1.5F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> CHARRED_WOOD = ModBlocks.reg("charred_wood",
            () -> new StrippablePillarBlock(p(MapColor.COLOR_BLACK, 1.5F, SoundType.WOOD), STRIPPED_CHARRED_WOOD));
    public static final RegistryObject<Block> CHARRED_PLANKS = cube("charred_planks", p(MapColor.COLOR_BLACK, 2.0F, SoundType.WOOD));
    public static final RegistryObject<StairBlock> CHARRED_STAIRS = stairs("charred_stairs", CHARRED_PLANKS);
    public static final RegistryObject<SlabBlock> CHARRED_SLAB = slab("charred_slab", CHARRED_PLANKS);
    public static final RegistryObject<FenceBlock> CHARRED_FENCE = ModBlocks.reg("charred_fence",
            () -> new FenceBlock(p(MapColor.COLOR_BLACK, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<FenceGateBlock> CHARRED_FENCE_GATE = ModBlocks.reg("charred_fence_gate",
            () -> new FenceGateBlock(p(MapColor.COLOR_BLACK, 2.0F, SoundType.WOOD), WoodType.CRIMSON));
    public static final RegistryObject<DoorBlock> CHARRED_DOOR = ModBlocks.reg("charred_door",
            () -> new DoorBlock(p(MapColor.COLOR_BLACK, 3.0F, SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY), BlockSetType.CRIMSON));
    public static final RegistryObject<TrapDoorBlock> CHARRED_TRAPDOOR = ModBlocks.reg("charred_trapdoor",
            () -> new TrapDoorBlock(p(MapColor.COLOR_BLACK, 3.0F, SoundType.WOOD).noOcclusion(), BlockSetType.CRIMSON));
    public static final RegistryObject<PressurePlateBlock> CHARRED_PRESSURE_PLATE = ModBlocks.reg("charred_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    p(MapColor.COLOR_BLACK, 0.5F, SoundType.WOOD).noCollission().pushReaction(PushReaction.DESTROY), BlockSetType.CRIMSON));
    public static final RegistryObject<ButtonBlock> CHARRED_BUTTON = ModBlocks.reg("charred_button",
            () -> new ButtonBlock(p(MapColor.COLOR_BLACK, 0.5F, SoundType.WOOD).noCollission().pushReaction(PushReaction.DESTROY),
                    BlockSetType.CRIMSON, 30, true));

    public static final RegistryObject<StairBlock> ASHEN_COBBLESTONE_STAIRS = stairs("ashen_cobblestone_stairs", ASHEN_COBBLESTONE);
    public static final RegistryObject<SlabBlock> ASHEN_COBBLESTONE_SLAB = slab("ashen_cobblestone_slab", ASHEN_COBBLESTONE);
    public static final RegistryObject<WallBlock> ASHEN_COBBLESTONE_WALL = ModBlocks.reg("ashen_cobblestone_wall",
            () -> new WallBlock(p(MapColor.COLOR_GRAY, 2.2F, SoundType.BASALT)));

    public static void init() {
    }
}
