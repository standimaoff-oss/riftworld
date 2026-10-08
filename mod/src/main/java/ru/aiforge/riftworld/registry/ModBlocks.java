package ru.aiforge.riftworld.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.CrateBlock;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.block.PartBlock;

/**
 * Every block of the mod. The laboratory is gone (Rev 5): what is left here are the shared helpers,
 * the Ashen-Lands magma meander, the invisible multiblock filler and the 3D model blocks.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RiftWorld.MODID);
    /** Blocks that get a BlockItem, in creative-tab order. */
    public static final List<RegistryObject<? extends Block>> WITH_ITEM = new ArrayList<>();
    public static final Map<ModelKind, RegistryObject<ModelBlock>> MODELS = new EnumMap<>(ModelKind.class);

    private ModBlocks() {
    }

    static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> s) {
        RegistryObject<T> r = BLOCKS.register(id, s);
        WITH_ITEM.add(r);
        return r;
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    private static RegistryObject<Block> cube(String id, BlockBehaviour.Properties p) {
        return reg(id, () -> new Block(p));
    }

    // --- building blocks
    public static final RegistryObject<Block> MAGMA_MEANDER = reg("magma_meander", () -> new ru.aiforge.riftworld.block.MagmaMeanderBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5F).sound(SoundType.BASALT).lightLevel(st -> 13)
                    .emissiveRendering((st, l, pos) -> true).hasPostProcess((st, l, pos) -> true).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> WALL_VENT = cube("wall_vent", metal());

    // --- invisible multiblock filler
    public static final RegistryObject<Block> PART = BLOCKS.register("part", () -> new PartBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));

    static {
        for (ModelKind k : ModelKind.values()) {
            if (!MODELS.containsKey(k)) model(k);
        }
        ElisarBlocks.init();
        CastleBlocks.init();
        ArenaBlocks.init();
        TempleBlocks.init();
        RiftBlocks.init();
    }

    private static RegistryObject<ModelBlock> model(ModelKind k) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL)
                .noOcclusion().pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, pp) -> false).isSuffocating((s, l, pp) -> false);
        if (k == ModelKind.CRATE_WOOD) p = p.sound(SoundType.WOOD).strength(2.0F, 3.0F).mapColor(MapColor.WOOD);
        if (k == ModelKind.CRATE_METAL) p = p.strength(3.0F, 6.0F).mapColor(MapColor.COLOR_GRAY);
        if (k.passable()) p = p.noCollission();
        if (k.isElisar()) {
            p = p.sound(SoundType.WOOD).strength(1.5F, 3.0F).mapColor(MapColor.WOOD).lightLevel(s -> k.light(s.getValue(ModelBlock.POWERED)));
            if (k == ModelKind.WELL || k == ModelKind.IDOL || k == ModelKind.ALTAR_STONE || k == ModelKind.FORGE_HEARTH || k == ModelKind.FIREPLACE)
                p = p.sound(SoundType.STONE).strength(3.0F, 6.0F).mapColor(MapColor.STONE);
            if (k == ModelKind.CANDLE_STAND) p = p.sound(SoundType.LANTERN).strength(1.0F, 2.0F).mapColor(MapColor.METAL);
            if (k == ModelKind.BOSS_THRONE) p = p.strength(25.0F, 1200.0F).mapColor(MapColor.COLOR_BLACK);
            if (k == ModelKind.CASTLE_CHANDELIER || k == ModelKind.WALL_SCONCE || k == ModelKind.WALL_SHACKLES)
                p = p.sound(SoundType.LANTERN).strength(1.0F, 2.0F).mapColor(MapColor.METAL);
            if (k == ModelKind.GUARD_ARMOR || k == ModelKind.GOLD_HOARD) p = p.sound(SoundType.METAL).strength(2.0F, 4.0F).mapColor(MapColor.METAL);
            if (k == ModelKind.CHAPEL_ALTAR) p = p.sound(SoundType.STONE).strength(3.0F, 6.0F).mapColor(MapColor.STONE);
            if (k == ModelKind.BONE_PILE) p = p.sound(SoundType.BONE_BLOCK).strength(0.6F);
            if (k == ModelKind.TAPESTRY_HORN || k == ModelKind.CANOPY_BED) p = p.sound(SoundType.WOOL).strength(0.8F);
            if (k == ModelKind.RUG) p = p.sound(SoundType.WOOL).strength(0.5F);
        }
        final BlockBehaviour.Properties props = p;
        RegistryObject<ModelBlock> r = reg(k.name().toLowerCase(java.util.Locale.ROOT), () -> switch (k) {
            case CRATE_WOOD, CRATE_METAL, CASTLE_CHEST -> new CrateBlock(k, props);
            default -> new ModelBlock(k, props);
        });
        MODELS.put(k, r);
        return r;
    }
}
