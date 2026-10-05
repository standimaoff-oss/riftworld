package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.arena.ArenaCoreBlock;
import ru.aiforge.riftworld.arena.ArenaCoreBlockEntity;
import ru.aiforge.riftworld.arena.BossColumnBlock;
import ru.aiforge.riftworld.arena.BossColumnBlockEntity;

/** Rev 3 / C-3: throne-hall mechanics (healing columns, arena heart). Placed by the castle template only (no items). */
public final class ArenaBlocks {
    private ArenaBlocks() {
    }

    public static final RegistryObject<Block> BOSS_COLUMN = ModBlocks.BLOCKS.register("boss_column", () -> new BossColumnBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).sound(SoundType.STONE).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final RegistryObject<Block> ARENA_CORE = ModBlocks.BLOCKS.register("arena_core", () -> new ArenaCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).sound(SoundType.STONE).noLootTable()
                    .pushReaction(PushReaction.BLOCK)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BossColumnBlockEntity>> COLUMN_BE = ModBlockEntities.TYPES.register("boss_column",
            () -> BlockEntityType.Builder.of(BossColumnBlockEntity::new, BOSS_COLUMN.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArenaCoreBlockEntity>> CORE_BE = ModBlockEntities.TYPES.register("arena_core",
            () -> BlockEntityType.Builder.of(ArenaCoreBlockEntity::new, ARENA_CORE.get()).build(null));

    public static void init() {
    }
}
