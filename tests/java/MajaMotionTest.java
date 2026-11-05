import ru.aiforge.riftworld.world.MajaMotion;
public final class MajaMotionTest {
    public static void main(String[] args) {
        if (MajaMotion.breach(0) != 0 || MajaMotion.breach(MajaMotion.BREACH_TICKS) != 0) throw new AssertionError("cycle boundary");
        if (MajaMotion.targetY(62, 140) < 85.9) throw new AssertionError("breach apex");
        double y = 60, low = y, high = y;
        for (int t = 0; t <= MajaMotion.PERIOD; t++) {
            double v = MajaMotion.verticalSpeed(y, MajaMotion.targetY(62, t));
            if (Math.abs(v) > .320001) throw new AssertionError("unbounded vertical motion");
            y += v; low = Math.min(low, y); high = Math.max(high, y);
        }
        if (high < 80 || Math.abs(y - 60) > .01) throw new AssertionError("breach or return failed: " + high + ", " + y);
        if (Math.abs(MajaMotion.breach(-1)) > .0001) throw new AssertionError("negative phase");
        System.out.println("OK: breach rises, returns and stays within bounded vertical speed; peak=" + high);
    }
}
