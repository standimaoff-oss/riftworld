package ru.aiforge.riftworld.world;

/** Pure, bounded movement schedule; geometry still uses the full collision envelopes. */
public final class MajaMotion {
    public static final int PERIOD = 1800;
    public static final int BREACH_TICKS = 280;
    private MajaMotion() {}
    public static double breach(int tick) {
        int t = Math.floorMod(tick, PERIOD);
        return t < BREACH_TICKS ? Math.sin(Math.PI * t / BREACH_TICKS) : 0.0;
    }
    public static double targetY(int sea, int tick) {
        return sea - 2.0 + 26.0 * breach(tick);
    }
    public static double verticalSpeed(double y, double target) {
        return Math.max(-0.32, Math.min(0.32, (target - y) * 0.055));
    }
}
