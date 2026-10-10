package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.block.BridgePostBlock;
import ru.aiforge.riftworld.block.BridgeWalkBlock;

/** Rev 4 item 13: the self-assembling swaying rope bridge (4 posts, rope, planks). */
public final class BridgeBlocks {
    private BridgeBlocks() {
    }

    private static BlockBehaviour.Properties p(MapColor c, float h, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(h, h * 3).sound(s);
    }

    public static final RegistryObject<BridgePostBlock> POST = ModBlocks.reg("bridge_post", () -> new BridgePostBlock(p(MapColor.WOOD, 2.0F, SoundType.WOOD)));
    /** Invisible collision cells of the deck (no item, no loot - owned by the posts). */
    public static final RegistryObject<BridgeWalkBlock> WALK =
            ModBlocks.BLOCKS.register("bridge_walk", () -> new BridgeWalkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                    .strength(0.4F).sound(SoundType.WOOD).noOcclusion().noLootTable()
                    .isViewBlocking((s, l, pp) -> false).isSuffocating((s, l, pp) -> false)));

    public static void init() {
    }
}
