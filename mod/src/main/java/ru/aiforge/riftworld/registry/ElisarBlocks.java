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
    public static final RegistryObject<Block> SIZO_PLANKS = cube("sizo_planks", MapColor.COLOR_LIGHT_GRAY, 2.0F, SoundType.WOOD);
    public static final RegistryObject<Block> AMBRA_PLANKS = cube("ambra_planks", MapColor.SAND, 2.0F, SoundType.WOOD);
    public static final RegistryObject<Block> THATCH = cube("thatch", MapColor.COLOR_YELLOW, 0.5F, SoundType.GRASS);
    public static final RegistryObject<Block> ROOF_RED = cube("roof_red", MapColor.COLOR_RED, 1.5F, SoundType.STONE);
    public static final RegistryObject<Block> ROOF_BLUE = cube("roof_blue", MapColor.COLOR_BLUE, 1.5F, SoundType.STONE);
    // trees
    public static final RegistryObject<RotatedPillarBlock> SIZO_LOG = ModBlocks.reg("sizo_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_LIGHT_GRAY, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<RotatedPillarBlock> AMBRA_LOG = ModBlocks.reg("ambra_log", () -> new RotatedPillarBlock(p(MapColor.COLOR_LIGHT_GRAY, 2.0F, SoundType.WOOD)));
    // Рев 12: листва снова белая/холодная — на карте и в палитре мира цвет должен совпадать
    public static final RegistryObject<LeavesBlock> SIZO_LEAVES = ModBlocks.reg("sizo_leaves", () -> new LeavesBlock(leaves(MapColor.SNOW)));
    public static final RegistryObject<LeavesBlock> AMBRA_LEAVES = ModBlocks.reg("ambra_leaves", () -> new LeavesBlock(leaves(MapColor.QUARTZ)));
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
    /** Рев 14.25: плющ Висячих садов — сочно-зелёный (на референсе стены увиты зеленью, а не сухими плетями). */
    public static final RegistryObject<VineBlock> GARDEN_IVY = ModBlocks.reg("garden_ivy", () -> new VineBlock(plant().randomTicks()));
    // ----- Рев 14 «Висячие сады по референсу»: у биома 07 своя зелёная палитра.
    // На референсе полки — ярко-зелёная трава, обрывы увиты плющом, деревья — обычные дубы
    // с зелёной кроной и бурой корой. В Элисаре трава кремовая, а деревья белые, поэтому
    // садам нужны собственные блоки, иначе биом не совпадает с фотографией.
    public static final RegistryObject<Block> GARDEN_GRASS = cube("garden_grass", MapColor.GRASS, 0.6F, SoundType.GRASS);
    public static final RegistryObject<RotatedPillarBlock> GARDEN_LOG =
            ModBlocks.reg("garden_log", () -> new RotatedPillarBlock(p(MapColor.WOOD, 2.0F, SoundType.WOOD)));
    public static final RegistryObject<LeavesBlock> GARDEN_LEAVES =
            ModBlocks.reg("garden_leaves", () -> new LeavesBlock(leaves(MapColor.PLANT)));
    // ----- Рев 14.29 «Фиолетовые холмы по референсу»: на фото деревья с СИРЕНЕВОЙ кроной
    // (лиловые шапки над полями лаванды). У Элисара деревья белые, поэтому биому 04 нужна
    // собственная листва — иначе поля фиолетовые, а деревья чужие.
    public static final RegistryObject<LeavesBlock> VIOLET_LEAVES =
            ModBlocks.reg("violet_leaves", () -> new LeavesBlock(leaves(MapColor.COLOR_PURPLE)));
    // обрывы: светлый известняк горизонтальными пластами (мох — у самого низа стены)
    public static final RegistryObject<Block> GARDEN_LIMESTONE = cube("garden_limestone", MapColor.QUARTZ, 1.5F, SoundType.STONE);
    public static final RegistryObject<Block> GARDEN_LIMESTONE_MOSSY = cube("garden_limestone_mossy", MapColor.QUARTZ, 1.5F, SoundType.STONE);
    // ===== Рев 8 «Живое море»: дно, рифы и подводная флора =====
    // породы дна
    public static final RegistryObject<Block> SILT = cube("elisar_silt", MapColor.COLOR_BLACK, 0.7F, SoundType.SOUL_SAND);
    public static final RegistryObject<Block> REEF_STONE = cube("reef_stone", MapColor.TERRACOTTA_PURPLE, 2.0F, SoundType.STONE);
    public static final RegistryObject<Block> CORAL_BRAIN = ModBlocks.reg("coral_brain",
            () -> new Block(p(MapColor.COLOR_PINK, 1.2F, SoundType.CORAL_BLOCK).lightLevel(s -> 6)));
    // растения (крестики, живут только на дне)
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CORAL_BRANCH =
            ModBlocks.reg("coral_branch", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CORAL_FAN =
            ModBlocks.reg("coral_fan", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CORAL_TUBE =
            ModBlocks.reg("coral_tube", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CORAL_GLOW =
            ModBlocks.reg("coral_glow", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 14)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> SEA_GRASS =
            ModBlocks.reg("sea_grass", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> SEA_KELP =
            ModBlocks.reg("sea_kelp", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant(), true));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> SEA_KELP_TOP =
            ModBlocks.reg("sea_kelp_top", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant(), true));
    // Рев 13: свет 11 не пробивал толщу воды — пещеры выглядели чёрным экраном.
    // 15 — как у морского фонаря: нарост освещает всю полость.
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CAVE_GLOW =
            ModBlocks.reg("cave_glow", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 15)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaDecalBlock> SEA_SHELLS =
            ModBlocks.reg("sea_shells", () -> new ru.aiforge.riftworld.block.SeaDecalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND).strength(0.2F).sound(SoundType.CORAL_BLOCK).noCollission()
                    .pushReaction(PushReaction.DESTROY).noOcclusion()));
    // ----- Рев 10 «Живое море 2»: тропический риф. Три палитры рифовых шапок + новая жизнь на дне.
    public static final RegistryObject<Block> CORAL_CAP_PINK = cube("coral_cap_pink", MapColor.COLOR_PINK, 1.2F, SoundType.CORAL_BLOCK);
    public static final RegistryObject<Block> CORAL_CAP_TEAL = cube("coral_cap_teal", MapColor.WARPED_WART_BLOCK, 1.2F, SoundType.CORAL_BLOCK);
    public static final RegistryObject<Block> CORAL_CAP_VIOLET = cube("coral_cap_violet", MapColor.COLOR_PURPLE, 1.2F, SoundType.CORAL_BLOCK);
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> ANEMONE =
            ModBlocks.reg("anemone", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 9)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> CORAL_BUSH =
            ModBlocks.reg("coral_bush", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> TUBE_WORMS =
            ModBlocks.reg("tube_worms", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 4)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaDecalBlock> SEA_STAR =
            ModBlocks.reg("sea_star", () -> new ru.aiforge.riftworld.block.SeaDecalBlock(decal()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaDecalBlock> SEA_URCHIN =
            ModBlocks.reg("sea_urchin", () -> new ru.aiforge.riftworld.block.SeaDecalBlock(decal()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaDecalBlock> SHELL_BED =
            ModBlocks.reg("shell_bed", () -> new ru.aiforge.riftworld.block.SeaDecalBlock(decal()));

    // ----- Рев 11 «Кораллы-растения»: шесть НОВЫХ кастомных кораллов с настоящей 3D-моделью
    // (ветки под углами, пластины, трубки) — старые крестики остаются на месте, ничего не удаляем.
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> STAGHORN_CORAL =
            ModBlocks.reg("staghorn_coral", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> FEATHER_CORAL =
            ModBlocks.reg("feather_coral", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 3)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> SEA_WHIP =
            ModBlocks.reg("sea_whip", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 5)));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> TABLE_CORAL =
            ModBlocks.reg("table_coral", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> TUBE_SPONGE_CLUSTER =
            ModBlocks.reg("tube_sponge_cluster", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant()));
    public static final RegistryObject<ru.aiforge.riftworld.block.SeaPlantBlock> RIBBON_CORAL =
            ModBlocks.reg("ribbon_coral", () -> new ru.aiforge.riftworld.block.SeaPlantBlock(seaPlant().lightLevel(s -> 7)));

    // ----- Рев 12: СТВОЛЫ коралловых деревьев. Раньше ствол и ветви складывались из полных кубов
    // шапок — на скриншотах это читалось как «стопка блоков», а не как растение. Ствол — узкая
    // колонна 6x16x6 (своя модель, не полный куб), поэтому силуэт ветви читается.
    public static final RegistryObject<Block> CORAL_STEM_PINK = ModBlocks.reg("coral_stem_pink", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.CORAL_BLOCK).noOcclusion()));
    public static final RegistryObject<Block> CORAL_STEM_TEAL = ModBlocks.reg("coral_stem_teal", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_WART_BLOCK).strength(1.0F).sound(SoundType.CORAL_BLOCK).noOcclusion()));
    public static final RegistryObject<Block> CORAL_STEM_VIOLET = ModBlocks.reg("coral_stem_violet", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.CORAL_BLOCK).noOcclusion()));

    // ----- Рев 12: коралловая листва для аддона Dynamic Trees.
    // DT генерирует собственный блок листьев и берёт модель у «примитивного» блока, поэтому нужен
    // обычный LeavesBlock с готовой текстурой — иначе коралловое дерево рендерится пустым кубом.
    public static final RegistryObject<LeavesBlock> CORAL_FOLIAGE_PINK =
            ModBlocks.reg("coral_foliage_pink", () -> new LeavesBlock(leaves(MapColor.COLOR_PINK)));
    public static final RegistryObject<LeavesBlock> CORAL_FOLIAGE_TEAL =
            ModBlocks.reg("coral_foliage_teal", () -> new LeavesBlock(leaves(MapColor.WARPED_WART_BLOCK)));
    public static final RegistryObject<LeavesBlock> CORAL_FOLIAGE_VIOLET =
            ModBlocks.reg("coral_foliage_violet", () -> new LeavesBlock(leaves(MapColor.COLOR_PURPLE)));

    private static BlockBehaviour.Properties decal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.2F).sound(SoundType.CORAL_BLOCK)
                .noCollission().pushReaction(PushReaction.DESTROY).noOcclusion();
    }

    private static BlockBehaviour.Properties seaPlant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).replaceable().noCollission().instabreak()
                .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY).noOcclusion();
    }

    /** Рев 14.24: бирюзовая вода Висячих садов (своя текстура). */
    public static final RegistryObject<LiquidBlock> GARDEN_WATER = ModBlocks.BLOCKS.register("garden_water", () -> new LiquidBlock(ModFluids.GARDEN_SOURCE,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).replaceable().noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY)
                    .noLootTable().liquid()));

    // water (no item: the bucket is the item)
    public static final RegistryObject<LiquidBlock> WATER = ModBlocks.BLOCKS.register("elisar_water", () -> new LiquidBlock(ModFluids.SOURCE,
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
