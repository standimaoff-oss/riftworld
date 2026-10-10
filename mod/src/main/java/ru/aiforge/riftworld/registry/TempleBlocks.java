package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.LightBridgeBlock;
import ru.aiforge.riftworld.block.DemonGateBlock;
import ru.aiforge.riftworld.block.MosaicBlock;
import ru.aiforge.riftworld.block.MosaicCoreBlock;
import ru.aiforge.riftworld.block.RitualCandleBlock;

/**
 * Rev 4 item 3: building blocks of the Great Temple on the main island (ivory & gold; the ritual floor). 16x16 textures.
 */
public final class TempleBlocks {
    private TempleBlocks() {
    }

    private static BlockBehaviour.Properties p(MapColor c, float h, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(h, h * 3).sound(s);
    }

    public static final RegistryObject<Block> BRICKS = ModBlocks.reg("temple_bricks", () -> new Block(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<StairBlock> STAIRS = ModBlocks.reg("temple_bricks_stairs",
            () -> new StairBlock(() -> BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(BRICKS.get())));
    public static final RegistryObject<SlabBlock> SLAB = ModBlocks.reg("temple_bricks_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(BRICKS.get())));
    public static final RegistryObject<Block> GOLD = ModBlocks.reg("temple_gold", () -> new Block(p(MapColor.GOLD, 2.0F, SoundType.METAL)));
    public static final RegistryObject<RotatedPillarBlock> PILLAR = ModBlocks.reg("temple_pillar",
            () -> new RotatedPillarBlock(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<Block> FLOOR = ModBlocks.reg("temple_floor", () -> new Block(p(MapColor.QUARTZ, 2.0F, SoundType.STONE)));
    public static final RegistryObject<MosaicBlock> MOSAIC = ModBlocks.reg("temple_mosaic",
            () -> new MosaicBlock(p(MapColor.COLOR_BLUE, 3.0F, SoundType.STONE)));
    public static final RegistryObject<MosaicCoreBlock> MOSAIC_CORE = ModBlocks.reg("mosaic_core",
            () -> new MosaicCoreBlock(p(MapColor.COLOR_BLUE, 3.0F, SoundType.STONE)));
    public static final RegistryObject<RitualCandleBlock> RITUAL_CANDLE = ModBlocks.reg("ritual_candle",
            () -> new RitualCandleBlock(p(MapColor.QUARTZ, 0.5F, SoundType.CANDLE).noOcclusion()));
    public static final RegistryObject<LightBridgeBlock> LIGHT_BRIDGE = ModBlocks.reg("light_bridge", LightBridgeBlock::new);
    // Part 7 «Храм полностью»: ruin/moss craft for the overworld temple + the demonic teleport set
    public static final RegistryObject<Block> CRACKED = ModBlocks.reg("temple_bricks_cracked", () -> new Block(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<Block> IVY = ModBlocks.reg("temple_bricks_ivy", () -> new Block(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
    public static final RegistryObject<ru.aiforge.riftworld.block.GoldRuneBlock> GOLD_RUNE = ModBlocks.reg("temple_gold_rune",
            () -> new ru.aiforge.riftworld.block.GoldRuneBlock(p(MapColor.GOLD, 2.0F, SoundType.METAL)));
    public static final RegistryObject<Block> DEMON_FRAME = ModBlocks.reg("demon_frame",
            () -> new Block(p(MapColor.COLOR_BLACK, 8.0F, SoundType.DEEPSLATE).lightLevel(s -> 3)));
        /** Part 11: a sleeping carved face that wakes and whispers at the living. */
    public static final RegistryObject<ru.aiforge.riftworld.block.TempleSpiritBlock> SPIRIT = ModBlocks.reg("temple_spirit",
            () -> new ru.aiforge.riftworld.block.TempleSpiritBlock(p(MapColor.TERRACOTTA_WHITE, 2.5F, SoundType.STONE)));
public static final RegistryObject<DemonGateBlock> DEMON_GATE = ModBlocks.reg("demon_gate", DemonGateBlock::new);

    public static void init() {
    }
}
