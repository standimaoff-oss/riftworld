package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Pool of liquid light left by the ritual: the bridge home. Glowing, walk-through, unmineable. */
public class LightBridgeBlock extends Block {
    public LightBridgeBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission().strength(-1.0F, 3600000.0F)
                .sound(SoundType.AMETHYST).lightLevel(s -> 15).noLootTable());
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) {
        return 1.0F;
    }
}
