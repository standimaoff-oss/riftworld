package ru.aiforge.riftworld.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.CrateBlock;
import ru.aiforge.riftworld.block.GrateBlock;
import ru.aiforge.riftworld.block.LabDoorBlock;
import ru.aiforge.riftworld.block.LabLampBlock;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.block.PartBlock;
import ru.aiforge.riftworld.block.PipeBlock;
import ru.aiforge.riftworld.block.PortalHelmBlock;
import ru.aiforge.riftworld.block.PortalLeverBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RiftWorld.MODID);
    /** Blocks that get a BlockItem, in creative-tab order. */
    public static final List<RegistryObject<? extends Block>> WITH_ITEM = new ArrayList<>();
    public static final Map<ModelKind, RegistryObject<ModelBlock>> MODELS = new EnumMap<>(ModelKind.class);

    private ModBlocks() {
    }

    private static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> s) {
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
    public static final RegistryObject<Block> LAB_PANEL = cube("lab_panel", metal().mapColor(MapColor.SNOW));
    public static final RegistryObject<Block> LAB_PANEL_DIRTY = cube("lab_panel_dirty", metal().mapColor(MapColor.TERRACOTTA_WHITE));
    public static final RegistryObject<Block> LAB_PANEL_STRIPE = cube("lab_panel_stripe", metal().mapColor(MapColor.SNOW));
    public static final RegistryObject<Block> LAB_PANEL_STRIPE_DIRTY = cube("lab_panel_stripe_dirty", metal().mapColor(MapColor.TERRACOTTA_WHITE));
    public static final RegistryObject<Block> LAB_TILE = cube("lab_tile", stone().mapColor(MapColor.QUARTZ));
    public static final RegistryObject<Block> LAB_TILE_CRACKED = cube("lab_tile_cracked", stone().mapColor(MapColor.QUARTZ));
    public static final RegistryObject<Block> STEEL_FLOOR = cube("steel_floor", metal());
    public static final RegistryObject<Block> STEEL_FLOOR_RUSTY = cube("steel_floor_rusty", metal().mapColor(MapColor.TERRACOTTA_BROWN));
    public static final RegistryObject<Block> LAB_CONCRETE = cube("lab_concrete", stone());
    public static final RegistryObject<Block> LAB_CONCRETE_CRACKED = cube("lab_concrete_cracked", stone());
    public static final RegistryObject<Block> LAB_FACADE = cube("lab_facade", stone().mapColor(MapColor.COLOR_LIGHT_GRAY));
    public static final RegistryObject<Block> LAB_CEILING = cube("lab_ceiling", stone().mapColor(MapColor.SNOW).strength(1.5F, 4.0F));
    public static final RegistryObject<Block> LAB_CEILING_DIRTY = cube("lab_ceiling_dirty", stone().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5F, 4.0F));
    public static final RegistryObject<Block> HAZARD_BLOCK = cube("hazard_block", metal().mapColor(MapColor.COLOR_YELLOW));
    public static final RegistryObject<Block> HAZARD_BLOCK_WORN = cube("hazard_block_worn", metal().mapColor(MapColor.COLOR_YELLOW));
    public static final RegistryObject<Block> SHAFT_WALL = cube("shaft_wall", metal().mapColor(MapColor.COLOR_GRAY).strength(5.0F, 12.0F));

    public static final RegistryObject<Block> LAB_CONCRETE_STAIRS = reg("lab_concrete_stairs", () -> new StairBlock(() -> LAB_CONCRETE.get().defaultBlockState(), stone()));
    public static final RegistryObject<Block> LAB_CONCRETE_SLAB = reg("lab_concrete_slab", () -> new SlabBlock(stone()));
    public static final RegistryObject<Block> LAB_TILE_SLAB = reg("lab_tile_slab", () -> new SlabBlock(stone().mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> STEEL_FLOOR_STAIRS = reg("steel_floor_stairs", () -> new StairBlock(() -> STEEL_FLOOR.get().defaultBlockState(), metal()));
    public static final RegistryObject<Block> STEEL_FLOOR_SLAB = reg("steel_floor_slab", () -> new SlabBlock(metal()));

    public static final RegistryObject<Block> METAL_GRATE = reg("metal_grate", () -> new GrateBlock(metal().noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> RUSTY_GRATE = reg("rusty_grate", () -> new GrateBlock(metal().noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> LAB_GLASS = reg("lab_glass", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(1.0F, 6.0F)));
    public static final RegistryObject<Block> LAB_GLASS_CRACKED = reg("lab_glass_cracked", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)));
    public static final RegistryObject<Block> LAB_LAMP = reg("lab_lamp", () -> new LabLampBlock(stone().mapColor(MapColor.SNOW).strength(1.0F)
            .lightLevel(s -> s.getValue(LabLampBlock.LIT) ? 15 : 0).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> EMERGENCY_LAMP = reg("emergency_lamp", () -> new LabLampBlock(stone().mapColor(MapColor.COLOR_RED).strength(1.0F)
            .lightLevel(s -> s.getValue(LabLampBlock.LIT) ? 7 : 0).sound(SoundType.GLASS)));
    public static final RegistryObject<Block> PIPE = reg("pipe", () -> new PipeBlock(metal().noOcclusion()));

    public static final RegistryObject<Block> DEBRIS = reg("debris", () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
            .strength(0.2F).sound(SoundType.GRAVEL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> DEAD_MOSS = reg("dead_moss", () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(0.1F).sound(SoundType.MOSS_CARPET).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> DEAD_VINES = reg("dead_vines", () -> new VineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
            .replaceable().noCollission().randomTicks().strength(0.2F).sound(SoundType.VINE).pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<Block> DIRT_PILE = reg("dirt_pile", () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
            .strength(0.1F).sound(SoundType.SAND).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> LAB_RAILING = reg("lab_railing", () -> new net.minecraft.world.level.block.IronBarsBlock(metal().noOcclusion()));
    public static final RegistryObject<Block> LAB_TILE_STAIRS = reg("lab_tile_stairs", () -> new StairBlock(() -> LAB_TILE.get().defaultBlockState(), stone().mapColor(MapColor.QUARTZ)));
    public static final RegistryObject<Block> WALL_VENT = cube("wall_vent", metal());
    public static final RegistryObject<Block> LAUNCH_CORE = BLOCKS.register("launch_core", () -> new ru.aiforge.riftworld.block.LaunchCoreBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noOcclusion().noLootTable()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> CAPSULE_TERMINAL = BLOCKS.register("capsule_terminal", () -> new ru.aiforge.riftworld.block.CapsuleTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).noOcclusion().noLootTable()));

    // --- invisible multiblock filler
    public static final RegistryObject<Block> PART = BLOCKS.register("part", () -> new PartBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));

    // --- 3D models
    public static final RegistryObject<ModelBlock> PORTAL_HELM = model(ModelKind.PORTAL_HELM);
    public static final RegistryObject<ModelBlock> PORTAL_LEVER = model(ModelKind.PORTAL_LEVER);
    static {
        for (ModelKind k : ModelKind.values()) {
            if (!MODELS.containsKey(k)) model(k);
        }
    }

    private static RegistryObject<ModelBlock> model(ModelKind k) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL)
                .noOcclusion().pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, pp) -> false).isSuffocating((s, l, pp) -> false);
        if (k == ModelKind.CRATE_WOOD || k == ModelKind.LAB_TABLE || k == ModelKind.SHELF) p = p.sound(SoundType.WOOD).strength(2.0F, 3.0F).mapColor(MapColor.WOOD);
        if (k == ModelKind.OFFICE_CHAIR || k == ModelKind.CHAIR_FALLEN || k == ModelKind.PAPER_PILE || k == ModelKind.WALL_NOTES
                || k == ModelKind.BOX_PILE || k == ModelKind.TRASH_BIN_TIPPED) p = p.strength(0.8F);
        if (k == ModelKind.PAPER_PILE || k == ModelKind.WALL_NOTES || k == ModelKind.BOX_PILE) p = p.sound(SoundType.WOOL);
        if (k.passable()) p = p.noCollission();
        if (k == ModelKind.CHANDELIER) p = p.lightLevel(s -> s.getValue(ModelBlock.POWERED) ? 15 : 0);
        if (k == ModelKind.SIREN) p = p.lightLevel(s -> s.getValue(ModelBlock.POWERED) ? 6 : 0);
        if (k == ModelKind.PORTAL_HELM || k == ModelKind.PORTAL_LEVER) p = p.strength(-1.0F, 3600000.0F).noLootTable();
        final BlockBehaviour.Properties props = p;
        RegistryObject<ModelBlock> r = reg(k.name().toLowerCase(java.util.Locale.ROOT), () -> switch (k) {
            case PORTAL_HELM -> new PortalHelmBlock(k, props);
            case PORTAL_LEVER -> new PortalLeverBlock(k, props);
            case LAB_DOOR -> new LabDoorBlock(k, props);
            case CRATE_WOOD, CRATE_METAL -> new CrateBlock(k, props);
            default -> new ModelBlock(k, props);
        });
        MODELS.put(k, r);
        return r;
    }
}
