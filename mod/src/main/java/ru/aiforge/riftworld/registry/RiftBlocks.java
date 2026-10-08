package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.RiftAltarBlock;
import ru.aiforge.riftworld.block.RiftAltarBlockEntity;
import ru.aiforge.riftworld.block.RitualCandleBlock;

/**
 * Rev 5 «Разлом»: the hellish masonry of the ritual hall and the rift altar itself.
 * Everything is a 16x16 custom texture of the mod - no vanilla block is used anywhere in the temple.
 */
public final class RiftBlocks {
    private RiftBlocks() {
    }

    private static BlockBehaviour.Properties stone(float h) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(h, h * 3).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops();
    }

    /** Charcoal-black stone with faint crimson veins. */
    public static final RegistryObject<Block> RIFT_STONE = ModBlocks.reg("rift_stone", () -> new Block(stone(2.5F)));
    public static final RegistryObject<Block> RIFT_STONE_BRICKS = ModBlocks.reg("rift_stone_bricks", () -> new Block(stone(2.5F)));
    public static final RegistryObject<Block> RIFT_STONE_BRICKS_CRACKED = ModBlocks.reg("rift_stone_bricks_cracked", () -> new Block(stone(2.0F)));
    public static final RegistryObject<StairBlock> RIFT_STONE_STAIRS = ModBlocks.reg("rift_stone_stairs",
            () -> new StairBlock(() -> RIFT_STONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(RIFT_STONE_BRICKS.get())));
    public static final RegistryObject<SlabBlock> RIFT_STONE_SLAB = ModBlocks.reg("rift_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(RIFT_STONE_BRICKS.get())));
    public static final RegistryObject<WallBlock> RIFT_STONE_WALL = ModBlocks.reg("rift_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.copy(RIFT_STONE_BRICKS.get()).forceSolidOn()));
    /** Glowing vein of the rift: crimson light through black rock. */
    public static final RegistryObject<Block> RIFT_VEIN = ModBlocks.reg("rift_vein", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.DEEPSLATE).lightLevel(s -> 13)
            .emissiveRendering((s, l, p) -> true).hasPostProcess((s, l, p) -> true).requiresCorrectToolForDrops()));
    public static final RegistryObject<RotatedPillarBlock> RIFT_PILLAR = ModBlocks.reg("rift_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(RIFT_STONE.get())));
    /** Ash of the burnt world: a thin, silent carpet over the temple floor. */
    public static final RegistryObject<Block> RIFT_ASH = ModBlocks.reg("rift_ash", () -> new CarpetBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(0.15F).sound(SoundType.SAND).noOcclusion().pushReaction(PushReaction.DESTROY)));
    /** Hellish candle: black wax, crimson flame. Lit with flint & steel (or a torch) like the ivory ones. */
    public static final RegistryObject<RitualCandleBlock> RIFT_CANDLE = ModBlocks.reg("rift_candle",
            () -> new RitualCandleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.CANDLE).noOcclusion()));

    /** The altar of the Rift: carries the red hexagram floor plate and drives the whole ritual. */
    public static final RegistryObject<RiftAltarBlock> ALTAR = ModBlocks.reg("rift_altar", RiftAltarBlock::new);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RiftAltarBlockEntity>> ALTAR_BE = ModBlockEntities.TYPES.register("rift_altar",
            () -> BlockEntityType.Builder.of(RiftAltarBlockEntity::new, ALTAR.get()).build(null));

    public static void init() {
    }
}
