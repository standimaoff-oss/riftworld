package ru.aiforge.riftworld.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.StrippablePillarBlock;

/**
 * v0.31.0 — блоки биома «Мороковая Падь» ({@code riftworld:morok_hollow}).
 *
 * <p>Всё строго в ванильных формах: кубы, колонны, лестницы/плиты/ограды, крестики,
 * ковёр и фонарь. Никаких «супер-моделей», только 16x16 текстуры из
 * {@code tools/rift/gen_v031_morok.py}.
 *
 * <p>Палитра концепта: кость {@code #D9D2C4} + чёрные трещины {@code #1B1B1F},
 * хвоя {@code #4A5F58}, мёрзлая земля {@code #22262B}, мох {@code #9FAFA6},
 * лёд {@code #BBD6E4}, холодный свет фонаря {@code #8FF0F0}.
 */
public final class MorokBlocks {
    private MorokBlocks() {
    }

    // ------------------------------------------------------------------ helpers
    private static BlockBehaviour.Properties p(MapColor c, float h, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(h, h * 2).sound(s);
    }

    private static RegistryObject<Block> cube(String id, MapColor c, float h, SoundType s) {
        return ModBlocks.reg(id, () -> new Block(p(c, h, s)));
    }

    private static BlockBehaviour.Properties leavesProps(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(0.2F).sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn((s, l, pp, e) -> false).isSuffocating((s, l, pp) -> false).isViewBlocking((s, l, pp) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties plantProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak()
                .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    private static RegistryObject<RotatedPillarBlock> woodPillar(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new RotatedPillarBlock(p(c, 2.0F, SoundType.WOOD)));
    }

    private static RegistryObject<RotatedPillarBlock> strippableWood(String id, MapColor c,
                                                                     java.util.function.Supplier<? extends Block> stripped) {
        return ModBlocks.reg(id, () -> new StrippablePillarBlock(p(c, 2.0F, SoundType.WOOD), stripped));
    }

    private static RegistryObject<StairBlock> stairs(String id, RegistryObject<Block> base) {
        return ModBlocks.reg(id, () -> new StairBlock(() -> base.get().defaultBlockState(),
                BlockBehaviour.Properties.copy(base.get())));
    }

    private static RegistryObject<SlabBlock> slab(String id, MapColor c, SoundType s) {
        return ModBlocks.reg(id, () -> new SlabBlock(p(c, 1.5F, s)));
    }

    private static RegistryObject<WallBlock> stoneWall(String id, MapColor c, SoundType s) {
        return ModBlocks.reg(id, () -> new WallBlock(p(c, 2.0F, s)));
    }

    private static RegistryObject<FenceBlock> woodFence(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new FenceBlock(p(c, 2.0F, SoundType.WOOD)));
    }

