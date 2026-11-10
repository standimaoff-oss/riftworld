package ru.aiforge.riftworld.entity.fauna;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.MorokBlocks;

/**
 * Shared ecological helpers for Riftworld v0.47.0 Elisar fauna:
 * Elisar + vanilla water detection, Elisar flora/crystal/light checks, and smooth searches.
 */
public final class ElisarFaunaUtil {
    private ElisarFaunaUtil() {
    }

    /** True if {@code pos} holds Elisar sea water, Garden turquoise water, or vanilla water. */
    public static boolean isAnyWater(Level level, BlockPos pos) {
        BlockState bs = level.getBlockState(pos);
        if (bs.is(ElisarBlocks.WATER.get()) || bs.is(ElisarBlocks.GARDEN_WATER.get()) || bs.is(Blocks.WATER)) {
            return true;
        }
        FluidState fs = bs.getFluidState();
        return !fs.isEmpty() && (fs.is(FluidTags.WATER)
                || bs.getFluidState().getFluidType() == ru.aiforge.riftworld.registry.ModFluids.TYPE.get()
                || bs.getFluidState().getFluidType() == ru.aiforge.riftworld.registry.ModFluids.GARDEN_TYPE.get());
    }

    /** True if the entity's body or feet are submerged in Elisar or vanilla water. */
    public static boolean isSubmergedInAnyWater(Entity entity) {
        if (entity.isInWaterOrBubble()) return true;
        BlockPos feet = entity.blockPosition();
        BlockPos mid = BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.45D, entity.getZ());
        return isAnyWater(entity.level(), feet) || isAnyWater(entity.level(), mid);
    }

    /** Elisar or vanilla flowers appreciated by Porcelain Crane, Amethyst Fox, Blossom Snail, etc. */
    public static boolean isElisarFlowerBlock(BlockState state) {
        return state.is(ElisarBlocks.WHITE_LILY_CLUSTER.get())
                || state.is(ElisarBlocks.FLOWERS.get())
                || state.is(ElisarBlocks.VIOLET_BLOOM.get())
                || state.is(ElisarBlocks.LILAC_BLOOM.get())
                || state.is(ElisarBlocks.PALE_BLOOM.get())
                || state.is(ElisarBlocks.LAVENDER_BED.get())
                || state.is(BlockTags.FLOWERS);
    }

    public static boolean isFlowerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ItemTags.FLOWERS)) return true;
        return stack.is(ElisarBlocks.WHITE_LILY_CLUSTER.get().asItem())
                || stack.is(ElisarBlocks.FLOWERS.get().asItem())
                || stack.is(ElisarBlocks.VIOLET_BLOOM.get().asItem())
                || stack.is(ElisarBlocks.LILAC_BLOOM.get().asItem())
                || stack.is(ElisarBlocks.PALE_BLOOM.get().asItem())
                || stack.is(ElisarBlocks.LAVENDER_BED.get().asItem());
    }

    public static boolean isFoxOrLeafwingTreat(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || isFlowerItem(stack)
                || stack.is(ElisarBlocks.CELADON_FERN.get().asItem());
    }

    /** Valuable crystal / ore blocks sniffed out by a trusted Amethyst Fox. */
    public static boolean isValuableCrystalOrOre(BlockState state) {
        return state.is(ElisarBlocks.RIFTCRYSTAL_ORE.get())
                || state.is(ElisarBlocks.DUCAT_ORE.get())
                || state.is(ElisarBlocks.DEEPSLATE_DUCAT_ORE.get())
                || state.is(ElisarBlocks.AURITE_ORE.get())
                || state.is(Blocks.AMETHYST_CLUSTER)
                || state.is(Blocks.LARGE_AMETHYST_BUD)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.AMETHYST_BLOCK);
    }

    /** Crystalline surfaces for Crystal Gecko camouflage. */
    public static boolean isCrystalSurface(BlockState state) {
        return state.is(ElisarBlocks.CRYSTAL_BLOCK.get())
                || state.is(ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get())
                || state.is(ElisarBlocks.CRYSTAL_BLOCK_DEEP.get())
                || state.is(ElisarBlocks.CRYSTAL_CLUSTER.get())
                || state.is(ElisarBlocks.RIFT_MARBLE.get())
                || state.is(ElisarBlocks.RIFT_MARBLE_MOSSY.get())
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.AMETHYST_CLUSTER)
                || state.is(Blocks.CALCITE);
    }

    /** Warm lanterns/torches/lava sought by nocturnal Ash Moths. */
    public static boolean isWarmBeacon(BlockState state) {
        return state.is(ElisarBlocks.LANTERN.get())
                || state.is(ElisarBlocks.CAGE_LANTERN.get())
                || state.is(ElisarBlocks.PEARL_PRISM_LANTERN.get())
                || state.is(MorokBlocks.FOG_LANTERN.get())
                || state.is(CastleBlocks.CARVED_BASALT_RUNES.get())
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SHROOMLIGHT)
                || state.is(Blocks.GLOWSTONE);
    }

    /** Coral blocks/plants near which Reef Seahorses and Hermit Crabs congregate. */
    public static boolean isReefCoral(BlockState state) {
        return state.is(ElisarBlocks.CORAL_BRAIN.get())
                || state.is(ElisarBlocks.CORAL_BRANCH.get())
                || state.is(ElisarBlocks.CORAL_FAN.get())
                || state.is(ElisarBlocks.CORAL_TUBE.get())
                || state.is(ElisarBlocks.CORAL_GLOW.get())
                || state.is(ElisarBlocks.CORAL_CAP_PINK.get())
                || state.is(ElisarBlocks.CORAL_CAP_TEAL.get())
                || state.is(ElisarBlocks.CORAL_CAP_VIOLET.get())
                || state.is(ElisarBlocks.ANEMONE.get())
                || state.is(ElisarBlocks.CORAL_BUSH.get())
                || state.is(BlockTags.CORALS)
                || state.is(BlockTags.CORAL_BLOCKS);
    }

    @Nullable
    public static BlockPos findNearestBlock(Level level, BlockPos origin, int rx, int ry, int rz,
                                            java.util.function.Predicate<BlockState> pred) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -ry; dy <= ry; ++dy) {
            for (int dx = -rx; dx <= rx; ++dx) {
                for (int dz = -rz; dz <= rz; ++dz) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunk(cursor.getX() >> 4, cursor.getZ() >> 4)) continue;
                    if (pred.test(level.getBlockState(cursor))) {
                        double d2 = origin.distSqr(cursor);
                        if (d2 < bestDist) {
                            bestDist = d2;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}
