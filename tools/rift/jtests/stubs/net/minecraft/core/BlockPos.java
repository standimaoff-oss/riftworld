package net.minecraft.core;

import java.util.Objects;

/**
 * Test-only stub of Minecraft's BlockPos: just enough of the real API for the classes under test
 * (BridgeDeckGeom) to compile and run outside the game. NOT shipped: lives under tools/rift/jtests/stubs.
 */
public class BlockPos implements Comparable<BlockPos> {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);

    private final int x, y, z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public BlockPos offset(int dx, int dy, int dz) { return new BlockPos(x + dx, y + dy, z + dz); }
    public BlockPos offset(int dx, int dz) { return new BlockPos(x + dx, y, z + dz); }
    public BlockPos above() { return new BlockPos(x, y + 1, z); }
    public BlockPos above(int n) { return new BlockPos(x, y + n, z); }
    public BlockPos below() { return new BlockPos(x, y - 1, z); }
    public BlockPos below(int n) { return new BlockPos(x, y - n, z); }
    public BlockPos relative(net.minecraft.core.Direction d) { return this; }
    public BlockPos immutable() { return this; }
    public long asLong() { return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL); }

    @Override
    public int compareTo(BlockPos o) {
        if (y != o.y) return Integer.compare(y, o.y);
        if (z != o.z) return Integer.compare(z, o.z);
        return Integer.compare(x, o.x);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockPos p)) return false;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() { return Objects.hash(x, y, z); }

    @Override
    public String toString() { return "[" + x + ", " + y + ", " + z + "]"; }
}