    private static RegistryObject<FenceGateBlock> woodGate(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new FenceGateBlock(p(c, 2.0F, SoundType.WOOD), WoodType.OAK));
    }

    private static RegistryObject<DoorBlock> woodDoor(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new DoorBlock(p(c, 3.0F, SoundType.WOOD).noOcclusion()
                .pushReaction(PushReaction.DESTROY), BlockSetType.OAK));
    }

    private static RegistryObject<TrapDoorBlock> woodTrapdoor(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new TrapDoorBlock(p(c, 3.0F, SoundType.WOOD).noOcclusion(), BlockSetType.OAK));
    }

    private static RegistryObject<PressurePlateBlock> woodPlate(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                p(c, 0.5F, SoundType.WOOD).noCollission().pushReaction(PushReaction.DESTROY), BlockSetType.OAK));
    }

    private static RegistryObject<ButtonBlock> woodButton(String id, MapColor c) {
        return ModBlocks.reg(id, () -> new ButtonBlock(p(c, 0.5F, SoundType.WOOD).noCollission()
                .pushReaction(PushReaction.DESTROY), BlockSetType.OAK, 30, true));
    }

    // ------------------------------------------------------------------ дерево
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_MOROK_LOG = woodPillar("stripped_morok_log", MapColor.QUARTZ);
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_MOROK_WOOD = woodPillar("stripped_morok_wood", MapColor.QUARTZ);
    public static final RegistryObject<RotatedPillarBlock> MOROK_LOG =
            strippableWood("morok_log", MapColor.QUARTZ, STRIPPED_MOROK_LOG);
    public static final RegistryObject<RotatedPillarBlock> MOROK_WOOD =
            strippableWood("morok_wood", MapColor.QUARTZ, STRIPPED_MOROK_WOOD);

    public static final RegistryObject<Block> MOROK_PLANKS = cube("morok_planks", MapColor.QUARTZ, 2.0F, SoundType.WOOD);
    public static final RegistryObject<StairBlock> MOROK_STAIRS = stairs("morok_planks_stairs", MOROK_PLANKS);
    public static final RegistryObject<SlabBlock> MOROK_SLAB = slab("morok_planks_slab", MapColor.QUARTZ, SoundType.WOOD);
    public static final RegistryObject<FenceBlock> MOROK_FENCE = woodFence("morok_fence", MapColor.QUARTZ);
    public static final RegistryObject<FenceGateBlock> MOROK_FENCE_GATE = woodGate("morok_fence_gate", MapColor.QUARTZ);
    public static final RegistryObject<DoorBlock> MOROK_DOOR = woodDoor("morok_door", MapColor.QUARTZ);
    public static final RegistryObject<TrapDoorBlock> MOROK_TRAPDOOR = woodTrapdoor("morok_trapdoor", MapColor.QUARTZ);
    public static final RegistryObject<PressurePlateBlock> MOROK_PRESSURE_PLATE = woodPlate("morok_pressure_plate", MapColor.QUARTZ);
    public static final RegistryObject<ButtonBlock> MOROK_BUTTON = woodButton("morok_button", MapColor.QUARTZ);
    public static final RegistryObject<LeavesBlock> MOROK_LEAVES =
            ModBlocks.reg("morok_leaves", () -> new LeavesBlock(leavesProps(MapColor.PLANT)));
    public static final RegistryObject<PlantBlock> MOROK_SAPLING =
            ModBlocks.reg("morok_sapling", () -> new PlantBlock(plantProps()));

    // ------------------------------------------------------------------ v0.49.0: два своих дерева Пади
    // «Гнилая роща» (Rotwood): тёмное, узловатое, с чёрной древесиной и густой
    // кроной — основа ночного леса Пади. «Морозный терновник» (Frostbriar):
    // скрученный, колючий кустарник с ледяной листвой — растёт по краям болот.
    // Своё дерево нужно, чтобы лес читался как МОСТРОЕ место, а не как обычный
    // берёзовый лес с перекрашенной землёй.
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_ROTWOOD_LOG =
            woodPillar("stripped_rotwood_log", MapColor.COLOR_BLACK);
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_ROTWOOD_WOOD =
            woodPillar("stripped_rotwood_wood", MapColor.COLOR_BLACK);
    public static final RegistryObject<RotatedPillarBlock> ROTWOOD_LOG =
            strippableWood("rotwood_log", MapColor.COLOR_BLACK, STRIPPED_ROTWOOD_LOG);
    public static final RegistryObject<RotatedPillarBlock> ROTWOOD_WOOD =
            strippableWood("rotwood_wood", MapColor.COLOR_BLACK, STRIPPED_ROTWOOD_WOOD);

    public static final RegistryObject<Block> ROTWOOD_PLANKS = cube("rotwood_planks", MapColor.COLOR_BLACK, 2.0F, SoundType.WOOD);
    public static final RegistryObject<StairBlock> ROTWOOD_STAIRS = stairs("rotwood_planks_stairs", ROTWOOD_PLANKS);
    public static final RegistryObject<SlabBlock> ROTWOOD_SLAB = slab("rotwood_planks_slab", MapColor.COLOR_BLACK, SoundType.WOOD);
    public static final RegistryObject<FenceBlock> ROTWOOD_FENCE = woodFence("rotwood_fence", MapColor.COLOR_BLACK);
    public static final RegistryObject<FenceGateBlock> ROTWOOD_FENCE_GATE = woodGate("rotwood_fence_gate", MapColor.COLOR_BLACK);
    public static final RegistryObject<DoorBlock> ROTWOOD_DOOR = woodDoor("rotwood_door", MapColor.COLOR_BLACK);
    public static final RegistryObject<TrapDoorBlock> ROTWOOD_TRAPDOOR = woodTrapdoor("rotwood_trapdoor", MapColor.COLOR_BLACK);
    public static final RegistryObject<PressurePlateBlock> ROTWOOD_PRESSURE_PLATE =
            woodPlate("rotwood_pressure_plate", MapColor.COLOR_BLACK);
    public static final RegistryObject<ButtonBlock> ROTWOOD_BUTTON = woodButton("rotwood_button", MapColor.COLOR_BLACK);
    public static final RegistryObject<LeavesBlock> ROTWOOD_LEAVES =
            ModBlocks.reg("rotwood_leaves", () -> new LeavesBlock(leavesProps(MapColor.COLOR_DARK_GREEN)));
    public static final RegistryObject<PlantBlock> ROTWOOD_SAPLING =
            ModBlocks.reg("rotwood_sapling", () -> new PlantBlock(plantProps()));

    public static final RegistryObject<RotatedPillarBlock> STRIPPED_FROSTBRIAR_LOG =
            woodPillar("stripped_frostbriar_log", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_FROSTBRIAR_WOOD =
            woodPillar("stripped_frostbriar_wood", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<RotatedPillarBlock> FROSTBRIAR_LOG =
            strippableWood("frostbriar_log", MapColor.COLOR_LIGHT_GRAY, STRIPPED_FROSTBRIAR_LOG);
    public static final RegistryObject<RotatedPillarBlock> FROSTBRIAR_WOOD =
            strippableWood("frostbriar_wood", MapColor.COLOR_LIGHT_GRAY, STRIPPED_FROSTBRIAR_WOOD);

    public static final RegistryObject<Block> FROSTBRIAR_PLANKS = cube("frostbriar_planks", MapColor.COLOR_LIGHT_GRAY, 2.0F, SoundType.WOOD);
    public static final RegistryObject<StairBlock> FROSTBRIAR_STAIRS = stairs("frostbriar_planks_stairs", FROSTBRIAR_PLANKS);
    public static final RegistryObject<SlabBlock> FROSTBRIAR_SLAB = slab("frostbriar_planks_slab", MapColor.COLOR_LIGHT_GRAY, SoundType.WOOD);
    public static final RegistryObject<FenceBlock> FROSTBRIAR_FENCE = woodFence("frostbriar_fence", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<FenceGateBlock> FROSTBRIAR_FENCE_GATE = woodGate("frostbriar_fence_gate", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<DoorBlock> FROSTBRIAR_DOOR = woodDoor("frostbriar_door", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<TrapDoorBlock> FROSTBRIAR_TRAPDOOR = woodTrapdoor("frostbriar_trapdoor", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<PressurePlateBlock> FROSTBRIAR_PRESSURE_PLATE =
            woodPlate("frostbriar_pressure_plate", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<ButtonBlock> FROSTBRIAR_BUTTON = woodButton("frostbriar_button", MapColor.COLOR_LIGHT_GRAY);
    public static final RegistryObject<LeavesBlock> FROSTBRIAR_LEAVES =
            ModBlocks.reg("frostbriar_leaves", () -> new LeavesBlock(leavesProps(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<PlantBlock> FROSTBRIAR_SAPLING =
            ModBlocks.reg("frostbriar_sapling", () -> new PlantBlock(plantProps()));

    // ------------------------------------------------------------------ земля
    public static final RegistryObject<Block> FROZEN_SOIL = cube("frozen_soil", MapColor.COLOR_BLACK, 0.5F, SoundType.GRAVEL);
    public static final RegistryObject<Block> RIME_GRASS = cube("rime_grass", MapColor.COLOR_LIGHT_GREEN, 0.6F, SoundType.GRASS);
    public static final RegistryObject<Block> PALE_MOSS_BLOCK = cube("pale_moss_block", MapColor.COLOR_LIGHT_GREEN, 0.5F, SoundType.MOSS);
    public static final RegistryObject<CarpetBlock> PALE_MOSS_CARPET = ModBlocks.reg("pale_moss_carpet",
            () -> new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.1F).sound(SoundType.MOSS).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<PlantBlock> ICE_FANG =
            ModBlocks.reg("ice_fang", () -> new PlantBlock(plantProps().sound(SoundType.GLASS)));
    public static final RegistryObject<PlantBlock> BONE_BRAMBLE =
            ModBlocks.reg("bone_bramble", () -> new PlantBlock(plantProps()));

    // ------------------------------------------------------------------ руины
    public static final RegistryObject<Block> CRACKED_RIME_BRICKS =
            cube("cracked_rime_bricks", MapColor.QUARTZ, 2.0F, SoundType.STONE);
    public static final RegistryObject<StairBlock> CRACKED_RIME_BRICKS_STAIRS =
            stairs("cracked_rime_bricks_stairs", CRACKED_RIME_BRICKS);
    public static final RegistryObject<SlabBlock> CRACKED_RIME_BRICKS_SLAB =
            slab("cracked_rime_bricks_slab", MapColor.QUARTZ, SoundType.STONE);
    public static final RegistryObject<WallBlock> CRACKED_RIME_BRICKS_WALL =
            stoneWall("cracked_rime_bricks_wall", MapColor.QUARTZ, SoundType.STONE);
    public static final RegistryObject<RotatedPillarBlock> RIME_BRICK_PILLAR =
            woodPillar("rime_brick_pillar", MapColor.QUARTZ);

    // ------------------------------------------------------------------ свет
    /** Холодный фонарь биома: свет 7 (в тумане виден, но не «прожектор»). */
    public static final RegistryObject<net.minecraft.world.level.block.LanternBlock> FOG_LANTERN =
            ModBlocks.reg("fog_lantern", () -> new net.minecraft.world.level.block.LanternBlock(
                    p(MapColor.METAL, 1.0F, SoundType.LANTERN).lightLevel(s -> 7).noOcclusion()));

    public static void init() {
    }

    /** Крестик-растение, растущее только на мерзлоте Мороковой Пади. */
    public static class PlantBlock extends BushBlock {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

        public PlantBlock(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected boolean mayPlaceOn(BlockState s, BlockGetter level, BlockPos pos) {
            return s.is(FROZEN_SOIL.get()) || s.is(RIME_GRASS.get()) || s.is(PALE_MOSS_BLOCK.get())
                    || s.is(ElisarBlocks.SOIL.get()) || s.is(ElisarBlocks.GRASS.get())
                    || s.is(net.minecraft.tags.BlockTags.DIRT);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE.move(s.getOffset(level, pos).x, 0, s.getOffset(level, pos).z);
        }
    }
}
