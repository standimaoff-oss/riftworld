package net.minecraft.core;

/** Test-only stub: the handful of direction constants used by the mod's data classes. */
public enum Direction {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;

    public net.minecraft.core.Direction getOpposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    public int getStepX() { return this == EAST ? 1 : this == WEST ? -1 : 0; }
    public int getStepY() { return this == UP ? 1 : this == DOWN ? -1 : 0; }
    public int getStepZ() { return this == SOUTH ? 1 : this == NORTH ? -1 : 0; }
    public boolean isHorizontal() { return getStepY() == 0; }
    public Axis getAxis() { return getStepY() != 0 ? Axis.Y : getStepX() != 0 ? Axis.X : Axis.Z; }

    public enum Axis {
        X, Y, Z;

        public boolean isHorizontal() { return this != Y; }
        public boolean isVertical() { return this == Y; }
    }
}
