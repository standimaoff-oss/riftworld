package ru.aiforge.riftworld.arena;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.registry.ArenaBlocks;

/** Hidden heart of the throne hall (inside the dais, looks like ashlar): runs the entry / awakening / column sequence. */
public class ArenaCoreBlock extends Block implements EntityBlock {
    public ArenaCoreBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArenaCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ArenaBlocks.CORE_BE.get()) return null;
        return (l, p, s, be) -> ((ArenaCoreBlockEntity) be).tick((ServerLevel) l);
    }
}
