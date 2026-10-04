package ru.aiforge.riftworld.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Footprint and GLB of every 3D-model block. Offsets are given in the model frame (front = south):
 * x = to the viewer's right (east), back = rows behind the front row (north), h = height in blocks.
 */
public enum ModelKind {
    PORTAL_HELM("portal_helm", -1, 1, 1, 3, Block.box(0, 0, 0, 16, 16, 16)),
    PORTAL_LEVER("portal_lever", 0, 0, 0, 2, Block.box(3, 0, 3, 13, 16, 13)),
    CONTROL_CONSOLE("control_console", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    LATHE("lathe", 0, 1, 0, 1, Block.box(0, 0, 2, 16, 16, 14)),
    MILLING_MACHINE("milling_machine", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    SPECIMEN_TANK("specimen_tank", 0, 0, 0, 3, Block.box(1, 0, 1, 15, 16, 15)),
    ELECTRICAL_CABINET("electrical_cabinet", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    GENERATOR("generator", -1, 1, 1, 2, Block.box(0, 0, 0, 16, 16, 16)),
    TRANSFORMER("transformer", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    SERVER_RACK("server_rack", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    SECURITY_DESK("security_desk", -1, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    CRATE_WOOD("crate_wood", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 15, 15)),
    CRATE_METAL("crate_metal", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 14, 15)),
    LAB_DOOR("lab_door", 0, 1, 0, 3, Block.box(0, 0, 6, 16, 16, 10)),
    LAB_TABLE("lab_table", 0, 1, 0, 1, Block.box(0, 0, 0, 16, 15, 16)),
    SHELF("shelf", 0, 1, 0, 3, Block.box(0, 0, 1, 16, 16, 15)),
    OFFICE_CHAIR("office_chair", 0, 0, 0, 1, Block.box(3, 0, 3, 13, 15, 13)),
    LOCKER("locker", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    TRASH_BIN_TIPPED("trash_bin_tipped", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 8, 15)),
    CHANDELIER("chandelier", 0, 0, 0, 1, Block.box(4, 5, 4, 12, 16, 12)),
    SIREN("siren", 0, 0, 0, 1, Block.box(3, 4, 0, 13, 14, 10)),
    SHELF_FALLEN("shelf_fallen", -1, 1, 0, 1, Block.box(0, 0, 1, 16, 10, 15)),
    WALL_NOTES("wall_notes", 0, 0, 0, 1, Block.box(1, 2, 0, 15, 14, 2)),
    DESK_COMPUTER("desk_computer", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 13, 16)),
    TABLE_FLIPPED("table_flipped", 0, 1, 0, 1, Block.box(0, 0, 0, 16, 16, 16)),
    BARRELS("barrels", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 14, 15)),
    FIRE_EXTINGUISHER("fire_extinguisher", 0, 0, 0, 1, Block.box(4, 2, 0, 12, 13, 9)),
    BOX_PILE("box_pile", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 13, 15)),
    PAPER_PILE("paper_pile", 0, 0, 0, 1, Block.box(0, 0, 0, 16, 1, 16)),
    CHAIR_FALLEN("chair_fallen", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 9, 15));

    public final String glb;
    public final int x0, x1, back, h;
    public final VoxelShape masterShape;

    ModelKind(String glb, int x0, int x1, int back, int h, VoxelShape shape) {
        this.glb = glb;
        this.x0 = x0;
        this.x1 = x1;
        this.back = back;
        this.h = h;
        this.masterShape = shape;
    }

    public boolean passable() {
        return this == WALL_NOTES || this == FIRE_EXTINGUISHER || this == PAPER_PILE || this == CHANDELIER;
    }

    public boolean isContainer() {
        return this == CRATE_WOOD || this == CRATE_METAL;
    }

    public int width() {
        return x1 - x0 + 1;
    }

    public int depth() {
        return back + 1;
    }

    /** Model-frame offset (x right, z = -back row) rotated to world for the given facing. */
    public static BlockPos rotate(int x, int y, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case EAST -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    /** World offsets of all cells except the master (0,0,0). */
    public List<BlockPos> partOffsets(Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = x0; x <= x1; ++x)
            for (int b = 0; b <= back; ++b)
                for (int y = 0; y < h; ++y) {
                    if (x == 0 && b == 0 && y == 0) continue;
                    out.add(rotate(x, y, -b, facing));
                }
        return out;
    }
}
