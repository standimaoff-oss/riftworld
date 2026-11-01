package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ModBlockEntities;

/**
 * Якорь гигантского небесного 3D-прорыва пространства ({@code SpatialRift}).
 * Возвращает {@link #INFINITE_EXTENT_AABB}, чтобы уходящий на 168 блоков в небо Разлом
 * никогда не отсекался фрустумом при взгляде вверх в небо.
 */
public class DemonGateBE extends BlockEntity {
    public DemonGateBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_GATE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
