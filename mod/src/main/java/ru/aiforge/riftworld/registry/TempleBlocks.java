package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.RitualCandleBlock;

/**
 * Building blocks of the Great Temple of the Crossing (ivory &amp; gold). 16x16 textures of the mod only.
 * <p>
 * Rev 5: the ritual floor of the old mosaic is gone - the ritual now runs on the red hexagram of the
 * {@code rift_altar} in the middle of a hall paved with {@link RiftBlocks} (see that class).
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
    public static final RegistryObject<RitualCandleBlock> RITUAL_CANDLE = ModBlocks.reg("ritual_candle",
            () -> new RitualCandleBlock(p(MapColor.QUARTZ, 0.5F, SoundType.CANDLE).noOcclusion()));

    /** Touched by {@link ModBlocks} so the class (and its DeferredRegister entries) is loaded. */
    public static void init() {
    }
}
